alter table if exists "event_stack_resource"
    add column "env_id" varchar;
alter table if exists "event_stack_resource"
    add column "app_env_depl_id" varchar;
alter table if exists "storage_bucket_stack_resource"
    add column "env_id" varchar;
alter table if exists "storage_bucket_stack_resource"
    add column "app_env_depl_id" varchar;
alter table if exists "storage_bucket_stack_resource" add constraint fk_storage_bucket_stack_resource_env foreign key (env_id) references environment(id);
alter table if exists "storage_bucket_stack_resource" add constraint fk_storage_bucket_stack_resource_app_env_depl foreign key (app_env_depl_id) references app_environment_deployment(id);
alter table if exists "event_stack_resource" add constraint fk_event_stack_resource_env foreign key (env_id) references environment(id);
alter table if exists "event_stack_resource" add constraint fk_event_stack_resource_app_env_depl foreign key (app_env_depl_id) references app_environment_deployment(id);
