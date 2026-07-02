CREATE TABLE IF NOT EXISTS notification_logs (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    invitation_id UUID NOT NULL UNIQUE,
    sent_at TIMESTAMP NOT NULL
);

CREATE INDEX idx_notification_logs_invitation_id 
    ON notification_logs(invitation_id);