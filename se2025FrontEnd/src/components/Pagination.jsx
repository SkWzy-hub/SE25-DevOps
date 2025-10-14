import React from 'react';
import './Pagination.css';

const Pagination = ({ 
  currentPage, 
  totalPages, 
  totalElements, 
  pageSize,
  onPageChange,
  disabled = false 
}) => {
  // 计算显示的页码范围
  const getPageNumbers = () => {
    const delta = 2; // 当前页左右显示的页数
    const range = [];
    const rangeWithDots = [];

    // 计算显示范围
    const start = Math.max(0, currentPage - delta);
    const end = Math.min(totalPages - 1, currentPage + delta);

    // 添加页码
    for (let i = start; i <= end; i++) {
      range.push(i);
    }

    // 添加首页和省略号
    if (start > 0) {
      rangeWithDots.push(0);
      if (start > 1) {
        rangeWithDots.push('...');
      }
    }

    // 添加中间页码
    rangeWithDots.push(...range);

    // 添加末页和省略号
    if (end < totalPages - 1) {
      if (end < totalPages - 2) {
        rangeWithDots.push('...');
      }
      rangeWithDots.push(totalPages - 1);
    }

    return rangeWithDots;
  };

  if (totalPages <= 1) {
    return null; // 不显示分页
  }

  const pageNumbers = getPageNumbers();
  const startItem = currentPage * pageSize + 1;
  const endItem = Math.min((currentPage + 1) * pageSize, totalElements);

  return (
    <div className="pagination-container">
      <div className="pagination-info">
        第 {startItem}-{endItem} 条，共 {totalElements} 条
      </div>
      
      <div className="pagination-controls">
        {/* 上一页按钮 */}
        <button
          className="pagination-btn"
          onClick={() => onPageChange(currentPage - 1)}
          disabled={disabled || currentPage === 0}
          title="上一页"
        >
          <i className="fas fa-chevron-left"></i>
        </button>

        {/* 页码按钮 */}
        {pageNumbers.map((pageNum, index) => (
          <React.Fragment key={index}>
            {pageNum === '...' ? (
              <span className="pagination-dots">...</span>
            ) : (
              <button
                className={`pagination-btn ${pageNum === currentPage ? 'active' : ''}`}
                onClick={() => onPageChange(pageNum)}
                disabled={disabled}
              >
                {pageNum + 1}
              </button>
            )}
          </React.Fragment>
        ))}

        {/* 下一页按钮 */}
        <button
          className="pagination-btn"
          onClick={() => onPageChange(currentPage + 1)}
          disabled={disabled || currentPage >= totalPages - 1}
          title="下一页"
        >
          <i className="fas fa-chevron-right"></i>
        </button>
      </div>

      {/* 页面跳转 */}
      <div className="pagination-jump">
        <span>跳转到</span>
        <input
          type="number"
          min="1"
          max={totalPages}
          placeholder="页码"
          onKeyPress={(e) => {
            if (e.key === 'Enter') {
              const page = parseInt(e.target.value) - 1;
              if (page >= 0 && page < totalPages) {
                onPageChange(page);
                e.target.value = '';
              }
            }
          }}
          disabled={disabled}
        />
        <span>页</span>
      </div>
    </div>
  );
};

export default Pagination; 