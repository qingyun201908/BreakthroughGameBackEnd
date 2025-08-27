// 文件：BreakthroughGame/backEnd/UserModule/UserRestful/auth/CharacterDungeonRunController.java
package BreakthroughGame.backEnd.UserModule.UserRestful.auth;

import BreakthroughGame.backEnd.UserModule.UserRestful.dto.DungeonPassResult;
import BreakthroughGame.backEnd.UserModule.UserRestful.request.DungeonPassRequest;
import BreakthroughGame.backEnd.UserModule.UserRestful.response.AllResponse;
import BreakthroughGame.backEnd.UserModule.UserRestful.response.DungeonPassResponse;
import BreakthroughGame.backEnd.UserModule.UserRestful.service.CharacterDungeonRunService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

/**
 * 中文备注：通关上报接口
 */
@Tag(name = "DungeonRun", description = "角色-关卡通过记录 上报接口")
@CrossOrigin(        origins = { "http://localhost:9000", "http://127.0.0.1:9000","http://127.0.0.1:5173","http://localhost:5173" },
         allowCredentials = "true", maxAge = 3600)
@RestController
@RequestMapping("/api/dungeonRun")
@RequiredArgsConstructor
@Validated
public class CharacterDungeonRunController {

    private static final Logger LOGGER = LoggerFactory.getLogger(CharacterDungeonRunController.class);

    private final CharacterDungeonRunService service;

    @Operation(
            summary = "上报通关结果（幂等）",
            description = "接受角色-关卡通过记录；同一 traceId 重复上报不重复计数；业务日以 Asia/Tokyo 计算。",
            responses = {
                    @ApiResponse(responseCode = "200", description = "OK",
                            content = @Content(schema = @Schema(implementation = DungeonPassResponse.class)))
            }
    )
    @PostMapping(value = "/pass")
    public AllResponse pass(@Valid @RequestBody DungeonPassRequest req) {
        String rid = UUID.randomUUID().toString().substring(0, 8);
        MDC.put("rid", rid);
        long startNs = System.nanoTime();
        try {
            DungeonPassResult result = service.recordPass(req);

            String msg = switch (result.getResultCode()) {
                case "OK" -> "记录成功，已计入当日次数";
                case "NO_TIMES" -> "记录成功，但今日次数已用尽，未计入";
                case "DUPLICATE" -> "重复上报，已返回现有结果";
                case "INVALID_DUNGEON" -> "副本不可用或不存在";
                default -> "处理完成";
            };
            return DungeonPassResponse.ok(msg, result);
        } catch (IllegalArgumentException ex) {
            LOGGER.warn("BIZ|RUN_PASS_BAD_REQ|rid={}|msg={}", rid, ex.getMessage(), ex);
            throw ex;
        } catch (Exception ex) {
            LOGGER.error("BIZ|RUN_PASS_ERR|rid={}|ex={}", rid, ex.toString(), ex);
            throw ex;
        } finally {
            long tookMs = (System.nanoTime() - startNs) / 1_000_000L;
            LOGGER.info("BIZ|RUN_PASS_DONE|rid={}|tookMs={}", rid, tookMs);
            MDC.remove("rid");
        }
    }
}
