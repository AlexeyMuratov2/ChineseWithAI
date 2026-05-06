import { useEffect, useMemo, useState } from 'react'
import { useMutation, useQueryClient } from '@tanstack/react-query'

import { lessonApi, type Lesson, type LessonDraft, type LessonModule } from '@/entities/lesson'
import { Button } from '@/shared/ui/Button'
import { Input } from '@/shared/ui/Input'
import { Textarea } from '@/shared/ui/Textarea'

type SourceMode = 'text' | 'file'

type Props = {
  modules: LessonModule[]
  selectedModuleKey: string
  onSelectedModuleKeyChange: (moduleKey: string) => void
  onGenerated: (lesson: Lesson) => void
}

const defaultExplanationLanguage = 'ru'
const defaultTranslationLanguage = 'ru'

const normalizeOptional = (value: string) => {
  const normalized = value.trim()
  return normalized.length > 0 ? normalized : undefined
}

const stringifyJson = (value: unknown) => JSON.stringify(value, null, 2)

const lessonSourceAccept = [
  'application/pdf',
  'image/*',
  'text/*',
  '.pdf',
  '.jpg',
  '.jpeg',
  '.png',
  '.webp',
  '.gif',
  '.heic',
  '.heif',
  '.txt',
  '.md',
  '.csv',
  '.json',
].join(',')

const supportedTextExtensions = ['.txt', '.md', '.csv', '.json']
const supportedImageExtensions = ['.jpg', '.jpeg', '.png', '.webp', '.gif', '.heic', '.heif']

const hasFileExtension = (fileName: string, extensions: string[]) => {
  const normalized = fileName.trim().toLowerCase()
  return extensions.some((extension) => normalized.endsWith(extension))
}

const isSupportedLessonSourceFile = (file: File) => {
  const contentType = file.type.toLowerCase()
  return (
    contentType === 'application/pdf' ||
    contentType.startsWith('image/') ||
    contentType.startsWith('text/') ||
    hasFileExtension(file.name, ['.pdf', ...supportedImageExtensions, ...supportedTextExtensions])
  )
}

const fileKindLabel = (file: File) => {
  const contentType = file.type.toLowerCase()
  if (contentType === 'application/pdf' || hasFileExtension(file.name, ['.pdf'])) {
    return 'PDF'
  }
  if (contentType.startsWith('image/') || hasFileExtension(file.name, supportedImageExtensions)) {
    return 'Фото'
  }
  return 'Текст'
}

const formatFileSize = (bytes: number) => {
  if (bytes < 1024) {
    return `${bytes} Б`
  }
  const kilobytes = bytes / 1024
  if (kilobytes < 1024) {
    return `${kilobytes.toFixed(1)} КБ`
  }
  return `${(kilobytes / 1024).toFixed(1)} МБ`
}

