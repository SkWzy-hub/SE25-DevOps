# 高校二手交易平台 - React版本

这是从原HTML/CSS/JS项目转换而来的React版本，保持了原有界面设计和功能。

## 项目结构

```
se2025FrontEnd/
├── src/
│   ├── components/          # 组件目录
│   │   └── Navbar.jsx      # 导航栏组件
│   │   └── ...            # 其他组件
│   ├── pages/              # 页面目录
│   │   ├── HomePage.jsx    # 主页
│   │   ├── LoginPage.jsx   # 登录页
│   │   ├── RegisterPage.jsx # 注册页
│   │   ├── SearchPage.jsx  # 搜索页
│   │   ├── ProfilePage.jsx # 个人信息页
│   │   ├── MyPurchasesPage.jsx # 我的购买
│   │   ├── MySalesPage.jsx # 我的销售
│   │   ├── MyFavoritesPage.jsx # 我的收藏
│   │   ├── MyPostsPage.jsx # 我的发布
│   │   ├── ProductDetailPage.jsx # 商品详情页
│   │   └── OrderDetailPage.jsx # 订单详情页
│   ├── data/
│   │   └── mockData.js     # 模拟数据
│   ├── App.jsx             # 主应用组件
│   ├── App.css             # 样式文件
│   └── main.jsx            # 入口文件
├── package.json
└── README.md
```

## 功能特性

### 已实现功能
- ✅ 用户登录/注册（用户名：admin，密码：123456）
- ✅ 商品浏览和搜索
- ✅ 商品详情查看
- ✅ 商品收藏功能
- ✅ 购买流程
- ✅ 订单管理（买入/卖出）
- ✅ 个人中心
- ✅ 我的发布管理
- ✅ 搜索和筛选
- ✅ 响应式设计

### 页面导航
1. **主页** (`/`) - 商品展示、分类、排序筛选
2. **搜索页** (`/search`) - 高级搜索和筛选
3. **登录页** (`/login`) - 用户登录
4. **注册页** (`/register`) - 用户注册
5. **个人信息** (`/profile`) - 个人中心入口
6. **我的购买** (`/my-purchases`) - 购买订单管理
7. **我的销售** (`/my-sales`) - 销售订单管理
8. **我的收藏** (`/my-favorites`) - 收藏商品管理
9. **我的发布** (`/my-posts`) - 发布商品管理
10. **商品详情** (`/product/:id`) - 商品详细信息
11. **订单详情** (`/order/:id`) - 订单详细信息

## 启动项目

### 开发环境启动
```bash
cd se2025FrontEnd
npm install  # 如果还没有安装依赖
npm run dev
```

访问：http://localhost:5173

### 生产环境构建
```bash
npm run build
npm run preview
```

## 测试账号

- **用户名**：admin
- **密码**：123456

## 技术栈

- **React 19** - 前端框架
- **React Router** - 路由管理
- **Vite** - 构建工具
- **Font Awesome** - 图标库
- **CSS3** - 样式（保持原项目样式）

## 数据管理

当前使用模拟数据（`src/data/mockData.js`），包含：
- 商品数据
- 订单数据
- 用户评价数据
- 分类数据

## 后续集成Spring Boot

项目结构已为后端集成做好准备：

1. **API接口**：可在各组件中替换模拟数据调用为实际API调用
2. **状态管理**：使用React Context进行全局状态管理
3. **认证**：已实现基础认证流程，可扩展为JWT token验证
4. **数据模型**：模拟数据结构与数据库模型对应

## 主要改进

相比原HTML版本：
- ✅ 组件化架构，便于维护
- ✅ 路由管理，单页面应用体验
- ✅ 状态管理，数据共享
- ✅ 更好的代码组织结构
- ✅ 现代开发工具链

## 界面保持度

- ✅ 完全保持原项目的视觉设计
- ✅ 所有CSS样式已转换
- ✅ 响应式布局保持一致
- ✅ 交互逻辑功能对等

## 注意事项

1. 确保已安装Node.js (建议16+版本)
2. 首次运行需要安装依赖：`npm install`
3. 开发服务器默认端口5173，如被占用会自动选择其他端口
4. 模拟数据仅用于演示，实际使用需要连接后端API
