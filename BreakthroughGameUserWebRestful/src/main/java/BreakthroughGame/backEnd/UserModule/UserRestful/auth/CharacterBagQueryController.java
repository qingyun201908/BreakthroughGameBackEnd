// 文件：BreakthroughGame/backEnd/UserModule/UserRestful/auth/CharacterBagQueryController.java
package BreakthroughGame.backEnd.UserModule.UserRestful.auth;

import BreakthroughGame.backEnd.UserModule.UserRestful.dto.EquipableItemDto;
import BreakthroughGame.backEnd.UserModule.UserRestful.response.AllDataResponse;
import BreakthroughGame.backEnd.UserModule.UserRestful.response.AllResponse;
import BreakthroughGame.backEnd.UserModule.UserRestful.service.CharacterBagQueryService;
import io.swagger.v3.oas.annotations.Operation;
import jakarta.validation.constraints.NotNull;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/api/character")
@RequiredArgsConstructor
@CrossOrigin(
        // 中文备注：按您现有网关/前端域名配置 CORS
        origins = { "http://localhost:9000", "http://127.0.0.1:9000","http://127.0.0.1:5173","http://localhost:5173" },
        allowCredentials = "true",
        maxAge = 3600
)
public class CharacterBagQueryController {

    private final CharacterBagQueryService service;

    @Operation(summary = "列出角色可穿戴装备（背包）")
    @GetMapping("/{characterId}/bag/equipables")
    public AllDataResponse<Page<EquipableItemDto>> list(
            @PathVariable @NotNull UUID characterId,
            @RequestParam(required = false) String slot,  // 中文备注：cloak/glove/...
            @RequestParam(required = false) String q,     // 中文备注：名称搜索
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size
    ) {
        return AllDataResponse.ok(service.listEquipables(characterId, slot, q, page, size));
    }
}
