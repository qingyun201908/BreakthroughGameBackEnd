// 文件：BreakthroughGame/backEnd/UserModule/UserInfo/repository/CharacterXpRepository.java
package BreakthroughGame.backEnd.UserModule.UserInfo.repository;

import BreakthroughGame.backEnd.UserModule.UserInfo.entity.CharacterXp;
import org.springframework.data.jpa.repository.*;
import org.springframework.data.repository.query.Param;

import java.util.Optional;
import java.util.UUID;

public interface CharacterXpRepository extends JpaRepository<CharacterXp, UUID> {

    Optional<CharacterXp> findByCharacterId(UUID characterId);

    // 1) 确保记录存在：不存在插入0经验，存在则不变（无返回结果集）
    @Modifying
    @Query(value = """
        INSERT INTO character_xp (id, character_id, total_xp, updated_at)
        VALUES (gen_random_uuid(), :cid, 0, now())
        ON CONFLICT (character_id) DO NOTHING
        """, nativeQuery = true)
    int ensureExists(@Param("cid") UUID characterId); // 返回影响行数或0

    // 2) 增量更新（无返回结果集；触发器会刷新冗余列）
    @Modifying
    @Query(value = """
        UPDATE character_xp
           SET total_xp  = total_xp + :delta,
               updated_at = now()
         WHERE character_id = :cid
        """, nativeQuery = true)
    int addDelta(@Param("cid") UUID characterId, @Param("delta") long delta);
}
