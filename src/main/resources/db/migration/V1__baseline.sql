-- USERS
CREATE TABLE IF NOT EXISTS users (
    id UUID PRIMARY KEY,
    created_at TIMESTAMP NOT NULL,
    updated_at TIMESTAMP NOT NULL,
    email VARCHAR(255) NOT NULL UNIQUE,
    first_name VARCHAR(255) NOT NULL,
    last_name VARCHAR(255) NOT NULL,
    password VARCHAR(255) NOT NULL
);

-- TOKENS
CREATE TABLE IF NOT EXISTS tokens (
    id UUID PRIMARY KEY,
    created_at TIMESTAMP NOT NULL,
    updated_at TIMESTAMP NOT NULL,
    expired BOOLEAN NOT NULL,
    revoked BOOLEAN NOT NULL,
    token_type VARCHAR(255) NOT NULL,
    token_value VARCHAR(1000) NOT NULL UNIQUE,
    user_id UUID NOT NULL,
    CONSTRAINT fk_tokens_user FOREIGN KEY (user_id) REFERENCES users(id)
);

-- EVENTS
CREATE TABLE IF NOT EXISTS events (
    id UUID PRIMARY KEY,
    created_at TIMESTAMP NOT NULL,
    updated_at TIMESTAMP NOT NULL,
    description VARCHAR(1000),
    event_date TIMESTAMP NOT NULL,
    latitude DOUBLE PRECISION NOT NULL,
    location VARCHAR(255) NOT NULL,
    location_address VARCHAR(255) NOT NULL,
    longitude DOUBLE PRECISION NOT NULL,
    place_id VARCHAR(255) NOT NULL,
    status VARCHAR(255) NOT NULL,
    title VARCHAR(255) NOT NULL,
    created_by UUID NOT NULL,
    CONSTRAINT fk_events_created_by FOREIGN KEY (created_by) REFERENCES users(id)
);

-- EVENT INVITATIONS
CREATE TABLE IF NOT EXISTS event_invitations (
    id UUID PRIMARY KEY,
    created_at TIMESTAMP NOT NULL,
    updated_at TIMESTAMP NOT NULL,
    status VARCHAR(255) NOT NULL,
    event_id UUID NOT NULL,
    invited_by UUID NOT NULL,
    invited_user_id UUID NOT NULL,
    CONSTRAINT fk_event_invitations_event FOREIGN KEY (event_id) REFERENCES events(id),
    CONSTRAINT fk_event_invitations_invited_by FOREIGN KEY (invited_by) REFERENCES users(id),
    CONSTRAINT fk_event_invitations_invited_user FOREIGN KEY (invited_user_id) REFERENCES users(id)
);

-- EXTERNAL INVITATIONS
CREATE TABLE IF NOT EXISTS external_invitations (
    id UUID PRIMARY KEY,
    created_at TIMESTAMP NOT NULL,
    updated_at TIMESTAMP NOT NULL,
    claimed_at TIMESTAMP,
    invited_email VARCHAR(255) NOT NULL,
    status VARCHAR(255) NOT NULL,
    token VARCHAR(255) NOT NULL UNIQUE,
    event_id UUID NOT NULL,
    invited_by UUID NOT NULL,
    CONSTRAINT fk_external_invitations_event FOREIGN KEY (event_id) REFERENCES events(id),
    CONSTRAINT fk_external_invitations_invited_by FOREIGN KEY (invited_by) REFERENCES users(id)
);

-- CONTRIBUTIONS
CREATE TABLE IF NOT EXISTS contributions (
    id UUID PRIMARY KEY,
    created_at TIMESTAMP NOT NULL,
    updated_at TIMESTAMP NOT NULL,
    completed BOOLEAN NOT NULL,
    cost NUMERIC(10, 2),
    description VARCHAR(255),
    name VARCHAR(100) NOT NULL,
    split_cost BOOLEAN NOT NULL,
    created_by UUID NOT NULL,
    event_id UUID NOT NULL,
    CONSTRAINT fk_contributions_created_by FOREIGN KEY (created_by) REFERENCES users(id),
    CONSTRAINT fk_contributions_event FOREIGN KEY (event_id) REFERENCES events(id)
);

-- NOTIFICATION LOGS
CREATE TABLE IF NOT EXISTS notification_logs (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    invitation_id UUID NOT NULL UNIQUE,
    sent_at TIMESTAMP NOT NULL
);

-- INDEXES
CREATE INDEX IF NOT EXISTS idx_contribution_event ON contributions(event_id);
CREATE INDEX IF NOT EXISTS idx_contribution_created_by ON contributions(created_by);
CREATE INDEX IF NOT EXISTS idx_notification_logs_invitation_id ON notification_logs(invitation_id);