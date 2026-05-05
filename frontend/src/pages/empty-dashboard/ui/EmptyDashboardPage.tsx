import { useEffect, useMemo, useState } from 'react'

import { httpClient } from '@/shared/api/http-client'
import { PageShell } from '@/shared/ui/PageShell'

type BackendLesson = {
  id: string
  title: string
}

type LessonModuleResponse = {
  moduleKey: string
  displayName: string
  schemaVersion: number
  active: boolean
  lessons?: BackendLesson[]
}

type LessonResponse = {
  id: string
  title: string
}

export const EmptyDashboardPage = () => {
  const [modules, setModules] = useState<LessonModuleResponse[]>([])
  const [isLoading, setIsLoading] = useState(true)
  const [error, setError] = useState<string | null>(null)

  useEffect(() => {
    const loadModules = async () => {
      setIsLoading(true)
      setError(null)

      try {
        const modulesResponse = await httpClient<LessonModuleResponse[]>('/api/v1/lessons/modules')

        const modulesWithLessons = await Promise.all(
          modulesResponse.map(async (module) => {
            try {
              const lessonsResponse = await httpClient<LessonResponse[]>(
                `/api/v1/lessons/modules/${encodeURIComponent(module.moduleKey)}`,
              )

              return {
                ...module,
                lessons: lessonsResponse.map((lesson) => ({
                  id: lesson.id,
                  title: lesson.title,
                })),
              }
            } catch {
              return {
                ...module,
                lessons: [],
              }
            }
          }),
        )

        setModules(modulesWithLessons)
      } catch (loadError) {
        const message = loadError instanceof Error ? loadError.message : 'Failed to load modules'
        setError(message)
      } finally {
        setIsLoading(false)
      }
    }

    void loadModules()
  }, [])

  const visibleModules = useMemo(() => modules.filter((module) => module.active), [modules])

  return (
    <PageShell
      title="Учебные модули"
      description="Здесь собраны модули и уроки. Контент самих уроков откроем на следующем этапе."
    >
      <div className="space-y-4">
        {isLoading ? <p className="text-sm text-app-muted">Загрузка модулей...</p> : null}
        {error ? <p className="text-sm text-red-600">{error}</p> : null}
        {!isLoading && !error && visibleModules.length === 0 ? (
          <p className="text-sm text-app-muted">Активные модули пока не найдены.</p>
        ) : null}

        {!isLoading && !error
          ? visibleModules.map((module) => (
              <section key={module.moduleKey} className="rounded-lg border bg-white p-5 shadow-sm">
                <header className="flex flex-wrap items-center justify-between gap-3">
                  <h2 className="text-xl font-semibold text-app-fg">{module.displayName}</h2>
                  <span className="rounded-md border px-2 py-1 text-xs text-app-muted">
                    {module.moduleKey} · v{module.schemaVersion}
                  </span>
                </header>
                <ul className="mt-3 space-y-2">
                  {(module.lessons ?? []).length > 0 ? (
                    (module.lessons ?? []).map((lesson) => (
                      <li key={lesson.id} className="rounded-md border bg-app-bg px-3 py-2 text-sm text-app-fg">
                        {lesson.title}
                      </li>
                    ))
                  ) : (
                    <li className="text-sm text-app-muted">Для этого модуля пока нет уроков.</li>
                  )}
                </ul>
              </section>
            ))
          : null}
      </div>
    </PageShell>
  )
}
