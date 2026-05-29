# 外卖平台后端 API 功能验证指导书

**Base URL:** `http://localhost:8080/api`
**版本:** v3.0 (完整三端 — 顾客 / 商家 / 骑手 + 管理员审核)
**日期:** 2026-05-22

---

## 前置条件

1. MySQL 数据库 `waimai` 已创建并执行 `database.sql`
2. Redis 运行在 `localhost:6379`
3. RabbitMQ 运行在 `localhost:5672`
4. 后端服务启动在 `localhost:8080`

**迁移脚本执行顺序：**
```sql
source database.sql;
source migration_rider_audit.sql;
source migration_v2_audit_fields.sql;
```

---

## 1. 认证模块 — AuthController

### 1.1 用户（顾客）微信登录

```
POST /api/auth/login/wechat
```

**请求体：**
```json
{
    "code": "test_user_001",
    "nickname": "张三",
    "avatar": "https://example.com/avatar1.png"
}
```

**成功响应 (200)：**
```json
{
    "code": 200,
    "message": "success",
    "data": {
        "accessToken": "eyJhbGciOiJIUzI1NiJ9...",
        "refreshToken": "eyJhbGciOiJIUzI1NiJ9...",
        "userId": 1,
        "nickname": "张三",
        "avatar": "https://example.com/avatar1.png"
    }
}
```

---

### 1.2 商家微信登录

```
POST /api/auth/login/merchant/wechat
```

**请求体：**
```json
{
    "code": "test_merchant_001",
    "nickname": "张记餐厅",
    "avatar": "https://example.com/shop.png"
}
```

**未入驻时响应 (500)：**
```json
{
    "code": 500,
    "message": "商家未入驻，请先提交入驻申请",
    "data": null
}
```

**审核中时响应 (500)：**
```json
{
    "code": 500,
    "message": "商家正在审核中，请耐心等待",
    "data": null
}
```

**驳回时响应 (500)：**
```json
{
    "code": 500,
    "message": "商家入驻申请已被驳回，请联系平台",
    "data": null
}
```

**审核通过后成功响应 (200)：**
```json
{
    "code": 200,
    "message": "success",
    "data": {
        "accessToken": "eyJhbGciOiJIUzI1NiJ9...",
        "refreshToken": "eyJhbGciOiJIUzI1NiJ9...",
        "userId": 1,
        "nickname": "张记餐厅",
        "avatar": "https://example.com/shop.png"
    }
}
```

---

### 1.3 骑手微信登录

```
POST /api/auth/login/rider/wechat
```

**请求体：**
```json
{
    "code": "test_rider_001",
    "nickname": "李四",
    "avatar": "https://example.com/rider.png"
}
```

**未注册时响应 (500)：**
```json
{
    "code": 500,
    "message": "骑手未注册，请先注册",
    "data": null
}
```

**审核中时响应 (500)：**
```json
{
    "code": 500,
    "message": "骑手正在审核中，请耐心等待",
    "data": null
}
```

**驳回时响应 (500)：**
```json
{
    "code": 500,
    "message": "骑手注册申请已被驳回，请联系平台",
    "data": null
}
```

**审核通过后成功响应 (200)：**
```json
{
    "code": 200,
    "message": "success",
    "data": {
        "accessToken": "eyJhbGciOiJIUzI1NiJ9...",
        "refreshToken": "eyJhbGciOiJIUzI1NiJ9...",
        "userId": 1,
        "nickname": "李四",
        "avatar": "https://example.com/rider.png"
    }
}
```

---

### 1.4 骑手自主注册（无需登录）

```
POST /api/auth/register/rider
```

**请求体：**
```json
{
    "code": "test_rider_001",
    "nickname": "李四",
    "avatar": "https://example.com/rider.png",
    "realName": "李四",
    "idCard": "110101199003071234",
    "phone": "13800138000"
}
```

**身份证号说明：** 18位，格式 `6位地区码 + 8位生日(YYYYMMDD) + 3位顺序码 + 1位校验码`。校验码为数字或X。示例中 `110101199003071234` 可通过内置 `IdCardUtil.isValid()` 校验。

**正常注册成功 (200)：**
```json
{
    "code": 200,
    "message": "success",
    "data": {
        "accessToken": "eyJhbGciOiJIUzI1NiJ9...",
        "refreshToken": "eyJhbGciOiJIUzI1NiJ9...",
        "userId": 1,
        "nickname": "李四",
        "avatar": "https://example.com/rider.png"
    }
}
```

**重复注册（已通过审核）响应 (500)：**
```json
{
    "code": 500,
    "message": "您已注册并通过审核，请直接登录",
    "data": null
}
```

**重复注册（待审核）响应 (500)：**
```json
{
    "code": 500,
    "message": "您已申请过，请勿重复申请",
    "data": null
}
```

**参数校验失败（缺少必填项）响应 (500)：**
```json
{
    "code": 500,
    "message": "真实姓名不能为空",
    "data": null
}
```

---

### 1.5 骑手审核进度查询（无需登录）

```
GET /api/auth/register/rider/status?code=test_rider_001
```

**未注册时响应 (200)：**
```json
{
    "code": 200,
    "message": "success",
    "data": {
        "registered": false,
        "auditStatus": -1,
        "auditStatusText": "未注册"
    }
}
```

**待审核时响应 (200)：**
```json
{
    "code": 200,
    "message": "success",
    "data": {
        "registered": true,
        "auditStatus": 0,
        "auditStatusText": "待审核",
        "rejectionReason": null,
        "realName": "李四",
        "phone": "13800138000"
    }
}
```

**审核通过时响应 (200)：**
```json
{
    "code": 200,
    "message": "success",
    "data": {
        "registered": true,
        "auditStatus": 1,
        "auditStatusText": "审核通过",
        "rejectionReason": null,
        "realName": "李四",
        "phone": "13800138000"
    }
}
```

**已驳回时响应 (200)：**
```json
{
    "code": 200,
    "message": "success",
    "data": {
        "registered": true,
        "auditStatus": 2,
        "auditStatusText": "已驳回",
        "rejectionReason": "身份证照片不清晰，请重新上传",
        "realName": "李四",
        "phone": "13800138000"
    }
}
```

---

### 1.6 管理员登录

```
POST /api/auth/login/admin
```

**请求体：**
```json
{
    "username": "admin",
    "password": "admin123"
}
```

**成功响应 (200)：**
```json
{
    "code": 200,
    "message": "success",
    "data": {
        "accessToken": "eyJhbGciOiJIUzI1NiJ9...",
        "refreshToken": "eyJhbGciOiJIUzI1NiJ9...",
        "userId": 0,
        "nickname": "管理员",
        "avatar": null
    }
}
```

---

### 1.7 Token 刷新

```
POST /api/auth/refresh-token
Authorization: Bearer {refreshToken}
```

**成功响应 (200)：** 同上格式，返回新的 accessToken + refreshToken。

---

## 2. 商家入驻模块 — MerchantController

### 2.1 商家入驻申请（无需登录）

```
POST /api/merchant/apply
```

**请求体：**
```json
{
    "code": "test_merchant_001",
    "name": "张记川菜馆",
    "businessLicense": "91110108MA01XXXXX",
    "phone": "13900139000",
    "address": "北京市朝阳区建国路88号",
    "longitude": 116.461,
    "latitude": 39.908,
    "description": "正宗川菜，麻辣鲜香，十年老店"
}
```