export const LessonForgePanel = ({
  modules,
  selectedModuleKey,
  onSelectedModuleKeyChange,
  onGenerated,
}: Props) => {
  const queryClient = useQueryClient()
  const [title, setTitle] = useState('')
  const [description, setDescription] = useState('')
  const [userInstructions, setUserInstructions] = useState('')
  const [explanationLanguage, setExplanationLanguage] = useState(defaultExplanationLanguage)
  const [translationLanguage, setTranslationLanguage] = useState(defaultTranslationLanguage)
  const [sourceMode, setSourceMode] = useState<SourceMode>('text')
  const [textSource, setTextSource] = useState('')
  const [fileSource, setFileSource] = useState<File | null>(null)
  const [filePreviewUrl, setFilePreviewUrl] = useState<string | null>(null)
  const [modelKey, setModelKey] = useState('')
  const [draft, setDraft] = useState<LessonDraft | null>(null)
  const [generatedLesson, setGeneratedLesson] = useState<Lesson | null>(null)
  const [statusText, setStatusText] = useState('Готово к сборке')

  const selectedModule = useMemo(
    () => modules.find((module) => module.moduleKey === selectedModuleKey) ?? modules[0],
    [modules, selectedModuleKey],
  )

  useEffect(() => {
    if (fileSource === null || !fileSource.type.toLowerCase().startsWith('image/')) {
      setFilePreviewUrl(null)
      return undefined
    }

    const previewUrl = URL.createObjectURL(fileSource)
    setFilePreviewUrl(previewUrl)
    return () => URL.revokeObjectURL(previewUrl)
  }, [fileSource])

  const createDraftMutation = useMutation({
    mutationFn: async () => {
      const normalizedTitle = title.trim()
      if (!normalizedTitle) {
        throw new Error('Название урока обязательно')
      }
      if (sourceMode === 'text' && !textSource.trim()) {
        throw new Error('Добавьте текст источника')
      }
      if (sourceMode === 'file' && fileSource === null) {
        throw new Error('Выберите файл-источник')
      }
      if (sourceMode === 'file' && fileSource !== null && !isSupportedLessonSourceFile(fileSource)) {
        throw new Error('Поддерживаются PDF, фотографии и UTF-8 текстовые файлы')
      }

      setGeneratedLesson(null)
      setStatusText('Создаем черновик')
      const createdDraft = await lessonApi.createDraft({
        title: normalizedTitle,
        description: normalizeOptional(description),
        userInstructions: normalizeOptional(userInstructions),
        explanationLanguage: normalizeOptional(explanationLanguage),
        translationLanguage: normalizeOptional(translationLanguage),
      })

      if (sourceMode === 'text') {
        setStatusText('Добавляем текстовый источник')
        return await lessonApi.addDraftSource(createdDraft.id, {
          type: 'TEXT_NOTE',
          textContent: textSource.trim(),
        })
      }

      const file = fileSource
      if (file === null) {
        throw new Error('Выберите файл-источник')
      }

      setStatusText('Готовим upload session')
      const session = await lessonApi.createUploadSession({
        scenario: 'LESSON_SOURCE',
        expectedContentLength: file.size,
        declaredContentType: file.type || 'application/octet-stream',
        originalFileName: file.name,
      })

      setStatusText('Загружаем файл')
      const uploadedFile = await lessonApi.uploadFileContent(session.sessionId, file)

      setStatusText('Добавляем файловый источник')
      return await lessonApi.addDraftSource(createdDraft.id, {
        type: 'DOCUMENT_FILE',
        documentFileId: uploadedFile.id,
        documentOriginalFileName: uploadedFile.originalFileName ?? file.name,
      })
    },
    onSuccess: (createdDraft) => {
      setDraft(createdDraft)
      setStatusText('Черновик создан')
    },
  })

  const generateLessonMutation = useMutation({
    mutationFn: async () => {
      if (draft === null) {
        throw new Error('Сначала создайте черновик')
      }
      if (!selectedModule) {
        throw new Error('Выберите модуль')
      }

      setStatusText('Генерируем урок')
      return await lessonApi.generateLesson({
        draftId: draft.id,
        moduleKey: selectedModule.moduleKey,
        modelKey: normalizeOptional(modelKey),
      })
    },
    onSuccess: (lesson) => {
      setGeneratedLesson(lesson)
      setStatusText('JSON готов')
      void queryClient.invalidateQueries({ queryKey: ['lessons', lesson.moduleKey ?? selectedModuleKey] })
      onGenerated(lesson)
    },
  })

  const createError = createDraftMutation.error instanceof Error ? createDraftMutation.error.message : null
  const generateError = generateLessonMutation.error instanceof Error ? generateLessonMutation.error.message : null
  const isCreating = createDraftMutation.isPending
  const isGenerating = generateLessonMutation.isPending
  const selectedFileUnsupported = fileSource !== null && !isSupportedLessonSourceFile(fileSource)

  return (
    <section className="overflow-hidden rounded-lg border border-[#dbe7f3] bg-white shadow-[0_24px_80px_rgba(36,50,75,0.10)]">
      <div className="grid gap-0 lg:grid-cols-[0.95fr_1.05fr]">
        <aside className="relative min-h-[520px] bg-[#173b4b] p-6 text-white">
          <div className="absolute inset-x-0 top-0 h-2 bg-gradient-to-r from-[#ff6b4a] via-[#ffcf5a] to-[#19a7a0]" />
          <div className="relative flex h-full flex-col">
            <p className="font-display text-5xl leading-none text-[#ffcf5a]">课</p>
            <div className="mt-6">
              <p className="text-xs font-bold uppercase text-[#91d9d2]">Мастер урока</p>
              <h2 className="mt-2 font-display text-4xl leading-tight">Собери новый уровень</h2>
              <p className="mt-3 max-w-sm text-sm leading-6 text-[#d3e7ed]">
                Модуль задает правила, источник дает материал, генератор возвращает JSON.
              </p>
            </div>

            <div className="mt-8 space-y-3">
              {modules.map((module, index) => {
                const isSelected = module.moduleKey === selectedModuleKey
                return (
                  <button
                    key={module.moduleKey}
                    type="button"
                    onClick={() => onSelectedModuleKeyChange(module.moduleKey)}
                    className={[
                      'group grid w-full grid-cols-[2.75rem_1fr_auto] items-center gap-3 rounded-lg border px-3 py-3 text-left transition',
                      isSelected
                        ? 'border-[#ffcf5a] bg-white text-[#173b4b] shadow-[0_16px_28px_rgba(0,0,0,0.18)]'
                        : 'border-white/15 bg-white/6 text-white hover:border-white/35 hover:bg-white/10',
                    ].join(' ')}
                  >
                    <span
                      className={[
                        'grid h-10 w-10 place-items-center rounded-lg text-sm font-black',
                        isSelected ? 'bg-[#ffcf5a] text-[#173b4b]' : 'bg-white/12 text-[#ffcf5a]',
                      ].join(' ')}
                    >
                      {index + 1}
                    </span>
                    <span>
                      <span className="block text-sm font-black">{module.displayName}</span>
                      <span className={isSelected ? 'text-xs text-[#587085]' : 'text-xs text-[#b8d5dc]'}>
                        {module.moduleKey} · v{module.schemaVersion}
                      </span>
                    </span>
                    <span className={isSelected ? 'text-lg text-[#ff6b4a]' : 'text-lg text-[#91d9d2]'}>中</span>
                  </button>
                )
              })}
            </div>

            <div className="mt-auto pt-8">
              <div className="rounded-lg border border-white/15 bg-white/8 p-4">
                <p className="text-xs font-bold uppercase text-[#91d9d2]">Статус</p>
                <p className="mt-1 text-lg font-black">{statusText}</p>
              </div>
            </div>
          </div>
        </aside>

        <div className="space-y-7 p-5 sm:p-7">
          <div className="grid gap-4 md:grid-cols-2">
            <label className="space-y-2">
              <span className="text-xs font-black uppercase text-app-muted">Название</span>
              <Input value={title} onChange={(event) => setTitle(event.target.value)} placeholder="Урок про дорогу" />
            </label>
            <label className="space-y-2">
              <span className="text-xs font-black uppercase text-app-muted">Модель</span>
              <Input value={modelKey} onChange={(event) => setModelKey(event.target.value)} placeholder="backend default" />
            </label>
          </div>

          <div className="grid gap-4 md:grid-cols-2">
            <label className="space-y-2">
              <span className="text-xs font-black uppercase text-app-muted">Язык объяснений</span>
              <Input value={explanationLanguage} onChange={(event) => setExplanationLanguage(event.target.value)} />
            </label>
            <label className="space-y-2">
              <span className="text-xs font-black uppercase text-app-muted">Язык перевода</span>
              <Input value={translationLanguage} onChange={(event) => setTranslationLanguage(event.target.value)} />
            </label>
          </div>

          <label className="space-y-2">
            <span className="text-xs font-black uppercase text-app-muted">Описание</span>
            <Textarea
              value={description}
              onChange={(event) => setDescription(event.target.value)}
              placeholder="Короткий контекст для будущего урока"
              className="min-h-20"
            />
          </label>

          <label className="space-y-2">
            <span className="text-xs font-black uppercase text-app-muted">Инструкции</span>
            <Textarea
              value={userInstructions}
              onChange={(event) => setUserInstructions(event.target.value)}
              placeholder="На что обратить внимание генератору"
              className="min-h-20"
            />
          </label>

          <div className="space-y-3">
            <div className="flex flex-wrap items-center justify-between gap-3">
              <span className="text-xs font-black uppercase text-app-muted">Источник</span>
              <div className="grid grid-cols-2 rounded-lg bg-[#edf6fb] p-1">
                <button
                  type="button"
                  onClick={() => setSourceMode('text')}
                  className={[
                    'rounded-md px-4 py-2 text-sm font-black transition',
                    sourceMode === 'text' ? 'bg-white text-[#173b4b] shadow-sm' : 'text-[#587085]',
                  ].join(' ')}
                >
                  Текст
                </button>
                <button
                  type="button"
                  onClick={() => setSourceMode('file')}
                  className={[
                    'rounded-md px-4 py-2 text-sm font-black transition',
                    sourceMode === 'file' ? 'bg-white text-[#173b4b] shadow-sm' : 'text-[#587085]',
                  ].join(' ')}
                >
                  PDF / фото
                </button>
              </div>
            </div>

            {sourceMode === 'text' ? (
              <Textarea
                value={textSource}
                onChange={(event) => setTextSource(event.target.value)}
                placeholder="中文材料 для генерации урока"
                className="min-h-36"
              />
            ) : (
              <label
                className={[
                  'grid min-h-36 cursor-pointer gap-4 rounded-lg border border-dashed px-4 py-5 transition sm:grid-cols-[7rem_1fr] sm:items-center',
                  selectedFileUnsupported
                    ? 'border-[#ff8c73] bg-[#fff4f1]'
                    : 'border-[#9bd8d3] bg-[#f2fbfa] hover:border-[#19a7a0] hover:bg-[#e7f7f4]',
                ].join(' ')}
              >
                <span className="grid aspect-square w-full place-items-center overflow-hidden rounded-lg bg-white text-center shadow-sm">
                  {filePreviewUrl ? (
                    <img src={filePreviewUrl} alt="" className="h-full w-full object-cover" />
                  ) : (
                    <span className="font-display text-2xl text-[#19a7a0]">
                      {fileSource ? fileKindLabel(fileSource) : 'PDF'}
                    </span>
                  )}
                </span>
                <span>
                  <span className="block text-sm font-black text-[#173b4b]">
                    {fileSource ? fileSource.name : 'Выбрать PDF, фото или текстовый файл'}
                  </span>
                  <span className={selectedFileUnsupported ? 'mt-1 block text-xs font-bold text-[#a33d27]' : 'mt-1 block text-xs text-app-muted'}>
                    {fileSource
                      ? `${fileKindLabel(fileSource)} · ${formatFileSize(fileSource.size)}`
                      : 'PDF, JPG, PNG, WebP, HEIC, TXT, MD, CSV, JSON'}
                  </span>
                  {selectedFileUnsupported ? (
                    <span className="mt-2 block text-xs font-bold text-[#a33d27]">
                      Этот формат не получится добавить как источник урока.
                    </span>
                  ) : null}
                </span>
                <input
                  type="file"
                  accept={lessonSourceAccept}
                  className="sr-only"
                  onChange={(event) => setFileSource(event.target.files?.[0] ?? null)}
                />
              </label>
            )}
          </div>

          <div className="flex flex-wrap items-center gap-3 border-t border-[#edf1f6] pt-5">
            <Button onClick={() => createDraftMutation.mutate()} disabled={isCreating || isGenerating || !selectedModule}>
              {isCreating ? 'Создаем...' : draft ? 'Создать новый черновик' : 'Создать черновик'}
            </Button>
            <Button
              variant="secondary"
              onClick={() => generateLessonMutation.mutate()}
              disabled={draft === null || isCreating || isGenerating || !selectedModule}
            >
              {isGenerating ? 'Генерируем...' : 'Сгенерировать урок'}
            </Button>
            {draft ? <span className="text-sm font-bold text-[#19a7a0]">Draft {draft.id.slice(0, 8)}</span> : null}
          </div>

          {draft && draft.sources.length > 0 ? (
            <div className="grid gap-2">
              {draft.sources.map((source) => (
                <div
                  key={source.id}
                  className="grid grid-cols-[auto_1fr] items-center gap-3 rounded-lg border border-[#dbe7f3] bg-[#fbfdff] px-3 py-3"
                >
                  <span className="grid h-9 w-9 place-items-center rounded-md bg-[#173b4b] text-xs font-black text-[#ffcf5a]">
                    {source.type === 'TEXT_NOTE' ? 'TXT' : 'FILE'}
                  </span>
                  <span>
                    <span className="block text-sm font-black text-app-fg">
                      {source.type === 'TEXT_NOTE' ? 'Текстовый источник' : source.documentOriginalFileName ?? 'Файл-источник'}
                    </span>
                    <span className="text-xs text-app-muted">
                      {source.textContent
                        ? `${source.textContent.length} символов передано модели как текст`
                        : 'Файл будет передан модели как вложенный source'}
                    </span>
                  </span>
                </div>
              ))}
            </div>
          ) : null}

          {createError || generateError ? (
            <div className="rounded-lg border border-[#ffd0c5] bg-[#fff4f1] px-4 py-3 text-sm font-bold text-[#a33d27]">
              {createError ?? generateError}
            </div>
          ) : null}

          {generatedLesson ? (
            <div className="overflow-hidden rounded-lg border border-[#dbe7f3] bg-[#111925]">
              <div className="flex items-center justify-between border-b border-white/10 px-4 py-3 text-white">
                <span className="text-sm font-black">Raw JSON</span>
                <span className="text-xs text-[#b8c7d7]">{generatedLesson.title}</span>
              </div>
              <pre className="max-h-[520px] overflow-auto p-4 text-xs leading-5 text-[#e8f4ff]">
                {stringifyJson(generatedLesson.content)}
              </pre>
            </div>
          ) : null}
        </div>
      </div>
    </section>
  )
}
