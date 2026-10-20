import { get, post, put, del } from './http'
import type { PageResult, SessionView } from './types'
export type ProductType = 'SESSION' | 'PACKAGE'
export interface CheckoutLine { itemType: ProductType; itemId: number; quantity: number }
export interface CheckoutRequest { contactId: number; items: CheckoutLine[]; remark: string }
export interface SessionSnapshot { sessionId: number; activityId: number; activityName: string; address: string; startTime: string; endTime: string; bookingDeadline: string; priceCents: number }
export interface Quote { itemType: ProductType; itemId: number; name: string; unitPriceCents: number; packageVersion: number | null; sessions: SessionSnapshot[]; available: number }
export interface Contact { id: number; name: string; phone: string; defaultContact: boolean }
export interface ContactRequest { name: string; phone: string; defaultContact: boolean }
export interface CartRow { item: CheckoutLine & { id: number; selected: boolean }; product: Quote | null; valid: boolean; invalidReason: string | null }
export interface Package { id: number; categoryId: number; name: string; description: string; packagePriceCents: number; version: number; familyId: number; status: 'ON_SALE' | 'OFF_SALE' }
export interface PackageDetail { product: Package; sessions: SessionView[]; composition: SessionSnapshot[] }
export interface PackageRequest { categoryId: number; name: string; description: string; packagePriceCents: number; sessionIds: number[] }
export interface Preview { totalCents: number; items: { request: CheckoutLine; product: Quote; lineTotalCents: number }[] }
export interface OrderSummary { id: number; orderNo: string; totalCents: number; status: string; contactName: string; contactPhone: string; remark: string; expireAt: string; createdAt: string }
export interface OrderItem { id: number; itemType: ProductType; itemId: number; name: string; quantity: number; unitPriceCents: number; lineTotalCents: number; sessionSnapshot: SessionSnapshot | null; packageSnapshot: Quote | null }
export interface OrderDetail { order: OrderSummary; items: OrderItem[] }
export const listContacts = () => get<Contact[]>('/contacts')
export const saveContact = (id: number | undefined, payload: ContactRequest) => id ? put<Contact>(`/contacts/${id}`, payload) : post<Contact>('/contacts', payload)
export const deleteContact = (id: number) => del<void>(`/contacts/${id}`)
export const getCart = () => get<CartRow[]>('/cart')
export const addCartItem = (payload: CheckoutLine) => post<void>('/cart', payload)
export const updateCartItem = (id: number, quantity: number, selected: boolean) => put<void>(`/cart/${id}`, { quantity, selected })
export const deleteCartItem = (id: number) => del<void>(`/cart/${id}`)
export const clearCart = () => del<void>('/cart')
export const previewOrder = (payload: CheckoutRequest) => post<Preview>('/orders/preview', payload)
export const createOrder = (payload: CheckoutRequest, key: string) => post<OrderDetail>('/orders', payload, { headers: { 'Idempotency-Key': key } })
export const listOrders = (page: number) => get<PageResult<OrderSummary>>('/orders', { params: { page, pageSize: 10 } })
export const getOrder = (id: number) => get<OrderDetail>(`/orders/${id}`)
export const listPackages = (admin: boolean, page: number, keyword?: string, categoryId?: number) => get<PageResult<Package>>(admin ? '/admin/packages' : '/packages', { params: { page, pageSize: 10, keyword: keyword || undefined, categoryId } })
export const getPackage = (id: number, admin = false) => get<PackageDetail>(`/${admin ? 'admin/' : ''}packages/${id}`)
export const savePackage = (payload: PackageRequest, source?: number) => post<PackageDetail>(source ? `/admin/packages/${source}/versions` : '/admin/packages', payload)
export const setPackageStatus = (id: number, status: 'ON_SALE' | 'OFF_SALE') => put<PackageDetail>(`/admin/packages/${id}/status`, { status })
