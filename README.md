# 🛵 外卖配送平台 (Waimai Delivery Platform)

基于 Spring Boot 3 + Vue 3 的全栈外卖配送系统，支持**客户端点餐**、**商户管理**、**骑手配送**、**后台管理**四端。

---

## 项目结构

```
waimai-backend/
├── api/                        # Spring Boot 应用层 (端口 8080)
│   └── src/main/java/com/waimai/api/
│       ├── controller/         # 15 个 REST 控制器
│       ├── config/             # WebMvc, Redis, WebSocket, 异常处理, MyBatis 自动填充
│       ├── interceptor/        # 登录拦截器, 限流拦截器
│       └── WaimaiApplication.java
├── common/                     # 共享模块
│   └── src/main/java/com/waimai/common/
│       ├── entity/             # 20 个数据库实体
│       ├── dto/                # 13 个数据传输对象
│       ├── vo/                 # 8 个视图对象
│       ├── constant/           # 状态常量 (OrderStatus, RiderStatus 等)
│       ├── utils/              # JWT, Snowflake ID, 高德地图工具
│       └── Result.java         # 统一响应格式
├── service/                    # 业务逻辑层
│   └── src/main/java/com/waimai/service/
│       ├── service/            # 25 个服务接口
│       ├── impl/               # 16 个服务实现
│       ├── mapper/             # 20 个 MyBatis-Plus Mapper
│       ├── consumer/           # RabbitMQ 消费者 (订单超时取消)
│       └── push/               # WebSocket 推送服务
├── web-customer/               # 🧑 客户端 Vue3 + Vant4 (端口 5174)
├── web-merchant/               # 🏪 商户端 Vue3 + Vant4 (端口 5176)
├── web-rider/                  # 🛵 骑手端 Vue3 + Vant4 (端口 5175)
├── web-admin/                  # ⚙️ 管理后台 Vue3 + Element Plus (端口 5173)
├── init.sql                    # 数据库初始化脚本 (20 张表)
└── pom.xml                     # Maven 父 POM
```

---

## 技术栈

| 层级 | 技术 |
|------|------|
| **后端框架** | Spring Boot 3.2.5, Java 17 |
| **ORM** | MyBatis-Plus 3.5.6 |
| **数据库** | MySQL 8.x |
| **缓存** | Redis (GEO 位置索引, Lua 库存扣减) |
| **消息队列** | RabbitMQ (订单超时延迟取消) |
| **实时通信** | WebSocket (骑手位置推送, 订单通知) |
| **API 文档** | SpringDoc OpenAPI 2.5.0 |
| **认证** | JWT (access + refresh token) |
| **前端 (管理后台)** | Vue 3 + Element Plus 2 + ECharts 6 |
| **前端 (移动端)** | Vue 3 + Vant 4 + 高德地图 |
| **构建工具** | Maven (后端) + Vite (前端) |

---

## 实体说明

### 核心业务实体

| 实体 | 表名 | 说明 |
|------|------|------|
| `User` | user | 顾客（微信登录，openid 唯一标识） |
| `Merchant` | merchant | 商家（需审核入驻，含营业执照、评分、位置） |
| `Rider` | rider | 骑手（实名认证、审核、等级积分、余额） |
| `Category` | category | 菜品分类（每个商家自定义） |
| `Dish` | dish | 菜品（价格、库存、上下架） |
| `Order` | `order` | 订单（完整状态机，含ETA、超时、联合配送标记） |

### 订单子实体

| 实体 | 表名 | 说明 |
|------|------|------|
| `OrderDetail` | order_detail | 订单明细（下单时快照菜名和价格） |
| `OrderDispute` | order_dispute | 纠纷/退款（客户申请→商户处理→平台审核） |
| `Payment` | payment | 支付记录（支持幂等校验） |

### 配送相关

| 实体 | 表名 | 说明 |
|------|------|------|
| `DeliveryTrack` | delivery_track | 骑手 GPS 轨迹 |
| `DeliveryException` | delivery_exception | 骑手配送异常上报 |
| `JointDeliveryGroup` | joint_delivery_group | 联合配送组（多骑手协作） |
| `JointDeliveryMember` | joint_delivery_member | 联合配送成员 |

