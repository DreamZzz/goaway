package com.goaway.platform.security;

import com.goaway.contexts.account.domain.User;
import com.goaway.contexts.account.infrastructure.persistence.UserRepository;
import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import java.util.Optional;

@Service
public class CurrentUserService {
    private final UserRepository userRepository;

    public CurrentUserService(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    public Optional<User> getCurrentUser() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !authentication.isAuthenticated() || authentication instanceof AnonymousAuthenticationToken) {
            return Optional.empty();
        }

        Object principal = authentication.getPrincipal();
        if (principal instanceof User user) {
            return Optional.of(user);
        }

        String username = authentication.getName();
        if (username == null || username.isBlank()) {
            return Optional.empty();
        }

        return userRepository.findByUsername(username);
    }

    /**
     * 解析当前真实登录用户的 id（游客或未登录抛 401）。
     * 供需登录才能使用的能力（数据上报、画像、排行榜上榜）复用。
     */
    public Long requireRealUserId() {
        if (isGuest()) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "请登录后使用该功能");
        }
        return getCurrentUser()
                .map(com.goaway.contexts.account.domain.User::getId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.UNAUTHORIZED, "请登录后使用该功能"));
    }

    public boolean isGuest() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        return authentication != null
                && authentication.isAuthenticated()
                && !(authentication instanceof AnonymousAuthenticationToken)
                && Boolean.TRUE.equals(authentication.getDetails());
    }
}
