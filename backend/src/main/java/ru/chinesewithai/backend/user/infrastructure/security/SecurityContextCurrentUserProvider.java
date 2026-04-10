package ru.chinesewithai.backend.user.infrastructure.security;

import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import ru.chinesewithai.backend.user.application.exception.AuthenticationRequiredException;
import ru.chinesewithai.backend.user.application.port.out.CurrentUserProvider;
import ru.chinesewithai.backend.user.domain.model.UserId;

@Component
public class SecurityContextCurrentUserProvider implements CurrentUserProvider {

    @Override
    public UserId getCurrentUserId() {
        var authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null
                || !authentication.isAuthenticated()
                || authentication instanceof AnonymousAuthenticationToken) {
            throw new AuthenticationRequiredException();
        }

        if (authentication.getPrincipal() instanceof AuthenticatedUserPrincipal principal) {
            return principal.userId();
        }

        throw new AuthenticationRequiredException();
    }
}
