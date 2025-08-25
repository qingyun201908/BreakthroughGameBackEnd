// 文件：BreakthroughGame/backEnd/UserModule/UserRestful/auth/DungeonQueryController.java
package BreakthroughGame.backEnd.UserModule.UserRestful.auth;

import BreakthroughGame.backEnd.UserModule.UserRestful.dto.PageResult;
import BreakthroughGame.backEnd.UserModule.UserRestful.dto.DungeonVO;
import BreakthroughGame.backEnd.UserModule.UserRestful.response.AllResponse;
import BreakthroughGame.backEnd.UserModule.UserRestful.response.DungeonListResponse;
import BreakthroughGame.backEnd.UserModule.UserRestful.response.DungeonDetailResponse;
import BreakthroughGame.backEnd.UserModule.UserRestful.service.DungeonQueryService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

/**
 * 中文备注：副本查询 REST 控制器
 */
@CrossOrigin(
        origins = { "http://localhost:9000", "http://127.0.0.1:9000","http://127.0.0.1:5173","http://localhost:5173" },
        maxAge = 3600,
        methods = { RequestMethod.GET, RequestMethod.OPTIONS }
)
@Tag(name = "Dungeon Query", description = "副本查询接口")
@RestController
@RequestMapping("/api/dungeons")
@RequiredArgsConstructor
@Validated
public class DungeonQueryController {

    private static final Logger LOGGER = LoggerFactory.getLogger(DungeonQueryController.class);

    private final DungeonQueryService service;

    @Operation(
            summary = "分页查询副本",
            description = "支持按 active 过滤、按 keyword 模糊搜索（作用于 dungeon_key 与 title），并支持分页与排序。"
    )
    @ApiResponse(
            responseCode = "200",
            description = "成功，返回分页结果",
            content = @Content(schema = @Schema(implementation = DungeonListResponse.class))
    )
    @ApiResponse(
            responseCode = "400",
            description = "请求不合法",
            content = @Content(schema = @Schema(implementation = AllResponse.class))
    )
    @GetMapping
    public DungeonListResponse page(
            @RequestParam(name = "active", required = false) Boolean active,     // 中文备注：是否启用（可不传）
            @RequestParam(name = "keyword", required = false) String keyword,    // 中文备注：模糊搜索关键字
            @RequestParam(name = "page", defaultValue = "0") int page,           // 中文备注：页码（从0开始）
            @RequestParam(name = "size", defaultValue = "20") int size,          // 中文备注：每页大小（最大200）
            @RequestParam(name = "sortBy", defaultValue = "sortOrder") String sortBy,   // 中文备注：排序字段
            @RequestParam(name = "sortDir", defaultValue = "asc") String sortDir        // 中文备注：asc/desc
    ) {
        // ========= 业务日志：追踪ID与耗时 =========
        final long startNs = System.nanoTime();
        final String rid = UUID.randomUUID().toString();
        MDC.put("rid", rid);

        LOGGER.info("BIZ|DUNGEON_PAGE_REQ|rid={}|active={}|keyword={}|page={}|size={}|sortBy={}|sortDir={}",
                rid, active, keyword, page, size, sortBy, sortDir);
        try {
            PageResult<DungeonVO> result = service.pageQuery(active, keyword, page, size, sortBy, sortDir);
            LOGGER.info("BIZ|DUNGEON_PAGE_OK|rid={}|totalElements={}|totalPages={}", rid, result.getTotalElements(), result.getTotalPages());
            return DungeonListResponse.ok("查询成功", result);
        } catch (IllegalArgumentException ex) {
            LOGGER.warn("BIZ|DUNGEON_PAGE_BAD_REQ|rid={}|msg={}", rid, ex.getMessage(), ex);
            throw ex;
        } catch (Exception ex) {
            LOGGER.error("BIZ|DUNGEON_PAGE_ERR|rid={}|ex={}", rid, ex.toString(), ex);
            throw ex;
        } finally {
            long tookMs = (System.nanoTime() - startNs) / 1_000_000L;
            LOGGER.info("BIZ|DUNGEON_PAGE_DONE|rid={}|tookMs={}", rid, tookMs);
            MDC.remove("rid");
        }
    }

    @Operation(
            summary = "根据 dungeon_key 查询单个副本",
            description = "精确匹配 dungeon_key，返回单条记录。"
    )
    @ApiResponse(
            responseCode = "200",
            description = "成功，返回详情",
            content = @Content(schema = @Schema(implementation = DungeonDetailResponse.class))
    )
    @ApiResponse(
            responseCode = "400",
            description = "请求不合法或未找到",
            content = @Content(schema = @Schema(implementation = AllResponse.class))
    )
    @GetMapping("/{dungeonKey}")
    public DungeonDetailResponse getOne(@PathVariable("dungeonKey") String dungeonKey) {
        final long startNs = System.nanoTime();
        final String rid = UUID.randomUUID().toString();
        MDC.put("rid", rid);

        LOGGER.info("BIZ|DUNGEON_GET_REQ|rid={}|dungeonKey={}", rid, dungeonKey);
        try {
            DungeonVO vo = service.getByKey(dungeonKey);
            LOGGER.info("BIZ|DUNGEON_GET_OK|rid={}|dungeonKey={}", rid, dungeonKey);
            return DungeonDetailResponse.ok("查询成功", vo);
        } catch (IllegalArgumentException ex) {
            LOGGER.warn("BIZ|DUNGEON_GET_BAD_REQ|rid={}|msg={}", rid, ex.getMessage(), ex);
            throw ex;
        } catch (Exception ex) {
            LOGGER.error("BIZ|DUNGEON_GET_ERR|rid={}|ex={}", rid, ex.toString(), ex);
            throw ex;
        } finally {
            long tookMs = (System.nanoTime() - startNs) / 1_000_000L;
            LOGGER.info("BIZ|DUNGEON_GET_DONE|rid={}|tookMs={}", rid, tookMs);
            MDC.remove("rid");
        }
    }
}
