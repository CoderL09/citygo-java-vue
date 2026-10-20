export function statusText(status: string): string {
  return ({ PENDING_PAYMENT:'待支付',PENDING_CONFIRMATION:'待确认',CONFIRMED:'已确认',COMPLETED:'已完成',CANCELLED:'已取消',CLOSED:'已关闭',REFUNDING:'退款中',REFUNDED:'已退款' } as Record<string,string>)[status] || status
}
