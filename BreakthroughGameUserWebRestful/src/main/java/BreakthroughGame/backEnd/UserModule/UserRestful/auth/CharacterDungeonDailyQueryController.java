// 文件：BreakthroughGame/backEnd/UserModule/UserRestful/auth/CharacterDungeonDailyQueryController.java
package BreakthroughGame.backEnd.UserModule.UserRestful.auth;

import BreakthroughGame.backEnd.UserModule.UserRestful.dto.CharacterDailyVO;
import BreakthroughGame.backEnd.UserModule.UserRestful.dto.PageResult;
import BreakthroughGame.backEnd.UserModule.UserRestful.response.AllResponse;
import BreakthroughGame.backEnd.UserModule.UserRestful.response.CharacterDailyDetailResponse;
import BreakthroughGame.backEnd.UserModule.UserRestful.response.CharacterDailyListResponse;
import BreakthroughGame.backEnd.UserModule.UserRestful.service.CharacterDungeonDailyQueryService;

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

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

@Tag(name = "DungeonDaily", description = "角色-副本当日计数 查询接口")
@CrossOrigin(
        // 中文备注：按您现有网关/前端域名配置 CORS
        origins = { "http://localhost:9000", "http://127.0.0.1:9000","http://127.0.0.1:5173","http://localhost:5173" },
        allowCredentials = "true",
        maxAge = 3600
)
@RestController
@RequestMapping("/api/dungeonDaily")
@RequiredArgsConstructor
@Validated
public class CharacterDungeonDailyQueryController {

    private static final Logger LOGGER = LoggerFactory.getLogger(CharacterDungeonDailyQueryController.class);
    private final CharacterDungeonDailyQueryService service;

    // —— 1) 今日(JST) 列表 —— //
    @Operation(
            summary = "今日(JST)计数列表",
            description = "按 Asia/Tokyo 计算 dayKey，返回该角色今日所有副本的计数条目",
            responses = {
                    @ApiResponse(responseCode = "200", description = "OK", content = @Content(schema = @Schema(implementation = CharacterDailyListResponse.class)))
            }
    )
    @GetMapping("/today")
    public AllResponse listToday(@RequestParam("characterId") UUID characterId) {
        String rid = UUID.randomUUID().toString().substring(0, 8);
        MDC.put("rid", rid);
        long startNs = System.nanoTime();
        try {
            List<CharacterDailyVO> rows = service.listToday(characterId);
            // 中文备注：当日列表本质不分页，这里统一包装为 PageResult；页码从 0 开始
            int page = 0;                                                // 当前页码固定为 0
            int size = rows.size();                                      // 当前页大小 = 实际条数
            long totalElements = rows.size();                            // 总记录数
            int totalPages = rows.isEmpty() ? 0 : 1;                     // 空列表=0页，否则=1页
            PageResult<CharacterDailyVO> pr = new PageResult<>(page, size, totalElements, totalPages, rows);
            return CharacterDailyListResponse.ok("查询成功", pr);
        } catch (Exception ex) {
            LOGGER.error("BIZ|DGD_TODAY_ERR|rid={}|ex={}", rid, ex.toString(), ex);
            throw ex;
        } finally {
            long tookMs = (System.nanoTime() - startNs) / 1_000_000L;
            LOGGER.info("BIZ|DGD_TODAY_DONE|rid={}|tookMs={}", rid, tookMs);
            MDC.remove("rid");
        }
    }

    // —— 2) 单条详情（指定副本 + 指定日） —— //
    @Operation(
            summary = "查询单条详情",
            description = "指定 characterId + dungeonKey + dayKey（yyyy-MM-dd）",
            responses = {
                    @ApiResponse(responseCode = "200", description = "OK", content = @Content(schema = @Schema(implementation = CharacterDailyDetailResponse.class)))
            }
    )
    @GetMapping("/one")
    public AllResponse getOne(
            @RequestParam("characterId") UUID characterId,
            @RequestParam("dungeonKey")  String dungeonKey,
            @RequestParam("dayKey")      String dayKey // 中文备注：yyyy-MM-dd
    ) {
        String rid = UUID.randomUUID().toString().substring(0, 8);
        MDC.put("rid", rid);
        long startNs = System.nanoTime();
        try {
            LocalDate day = LocalDate.parse(dayKey);
            CharacterDailyVO vo = service.getOne(characterId, dungeonKey, day);
            return CharacterDailyDetailResponse.ok("查询成功", vo);
        } catch (Exception ex) {
            LOGGER.error("BIZ|DGD_ONE_ERR|rid={}|ex={}", rid, ex.toString(), ex);
            throw ex;
        } finally {
            long tookMs = (System.nanoTime() - startNs) / 1_000_000L;
            LOGGER.info("BIZ|DGD_ONE_DONE|rid={}|tookMs={}", rid, tookMs);
            MDC.remove("rid");
        }
    }

    // —— 3) 分页查询（可选条件） —— //
    @Operation(
            summary = "分页查询（可选条件）",
            description = "可选过滤：characterId / dungeonKey / dayFrom / dayTo；按 updatedAt 倒序；页码从 0 开始",
            responses = {
                    @ApiResponse(responseCode = "200", description = "OK", content = @Content(schema = @Schema(implementation = CharacterDailyListResponse.class)))
            }
    )
    @GetMapping("/page")
    public AllResponse page(
            @RequestParam(value = "characterId", required = false) UUID characterId,
            @RequestParam(value = "dungeonKey",  required = false) String dungeonKey,
            @RequestParam(value = "dayFrom",     required = false) String dayFrom,
            @RequestParam(value = "dayTo",       required = false) String dayTo,
            @RequestParam(value = "pageNo",      defaultValue = "0")  int pageNo,     // 中文备注：从 0 开始
            @RequestParam(value = "pageSize",    defaultValue = "20") int pageSize
    ) {
        String rid = UUID.randomUUID().toString().substring(0, 8);
        MDC.put("rid", rid);
        long startNs = System.nanoTime();
        try {
            LocalDate from = (dayFrom == null || dayFrom.isBlank()) ? null : LocalDate.parse(dayFrom);
            LocalDate to   = (dayTo   == null || dayTo.isBlank())   ? null : LocalDate.parse(dayTo);
            PageResult<CharacterDailyVO> pr = service.pageQuery(characterId, dungeonKey, from, to, pageNo, pageSize);
            return CharacterDailyListResponse.ok("查询成功", pr);
        } catch (Exception ex) {
            LOGGER.error("BIZ|DGD_PAGE_ERR|rid={}|ex={}", rid, ex.toString(), ex);
            throw ex;
        } finally {
            long tookMs = (System.nanoTime() - startNs) / 1_000_000L;
            LOGGER.info("BIZ|DGD_PAGE_DONE|rid={}|tookMs={}", rid, tookMs);
            MDC.remove("rid");
        }
    }
}
