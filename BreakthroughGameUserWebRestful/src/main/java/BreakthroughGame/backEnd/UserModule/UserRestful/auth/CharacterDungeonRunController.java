// 文件：BreakthroughGame/backEnd/UserModule/UserRestful/auth/CharacterDungeonRunController.java
package BreakthroughGame.backEnd.UserModule.UserRestful.auth;

import BreakthroughGame.backEnd.UserModule.UserRestful.dto.DungeonPassResult;
import BreakthroughGame.backEnd.UserModule.UserRestful.dto.DungeonPassView;
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
            summary = "上报通关结果（含掉落详情与背包入账）",
            responses = {
                    @ApiResponse(responseCode = "200", description = "OK",
                            content = @Content(schema = @Schema(implementation = DungeonPassResponse.class)))
            }
    )
    @PostMapping("/pass")
    public DungeonPassResponse pass(@Valid @RequestBody DungeonPassRequest req) { // 中文备注：返回专用响应
        final String rid = java.util.UUID.randomUUID().toString();
        final long startNs = System.nanoTime();
        MDC.put("rid", rid);
        try {
            DungeonPassView view = service.passAndLoot(req);   // 中文备注：通关 + 掉落 + 入背包
            String msg = (view.getLoot() == null) ? "通关成功（无掉落配置）" : "通关成功，掉落已发放";
            return DungeonPassResponse.ok(msg, view);          // 中文备注：data=通关+掉落
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
