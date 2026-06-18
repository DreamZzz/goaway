package com.goaway.contexts.account.api;

import com.goaway.contexts.account.api.dto.AppleLoginRequest;
import com.goaway.contexts.account.api.dto.AuthErrorResponse;
import com.goaway.contexts.account.api.dto.AuthRequest;
import com.goaway.contexts.account.api.dto.AuthResponse;
import com.goaway.contexts.account.api.dto.CaptchaResponse;
import com.goaway.contexts.account.api.dto.ChangePasswordRequest;
import com.goaway.contexts.account.api.dto.ConfirmEmailChangeRequest;
import com.goaway.contexts.account.api.dto.ForgotPasswordRequest;
import com.goaway.contexts.account.api.dto.LoginRequest;
import com.goaway.contexts.account.api.dto.ResetPasswordRequest;
import com.goaway.contexts.account.api.dto.SendEmailChangeCodeRequest;
import com.goaway.contexts.account.api.dto.SmsCodeRequest;
import com.goaway.contexts.account.api.dto.SmsLoginRequest;
import com.goaway.contexts.account.api.dto.UserDTO;
import com.goaway.contexts.account.application.DemoLoginService;
import com.goaway.contexts.account.application.GuestSessionService;
import com.goaway.contexts.account.domain.User;
import com.goaway.platform.security.AppleTokenVerifier;
import com.goaway.platform.security.CurrentUserService;
import io.jsonwebtoken.Claims;
import com.goaway.shared.dto.MessageResponse;
import com.goaway.platform.security.GuestSecuritySupport;
import com.goaway.platform.security.JwtUtils;
import com.goaway.platform.provider.mail.EmailDeliveryException;
import com.goaway.contexts.account.application.CaptchaService;
import com.goaway.platform.provider.mail.MailSender;
import com.goaway.platform.provider.sms.SmsSender;
import com.goaway.platform.provider.sms.SmsDeliveryException;
import com.goaway.contexts.account.application.UserService;
import jakarta.validation.Valid;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Locale;
import java.util.Optional;

@RestController
@RequestMapping("/api/auth")
public class AuthController {

    private final UserService userService;
    private final DemoLoginService demoLoginService;
    private final GuestSessionService guestSessionService;
    private final JwtUtils jwtUtils;
    private final CaptchaService captchaService;
    private final GuestSecuritySupport guestSecuritySupport;
    private final CurrentUserService currentUserService;
    private final AppleTokenVerifier appleTokenVerifier;

    public AuthController(
            UserService userService,
            DemoLoginService demoLoginService,
            GuestSessionService guestSessionService,
            JwtUtils jwtUtils,
            CaptchaService captchaService,
            GuestSecuritySupport guestSecuritySupport,
            CurrentUserService currentUserService,
            AppleTokenVerifier appleTokenVerifier
    ) {
        this.userService = userService;
        this.demoLoginService = demoLoginService;
        this.guestSessionService = guestSessionService;
        this.jwtUtils = jwtUtils;
        this.captchaService = captchaService;
        this.guestSecuritySupport = guestSecuritySupport;
        this.currentUserService = currentUserService;
        this.appleTokenVerifier = appleTokenVerifier;
    }

    @PostMapping("/register")
    public ResponseEntity<?> register(@Valid @RequestBody AuthRequest authRequest) {
        Optional<User> user = userService.register(authRequest);
        if (user.isEmpty()) {
            return ResponseEntity
                    .status(HttpStatus.BAD_REQUEST)
                    .body(new MessageResponse("用户名、邮箱或手机号已存在"));
        }

        return ResponseEntity.ok(buildAuthResponse(user.get()));
    }

    @GetMapping("/captcha")
    public ResponseEntity<CaptchaResponse> getCaptcha() {
        return ResponseEntity.ok(captchaService.createCaptcha());
    }

    @PostMapping("/login")
    public ResponseEntity<?> login(@Valid @RequestBody LoginRequest loginRequest) {
        UserService.PasswordLoginResult result = userService.authenticatePassword(loginRequest);
        if (!result.isSuccess()) {
            return ResponseEntity
                    .status(HttpStatus.UNAUTHORIZED)
                    .body(new AuthErrorResponse(
                            result.getErrorCode(),
                            result.getMessage(),
                            result.isCaptchaRequired(),
                            result.getRemainingAttempts()
                    ));
        }

        return ResponseEntity.ok(buildAuthResponse(result.getUser()));
    }

