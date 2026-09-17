import axios from './axios';

// 本村农户列表（村长）
export const getVillageFarmers = () => {
  return axios.get('/fmr/ls');
};

// 村长新增本村农户
export const addVillageFarmer = (villageId: number, data: any) => {
  return axios.post('/fmr/new', data, {
    params: { villageId }
  });
};

// 村长修改本村农户
export const updateVillageFarmer = (id: number, data: any) => {
  return axios.post(`/fmr/modify/${id}`, data);
};

// 村长删除本村农户
export const deleteVillageFarmer = (id: number) => {
  return axios.post(`/fmr/remote/${id}`);
};

// 我的村（农户/村长）
export const getMyVillage = () => {
  return axios.get('/fmr/vlg');
};

// 我的景点（农户/村长）
export const getMyScenics = () => {
  return axios.get('/fmr/sc');
};
