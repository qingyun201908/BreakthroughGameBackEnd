// 文件：.../repository/EquipmentDefinitionDungeonRepository.java
package BreakthroughGame.backEnd.DefinitionModule.repository;

import BreakthroughGame.backEnd.DefinitionModule.entity.EquipmentDefinitionDungeon;
import BreakthroughGame.backEnd.DefinitionModule.entity.EquipmentDefinitionDungeon.PK;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface EquipmentDefinitionDungeonRepository extends JpaRepository<EquipmentDefinitionDungeon, PK> {
    List<EquipmentDefinitionDungeon> findByEquipKey(String equipKey);     // 中文备注：按装备键取来源副本
    void deleteByEquipKey(String equipKey);                               // 覆盖式更新时先删
}
