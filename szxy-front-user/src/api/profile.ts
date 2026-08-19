import axios from './axios';
/**
 * 获取用户历史点赞景点
 * @returns ScenicVO[]
 */
export const getLikes = () => {
    return axios.get('/ur/like')
}
/**
 * 获取用户历史收藏景点
 * @returns ScenicVO[]
 */
export const getCollects = () => {
    return axios.get('/ur/collection')
}
/**
 * 获取用户历史评论景点
 * @returns ScenicVO[]
 */
export const getComments = () => {
    return axios.get('/ur/comment')
}
