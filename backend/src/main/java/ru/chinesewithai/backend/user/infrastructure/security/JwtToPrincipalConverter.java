package ru.chinesewithai.backend.user.infrastructure.security;

import java.util.Collections;
import java.util.UUID;
import org.springframework.core.convert.converter.Converter;
import org.springframework.security.authentication.AbstractAuthenticationToken;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.oauth2.jwt.BadJwtException;
import org.springframework.security.oauth2.jwt.Jwt;
import ru.chinesewithai.backend.user.domain.model.UserId;

public class JwtToPrincipalConverter implements Converter<Jwt, AbstractAuthenticationToken> {

    @Override
    public AbstractAuthenticationToken convert(Jwt jwt) {
        try {
            var userId = new UserId(UUID.fromString(jwt.getSubject()));
            var username = jwt.getClaimAsString("username");
            var displayName = jwt.getClaimAsString("name");

            if (username == null || username.isBlank()) {
                throw new BadJwtException("username claim is required");
            }

            var principal = new AuthenticatedUserPrincipal(userId, username, displayName);
            return new UsernamePasswordAuthenticationToken(principal, jwt, Collections.emptyList());
        } catch (IllegalArgumentException ex) {
            throw new BadJwtException("Invalid token subject", ex);
        }
    }
}
