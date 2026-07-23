import axios from './axios';

// 查看优质景点信息TOP10
export const getTopScenic = () => {
  return axios.get('/scenic/scenic');
};

// 查看景点详情
export const getScenicDetail = (id: number) => {
  return axios.get(`/scenic/detail?id=${id}`);
};

// 查看景点评论
export const getScComments = (id: number) => {
  return axios.get('/scenic/sc_comments', {
    params: { id }
  });
};