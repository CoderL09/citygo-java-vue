/** 后端统一响应：code=0 表示成功 */
export interface ApiResponse<T = unknown> {
  code: number
  message: string
  data: T
}

/** 统一分页：page 从 1 开始，pageSize ≤ 100 */
export interface PageResult<T> {
  items: T[]
  total: number
  page: number
  pageSize: number
}

/** 对外用户视图（后端绝不下发密码摘要） */
export interface UserView {
  id: number
  username: string
  nickname: string | null
  phone: string | null
  role: 'USER' | 'OPERATOR' | 'ADMIN'
  enabled: boolean
}

export interface LoginResponse {
  token: string
  expiresAt: string
  user: UserView
}

export interface CategoryView {
  id: number
  categoryType: 'ACTIVITY' | 'PACKAGE'
  name: string
  sortOrder: number
  status: boolean
}

export interface ActivityView {
  id: number
  categoryId: number
  categoryName: string | null
  name: string
  description: string | null
  address: string
  status: 'ON_SALE' | 'OFF_SALE'
}

export interface SessionView {
  id: number
  activityId: number
  startTime: string
  endTime: string
  bookingDeadline: string
  priceCents: number
  capacity: number
  available: number
  status: 'ON_SALE' | 'OFF_SALE'
}

export interface ActivityImageView {
  id: number
  activityId: number
  fileObjectId: number
  url: string
  sortOrder: number
}

export interface ActivityDetailView extends ActivityView {
  images: ActivityImageView[]
  sessions: SessionView[]
}

export interface FileView {
  id: number
  url: string
  originalName: string
  contentType: string
  sizeBytes: number
}

export interface AcceptingOrdersView {
  acceptingOrders: boolean
}
