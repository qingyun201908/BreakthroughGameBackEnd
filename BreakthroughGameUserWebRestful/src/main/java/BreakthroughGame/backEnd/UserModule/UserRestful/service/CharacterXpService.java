// 文件：BreakthroughGame/backEnd/UserModule/UserRestful/service/CharacterXpService.java
package BreakthroughGame.backEnd.UserModule.UserRestful.service;

import BreakthroughGame.backEnd.UserModule.UserInfo.repository.CharacterXpRepository;
import BreakthroughGame.backEnd.UserModule.UserRestful.common.api.ResultCode;
import BreakthroughGame.backEnd.UserModule.UserRestful.common.exception.BizException;
import BreakthroughGame.backEnd.UserModule.UserRestful.dto.LevelSnapshot;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Service
public class CharacterXpService {

    private final CharacterXpRepository repo;

    public CharacterXpService(CharacterXpRepository repo) {
        this.repo = repo;
    }

    /** 原子：确保存在→加经验→再查一次快照返回 */
    @Transactional
    public LevelSnapshot addXp(UUID characterId, long delta) {
        if (characterId == null) {
            throw new BizException(ResultCode.A_BAD_REQUEST, "角色ID不能为空");
        }
        if (delta < 0) {
            throw new BizException(ResultCode.A_BAD_REQUEST, "经验增量不能为负");
        }

        // 1) 确保有一条经验记录（不存在则插入 total_xp=0）
        repo.ensureExists(characterId);

        // 2) 增量更新（触发器会刷新冗余列）
        if (delta > 0) repo.addDelta(characterId, delta);

        // 3) 再查一次，拿到最新快照（同事务下读取到的是最新）
        return repo.findByCharacterId(characterId)
                .map(x -> new LevelSnapshot(
                        x.getTotalXp(), x.getCurrentLevel(), x.getProgress(),
                        x.getXpIntoLevel(), x.getXpToNextLevel(),
                        x.getLevelStartTotalXp(), x.getNextLevelTotalXp(),
                        x.isIsMaxLevel()
                ))
                .orElseThrow(() -> new BizException(ResultCode.A_CONFLICT, "经验记录不存在或已删除"));
    }

    /** 查询快照：无记录返回默认值（保持你原有语义） */
    @Transactional(readOnly = true)
    public LevelSnapshot snapshot(UUID characterId) {
        if (characterId == null) {
            throw new BizException(ResultCode.A_BAD_REQUEST, "角色ID不能为空");
        }
        return repo.findByCharacterId(characterId)
                .map(x -> new LevelSnapshot(
                        x.getTotalXp(), x.getCurrentLevel(), x.getProgress(),
                        x.getXpIntoLevel(), x.getXpToNextLevel(),
                        x.getLevelStartTotalXp(), x.getNextLevelTotalXp(),
                        x.isIsMaxLevel()
                ))
                .orElseGet(() -> new LevelSnapshot(0, 1, 0.0, 0, 0, 0, 0, false));
    }

    /** 注册流程用：只保证存在 +（可选）初始经验，不关心返回值 */
    @Transactional
    public void initXpForRegistration(UUID characterId, long initialXp) {
        if (characterId == null) {
            throw new BizException(ResultCode.A_BAD_REQUEST, "角色ID不能为空");
        }
        repo.ensureExists(characterId);
        if (initialXp > 0) {
            repo.addDelta(characterId, initialXp);
        }
    }
}
