package com.sjxm.springbootinit.model.enums;

import com.sjxm.springbootinit.common.ErrorCode;
import lombok.Getter;
import lombok.Setter;

/**
 * @Author: 四季夏目
 * @Date: 2025/1/2
 * @Description: mq查重消息枚举类
 */
@Getter
public enum CheckStatusEnum {
    WAITING(0),
    PROCESSING(1),
    COMPLETED(2),
    FAILED(3),
    DEAD(4);

    private final int code;

    CheckStatusEnum(int code){
        this.code = code;
    }
}
