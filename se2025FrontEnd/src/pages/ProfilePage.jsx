import { useEffect } from 'react';
import { useNavigate } from 'react-router-dom';
import { useAppContext } from '../App';

// 默认头像URL - 与个人详情页保持一致
const DEFAULT_AVATAR = 'https://via.placeholder.com/120x120/f0f0f0/999999?text=%E5%A4%B4%E5%83%8F';

const ProfilePage = () => {
  const { currentUser } = useAppContext();
  const navigate = useNavigate();

  // 页面加载时滚动到顶部
  useEffect(() => {
    window.scrollTo(0, 0);
  }, []);

  if (!currentUser) {
    navigate('/login');
    return null;
  }

  const handleSmartBack = () => {
    // 智能返回：直接回到主页，避免循环跳转
    navigate('/', { replace: true });
  };

  return (
    <div className="page">
      <div className="profile-container">
        <div className="page-header">
          <button className="back-btn" onClick={handleSmartBack}>
            <i className="fas fa-arrow-left"></i>
          </button>
          <h2>个人信息</h2>
          <button className="back-btn" onClick={() => navigate('/')}>
            <i className="fas fa-home"></i>
          </button>
        </div>
        
        <div className="profile-header" onClick={() => navigate('/profile-detail')}>
          <div className="profile-avatar">
            <img 
              src={currentUser.avatar || DEFAULT_AVATAR} 
              alt="头像"
              onError={(e) => {
                if (e.target.src !== DEFAULT_AVATAR) {
                  e.target.src = DEFAULT_AVATAR;
                }
              }}
            />
          </div>
          <div className="profile-info">
            <h2>{currentUser.username}</h2>
            <p>{currentUser.email}</p>
          </div>
          <div className="profile-arrow">
            <i className="fas fa-chevron-right"></i>
          </div>
        </div>
        
        <div className="profile-menu">
          <div className="menu-item" onClick={() => navigate('/my-purchases')}>
            <i className="fas fa-shopping-bag"></i>
            <span>我买入的</span>
            <i className="fas fa-chevron-right"></i>
          </div>
          <div className="menu-item" onClick={() => navigate('/my-sales')}>
            <i className="fas fa-store"></i>
            <span>我卖出的</span>
            <i className="fas fa-chevron-right"></i>
          </div>
          <div className="menu-item" onClick={() => navigate('/my-favorites')}>
            <i className="fas fa-heart"></i>
            <span>我的收藏</span>
            <i className="fas fa-chevron-right"></i>
          </div>
          <div className="menu-item" onClick={() => navigate('/my-posts')}>
            <i className="fas fa-plus-circle"></i>
            <span>我的发布</span>
            <i className="fas fa-chevron-right"></i>
          </div>
        </div>
      </div>
    </div>
  );
};

export default ProfilePage; 