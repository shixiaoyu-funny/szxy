/** 订单状态文案（与后端 OrderStatusEnum code 对齐） */
export const ORDER_STATUS_TEXT: Record<number, string> = {
  1: '待支付',
  2: '已取消',
  3: '待使用',
  4: '待发货',
  5: '待收货',
  6: '待签收',
  7: '已签收',
  8: '已使用',
  9: '退款中',
  10: '已退款'
}

export const FULFILLMENT_TEXT: Record<number, string> = {
  1: '虚拟核销',
  2: '实物物流'
}

export const PRODUCT_TYPE_TEXT: Record<number, string> = {
  1: '实体商品',
  2: '门票核销',
  3: '住宿核销'
}

export function orderStatusText(status: number | undefined | null): string {
  if (status == null) return ''
  return ORDER_STATUS_TEXT[status] ?? `状态${status}`
}

export function formatMoney(amount: number | string | null | undefined): string {
  if (amount == null || amount === '') return '¥0.00'
  const n = Number(amount)
  if (Number.isNaN(n)) return '¥0.00'
  return `¥${n.toFixed(2)}`
}
