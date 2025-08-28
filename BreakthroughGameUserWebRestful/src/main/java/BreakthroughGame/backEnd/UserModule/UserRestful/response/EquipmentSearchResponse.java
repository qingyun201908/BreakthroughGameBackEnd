package BreakthroughGame.backEnd.UserModule.UserRestful.response;// 文件：BreakthroughGame/backEnd/DefinitionModule/rest/response/EquipmentSearchResponse.java

import BreakthroughGame.backEnd.UserModule.UserRestful.dto.EquipmentItemVO;
import BreakthroughGame.backEnd.UserModule.UserRestful.response.AllResponse;
import lombok.*;

import java.util.List;

/** 中文备注：分页查询响应，继承 AllResponse */
@Data @NoArgsConstructor @AllArgsConstructor @Builder
public class EquipmentSearchResponse extends AllResponse {
    private List<EquipmentItemVO> items; // 中文备注：当前页数据
    private long total;                  // 中文备注：总条数
    private int page;                    // 中文备注：页码，从 1 开始
    private int size;                    // 中文备注：每页大小
    private boolean hasNext;             // 中文备注：是否有下一页

    public static EquipmentSearchResponse ok(List<EquipmentItemVO> items, long total, int page, int size) {
        return EquipmentSearchResponse.builder()
                .items(items).total(total).page(page).size(size)
                .hasNext((long)page * size < total)
                .build();
    }
}
