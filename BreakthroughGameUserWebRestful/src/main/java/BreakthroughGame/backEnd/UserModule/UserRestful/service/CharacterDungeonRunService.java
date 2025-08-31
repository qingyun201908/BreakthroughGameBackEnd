// 文件：BreakthroughGame/backEnd/UserModule/UserRestful/service/CharacterDungeonRunService.java
package BreakthroughGame.backEnd.UserModule.UserRestful.service;

import BreakthroughGame.backEnd.DefinitionModule.entity.EquipmentDefinition;
import BreakthroughGame.backEnd.DefinitionModule.entity.EquipmentDefinitionDungeon;
import BreakthroughGame.backEnd.DefinitionModule.entity.ItemType;
import BreakthroughGame.backEnd.DefinitionModule.repository.EquipmentDefinitionDungeonRepository;
import BreakthroughGame.backEnd.DefinitionModule.repository.EquipmentDefinitionRepository;
import BreakthroughGame.backEnd.UserModule.UserRestful.dto.*;
import BreakthroughGame.backEnd.UserModule.UserRestful.request.DungeonPassRequest;
import BreakthroughGame.backEnd.WarcraftDungeonsInfo.entity.CharacterDungeonDaily;
import BreakthroughGame.backEnd.WarcraftDungeonsInfo.entity.DungeonDefinition;
import BreakthroughGame.backEnd.WarcraftDungeonsInfo.entity.DungeonRunLog;
import BreakthroughGame.backEnd.WarcraftDungeonsInfo.repository.CharacterDungeonDailyRepository;
import BreakthroughGame.backEnd.WarcraftDungeonsInfo.repository.DungeonDefinitionRepository;
import BreakthroughGame.backEnd.WarcraftDungeonsInfo.repository.DungeonRunLogRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.dao.OptimisticLockingFailureException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.time.ZoneId;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.ThreadLocalRandom;
import java.util.stream.Collectors;

/**
 * 中文备注：通关上报服务
 * - 幂等：traceId + (characterId, dungeonKey, dayKey)
 * - 计数：若当日仍可挑战 or allowOverride=true，则 +1，否则仅记录流水但不计入
 */
@Service
@RequiredArgsConstructor
public class CharacterDungeonRunService {

    private final CharacterDungeonDailyRepository dailyRepo;
    private final DungeonRunLogRepository runLogRepo;
    private final DungeonDefinitionRepository defRepo;


    // ================== 新增依赖 ==================
    private final EquipmentDefinitionRepository equipDefRepo;                // 中文备注：装备图鉴主表
    private final EquipmentDefinitionDungeonRepository equipDungeonRepo;     // 中文备注：图鉴-副本映射（equip_key <-> dungeon_key）
    private final CharacterBagService bagService;                            // 中文备注：人物背包服务
    // ==============================================

    private final CharacterDungeonDailyQueryService dailyQueryService; // 中文备注：新增依赖，用于拿“今日快照视图 VO”

    /** 中文备注：与实体注释保持一致，按 JST 计算业务日 */
    public static final ZoneId ZONE_JST = ZoneId.of("Asia/Tokyo");



