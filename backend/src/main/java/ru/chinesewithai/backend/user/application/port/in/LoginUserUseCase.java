package ru.chinesewithai.backend.user.application.port.in;

import ru.chinesewithai.backend.user.application.command.LoginCommand;
import ru.chinesewithai.backend.user.application.view.AuthTokenView;

public interface LoginUserUseCase {
    AuthTokenView login(LoginCommand command);
}
