// 文件：BreakthroughGame/backEnd/UserModule/UserRestful/service/DungeonImportService.java
package BreakthroughGame.backEnd.UserModule.UserRestful.service;

import BreakthroughGame.backEnd.UserModule.UserRestful.common.api.ResultCode;
import BreakthroughGame.backEnd.UserModule.UserRestful.common.exception.BizException;
import BreakthroughGame.backEnd.UserModule.UserRestful.dto.DungeonImportMode;
import BreakthroughGame.backEnd.UserModule.UserRestful.dto.DungeonImportResult;
import BreakthroughGame.backEnd.WarcraftDungeonsInfo.entity.DungeonDefinition;
import BreakthroughGame.backEnd.WarcraftDungeonsInfo.repository.DungeonDefinitionRepository;
import lombok.RequiredArgsConstructor;
import org.apache.poi.EncryptedDocumentException;
import org.apache.poi.ss.usermodel.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.InputStream;
import java.time.OffsetDateTime;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;

/**
 * 中文备注：
 * - 解析 Excel（首个 Sheet），根据表头映射列
 * - 逐行校验与入库，按导入模式决定新增/更新行为
 * - 发生单行错误时记录错误但不中断整个导入
 * - 顶层异常（入参非法/受保护/解析失败/模式冲突）统一转换为 BizException + ResultCode
 */
@Service
@RequiredArgsConstructor
public class DungeonImportService {

    private final DungeonDefinitionRepository repo;

    /** 中文备注：单次导入最大处理行数（防止过载） */
    private static final int MAX_ROWS = 10_000;

    /** 中文备注：限制最大文件大小（可按需调整，单位：字节；<=0 表示不限制） */
    private static final long MAX_FILE_SIZE = 20L * 1024 * 1024;

