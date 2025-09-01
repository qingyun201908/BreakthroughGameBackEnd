// 文件：BreakthroughGame/backEnd/UserModule/UserRestful/service/CharacterAttributeRecalcService.java
// 中文备注：根据角色基础属性 + 当前已穿戴装备的属性加成，重算并回写独立表 character_combat_attributes

package BreakthroughGame.backEnd.UserModule.UserRestful.service;

import BreakthroughGame.backEnd.UserModule.UserInfo.entity.CombatAttributes;
import BreakthroughGame.backEnd.UserModule.UserInfo.entity.GameCharacter;
import BreakthroughGame.backEnd.UserModule.UserInfo.entity.CharacterCombatAttributes;
import BreakthroughGame.backEnd.UserModule.UserInfo.repository.GameCharacterRepository;
import BreakthroughGame.backEnd.UserModule.UserInfo.repository.CharacterCombatAttributesRepository;

import BreakthroughGame.backEnd.DefinitionModule.entity.EquipmentDefinition;
import BreakthroughGame.backEnd.DefinitionModule.entity.CharacterEquipment;
import BreakthroughGame.backEnd.DefinitionModule.repository.EquipmentDefinitionRepository;
import BreakthroughGame.backEnd.DefinitionModule.repository.CharacterEquipmentRepository;

