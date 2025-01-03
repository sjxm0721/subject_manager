package com.sjxm.springbootinit.model.enums;

import cn.hutool.core.util.StrUtil;
import org.apache.commons.lang3.ObjectUtils;

import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

/**
 * @Author: 四季夏目
 * @Date: 2024/12/27
 * @Description:
 */

//0-申请中
//        1-已同意
//        2-拒绝
//        3-归还中
//        4-已归还

public enum ApplyDeviceStatusEnum {
    APPLYING("申请中", 0),
    APPROVE("已同意", 1),
    REFUSE("拒绝", 2),

    RETURNING("归还中",3),

    RETURNED("已归还",4);

    private final String text;

    private final Integer value;

    ApplyDeviceStatusEnum(String text, Integer value) {
        this.text = text;
        this.value = value;
    }

    /**
     * 获取值列表
     *
     * @return
     */
    public static List<Integer> getValues() {
        return Arrays.stream(values()).map(item -> item.value).collect(Collectors.toList());
    }

    /**
     * 根据 value 获取枚举
     *
     * @param text
     * @return
     */
    public static ApplyDeviceStatusEnum getEnumByText(String text) {
        if (StrUtil.isEmpty(text)) {
            return null;
        }
        for (ApplyDeviceStatusEnum anEnum : ApplyDeviceStatusEnum.values()) {
            if (anEnum.text.equals(text)) {
                return anEnum;
            }
        }
        return null;
    }

    public static ApplyDeviceStatusEnum getEnumByValue(Integer value) {
        if (ObjectUtils.isEmpty(value)) {
            return null;
        }
        for (ApplyDeviceStatusEnum anEnum : ApplyDeviceStatusEnum.values()) {
            if (anEnum.value.equals(value)) {
                return anEnum;
            }
        }
        return null;
    }

    public Integer getValue() {
        return value;
    }

    public String getText() {
        return text;
    }
}
