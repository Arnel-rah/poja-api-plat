create table if not exists "organization"
(
    id       varchar
        constraint pk_organization primary key default uuid_generate_v4(),
    name     varchar unique,
    creation_datetime                 timestamp without time zone default now(),
    owner_id varchar,
    constraint fk_organization_owner foreign key (owner_id) references "user" ("id")
);
do
$$
    begin
        if not exists(select from pg_type where typname = 'organization_invite_status') then
            create type organization_invite_status as enum ('ACCEPTED', 'REJECTED', 'PENDING');
        end if;
    end
$$;
