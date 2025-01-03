package com.sjxm.springbootinit.controller;


import com.sjxm.springbootinit.common.BaseResponse;
import com.sjxm.springbootinit.common.ResultUtils;
import com.sjxm.springbootinit.utils.AliOssUtil;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import javax.annotation.Resource;
import java.io.IOException;
import java.util.UUID;

@Api(tags = "通用接口")
@RestController
@RequestMapping("/common")
@Slf4j
public class CommonController {

    @Resource
    private AliOssUtil aliOssUtil;

    @PostMapping("/upload")
    @ApiOperation("文件上传")
    public BaseResponse<String> upload(MultipartFile file) {
        try{
            //原始文件名
            String originalFilename=file.getOriginalFilename();
            //获取原始文件名的后缀
            String extension = originalFilename.substring(originalFilename.lastIndexOf("."));
            //构造新文件名称
            String objectName = UUID.randomUUID().toString()+extension;

            //文件的请求路径
            String filePath=aliOssUtil.upload(file.getBytes(),objectName);
            return ResultUtils.success(filePath);
        }catch (IOException e){
            log.error("文件上传失败：{}",e);
        }

        return null;
    }
}

