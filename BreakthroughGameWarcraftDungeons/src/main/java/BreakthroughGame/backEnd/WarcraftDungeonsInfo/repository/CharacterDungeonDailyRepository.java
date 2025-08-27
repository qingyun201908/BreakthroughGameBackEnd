// 文件：BreakthroughGame/backEnd/WarcraftDungeonsInfo/repository/CharacterDungeonDailyRepository.java
package BreakthroughGame.backEnd.WarcraftDungeonsInfo.repository;

import BreakthroughGame.backEnd.WarcraftDungeonsInfo.entity.CharacterDungeonDaily;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.*;
import org.springframework.data.repository.query.Param;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * 中文备注：角色-副本当日计数 查询仓储
 * - 提供按角色+日期的列表查询
 * - 提供按角色+副本+日期的单条查询
 * - 提供可选过滤的分页查询（使用 JPQL 动态 where，可空参数不参与过滤）
 */
public interface CharacterDungeonDailyRepository extends JpaRepository<CharacterDungeonDaily, UUID> {

    // 中文备注：查询“某角色在某天”的所有副本计数，按更新时间倒序
    List<CharacterDungeonDaily> findByCharacterIdAndDayKeyOrderByUpdatedAtDesc(UUID characterId, LocalDate dayKey);

    // 中文备注：查询“某角色在某天某副本”的单条
    Optional<CharacterDungeonDaily> findByCharacterIdAndDungeonKeyAndDayKey(UUID characterId, String dungeonKey, LocalDate dayKey);

    // 中文备注：分页查询（可选条件：角色ID、日期起止、dungeonKey）
    @Query("""
            SELECT d FROM CharacterDungeonDaily d
            WHERE (:characterId IS NULL OR d.characterId = :characterId)
              AND (:dungeonKey  IS NULL OR d.dungeonKey  = :dungeonKey)
              AND (:dayFrom     IS NULL OR d.dayKey >= :dayFrom)
              AND (:dayTo       IS NULL OR d.dayKey <= :dayTo)
            """)
    Page<CharacterDungeonDaily> pageQuery(
            @Param("characterId") UUID characterId,
            @Param("dungeonKey")  String dungeonKey,
            @Param("dayFrom")     LocalDate dayFrom,
            @Param("dayTo")       LocalDate dayTo,
            Pageable pageable
    );
}
