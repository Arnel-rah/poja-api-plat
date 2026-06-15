alter table if exists "application"
    add column if not exists archived_at timestamp without time zone;
update application
set archived_at = now()
where archived = true
  and archived_at is null;