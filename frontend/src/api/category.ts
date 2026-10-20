import { del, get, post, put } from './http'
import type { CategoryView, PageResult } from './types'

export function pagePublicCategories(params: { type?: string; page: number; pageSize: number }) {
  return get<PageResult<CategoryView>>('/categories', { params })
}

export function pageAdminCategories(params: {
  type?: string
  status?: boolean
  keyword?: string
  page: number
  pageSize: number
}) {
  return get<PageResult<CategoryView>>('/admin/categories', { params })
}

export function createCategory(payload: { categoryType: string; name: string; sortOrder?: number }) {
  return post<CategoryView>('/admin/categories', payload)
}

export function updateCategory(id: number, payload: { name: string; sortOrder?: number }) {
  return put<CategoryView>(`/admin/categories/${id}`, payload)
}

export function setCategoryStatus(id: number, status: boolean) {
  return put<CategoryView>(`/admin/categories/${id}/status`, { status })
}

export function deleteCategory(id: number) {
  return del<void>(`/admin/categories/${id}`)
}
