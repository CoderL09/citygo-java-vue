import { get, post, put } from './http'
import type { LoginResponse, UserView } from './types'

export function login(username: string, password: string) {
  return post<LoginResponse>('/auth/login', { username, password })
}

export function register(payload: {
  username: string
  password: string
  nickname?: string
  phone?: string
}) {
  return post<LoginResponse>('/auth/register', payload)
}

export function logout() {
  return post<void>('/auth/logout')
}

export function fetchMe() {
  return get<UserView>('/auth/me')
}

export function updateProfile(payload: { nickname?: string; phone?: string }) {
  return put<UserView>('/user/profile', payload)
}
