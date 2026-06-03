CREATE UNIQUE INDEX IF NOT EXISTS uq_notifications_pantry_expiry_event
    ON notifications(user_id, type, related_entity_type, related_entity_id)
    WHERE type IN ('pantry_expired', 'pantry_expiring')
      AND related_entity_type IS NOT NULL
      AND related_entity_id IS NOT NULL;
