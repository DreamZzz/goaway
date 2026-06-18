package com.goaway.contexts.account.api;

import com.goaway.contexts.account.application.DemoLoginService;
import com.goaway.contexts.account.application.GuestSessionService;
import com.goaway.contexts.account.domain.User;
import com.goaway.platform.security.GuestSecuritySupport;
import com.goaway.platform.security.JwtUtils;
import com.goaway.platform.security.SecurityConfig;
import com.goaway.platform.security.AppleTokenVerifier;
import com.goaway.platform.security.CurrentUserService;
import com.goaway.contexts.account.application.CaptchaService;
import com.goaway.platform.provider.mail.EmailDeliveryException;
import com.goaway.platform.provider.mail.MailSender;
import com.goaway.platform.provider.sms.SmsDeliveryException;
import com.goaway.contexts.account.application.UserService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.test.web.servlet.MockMvc;

import jakarta.servlet.http.HttpServletRequest;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(AuthController.class)
@Import(SecurityConfig.class)
class AuthControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private UserService userService;

    @MockBean
    private DemoLoginService demoLoginService;

    @MockBean
    private GuestSessionService guestSessionService;

    @MockBean
    private JwtUtils jwtUtils;

    @MockBean
    private CaptchaService captchaService;

    @MockBean
    private GuestSecuritySupport guestSecuritySupport;

    @MockBean
    private CurrentUserService currentUserService;

    @MockBean
    private AppleTokenVerifier appleTokenVerifier;

    @MockBean
    private UserDetailsService userDetailsService;

    @Test
    @DisplayName("POST /api/auth/guest should return guest auth response")
    void guestLogin_ShouldReturnGuestAuthResponse() throws Exception {
        User guestUser = new User();
        guestUser.setId(501L);
        guestUser.setUsername("guest_abc123");
        guestUser.setDisplayName("游客");
        guestUser.setEmail("guest@example.com");

        when(guestSecuritySupport.isValidInstallationId("550e8400-e29b-41d4-a716-446655440000")).thenReturn(true);
        when(guestSecuritySupport.resolveClientIp(any(HttpServletRequest.class))).thenReturn("1.1.1.1");
        when(guestSessionService.issueGuestSession(
                "550e8400-e29b-41d4-a716-446655440000",
                "1.1.1.1",
                null
        )).thenReturn(new GuestSessionService.GuestAuthSession(guestUser, 9001L, "guest-token", 2));

        mockMvc.perform(post("/api/auth/guest")
                        .header("X-Guest-Installation-Id", "550e8400-e29b-41d4-a716-446655440000"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.token").value("guest-token"))
                .andExpect(jsonPath("$.isGuest").value(true))
                .andExpect(jsonPath("$.guestTrialRemaining").value(2))
                .andExpect(jsonPath("$.username").value("guest_abc123"));
    }

    @Test
    @DisplayName("POST /api/auth/guest should return 400 when installation id is missing")
    void guestLogin_ShouldReturnBadRequest_WhenInstallationIdMissing() throws Exception {
        when(guestSecuritySupport.isValidInstallationId(null)).thenReturn(false);

        mockMvc.perform(post("/api/auth/guest"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("缺少合法的游客设备标识"));
    }

    @Test
    @DisplayName("POST /api/auth/demo-login should return auth response when enabled")
    void demoLogin_ShouldReturnAuthResponse_WhenEnabled() throws Exception {
        User demoUser = new User();
        demoUser.setId(88L);
        demoUser.setUsername("demo_guest");
        demoUser.setDisplayName("Demo Guest");
        demoUser.setEmail("demo-guest@example.com");
        demoUser.setAvatarUrl("");

        when(demoLoginService.isEnabled()).thenReturn(true);
        when(demoLoginService.getOrCreateDemoUser()).thenReturn(demoUser);
        when(jwtUtils.generateToken("demo_guest")).thenReturn("demo-token");

        mockMvc.perform(post("/api/auth/demo-login"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.token").value("demo-token"))
                .andExpect(jsonPath("$.username").value("demo_guest"))
                .andExpect(jsonPath("$.displayName").value("Demo Guest"));
    }

    @Test
    @DisplayName("POST /api/auth/demo-login should return 403 when disabled")
    void demoLogin_ShouldReturnForbidden_WhenDisabled() throws Exception {
        when(demoLoginService.isEnabled()).thenReturn(false);

        mockMvc.perform(post("/api/auth/demo-login"))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.message").value("测试模式未启用"));
    }

    @Test
    @DisplayName("POST /api/auth/sms/send should return 400 when phone is not bound")
    void sendLoginSmsCode_ShouldReturnBadRequest_WhenPhoneNotBound() throws Exception {
        when(userService.sendLoginCode("13800000000")).thenReturn(false);

        mockMvc.perform(post("/api/auth/sms/send")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"phone\":\"13800000000\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("该手机号未绑定账号"));
    }

    @Test
    @DisplayName("POST /api/auth/sms/send should return 503 when sms provider is misconfigured")
    void sendLoginSmsCode_ShouldReturnServiceUnavailable_WhenSmsProviderMisconfigured() throws Exception {
        when(userService.sendLoginCode("13800000000"))
                .thenThrow(new SmsDeliveryException(
                        SmsDeliveryException.FailureType.CONFIGURATION,
                        "短信服务未配置完整"
                ));

        mockMvc.perform(post("/api/auth/sms/send")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"phone\":\"13800000000\"}"))
                .andExpect(status().isServiceUnavailable())
                .andExpect(jsonPath("$.message").value("短信服务未配置完整"));
    }

    @Test
    @DisplayName("POST /api/auth/sms/send should return 502 when sms delivery fails")
    void sendLoginSmsCode_ShouldReturnBadGateway_WhenSmsProviderFails() throws Exception {
        when(userService.sendLoginCode("13800000000"))
                .thenThrow(new SmsDeliveryException(
                        SmsDeliveryException.FailureType.DELIVERY,
                        "短信发送失败，请稍后重试"
                ));

        mockMvc.perform(post("/api/auth/sms/send")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"phone\":\"13800000000\"}"))
                .andExpect(status().isBadGateway())
                .andExpect(jsonPath("$.message").value("短信发送失败，请稍后重试"));
    }

    @Test
    @DisplayName("POST /api/auth/sms/send should return 200 when sms is logged (log provider)")
    void sendLoginSmsCode_ShouldReturnOk_WhenSmsLogged() throws Exception {
        when(userService.sendLoginCode("13800000000")).thenReturn(true);
        when(userService.getSmsDeliveryMode()).thenReturn(com.goaway.platform.provider.sms.SmsSender.ProviderMode.LOG);

        mockMvc.perform(post("/api/auth/sms/send")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"phone\":\"13800000000\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message").value("验证码已生成并记录到日志"));
    }

    @Test
    @DisplayName("POST /api/auth/password/forgot should return 400 when email is not bound")
    void forgotPassword_ShouldReturnBadRequest_WhenEmailNotBound() throws Exception {
        when(userService.sendPasswordResetCode("user@example.com")).thenReturn(false);

        mockMvc.perform(post("/api/auth/password/forgot")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"email\":\"user@example.com\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("该邮箱未注册账号"));
    }

    @Test
    @DisplayName("POST /api/auth/password/forgot should return 503 when email provider is misconfigured")
    void forgotPassword_ShouldReturnServiceUnavailable_WhenEmailProviderMisconfigured() throws Exception {
        when(userService.sendPasswordResetCode("user@example.com"))
                .thenThrow(new EmailDeliveryException(
                        EmailDeliveryException.FailureType.CONFIGURATION,
                        "邮箱服务未配置完整，请检查 APP_AUTH_PASSWORD_RESET_PROVIDER、spring.mail.host 和 app.mail.from"
                ));

        mockMvc.perform(post("/api/auth/password/forgot")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"email\":\"user@example.com\"}"))
                .andExpect(status().isServiceUnavailable())
                .andExpect(jsonPath("$.message").value("邮箱服务未配置完整，请检查 APP_AUTH_PASSWORD_RESET_PROVIDER、spring.mail.host 和 app.mail.from"));
    }

    @Test
    @DisplayName("POST /api/auth/password/forgot should return 502 when email delivery fails")
    void forgotPassword_ShouldReturnBadGateway_WhenEmailDeliveryFails() throws Exception {
        when(userService.sendPasswordResetCode("user@example.com"))
                .thenThrow(new EmailDeliveryException(
                        EmailDeliveryException.FailureType.DELIVERY,
                        "邮件发送失败，请稍后重试"
                ));

        mockMvc.perform(post("/api/auth/password/forgot")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"email\":\"user@example.com\"}"))
                .andExpect(status().isBadGateway())
                .andExpect(jsonPath("$.message").value("邮件发送失败，请稍后重试"));
    }

    @Test
    @DisplayName("POST /api/auth/password/forgot should return log-mode message when provider is log")
    void forgotPassword_ShouldReturnLogMessage_WhenProviderIsLog() throws Exception {
        when(userService.sendPasswordResetCode("user@example.com")).thenReturn(true);
        when(userService.getPasswordResetDeliveryMode()).thenReturn(MailSender.ProviderMode.LOG);

        mockMvc.perform(post("/api/auth/password/forgot")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"email\":\"user@example.com\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message").value("找回密码验证码已生成并记录到日志"));
    }

    @Test
    @DisplayName("POST /api/auth/password/forgot should return mail-mode message when provider is mail")
    void forgotPassword_ShouldReturnMailMessage_WhenProviderIsMail() throws Exception {
        when(userService.sendPasswordResetCode("user@example.com")).thenReturn(true);
        when(userService.getPasswordResetDeliveryMode()).thenReturn(MailSender.ProviderMode.MAIL);

        mockMvc.perform(post("/api/auth/password/forgot")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"email\":\"user@example.com\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message").value("找回密码验证码已发送至邮箱"));
    }

    @Test
    @DisplayName("POST /api/auth/password/reset should return 400 when code is invalid")
    void resetPassword_ShouldReturnBadRequest_WhenCodeIsInvalid() throws Exception {
        when(userService.resetPassword("user@example.com", "123456", "new-password")).thenReturn(false);

        mockMvc.perform(post("/api/auth/password/reset")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"email\":\"user@example.com\",\"code\":\"123456\",\"newPassword\":\"new-password\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("邮箱或验证码错误"));
    }

    @Test
    @DisplayName("POST /api/auth/password/reset should return 200 when code is valid")
    void resetPassword_ShouldReturnOk_WhenCodeIsValid() throws Exception {
        when(userService.resetPassword("user@example.com", "123456", "new-password")).thenReturn(true);

        mockMvc.perform(post("/api/auth/password/reset")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"email\":\"user@example.com\",\"code\":\"123456\",\"newPassword\":\"new-password\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message").value("密码重置成功"));
    }
}
