// 文件：BreakthroughGame/backEnd/UserModule/UserInfo/repository/CharacterCombatAttributesRepository.java
// 中文备注：基础仓储 + 按角色查找

package BreakthroughGame.backEnd.UserModule.UserInfo.repository;

import BreakthroughGame.backEnd.UserModule.UserInfo.entity.CharacterCombatAttributes;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

public interface CharacterCombatAttributesRepository extends JpaRepository<CharacterCombatAttributes, UUID> {

    // 中文备注：按角色 ID 查询其战斗属性（1:1，因此返回 Optional）
    Optional<CharacterCombatAttributes> findByCharacterId(UUID characterId);

    // 如需并发更新校验，可配合 @Version 使用 save()，无需额外方法
}
