-- Add additional columns to reaction_result table for better tracking
ALTER TABLE reaction_result ADD COLUMN clicked_at TIMESTAMP;
ALTER TABLE reaction_result ADD COLUMN false_start BOOLEAN DEFAULT FALSE;