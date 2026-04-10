package ru.chinesewithai.backend.user.infrastructure.security;

import java.time.Instant;
import org.springframework.security.oauth2.jose.jws.MacAlgorithm;
import org.springframework.security.oauth2.jwt.JwtClaimsSet;
import org.springframework.security.oauth2.jwt.JwtEncoder;
import org.springframework.security.oauth2.jwt.JwtEncoderParameters;
import org.springframework.security.oauth2.jwt.JwsHeader;
import org.springframework.stereotype.Component;
import ru.chinesewithai.backend.user.application.port.out.JwtTokenIssuer;
import ru.chinesewithai.backend.user.application.view.AuthTokenView;
import ru.chinesewithai.backend.user.domain.model.User;

@Component
public class JwtTokenService implements JwtTokenIssuer {

    private final JwtEncoder jwtEncoder;
    private final JwtProperties jwtProperties;

    public JwtTokenService(JwtEncoder jwtEncoder, JwtProperties jwtProperties) {
        this.jwtEncoder = jwtEncoder;
        this.jwtProperties = jwtProperties;
    }

    @Override
    public AuthTokenView issue(User user) {
        var issuedAt = Instant.now();
        var expiresAt = issuedAt.plus(jwtProperties.accessTokenTtl());

        var claims = JwtClaimsSet.builder()
                .issuer(jwtProperties.issuer())
                .subject(user.id().value().toString())
                .issuedAt(issuedAt)
                .expiresAt(expiresAt)
                .claim("username", user.username().value())
                .claim("name", user.displayName())
                .build();

        var jwsHeader = JwsHeader.with(MacAlgorithm.HS256).build();
        var tokenValue =
                jwtEncoder.encode(JwtEncoderParameters.from(jwsHeader, claims)).getTokenValue();

        return new AuthTokenView(tokenValue, "Bearer", jwtProperties.accessTokenTtl().toSeconds());
    }
}