### 其他

| 实体 | 表名 | 说明 |
|------|------|------|
| `Review` | review | 评价（每订单可分别评价商家和骑手） |
| `Coupon` | coupon | 优惠券模板（满减/折扣/免配送费） |
| `UserCoupon` | user_coupon | 用户领取的优惠券 |
| `UserAddress` | user_address | 用户收货地址 |
| `RiderIncome` | rider_income | 骑手收入记录 |
| `RiderWithdrawal` | rider_withdrawal | 骑手提现记录 |
| `SystemConfig` | system_config | 系统配置键值对 |

### 订单状态机

```
PENDING_PAYMENT ──(支付)──▶ PAID ──(商户接单)──▶ PREPARING
      │                      │                        │
      ▼                      ▼                        ▼
  CANCELLED            CANCELLED              (骑手接单) ACCEPTED
                                                   │
                                          (取餐) DELIVERING
                                                   │
                                          (送达) COMPLETED

任意已支付状态 ──(申请退款)──▶ REFUNDING ──(商户同意)──▶ REFUNDED
                                   │
                             (商户拒绝)──▶ 恢复原状态
```

---

## API 控制器

| 控制器 | 路径 | 功能 |
|--------|------|------|
| `AuthController` | `/api/auth` | 登录/注册/刷新 Token |
| `UserController` | `/api/user` | 用户信息/地址管理 |
| `MerchantController` | `/api/merchant` | 商家入驻/信息/附近搜索/图片上传/报表 |
| `DishController` | `/api/merchant/{id}/dishes` | 菜品 CRUD |
| `CategoryController` | `/api/merchant/{id}/categories` | 分类管理 |
| `CartController` | `/api/cart` | 购物车 |
| `OrderController` | `/api/order` | 下单/支付/取消/轨迹查询 |
| `RiderController` | `/api/rider` | 骑手认证/上下线/接单/取餐/送达/收入/异常上报 |
| `ReviewController` | `/api/review` | 评价提交/查询 |
| `CouponController` | `/api/coupon` | 优惠券领取/查询 |
| `DisputeController` | `/api/dispute` | 纠纷创建/退款申请/取消 |
| `JointDeliveryController` | `/api/joint-delivery` | 联合配送组管理 |
| `RecommendController` | `/api/recommend` | AI 菜品推荐 |
| `AdminController` | `/api/admin` | 管理后台（仪表盘/审核/订单监控/优惠券/纠纷处理） |
| `SystemConfigController` | `/api/admin/config` | 系统配置管理 |

---

## 启动说明

### 前置条件

- JDK 17+
- Maven 3.8+
- Node.js 18+ (前端)
- MySQL 8.x
- Redis
- RabbitMQ

### 1. 初始化数据库

```bash
mysql -u root -p < init.sql
```

### 2. 配置修改

编辑 `api/src/main/resources/application.yml`，修改数据库密码、Redis 密码、RabbitMQ 配置：

```yaml
spring:
  datasource:
    password: your_mysql_password
  data:
    redis:
      password: your_redis_password
  rabbitmq:
    password: your_rabbitmq_password
```

### 3. 启动后端

```bash
# 在项目根目录
mvn clean install -DskipTests
mvn -pl api spring-boot:run
```

后端启动后访问: `http://localhost:8080`

API 文档: `http://localhost:8080/swagger-ui.html`

### 4. 启动前端

```bash
# 管理后台 (端口 5173)
cd web-admin && npm install && npm run dev

# 客户端 (端口 5174)
cd web-customer && npm install && npm run dev

# 骑手端 (端口 5175)
cd web-rider && npm install && npm run dev

# 商户端 (端口 5176)
cd web-merchant && npm install && npm run dev
```

### 5. 访问地址

| 端 | 地址 |
|----|------|
| 管理后台 | http://localhost:5173 |
| 客户端 | http://localhost:5174 |
| 骑手端 | http://localhost:5175 |
| 商户端 | http://localhost:5176 |

