-- ============================================================
-- Migration: Order Dispute System
-- ============================================================
USE waimai;

CREATE TABLE IF NOT EXISTS `order_dispute` (
    `id`              BIGINT        NOT NULL AUTO_INCREMENT,
    `order_id`        BIGINT        NOT NULL,
    `user_id`         BIGINT        NOT NULL,
    `type`            VARCHAR(32)   NOT NULL COMMENT 'WRONG_ITEM/MISSING_ITEM/QUALITY_ISSUE/OTHER',
    `description`     VARCHAR(512)  NOT NULL COMMENT '问题描述',
    `images`          VARCHAR(1024) DEFAULT NULL COMMENT '图片(JSON)',
    `status`          VARCHAR(24)   NOT NULL DEFAULT 'PENDING' COMMENT 'PENDING/INVESTIGATING/RESOLVED/REJECTED',
    `admin_remark`    VARCHAR(512)  DEFAULT NULL COMMENT '处理备注',
    `resolution`      VARCHAR(256)  DEFAULT NULL COMMENT '处理结果',
    `create_time`     DATETIME      NOT NULL DEFAULT CURRENT_TIMESTAMP,
    `update_time`     DATETIME      NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    PRIMARY KEY (`id`),
    KEY `idx_order` (`order_id`),
    KEY `idx_user` (`user_id`),
    KEY `idx_status` (`status`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='订单纠纷表';
