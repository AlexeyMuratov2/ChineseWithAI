package ru.chinesewithai.backend.user.application.port.out;

import ru.chinesewithai.backend.user.application.view.AuthTokenView;
import ru.chinesewithai.backend.user.domain.model.User;

public interface JwtTokenIssuer {
    AuthTokenView issue(User user);
}
