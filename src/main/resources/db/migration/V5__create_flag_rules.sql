CREATE TABLE flag_rules (
    id UUID PRIMARY KEY,
    flag_id UUID NOT NULL REFERENCES feature_flags(id) ON DELETE CASCADE,
    attribute_key VARCHAR(255) NOT NULL,
    operator VARCHAR(50) NOT NULL,
    attribute_value TEXT NOT NULL,
    created_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX idx_flag_rules_flag_id ON flag_rules(flag_id);
