-- ============================================================
-- Waimai Delivery Platform — Database Schema
-- Version: 1.0.0
-- Engine: MySQL 8.x / InnoDB
-- ============================================================

CREATE DATABASE IF NOT EXISTS waimai DEFAULT CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
USE waimai;

-- ============================================================
-- 1. 顾客表 (user)
-- 微信小程序登录，openid 唯一标识
-- ============================================================
CREATE TABLE `user` (
    `id`            BIGINT       NOT NULL AUTO_INCREMENT COMMENT '用户ID',
    `openid`        VARCHAR(64)  NOT NULL COMMENT '微信openid',
    `nickname`      VARCHAR(64)  DEFAULT NULL COMMENT '昵称',
    `phone`         VARCHAR(20)  DEFAULT NULL COMMENT '手机号',
    `avatar`        VARCHAR(512) DEFAULT NULL COMMENT '头像URL',
    `gender`        TINYINT      DEFAULT 0 COMMENT '性别: 0未知 1男 2女',
    `status`        TINYINT      DEFAULT 1 COMMENT '状态: 0禁用 1正常',
    `create_time`   DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '注册时间',
    `update_time`   DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_openid` (`openid`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='顾客表';

-- ============================================================
-- 2. 商家表 (merchant)
-- 商家入驻需审核，status 控制可用状态
-- ============================================================
CREATE TABLE `merchant` (
    `id`              BIGINT       NOT NULL AUTO_INCREMENT COMMENT '商家ID',
    `openid`          VARCHAR(64)  NOT NULL COMMENT '微信openid(商家端)',
    `name`            VARCHAR(100) NOT NULL COMMENT '店铺名称',
    `logo`            VARCHAR(512) DEFAULT NULL COMMENT '店铺logo URL',
    `banner`          VARCHAR(512) DEFAULT NULL COMMENT '店铺横幅图',
    `description`     VARCHAR(512) DEFAULT NULL COMMENT '店铺简介',
    `phone`             VARCHAR(20)  DEFAULT NULL COMMENT '联系电话',
    `business_license`  VARCHAR(64)  DEFAULT NULL COMMENT '营业执照号',
    `rejection_reason`  VARCHAR(256) DEFAULT NULL COMMENT '审核驳回原因',
    `address`           VARCHAR(256) DEFAULT NULL COMMENT '店铺地址',
    `longitude`       DECIMAL(10,7) DEFAULT NULL COMMENT '经度',
    `latitude`        DECIMAL(10,7) DEFAULT NULL COMMENT '纬度',
    `business_hours`  VARCHAR(64)  DEFAULT '09:00-22:00' COMMENT '营业时间',
    `min_delivery`    DECIMAL(10,2) DEFAULT 0.00 COMMENT '起送价',
    `delivery_fee`    DECIMAL(10,2) DEFAULT 0.00 COMMENT '配送费',
    `avg_delivery_time` INT         DEFAULT 30 COMMENT '平均配送时间/分钟',
    `monthly_sales`   INT          DEFAULT 0 COMMENT '月销量',
    `score`           DECIMAL(2,1) DEFAULT 5.0 COMMENT '评分(1-5)',
    `status`          TINYINT      DEFAULT 0 COMMENT '状态: 0待审核 1审核通过 2审核拒绝 3停用',
    `create_time`     DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '入驻时间',
    `update_time`     DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_openid` (`openid`),
    KEY `idx_status` (`status`),
    KEY `idx_location` (`longitude`, `latitude`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='商家表';

-- ============================================================
-- 3. 骑手表 (rider)
-- 骑手需实名认证，审核通过后上线
-- ============================================================
CREATE TABLE `rider` (
    `id`            BIGINT       NOT NULL AUTO_INCREMENT COMMENT '骑手ID',
    `openid`        VARCHAR(64)  NOT NULL COMMENT '微信openid(骑手端)',
    `audit_status`      TINYINT      NOT NULL DEFAULT 0 COMMENT '审核状态: 0待审核 1审核通过 2驳回',
    `rejection_reason`  VARCHAR(256) DEFAULT NULL COMMENT '审核驳回原因',
    `real_name`         VARCHAR(32)  NOT NULL COMMENT '真实姓名',
    `id_card`       VARCHAR(18)  NOT NULL COMMENT '身份证号',
    `phone`         VARCHAR(20)  NOT NULL COMMENT '手机号',
    `avatar`        VARCHAR(512) DEFAULT NULL COMMENT '头像URL',
    `current_lng`   DECIMAL(10,7) DEFAULT NULL COMMENT '当前位置经度',
    `current_lat`   DECIMAL(10,7) DEFAULT NULL COMMENT '当前位置纬度',
    `status`        TINYINT      DEFAULT 4 COMMENT '操作状态: 3在线 4离线 5禁用',
    `total_orders`  INT          DEFAULT 0 COMMENT '完成订单数',
    `score`         DECIMAL(2,1) DEFAULT 5.0 COMMENT '评分(1-5)',
    `create_time`   DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '注册时间',
    `update_time`   DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_openid` (`openid`),
    KEY `idx_audit_status` (`audit_status`),
    KEY `idx_status` (`status`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='骑手表';

-- ============================================================
-- 4. 菜品分类表 (category)
-- 每个商家自定义菜品分类
-- ============================================================
CREATE TABLE `category` (
    `id`          BIGINT      NOT NULL AUTO_INCREMENT COMMENT '分类ID',
    `merchant_id` BIGINT      NOT NULL COMMENT '商家ID',
    `name`        VARCHAR(64) NOT NULL COMMENT '分类名称',
    `sort`        INT         DEFAULT 0 COMMENT '排序(升序)',
    `create_time` DATETIME    NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `update_time` DATETIME    NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    PRIMARY KEY (`id`),
    KEY `idx_merchant` (`merchant_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='菜品分类表';

-- ============================================================
-- 5. 菜品表 (dish)
-- 含富文本描述字段，支持上下架操作
-- ============================================================
CREATE TABLE `dish` (
    `id`               BIGINT        NOT NULL AUTO_INCREMENT COMMENT '菜品ID',
    `merchant_id`      BIGINT        NOT NULL COMMENT '商家ID',
    `category_id`      BIGINT        DEFAULT NULL COMMENT '分类ID',
    `name`             VARCHAR(128)  NOT NULL COMMENT '菜品名称',
    `image`            VARCHAR(512)  DEFAULT NULL COMMENT '菜品图片URL',
    `price`            DECIMAL(10,2) NOT NULL COMMENT '价格',
    `original_price`   DECIMAL(10,2) DEFAULT NULL COMMENT '原价(划线价)',
    `rich_description` TEXT          DEFAULT NULL COMMENT '富文本描述(含图片)',
    `summary`          VARCHAR(256)  DEFAULT NULL COMMENT '简介(纯文本)',
    `stock`            INT           DEFAULT 0 COMMENT '当前库存',
    `monthly_sales`    INT           DEFAULT 0 COMMENT '月销量',
    `sort`             INT           DEFAULT 0 COMMENT '排序(升序)',
    `status`           TINYINT       DEFAULT 1 COMMENT '状态: 0下架 1上架',
    `create_time`      DATETIME      NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `update_time`      DATETIME      NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    PRIMARY KEY (`id`),
    KEY `idx_merchant` (`merchant_id`),
    KEY `idx_category` (`category_id`),
    KEY `idx_status` (`status`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='菜品表';

-- ============================================================
-- 6. 订单主表 (order)
-- 状态机: PENDING_PAYMENT → PAID → PREPARING → DELIVERING → COMPLETED
--          PENDING_PAYMENT → CANCELLED (超时或用户取消)
-- ============================================================
CREATE TABLE `order` (
    `id`              BIGINT        NOT NULL AUTO_INCREMENT COMMENT '主键ID',
    `order_no`        VARCHAR(32)   NOT NULL COMMENT '订单号(雪花算法生成)',
    `user_id`         BIGINT        NOT NULL COMMENT '顾客ID',
    `merchant_id`     BIGINT        NOT NULL COMMENT '商家ID',
    `rider_id`        BIGINT        DEFAULT NULL COMMENT '骑手ID(接单后赋值)',
    `status`          VARCHAR(24)   NOT NULL DEFAULT 'PENDING_PAYMENT' COMMENT '订单状态: PENDING_PAYMENT/PAID/PREPARING/DELIVERING/COMPLETED/CANCELLED',
    `total_amount`    DECIMAL(10,2) NOT NULL COMMENT '商品总价',
    `delivery_fee`    DECIMAL(10,2) DEFAULT 0.00 COMMENT '配送费',
    `discount_amount` DECIMAL(10,2) DEFAULT 0.00 COMMENT '优惠金额',
    `pay_amount`      DECIMAL(10,2) NOT NULL COMMENT '实付金额',
    `address`         VARCHAR(256)  NOT NULL COMMENT '收货地址',
    `address_lng`     DECIMAL(10,7) DEFAULT NULL COMMENT '收货地址经度',
    `address_lat`     DECIMAL(10,7) DEFAULT NULL COMMENT '收货地址纬度',
    `remark`          VARCHAR(256)  DEFAULT NULL COMMENT '订单备注',
    `pay_time`        DATETIME      DEFAULT NULL COMMENT '支付时间',
    `deliver_time`    DATETIME      DEFAULT NULL COMMENT '配送开始时间',
    `complete_time`   DATETIME      DEFAULT NULL COMMENT '完成时间',
    `create_time`     DATETIME      NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '下单时间',
    `update_time`     DATETIME      NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_order_no` (`order_no`),
    KEY `idx_user` (`user_id`),
    KEY `idx_merchant` (`merchant_id`),
    KEY `idx_rider` (`rider_id`),
    KEY `idx_status` (`status`),
    KEY `idx_create_time` (`create_time`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='订单主表';

-- ============================================================
-- 7. 订单明细表 (order_detail)
-- 下单时快照菜品名和价格，防止商家改价后历史订单数据失真
-- ============================================================
CREATE TABLE `order_detail` (
    `id`          BIGINT        NOT NULL AUTO_INCREMENT COMMENT '明细ID',
    `order_id`    BIGINT        NOT NULL COMMENT '订单ID',
    `dish_id`     BIGINT        NOT NULL COMMENT '菜品ID',
    `dish_name`   VARCHAR(128)  NOT NULL COMMENT '菜品名称(快照)',
    `dish_image`  VARCHAR(512)  DEFAULT NULL COMMENT '菜品图片(快照)',
    `price`       DECIMAL(10,2) NOT NULL COMMENT '下单时单价',
    `quantity`    INT           NOT NULL COMMENT '数量',
    `create_time` DATETIME      NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    PRIMARY KEY (`id`),
    KEY `idx_order` (`order_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='订单明细表';

-- ============================================================
-- 8. 评价表 (review)
-- 一笔订单可分别评价商家和骑手
-- ============================================================
CREATE TABLE `review` (
    `id`          BIGINT       NOT NULL AUTO_INCREMENT COMMENT '评价ID',
    `order_id`    BIGINT       NOT NULL COMMENT '订单ID',
    `user_id`     BIGINT       NOT NULL COMMENT '评价人ID',
    `merchant_id` BIGINT       DEFAULT NULL COMMENT '被评价商家ID',
    `rider_id`    BIGINT       DEFAULT NULL COMMENT '被评价骑手ID',
    `rating`      TINYINT      NOT NULL COMMENT '评分(1-5)',
    `content`     VARCHAR(512) DEFAULT NULL COMMENT '评价内容',
    `images`      VARCHAR(1024) DEFAULT NULL COMMENT '评价图片(JSON数组)',
    `type`        VARCHAR(16)  NOT NULL DEFAULT 'MERCHANT' COMMENT '评价类型: MERCHANT/RIDER',
    `create_time` DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '评价时间',
    PRIMARY KEY (`id`),
    KEY `idx_order` (`order_id`),
    KEY `idx_user` (`user_id`),
    KEY `idx_merchant` (`merchant_id`),
    KEY `idx_rider` (`rider_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='评价表';

-- ============================================================
-- 9. 支付记录表 (payment)
-- 记录每笔支付流水，支持幂等校验
-- ============================================================
CREATE TABLE `payment` (
    `id`          BIGINT        NOT NULL AUTO_INCREMENT COMMENT '支付记录ID',
    `order_id`    BIGINT        NOT NULL COMMENT '订单ID',
    `pay_no`      VARCHAR(64)   NOT NULL COMMENT '支付流水号',
    `amount`      DECIMAL(10,2) NOT NULL COMMENT '支付金额',
    `method`      VARCHAR(32)   DEFAULT 'WECHAT' COMMENT '支付方式: WECHAT/ALIPAY/MOCK',
    `status`      VARCHAR(24)   NOT NULL DEFAULT 'PENDING' COMMENT '支付状态: PENDING/SUCCESS/FAILED/REFUNDED',
    `pay_time`    DATETIME      DEFAULT NULL COMMENT '支付完成时间',
    `create_time` DATETIME      NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `update_time` DATETIME      NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_pay_no` (`pay_no`),
    KEY `idx_order` (`order_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='支付记录表';

-- ============================================================
-- 10. 配送轨迹表 (delivery_track)
-- 记录骑手配送过程中的GPS坐标，用于轨迹回放
-- ============================================================
CREATE TABLE `delivery_track` (
    `id`          BIGINT        NOT NULL AUTO_INCREMENT COMMENT '轨迹ID',
    `order_id`    BIGINT        NOT NULL COMMENT '订单ID',
    `rider_id`    BIGINT        NOT NULL COMMENT '骑手ID',
    `longitude`   DECIMAL(10,7) NOT NULL COMMENT '经度',
    `latitude`    DECIMAL(10,7) NOT NULL COMMENT '纬度',
    `create_time` DATETIME      NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '上报时间',
    PRIMARY KEY (`id`),
    KEY `idx_order` (`order_id`),
    KEY `idx_rider_time` (`rider_id`, `create_time`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='配送轨迹表';
