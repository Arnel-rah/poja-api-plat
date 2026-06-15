do
$$
begin
        if not exists(select from pg_type where typname = 'organization_setup_state_status') then
create type organization_setup_state_status as enum (
                'ORGANIZATION_SETUP_IN_PROGRESS',
                'ORGANIZATION_SETUP_FAILED',
                'ORGANIZATION_SETUP_COMPLETED'
            );
end if;
end;
$$;

create table if not exists "organization_setup_state"(
    id varchar constraint pk_organization_setup_state primary key default uuid_generate_v4(),
    progression_status organization_setup_state_status not null,
    timestamp timestamp without time zone,
    execution_type execution_type not null,
    org_id varchar,
    constraint fk_organization_setup_state foreign key (org_id) references "organization"(id)
);