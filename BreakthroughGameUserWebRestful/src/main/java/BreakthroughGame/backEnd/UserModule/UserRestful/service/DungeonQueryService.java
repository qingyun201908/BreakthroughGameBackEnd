// 文件：BreakthroughGame/backEnd/UserModule/UserRestful/service/DungeonQueryService.java
package BreakthroughGame.backEnd.UserModule.UserRestful.service;

import BreakthroughGame.backEnd.UserModule.UserRestful.common.api.ResultCode;
import BreakthroughGame.backEnd.UserModule.UserRestful.common.exception.BizException;
import BreakthroughGame.backEnd.UserModule.UserRestful.dto.DungeonVO;
import BreakthroughGame.backEnd.UserModule.UserRestful.dto.PageResult;
import BreakthroughGame.backEnd.WarcraftDungeonsInfo.entity.DungeonDefinition;
import BreakthroughGame.backEnd.WarcraftDungeonsInfo.repository.DungeonDefinitionRepository;
import jakarta.annotation.Nullable;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.*;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.Locale;
import java.util.Objects;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class DungeonQueryService {

    private final DungeonDefinitionRepository repo;

    /** 中文备注：分页查询（支持 active 过滤与 keyword 模糊搜索 dungeonKey/title） */
    @Transactional(readOnly = true)
    public PageResult<DungeonVO> pageQuery(@Nullable Boolean active,
                                           @Nullable String keyword,
                                           int page, int size,
                                           String sortBy, String sortDir) {

        // —— 入参兜底 & 限流 —— //
        page = Math.max(0, page);
        size = Math.min(Math.max(1, size), 200); // 中文备注：最大每页200
        if (sortBy == null || sortBy.isBlank()) sortBy = "sortOrder"; // 中文备注：默认按 sortOrder
        Sort.Direction dir = "desc".equalsIgnoreCase(sortDir) ? Sort.Direction.DESC : Sort.Direction.ASC;

        Pageable pageable = PageRequest.of(page, size, Sort.by(dir, sortBy));

        // —— 动态条件 —— //
        Specification<DungeonDefinition> spec = (root, query, cb) -> {
            var list = new ArrayList<jakarta.persistence.criteria.Predicate>();
            if (active != null) {
                list.add(cb.equal(root.get("isActive"), active));
            }
            if (keyword != null && !keyword.isBlank()) {
                String like = "%" + keyword.trim().toLowerCase(Locale.ROOT) + "%";
                list.add(
                        cb.or(
                                cb.like(cb.lower(root.get("dungeonKey")), like),
                                cb.like(cb.lower(root.get("title")), like)
                        )
                );
            }
            return list.isEmpty() ? cb.conjunction() : cb.and(list.toArray(new jakarta.persistence.criteria.Predicate[0]));
        };

        Page<DungeonDefinition> pageData = repo.findAll(spec, pageable);

        // —— 映射为 VO —— //
        var items = pageData.getContent().stream().map(this::toVO).collect(Collectors.toList());

        return new PageResult<>(
                pageData.getNumber(),
                pageData.getSize(),
                pageData.getTotalElements(),
                pageData.getTotalPages(),
                items
        );
    }

    /** 中文备注：根据 dungeon_key 查询单条 */
    @Transactional(readOnly = true)
    public DungeonVO getByKey(String dungeonKey) {
        if (dungeonKey == null || dungeonKey.isBlank()) {
            throw new BizException(ResultCode.A_BAD_REQUEST, "dungeon_key 不能为空");
        }
        DungeonDefinition def = repo.findByDungeonKey(dungeonKey)
                .orElseThrow(() -> new BizException(ResultCode.A_BAD_REQUEST, "未找到该副本：" + dungeonKey));
        return toVO(def);
    }

    /** 中文备注：实体 -> VO 的映射（避免直接暴露实体） */
    private DungeonVO toVO(DungeonDefinition d) {
        DungeonVO vo = new DungeonVO();
        vo.setId(d.getId());
        vo.setDungeonKey(d.getDungeonKey());
        vo.setTitle(d.getTitle());
        vo.setDailyRunsMax(d.getDailyRunsMax());
        vo.setMaxDifficulty(d.getMaxDifficulty());
        vo.setActive(d.isActive());
        vo.setSortOrder(d.getSortOrder());
        vo.setUpdatedAt(d.getUpdatedAt());
        return vo;
    }
}
