/** 金额：整数分 → 元字符串 */
export function formatPrice(cents: number): string {
  return (cents / 100).toFixed(2)
}

/** 场次按上海时间展示，不依赖浏览器所在时区。 */
export function formatDateTime(iso: string | null | undefined): string {
  if (!iso) return '-'
  const date = new Date(iso)
  if (Number.isNaN(date.getTime())) return '-'
  return new Intl.DateTimeFormat('sv-SE', {
    timeZone: 'Asia/Shanghai', year: 'numeric', month: '2-digit', day: '2-digit',
    hour: '2-digit', minute: '2-digit', second: '2-digit', hourCycle: 'h23',
  }).format(date)
}

/** 日期选择器输入上海墙上时间；传输时转换成显式 UTC。 */
export function toUtcDateTime(value: string): string {
  return new Date(value + '+08:00').toISOString()
}

export function toShanghaiInput(iso: string): string {
  return formatDateTime(iso).replace(' ', 'T')
}
