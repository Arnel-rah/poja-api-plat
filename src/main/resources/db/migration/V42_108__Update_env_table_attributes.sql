alter table if exists "environment"
    add column if not exists "status" env_status not null default 'ACTIVE';