    /**
     * 中文备注：通关 + 随机掉落 1 件 + 入背包；返回“通关+掉落”的组合视图
     */
    @Transactional
    public DungeonPassView passAndLoot(DungeonPassRequest req) {
        // 1) 先走你已有的通关入库与计数逻辑
        DungeonPassResult pass = recordPass(req);

        // 2) 根据副本配置随机 1 件装备（无配置则为 null，不抛错）
        EquipmentDefinition drop = randomOneByDungeon(req.getDungeonKey());

        LootItemVO lootVO = null;
        if (drop != null) {
            // 3) 入背包（相同 item_key 叠加）
            int rarityInt = (drop.getRarity() == null) ? 1 : (drop.getRarity().ordinal() + 1);  // 中文备注：枚举转 1~N
            String desc = (drop.getDescription() == null) ? ("来源副本：" + req.getDungeonKey()) : drop.getDescription();

            bagService.addItem(
                    req.getCharacterId(),         // 中文备注：角色 UUID
                    drop.getEquipKey(),           // 中文备注：物品 key（与前端一致）
                    drop.getName(),               // 中文备注：名称冗余
                    ItemType.equipment,           // 中文备注：类型=装备
                    rarityInt,                    // 中文备注：稀有度 int
                    desc,                         // 中文备注：描述冗余
                    1                             // 中文备注：数量 +1
            );

            // 4) 组装返回给前端的掉落 VO（含 icon/description）
            lootVO = LootItemVO.builder()
                    .equipKey(drop.getEquipKey())
                    .name(drop.getName())
                    .rarity(rarityInt)
                    .rarityName(drop.getRarity() == null ? "COMMON" : drop.getRarity().name())
                    .icon(drop.getIcon())                // 中文备注：字段来自 EquipmentDefinition.icon
                    .description(drop.getDescription())  // 中文备注：字段来自 EquipmentDefinition.description
                    .build();
        }
        ensureTodayDaily(pass, req);              // 中文备注：强制把今日快照塞进 pass.daily

        return DungeonPassView.builder().pass(pass).loot(lootVO).build();
    }

    /** 中文备注：补齐今日快照到 pass.daily（不覆盖已有值） */
    private void ensureTodayDaily(DungeonPassResult pass, DungeonPassRequest req) {
        if (pass == null) return;
        if (pass.getDaily() != null) return;  // 中文备注：recordPass 已经给了就不覆盖

        // 中文备注：拿“今日快照视图 VO”（字段：runsUsed / runsMax / selected / canChallenge / ...）
        var todayView = dailyQueryService.getOne(req.getCharacterId(), req.getDungeonKey(),LocalDate.now(ZONE_JST));

        // 中文备注：塞回 pass（类型按你的 DTO 来）
        pass.setDaily(todayView);
    }

    /** 中文备注：从当前副本掉落池中随机抽 1 件；无配置返回 null */
    private EquipmentDefinition randomOneByDungeon(String dungeonKey) {
        List<EquipmentDefinitionDungeon> mappings = equipDungeonRepo.findAllByDungeonKey(dungeonKey);
        if (mappings == null || mappings.isEmpty()) return null;
        List<String> keys = mappings.stream().map(EquipmentDefinitionDungeon::getEquipKey).collect(Collectors.toList());
        String equipKey = keys.get(ThreadLocalRandom.current().nextInt(keys.size()));
        return equipDefRepo.findByEquipKey(equipKey).orElse(null);
    }



