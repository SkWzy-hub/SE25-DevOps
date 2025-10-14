import React, { useState, useEffect } from 'react';
import { useNavigate, useSearchParams } from 'react-router-dom';
import { useAppContext } from '../App';

import { createItem, updateItem, getItemDetail, generateDescription } from '../service/item';


const PublishPage = () => {
  const { currentUser } = useAppContext();
  const navigate = useNavigate();
  const [searchParams] = useSearchParams();
  const editId = searchParams.get('edit');
  const isEditing = !!editId;
  
  const [formData, setFormData] = useState({
    title: '',
    price: '',
    category: '',
    condition: '',
    description: '',
    image: null  // 只允许一张图片
  });

  const [errors, setErrors] = useState({});
  const [loading, setLoading] = useState(false);
  const [submitLoading, setSubmitLoading] = useState(false);
  const [aiGenerating, setAiGenerating] = useState(false);

  const categories = [
    { value: 'books', label: 'books' },
    { value: 'electronics', label: 'electronics' },
    { value: 'clothing', label: 'clothing' },
    { value: 'sports', label: 'sports' },
    { value: 'home', label: 'home' },
    { value: 'entertainment', label: 'entertainment' },
    { value: 'transport', label: 'transport' },
    { value: 'furniture', label: 'furniture' },
    { value: 'baby', label: 'baby' },
    { value: 'pets', label: 'pets' }
  ];

  // 根据category值获取对应的ID（顺序1-10）
  const getCategoryId = (categoryValue) => {
    const categoryMap = {
      'books': 1,
      'electronics': 2,
      'clothing': 3,
      'sports': 4,
      'home': 5,
      'entertainment': 6,
      'transport': 7,
      'furniture': 8,
      'baby': 9,
      'pets': 10
    };
    return categoryMap[categoryValue] || 1;
  };

  // 根据categoryId获取对应的category值
  const getCategoryValueById = (categoryId) => {
    const idMap = {
      1: 'books',
      2: 'electronics',
      3: 'clothing',
      4: 'sports',
      5: 'home',
      6: 'entertainment',
      7: 'transport',
      8: 'furniture',
      9: 'baby',
      10: 'pets'
    };
    return idMap[categoryId] || 'books';
  };

  // 页面加载时滚动到顶部
  useEffect(() => {
    window.scrollTo(0, 0);
  }, []);

  // 如果是编辑模式，加载商品详情
  useEffect(() => {
    if (isEditing && editId) {
      const loadItemForEdit = async () => {
        try {
          setLoading(true);
          const itemData = await getItemDetail(editId);
          if (itemData) {
            setFormData({
              title: itemData.title || '',
              price: itemData.price?.toString() || '',
              category: itemData.categoryName || getCategoryValueById(itemData.categoryId) || '',
              condition: itemData.condition || '',
              description: itemData.description || '',
              image: itemData.imageUrls && itemData.imageUrls.length > 0 ? {
                id: 0,
                url: itemData.imageUrls[0] || '/grape.png', // 如果URL为空则使用默认图片
                file: null,
                isExisting: true
              } : null
            });
          } else {
            alert('商品不存在或已被删除');
            navigate('/my-posts');
          }
        } catch (error) {
          console.error('加载商品详情失败:', error);
          alert('加载商品详情失败，请稍后重试');
          navigate('/my-posts');
        } finally {
          setLoading(false);
        }
      };
      
      loadItemForEdit();
    }
  }, [isEditing, editId, navigate]);

  if (!currentUser) {
    navigate('/login');
    return null;
  }

  const handleInputChange = (e) => {
    const { name, value } = e.target;
    
    // 清除当前字段的错误信息
    if (errors[name]) {
      setErrors(prev => ({
        ...prev,
        [name]: ''
      }));
    }

    // 字段验证
    let newErrors = {};
    
    if (name === 'title') {
      if (value.length > 100) {
        newErrors.title = '商品标题不能超过100个字符';
        return;
      }
    }
    
    if (name === 'price') {
      // 允许输入数字、小数点，但进行基本格式验证
      if (value) {
        // 只允许数字和一个小数点，且小数点后最多2位
        if (!/^\d*\.?\d{0,2}$/.test(value)) {
          // 如果格式完全不对，不更新值
          return;
        }
        
        // 检查是否超出最大值（只在输入完整数字时检查）
        if (/^\d+(\.\d{1,2})?$/.test(value)) {
          const numValue = parseFloat(value);
          if (numValue > 99999999.99) {
            newErrors.price = '价格不能超过99999999.99';
            setErrors(prev => ({ ...prev, ...newErrors }));
            return;
          }
        }
      }
    }

    // 更新表单数据
    setFormData(prev => ({
      ...prev,
      [name]: value
    }));
  };

  const handleImageUpload = (e) => {
    const file = e.target.files[0];
    if (!file) return;

    // 检查文件大小（限制为5MB）
    if (file.size > 5 * 1024 * 1024) {
      alert('图片大小不能超过5MB');
      return;
    }

    // 检查文件类型
    if (!file.type.startsWith('image/')) {
      alert('请选择图片文件');
      return;
    }

    const reader = new FileReader();
    reader.onload = (event) => {
      setFormData(prev => ({
        ...prev,
        image: {
          id: Date.now(),
          url: event.target.result,
          file: file,
          isExisting: false
        }
      }));
    };
    reader.readAsDataURL(file);
  };

  const removeImage = () => {
    setFormData(prev => ({
      ...prev,
      image: null
    }));
  };

  const autoGenerateDescription = async () => {
    if (!formData.title || !formData.category || !formData.condition) {
      alert('请先填写商品标题、分类和新旧程度');
      return;
    }

    try {
      setAiGenerating(true);
      
      // 调用后端AI接口生成描述
      const aiDescription = await generateDescription(
        formData.title,
        formData.category,
        formData.condition
      );
      
      setFormData(prev => ({
        ...prev,
        description: aiDescription
      }));
      
    } catch (error) {
      console.error('AI描述生成失败:', error);
      
      // AI失败时使用本地模板作为备选方案
      const categoryTexts = {
        'books': '这是一本教材书籍',
        'electronics': '这是一件数码产品',
        'clothing': '这是一件服装配饰',
        'sports': '这是一件运动用品',
        'home': '这是一件生活用品',
        'entertainment': '这是一件娱乐休闲用品',
        'transport': '这是一件交通工具',
        'furniture': '这是一件家具家居用品',
        'baby': '这是一件母婴用品',
        'pets': '这是一件宠物用品'
      };

      const fallbackDescription = `${categoryTexts[formData.category] || '这是一件闲置物品'}，${formData.condition}。因为${formData.category === 'books' ? '已经学完这门课' : '升级换新/不常使用'}，现在转让给需要的同学。价格实惠，有意者请联系！

💰 价格可小刀，诚心出售
📞 联系方式：站内私信或电话联系

温馨提示：支持当面验货，确保交易安全！`;

      setFormData(prev => ({
        ...prev,
        description: fallbackDescription
      }));
      
      alert('AI描述生成失败，已使用默认模板生成描述');
    } finally {
      setAiGenerating(false);
    }
  };

  const handleSubmit = async (e) => {
    e.preventDefault();
    
    // 验证必填项
    let newErrors = {};
    
    if (!formData.title.trim()) {
      newErrors.title = '请输入商品标题';
    } else if (formData.title.length > 100) {
      newErrors.title = '商品标题不能超过100个字符';
    }
    
    if (!formData.price || formData.price.trim() === '') {
      newErrors.price = '请输入售价';
    } else {
      const priceStr = formData.price.toString().trim();
      // 更灵活的价格格式验证，允许 "123", "123.45", ".5", "0.5" 等格式
      if (!/^(\d+\.?\d{0,2}|\.\d{1,2})$/.test(priceStr)) {
        newErrors.price = '价格格式不正确，请输入有效的数字（最多2位小数）';
      } else {
        const numValue = parseFloat(priceStr);
        if (isNaN(numValue) || numValue <= 0) {
          newErrors.price = '价格必须大于0';
        } else if (numValue > 99999999.99) {
          newErrors.price = '价格不能超过99999999.99';
        }
      }
    }
    
    if (!formData.category) {
      newErrors.category = '请选择商品分类';
    }
    
    if (!formData.condition.trim()) {
      newErrors.condition = '请填写新旧程度';
    }

    if (!formData.image) {
      newErrors.image = '请上传一张商品图片';
    }

    if (Object.keys(newErrors).length > 0) {
      setErrors(newErrors);
      alert('请检查并修正表单中的错误');
      return;
    }

    try {
      setSubmitLoading(true);
      
      // 准备提交数据
      const submitData = {
        title: formData.title.trim(),
        price: parseFloat(formData.price),
        category: formData.category,
        categoryId: getCategoryId(formData.category), // 添加分类ID
        condition: formData.condition.trim(),
        description: formData.description.trim(),
        sellerId: currentUser.id || 1, // 使用当前用户ID，如果没有则使用默认值
      };

      // 如果有新图片文件，则添加到提交数据中
      if (formData.image && !formData.image.isExisting) {
        submitData.image = formData.image.file;
      }

      let response;
      if (isEditing) {
        // 更新商品
        response = await updateItem(editId, submitData);
      } else {
        // 发布新商品
        response = await createItem(submitData);
      }

      if (response.success || response.code === 200) {
        alert(isEditing ? '商品更新成功！' : '商品发布成功！');
        navigate('/my-posts');
      } else {
        throw new Error(response.message || '操作失败');
      }
      
    } catch (error) {
      console.error(isEditing ? '更新商品失败:' : '发布商品失败:', error);
      
      let errorMessage = '操作失败，请稍后重试';
      if (error.response?.data?.message) {
        errorMessage = error.response.data.message;
      } else if (error.message) {
        errorMessage = error.message;
      }
      
      alert(errorMessage);
    } finally {
      setSubmitLoading(false);
    }
  };

  if (loading) {
    return (
      <div className="page">
        <div className="page-header">
          <button className="back-btn" onClick={() => navigate(-1)}>
            <i className="fas fa-arrow-left"></i>
          </button>
          <h2>加载中...</h2>
        </div>
        <div style={{ padding: '2rem', textAlign: 'center' }}>
          <div className="loading-spinner"></div>
          <p>正在加载商品信息...</p>
        </div>
      </div>
    );
  }

  return (
    <div className="page">
      <div className="page-header">
        <button className="back-btn" onClick={() => navigate(-1)}>
          <i className="fas fa-arrow-left"></i>
        </button>
        <h2>{isEditing ? '编辑商品' : '发布商品'}</h2>
        <button className="back-btn" onClick={() => navigate('/')}>
          <i className="fas fa-home"></i>
        </button>
      </div>

      <form className="publish-form" onSubmit={handleSubmit}>
        {/* 图片上传区域 */}
        <div className="form-section">
          <h3>商品图片 <span className="required">*</span></h3>
          <div className="image-upload-container">
            <div className="image-preview">
              {formData.image && (
                <div className="image-item">
                  <img 
                    src={formData.image.url} 
                    alt="商品图片" 
                    className="item-image"
                    onError={(e) => {
                      e.target.src = '/grape.png';
                    }}
                    style={{
                      width: '100%',
                      height: '100%',
                      objectFit: 'cover'
                    }}
                  />
                  <button type="button" className="remove-image" onClick={removeImage}>
                    <i className="fas fa-times"></i>
                  </button>
                </div>
              )}
              
              {!formData.image && (
                <label className="upload-placeholder">
                  <input
                    type="file"
                    accept="image/*"
                    onChange={handleImageUpload}
                    className="image-input"
                  />
                  <i className="fas fa-plus"></i>
                  <p>上传图片</p>
                  <span>限1张，最大5MB</span>
                </label>
              )}
            </div>
            {errors.image && <span className="error-message">{errors.image}</span>}
          </div>
        </div>

        {/* 基本信息 */}
        <div className="form-section">
          <h3>基本信息</h3>
          
          {/* 商品名称和售价在同一行 */}
          <div className="form-row">
          <div className="form-group">
            <label>商品标题 <span className="required">*</span></label>
            <input
              type="text"
              name="title"
              value={formData.title}
              onChange={handleInputChange}
              placeholder="请输入商品标题"
                maxLength="100"
              required
                className={errors.title ? 'error' : ''}
            />
              {errors.title && <span className="error-message">{errors.title}</span>}
          </div>
            <div className="form-group">
              <label>售价 <span className="required">*</span></label>
              <input
                type="text"
                name="price"
                value={formData.price}
                onChange={handleInputChange}
                placeholder="0.00"
                required
                className={errors.price ? 'error' : ''}
              />
              {errors.price && <span className="error-message">{errors.price}</span>}
            </div>
          </div>

          {/* 分类和新旧程度在同一行 */}
          <div className="form-row">
            <div className="form-group">
              <label>商品分类 <span className="required">*</span></label>
              <select
                name="category"
                value={formData.category}
                onChange={handleInputChange}
                required
                className={errors.category ? 'error' : ''}
              >
                <option value="">请选择分类</option>
                {categories.map(cat => (
                  <option key={cat.value} value={cat.value}>{cat.label}</option>
                ))}
              </select>
              {errors.category && <span className="error-message">{errors.category}</span>}
            </div>
            <div className="form-group">
              <label>新旧程度 <span className="required">*</span></label>
              <input
                type="text"
                name="condition"
                value={formData.condition}
                onChange={handleInputChange}
                placeholder="如：九成新、几乎全新等"
                required
                className={errors.condition ? 'error' : ''}
              />
              {errors.condition && <span className="error-message">{errors.condition}</span>}
            </div>
          </div>

        </div>

        {/* 商品描述 */}
        <div className="form-section">
          <div className="description-header">
            <h3>商品描述</h3>
            <button
              type="button"
              className="btn-auto-generate"
              onClick={autoGenerateDescription}
              disabled={aiGenerating}
            >
              {aiGenerating ? (
                <>
                  <i className="fas fa-spinner fa-spin"></i>
                  AI生成中...
                </>
              ) : (
                <>
                  <i className="fas fa-magic"></i>
                  AI智能生成
                </>
              )}
            </button>
          </div>
          <div className="form-group">
            <textarea
              name="description"
              value={formData.description}
              onChange={handleInputChange}
              placeholder="请描述商品的详细信息，包括购买时间、使用情况、转让原因等..."
              rows="6"
            />
          </div>
        </div>

        {/* 提交按钮 */}
        <div className="form-actions">
          <button 
            type="button" 
            className="btn-secondary" 
            onClick={() => navigate(-1)}
            disabled={submitLoading}
          >
            取消
          </button>
          <button 
            type="submit" 
            className="btn-primary"
            disabled={submitLoading}
          >
            {submitLoading ? (
              <>
                <i className="fas fa-spinner fa-spin"></i>
                {isEditing ? '更新中...' : '发布中...'}
              </>
            ) : (
              isEditing ? '更新商品' : '发布商品'
            )}
          </button>
        </div>
      </form>
    </div>
  );
};

export default PublishPage;