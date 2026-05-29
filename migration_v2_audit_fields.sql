-- ============================================================
-- Migration v2: Rejection reason + business license fields
-- Date: 2026-05-22
-- Prerequisite: migration_rider_audit.sql must have been run first
-- ============================================================

-- Rider: add rejection reason
ALTER TABLE rider
    ADD COLUMN `rejection_reason` VARCHAR(256) DEFAULT NULL COMMENT '审核驳回原因'
    AFTER `audit_status`;

-- Merchant: add business license + rejection reason
ALTER TABLE merchant
    ADD COLUMN `business_license` VARCHAR(64) DEFAULT NULL COMMENT '营业执照号'
    AFTER `phone`,
    ADD COLUMN `rejection_reason` VARCHAR(256) DEFAULT NULL COMMENT '审核驳回原因'
    AFTER `business_license`;