    @Transactional
    public DungeonPassResult recordPass(DungeonPassRequest req) {
        // —— 1) 归一化 dayKey（JST） —— //
        LocalDate dayKey = (req.getDayKey() != null) ? req.getDayKey() : LocalDate.now(ZONE_JST);
        String traceId = (req.getTraceId() == null || req.getTraceId().isBlank())
                ? ("auto_" + UUID.randomUUID())
                : req.getTraceId();

        // —— 2) 幂等检查 —— //
        Optional<DungeonRunLog> dup = runLogRepo.findByCharacterIdAndDungeonKeyAndDayKeyAndTraceId(
                req.getCharacterId(), req.getDungeonKey(), dayKey, traceId
        );
        if (dup.isPresent()) {
            // 中文备注：重复上报，直接返回已有结果及当日快照
            CharacterDungeonDaily existedDaily = dailyRepo
                    .findByCharacterIdAndDungeonKeyAndDayKey(req.getCharacterId(), req.getDungeonKey(), dayKey)
                    .orElse(null);

            DungeonPassResult r = new DungeonPassResult();
            r.setRunLogId(dup.get().getId());
            r.setTraceId(traceId);
            r.setResultCode("DUPLICATE");
            r.setCounted(dup.get().isResultSuccess()); // 中文备注：如果上次已成功则视为已计数
            r.setDaily(existedDaily == null ? null : CharacterDailyMapper.toVO(existedDaily));
            return r;
        }

        // —— 3) 读定义，准备每日快照 —— //
        DungeonDefinition def = defRepo.findByDungeonKey(req.getDungeonKey())
                .orElse(null);
        if (def == null || !def.isActive()) {
            // 中文备注：无效副本，记录失败流水并返回
            DungeonRunLog log = new DungeonRunLog();
            log.setCharacterId(req.getCharacterId());
            log.setDungeonKey(req.getDungeonKey());
            log.setDifficulty(req.getDifficulty());
            log.setDayKey(dayKey);
            log.setBeforeRunsUsed(0);
            log.setAfterRunsUsed(0);
            log.setResultSuccess(false);
            log.setResultCode("INVALID_DUNGEON");
            log.setTraceId(traceId);
            log.setCreatedAt(OffsetDateTime.now());
            runLogRepo.save(log);

            DungeonPassResult r = new DungeonPassResult();
            r.setRunLogId(log.getId());
            r.setTraceId(traceId);
            r.setResultCode("INVALID_DUNGEON");
            r.setCounted(false);
            r.setDaily(null);
            return r;
        }

        // —— 4) 取/建当日计数 —— //
        CharacterDungeonDaily daily = dailyRepo
                .findByCharacterIdAndDungeonKeyAndDayKey(req.getCharacterId(), req.getDungeonKey(), dayKey)
                .orElseGet(() -> {
                    CharacterDungeonDaily d = new CharacterDungeonDaily();
                    d.setCharacterId(req.getCharacterId());
                    d.setDungeonKey(req.getDungeonKey());
                    d.setDayKey(dayKey);
                    d.setRunsUsed(0);
                    d.setRunsMaxSnapshot(def.getDailyRunsMax()); // 中文备注：用定义表拍快照
                    d.setLastSelectedDifficulty(Math.max(req.getDifficulty(), 1));
                    d.setAllowOverride(false);
                    d.setUpdatedAt(OffsetDateTime.now());
                    return d;
                });

        // —— 5) 计数与日志（带乐观锁重试） —— //
        final int MAX_RETRY = 3;
        for (int i = 0; i < MAX_RETRY; i++) {
            try {
                int before = daily.getRunsUsed();
                boolean canCount = daily.isCanChallenge(); // 中文备注：考虑 allowOverride

                if (canCount) {
                    daily.setRunsUsed(before + 1);                          // 消耗一次
                    daily.setLastSelectedDifficulty(Math.max(req.getDifficulty(), 1));
                    daily.setUpdatedAt(OffsetDateTime.now());
                    daily = dailyRepo.saveAndFlush(daily);                  // 可能抛乐观锁异常
                }

                // —— 6) 记录流水 —— //
                DungeonRunLog log = new DungeonRunLog();
                log.setCharacterId(req.getCharacterId());
                log.setDungeonKey(req.getDungeonKey());
                log.setDifficulty(req.getDifficulty());
                log.setDayKey(dayKey);
                log.setBeforeRunsUsed(before);
                log.setAfterRunsUsed(canCount ? before + 1 : before);
                log.setResultSuccess(canCount);
                log.setResultCode(canCount ? "OK" : "NO_TIMES");
                log.setTraceId(traceId);
                log.setCreatedAt(OffsetDateTime.now());
                runLogRepo.save(log);

                // —— 7) 返回结果 —— //
                CharacterDailyVO vo = CharacterDailyMapper.toVO(daily);
                DungeonPassResult r = new DungeonPassResult();
                r.setRunLogId(log.getId());
                r.setTraceId(traceId);
                r.setResultCode(canCount ? "OK" : "NO_TIMES");
                r.setCounted(canCount);
                r.setDaily(vo);
                return r;

            } catch (OptimisticLockingFailureException e) {
                // 中文备注：并发冲突，短重试
                if (i == MAX_RETRY - 1) throw e;
                daily = dailyRepo.findByCharacterIdAndDungeonKeyAndDayKey(
                        req.getCharacterId(), req.getDungeonKey(), dayKey
                ).orElseThrow(); // 再读一次后重试
            }
        }

        // 理论不可达
        throw new IllegalStateException("Unexpected state in recordPass");
    }
}
