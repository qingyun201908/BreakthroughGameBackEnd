// 文件：BreakthroughGame/backEnd/UserModule/UserRestful/auth/CharacterBagController.java
package BreakthroughGame.backEnd.UserModule.UserRestful.auth;

import BreakthroughGame.backEnd.UserModule.UserRestful.dto.BagQuery;
import BreakthroughGame.backEnd.UserModule.UserRestful.response.BagSnapshotResponse;
import BreakthroughGame.backEnd.UserModule.UserRestful.service.CharacterBagService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Pattern;
import lombok.RequiredArgsConstructor;
import org.slf4j.MDC;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

/** 中文备注：人物背包接口（查询快照 + 示例增减） */
@Tag(name = "Bag", description = "人物背包接口")
@Validated
@RestController
@RequestMapping("/api/characters/{characterId}/bag")
@RequiredArgsConstructor
@CrossOrigin(
        // 中文备注：按您现有网关/前端域名配置 CORS
        origins = { "http://localhost:9000", "http://127.0.0.1:9000","http://127.0.0.1:5173","http://localhost:5173" },
        allowCredentials = "true",
        maxAge = 3600
)public class CharacterBagController {

    private final CharacterBagService bagService;

    @Operation(summary = "查询背包快照", description = "支持搜索(q)、类型(type)与排序(sort)")
    @ApiResponse(responseCode = "200", description = "OK",
            content = @Content(schema = @Schema(implementation = BagSnapshotResponse.class)))
    @GetMapping
    public BagSnapshotResponse getBag(
            @PathVariable("characterId") UUID characterId,
            @RequestParam(value = "q", required = false) String q,
            @RequestParam(value = "type", required = false)
            @Pattern(regexp = "all|consumable|equipment|material|quest|misc", message = "非法类型") String type,
            @RequestParam(value = "sort", required = false)
            @Pattern(regexp = "default|rarity|name|qty", message = "非法排序") String sort
    ) {
        MDC.put("rid", UUID.randomUUID().toString()); // 中文备注：示例：打业务链路日志
        BagQuery query = new BagQuery();
        query.setQ(q);
        // 中文备注：type=all 等价于不筛选
        query.setType("all".equalsIgnoreCase(type) ? null : type);
        query.setSort(sort == null ? "default" : sort);
        return BagSnapshotResponse.ok(bagService.getSnapshot(characterId, query));
    }

    /** 中文备注：示例增加物品（后续可接入掉落逻辑/GM 命令等） */
    @PostMapping("/add")
    public BagSnapshotResponse add(
            @PathVariable("characterId") UUID characterId,
            @RequestParam("itemKey") String itemKey,
            @RequestParam("name") String name,
            @RequestParam("type")
            @Pattern(regexp = "consumable|equipment|material|quest|misc", message = "非法类型") String type,
            @RequestParam("rarity") int rarity,
            @RequestParam(value = "desc", required = false) String desc,
            @RequestParam("qty") int qty
    ) {
        bagService.addItem(characterId, itemKey, name,
                BreakthroughGame.backEnd.DefinitionModule.entity.ItemType.fromNullable(type),
                rarity, desc, qty);
        return BagSnapshotResponse.ok("OK");
    }

    /** 中文备注：示例扣减物品（使用/丢弃皆可） */
    @PostMapping("/remove")
    public BagSnapshotResponse remove(
            @PathVariable("characterId") UUID characterId,
            @RequestParam("itemKey") String itemKey,
            @RequestParam("qty") int qty
    ) {
        bagService.removeItem(characterId, itemKey, qty);
        return BagSnapshotResponse.ok("OK");
    }
}