    @Transactional
    public DungeonImportResult importFromExcel(MultipartFile file, DungeonImportMode mode) {
        // —— 顶层参数与文件校验（统一抛 BizException）—— //
        if (file == null || file.isEmpty()) {
            throw new BizException(ResultCode.A_BAD_REQUEST, "请上传 Excel 文件（file 不能为空）");
        }
        if (!isExcelFile(file)) {
            // 中文备注：仅放行 .xlsx/.xls；其余类型拒绝
            throw new BizException(ResultCode.A_BAD_REQUEST, "文件类型不支持，仅支持 .xlsx 或 .xls");
        }
        if (MAX_FILE_SIZE > 0 && file.getSize() > MAX_FILE_SIZE) {
            throw new BizException(ResultCode.A_BAD_REQUEST, "文件过大，最大支持 " + (MAX_FILE_SIZE / (1024 * 1024)) + "MB");
        }
        if (mode == null) mode = DungeonImportMode.UPSERT;

        final DataFormatter fmt = new DataFormatter(Locale.CHINA);
        final DungeonImportResult report = new DungeonImportResult();

        try (InputStream is = file.getInputStream(); Workbook wb = WorkbookFactory.create(is)) {

            Sheet sheet = wb.getNumberOfSheets() > 0 ? wb.getSheetAt(0) : null;
            if (sheet == null) {
                throw new BizException(ResultCode.A_BAD_REQUEST, "Excel 内不存在任何工作表");
            }
            if (sheet.getPhysicalNumberOfRows() <= 1) {
                // 只有表头或空
                return report;
            }

            // —— 读取表头并建立列名 → 索引的映射 —— //
            Row head = sheet.getRow(sheet.getFirstRowNum());
            if (head == null) {
                throw new BizException(ResultCode.A_BAD_REQUEST, "缺少表头行（第 1 行）");
            }
            Map<String, Integer> colIndex = new HashMap<>();
            for (int i = head.getFirstCellNum(); i < head.getLastCellNum(); i++) {
                Cell c = head.getCell(i);
                if (c == null) continue;
                String name = fmt.formatCellValue(c).trim().toLowerCase(Locale.ROOT);
                if (!name.isEmpty()) colIndex.put(name, i);
            }

            // 必需列校验（抛业务异常）
            requireColumn(colIndex, "dungeon_key");
            requireColumn(colIndex, "title");

            // —— 逐行读取 —— //
            int first = sheet.getFirstRowNum() + 1;    // 数据从第二行开始
            int last = Math.min(sheet.getLastRowNum(), first - 1 + MAX_ROWS);

            for (int r = first; r <= last; r++) {
                Row row = sheet.getRow(r);
                if (row == null || isRowBlank(row)) continue; // 空行跳过

                report.setTotalRows(report.getTotalRows() + 1);

                try {
                    // —— 取值 + 业务校验 —— //
                    String dungeonKey = getString(fmt, row, colIndex.get("dungeon_key"));
                    String title = getString(fmt, row, colIndex.get("title"));
                    if (isBlank(dungeonKey)) throw new IllegalArgumentException("dungeon_key 不能为空");
                    if (isBlank(title))      throw new IllegalArgumentException("title 不能为空");

                    Integer dailyRunsMax  = getInt(fmt, row, colIndex.get("daily_runs_max"), 3);
                    Integer maxDifficulty = getInt(fmt, row, colIndex.get("max_difficulty"), 9);
                    Boolean isActive      = getBool(fmt, row, colIndex.get("is_active"), true);
                    Integer sortOrder     = getInt(fmt, row, colIndex.get("sort_order"), 0);

                    if (dailyRunsMax < 0)  throw new IllegalArgumentException("daily_runs_max 必须 ≥ 0");
                    if (maxDifficulty < 1) throw new IllegalArgumentException("max_difficulty 必须 ≥ 1");

                    // —— UPSERT / INSERT_ONLY —— //
                    Optional<DungeonDefinition> opt = repo.findByDungeonKey(dungeonKey);
                    if (opt.isPresent()) {
                        if (mode == DungeonImportMode.INSERT_ONLY) {
                            // 中文备注：模式冲突 → 抛业务异常（顶层语义错误）
                            throw new BizException(ResultCode.A_CONFLICT, "INSERT_ONLY 模式下已存在相同 dungeon_key：" + dungeonKey);
                        }
                        // 更新
                        DungeonDefinition def = opt.get();
                        def.setTitle(title);
                        def.setDailyRunsMax(dailyRunsMax);
                        def.setMaxDifficulty(maxDifficulty);
                        def.setActive(Boolean.TRUE.equals(isActive));
                        def.setSortOrder(sortOrder);
                        def.setUpdatedAt(OffsetDateTime.now());
                        repo.save(def);
                    } else {
                        // 新增
                        DungeonDefinition def = new DungeonDefinition();
                        def.setDungeonKey(dungeonKey);
                        def.setTitle(title);
                        def.setDailyRunsMax(dailyRunsMax);
                        def.setMaxDifficulty(maxDifficulty);
                        def.setActive(Boolean.TRUE.equals(isActive));
                        def.setSortOrder(sortOrder);
                        def.setUpdatedAt(OffsetDateTime.now());
                        repo.save(def);
                    }

                    report.setSuccess(report.getSuccess() + 1);
                } catch (BizException be) {
                    // 中文备注：行内抛出的 BizException（如 INSERT_ONLY 冲突），记为该行错误并继续
                    report.setFailed(report.getFailed() + 1);
                    report.getErrors().add(new DungeonImportResult.RowError(r + 1, be.getMessage()));
                } catch (Exception rowEx) {
                    // 中文备注：其他行级错误（解析/校验等）一律记录并继续
                    report.setFailed(report.getFailed() + 1);
                    report.getErrors().add(new DungeonImportResult.RowError(r + 1, rowEx.getMessage()));
                }
            }

            // —— 可选：若你希望“有错即失败”，可在此启用严格模式 —— //
            // if (report.getFailed() > 0) throw new BizException(ResultCode.A_BAD_REQUEST, "导入部分失败，请修正错误后重试");

            return report;

        } catch (EncryptedDocumentException e) {
            // 中文备注：Excel 受保护/加密
            throw new BizException(ResultCode.A_BAD_REQUEST, "无法打开受保护的 Excel 文件");
        } catch (BizException be) {
            // 中文备注：继续向上抛出业务异常（已带 code+message）
            throw be;
        } catch (Exception e) {
            // 中文备注：兜底异常 → 统一为业务异常，避免泄露底层细节
            throw new BizException(ResultCode.A_BAD_REQUEST, "Excel 解析失败：" + e.getMessage());
        }
    }

