package BreakthroughGame.backEnd.DefinitionModule.repository;// 文件：BreakthroughGame/backEnd/UserModule/UserInfo/repository/CharacterEquipmentRepository.java

import BreakthroughGame.backEnd.DefinitionModule.entity.CharacterEquipment;
import BreakthroughGame.backEnd.DefinitionModule.entity.EquipmentSlot;
import org.springframework.data.jpa.repository.*;
import org.springframework.data.repository.query.Param;

import java.util.*;

public interface CharacterEquipmentRepository extends JpaRepository<CharacterEquipment, UUID> {

    Optional<CharacterEquipment> findByCharacterIdAndSlot(UUID characterId, EquipmentSlot slot);

    List<CharacterEquipment> findByCharacterId(UUID characterId);

    // 中文备注：便于“清空所有”场景
    @Modifying
    @Query("delete from CharacterEquipment e where e.characterId = :cid")
    int deleteByCharacterId(@Param("cid") UUID characterId);
}
