package BreakthroughGame.backEnd.UserModule.UserRestful.service;// 文件：BreakthroughGame/backEnd/UserModule/UserInfo/service/CharacterCombatAttributesService.java
// 中文备注：提供按角色获取/初始化的服务层封装


import BreakthroughGame.backEnd.UserModule.UserInfo.entity.CharacterCombatAttributes;
import BreakthroughGame.backEnd.UserModule.UserInfo.repository.CharacterCombatAttributesRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Service
@RequiredArgsConstructor
public class CharacterCombatAttributesService {

    private final CharacterCombatAttributesRepository repo;

    /**
     * 中文备注：获取角色战斗属性；若不存在则“初始化一行默认记录并返回”
     */
    @Transactional
    public CharacterCombatAttributes getOrInit(UUID characterId) {
        return repo.findByCharacterId(characterId)
                .orElseGet(() -> repo.save(CharacterCombatAttributes.initFor(characterId)));
    }

    /**
     * 中文备注：保存（带乐观锁 version）；外层可捕获 OptimisticLockingFailureException
     */
    @Transactional
    public CharacterCombatAttributes save(CharacterCombatAttributes attrs) {
        return repo.save(attrs);
    }
}
