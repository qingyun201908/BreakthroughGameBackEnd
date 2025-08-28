// 文件：BreakthroughGame/backEnd/UserModule/UserRestful/service/EquipmentImportService.java
package BreakthroughGame.backEnd.UserModule.UserRestful.service;

import BreakthroughGame.backEnd.DefinitionModule.entity.*;
import BreakthroughGame.backEnd.DefinitionModule.repository.EquipmentDefinitionDungeonRepository;
import BreakthroughGame.backEnd.DefinitionModule.repository.EquipmentDefinitionRepository;
import BreakthroughGame.backEnd.DefinitionModule.repository.EquipmentDefinitionTagRepository;
import BreakthroughGame.backEnd.UserModule.UserInfo.entity.CombatAttributes;
import BreakthroughGame.backEnd.UserModule.UserRestful.common.api.ResultCode;
import BreakthroughGame.backEnd.UserModule.UserRestful.common.exception.BizException;
import BreakthroughGame.backEnd.UserModule.UserRestful.dto.EquipmentImportMode;
import BreakthroughGame.backEnd.UserModule.UserRestful.dto.EquipmentImportResult;
import lombok.RequiredArgsConstructor;
import org.apache.poi.ss.usermodel.*;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.InputStream;
import java.util.*;
import java.util.regex.Pattern;

/**
 * 中文备注：装备图鉴导入服务（无外键版本）
 * - 支持 Sheet 名 "codex" 或第 0 张表
 * - 支持 UPSERT / INSERT_ONLY / UPDATE_ONLY
 * - 标签与来源副本为“覆盖式更新”（先删后插）
 */
@Service
@RequiredArgsConstructor
public class EquipmentImportService {

    private final EquipmentDefinitionRepository defRepo;
    private final EquipmentDefinitionTagRepository tagRepo;
    private final EquipmentDefinitionDungeonRepository dungeonRepo;

    private static final Set<String> SLOT_KEYS = Set.of(
            "cloak", "wrist", "glove", "armor", "belt", "pants", "shoes", "ring", "necklace", "shoulder");
    private static final Set<String> RARITIES = Set.of(
            "COMMON", "UNCOMMON", "RARE", "EPIC", "LEGENDARY", "MYTHIC");
    private static final Pattern HEX = Pattern.compile("^#[0-9a-fA-F]{6}$");

    // 中文备注：表头字段（不区分大小写）
    private static final String[] COLUMNS = {
            "equip_key", "name", "slot", "rarity", "star_max", "level_requirement", "color_hex",
            "icon", "description", "sort_order", "enabled", "release_version",
            "attr_attack", "attr_atk_speed_pct", "attr_crit_rate_pct", "attr_crit_dmg_pct", "attr_hit_pct", "attr_penetration",
            "attr_metal", "attr_wood", "attr_water", "attr_fire", "attr_earth", "attr_chaos",
            "tags", "dungeon_keys"
    };

