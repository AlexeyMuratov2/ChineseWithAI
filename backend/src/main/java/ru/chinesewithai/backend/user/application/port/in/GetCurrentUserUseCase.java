package ru.chinesewithai.backend.user.application.port.in;

import ru.chinesewithai.backend.user.application.view.UserProfileView;

public interface GetCurrentUserUseCase {
    UserProfileView getCurrentUser();
}
