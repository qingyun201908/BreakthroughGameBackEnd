package BreakthroughGame.backEnd.UserModule.UserRestful.dto;// 文件：BreakthroughGame/backEnd/DungeonModule/DailyChallenge/dto/DungeonImportResult.java


import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.ArrayList;
import java.util.List;

/**
 * 中文备注：导入结果报告（返回给前端）
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class DungeonImportResult {
    private int totalRows;                 // 总行数（有效数据行）
    private int success;                   // 成功条数
    private int failed;                    // 失败条数
    private List<RowError> errors = new ArrayList<>(); // 错误明细

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class RowError {
        private int rowIndex;              // Excel 行号（从 2 开始：首行为表头）
        private String message;            // 错误原因
    }
}
