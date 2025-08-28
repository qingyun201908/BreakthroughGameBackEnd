package BreakthroughGame.backEnd.UserModule.UserRestful.service;// 文件：BreakthroughGame/backEnd/DefinitionModule/service/EquipmentCatalogService.java


import BreakthroughGame.backEnd.DefinitionModule.entity.EquipmentDefinition;
import BreakthroughGame.backEnd.DefinitionModule.entity.EquipmentRarity;
import BreakthroughGame.backEnd.DefinitionModule.entity.EquipmentSlot;
import BreakthroughGame.backEnd.DefinitionModule.repository.EquipmentDefinitionDungeonRepository;
import BreakthroughGame.backEnd.DefinitionModule.repository.EquipmentDefinitionRepository;
import BreakthroughGame.backEnd.DefinitionModule.repository.EquipmentDefinitionTagRepository;

import BreakthroughGame.backEnd.UserModule.UserRestful.common.api.ResultCode;          // 中文备注：你项目已有
import BreakthroughGame.backEnd.UserModule.UserRestful.common.exception.BizException; // 中文备注：按需抛出
import BreakthroughGame.backEnd.UserModule.UserRestful.dto.EquipmentItemVO;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.util.*;
import java.util.function.Predicate;
import java.util.stream.Collectors;

/** 中文备注：装备图鉴查询聚合服务 */
@Service
@RequiredArgsConstructor
public class EquipmentCatalogService {

    private final EquipmentDefinitionRepository defRepo;
    private final EquipmentDefinitionTagRepository tagRepo;
    private final EquipmentDefinitionDungeonRepository dungeonRepo;

    /** 中文备注：统一列表查询（含内存过滤与分页） */
    public List<EquipmentItemVO> search(String slotKey,
                                        String rarityKey,
                                        String tag,
                                        String dungeonKey,
                                        String keyword,
                                        int page, int size,
                                        boolean includeTags, boolean includeDungeons) {

        // 1) 参数规范化与校验 —— 校验失败抛 BizException
        EquipmentSlot slotEnum = null;
        if (StringUtils.hasText(slotKey)) {
            slotEnum = parseSlot(slotKey);
            if (slotEnum == null) {
                throw new BizException(ResultCode.VALIDATE_FAILED, "非法槽位参数：" + slotKey); // 中文备注：示例枚举值
            }
        }

        EquipmentRarity rarityEnum = null;
        if (StringUtils.hasText(rarityKey)) {
            rarityEnum = parseRarity(rarityKey);
            if (rarityEnum == null) {
                throw new BizException(ResultCode.VALIDATE_FAILED, "非法稀有度参数：" + rarityKey);
            }
        }

        if (page <= 0 || size <= 0 || size > 200) {
            throw new BizException(ResultCode.VALIDATE_FAILED, "分页参数不合法（page 从 1 起，1 ≤ size ≤ 200）");
        }

        // 2) 基础数据获取 —— 尽量用仓储已有排序，减少内存排序开销
        List<EquipmentDefinition> baseList;
        if (slotEnum != null && rarityEnum == null) {
            baseList = defRepo.findByEnabledTrueAndSlotOrderByRarityDescNameAsc(slotEnum);
        } else if (slotEnum == null && rarityEnum != null) {
            baseList = defRepo.findByEnabledTrueAndRarityOrderByNameAsc(rarityEnum);
        } else {
            baseList = defRepo.findByEnabledTrueOrderByRarityDescNameAsc();
        }

        // 3) 追加过滤：tag / dungeon / keyword
        Predicate<EquipmentDefinition> filter = e -> true;

        if (StringUtils.hasText(tag)) {
            // 中文备注：弱关联过滤 —— 先取 equipKey 的标签集合，再判断包含
            Set<String> equipKeysWithTag = tagRepo.findAll().stream()
                    .filter(t -> tag.equalsIgnoreCase(t.getTag()))
                    .map(t -> t.getEquipKey())
                    .collect(Collectors.toSet());
            filter = filter.and(e -> equipKeysWithTag.contains(e.getEquipKey()));
        }

        if (StringUtils.hasText(dungeonKey)) {
            Set<String> equipKeysWithDungeon = dungeonRepo.findAll().stream()
                    .filter(d -> dungeonKey.equalsIgnoreCase(d.getDungeonKey()))
                    .map(d -> d.getEquipKey())
                    .collect(Collectors.toSet());
            filter = filter.and(e -> equipKeysWithDungeon.contains(e.getEquipKey()));
        }

        if (StringUtils.hasText(keyword)) {
            final String kw = keyword.trim().toLowerCase(Locale.ROOT);
            filter = filter.and(e ->
                    (e.getName() != null && e.getName().toLowerCase(Locale.ROOT).contains(kw))
                            || (e.getEquipKey() != null && e.getEquipKey().toLowerCase(Locale.ROOT).contains(kw))
            );
        }

        List<EquipmentDefinition> filtered = baseList.stream().filter(filter).toList();

        // 4) 分页切片
        int from = (page - 1) * size;
        if (from >= filtered.size()) return List.of();
        int to = Math.min(from + size, filtered.size());
        List<EquipmentDefinition> pageList = filtered.subList(from, to);

        // 5) 组装 VO（可选地附带 tags/dungeons）
        Map<String, List<String>> tagMap;
        Map<String, List<String>> dungeonMap;
        if (includeTags) {
            tagMap = tagRepo.findAll().stream()
                    .collect(Collectors.groupingBy(
                            t -> t.getEquipKey(),
                            Collectors.mapping(t -> t.getTag(), Collectors.toList())
                    ));
        } else {
            tagMap = Collections.emptyMap();
        }
        if (includeDungeons) {
            dungeonMap = dungeonRepo.findAll().stream()
                    .collect(Collectors.groupingBy(
                            d -> d.getEquipKey(),
                            Collectors.mapping(d -> d.getDungeonKey(), Collectors.toList())
                    ));
        } else {
            dungeonMap = Collections.emptyMap();
        }

        return pageList.stream().map(e -> EquipmentItemVO.builder()
                .equipKey(e.getEquipKey())
                .name(e.getName())
                .slot(e.getSlot())
                .rarity(e.getRarity())
                .starMax(safeInt(e.getStarMax()))
                .enabled(e.isEnabled())
                // ====== 新增：透传描述与图标 ======
                .description(e.getDescription())     // 中文备注：来自实体字段 description
                .icon(e.getIcon())                   // 中文备注：来自实体字段 icon
                .tags(includeTags ? tagMap.getOrDefault(e.getEquipKey(), List.of()) : null)
                .dungeons(includeDungeons ? dungeonMap.getOrDefault(e.getEquipKey(), List.of()) : null)
                .build()
        ).toList();
    }

