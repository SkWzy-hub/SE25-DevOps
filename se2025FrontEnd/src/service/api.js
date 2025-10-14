// API服务层 - 替代mockData
import { post, get, put, del, TokenService, PREFIX } from './common.jsx';

/**
 * 用户相关API
 */
export const UserAPI = {
    // 登录
    async login(email, password) {
        try {
            const response = await post(`${PREFIX}/login`, { 
                email: email, 
                password: password 
            });
            
            if (response.success && response.data.token) {
                // 保存token
                TokenService.setToken(response.data.token);
                return response.data;
            }
            throw new Error(response.message || '登录失败');
        } catch (error) {
            console.error('登录API错误:', error);
            throw error;
        }
    },

    // 获取当前用户信息
    async getCurrentUser() {
        try {
            const response = await get(`${PREFIX}/user/current`);
            return response.success ? response.data : null;
        } catch (error) {
            console.error('获取用户信息错误:', error);
            return null;
        }
    },

    // 登出
    async logout() {
        try {
            await post(`${PREFIX}/logout`, {});
            TokenService.removeToken();
            return true;
        } catch (error) {
            console.error('登出API错误:', error);
            TokenService.removeToken(); // 即使API失败也清除本地token
            return true;
        }
    },

    // 获取用户详细信息
    async getUserProfile(userId) {
        try {
            const response = await get(`${PREFIX}/users/${userId}/profile`);
            return response.success ? response.data : null;
        } catch (error) {
            console.error('获取用户详细信息错误:', error);
            return null;
        }
    },

    // 更新用户基本信息
    async updateUserProfile(userId, profileData) {
        try {
            const response = await put(`${PREFIX}/users/${userId}/profile`, profileData);
            if (response.success) {
                return response.data;
            }
            throw new Error(response.message || '更新用户信息失败');
        } catch (error) {
            console.error('更新用户信息错误:', error);
            throw error;
        }
    },

    // 上传用户头像
    async uploadUserAvatar(userId, file) {
        try {
            const formData = new FormData();
            formData.append('avatar', file);

            const response = await fetch(`${PREFIX}/users/${userId}/avatar/upload`, {
                method: 'POST',
                body: formData,
                headers: {
                    'Authorization': `Bearer ${TokenService.getToken()}`
                }
            });

            const result = await response.json();
            if (result.success) {
                return result.data;
            }
            throw new Error(result.message || '头像上传失败');
        } catch (error) {
            console.error('上传头像错误:', error);
            throw error;
        }
    },

    // 修改用户密码
    async changePassword(userId, passwordData) {
        try {
            const response = await put(`${PREFIX}/users/${userId}/password`, passwordData);
            console.log('修改密码API响应:', response); // 调试日志
            
            // 检查多种成功条件
            if (response.success || 
                (response.message && response.message.includes('成功')) ||
                (response.message && response.message.includes('密码更新成功'))) {
                console.log('密码修改成功响应'); // 调试日志
                return response.data || response;
            }
            
            throw new Error(response.message || '修改密码失败');
        } catch (error) {
            console.error('修改密码错误:', error);
            
            // 特殊处理：如果错误消息包含"成功"，则认为是成功的
            if (error.message && error.message.includes('成功')) {
                console.log('从错误消息中识别出成功状态'); // 调试日志
                return { success: true, message: error.message };
            }
            
            throw error;
        }
    },

    // 忘记密码 - 发送验证码
    async sendForgotPasswordCode(email) {
        try {
            const response = await post(`${PREFIX}/forgot-password/send-code`, { email });
            if (response.success) {
                return response;
            }
            throw new Error(response.message || '发送验证码失败');
        } catch (error) {
            console.error('发送验证码错误:', error);
            throw error;
        }
    },

    // 忘记密码 - 验证验证码
    async verifyForgotPasswordCode(email, code) {
        try {
            const response = await post(`${PREFIX}/forgot-password/verify-code`, { email, code });
            if (response.success) {
                return response;
            }
            throw new Error(response.message || '验证码验证失败');
        } catch (error) {
            console.error('验证验证码错误:', error);
            throw error;
        }
    },

    // 忘记密码 - 重置密码
    async resetPasswordByEmail(email, newPassword, resetToken) {
        try {
            const response = await post(`${PREFIX}/forgot-password/reset`, { 
                email, 
                newPassword, 
                resetToken 
            });
            if (response.success) {
                return response;
            }
            throw new Error(response.message || '密码重置失败');
        } catch (error) {
            console.error('重置密码错误:', error);
            throw error;
        }
    },

    // 注册 - 发送验证码
    async sendRegistrationCode(email) {
        try {
            const response = await post(`${PREFIX}/register/send-code`, { email });
            if (response.success) {
                return response;
            }
            throw new Error(response.message || '发送验证码失败');
        } catch (error) {
            console.error('发送注册验证码错误:', error);
            throw error;
        }
    },

    // 用户注册
    async register(registerData) {
        try {
            const response = await post(`${PREFIX}/register`, registerData);
            if (response.success) {
                return response;
            }
            throw new Error(response.message || '注册失败');
        } catch (error) {
            console.error('注册错误:', error);
            throw error;
        }
    }
};

