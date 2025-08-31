package BreakthroughGame.backEnd.DefinitionModule.repository;// 文件：BreakthroughGame/backEnd/UserModule/UserInfo/repository/CharacterBagItemRepository.java

import BreakthroughGame.backEnd.DefinitionModule.entity.CharacterBagItem;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

import java.util.Optional;
import java.util.UUID;

public interface CharacterBagItemRepository extends JpaRepository<CharacterBagItem, UUID>, JpaSpecificationExecutor<CharacterBagItem> {
    Optional<CharacterBagItem> findByCharacterIdAndItemKey(UUID characterId, String itemKey);
    long countByCharacterId(UUID characterId);
}
