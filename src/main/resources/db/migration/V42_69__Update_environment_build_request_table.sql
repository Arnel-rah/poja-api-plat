alter table "env_build_request"
    add column app_env_depl_id varchar;
alter table "env_build_request"
    add constraint fk_env_build_app_env_depl foreign key (app_env_depl_id) references app_environment_deployment (id);
