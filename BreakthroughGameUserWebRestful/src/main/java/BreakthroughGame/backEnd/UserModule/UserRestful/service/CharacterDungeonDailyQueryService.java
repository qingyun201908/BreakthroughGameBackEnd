// 文件：BreakthroughGame/backEnd/UserModule/UserRestful/service/CharacterDungeonDailyQueryService.java
package BreakthroughGame.backEnd.UserModule.UserRestful.service;

import BreakthroughGame.backEnd.UserModule.UserRestful.dto.CharacterDailyMapper;  // 中文备注：按你当前包路径保留
import BreakthroughGame.backEnd.UserModule.UserRestful.dto.CharacterDailyVO;
import BreakthroughGame.backEnd.UserModule.UserRestful.dto.PageResult;
import BreakthroughGame.backEnd.WarcraftDungeonsInfo.entity.CharacterDungeonDaily;
import BreakthroughGame.backEnd.WarcraftDungeonsInfo.repository.CharacterDungeonDailyRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.*;
import org.springframework.stereotype.Service;

import java.time.*;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * 中文备注：角色-副本当日计数 查询服务
 * - 提供“今日(JST)列表”
 * - 提供“单条详情”
 * - 提供“分页查询”
 */
@Service
@RequiredArgsConstructor
public class CharacterDungeonDailyQueryService {

    private final CharacterDungeonDailyRepository repository;

    /** 中文备注：统一时区（与实体注释保持一致，按 Asia/Tokyo 计算 dayKey） */
    public static final ZoneId ZONE_JST = ZoneId.of("Asia/Tokyo");

    /** 中文备注：获取“今日(JST)所有副本”的计数列表 */
    public List<CharacterDailyVO> listToday(UUID characterId) {
        LocalDate dayKey = LocalDate.now(ZONE_JST);
        List<CharacterDungeonDaily> rows =
                repository.findByCharacterIdAndDayKeyOrderByUpdatedAtDesc(characterId, dayKey);
        return rows.stream().map(CharacterDailyMapper::toVO).collect(Collectors.toList());
    }

    /** 中文备注：获取“指定副本 + 指定日”的单条 */
    public CharacterDailyVO getOne(UUID characterId, String dungeonKey, LocalDate dayKey) {
        CharacterDungeonDaily e = repository
                .findByCharacterIdAndDungeonKeyAndDayKey(characterId, dungeonKey, dayKey)
                .orElse(null);
        return e == null ? null : CharacterDailyMapper.toVO(e);
    }

    /** 中文备注：分页查询（可选过滤）；注意：页码从 0 开始 */
    public PageResult<CharacterDailyVO> pageQuery(UUID characterId,
                                                  String dungeonKey,
                                                  LocalDate dayFrom,
                                                  LocalDate dayTo,
                                                  int pageNo,
                                                  int pageSize) {
        // 中文备注：pageNo 不再 -1，直接使用 0 基页码；保护性下限
        Pageable pageable = PageRequest.of(Math.max(pageNo, 0), Math.max(pageSize, 1),
                Sort.by(Sort.Direction.DESC, "updatedAt"));

        Page<CharacterDungeonDaily> page = repository.pageQuery(characterId, dungeonKey, dayFrom, dayTo, pageable);
        List<CharacterDailyVO> vos = page.getContent().stream()
                .map(CharacterDailyMapper::toVO)
                .collect(Collectors.toList());

        // 中文备注：严格按照 PageResult 的字段顺序与语义（page 从 0 开始）
        return new PageResult<>(
                page.getNumber(),           // 当前页（0 基）
                page.getSize(),             // 每页大小
                page.getTotalElements(),    // 总记录数
                page.getTotalPages(),       // 总页数
                vos                         // 当前页数据列表
        );
    }
}
