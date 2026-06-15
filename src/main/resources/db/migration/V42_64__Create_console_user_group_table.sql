create table if not exists "console_user_group"
(
    id      varchar not null primary key default uuid_generate_v4(),
    current boolean not null             default false,
    user_id varchar not null,
    name    varchar not null,
    constraint fk_console_user_group_user foreign key (user_id) references "user" (id)
);
