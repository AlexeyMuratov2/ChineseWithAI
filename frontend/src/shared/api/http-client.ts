import { env } from '@/shared/config/env'

export type HttpRequestConfig = {
  method?: 'GET' | 'POST' | 'PUT' | 'PATCH' | 'DELETE'
  headers?: Record<string, string>
  body?: unknown
}

export class HttpError extends Error {
  readonly status: number
  readonly payload: unknown

  constructor(status: number, message: string, payload: unknown) {
    super(message)
    this.name = 'HttpError'
    this.status = status
    this.payload = payload
  }
}

const isRecord = (value: unknown): value is Record<string, unknown> => {
  return typeof value === 'object' && value !== null
}

const parseResponsePayload = async (response: Response) => {
  if (response.status === 204) {
    return null
  }

  const contentType = response.headers.get('content-type') ?? ''

  if (contentType.includes('application/json')) {
    return await response.json()
  }

  return await response.text()
}

export const httpClient = async <T>(path: string, config: HttpRequestConfig = {}) => {
  const response = await fetch(`${env.apiBaseUrl}${path}`, {
    method: config.method ?? 'GET',
    headers: {
      'Content-Type': 'application/json',
      ...config.headers,
    },
    body: config.body ? JSON.stringify(config.body) : undefined,
  })

  const payload = await parseResponsePayload(response)

  if (!response.ok) {
    let message = `HTTP error: ${response.status}`

    if (isRecord(payload) && typeof payload.detail === 'string') {
      message = payload.detail
    } else if (typeof payload === 'string' && payload.length > 0) {
      message = payload
    }

    throw new HttpError(response.status, message, payload)
  }

  return payload as T
}
