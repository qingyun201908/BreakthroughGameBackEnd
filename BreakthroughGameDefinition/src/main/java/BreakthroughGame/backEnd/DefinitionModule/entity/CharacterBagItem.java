package BreakthroughGame.backEnd.DefinitionModule.entity;// 文件：BreakthroughGame/backEnd/UserModule/UserInfo/entity/CharacterBagItem.java

import BreakthroughGame.backEnd.DefinitionModule.entity.ItemType;
import jakarta.persistence.*;
import lombok.Data;
import java.time.OffsetDateTime;
import java.util.UUID;

/**
 * 中文备注：人物背包条目（每种 itemKey 一条栈，qty 叠加）
 * - 为提升检索效率冗余了 name/type/rarity/desc（来源可为主数据或掉落时写入）
 */
@Entity
@Table(
        name = "character_bag_item",
        uniqueConstraints = @UniqueConstraint(name = "uk_bagitem_char_itemkey", columnNames = {"character_id", "item_key"}),
        indexes = {
                @Index(name = "idx_bagitem_character_id", columnList = "character_id"),
                @Index(name = "idx_bagitem_type", columnList = "type"),
                @Index(name = "idx_bagitem_rarity", columnList = "rarity"),
                @Index(name = "idx_bagitem_updated_at", columnList = "updated_at"),
                @Index(name = "idx_bagitem_name", columnList = "name") // 中文备注：简单前缀搜索，必要时可换 trigram
        }
)
@Data
public class CharacterBagItem {

    @Id @GeneratedValue
    private UUID id;

    @Column(name = "character_id", nullable = false)
    private UUID characterId;         // 中文备注：仅存 UUID；不建外键

    @Column(name = "item_key", nullable = false, length = 64)
    private String itemKey;           // 中文备注：物品唯一键（掉落/策划定义），用于叠加

    @Column(name = "name", nullable = false, length = 64)
    private String name;              // 中文备注：展示用名称（冗余）

    @Enumerated(EnumType.STRING)
    @Column(name = "type", nullable = false, length = 16)
    private ItemType type;            // 中文备注：类型（与前端一致）

    @Column(name = "rarity", nullable = false)
    private int rarity = 1;           // 中文备注：稀有度 1~5

    @Column(name = "qty", nullable = false)
    private int qty = 0;              // 中文备注：栈内数量（>=0）

    @Column(name = "description", columnDefinition = "text")  // 中文备注：不再需要引号
    private String desc;              // 中文备注：描述（冗余）

    @Version
    private long version;             // 中文备注：并发安全

    @Column(name = "updated_at", nullable = false)
    private OffsetDateTime updatedAt = OffsetDateTime.now();
}
