import { BrowserRouter as Router, Routes, Route, Navigate, useLocation } from 'react-router-dom';
import { useState, createContext, useContext, useEffect } from 'react';
import { PREFIX, TokenService } from './service/common';
import './App.css';

// 导入组件
import Navbar from './components/Navbar';
import HomePage from './pages/HomePage';
import LoginPage from './pages/LoginPage';
import RegisterPage from './pages/RegisterPage';
import ProfilePage from './pages/ProfilePage';
import ProfileDetailPage from './pages/ProfileDetailPage';
import MyPurchasesPage from './pages/MyPurchasesPage';
import MySalesPage from './pages/MySalesPage';
import MyFavoritesPage from './pages/MyFavoritesPage';
import MyPostsPage from './pages/MyPostsPage';
import ProductDetailPage from './pages/ProductDetailPage';
import OrderDetailPage from './pages/OrderDetailPage';
import PublishPage from './pages/PublishPage';
import SearchPage from './pages/SearchPage';
import ChangePasswordPage from './pages/ChangePasswordPage';

// 应用状态上下文
const AppContext = createContext();

export const useAppContext = () => {
  const context = useContext(AppContext);
  if (!context) {
    throw new Error('useAppContext must be used within an AppProvider');
  }
  return context;
};

// 滚动到顶部组件
const ScrollToTop = () => {
  const { pathname } = useLocation();

  useEffect(() => {
    window.scrollTo(0, 0);
  }, [pathname]);

  return null;
};

function App() {
  // 应用状态
  const [currentUser, setCurrentUser] = useState(null);
  const [products, setProducts] = useState([]);
  const [orders, setOrders] = useState([]);
  const [favorites, setFavorites] = useState([]);
  const [posts, setPosts] = useState([]);

  // 应用启动时从localStorage恢复用户信息
  useEffect(() => {
    const savedUser = localStorage.getItem('currentUser');
    if (savedUser) {
      try {
        const user = JSON.parse(savedUser);
        setCurrentUser(user);
      } catch (error) {
        console.error('恢复用户信息失败:', error);
        localStorage.removeItem('currentUser');
      }
    }
  }, []);

  // 页面关闭时自动退出登录
  useEffect(() => {
    const handleBeforeUnload = (event) => {
      // 只有在用户已登录时才执行退出登录
      const token = TokenService.getToken();
      if (currentUser && token) {
        try {
          // 使用专门的API端点和sendBeacon发送退出登录请求
          const logoutData = new Blob([JSON.stringify({ token: token })], {
            type: 'application/json'
          });
          
          navigator.sendBeacon(`${PREFIX}/logout-on-close`, logoutData);
          
          // 立即清理本地存储
          TokenService.removeToken();
          localStorage.removeItem('currentUser');
          
          console.log('页面关闭，已自动退出登录');
        } catch (error) {
          console.error('自动退出登录失败:', error);
        }
      }
    };

    const handleVisibilityChange = () => {
      // 当页面变为不可见且用户已登录时，也执行退出登录
      const token = TokenService.getToken();
      if (document.visibilityState === 'hidden' && currentUser && token) {
        try {
          const logoutData = new Blob([JSON.stringify({ token: token })], {
            type: 'application/json'
          });
          
          navigator.sendBeacon(`${PREFIX}/logout-on-close`, logoutData);
          
          // 延迟清理本地存储，避免用户只是切换标签页的情况
          setTimeout(() => {
            if (document.visibilityState === 'hidden') {
              TokenService.removeToken();
              localStorage.removeItem('currentUser');
              setCurrentUser(null);
            }
          }, 3000); // 3秒后如果页面仍然隐藏，则清理本地数据
          
        } catch (error) {
          console.error('页面隐藏时自动退出登录失败:', error);
        }
      }
    };

    // 添加事件监听器
    window.addEventListener('beforeunload', handleBeforeUnload);
    document.addEventListener('visibilitychange', handleVisibilityChange);

    // 清理事件监听器
    return () => {
      window.removeEventListener('beforeunload', handleBeforeUnload);
      document.removeEventListener('visibilitychange', handleVisibilityChange);
    };
  }, [currentUser]); // 依赖currentUser，当用户状态改变时重新绑定事件

  // 应用状态值
  const appState = {
    currentUser,
    setCurrentUser,
    products,
    setProducts,
    orders,
    setOrders,
    favorites,
    setFavorites,
    posts,
    setPosts
  };

  return (
    <AppContext.Provider value={appState}>
      <Router>
        <div className="App">
          <ScrollToTop />
          <Navbar />
          <main className="main-content">
            <Routes>
              <Route path="/" element={<HomePage />} />
              <Route path="/login" element={<LoginPage />} />
              <Route path="/register" element={<RegisterPage />} />
              <Route path="/profile" element={<ProfilePage />} />
              <Route path="/profile-detail" element={<ProfileDetailPage />} />
              <Route path="/my-purchases" element={<MyPurchasesPage />} />
              <Route path="/my-sales" element={<MySalesPage />} />
              <Route path="/my-favorites" element={<MyFavoritesPage />} />
              <Route path="/my-posts" element={<MyPostsPage />} />
              <Route path="/product/:id" element={<ProductDetailPage />} />
              <Route path="/order/:id" element={<OrderDetailPage />} />
              <Route path="/search" element={<SearchPage />} />
              <Route path="/publish" element={<PublishPage />} />
              <Route path="/change-password" element={<ChangePasswordPage />} />
              <Route path="*" element={<Navigate to="/" replace />} />
            </Routes>
          </main>
        </div>
      </Router>
    </AppContext.Provider>
  );
}

export default App;
