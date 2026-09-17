import axios from './axios'

/** 购物车列表 */
export const listCart = () => axios.get('/cart/ls')

/** 加购 */
export const addCart = (data: { productId: number; quantity?: number }) =>
  axios.post('/cart/add', data)

/** 改数量/勾选 */
export const alterCart = (id: number, data: { quantity?: number; selected?: number }) =>
  axios.post(`/cart/alter/${id}`, data)

/** 删单项 */
export const delCart = (id: number) => axios.post(`/cart/del/${id}`)

/** 清空 */
export const clearCart = () => axios.post('/cart/clear')
