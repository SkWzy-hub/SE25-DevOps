import { useState, useEffect } from 'react';
import { useNavigate } from 'react-router-dom';
import { UserAPI } from '../service/api';

const ChangePasswordPage = () => {
  const navigate = useNavigate();
  const [currentStep, setCurrentStep] = useState(1); // 1: 输入邮箱, 2: 输入验证码, 3: 设置新密码
  const [formData, setFormData] = useState({
    email: '',
    verificationCode: '',
    newPassword: '',
    confirmPassword: ''
  });
  const [error, setError] = useState('');
  const [success, setSuccess] = useState('');
  const [userInfo, setUserInfo] = useState(null);
  const [codeSent, setCodeSent] = useState(false);
  const [countdown, setCountdown] = useState(0);
  const [resetToken, setResetToken] = useState(null);

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
    // 清除错误信息
    if (error) setError('');
    if (success) setSuccess('');
  };

  // 发送验证码
  const sendVerificationCode = async () => {
    if (!formData.email) {
      setError('请输入邮箱地址');
      return;
    }

    // 邮箱后缀验证
    if (!formData.email.endsWith('@sjtu.edu.cn')) {
      setError('请使用上海交通大学邮箱（@sjtu.edu.cn）');
      return;
    }

    try {
      // 调用后端API发送验证码
      const response = await UserAPI.sendForgotPasswordCode(formData.email);
      
      setUserInfo({ email: formData.email });
      setCodeSent(true);
      setCountdown(60);
      setError('');
      setSuccess(`验证码已发送到邮箱 ${formData.email.replace(/(.{2}).*(@.*)/, '$1****$2')}`);
      setCurrentStep(2);
      
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

  // 重新发送验证码
  const resendCode = async () => {
    if (countdown > 0) return;
    
    try {
      // 调用后端API重新发送验证码
      await UserAPI.sendForgotPasswordCode(formData.email);
      
      setCodeSent(true);
      setCountdown(60);
      setError('');
      setSuccess(`验证码已重新发送到邮箱 ${formData.email.replace(/(.{2}).*(@.*)/, '$1****$2')}`);
      
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
      setError(error.message || '重新发送验证码失败，请稍后重试');
      console.error('重新发送验证码失败:', error);
    }
  };

  // 验证验证码
  const verifyCode = async () => {
    if (!formData.verificationCode) {
      setError('请输入验证码');
      return;
    }

    if (formData.verificationCode.length !== 6) {
      setError('验证码应为6位数字');
      return;
    }

    try {
      // 调用后端API验证验证码
      const response = await UserAPI.verifyForgotPasswordCode(formData.email, formData.verificationCode);
      
      // 保存重置令牌
      setResetToken(response.data);
      setError('');
      setSuccess('验证码验证成功！');
      setCurrentStep(3);
      
    } catch (error) {
      setError(error.message || '验证码验证失败，请重新输入');
      console.error('验证验证码失败:', error);
    }
  };

  // 重置密码
  const resetPassword = async () => {
    if (!formData.newPassword || !formData.confirmPassword) {
      setError('请填写完整的密码信息');
      return;
    }

    if (formData.newPassword.length < 6) {
      setError('新密码长度至少6位');
      return;
    }

    if (formData.newPassword !== formData.confirmPassword) {
      setError('两次输入的新密码不一致');
      return;
    }

    if (!resetToken) {
      setError('重置令牌无效，请重新验证');
      return;
    }

    try {
      // 调用后端API重置密码
      await UserAPI.resetPasswordByEmail(formData.email, formData.newPassword, resetToken);
      
      setError('');
      setSuccess('密码重置成功！3秒后自动跳转到登录页面...');
      setTimeout(() => {
        navigate('/login');
      }, 3000);
      
    } catch (error) {
      setError(error.message || '密码重置失败，请稍后重试');
      console.error('密码重置失败:', error);
    }
  };

  const handleSubmit = (e) => {
    e.preventDefault();
    
    if (currentStep === 1) {
      sendVerificationCode();
    } else if (currentStep === 2) {
      verifyCode();
    } else if (currentStep === 3) {
      resetPassword();
    }
  };

  const goBack = () => {
    if (currentStep > 1) {
      setCurrentStep(currentStep - 1);
      setError('');
      setSuccess('');
    } else {
      navigate('/login');
    }
  };

  const getStepTitle = () => {
    switch (currentStep) {
      case 1: return '找回密码 - 验证邮箱';
      case 2: return '找回密码 - 输入验证码';
      case 3: return '找回密码 - 设置新密码';
      default: return '找回密码';
    }
  };

  const getButtonText = () => {
    switch (currentStep) {
      case 1: return '发送验证码';
      case 2: return '验证并继续';
      case 3: return '完成重置';
      default: return '下一步';
    }
  };

  return (
    <div className="page">
      <div className="page-header">
        <button className="back-btn" onClick={goBack}>
          <i className="fas fa-arrow-left"></i>
        </button>
        <h2>{getStepTitle()}</h2>
        <button className="back-btn" onClick={() => navigate('/login')}>
          <i className="fas fa-home"></i>
        </button>
      </div>
      <div className="auth-container">
        <div className="auth-form">
          <div className="step-indicator">
            <div className="steps">
              <div className={`step ${currentStep >= 1 ? 'active' : ''}`}>1</div>
              <div className={`step-line ${currentStep >= 2 ? 'active' : ''}`}></div>
              <div className={`step ${currentStep >= 2 ? 'active' : ''}`}>2</div>
              <div className={`step-line ${currentStep >= 3 ? 'active' : ''}`}></div>
              <div className={`step ${currentStep >= 3 ? 'active' : ''}`}>3</div>
            </div>
            <div className="step-labels">
              <span>验证邮箱</span>
              <span>输入验证码</span>
              <span>设置新密码</span>
            </div>
          </div>

          <form onSubmit={handleSubmit}>
            {currentStep === 1 && (
              <>
                <div className="form-group">
                  <label htmlFor="email">邮箱</label>
                  <input
                    type="email"
                    id="email"
                    name="email"
                    value={formData.email}
                    onChange={handleInputChange}
                    placeholder="请输入您的邮箱"
                    required
                  />
                </div>
                <div className="form-tip">
                  <i className="fas fa-info-circle"></i>
                  我们将向您的注册邮箱发送验证码
                </div>
              </>
            )}

            {currentStep === 2 && (
              <>
                <div className="form-group">
                  <label htmlFor="verificationCode">验证码</label>
                  <div className="verification-group">
                    <input
                      type="text"
                      id="verificationCode"
                      name="verificationCode"
                      value={formData.verificationCode}
                      onChange={handleInputChange}
                      placeholder="请输入6位验证码"
                      maxLength="6"
                      required
                    />
                    <button
                      type="button"
                      className="btn-secondary"
                      onClick={resendCode}
                      disabled={countdown > 0}
                    >
                      {countdown > 0 ? `${countdown}s` : '重新发送'}
                    </button>
                  </div>
                </div>
                <div className="form-tip">
                  <i className="fas fa-info-circle"></i>
                  验证码已发送到 {formData.email.replace(/(.{2}).*(@.*)/, '$1****$2')}
                  <br />
                  <small>请查看您的邮箱获取6位验证码</small>
                </div>
              </>
            )}

            {currentStep === 3 && (
              <>
                <div className="form-group">
                  <label htmlFor="newPassword">新密码</label>
                  <input
                    type="password"
                    id="newPassword"
                    name="newPassword"
                    value={formData.newPassword}
                    onChange={handleInputChange}
                    placeholder="请输入新密码（至少6位）"
                    required
                  />
                </div>
                <div className="form-group">
                  <label htmlFor="confirmPassword">确认新密码</label>
                  <input
                    type="password"
                    id="confirmPassword"
                    name="confirmPassword"
                    value={formData.confirmPassword}
                    onChange={handleInputChange}
                    placeholder="请再次输入新密码"
                    required
                  />
                </div>
              </>
            )}

            {error && <div className="error-message">{error}</div>}
            {success && <div className="success-message">{success}</div>}
            <button type="submit" className="btn-primary">{getButtonText()}</button>
          </form>
        </div>
      </div>
    </div>
  );
};

export default ChangePasswordPage; 