-- Create schema for queue service
CREATE SCHEMA IF NOT EXISTS queue;

-- Grant permissions
GRANT ALL PRIVILEGES ON SCHEMA queue TO postgres;

-- Create extension for UUID
CREATE EXTENSION IF NOT EXISTS "uuid-ossp";

-- Create queue_items table
CREATE TABLE queue.queue_items
(
    id          UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    user_id      UUID NOT NULL UNIQUE,
    joined_at    TIMESTAMP WITHOUT TIME ZONE NOT NULL DEFAULT (now() AT TIME ZONE 'utc'),
    status       VARCHAR(20) NOT NULL DEFAULT 'WAITING'
);

-- Create indexes for better query performance
CREATE INDEX idx_queue_items_user_id ON queue.queue_items(user_id);
CREATE INDEX idx_queue_items_status ON queue.queue_items(status);
CREATE INDEX idx_queue_items_joined_at ON queue.queue_items(joined_at);
