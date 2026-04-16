const trimTrailingSlashes = (value: string) => value.replace(/\/+$/, '')

const requiredEnv = {
  appName: import.meta.env.VITE_APP_NAME ?? 'ChineseWithAI',
  apiBaseUrl: trimTrailingSlashes(import.meta.env.VITE_API_BASE_URL ?? ''),
}

export const env = Object.freeze(requiredEnv)
