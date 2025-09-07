-- Add private room support to game_session table
ALTER TABLE game_session 
ADD COLUMN is_private BOOLEAN NOT NULL DEFAULT FALSE,
ADD COLUMN pin_hash VARCHAR(100);

-- Create index for private room queries
CREATE INDEX idx_game_session_private ON game_session(is_private);