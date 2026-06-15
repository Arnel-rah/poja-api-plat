alter table if exists "app_environment_deployment"
    add column if not exists "is_redepl" boolean default false;