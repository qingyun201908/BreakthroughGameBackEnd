// 文件：BreakthroughGame/backEnd/UserModule/UserRestful/service/CharacterBagService.java
package BreakthroughGame.backEnd.UserModule.UserRestful.service;


import BreakthroughGame.backEnd.DefinitionModule.entity.CharacterBagItem;
import BreakthroughGame.backEnd.DefinitionModule.entity.CharacterBagMeta;
import BreakthroughGame.backEnd.DefinitionModule.entity.ItemType;
import BreakthroughGame.backEnd.DefinitionModule.repository.CharacterBagItemRepository;
import BreakthroughGame.backEnd.DefinitionModule.repository.CharacterBagMetaRepository;
import BreakthroughGame.backEnd.UserModule.UserRestful.common.exception.BizException;
import BreakthroughGame.backEnd.UserModule.UserRestful.dto.BagItemView;
import BreakthroughGame.backEnd.UserModule.UserRestful.dto.BagQuery;
import BreakthroughGame.backEnd.UserModule.UserRestful.dto.BagSnapshot;
import jakarta.persistence.criteria.Predicate;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;
import java.util.stream.Collectors;

import static BreakthroughGame.backEnd.UserModule.UserRestful.common.api.ResultCode.B_BIZ_ERROR;

/** 中文备注：背包服务：查询/增减（预留） */
@Service
@RequiredArgsConstructor
public class CharacterBagService {

    private final CharacterBagItemRepository bagItemRepo;
    private final CharacterBagMetaRepository bagMetaRepo;

    /** 中文备注：获取背包快照（支持搜索/筛选/排序） */
    @Transactional(readOnly = true)
    public BagSnapshot getSnapshot(UUID characterId, BagQuery q) {
        // 1) 容量
        int capacity = bagMetaRepo.findByCharacterId(characterId)
                .map(CharacterBagMeta::getCapacity)
                .orElse(24); // 中文备注：默认 24

        // 2) 过滤条件
        Specification<CharacterBagItem> spec = (root, query, cb) -> {
            List<Predicate> ps = new ArrayList<>();
            ps.add(cb.equal(root.get("characterId"), characterId));

            // 搜索：name 包含（忽略大小写）
            if (q.getQ() != null && !q.getQ().isBlank()) {
                String pattern = "%" + q.getQ().trim().toLowerCase() + "%";
                ps.add(cb.like(cb.lower(root.get("name")), pattern));
            }
            // 类型筛选
            ItemType type = ItemType.fromNullable(q.getType());
            if (type != null) {
                ps.add(cb.equal(root.get("type"), type));
            }
            return cb.and(ps.toArray(new Predicate[0]));
        };

        // 3) 排序
        Sort sort = switch (q.getSort() == null ? "default" : q.getSort()) {
            case "rarity" -> Sort.by(Sort.Order.desc("rarity"), Sort.Order.asc("name"));
            case "name"   -> Sort.by(Sort.Order.asc("name"));
            case "qty"    -> Sort.by(Sort.Order.desc("qty"), Sort.Order.asc("name"));
            case "default" -> Sort.by(Sort.Order.desc("updatedAt")); // 中文备注：默认按更新时间
            default -> throw new BizException(B_BIZ_ERROR,"非法排序字段：" + q.getSort());
        };

        List<CharacterBagItem> list = bagItemRepo.findAll(spec, sort);

        // 4) 映射为前端视图
        List<BagItemView> views = list.stream().map(it ->
                new BagItemView(
                        it.getId().toString(),
                        it.getName(),
                        it.getQty(),
                        it.getType().name(),
                        it.getRarity(),
                        it.getDesc()
                )
        ).collect(Collectors.toList());

        return new BagSnapshot(capacity, views);
    }

    /** 中文备注：向背包增加指定 itemKey 的数量（叠加栈）；此处仅示例，接口待你接入掉落/使用 */
    @Transactional
    public void addItem(UUID characterId, String itemKey, String name, ItemType type, int rarity, String desc, int deltaQty) {
        if (deltaQty <= 0) throw new BizException(B_BIZ_ERROR,"增加的数量必须>0");
        CharacterBagItem item = bagItemRepo.findByCharacterIdAndItemKey(characterId, itemKey)
                .orElseGet(() -> {
                    CharacterBagItem n = new CharacterBagItem();
                    n.setCharacterId(characterId);
                    n.setItemKey(itemKey);
                    n.setName(name);
                    n.setType(type);
                    n.setRarity(rarity);
                    n.setDesc(desc);
                    n.setQty(0);
                    return n;
                });
        item.setQty(Math.addExact(item.getQty(), deltaQty)); // 中文备注：溢出保护
        item.setUpdatedAt(java.time.OffsetDateTime.now());
        bagItemRepo.save(item);
    }

    /** 中文备注：从背包扣减指定 itemKey 的数量（可为使用或丢弃） */
    @Transactional
    public void removeItem(UUID characterId, String itemKey, int deltaQty) {
        if (deltaQty <= 0) throw new BizException(B_BIZ_ERROR,"扣减的数量必须>0");
        CharacterBagItem item = bagItemRepo.findByCharacterIdAndItemKey(characterId, itemKey)
                .orElseThrow(() -> new BizException(B_BIZ_ERROR,"物品不存在：" + itemKey));
        if (item.getQty() < deltaQty) throw new BizException(B_BIZ_ERROR,"数量不足");
        item.setQty(item.getQty() - deltaQty);
        item.setUpdatedAt(java.time.OffsetDateTime.now());
        if (item.getQty() == 0) {
            bagItemRepo.delete(item); // 中文备注：0 则删栈，避免脏数据
        } else {
            bagItemRepo.save(item);
        }
    }
}
