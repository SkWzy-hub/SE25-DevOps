import { useState, useEffect } from 'react';
import { useNavigate } from 'react-router-dom';
import { useAppContext } from '../App';

import RatingDialog from '../components/RatingDialog';
import { getSoldOrderList, sellerConfirmOrder, sellerCancelOrder, sellerCreditOrder, sellerCompleteOrder, getOrderDetail } from '../service/order';

// 格式化日期时间函数
const formatDateTime = (dateTimeData) => {
  if (!dateTimeData) return '';
  
  try {
    let date;
    
    // 如果后端返回的是数组格式 [year, month, day, hour, minute]
    if (Array.isArray(dateTimeData)) {
      const [year, month, day, hour = 0, minute = 0, second = 0] = dateTimeData;
      date = new Date(year, month - 1, day, hour, minute, second); // month需要减1，因为JS的月份从0开始
    } else {
      // 如果是字符串格式，直接解析
      date = new Date(dateTimeData);
    }
    
    if (isNaN(date.getTime())) return '日期格式错误';
    
    return date.toLocaleString('zh-CN', {
      year: 'numeric',
      month: '2-digit',
      day: '2-digit',
      hour: '2-digit',
      minute: '2-digit',
      second: '2-digit'
    });
  } catch (error) {
    return '日期解析错误';
  }
};

const PAGE_SIZE = 5;

