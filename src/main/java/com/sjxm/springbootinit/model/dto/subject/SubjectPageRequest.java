package com.sjxm.springbootinit.model.dto.subject;

import com.sjxm.springbootinit.common.PageRequest;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.io.Serializable;

/**
 * @Author: 四季夏目
 * @Date: 2024/12/30
 * @Description:
 */
@Data
@EqualsAndHashCode(callSuper = true)
public class SubjectPageRequest extends PageRequest implements Serializable {
}
