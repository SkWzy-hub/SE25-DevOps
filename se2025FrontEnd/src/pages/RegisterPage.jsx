import { useState, useEffect } from 'react';
import { useNavigate, Link } from 'react-router-dom';
import { UserAPI } from '../service/api';

const RegisterPage = () => {
  const navigate = useNavigate();
  const [formData, setFormData] = useState({
    username: '',
    email: '',
    phone: '',
    verificationCode: '',
    password: '',
    confirmPassword: ''
  });
  const [error, setError] = useState('');
  const [success, setSuccess] = useState('');
  const [codeSent, setCodeSent] = useState(false);
  const [countdown, setCountdown] = useState(0);

  // 页面加载时滚动到顶部
  useEffect(() => {
    window.scrollTo(0, 0);
  }, []);

  const handleInputChange = (e) => {
    const { name, value } = e.target;
    
    // 手机号输入时只允许数字
    if (name === 'phone') {
      const numericValue = value.replace(/\D/g, '');
      setFormData(prev => ({
        ...prev,
        [name]: numericValue
      }));
    } else {
      setFormData(prev => ({
        ...prev,
        [name]: value
      }));
    }
    
    // 清除错误信息
    if (error) {
      setError('');
    }
    if (success) {
      setSuccess('');
    }
  };

  const sendVerificationCode = async () => {
    if (!formData.username) {
      setError('请输入昵称');
      return;
    }
    
    if (!formData.email) {
      setError('请输入邮箱地址');
      return;
    }
    
    // 邮箱后缀验证
    if (!formData.email.endsWith('@sjtu.edu.cn')) {
      setError('请使用上海交通大学邮箱（@sjtu.edu.cn）');
      return;
    }
    
    // 邮箱格式验证
    const emailRegex = /^[a-zA-Z0-9._%+-]+@sjtu\.edu\.cn$/;
    if (!emailRegex.test(formData.email)) {
      setError('请输入正确的邮箱格式');
      return;
    }
    
    if (!formData.phone) {
      setError('请输入手机号码');
      return;
    }
    
    // 手机号格式验证
    const phoneRegex = /^1[3-9]\d{9}$/;
    if (!phoneRegex.test(formData.phone)) {
      setError('请输入正确的手机号码格式');
      return;
    }
    
    try {
      // 调用后端API发送验证码
      await UserAPI.sendRegistrationCode(formData.email);
      
      setCodeSent(true);
      setCountdown(60);
      setError('');
      setSuccess(`验证码已发送到邮箱 ${formData.email.replace(/(.{2}).*(@.*)/, '$1****$2')}`);
      
      // 启动倒计时
      const timer = setInterval(() => {
        setCountdown(prev => {
          if (prev <= 1) {
            clearInterval(timer);
            return 0;
          }
          return prev - 1;
        });
      }, 1000);
      
    } catch (error) {
      setError(error.message || '发送验证码失败，请稍后重试');
      console.error('发送验证码失败:', error);
    }
  };

  const handleSubmit = async (e) => {
    e.preventDefault();
    
    if (!formData.username || !formData.email || !formData.phone || !formData.verificationCode || !formData.password || !formData.confirmPassword) {
      setError('请填写完整信息');
      return;
    }

    // 昵称长度验证
    if (formData.username.length < 2 || formData.username.length > 20) {
      setError('昵称长度应在2-20个字符之间');
      return;
    }

    // 邮箱后缀验证
    if (!formData.email.endsWith('@sjtu.edu.cn')) {
      setError('请使用上海交通大学邮箱（@sjtu.edu.cn）');
      return;
    }

    // 邮箱格式验证
    const emailRegex = /^[a-zA-Z0-9._%+-]+@sjtu\.edu\.cn$/;
    if (!emailRegex.test(formData.email)) {
      setError('请输入正确的邮箱格式');
      return;
    }

    if (formData.password !== formData.confirmPassword) {
      setError('两次密码输入不一致');
      return;
    }

    if (formData.password.length < 6) {
      setError('密码长度至少6位');
      return;
    }

    try {
      // 调用后端注册API
      const response = await UserAPI.register({
        username: formData.username,
        email: formData.email,
        phone: formData.phone,
        verificationCode: formData.verificationCode,
        password: formData.password,
        confirmPassword: formData.confirmPassword
      });

      setError('');
      setSuccess(`注册成功！欢迎您，${response.data.username}！3秒后自动跳转到登录页面...`);
      
      // 3秒后跳转到登录页面
      setTimeout(() => {
        navigate('/login');
      }, 3000);
      
    } catch (error) {
      setError(error.message || '注册失败，请稍后重试');
      console.error('注册失败:', error);
    }
  };

  return (
    <div className="page">
      <div className="page-header">
        <button className="back-btn" onClick={() => navigate('/login')}>
          <i className="fas fa-arrow-left"></i>
        </button>
        <h2>用户注册</h2>
        <button className="back-btn" onClick={() => navigate('/')}>
          <i className="fas fa-home"></i>
        </button>
      </div>
      <div className="auth-container">
        <div className="auth-form">
          <h2>注册</h2>
          <form onSubmit={handleSubmit}>
            <div className="form-group">
              <label htmlFor="username">昵称 <span className="required">*</span></label>
              <input
                type="text"
                id="username"
                name="username"
                value={formData.username}
                onChange={handleInputChange}
                placeholder="请输入昵称（2-20个字符）"
                maxLength="20"
                required
              />
            </div>
            <div className="form-group">
              <label htmlFor="email">学校邮箱 <span className="required">*</span></label>
              <input
                type="email"
                id="email"
                name="email"
                value={formData.email}
                onChange={handleInputChange}
                placeholder="请输入@sjtu.edu.cn邮箱"
                required
              />
              <div className="form-tip">
                <i className="fas fa-info-circle"></i>
                仅支持上海交通大学邮箱（@sjtu.edu.cn）
              </div>
            </div>
            <div className="form-group">
              <label htmlFor="phone">手机号码 <span className="required">*</span></label>
              <input
                type="tel"
                id="phone"
                name="phone"
                value={formData.phone}
                onChange={handleInputChange}
                placeholder="请输入11位手机号码"
                maxLength="11"
                required
              />
            </div>
            <div className="form-group">
              <label htmlFor="verificationCode">验证码 <span className="required">*</span></label>
              <div className="verification-group">
                <input
                  type="text"
                  id="verificationCode"
                  name="verificationCode"
                  value={formData.verificationCode}
                  onChange={handleInputChange}
                  placeholder="请输入验证码"
                  maxLength="6"
                  required
                />
                <button
                  type="button"
                  className="btn-secondary"
                  onClick={sendVerificationCode}
                  disabled={countdown > 0}
                >
                  {countdown > 0 ? `${countdown}s` : '发送验证码'}
                </button>
              </div>
            </div>
            <div className="form-group">
              <label htmlFor="password">密码 <span className="required">*</span></label>
              <input
                type="password"
                id="password"
                name="password"
                value={formData.password}
                onChange={handleInputChange}
                placeholder="请输入至少6位密码"
                required
              />
            </div>
            <div className="form-group">
              <label htmlFor="confirmPassword">确认密码 <span className="required">*</span></label>
              <input
                type="password"
                id="confirmPassword"
                name="confirmPassword"
                value={formData.confirmPassword}
                onChange={handleInputChange}
                placeholder="请再次输入密码"
                required
              />
            </div>
            {error && <div className="error-message">{error}</div>}
            {success && <div className="success-message">{success}</div>}
            <button type="submit" className="btn-primary">注册</button>
          </form>
          <div className="auth-link">
            <p>已有账号？<Link to="/login">立即登录</Link></p>
          </div>
        </div>
      </div>
    </div>
  );
};

export default RegisterPage; 