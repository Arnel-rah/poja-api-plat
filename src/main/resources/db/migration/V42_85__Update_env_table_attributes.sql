alter table if exists "environment"
    add column if not exists "applied_conf_id" varchar;
alter table if exists "environment"
    add column if not exists "current_conf_id" varchar;