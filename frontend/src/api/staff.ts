import { get, post, put } from './http'
import type { PageResult, UserView } from './types'

export function pageStaff(params: { keyword?: string; page: number; pageSize: number }) {
  return get<PageResult<UserView>>('/admin/staff', { params })
}

export function createStaff(payload: {
  username: string
  password: string
  nickname?: string
  role: 'OPERATOR' | 'ADMIN'
}) {
  return post<UserView>('/admin/staff', payload)
}

export function updateStaff(id: number, payload: { nickname?: string; role: 'OPERATOR' | 'ADMIN' }) {
  return put<UserView>(`/admin/staff/${id}`, payload)
}

export function setStaffEnabled(id: number, enabled: boolean) {
  return put<UserView>(`/admin/staff/${id}/enabled`, { enabled })
}

export function resetStaffPassword(id: number, password: string) {
  return put<void>(`/admin/staff/${id}/password`, { password })
}
