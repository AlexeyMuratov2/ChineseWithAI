package ru.chinesewithai.backend.lesson.application.service;

import java.util.List;
import org.springframework.stereotype.Service;
import ru.chinesewithai.backend.lesson.application.port.in.ListLessonModulesUseCase;
import ru.chinesewithai.backend.lesson.application.port.out.LessonModuleRepository;
import ru.chinesewithai.backend.lesson.application.view.LessonModuleSummaryView;

@Service
public class LessonModuleApplicationService implements ListLessonModulesUseCase {

    private final LessonModuleRepository lessonModuleRepository;

    public LessonModuleApplicationService(LessonModuleRepository lessonModuleRepository) {
        this.lessonModuleRepository = lessonModuleRepository;
    }

    @Override
    public List<LessonModuleSummaryView> listAll() {
        return lessonModuleRepository.findAllOrderByModuleKeyAsc().stream()
                .map(m -> new LessonModuleSummaryView(
                        m.moduleKey(), m.displayName(), m.schemaVersion(), m.active()))
                .toList();
    }
}
