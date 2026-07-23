import axios from './axios';

// 文件上传
export const uploadFile = (file: File) => {
  const formData = new FormData();
  formData.append('file', file);
  return axios.post('/upload', formData, {
    headers: {
      'Content-Type': 'multipart/form-data'
    }
  });
};