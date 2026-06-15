alter table if exists "environment" add column if not exists archived_at timestamp without time zone;
update environment set archived_at = now() where archived = true and archived_at IS NULL;