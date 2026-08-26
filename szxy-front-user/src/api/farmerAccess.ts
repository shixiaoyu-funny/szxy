import axios from './axios';

/** 提交/改提农户申请 */
export const applyFarmerAccess = (data: {
  idCard: string;
  villageId: number;
  businessType: number;
}) => axios.post('/fmr/access/apply', data);

/** 本人最新申请 */
export const getMyFarmerAccess = () => axios.get('/fmr/access/mine');
