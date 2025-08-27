// 文件：BreakthroughGame/backEnd/WarcraftDungeonsInfo/repository/DungeonRunLogRepository.java
package BreakthroughGame.backEnd.WarcraftDungeonsInfo.repository;

import BreakthroughGame.backEnd.WarcraftDungeonsInfo.entity.DungeonRunLog;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDate;
import java.util.Optional;
import java.util.UUID;

/**
 * 中文备注：副本挑战流水仓储（用于幂等与审计）
 */
public interface DungeonRunLogRepository extends JpaRepository<DungeonRunLog, UUID> {

    // 中文备注：基于 traceId + 关键维度做幂等
    Optional<DungeonRunLog> findByCharacterIdAndDungeonKeyAndDayKeyAndTraceId(
            UUID characterId, String dungeonKey, LocalDate dayKey, String traceId
    );
}
