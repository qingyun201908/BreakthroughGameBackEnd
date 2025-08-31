// 文件：BreakthroughGame/backEnd/UserModule/UserRestful/dto/BagSnapshot.java
package BreakthroughGame.backEnd.UserModule.UserRestful.dto;

import lombok.AllArgsConstructor;
import lombok.Data;

import java.util.List;

/** 中文备注：背包快照（容量 + 条目） */
@Data
@AllArgsConstructor
public class BagSnapshot {
    private int capacity;
    private List<BagItemView> items;
}