    @Transactional
    public EquipmentImportResult importFromExcel(MultipartFile file, EquipmentImportMode mode) throws Exception {
        EquipmentImportResult report = new EquipmentImportResult();
        try (InputStream is = file.getInputStream(); Workbook wb = new XSSFWorkbook(is)) {
            Sheet sheet = Optional.ofNullable(wb.getSheet("codex")).orElse(wb.getSheetAt(0));
            if (sheet == null)
                throw new BizException(ResultCode.B_BIZ_ERROR, "Excel 中没有可读取的工作表。");

            // 解析表头
            Map<String, Integer> head = parseHeader(sheet.getRow(0));
            int rows = sheet.getPhysicalNumberOfRows();
            report.setTotalRows(Math.max(0, rows - 1));

            for (int r = 1; r < rows; r++) {
                Row row = sheet.getRow(r);
                if (row == null || isEmptyRow(row)) {
                    report.setSkipped(report.getSkipped() + 1);
                    continue;
                }

                try {
                    // ===== 基础必填 =====
                    String equipKey = getString(row, head, "equip_key");
                    String name = getString(row, head, "name");
                    String slotStr = optLower(getString(row, head, "slot"));
                    String rarity = optUpper(getString(row, head, "rarity"));

                    if (isBlank(equipKey) || isBlank(name) || isBlank(slotStr) || isBlank(rarity)) {
                        throw new BizException(ResultCode.B_BIZ_ERROR, "必填字段缺失：equip_key/name/slot/rarity");

                    }
                    if (!SLOT_KEYS.contains(slotStr)) {
                        throw new BizException(ResultCode.B_BIZ_ERROR, "非法 slot：" + slotStr);

                    }
                    if (!RARITIES.contains(rarity)) {
                        throw new BizException(ResultCode.B_BIZ_ERROR, "非法 rarity：" + rarity);

                    }

                    // ===== 其余字段 =====
                    Integer starMax = getInt(row, head, "star_max", 5);
                    Integer lvlReq = getInt(row, head, "level_requirement", 1); // 若 DB 列为 level_req，实体已映射
                    String colorHex = getString(row, head, "color_hex");         // 可空：DB 触发器按稀有度补
                    String icon = getString(row, head, "icon");
                    String desc = getString(row, head, "description");
                    Integer sortOrder = getInt(row, head, "sort_order", 0);
                    Boolean enabled = getBool(row, head, "enabled", true);
                    String relVer = getString(row, head, "release_version");

                    if (!isBlank(colorHex) && !HEX.matcher(colorHex).matches()) {
                        throw new BizException(ResultCode.B_BIZ_ERROR, "color_hex 非法（期望 #RRGGBB）： " + colorHex);

                    }
                    if (starMax < 0)
                        throw new BizException(ResultCode.B_BIZ_ERROR, "star_max 需 ≥ 0");

                    if (lvlReq < 1)
                        throw new BizException(ResultCode.B_BIZ_ERROR, "level_requirement 需 ≥ 1");


                    // ===== 属性字段 =====
                    CombatAttributes attrs = new CombatAttributes();
                    attrs.setAttack(getInt(row, head, "attr_attack", 0));
                    attrs.setAttackSpeedPercent(getInt(row, head, "attr_atk_speed_pct", 0));
                    attrs.setCritRatePercent(getInt(row, head, "attr_crit_rate_pct", 0));
                    attrs.setCritDamagePercent(getInt(row, head, "attr_crit_dmg_pct", 0));
                    attrs.setHitPercent(getInt(row, head, "attr_hit_pct", 0));
                    attrs.setPenetration(getInt(row, head, "attr_penetration", 0));
                    attrs.setMetal(getInt(row, head, "attr_metal", 0));
                    attrs.setWood(getInt(row, head, "attr_wood", 0));
                    attrs.setWater(getInt(row, head, "attr_water", 0));
                    attrs.setFire(getInt(row, head, "attr_fire", 0));
                    attrs.setEarth(getInt(row, head, "attr_earth", 0));
                    attrs.setChaos(getInt(row, head, "attr_chaos", 0));

                    // ===== 标签/来源（CSV）=====
                    String tagCsv = getString(row, head, "tags");
                    String dgnCsv = getString(row, head, "dungeon_keys");
                    Set<String> tags = parseCsv(tagCsv);
                    Set<String> dgns = parseCsv(dgnCsv);

                    // ===== upsert 主表 =====
                    boolean exists = defRepo.existsByEquipKey(equipKey);
                    if (mode == EquipmentImportMode.INSERT_ONLY && exists) {

                        throw new BizException(ResultCode.B_BIZ_ERROR, "INSERT_ONLY：重复 equip_key=" + equipKey);

                    }
                    if (mode == EquipmentImportMode.UPDATE_ONLY && !exists) {
                        throw new BizException(ResultCode.B_BIZ_ERROR, "UPDATE_ONLY：不存在 equip_key=" + equipKey);
                    }

                    EquipmentDefinition def = defRepo.findByEquipKey(equipKey).orElseGet(EquipmentDefinition::new);
                    boolean isNew = (def.getId() == null);

                    def.setEquipKey(equipKey);
                    def.setName(name);
                    def.setSlot(EquipmentSlot.ofFrontKey(slotStr));        // 通过前端 key 反查枚举
                    def.setRarity(EquipmentRarity.valueOf(rarity));        // 枚举大写
                    def.setStarMax(starMax);
                    def.setLevelRequirement(lvlReq);
                    def.setColorHex(isBlank(colorHex) ? null : colorHex);  // 空则触发器补色
                    def.setIcon(icon);
                    def.setDescription(desc);
                    def.setSortOrder(sortOrder);
                    def.setVersion(0L);
                    def.setAttributes(attrs);
                    def.setEnabled(Boolean.TRUE.equals(enabled));
                    def.setReleaseVersion(isBlank(relVer) ? "1.0.0" : relVer);

                    defRepo.save(def);
                    if (isNew) report.setInserted(report.getInserted() + 1);
                    else report.setUpdated(report.getUpdated() + 1);

                    // ===== 覆盖式更新 标签/来源（无外键：先删后插）=====
                    tagRepo.deleteByEquipKey(equipKey);
                    if (tags != null) {
                        for (String t : tags) {
                            if (!t.isBlank()) tagRepo.save(
                                    EquipmentDefinitionTag.builder().equipKey(equipKey).tag(t).build());
                        }
                    }

                    dungeonRepo.deleteByEquipKey(equipKey);
                    if (dgns != null) {
                        for (String dk : dgns) {
                            if (!dk.isBlank()) dungeonRepo.save(
                                    EquipmentDefinitionDungeon.builder().equipKey(equipKey).dungeonKey(dk).build());
                        }
                    }

                } catch (Exception ex) {
                    report.getErrors().add(new EquipmentImportResult.RowError(r + 1, ex.getMessage()));
                    report.setFailed(report.getFailed() + 1);
                }
            }
        }
        return report;
    }

