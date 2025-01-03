package com.sjxm.springbootinit.model.vo;

import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableLogic;
import com.sjxm.springbootinit.model.entity.Grade;
import lombok.Data;

import java.io.Serializable;
import java.util.Date;
import java.util.List;

/**
 * @Author: 四季夏目
 * @Date: 2024/12/21
 * @Description:
 */
@Data
public class HomeworkVO implements Serializable {

    private Long id;

    /**
     *
     */
    private Long subjectId;

    /**
     * 课程名称
     */
    private String subjectName;

    /**
     * 组号
     */
    private Integer groupNum;

    /**
     *
     */
    private String title;

    /**
     * 0-不推荐
     1-推荐
     */
    private Integer commend;

    /**
     *
     */
    private String brief;

    /**
     *
     */
    private String hardwareTech;

    /**
     *
     */
    private String softwareTech;

    /**
     *
     */
    private String background;

    /**
     *
     */
    private String systemDesign;

    /**
     *
     */
    private String attachmentWord;

    /**
     *
     */
    private String attachmentPdf;

    /**
     *
     */
    private String attachmentSource;

    /**
     *
     */
    private String attachmentMp4;

    /**
     * 测试时间
     */
    private Date createTime;

    /**
     * 类型 0-嵌入式  1-物联网
     */
    private Integer subjectType;

    private List<UserVO> member;

    private String grade;

    private Integer isCorrect;

    private List<Grade> scores;

}
