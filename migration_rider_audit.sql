-- ============================================================
-- Migration: Add audit_status to rider table
-- Date: 2026-05-22
-- Description: Separate audit state from operational state
--   audit_status: 0-待审核 1-审核通过 2-驳回
--   status:       3-在线 4-离线 5-禁用 (operational only)
-- ============================================================

-- Step 1: Add new column
ALTER TABLE rider
    ADD COLUMN `audit_status` TINYINT NOT NULL DEFAULT 0 COMMENT '审核状态: 0待审核 1审核通过 2驳回'
    AFTER `openid`;

-- Step 2: Migrate existing data
-- Existing status values: 0=PENDING, 1=APPROVED, 2=REJECTED, 3=ONLINE, 4=OFFLINE, 5=DISABLED
-- New audit_status: 0=PENDING, 1=APPROVED, 2=REJECTED
-- New status: 3=ONLINE, 4=OFFLINE, 5=DISABLED

UPDATE rider SET audit_status = 1 WHERE status IN (1, 3, 4);   -- APPROVED/ONLINE/OFFLINE → audit approved
UPDATE rider SET audit_status = 0 WHERE status = 0;             -- PENDING stays pending
UPDATE rider SET audit_status = 2 WHERE status IN (2, 5);       -- REJECTED/DISABLED → audit rejected

-- set operational status: ONLINE→3, OFFLINE→4, others→4(offline)
UPDATE rider SET status = 3 WHERE status = 3;
UPDATE rider SET status = 4 WHERE status IN (0, 1, 2, 4, 5);

-- Step 3: Add index for audit queries
ALTER TABLE rider ADD KEY `idx_audit_status` (`audit_status`);
