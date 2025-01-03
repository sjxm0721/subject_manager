package com.sjxm.springbootinit.model.vo;

import lombok.Data;

import java.io.Serializable;
import java.util.List;

/**
 * @Author: 四季夏目
 * @Date: 2024/12/22
 * @Description:
 */
@Data
public class HomeworkHistoryVO implements Serializable {
    private Long id;
    private Long subjectId;
    private String subjectName;
    private String groupNum;
    private List<UserVO> member;
    private String post;
    private Integer submitYear;
    private Integer commend;
    private String brief;
    private String title;
}
