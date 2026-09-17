import axios from './axios';

// 优质景点 TOP10（按点赞）
export const getTopScenic = () => {
  return axios.get('/sc/top10');
};

// 景点详情
export const getScenicDetail = (id: number) => {
  return axios.get(`/sc/detail?id=${id}`);
};

// 景点评论
export const getScComments = (id: number) => {
  return axios.get('/sc/sccomments', {
    params: { id }
  });
};

// 农户/村长在所属村直接新增景点
export const registerScenic = (data: any) => {
  return axios.post('/sc/rg', data);
};