---

## 文件说明

### 后端核心文件

| 文件 | 说明 |
|------|------|
| `pom.xml` | Maven 父 POM，管理 Spring Boot 3.2.5 + 依赖版本 |
| `api/pom.xml` | API 模块 POM |
| `common/pom.xml` | 公共模块 POM |
| `service/pom.xml` | 服务模块 POM |
| `api/.../WaimaiApplication.java` | Spring Boot 启动类 |
| `api/.../application.yml` | 主配置文件（数据库/Redis/RabbitMQ/JWT/高德地图/DeepSeek） |
| `api/.../WebMvcConfig.java` | 静态资源映射 + 拦截器注册 |
| `api/.../GlobalExceptionHandler.java` | 全局异常处理 (BusinessException/405/500) |
| `api/.../MybatisMetaHandler.java` | MyBatis-Plus 自动填充 createTime/updateTime/receiveTime |
| `api/.../LoginInterceptor.java` | JWT 登录拦截器 |
| `api/.../RateLimitInterceptor.java` | API 限流拦截器 |
| `service/.../RabbitMQConfig.java` | RabbitMQ 延迟队列配置 (30分钟超时取消) |
| `service/.../OrderTimeoutConsumer.java` | 订单超时消费者 |
| `common/.../Result.java` | 统一响应 `{code, message, data}` |
| `common/.../BusinessException.java` | 业务异常类 |
| `common/.../UserContext.java` | ThreadLocal 用户上下文 |
| `common/.../SnowflakeUtil.java` | 雪花 ID 生成器 |
| `common/.../JwtUtil.java` | JWT 令牌工具 |
| `common/.../MapUtils.java` | 高德地图坐标转换/距离计算 |
| `api/.../lua/deduct_inventory.lua` | Redis Lua 库存扣减脚本 |
| `api/.../lua/rollback_inventory.lua` | Redis Lua 库存回滚脚本 |

### 前端核心文件 (以 web-customer 为例)

| 文件 | 说明 |
|------|------|
| `package.json` | 依赖和脚本 (Vue3, Vant4, Pinia, Axios) |
| `vite.config.ts` | Vite 配置 (端口, 代理 /api→8080, /uploads→8080) |
| `index.html` | HTML 入口 |
| `src/main.ts` | Vue 应用启动 |
| `src/App.vue` | 根组件 (CSS 变量 + 页面过渡动画) |
| `src/router/index.ts` | 路由配置 (含登录守卫) |
| `src/utils/request.ts` | Axios 封装 (Token 注入 + 错误处理) |
| `src/vant-plugin.ts` | Vant 组件注册 |

### 脚本工具

| 文件 | 说明 |
|------|------|
| `start-all.bat` | Windows 一键启动四端前端 (5173-5176) |
| `stop-all.bat` | Windows 一键停止所有前端 dev server |
| `test-joint-delivery.sh` | 多骑手联合配送端到端测试脚本 (curl) |

```bash
# 一键启动所有前端
start-frontend.ps1
```

### 数据库

| 文件 | 说明 |
|------|------|
| `init.sql` | 完整建库脚本，含 20 张表 + 种子数据，一键初始化 |

---

## 关键业务特性

- **智能派单**: 基于 Redis GEO 搜索附近骑手，距离+负载加权评分自动派单
- **联合配送**: 大订单自动拆分为多个子任务，多骑手协作配送
- **库存扣减**: Redis Lua 脚本原子性扣减，防止超卖
- **超时取消**: RabbitMQ 延迟队列 30 分钟未支付自动取消并回滚库存
- **退款处理**: 客户申请→商户同意/拒绝→回滚库存→标记支付已退款
- **骑手异常**: 支持上报联系不上顾客、地址错误、商品损坏等异常
- **AI 推荐**: 接入 DeepSeek API 智能推荐菜品
- **实时推送**: WebSocket 推送骑手位置、新订单通知
- **文件上传**: 菜品图片上传，支持本地存储
