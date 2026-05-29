-- ============================================================
-- Migration: System Configuration
-- ============================================================
USE waimai;

CREATE TABLE IF NOT EXISTS `system_config` (
    `id`          BIGINT       NOT NULL AUTO_INCREMENT,
    `config_key`  VARCHAR(64)  NOT NULL COMMENT '配置键',
    `config_value` VARCHAR(512) NOT NULL COMMENT '配置值',
    `description` VARCHAR(256) DEFAULT NULL COMMENT '说明',
    `create_time` DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    `update_time` DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_key` (`config_key`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='系统配置表';

INSERT IGNORE INTO system_config (config_key, config_value, description) VALUES
('delivery_fee_default', '5.00', '默认配送费(元)'),
('auto_cancel_minutes', '30', '未支付自动取消时间(分钟)'),
('max_delivery_radius', '5.0', '最大配送半径(公里)'),
('rider_income_per_order', '5.00', '骑手每单基础收入(元)');
