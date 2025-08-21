package BreakthroughGame.backEnd.UserModule.UserInfo.repository;

import BreakthroughGame.backEnd.UserModule.UserInfo.entity.GameCharacter;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

// 人物仓库：通过 userId 查人物列表，实现“用户 -> 人物”
public interface GameCharacterRepository extends JpaRepository<GameCharacter, UUID> {

    Optional<GameCharacter> findByUserId(UUID userId);   // 中文备注：单用户单角色 → Optional 单条

    boolean existsByUserId(UUID userId);

    // 批量：user_id IN (...)
//    List<GameCharacter> findAllByUserIdIn(Iterable<UUID> userIds);

    // 同用户下按名字查单个（可选）
    GameCharacter findByUserIdAndName(UUID userId, String name);
}
