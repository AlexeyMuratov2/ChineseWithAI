export type LessonModule = {
  moduleKey: string
  displayName: string
  schemaVersion: number
  active: boolean
}

export type Lesson = {
  id: string
  moduleKey: string | null
  sourceDraftId: string | null
  generatorSessionId: string | null
  title: string
  studyLanguage: string
  explanationLanguage: string
  translationLanguage: string
  content: unknown
  createdAt: string
  updatedAt: string
  version: number
}

export type LessonDraftSourceType = 'TEXT_NOTE' | 'DOCUMENT_FILE'

export type LessonDraftSource = {
  id: string
  type: LessonDraftSourceType
  position: number
  textContent: string | null
  documentFileId: string | null
  documentOriginalFileName: string | null
  createdAt: string
  updatedAt: string
}

export type LessonDraft = {
  id: string
  title: string
  description: string | null
  userInstructions: string | null
  explanationLanguage: string
  translationLanguage: string
  sources: LessonDraftSource[]
  createdAt: string
  updatedAt: string
  version: number
}

export type CreateLessonDraftPayload = {
  title: string
  description?: string
  userInstructions?: string
  explanationLanguage?: string
  translationLanguage?: string
}

export type AddLessonDraftSourcePayload = {
  type: LessonDraftSourceType
  textContent?: string
  documentFileId?: string
  documentOriginalFileName?: string
}

export type GenerateLessonPayload = {
  draftId: string
  moduleKey: string
  modelKey?: string
}

export type CreateUploadSessionPayload = {
  scenario: 'GENERIC_UPLOAD' | 'LESSON_SOURCE'
  expectedContentLength: number
  declaredContentType?: string
  originalFileName?: string
}

export type CreateUploadSessionResponse = {
  sessionId: string
}

export type StoredFileMetadata = {
  id: string
  sizeBytes: number
  contentType: string | null
  originalFileName: string | null
  createdAt: string
}
