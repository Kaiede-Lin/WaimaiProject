-- ============================================================
-- Migration: Coupon System
-- ============================================================
USE waimai;

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

CREATE TABLE IF NOT EXISTS `user_coupon` (
    `id`          BIGINT       NOT NULL AUTO_INCREMENT,
    `user_id`     BIGINT       NOT NULL,
    `coupon_id`   BIGINT       NOT NULL,
    `status`      VARCHAR(16)  NOT NULL DEFAULT 'UNUSED' COMMENT 'UNUSED/USED/EXPIRED',
    `receive_time` DATETIME    NOT NULL DEFAULT CURRENT_TIMESTAMP,
    `use_time`    DATETIME     DEFAULT NULL,
    `expire_time` DATETIME     NOT NULL COMMENT '过期时间',
    PRIMARY KEY (`id`),
    KEY `idx_user_status` (`user_id`, `status`),
    KEY `idx_coupon` (`coupon_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='用户优惠券';
