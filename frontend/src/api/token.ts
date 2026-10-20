/**
 * 内存 token 持有器（实施书 §4：前端内存保存，刷新需重新登录）。
 * 独立小模块避免 http ↔ store 循环依赖。
 */
let token: string | null = null

export function getToken(): string | null {
  return token
}

export function setToken(value: string | null) {
  token = value
}
