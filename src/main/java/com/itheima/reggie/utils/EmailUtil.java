package com.itheima.reggie.utils;

import org.apache.commons.mail.EmailException;
import org.apache.commons.mail.SimpleEmail;

/**
 * 生成验证码的工具类[QQ邮箱]
 */
public class EmailUtil {
    public static void sendAuthCodeEmail(String email, String authCode) {
        try {
            SimpleEmail mail = new SimpleEmail();

            //发送邮件的服务器,这个是 qq 邮箱的，不用修改
            mail.setHostName("smtp.qq.com");

            //第一个参数 userName 是对应的邮箱用户名，一般就是自己的邮箱；
            //第二个参数 password 就是 SMTP 的密码，这个需要手动获取
            mail.setAuthentication("139@qq.com", "SMTP 的密码");

            //第一个参数 email 是发送邮件的邮箱
            //第二个参数 name 是发件人
            mail.setFrom("139@qq.com", "mrs");

            mail.setSSLOnConnect(true);//使用安全链接
            mail.addTo(email);//接收的邮箱
            mail.setSubject("验证码");//设置邮件的主题
            //设置邮件的内容
            mail.setMsg("尊敬的用户:你好!\n 登陆验证码为:" + authCode + "\n" + "     (有效时长为一分钟)");
            mail.send();//发送
        } catch (EmailException e) {
            e.printStackTrace();
        }
    }
}