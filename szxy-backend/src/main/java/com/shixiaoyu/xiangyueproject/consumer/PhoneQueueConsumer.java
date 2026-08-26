package com.shixiaoyu.xiangyueproject.consumer;

import com.aliyun.tea.TeaException;
import com.shixiaoyu.xiangyueproject.constants.CommonConstants;
import com.shixiaoyu.xiangyueproject.utils.PhoneUtil;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.annotation.*;
import org.springframework.stereotype.Component;

import java.util.Map;

@Slf4j
@Component
@RabbitListener(bindings = @QueueBinding(value=@Queue(name= CommonConstants.PHONE_QUEUE_NAME),
        exchange = @Exchange(name = CommonConstants.EXCHANGE_NAME),
        key = CommonConstants.PHONE_ROUTING_KEY))
public class PhoneQueueConsumer {
    @Resource
    private PhoneUtil phoneUtil;

    @RabbitHandler
    public void handlerPhoneReceive(Map<String,String> map) throws Exception {
        String phone= map.get("phone");
        String code= map.get("code");

        com.aliyun.dypnsapi20170525.Client client = phoneUtil.createClient();
        com.aliyun.dypnsapi20170525.models.SendSmsVerifyCodeRequest sendSmsVerifyCodeRequest = new com.aliyun.dypnsapi20170525.models.SendSmsVerifyCodeRequest()
                .setSignName("速通互联验证码")
                .setTemplateCode("100001")
                .setPhoneNumber(phone)
                .setTemplateParam("{\"code\":\""+code+"\",\"min\":\"3\"}");
        com.aliyun.teautil.models.RuntimeOptions runtime = new com.aliyun.teautil.models.RuntimeOptions();
        try {
            com.aliyun.dypnsapi20170525.models.SendSmsVerifyCodeResponse resp = client.sendSmsVerifyCodeWithOptions(sendSmsVerifyCodeRequest, runtime);
            log.info(new com.google.gson.Gson().toJson(resp));
        } catch (TeaException error) {
            // 错误 message
            log.error(error.getMessage());
            // 诊断地址
            log.error(error.getData().get("Recommend").toString());
        } catch (Exception _error) {
            TeaException error = new TeaException(_error.getMessage(), _error);
            // 错误 message
            log.error(error.getMessage());
            // 诊断地址
            log.error(error.getData().get("Recommend").toString());
        }
    }
}
