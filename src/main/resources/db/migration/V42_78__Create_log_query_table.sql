do
$$
    begin
        if not exists(select from pg_type where typname = 'log_query_status') then
            create type log_query_status as enum ('PENDING','RUNNING','COMPLETED', 'FAILED', 'CANCELLED', 'TIMED_OUT', 'UNKNOWN');
        end if;
    end
$$;
create table if not exists "user_app_log_query"
(
    id                varchar
        constraint pk_log_query_info primary key default uuid_generate_v4(),
    creation_datetime timestamp without time zone,
    filter_keywords     text[],
    user_id           varchar,
    constraint fk_query_user foreign key (user_id) references "user" (id),
    app_id            varchar,
    constraint fk_query_app foreign key (app_id) references "application" (id),
    query_id          varchar,
    query_status      log_query_status           default 'PENDING'::log_query_status
)