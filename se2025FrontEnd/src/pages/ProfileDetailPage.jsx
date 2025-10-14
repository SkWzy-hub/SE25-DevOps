import React, { useState, useEffect, useCallback } from 'react';
import { useNavigate } from 'react-router-dom';
import { get, put, PREFIX, TokenService } from '../service/common';
import { UserAPI } from '../service/api';
import { useAppContext } from '../App';

// 默认头像URL - 使用稳定的占位图片
const DEFAULT_AVATAR = 'https://via.placeholder.com/120x120/f0f0f0/999999?text=%E5%A4%B4%E5%83%8F';

const ProfileDetailPage = () => {
  const navigate = useNavigate();
  // 使用全局的 currentUser 状态
  const { currentUser, setCurrentUser } = useAppContext();
  const [loading, setLoading] = useState(true);
  const [isEditing, setIsEditing] = useState(false);
  const [uploading, setUploading] = useState(false);
  const [initialized, setInitialized] = useState(false);
  const [formData, setFormData] = useState({
    username: '',
    email: '',
    phone: '',
    avatar: DEFAULT_AVATAR,
    note: '',
    creditScore: 0,
    dealTime: 0,
    role: '',
    status: true
  });

  // 修改密码相关状态
  const [isChangingPassword, setIsChangingPassword] = useState(false);
  const [passwordData, setPasswordData] = useState({
    oldPassword: '',
    newPassword: '',
    confirmPassword: ''
  });
  const [passwordError, setPasswordError] = useState('');
  const [passwordSuccess, setPasswordSuccess] = useState('');
  const [passwordLoading, setPasswordLoading] = useState(false);

  const loadUserProfile = useCallback(async (forceReload = false) => {
    if (!currentUser) return;
    
    // 如果已经初始化且不是强制重新加载，则跳过
    if (!forceReload && initialized) {
      console.log('已初始化，跳过重复加载');
      setLoading(false);
      return;
    }

    try {
      setLoading(true);
      const response = await get(`${PREFIX}/users/${currentUser.id}/profile`);
      const data = await response.json();
      
      if (data && data.code === 200 && data.data) {
        const userData = data.data;
        setFormData({
          username: userData.username || '',
          email: userData.email || '',
          phone: userData.phone || '',
          avatar: userData.avatar || DEFAULT_AVATAR,
          note: userData.note || '',
          creditScore: userData.creditScore || 0,
          dealTime: userData.dealTime || 0,
          role: userData.role || '',
          status: userData.status || true
        });
        setInitialized(true);
      } else {
        console.error('获取用户信息失败:', data);
      }
    } catch (error) {
      console.error('获取用户信息失败:', error);
    } finally {
      setLoading(false);
    }
  }, [currentUser, initialized]);

  // 当用户切换时重置初始化状态
  useEffect(() => {
    setInitialized(false);
    setFormData(prev => ({
      ...prev,
      avatar: DEFAULT_AVATAR
    }));
  }, [currentUser?.id]);

  // 检查用户登录状态并加载用户信息
  useEffect(() => {
    if (currentUser) {
      loadUserProfile();
    } else {
      navigate('/login');
    }
  }, [currentUser, loadUserProfile]);

  if (!currentUser) {
    navigate('/login');
    return null;
  }

  const handleInputChange = (e) => {
    const { name, value } = e.target;
    setFormData(prev => ({
      ...prev,
      [name]: value
    }));
  };

  const handleSave = async () => {
    try {
      // 调用后端API更新用户基本信息
      const response = await put(`${PREFIX}/users/${currentUser.id}/profile`, {
        username: formData.username,
        phone: formData.phone,
        note: formData.note
      });
      
      if (response.code === 200) {
        // 构建更新后的用户信息
        const updatedUser = {
          ...currentUser,
          username: formData.username,
          phone: formData.phone,
          note: formData.note
        };
        
        // 同时更新全局状态和localStorage
        setCurrentUser(updatedUser);
        localStorage.setItem('currentUser', JSON.stringify(updatedUser));
        
        setIsEditing(false);
        alert('个人信息更新成功！');
      } else {
        alert('更新失败：' + (response.message || '未知错误'));
      }
    } catch (error) {
      console.error('更新用户信息失败:', error);
      alert('网络错误，请稍后重试');
    }
  };

  const handleCancel = () => {
    // 强制重新加载用户信息以恢复原始数据
    loadUserProfile(true);
    setIsEditing(false);
  };

  const handleAvatarChange = () => {
    if (uploading) return; // 防止重复上传
    
    // 创建一个隐藏的文件输入元素
    const fileInput = document.createElement('input');
    fileInput.type = 'file';
    fileInput.accept = 'image/*';
    fileInput.style.display = 'none';
    
    fileInput.onchange = async (event) => {
      const file = event.target.files[0];
      if (!file) return;
      
      // 验证文件类型
      if (!file.type.startsWith('image/')) {
        alert('请选择图片文件！');
        return;
      }
      
      // 验证文件大小 (2MB)
      if (file.size > 2 * 1024 * 1024) {
        alert('图片文件大小不能超过2MB！');
        return;
      }
      
      try {
        setUploading(true);
        
        // 创建FormData对象
        const uploadFormData = new FormData();
        uploadFormData.append('avatar', file);
        
        // 调用文件上传API
        const response = await fetch(`${PREFIX}/users/${currentUser.id}/avatar/upload`, {
          method: 'POST',
          headers: {
            'Authorization': `Bearer ${TokenService.getToken()}`
          },
          body: uploadFormData
        });
        
        const result = await response.json();
        
        if (result.code === 200) {
          const newAvatarUrl = result.data;
          
          // 直接更新formData中的头像
          setFormData(prev => ({
            ...prev,
            avatar: newAvatarUrl || DEFAULT_AVATAR
          }));
          
          // 构建更新后的用户信息
          const updatedUser = {
            ...currentUser,
            avatar: newAvatarUrl || DEFAULT_AVATAR
          };
          
          // 同时更新全局状态和localStorage
          setCurrentUser(updatedUser);
          localStorage.setItem('currentUser', JSON.stringify(updatedUser));
          
          alert('头像更新成功！');
        } else {
          console.error('头像更新失败:', result);
          alert('头像更新失败：' + (result.message || '未知错误'));
        }
      } catch (error) {
        console.error('更新头像失败:', error);
        alert('网络错误，请稍后重试');
      } finally {
        setUploading(false);
      }
    };
    
    // 触发文件选择
    document.body.appendChild(fileInput);
    fileInput.click();
    document.body.removeChild(fileInput);
  };

  // 处理密码输入变化
  const handlePasswordChange = (e) => {
    const { name, value } = e.target;
    setPasswordData(prev => ({
      ...prev,
      [name]: value
    }));
    // 清除错误和成功信息
    if (passwordError) setPasswordError('');
    if (passwordSuccess) setPasswordSuccess('');
  };

  // 修改密码
  const handleChangePassword = async () => {
    console.log('=== 修改密码函数被调用 ===');
    console.log('当前密码数据:', passwordData);
    
    // 验证表单
    if (!passwordData.oldPassword) {
      console.log('验证失败：原密码为空');
      setPasswordError('请输入原密码');
      return;
    }
    
    if (!passwordData.newPassword) {
      console.log('验证失败：新密码为空');
      setPasswordError('请输入新密码');
      return;
    }
    
    if (passwordData.newPassword.length < 6) {
      console.log('验证失败：新密码长度不足');
      setPasswordError('新密码长度至少6位');
      return;
    }
    
    if (passwordData.newPassword !== passwordData.confirmPassword) {
      console.log('验证失败：两次密码不一致');
      setPasswordError('新密码和确认密码不一致');
      return;
    }
    
    if (passwordData.oldPassword === passwordData.newPassword) {
      console.log('验证失败：新旧密码相同');
      setPasswordError('新密码不能与原密码相同');
      return;
    }

    console.log('表单验证通过，开始修改密码...');

    try {
      console.log('设置加载状态为true');
      setPasswordLoading(true);
      setPasswordError('');
      setPasswordSuccess('');
      
      console.log('调用API修改密码...');
      console.log('API参数 - userId:', currentUser.id, 'passwordData:', {
        oldPassword: '***',
        newPassword: '***',
        confirmPassword: '***'
      });
      
      const apiResponse = await UserAPI.changePassword(currentUser.id, passwordData);
      
      console.log('✅ API调用成功！响应数据:', apiResponse);
      console.log('开始设置成功状态...');
      
      // 立即设置成功状态
      setPasswordLoading(false);
      setPasswordSuccess('密码修改成功！');
      setPasswordError('');
      
      console.log('✅ 状态设置完成，1.5秒后将重置到初始状态');
      
      // 1.5秒后重置到初始状态
      setTimeout(() => {
        console.log('🔄 开始重置到初始状态...');
        setPasswordData({
          oldPassword: '',
          newPassword: '',
          confirmPassword: ''
        });
        setPasswordSuccess('');
        setPasswordError('');
        setIsChangingPassword(false);
        console.log('✅ 重置完成，应该回到初始状态');
      }, 1500);
      
    } catch (error) {
      console.error('❌ 修改密码API抛出异常:', error);
      console.error('错误详情:', error.response || error.message);
      
      // 特殊处理：如果错误消息包含"成功"，说明实际上成功了
      if (error.message && error.message.includes('成功')) {
        console.log('🎯 检测到成功消息，转为成功处理');
        
        // 按成功处理
        setPasswordLoading(false);
        setPasswordSuccess('密码修改成功！');
        setPasswordError('');
        
        console.log('✅ 状态设置完成，1.5秒后将重置到初始状态');
        
        // 1.5秒后重置到初始状态
        setTimeout(() => {
          console.log('🔄 开始重置到初始状态...');
          setPasswordData({
            oldPassword: '',
            newPassword: '',
            confirmPassword: ''
          });
          setPasswordSuccess('');
          setPasswordError('');
          setIsChangingPassword(false);
          console.log('✅ 重置完成，应该回到初始状态');
        }, 1500);
        
        return; // 重要：退出异常处理
      }
      
      // 真正的错误处理
      setPasswordError(error.message || '修改密码失败，请稍后重试');
      setPasswordLoading(false);
      setPasswordSuccess('');
    }
  };

  // 取消修改密码
  const handleCancelPasswordChange = () => {
    setPasswordData({
      oldPassword: '',
      newPassword: '',
      confirmPassword: ''
    });
    setPasswordError('');
    setPasswordSuccess('');
    setIsChangingPassword(false);
  };



  return (
    <div className="page">
      <div className="page-header">
        <button className="back-btn" onClick={() => navigate('/profile')}>
          <i className="fas fa-arrow-left"></i>
        </button>
        <h2>个人详情</h2>
        <button className="back-btn" onClick={() => navigate('/')}>
          <i className="fas fa-home"></i>
        </button>
      </div>

      {loading ? (
        <div className="loading-state">
          <i className="fas fa-spinner fa-spin"></i>
          <p>加载中...</p>
        </div>
      ) : (
        <div className="profile-detail-container">
          <div className="avatar-section">
            <div className="avatar-container">
              <img 
                src={formData.avatar} 
                alt="头像" 
                className="profile-avatar-large"
                onError={(e) => {
                  if (e.target.src !== DEFAULT_AVATAR) {
                    e.target.src = DEFAULT_AVATAR;
                  }
                }}
              />
              {isEditing && (
                <div className="avatar-upload">
                  <button 
                    type="button" 
                    className="avatar-upload-btn" 
                    onClick={handleAvatarChange}
                    disabled={uploading}
                  >
                    <i className={uploading ? "fas fa-spinner fa-spin" : "fas fa-camera"}></i>
                    {uploading ? '上传中...' : '选择图片'}
                  </button>
                </div>
              )}
            </div>
          </div>

          <div className="profile-form">
            <div className="form-section">
              <h3>基本信息</h3>
              
              <div className="form-group">
                <label>用户名</label>
                {isEditing ? (
                  <input
                    type="text"
                    name="username"
                    value={formData.username}
                    onChange={handleInputChange}
                    placeholder="请输入用户名"
                  />
                ) : (
                  <div className="form-value">{formData.username || '未设置'}</div>
                )}
              </div>

              <div className="form-group">
                <label>邮箱</label>
                <div className="form-value">{formData.email || '未设置'}</div>
                <div className="form-tip">
                  <i className="fas fa-info-circle"></i>
                  邮箱地址不可更改，如需修改请联系管理员
                </div>
              </div>

              <div className="form-group">
                <label>手机号码</label>
                {isEditing ? (
                  <input
                    type="tel"
                    name="phone"
                    value={formData.phone}
                    onChange={handleInputChange}
                    placeholder="请输入手机号码"
                  />
                ) : (
                  <div className="form-value">{formData.phone || '未设置'}</div>
                )}
              </div>

              <div className="form-group">
                <label>个人签名</label>
                {isEditing ? (
                  <textarea
                    name="note"
                    value={formData.note}
                    onChange={handleInputChange}
                    placeholder="写下你的个人签名..."
                    rows="3"
                  />
                ) : (
                  <div className="form-value signature">
                    {formData.note || '这个人很懒，什么都没有留下...'}
                  </div>
                )}
              </div>
            </div>

            <div className="form-section">
              <h3>安全设置</h3>
              
              {!isChangingPassword ? (
                <div className="security-actions">
                  <button 
                    className="btn-outline" 
                    onClick={() => setIsChangingPassword(true)}
                    disabled={isEditing}
                  >
                    <i className="fas fa-key"></i>
                    修改密码
                  </button>
                  {isEditing && (
                    <div className="form-tip">
                      <i className="fas fa-info-circle"></i>
                      请先保存当前编辑的信息，再修改密码
                    </div>
                  )}
                </div>
              ) : (
                <div className="password-form">
                  <div className="form-group">
                    <label>原密码</label>
                    <input
                      type="password"
                      name="oldPassword"
                      value={passwordData.oldPassword}
                      onChange={handlePasswordChange}
                      placeholder="请输入原密码"
                      disabled={passwordLoading || passwordSuccess}
                    />
                  </div>

                  <div className="form-group">
                    <label>新密码</label>
                    <input
                      type="password"
                      name="newPassword"
                      value={passwordData.newPassword}
                      onChange={handlePasswordChange}
                      placeholder="请输入新密码（至少6位）"
                      disabled={passwordLoading || passwordSuccess}
                    />
                  </div>

                  <div className="form-group">
                    <label>确认新密码</label>
                    <input
                      type="password"
                      name="confirmPassword"
                      value={passwordData.confirmPassword}
                      onChange={handlePasswordChange}
                      placeholder="请再次输入新密码"
                      disabled={passwordLoading || passwordSuccess}
                    />
                  </div>

                  {passwordError && (
                    <div className="error-message">
                      <i className="fas fa-exclamation-circle"></i>
                      {passwordError}
                    </div>
                  )}

                  {passwordSuccess && (
                    <div 
                      className="success-message"
                      style={{
                        backgroundColor: '#d4edda',
                        border: '1px solid #c3e6cb',
                        color: '#155724',
                        padding: '12px',
                        borderRadius: '8px',
                        marginBottom: '16px',
                        display: 'flex',
                        alignItems: 'center',
                        gap: '8px'
                      }}
                    >
                      <i className="fas fa-check-circle" style={{ color: '#28a745' }}></i>
                      {passwordSuccess}
                    </div>
                  )}

                  <div className="password-actions">
                    <button 
                      className="btn-secondary" 
                      onClick={handleCancelPasswordChange}
                      disabled={passwordLoading}
                    >
                      取消
                    </button>
                    <button 
                      className="btn-primary" 
                      onClick={handleChangePassword}
                      disabled={passwordLoading || passwordSuccess}
                    >
                      {passwordLoading ? (
                        <>
                          <i className="fas fa-spinner fa-spin"></i>
                          修改中...
                        </>
                      ) : passwordSuccess ? (
                        <>
                          <i className="fas fa-check"></i>
                          修改成功
                        </>
                      ) : (
                        <>
                          <i className="fas fa-save"></i>
                          确认修改
                        </>
                      )}
                    </button>
                  </div>
                </div>
              )}
            </div>

            <div className="form-actions">
              {isEditing ? (
                <>
                  <button className="btn-secondary" onClick={handleCancel}>
                    取消
                  </button>
                  <button className="btn-primary" onClick={handleSave}>
                    保存
                  </button>
                </>
              ) : (
                <button className="btn-primary" onClick={() => setIsEditing(true)}>
                  <i className="fas fa-edit"></i>
                  编辑资料
                </button>
              )}
            </div>
          </div>
        </div>
      )}
    </div>
  );
};

export default ProfileDetailPage; 