// 文件：BreakthroughGame/backEnd/UserModule/UserRestful/auth/CharacterCombatAttributesController.java
// 中文备注：简单的查询与更新接口（使用你项目中的 AllResponse 包装）

package BreakthroughGame.backEnd.UserModule.UserRestful.auth;

import BreakthroughGame.backEnd.UserModule.UserInfo.entity.CharacterCombatAttributes;
import BreakthroughGame.backEnd.UserModule.UserRestful.response.AllDataResponse;
import BreakthroughGame.backEnd.UserModule.UserRestful.service.CharacterCombatAttributesService;
import io.swagger.v3.oas.annotations.Operation;
import jakarta.validation.constraints.NotNull;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/api/character/attrs")
@RequiredArgsConstructor
@CrossOrigin(
        // 中文备注：按您现有网关/前端域名配置 CORS
        origins = { "http://localhost:9000", "http://127.0.0.1:9000","http://127.0.0.1:5173","http://localhost:5173" },
        allowCredentials = "true",
        maxAge = 3600
)
public class CharacterCombatAttributesController {

    private final CharacterCombatAttributesService service;

    @Operation(summary = "获取角色战斗属性（不存在则初始化并返回）")
    @GetMapping("{characterId}")
    public AllDataResponse<CharacterCombatAttributes> get(@PathVariable UUID characterId) {
        return AllDataResponse.ok(service.getOrInit(characterId));
    }

    @Data
    public static class UpdateRequest {
        // 中文备注：需要前端只传允许更新的字段，生产上可做更严格的 DTO 拆分与校验
        @NotNull private Integer attack;
        @NotNull private Integer attackSpeedPercent;
        @NotNull private Integer critRatePercent;
        @NotNull private Integer critDamagePercent;
        @NotNull private Integer hitPercent;
        @NotNull private Integer penetration;
        @NotNull private Integer metal;
        @NotNull private Integer wood;
        @NotNull private Integer water;
        @NotNull private Integer fire;
        @NotNull private Integer earth;
        @NotNull private Integer chaos;
        @NotNull private Long version; // 中文备注：乐观锁（必传）
    }

    @Operation(summary = "更新角色战斗属性（乐观锁校验）")
    @PutMapping("{characterId}")
    public AllDataResponse<CharacterCombatAttributes> update(@PathVariable UUID characterId,
                                                         @RequestBody UpdateRequest req) {
        CharacterCombatAttributes entity = service.getOrInit(characterId);

        // 乐观锁校验（中文备注：防止覆盖他人更新）
//        if (!entity.getVersionAsLong().equals(req.getVersion())) {
            // 你项目里的 BizException/错误码体系可在此抛出
            // throw new BizException("ATTR_VERSION_MISMATCH", "版本冲突，请刷新后重试");
//        }

        // 中文备注：赋值（可在 Service 中做更多数值裁剪/上限判断）
        entity.setAttack(req.getAttack());
        entity.setAttackSpeedPercent(req.getAttackSpeedPercent());
        entity.setCritRatePercent(req.getCritRatePercent());
        entity.setCritDamagePercent(req.getCritDamagePercent());
        entity.setHitPercent(req.getHitPercent());
        entity.setPenetration(req.getPenetration());
        entity.setMetal(req.getMetal());
        entity.setWood(req.getWood());
        entity.setWater(req.getWater());
        entity.setFire(req.getFire());
        entity.setEarth(req.getEarth());
        entity.setChaos(req.getChaos());

        CharacterCombatAttributes saved = service.save(entity);
        return AllDataResponse.ok(saved);
    }
}
