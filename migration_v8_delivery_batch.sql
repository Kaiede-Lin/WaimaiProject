-- ============================================================
-- Migration: Multi-Rider Collaborative Delivery
-- ============================================================
USE waimai;

CREATE TABLE IF NOT EXISTS `delivery_batch` (
    `id`                  BIGINT      NOT NULL AUTO_INCREMENT,
    `order_id`            BIGINT      NOT NULL COMMENT '主订单ID',
    `batch_no`            VARCHAR(32) NOT NULL COMMENT '批次号',
    `total_sub_count`     INT         NOT NULL DEFAULT 0 COMMENT '子任务总数',
    `completed_sub_count` INT         NOT NULL DEFAULT 0 COMMENT '已完成子任务数',
    `status`              VARCHAR(24) NOT NULL DEFAULT 'DISPATCHING' COMMENT 'DISPATCHING/IN_PROGRESS/COMPLETED',
    `create_time`         DATETIME    NOT NULL DEFAULT CURRENT_TIMESTAMP,
    `update_time`         DATETIME    NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_batch_no` (`batch_no`),
    KEY `idx_order` (`order_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='配送批次表';

CREATE TABLE IF NOT EXISTS `delivery_sub_task` (
    `id`               BIGINT        NOT NULL AUTO_INCREMENT,
    `batch_id`         BIGINT        NOT NULL COMMENT '批次ID',
    `order_id`         BIGINT        NOT NULL COMMENT '主订单ID',
    `rider_id`         BIGINT        DEFAULT NULL COMMENT '骑手ID',
    `sub_address`      VARCHAR(256)  NOT NULL COMMENT '子配送地址',
    `sub_address_lng`  DECIMAL(10,7) DEFAULT NULL,
    `sub_address_lat`  DECIMAL(10,7) DEFAULT NULL,
    `items_json`       TEXT          DEFAULT NULL COMMENT '配送菜品JSON',
    `status`           VARCHAR(24)   NOT NULL DEFAULT 'PENDING' COMMENT 'PENDING/ASSIGNED/PICKED_UP/DELIVERING/COMPLETED',
    `estimated_minutes` INT          DEFAULT NULL,
    `deliver_time`     DATETIME      DEFAULT NULL,
    `complete_time`    DATETIME      DEFAULT NULL,
    `create_time`      DATETIME      NOT NULL DEFAULT CURRENT_TIMESTAMP,
    `update_time`      DATETIME      NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    PRIMARY KEY (`id`),
    KEY `idx_batch` (`batch_id`),
    KEY `idx_rider` (`rider_id`),
    KEY `idx_order` (`order_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='配送子任务表';
