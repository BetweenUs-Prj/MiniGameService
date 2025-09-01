-- V4: Add integrity constraints for quiz_answer table
-- Purpose: Prevent 500 errors by adding proper DB constraints

-- 1. Add unique constraint to prevent duplicate submissions
-- Note: This might already exist, so we use IF NOT EXISTS equivalent
ALTER TABLE quiz_answer
  DROP INDEX IF EXISTS uq_round_user;

ALTER TABLE quiz_answer
  ADD CONSTRAINT uq_round_user UNIQUE (round_id, user_uid);

-- 2. Add option_id column to maintain FK relationship (optional but recommended)
-- Check if column already exists
SET @column_exists = (
  SELECT COUNT(*) 
  FROM information_schema.columns 
  WHERE table_schema = DATABASE() 
    AND table_name = 'quiz_answer' 
    AND column_name = 'option_id'
);

SET @sql = IF(@column_exists = 0,
  'ALTER TABLE quiz_answer ADD COLUMN option_id BIGINT NULL AFTER choice_index',
  'SELECT "Column option_id already exists"'
);

PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

-- 3. Add foreign key constraint for option_id
SET @fk_exists = (
  SELECT COUNT(*)
  FROM information_schema.table_constraints
  WHERE table_schema = DATABASE()
    AND table_name = 'quiz_answer'
    AND constraint_name = 'fk_quiz_answer_option'
);

SET @sql = IF(@fk_exists = 0,
  'ALTER TABLE quiz_answer ADD CONSTRAINT fk_quiz_answer_option FOREIGN KEY (option_id) REFERENCES quiz_question_option(option_id) ON DELETE SET NULL',
  'SELECT "FK fk_quiz_answer_option already exists"'
);

PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

-- 4. Make answer_text nullable (to handle cases where option text is not available)
ALTER TABLE quiz_answer 
  MODIFY COLUMN answer_text TEXT NULL;

-- 5. Add index for better query performance
ALTER TABLE quiz_answer
  DROP INDEX IF EXISTS idx_quiz_answer_round_user;

ALTER TABLE quiz_answer
  ADD INDEX idx_quiz_answer_round_user (round_id, user_uid);

ALTER TABLE quiz_answer
  DROP INDEX IF EXISTS idx_quiz_answer_session;

ALTER TABLE quiz_answer
  ADD INDEX idx_quiz_answer_session (round_id);

-- 6. Ensure round_id has proper FK constraint
SET @fk_round_exists = (
  SELECT COUNT(*)
  FROM information_schema.table_constraints
  WHERE table_schema = DATABASE()
    AND table_name = 'quiz_answer'
    AND constraint_name = 'fk_quiz_answer_round'
);

SET @sql = IF(@fk_round_exists = 0,
  'ALTER TABLE quiz_answer ADD CONSTRAINT fk_quiz_answer_round FOREIGN KEY (round_id) REFERENCES quiz_round(round_id) ON DELETE CASCADE',
  'SELECT "FK fk_quiz_answer_round already exists"'
);

PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

-- Log completion
SELECT 'Quiz answer constraints applied successfully' AS status;