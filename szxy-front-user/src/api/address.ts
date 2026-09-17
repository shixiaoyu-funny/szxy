import axios from './axios'

/** 地址列表 */
export const listAddress = () => axios.get('/address/ls')

/** 新增地址 */
export const addAddress = (data: {
  receiverName: string
  phone: string
  province: string
  city: string
  county: string
  detail: string
  isDefault: number
}) => axios.post('/address/add', data)

/** 修改地址（须带 id、isDefault 0/1） */
export const alterAddress = (data: {
  id: number
  receiverName: string
  phone: string
  province: string
  city: string
  county: string
  detail: string
  isDefault: number
}) => axios.post('/address/alter', data)

/** 设默认 */
export const setDefaultAddress = (id: number) => axios.post(`/address/default/${id}`)

/** 删除非默认 */
export const delAddress = (id: number) => axios.post(`/address/del/${id}`)
