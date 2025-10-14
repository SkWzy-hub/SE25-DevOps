import { useState } from 'react';

const RatingDialog = ({ isOpen, onClose, onSubmit, targetUser, product, userRole }) => {
  const [rating, setRating] = useState(0);
  const [hoveredRating, setHoveredRating] = useState(0);

  const handleStarClick = (starRating) => {
    setRating(starRating);
  };

  const handleStarHover = (starRating) => {
    setHoveredRating(starRating);
  };

  const handleStarLeave = () => {
    setHoveredRating(0);
  };

  const handleSubmit = () => {
    if (rating === 0) {
      alert('请选择评分');
      return;
    }
    onSubmit(rating);
    handleClose();
  };

  const handleClose = () => {
    setRating(0);
    setHoveredRating(0);
    onClose();
  };

  const getRatingText = (currentRating) => {
    switch (currentRating) {
      case 1: return '很差';
      case 2: return '较差';
      case 3: return '一般';
      case 4: return '良好';
      case 5: return '优秀';
      default: return '请选择评分';
    }
  };

  if (!isOpen) return null;

  return (
    <div className="modal">
      <div className="modal-content">
        <div className="rating-dialog">
          <h3>评价{userRole === 'buyer' ? '卖家' : '买家'}</h3>
          
          <div className="rating-product-info">
            <div className="product-title">{product.title}</div>
            <div className="seller-name">
              {userRole === 'buyer' ? '卖家' : '买家'}: {targetUser.username}
            </div>
          </div>

          <div className="rating-section">
            <label>请为本次交易评分</label>
            <div className="star-rating">
              {[1, 2, 3, 4, 5].map((star) => (
                <i
                  key={star}
                  className={`${
                    star <= (hoveredRating || rating) ? 'fas' : 'far'
                  } fa-star`}
                  onClick={() => handleStarClick(star)}
                  onMouseEnter={() => handleStarHover(star)}
                  onMouseLeave={handleStarLeave}
                ></i>
              ))}
            </div>
            <div className="rating-text-indicator">
              {getRatingText(hoveredRating || rating)}
            </div>
          </div>

          <div className="rating-buttons">
            <button className="btn-secondary" onClick={handleClose}>
              取消
            </button>
            <button className="btn-primary" onClick={handleSubmit}>
              提交评价
            </button>
          </div>
        </div>
      </div>
    </div>
  );
};

export default RatingDialog; 