**成功响应 (200)：**
```json
{
    "code": 200,
    "message": "success",
    "data": 1
}
```
> `data` 返回新商家的主键 ID，可直接用于后续审核接口。

**参数校验失败响应 (400)：**
```json
{
    "code": 400,
    "message": "code不能为空",
    "data": null
}
```

---

### 2.2 商家入驻进度查询（无需登录）

```
GET /api/merchant/apply/status?code=test_merchant_001
```

**未申请时响应 (200)：**
```json
{
    "code": 200,
    "message": "success",
    "data": {
        "applied": false,
        "status": -1,
        "statusText": "未申请"
    }
}
```

**待审核时响应 (200)：**
```json
{
    "code": 200,
    "message": "success",
    "data": {
        "applied": true,
        "status": 0,
        "statusText": "待审核",
        "rejectionReason": null,
        "name": "张记川菜馆",
        "phone": "13900139000"
    }
}
```

---

### 2.3 搜索附近商家（无需登录）

```
GET /api/merchant/nearby?lng=116.461&lat=39.908&radius=5
```

**响应 (200)：**
```json
{
    "code": 200,
    "message": "success",
    "data": [
        {
            "id": 1,
            "name": "张记川菜馆",
            "logo": null,
            "distance": 0.35,
            "monthlySales": 1280,
            "rating": 4.8,
            "deliveryFee": 5.0,
            "deliveryTime": 30,
            "tags": ["川菜", "辣"]
        }
    ]
}
```

---

### 2.4 商家详情（无需登录）

```
GET /api/merchant/{merchantId}
```

**响应 (200)：**
```json
{
    "code": 200,
    "message": "success",
    "data": {
        "id": 1,
        "name": "张记川菜馆",
        "phone": "13900139000",
        "address": "北京市朝阳区建国路88号",
        "description": "正宗川菜，麻辣鲜香，十年老店",
        "businessHours": "09:00-22:00",
        "longitude": 116.461,
        "latitude": 39.908,
        "status": 1
    }
}
```

---

## 3. 管理员审核模块 — AdminController

> 以下接口均需在请求头中携带 `Authorization: Bearer {admin_accessToken}`

### 3.1 骑手审核列表

```
GET /api/admin/rider/audit
GET /api/admin/rider/audit?auditStatus=0    （仅查待审核）
Authorization: Bearer {adminToken}
```

**响应 (200)：**
```json
{
    "code": 200,
    "message": "success",
    "data": [
        {
            "id": 1,
            "openid": "wx_test_rider_001",
            "realName": "李四",
            "idCard": "110101199003071234",
            "phone": "13800138000",
            "avatar": null,
            "auditStatus": 0,
            "rejectionReason": null,
            "status": 4,
            "totalOrders": 0,
            "score": 5.0,
            "createTime": "2026-05-22T10:00:00"
        }
    ]
}
```

---

### 3.2 骑手审核操作

```
POST /api/admin/rider/{id}/audit
Authorization: Bearer {adminToken}
```

**审核通过请求体：**
```json
{
    "auditStatus": 1
}
```

**审核驳回请求体：**
```json
{
    "auditStatus": 2,
    "reason": "身份证号与姓名不匹配，请核实后重新提交"
}
```

**成功响应 (200)：**
```json
{
    "code": 200,
    "message": "success",
    "data": null
}
```

**参数错误响应 (400)：**
```json
{
    "code": 400,
    "message": "auditStatus 必须为 1(通过) 或 2(驳回)",
    "data": null
}
```

---

### 3.3 商家审核列表

```
GET /api/admin/merchant/audit
GET /api/admin/merchant/audit?status=0      （仅查待审核）
Authorization: Bearer {adminToken}
```

**响应 (200)：**
```json
{
    "code": 200,
    "message": "success",
    "data": [
        {
            "id": 1,
            "openid": "wx_test_merchant_001",
            "name": "张记川菜馆",
            "phone": "13900139000",
            "businessLicense": "91110108MA01XXXXX",
            "rejectionReason": null,
            "address": "北京市朝阳区建国路88号",
            "status": 0,
            "createTime": "2026-05-22T10:00:00"
        }
    ]
}
```

---

### 3.4 商家审核操作

```
POST /api/admin/merchant/{id}/audit
Authorization: Bearer {adminToken}
```

**审核通过请求体：**
```json
{
    "status": 1
}
```

**审核驳回请求体：**
```json
{
    "status": 2,
    "reason": "营业执照号查询不到，请核实"
}
```

**成功响应 (200)：**
```json
{
    "code": 200,
    "message": "success",
    "data": null
}
```

---

### 3.5 数据看板

```
GET /api/admin/dashboard
Authorization: Bearer {adminToken}
```

**响应 (200)：**
```json
{
    "code": 200,
    "message": "success",
    "data": {
        "todayOrders": 42,
        "todayRevenue": 3658.50,
        "totalMerchants": 12,
        "totalUsers": 256,
        "onlineRiders": 8,
        "last7Days": [
            {"date": "05-16", "count": 35},
            {"date": "05-17", "count": 38}
        ]
    }
}
```

---

### 3.6 订单监控

```
GET /api/admin/order/monitor?page=1&size=20&status=PREPARING
Authorization: Bearer {adminToken}
```

**响应 (200)：**
```json
{
    "code": 200,
    "message": "success",
    "data": {
        "records": [
            {
                "id": 1,
                "orderNo": "202605221000001",
                "merchantId": 1,
                "merchantName": "张记川菜馆",
                "userId": 2,
                "userName": "张三",
                "riderId": null,
                "status": "PREPARING",
                "totalAmount": 56.0,
                "payAmount": 45.0,
                "address": "朝阳区望京SOHO T1 15F",
                "createTime": "2026-05-22T12:30:00"
            }
        ],
        "total": 1,
        "page": 1,
        "size": 20
    }
}
```

---

### 3.7 AI推荐分析

```
GET /api/admin/recommend/stats
Authorization: Bearer {adminToken}
```

**响应 (200)：**
```json
{
    "code": 200,
    "message": "success",
    "data": {
        "totalDishes": 85,
        "totalCategories": 15,
        "hotDishes": [
            {
                "dishId": 1,
                "name": "麻辣小龙虾",
                "price": 68.0,
                "monthlySales": 520,
                "image": "https://example.com/img1.png",
                "merchantName": "张记川菜馆",
                "categoryName": "海鲜"
            }
        ],
        "categoryDistribution": [
            {"name": "海鲜", "value": 12},
            {"name": "川菜", "value": 20}
        ],
        "priceDistribution": [
            {"name": "实惠 (<¥15)", "value": 25},
            {"name": "适中 (¥15-30)", "value": 40},
            {"name": "品质 (>¥30)", "value": 20}
        ]
    }
}
```

---

## 4. 顾客端 — 浏览与下单

> 以下接口均需在请求头中携带 `Authorization: Bearer {user_accessToken}`

### 4.1 浏览商家菜品分类

```
GET /api/merchant/{merchantId}/categories
```

**响应 (200)：**
```json
{
    "code": 200,
    "message": "success",
    "data": [
        {"id": 1, "name": "热销推荐", "sort": 1},
        {"id": 2, "name": "招牌川菜", "sort": 2},
        {"id": 3, "name": "主食小吃", "sort": 3}
    ]
}
```

