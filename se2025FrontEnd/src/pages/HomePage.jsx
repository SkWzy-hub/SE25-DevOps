import { useState, useEffect } from 'react';
import { useNavigate } from 'react-router-dom';
import { useAppContext } from '../App';
import { getItemsPage, getItemsByCategoryPaged } from '../service/item';
import Pagination from '../components/Pagination';

const HomePage = () => {
  const { currentUser } = useAppContext();
  const navigate = useNavigate();
  
  // 排序和筛选状态

  const [sortBy, setSortBy] = useState('updateTime');

  const [sortDirection, setSortDirection] = useState('DESC');
  const [selectedCategory, setSelectedCategory] = useState(null);
  
  // 分页和数据状态
  const [currentPage, setCurrentPage] = useState(0);

  const [pageSize] = useState(6); // 每页显示6个商品
  const [pageData, setPageData] = useState({
    content: [],
    page: 0,
    size: 6,

    totalElements: 0,
    totalPages: 0,
    first: true,
    last: true,
    empty: true
  });
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState(null);

  // 页面加载时滚动到顶部
  useEffect(() => {
    window.scrollTo(0, 0);
  }, []);


  const fetchItems = async (resetPage = false) => {
    try {
      setLoading(true);
      setError(null);

      const pageToFetch = resetPage ? 0 : currentPage;

      const params = {
        page: pageToFetch,
        size: pageSize,
        sortBy: sortBy,
        sortDirection: sortDirection
      };

      let response;
      if (selectedCategory) {
        response = await getItemsByCategoryPaged(selectedCategory, params);
      } else {
        response = await getItemsPage(params);
      }
      if (resetPage) {
      setPageData(response);
        if (currentPage !== 0) setCurrentPage(0);
      } else {
        setPageData(prev => ({
          ...response,
          content: [...prev.content, ...response.content],
          page: response.page,
          size: response.size
        }));
      }
    } catch (err) {
      console.error('获取商品失败:', err);
      setError('获取商品失败，请稍后重试');
      setPageData({
        content: [],
        page: 0,
        size: 6,
        totalElements: 0,
        totalPages: 0,
        first: true,
        last: true,
        empty: true
      });
    } finally {
      setLoading(false);
    }
  };

  // 初始加载和筛选条件变化时重新获取数据
  useEffect(() => {
    setPageData({
      content: [],
      page: 0,
      size: pageSize,
      totalElements: 0,
      totalPages: 0,
      first: true,
      last: true,
      empty: true
    });
    setCurrentPage(0);
    fetchItems(true); // 重置到第一页
  }, [sortBy, sortDirection, selectedCategory]);

  // 滚动到底部自动加载下一页
  useEffect(() => {
    const handleScroll = () => {
      if (loading || error) return;
      if (!pageData || pageData.last) return;
      const scrollTop = window.scrollY || document.documentElement.scrollTop;
      const windowHeight = window.innerHeight || document.documentElement.clientHeight;
      const docHeight = document.documentElement.scrollHeight;
      // 距底部200px时触发
      if (scrollTop + windowHeight >= docHeight - 200) {
        setCurrentPage((prev) => {
          if (pageData.last) return prev;
          return prev + 1;
        });
      }
    };
    window.addEventListener('scroll', handleScroll);
    return () => window.removeEventListener('scroll', handleScroll);
  }, [loading, error, pageData]);

  // 支持自动加载下一页，合并内容
  useEffect(() => {
    if (currentPage > 0) {

      fetchItems(false);
    }
  }, [currentPage]);

  const handleProductClick = (productId) => {
    navigate(`/product/${productId}`);
  };

  const handleCategoryClick = (categoryId) => {
    setSelectedCategory(selectedCategory === categoryId ? null : categoryId);
  };

  const handleSortChange = (newSortBy) => {
    if (newSortBy === sortBy) {

      setSortDirection(sortDirection === 'DESC' ? 'ASC' : 'DESC');
    } else {

      setSortBy(newSortBy);
      setSortDirection('DESC');
    }
  };

  const handleClearSort = () => {

    setSortBy('updateTime');

    setSortDirection('DESC');
    setSelectedCategory(null);
  };



  // 分类数据
  const categories = [
    { id: 1, name: 'books', icon: 'fas fa-book' },
    { id: 2, name: 'electronics', icon: 'fas fa-laptop' },
    { id: 3, name: 'clothing', icon: 'fas fa-tshirt' },
    { id: 4, name: 'sports', icon: 'fas fa-dumbbell' },
    { id: 5, name: 'home', icon: 'fas fa-home' },
    { id: 6, name: 'entertainment', icon: 'fas fa-gamepad' },
    { id: 7, name: 'transport', icon: 'fas fa-bicycle' },
    { id: 8, name: 'furniture', icon: 'fas fa-couch' },
    { id: 9, name: 'baby', icon: 'fas fa-baby' },
    { id: 10, name: 'pets', icon: 'fas fa-dog' }
  ];

  return (
    <div className="page">
      {/* 英雄区域 */}
      <div className="hero-section">
        <h1>
          <img src="/grape.png" alt="葡萄" className="grape-icon hero-grape" />
          交大葡tao zhen hao
          <img src="/grape.png" alt="葡萄" className="grape-icon hero-grape" />
        </h1>
        <p>
          <i className="fas fa-university" style={{color: '#3498db', marginRight: '0.25rem'}}></i>
          交大校园二手交易平台，像葡萄一样甜美的淘宝体验
          <i className="fas fa-heart" style={{color: '#e74c3c', marginLeft: '0.25rem'}}></i>
        </p>
      </div>
      
      {/* 分类区域 */}
      <div className="categories">
        {categories.map(category => (
          <div 
            key={category.id} 
            className={`category-item ${selectedCategory === category.id ? 'active' : ''}`}
            onClick={() => handleCategoryClick(category.id)}
          >
            <i className={category.icon}></i>
            <span>{category.name}</span>
          </div>
        ))}
      </div>
      
      {/* 商品区域 */}
      <div className="products-section">
        <div className="products-header">
          <h2>
            商品 
            {!loading && (
              <span className="product-count">
                (共{pageData.totalElements}件)
              </span>
            )}
          </h2>
          
          <div className="products-controls">
            <div className="sort-controls">
              <button 

                className={`sort-btn ${sortBy === 'updateTime' ? 'active' : ''}`}
                onClick={() => handleSortChange('updateTime')}
              >
                更新时间
                {sortBy === 'updateTime' && (

                  <i className={`fas fa-sort-${sortDirection === 'DESC' ? 'down' : 'up'}`}></i>
                )}
              </button>
              <button 
                className={`sort-btn ${sortBy === 'price' ? 'active' : ''}`}
                onClick={() => handleSortChange('price')}
              >
                价格
                {sortBy === 'price' && (
                  <i className={`fas fa-sort-${sortDirection === 'DESC' ? 'down' : 'up'}`}></i>
                )}
              </button>
              <button 
                className={`sort-btn ${sortBy === 'likes' ? 'active' : ''}`}
                onClick={() => handleSortChange('likes')}
              >
                收藏数
                {sortBy === 'likes' && (
                  <i className={`fas fa-sort-${sortDirection === 'DESC' ? 'down' : 'up'}`}></i>
                )}
              </button>
            </div>
            
            <div className="filter-controls">
              <button 
                onClick={handleClearSort} 
                className="clear-btn"
                disabled={loading}
              >
                清除筛选
              </button>
            </div>
          </div>
        </div>
        
        {/* 加载状态 */}
        {loading && (
          <div className="loading-state">
            <i className="fas fa-spinner fa-spin"></i>
            <p>加载中...</p>
          </div>
        )}
        
        {/* 错误状态 */}
        {error && (
          <div className="error-state">
            <i className="fas fa-exclamation-circle"></i>
            <p>{error}</p>
            <button onClick={() => fetchItems(true)}>重试</button>
          </div>
        )}
        
        {/* 商品网格 */}
        {!loading && !error && (
          <>
            <div className="products-grid">
              {pageData.content.map(product => (
                <div 
                  key={product.itemId || product.id} 
                  className="product-card"
                  onClick={() => handleProductClick(product.itemId || product.id)}
                >
                  <div className="product-image">
                    <img 
                      src={product.imageUrl || product.imageUrls?.[0] || '/grape.png'} 
                      alt={product.itemName || product.title}
                      className="product-card-image"
                      onError={(e) => {
                        e.target.src = '/grape.png';
                      }}
                      style={{
                        width: '100%',
                        height: '100%',
                        objectFit: 'cover'
                      }}
                    />
                  </div>
                  <div className="product-info">
                    <h3 className="product-title">{product.itemName || product.title}</h3>
                    <div className="product-price">¥{product.price}</div>
                    <div className="product-meta">
                      <span className="product-condition">{product.itemCondition || product.condition}</span>
                      <span className="product-likes">
                        <i className="fas fa-heart"></i> {product.likes || 0}
                      </span>
                    </div>
                  </div>
                </div>
              ))}
            </div>
            
            {/* 空状态 */}
            {pageData.empty && (
              <div className="empty-state">
                <i className="fas fa-box-open"></i>
                <h3>暂无商品</h3>
                <p>没有找到符合条件的商品</p>
              </div>
            )}
            

            {/* 分页导航 */}
            {/* <Pagination
              currentPage={pageData.page}
              totalPages={pageData.totalPages}
              totalElements={pageData.totalElements}
              pageSize={pageData.size}
              onPageChange={handlePageChange}
              disabled={loading}
            /> */}

          </>
        )}
      </div>
    </div>
  );
};

export default HomePage; 