-- ============================================================
-- Migration: Rider Income & Withdrawal System
-- ============================================================
USE waimai;

CREATE TABLE IF NOT EXISTS `rider_income` (
    `id`          BIGINT        NOT NULL AUTO_INCREMENT,
    `rider_id`    BIGINT        NOT NULL,
    `order_id`    BIGINT        NOT NULL,
    `amount`      DECIMAL(10,2) NOT NULL DEFAULT 0.00 COMMENT '该单收入(元)',
    `type`        VARCHAR(16)   NOT NULL DEFAULT 'DELIVERY' COMMENT '收入类型: DELIVERY/TIP/BONUS',
    `create_time` DATETIME      NOT NULL DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (`id`),
    KEY `idx_rider_time` (`rider_id`, `create_time`),
    UNIQUE KEY `uk_order` (`order_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='骑手收入记录';

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

ALTER TABLE `rider` ADD COLUMN IF NOT EXISTS `balance` DECIMAL(10,2) DEFAULT 0.00 COMMENT '可提现余额' AFTER `score`;
ALTER TABLE `rider` ADD COLUMN IF NOT EXISTS `level` VARCHAR(16) DEFAULT 'BRONZE' COMMENT '骑手等级' AFTER `balance`;
ALTER TABLE `rider` ADD COLUMN IF NOT EXISTS `level_score` INT DEFAULT 0 COMMENT '等级积分' AFTER `level`;