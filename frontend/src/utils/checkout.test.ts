import { expect, it } from 'vitest'
import { CheckoutAttempt } from './checkout'
import type { CheckoutRequest } from '@/api/booking'
it('未知网络结果重试保留键，购物车顺序不影响同一请求', () => {
  let n = 0
  const attempt = new CheckoutAttempt(() => 'key-' + ++n)
  const request: CheckoutRequest = { contactId: 1, remark: '', items: [{ itemType: 'SESSION', itemId: 2, quantity: 1 }, { itemType: 'PACKAGE', itemId: 3, quantity: 1 }] }
  const key = attempt.forRequest(request)
  expect(attempt.forRequest({ ...request, items: [...request.items].reverse() })).toBe(key)
  expect(attempt.forRequest({ ...request, contactId: 2 })).not.toBe(key)
})
it('数量改变或成功后再次购买使用新键', () => {
  let n = 0
  const attempt = new CheckoutAttempt(() => 'key-' + ++n)
  const request: CheckoutRequest = { contactId: 1, remark: '', items: [{ itemType: 'SESSION', itemId: 2, quantity: 1 }] }
  const original = attempt.forRequest(request)
  const changed = attempt.forRequest({ ...request, items: [{ ...request.items[0], quantity: 2 }] })
  expect(changed).not.toBe(original)
  attempt.complete()
  expect(attempt.forRequest(request)).not.toBe(original)
})