    @PostMapping("/demo-login")
    public ResponseEntity<?> demoLogin() {
        if (!demoLoginService.isEnabled()) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN)
                    .body(new MessageResponse("测试模式未启用"));
        }

        return ResponseEntity.ok(buildAuthResponse(demoLoginService.getOrCreateDemoUser()));
    }

    @PostMapping("/guest")
    public ResponseEntity<?> guestLogin(
            @RequestHeader(value = GuestSecuritySupport.GUEST_INSTALLATION_HEADER, required = false) String installationId,
            HttpServletRequest request
    ) {
        if (!guestSecuritySupport.isValidInstallationId(installationId)) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(new MessageResponse("缺少合法的游客设备标识"));
        }

        String requestId = request.getHeader("X-Request-ID");
        GuestSessionService.GuestAuthSession session = guestSessionService.issueGuestSession(
                installationId,
                guestSecuritySupport.resolveClientIp(request),
                requestId
        );
        return ResponseEntity.ok(buildGuestAuthResponse(session));
    }

    @PostMapping("/sms/send")
    public ResponseEntity<?> sendLoginSmsCode(@Valid @RequestBody SmsCodeRequest request) {
        try {
            boolean sent = userService.sendLoginCode(request.getPhone());
            if (!sent) {
                return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                        .body(new MessageResponse("该手机号未绑定账号"));
            }
        } catch (com.goaway.contexts.account.application.VerificationCodeCooldownException e) {
            return ResponseEntity.status(HttpStatus.TOO_MANY_REQUESTS)
                    .body(new MessageResponse(e.getMessage()));
        } catch (SmsDeliveryException exception) {
            SmsSender.ProviderMode providerMode = userService.getSmsDeliveryMode();
            HttpStatus status = exception.getFailureType() == SmsDeliveryException.FailureType.CONFIGURATION
                    ? HttpStatus.SERVICE_UNAVAILABLE
                    : HttpStatus.BAD_GATEWAY;
            return ResponseEntity.status(status).body(buildMessageResponse(
                    exception.getMessage(),
                    "sms",
                    toProviderName(providerMode, "aliyun"),
                    exception.getFailureType() == SmsDeliveryException.FailureType.CONFIGURATION
            ));
        }
        SmsSender.ProviderMode providerMode = userService.getSmsDeliveryMode();
        boolean setupRequired = providerMode == SmsSender.ProviderMode.LOG;
        String message = setupRequired ? "验证码已生成并记录到日志" : "验证码已发送";
        return ResponseEntity.ok(buildMessageResponse(
                message,
                "sms",
                toProviderName(providerMode, setupRequired ? "log" : "aliyun"),
                setupRequired
        ));
    }

    @PostMapping("/login/sms")
    public ResponseEntity<?> loginWithSms(@Valid @RequestBody SmsLoginRequest request) {
        Optional<User> user = userService.authenticateSms(request);
        if (user.isEmpty()) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(new MessageResponse("手机号或验证码错误"));
        }
        return ResponseEntity.ok(buildAuthResponse(user.get()));
    }

    @PostMapping("/apple")
    public ResponseEntity<?> appleLogin(@Valid @RequestBody AppleLoginRequest request) {
        Claims claims;
        try {
            claims = appleTokenVerifier.verify(request.getIdentityToken());
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(new MessageResponse("Apple 身份验证失败"));
        }
        String appleUserId = claims.getSubject();
        User user = userService.findOrCreateAppleUser(appleUserId, request.getEmail(), request.getFullName());
        return ResponseEntity.ok(buildAuthResponse(user));
    }

    @PostMapping("/password/forgot")
    public ResponseEntity<?> forgotPassword(@Valid @RequestBody ForgotPasswordRequest request) {
        try {
            boolean sent = userService.sendPasswordResetCode(request.getEmail());
            if (!sent) {
                return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                        .body(new MessageResponse("该邮箱未注册账号"));
            }
        } catch (com.goaway.contexts.account.application.VerificationCodeCooldownException e) {
            return ResponseEntity.status(HttpStatus.TOO_MANY_REQUESTS)
                    .body(new MessageResponse(e.getMessage()));
        } catch (EmailDeliveryException exception) {
            HttpStatus status = exception.getFailureType() == EmailDeliveryException.FailureType.CONFIGURATION
                    ? HttpStatus.SERVICE_UNAVAILABLE
                    : HttpStatus.BAD_GATEWAY;
            MailSender.ProviderMode providerMode = userService.getPasswordResetDeliveryMode();
            return ResponseEntity.status(status).body(buildMessageResponse(
                    exception.getMessage(),
                    "mail",
                    toProviderName(providerMode, "mail"),
                    exception.getFailureType() == EmailDeliveryException.FailureType.CONFIGURATION
            ));
        }
        MailSender.ProviderMode providerMode = userService.getPasswordResetDeliveryMode();
        boolean setupRequired = providerMode == MailSender.ProviderMode.LOG;
        return ResponseEntity.ok(buildMessageResponse(
                buildForgotPasswordSuccessMessage(),
                "mail",
                toProviderName(providerMode, setupRequired ? "log" : "mail"),
                setupRequired
        ));
    }

    @PostMapping("/password/reset")
    public ResponseEntity<?> resetPassword(@Valid @RequestBody ResetPasswordRequest request) {
        boolean success = userService.resetPassword(request.getEmail(), request.getCode(), request.getNewPassword());
        if (!success) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(new MessageResponse("邮箱或验证码错误"));
        }
        return ResponseEntity.ok(new MessageResponse("密码重置成功"));
    }

    @PostMapping("/password/change")
    public ResponseEntity<?> changePassword(@Valid @RequestBody ChangePasswordRequest request) {
        Optional<User> currentUser = currentUserService.getCurrentUser();
        if (currentUser.isEmpty()) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(new MessageResponse("请先登录"));
        }
        boolean success = userService.changePassword(currentUser.get().getId(), request.getCurrentPassword(), request.getNewPassword());
        if (!success) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(new MessageResponse("当前密码错误"));
        }
        return ResponseEntity.ok(new MessageResponse("密码修改成功"));
    }

    @PostMapping("/email/change/send-code")
    public ResponseEntity<?> sendEmailChangeCode(@Valid @RequestBody SendEmailChangeCodeRequest request) {
        Optional<User> currentUser = currentUserService.getCurrentUser();
        if (currentUser.isEmpty()) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(new MessageResponse("请先登录"));
        }
        try {
            boolean sent = userService.initiateEmailChange(currentUser.get().getId(), request.getNewEmail());
            if (!sent) {
                return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(new MessageResponse("该邮箱已被其他账号使用"));
            }
        } catch (com.goaway.contexts.account.application.VerificationCodeCooldownException e) {
            return ResponseEntity.status(HttpStatus.TOO_MANY_REQUESTS).body(new MessageResponse(e.getMessage()));
        } catch (EmailDeliveryException e) {
            return ResponseEntity.status(HttpStatus.BAD_GATEWAY).body(new MessageResponse("邮件发送失败，请稍后重试"));
        }
        return ResponseEntity.ok(new MessageResponse("验证码已发送至新邮箱"));
    }

    @PostMapping("/email/change/confirm")
    public ResponseEntity<?> confirmEmailChange(@Valid @RequestBody ConfirmEmailChangeRequest request) {
        Optional<User> currentUser = currentUserService.getCurrentUser();
        if (currentUser.isEmpty()) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(new MessageResponse("请先登录"));
        }
        Optional<User> updated = userService.confirmEmailChange(currentUser.get().getId(), request.getNewEmail(), request.getCode());
        if (updated.isEmpty()) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(new MessageResponse("验证码错误或邮箱已被占用"));
        }
        return ResponseEntity.ok(UserDTO.fromUser(updated.get()));
    }

    private AuthResponse buildAuthResponse(User user) {
        String token = jwtUtils.generateToken(user.getUsername());
        AuthResponse response = new AuthResponse(
                token,
                user.getId(),
                user.getUsername(),
                user.getDisplayName(),
                user.getEmail(),
                user.getPhone(),
                user.getAvatarUrl()
        );
        response.setIsGuest(false);
        response.setGuestTrialRemaining(null);
        return response;
    }

    private AuthResponse buildGuestAuthResponse(GuestSessionService.GuestAuthSession session) {
        User user = session.user();
        return new AuthResponse(
                session.token(),
                user.getId(),
                user.getUsername(),
                user.getDisplayName(),
                user.getEmail(),
                user.getPhone(),
                user.getAvatarUrl(),
                true,
                session.trialRemaining()
        );
    }

    private String buildForgotPasswordSuccessMessage() {
        return switch (userService.getPasswordResetDeliveryMode()) {
            case MAIL -> "找回密码验证码已发送至邮箱";
            case LOG -> "找回密码验证码已生成并记录到日志";
        };
    }

    private MessageResponse buildMessageResponse(
            String message,
            String service,
            String provider,
            boolean setupRequired
    ) {
        return new MessageResponse(message, service, provider, setupRequired);
    }

    private String toProviderName(Enum<?> providerMode, String fallbackProvider) {
        return providerMode == null ? fallbackProvider : providerMode.name().toLowerCase(Locale.ROOT);
    }
}
