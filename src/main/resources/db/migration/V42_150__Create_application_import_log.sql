do
$$
begin
        if not exists(select from pg_type where typname = 'application_import_log_type') then
            create type application_import_log_type as enum (
                        'WARNING',
                        'ERROR',
                        'INFO'
                    );
        end if;
end;
$$;

create table if not exists "application_import_log"(
    id varchar constraint pk_app_import_log primary key default uuid_generate_v4(),
    type application_import_log_type not null,
    message varchar not null,
    timestamp timestamp without time zone,
    state_id varchar not null,
    constraint fk_app_import_log_state foreign key (state_id) references "application_import_state"(id)
);