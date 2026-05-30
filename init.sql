-- ============================================================
-- Waimai Delivery Platform — Complete Database Initialization
-- Version: 2.0.0
-- Engine: MySQL 8.x / InnoDB
-- Usage: mysql -u root -p < init.sql
-- ============================================================

CREATE DATABASE IF NOT EXISTS waimai DEFAULT CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
USE waimai;

-- ============================================================
-- 1. 顾客表 (user)
-- ============================================================
CREATE TABLE IF NOT EXISTS `user` (
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
-- ============================================================
CREATE TABLE IF NOT EXISTS `merchant` (
    `id`               BIGINT       NOT NULL AUTO_INCREMENT COMMENT '商家ID',
    `openid`           VARCHAR(64)  NOT NULL COMMENT '微信openid(商家端)',
    `name`             VARCHAR(100) NOT NULL COMMENT '店铺名称',
    `logo`             VARCHAR(512) DEFAULT NULL COMMENT '店铺logo URL',
    `banner`           VARCHAR(512) DEFAULT NULL COMMENT '店铺横幅图',
    `description`      VARCHAR(512) DEFAULT NULL COMMENT '店铺简介',
    `phone`            VARCHAR(20)  DEFAULT NULL COMMENT '联系电话',
    `business_license` VARCHAR(64)  DEFAULT NULL COMMENT '营业执照号',
    `rejection_reason` VARCHAR(256) DEFAULT NULL COMMENT '审核驳回原因',
    `address`          VARCHAR(256) DEFAULT NULL COMMENT '店铺地址',
    `longitude`        DECIMAL(10,7) DEFAULT NULL COMMENT '经度',
    `latitude`         DECIMAL(10,7) DEFAULT NULL COMMENT '纬度',
    `business_hours`   VARCHAR(64)  DEFAULT '09:00-22:00' COMMENT '营业时间',
    `min_delivery`     DECIMAL(10,2) DEFAULT 0.00 COMMENT '起送价',
    `delivery_fee`     DECIMAL(10,2) DEFAULT 0.00 COMMENT '配送费',
    `avg_delivery_time` INT         DEFAULT 30 COMMENT '平均配送时间/分钟',
    `monthly_sales`    INT          DEFAULT 0 COMMENT '月销量',
    `score`            DECIMAL(2,1) DEFAULT 5.0 COMMENT '评分(1-5)',
    `status`           TINYINT      DEFAULT 0 COMMENT '状态: 0待审核 1审核通过 2审核拒绝 3停用',
    `create_time`      DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '入驻时间',
    `update_time`      DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_openid` (`openid`),
    KEY `idx_status` (`status`),
    KEY `idx_location` (`longitude`, `latitude`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='商家表';

-- ============================================================
-- 3. 骑手表 (rider)
-- ============================================================
CREATE TABLE IF NOT EXISTS `rider` (
    `id`               BIGINT       NOT NULL AUTO_INCREMENT COMMENT '骑手ID',
    `openid`           VARCHAR(64)  NOT NULL COMMENT '微信openid(骑手端)',
    `audit_status`     TINYINT      NOT NULL DEFAULT 0 COMMENT '审核状态: 0待审核 1审核通过 2驳回',
    `rejection_reason` VARCHAR(256) DEFAULT NULL COMMENT '审核驳回原因',
    `real_name`        VARCHAR(32)  NOT NULL COMMENT '真实姓名',
    `id_card`          VARCHAR(18)  NOT NULL COMMENT '身份证号',
    `phone`            VARCHAR(20)  NOT NULL COMMENT '手机号',
    `avatar`           VARCHAR(512) DEFAULT NULL COMMENT '头像URL',
    `current_lng`      DECIMAL(10,7) DEFAULT NULL COMMENT '当前位置经度',
    `current_lat`      DECIMAL(10,7) DEFAULT NULL COMMENT '当前位置纬度',
    `status`           TINYINT      DEFAULT 4 COMMENT '操作状态: 3在线 4离线 5禁用',
    `total_orders`     INT          DEFAULT 0 COMMENT '完成订单数',
    `score`            DECIMAL(2,1) DEFAULT 5.0 COMMENT '评分(1-5)',
    `balance`          DECIMAL(10,2) DEFAULT 0.00 COMMENT '可提现余额',
    `level`            VARCHAR(16)  DEFAULT 'BRONZE' COMMENT '骑手等级',
    `level_score`      INT          DEFAULT 0 COMMENT '等级积分',
    `create_time`      DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '注册时间',
    `update_time`      DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_openid` (`openid`),
    KEY `idx_audit_status` (`audit_status`),
    KEY `idx_status` (`status`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='骑手表';

-- ============================================================
-- 4. 菜品分类表 (category)
-- ============================================================
CREATE TABLE IF NOT EXISTS `category` (
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
-- ============================================================
CREATE TABLE IF NOT EXISTS `dish` (
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
-- ============================================================
CREATE TABLE IF NOT EXISTS `order` (
    `id`               BIGINT        NOT NULL AUTO_INCREMENT COMMENT '主键ID',
    `order_no`         VARCHAR(32)   NOT NULL COMMENT '订单号(雪花算法生成)',
    `user_id`          BIGINT        NOT NULL COMMENT '顾客ID',
    `merchant_id`      BIGINT        NOT NULL COMMENT '商家ID',
    `rider_id`         BIGINT        DEFAULT NULL COMMENT '骑手ID(接单后赋值)',
    `status`           VARCHAR(24)   NOT NULL DEFAULT 'PENDING_PAYMENT' COMMENT '订单状态',
    `total_amount`     DECIMAL(10,2) NOT NULL COMMENT '商品总价',
    `delivery_fee`     DECIMAL(10,2) DEFAULT 0.00 COMMENT '配送费',
    `discount_amount`  DECIMAL(10,2) DEFAULT 0.00 COMMENT '优惠金额',
    `pay_amount`       DECIMAL(10,2) NOT NULL COMMENT '实付金额',
    `address`          VARCHAR(256)  NOT NULL COMMENT '收货地址',
    `address_lng`      DECIMAL(10,7) DEFAULT NULL COMMENT '收货地址经度',
    `address_lat`      DECIMAL(10,7) DEFAULT NULL COMMENT '收货地址纬度',
    `remark`           VARCHAR(256)  DEFAULT NULL COMMENT '订单备注',
    `pay_time`         DATETIME      DEFAULT NULL COMMENT '支付时间',
    `deliver_time`     DATETIME      DEFAULT NULL COMMENT '配送开始时间',
    `complete_time`    DATETIME      DEFAULT NULL COMMENT '完成时间',
    `estimated_minutes` INT          DEFAULT NULL COMMENT '预计送达时长/分钟',
    `is_overtime`      TINYINT       DEFAULT 0 COMMENT '是否超时 0否 1是',
    `is_joint_delivery` TINYINT      DEFAULT 0 COMMENT '是否联合配送 0否 1是',
    `create_time`      DATETIME      NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '下单时间',
    `update_time`      DATETIME      NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
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
-- ============================================================
CREATE TABLE IF NOT EXISTS `order_detail` (
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
-- ============================================================
CREATE TABLE IF NOT EXISTS `review` (
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
-- ============================================================
CREATE TABLE IF NOT EXISTS `payment` (
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
-- ============================================================
CREATE TABLE IF NOT EXISTS `delivery_track` (
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

-- ============================================================
-- 11. 骑手收入表 (rider_income)
-- ============================================================
CREATE TABLE IF NOT EXISTS `rider_income` (
    `id`          BIGINT        NOT NULL AUTO_INCREMENT,
    `rider_id`    BIGINT        NOT NULL,
    `order_id`    BIGINT        NOT NULL,
    `amount`      DECIMAL(10,2) NOT NULL DEFAULT 0.00 COMMENT '该单收入(元)',
    `type`        VARCHAR(16)   NOT NULL DEFAULT 'DELIVERY' COMMENT '收入类型: DELIVERY/TIP/BONUS',
    `create_time` DATETIME      NOT NULL DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (`id`),
    KEY `idx_rider_time` (`rider_id`, `create_time`),
    UNIQUE KEY `uk_rider_order` (`rider_id`, `order_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='骑手收入记录';

-- ============================================================
-- 12. 骑手提现表 (rider_withdrawal)
-- ============================================================
CREATE TABLE IF NOT EXISTS `rider_withdrawal` (
    `id`            BIGINT        NOT NULL AUTO_INCREMENT,
    `rider_id`      BIGINT        NOT NULL,
    `amount`        DECIMAL(10,2) NOT NULL COMMENT '提现金额',
    `status`        VARCHAR(16)   NOT NULL DEFAULT 'PENDING' COMMENT 'PENDING/SUCCESS/REJECTED',
    `remark`        VARCHAR(256)  DEFAULT NULL,
    `create_time`   DATETIME      NOT NULL DEFAULT CURRENT_TIMESTAMP,
    `update_time`   DATETIME      NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    PRIMARY KEY (`id`),
    KEY `idx_rider` (`rider_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='骑手提现记录';

-- ============================================================
-- 13. 系统配置表 (system_config)
-- ============================================================
CREATE TABLE IF NOT EXISTS `system_config` (
    `id`           BIGINT       NOT NULL AUTO_INCREMENT,
    `config_key`   VARCHAR(64)  NOT NULL COMMENT '配置键',
    `config_value` VARCHAR(512) NOT NULL COMMENT '配置值',
    `description`  VARCHAR(256) DEFAULT NULL COMMENT '说明',
    `create_time`  DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    `update_time`  DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_key` (`config_key`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='系统配置表';

INSERT IGNORE INTO system_config (config_key, config_value, description) VALUES
('delivery_fee_default', '5.00', '默认配送费(元)'),
('auto_cancel_minutes', '30', '未支付自动取消时间(分钟)'),
('max_delivery_radius', '5.0', '最大配送半径(公里)'),
('rider_income_per_order', '5.00', '骑手每单基础收入(元)');

-- ============================================================
-- 14. 优惠券模板表 (coupon)
-- ============================================================
CREATE TABLE IF NOT EXISTS `coupon` (
    `id`              BIGINT        NOT NULL AUTO_INCREMENT,
    `name`            VARCHAR(128)  NOT NULL COMMENT '优惠券名称',
    `type`            VARCHAR(32)   NOT NULL COMMENT 'FULL_REDUCTION/DISCOUNT/FREE_DELIVERY',
    `threshold`       DECIMAL(10,2) DEFAULT NULL COMMENT '满减门槛金额',
    `discount_value`  DECIMAL(10,2) NOT NULL COMMENT '优惠值',
    `total_count`     INT           NOT NULL DEFAULT 0 COMMENT '发放总量',
    `received_count`  INT           NOT NULL DEFAULT 0 COMMENT '已领取数量',
    `used_count`      INT           NOT NULL DEFAULT 0 COMMENT '已使用数量',
    `merchant_id`     BIGINT        DEFAULT NULL COMMENT '商家ID(NULL=平台券)',
    `valid_days`      INT           NOT NULL DEFAULT 7 COMMENT '领取后有效天数',
    `start_time`      DATETIME      DEFAULT NULL COMMENT '活动开始时间',
    `end_time`        DATETIME      DEFAULT NULL COMMENT '活动结束时间',
    `status`          TINYINT       DEFAULT 1 COMMENT '0停用 1启用',
    `create_time`     DATETIME      NOT NULL DEFAULT CURRENT_TIMESTAMP,
    `update_time`     DATETIME      NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    PRIMARY KEY (`id`),
    KEY `idx_merchant` (`merchant_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='优惠券模板';

-- ============================================================
-- 15. 用户优惠券表 (user_coupon)
-- ============================================================
CREATE TABLE IF NOT EXISTS `user_coupon` (
    `id`           BIGINT       NOT NULL AUTO_INCREMENT,
    `user_id`      BIGINT       NOT NULL,
    `coupon_id`    BIGINT       NOT NULL,
    `status`       VARCHAR(16)  NOT NULL DEFAULT 'UNUSED' COMMENT 'UNUSED/USED/EXPIRED',
    `receive_time` DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    `use_time`     DATETIME     DEFAULT NULL,
    `expire_time`  DATETIME     NOT NULL COMMENT '过期时间',
    PRIMARY KEY (`id`),
    KEY `idx_user_status` (`user_id`, `status`),
    KEY `idx_coupon` (`coupon_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='用户优惠券';

-- ============================================================
-- 16. 订单纠纷表 (order_dispute)
-- ============================================================
CREATE TABLE IF NOT EXISTS `order_dispute` (
    `id`              BIGINT        NOT NULL AUTO_INCREMENT,
    `order_id`        BIGINT        NOT NULL,
    `user_id`         BIGINT        NOT NULL,
    `type`            VARCHAR(32)   NOT NULL COMMENT 'WRONG_ITEM/MISSING_ITEM/QUALITY_ISSUE/NOT_DELIVERED/OTHER',
    `description`     VARCHAR(512)  NOT NULL COMMENT '问题描述',
    `images`          VARCHAR(1024) DEFAULT NULL COMMENT '图片(JSON)',
    `status`          VARCHAR(24)   NOT NULL DEFAULT 'PENDING' COMMENT 'PENDING/INVESTIGATING/RESOLVED/REJECTED',
    `refund_status`   VARCHAR(24)   DEFAULT NULL COMMENT '退款状态: REQUESTED/APPROVED/REJECTED',
    `merchant_remark` VARCHAR(512)  DEFAULT NULL COMMENT '商户处理备注',
    `previous_status` VARCHAR(24)   DEFAULT NULL COMMENT '退款前订单状态',
    `admin_remark`    VARCHAR(512)  DEFAULT NULL COMMENT '管理员备注',
    `resolution`      VARCHAR(256)  DEFAULT NULL COMMENT '处理结果',
    `create_time`     DATETIME      NOT NULL DEFAULT CURRENT_TIMESTAMP,
    `update_time`     DATETIME      NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    PRIMARY KEY (`id`),
    KEY `idx_order` (`order_id`),
    KEY `idx_user` (`user_id`),
    KEY `idx_status` (`status`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='订单纠纷表';

-- ============================================================
-- 17. 用户收货地址表 (user_address)
-- ============================================================
CREATE TABLE IF NOT EXISTS `user_address` (
    `id`          BIGINT        NOT NULL AUTO_INCREMENT COMMENT '主键ID',
    `user_id`     BIGINT        NOT NULL COMMENT '用户ID',
    `name`        VARCHAR(32)   NOT NULL COMMENT '收货人姓名',
    `phone`       VARCHAR(20)   NOT NULL COMMENT '收货人手机号',
    `address`     VARCHAR(256)  NOT NULL COMMENT '收货地址',
    `detail`      VARCHAR(128)  DEFAULT NULL COMMENT '门牌号/补充',
    `longitude`   DECIMAL(10,7) DEFAULT NULL COMMENT '地址经度',
    `latitude`    DECIMAL(10,7) DEFAULT NULL COMMENT '地址纬度',
    `is_default`  TINYINT       DEFAULT 0 COMMENT '是否默认地址 0-否 1-是',
    `create_time` DATETIME      NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `update_time` DATETIME      NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    PRIMARY KEY (`id`),
    KEY `idx_user_id` (`user_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='用户收货地址表';

-- ============================================================
-- 18. 联合配送组表 (joint_delivery_group)
-- ============================================================
CREATE TABLE IF NOT EXISTS `joint_delivery_group` (
    `id`                     BIGINT        NOT NULL AUTO_INCREMENT,
    `order_id`               BIGINT        NOT NULL COMMENT '订单ID',
    `group_no`               VARCHAR(32)   NOT NULL COMMENT '联合配送编号',
    `required_rider_count`   INT           NOT NULL DEFAULT 2 COMMENT '所需骑手数',
    `joined_rider_count`     INT           NOT NULL DEFAULT 0 COMMENT '已加入骑手数',
    `completed_rider_count`  INT           NOT NULL DEFAULT 0 COMMENT '已完成骑手数',
    `status`                 VARCHAR(24)   NOT NULL DEFAULT 'RECRUITING' COMMENT 'RECRUITING/READY/DELIVERING/COMPLETED/CANCELLED',
    `delivery_fee_total`     DECIMAL(10,2) NOT NULL DEFAULT 0.00 COMMENT '总配送费',
    `create_time`            DATETIME      NOT NULL DEFAULT CURRENT_TIMESTAMP,
    `update_time`            DATETIME      NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_group_no` (`group_no`),
    UNIQUE KEY `uk_order` (`order_id`),
    KEY `idx_status` (`status`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='联合配送组表';

-- ============================================================
-- 19. 联合配送成员表 (joint_delivery_member)
-- ============================================================
CREATE TABLE IF NOT EXISTS `joint_delivery_member` (
    `id`              BIGINT        NOT NULL AUTO_INCREMENT,
    `group_id`        BIGINT        NOT NULL COMMENT '联合配送组ID',
    `rider_id`        BIGINT        NOT NULL COMMENT '骑手ID',
    `order_id`        BIGINT        NOT NULL COMMENT '订单ID(冗余便于查询)',
    `status`          VARCHAR(24)   NOT NULL DEFAULT 'INVITED' COMMENT 'INVITED/JOINED/PICKED_UP/COMPLETED/CANCELLED',
    `earnings`        DECIMAL(10,2) NOT NULL DEFAULT 0.00 COMMENT '该骑手收益',
    `join_time`       DATETIME      DEFAULT NULL COMMENT '加入时间',
    `pickup_time`     DATETIME      DEFAULT NULL COMMENT '取餐时间',
    `complete_time`   DATETIME      DEFAULT NULL COMMENT '完成时间',
    `create_time`     DATETIME      NOT NULL DEFAULT CURRENT_TIMESTAMP,
    `update_time`     DATETIME      NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_group_rider` (`group_id`, `rider_id`),
    KEY `idx_rider_status` (`rider_id`, `status`),
    KEY `idx_order` (`order_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='联合配送成员表';

-- ============================================================
-- 20. 骑手配送异常表 (delivery_exception)
-- ============================================================
CREATE TABLE IF NOT EXISTS `delivery_exception` (
    `id`          BIGINT        NOT NULL AUTO_INCREMENT,
    `order_id`    BIGINT        NOT NULL,
    `rider_id`    BIGINT        NOT NULL,
    `type`        VARCHAR(32)   NOT NULL COMMENT 'CUSTOMER_UNREACHABLE/WRONG_ADDRESS/GOODS_DAMAGED/OTHER',
    `description` VARCHAR(512)  NOT NULL,
    `images`      VARCHAR(1024) DEFAULT NULL,
    `status`      VARCHAR(24)   NOT NULL DEFAULT 'REPORTED' COMMENT 'REPORTED/RESOLVED',
    `create_time` DATETIME      NOT NULL DEFAULT CURRENT_TIMESTAMP,
    `update_time` DATETIME      NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    PRIMARY KEY (`id`),
    KEY `idx_order` (`order_id`),
    KEY `idx_rider` (`rider_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='骑手配送异常表';

-- ============================================================
-- Seed Data — Sample Coupons
-- ============================================================
INSERT IGNORE INTO coupon (name, type, threshold, discount_value, total_count, valid_days, status) VALUES
('新用户满20减5', 'FULL_REDUCTION', 20.00, 5.00, 1000, 30, 1),
('全场9折券', 'DISCOUNT', NULL, 10.00, 500, 7, 1),
('免配送费券', 'FREE_DELIVERY', NULL, 5.00, 200, 14, 1);
