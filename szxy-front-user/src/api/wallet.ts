import axios from './axios'

/** 余额 */
export const getBalance = () => axios.get('/wallet/balance')

/** 流水双分页 */
export const getWalletRecords = (pageNo = 1, pageSize = 10) =>
  axios.get('/wallet/records', { params: { pageNo, pageSize } })

/** 模拟充值 */
export const recharge = (amount: number) => axios.post('/wallet/recharge', { amount })

/** 余额支付订单 */
export const payOrder = (orderId: number) => axios.post(`/wallet/pay/${orderId}`)
