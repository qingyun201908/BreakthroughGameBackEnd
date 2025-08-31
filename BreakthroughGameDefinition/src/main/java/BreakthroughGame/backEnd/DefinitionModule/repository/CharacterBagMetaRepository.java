package BreakthroughGame.backEnd.DefinitionModule.repository;// 文件：BreakthroughGame/backEnd/UserModule/UserInfo/repository/CharacterBagMetaRepository.java

import BreakthroughGame.backEnd.DefinitionModule.entity.CharacterBagMeta;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

public interface CharacterBagMetaRepository extends JpaRepository<CharacterBagMeta, UUID> {
    Optional<CharacterBagMeta> findByCharacterId(UUID characterId);
}
