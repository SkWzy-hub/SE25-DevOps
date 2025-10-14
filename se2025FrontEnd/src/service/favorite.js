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

// 检查商品是否被收藏
export const checkFavoriteStatus = async (itemId) => {
  console.log('Checking favorite status for item:', itemId);
  try {
    const response = await axios.get(`/api/items/${itemId}/favorite/check`);
    console.log('Favorite status response:', response.data);
    // 新的API返回ApiResponse格式: { success: true, data: boolean, message: string }
    return response.data.success ? response.data.data : false;
  } catch (error) {
    console.error('检查收藏状态失败', error.response || error);
    return false;
  }
};

// 获取商品收藏数量
export const getFavoriteCount = async (itemId) => {
  console.log('Getting favorite count for item:', itemId);
  try {
    const response = await axios.get(`/api/items/${itemId}/favorite/count`);
    console.log('Favorite count response:', response.data);
    return response.data.success ? response.data.data : 0;
  } catch (error) {
    console.error('获取收藏数量失败', error.response || error);
    return 0;
  }
};

// 添加收藏
export const addFavorite = async (itemId) => {
  console.log('Adding favorite for item:', itemId);
  try {
    const response = await axios.post(`/api/items/${itemId}/favorite`);
    console.log('Add favorite response:', response.data);
    return response.data.success;
  } catch (error) {
    console.error('添加收藏失败', error.response || error);
    return false;
  }
};

// 取消收藏
export const removeFavorite = async (itemId) => {
  console.log('Removing favorite for item:', itemId);
  try {
    const response = await axios.delete(`/api/items/${itemId}/favorite`);
    console.log('Remove favorite response:', response.data);
    return response.data.success;
  } catch (error) {
    console.error('取消收藏失败', error.response || error);
    return false;
  }
};

// 获取用户收藏列表
export const getUserFavorites = async () => {
  console.log('Getting user favorites');
  try {
    const response = await axios.get('/api/items/favorites/user');
    console.log('User favorites response:', response.data);
    // 新的API返回ApiResponse格式: { success: true, data: ItemResponseDTO[], message: string }
    return response.data.success ? response.data.data : [];
  } catch (error) {
    console.error('获取收藏列表失败', error.response || error);
    return [];
  }
}; 