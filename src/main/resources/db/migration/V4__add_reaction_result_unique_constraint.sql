-- Add unique constraint to prevent duplicate clicks in reaction game
ALTER TABLE reaction_result ADD CONSTRAINT ux_reaction_session_user UNIQUE (session_id, user_uid);