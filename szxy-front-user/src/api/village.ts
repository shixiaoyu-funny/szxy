import axios from './axios';

// 分页农村信息（公开）
export const getVillageList = (pageNo?: number, pageSize?: number) => {
  return axios.get('/vlg/ls', {
    params: { pageNo, pageSize }
  });
};

// 优质农村 top10（按下属景点点赞）
export const getTopVillageByLikes = () => {
  return axios.get('/vlg/likes');
};

// 优质农村 top10（按下属景点收藏）
export const getTopVillageByCollections = () => {
  return axios.get('/vlg/collections');
};

// 农村详情
export const getVillageDetail = (id: number) => {
  return axios.get('/vlg/detail', { params: { id } });
};
