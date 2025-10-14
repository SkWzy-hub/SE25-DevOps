package com.SE2025BackEnd_16.project.service;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Service;

@Service
public class EmailService {

    @Autowired(required = false)
    private JavaMailSender mailSender;

    @Value("${spring.mail.username:}")
    private String fromEmail;

    @Value("${app.mail.enabled:false}")
    private boolean mailEnabled;

    /**
     * 发送忘记密码验证码邮件
     */
    public boolean sendForgotPasswordCode(String toEmail, String verificationCode) {
        try {
            // 如果邮件功能未启用或未配置，则使用控制台输出
            if (!mailEnabled || mailSender == null || fromEmail.isEmpty()) {
                System.out.println("===========================================");
                System.out.println("邮件服务未配置，验证码已输出到控制台");
                System.out.println("收件人: " + toEmail);
                System.out.println("验证码: " + verificationCode);
                System.out.println("===========================================");
                return true;
            }

            System.out.println("准备发送邮件到: " + toEmail + "，从: " + fromEmail);
            
            SimpleMailMessage message = new SimpleMailMessage();
            message.setFrom(fromEmail);
            message.setTo(toEmail);
            message.setSubject("校园二手交易平台 - 忘记密码验证码");
            message.setText(buildEmailContent(verificationCode));

            mailSender.send(message);
            System.out.println("✅ 忘记密码验证码邮件已成功发送到: " + toEmail);
            return true;

        } catch (Exception e) {
            System.err.println("❌ 发送邮件失败: " + e.getMessage());
            e.printStackTrace(); // 打印完整错误信息
            
            // 邮件发送失败时回退到控制台输出
            System.out.println("===========================================");
            System.out.println("邮件发送失败，验证码已输出到控制台");
            System.out.println("收件人: " + toEmail);
            System.out.println("验证码: " + verificationCode);
            System.out.println("错误信息: " + e.getMessage());
            System.out.println("===========================================");
            return true; // 开发阶段仍然返回成功
        }
    }

    /**
     * 发送注册验证码邮件
     */
    public boolean sendRegistrationCode(String toEmail, String verificationCode) {
        try {
            // 如果邮件功能未启用或未配置，则使用控制台输出
            if (!mailEnabled || mailSender == null || fromEmail.isEmpty()) {
                System.out.println("===========================================");
                System.out.println("邮件服务未配置，注册验证码已输出到控制台");
                System.out.println("收件人: " + toEmail);
                System.out.println("验证码: " + verificationCode);
                System.out.println("===========================================");
                return true;
            }

            System.out.println("准备发送注册验证码邮件到: " + toEmail + "，从: " + fromEmail);
            
            SimpleMailMessage message = new SimpleMailMessage();
            message.setFrom(fromEmail);
            message.setTo(toEmail);
            message.setSubject("校园二手交易平台 - 注册验证码");
            message.setText(buildRegistrationEmailContent(verificationCode));

            mailSender.send(message);
            System.out.println("✅ 注册验证码邮件已成功发送到: " + toEmail);
            return true;

        } catch (Exception e) {
            System.err.println("❌ 发送注册验证码邮件失败: " + e.getMessage());
            return false;
        }
    }

    /**
     * 构建邮件内容
     */
    private String buildEmailContent(String verificationCode) {
        return String.format(
            "亲爱的用户，\n\n" +
            "您好！您正在进行密码重置操作，验证码为：\n\n" +
            "%s\n\n" +
            "该验证码5分钟内有效，请尽快使用。\n" +
            "如果这不是您本人的操作，请忽略此邮件。\n\n" +
            "此邮件由系统自动发送，请勿回复。\n\n" +
            "校园二手交易平台",
            verificationCode
        );
    }

    /**
     * 构建注册验证码邮件内容
     */
    private String buildRegistrationEmailContent(String verificationCode) {
        return "【校园二手交易平台】\n\n" +
                "您正在注册校园二手交易平台账户，您的验证码是：\n\n" +
                verificationCode + "\n\n" +
                "验证码有效期为10分钟，请及时使用。\n\n" +
                "如果这不是您本人的操作，请忽略此邮件。\n\n" +
                "——校园二手交易平台";
    }
} 