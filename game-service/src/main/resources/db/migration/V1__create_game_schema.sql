-- Create schema for game service
CREATE SCHEMA IF NOT EXISTS game;

-- Grant permissions
GRANT ALL PRIVILEGES ON SCHEMA game TO postgres;

-- Create extension for UUID
CREATE EXTENSION IF NOT EXISTS "uuid-ossp";

-- Create matches table
CREATE TABLE game.matches
(
    id                  UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    player1_id          UUID NOT NULL,
    player2_id          UUID NOT NULL,
    winner_id           UUID,
    status              VARCHAR(20) NOT NULL,
    player1_hp          INTEGER NOT NULL,
    player2_hp          INTEGER NOT NULL,
    turn_number         INTEGER NOT NULL DEFAULT 1,
    current_turn_start  TIMESTAMP WITHOUT TIME ZONE,
    created             TIMESTAMP WITHOUT TIME ZONE NOT NULL DEFAULT (now() AT TIME ZONE 'utc'),
    updated             TIMESTAMP WITHOUT TIME ZONE NOT NULL DEFAULT (now() AT TIME ZONE 'utc'),
    finished_at         TIMESTAMP WITHOUT TIME ZONE,
    duration            INTEGER,
    turn_count          INTEGER
);

-- Create moves table
CREATE TABLE game.moves
(
    id              UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    match_id        UUID NOT NULL,
    player_id       UUID NOT NULL,
    attack_target   VARCHAR(10) NOT NULL,
    defense_target  VARCHAR(10) NOT NULL,
    turn_number     INTEGER NOT NULL,
    damage          INTEGER,
    created         TIMESTAMP WITHOUT TIME ZONE NOT NULL DEFAULT (now() AT TIME ZONE 'utc')
);

-- Create indexes for better query performance
CREATE INDEX idx_matches_player1_id ON game.matches(player1_id);
CREATE INDEX idx_matches_player2_id ON game.matches(player2_id);
CREATE INDEX idx_matches_status ON game.matches(status);
CREATE INDEX idx_moves_match_id ON game.moves(match_id);
CREATE INDEX idx_moves_player_id ON game.moves(player_id);
CREATE INDEX idx_moves_match_turn ON game.moves(match_id, turn_number);