/**
 * 商品相关API
 */
export const ItemAPI = {
    // 获取商品列表
    async getItems(params = {}) {
        try {
            const queryParams = new URLSearchParams();
            
            // 添加查询参数
            Object.keys(params).forEach(key => {
                if (params[key] !== undefined && params[key] !== null && params[key] !== '') {
                    queryParams.append(key, params[key]);
                }
            });
            
            const url = `${PREFIX}/items?${queryParams.toString()}`;
            const response = await get(url);
            
            return response.success ? response.data : { content: [], totalElements: 0 };
        } catch (error) {
            console.error('获取商品列表错误:', error);
            return { content: [], totalElements: 0 };
        }
    },

    // 获取商品详情
    async getItemById(id) {
        try {
            const response = await get(`${PREFIX}/items/${id}`);
            return response.success ? response.data : null;
        } catch (error) {
            console.error('获取商品详情错误:', error);
            return null;
        }
    },

    // 发布商品
    async createItem(itemData) {
        try {
            const response = await post(`${PREFIX}/items`, itemData);
            if (response.success) {
                return response.data;
            }
            throw new Error(response.message || '发布商品失败');
        } catch (error) {
            console.error('发布商品错误:', error);
            throw error;
        }
    },

    // 更新商品
    async updateItem(id, itemData) {
        try {
            const response = await put(`${PREFIX}/items/${id}`, itemData);
            if (response.success) {
                return response.data;
            }
            throw new Error(response.message || '更新商品失败');
        } catch (error) {
            console.error('更新商品错误:', error);
            throw error;
        }
    },

    // 删除商品
    async deleteItem(id) {
        try {
            const response = await del(`${PREFIX}/items/${id}`, {});
            return response.success;
        } catch (error) {
            console.error('删除商品错误:', error);
            return false;
        }
    },

    // 获取我发布的商品
    async getMyItems() {
        try {
            const response = await get(`${PREFIX}/my/items`);
            return response.success ? response.data : [];
        } catch (error) {
            console.error('获取我的商品错误:', error);
            return [];
        }
    }
};

/**
 * 收藏相关API
 */
export const FavoriteAPI = {
    // 获取我的收藏
    async getMyFavorites() {
        try {
            const response = await get(`${PREFIX}/favorites`);
            return response.success ? response.data : [];
        } catch (error) {
            console.error('获取收藏列表错误:', error);
            return [];
        }
    },

    // 添加收藏
    async addFavorite(itemId) {
        try {
            const response = await post(`${PREFIX}/favorites/${itemId}`, {});
            return response.success;
        } catch (error) {
            console.error('添加收藏错误:', error);
            return false;
        }
    },

    // 取消收藏
    async removeFavorite(itemId) {
        try {
            const response = await del(`${PREFIX}/favorites/${itemId}`, {});
            return response.success;
        } catch (error) {
            console.error('取消收藏错误:', error);
            return false;
        }
    },

    // 检查是否已收藏
    async checkFavorite(itemId) {
        try {
            const response = await get(`${PREFIX}/favorites/${itemId}/check`);
            return response.success ? response.data : false;
        } catch (error) {
            console.error('检查收藏状态错误:', error);
            return false;
        }
    }
};

/**
 * 通用工具函数
 */
export const APIUtils = {
    // 处理API错误
    handleError(error) {
        if (error.response) {
            // 请求成功但状态码不在2xx范围内
            const status = error.response.status;
            const message = error.response.data?.message || '请求失败';
            
            if (status === 401) {
                // 未授权，清除token并跳转登录
                TokenService.removeToken();
                window.location.href = '/login';
                return '登录已过期，请重新登录';
            } else if (status === 403) {
                return '没有权限执行此操作';
            } else if (status === 404) {
                return '请求的资源不存在';
            } else if (status >= 500) {
                return '服务器错误，请稍后重试';
            }
            
            return message;
        } else if (error.request) {
            // 请求已发出但没有收到响应
            return '网络连接错误，请检查网络';
        } else {
            // 其他错误
            return error.message || '未知错误';
        }
    },

    // 格式化价格
    formatPrice(price) {
        return typeof price === 'number' ? `¥${price}` : price;
    },

    // 格式化日期
    formatDate(dateString) {
        const date = new Date(dateString);
        return date.toLocaleDateString('zh-CN');
    }
};

// 导出所有API
export default {
    UserAPI,
    ItemAPI,
    FavoriteAPI,
    APIUtils
}; 