package ru.chinesewithai.backend.user.application.service;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ru.chinesewithai.backend.user.application.exception.AccountDisabledException;
import ru.chinesewithai.backend.user.application.exception.UserNotFoundException;
import ru.chinesewithai.backend.user.application.port.in.GetCurrentUserUseCase;
import ru.chinesewithai.backend.user.application.port.out.CurrentUserProvider;
import ru.chinesewithai.backend.user.application.port.out.UserRepository;
import ru.chinesewithai.backend.user.application.view.UserProfileView;

@Service
public class CurrentUserApplicationService implements GetCurrentUserUseCase {

    private final CurrentUserProvider currentUserProvider;
    private final UserRepository userRepository;

    public CurrentUserApplicationService(CurrentUserProvider currentUserProvider, UserRepository userRepository) {
        this.currentUserProvider = currentUserProvider;
        this.userRepository = userRepository;
    }

    @Override
    @Transactional(readOnly = true)
    public UserProfileView getCurrentUser() {
        var userId = currentUserProvider.getCurrentUserId();
        var user = userRepository.findById(userId).orElseThrow(() -> new UserNotFoundException(userId.value()));

        if (!user.isActive()) {
            throw new AccountDisabledException();
        }

        return UserAuthApplicationService.toProfileView(user);
    }
}
