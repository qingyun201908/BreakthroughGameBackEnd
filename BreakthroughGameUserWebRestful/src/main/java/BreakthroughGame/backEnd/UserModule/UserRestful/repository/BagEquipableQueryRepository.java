// 文件：BreakthroughGame/backEnd/UserModule/UserRestful/repository/BagEquipableQueryRepository.java
package BreakthroughGame.backEnd.UserModule.UserRestful.repository;

import BreakthroughGame.backEnd.UserModule.UserRestful.dto.EquipableItemDto;
import java.util.*;
import org.hibernate.query.NativeQuery;
import org.hibernate.type.StandardBasicTypes;
import org.springframework.data.domain.*;
import org.springframework.data.jpa.repository.*;
import org.springframework.stereotype.Repository;

@Repository
public interface BagEquipableQueryRepository extends JpaRepository<BagEquipableQueryRepositoryImpl.DummyEntity, UUID>, BagEquipableQueryRepositoryCustom {
    // 占位实体（因为只跑自定义查询），也可以挂在任意现有实体仓库上。
}

// ===== 自定义接口 =====
interface BagEquipableQueryRepositoryCustom {
    Page<EquipableItemDto> findEquipables(UUID characterId, String slot, String q, Pageable pageable);
}

// ===== 实现：使用原生 SQL 进行 JOIN + 过滤 + 排序 =====
class BagEquipableQueryRepositoryImpl implements BagEquipableQueryRepositoryCustom {

    private final jakarta.persistence.EntityManager em;

    public BagEquipableQueryRepositoryImpl(jakarta.persistence.EntityManager em) { this.em = em; }

    @Override
    public Page<EquipableItemDto> findEquipables(UUID characterId, String slot, String q, Pageable pageable) {
        // 核心 SQL——背包表 join 图鉴表，过滤仅装备、qty>0、图鉴可见，支持 slot/q
        final String base = """
            FROM character_bag_item bi
            JOIN equipment_definition ed ON ed.equip_key = bi.item_key
            WHERE bi.character_id = CAST(:cid AS uuid)
              AND bi.type = 'equipment'      -- 仅装备条目
              AND bi.qty > 0                 -- 数量需大于 0
              AND ed.enabled = true          -- 图鉴可见
              AND ( CAST(:slot AS text) IS NULL OR ed.slot = CAST(:slot AS text) )   -- 槽位过滤（显式 text）
              AND ( CAST(:q    AS text) IS NULL OR bi.name ILIKE ('%' || CAST(:q AS text) || '%') )  -- 名称模糊（显式 text）
            """;

        final String select = """
            SELECT bi.id,
                   bi.item_key,
                   COALESCE(bi.name, ed.name) AS name,              -- 优先背包冗余名
                   bi.rarity AS star,                                -- star 直接取背包 rarity
                   COALESCE(ed.color_hex, '#9ca3af') AS color,       -- 颜色优先图鉴色
                   ed.slot AS slot_key,                              -- 槽位来自图鉴
                   ed.icon,
                   COALESCE(bi.description, ed.description) AS description, -- 描述优先背包
                   bi.qty
            """;

        final String orderAndPage = """
            ORDER BY ed.sort_order ASC, bi.updated_at DESC
            LIMIT :_limit OFFSET :_offset
            """;

        // -------- count（强类型参数绑定）--------
        var countQuery = em.createNativeQuery("SELECT COUNT(*) " + base);
        NativeQuery<?> cq = countQuery.unwrap(NativeQuery.class);
        cq.setParameter("cid",  characterId, StandardBasicTypes.UUID);
        cq.setParameter("slot", (slot == null || slot.isBlank()) ? null : slot, StandardBasicTypes.STRING);
        cq.setParameter("q",    (q == null || q.isBlank()) ? null : q,     StandardBasicTypes.STRING);

        long total = ((Number) cq.getSingleResult()).longValue();
        if (total == 0) return new PageImpl<>(List.of(), pageable, 0);

        // -------- list（把分页写进 SQL；不再用 setFirstResult/setMaxResults）--------
        var listQuery = em.createNativeQuery(select + base + orderAndPage);
        NativeQuery<?> lq = listQuery.unwrap(NativeQuery.class);
        lq.setParameter("cid",  characterId, StandardBasicTypes.UUID);
        lq.setParameter("slot", (slot == null || slot.isBlank()) ? null : slot, StandardBasicTypes.STRING);
        lq.setParameter("q",    (q == null || q.isBlank()) ? null : q,     StandardBasicTypes.STRING);
        lq.setParameter("_limit",  pageable.getPageSize(),                 StandardBasicTypes.INTEGER);
        lq.setParameter("_offset", (int) pageable.getOffset(),             StandardBasicTypes.INTEGER);

        @SuppressWarnings("unchecked")
        List<Object[]> rows = (List<Object[]>) lq.getResultList();

        List<EquipableItemDto> data = new ArrayList<>(rows.size());
        for (Object[] r : rows) {
            data.add(EquipableItemDto.builder()
                    .id((UUID) r[0])
                    .itemKey((String) r[1])
                    .name((String) r[2])
                    .star(((Number) r[3]).intValue())
                    .color((String) r[4])
                    .slotKey((String) r[5])
                    .icon((String) r[6])
                    .description((String) r[7])
                    .qty(((Number) r[8]).intValue())
                    .build());
        }
        return new PageImpl<>(data, pageable, total);
    }

    // 因为 JpaRepository 需要一个实体类型，提供一个空实体占位（不会被实际使用）。
    @jakarta.persistence.Entity(name = "z_dummy_entity_bag_query")
    @jakarta.persistence.Table(name = "character_bag_item")
    static class DummyEntity {
        @jakarta.persistence.Id
        private UUID id;
    }
}
