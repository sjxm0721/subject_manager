package com.sjxm.springbootinit.model.vo;

import lombok.Data;

import java.io.Serializable;
import java.math.BigDecimal;

/**
 * @Author: 四季夏目
 * @Date: 2025/1/3
 * @Description:
 */
@Data
public class DuplicateCheckVO implements Serializable {

    private Long id;

    private Long sourceId;

    private String sourceName;

    private Long targetId;

    private String targetName;

    private BigDecimal similarity;

    private BigDecimal briefValue;

    private BigDecimal backgroundValue;

    private BigDecimal systemDesignValue;

    private BigDecimal wordValue;

    private BigDecimal pdfValue;

    private BigDecimal sourceValue;


}