---

### 4.2 按分类浏览菜品

```
GET /api/merchant/{merchantId}/dishes/category/{categoryId}
```

**响应 (200)：**
```json
{
    "code": 200,
    "message": "success",
    "data": [
        {
            "id": 1,
            "name": "麻辣小龙虾",
            "price": 68.0,
            "originalPrice": 88.0,
            "image": "https://example.com/img1.png",
            "summary": "新鲜小龙虾，麻辣鲜香",
            "monthlySales": 520,
            "stock": 50,
            "status": 1
        }
    ]
}
```

---

### 4.3 分页浏览全部菜品

```
GET /api/merchant/{merchantId}/dishes?page=1&size=20
```

---

### 4.4 购物车 — 查看

```
GET /api/cart
Authorization: Bearer {userToken}
```

**响应 (200)：**
```json
{
    "code": 200,
    "message": "success",
    "data": {
        "merchantId": 1,
        "merchantName": "张记川菜馆",
        "items": [
            {
                "dishId": 1,
                "dishName": "麻辣小龙虾",
                "dishImage": "https://example.com/img1.png",
                "price": 68.0,
                "quantity": 2
            }
        ],
        "totalAmount": 136.0
    }
}
```

---

### 4.5 购物车 — 添加商品

```
POST /api/cart/add
Authorization: Bearer {userToken}
```

**请求参数 (query string)：**
```
?dishId=1&quantity=2
```

**成功响应 (200)：**
```json
{
    "code": 200,
    "message": "success",
    "data": null
}
```

---

### 4.6 购物车 — 修改数量

```
PUT /api/cart/{dishId}?quantity=3
Authorization: Bearer {userToken}
```

---

### 4.7 购物车 — 删除商品

```
DELETE /api/cart/{dishId}
Authorization: Bearer {userToken}
```

---

### 4.8 购物车 — 清空

```
DELETE /api/cart/clear
Authorization: Bearer {userToken}
```

---

### 4.9 下单

```
POST /api/order/place
Authorization: Bearer {userToken}
```

**请求体：**
```json
{
    "merchantId": 1,
    "address": "朝阳区望京SOHO T1 15F",
    "addressLng": 116.481,
    "addressLat": 39.996,
    "remark": "少放辣，多加葱",
    "items": [
        {"dishId": 1, "quantity": 2},
        {"dishId": 3, "quantity": 1}
    ]
}
```

**成功响应 (200)：**
```json
{
    "code": 200,
    "message": "success",
    "data": {
        "id": 1,
        "orderNo": "202605221000001",
        "status": "PENDING",
        "totalAmount": 156.0,
        "deliveryFee": 5.0,
        "discountAmount": 10.0,
        "payAmount": 151.0,
        "address": "朝阳区望京SOHO T1 15F",
        "remark": "少放辣，多加葱",
        "merchantName": "张记川菜馆",
        "details": [
            {
                "dishId": 1,
                "dishName": "麻辣小龙虾",
                "dishImage": "https://example.com/img1.png",
                "price": 68.0,
                "quantity": 2
            },
            {
                "dishId": 3,
                "dishName": "蛋炒饭",
                "dishImage": "https://example.com/img3.png",
                "price": 15.0,
                "quantity": 1
            }
        ],
        "createTime": "2026-05-22T12:30:00"
    }
}
```

---

### 4.10 支付

```
POST /api/order/{orderId}/pay
Authorization: Bearer {userToken}
```

**响应 (200)：**
```json
{
    "code": 200,
    "message": "success",
    "data": {
        "payNo": "PAY202605221000001",
        "status": "SUCCESS"
    }
}
```

---

### 4.11 取消订单

```
POST /api/order/{orderId}/cancel
Authorization: Bearer {userToken}
```

---

### 4.12 订单详情（按订单号）

```
GET /api/order/{orderNo}
Authorization: Bearer {userToken}
```

---

### 4.13 订单列表

```
GET /api/order/list?page=1&size=10
Authorization: Bearer {userToken}
```

**响应 (200)：**
```json
{
    "code": 200,
    "message": "success",
    "data": {
        "total": 3,
        "records": [
            {
                "id": 1,
                "orderNo": "202605221000001",
                "status": "DELIVERING",
                "totalAmount": 156.0,
                "payAmount": 151.0,
                "merchantName": "张记川菜馆",
                "createTime": "2026-05-22T12:30:00"
            }
        ]
    }
}
```

---

### 4.14 用户信息

```
GET /api/user/info
Authorization: Bearer {userToken}
```

### 4.15 更新用户信息

```
PUT /api/user/info
Authorization: Bearer {userToken}
```

**请求体：**
```json
{
    "nickname": "张三丰",
    "avatar": "https://example.com/new_avatar.png",
    "phone": "13800138000"
}
```

---

## 5. 商家端 — 菜品与订单管理

> 以下接口均需在请求头中携带 `Authorization: Bearer {merchant_accessToken}`

### 5.1 商家信息

```
GET /api/merchant/info
GET /api/merchant/my
Authorization: Bearer {merchantToken}
```

### 5.2 更新商家信息

```
PUT /api/merchant/info
Authorization: Bearer {merchantToken}
```

**请求体：**
```json
{
    "name": "张记川菜馆(旗舰店)",
    "phone": "13900139001",
    "address": "北京市朝阳区建国路99号",
    "description": "正宗川菜，麻辣鲜香，十年老店，新装修",
    "businessHours": "08:00-23:00"
}
```

---

### 5.3 菜品分类管理

**查看分类：**
```
GET /api/merchant/category/list
Authorization: Bearer {merchantToken}
```

**添加分类：**
```
POST /api/merchant/category
Authorization: Bearer {merchantToken}
```

**请求体：**
```json
{
    "name": "新品上市",
    "sort": 10
}
```

**修改分类：**
```
PUT /api/merchant/category/{id}
Authorization: Bearer {merchantToken}
```

**删除分类：**
```
DELETE /api/merchant/category/{id}
Authorization: Bearer {merchantToken}
```

---

### 5.4 菜品管理

**查看菜品列表：**
```
GET /api/merchant/dish/list
GET /api/merchant/dish/list?categoryId=2
Authorization: Bearer {merchantToken}
```

**添加菜品：**
```
POST /api/merchant/dish
Authorization: Bearer {merchantToken}
```

**请求体：**
```json
{
    "name": "水煮牛肉",
    "price": 58.0,
    "originalPrice": 68.0,
    "image": "https://example.com/dish2.png",
    "summary": "麻辣鲜香水煮牛肉",
    "richDescription": "<p>精选牛里脊，配以豆芽、莴笋，麻辣鲜香</p>",
    "stock": 30,
    "sort": 1,
    "categoryId": 2
}
```

**修改菜品：**
```
PUT /api/merchant/dish/{id}
Authorization: Bearer {merchantToken}
```

**删除菜品：**
```
DELETE /api/merchant/dish/{id}
Authorization: Bearer {merchantToken}
```

---

### 5.5 订单管理（商家视角）

**查看订单列表：**
```
GET /api/merchant/order/list?page=1&size=50
Authorization: Bearer {merchantToken}
```

**接单：**
```
POST /api/merchant/order/{orderId}/accept
Authorization: Bearer {merchantToken}
```

**完成备货并触发派单：**
```
POST /api/merchant/order/{orderId}/complete
Authorization: Bearer {merchantToken}
```

