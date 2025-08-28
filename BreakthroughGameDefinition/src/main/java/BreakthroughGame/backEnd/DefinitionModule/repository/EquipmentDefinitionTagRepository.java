// 文件：.../repository/EquipmentDefinitionTagRepository.java
package BreakthroughGame.backEnd.DefinitionModule.repository;

import BreakthroughGame.backEnd.DefinitionModule.entity.EquipmentDefinitionTag;
import BreakthroughGame.backEnd.DefinitionModule.entity.EquipmentDefinitionTag.PK;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface EquipmentDefinitionTagRepository extends JpaRepository<EquipmentDefinitionTag, PK> {
    List<EquipmentDefinitionTag> findByEquipKey(String equipKey);         // 中文备注：按装备键取标签
    void deleteByEquipKey(String equipKey);                               // 覆盖式更新时先删
}
