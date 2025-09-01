// 文件：BreakthroughGame/backEnd/UserModule/UserRestful/service/CharacterEquipmentService.java
package BreakthroughGame.backEnd.UserModule.UserRestful.service;

import BreakthroughGame.backEnd.DefinitionModule.entity.*;
import BreakthroughGame.backEnd.DefinitionModule.repository.CharacterBagItemRepository;
import BreakthroughGame.backEnd.DefinitionModule.repository.CharacterEquipmentHistoryRepository;
import BreakthroughGame.backEnd.DefinitionModule.repository.CharacterEquipmentRepository;
import BreakthroughGame.backEnd.DefinitionModule.repository.EquipmentDefinitionRepository;

import BreakthroughGame.backEnd.UserModule.UserRestful.common.exception.BizException;   // 中文备注：你的通用业务异常
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.slf4j.MDC;
import org.springframework.stereotype.Service;

import java.util.*;

import static BreakthroughGame.backEnd.UserModule.UserRestful.common.api.ResultCode.B_BIZ_ERROR;

@Service
@RequiredArgsConstructor
public class CharacterEquipmentService {

    private final CharacterEquipmentRepository equipRepo;
    private final CharacterEquipmentHistoryRepository hisRepo;
    private final CharacterBagItemRepository bagRepo;
    private final EquipmentDefinitionRepository defRepo;
    private final CharacterAttributeRecalcService attrRecalcService; // ✅ 注入重算服务


    /** 中文备注：列出角色当前穿戴（按槽位） */
    public List<CharacterEquipment> list(UUID characterId){
        return equipRepo.findByCharacterId(characterId);
    }

    /**
     * 中文备注：穿戴装备（若槽位已有装备则自动 SWAP）
     * @param characterId 角色 UUID
     * @param itemKey     要穿的装备图鉴 key（对应背包中的同 key）
     * @param slotOpt     槽位（可空；为空则以图鉴定义为准）
     */
    @Transactional
    public CharacterEquipment equip(UUID characterId, String itemKey, EquipmentSlot slotOpt){
        final String traceId = MDC.get("rid");

        // 1) 校验：背包是否有该物品，且类型为“装备”，qty >= 1
        CharacterBagItem bag = bagRepo.findByCharacterIdAndItemKey(characterId, itemKey)
                .orElseThrow(() -> new BizException(B_BIZ_ERROR,"背包中不存在该物品：" + itemKey));
        if (bag.getQty() <= 0) throw new BizException(B_BIZ_ERROR,"该物品数量不足：" + itemKey);
        if (bag.getType() != ItemType.equipment) throw new BizException(B_BIZ_ERROR,"该物品不是可穿戴装备：" + itemKey);

        // 2) 读取图鉴，确定槽位与展示字段（无外键，弱关联）
        EquipmentDefinition def = defRepo.findByEquipKey(itemKey)
                .orElseThrow(() -> new BizException(B_BIZ_ERROR,"未找到图鉴定义：" + itemKey));
        EquipmentSlot slot = (slotOpt != null) ? slotOpt : def.getSlot();

        // 3) 若该槽位已有装备：先回退旧装备到背包（+1），再占位
        Optional<CharacterEquipment> oldOpt = equipRepo.findByCharacterIdAndSlot(characterId, slot);
        if (oldOpt.isPresent()){
            CharacterEquipment old = oldOpt.get();
            // 背包 +1（若没有对应栈，可在业务层“找不到则创建栈”，此处为简化直接尝试找）
            bagRepo.findByCharacterIdAndItemKey(characterId, old.getItemKey())
                    .ifPresentOrElse(
                            stack -> bagRepo.addQty(stack.getId(), +1),
                            () -> {
                                // 若缺少旧物品栈，这里可以自动建一条 CharacterBagItem（仅值/弱关联）
                                CharacterBagItem newStack = new CharacterBagItem();
                                newStack.setId(UUID.randomUUID());
                                newStack.setCharacterId(characterId);
                                newStack.setItemKey(old.getItemKey());
                                newStack.setName(old.getName());
                                newStack.setType(ItemType.equipment);
                                newStack.setRarity(old.getRarity());
                                newStack.setQty(1);
                                newStack.setDesc(old.getDescription());
                                bagRepo.save(newStack);
                            }
                    );

            // 写历史：SWAP
            CharacterEquipmentHistory h = new CharacterEquipmentHistory();
            h.setCharacterId(characterId);
            h.setSlot(slot);
            h.setOldItemKey(old.getItemKey());
            h.setNewItemKey(itemKey);
            h.setReason("SWAP");
            h.setTraceId(traceId);
            hisRepo.save(h);
        }else{
            // 写历史：EQUIP
            CharacterEquipmentHistory h = new CharacterEquipmentHistory();
            h.setCharacterId(characterId);
            h.setSlot(slot);
            h.setOldItemKey(null);
            h.setNewItemKey(itemKey);
            h.setReason("EQUIP");
            h.setTraceId(traceId);
            hisRepo.save(h);
        }

        // 4) 背包数量 -1（使用弱关联，配合 @Version 乐观锁避免并发丢失）
        bagRepo.addQty(bag.getId(), -1);

        // 5) upsert 到穿戴表（无外键，仅值复制冗余）
        CharacterEquipment row = equipRepo.findByCharacterIdAndSlot(characterId, slot).orElseGet(() -> {
            CharacterEquipment r = new CharacterEquipment();
            r.setId(UUID.randomUUID());
            r.setCharacterId(characterId);
            r.setSlot(slot);
            return r;
        });
        row.setItemKey(itemKey);
        row.setName(def.getName());
        row.setRarity(def.getRarity().ordinal() + 1); // 中文备注：若你定义是枚举，可自行换算为 1~6
        row.setIcon(def.getIcon());                   // 中文备注：图鉴里若有 icon 字段
        row.setDescription(def.getDescription());     // 中文备注：图鉴里若有 description 字段
        row.setBagItemId(bag.getId());
        equipRepo.save(row);
        attrRecalcService.recalcAndSave(characterId);

        return row;
    }

    /** 中文备注：卸下（若槽位为空则幂等） */
    @Transactional
    public void unequip(UUID characterId, EquipmentSlot slot){
        final String traceId = MDC.get("rid");
        Optional<CharacterEquipment> oldOpt = equipRepo.findByCharacterIdAndSlot(characterId, slot);
        if (oldOpt.isEmpty()) return; // 幂等

        CharacterEquipment old = oldOpt.get();

        // 背包 +1（如上）
        bagRepo.findByCharacterIdAndItemKey(characterId, old.getItemKey())
                .ifPresentOrElse(
                        stack -> bagRepo.addQty(stack.getId(), +1),
                        () -> {
                            CharacterBagItem newStack = new CharacterBagItem();
                            newStack.setId(UUID.randomUUID());
                            newStack.setCharacterId(characterId);
                            newStack.setItemKey(old.getItemKey());
                            newStack.setName(old.getName());
                            newStack.setType(ItemType.equipment);
                            newStack.setRarity(old.getRarity());
                            newStack.setQty(1);
                            newStack.setDesc(old.getDescription());
                            bagRepo.save(newStack);
                        }
                );

        // 删除穿戴记录
        equipRepo.delete(old);
        attrRecalcService.recalcAndSave(characterId);

        // 历史
        CharacterEquipmentHistory h = new CharacterEquipmentHistory();
        h.setCharacterId(characterId);
        h.setSlot(slot);
        h.setOldItemKey(old.getItemKey());
        h.setNewItemKey(null);
        h.setReason("UNEQUIP");
        h.setTraceId(traceId);
        hisRepo.save(h);
    }
}
