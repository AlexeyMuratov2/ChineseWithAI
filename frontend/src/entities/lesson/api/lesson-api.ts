import { httpClient, rawHttpClient } from '@/shared/api/http-client'

import type {
  AddLessonDraftSourcePayload,
  CreateLessonDraftPayload,
  CreateUploadSessionPayload,
  CreateUploadSessionResponse,
  GenerateLessonPayload,
  Lesson,
  LessonDraft,
  LessonModule,
  StoredFileMetadata,
} from '../model/types'

export const lessonApi = {
  listModules: () => httpClient<LessonModule[]>('/api/v1/lessons/modules'),

  listLessonsByModule: (moduleKey: string) =>
    httpClient<Lesson[]>(`/api/v1/lessons/modules/${encodeURIComponent(moduleKey)}`),

  createDraft: (payload: CreateLessonDraftPayload) =>
    httpClient<LessonDraft>('/api/v1/lesson-drafts', {
      method: 'POST',
      body: payload,
    }),

  addDraftSource: (draftId: string, payload: AddLessonDraftSourcePayload) =>
    httpClient<LessonDraft>(`/api/v1/lesson-drafts/${encodeURIComponent(draftId)}/sources`, {
      method: 'POST',
      body: payload,
    }),

  createUploadSession: (payload: CreateUploadSessionPayload) =>
    httpClient<CreateUploadSessionResponse>('/api/v1/stored-files/upload-sessions', {
      method: 'POST',
      body: payload,
    }),

  uploadFileContent: (sessionId: string, file: File) =>
    rawHttpClient<StoredFileMetadata>(
      `/api/v1/stored-files/upload-sessions/${encodeURIComponent(sessionId)}/content`,
      {
        method: 'POST',
        headers: {
          'Content-Type': file.type || 'application/octet-stream',
          'X-Upload-Original-File-Name': file.name,
        },
        body: file,
      },
    ),

  generateLesson: (payload: GenerateLessonPayload) =>
    httpClient<Lesson>('/api/v1/lessons/generate', {
      method: 'POST',
      body: payload,
    }),
}
