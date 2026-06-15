do
$$
    begin
        if not exists(select from pg_type where typname = 'app_import_gha_run_job_name') then
            create type app_import_gha_run_job_name as enum (
                'PRE_TRANSFORMATION_TEST',
                'POST_TRANSFORMATION_TEST'
            );
        end if;
    end;
$$;

create table if not exists "application_import_gha_run" (
    id varchar constraint pk_app_import_gha_run primary key default uuid_generate_v4(),
    job_name app_import_gha_run_job_name not null,
    run_uri varchar not null,
    app_import_id varchar not null,
    constraint fk_gha_run_app_import foreign key (app_import_id) references "application_import"(id)
);