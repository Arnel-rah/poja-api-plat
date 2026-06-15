alter table "environment"
    add column "current_deployment_id" varchar;
alter table "environment"
    add constraint "fk_current_env_depl" foreign key (current_deployment_id) references app_environment_deployment (id);
update environment e
set current_deployment_id = (select id
                             from app_environment_deployment aed
                             where aed.env_id = e.id
                             order by aed.creation_datetime desc
                             limit 1)
where e.current_deployment_id is null;