    // ================= 工具方法（包含中文备注） ================= //

    /** 中文备注：判定是否为 Excel 文件（后缀或常见 Content-Type） */
    private static boolean isExcelFile(MultipartFile file) {
        String name = file.getOriginalFilename();
        String lower = name == null ? "" : name.toLowerCase(Locale.ROOT);
        if (lower.endsWith(".xlsx") || lower.endsWith(".xls")) return true;

        String ct = file.getContentType();
        if (ct == null) return false;
        String lct = ct.toLowerCase(Locale.ROOT);
        return lct.contains("spreadsheetml") || lct.contains("excel");
    }

    /** 中文备注：必需列校验，不存在即抛业务异常 */
    private static void requireColumn(Map<String, Integer> colIndex, String name) {
        if (!colIndex.containsKey(name)) {
            throw new BizException(ResultCode.A_BAD_REQUEST, "缺少必需表头列：" + name);
        }
    }

    /** 中文备注：判空行（任一非空单元格即视为非空） */
    private static boolean isRowBlank(Row row) {
        DataFormatter df = new DataFormatter();
        for (int i = row.getFirstCellNum(); i < row.getLastCellNum(); i++) {
            Cell c = row.getCell(i);
            if (c != null && c.getCellType() != CellType.BLANK && !df.formatCellValue(c).trim().isEmpty()) {
                return false;
            }
        }
        return true;
    }

    /** 中文备注：字符串取值（去空格；空字符串→null） */
    private static String getString(DataFormatter fmt, Row row, Integer idx) {
        if (idx == null) return null;
        String v = fmt.formatCellValue(row.getCell(idx)).trim();
        return v.isEmpty() ? null : v;
    }

    /** 中文备注：整数取值（允许 "1,234"、小数会四舍五入；空→默认值） */
    private static Integer getInt(DataFormatter fmt, Row row, Integer idx, Integer defVal) {
        if (idx == null) return defVal;
        String raw = fmt.formatCellValue(row.getCell(idx)).trim();
        if (raw.isEmpty()) return defVal;
        try {
            double d = Double.parseDouble(raw.replaceAll(",", ""));
            return (int) Math.round(d);
        } catch (NumberFormatException e) {
            throw new IllegalArgumentException("整数解析失败：" + raw);
        }
    }

    /** 中文备注：布尔取值（支持 true/false/1/0/是/否/y/n；空→默认值） */
    private static Boolean getBool(DataFormatter fmt, Row row, Integer idx, Boolean defVal) {
        if (idx == null) return defVal;
        String raw = fmt.formatCellValue(row.getCell(idx)).trim().toLowerCase(Locale.ROOT);
        if (raw.isEmpty()) return defVal;
        switch (raw) {
            case "1": case "true": case "是": case "y": case "yes": return true;
            case "0": case "false": case "否": case "n": case "no":  return false;
            default: throw new IllegalArgumentException("布尔解析失败（支持 true/false/1/0/是/否/y/n）：" + raw);
        }
    }

    /** 中文备注：字符串是否为空白 */
    private static boolean isBlank(String s) {
        return s == null || s.trim().isEmpty();
    }
}
