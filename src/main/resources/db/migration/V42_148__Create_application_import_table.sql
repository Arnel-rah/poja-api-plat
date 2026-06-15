do
$$
begin
        if not exists(select from pg_type where typname = 'application_import_status') then
        create type application_import_status as enum (
                        'PENDING',
                        'IN_PROGRESS',
                        'SUCCESSFUL',
                        'FAILED'
                    );
        end if;
end;
$$;

create table if not exists "application_import"(
    id varchar constraint pk_app_import primary key default uuid_generate_v4(),
    app_name varchar,
    github_repository_name varchar,
    github_repository_http_url varchar,
    creation_datetime timestamp without time zone default now(),
    status application_import_status default 'PENDING',
    org_id varchar not null,
    app_installation_id varchar not null,
    constraint fk_app_import_organization foreign key (org_id) references "organization"(id),
    constraint fk_app_import_installation foreign key (app_installation_id) references "app_installation"(id)
);