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

// 获取商品详情
export const getItemDetail = async (itemId) => {
  console.log('Fetching item details for ID:', itemId);
  try {
    const response = await axios.get(`/api/items/${itemId}`);
    console.log('Item details response:', response);
    
    // 处理ApiResponse包装
    const apiResponse = response.data;
    if (!apiResponse || typeof apiResponse.code === 'undefined') {
      console.error('Invalid API response format:', apiResponse);
      throw new Error('服务器响应格式不正确');
    }

    if (apiResponse.code !== 200) {
      console.error('API error:', apiResponse.message);
      throw new Error(apiResponse.message || '获取商品详情失败');
    }

    const itemData = apiResponse.data;
    if (!itemData || !itemData.itemId) {
      console.error('Invalid item data received:', itemData);
      throw new Error('商品数据格式不正确');
    }
    
    return itemData;
  } catch (error) {
    console.error('获取商品详情失败:', error.response || error);
    if (error.response?.status === 404) {
      return null;
    }
    throw error;
  }
};

// 获取商品列表
export const getItems = async (params = {}) => {
  try {
    const response = await axios.get('/api/items', { params });
    const apiResponse = response.data;
    return apiResponse.success ? apiResponse.data : [];
  } catch (error) {
    console.error('获取商品列表失败:', error.response || error);
    return [];
  }
};

// 分页获取商品列表（用于首页）
export const getItemsPage = async (pageParams = {}) => {
  try {
    const {
      page = 0,
      size = 20,
      sortBy = 'update_time',
      sortDirection = 'DESC',
      categoryId,
      minPrice,
      maxPrice,
      condition,
      keyword
    } = pageParams;

    const params = {
      page,
      size,
      sortBy,
      sortDirection
    };

    // 添加可选参数
    if (categoryId) params.categoryId = categoryId;
    if (minPrice) params.minPrice = minPrice;
    if (maxPrice) params.maxPrice = maxPrice;
    if (condition) params.condition = condition;
    if (keyword) params.keyword = keyword;

    console.log('Fetching items page with params:', params);
    
    const response = await axios.get('/api/items/page', { params });
    const apiResponse = response.data;
    
    if (apiResponse.success || response.code === 200) {
      return apiResponse.data; // 返回PageResponseDTO
    } else {
      console.error('API error:', apiResponse.message);
      return {
        content: [],
        page: 0,
        size: 20,
        totalElements: 0,
        totalPages: 0,
        first: true,
        last: true,
        empty: true
      };
    }
  } catch (error) {
    console.error('获取分页商品失败:', error.response || error);
    return {
      content: [],
      page: 0,
      size: 20,
      totalElements: 0,
      totalPages: 0,
      first: true,
      last: true,
      empty: true
    };
  }
};

// 获取可售商品列表
export const getAvailableItems = async () => {
  try {
    const response = await axios.get('/api/items/available');
    const apiResponse = response.data;
    return apiResponse.success ? apiResponse.data : [];
  } catch (error) {
    console.error('获取可售商品失败:', error.response || error);
    return [];
  }
};

// 获取用户发布的商品
export const getUserItems = async (userId) => {
  if (!userId) {
    throw new Error('用户ID不能为空');
  }
  try {
    const response = await axios.get(`/api/items/seller/${userId}`);
    const apiResponse = response.data;
    return apiResponse.success ? apiResponse.data : [];
  } catch (error) {
    console.error('获取用户商品失败:', error.response || error);
    return [];
  }
};

// 发布商品（使用FormData格式）
export const createItem = async (itemData) => {
  try {
    console.log('Creating item with data:', itemData);
    console.log('itemData.condition:', itemData.condition);
    console.log('itemData.condition type:', typeof itemData.condition);
    console.log('itemData.condition length:', itemData.condition ? itemData.condition.length : 'undefined');
    
    // 验证必要字段
    if (!itemData.condition || itemData.condition.trim() === '') {
      throw new Error('新旧程度不能为空');
    }
    
    // 创建FormData对象
    const formData = new FormData();
    formData.append('title', itemData.title);
    formData.append('price', itemData.price.toString());
    formData.append('category', itemData.category);
    formData.append('condition', itemData.condition.trim());
    
    if (!itemData.sellerId) {
      throw new Error('卖家ID不能为空');
    }
    formData.append('sellerId', itemData.sellerId);
    
    if (itemData.description) {
      formData.append('description', itemData.description);
    }
    
    if (itemData.image) {
      formData.append('image', itemData.image);
    }

    // 打印FormData内容用于调试
    console.log('FormData entries:');
    for (let [key, value] of formData.entries()) {
      console.log(`${key}:`, value);
      if (key === 'condition') {
        console.log('  condition详细信息:');
        console.log('  - 类型:', typeof value);
        console.log('  - 长度:', value ? value.length : 'undefined/null');
        console.log('  - 值:', JSON.stringify(value));
        console.log('  - 是否为空:', !value || value.trim() === '');
      }
    }

    const response = await axios.post('/api/items', formData, {
      headers: {
        'Content-Type': 'multipart/form-data',
      },
    });
    
    console.log('Create item response:', response.data);
    return response.data;
  } catch (error) {
    console.error('发布商品失败:', error.response || error);
    throw error;
  }
};

