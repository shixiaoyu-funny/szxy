import axios from './axios';

// AI智能问答
export const aiInquire = (content: string) => {
  return axios.post('/common/ai/inquire', null,
    {
      params: { content }
    }
  );
};
// AI记忆
export const memory = () => {
  return axios.get('/common/ai/memory');
}

// 多模态识别农产品
export const aiMultimodal = (imageUrl: string) => {
  return axios.post('/common/ai/multimodal', null, {
    params: { imageUrl }
 });
 };
