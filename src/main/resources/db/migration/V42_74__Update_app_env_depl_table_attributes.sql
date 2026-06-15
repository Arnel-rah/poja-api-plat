alter table if exists "app_environment_deployment"
    add column if not exists "gh_tag_name" varchar;
alter table if exists "app_environment_deployment"
    add column if not exists "gh_tag_message" varchar;
