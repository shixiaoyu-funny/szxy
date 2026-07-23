import axios from './axios';
/**
 * 获取用户历史点赞列表
 * @param id 
 * @returns 
 */
export const getLikes = () => {
    return axios.get('/user/like')
}
/**
 * 获取用户历史收藏列表
 * @param id 
 * @returns 
 */
export const getCollects = () => {
    return axios.get('/user/collection')
}
/**
 * 获取用户历史评论列表
 * @param id 
 * @returns 
 */
export const getComments = () => {
    return axios.get('/user/comment')
}
