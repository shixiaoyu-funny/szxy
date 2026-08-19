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

// 管理端：分页全部景点
export const adminListScenic = (pageNo: number, pageSize: number) => {
  return axios.get('/sc/ls', {
    params: { page_no: pageNo, page_size: pageSize }
  });
};

// 管理端：直接新增景点
export const adminAddScenic = (villageId: number, data: any) => {
  return axios.post('/sc/new', data, {
    params: { village_id: villageId }
  });
};

// 管理端：修改景点
export const adminUpdateScenic = (id: number, data: any) => {
  return axios.post(`/sc/modify/${id}`, data);
};

// 管理端：删除景点
export const adminDeleteScenic = (id: number) => {
  return axios.post(`/sc/del/${id}`);
};
