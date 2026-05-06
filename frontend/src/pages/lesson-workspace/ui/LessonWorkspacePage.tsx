import { useEffect, useMemo, useState } from 'react'
import { useQueries, useQuery } from '@tanstack/react-query'

import { lessonApi, type Lesson, type LessonModule } from '@/entities/lesson'
import { LessonForgePanel } from '@/features/lesson-forge'
import { EmptyState } from '@/shared/ui/EmptyState'

type ModuleWithLessons = LessonModule & {
  lessons: Lesson[]
  lessonsLoading: boolean
}

export const LessonWorkspacePage = () => {
  const [selectedModuleKey, setSelectedModuleKey] = useState('')
  const [recentLessonId, setRecentLessonId] = useState<string | null>(null)

  const modulesQuery = useQuery({
    queryKey: ['lesson-modules'],
    queryFn: lessonApi.listModules,
  })

  const activeModules = useMemo(
    () => (modulesQuery.data ?? []).filter((module) => module.active),
    [modulesQuery.data],
  )

  useEffect(() => {
    if (!selectedModuleKey && activeModules.length > 0) {
      setSelectedModuleKey(activeModules[0].moduleKey)
    }
  }, [activeModules, selectedModuleKey])

  const lessonQueries = useQueries({
    queries: activeModules.map((module) => ({
      queryKey: ['lessons', module.moduleKey],
      queryFn: () => lessonApi.listLessonsByModule(module.moduleKey),
      enabled: module.active,
    })),
  })

  const modulesWithLessons: ModuleWithLessons[] = activeModules.map((module, index) => ({
    ...module,
    lessons: lessonQueries[index]?.data ?? [],
    lessonsLoading: lessonQueries[index]?.isLoading ?? false,
  }))

  const selectedModule =
    modulesWithLessons.find((module) => module.moduleKey === selectedModuleKey) ?? modulesWithLessons[0]

  const handleGenerated = (lesson: Lesson) => {
    setRecentLessonId(lesson.id)
  }

  return (
    <div className="space-y-8">
      <section className="relative overflow-hidden rounded-lg border border-[#dbe7f3] bg-[#fffdf7] px-5 py-6 shadow-[0_20px_70px_rgba(36,50,75,0.08)] sm:px-8">
        <div className="absolute inset-y-0 right-0 hidden w-72 bg-[radial-gradient(circle_at_50%_45%,rgba(25,167,160,0.24),transparent_58%)] lg:block" />
        <div className="relative grid gap-5 lg:grid-cols-[1fr_auto] lg:items-end">
          <div>
            <p className="text-xs font-black uppercase text-[#19a7a0]">ChineseWithAI</p>
            <h1 className="mt-2 font-display text-4xl leading-tight text-[#24324b] sm:text-5xl">
              Уроки внутри модулей
            </h1>
            <p className="mt-3 max-w-2xl text-sm leading-6 text-app-muted">
              Создавайте источник, запускайте генерацию и проверяйте сырой JSON без лишних экранов.
            </p>
          </div>
          <div className="grid grid-cols-3 gap-2 text-center">
            <div className="rounded-lg bg-[#173b4b] px-4 py-3 text-white">
              <span className="block text-2xl font-black">{activeModules.length}</span>
              <span className="text-xs text-[#b8d5dc]">модулей</span>
            </div>
            <div className="rounded-lg bg-[#19a7a0] px-4 py-3 text-white">
              <span className="block text-2xl font-black">
                {modulesWithLessons.reduce((total, module) => total + module.lessons.length, 0)}
              </span>
              <span className="text-xs text-[#e7fffc]">уроков</span>
            </div>
            <div className="rounded-lg bg-[#ff6b4a] px-4 py-3 text-white">
              <span className="block font-display text-2xl">学</span>
              <span className="text-xs text-[#fff1ec]">forge</span>
            </div>
          </div>
        </div>
      </section>

      {modulesQuery.isLoading ? (
        <EmptyState title="Загружаем модули" description="Каталог уроков скоро появится." />
      ) : null}

      {modulesQuery.error instanceof Error ? (
        <EmptyState title="Модули не загрузились" description={modulesQuery.error.message} />
      ) : null}

      {!modulesQuery.isLoading && !modulesQuery.error && activeModules.length === 0 ? (
        <EmptyState title="Нет активных модулей" description="На бэке пока не включен ни один модуль уроков." />
      ) : null}

      {selectedModule ? (
        <LessonForgePanel
          modules={activeModules}
          selectedModuleKey={selectedModule.moduleKey}
          onSelectedModuleKeyChange={setSelectedModuleKey}
          onGenerated={handleGenerated}
        />
      ) : null}

      {modulesWithLessons.length > 0 ? (
        <section className="space-y-4">
          <div className="flex flex-wrap items-end justify-between gap-3">
            <div>
              <p className="text-xs font-black uppercase text-[#19a7a0]">Карта модулей</p>
              <h2 className="font-display text-3xl text-app-fg">Готовые уроки</h2>
            </div>
            <p className="text-sm font-bold text-app-muted">{selectedModule?.displayName}</p>
          </div>

          <div className="grid gap-4 lg:grid-cols-2">
            {modulesWithLessons.map((module) => (
              <section
                key={module.moduleKey}
                className="rounded-lg border border-[#dbe7f3] bg-white p-5 shadow-[0_16px_45px_rgba(36,50,75,0.06)]"
              >
                <header className="flex items-center justify-between gap-4">
                  <div>
                    <h3 className="text-lg font-black text-app-fg">{module.displayName}</h3>
                    <p className="text-xs font-bold text-app-muted">
                      {module.moduleKey} · schema v{module.schemaVersion}
                    </p>
                  </div>
                  <span className="rounded-lg bg-[#fff0d4] px-3 py-2 text-sm font-black text-[#9b5a00]">
                    {module.lessons.length}
                  </span>
                </header>

                <div className="mt-4 space-y-2">
                  {module.lessonsLoading ? <p className="text-sm text-app-muted">Загрузка уроков...</p> : null}
                  {!module.lessonsLoading && module.lessons.length === 0 ? (
                    <p className="rounded-lg border border-dashed border-[#dbe7f3] px-3 py-3 text-sm text-app-muted">
                      Уроков пока нет.
                    </p>
                  ) : null}
                  {module.lessons.map((lesson) => (
                    <article
                      key={lesson.id}
                      className={[
                        'grid grid-cols-[auto_1fr] gap-3 rounded-lg border px-3 py-3 transition',
                        lesson.id === recentLessonId
                          ? 'border-[#19a7a0] bg-[#f2fbfa]'
                          : 'border-[#edf1f6] bg-[#fbfdff]',
                      ].join(' ')}
                    >
                      <span className="grid h-9 w-9 place-items-center rounded-md bg-[#173b4b] font-display text-lg text-[#ffcf5a]">
                        课
                      </span>
                      <span>
                        <span className="block text-sm font-black text-app-fg">{lesson.title}</span>
                        <span className="text-xs text-app-muted">{lesson.id}</span>
                      </span>
                    </article>
                  ))}
                </div>
              </section>
            ))}
          </div>
        </section>
      ) : null}
    </div>
  )
}
