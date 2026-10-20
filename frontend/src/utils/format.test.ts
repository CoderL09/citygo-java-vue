import { describe, expect, it } from 'vitest'
import { formatDateTime, formatPrice, toUtcDateTime, toShanghaiInput } from './format'

describe('展示和编辑场次时间', () => {
  it('上海日期跨 UTC 自然日仍正确', () => {
    expect(toUtcDateTime('2027-01-01T01:30:00')).toBe('2026-12-31T17:30:00.000Z')
    expect(formatDateTime('2026-12-31T17:30:00Z')).toBe('2027-01-01 01:30:00')
  })
  it('编辑后保存保持同一时刻', () => {
    const iso = '2027-01-01T02:00:00.000Z'
    expect(toUtcDateTime(toShanghaiInput(iso))).toBe(iso)
  })
  it('空时间和无效时间有占位', () => {
    expect(formatDateTime(null)).toBe('-')
    expect(formatDateTime('bad')).toBe('-')
  })
  it('金额以分计价显示两位小数', () => {
    expect(formatPrice(9901)).toBe('99.01')
    expect(formatPrice(0)).toBe('0.00')
  })
})
