// 文件：BreakthroughGame/backEnd/UserModule/UserRestful/dto/EquipmentImportResult.java
package BreakthroughGame.backEnd.UserModule.UserRestful.dto;

import lombok.*;

import java.util.ArrayList;
import java.util.List;

/** 中文备注：导入结果报告 */
@Data @NoArgsConstructor @AllArgsConstructor @Builder
public class EquipmentImportResult {
    private int totalRows;     // 解析的总行数（不含表头）
    private int inserted;      // 插入数量
    private int updated;       // 更新数量
    private int skipped;       // 跳过数量（空行/无效）
    private int failed;        // 失败数量（模式冲突/解析错误）
    @Builder.Default
    private List<RowError> errors = new ArrayList<>(); // 行级错误明细


    @Data @NoArgsConstructor @AllArgsConstructor
    public static class RowError {
        private int rowIndex;    // Excel 行号（人类视角：从1开始；含表头请自述）
        private String message;  // 错误信息
    }
}
