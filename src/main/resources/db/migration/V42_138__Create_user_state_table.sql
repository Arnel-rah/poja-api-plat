do
$$
    begin
        if not exists(select from pg_type where typname = 'execution_type') then
            create type execution_type as enum ('SYNCHRONOUS','ASYNCHRONOUS');
        end if;

        if not exists(select from pg_type where typname = 'user_progression_status') then
            create type user_progression_status as enum ('ACTIVE', 'SUSPENDED', 'UNDER_MODIFICATION');
        end if;
    end
$$;

create table if not exists "user_state"(
    id varchar constraint pk_user_state primary key default uuid_generate_v4(),
    progression_status user_progression_status not null,
    timestamp timestamp without time zone,
    execution_type execution_type not null,
    description varchar default '',
    user_id varchar,
    constraint fk_user_state foreign key (user_id) references "user"(id)
);
