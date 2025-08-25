// 文件：BreakthroughGame/backEnd/WarcraftDungeonsInfo/repository/DungeonDefinitionRepository.java
package BreakthroughGame.backEnd.WarcraftDungeonsInfo.repository;

import BreakthroughGame.backEnd.WarcraftDungeonsInfo.entity.DungeonDefinition;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor; // 中文备注：支持动态条件
import java.util.Optional;
import java.util.UUID;

/**
 * 中文备注：副本定义表仓库
 */
public interface DungeonDefinitionRepository
        extends JpaRepository<DungeonDefinition, UUID>, JpaSpecificationExecutor<DungeonDefinition> {

    Optional<DungeonDefinition> findByDungeonKey(String dungeonKey);

    boolean existsByDungeonKey(String dungeonKey);
}
