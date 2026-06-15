alter table if exists "storage_bucket_stack_resource"
    add constraint unique_storage_bucket_stack_resource_app_env_depl_per_stack unique (app_env_depl_id, stack_id);
alter table if exists "event_stack_resource"
    add constraint unique_event_stack_resource_app_env_depl_per_stack unique (app_env_depl_id, stack_id);
alter table if exists "compute_resources"
    add constraint unique_compute_resources_app_env_depl_per_stack unique (app_env_depl_id, stack_id);
