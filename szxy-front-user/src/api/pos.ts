import axios from './axios'

/** 登录用户上报当前定位（省市区），后端 Redis 比对变更 */
export const reportPos = (data: { province: string; city: string; county: string }) =>
  axios.post('/pos/report', data)
