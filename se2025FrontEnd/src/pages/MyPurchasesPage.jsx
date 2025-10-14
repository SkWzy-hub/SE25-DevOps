import { useState, useEffect } from 'react';
import { useNavigate } from 'react-router-dom';
import { useAppContext } from '../App';

import RatingDialog from '../components/RatingDialog';
import { getPurchasedOrderList, buyerCompleteOrder, buyerCreditOrder } from '../service/order';

// 格式化日期时间函数
const formatDateTime = (dateTimeString) => {
  if (!dateTimeString) return '';
  
  try {
    const date = new Date(dateTimeString);
    if (isNaN(date.getTime())) return dateTimeString; // 如果解析失败，返回原字符串
    
    return date.toLocaleString('zh-CN', {
      year: 'numeric',
      month: '2-digit',
      day: '2-digit',
      hour: '2-digit',
      minute: '2-digit',
      second: '2-digit'
    });
  } catch (error) {
    return dateTimeString; // 出错时返回原字符串
  }
};

const PAGE_SIZE = 5;

const MyPurchasesPage = () => {
  const { currentUser } = useAppContext();
  const navigate = useNavigate();
  const [orders, setOrders] = useState([]);
  const [page, setPage] = useState(0);
  const [hasMore, setHasMore] = useState(true);
  const [isFetchingNextPage, setIsFetchingNextPage] = useState(false);
  const [forceUpdate, setForceUpdate] = useState(0);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState(null);
  const [ratingDialog, setRatingDialog] = useState({
    isOpen: false,
    targetUser: null,
    product: null,
    orderId: null
  });

  // 分页获取订单
  const fetchOrders = async (pageToFetch = 0, append = false) => {
    if (!currentUser) {
      console.log('fetchOrders: currentUser为空，跳过获取');
      return;
    }
    if (!hasMore && append) return;
    if (append) setIsFetchingNextPage(true);
    else setLoading(true);
    setError(null);
    try {
      console.log(`fetchOrders: 开始获取第${pageToFetch}页订单，用户:`, currentUser.username);
      const res = await getPurchasedOrderList(pageToFetch, PAGE_SIZE);
      console.log('fetchOrders: 获取到的原始响应:', res);
      let orderList = Array.isArray(res) ? res : (res.content || []);
      console.log('fetchOrders: 处理后的订单列表:', orderList);
      if (append) {
        setOrders(prev => [...prev, ...orderList]);
      } else {
        setOrders(orderList);
      }
      setHasMore(res.last === false); // last为false表示还有下一页
      setPage(pageToFetch);
      console.log(`fetchOrders: 完成，共${orderList.length}条订单，hasMore=${res.last === false}`);
      // 强制重新渲染
      setForceUpdate(prev => prev + 1);
    } catch (err) {
      console.error('fetchOrders: 获取订单失败:', err);
      setError('订单获取失败');
    } finally {
      if (append) setIsFetchingNextPage(false);
      else setLoading(false);
    }
  };

  // 首次加载
  useEffect(() => {
    window.scrollTo(0, 0);
    setOrders([]);
    setPage(0);
    setHasMore(true);
    fetchOrders(0, false);
  }, [currentUser]);

  // 滚动监听
  useEffect(() => {
    const handleScroll = () => {
      if (
        window.innerHeight + document.documentElement.scrollTop + 100 >= document.documentElement.offsetHeight &&
        hasMore &&
        !isFetchingNextPage &&
        !loading
      ) {
        fetchOrders(page + 1, true);
      }
    };
    window.addEventListener('scroll', handleScroll);
    return () => window.removeEventListener('scroll', handleScroll);
  }, [page, hasMore, isFetchingNextPage, loading]);

  // if (!currentUser) {
  //   navigate('/login');
  //   return null;
  // }

  // 后端已经根据JWT token过滤了用户的购买订单，不需要前端再过滤
  const myPurchases = orders;
  
  // 调试信息
  console.log('MyPurchasesPage 渲染状态:', {
    loading,
    error,
    ordersLength: orders.length,
    myPurchasesLength: myPurchases.length,
    currentUser: currentUser?.username,
    hasMore,
    page,
    forceUpdate
  });
  
  // 检查订单数据结构
  if (orders.length > 0) {
    console.log('第一个订单的详细数据:', orders[0]);
  }

  const getStatusText = (order) => {
    const { orderStatus, ifBuyerConfirm, ifSellerConfirm } = order;
    console.log(`getStatusText: 订单${order.orderId}状态 - orderStatus=${orderStatus}, ifBuyerConfirm=${ifBuyerConfirm}, ifSellerConfirm=${ifSellerConfirm}`);
    
    switch (orderStatus) {
      case 0: return '等待确认';
      case 1: return '已确认';
      case 2: 
        if (ifBuyerConfirm) return '等待卖家确认';
        if (ifSellerConfirm) return '卖家已确认';
        return '等待双方确认';
      case 3: return '已完成';
      case 4: return '已取消';
      default: return '未知状态';
    }
  };

  const getStatusClass = (order) => {
    const { orderStatus, ifBuyerConfirm, ifSellerConfirm } = order;
    
    switch (orderStatus) {
      case 0: return 'status-pending';
      case 1: return 'status-confirmed';
      case 2: 
        if (ifBuyerConfirm) return 'status-buyer-completed';
        if (ifSellerConfirm) return 'status-seller-completed';
        return 'status-waiting';
      case 3: return 'status-completed';
      case 4: return 'status-cancelled';
      default: return '';
    }
  };

  const handleOpenRating = (order) => {
    setRatingDialog({
      isOpen: true,
      targetUser: order.seller,
      product: { title: order.item?.itemName },
      orderId: order.orderId
    });
  };

  const handleCloseRating = () => {
    setRatingDialog({
      isOpen: false,
      targetUser: null,
      product: null,
      orderId: null
    });
  };

  // 评价提交逻辑
  const handleSubmitRating = async (rating) => {
    if (!ratingDialog.orderId) return;
    const res = await buyerCreditOrder(ratingDialog.orderId, rating);
    if (res && res.success !== false) {
      alert(`评价提交成功！评分：${rating}星`);
      // 直接更新本地状态，不重新获取
      setOrders(prev => prev.map(order => 
        order.orderId === ratingDialog.orderId 
          ? { ...order, sellerCredit: { rating: rating } } 
          : order
      ));
      handleCloseRating();
    } else {
      alert(res.message || '评价提交失败');
    }
  };

  // 买家确认收货
  const handleBuyerComplete = async (orderId) => {
    if (!window.confirm('确认已收到商品且满意？')) return;
    const res = await buyerCompleteOrder(orderId);
    if (res && res.success !== false) {
      alert('已确认收货！');
      // 直接更新本地状态，不重新获取
      setOrders(prev => prev.map(order => {
        if (order.orderId === orderId) {
          const newOrder = { 
            ...order, 
            ifBuyerConfirm: 1,
            finishTime: new Date().toISOString() 
          };
          
          // 检查是否双方都已确认，如果是则设置为已完成状态
          if (order.ifSellerConfirm === 1 && newOrder.ifBuyerConfirm === 1) {
            newOrder.orderStatus = 3; // 已完成
          } else {
            newOrder.orderStatus = 2; // 等待双方确认
          }
          
          return newOrder;
        }
        return order;
      }));
    } else {
      alert(res.message || '确认收货失败');
    }
  };

  // 订单操作后刷新（如评价、确认收货）
  // 只需重置分页重新拉取
  const refreshOrders = () => {
    setOrders([]);
    setPage(0);
    setHasMore(true);
    fetchOrders(0, false);
  };

  return (
    <div className="page">
      <div className="page-header">
        <button className="back-btn" onClick={() => navigate('/profile', { replace: true })}>
          <i className="fas fa-arrow-left"></i>
        </button>
        <h2>我买入的</h2>
        <button className="back-btn" onClick={() => navigate('/') }>
          <i className="fas fa-home"></i>
        </button>
      </div>
      <div className="orders-list">
        {loading ? (
          <div className="empty-state"><h3>加载中...</h3></div>
        ) : error ? (
          <div className="empty-state"><h3>{error}</h3></div>
        ) : myPurchases.length === 0 ? (
          <div className="empty-state">
            <i className="fas fa-shopping-cart"></i>
            <h3>暂无购买记录</h3>
            <p>您还没有购买过任何商品</p>
          </div>
        ) : (
          <>
            {myPurchases.map((order, index) => {
              console.log(`渲染订单卡片 ${index}:`, order);
              return (
                <div key={order.orderId} className="order-card">
                  <div className="order-header">
                    <span className="order-id">订单号: {order.orderId || '未知'}</span>
                    <span className={`order-status ${getStatusClass(order)}`}>
                      {getStatusText(order)}
                    </span>
                  </div>
                  <div className="order-product">
                    <div className="order-product-image">
                      <img 
                        src={order.item?.imageUrl || order.item?.itemImage || 'https://via.placeholder.com/100'} 
                        alt={order.item?.itemName || '商品图片'} 
                        onError={(e) => {
                          e.target.src = 'https://via.placeholder.com/100';
                        }}
                      />
                    </div>
                    <div className="order-product-info">
                      <h4>{order.item?.itemName || '商品名称'}</h4>
                      <p>卖家: {order.seller?.username || '未知'}</p>
                      <p>价格: ¥{order.orderAmount || 0}</p>
                      <p>下单时间: {order.createTime ? formatDateTime(order.createTime) : '未知'}</p>
                      {order.finishTime && (
                        <p>完成时间: {formatDateTime(order.finishTime)}</p>
                      )}
                    </div>
                  </div>
                  <div className="order-actions">
                    <button 
                      className="btn-secondary"
                      onClick={() => navigate(`/order/${order.orderId}`)}
                    >
                      查看详情
                    </button>
                    {/* 买家确认收货按钮：当订单状态为1或状态为2且买家未确认时 */}
                    {(order.orderStatus === 1 || (order.orderStatus === 2 && order.ifBuyerConfirm === 0)) && (
                      <button className="btn-primary" onClick={() => handleBuyerComplete(order.orderId)}>
                        确认收货
                      </button>
                    )}
                    {order.orderStatus === 3 && (
                      <>
                        {!order.sellerCredit ? (
                          <button 
                            className="rating-btn"
                            onClick={() => handleOpenRating(order)}
                          >
                            <i className="fas fa-star"></i>
                            评价订单
                          </button>
                        ) : (
                          <button className="rating-btn" style={{cursor: 'default', fontWeight: 600}} disabled>
                            <i className="fas fa-check"></i>
                            已评价
                          </button>
                        )}
                      </>
                    )}
                  </div>
                </div>
              );
            })}
          </>
        )}
        {isFetchingNextPage && <div className="empty-state"><h4>加载更多...</h4></div>}
        {!hasMore && orders.length > 0 && <div className="empty-state"><h4>没有更多订单了</h4></div>}
      </div>
      {/* 评价对话框 */}
      <RatingDialog
        isOpen={ratingDialog.isOpen}
        onClose={handleCloseRating}
        onSubmit={handleSubmitRating}
        targetUser={ratingDialog.targetUser}
        product={ratingDialog.product}
        userRole="buyer"
      />
    </div>
  );
};

export default MyPurchasesPage; 