/**
 * 登录凭证存取（localStorage）集中管理：
 * request.ts 拦截器、user store、路由守卫共用，避免 key 散落
 */

export interface CredentialPayload {
  accessToken: string
  refreshToken: string
  username: string
  role: string
}

const KEYS = {
  access: 'access_token',
  refresh: 'refresh_token',
  username: 'username',
  role: 'role',
} as const

export function getAccessToken(): string {
  return localStorage.getItem(KEYS.access) ?? ''
}

export function getRefreshToken(): string {
  return localStorage.getItem(KEYS.refresh) ?? ''
}

export function getRole(): string {
  return localStorage.getItem(KEYS.role) ?? ''
}

export function saveCredentials(payload: CredentialPayload): void {
  localStorage.setItem(KEYS.access, payload.accessToken ?? '')
  localStorage.setItem(KEYS.refresh, payload.refreshToken ?? '')
  localStorage.setItem(KEYS.username, payload.username ?? '')
  localStorage.setItem(KEYS.role, payload.role ?? '')
}

export function clearCredentials(): void {
  for (const key of Object.values(KEYS)) {
    localStorage.removeItem(key)
  }
}
