// 文件：BreakthroughGame/backEnd/UserModule/UserRestful/auth/EquipmentImportController.java
package BreakthroughGame.backEnd.UserModule.UserRestful.auth;

import BreakthroughGame.backEnd.UserModule.UserRestful.dto.EquipmentImportMode;
import BreakthroughGame.backEnd.UserModule.UserRestful.dto.EquipmentImportResult;
import BreakthroughGame.backEnd.UserModule.UserRestful.response.EquipmentImportResponse;
import BreakthroughGame.backEnd.UserModule.UserRestful.service.EquipmentImportService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.constraints.NotNull;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import org.apache.poi.ss.usermodel.*;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;

import java.io.ByteArrayOutputStream;
import java.nio.charset.StandardCharsets;
import java.util.UUID;

/**
 * 装备图鉴导入 REST 接口
 * - 接收 Excel（.xlsx）上传，批量新增/更新 equipment_definition（无外键）
 * - 导入模式：UPSERT（默认）/ INSERT_ONLY / UPDATE_ONLY
 * - 提供模板下载（表头齐全）
 */
@CrossOrigin(
        origins = { "http://localhost:9000", "http://127.0.0.1:9000","http://127.0.0.1:5173","http://localhost:5173" },
        maxAge = 3600,
        methods = { RequestMethod.POST, RequestMethod.GET, RequestMethod.OPTIONS }
)
@Tag(name = "Equipment Codex Import", description = "装备图鉴导入")
@RestController
@RequestMapping("/api/equipment/import")
@RequiredArgsConstructor
@Validated
public class EquipmentImportController {

    private static final Logger LOGGER = LoggerFactory.getLogger(EquipmentImportController.class);

    private final EquipmentImportService service;

    @Operation(summary = "导入装备图鉴（Excel）",
            description = "上传 Excel（.xlsx），批量导入/更新装备图鉴。INSERT_ONLY/UPDATE_ONLY 可控制幂等策略。")
    @ApiResponse(responseCode = "200", description = "成功，返回导入报告",
            content = @Content(schema = @Schema(implementation = EquipmentImportResponse.class)))
    @PostMapping(consumes = MediaType.MULTIPART_FORM_DATA_VALUE, produces = MediaType.APPLICATION_JSON_VALUE)
    public EquipmentImportResponse importExcel(
            @RequestPart("file") @NotNull MultipartFile file,
            @RequestParam(name = "mode", defaultValue = "UPSERT") EquipmentImportMode mode
    ) throws Exception {
        final long startNs = System.nanoTime();
        final String rid = UUID.randomUUID().toString();
        MDC.put("rid", rid);

        LOGGER.info("BIZ|EQ_IMPORT_REQ|rid={}|mode={}|filename={}|size={}",
                rid, mode, file != null ? file.getOriginalFilename() : "null", file != null ? file.getSize() : -1);

        try {
            EquipmentImportResult report = service.importFromExcel(file, mode);
            LOGGER.info("BIZ|EQ_IMPORT_OK|rid={}|mode={}|totalRows={}|inserted={}|updated={}|failed={}",
                    rid, mode, report.getTotalRows(), report.getInserted(), report.getUpdated(), report.getFailed());
            return EquipmentImportResponse.ok("导入完成", report);
        } catch (IllegalArgumentException ex) {
            LOGGER.warn("BIZ|EQ_IMPORT_BAD_REQ|rid={}|mode={}|msg={}", rid, mode, ex.getMessage(), ex);
            throw ex;
        } catch (Exception ex) {
            LOGGER.error("BIZ|EQ_IMPORT_ERR|rid={}|mode={}|ex={}", rid, mode, ex.toString(), ex);
            throw ex;
        } finally {
            long tookMs = (System.nanoTime() - startNs) / 1_000_000L;
            LOGGER.info("BIZ|EQ_IMPORT_DONE|rid={}|tookMs={}", rid, tookMs);
            MDC.remove("rid");
        }
    }

    @Operation(summary = "下载 Excel 模板", description = "返回包含表头与示例行的 .xlsx 模板")
    @GetMapping(value = "/template", produces = "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet")
    public ResponseEntity<byte[]> downloadTemplate() throws Exception {
        try (Workbook wb = new XSSFWorkbook(); ByteArrayOutputStream bos = new ByteArrayOutputStream()) {
            Sheet sheet = wb.createSheet("codex");
            Row head = sheet.createRow(0);
            String[] headers = new String[]{
                    "equip_key","name","slot","rarity","star_max","level_requirement","color_hex",
                    "icon","description","sort_order","enabled","release_version",
                    "attr_attack","attr_atk_speed_pct","attr_crit_rate_pct","attr_crit_dmg_pct","attr_hit_pct","attr_penetration",
                    "attr_metal","attr_wood","attr_water","attr_fire","attr_earth","attr_chaos",
                    "tags","dungeon_keys"
            };
            for (int i = 0; i < headers.length; i++) {
                head.createCell(i).setCellValue(headers[i]);     // 中文备注：表头
                sheet.setColumnWidth(i, 18 * 256);               // 列宽
            }
            // 示例行（方便前端/策划理解字段）
            Row demo = sheet.createRow(1);
            demo.createCell(0).setCellValue("glove_taiji");                // equip_key（唯一）
            demo.createCell(1).setCellValue("太极手套");                      // name
            demo.createCell(2).setCellValue("glove");                      // slot: cloak/wrist/.../shoulder
            demo.createCell(3).setCellValue("COMMON");                     // rarity
            demo.createCell(4).setCellValue(5);                            // star_max
            demo.createCell(5).setCellValue(1);                            // level_requirement（若 DB 列为 level_req，无需关心）
            demo.createCell(6).setCellValue("#9ca3af");                    // color_hex（可留空，DB 触发器补色）
            demo.createCell(7).setCellValue("icons/gloves/taiji.png");     // icon
            demo.createCell(8).setCellValue("入门拳法手套，蕴含太极之意。");     // description
            demo.createCell(9).setCellValue(0);                            // sort_order
            demo.createCell(10).setCellValue(true);                        // enabled
            demo.createCell(11).setCellValue("1.0.0");                     // release_version
            demo.createCell(12).setCellValue(10);                          // attr_attack
            demo.createCell(13).setCellValue(0);  demo.createCell(14).setCellValue(0);
            demo.createCell(15).setCellValue(0);  demo.createCell(16).setCellValue(0);
            demo.createCell(17).setCellValue(0);  demo.createCell(18).setCellValue(0);
            demo.createCell(19).setCellValue(0);  demo.createCell(20).setCellValue(0);
            demo.createCell(21).setCellValue(0);  demo.createCell(22).setCellValue(0);
            demo.createCell(23).setCellValue(0);
            demo.createCell(24).setCellValue("入门,拳套,太极");               // tags（英文/中文逗号或分号分隔）
            demo.createCell(25).setCellValue("equip");                      // dungeon_keys（与 dungeon_definition.dungeon_key 对齐）

            wb.write(bos);
            byte[] bytes = bos.toByteArray();
            String filename = "equipment_codex_template.xlsx";
            return ResponseEntity.ok()
                    .header(HttpHeaders.CONTENT_DISPOSITION,
                            "attachment; filename=" +
                                    new String(filename.getBytes(StandardCharsets.UTF_8), StandardCharsets.ISO_8859_1))
                    .contentType(MediaType.parseMediaType(
                            "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"))
                    .body(bytes);
        }
    }
}
