package com.jobportal.security;

import com.jobportal.entity.User;
import com.jobportal.exception.UnauthorizedException;
import com.jobportal.repository.UserRepository;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;

import java.util.Optional;

@Component
public class SecurityUtils {

    private final UserRepository userRepository;

    public SecurityUtils(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    public static Optional<String> getCurrentUserEmail() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || !auth.isAuthenticated() || "anonymousUser".equals(auth.getPrincipal())) {
            return Optional.empty();
        }
        return Optional.of(auth.getName());
    }

    public User getCurrentUserOrThrow() {
        String email = getCurrentUserEmail()
                .orElseThrow(() -> new UnauthorizedException("User is not authenticated"));
        return userRepository.findByEmail(email)
                .orElseThrow(() -> new UnauthorizedException("Authenticated user not found in database"));
    }

    public Optional<User> getCurrentUser() {
        return getCurrentUserEmail().flatMap(userRepository::findByEmail);
    }
}
