package BreakthroughGame.backEnd.UserModule.UserRestful.auth;// 文件：BreakthroughGame/backEnd/DefinitionModule/rest/EquipmentCatalogController.java

import BreakthroughGame.backEnd.UserModule.UserRestful.dto.EquipmentItemVO;
import BreakthroughGame.backEnd.UserModule.UserRestful.response.AllResponse;
import BreakthroughGame.backEnd.UserModule.UserRestful.response.EquipmentDetailResponse;
import BreakthroughGame.backEnd.UserModule.UserRestful.response.EquipmentSearchResponse;
import BreakthroughGame.backEnd.UserModule.UserRestful.service.EquipmentCatalogService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.constraints.Min;
import lombok.RequiredArgsConstructor;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/** 中文备注：装备图鉴 REST */
@Tag(name = "EquipmentCatalog", description = "装备图鉴查询")
@Validated
@RestController
@RequestMapping("/api/definition/equipment")
@RequiredArgsConstructor
@CrossOrigin(
        origins = { "http://localhost:9000", "http://127.0.0.1:9000","http://127.0.0.1:5173","http://localhost:5173" },
        maxAge = 3600,
        methods = { RequestMethod.POST, RequestMethod.GET, RequestMethod.OPTIONS }
)
public class EquipmentCatalogController {

    private final EquipmentCatalogService service;

    @Operation(summary = "分页查询装备（按 slot/rarity/tag/dungeon/keyword 过滤）")
    @GetMapping("/search")
    public EquipmentSearchResponse search(
            @RequestParam(required = false) String slot,          // 中文备注：例 "cloak" 或 "CLOAK"
            @RequestParam(required = false) String rarity,        // 中文备注：例 "RARE" 或 "#3b82f6"
            @RequestParam(required = false) String tag,           // 中文备注：标签纯文本
            @RequestParam(required = false) String dungeon,       // 中文备注：来源副本 key
            @RequestParam(required = false) String keyword,       // 中文备注：模糊匹配 name/equipKey
            @RequestParam(defaultValue = "1") @Min(1) int page,   // 中文备注：从 1 开始
            @RequestParam(defaultValue = "20") @Min(1) int size,
            @RequestParam(defaultValue = "true") boolean includeTags,
            @RequestParam(defaultValue = "true") boolean includeDungeons
    ) {
        List<EquipmentItemVO> items = service.search(
                slot, rarity, tag, dungeon, keyword, page, size, includeTags, includeDungeons
        );
        // 中文备注：总数以过滤后 size 估算；若需精确总数，请在 Service 返回 total
        long total = (long) ((page - 1) * size + items.size());
        EquipmentSearchResponse resp = EquipmentSearchResponse.ok(items, total, page, size);
        resp.setSuccess(true);
        resp.setMessage("OK");
        return resp;
    }

    @Operation(summary = "装备详情（按 equipKey）")
    @GetMapping("/{equipKey}")
    public EquipmentDetailResponse detail(
            @PathVariable String equipKey,
            @RequestParam(defaultValue = "true") boolean includeTags,
            @RequestParam(defaultValue = "true") boolean includeDungeons
    ) {
        EquipmentItemVO vo = service.detail(equipKey, includeTags, includeDungeons);
        EquipmentDetailResponse resp = EquipmentDetailResponse.ok(vo);
        resp.setSuccess(true);
        resp.setMessage("OK");
        return resp;
    }
    @Operation(summary = "健康检查")
    /** 中文备注：健康检查/占位接口，可用于验证统一响应体 */
    @GetMapping("/ping")
    public AllResponse ping() {
        AllResponse r = new AllResponse();
        r.setSuccess(true);
        r.setMessage("pong");
        return r;
    }
}