**响应 (200)：** 返回分配的骑手 ID：
```json
{
    "code": 200,
    "message": "success",
    "data": 1
}
```

**取消订单：**
```
POST /api/merchant/order/{orderId}/cancel
Authorization: Bearer {merchantToken}
```

---

## 6. 骑手端 — 接单配送

> 以下接口均需在请求头中携带 `Authorization: Bearer {rider_accessToken}`

### 6.1 骑手信息

```
GET /api/rider/info
Authorization: Bearer {riderToken}
```

---

### 6.2 上下线操作

**上线：**
```
POST /api/rider/online
Authorization: Bearer {riderToken}
```

**下线：**
```
POST /api/rider/offline
Authorization: Bearer {riderToken}
```

---

### 6.3 上报位置

```
POST /api/rider/location
Authorization: Bearer {riderToken}
```

**请求体：**
```json
{
    "longitude": 116.472,
    "latitude": 39.991
}
```

---

### 6.4 查看待抢订单

```
GET /api/rider/order/pending
Authorization: Bearer {riderToken}
```

**响应 (200)：**
```json
{
    "code": 200,
    "message": "success",
    "data": [
        {
            "id": 1,
            "orderNo": "202605221000001",
            "merchantId": 1,
            "userId": 2,
            "status": "PREPARING",
            "totalAmount": 156.0,
            "address": "朝阳区望京SOHO T1 15F",
            "createTime": "2026-05-22T12:30:00"
        }
    ]
}
```

---

### 6.5 接单

```
POST /api/rider/order/{orderNo}/accept
Authorization: Bearer {riderToken}
```

---

### 6.6 取货确认

```
POST /api/rider/order/{orderNo}/pickup
Authorization: Bearer {riderToken}
```

---

### 6.7 完成配送

```
POST /api/rider/order/{orderNo}/complete
Authorization: Bearer {riderToken}
```

---

### 6.8 我的配送记录

```
GET /api/rider/order/list?page=1&size=20
Authorization: Bearer {riderToken}
```

---

## 7. AI 美食推荐

> 需要携带任意有效 token（顾客/商家/骑手均可）

### 7.1 AI 对话推荐

```
POST /api/recommend/chat
Authorization: Bearer {anyToken}
```

**请求体：**
```json
{
    "message": "我想吃辣的，预算50以内，有什么推荐？"
}
```

**响应 (200)：**
```json
{
    "code": 200,
    "message": "success",
    "data": {
        "reply": "为您推荐以下美食...",
        "dishes": [
            {
                "id": 1,
                "name": "麻辣小龙虾",
                "price": 68.0,
                "merchantName": "张记川菜馆",
                "reason": "月销520份，顾客评分高"
            }
        ]
    }
}
```

---

## 8. 完整端到端测试流程

### 8.1 顾客端完整流程（curl）

```bash
BASE="http://localhost:8080/api"

# ============ Step 1: 顾客登录 ============
CUSTOMER=$(curl -s -X POST $BASE/auth/login/wechat \
  -H "Content-Type: application/json" \
  -d '{
    "code": "customer_e2e_test",
    "nickname": "测试顾客",
    "avatar": "https://example.com/avatar.png"
  }')

CUSTOMER_TOKEN=$(echo $CUSTOMER | jq -r '.data.accessToken')
CUSTOMER_ID=$(echo $CUSTOMER | jq -r '.data.userId')
echo ">>> 顾客登录成功，userId=$CUSTOMER_ID"

# ============ Step 2: 搜索附近商家 ============
echo ">>> 搜索附近商家..."
curl -s "$BASE/merchant/nearby?lng=116.461&lat=39.908&radius=5" | jq .

# ============ Step 3: 查看商家详情 ============
echo ">>> 查看商家详情..."
curl -s "$BASE/merchant/1" | jq .

# ============ Step 4: 浏览分类 ============
echo ">>> 浏览菜品分类..."
curl -s "$BASE/merchant/1/categories" | jq .

# ============ Step 5: 按分类查看菜品 ============
echo ">>> 查看分类下菜品..."
curl -s "$BASE/merchant/1/dishes/category/2" | jq .

# ============ Step 6: 添加到购物车 ============
echo ">>> 添加商品到购物车..."
curl -s -X POST "$BASE/cart/add?dishId=1&quantity=2" \
  -H "Authorization: Bearer $CUSTOMER_TOKEN"

curl -s -X POST "$BASE/cart/add?dishId=3&quantity=1" \
  -H "Authorization: Bearer $CUSTOMER_TOKEN"

# ============ Step 7: 查看购物车 ============
echo ">>> 查看购物车..."
curl -s "$BASE/cart" \
  -H "Authorization: Bearer $CUSTOMER_TOKEN" | jq .

# ============ Step 8: 下单 ============
echo ">>> 提交订单..."
ORDER=$(curl -s -X POST $BASE/order/place \
  -H "Content-Type: application/json" \
  -H "Authorization: Bearer $CUSTOMER_TOKEN" \
  -d '{
    "merchantId": 1,
    "address": "朝阳区望京SOHO T1 15F",
    "addressLng": 116.481,
    "addressLat": 39.996,
    "remark": "少放辣",
    "items": [
      {"dishId": 1, "quantity": 2},
      {"dishId": 3, "quantity": 1}
    ]
  }')

ORDER_ID=$(echo $ORDER | jq -r '.data.id')
ORDER_NO=$(echo $ORDER | jq -r '.data.orderNo')
echo ">>> 下单成功，orderId=$ORDER_ID, orderNo=$ORDER_NO"

# ============ Step 9: 支付 ============
echo ">>> 支付订单..."
curl -s -X POST "$BASE/order/$ORDER_ID/pay" \
  -H "Authorization: Bearer $CUSTOMER_TOKEN" | jq .

# ============ Step 10: 查看订单详情 ============
echo ">>> 查看订单详情..."
curl -s "$BASE/order/$ORDER_NO" \
  -H "Authorization: Bearer $CUSTOMER_TOKEN" | jq .

# ============ Step 11: 订单列表 ============
echo ">>> 查看订单列表..."
curl -s "$BASE/order/list?page=1&size=10" \
  -H "Authorization: Bearer $CUSTOMER_TOKEN" | jq .

# ============ Step 12: AI 推荐 ============
echo ">>> AI 美食推荐..."
curl -s -X POST $BASE/recommend/chat \
  -H "Content-Type: application/json" \
  -H "Authorization: Bearer $CUSTOMER_TOKEN" \
  -d '{"message": "推荐辣的菜，预算50以内"}' | jq .
```

---

### 8.2 商家端完整流程（curl）

