-- =============================================
-- Fortune Telling Application - Database Schema
-- Version: 1.26
-- Description: Update week_date from 1 (Monday old system) to 8 (Monday new system)
--              New system: week_date runs from 2 (Monday) to 8 (Sunday)
-- =============================================

-- Update all package_available_time records where week_date = 1 to week_date = 8
UPDATE package_available_time
SET week_date = 8,
    updated_at = CURRENT_TIMESTAMP
WHERE week_date = 1;

-- =============================================
-- END OF MIGRATION
-- =============================================