    /** 中文备注：详情 */
    public EquipmentItemVO detail(String equipKey, boolean includeTags, boolean includeDungeons) {
        if (!StringUtils.hasText(equipKey)) {
            throw new BizException(ResultCode.VALIDATE_FAILED, "equipKey 不能为空");
        }
        EquipmentDefinition e = defRepo.findByEquipKey(equipKey)
                .orElseThrow(() -> new BizException(ResultCode.NOT_FOUND, "装备不存在：" + equipKey));

        List<String> tags = includeTags
                ? tagRepo.findByEquipKey(equipKey).stream().map(t -> t.getTag()).toList()
                : null;
        List<String> dungeons = includeDungeons
                ? dungeonRepo.findByEquipKey(equipKey).stream().map(d -> d.getDungeonKey()).toList()
                : null;

        return EquipmentItemVO.builder()
                .equipKey(e.getEquipKey())
                .name(e.getName())
                .slot(e.getSlot())
                .rarity(e.getRarity())
                .starMax(safeInt(e.getStarMax()))
                .enabled(e.isEnabled())
                // ====== 新增：透传描述与图标 ======
                .description(e.getDescription())     // 中文备注：实体字段
                .icon(e.getIcon())                   // 中文备注：实体字段
                .tags(tags)
                .dungeons(dungeons)
                .build();
    }

    // ====================== 工具方法 ======================

    /** 中文备注：将前端 slotKey（如 "cloak"）解析为枚举；兼容传 CLOAK */
    private EquipmentSlot parseSlot(String slotKey) {
        String s = slotKey.trim();
        // 1) 先按枚举内部 key 字段匹配（与前端 BagPage.vue slots.key 对齐）
        for (EquipmentSlot es : EquipmentSlot.values()) {
            if (es.getFrontKey().equalsIgnoreCase(s)) return es;
        }
        // 2) 回退按枚举名匹配（CLOAK / ARMOR 等）
        try { return EquipmentSlot.valueOf(s.toUpperCase(Locale.ROOT)); }
        catch (IllegalArgumentException ex) { return null; }
    }

    /** 中文备注：解析稀有度；兼容传入 colorHex 或名称 */
    private EquipmentRarity parseRarity(String rarityKey) {
        String s = rarityKey.trim();
        for (EquipmentRarity er : EquipmentRarity.values()) {
            if (er.name().equalsIgnoreCase(s)) return er;
            if (er.getColorHex().equalsIgnoreCase(s)) return er; // 支持按颜色码传参
        }
        return null;
    }

    private Integer safeInt(Integer v){ return v == null ? null : v; }
}
