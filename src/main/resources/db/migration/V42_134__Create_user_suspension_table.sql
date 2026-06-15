create table if not exists "user_suspension"
(
    id             varchar
    constraint pk_user_suspension primary key default uuid_generate_v4(),
    user_id        varchar not null,
    suspended_at timestamp without time zone,
    suspension_reason varchar,
    constraint fk_suspended_user foreign key (user_id) references "user" (id)
    );
