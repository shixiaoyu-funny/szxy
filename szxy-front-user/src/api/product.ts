import axios from './axios'

/** 景点下上架商品 */
export const listProductsByScenic = (scenicId: number) =>
  axios.get(`/product/scenic/${scenicId}`)
