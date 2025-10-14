import { useState, useEffect } from 'react';
import { useParams, useNavigate } from 'react-router-dom';
import { useAppContext } from '../App';
import { get, PREFIX } from '../service/common';
import RatingDialog from '../components/RatingDialog';
import { getOrderDetail, sellerConfirmOrder, sellerCancelOrder, buyerCompleteOrder, sellerCompleteOrder, buyerCreditOrder, sellerCreditOrder } from '../service/order';

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

const OrderDetailPage = () => {
  const { id } = useParams();
  const { currentUser } = useAppContext();
  const navigate = useNavigate();
  const [order, setOrder] = useState(null);
  const [loading, setLoading] = useState(true);

  const [error, setError] = useState(null);

  const [ratingDialog, setRatingDialog] = useState({
    isOpen: false,
    targetUser: null,
    userRole: null
  });


  // 页面加载时滚动到顶部并获取订单详情
  useEffect(() => {
    window.scrollTo(0, 0);
    const fetchOrder = async () => {
      setLoading(true);
      setError(null);
      try {
        const res = await getOrderDetail(id);
        if (res && res.orderId) {
          setOrder(res);
        } else {
          setError('订单不存在');
          setTimeout(() => navigate(-1), 1500);
        }
      } catch (err) {
        setError('订单详情获取失败');
        setTimeout(() => navigate(-1), 1500);
      } finally {
        setLoading(false);
      }
    };
    fetchOrder();
  }, [id, navigate]);


  if (!currentUser) {
    navigate('/login');
    return null;
  }


  if (loading) return <div>加载中...</div>;
  if (error) return <div>{error}</div>;
  if (!order) return <div>订单不存在</div>;

  // 判断当前用户角色

  const isUserBuyer = currentUser.id === order.buyer?.userId;
  const isUserSeller = currentUser.id === order.seller?.userId;

  // 获取订单状态显示文本
  const getStatusText = () => {
    switch (order.orderStatus) {
      case 0:
        return '等待卖家确认';
      case 1:
        return '卖家已确认，等待双方确认完成';
      case 2:
        if (order.ifBuyerConfirm && order.ifSellerConfirm) return '双方已确认，等待系统结算';
        if (order.ifBuyerConfirm && !order.ifSellerConfirm) return '买家已确认，待卖家确认';
        if (!order.ifBuyerConfirm && order.ifSellerConfirm) return '卖家已确认，待买家确认';
        return '待双方确认';
      case 3:
        return '交易完成';
      case 4:
        return '已取消';
      default:
        return '未知状态';
    }
  };

  // 获取订单流程步骤
  const getFlowSteps = () => {
    const isCancelledNoConfirm = order.orderStatus === 4 && !order.ifBuyerConfirm && !order.ifSellerConfirm;
    const steps = [
      {
        key: 'ordered',
        label: '买家下单',
        completed: true,
        time: formatDateTime(order.createTime)
      },
      {
        key: 'confirmed',
        label: '卖家确认',
        completed: isCancelledNoConfirm ? false : order.orderStatus > 0,
        time: order.confirmTime ? formatDateTime(order.confirmTime) : null,
        hasBranches: order.orderStatus > 0
      }
    ];

    // 卖家确认后才有分支
    steps.push({
      key: 'completion_branches',
      isBranch: true,
      branches: [
        {
          key: 'buyer_completed',
          label: '买家确认完成',
          completed: isCancelledNoConfirm ? false : !!order.ifBuyerConfirm,
          time: order.ifBuyerConfirm && order.finishTime ? formatDateTime(order.finishTime) : null
        },
        {
          key: 'seller_completed',
          label: '卖家确认完成',
          completed: isCancelledNoConfirm ? false : !!order.ifSellerConfirm,
          time: order.ifSellerConfirm && order.finishTime ? formatDateTime(order.finishTime) : null
        }
      ]
    });

    steps.push({
      key: 'completed',
      label: '交易完成',
      completed: isCancelledNoConfirm ? false : order.orderStatus === 3,
      time: order.orderStatus === 3 && order.finishTime ? formatDateTime(order.finishTime) : null,
      requiresBoth: order.orderStatus > 0
    });

    return steps;
  };

  // 处理卖家确认订单
  const handleSellerConfirm = async () => {
    const confirmed = window.confirm('确认接受此订单？');
    if (confirmed) {
      const res = await sellerConfirmOrder(order.orderId);
      if (res && res.success !== false) {
        alert('订单已确认！');
        // 直接更新本地状态，不重新获取
        setOrder(prev => ({
          ...prev,
          orderStatus: 1,
          confirmTime: new Date().toISOString()
        }));
      } else {
        alert(res.message || '确认订单失败');
      }
    }
  };

  // 处理买家确认完成
  const handleBuyerComplete = async () => {
    const confirmed = window.confirm('确认已收到商品且满意？');
    if (confirmed) {
      const res = await buyerCompleteOrder(order.orderId);
      if (res && res.success !== false) {
        alert('您已确认完成交易，等待卖家确认。');
        // 直接更新本地状态，不重新获取
        setOrder(prev => ({
          ...prev,
          orderStatus: 2,
          ifBuyerConfirm: 1,
          finishTime: new Date().toISOString()
        }));
      } else {
        alert(res.message || '确认失败');
      }
    }
  };

  // 处理卖家确认完成
  const handleSellerComplete = async () => {
    const confirmed = window.confirm('确认交易已完成？');
    if (confirmed) {
      const res = await sellerCompleteOrder(order.orderId);
      if (res && res.success !== false) {
        alert('您已确认完成交易，等待买家确认。');
        // 直接更新本地状态，不重新获取
        setOrder(prev => ({
          ...prev,
          orderStatus: 2,
          ifSellerConfirm: 1,
          finishTime: new Date().toISOString()
        }));
      } else {
        alert(res.message || '确认失败');
      }
    }
  };

  // 处理取消订单

  const handleCancelOrder = async () => {
    const confirmed = window.confirm('确认要取消此订单吗？');
    if (confirmed) {
      const res = await sellerCancelOrder(order.orderId);
      if (res && res.success !== false) {
        alert('订单已取消');
        // 直接更新本地状态，不重新获取
        setOrder(prev => ({
          ...prev,
          orderStatus: 4,
          cancelTime: new Date().toISOString()
        }));
      } else {
        alert(res.message || '取消订单失败');
      }
    }
  };

  // 处理评价相关
  const handleOpenRating = (targetUser, userRole) => {
    setRatingDialog({
      isOpen: true,
      targetUser,
      userRole
    });
  };

  const handleCloseRating = () => {
    setRatingDialog({
      isOpen: false,
      targetUser: null,
      userRole: null
    });
  };

  const handleSubmitRating = async (rating) => {
    let res;
    if (ratingDialog.userRole === 'buyer') {
      res = await buyerCreditOrder(order.orderId, rating);
    } else {
      res = await sellerCreditOrder(order.orderId, rating);
    }
    if (res && res.success !== false) {
      alert('评价提交成功！');
      setOrder(prev => ({
        ...prev,
        ...(ratingDialog.userRole === 'buyer' 
          ? { sellerCredit: rating } 
          : { buyerCredit:  rating  }
        )
      }));
      handleCloseRating();
    } else {
      alert(res.message || '评价失败');
    }
  };

  const generateStarRating = (rating) => {
    const stars = [];
    const fullStars = Math.floor(rating);
    
    for (let i = 0; i < fullStars; i++) {
      stars.push(<i key={i} className="fas fa-star"></i>);
    }
    
    const emptyStars = 5 - fullStars;
    for (let i = 0; i < emptyStars; i++) {
      stars.push(<i key={`empty-${i}`} className="far fa-star"></i>);
    }
    
    return stars;
  };

  // 判断是否可以取消订单
  const canCancelOrder = () => {
    return order.status !== 'completed' && order.status !== 'cancelled';
  };

  // 判断是否显示联系信息
  const shouldShowContact = () => {
    return order.orderStatus === 1 || order.orderStatus === 2 || order.orderStatus === 3;
  };

  // 操作按钮区域（只改判断，不改布局）
  // 卖家确认订单按钮
  const showSellerConfirmBtn = isUserSeller && order.orderStatus === 0;
  // 买家确认完成按钮
  const showBuyerCompleteBtn = isUserBuyer && ((order.orderStatus === 1 && !order.ifBuyerConfirm) || (order.orderStatus === 2 && !order.ifBuyerConfirm));
  // 卖家确认完成按钮
  const showSellerCompleteBtn = isUserSeller && ((order.orderStatus === 1 && !order.ifSellerConfirm) || (order.orderStatus === 2 && !order.ifSellerConfirm));

  return (
    <div className="page">
      <div className="page-header">
        <button className="back-btn" onClick={() => navigate(-1)}>
          <i className="fas fa-arrow-left"></i>
        </button>
        <h2>订单详情</h2>
        <button className="back-btn" onClick={() => navigate('/')}>
          <i className="fas fa-home"></i>
        </button>
      </div>

      <div className="order-detail">
        {/* 订单基本信息 */}
        <div className="order-info-card">
          <div className="order-header">
            <span className="order-id">订单号: {order.orderId}</span>
            <span className={`order-status status-${order.orderStatus}`}>
              {getStatusText()}
            </span>
          </div>
          
          <div className="order-product">
            <div className="order-product-image">
              <img src={order.item?.imageUrl} alt={order.item?.itemName} />
            </div>
            <div className="order-product-info">
              <h3>{order.item?.itemName}</h3>
              <p className="product-price">¥{order.orderAmount}</p>
              <p>下单时间: {formatDateTime(order.createTime)}</p>
              {order.confirmTime && (
                <p>确认时间: {formatDateTime(order.confirmTime)}</p>
              )}
              {order.finishTime && (
                <p>完成时间: {formatDateTime(order.finishTime)}</p>
              )}

              {order.cancelTime && (
                <div>
                  <p>取消时间: {formatDateTime(order.cancelTime)}</p>
                  <p>取消原因: {order.cancelReason}</p>
                </div>

              )}
            </div>
          </div>
        </div>

        {/* 订单流程 */}
        {order.status !== 'cancelled' && (
          <div className="order-flow">
            <h3>订单流程</h3>
            <div className="flow-container">
              {getFlowSteps().map((step, index) => (
                <div key={step.key} className="flow-step-container">
                  {/* 普通步骤 */}
                  {!step.isBranch && (
                    <div className="flow-step-wrapper">
                      {step.requiresBoth && (
                        <div className={`convergence-lines ${step.completed ? 'completed' : ''}`}>
                        </div>
                      )}
                      <div className={`flow-step ${step.completed ? 'completed' : ''} ${step.hasBranches ? 'has-branches' : ''}`}>
                        <div className="flow-icon">
                          {step.completed ? (
                            <i className="fas fa-check"></i>
                          ) : (
                            <i className="fas fa-circle"></i>
                          )}
                        </div>
                        <div className="flow-content">
                          <div className="flow-label">{step.label}</div>
                          {step.time && <div className="flow-time">{step.time}</div>}
                        </div>
                      </div>
                    </div>
                  )}
                  
                  {/* 分支步骤 */}
                  {step.isBranch && (
                    <div className={`flow-branches-container ${step.branches.every(b => b.completed) ? 'has-completed' : ''}`}>
                      <div className="flow-branches">
                        {step.branches.map((branch, branchIndex) => (
                          <div key={branch.key} className={`flow-branch ${branch.completed ? 'completed' : ''}`}>
                            <div className="branch-icon">
                              {branch.completed ? (
                                <i className="fas fa-check"></i>
                              ) : (
                                <i className="fas fa-circle"></i>
                              )}
                            </div>
                            <div className="branch-content">
                              <div className="branch-label">{branch.label}</div>
                              {branch.time && <div className="branch-time">{branch.time}</div>}
                            </div>
                          </div>
                        ))}
                      </div>
                    </div>
                  )}
                </div>
              ))}
            </div>
          </div>
        )}

        {/* 联系信息 */}
        {shouldShowContact() && (
          <div className="contact-info">
            <h3>联系信息</h3>
            <div className="contact-cards">
              <div className="contact-card">
                <h4>
                  <i className="fas fa-user"></i>
                  买家信息
                </h4>
                <div className="contact-item">
                  <i className="fas fa-user-circle"></i>
                  <span>{order.buyer?.username}</span>
                </div>
                <div className="contact-item">
                  <i className="fas fa-phone"></i>
                  <span>{order.buyer?.phone}</span>
                </div>
              </div>
              
              <div className="contact-card">
                <h4>
                  <i className="fas fa-store"></i>
                  卖家信息
                </h4>
                <div className="contact-item">
                  <i className="fas fa-user-circle"></i>
                  <span>{order.seller?.username}</span>
                </div>
                <div className="contact-item">
                  <i className="fas fa-phone"></i>
                  <span>{order.seller?.phone}</span>
                </div>
              </div>
            </div>
          </div>
        )}

        {/* 操作按钮 */}
        <div className="order-actions">
          {/* 卖家确认订单 */}
          {showSellerConfirmBtn && (
            <div className="action-group">
              <button className="btn-primary" onClick={handleSellerConfirm}>
                确认订单
              </button>
              <button className="btn-danger" onClick={handleCancelOrder}>
                拒绝订单
              </button>
            </div>
          )}

          {/* 买家确认完成 */}
          {showBuyerCompleteBtn && (
            <div className="action-group">
              <button className="btn-primary" onClick={handleBuyerComplete}>
                确认收货完成
              </button>
              {/* <button className="btn-secondary" onClick={handleCancelOrder}>
                取消订单
              </button> */}
            </div>
          )}

          {/* 卖家确认完成 */}
          {showSellerCompleteBtn && (
            <div className="action-group">
              <button className="btn-primary" onClick={handleSellerComplete}>
                确认交易完成
              </button>
              <button className="btn-secondary" onClick={handleCancelOrder}>
                取消订单
              </button>
            </div>
          )}

          {/* 买家在未完成前都能取消订单 */}
          {isUserBuyer && [0,1,2].includes(order.orderStatus) && (
            <button className="btn-secondary" onClick={handleCancelOrder}>
              取消订单
            </button>
          )}

          {/* 评价按钮 - 交易完成后可评价 */}
          {order.orderStatus === 3 && (
            <div className="rating-actions">
              {!order.sellerCredit && isUserBuyer && (
                <button 
                  className="rating-btn"
                  onClick={() => handleOpenRating(order.seller, 'buyer')}
                >
                  <i className="fas fa-star"></i>
                  评价卖家
                </button>
              )}
              {!order.buyerCredit && isUserSeller && (
                <button 
                  className="rating-btn"
                  onClick={() => handleOpenRating(order.buyer, 'seller')}
                >
                  <i className="fas fa-star"></i>
                  评价买家
                </button>
              )}
              {order.sellerCredit && isUserBuyer && (
                <button className="rating-btn" style={{cursor: 'default', fontWeight: 600}} disabled>
                  <i className="fas fa-check"></i>
                  已评价
                </button>
              )}
              {order.buyerCredit && isUserSeller && (
                <button className="rating-btn" style={{cursor: 'default', fontWeight: 600}} disabled>
                  <i className="fas fa-check"></i>
                  已评价
                </button>
              )}
            </div>
          )}
        </div>

        {/* 状态说明 */}
        <div className="order-status-info">
          <h3>状态说明</h3>
          <div className="status-description">
            {order.orderStatus === 0 && (
              <p><i className="fas fa-clock"></i> 等待卖家确认订单，卖家可以选择接受或拒绝订单。</p>
            )}
            {order.orderStatus === 1 && (
              <p><i className="fas fa-handshake"></i> 卖家已确认，等待买家和卖家双方确认完成。</p>
            )}
            {order.orderStatus === 2 && order.ifBuyerConfirm && order.ifSellerConfirm && (
              <p><i className="fas fa-check-circle"></i> 双方已确认，等待系统结算。</p>
            )}
            {order.orderStatus === 2 && order.ifBuyerConfirm && !order.ifSellerConfirm && (
              <p><i className="fas fa-user-check"></i> 买家已确认，待卖家确认。需要双方都确认后交易才算完成。</p>
            )}
            {order.orderStatus === 2 && !order.ifBuyerConfirm && order.ifSellerConfirm && (
              <p><i className="fas fa-store"></i> 卖家已确认，待买家确认。需要双方都确认后交易才算完成。</p>
            )}
            {order.orderStatus === 2 && !order.ifBuyerConfirm && !order.ifSellerConfirm && (
              <p><i className="fas fa-clock"></i> 待双方确认。需要双方都确认后交易才算完成。</p>
            )}
            {order.orderStatus === 3 && (
              <p><i className="fas fa-check-circle"></i> 买卖双方都已确认，交易完成！现在可以进行评价。</p>
            )}
            {order.orderStatus === 4 && (
              <p><i className="fas fa-times-circle"></i> 订单已取消。</p>
            )}
          </div>
        </div>

        {/* 订单评价 */}

        {order.orderStatus === 3 && (
          <div className="order-ratings-section">
            <h3>订单评价</h3>
            {order.buyerCredit && (

              <div className="rating-item">
                <div className="rating-header">
                  <div className="rater-info">
                    <i className="fas fa-user-circle"></i>
                    <span className="rater-name">{order.buyer?.username}</span>
                    <span className="rater-role">(买家)</span>
                  </div>
                  <div className="rating-stars">

                    {generateStarRating(order.buyerCredit)}
                    <span style={{marginLeft: '0.5em', color: '#666', fontSize: '0.95em'}}>{order.buyerCredit.rating}</span>
                  </div>
                </div>
              </div>
            )}
            {order.sellerCredit && (

              <div className="rating-item">
                <div className="rating-header">
                  <div className="rater-info">
                    <i className="fas fa-user-circle"></i>
                    <span className="rater-name">{order.seller?.username}</span>
                    <span className="rater-role">(卖家)</span>
                  </div>
                  <div className="rating-stars">

                    {generateStarRating(order.sellerCredit)}
                    <span style={{marginLeft: '0.5em', color: '#666', fontSize: '0.95em'}}>{order.sellerCredit.rating}</span>
                  </div>
                </div>
              </div>
            )}
            {!order.buyerCredit && !order.sellerCredit && (

              <div className="no-ratings">
                <i className="fas fa-star-half-alt"></i>
                <p>暂无评价</p>
              </div>
            )}
          </div>
        )}
      </div>

      {/* 评价对话框 */}
      <RatingDialog
        isOpen={ratingDialog.isOpen}
        onClose={handleCloseRating}
        onSubmit={handleSubmitRating}
        targetUser={ratingDialog.targetUser}
        product={{ title: order.item?.itemName }}
        userRole={ratingDialog.userRole}
      />
    </div>
  );
};

export default OrderDetailPage; 
 