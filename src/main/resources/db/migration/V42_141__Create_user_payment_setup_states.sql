do
$$
begin
        if not exists(select from pg_type where typname = 'user_payment_setup_status') then
            create type user_payment_setup_status as enum (
                'PAYMENT_SETUP_IN_PROGRESS',
                'PAYMENT_SETUP_FAILED',
                'PAYMENT_SETUP_COMPLETED'
            );
        end if;
end;
$$;

create table if not exists "user_payment_setup_state"(
    id varchar constraint pk_user_payment_setup_state primary key default uuid_generate_v4(),
    progression_status user_payment_setup_status not null,
    timestamp timestamp without time zone,
    execution_type execution_type not null,
    user_id varchar,
    constraint fk_user_payment_setup_state foreign key (user_id) references "user"(id)
);