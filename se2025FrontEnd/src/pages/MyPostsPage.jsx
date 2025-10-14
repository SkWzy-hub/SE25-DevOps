import { useState, useEffect } from 'react';
import { useNavigate } from 'react-router-dom';
import { useAppContext } from '../App';

import { getUserItems, deleteItem, toggleItemAvailability } from '../service/item';


const MyPostsPage = () => {
  const { currentUser } = useAppContext();
  const navigate = useNavigate();
  const [activeTab, setActiveTab] = useState('all');
  const [myPosts, setMyPosts] = useState([]);
  const [loading, setLoading] = useState(true);

  const [error, setError] = useState(null);
  const [actionLoading, setActionLoading] = useState({});


  // 页面加载时滚动到顶部
  useEffect(() => {
    window.scrollTo(0, 0);
  }, []);


  // 获取用户发布的商品
  useEffect(() => {
    const fetchUserItems = async () => {
      if (!currentUser) return;
      
      try {
        setLoading(true);
        setError(null);
        const items = await getUserItems(currentUser.id || 1);
        console.log('Received user items:', items);
        
        // 转换后端数据格式为前端需要的格式
        const formattedItems = items.map(item => ({
          id: item.itemId,
          itemId: item.itemId,
          title: item.title,
          price: item.price,
          category: item.categoryName,
          condition: item.condition,
          description: item.description,
          images: item.imageUrls || [],
          imageUrl: item.imageUrls?.[0] || '',
          status: item.isAvailable ? 'selling' : 'offline',
          favorites: item.likes || 0,
          createTime: item.createTime,
          sellerName: item.sellerName
        }));
        
        setMyPosts(formattedItems);
      } catch (err) {
        console.error('获取用户商品失败:', err);
        setError('获取商品列表失败，请稍后重试');
      } finally {
        setLoading(false);
      }
    };

    fetchUserItems();
  }, [currentUser]);


  if (!currentUser) {
    navigate('/login');
    return null;
  }


  // 统计数据
  const stats = {
    published: myPosts.length,
    selling: myPosts.filter(post => post.status === 'selling').length,
    offline: myPosts.filter(post => post.status === 'offline').length

  };

  // 根据选项卡筛选商品
  const filteredPosts = myPosts.filter(post => {
    switch (activeTab) {
      case 'selling':
        return post.status === 'selling';
      case 'offline':

        return post.status === 'offline';
      default:
        return true;
    }
  });


  const getStatusBadge = (status) => {
    const badges = {
      selling: { text: '在售中', class: 'selling' },
      offline: { text: '已下架', class: 'offline' }
    };
    return badges[status] || { text: status, class: '' };

  };

  const getTabName = (tab) => {
    const names = {
      all: '全部',
      selling: '在售中',
      offline: '已下架'
    };
    return names[tab] || tab;
  };

  const handleProductClick = (productId) => {
    navigate(`/product/${productId}`);
  };

  const handleEditProduct = (e, productId) => {
    e.stopPropagation(); // 阻止事件冒泡
    navigate(`/publish?edit=${productId}`);
  };

  const handleDeleteProduct = async (e, productId) => {
    e.stopPropagation(); // 阻止事件冒泡

    
    if (!window.confirm('确定要删除这个商品吗？删除后无法恢复。')) {
      return;
    }

    try {
      setActionLoading(prev => ({ ...prev, [`delete_${productId}`]: true }));
      
      const success = await deleteItem(productId, currentUser.id || 1, false);
      
      if (success) {
        // 从本地状态中移除已删除的商品
        setMyPosts(prev => prev.filter(post => post.id !== productId));
        alert('商品删除成功！');
      } else {
        throw new Error('删除操作失败');
      }
    } catch (error) {
      console.error('删除商品失败:', error);
      
      let errorMessage = '删除商品失败，请稍后重试';
      if (error.response?.data?.message) {
        errorMessage = error.response.data.message;
      } else if (error.message) {
        errorMessage = error.message;
      }
      
      alert(errorMessage);
    } finally {
      setActionLoading(prev => ({ ...prev, [`delete_${productId}`]: false }));
    }
  };

  const handleToggleStatus = async (e, productId, currentStatus) => {
    e.stopPropagation(); // 阻止事件冒泡
    
    const isCurrentlySelling = currentStatus === 'selling';
    const actionText = isCurrentlySelling ? '下架' : '重新上架';
    const confirmText = `确定要${actionText}这个商品吗？`;
    
    if (!window.confirm(confirmText)) {
      return;
    }

    try {
      setActionLoading(prev => ({ ...prev, [`toggle_${productId}`]: true }));
      
      const newAvailability = !isCurrentlySelling;
      // 调用item.js中的toggleItemAvailability
      const success = await toggleItemAvailability(
        productId, 
        newAvailability, 
        currentUser.id || 1,
        isCurrentlySelling ? '用户手动下架' : '用户重新上架'
      );
      
      if (success) {
        // 更新本地状态
        setMyPosts(prev => prev.map(post => 
          post.id === productId 
            ? { ...post, status: newAvailability ? 'selling' : 'offline' }
            : post
        ));
        alert(`商品${actionText}成功！`);
      } else {
        throw new Error(`${actionText}操作失败`);
      }
    } catch (error) {
      console.error(`${actionText}商品失败:`, error);
      
      let errorMessage = `${actionText}商品失败，请稍后重试`;
      if (error.response?.data?.message) {
        errorMessage = error.response.data.message;
      } else if (error.message) {
        errorMessage = error.message;
      }
      
      alert(errorMessage);
    } finally {
      setActionLoading(prev => ({ ...prev, [`toggle_${productId}`]: false }));
    }
  };

  const handlePublishNew = () => {
    navigate('/publish');
  };

  if (loading) {
    return (
      <div className="page">
        <div className="posts-page-header">
          <div className="posts-nav">
            <button className="back-btn" onClick={() => navigate('/profile', { replace: true })}>
              <i className="fas fa-arrow-left"></i>
            </button>
            <button className="back-btn" onClick={() => navigate('/')}>
              <i className="fas fa-home"></i>
            </button>
          </div>
          
          <div className="posts-title-section">
            <h2 className="posts-main-title">我的发布</h2>
          </div>
        </div>
        
        <div style={{ padding: '2rem', textAlign: 'center' }}>
          <div className="loading-spinner"></div>
          <p>正在加载商品列表...</p>
        </div>
      </div>
    );
  }

  if (error) {
    return (
      <div className="page">
        <div className="posts-page-header">
          <div className="posts-nav">
            <button className="back-btn" onClick={() => navigate('/profile', { replace: true })}>
              <i className="fas fa-arrow-left"></i>
            </button>
            <button className="back-btn" onClick={() => navigate('/')}>
              <i className="fas fa-home"></i>
            </button>
          </div>
          
          <div className="posts-title-section">
            <h2 className="posts-main-title">我的发布</h2>
          </div>
        </div>
        
        <div style={{ padding: '2rem', textAlign: 'center' }}>
          <i className="fas fa-exclamation-circle" style={{ fontSize: '3rem', color: '#e74c3c', marginBottom: '1rem' }}></i>
          <h3>{error}</h3>
          <button 
            onClick={() => window.location.reload()}
            style={{ 
              padding: '0.5rem 1rem', 
              marginTop: '1rem',
              background: '#007bff',
              color: 'white',
              border: 'none',
              borderRadius: '4px',
              cursor: 'pointer'
            }}
          >
            重试
          </button>
        </div>
      </div>
    );
  }

  return (
    <div className="page">
      <div className="posts-page-header">
        <div className="posts-nav">
          <button className="back-btn" onClick={() => navigate('/profile', { replace: true })}>
            <i className="fas fa-arrow-left"></i>
          </button>
          <button className="back-btn" onClick={() => navigate('/')}>
            <i className="fas fa-home"></i>
          </button>
        </div>
        
        <div className="posts-title-section">
          <h2 className="posts-main-title">我的发布</h2>
          <button className="publish-btn" onClick={handlePublishNew}>
            <i className="fas fa-plus"></i>
            发布新商品
          </button>
        </div>
      </div>
      
      {/* 统计卡片 */}
      <div className="posts-stats">
        <div className="stat-card">
          <div className="stat-icon">
            <i className="fas fa-box"></i>
          </div>
          <div className="stat-info">
            <div className="stat-number">{stats.published}</div>
            <div className="stat-label">总发布</div>
          </div>
        </div>
        <div className="stat-card">
          <div className="stat-icon">
            <i className="fas fa-store"></i>
          </div>
          <div className="stat-info">
            <div className="stat-number">{stats.selling}</div>
            <div className="stat-label">在售中</div>
          </div>
        </div>
        <div className="stat-card">
          <div className="stat-icon">
            <i className="fas fa-archive"></i>
          </div>
          <div className="stat-info">
            <div className="stat-number">{stats.offline}</div>
            <div className="stat-label">已下架</div>
          </div>
        </div>
      </div>
      
      {/* 状态筛选标签页 */}
      <div className="posts-tabs">
        {['all', 'selling', 'offline'].map(tab => (
          <button
            key={tab}
            className={`tab-btn ${activeTab === tab ? 'active' : ''}`}
            onClick={() => setActiveTab(tab)}
          >
            {getTabName(tab)}
          </button>
        ))}
      </div>
      
      {/* 商品列表 */}
      <div className="posts-grid">
        {filteredPosts.map(product => {
          const badge = getStatusBadge(product.status);
          const isDeleteLoading = actionLoading[`delete_${product.id}`];
          const isToggleLoading = actionLoading[`toggle_${product.id}`];
          
          return (
            <div 
              key={product.id} 
              className="enhanced-card"
              onClick={() => handleProductClick(product.id)}
            >
              <div className="product-image">
                <img 
                  src={product.imageUrl || product.images?.[0] || '/default-product-image.png'} 
                  alt={product.title}
                  onError={(e) => {
                    e.target.src = '/default-product-image.png';
                  }}
                />
                <div className="product-actions">
                  <button 
                    className="btn-edit"
                    onClick={(e) => handleEditProduct(e, product.id)}
                    title="编辑商品"
                    disabled={isDeleteLoading || isToggleLoading}
                  >
                    <i className="fas fa-edit"></i>
                  </button>
                  
                  {/* 根据商品状态显示上架/下架按钮 */}
                  {product.status === 'selling' ? (
                    <button 
                      className="btn-offline"
                      onClick={(e) => handleToggleStatus(e, product.id, product.status)}
                      title="下架商品"
                      disabled={isDeleteLoading || isToggleLoading}
                    >
                      {isToggleLoading ? (
                        <i className="fas fa-spinner fa-spin"></i>
                      ) : (
                        <i className="fas fa-eye-slash"></i>
                      )}
                    </button>
                  ) : (
                    <button 
                      className="btn-online"
                      onClick={(e) => handleToggleStatus(e, product.id, product.status)}
                      title="重新上架"
                      disabled={isDeleteLoading || isToggleLoading}
                    >
                      {isToggleLoading ? (
                        <i className="fas fa-spinner fa-spin"></i>
                      ) : (
                        <i className="fas fa-eye"></i>
                      )}
                    </button>
                  )}
                  
                  <button 
                    className="btn-delete"
                    onClick={(e) => handleDeleteProduct(e, product.id)}
                    title="删除商品"
                    disabled={isDeleteLoading || isToggleLoading}
                  >
                    {isDeleteLoading ? (
                      <i className="fas fa-spinner fa-spin"></i>
                    ) : (
                      <i className="fas fa-trash"></i>
                    )}
                  </button>
                </div>
              </div>
              
              <div className="product-info">
                <h3 className="product-title">{product.title}</h3>
                <div className="product-price">¥{product.price}</div>
                <div className="product-stats">
                  <span><i className="fas fa-heart"></i> {product.favorites || 0}</span>
                  <span className={`status-badge ${badge.class}`}>
                    {badge.text}
                  </span>
                </div>
              </div>
            </div>
          );
        })}
      </div>
      

      {filteredPosts.length === 0 && !loading && (

        <div className="empty-state">
          <i className="fas fa-box-open"></i>
          <h3>
            {activeTab === 'all' && '暂无商品'}
            {activeTab === 'selling' && '暂无在售商品'}
            {activeTab === 'offline' && '暂无下架商品'}
          </h3>
          <p>
            {activeTab === 'all' && '快去发布你的闲置商品吧'}
            {activeTab === 'selling' && '暂时没有在售的商品'}
            {activeTab === 'offline' && '暂时没有下架的商品'}
          </p>
          {activeTab === 'all' && (
            <button 
              onClick={handlePublishNew}
              style={{ 
                padding: '0.5rem 1rem', 
                marginTop: '1rem',
                background: '#007bff',
                color: 'white',
                border: 'none',
                borderRadius: '4px',
                cursor: 'pointer'
              }}
            >
              发布新商品
            </button>
          )}
        </div>
      )}
    </div>
  );
};

export default MyPostsPage; 