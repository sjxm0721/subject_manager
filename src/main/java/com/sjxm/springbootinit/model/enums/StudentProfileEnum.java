package com.sjxm.springbootinit.model.enums;

import lombok.Getter;
import lombok.Setter;

/**
 * @Author: 四季夏目
 * @Date: 2025/1/8
 * @Description:
 */
@Getter
public enum StudentProfileEnum {

    CHECK("check"),
    UPLOAD("upload");
    private final String value;

    StudentProfileEnum(String value){
        this.value = value;
    }

}
