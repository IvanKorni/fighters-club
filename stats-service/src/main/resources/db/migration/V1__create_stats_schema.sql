-- Create schema for stats service
CREATE SCHEMA IF NOT EXISTS stats;

-- Grant permissions
GRANT ALL PRIVILEGES ON SCHEMA stats TO postgres;

-- Create extension for UUID
CREATE EXTENSION IF NOT EXISTS "uuid-ossp";

-- Create stats table
CREATE TABLE stats.stats
(
    user_id                 UUID PRIMARY KEY,
    wins                    INTEGER NOT NULL DEFAULT 0,
    losses                  INTEGER NOT NULL DEFAULT 0,
    draws                   INTEGER NOT NULL DEFAULT 0,
    total_matches           INTEGER NOT NULL DEFAULT 0,
    win_rate                DOUBLE PRECISION DEFAULT 0.0,
    average_match_duration  INTEGER DEFAULT 0,
    rating                  INTEGER
);

-- Create matches table for stats service
CREATE TABLE stats.matches
(
    id          UUID PRIMARY KEY,
    player1_id  UUID NOT NULL,
    player2_id  UUID NOT NULL,
    winner_id   UUID,
    status      VARCHAR(20) NOT NULL,
    duration    INTEGER,
    turn_count  INTEGER,
    created     TIMESTAMP WITHOUT TIME ZONE NOT NULL DEFAULT (now() AT TIME ZONE 'utc'),
    finished_at TIMESTAMP WITHOUT TIME ZONE
);

-- Create indexes for better query performance
CREATE INDEX idx_matches_player1_id ON stats.matches(player1_id);
CREATE INDEX idx_matches_player2_id ON stats.matches(player2_id);
CREATE INDEX idx_matches_created ON stats.matches(created DESC);


