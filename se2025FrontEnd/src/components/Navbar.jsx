import { useState, useEffect, useRef } from 'react';
import { Link, useNavigate, useLocation } from 'react-router-dom';
import { useAppContext } from '../App';
import { TokenService } from '../service/common';
import { UserAPI } from '../service/api';

const Navbar = () => {
  const { currentUser, setCurrentUser } = useAppContext();
  const navigate = useNavigate();
  const location = useLocation();
  const [dropdownOpen, setDropdownOpen] = useState(false);
  const dropdownRef = useRef(null);

  // 判断是否显示搜索框
  const shouldShowSearch = location.pathname === '/';

  // 点击外部关闭下拉菜单
  useEffect(() => {
    const handleClickOutside = (event) => {
      if (dropdownRef.current && !dropdownRef.current.contains(event.target)) {
        setDropdownOpen(false);
      }
    };

    document.addEventListener('mousedown', handleClickOutside);
    return () => document.removeEventListener('mousedown', handleClickOutside);
  }, []);

  const handleSearchClick = () => {
    navigate('/search');
  };

  const handleLogout = async () => {
    try {
      // 调用后端logout API清除Redis缓存
      await UserAPI.logout();
      console.log('后端logout成功，Redis缓存已清除');
    } catch (error) {
      console.error('后端logout失败:', error);
      // 即使后端失败也继续清除前端状态
    }
    
    // 清除前端状态
    setCurrentUser(null);
    TokenService.removeToken();
    localStorage.removeItem('currentUser');
    setDropdownOpen(false);
    navigate('/');
  };

  return (
    <nav className="navbar">
      <div className="nav-container">
        <div className="nav-logo">
          <img src="/grape.png" alt="葡萄" className="grape-icon" />
          <span>交大葡淘</span>
          <img src="/grape.png" alt="葡萄" className="grape-icon" />
        </div>
        
        {shouldShowSearch && (
          <div className="nav-search">
            <input 
              type="text" 
              placeholder="搜索商品..." 
              className="search-input"
              onClick={handleSearchClick}
              readOnly
            />
            <button className="search-btn" onClick={handleSearchClick}>
              <i className="fas fa-search"></i>
            </button>
          </div>
        )}
        
        <div className="nav-user">
          <div className="user-dropdown" ref={dropdownRef}>
            <span 
              className="user-info" 
              onClick={() => setDropdownOpen(!dropdownOpen)}
            >
              <i className="fas fa-user"></i>
              <span>{currentUser ? currentUser.username : '未登录'}</span>
              <i className="fas fa-chevron-down"></i>
            </span>
            
            {dropdownOpen && (
              <div className="dropdown-menu">
                {!currentUser ? (
                  <div className="dropdown-content">
                    <Link to="/login" onClick={() => setDropdownOpen(false)}>登录</Link>
                    <Link to="/register" onClick={() => setDropdownOpen(false)}>注册</Link>
                  </div>
                ) : (
                  <div className="dropdown-content">
                    <Link to="/profile" onClick={() => setDropdownOpen(false)}>个人信息</Link>
                    <button onClick={handleLogout}>退出登录</button>
                  </div>
                )}
              </div>
            )}
          </div>
        </div>
      </div>
    </nav>
  );
};

export default Navbar; 