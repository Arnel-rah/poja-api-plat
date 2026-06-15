do
$$
    begin
        if not exists(select from pg_type where typname = 'app_setup_state_status') then
            create type app_setup_state_status as enum (
                'ENV_CREATION_IN_PROGRESS',
                'ENV_CREATION_FAILED',
                'ENV_CREATION_SUCCESS',
                'REPO_CREATION_IN_PROGRESS',
                'REPO_CREATION_FAILED',
                'REPO_CREATION_SUCCESS',
                'ENV_DEPLOYMENT_INITIATION_FAILED',
                'ENV_DEPLOYMENT_INITIATED',
                'ENV_DEPLOYMENT_INITIATION_IN_PROGRESS'
                );
        end if;
    end;
$$;

create table if not exists "app_setup_state" (
    id varchar constraint pk_app_setup_state primary key  default uuid_generate_v4(),
    progression_status app_setup_state_status not null ,
    "timestamp" timestamp without time zone,
    execution_type execution_type not null,
    app_id varchar not null,
    org_id varchar not null,
    constraint fk_app_setup_state foreign KEY (app_id) references "application"(id),
    constraint fk_organization_setup_state foreign key (org_id) references "organization"(id)
);
