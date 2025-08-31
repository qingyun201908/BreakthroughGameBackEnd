// 文件：BreakthroughGame/backEnd/UserModule/UserRestful/auth/CharacterEquipmentController.java
package BreakthroughGame.backEnd.UserModule.UserRestful.auth;

import BreakthroughGame.backEnd.DefinitionModule.entity.CharacterEquipment;
import BreakthroughGame.backEnd.DefinitionModule.entity.EquipmentSlot;
import BreakthroughGame.backEnd.UserModule.UserRestful.response.AllDataResponse; // ✅ 引入泛型基类
import BreakthroughGame.backEnd.UserModule.UserRestful.service.CharacterEquipmentService;
import io.swagger.v3.oas.annotations.Operation;
import jakarta.validation.constraints.NotNull;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.*;

@RestController
@RequestMapping("/api/character/equipment")
@RequiredArgsConstructor
@CrossOrigin(
        // 中文备注：按您现有网关/前端域名配置 CORS
        origins = { "http://localhost:9000", "http://127.0.0.1:9000","http://127.0.0.1:5173","http://localhost:5173" },
        allowCredentials = "true",
        maxAge = 3600
)
public class CharacterEquipmentController {

    private final CharacterEquipmentService service;

    @Operation(summary = "列出角色当前穿戴")
    @GetMapping("{characterId}")
    public AllDataResponse<List<CharacterEquipment>> list(@PathVariable UUID characterId){
        // 中文备注：AllResponse 不是泛型，这里改用 AllDataResponse
        return AllDataResponse.ok(service.list(characterId));  // ✅ 使用 AllDataResponse.ok
    }

    @Data
    public static class EquipRequest {
        @NotNull private String itemKey;   // 中文备注：要穿戴的装备 key（背包中需有）
        private String slotKey;            // 中文备注：可选；为空则用图鉴定义的 slot
    }

    @Operation(summary = "穿戴（若有旧装备会自动替换）")
    @PostMapping("{characterId}/equip")
    public AllDataResponse<CharacterEquipment> equip(@PathVariable UUID characterId,
                                                     @RequestBody EquipRequest req){
        EquipmentSlot slot = (req.getSlotKey() == null || req.getSlotKey().isBlank())
                ? null
                : EquipmentSlot.ofFrontKey(req.getSlotKey()); // 中文备注：用已有 ofFrontKey
        return AllDataResponse.ok(service.equip(characterId, req.getItemKey(), slot)); // ✅
    }

    @Operation(summary = "卸下某槽位装备")
    @PostMapping("{characterId}/unequip/{slotKey}")
    public AllDataResponse<Boolean> unequip(@PathVariable UUID characterId,
                                            @PathVariable String slotKey){
        EquipmentSlot slot = EquipmentSlot.ofFrontKey(slotKey);
        service.unequip(characterId, slot);
        return AllDataResponse.ok(true); // ✅
    }
}
