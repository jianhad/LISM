import { http } from './http'

export interface LoginResponse {
  accessToken: string
  tokenType: string
  expiresAt: string
}

export interface CurrentUser {
  username: string
  permissions: string[]
}

export const login = (username: string, password: string) =>
  http.post<LoginResponse>('/auth/login', { username, password }).then((response) => response.data)

export const getCurrentUser = () =>
  http.get<CurrentUser>('/auth/me').then((response) => response.data)

