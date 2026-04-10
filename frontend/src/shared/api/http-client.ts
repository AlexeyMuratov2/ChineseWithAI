import { env } from '@/shared/config/env'

export type HttpRequestConfig = {
  method?: 'GET' | 'POST' | 'PUT' | 'PATCH' | 'DELETE'
  headers?: Record<string, string>
  body?: unknown
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

  if (!response.ok) {
    throw new Error(`HTTP error: ${response.status}`)
  }

  return (await response.json()) as T
}
