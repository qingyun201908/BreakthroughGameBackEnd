package BreakthroughGame.backEnd.UserModule.UserRestful.service;

import BreakthroughGame.backEnd.UserModule.UserInfo.entity.CombatAttributes;
import BreakthroughGame.backEnd.UserModule.UserInfo.entity.GameCharacter;
import BreakthroughGame.backEnd.UserModule.UserInfo.repository.GameCharacterRepository;
import BreakthroughGame.backEnd.UserModule.UserInfo.repository.UserRepository;

import BreakthroughGame.backEnd.UserModule.UserRestful.common.api.ResultCode;
import BreakthroughGame.backEnd.UserModule.UserRestful.common.exception.BizException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.DataIntegrityViolationException;   // 中文备注：并发下唯一约束冲突
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;
import java.util.UUID;

/**
 * 中文备注：人物服务（与用户弱关联：仅存 userId，不使用外键）
 * 单用户单角色：依赖 DB 约束 UNIQUE(user_id) 保证唯一
 */
@Service
public class CharacterService {

    private static final Logger log = LoggerFactory.getLogger(CharacterService.class);

    private final GameCharacterRepository characterRepo;
    private final UserRepository userRepo;

    public CharacterService(GameCharacterRepository characterRepo, UserRepository userRepo) {
        this.characterRepo = characterRepo;
        this.userRepo = userRepo;
    }

    /**
     * 幂等创建：如果不存在则创建；若已存在直接返回
     * 并发场景：若同时插入引发唯一约束异常，捕获后回查返回
     */
    @Transactional
    public GameCharacter createIfAbsent(UUID userId) {
        // 可选：校验用户存在性（避免脏 userId）
        userRepo.findById(userId).orElseThrow(() -> new IllegalArgumentException("用户不存在"));

        Optional<GameCharacter> exists = characterRepo.findByUserId(userId);
        if (exists.isPresent()) {
            return exists.get();
        }

        GameCharacter ch = new GameCharacter();
        ch.setUserId(userId);
        ch.setName(userRepo.findById(userId).get().getUsername());    // 中文备注：取用户名
        ch.setExp(0L);

        // 中文备注：初始化战斗属性（可按你的 UI 默认值设置）
        CombatAttributes attrs = new CombatAttributes();
        attrs.setAttack(100);
        attrs.setAttackSpeedPercent(100);
        attrs.setCritRatePercent(16);
        attrs.setCritDamagePercent(154);
        attrs.setHitPercent(95);
        attrs.setPenetration(12);
        attrs.setMetal(0);
        attrs.setWood(0);
        attrs.setWater(0);
        attrs.setFire(0);
        attrs.setEarth(0);
        attrs.setChaos(0);
        ch.setAttributes(attrs);

        try {
            return characterRepo.save(ch);
        } catch (DataIntegrityViolationException e) {
            // 中文备注：并发下可能撞 UNIQUE(user_id)，回查后返回
            log.warn("createIfAbsent 并发冲突，回查已有角色: userId={}", userId);
            return characterRepo.findByUserId(userId)
                    .orElseThrow(() -> e); // 理论上一定能查到
        }
    }

    @Transactional(readOnly = true)
    public Optional<GameCharacter> findOptionalByUserId(UUID userId) {
        return characterRepo.findByUserId(userId);
    }

    @Transactional(readOnly = true)
    public GameCharacter getByUserIdOrThrow(UUID userId) {
        return characterRepo.findByUserId(userId)
                .orElseThrow(() -> new BizException(ResultCode.A_UNAUTHORIZED, "未找到该用户的角色"));
    }
}
