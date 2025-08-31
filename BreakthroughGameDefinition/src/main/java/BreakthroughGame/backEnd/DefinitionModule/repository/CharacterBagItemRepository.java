// 文件：BreakthroughGame/backEnd/DefinitionModule/repository/CharacterBagItemRepository.java
package BreakthroughGame.backEnd.DefinitionModule.repository;

import BreakthroughGame.backEnd.DefinitionModule.entity.CharacterBagItem;
import org.springframework.data.jpa.repository.*;
import org.springframework.data.repository.query.Param;

import java.util.Optional;
import java.util.UUID;

public interface CharacterBagItemRepository
        extends JpaRepository<CharacterBagItem, UUID>, JpaSpecificationExecutor<CharacterBagItem> {

    Optional<CharacterBagItem> findByCharacterIdAndItemKey(UUID characterId, String itemKey);

    long countByCharacterId(UUID characterId);

    // 中文备注：数量增量更新，且不允许变成负数；更新失败返回 0 行
    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query("update CharacterBagItem b " +
            "set b.qty = b.qty + :delta, b.updatedAt = CURRENT_TIMESTAMP " +
            "where b.id = :id and b.qty + :delta >= 0")
    int addQty(@Param("id") UUID id, @Param("delta") int delta);
}
