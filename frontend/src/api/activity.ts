import { del, get, post, put } from './http'
import type { ActivityDetailView, ActivityImageView, ActivityView, PageResult, SessionView } from './types'

export function pagePublicActivities(params: {
  categoryId?: number
  keyword?: string
  page: number
  pageSize: number
}) {
  return get<PageResult<ActivityView>>('/activities', { params })
}

export function getPublicActivity(id: number) {
  return get<ActivityDetailView>(`/activities/${id}`)
}

export function getAdminActivity(id: number) {
  return get<ActivityDetailView>(`/admin/activities/${id}`)
}

export function pageAdminActivities(params: {
  categoryId?: number
  status?: string
  keyword?: string
  page: number
  pageSize: number
}) {
  return get<PageResult<ActivityView>>('/admin/activities', { params })
}

export function createActivity(payload: {
  categoryId: number
  name: string
  description?: string
  address: string
}) {
  return post<ActivityView>('/admin/activities', payload)
}

export function updateActivity(
  id: number,
  payload: { categoryId: number; name: string; description?: string; address: string },
) {
  return put<ActivityView>(`/admin/activities/${id}`, payload)
}

export function setActivityStatus(id: number, status: 'ON_SALE' | 'OFF_SALE') {
  return put<ActivityView>(`/admin/activities/${id}/status`, { status })
}

export function deleteActivity(id: number) {
  return del<void>(`/admin/activities/${id}`)
}

export function listAdminSessions(activityId: number) {
  return get<SessionView[]>(`/admin/activities/${activityId}/sessions`)
}

export function createSession(
  activityId: number,
  payload: {
    startTime: string
    endTime: string
    bookingDeadline: string
    priceCents: number
    capacity: number
  },
) {
  return post<SessionView>(`/admin/activities/${activityId}/sessions`, payload)
}

export function updateSession(id: number, payload: {
  startTime: string
  endTime: string
  bookingDeadline: string
  priceCents: number
  capacity: number
}) {
  return put<SessionView>(`/admin/sessions/${id}`, payload)
}

export function setSessionStatus(id: number, status: 'ON_SALE' | 'OFF_SALE') {
  return put<SessionView>(`/admin/sessions/${id}/status`, { status })
}

export function deleteSession(id: number) {
  return del<void>(`/admin/sessions/${id}`)
}

export function addActivityImage(activityId: number, fileObjectId: number, sortOrder: number) {
  return post<ActivityImageView>(`/admin/activities/${activityId}/images`, { fileObjectId, sortOrder })
}

export function updateImageSort(imageId: number, sortOrder: number) {
  return put<void>(`/admin/activity-images/${imageId}/sort`, { sortOrder })
}

export function deleteActivityImage(imageId: number) {
  return del<void>(`/admin/activity-images/${imageId}`)
}
