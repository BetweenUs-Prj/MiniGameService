-- V5: Add session_id column to reaction_result table
-- Purpose: Fix reaction game current-round API by adding missing session_id column

-- Check if session_id column already exists
SET @column_exists = (
  SELECT COUNT(*) 
  FROM information_schema.columns 
  WHERE table_schema = DATABASE() 
    AND table_name = 'reaction_result' 
    AND column_name = 'session_id'
);

-- Add session_id column if it doesn't exist
SET @sql = IF(@column_exists = 0,
  'ALTER TABLE reaction_result ADD COLUMN session_id BIGINT NOT NULL DEFAULT 0 AFTER result_id',
  'SELECT "Column session_id already exists"'
);

PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

-- Add index for better query performance
SET @index_exists = (
  SELECT COUNT(*)
  FROM information_schema.statistics
  WHERE table_schema = DATABASE()
    AND table_name = 'reaction_result'
    AND index_name = 'idx_reaction_result_session'
);

SET @sql = IF(@index_exists = 0,
  'ALTER TABLE reaction_result ADD INDEX idx_reaction_result_session (session_id)',
  'SELECT "Index idx_reaction_result_session already exists"'
);

PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

-- Add index for session_id and user_id combination for better performance
SET @combo_index_exists = (
  SELECT COUNT(*)
  FROM information_schema.statistics
  WHERE table_schema = DATABASE()
    AND table_name = 'reaction_result'
    AND index_name = 'idx_reaction_result_session_user'
);

SET @sql = IF(@combo_index_exists = 0,
  'ALTER TABLE reaction_result ADD INDEX idx_reaction_result_session_user (session_id, user_id)',
  'SELECT "Index idx_reaction_result_session_user already exists"'
);

PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

-- Log completion
SELECT 'Reaction result session_id column added successfully' AS status;