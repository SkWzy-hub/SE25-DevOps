import { useState, useEffect } from 'react';
import { useNavigate, Link } from 'react-router-dom';
import { useAppContext } from '../App';
import { post, PREFIX, TokenService } from '../service/common';

const LoginPage = () => {
  const { setCurrentUser } = useAppContext();
  const navigate = useNavigate();
  const [formData, setFormData] = useState({
    email: '',
    password: ''
  });
  const [error, setError] = useState('');

  // 页面加载时滚动到顶部
  useEffect(() => {
    window.scrollTo(0, 0);
  }, []);

  const handleInputChange = (e) => {
    const { name, value } = e.target;
    setFormData(prev => ({
      ...prev,
      [name]: value
    }));
  };

  const handleSubmit = async (e) => {
    e.preventDefault();
    
    if (!formData.email || !formData.password) {
      setError('请填写完整信息');
      return;
    }

    // 邮箱格式验证
    if (!formData.email.endsWith('@sjtu.edu.cn')) {
      setError('请使用上海交通大学邮箱登录');
      return;
    }

    try {
      // 调用后端登录API
      const response = await post(`${PREFIX}/login`, {
        email: formData.email,
        password: formData.password
      });

      if (response.success) {
        // 保存JWT token
        TokenService.setToken(response.data.token);
        
        // 设置当前用户信息（注意：后端返回的是userInfo，不是user）
        const user = {
          id: response.data.userInfo.userId,
          username: response.data.userInfo.username,
          email: response.data.userInfo.email,
          phone: response.data.userInfo.phone,
          avatar: response.data.userInfo.avatar,
          note: response.data.userInfo.note
        };
        setCurrentUser(user);
        
        // 同时保存到localStorage，以便其他页面使用
        localStorage.setItem('currentUser', JSON.stringify(user));
        
        // 跳转到首页
        navigate('/');
      } else {
        setError(response.message || '登录失败');
      }
    } catch (error) {
      console.error('登录请求失败:', error);
      setError('网络错误，请稍后重试');
    }
  };

  return (
    <div className="page">
      <div className="page-header">
        <button className="back-btn" onClick={() => navigate('/')}>
          <i className="fas fa-arrow-left"></i>
        </button>
        <h2>用户登录</h2>
        <button className="back-btn" onClick={() => navigate('/')}>
          <i className="fas fa-home"></i>
        </button>
      </div>
      <div className="auth-container">
        <div className="auth-form">
          <h2>登录</h2>
          <form onSubmit={handleSubmit}>
            <div className="form-group">
              <label htmlFor="email">邮箱</label>
              <input
                type="email"
                id="email"
                name="email"
                value={formData.email}
                onChange={handleInputChange}
                placeholder="请输入@sjtu.edu.cn邮箱"
                required
              />
            </div>
            <div className="form-group">
              <label htmlFor="password">密码</label>
              <input
                type="password"
                id="password"
                name="password"
                value={formData.password}
                onChange={handleInputChange}
                placeholder="请输入密码"
                required
              />
            </div>
            {error && <div className="error-message">{error}</div>}
            <button type="submit" className="btn-primary">登录</button>
          </form>
          <div className="auth-link">
            <p>还没有账号？<Link to="/register">立即注册</Link></p>
            <p>忘记密码？<Link to="/change-password">找回密码</Link></p>
          </div>
          <div className="login-tips">
            <div className="form-tip">
              <i className="fas fa-info-circle"></i>
              <div>
                <strong>测试账号：</strong>
                <br />
                管理员：admin@sjtu.edu.cn / 123456
                <br />
                普通用户：user1@sjtu.edu.cn / 123456
              </div>
            </div>
          </div>
        </div>
      </div>
    </div>
  );
};

export default LoginPage; 