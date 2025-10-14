-- 创建用户信息表
CREATE TABLE users_info(
    user_id INT PRIMARY KEY AUTO_INCREMENT,        -- 用户ID（主键）
    username VARCHAR(50) NOT NULL ,         -- 用户名（不唯一）                          
    phone VARCHAR(20) NOT NULL,                   -- 联系电话
    email VARCHAR(100) UNIQUE,                    -- 邮箱（唯一）
    avatar VARCHAR(200),                          -- 头像URL
    credit_score DECIMAL(10, 2) DEFAULT 5,        -- 信用评分
    note VARCHAR(200) DEFAULT '这个人很懒，什么都没有留下',     -- 个人签名
    deal_time INT DEFAULT 0,                     -- 交易次数
    status TINYINT(1) DEFAULT 1 ,                  -- 账户状态（1正常，0封禁）
    credit_time INT DEFAULT '0',                  --评价次数
    role ENUM('user', 'admin') NOT NULL DEFAULT 'user' -- 用户角色（user=普通用户，admin=管理员）
);
CREATE TABLE users_password(
    user_id INT PRIMARY KEY AUTO_INCREMENT,        -- 用户ID（主键）
    password VARCHAR(100) NOT NULL                 -- 加密密码
);

CREATE TABLE categories (
    category_id INT PRIMARY KEY AUTO_INCREMENT,   -- 分类ID（主键）
    category_name VARCHAR(50) NOT NULL,           -- 分类名称
    icon VARCHAR(100)                             -- 分类图标URL
);
CREATE TABLE items (
    item_id INT PRIMARY KEY AUTO_INCREMENT,       -- 物品ID（主键）
    image_url VARCHAR(200) NOT NULL,              -- 图片URL
    seller_id INT NOT NULL,                       -- 卖家ID（外键关联users表）
    category_id INT NOT NULL,                     -- 分类ID（外键关联categories表）
    item_name VARCHAR(100) NOT NULL,              -- 物品名称
    price DECIMAL(10, 2) NOT NULL,                -- 价格
    item_condition VARCHAR(20) NOT NULL,           -- 新旧程度（如"全新"、"九成新"）
    description TEXT,                             -- 物品描述（可由图片生成）
    likes INT DEFAULT 0,                          -- 收藏数
    is_available TINYINT(1) DEFAULT 1,            -- 是否可售（1是，0否）
    is_deleted TINYINT(1) DEFAULT 0,              -- 是否已删除（1是，0否）
    update_time DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,  -- 更新时间(包括生成时间）
    FOREIGN KEY (seller_id) REFERENCES users_info(user_id),
    FOREIGN KEY (category_id) REFERENCES categories(category_id)
);
CREATE TABLE orders (
    order_id VARCHAR(32) PRIMARY KEY,             -- 订单ID（主键，唯一编号）
    item_id INT NOT NULL,                         -- 物品ID（外键关联items表）
    buyer_id INT NOT NULL,                        -- 买家ID（外键关联users表）
    seller_id INT NOT NULL,                       -- 卖家ID（外键关联users表）
    seller_credit INT DEFAULT 0,                  -- 卖家(被)评分
    buyer_credit INT DEFAULT 0,                   -- 买家(被)评分
    order_amount DECIMAL(10, 2) NOT NULL,         -- 订单金额
    if_buyer_confirm INT DEFAULT 0,               -- 买家确认交易完成
    if_seller_confirm INT DEFAULT 0,              -- 卖家确认交易完成
    order_status TINYINT(1) DEFAULT 0,            -- 订单状态（0待确认，1已确认，2等待双方确认，3已完成，4已取消）
    create_time DATETIME DEFAULT CURRENT_TIMESTAMP,  -- 下单时间
    confirm_time DATETIME,                        -- 卖家确认时间
    finish_time DATETIME,                         -- 交易完成时间
    cancel_time DATETIME,                         -- 取消时间
    FOREIGN KEY (item_id) REFERENCES items(item_id),
    FOREIGN KEY (buyer_id) REFERENCES users_info(user_id),
    FOREIGN KEY (seller_id) REFERENCES users_info(user_id)
);
CREATE TABLE messages (
    message_id INT PRIMARY KEY AUTO_INCREMENT,    -- 留言ID（主键）
    item_id INT NOT NULL,                         -- 物品ID（外键关联items表）
    user_id INT NOT NULL,                         -- 留言用户ID（外键关联users表）
    parent_id INT DEFAULT 0,                      -- 父留言ID（0为根留言）
    content TEXT NOT NULL,                        -- 留言内容
    reply_time DATETIME DEFAULT CURRENT_TIMESTAMP,  -- 留言时间
    is_deleted TINYINT(1) DEFAULT 0,              -- 是否已删除（1是，0否）
    FOREIGN KEY (item_id) REFERENCES items(item_id),
    FOREIGN KEY (user_id) REFERENCES users_info(user_id)
);
CREATE TABLE user_views_record(
    id INT PRIMARY KEY AUTO_INCREMENT,
    user_id INT NOT NULL,
    category_id INT NOT NULL,
    view_date DATE NOT NULL,                     -- 浏览日期
    category_view_counts INT DEFAULT 0,         -- 当天浏览次数
    UNIQUE KEY unique_user_category_date (user_id, category_id, view_date),
    FOREIGN KEY (user_id) REFERENCES users_info(user_id),
    FOREIGN KEY (category_id) REFERENCES categories(category_id)
);

CREATE TABLE favorites (
    favorite_id INT PRIMARY KEY AUTO_INCREMENT,   -- 收藏ID（主键）
    user_id INT NOT NULL,                         -- 用户ID（外键关联users表）
    item_id INT NOT NULL,                         -- 物品ID（外键关联items表）
    UNIQUE KEY (user_id, item_id)                 -- 唯一约束：用户不能重复收藏同一物品
);

-- 清理现有数据（按外键依赖顺序删除）
DELETE FROM favorites;
DELETE FROM messages;
DELETE FROM orders;
DELETE FROM items;
DELETE FROM categories;
DELETE FROM users_password;
DELETE FROM users_info;
DELETE FROM user_views_record;

-- 重置自增ID
ALTER TABLE users_info AUTO_INCREMENT = 1;
ALTER TABLE categories AUTO_INCREMENT = 1;
ALTER TABLE items AUTO_INCREMENT = 1;
ALTER TABLE favorites AUTO_INCREMENT = 1;
ALTER TABLE messages AUTO_INCREMENT = 1;
ALTER TABLE user_views_record AUTO_INCREMENT = 1;

-- 插入用户数据（基于mockData）
-- 插入用户数据（基于mockData）
INSERT INTO users_info (username, phone, email, avatar, credit_score, note, deal_time, status, role) VALUES
                                                                                                         ('管理员', '13800138000', 'afuloowa@sjtu.edu.cn', 'https://via.placeholder.com/50', 5, '系统管理员，为大家提供最好的服务！', 5, 1, 'admin'),
                                                                                                         ('小李', '13800138001', 'user1@sjtu.edu.cn', 'https://via.placeholder.com/50', 5, '热爱生活，喜欢分享好物！', 0, 1, 'user'),
                                                                                                         ('小王', '13800138002', 'user2@sjtu.edu.cn', 'https://via.placeholder.com/50', 5, '诚信交易，互利共赢！', 0, 1, 'user'),
                                                                                                         ('书虫小明', '13800138003', 'xiaoming@sjtu.edu.cn', 'https://via.placeholder.com/50', 5, '专业卖书，质量保证！', 32, 1, 'user'),
                                                                                                         ('数码达人', '13800138004', 'xiaohong@sjtu.edu.cn', 'https://via.placeholder.com/50', 5, '数码产品专家，价格实惠！', 22, 1, 'user'),
                                                                                                         ('时尚小张', '13800138005', 'xiaozhang@sjtu.edu.cn', 'https://via.placeholder.com/50', 5, '时尚潮流，品味生活！', 15, 1, 'user'),
                                                                                                         ('运动健将', '13800138006', 'xiaoli@sjtu.edu.cn', 'https://via.placeholder.com/50', 5, '运动装备专业户！', 10, 1, 'user'),
                                                                                                         ('商务精英', '13800138007', 'xiaowang@sjtu.edu.cn', 'https://via.placeholder.com/50', 5, '商务办公用品，品质优先！', 8, 1, 'user'),
                                                                                                         ('羽球高手', '13800138008', 'xiaozhao@sjtu.edu.cn', 'https://via.placeholder.com/50', 5, '羽毛球装备专业推荐！', 5, 1, 'user');

-- 插入用户密码 (所有用户密码都是: 123456)
INSERT INTO users_password (user_id, password) VALUES   -- 密码加密方式：BCrypt
                                                        (1, '$2a$10$wTGlspaRRIiVouEOKVYRA.4zFTObZN1wZ0sbshiwVdn63jO2qWhjW'),
                                                        (2, '$2a$10$wTGlspaRRIiVouEOKVYRA.4zFTObZN1wZ0sbshiwVdn63jO2qWhjW'),
                                                        (3, '$2a$10$wTGlspaRRIiVouEOKVYRA.4zFTObZN1wZ0sbshiwVdn63jO2qWhjW'),
                                                        (4, '$2a$10$wTGlspaRRIiVouEOKVYRA.4zFTObZN1wZ0sbshiwVdn63jO2qWhjW'),
                                                        (5, '$2a$10$wTGlspaRRIiVouEOKVYRA.4zFTObZN1wZ0sbshiwVdn63jO2qWhjW'),
                                                        (6, '$2a$10$wTGlspaRRIiVouEOKVYRA.4zFTObZN1wZ0sbshiwVdn63jO2qWhjW'),
                                                        (7, '$2a$10$wTGlspaRRIiVouEOKVYRA.4zFTObZN1wZ0sbshiwVdn63jO2qWhjW'),
                                                        (8, '$2a$10$wTGlspaRRIiVouEOKVYRA.4zFTObZN1wZ0sbshiwVdn63jO2qWhjW'),
                                                        (9, '$2a$10$wTGlspaRRIiVouEOKVYRA.4zFTObZN1wZ0sbshiwVdn63jO2qWhjW');

-- 插入商品分类（与前端mockData保持一致）
INSERT INTO categories (category_name, icon) VALUES
                                                 ('books', '📚'),
                                                 ('electronics', '💻'),
                                                 ('clothing', '👕'),
                                                 ('sports', '⚽'),
                                                 ('home', '🏠'),
                                                 ('entertainment', '🎮'),
                                                 ('transport', '🚲'),
                                                 ('furniture', '🛋️'),
                                                 ('baby', '👶'),
                                                 ('pets', '🐾');

-- 插入商品数据（基于mockData products）
INSERT INTO items (item_id, image_url, seller_id, category_id, item_name, price, item_condition, description, likes, is_available, update_time) VALUES
                                                                                                                                                    (1, 'https://via.placeholder.com/400x300', 4, 1, '高等数学教材（第七版）', 35.00, '九成新', '同济大学出版社，课本保护很好，几乎没有笔记，适合下学期使用。', 25, 1, '2024-01-15 10:00:00'),
                                                                                                                                                    (2, 'https://via.placeholder.com/400x300', 5, 2, 'MacBook Air M1 13寸', 6800.00, '八成新', '2021年购买，使用一年多，电池健康度95%，无拆修，外观轻微使用痕迹。', 45, 1, '2024-01-14 10:00:00'),
                                                                                                                                                    (3, 'https://via.placeholder.com/400x300', 6, 3, '北面羽绒服 黑色L码', 280.00, '九成新', '去年冬天买的，只穿过几次，保暖效果很好，适合北方冬天。', 18, 1, '2024-01-13 10:00:00'),
                                                                                                                                                    (4, 'https://via.placeholder.com/400x300', 7, 4, '篮球 斯伯丁正品', 80.00, '八成新', '正品斯伯丁篮球，手感很好，因为要毕业了所以出售。', 12, 1, '2024-01-12 10:00:00'),
                                                                                                                                                    (5, 'https://via.placeholder.com/400x300', 8, 2, '联想笔记本电脑 ThinkPad', 3200.00, '八成新', '商务办公本，性能稳定，轻薄便携，适合学习和工作使用。', 8, 1, '2024-01-11 10:00:00'),
                                                                                                                                                    (6, 'https://via.placeholder.com/400x300', 9, 4, '羽毛球拍 尤尼克斯', 120.00, '九成新', '尤尼克斯入门级球拍，手感不错，现在升级了新拍子所以出售。', 6, 0, '2024-01-10 10:00:00'),
                                                                                                                                                    (7, 'https://via.placeholder.com/400x300', 1, 1, '编程珠玑（第二版）', 25.00, '九成新', '经典编程书籍，内容深入浅出，适合程序员进阶学习。', 12, 1, '2024-01-24 10:00:00'),
                                                                                                                                                    (8, 'https://via.placeholder.com/400x300', 1, 1, 'Java核心技术', 45.00, '八成新', 'Java学习必备教材，涵盖核心概念和实践应用。', 8, 0, '2024-01-22 10:00:00'),
                                                                                                                                                    (9, 'https://via.placeholder.com/400x300', 1, 1, '算法导论（第三版）', 55.00, '九成新', '算法学习经典教材，理论与实践并重，适合计算机专业学生。', 15, 1, '2024-01-23 10:00:00'),
                                                                                                                                                    (10, 'https://via.placeholder.com/400x300', 1, 1, '数据结构与算法分析', 38.00, '八成新', '数据结构入门教材，配有详细的算法分析和代码示例。', 9, 0, '2024-01-19 10:00:00'),
                                                                                                                                                    (11, 'https://via.placeholder.com/400x300', 1, 1, '线性代数教材', 28.00, '九成新', '同济大学版线性代数教材，内容清晰，例题丰富。', 5, 1, '2024-01-18 10:00:00'),
                                                                                                                                                    (12, 'https://via.placeholder.com/400x300', 6, 4, '自行车', 80.00, '九成新', '自行车，适合锻炼身体，价格便宜，适合学生使用。', 5, 0, '2024-01-18 10:00:00');
- 插入100条全新商品数据（避免与现有数据重复）
-- 分类ID: 1-books, 2-electronics, 3-clothing, 4-sports, 5-home, 6-entertainment, 7-transport

INSERT INTO items (image_url, item_name, price, item_condition, description, likes, is_available, is_deleted, update_time, seller_id, category_id) VALUES

-- 电子产品 (分类ID: 2) - 15条
('https://example.com/items/201.jpg', 'Samsung Galaxy S23 Ultra', 7999.00, '九成新', '512GB 暗夜黑，S Pen功能完好', 28, true, false, NOW(), 1, 2),
('https://example.com/items/202.jpg', '联想ThinkPad X1', 8999.00, '九五新', '11代i7 16+1TB 碳纤维版', 35, true, false, NOW(), 2, 2),
('https://example.com/items/203.jpg', 'OPPO Find X5 Pro', 4599.00, '八成新', '哈苏影像系统，拍照出色', 22, true, false, NOW(), 3, 2),
('https://example.com/items/204.jpg', 'Microsoft Surface Pro 9', 6799.00, '九成新', 'i5处理器 8+256GB 配键盘套', 26, true, false, NOW(), 4, 2),
('https://example.com/items/205.jpg', 'vivo X90 Pro+', 5299.00, '九五新', '蔡司光学镜头，夜拍神器', 31, true, false, NOW(), 5, 2),
('https://example.com/items/206.jpg', 'MacBook Pro 14英寸', 14999.00, '九成新', 'M2 Pro芯片 16+512GB 深空灰', 42, true, false, NOW(), 1, 2),
('https://example.com/items/207.jpg', 'AirPods Max', 3999.00, '九成新', '头戴式降噪耳机，太空灰', 29, true, false, NOW(), 2, 2),
('https://example.com/items/208.jpg', 'Pixel 7 Pro', 4299.00, '八成新', '谷歌原生系统，拍照算法强', 18, true, false, NOW(), 3, 2),
('https://example.com/items/209.jpg', 'LG OLED 55英寸', 6999.00, '九成新', 'C2系列 4K HDR 游戏模式', 33, true, false, NOW(), 4, 2),
('https://example.com/items/210.jpg', 'Meta Quest 2', 1999.00, '八成新', 'VR头显 256GB 配精英绑带', 24, true, false, NOW(), 5, 2),
('https://example.com/items/211.jpg', 'Steam Deck', 3199.00, '九成新', '掌机游戏设备，512GB版本', 27, true, false, NOW(), 1, 2),
('https://example.com/items/212.jpg', 'JBL Charge 5', 699.00, '九成新', '便携蓝牙音箱，防水设计', 16, true, false, NOW(), 2, 2),
('https://example.com/items/213.jpg', 'Alienware游戏鼠标', 599.00, '九五新', '电竞级鼠标，RGB灯效', 15, true, false, NOW(), 3, 2),
('https://example.com/items/214.jpg', 'iPad mini 6', 3299.00, '九成新', '8.3英寸 256GB WiFi版', 21, true, false, NOW(), 4, 2),
('https://example.com/items/215.jpg', 'Redmi Note 12 Pro', 1599.00, '九五新', '2亿像素超清影像', 19, true, false, NOW(), 5, 2),

-- 服装鞋帽 (分类ID: 3) - 15条
('https://example.com/items/301.jpg', 'Canada Goose羽绒服', 4999.00, '九成新', 'XL码 军绿色 防寒等级-30°C', 38, true, false, NOW(), 1, 3),
('https://example.com/items/302.jpg', 'Jordan AJ1复刻', 1299.00, '八成新', '43码 芝加哥配色 经典款', 32, true, false, NOW(), 2, 3),
('https://example.com/items/303.jpg', 'Moncler polo衫', 899.00, '九五新', 'L码 海军蓝 意大利制造', 26, true, false, NOW(), 3, 3),
('https://example.com/items/304.jpg', 'Dr.Martens马丁靴', 799.00, '九成新', '40码 黑色 经典1460款', 29, true, false, NOW(), 4, 3),
('https://example.com/items/305.jpg', 'Supreme卫衣', 1999.00, '九成新', 'M码 Box Logo 红白配色', 45, true, false, NOW(), 5, 3),
('https://example.com/items/306.jpg', '始祖鸟冲锋衣', 2999.00, '九成新', 'L码 黑色 Beta AR系列', 34, true, false, NOW(), 1, 3),
('https://example.com/items/307.jpg', 'Gucci腰带', 2199.00, '九五新', '双G标志 黑色皮革 110cm', 28, true, false, NOW(), 2, 3),
('https://example.com/items/308.jpg', 'Stone Island卫裤', 1699.00, '八成新', 'L码 橄榄绿 经典罗盘标', 23, true, false, NOW(), 3, 3),
('https://example.com/items/309.jpg', 'Balenciaga老爹鞋', 2799.00, '九成新', '42码 Triple S 白灰配色', 31, true, false, NOW(), 4, 3),
('https://example.com/items/310.jpg', 'Palm Angels T恤', 899.00, '九五新', 'M码 黑色 荧光绿印花', 22, true, false, NOW(), 5, 3),
('https://example.com/items/311.jpg', 'Off-White牛仔裤', 1499.00, '八成新', '32码 破洞设计 蓝色', 27, true, false, NOW(), 1, 3),
('https://example.com/items/312.jpg', 'Yeezy 350 V2', 1899.00, '九成新', '42码 Zebra配色 椰子鞋', 36, true, false, NOW(), 2, 3),
('https://example.com/items/313.jpg', 'Fear of God卫衣', 2299.00, '九成新', 'L码 雾霾蓝 Essentials系列', 25, true, false, NOW(), 3, 3),
('https://example.com/items/314.jpg', 'Chrome Hearts眼镜', 3999.00, '九五新', '银色镜框 限量版设计', 30, true, false, NOW(), 4, 3),
('https://example.com/items/315.jpg', 'Stussy渔夫帽', 299.00, '九成新', '黑色 经典Logo刺绣', 14, true, false, NOW(), 5, 3),

-- 图书文具 (分类ID: 1) - 15条
('https://example.com/items/401.jpg', '深度学习', 129.00, '九成新', 'Ian Goodfellow著 AI圣经', 42, true, false, NOW(), 1, 1),
('https://example.com/items/402.jpg', '万历十五年', 35.00, '八成新', '黄仁宇史学名著', 28, true, false, NOW(), 2, 1),
('https://example.com/items/403.jpg', '数据结构与算法', 89.00, '九成新', 'C++版本 清华大学出版', 35, true, false, NOW(), 3, 1),
('https://example.com/items/404.jpg', 'Moleskine笔记本', 199.00, '全新', '经典款硬面 点格内页', 18, true, false, NOW(), 4, 1),
('https://example.com/items/405.jpg', '围城', 29.00, '七成新', '钱钟书代表作 人民文学版', 22, true, false, NOW(), 5, 1),
('https://example.com/items/406.jpg', 'Wacom数位板', 599.00, '九成新', 'CTL-472 数字绘画入门', 26, true, false, NOW(), 1, 1),
('https://example.com/items/407.jpg', '编译原理', 79.00, '八成新', '龙书 计算机经典教材', 31, true, false, NOW(), 2, 1),
('https://example.com/items/408.jpg', 'Uni-ball签字笔', 89.00, '全新', 'UB-150 黑色 12支装', 12, true, false, NOW(), 3, 1),
('https://example.com/items/409.jpg', '红楼梦', 69.00, '九成新', '人民文学出版社 四大名著', 25, true, false, NOW(), 4, 1),
('https://example.com/items/410.jpg', 'iPad画笔套装', 299.00, '九成新', '专业数字绘画配件', 20, true, false, NOW(), 5, 1),
('https://example.com/items/411.jpg', '概率论与数理统计', 59.00, '八成新', '同济版 工科数学必修', 17, true, false, NOW(), 1, 1),
('https://example.com/items/412.jpg', 'Rhodia方格笔记本', 129.00, '全新', '法国品牌 A5尺寸', 15, true, false, NOW(), 2, 1),
('https://example.com/items/413.jpg', '史记', 89.00, '九成新', '中华书局点校本', 19, true, false, NOW(), 3, 1),
('https://example.com/items/414.jpg', 'Lamy钢笔', 299.00, '九五新', 'Safari系列 EF尖', 21, true, false, NOW(), 4, 1),
('https://example.com/items/415.jpg', 'GRE词汇精选', 45.00, '八成新', '红宝书 出国考试必备', 16, true, false, NOW(), 5, 1),

-- 家居用品 (分类ID: 5) - 15条
('https://example.com/items/501.jpg', 'Dyson V15吸尘器', 2999.00, '九成新', '激光显微尘检测 配全套刷头', 38, true, false, NOW(), 1, 5),
('https://example.com/items/502.jpg', 'Herman Miller座椅', 6999.00, '九成新', 'Aeron人体工学椅 Size B', 45, true, false, NOW(), 2, 5),
('https://example.com/items/503.jpg', 'Nespresso咖啡机', 1299.00, '九成新', 'Vertuo系列 配胶囊', 29, true, false, NOW(), 3, 5),
('https://example.com/items/504.jpg', 'MUJI香薰机', 299.00, '九五新', '超声波雾化 定时功能', 23, true, false, NOW(), 4, 5),
('https://example.com/items/505.jpg', 'Shark蒸汽拖把', 799.00, '八成新', '高温杀菌 地面清洁', 25, true, false, NOW(), 5, 5),
('https://example.com/items/506.jpg', 'Simmons床垫', 3999.00, '九成新', '1.8米 独立袋装弹簧', 32, true, false, NOW(), 1, 5),
('https://example.com/items/507.jpg', 'Philips空气炸锅', 699.00, '九成新', '5.2L大容量 少油烹饪', 27, true, false, NOW(), 2, 5),
('https://example.com/items/508.jpg', 'IKEA立式衣架', 199.00, '八成新', '松木材质 可移动设计', 16, true, false, NOW(), 3, 5),
('https://example.com/items/509.jpg', 'Blueair空气净化器', 1999.00, '九成新', '适用50㎡ 瑞典品牌', 31, true, false, NOW(), 4, 5),
('https://example.com/items/510.jpg', 'Tempur记忆枕', 899.00, '九五新', '太空记忆棉 护颈设计', 24, true, false, NOW(), 5, 5),
('https://example.com/items/511.jpg', 'Zojirushi电饭煲', 1299.00, '九成新', '象印牌 5.5合 日本制造', 28, true, false, NOW(), 1, 5),
('https://example.com/items/512.jpg', 'Marimekko窗帘', 599.00, '九成新', '芬兰设计 2.5x1.5米', 22, true, false, NOW(), 2, 5),
('https://example.com/items/513.jpg', 'Bissell地毯清洁机', 899.00, '八成新', '深度清洁 宠物异味去除', 19, true, false, NOW(), 3, 5),
('https://example.com/items/514.jpg', 'Casper枕头', 399.00, '九成新', '三层结构 支撑透气', 18, true, false, NOW(), 4, 5),
('https://example.com/items/515.jpg', 'Vitamix破壁机', 2199.00, '九五新', '美国原装 2.0L容量', 26, true, false, NOW(), 5, 5),

-- 运动户外 (分类ID: 4) - 15条
('https://example.com/items/601.jpg', 'Trek公路车', 3999.00, '九成新', 'Domane SL5 碳纤维车架', 42, true, false, NOW(), 1, 4),
('https://example.com/items/602.jpg', 'Peloton动感单车', 8999.00, '八成新', '智能健身 配屏幕课程', 35, true, false, NOW(), 2, 4),
('https://example.com/items/603.jpg', 'Garmin跑步手表', 1999.00, '九成新', 'Forerunner 955 GPS定位', 38, true, false, NOW(), 3, 4),
('https://example.com/items/604.jpg', 'Thule车顶箱', 2299.00, '九成新', '460L大容量 空气动力学', 24, true, false, NOW(), 4, 4),
('https://example.com/items/605.jpg', 'Osprey登山包', 899.00, '九成新', 'Atmos 65L 透气背负', 29, true, false, NOW(), 5, 4),
('https://example.com/items/606.jpg', 'PowerBlock哑铃', 1999.00, '九成新', '可调节5-45磅 节省空间', 33, true, false, NOW(), 1, 4),
('https://example.com/items/607.jpg', 'Specialized头盔', 699.00, '九五新', 'Evade II 空气动力学', 21, true, false, NOW(), 2, 4),
('https://example.com/items/608.jpg', 'Patagonia冲锋衣', 1599.00, '九成新', 'Torrentshell 3L 防水透气', 27, true, false, NOW(), 3, 4),
('https://example.com/items/609.jpg', 'TRX悬挂训练器', 899.00, '八成新', '全身训练 便携设计', 26, true, false, NOW(), 4, 4),
('https://example.com/items/610.jpg', 'Hydro Flask保温杯', 299.00, '九成新', '32oz 双壁真空保温', 18, true, false, NOW(), 5, 4),
('https://example.com/items/611.jpg', 'Yeti冰桶', 1299.00, '九成新', 'Tundra 45 超长保冰', 31, true, false, NOW(), 1, 4),
('https://example.com/items/612.jpg', 'Bowflex健身器', 2999.00, '八成新', 'SelectTech多功能训练', 28, true, false, NOW(), 2, 4),
('https://example.com/items/613.jpg', 'Yakima自行车架', 599.00, '九成新', '车顶承载 安全固定', 22, true, false, NOW(), 3, 4),
('https://example.com/items/614.jpg', 'Manduka瑜伽垫', 799.00, '九成新', 'PRO系列 专业级厚度', 25, true, false, NOW(), 4, 4),
('https://example.com/items/615.jpg', 'Marmot睡袋', 999.00, '九成新', '-5°C舒适温度 鹅绒填充', 30, true, false, NOW(), 5, 4),

-- 娱乐休闲 (分类ID: 6) - 15条
('https://example.com/items/701.jpg', 'PlayStation 5', 3999.00, '九成新', '825GB SSD 白色版本游戏机', 45, true, false, NOW(), 1, 6),
('https://example.com/items/702.jpg', 'Xbox Series X', 3599.00, '九成新', '1TB 游戏主机 向下兼容', 38, true, false, NOW(), 2, 6),
('https://example.com/items/703.jpg', 'Yamaha电钢琴', 8999.00, '九成新', 'P-125 88键加权键盘', 42, true, false, NOW(), 3, 6),
('https://example.com/items/704.jpg', 'Board Game桌游', 299.00, '九成新', '卡坦岛+狼人杀+剧本杀套装', 32, true, false, NOW(), 4, 6),
('https://example.com/items/705.jpg', 'Lego积木', 899.00, '九五新', '建筑师系列 完整收藏版', 26, true, false, NOW(), 5, 6),
('https://example.com/items/706.jpg', 'Guitar吉他', 1599.00, '八成新', 'Martin D-28 民谣吉他', 35, true, false, NOW(), 1, 6),
('https://example.com/items/707.jpg', '棋牌套装', 199.00, '九成新', '象棋+围棋+五子棋套装', 18, true, false, NOW(), 2, 6),
('https://example.com/items/708.jpg', 'VR眼镜', 1999.00, '九成新', 'Oculus Quest 2 虚拟现实', 29, true, false, NOW(), 3, 6),
('https://example.com/items/709.jpg', '魔方收藏', 399.00, '九成新', '3x3+4x4+5x5阶魔方套装', 22, true, false, NOW(), 4, 6),
('https://example.com/items/710.jpg', 'Switch游戏卡', 299.00, '九成新', '塞尔达传说+马里奥卡丁车', 33, true, false, NOW(), 5, 6),
('https://example.com/items/711.jpg', 'DJ打碟机', 2999.00, '九成新', 'Pioneer DDJ-FLX4 控制器', 27, true, false, NOW(), 1, 6),
('https://example.com/items/712.jpg', '模型收藏', 699.00, '九成新', '高达模型MG系列组装完成', 24, true, false, NOW(), 2, 6),
('https://example.com/items/713.jpg', 'Drone航拍器', 1999.00, '九成新', 'DJI Mini 3 小型无人机', 30, true, false, NOW(), 3, 6),
('https://example.com/items/714.jpg', '电子书阅读器', 899.00, '九成新', 'Kindle Paperwhite 护眼屏', 21, true, false, NOW(), 4, 6),
('https://example.com/items/715.jpg', '投影仪', 1599.00, '九五新', '小米投影仪 1080P家用', 28, true, false, NOW(), 5, 6),

-- 交通工具 (分类ID: 7) - 10条
('https://example.com/items/801.jpg', '小牛电动车', 3999.00, '九成新', 'NGT青春版 续航65km', 32, true, false, NOW(), 1, 7),
('https://example.com/items/802.jpg', '雅马哈踏板车', 8999.00, '八成新', '巧格i 125cc 省油代步', 28, true, false, NOW(), 2, 7),
('https://example.com/items/803.jpg', '折叠自行车', 1299.00, '九成新', '20寸变速折叠便携出行', 19, true, false, NOW(), 3, 7),
('https://example.com/items/804.jpg', '平衡车', 1999.00, '九成新', '10寸大轮自平衡电动车', 25, true, false, NOW(), 4, 7),
('https://example.com/items/805.jpg', '滑板车', 599.00, '八成新', '成人电动滑板车代步神器', 16, true, false, NOW(), 5, 7),
('https://example.com/items/806.jpg', '山地自行车', 2599.00, '九成新', '26寸21速变速越野自行车', 35, true, false, NOW(), 1, 7),
('https://example.com/items/807.jpg', '电动汽车充电桩', 2999.00, '九五新', '家用220V便携式充电器', 22, true, false, NOW(), 2, 7),
('https://example.com/items/808.jpg', '汽车脚垫', 299.00, '全新', '全包围皮革脚垫防水耐脏', 14, true, false, NOW(), 3, 7),
('https://example.com/items/809.jpg', '行车记录仪', 899.00, '九成新', '4K高清前后双录夜视', 26, true, false, NOW(), 4, 7),
('https://example.com/items/810.jpg', '车载充电器', 199.00, '九五新', '快充双USB车充适配器', 12, true, false, NOW(), 5, 7);
INSERT INTO items (image_url, item_name, price, item_condition, description, likes, is_available, is_deleted, update_time, seller_id, category_id) VALUES

-- 电子产品 (分类ID: 2) - 8条
('https://example.com/items/811.jpg', '小米13 Ultra', 4599.00, '九成新', '1英寸大底徕卡镜头，摄影旗舰', 33, true, false, NOW(), 1, 2),
('https://example.com/items/812.jpg', 'ROG游戏本', 12999.00, '九成新', 'RTX 4080 32GB 240Hz屏幕', 48, true, false, NOW(), 2, 2),
('https://example.com/items/813.jpg', 'Sony WH-1000XM5', 1999.00, '九五新', '头戴式降噪耳机，音质出色', 29, true, false, NOW(), 3, 2),
('https://example.com/items/814.jpg', 'Nintendo Switch OLED', 2099.00, '九成新', '任天堂游戏机，7英寸OLED屏', 35, true, false, NOW(), 4, 2),
('https://example.com/items/815.jpg', 'GoPro Hero 11', 2299.00, '八成新', '运动相机，5.3K视频拍摄', 24, true, false, NOW(), 5, 2),
('https://example.com/items/816.jpg', 'Bose SoundLink Max', 1299.00, '九成新', '便携蓝牙音箱，重低音效果', 22, true, false, NOW(), 1, 2),
('https://example.com/items/817.jpg', 'Kindle Oasis', 1699.00, '九五新', '7英寸电子书阅读器，护眼屏', 18, true, false, NOW(), 2, 2),
('https://example.com/items/818.jpg', 'DJI Action 2', 1599.00, '九成新', '迷你运动相机，磁吸设计', 27, true, false, NOW(), 3, 2),

-- 服装鞋帽 (分类ID: 3) - 8条
('https://example.com/items/819.jpg', 'The North Face羽绒服', 2299.00, '九成新', 'L码 黑色 700蓬松度鹅绒', 31, true, false, NOW(), 4, 3),
('https://example.com/items/820.jpg', 'Nike Air Max 270', 899.00, '八成新', '42.5码 白黑配色 气垫缓震', 26, true, false, NOW(), 5, 3),
('https://example.com/items/821.jpg', 'Uniqlo优衣库羊绒衫', 399.00, '九五新', 'M码 驼色 100%羊绒', 19, true, false, NOW(), 1, 3),
('https://example.com/items/822.jpg', 'Converse帆布鞋', 299.00, '九成新', '39码 经典白色 All Star', 15, true, false, NOW(), 2, 3),
('https://example.com/items/823.jpg', 'Zara风衣', 599.00, '九成新', 'S码 卡其色 双排扣设计', 23, true, false, NOW(), 3, 3),
('https://example.com/items/824.jpg', 'Adidas Stan Smith', 599.00, '八成新', '41码 白绿配色 经典款', 21, true, false, NOW(), 4, 3),
('https://example.com/items/825.jpg', 'COS毛衣', 799.00, '九五新', 'L码 灰色 羊毛混纺', 17, true, false, NOW(), 5, 3),
('https://example.com/items/826.jpg', 'New Balance 990v5', 1299.00, '九成新', '43码 灰色 美产限量版', 28, true, false, NOW(), 1, 3),

-- 图书文具 (分类ID: 1) - 7条
('https://example.com/items/827.jpg', '计算机网络', 79.00, '九成新', '谢希仁第8版 网络技术教材', 32, true, false, NOW(), 2, 1),
('https://example.com/items/828.jpg', '百年孤独', 45.00, '八成新', '马尔克斯代表作 魔幻现实主义', 25, true, false, NOW(), 3, 1),
('https://example.com/items/829.jpg', 'Pilot百乐钢笔', 199.00, '九五新', 'Kakuno系列 F尖 透明笔身', 16, true, false, NOW(), 4, 1),
('https://example.com/items/830.jpg', '线性代数', 69.00, '九成新', '同济版 理工科数学基础', 19, true, false, NOW(), 5, 1),
('https://example.com/items/831.jpg', 'iPad Pro妙控键盘', 1999.00, '九成新', '11英寸版本 悬浮式设计', 24, true, false, NOW(), 1, 1),
('https://example.com/items/832.jpg', '三体', 89.00, '九成新', '刘慈欣科幻三部曲 完整版', 38, true, false, NOW(), 2, 1),
('https://example.com/items/833.jpg', 'Staedtler自动铅笔', 59.00, '全新', '925 25专业绘图铅笔', 12, true, false, NOW(), 3, 1),

-- 家居用品 (分类ID: 5) - 7条
('https://example.com/items/834.jpg', 'Xiaomi空气净化器', 899.00, '九成新', '4 MAX版本 适用60㎡', 28, true, false, NOW(), 4, 5),
('https://example.com/items/835.jpg', 'KitchenAid厨师机', 3299.00, '九成新', '5夸脱 经典红色 全能料理', 35, true, false, NOW(), 5, 5),
('https://example.com/items/836.jpg', 'Midea美的洗碗机', 2199.00, '八成新', '8套餐具 嵌入式设计', 22, true, false, NOW(), 1, 5),
('https://example.com/items/837.jpg', 'Tefal特福电压力锅', 599.00, '九成新', '6L容量 智能烹饪程序', 19, true, false, NOW(), 2, 5),
('https://example.com/items/838.jpg', '3M净水器', 1299.00, '九成新', '反渗透技术 直饮净水', 26, true, false, NOW(), 3, 5),
('https://example.com/items/839.jpg', 'Electrolux伊莱克斯加湿器', 499.00, '九五新', '超声波雾化 4L大容量', 17, true, false, NOW(), 4, 5),
('https://example.com/items/840.jpg', 'LOCK&LOCK保鲜盒', 199.00, '全新', '冰箱收纳套装 密封性好', 14, true, false, NOW(), 5, 5),

-- 运动户外 (分类ID: 4) - 7条
('https://example.com/items/841.jpg', 'Giant捷安特山地车', 2999.00, '九成新', 'ATX 770 27速变速系统', 31, true, false, NOW(), 1, 4),
('https://example.com/items/842.jpg', 'Under Armour运动裤', 399.00, '九成新', 'L码 训练裤 透气排汗', 18, true, false, NOW(), 2, 4),
('https://example.com/items/843.jpg', 'Fitbit智能手环', 999.00, '八成新', 'Versa 4 健康监测', 25, true, false, NOW(), 3, 4),
('https://example.com/items/844.jpg', 'Columbia哥伦比亚抓绒衣', 599.00, '九成新', 'M码 保暖透气 户外必备', 20, true, false, NOW(), 4, 4),
('https://example.com/items/845.jpg', 'Reebok跑步机', 4999.00, '八成新', '家用电动跑步机 减震设计', 33, true, false, NOW(), 5, 4),
('https://example.com/items/846.jpg', 'Salomon登山鞋', 1299.00, '九成新', '42码 防水透气 户外徒步', 27, true, false, NOW(), 1, 4),
('https://example.com/items/847.jpg', 'Wilson网球拍', 899.00, '九成新', 'Pro Staff 经典款', 22, true, false, NOW(), 2, 4),

-- 娱乐休闲 (分类ID: 6) - 7条
('https://example.com/items/848.jpg', 'Nintendo Switch游戏', 399.00, '九成新', '塞尔达王国之泪 限定版', 32, true, false, NOW(), 3, 6),
('https://example.com/items/849.jpg', 'Apple TV 4K', 1299.00, '九成新', '第三代 64GB 流媒体播放器', 24, true, false, NOW(), 4, 6),
('https://example.com/items/850.jpg', 'Beats耳机', 1599.00, '九成新', 'Studio3 Wireless 降噪', 38, true, false, NOW(), 5, 6),
('https://example.com/items/851.jpg', '大富翁桌游', 299.00, '九五新', '经典版+扩展包全套', 18, true, false, NOW(), 1, 6),
('https://example.com/items/852.jpg', 'Ukulele尤克里里', 599.00, '九成新', '23寸初学者适用', 21, true, false, NOW(), 2, 6),
('https://example.com/items/853.jpg', 'iPad游戏手柄', 299.00, '九成新', 'Razer Kishi 手机游戏手柄', 16, true, false, NOW(), 3, 6),
('https://example.com/items/854.jpg', '天文望远镜', 1999.00, '九成新', '80mm口径 天体观测入门', 28, true, false, NOW(), 4, 6),

-- 交通工具 (分类ID: 7) - 6条
('https://example.com/items/855.jpg', '电动滑板', 1999.00, '九成新', '四轮电动滑板 遥控操作', 26, true, false, NOW(), 5, 7),
('https://example.com/items/856.jpg', '独轮车', 799.00, '九成新', '成人学习独轮车 平衡训练', 14, true, false, NOW(), 1, 7),
('https://example.com/items/857.jpg', '电动车头盔', 299.00, '九五新', '3C认证 安全防护头盔', 18, true, false, NOW(), 2, 7),
('https://example.com/items/858.jpg', '车载支架', 199.00, '八成新', '手机导航支架 磁力吸附', 12, true, false, NOW(), 3, 7),
('https://example.com/items/859.jpg', '儿童自行车', 599.00, '九成新', '16寸儿童单车 辅助轮', 21, true, false, NOW(), 4, 7),
('https://example.com/items/860.jpg', '电瓶车雨衣', 99.00, '全新', '加厚防水雨衣套装', 8, true, false, NOW(), 5, 7);
-- 插入订单数据（基于mockData initialOrders）
INSERT INTO orders (order_id, item_id, buyer_id, seller_id, order_amount, if_buyer_confirm, if_seller_confirm, order_status, create_time, confirm_time, finish_time, cancel_time) VALUES
                                                                                                                                                                                      ('ORD001', 1, 1, 4, 35.00, 1, 0, 0, '2024-01-26 16:20:00', NULL, NULL, NULL),
                                                                                                                                                                                      ('ORD002', 2, 1, 5, 6800.00, 1, 1, 1, '2024-01-25 09:15:00', '2024-01-25 10:30:00', NULL, NULL),
                                                                                                                                                                                      ('ORD003', 3, 1, 6, 280.00, 1, 1, 2, '2024-01-24 14:00:00', '2024-01-24 15:00:00', NULL, NULL),
                                                                                                                                                                                      ('ORD004', 4, 1, 7, 80.00, 1, 1, 2, '2024-01-23 11:00:00', '2024-01-23 12:00:00', NULL, NULL),
                                                                                                                                                                                      ('ORD005', 7, 2, 1, 25.00, 1, 0, 0, '2024-01-25 16:30:00', NULL, NULL, NULL),
                                                                                                                                                                                      ('ORD006', 8, 3, 1, 45.00, 1, 1, 1, '2024-01-24 10:15:00', '2024-01-24 11:00:00', NULL, NULL),
                                                                                                                                                                                      ('ORD007', 9, 4, 1, 55.00, 1, 1, 2, '2024-01-22 09:30:00', '2024-01-22 10:15:00', NULL, NULL),
                                                                                                                                                                                      ('ORD008', 10, 5, 1, 38.00, 1, 1, 2, '2024-01-21 13:20:00', '2024-01-21 14:00:00', NULL, NULL),
                                                                                                                                                                                      ('ORD009', 11, 6, 1, 28.00, 1, 1, 3, '2024-01-19 13:45:00', '2024-01-19 14:20:00', '2024-01-21 11:15:00', NULL),
                                                                                                                                                                                      ('ORD010', 5, 1, 8, 3200.00, 1, 0, 4, '2024-01-15 11:45:00', NULL, NULL, '2024-01-16 09:20:00'),
                                                                                                                                                                                      ('ORD011', 12, 1, 6, 80.00, 1, 1, 3, '2024-01-19 13:45:00', '2024-01-19 14:20:00', '2024-01-21 11:15:00', NULL);

-- 插入收藏数据（基于商品的favorites数量随机生成）
INSERT INTO favorites (user_id, item_id) VALUES
-- 商品1的收藏（25个收藏，随机分配给用户）
(2, 1), (3, 1), (5, 1), (6, 1), (7, 1), (8, 1), (9, 1),
-- 商品2的收藏（45个收藏，由于用户数量有限，部分用户收藏）
(2, 2), (3, 2), (4, 2), (6, 2), (7, 2), (8, 2), (9, 2),
-- 商品3的收藏（18个收藏）
(2, 3), (3, 3), (4, 3), (5, 3), (7, 3), (8, 3),
-- 商品4的收藏（12个收藏）
(2, 4), (3, 4), (5, 4), (6, 4), (8, 4),
-- 商品7的收藏（12个收藏）
(3, 7), (4, 7), (5, 7), (6, 7), (8, 7),
-- 商品9的收藏（15个收藏）
(2, 9), (3, 9), (5, 9), (6, 9), (7, 9), (8, 9);

-- 插入一些商品留言示例
INSERT INTO messages (item_id, user_id, parent_id, content, reply_time) VALUES
                                                                            (1, 2, 0, '这本书的内容怎么样？有习题答案吗？', '2024-01-16 09:30:00'),
                                                                            (1, 4, 1, '内容很好，没有习题答案，但是有详细的例题讲解。', '2024-01-16 10:15:00'),
                                                                            (2, 3, 0, '笔记本还在保修期内吗？', '2024-01-15 14:20:00'),
                                                                            (2, 5, 3, '已经过保了，但是使用状况很好，可以当面验货。', '2024-01-15 15:00:00'),
                                                                            (9, 6, 0, '这本算法书适合初学者吗？', '2024-01-24 16:30:00'),
                                                                            (9, 1, 5, '比较适合有一定基础的同学，初学者建议先学数据结构。', '2024-01-24 17:00:00');