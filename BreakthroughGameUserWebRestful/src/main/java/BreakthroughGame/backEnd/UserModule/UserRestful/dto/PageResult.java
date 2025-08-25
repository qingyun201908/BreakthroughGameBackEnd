// 文件：BreakthroughGame/backEnd/UserModule/UserRestful/dto/PageResult.java
package BreakthroughGame.backEnd.UserModule.UserRestful.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/** 中文备注：通用分页返回结构 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class PageResult<T> {
    @Schema(description = "当前页码（从0开始）")
    private int page;
    @Schema(description = "每页大小")
    private int size;
    @Schema(description = "总记录数")
    private long totalElements;
    @Schema(description = "总页数")
    private int totalPages;
    @Schema(description = "当前页数据列表")
    private List<T> items;
}