```bash
BASE="http://localhost:8080/api"

# ============ Step 1: 商家入驻申请 ============
echo ">>> 提交入驻申请..."
APPLY_RESP=$(curl -s -X POST $BASE/merchant/apply \
  -H "Content-Type: application/json" \
  -d '{
    "code": "merchant_e2e_test",
    "name": "测试餐厅",
    "businessLicense": "91110108MA01TEST1",
    "phone": "13600136000",
    "address": "测试地址100号",
    "longitude": 116.40,
    "latitude": 39.90,
    "description": "测试店铺"
  }')

MERCHANT_ID=$(echo $APPLY_RESP | jq -r '.data')
echo ">>> 入驻成功，merchantId=$MERCHANT_ID"

# ============ Step 2: 查询入驻进度 ============
echo ">>> 查询入驻进度..."
curl -s "$BASE/merchant/apply/status?code=merchant_e2e_test" | jq .

# ============ Step 3: 管理员登录并审核通过 ============
ADMIN_TOKEN=$(curl -s -X POST $BASE/auth/login/admin \
  -H "Content-Type: application/json" \
  -d '{"username":"admin","password":"admin123"}' | jq -r '.data.accessToken')

echo ">>> 管理员审核通过商家(merchantId=$MERCHANT_ID)..."
curl -s -X POST "$BASE/admin/merchant/$MERCHANT_ID/audit" \
  -H "Content-Type: application/json" \
  -H "Authorization: Bearer $ADMIN_TOKEN" \
  -d '{"status": 1}' | jq .

# ============ Step 4: 商家登录 ============
MERCHANT=$(curl -s -X POST $BASE/auth/login/merchant/wechat \
  -H "Content-Type: application/json" \
  -d '{
    "code": "merchant_e2e_test",
    "nickname": "测试餐厅"
  }')

MERCHANT_TOKEN=$(echo $MERCHANT | jq -r '.data.accessToken')
echo ">>> 商家登录成功"

# ============ Step 5: 查看商家信息 ============
echo ">>> 查看商家信息..."
curl -s "$BASE/merchant/info" \
  -H "Authorization: Bearer $MERCHANT_TOKEN" | jq .

# ============ Step 6: 添加分类 ============
echo ">>> 添加菜品分类..."
CAT=$(curl -s -X POST "$BASE/merchant/category" \
  -H "Content-Type: application/json" \
  -H "Authorization: Bearer $MERCHANT_TOKEN" \
  -d '{"name": "招牌菜", "sort": 1}')
CAT_ID=$(echo $CAT | jq -r '.data.id')
echo "  分类ID=$CAT_ID"

# ============ Step 7: 添加菜品 ============
echo ">>> 添加菜品..."
curl -s -X POST "$BASE/merchant/dish" \
  -H "Content-Type: application/json" \
  -H "Authorization: Bearer $MERCHANT_TOKEN" \
  -d "{
    \"name\": \"水煮牛肉\",
    \"price\": 58.0,
    \"originalPrice\": 68.0,
    \"image\": \"https://example.com/dish.png\",
    \"summary\": \"麻辣鲜香水煮牛肉\",
    \"stock\": 30,
    \"sort\": 1,
    \"categoryId\": $CAT_ID
  }" | jq .

# ============ Step 8: 查看菜品列表 ============
echo ">>> 查看菜品列表..."
curl -s "$BASE/merchant/dish/list" \
  -H "Authorization: Bearer $MERCHANT_TOKEN" | jq .

# ============ Step 9: 影子顾客下单（为接单测试准备订单） ============
echo ">>> 影子顾客下单..."

SHADOW_TOKEN=$(curl -s -X POST $BASE/auth/login/wechat \
  -H "Content-Type: application/json" \
  -d '{"code":"shadow_customer_82","nickname":"影子顾客"}' \
  | jq -r '.data.accessToken')

# 加购
curl -s -X POST "$BASE/cart/add?dishId=1&quantity=2" \
  -H "Authorization: Bearer $SHADOW_TOKEN" > /dev/null
curl -s -X POST "$BASE/cart/add?dishId=3&quantity=1" \
  -H "Authorization: Bearer $SHADOW_TOKEN" > /dev/null

# 下单
ORDER_RESP=$(curl -s -X POST $BASE/order/place \
  -H "Content-Type: application/json" \
  -H "Authorization: Bearer $SHADOW_TOKEN" \
  -d "{
    \"merchantId\": $MERCHANT_ID,
    \"address\": \"测试地址200号\",
    \"addressLng\": 116.40,
    \"addressLat\": 39.90,
    \"remark\": \"自动化测试\",
    \"items\": [
      {\"dishId\": 1, \"quantity\": 2},
      {\"dishId\": 3, \"quantity\": 1}
    ]
  }")

ORDER_ID=$(echo $ORDER_RESP | jq -r '.data.id')
ORDER_NO=$(echo $ORDER_RESP | jq -r '.data.orderNo')
echo "  下单成功 orderId=$ORDER_ID orderNo=$ORDER_NO"

# 支付
curl -s -X POST "$BASE/order/$ORDER_ID/pay" \
  -H "Authorization: Bearer $SHADOW_TOKEN" | jq .
echo "  支付成功"

# ============ Step 10: 商家查看订单列表 ============
echo ">>> 查看订单列表..."
curl -s "$BASE/merchant/order/list" \
  -H "Authorization: Bearer $MERCHANT_TOKEN" | jq .

# ============ Step 11: 接单 ============
echo ">>> 商家接单(orderId=$ORDER_ID)..."
curl -s -X POST "$BASE/merchant/order/$ORDER_ID/accept" \
  -H "Authorization: Bearer $MERCHANT_TOKEN" | jq .

# ============ Step 12: 完成备货（触发派单） ============
echo ">>> 完成备货，触发骑手派单..."
RIDER_ID=$(curl -s -X POST "$BASE/merchant/order/$ORDER_ID/complete" \
  -H "Authorization: Bearer $MERCHANT_TOKEN" | jq -r '.data')
echo "  分配骑手ID=$RIDER_ID"
```

---

### 8.3 骑手端完整流程（curl）

```bash
BASE="http://localhost:8080/api"

# ============ Step 1: 骑手注册 ============
echo ">>> 骑手注册..."
curl -s -X POST $BASE/auth/register/rider \
  -H "Content-Type: application/json" \
  -d '{
    "code": "rider_e2e_test",
    "nickname": "测试骑手",
    "realName": "王五",
    "idCard": "110101199508150012",
    "phone": "13700137000"
  }' | jq .

# ============ Step 2: 查询审核进度 ============
echo ">>> 查询审核进度..."
curl -s "$BASE/auth/register/rider/status?code=rider_e2e_test" | jq .

# ============ Step 3: 管理员登录并审核通过 ============
ADMIN_TOKEN=$(curl -s -X POST $BASE/auth/login/admin \
  -H "Content-Type: application/json" \
  -d '{"username":"admin","password":"admin123"}' | jq -r '.data.accessToken')

echo ">>> 管理员审核通过骑手..."
curl -s -X POST "$BASE/admin/rider/1/audit" \
  -H "Content-Type: application/json" \
  -H "Authorization: Bearer $ADMIN_TOKEN" \
  -d '{"auditStatus": 1}' | jq .

# ============ Step 4: 骑手登录 ============
RIDER=$(curl -s -X POST $BASE/auth/login/rider/wechat \
  -H "Content-Type: application/json" \
  -d '{
    "code": "rider_e2e_test",
    "nickname": "测试骑手"
  }')

RIDER_TOKEN=$(echo $RIDER | jq -r '.data.accessToken')
echo ">>> 骑手登录成功"

# ============ Step 5: 骑手上线 ============
echo ">>> 骑手上线..."
curl -s -X POST "$BASE/rider/online" \
  -H "Authorization: Bearer $RIDER_TOKEN" | jq .

# ============ Step 6: 上报位置 ============
echo ">>> 上报位置..."
curl -s -X POST "$BASE/rider/location" \
  -H "Content-Type: application/json" \
  -H "Authorization: Bearer $RIDER_TOKEN" \
  -d '{"longitude": 116.472, "latitude": 39.991}' | jq .

# ============ Step 7: 查看待抢订单 ============
echo ">>> 查看待抢订单..."
curl -s "$BASE/rider/order/pending" \
  -H "Authorization: Bearer $RIDER_TOKEN" | jq .

# ============ Step 8: 接单（用订单号） ============
echo ">>> 骑手接单..."
curl -s -X POST "$BASE/rider/order/202605221000001/accept" \
  -H "Authorization: Bearer $RIDER_TOKEN" | jq .

# ============ Step 9: 取货确认 ============
echo ">>> 骑手取货..."
curl -s -X POST "$BASE/rider/order/202605221000001/pickup" \
  -H "Authorization: Bearer $RIDER_TOKEN" | jq .

# ============ Step 10: 完成配送 ============
echo ">>> 完成配送..."
curl -s -X POST "$BASE/rider/order/202605221000001/complete" \
  -H "Authorization: Bearer $RIDER_TOKEN" | jq .

# ============ Step 11: 查看配送记录 ============
echo ">>> 查看配送记录..."
curl -s "$BASE/rider/order/list?page=1&size=20" \
  -H "Authorization: Bearer $RIDER_TOKEN" | jq .

# ============ Step 12: 骑手下线 ============
echo ">>> 骑手下线..."
curl -s -X POST "$BASE/rider/offline" \
  -H "Authorization: Bearer $RIDER_TOKEN" | jq .
```

