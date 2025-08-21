package BreakthroughGame.backEnd.WarcraftDungeonsInfo.repository;// 文件：BreakthroughGame/backEnd/DungeonModule/DailyChallenge/repository/DungeonDefinitionRepository.java

import BreakthroughGame.backEnd.WarcraftDungeonsInfo.entity.DungeonDefinition;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

/**
 * 中文备注：副本定义表仓库，支持通过 dungeonKey 查找用于 UPSERT
 */
public interface DungeonDefinitionRepository extends JpaRepository<DungeonDefinition, UUID> {

    Optional<DungeonDefinition> findByDungeonKey(String dungeonKey);

    boolean existsByDungeonKey(String dungeonKey);
}
