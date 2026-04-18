package ru.chinesewithai.backend.lesson.application.port.out;

import java.util.UUID;

public interface CurrentLessonOwnerProvider {
    UUID getCurrentOwnerId();
}
