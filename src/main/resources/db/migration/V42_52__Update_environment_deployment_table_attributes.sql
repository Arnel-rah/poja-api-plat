alter table if exists "compute_resources" add column "app_env_depl_id" varchar;
alter table if exists "compute_resources" add constraint fk_compute_resource_app_env_depl foreign key ("app_env_depl_id") references "app_environment_deployment"("id");
