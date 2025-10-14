import axios from 'axios';

// 配置axios拦截器，自动添加JWT token
axios.interceptors.request.use(
  (config) => {
    const token = localStorage.getItem('token');
    if (token) {
      config.headers.Authorization = `Bearer ${token}`;
    }
    return config;
  },
  (error) => {
    return Promise.reject(error);
  }
);

// 获取商品的所有根留言
export const getItemMessages = async (itemId) => {
  try {
    const response = await axios.get(`/api/messages/item/${itemId}`);
    const apiResponse = response.data;
    return apiResponse.success ? apiResponse.data : [];
  } catch (error) {
    console.error('获取留言失败:', error.response || error);
    return [];
  }
};

// 获取留言的所有回复
export const getMessageReplies = async (messageId) => {
  try {
    const response = await axios.get(`/api/messages/${messageId}/replies`);
    const apiResponse = response.data;
    return apiResponse.success ? apiResponse.data : [];
  } catch (error) {
    console.error('获取回复失败:', error.response || error);
    return [];
  }
};

// 添加留言或回复
export const addMessage = async (itemId, content, parentId = null) => {
  try {
    const requestData = {
      itemId: parseInt(itemId),
      content: content,
      parentId: parentId || 0
    };
    
    const response = await axios.post('/api/messages', requestData, {
      headers: {
        'Content-Type': 'application/json'
      }
    });
    
    const apiResponse = response.data;
    return apiResponse.success ? apiResponse.data : null;
  } catch (error) {
    console.error('添加留言失败:', error.response || error);
    return null;
  }
};

// 删除留言
export const deleteMessage = async (messageId) => {
  try {
    const response = await axios.delete(`/api/messages/${messageId}`);
    const apiResponse = response.data;
    return apiResponse.success;
  } catch (error) {
    console.error('删除留言失败:', error.response || error);
    return false;
  }
};

// 获取商品的留言数量
export const getMessageCount = async (itemId) => {
  try {
    const response = await axios.get(`/api/messages/item/${itemId}/count`);
    const apiResponse = response.data;
    return apiResponse.success ? apiResponse.data : 0;
  } catch (error) {
    console.error('获取留言数量失败:', error.response || error);
    return 0;
  }
}; 