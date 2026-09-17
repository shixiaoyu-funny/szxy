import axios from './axios'

export interface OrderSubmitBody {
  fromCart?: boolean
  productId?: number
  quantity?: number
  addressId?: number | null
  remark?: string
}

/** 确认页预览 */
export const previewOrder = (data: OrderSubmitBody) => axios.post('/order/preview', data)

/** 生成待支付订单 */
export const createOrder = (data: OrderSubmitBody) => axios.post('/order/add', data)

/** 改待支付订单 */
export const alterOrder = (data: { id: number; addressId?: number | null; remark?: string }) =>
  axios.post('/order/alter', data)

/** 买家订单分页 */
export const listOrders = (pageNo = 1, pageSize = 10, status?: number) =>
  axios.get('/order/ls', { params: { pageNo, pageSize, status } })

/** 订单详情 */
export const getOrderDetail = (id: number) => axios.get(`/order/${id}`)

export const cancelOrder = (id: number) => axios.post(`/order/cancel/${id}`)

export const applyRefund = (id: number, reason: string) =>
  axios.post(`/order/refund/${id}`, { reason })

export const confirmReceipt = (id: number) => axios.post(`/order/confirm/${id}`)

export const useOrder = (id: number) => axios.post(`/order/use/${id}`)

export const softDeleteOrder = (id: number) => axios.post(`/order/del/${id}`)

/** 卖家订单 */
export const listSellerOrders = (pageNo = 1, pageSize = 10, status?: number) =>
  axios.get('/order/seller/ls', { params: { pageNo, pageSize, status } })

export const sellerShip = (id: number) => axios.post(`/order/seller/ship/${id}`)

export const sellerHandleRefund = (id: number, handle: number, rejectReason?: string) =>
  axios.post(`/order/seller/refund/${id}`, { handle, rejectReason })
