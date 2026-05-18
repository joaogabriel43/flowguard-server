-- M-1: Performance indexes on tenant_id for all frequently queried tables
CREATE INDEX idx_users_tenant_id          ON users(tenant_id);
CREATE INDEX idx_feature_flags_tenant_id  ON feature_flags(tenant_id);
CREATE INDEX idx_audit_logs_tenant_id     ON audit_logs(tenant_id);
CREATE INDEX idx_audit_logs_flag_id       ON audit_logs(flag_id);

-- M-6: Add tenant_id to flag_rules for direct tenant isolation without requiring a join
ALTER TABLE flag_rules ADD COLUMN tenant_id UUID;

UPDATE flag_rules fr
    SET tenant_id = (SELECT ff.tenant_id FROM feature_flags ff WHERE ff.id = fr.flag_id);

ALTER TABLE flag_rules ALTER COLUMN tenant_id SET NOT NULL;
ALTER TABLE flag_rules ADD CONSTRAINT fk_flag_rules_tenants
    FOREIGN KEY (tenant_id) REFERENCES tenants(id);
CREATE INDEX idx_flag_rules_tenant_id ON flag_rules(tenant_id);

-- M-7: State tracking columns in audit_logs
ALTER TABLE audit_logs ADD COLUMN previous_state VARCHAR(255);
ALTER TABLE audit_logs ADD COLUMN new_state      VARCHAR(255);

-- M-7: Preserve audit logs when flags are deleted so DELETE audit entries survive cascade.
--      Change flag_id FK from ON DELETE CASCADE to ON DELETE SET NULL + make column nullable.
ALTER TABLE audit_logs ALTER COLUMN flag_id DROP NOT NULL;
ALTER TABLE audit_logs DROP CONSTRAINT audit_logs_flag_id_fkey;
ALTER TABLE audit_logs ADD CONSTRAINT audit_logs_flag_id_fkey
    FOREIGN KEY (flag_id) REFERENCES feature_flags(id) ON DELETE SET NULL;
