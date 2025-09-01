-- Add round_id column to reaction_result table for proper round tracking
ALTER TABLE reaction_result ADD COLUMN round_id BIGINT;

-- Create index for better query performance on round_id
CREATE INDEX idx_reaction_result_round_id ON reaction_result(round_id);