    // ====================== 工具方法（中文备注） ======================

    private Map<String, Integer> parseHeader(Row header) {
        if (header == null)
            throw new BizException(ResultCode.B_BIZ_ERROR, "缺少表头行");

        Map<String, Integer> map = new HashMap<>();
        for (int i = 0; i < header.getLastCellNum(); i++) {
            Cell c = header.getCell(i);
            if (c == null) continue;
            String k = c.getStringCellValue();
            if (k != null) map.put(k.trim().toLowerCase(), i);
        }
        // 基础列校验
        for (String must : List.of("equip_key", "name", "slot", "rarity")) {
            if (!map.containsKey(must))
                throw new BizException(ResultCode.B_BIZ_ERROR, "表头缺少必需列");
        }
        return map;
    }

    private boolean isEmptyRow(Row row) {
        for (int i = row.getFirstCellNum(); i < row.getLastCellNum(); i++) {
            Cell c = row.getCell(i);
            if (c == null) continue;
            if (c.getCellType() == CellType.STRING && !c.getStringCellValue().trim().isEmpty()) return false;
            if (c.getCellType() != CellType.STRING) return false;
        }
        return true;
    }

    private String getString(Row row, Map<String, Integer> idx, String col) {
        Integer i = idx.get(col.toLowerCase());
        if (i == null) return null;
        Cell c = row.getCell(i);
        if (c == null) return null;
        return switch (c.getCellType()) {
            case STRING -> c.getStringCellValue().trim();
            case NUMERIC -> String.valueOf((long) c.getNumericCellValue()); // 数字转字符串（兼容 Excel 键入）
            case BOOLEAN -> String.valueOf(c.getBooleanCellValue());
            default -> null;
        };
    }

    private String optLower(String s) {
        return s == null ? null : s.trim().toLowerCase();
    }

    private String optUpper(String s) {
        return s == null ? null : s.trim().toUpperCase();
    }

    private Integer getInt(Row row, Map<String, Integer> idx, String col, int defVal) {
        String s = getString(row, idx, col);
        if (isBlank(s)) return defVal;
        try {
            return Integer.parseInt(s);
        } catch (NumberFormatException e) {
            throw new BizException(ResultCode.B_BIZ_ERROR, col + " 需要整数，实际：" + s);
        }
    }

    private Boolean getBool(Row row, Map<String, Integer> idx, String col, boolean defVal) {
        String s = getString(row, idx, col);
        if (isBlank(s)) return defVal;
        if ("true".equalsIgnoreCase(s) || "1".equals(s)) return true;
        if ("false".equalsIgnoreCase(s) || "0".equals(s)) return false;
        throw new BizException(ResultCode.B_BIZ_ERROR, col + " 需要布尔：true/false 或 1/0，实际：" + s);

    }

    private boolean isBlank(String s) {
        return s == null || s.trim().isEmpty();
    }

    private Set<String> parseCsv(String csv) {
        if (isBlank(csv)) return Set.of();
        String[] parts = csv.split("[,，;；]");
        Set<String> set = new LinkedHashSet<>();
        for (String p : parts) {
            String v = p.trim();
            if (!v.isEmpty()) set.add(v);
        }
        return set;
    }
}
