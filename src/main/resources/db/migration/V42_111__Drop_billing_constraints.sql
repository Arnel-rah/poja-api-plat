alter table if exists "billing_info" alter column "app_id" drop not null;
alter table if exists "billing_info" alter column "env_id" drop not null;