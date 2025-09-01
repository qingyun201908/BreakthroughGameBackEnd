// 文件：BreakthroughGame/backEnd/UserModule/UserRestful/service/CharacterService.java
// 中文备注：创建人物时，同步将初始战斗属性写入独立表 character_combat_attributes（弱关联 character_id）

package BreakthroughGame.backEnd.UserModule.UserRestful.service;

import BreakthroughGame.backEnd.UserModule.UserInfo.entity.CombatAttributes;
import BreakthroughGame.backEnd.UserModule.UserInfo.entity.GameCharacter;
import BreakthroughGame.backEnd.UserModule.UserInfo.entity.CharacterCombatAttributes; // ✅ 新增：独立表实体
import BreakthroughGame.backEnd.UserModule.UserInfo.repository.GameCharacterRepository;
import BreakthroughGame.backEnd.UserModule.UserInfo.repository.UserRepository;
import BreakthroughGame.backEnd.UserModule.UserInfo.repository.CharacterCombatAttributesRepository; // ✅ 新增：独立表仓储

import BreakthroughGame.backEnd.UserModule.UserRestful.common.api.ResultCode;
import BreakthroughGame.backEnd.UserModule.UserRestful.common.exception.BizException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.DataIntegrityViolationException;   // 中文备注：并发下唯一约束冲突
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;
import java.util.UUID;

@Service
public class CharacterService {

    private static final Logger log = LoggerFactory.getLogger(CharacterService.class);

    private final GameCharacterRepository characterRepo;
    private final UserRepository userRepo;
    private final CharacterCombatAttributesRepository ccattrsRepo; // ✅ 新增

    public CharacterService(GameCharacterRepository characterRepo,
                            UserRepository userRepo,
                            CharacterCombatAttributesRepository ccattrsRepo) {
        this.characterRepo = characterRepo;
        this.userRepo = userRepo;
        this.ccattrsRepo = ccattrsRepo;
    }

    /**
     * 中文备注：
     *  幂等创建：如果不存在则创建；若已存在直接返回
     *  并发场景：若同时插入引发唯一约束异常，捕获后回查返回
     *  额外逻辑：在创建或回查后，确保 character_combat_attributes 表中存在一行（若无则初始化插入）
     */
    @Transactional
    public GameCharacter createIfAbsent(UUID userId) {
        // 可选：校验用户存在性（避免脏 userId）
        var user = userRepo.findById(userId).orElseThrow(() -> new IllegalArgumentException("用户不存在"));

        Optional<GameCharacter> exists = characterRepo.findByUserId(userId);
        if (exists.isPresent()) {
            // ✅ 已有角色时，也要确保独立表存在记录（修复历史数据或兼容旧版本）
            ensureAttrsRow(exists.get());
            return exists.get();
        }

        // ===== 不存在则创建 =====
        GameCharacter ch = new GameCharacter();
        ch.setUserId(userId);
        ch.setName(user.getUsername());    // 中文备注：取用户名
        ch.setExp(0L);

        // 中文备注：初始化战斗属性（UI 默认值，可按需调整）
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
        ch.setAttributes(attrs); // 中文备注：仍保留内嵌值，供现有逻辑使用（向后兼容）

        try {
            // 先保存角色，拿到 characterId
            GameCharacter saved = characterRepo.save(ch);

            // ✅ 同步写入/确保 独立表 character_combat_attributes
            upsertInitialCcattrs(saved, attrs);

            return saved;
        } catch (DataIntegrityViolationException e) {
            // 中文备注：并发下可能撞 UNIQUE(user_id)，回查后返回并确保独立表存在
            log.warn("createIfAbsent 并发冲突，回查已有角色: userId={}", userId);
            GameCharacter found = characterRepo.findByUserId(userId)
                    .orElseThrow(() -> e); // 理论上一定能查到
            ensureAttrsRow(found);
            return found;
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

    // ===================== 内部工具方法 =====================

    /**
     * 中文备注：确保 character_combat_attributes 表存在一行（不存在则按 GameCharacter 内嵌属性初始化插入）
     */
    private void ensureAttrsRow(GameCharacter ch) {
        if (ch == null || ch.getId() == null) return;
        ccattrsRepo.findByCharacterId(ch.getId()).ifPresentOrElse(
                it -> { /* 已存在，忽略 */ },
                () -> upsertInitialCcattrs(ch, ch.getAttributes())
        );
    }

    /**
     * 中文备注：将 GameCharacter 的内嵌 CombatAttributes 初始化/写入到独立表
     *  - 若并发下撞唯一约束，则回查返回（保证最终存在）
     */
    private void upsertInitialCcattrs(GameCharacter ch, CombatAttributes src) {
        if (ch == null || ch.getId() == null) return;

        // 若 src 为空，使用“合理默认值”
        CombatAttributes a = (src != null) ? src : defaultCombatAttributes();

        CharacterCombatAttributes row = CharacterCombatAttributes.initFor(ch.getId());
        // 将内嵌属性映射到独立表字段（中文备注：保持两边数据一致）
        row.setAttack(a.getAttack());
        row.setAttackSpeedPercent(a.getAttackSpeedPercent());
        row.setCritRatePercent(a.getCritRatePercent());
        row.setCritDamagePercent(a.getCritDamagePercent());
        row.setHitPercent(a.getHitPercent());
        row.setPenetration(a.getPenetration());
        row.setMetal(a.getMetal());
        row.setWood(a.getWood());
        row.setWater(a.getWater());
        row.setFire(a.getFire());
        row.setEarth(a.getEarth());
        row.setChaos(a.getChaos());

        try {
            ccattrsRepo.save(row);
        } catch (DataIntegrityViolationException ex) {
            // 并发下可能已被其他事务写入，忽略并回查
            log.warn("写入 character_combat_attributes 并发冲突，回查已有记录: characterId={}", ch.getId());
            ccattrsRepo.findByCharacterId(ch.getId()).orElseThrow(() -> ex);
        }
    }

    /**
     * 中文备注：当 GameCharacter 没有内嵌属性时的默认值（与表默认值/实体 initFor 对齐）
     */
    private CombatAttributes defaultCombatAttributes() {
        CombatAttributes a = new CombatAttributes();
        a.setAttack(20);
        a.setAttackSpeedPercent(5);
        a.setCritRatePercent(3);
        a.setCritDamagePercent(3);
        a.setHitPercent(3);
        a.setPenetration(3);
        a.setMetal(3);
        a.setWood(3);
        a.setWater(3);
        a.setFire(3);
        a.setEarth(3);
        a.setChaos(3);
        return a;
    }
}
