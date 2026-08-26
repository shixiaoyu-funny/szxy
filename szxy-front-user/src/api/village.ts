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

// 管理端：新增农村
export const adminAddVillage = (data: any) => {
  return axios.post('/vlg/new', data);
};

// 管理端：修改农村
export const adminUpdateVillage = (id: number, data: any) => {
  return axios.post(`/vlg/modify/${id}`, data);
};

// 管理端：删除农村
export const adminDeleteVillage = (id: number) => {
  return axios.post(`/vlg/del/${id}`);
};
