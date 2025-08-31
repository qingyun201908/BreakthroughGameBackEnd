// 文件：BreakthroughGame/backEnd/UserModule/UserRestful/service/CharacterBagQueryService.java
package BreakthroughGame.backEnd.UserModule.UserRestful.service;

import BreakthroughGame.backEnd.UserModule.UserRestful.common.exception.BizException;
import BreakthroughGame.backEnd.UserModule.UserRestful.dto.EquipableItemDto;
import BreakthroughGame.backEnd.UserModule.UserRestful.repository.BagEquipableQueryRepository;
import java.util.*;
import org.springframework.data.domain.*;
import org.springframework.stereotype.Service;

import static BreakthroughGame.backEnd.UserModule.UserRestful.common.api.ResultCode.VALIDATE_FAILED;

@Service
public class CharacterBagQueryService {

    private static final Set<String> SLOT_WHITELIST = Set.of(
            "cloak","wrist","glove","armor","belt","pants","shoes","ring","necklace","shoulder"
    );

    private final BagEquipableQueryRepository repo;

    public CharacterBagQueryService(BagEquipableQueryRepository repo) { this.repo = repo; }

    public Page<EquipableItemDto> listEquipables(UUID characterId, String slot, String q, int page, int size) {
        if (slot != null && !slot.isBlank() && !SLOT_WHITELIST.contains(slot)) {
            throw new BizException(VALIDATE_FAILED,"非法槽位：" + slot);
        }
        Pageable pageable = PageRequest.of(Math.max(0, page), Math.min(Math.max(1, size), 100));
        var result = repo.findEquipables(characterId, (slot == null || slot.isBlank()) ? null : slot, q, pageable);

        // 兜底颜色（当 ed.color_hex 为空或异常时）
        result.forEach(it -> {
            if (it.getColor() == null || it.getColor().isBlank()) {
                it.setColor("#9ca3af");
            }
        });
        return result;
    }
}