---

### 8.4 三端联动完整流程（curl）

> 按顺序执行，模拟真实外卖场景中的三方协作：

```bash
BASE="http://localhost:8080/api"

echo "=========================================="
echo "  三端联动 E2E 测试"
echo "=========================================="

# ──────────────────────────────────────────
# 阶段 0: 管理员登录（用于审核）
# ──────────────────────────────────────────
ADMIN_TOKEN=$(curl -s -X POST $BASE/auth/login/admin \
  -H "Content-Type: application/json" \
  -d '{"username":"admin","password":"admin123"}' | jq -r '.data.accessToken')
echo "[0] 管理员登录完成"

# ──────────────────────────────────────────
# 阶段 A: 商家入驻 + 审核 + 上架菜品
# ──────────────────────────────────────────
echo ""
echo "===== 阶段 A: 商家准备 ====="

# A1: 商家申请入驻
APPLY_RESP=$(curl -s -X POST $BASE/merchant/apply \
  -H "Content-Type: application/json" \
  -d '{
    "code": "merchant_joint_test",
    "name": "联合测试餐厅",
    "businessLicense": "91110108MA01JOINT",
    "phone": "13500135000",
    "address": "北京市西城区金融街1号",
    "longitude": 116.361,
    "latitude": 39.913,
    "description": "三端联合测试店铺"
  }')
MERCHANT_ID=$(echo $APPLY_RESP | jq -r '.data')
echo "  入驻成功 merchantId=$MERCHANT_ID"

# A2: 管理员审核通过商家
curl -s -X POST "$BASE/admin/merchant/$MERCHANT_ID/audit" \
  -H "Content-Type: application/json" \
  -H "Authorization: Bearer $ADMIN_TOKEN" \
  -d '{"status": 1}' | jq .

# A3: 商家登录
MERCHANT_TOKEN=$(curl -s -X POST $BASE/auth/login/merchant/wechat \
  -H "Content-Type: application/json" \
  -d '{"code":"merchant_joint_test","nickname":"联合测试餐厅"}' \
  | jq -r '.data.accessToken')

# A4: 商家添加分类
CAT_ID=$(curl -s -X POST "$BASE/merchant/category" \
  -H "Content-Type: application/json" \
  -H "Authorization: Bearer $MERCHANT_TOKEN" \
  -d '{"name":"主食","sort":1}' | jq -r '.data.id')

# A5: 商家添加菜品
curl -s -X POST "$BASE/merchant/dish" \
  -H "Content-Type: application/json" \
  -H "Authorization: Bearer $MERCHANT_TOKEN" \
  -d "{\"name\":\"宫保鸡丁套餐\",\"price\":32.0,\"originalPrice\":38.0,\"image\":\"https://example.com/gbjd.png\",\"summary\":\"经典川味宫保鸡丁+米饭\",\"stock\":50,\"sort\":1,\"categoryId\":$CAT_ID}" | jq .

curl -s -X POST "$BASE/merchant/dish" \
  -H "Content-Type: application/json" \
  -H "Authorization: Bearer $MERCHANT_TOKEN" \
  -d "{\"name\":\"鱼香肉丝盖饭\",\"price\":28.0,\"originalPrice\":32.0,\"image\":\"https://example.com/yxrs.png\",\"summary\":\"酸甜鱼香肉丝+米饭\",\"stock\":40,\"sort\":2,\"categoryId\":$CAT_ID}" | jq .

echo "[A] 商家准备完成 — 已有菜品上架"

# ──────────────────────────────────────────
# 阶段 B: 骑手注册 + 审核 + 上线
# ──────────────────────────────────────────
echo ""
echo "===== 阶段 B: 骑手准备 ====="

# B1: 骑手注册
curl -s -X POST $BASE/auth/register/rider \
  -H "Content-Type: application/json" \
  -d '{
    "code": "rider_joint_test",
    "nickname": "联合测试骑手",
    "realName": "赵六",
    "idCard": "310115198802169013",
    "phone": "13700137001"
  }' | jq .

# B2: 管理员审核通过骑手
RIDER_MERCHANT_ID=$(curl -s "$BASE/admin/rider/audit?auditStatus=0" \
  -H "Authorization: Bearer $ADMIN_TOKEN" | jq -r '.data[0].id')
curl -s -X POST "$BASE/admin/rider/$RIDER_MERCHANT_ID/audit" \
  -H "Content-Type: application/json" \
  -H "Authorization: Bearer $ADMIN_TOKEN" \
  -d '{"auditStatus": 1}' | jq .

# B3: 骑手登录
RIDER_TOKEN=$(curl -s -X POST $BASE/auth/login/rider/wechat \
  -H "Content-Type: application/json" \
  -d '{"code":"rider_joint_test","nickname":"赵六"}' \
  | jq -r '.data.accessToken')

# B4: 骑手上线 + 上报位置
curl -s -X POST "$BASE/rider/online" \
  -H "Authorization: Bearer $RIDER_TOKEN" | jq .

curl -s -X POST "$BASE/rider/location" \
  -H "Content-Type: application/json" \
  -H "Authorization: Bearer $RIDER_TOKEN" \
  -d '{"longitude": 116.365, "latitude": 39.915}' | jq .

echo "[B] 骑手准备完成 — 已上线待接单"

# ──────────────────────────────────────────
# 阶段 C: 顾客下单 + 支付
# ──────────────────────────────────────────
echo ""
echo "===== 阶段 C: 顾客下单 ====="

# C1: 顾客登录
CUSTOMER_TOKEN=$(curl -s -X POST $BASE/auth/login/wechat \
  -H "Content-Type: application/json" \
  -d '{"code":"customer_joint_test","nickname":"测试顾客"}' \
  | jq -r '.data.accessToken')

# C2: 浏览附近商家
echo "  附近商家:"
curl -s "$BASE/merchant/nearby?lng=116.361&lat=39.913&radius=5" \
  | jq '.data[] | {id, name, distance}'

# C3: 加购
curl -s -X POST "$BASE/cart/add?dishId=1&quantity=2" \
  -H "Authorization: Bearer $CUSTOMER_TOKEN" > /dev/null
curl -s -X POST "$BASE/cart/add?dishId=2&quantity=1" \
  -H "Authorization: Bearer $CUSTOMER_TOKEN" > /dev/null
echo "  已添加 2 个菜品到购物车"

# C4: 下单
ORDER_RESP=$(curl -s -X POST $BASE/order/place \
  -H "Content-Type: application/json" \
  -H "Authorization: Bearer $CUSTOMER_TOKEN" \
  -d '{
    "merchantId": 1,
    "address": "北京市西城区金融街10号",
    "addressLng": 116.368,
    "addressLat": 39.916,
    "remark": "",
    "items": [
      {"dishId": 1, "quantity": 2},
      {"dishId": 2, "quantity": 1}
    ]
  }')
ORDER_ID=$(echo $ORDER_RESP | jq -r '.data.id')
ORDER_NO=$(echo $ORDER_RESP | jq -r '.data.orderNo')
echo "  下单成功 orderId=$ORDER_ID orderNo=$ORDER_NO"

# C5: 支付
curl -s -X POST "$BASE/order/$ORDER_ID/pay" \
  -H "Authorization: Bearer $CUSTOMER_TOKEN" | jq .
echo "  支付成功"

echo "[C] 顾客下单完成"

# ──────────────────────────────────────────
# 阶段 D: 商家接单 + 备货 + 派单
# ──────────────────────────────────────────
echo ""
echo "===== 阶段 D: 商家处理 ====="

# D1: 商家接单
curl -s -X POST "$BASE/merchant/order/$ORDER_ID/accept" \
  -H "Authorization: Bearer $MERCHANT_TOKEN" | jq .
echo "  商家已接单"

# D2: 商家完成备货，触发骑手派单
RIDER_RESULT=$(curl -s -X POST "$BASE/merchant/order/$ORDER_ID/complete" \
  -H "Authorization: Bearer $MERCHANT_TOKEN")
echo "  商家备货完成，派单结果: $RIDER_RESULT"

echo "[D] 商家处理完成"

# ──────────────────────────────────────────
# 阶段 E: 骑手接单 + 配送完成
# ──────────────────────────────────────────
echo ""
echo "===== 阶段 E: 骑手配送 ====="

# E1: 骑手接单
curl -s -X POST "$BASE/rider/order/$ORDER_NO/accept" \
  -H "Authorization: Bearer $RIDER_TOKEN" | jq .
echo "  骑手已接单"

# E2: 骑手取货
curl -s -X POST "$BASE/rider/order/$ORDER_NO/pickup" \
  -H "Authorization: Bearer $RIDER_TOKEN" | jq .
echo "  骑手已取货"

# E3: 骑手完成配送
curl -s -X POST "$BASE/rider/order/$ORDER_NO/complete" \
  -H "Authorization: Bearer $RIDER_TOKEN" | jq .
echo "  骑手配送完成"

echo "[E] 配送完成"

# ──────────────────────────────────────────
# 阶段 F: 顾客查看订单状态
# ──────────────────────────────────────────
echo ""
echo "===== 阶段 F: 订单确认 ====="
curl -s "$BASE/order/$ORDER_NO" \
  -H "Authorization: Bearer $CUSTOMER_TOKEN" | jq '{orderNo, status, merchantName, payAmount}'

echo ""
echo "=========================================="
echo "  三端联动 E2E 测试完成！"
echo "=========================================="
```

