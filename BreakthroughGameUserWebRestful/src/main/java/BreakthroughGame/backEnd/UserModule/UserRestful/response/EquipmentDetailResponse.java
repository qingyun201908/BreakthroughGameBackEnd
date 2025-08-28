package BreakthroughGame.backEnd.UserModule.UserRestful.response;

import BreakthroughGame.backEnd.UserModule.UserRestful.dto.EquipmentItemVO;
import BreakthroughGame.backEnd.UserModule.UserRestful.response.AllResponse;
import lombok.*;

/** 中文备注：详情响应，继承 AllResponse */
@Data @NoArgsConstructor @AllArgsConstructor @Builder
public class EquipmentDetailResponse extends AllResponse {
    private EquipmentItemVO data;

    public static EquipmentDetailResponse ok(EquipmentItemVO vo){
        return EquipmentDetailResponse.builder().data(vo).build();
    }
}
