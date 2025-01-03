package com.sjxm.springbootinit.model.dto.homework;

import com.sjxm.springbootinit.common.PageRequest;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.io.Serializable;

/**
 * @Author: 四季夏目
 * @Date: 2024/12/22
 * @Description:
 */
@EqualsAndHashCode(callSuper = true)
@Data
public class HomeworkHistoryPageQueryRequest extends PageRequest implements Serializable {
    private Integer year;
}