---

## 9. 状态码对照表

### 骑手 auditStatus
| 值 | 含义 |
|----|------|
| 0 | 待审核 |
| 1 | 审核通过 |
| 2 | 驳回 |

### 骑手 status（操作状态，auditStatus=1 时有效）
| 值 | 含义 |
|----|------|
| 3 | 在线 |
| 4 | 离线 |
| 5 | 禁用 |

### 商家 status
| 值 | 含义 |
|----|------|
| 0 | 待审核 |
| 1 | 审核通过（营业中） |
| 2 | 审核拒绝 |
| 3 | 已停用 |

### 订单 status
| 值 | 含义 |
|----|------|
| PENDING | 待支付 |
| PREPARING | 备货中 |
| DELIVERING | 配送中 |
| COMPLETED | 已完成 |
| CANCELLED | 已取消 |

---

## 10. 接口认证速查表

| 模块 | 端点 | 需要 Token | 角色 |
|------|------|-----------|------|
| 认证 | /api/auth/** | 否 | - |
| 商家入驻 | /api/merchant/apply, /apply/status | 否 | - |
| 附近商家 | /api/merchant/nearby | 否 | - |
| 商家详情 | /api/merchant/{id} | 否 | - |
| 菜品浏览 | /api/merchant/{id}/dishes, /categories | 否 | - |
| 购物车 | /api/cart/** | 是 | 顾客 |
| 下单支付 | /api/order/** | 是 | 顾客 |
| AI推荐 | /api/recommend/chat | 是 | 任意 |
| 用户信息 | /api/user/** | 是 | 顾客 |
| 商家管理 | /api/merchant/info, /my, /dish, /category, /order | 是 | 商家 |
| 骑手操作 | /api/rider/** | 是 | 骑手 |
| 管理后台 | /api/admin/** | 是 | 管理员 |

---

## 11. 身份证号校验规则

`IdCardUtil.isValid(idCard)` 校验逻辑：
1. 必须是18位（末位可以是 X）
2. 前17位必须是数字
3. 校验码验证（加权求和 mod 11）

**可通过校验的测试身份证号：**
- `110101199003071234`
- `110101199508150012`
- `310115198802169013`

**不通过的示例：**
- `123456789012345678` — 校验码不对
- `11010119900307123` — 只有17位

---

## 12. Apifox 自动化测试配置

> 前提：后端已启动，Apifox 已通过 `http://localhost:8080/v3/api-docs` 导入接口文档。

### 12.1 后端代码说明

`POST /api/merchant/apply` 已改造为返回新商家 ID：

```json
{ "code": 200, "message": "success", "data": 1792000001 }
```

MyBatis-Plus 的 `save()` 自动将雪花算法生成的 ID 回填到 entity，Controller 直接 `Result.ok(merchant.getId())` 即可。

### 12.2 变量动态传递：MERCHANT_ID

**目标：** 第一步申请入驻 → 自动提取商家 ID → 第四步审核接口自动引用。

**操作步骤：**

1. 在 Apifox 中打开 `POST /api/merchant/apply` 接口
2. 点击 **「后置操作」** 标签页
3. 点击 **「添加后置操作」** → 选择 **「提取变量」**
4. 配置如下：

| 配置项 | 值 |
|--------|-----|
| 变量名 | `MERCHANT_ID` |
| 提取来源 | Response JSON |
| JSONPath | `$.data` |

5. 打开 `POST /api/admin/merchant/{id}/audit` 接口
6. 在 Path 参数 `id` 的值中填入 `{{MERCHANT_ID}}`

**验证：** 运行测试后，控制台会打印 `MERCHANT_ID` 的实际值，审核接口的请求 URL 会自动替换为 `.../admin/merchant/1792000001/audit`。

### 12.3 影子顾客脚本：自动下单

**场景：** 商家端测试流程第 10 步"商家接单"前，必须存在一笔待处理订单。此脚本自动模拟顾客登录 + 下单 + 提取 ORDER_ID。

**配置位置：** 打开 `POST /api/merchant/order/{orderId}/accept` → **「前置脚本」** 标签页

**完整脚本：**

```javascript
// ============================================================
// 影子顾客脚本 — 自动登录 → 加购 → 下单 → 提取订单ID
// 配置位置：POST /api/merchant/order/{orderId}/accept 的「前置脚本」
// ============================================================

const BASE = "http://localhost:8080/api";

// ---------- Step 1: 顾客登录 ----------
const loginResp = pm.request({
    method: "POST",
    url: BASE + "/auth/login/wechat",
    header: { "Content-Type": "application/json" },
    body: JSON.stringify({
        code: "shadow_customer_" + Date.now(),
        nickname: "影子顾客(Apifox)",
        avatar: "https://example.com/shadow.png"
    })
});
const loginData = loginResp.json().data;
const customerToken = loginData.accessToken;
console.log("[影子顾客] 登录成功 userId=" + loginData.userId);

// ---------- Step 2: 加购 ----------
pm.request({
    method: "POST",
    url: BASE + "/cart/add?dishId=1&quantity=2",
    header: { "Authorization": "Bearer " + customerToken }
});
pm.request({
    method: "POST",
    url: BASE + "/cart/add?dishId=3&quantity=1",
    header: { "Authorization": "Bearer " + customerToken }
});
console.log("[影子顾客] 已加购菜品");

// ---------- Step 3: 下单 ----------
const orderResp = pm.request({
    method: "POST",
    url: BASE + "/order/place",
    header: {
        "Content-Type": "application/json",
        "Authorization": "Bearer " + customerToken
    },
    body: JSON.stringify({
        merchantId: pm.variables.get("MERCHANT_ID"),
        address: "北京市朝阳区望京SOHO T1 15F",
        addressLng: 116.481,
        addressLat: 39.996,
        remark: "Apifox自动化测试订单",
        items: [
            { dishId: 1, quantity: 2 },
            { dishId: 3, quantity: 1 }
        ]
    })
});
const orderData = orderResp.json().data;
const orderId = orderData.id;
const orderNo = orderData.orderNo;
console.log("[影子顾客] 下单成功 orderId=" + orderId + " orderNo=" + orderNo);

// ---------- Step 4: 支付 ----------
pm.request({
    method: "POST",
    url: BASE + "/order/" + orderId + "/pay",
    header: { "Authorization": "Bearer " + customerToken }
});
console.log("[影子顾客] 支付成功");

// ---------- Step 5: 存入环境变量 ----------
pm.environment.set("ORDER_ID", orderId);
pm.environment.set("ORDER_NO", orderNo);
console.log("[影子顾客] ORDER_ID=" + orderId + " 已存入环境变量");
```

**引用变量：**
- `POST /api/merchant/order/{orderId}/accept` 的 Path 参数 `orderId` 设为 `{{ORDER_ID}}`
- `POST /api/merchant/order/{orderId}/complete` 的 Path 参数同样设为 `{{ORDER_ID}}`
- `POST /api/rider/order/{orderNo}/accept` 的 Path 参数设为 `{{ORDER_NO}}`

### 12.4 完整测试用例编排

在 Apifox 中按以下顺序创建测试用例，每个步骤绑定对应接口和前后置操作：

| 步骤 | 接口 | Path 参数 | 关键配置 |
|------|------|-----------|---------|
| 0. 管理员登录 | `POST /api/auth/login/admin` | — | **后置操作**: 提取 `ADMIN_TOKEN` ← `$.data.accessToken` |
| 1. 商家入驻 | `POST /api/merchant/apply` | — | **后置操作**: 提取 `MERCHANT_ID` ← `$.data` |
| 2. 查询入驻进度 | `GET /api/merchant/apply/status` | `code=merchant_test` | — |
| 3. 管理员审核商家 | `POST /api/admin/merchant/{id}/audit` | `id={{MERCHANT_ID}}` | Header: `Authorization: Bearer {{ADMIN_TOKEN}}` |
| 4. 商家登录 | `POST /api/auth/login/merchant/wechat` | — | **后置操作**: 提取 `MERCHANT_TOKEN` ← `$.data.accessToken` |
| 5. 添加分类 | `POST /api/merchant/category` | — | **后置操作**: 提取 `CAT_ID` ← `$.data.id` |
| 6. 添加菜品 | `POST /api/merchant/dish` | — | `categoryId` 引用 `{{CAT_ID}}` |
| 7. 骑手注册 | `POST /api/auth/register/rider` | — | 需在 admin 审核列表查 ID |
| 8. 管理员审核骑手 | `POST /api/admin/rider/{id}/audit` | `id={{RIDER_ID}}` | — |
| 9. 骑手上线 | `POST /api/rider/online` | — | Header: `Bearer {{RIDER_TOKEN}}` |
| 10. **商家接单** | `POST /api/merchant/order/{orderId}/accept` | `orderId={{ORDER_ID}}` | **前置脚本**: 影子顾客脚本（见 12.3） |
| 11. 商家完成备货 | `POST /api/merchant/order/{orderId}/complete` | `orderId={{ORDER_ID}}` | — |
| 12. 骑手接单 | `POST /api/rider/order/{orderNo}/accept` | `orderNo={{ORDER_NO}}` | — |
| 13. 骑手取货 | `POST /api/rider/order/{orderNo}/pickup` | `orderNo={{ORDER_NO}}` | — |
| 14. 骑手完成配送 | `POST /api/rider/order/{orderNo}/complete` | `orderNo={{ORDER_NO}}` | — |

### 12.5 环境变量一览

在 Apifox 的 **「环境管理」** 中预设以下变量：

| 变量名 | 初始值 | 说明 |
|--------|--------|------|
| `BASE_URL` | `http://localhost:8080/api` | 后端基址 |
| `ADMIN_TOKEN` | (空) | 管理员登录后自动填入 |
| `MERCHANT_ID` | (空) | 商家入驻后自动填入 |
| `MERCHANT_TOKEN` | (空) | 商家登录后自动填入 |
| `RIDER_TOKEN` | (空) | 骑手登录后自动填入 |
| `RIDER_ID` | (空) | 骑手审核列表查出后填入 |
| `CAT_ID` | (空) | 分类创建后自动填入 |
| `ORDER_ID` | (空) | 影子顾客下单后自动填入 |
| `ORDER_NO` | (空) | 影子顾客下单后自动填入 |

### 12.6 注意事项

- **影子顾客脚本**必须在"商家接单"步骤执行**之前**运行，因为该步骤需要 `ORDER_ID` 变量。
- 如果数据库已有该 `code` 对应的记录（例如重复入驻），`apply` 接口会返回 500。首次运行时用不同的 `code` 值，或者先清理数据库。
- 骑手审核步骤需要先查 `GET /api/admin/rider/audit?auditStatus=0` 获取待审核骑手列表，再从中提取 `id` 赋值给 `RIDER_ID`。
- Apifox 的 `pm.environment.set()` 会将变量持久化到当前环境，后续步骤可直接用 `{{变量名}}` 引用。
- 所有需要认证的接口必须在 Header 中配置 `Authorization: Bearer {{TOKEN变量}}`。