const MySalesPage = () => {
  const { currentUser } = useAppContext();
  const navigate = useNavigate();

  const [orders, setOrders] = useState([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState(null);

  const [ratingDialog, setRatingDialog] = useState({
    isOpen: false,
    targetUser: null,
    product: null,
    orderId: null
  });
  const [page, setPage] = useState(0);
  const [hasMore, setHasMore] = useState(true);
  const [isFetchingNextPage, setIsFetchingNextPage] = useState(false);

  // 页面加载时滚动到顶部并获取订单
  useEffect(() => {
    window.scrollTo(0, 0);
    setOrders([]);
    setPage(0);
    setHasMore(true);
    fetchOrders(0, false);
  }, [currentUser]);


  // 提取fetchOrders到组件作用域，便于多处调用
  const fetchOrders = async (pageToFetch = 0, append = false) => {
    if (!currentUser) return;
    if (!hasMore && append) return;
    if (append) setIsFetchingNextPage(true);
    else setLoading(true);
    setError(null);
    try {
      const res = await getSoldOrderList(pageToFetch, PAGE_SIZE);
      let orderList = Array.isArray(res) ? res : (res.content || []);
      if (append) {
        setOrders(prev => [...prev, ...orderList]);
      } else {
        setOrders(orderList);
      }
      setHasMore(res.last === false); // last为false表示还有下一页
      setPage(pageToFetch);
    } catch (err) {
      console.error('获取订单失败:', err);
      setError('订单获取失败');
    } finally {
      if (append) setIsFetchingNextPage(false);
      else setLoading(false);

    }
  };

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

  // 订单操作后刷新
  const refreshOrders = () => {
    setOrders([]);
    setPage(0);
    setHasMore(true);
    fetchOrders(0, false);
  };

  // 查看详情逻辑
  const handleViewDetail = async (orderId) => {
    try {
      const res = await getOrderDetail(orderId);
      if (res && res.success !== false && (res.data || res.orderId)) {
        // 跳转并传递订单详情数据
        navigate(`/order/${orderId}`, { state: { orderDetail: res.data || res } });
      } else {
        alert(res.message || '获取订单详情失败');
      }
    } catch (e) {
      alert('获取订单详情失败');
    }
  };

  if (!currentUser) {
    return (
      <div className="page">
        <div className="page-header">
          <button className="back-btn" onClick={() => navigate('/profile', { replace: true })}>
            <i className="fas fa-arrow-left"></i>
          </button>
          <h2>我卖出的</h2>
          <button className="back-btn" onClick={() => navigate('/') }>
            <i className="fas fa-home"></i>
          </button>
        </div>
        <div className="empty-state">
          <h3>请先登录</h3>
          <p>您需要登录后才能查看销售记录</p>
          <button className="btn-primary" onClick={() => navigate('/login')}>
            去登录
          </button>
        </div>
      </div>
    );
  }
  
  // 后端已经根据JWT token过滤了用户的销售订单，不需要前端再过滤
  const mySales = orders;

  const getStatusText = (order) => {
    const { orderStatus, ifBuyerConfirm, ifSellerConfirm } = order;
    
    switch (orderStatus) {
      case 0: return '等待确认';
      case 1: return '已确认';
      case 2: 
        if (ifBuyerConfirm) return '买家已确认';
        if (ifSellerConfirm) return '等待买家确认';
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
      targetUser: order.buyer,
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

  // 卖家评价提交逻辑
  const handleSubmitRating = async (rating) => {
    if (!ratingDialog.orderId) return;
    const res = await sellerCreditOrder(ratingDialog.orderId, rating);
    if (res && res.success !== false) {
      alert(`评价提交成功！评分：${rating}星`);
      setOrders(prev => prev.map(order => 
        order.orderId === ratingDialog.orderId 
          ? { ...order, buyerCredit: { rating: rating } } 
          : order
      ));
      handleCloseRating();
    } else {
      alert(res.message || '评价提交失败');
    }
  };

  // 卖家确认订单
  const handleSellerConfirm = async (orderId) => {
    if (!window.confirm('确认要接受该订单吗？')) return;
    const res = await sellerConfirmOrder(orderId);
    if (res && res.success !== false) {
      alert('订单已确认！');
      setOrders(prev => prev.map(order => 
        order.orderId === orderId 
          ? { ...order, orderStatus: 1, confirmTime: new Date().toISOString() } 
          : order
      ));
    } else {
      alert(res.message || '确认订单失败');
    }
  };

  // 卖家取消订单
  const handleSellerCancel = async (orderId) => {
    if (!window.confirm('确定要取消该订单吗？')) return;
    const res = await sellerCancelOrder(orderId);
    if (res && res.success !== false) {
      alert('订单已取消！');
      setOrders(prev => prev.map(order => 
        order.orderId === orderId 
          ? { ...order, orderStatus: 4, cancelTime: new Date().toISOString() } 
          : order
      ));
    } else {
      alert(res.message || '取消订单失败');
    }
  };

  // 卖家确认收货
  const handleSellerComplete = async (orderId) => {
    if (!window.confirm('确认要完成该订单吗？')) return;
    const res = await sellerCompleteOrder(orderId);
    if (res && res.success !== false) {
      alert('订单已完成！');
      setOrders(prev => prev.map(order => {
        if (order.orderId === orderId) {
          const newOrder = { 
            ...order, 
            ifSellerConfirm: 1,
            finishTime: new Date().toISOString() 
          };
          
          // 检查是否双方都已确认，如果是则设置为已完成状态
          if (order.ifBuyerConfirm === 1 && newOrder.ifSellerConfirm === 1) {
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

  return (
    <div className="page">
      <div className="page-header">
        <button className="back-btn" onClick={() => navigate('/profile', { replace: true })}>
          <i className="fas fa-arrow-left"></i>
        </button>
        <h2>我卖出的</h2>
        <button className="back-btn" onClick={() => navigate('/') }>
          <i className="fas fa-home"></i>
        </button>
      </div>
      <div className="orders-list">
        {loading ? (

          <div className="empty-state"><h3>加载中...</h3></div>
        ) : error ? (
          <div className="empty-state"><h3>{error}</h3></div>

        ) : mySales.length === 0 ? (
          <div className="empty-state">
            <i className="fas fa-store"></i>
            <h3>暂无销售记录</h3>
            <p>您还没有销售过任何商品</p>
          </div>
        ) : (
          mySales.map(order => (
          <div key={order.orderId} className="order-card">
            <div className="order-header">
              <span className="order-id">订单号: {order.orderId}</span>

              <span className={`order-status ${getStatusClass(order)}`}>
                {getStatusText(order)}

              </span>
            </div>
            <div className="order-product">
              <div className="order-product-image">

                <img src={order.item?.imageUrl} alt={order.item?.itemName} />
              </div>
              <div className="order-product-info">
                <h4>{order.item?.itemName}</h4>
                <p>买家: {order.buyer?.username}</p>
                <p>价格: ¥{order.orderAmount}</p>
                <p>下单时间: {formatDateTime(order.createTime)}</p>
                {order.finishTime && (
                  <p>完成时间: {formatDateTime(order.finishTime)}</p>

                )}
              </div>
            </div>
            <div className="order-actions">
              <button 
                className="btn-secondary"
                onClick={() => handleViewDetail(order.orderId)}
              >
                查看详情
              </button>
              {order.orderStatus === 0 && (
                <div className="action-buttons">
                  <button className="btn-primary" onClick={() => handleSellerConfirm(order.orderId)}>确认订单</button>
                  <button className="btn-cancel" onClick={() => handleSellerCancel(order.orderId)}>拒绝订单</button>
                </div>
              )}
              {/* 卖家确认收货：当订单状态为1，或订单状态为2且买家已确认但卖家未确认时 */}
              {(order.orderStatus === 1 || (order.orderStatus === 2 && order.ifBuyerConfirm === 1 && order.ifSellerConfirm === 0)) && (
                <button className="btn-primary" onClick={() => handleSellerComplete(order.orderId)}>
                  确认收货
                </button>
              )}
              {order.orderStatus === 3 && (
                <>
                  {!order.buyerCredit ? (
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
          ))
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
        userRole="seller"
      />
    </div>
  );
};

export default MySalesPage; 