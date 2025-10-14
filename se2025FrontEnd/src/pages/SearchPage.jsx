import { useState, useEffect } from 'react';
import { useNavigate } from 'react-router-dom';
import { useAppContext } from '../App';
import { get, PREFIX } from '../service/common';

const SearchPage = () => {
  const navigate = useNavigate();
  const [searchTerm, setSearchTerm] = useState('');
  const [category, setCategory] = useState('');
  const [minPrice, setMinPrice] = useState('');
  const [maxPrice, setMaxPrice] = useState('');
  const [condition, setCondition] = useState('');
  const [sortBy, setSortBy] = useState('relevance');
  const [searchResults, setSearchResults] = useState([]);
  const [categories, setCategories] = useState([]);
  const [loading, setLoading] = useState(false);
  const [totalElements, setTotalElements] = useState(0);
  const [currentPage, setCurrentPage] = useState(0);
  const [totalPages, setTotalPages] = useState(0);

  // 预定义的分类数据
  const predefinedCategories = [
    { id: 1, name: '📚 教材书籍', categoryName: 'books' },
    { id: 2, name: '💻 数码产品', categoryName: 'electronics' },
    { id: 3, name: '👕 服装配饰', categoryName: 'clothing' },
    { id: 4, name: '⚽ 运动用品', categoryName: 'sports' },
    { id: 5, name: '🏠 生活用品', categoryName: 'home' },
    { id: 6, name: '🎮 娱乐休闲', categoryName: 'entertainment' },
    { id: 7, name: '🚲 交通工具', categoryName: 'transport' },
    { id: 8, name: '🛋️ 家具家居', categoryName: 'furniture' },
    { id: 9, name: '👶 母婴用品', categoryName: 'baby' },
    { id: 10, name: '🐾 宠物用品', categoryName: 'pets' }
  ];

  // 页面加载时滚动到顶部
  useEffect(() => {
    window.scrollTo(0, 0);
    setCategories(predefinedCategories);
    // 初始加载时执行一次搜索（无关键词）
    performSearch();
  }, []);

  useEffect(() => {
    performSearch();
  }, [searchTerm, category, minPrice, maxPrice, condition, sortBy, currentPage]);

  const buildQueryParams = () => {
    const params = new URLSearchParams();
    
    if (searchTerm.trim()) {
      params.append('keyword', searchTerm.trim());
    }
    
    if (category) {
      params.append('categoryId', category);
    }
    
    if (minPrice) {
      params.append('minPrice', minPrice);
    }
    
    if (maxPrice) {
      params.append('maxPrice', maxPrice);
    }
    
    if (condition) {
      params.append('condition', condition);
    }
    
    params.append('page', currentPage.toString());
    params.append('size', '12');
    
    // 转换排序参数
    let sortParam = 'updateTime,desc'; // 默认值
    switch (sortBy) {
      case 'time-desc':
        sortParam = 'updateTime,desc';
        break;
      case 'price-asc':
        sortParam = 'price,asc';
        break;
      case 'price-desc':
        sortParam = 'price,desc';
        break;
      case 'relevance':
      default:
        sortParam = 'updateTime,desc';
        break;
    }
    params.append('sort', sortParam);
    
    return params.toString();
  };

  const performSearch = async () => {
    try {
      setLoading(true);
      const queryParams = buildQueryParams();
      const response = await get(`${PREFIX}/items/smart-search?${queryParams}`);
      
      if (response.ok) {
        const result = await response.json();
        if (result.success && result.data) {
          const { content, totalElements, totalPages, number } = result.data;
          setSearchResults(content || []);
          setTotalElements(totalElements || 0);
          setTotalPages(totalPages || 0);
          setCurrentPage(number || 0);
        } else {
          console.error('搜索失败:', result.message);
          setSearchResults([]);
          setTotalElements(0);
        }
      } else {
        console.error('搜索请求失败:', response.status);
        setSearchResults([]);
        setTotalElements(0);
      }
    } catch (error) {
      console.error('搜索异常:', error);
      setSearchResults([]);
      setTotalElements(0);
    } finally {
      setLoading(false);
    }
  };

  const handleProductClick = (productId) => {
    navigate(`/product/${productId}`);
  };

  const handleSearchSubmit = (e) => {
    e.preventDefault();
    setCurrentPage(0); // 重置到第一页
    performSearch();
  };

  const handleSortChange = (newSortBy) => {
    setSortBy(newSortBy);
    setCurrentPage(0); // 重置到第一页
  };

  const handleCategoryChange = (newCategory) => {
    setCategory(newCategory);
    setCurrentPage(0); // 重置到第一页
  };

  const handleConditionChange = (newCondition) => {
    setCondition(newCondition);
    setCurrentPage(0); // 重置到第一页
  };

  const handlePriceChange = () => {
    setCurrentPage(0); // 重置到第一页
  };

  return (
    <div className="page">
      <div className="search-container">
        <div className="search-page-header">
          <button className="back-btn" onClick={() => navigate('/', { replace: true })}>
            <i className="fas fa-arrow-left"></i>
          </button>
          <h2>搜索商品</h2>
          <button className="back-btn" onClick={() => navigate('/')}>
            <i className="fas fa-home"></i>
          </button>
        </div>
        
        {/* 搜索框区域 */}
        <div className="search-input-row">
          <div className="filter-item search-input-item">
            <label>关键词:</label>
            <div className="search-input-group">
              <input
                type="text"
                placeholder="搜索商品..."
                className="search-input-compact"
                value={searchTerm}
                onChange={(e) => setSearchTerm(e.target.value)}
                onKeyPress={(e) => e.key === 'Enter' && handleSearchSubmit(e)}
              />
              <button className="search-btn-compact" onClick={handleSearchSubmit}>
                <i className="fas fa-search"></i>
              </button>
            </div>
          </div>
        </div>
        
        {/* 筛选条件区域 */}
        <div className="search-filters">
          <div className="filter-item">
            <label>分类:</label>
            <select 
              value={category} 
              onChange={(e) => handleCategoryChange(e.target.value)}
            >
              <option value="">全部分类</option>
              {categories.map(cat => (
                <option key={cat.id} value={cat.id}>{cat.name}</option>
              ))}
            </select>
          </div>
          <div className="filter-item">
            <label>价格区间:</label>
            <div className="price-filter">
              <input
                type="number"
                placeholder="最低价"
                min="0"
                value={minPrice}
                onChange={(e) => {
                  setMinPrice(e.target.value);
                  handlePriceChange();
                }}
              />
              <span>-</span>
              <input
                type="number"
                placeholder="最高价"
                min="0"
                value={maxPrice}
                onChange={(e) => {
                  setMaxPrice(e.target.value);
                  handlePriceChange();
                }}
              />
            </div>
          </div>
          <div className="filter-item">
            <label>成色:</label>
            <select 
              value={condition} 
              onChange={(e) => handleConditionChange(e.target.value)}
            >
              <option value="">全部成色</option>
              <option value="全新">全新</option>
              <option value="九成新">九成新</option>
              <option value="八成新">八成新</option>
              <option value="七成新">七成新</option>
            </select>
          </div>
        </div>
        
        <div className="search-results">
          <div className="results-header">
            <h3>找到 {totalElements} 件商品</h3>
            <div className="results-sort">
              <select 
                value={sortBy} 
                onChange={(e) => handleSortChange(e.target.value)}
              >
                <option value="relevance">相关度</option>
                <option value="time-desc">最新发布</option>
                <option value="price-asc">价格从低到高</option>
                <option value="price-desc">价格从高到低</option>
              </select>
            </div>
          </div>
          {loading ? (
            <div className="loading-state">
              <i className="fas fa-spinner fa-spin"></i>
              <p>加载中...</p>
            </div>
          ) : (
            <div className="products-grid">
              {searchResults.map(product => (
                <div 
                  key={product.itemId} 
                  className="product-card"
                  onClick={() => handleProductClick(product.itemId)}
                >
                  <div className="product-image">
                    <img 
                      src={product.imageUrl || (product.imageUrls && product.imageUrls[0]) || 'https://via.placeholder.com/400x300'} 
                      alt={product.title} 
                    />
                  </div>
                  <div className="product-info">
                    <h3 className="product-title">{product.title}</h3>
                    <div className="product-price">¥{product.price}</div>
                    <div className="product-condition">{product.condition}</div>
                    {product.likes > 0 && (
                      <div className="product-likes">
                        <i className="fas fa-heart"></i> {product.likes}
                      </div>
                    )}
                  </div>
                </div>
              ))}
            </div>
          )}
          
          {/* 分页控件 */}
          {totalPages > 1 && (
            <div className="pagination">
              <button 
                onClick={() => setCurrentPage(currentPage - 1)}
                disabled={currentPage === 0}
                className="pagination-btn"
              >
                上一页
              </button>
              <span className="pagination-info">
                第 {currentPage + 1} 页，共 {totalPages} 页
              </span>
              <button 
                onClick={() => setCurrentPage(currentPage + 1)}
                disabled={currentPage >= totalPages - 1}
                className="pagination-btn"
              >
                下一页
              </button>
            </div>
          )}
        </div>
      </div>
    </div>
  );
};

export default SearchPage; 