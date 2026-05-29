-- ============================================================
-- 用户收货地址表 (user_address)
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
