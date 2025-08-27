package BreakthroughGame.backEnd.UserModule.UserRestful.dto;// 文件：BreakthroughGame/backEnd/UserModule/UserRestful/mapper/CharacterDailyMapper.java


import BreakthroughGame.backEnd.WarcraftDungeonsInfo.entity.CharacterDungeonDaily;
import BreakthroughGame.backEnd.UserModule.UserRestful.dto.CharacterDailyVO;

/** 中文备注：简单的手工映射，避免直接暴露实体 */
public final class CharacterDailyMapper {

    private CharacterDailyMapper() {}

    // 中文备注：实体 -> VO
    public static CharacterDailyVO toVO(CharacterDungeonDaily e) {
        CharacterDailyVO vo = new CharacterDailyVO();
        vo.setId(e.getId());
        vo.setCharacterId(e.getCharacterId());
        vo.setDungeonKey(e.getDungeonKey());
        vo.setDayKey(e.getDayKey());
        vo.setRunsUsed(e.getRunsUsed());
        vo.setRunsMax(e.getRunsMaxSnapshot());
        vo.setSelected(e.getLastSelectedDifficulty());
        vo.setCanChallenge(e.isCanChallenge());     // 中文备注：调用实体的 @Transient 逻辑
        vo.setAllowOverride(e.isAllowOverride());
        vo.setNotes(e.getNotes());
        vo.setUpdatedAt(e.getUpdatedAt());
        return vo;
    }
}
