package com.sjxm.springbootinit.model.dto.user;

import cloud.tianai.captcha.validator.common.model.dto.ImageCaptchaTrack;
import lombok.Data;

/**
 * @Author: 四季夏目
 * @Date: 2025/1/8
 * @Description:
 */
@Data
public class UserImageCaptchaRequest {

    private String id;
    private ImageCaptchaTrack data;

}
