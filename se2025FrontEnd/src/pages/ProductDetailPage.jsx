import { useState, useEffect } from 'react';
import { useParams, useNavigate } from 'react-router-dom';
import { useAppContext } from '../App';


import { buyProduct } from '../service/order';

import { getItemDetail } from '../service/item';
import { checkFavoriteStatus, addFavorite, removeFavorite } from '../service/favorite';
import { getItemMessages, addMessage, deleteMessage, getMessageReplies } from '../service/message';

// CSS for loading spinner
const spinnerStyles = `
  @keyframes spin {
    0% { transform: rotate(0deg); }
    100% { transform: rotate(360deg); }
  }
  
  .loading-spinner {
    width: 50px;
    height: 50px;
    border: 5px solid #f3f3f3;
    border-top: 5px solid #3498db;
    border-radius: 50%;
    animation: spin 1s linear infinite;
    margin: 20px auto;
  }
`;

// Add styles to document
const styleSheet = document.createElement("style");
styleSheet.innerText = spinnerStyles;
document.head.appendChild(styleSheet);


const ProductDetailPage = () => {
  const { id } = useParams();
  const { currentUser } = useAppContext();
  const navigate = useNavigate();
  const [product, setProduct] = useState(null);
  const [isFavorited, setIsFavorited] = useState(false);
  const [currentImageIndex, setCurrentImageIndex] = useState(0);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState(null);
  
  // 评论相关状态
  const [messages, setMessages] = useState([]);
  const [newMessage, setNewMessage] = useState('');
  const [replyTo, setReplyTo] = useState(null);
  const [loadingMessages, setLoadingMessages] = useState(false);

  // 页面加载时滚动到顶部
  useEffect(() => {
    window.scrollTo(0, 0);
  }, []);

  // 获取商品详情和收藏状态
  useEffect(() => {
    const fetchProductDetails = async () => {
      try {
        setLoading(true);
        setError(null);
        
        if (!id) {
          setError('商品ID无效');
          return;
        }

        const productData = await getItemDetail(id);
        console.log('Received product data:', productData);
        
        if (!productData) {
          setError('商品未找到');
          setTimeout(() => navigate('/'), 3000);
          return;
        }

        if (!productData.itemId) {
          setError('商品数据格式不正确');
          return;
        }

        setProduct(productData);
        
        if (currentUser) {
          try {
            console.log('Checking favorite status for itemId:', productData.itemId);
            const favoriteStatus = await checkFavoriteStatus(productData.itemId);
            setIsFavorited(favoriteStatus);
          } catch (favError) {
            console.error('Error checking favorite status:', favError);
            // 不影响主要功能，所以只记录错误
          }
        }
      } catch (err) {
        console.error('Error fetching product details:', err);
        setError(err.message || '加载商品详情失败');
      } finally {
        setLoading(false);
      }
    };

    fetchProductDetails();
  }, [id, currentUser, navigate]);

  // 加载评论
  useEffect(() => {
    const fetchMessages = async () => {
      if (!id) return;
      
      try {
        setLoadingMessages(true);
        const messagesData = await getItemMessages(id);
        
        // 获取每条根留言的回复
        const messagesWithReplies = await Promise.all(
          messagesData.map(async (message) => {
            const replies = await getMessageReplies(message.messageId);
            return { ...message, replies };
          })
        );
        
        setMessages(messagesWithReplies);
      } catch (err) {
        console.error('加载评论失败:', err);
      } finally {
        setLoadingMessages(false);
      }
    };

    fetchMessages();
  }, [id]);

  if (loading) {
    return (
      <div className="page">
        <div className="page-header">
          <button className="back-btn" onClick={() => navigate('/')}>
            <i className="fas fa-arrow-left"></i>
          </button>
          <h2>商品详情</h2>
        </div>
        <div style={{ padding: '2rem', textAlign: 'center' }}>
          <h3>加载中...</h3>
          <div className="loading-spinner"></div>
        </div>
      </div>
    );
  }

  if (error || !product) {
    return (
      <div className="page">
        <div className="page-header">
          <button className="back-btn" onClick={() => navigate('/')}>
            <i className="fas fa-arrow-left"></i>
          </button>
          <h2>商品详情</h2>
        </div>
        <div style={{ padding: '2rem', textAlign: 'center' }}>
          <h3>{error || '商品未找到'}</h3>
          <p>商品ID: {id}</p>
          <button 
            onClick={() => navigate('/')} 
            style={{ 
              padding: '1rem 2rem', 
              marginTop: '1rem',
              background: '#007bff',
              color: 'white',
              border: 'none',
              borderRadius: '4px',
              cursor: 'pointer'
            }}
          >
            返回首页
          </button>
        </div>
      </div>
    );
  }

  const handleFavoriteToggle = async () => {
    if (!currentUser) {
      navigate('/login');
      return;
    }

    if (!product || !product.itemId) {
      console.error('Invalid product data:', product);
      alert('商品数据无效，请刷新页面重试');
      return;
    }
    
    try {
      console.log('Current product:', product);
      if (isFavorited) {
        console.log('Removing favorite for itemId:', product.itemId);
        const success = await removeFavorite(product.itemId);
        if (success) {
          setIsFavorited(false);
          // 前端乐观更新收藏数-1
          setProduct(prev => ({ ...prev, likes: Math.max((prev.likes || 1) - 1, 0) }));
        }
      } else {
        console.log('Adding favorite for itemId:', product.itemId);
        const success = await addFavorite(product.itemId);
        if (success) {
          setIsFavorited(true);
          // 前端乐观更新收藏数+1
          setProduct(prev => ({ ...prev, likes: (prev.likes || 0) + 1 }));
        }
      }
    } catch (err) {
      console.error('收藏操作失败:', err);
      alert('收藏操作失败，请稍后重试');
    }
  };

  const handleBuyProduct = async () => {
    if (!currentUser) {
      navigate('/login');
      return;
    }
    const confirmed = window.confirm(`确认购买 ${product.title}？`);
    if (confirmed) {
      const res = await buyProduct(product.itemId);
      console.log(res.message? res.message : product.itemId);
      if (res && res.success != false) {
        alert('购买请求已发送给卖家');
        navigate('/my-purchases');
      } else {
        alert(res.message || '购买失败');
      }
    }
  };

  // 处理发表评论
  const handleSubmitMessage = async (e) => {
    e.preventDefault();
    
    if (!currentUser) {
      navigate('/login');
      return;
    }

    if (!newMessage.trim()) {
      alert('请输入评论内容');
      return;
    }

    try {
      const message = await addMessage(id, newMessage.trim(), replyTo?.messageId);
      
      if (replyTo) {
        // 如果是回复，更新原评论的回复列表
        setMessages(messages.map(msg => 
          msg.messageId === replyTo.messageId 
            ? { ...msg, replies: [...(msg.replies || []), message] }
            : msg
        ));
      } else {
        // 如果是新评论，添加到列表开头
        setMessages([{ ...message, replies: [] }, ...messages]);
      }
      
      setNewMessage('');
      setReplyTo(null);
    } catch (err) {
      console.error('发表评论失败:', err);
      alert('发表评论失败，请稍后重试');
    }
  };

  // 处理删除评论
  const handleDeleteMessage = async (messageId, parentId = null) => {
    if (!currentUser) return;
    
    const confirmed = window.confirm('确定要删除这条评论吗？');
    if (!confirmed) return;

    try {
      const success = await deleteMessage(messageId);
      if (success) {
        if (parentId) {
          // 删除回复
          setMessages(messages.map(msg => 
            msg.messageId === parentId
              ? { ...msg, replies: msg.replies.filter(reply => reply.messageId !== messageId) }
              : msg
          ));
        } else {
          // 删除根评论
          setMessages(messages.filter(msg => msg.messageId !== messageId));
        }
      }
    } catch (err) {
      console.error('删除评论失败:', err);
      alert('删除评论失败，请稍后重试');
    }
  };

  return (
    <div className="page">
      <div className="page-header">
        <button className="back-btn" onClick={() => navigate(-1)}>
          <i className="fas fa-arrow-left"></i>
        </button>
        <h2>商品详情</h2>
        <button className="back-btn" onClick={() => navigate('/')}>
          <i className="fas fa-home"></i>
        </button>
      </div>

      <div className="product-detail">
        {/* 商品图片 */}
        <div className="product-detail-image">
          <div className="product-detail-image-gallery">
            <div className="main-image">
              <img 
                src={product.imageUrls && product.imageUrls.length > 0 
                  ? product.imageUrls[currentImageIndex]
                  : '/grape.png'} 
                alt={product.title}
                className="main-product-image"
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
            {product.imageUrls && product.imageUrls.length > 1 && (
              <div className="thumbnail-list">
                {product.imageUrls.map((imageUrl, index) => (
                  <div 
                    key={index}
                    className={`thumbnail ${index === currentImageIndex ? 'active' : ''}`}
                    onClick={() => setCurrentImageIndex(index)}
                  >
                    <img 
                      src={imageUrl} 
                      alt={`${product.title} ${index + 1}`}
                      className="thumbnail-image"
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
                ))}
              </div>
            )}
          </div>
        </div>

        {/* 商品信息 */}
        <div className="product-detail-info">
          <h1 className="product-detail-title">{product.title}</h1>
          <div className="product-detail-price">¥{product.price}</div>

          <div className="product-detail-meta">
            <div className="meta-item">
              <span className="label">成色:</span>
              <span className="value">{product.condition}</span>
            </div>
            <div className="meta-item">
              <span className="label">发布时间:</span>
              <span className="value">{new Date(product.createTime).toLocaleString()}</span>
            </div>
            <div className="meta-item">
              <span className="label">分类:</span>
              <span className="value">{product.categoryName}</span>
            </div>
            <div className="meta-item">
              <span className="label">状态:</span>
              <span className="value">{product.isAvailable ? '在售' : '已售出'}</span>
            </div>
          </div>

          {/* 卖家信息 */}
          <div className="seller-info">
            <div className="seller-name">
              <i className="fas fa-user"></i>
              <span>{product.sellerName}</span>
            </div>
            <div className="seller-stats">
              <div className="seller-stats-row">
                <div className="stat-item">
                  <div className="stat-label">收藏数</div>
                  <div className="stat-value">{product.likes || 0}</div>
                </div>
              </div>
            </div>
          </div>

          <div className="product-detail-description">
            <h3>商品描述</h3>
            <p>{product.description}</p>
          </div>

          {/* 操作按钮区域 */}
          <div style={{ marginTop: '2rem', borderTop: '2px solid #e0e0e0', paddingTop: '1rem' }}>
            <h3 style={{ marginBottom: '1rem', color: '#333' }}>商品操作</h3>
            <div 
              style={{
                display: 'flex',
                gap: '1rem',
                backgroundColor: '#f9f9f9',
                borderRadius: '8px',
                padding: '1rem',
                border: '1px solid #ddd'
              }}
            >
              <button 
                onClick={handleFavoriteToggle}
                style={{
                  background: isFavorited ? '#e74c3c' : 'white',
                  border: '2px solid #e74c3c',
                  color: isFavorited ? 'white' : '#e74c3c',
                  padding: '12px 24px',
                  borderRadius: '25px',
                  cursor: 'pointer',
                  display: 'flex',
                  alignItems: 'center',
                  justifyContent: 'center',
                  gap: '8px',
                  minWidth: '120px',
                  fontWeight: '600',
                  fontSize: '16px',
                  transition: 'all 0.3s ease'
                }}
              >
                <i className={`${isFavorited ? 'fas' : 'far'} fa-heart`} style={{ fontSize: '16px' }}></i>
                {isFavorited ? '已收藏' : '收藏'}
              </button>
              {product.isAvailable && (
                <button 
                  onClick={handleBuyProduct}
                  style={{
                    flex: '1',
                    background: 'linear-gradient(135deg, #667eea 0%, #764ba2 100%)',
                    color: 'white',
                    border: 'none',
                    padding: '12px 24px',
                    fontSize: '16px',
                    borderRadius: '25px',
                    cursor: 'pointer',
                    display: 'flex',
                    alignItems: 'center',
                    justifyContent: 'center',
                    gap: '8px',
                    fontWeight: '600',
                    boxShadow: '0 4px 15px rgba(102, 126, 234, 0.3)',
                    transition: 'all 0.3s ease'
                  }}
                >
                  <i className="fas fa-shopping-cart" style={{ fontSize: '16px' }}></i>
                  立即购买
                </button>
              )}
            </div>
          </div>
        </div>
      </div>

      {/* 评论区域 */}
      <div className="comments-section" style={{ 
        padding: '2rem',
        backgroundColor: '#fff',
        marginTop: '2rem',
        border: '1px solid #e8e8e8',
        borderRadius: '8px'
      }}>
        <h3 style={{ 
          borderBottom: '2px solid #f0f0f0',
          paddingBottom: '1rem',
          marginBottom: '2rem'
        }}>商品评论</h3>
        
        {/* 发表评论 */}
        <div className="comment-form" style={{ 
          marginBottom: '2rem',
          backgroundColor: '#f9f9f9',
          padding: '1.5rem',
          borderRadius: '8px'
        }}>
          {replyTo && (
            <div style={{ 
              marginBottom: '1rem',
              color: '#666',
              display: 'flex',
              alignItems: 'center',
              backgroundColor: '#fff',
              padding: '0.5rem 1rem',
              borderRadius: '4px'
            }}>
              <span>回复 {replyTo.userName}：</span>
              <button 
                onClick={() => setReplyTo(null)}
                style={{ 
                  marginLeft: 'auto',
                  color: '#999',
                  border: 'none',
                  background: 'none',
                  cursor: 'pointer',
                  padding: '4px 8px'
                }}
              >
                取消回复
              </button>
            </div>
          )}
          <form onSubmit={handleSubmitMessage} style={{ display: 'flex', gap: '1rem' }}>
            <input
              type="text"
              value={newMessage}
              onChange={(e) => setNewMessage(e.target.value)}
              placeholder={replyTo ? "输入回复内容..." : "说点什么..."}
              style={{
                flex: 1,
                padding: '0.75rem 1rem',
                border: '1px solid #ddd',
                borderRadius: '4px',
                fontSize: '14px',
                backgroundColor: '#fff'
              }}
            />
            <button
              type="submit"
              style={{
                padding: '0.75rem 2rem',
                backgroundColor: '#1890ff',
                color: 'white',
                border: 'none',
                borderRadius: '4px',
                cursor: 'pointer',
                fontSize: '14px',
                fontWeight: '500'
              }}
            >
              发表
            </button>
          </form>
        </div>

        {/* 评论列表 */}
        {loadingMessages ? (
          <div style={{ textAlign: 'center', padding: '2rem' }}>
            <div className="loading-spinner"></div>
            <div style={{ marginTop: '1rem', color: '#666' }}>加载评论中...</div>
          </div>
        ) : messages.length > 0 ? (
          <div className="comments-list">
            {messages.map(message => (
              <div key={message.messageId} style={{ marginBottom: '1.5rem' }}>
                {/* 主评论 */}
                <div style={{ 
                  padding: '1rem',
                  borderBottom: '1px solid #f0f0f0'
                }}>
                  <div style={{ 
                    display: 'flex',
                    alignItems: 'center',
                    marginBottom: '0.5rem'
                  }}>
                    <div style={{ 
                      width: '32px',
                      height: '32px',
                      borderRadius: '50%',
                      backgroundColor: '#f0f0f0',
                      marginRight: '1rem',
                      display: 'flex',
                      alignItems: 'center',
                      justifyContent: 'center',
                      color: '#999'
                    }}>
                      <i className="fas fa-user"></i>
                    </div>
                    <div>
                      <div style={{ fontWeight: '500', color: '#333' }}>{message.userName}</div>
                      <div style={{ fontSize: '12px', color: '#999', marginTop: '2px' }}>
                        {new Date(message.createTime).toLocaleString()}
                      </div>
                    </div>
                  </div>
                  <div style={{ 
                    margin: '0.5rem 0 1rem 2.75rem',
                    color: '#333',
                    lineHeight: '1.5'
                  }}>
                    {message.content}
                  </div>
                  <div style={{ 
                    marginLeft: '2.75rem',
                    display: 'flex',
                    gap: '1rem'
                  }}>
                    <button
                      onClick={() => setReplyTo(message)}
                      style={{
                        border: 'none',
                        background: 'none',
                        color: '#1890ff',
                        cursor: 'pointer',
                        fontSize: '13px',
                        padding: '4px 0'
                      }}
                    >
                      <i className="far fa-comment" style={{ marginRight: '4px' }}></i>
                      回复
                    </button>
                    {currentUser && currentUser.id === message.userId && (
                      <button
                        onClick={() => handleDeleteMessage(message.messageId)}
                        style={{
                          border: 'none',
                          background: 'none',
                          color: '#ff4d4f',
                          cursor: 'pointer',
                          fontSize: '13px',
                          padding: '4px 0'
                        }}
                      >
                        <i className="far fa-trash-alt" style={{ marginRight: '4px' }}></i>
                        删除
                      </button>
                    )}
                  </div>
                </div>

                {/* 回复列表 */}
                {message.replies && message.replies.length > 0 && (
                  <div style={{ 
                    marginLeft: '2.75rem',
                    backgroundColor: '#f9f9f9',
                    padding: '0.5rem 1rem',
                    marginTop: '0.5rem',
                    borderRadius: '4px'
                  }}>
                    {message.replies.map(reply => (
                      <div key={reply.messageId} style={{ 
                        padding: '0.75rem 0',
                        borderBottom: '1px solid #f0f0f0'
                      }}>
                        <div style={{ 
                          display: 'flex',
                          alignItems: 'center',
                          marginBottom: '0.5rem'
                        }}>
                          <div style={{ 
                            width: '24px',
                            height: '24px',
                            borderRadius: '50%',
                            backgroundColor: '#f0f0f0',
                            marginRight: '0.75rem',
                            display: 'flex',
                            alignItems: 'center',
                            justifyContent: 'center',
                            color: '#999',
                            fontSize: '12px'
                          }}>
                            <i className="fas fa-user"></i>
                          </div>
                          <div>
                            <div style={{ fontWeight: '500', color: '#333', fontSize: '13px' }}>
                              {reply.userName}
                            </div>
                            <div style={{ fontSize: '12px', color: '#999', marginTop: '2px' }}>
                              {new Date(reply.createTime).toLocaleString()}
                            </div>
                          </div>
                        </div>
                        <div style={{ 
                          margin: '0.5rem 0 0.75rem 2.5rem',
                          color: '#333',
                          fontSize: '13px',
                          lineHeight: '1.5'
                        }}>
                          {reply.content}
                        </div>
                        <div style={{ 
                          marginLeft: '2.5rem',
                          display: 'flex',
                          gap: '1rem'
                        }}>
                          <button
                            onClick={() => setReplyTo(message)}
                            style={{
                              border: 'none',
                              background: 'none',
                              color: '#1890ff',
                              cursor: 'pointer',
                              fontSize: '12px',
                              padding: '2px 0'
                            }}
                          >
                            <i className="far fa-comment" style={{ marginRight: '4px' }}></i>
                            回复
                          </button>
                          {currentUser && currentUser.id === reply.userId && (
                            <button
                              onClick={() => handleDeleteMessage(reply.messageId, message.messageId)}
                              style={{
                                border: 'none',
                                background: 'none',
                                color: '#ff4d4f',
                                cursor: 'pointer',
                                fontSize: '12px',
                                padding: '2px 0'
                              }}
                            >
                              <i className="far fa-trash-alt" style={{ marginRight: '4px' }}></i>
                              删除
                            </button>
                          )}
                        </div>
                      </div>
                    ))}
                  </div>
                )}
              </div>
            ))}
          </div>
        ) : (
          <div style={{ 
            textAlign: 'center',
            color: '#999',
            padding: '3rem 0',
            backgroundColor: '#f9f9f9',
            borderRadius: '8px'
          }}>
            <i className="far fa-comment-dots" style={{ fontSize: '24px', marginBottom: '1rem' }}></i>
            <div>暂无评论，快来发表第一条评论吧！</div>
          </div>
        )}
      </div>
    </div>
  );
};

export default ProductDetailPage; 