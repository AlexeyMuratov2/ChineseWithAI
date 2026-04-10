package ru.chinesewithai.backend.user.application.port.in;

import ru.chinesewithai.backend.user.application.command.RegisterUserCommand;
import ru.chinesewithai.backend.user.application.view.UserProfileView;

public interface RegisterUserUseCase {
    UserProfileView register(RegisterUserCommand command);
}