import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class CharacterAttributeRecalcService {

    private static final Logger log = LoggerFactory.getLogger(CharacterAttributeRecalcService.class);

    private final GameCharacterRepository characterRepo;
    private final CharacterEquipmentRepository characterEquipmentRepo;
    private final EquipmentDefinitionRepository equipDefRepo;
    private final CharacterCombatAttributesRepository ccattrsRepo;

    /**
     * 中文备注：重算角色面板并写回独立表；若独立表无记录则初始化插入
     */
    @Transactional
    public CharacterCombatAttributes recalcAndSave(UUID characterId) {
        // 1) 读取角色基础属性
        GameCharacter ch = characterRepo.findById(characterId)
                .orElseThrow(() -> new IllegalArgumentException("角色不存在: " + characterId));

        CombatAttributes base = Optional.ofNullable(ch.getAttributes())
                .orElseGet(this::defaultBaseAttributes);

        // 2) 读取当前已穿戴装备清单
        List<CharacterEquipment> equipped = characterEquipmentRepo.findByCharacterId(characterId);

        // 3) 汇总“装备加成属性”
        CombatAttributes equipBonus = sumEquipAttributes(equipped);

        // 4) 计算总面板 = 基础 + 装备
        CombatAttributes total = addAttributes(base, equipBonus);
        clampPercent(total); // 可选：对百分比字段做范围裁剪，防止异常

        // 5) 写回/初始化独立表
        CharacterCombatAttributes row = ccattrsRepo.findByCharacterId(characterId)
                .orElseGet(() -> CharacterCombatAttributes.initFor(characterId));

        copy(total, row);
        try {
            return ccattrsRepo.save(row);
        } catch (DataIntegrityViolationException e) {
            // 并发兜底：唯一约束撞车则回查返回
            log.warn("保存独立属性并发冲突，回查已有记录: characterId={}", characterId);
            CharacterCombatAttributes existing = ccattrsRepo.findByCharacterId(characterId)
                    .orElseThrow(() -> e);
            copy(total, existing);
            return ccattrsRepo.save(existing);
        }
    }

    // ===================== 属性求和/转换 =====================

    /**
     * 中文备注：对全部已穿戴装备求“属性加成总和”
     */
    private CombatAttributes sumEquipAttributes(List<CharacterEquipment> list) {
        CombatAttributes sum = zeroAttributes();
        if (list == null || list.isEmpty()) return sum;

        for (CharacterEquipment ce : list) {
            if (ce == null || ce.getItemKey() == null) continue;

            // 兼容两种查询：按 equip_key（推荐）/或主键
            EquipmentDefinition def = equipDefRepo.findByEquipKey(ce.getItemKey())
                    .orElseGet(() -> equipDefRepo.findById(ce.getId()).orElse(null));
            if (def == null) {
                log.warn("未找到装备图鉴定义，忽略：itemKey={}", ce.getItemKey());
                continue;
            }

            CombatAttributes bonus = resolveAttributesFromDefinition(def);
            addInPlace(sum, bonus);
        }
        return sum;
    }

    /**
     * 中文备注：从装备图鉴解析其“属性加成”。你可以：
     *  1) 若图鉴内有内嵌 attrs（CombatAttributes），直接返回；
     *  2) 若图鉴为离散字段（如 attackBonus 等），在此聚合为 CombatAttributes；
     *  3) 若图鉴以 Tag/词条形式存在，在此做映射转换。
     */
    private CombatAttributes resolveAttributesFromDefinition(EquipmentDefinition def) {
        // 情况1：图鉴直接内嵌属性
        try {
            var method = def.getClass().getMethod("getAttributes"); // 反射兼容：避免编译期强耦合
            Object obj = method.invoke(def);
            if (obj instanceof CombatAttributes ca) {
                return ca;
            }
        } catch (Exception ignore) { }

        // 情况2：离散字段 → 聚合为 CombatAttributes（按你的字段名自行替换）
        CombatAttributes ca = zeroAttributes();
        try {
            ca.setAttack(readInt(def, "getAttack", 0));                    // 如：def.getAttack()
            ca.setAttackSpeedPercent(readInt(def, "getAttackSpeedPercent", 0));
            ca.setCritRatePercent(readInt(def, "getCritRatePercent", 0));
            ca.setCritDamagePercent(readInt(def, "getCritDamagePercent", 0));
            ca.setHitPercent(readInt(def, "getHitPercent", 0));
            ca.setPenetration(readInt(def, "getPenetration", 0));
            ca.setMetal(readInt(def, "getMetal", 0));
            ca.setWood(readInt(def, "getWood", 0));
            ca.setWater(readInt(def, "getWater", 0));
            ca.setFire(readInt(def, "getFire", 0));
            ca.setEarth(readInt(def, "getEarth", 0));
            ca.setChaos(readInt(def, "getChaos", 0));
        } catch (Exception e) {
            // 情况3：通过 Tag/词条映射（如有需要可在此实现：根据 def.getTags() → 属性）
            log.debug("图鉴属性采用默认零值，原因：未检测到可解析字段/内嵌属性。def={}", def.getClass().getSimpleName());
        }
        return ca;
    }

    // ===================== 小工具：属性运算/安全裁剪/复制 =====================

    private CombatAttributes zeroAttributes() { return new CombatAttributes(); }

    private CombatAttributes defaultBaseAttributes() {
        CombatAttributes a = new CombatAttributes();
        a.setAttack(0);
        a.setAttackSpeedPercent(100);
        a.setCritRatePercent(0);
        a.setCritDamagePercent(150);
        a.setHitPercent(100);
        a.setPenetration(0);
        a.setMetal(0); a.setWood(0); a.setWater(0); a.setFire(0); a.setEarth(0); a.setChaos(0);
        return a;
    }

    // 中文备注：total = a + b（返回新对象）
    private CombatAttributes addAttributes(CombatAttributes a, CombatAttributes b) {
        CombatAttributes r = new CombatAttributes();
        r.setAttack(s(a.getAttack()) + s(b.getAttack()));
        r.setAttackSpeedPercent(s(a.getAttackSpeedPercent()) + s(b.getAttackSpeedPercent()));
        r.setCritRatePercent(s(a.getCritRatePercent()) + s(b.getCritRatePercent()));
        r.setCritDamagePercent(s(a.getCritDamagePercent()) + s(b.getCritDamagePercent()));
        r.setHitPercent(s(a.getHitPercent()) + s(b.getHitPercent()));
        r.setPenetration(s(a.getPenetration()) + s(b.getPenetration()));

        r.setMetal(s(a.getMetal()) + s(b.getMetal()));
        r.setWood(s(a.getWood()) + s(b.getWood()));
        r.setWater(s(a.getWater()) + s(b.getWater()));
        r.setFire(s(a.getFire()) + s(b.getFire()));
        r.setEarth(s(a.getEarth()) + s(b.getEarth()));
        r.setChaos(s(a.getChaos()) + s(b.getChaos()));
        return r;
    }

    // 中文备注：sum += add
    private void addInPlace(CombatAttributes sum, CombatAttributes add) {
        if (sum == null || add == null) return;
        sum.setAttack(s(sum.getAttack()) + s(add.getAttack()));
        sum.setAttackSpeedPercent(s(sum.getAttackSpeedPercent()) + s(add.getAttackSpeedPercent()));
        sum.setCritRatePercent(s(sum.getCritRatePercent()) + s(add.getCritRatePercent()));
        sum.setCritDamagePercent(s(sum.getCritDamagePercent()) + s(add.getCritDamagePercent()));
        sum.setHitPercent(s(sum.getHitPercent()) + s(add.getHitPercent()));
        sum.setPenetration(s(sum.getPenetration()) + s(add.getPenetration()));

        sum.setMetal(s(sum.getMetal()) + s(add.getMetal()));
        sum.setWood(s(sum.getWood()) + s(add.getWood()));
        sum.setWater(s(sum.getWater()) + s(add.getWater()));
        sum.setFire(s(sum.getFire()) + s(add.getFire()));
        sum.setEarth(s(sum.getEarth()) + s(add.getEarth()));
        sum.setChaos(s(sum.getChaos()) + s(add.getChaos()));
    }

    // 中文备注：对百分比做简单裁剪（0%~500%，可按策划调整）
    private void clampPercent(CombatAttributes r) {
        if (r == null) return;
        r.setAttackSpeedPercent(clamp(r.getAttackSpeedPercent(), 0, 500));
        r.setCritRatePercent(clamp(r.getCritRatePercent(), 0, 500));
        r.setCritDamagePercent(clamp(r.getCritDamagePercent(), 0, 500));
        r.setHitPercent(clamp(r.getHitPercent(), 0, 500));
        r.setAttack(Math.max(0, s(r.getAttack())));
        r.setPenetration(Math.max(0, s(r.getPenetration())));
    }

    // 中文备注：将 CombatAttributes 的值复制到独立表实体
    private void copy(CombatAttributes src, CharacterCombatAttributes dst) {
        dst.setAttack(s(src.getAttack()));
        dst.setAttackSpeedPercent(s(src.getAttackSpeedPercent()));
        dst.setCritRatePercent(s(src.getCritRatePercent()));
        dst.setCritDamagePercent(s(src.getCritDamagePercent()));
        dst.setHitPercent(s(src.getHitPercent()));
        dst.setPenetration(s(src.getPenetration()));
        dst.setMetal(s(src.getMetal()));
        dst.setWood(s(src.getWood()));
        dst.setWater(s(src.getWater()));
        dst.setFire(s(src.getFire()));
        dst.setEarth(s(src.getEarth()));
        dst.setChaos(s(src.getChaos()));
    }

    private int s(Integer v) { return v == null ? 0 : v; }
    private int clamp(Integer v, int min, int max) {
        int x = s(v);
        return Math.min(max, Math.max(min, x));
    }

    // 反射读取 int 值（若无该方法或抛错则给默认值）
    private int readInt(Object obj, String getter, int def) {
        try { return (int) obj.getClass().getMethod(getter).invoke(obj); }
        catch (Throwable t) { return def; }
    }
}
