package BreakthroughGame.backEnd.DefinitionModule.repository;// 文件：BreakthroughGame/backEnd/UserModule/UserInfo/repository/EquipmentDefinitionRepository.java

import BreakthroughGame.backEnd.DefinitionModule.entity.EquipmentDefinition;
import BreakthroughGame.backEnd.DefinitionModule.entity.EquipmentRarity;
import BreakthroughGame.backEnd.DefinitionModule.entity.EquipmentSlot;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * 中文备注：图鉴定义仓储
 */
public interface EquipmentDefinitionRepository extends JpaRepository<EquipmentDefinition, UUID> {

    Optional<EquipmentDefinition> findByEquipKey(String equipKey);
    boolean existsByEquipKey(String equipKey);



    List<EquipmentDefinition> findByEnabledTrueOrderByRarityDescNameAsc();

    List<EquipmentDefinition> findByEnabledTrueAndSlotOrderByRarityDescNameAsc(EquipmentSlot slot);

    List<EquipmentDefinition> findByEnabledTrueAndRarityOrderByNameAsc(EquipmentRarity rarity);
}
