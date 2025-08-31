package BreakthroughGame.backEnd.DefinitionModule.repository;// 文件：BreakthroughGame/backEnd/UserModule/UserInfo/repository/CharacterEquipmentHistoryRepository.java

import BreakthroughGame.backEnd.DefinitionModule.entity.CharacterEquipmentHistory;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

public interface CharacterEquipmentHistoryRepository extends JpaRepository<CharacterEquipmentHistory, UUID> {}
