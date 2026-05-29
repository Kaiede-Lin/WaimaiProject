-- ============================================================
-- Migration: ETA + Overtime fields
-- ============================================================
USE waimai;

ALTER TABLE `order` ADD COLUMN IF NOT EXISTS `estimated_minutes` INT DEFAULT NULL COMMENT '预计送达时长/分钟' AFTER `complete_time`;
ALTER TABLE `order` ADD COLUMN IF NOT EXISTS `is_overtime` TINYINT DEFAULT 0 COMMENT '是否超时 0否 1是' AFTER `estimated_minutes`;
