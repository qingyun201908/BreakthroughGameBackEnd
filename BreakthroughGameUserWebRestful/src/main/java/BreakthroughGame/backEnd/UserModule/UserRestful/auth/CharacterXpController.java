// 文件：BreakthroughGame/backEnd/UserModule/UserRestful/auth/CharacterXpController.java
package BreakthroughGame.backEnd.UserModule.UserRestful.auth;

import BreakthroughGame.backEnd.UserModule.UserRestful.dto.LevelSnapshot;
import BreakthroughGame.backEnd.UserModule.UserRestful.response.AllResponse;
import BreakthroughGame.backEnd.UserModule.UserRestful.response.LevelSnapshotResponse; // ✅ 新增：专用响应
import BreakthroughGame.backEnd.UserModule.UserRestful.service.CharacterXpService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.media.Content;
import jakarta.validation.constraints.Min;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
// ⬇️ 业务日志：引入 MDC 以便在日志中输出 rid（请求追踪ID）
import org.slf4j.MDC;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

/**
 * 人物经验 REST 接口
 * - 增加经验：原子 upsert，返回最新快照
 * - 查询快照：无记录返回默认值
 */
@CrossOrigin(
        origins = { "http://localhost:9000", "http://127.0.0.1:9000","http://127.0.0.1:5173","http://localhost:5173" },
        maxAge = 3600,
        methods = { RequestMethod.POST, RequestMethod.OPTIONS,RequestMethod.GET }
)
@Tag(name = "Character XP", description = "人物经验相关接口")
@RestController
@RequestMapping("/api/characters")
@RequiredArgsConstructor
@Validated
public class CharacterXpController {
    private static final Logger LOGGER = LoggerFactory.getLogger(CharacterXpController.class);

    private final CharacterXpService service;

    @Operation(
            summary = "增加经验（原子）",
            description = "为指定人物原子增加经验值，并返回最新等级快照。负数将被业务层拒绝（A_BAD_REQUEST）。"
    )
    @ApiResponse(
            responseCode = "200",
            description = "成功，返回包裹式快照",
            content = @Content(schema = @Schema(implementation = LevelSnapshotResponse.class))
    )
    @ApiResponse(
            responseCode = "400",
            description = "请求不合法（如角色ID为空、delta < 0）",
            content = @Content(schema = @Schema(implementation = AllResponse.class))
    )
    @ApiResponse(
            responseCode = "409",
            description = "业务冲突（并发/数据异常等）",
            content = @Content(schema = @Schema(implementation = AllResponse.class))
    )
    @PostMapping("/{id}/xp:add")
    public LevelSnapshotResponse addXp(@PathVariable("id") UUID characterId,
                                       @RequestParam("delta") @Min(value = 0, message = "经验增量不能为负") long delta) {

        // ========= 业务日志开始（请求入参 + 耗时统计）=========
        final long startNs = System.nanoTime(); // 中文备注：纳秒计时，计算接口耗时
        final String rid = UUID.randomUUID().toString(); // 中文备注：本次调用的追踪ID
        MDC.put("rid", rid); // 中文备注：将 rid 放入 MDC，建议在日志配置中输出 %X{rid}

        // 中文备注：请求日志（关键业务维度尽量结构化打印）
        LOGGER.info("BIZ|XP_ADD_REQ|rid={}|characterId={}|delta={}", rid, characterId, delta);
        try {
            // 中文备注：服务层做原子 upsert，返回最新快照
            LevelSnapshot snapshot = service.addXp(characterId, delta);

            // 中文备注：结果日志（如快照过大，建议定制摘要或用 JSON 精简字段）
            LOGGER.info("BIZ|XP_ADD_OK|rid={}|characterId={}|snapshot={}", rid, characterId, snapshot);

            // ✅ 与 AllResponse 风格一致的包裹式返回
            return LevelSnapshotResponse.ok("增加经验成功", snapshot);
        } catch (IllegalArgumentException ex) {
            // 中文备注：可预期的参数/业务校验问题，用 warn 级别
            LOGGER.warn("BIZ|XP_ADD_BAD_REQ|rid={}|characterId={}|delta={}|msg={}", rid, characterId, delta, ex.getMessage(), ex);
            throw ex; // 中文备注：交由全局异常处理器转换为规范响应
        } catch (Exception ex) {
            // 中文备注：非预期异常，用 error 级别，保留堆栈
            LOGGER.error("BIZ|XP_ADD_ERR|rid={}|characterId={}|delta={}|ex={}", rid, characterId, delta, ex.toString(), ex);
            throw ex;
        } finally {
            // 中文备注：无论成功失败都打印耗时
            long tookMs = (System.nanoTime() - startNs) / 1_000_000L;
            LOGGER.info("BIZ|XP_ADD_DONE|rid={}|tookMs={}", rid, tookMs);
            MDC.remove("rid"); // 中文备注：清理 MDC，避免线程复用时污染
        }
        // ========= 业务日志结束 =========
    }

    @Operation(
            summary = "查询经验/等级快照",
            description = "返回人物的经验、等级、进度等快照信息。若不存在记录则返回默认值（totalXp=0，level=1）。"
    )
    @ApiResponse(
            responseCode = "200",
            description = "成功，返回包裹式快照",
            content = @Content(schema = @Schema(implementation = LevelSnapshotResponse.class))
    )
    @ApiResponse(
            responseCode = "400",
            description = "请求不合法（如角色ID为空）",
            content = @Content(schema = @Schema(implementation = AllResponse.class))
    )
    @GetMapping("/{id}/xp:snapshot")
    public LevelSnapshotResponse snapshot(@PathVariable("id") UUID characterId) {

        // ========= 业务日志开始（请求入参 + 耗时统计）=========
        final long startNs = System.nanoTime();
        final String rid = UUID.randomUUID().toString();
        MDC.put("rid", rid);

        LOGGER.info("BIZ|XP_SNAPSHOT_REQ|rid={}|characterId={}", rid, characterId);
        try {
            LevelSnapshot snapshot = service.snapshot(characterId);

            LOGGER.info("BIZ|XP_SNAPSHOT_OK|rid={}|characterId={}|snapshot={}", rid, characterId, snapshot);

            return LevelSnapshotResponse.ok("查询成功", snapshot);
        } catch (IllegalArgumentException ex) {
            LOGGER.warn("BIZ|XP_SNAPSHOT_BAD_REQ|rid={}|characterId={}|msg={}", rid, characterId, ex.getMessage(), ex);
            throw ex;
        } catch (Exception ex) {
            LOGGER.error("BIZ|XP_SNAPSHOT_ERR|rid={}|characterId={}|ex={}", rid, characterId, ex.toString(), ex);
            throw ex;
        } finally {
            long tookMs = (System.nanoTime() - startNs) / 1_000_000L;
            LOGGER.info("BIZ|XP_SNAPSHOT_DONE|rid={}|tookMs={}", rid, tookMs);
            MDC.remove("rid");
        }
        // ========= 业务日志结束 =========
    }
}
