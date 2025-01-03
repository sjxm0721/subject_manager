package com.sjxm.springbootinit.service.impl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.sjxm.springbootinit.mapper.MqMessageMapper;
import com.sjxm.springbootinit.model.entity.MqMessage;
import com.sjxm.springbootinit.service.MqMessageService;
import org.springframework.stereotype.Service;

/**
* @author sijixiamu
* @description 针对表【mq_message】的数据库操作Service实现
* @createDate 2025-01-02 18:26:25
*/
@Service
public class MqMessageServiceImpl extends ServiceImpl<MqMessageMapper, MqMessage>
    implements MqMessageService {

}




