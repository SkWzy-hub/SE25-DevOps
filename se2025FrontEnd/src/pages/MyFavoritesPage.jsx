import { useState, useEffect } from 'react';
import { useNavigate } from 'react-router-dom';
import { useAppContext } from '../App';
import { getUserFavorites, removeFavorite } from '../service/favorite';

const MyFavoritesPage = () => {
  const { currentUser, favorites, setFavorites } = useAppContext();
  const navigate = useNavigate();
  const [favoriteProducts, setFavoriteProducts] = useState([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState(null);

  // 页面加载时滚动到顶部并获取收藏数据
  useEffect(() => {
    window.scrollTo(0, 0);
    
    const fetchFavorites = async () => {
      try {
        setLoading(true);
        const data = await getUserFavorites();
        setFavoriteProducts(data);
        // 更新全局收藏状态
        setFavorites(data.map(item => item.itemId || item.id));
      } catch (err) {
        console.error('获取收藏列表失败:', err);
        setError('获取收藏列表失败，请稍后重试');
      } finally {
        setLoading(false);
      }
    };

    if (currentUser) {
      fetchFavorites();
    }
  }, [currentUser, setFavorites]);

  if (!currentUser) {
    navigate('/login');
    return null;
  }

  const handleProductClick = (productId) => {
    navigate(`/product/${productId}`);
  };

  const handleRemoveFavorite = async (productId, e) => {
    e.stopPropagation();
    try {
      const success = await removeFavorite(productId);
      if (success) {
        // 更新本地状态
        setFavoriteProducts(prev => prev.filter(p => p.itemId !== productId && p.id !== productId));
        // 更新全局收藏状态
        setFavorites(prev => prev.filter(id => id !== productId));
      }
    } catch (err) {
      console.error('取消收藏失败:', err);
      alert('取消收藏失败，请稍后重试');
    }
  };

  if (loading) {
    return (
      <div className="page">
        <div className="page-header">
          <button className="back-btn" onClick={() => navigate('/profile', { replace: true })}>
            <i className="fas fa-arrow-left"></i>
          </button>
          <h2>我的收藏</h2>
          <button className="back-btn" onClick={() => navigate('/')}>
            <i className="fas fa-home"></i>
          </button>
        </div>
        <div className="loading-state">
          <i className="fas fa-spinner fa-spin"></i>
          <p>加载中...</p>
        </div>
      </div>
    );
  }

  if (error) {
    return (
      <div className="page">
        <div className="page-header">
          <button className="back-btn" onClick={() => navigate('/profile', { replace: true })}>
            <i className="fas fa-arrow-left"></i>
          </button>
          <h2>我的收藏</h2>
          <button className="back-btn" onClick={() => navigate('/')}>
            <i className="fas fa-home"></i>
          </button>
        </div>
        <div className="error-state">
          <i className="fas fa-exclamation-circle"></i>
          <p>{error}</p>
          <button onClick={() => window.location.reload()}>重试</button>
        </div>
      </div>
    );
  }

  return (
    <div className="page">
      <div className="page-header">
        <button className="back-btn" onClick={() => navigate('/profile', { replace: true })}>
          <i className="fas fa-arrow-left"></i>
        </button>
        <h2>我的收藏</h2>
        <button className="back-btn" onClick={() => navigate('/')}>
          <i className="fas fa-home"></i>
        </button>
      </div>
      
      <div className="products-grid">
        {favoriteProducts.map(product => (
          <div 
            key={product.itemId || product.id} 
            className="product-card"
            onClick={() => handleProductClick(product.itemId || product.id)}
          >
            <div className="product-image">
              <img src={product.imageUrl || product.images?.[0]} alt={product.itemName || product.title} />
              <button 
                className="favorite-btn active"
                onClick={(e) => handleRemoveFavorite(product.itemId || product.id, e)}
              >
                <i className="fas fa-heart"></i>
              </button>
            </div>
            <div className="product-info">
              <h3 className="product-title">{product.itemName || product.title}</h3>
              <div className="product-price">¥{product.price}</div>
              
              <div className="product-stats">
                <span><i className="fas fa-heart"></i> {product.likes || 0}</span>
              </div>
            </div>
          </div>
        ))}
      </div>
      
      {favoriteProducts.length === 0 && (
        <div className="empty-state">
          <i className="fas fa-heart"></i>
          <h3>暂无收藏</h3>
          <p>快去收藏心仪的商品吧</p>
        </div>
      )}
    </div>
  );
};

export default MyFavoritesPage; 