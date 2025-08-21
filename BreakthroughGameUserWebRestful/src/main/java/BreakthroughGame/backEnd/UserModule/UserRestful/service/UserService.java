// 文件：BreakthroughGame/backEnd/UserModule/UserRestful/service/UserService.java
package BreakthroughGame.backEnd.UserModule.UserRestful.service;

import BreakthroughGame.backEnd.UserModule.UserInfo.entity.User;
import BreakthroughGame.backEnd.UserModule.UserInfo.entity.GameCharacter;             // 中文备注：人物实体
import BreakthroughGame.backEnd.UserModule.UserInfo.repository.UserRepository;
import BreakthroughGame.backEnd.UserModule.UserRestful.common.api.ResultCode;
import BreakthroughGame.backEnd.UserModule.UserRestful.common.exception.BizException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;          // 中文备注：读取 pepper、初始经验配置
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Locale;

@Service
public class UserService {

    private static final Logger LOGGER = LoggerFactory.getLogger(UserService.class);

    // ✅ 构造器注入
    private final UserRepository userRepository;            // 中文备注：用户仓储
    private final PasswordEncoder passwordEncoder;          // 中文备注：密码编码器
    private final CharacterService characterService;        // 中文备注：人物服务，用于注册时自动建人物
    private final CharacterXpService characterXpService;    // ✅ 中文备注：人物经验服务，用于初始化经验记录

    // 中文备注：全局 pepper（可选），默认空字符串不启用
    private final String pepper;

    // 中文备注：注册初始化经验（可选），默认 0
    private final long initialXp;

    // 中文备注：固定时延伪比较用的哈希，构造时生成一次
    private final String dummyHash;

    // ✅ 单构造器即可自动注入（@Autowired 可省略）
    public UserService(UserRepository userRepository,
                       PasswordEncoder passwordEncoder,
                       CharacterService characterService,
                       CharacterXpService characterXpService,                         // ✅ 注入经验服务
                       @Value("${security.password.pepper:}") String pepper,
                       @Value("${game.character.initial-xp:0}") long initialXp        // ✅ 可配置初始经验
    ) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.characterService = characterService;
        this.characterXpService = characterXpService;
        this.pepper = pepper == null ? "" : pepper;
        this.initialXp = Math.max(0L, initialXp);           // 中文备注：初始经验保底为非负
        this.dummyHash = passwordEncoder.encode("DUMMY-PASSWORD-" + System.currentTimeMillis());
    }

    /**
     * 注册用户 → 自动创建人物（单用户单角色） → 自动创建经验记录（初始经验，可配置）
     * 事务内：若人物/经验表有唯一约束 (user_id / character_id)，并发下由 DB 兜底保证唯一。
     */
    @Transactional
    public User register(String username, String email, String rawPassword) {
        final String normEmail = email == null ? null : email.trim().toLowerCase(Locale.ROOT); // 中文备注：标准化 email
        final String normUsername = username == null ? null : username.trim();                  // 中文备注：标准化用户名

        if (userRepository.existsByUsername(normUsername)) {
            throw new BizException(ResultCode.A_CONFLICT, "用户名已存在"); // 中文备注：交给全局异常处理统一返回
        }
        if (userRepository.existsByEmail(normEmail)) {
            throw new BizException(ResultCode.A_CONFLICT, "邮箱已存在");
        }

        LOGGER.info("[REGISTER] username={}, email={}", normUsername, normEmail);

        // 中文备注：哈希加 pepper（若配置了）
        String toHash = (rawPassword == null ? "" : rawPassword) + pepper;

        User u = new User();
        u.setUsername(normUsername);
        u.setEmail(normEmail);
        u.setPasswordHash(passwordEncoder.encode(toHash));

        // 1) 先保存用户
        User saved = userRepository.save(u);

        // 2) 再为该用户创建“唯一人物”（若并发或已存在则返回已有）
        GameCharacter character = characterService.createIfAbsent(saved.getId());
        if (character == null || character.getId() == null) {
            // 中文备注：理论不应发生；防御性处理，触发回滚
            throw new BizException(ResultCode.A_CONFLICT, "创建人物失败，请重试");
        }

        // 3) 自动创建经验记录（UPSERT）：Delta=initialXp（默认 0）
        //    - 若记录不存在：插入并由触发器回填冗余字段
        //    - 若记录已存在：增量加到 existing total_xp 上（init>0 时生效）
        characterXpService.initXpForRegistration(character.getId(), initialXp); // 中文备注：只保证存在与初始化

        return saved; // 中文备注：如需返回人物/快照，可在此扩展查询封装 DTO 返回
    }

    /**
     * 认证：邮箱 + 密码
     * 为了恒定时延，不存在用户时也走一次 dummyHash 的 matches。
     */
    @Transactional(readOnly = true)
    public boolean authenticate(String email, String rawPassword) {
        final String normEmail = email == null ? null : email.trim().toLowerCase(Locale.ROOT);
        final String toCheck = (rawPassword == null ? "" : rawPassword) + pepper;

        return userRepository.findByEmail(normEmail)
                .map(u -> passwordEncoder.matches(toCheck, u.getPasswordHash()))
                .orElseGet(() -> passwordEncoder.matches(toCheck, dummyHash)); // 中文备注：统一时延的伪比较
    }

    /**
     * 登录后需要用到：通过邮箱拿用户，不存在抛业务异常
     */
    @Transactional(readOnly = true)
    public User getByEmailOrThrow(String email) {
        final String normEmail = email == null ? null : email.trim().toLowerCase(Locale.ROOT);
        return userRepository.findByEmail(normEmail)
                .orElseThrow(() -> new BizException(ResultCode.A_UNAUTHORIZED, "邮箱或密码错误"));
    }
}
