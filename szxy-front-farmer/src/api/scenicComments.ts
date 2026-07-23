import axios from './axios';

/** 景点评论列表（与用户端一致：GET /scenic/sc_comments?id=） */
export const getScComments = (id: number) => {
  return axios.get('/scenic/sc_comments', { params: { id } });
};