// 更新商品信息（使用FormData格式）
export const updateItem = async (itemId, itemData) => {
  try {
    console.log('Updating item with ID:', itemId, 'data:', itemData);
    
    // 创建FormData对象
    const formData = new FormData();
    formData.append('title', itemData.title);
    formData.append('price', itemData.price.toString());
    formData.append('category', itemData.category);
    formData.append('condition', itemData.condition);
    
    if (!itemData.sellerId) {
      throw new Error('卖家ID不能为空');
    }
    formData.append('sellerId', itemData.sellerId);
    
    if (itemData.description) {
      formData.append('description', itemData.description);
    }
    
    // 只有当有新图片时才添加
    if (itemData.image && itemData.image instanceof File) {
      formData.append('image', itemData.image);
    }

    const response = await axios.put(`/api/items/${itemId}`, formData, {
      headers: {
        'Content-Type': 'multipart/form-data',
      },
    });
    
    console.log('Update item response:', response.data);
    return response.data;
  } catch (error) {
    console.error('更新商品失败:', error.response || error);
    throw error;
  }
};

// 删除商品
export const deleteItem = async (itemId, operatorId, forceDelete = false) => {
  if (!operatorId) {
    throw new Error('操作员ID不能为空');
  }
  try {
    console.log('Deleting item:', itemId);
    
    const response = await axios.delete(`/api/items/${itemId}`, {
      params: {
        operatorId,
        forceDelete
      }
    });
    
    console.log('Delete item response:', response.data);
    return response.data.success;
  } catch (error) {
    console.error('删除商品失败:', error.response || error);
    throw error;
  }
};

// 上下架商品
export const toggleItemAvailability = async (itemId, isAvailable, operatorId, reason = '') => {
  if (!operatorId) {
    throw new Error('操作员ID不能为空');
  }
  try {
    console.log('Toggling item availability:', itemId, 'to:', isAvailable);
    
    const response = await axios.patch(`/api/items/${itemId}/availability`, null, {
      params: {
        isAvailable,
        operatorId,
        reason
      }
    });
    
    console.log('Toggle availability response:', response.data);
    return response.data.success;
  } catch (error) {
    console.error('上下架商品失败:', error.response || error);
    throw error;
  }
};

// 搜索商品（分页查询）
export const searchItems = async (queryData) => {
  try {
    console.log('Searching items with query:', queryData);
    
    const response = await axios.post('/api/items/search', queryData);
    
    console.log('Search items response:', response.data);
    const apiResponse = response.data;
    return apiResponse.success ? apiResponse.data : { content: [], totalElements: 0 };
  } catch (error) {
    console.error('搜索商品失败:', error.response || error);
    return { content: [], totalElements: 0 };
  }
};

// 根据分类获取商品
export const getItemsByCategory = async (categoryId) => {
  try {
    const response = await axios.get(`/api/items/category/${categoryId}`);
    const apiResponse = response.data;
    return apiResponse.success ? apiResponse.data : [];
  } catch (error) {
    console.error('获取分类商品失败:', error.response || error);
    return [];
  }
}; 

export const getItemsByCategoryPaged = async (categoryId, params = {}) => {
  try {
    const response = await axios.get(`/api/items/category/${categoryId}/page`, { params });
    const apiResponse = response.data;
    return apiResponse.success ? apiResponse.data : {
      content: [],
      page: 0,
      size: params.size || 12,
      totalElements: 0,
      totalPages: 0,
      first: true,
      last: true,
      empty: true
    };
  } catch (error) {
    return {
      content: [],
      page: 0,
      size: params.size || 12,
      totalElements: 0,
      totalPages: 0,
      first: true,
      last: true,
      empty: true
    };
  }
}; 

// AI描述生成
export const generateDescription = async (title, category, condition) => {
  try {
    console.log('Generating AI description with params:', { title, category, condition });
    
    const response = await axios.post('/api/items/generate-description', {
      title: title,
      category: category,
      condition: condition
    });
    
    console.log('AI description response:', response);
    
    const apiResponse = response.data;
    if (!apiResponse || typeof apiResponse.code === 'undefined') {
      console.error('Invalid API response format:', apiResponse);
      throw new Error('服务器响应格式不正确');
    }

    if (apiResponse.code !== 200) {
      console.error('API error:', apiResponse.message);
      throw new Error(apiResponse.message || 'AI描述生成失败');
    }

    return apiResponse.data; // 返回生成的描述文本
  } catch (error) {
    console.error('AI描述生成失败:', error.response || error);
    throw error;
  }
}; 