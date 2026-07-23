import axios from './axios';

/** 申请村长资质（需已为正式农户） */
export const applyManager = (data: { farmName: string; idCard: string }) => {
  return axios.post('/farmer/apply_manager', data);
};

/** 申请农户资质 */
export const applyFarmer = (data: {
  villageName: string;
  farmName: string;
  idCard: string;
  type: number;
}) => {
  return axios.post('/farmer/apply_farmer', data);
};

/** 本村农户列表（村长） */
export const getFarmerList = () => {
  return axios.get('/farmer/list');
};

/** 本村新增农户（村长） */
export const addFarmerToVillage = (villageId: number, data: Record<string, unknown>) => {
  return axios.post('/farmer', data, { params: { villageId } });
};

/** 修改本村农户（村长），path 为关联 user 表的用户 id */
export const updateFarmerInVillage = (userId: number, data: Record<string, unknown>) => {
  return axios.put(`/farmer/${userId}`, data);
};

/** 移除本村农户（村长），path 为关联 user 表的用户 id */
export const deleteFarmerFromVillage = (userId: number) => {
  return axios.delete(`/farmer/${userId}`);
};
