package com.sjxm.springbootinit.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.sjxm.springbootinit.model.entity.DuplicateCheck;

/**
* @author sijixiamu
* @description 针对表【duplicate_check】的数据库操作Service
* @createDate 2025-01-02 11:33:17
*/
public interface DuplicateCheckService extends IService<DuplicateCheck> {

    void getSimilarity(Long homeworkId);


}
