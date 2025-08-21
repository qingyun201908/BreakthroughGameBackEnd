// 文件：BreakthroughGame/backEnd/UserModule/UserRestful/auth/DungeonImportController.java
package BreakthroughGame.backEnd.UserModule.UserRestful.auth;

import BreakthroughGame.backEnd.UserModule.UserRestful.dto.DungeonImportMode;
import BreakthroughGame.backEnd.UserModule.UserRestful.dto.DungeonImportResult;
import BreakthroughGame.backEnd.UserModule.UserRestful.response.AllResponse;
import BreakthroughGame.backEnd.UserModule.UserRestful.response.DungeonImportResponse; // ✅ 新增：专用响应
import BreakthroughGame.backEnd.UserModule.UserRestful.service.DungeonImportService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.constraints.NotNull;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
// ✅ 业务日志 MDC：输出 rid 便于追踪
import org.slf4j.MDC;
import org.springframework.http.MediaType;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.UUID;

/**
 * 副本定义导入 REST 接口
 * - 接收 Excel（.xlsx/.xls）上传，批量新增/更新 DungeonDefinition
 * - 导入模式：UPSERT（默认）/ INSERT_ONLY
 */
@CrossOrigin(
        origins = { "http://localhost:9000", "http://127.0.0.1:9000","http://127.0.0.1:5173","http://localhost:5173" },
        maxAge = 3600,
        methods = { RequestMethod.POST, RequestMethod.OPTIONS }
)
@Tag(name = "Dungeon Import", description = "副本定义导入")
@RestController
@RequestMapping("/api/dungeons/import")
@RequiredArgsConstructor
@Validated
public class DungeonImportController {

    private static final Logger LOGGER = LoggerFactory.getLogger(DungeonImportController.class);

    private final DungeonImportService service;

    @Operation(
            summary = "导入副本定义（Excel）",
            description = "上传 Excel 文件，批量导入/更新副本定义；INSERT_ONLY 模式下遇到重复会报业务冲突。"
    )
    @ApiResponse(
            responseCode = "200",
            description = "成功，返回导入报告（成功/失败/错误明细）",
            content = @Content(schema = @Schema(implementation = DungeonImportResponse.class))
    )
    @ApiResponse(
            responseCode = "400",
            description = "请求不合法（如未上传文件、文件类型不支持、Excel 解析失败）",
            content = @Content(schema = @Schema(implementation = AllResponse.class))
    )
    @ApiResponse(
            responseCode = "409",
            description = "业务冲突（INSERT_ONLY 模式下重复 key 等）",
            content = @Content(schema = @Schema(implementation = AllResponse.class))
    )
    @PostMapping(consumes = MediaType.MULTIPART_FORM_DATA_VALUE, produces = MediaType.APPLICATION_JSON_VALUE)
    public DungeonImportResponse importExcel(
            @RequestPart("file") @NotNull MultipartFile file,       // 中文备注：Excel 文件
            @RequestParam(name = "mode", defaultValue = "UPSERT") DungeonImportMode mode // 中文备注：导入模式
    ) {
        // ========= 业务日志：请求追踪与耗时统计 =========
        final long startNs = System.nanoTime();          // 中文备注：纳秒计时，统计接口耗时
        final String rid = UUID.randomUUID().toString(); // 中文备注：本次请求追踪ID
        MDC.put("rid", rid);

        // 请求入参日志（结构化字段）
        LOGGER.info("BIZ|DUNGEON_IMPORT_REQ|rid={}|mode={}|filename={}|size={}",
                rid, mode, file != null ? file.getOriginalFilename() : "null", file != null ? file.getSize() : -1);

        try {
            // 调用服务层解析并入库
            DungeonImportResult report = service.importFromExcel(file, mode);

            // 成功日志（视情况可裁剪 report）
            LOGGER.info("BIZ|DUNGEON_IMPORT_OK|rid={}|mode={}|totalRows={}|success={}|failed={}",
                    rid, mode, report.getTotalRows(), report.getSuccess(), report.getFailed());

            // ✅ 返回包裹式响应（与 AllResponse 风格一致）
            return DungeonImportResponse.ok("导入完成", report);

            // 如果你的项目暂不使用统一返回体，可改为：
            // return report;
        } catch (IllegalArgumentException ex) {
            // 预期的参数/解析类问题，用 warn
            LOGGER.warn("BIZ|DUNGEON_IMPORT_BAD_REQ|rid={}|mode={}|msg={}", rid, mode, ex.getMessage(), ex);
            throw ex; // 交由全局异常处理器转为 AllResponse
        } catch (Exception ex) {
            // 非预期异常，用 error 并保留堆栈
            LOGGER.error("BIZ|DUNGEON_IMPORT_ERR|rid={}|mode={}|ex={}", rid, mode, ex.toString(), ex);
            throw ex;
        } finally {
            long tookMs = (System.nanoTime() - startNs) / 1_000_000L;
            LOGGER.info("BIZ|DUNGEON_IMPORT_DONE|rid={}|tookMs={}", rid, tookMs);
            MDC.remove("rid"); // 清理 MDC，避免线程复用污染
        }
    }
}
