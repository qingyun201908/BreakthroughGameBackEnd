// 文件：BreakthroughGame/backEnd/UserModule/UserRestful/service/CharacterDungeonRunService.java
package BreakthroughGame.backEnd.UserModule.UserRestful.service;

import BreakthroughGame.backEnd.UserModule.UserRestful.dto.CharacterDailyMapper;   // 中文备注：你现有的 Mapper（dto 包下）
import BreakthroughGame.backEnd.UserModule.UserRestful.dto.CharacterDailyVO;
import BreakthroughGame.backEnd.UserModule.UserRestful.dto.DungeonPassResult;
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
import java.util.Optional;
import java.util.UUID;

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

    /** 中文备注：与实体注释保持一致，按 JST 计算业务日 */
    public static final ZoneId ZONE_JST = ZoneId.of("Asia/Tokyo");

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
