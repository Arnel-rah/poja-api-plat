create table if not exists "user_cost" (
    id varchar
       constraint pk_user_cost primary key default uuid_generate_v4(),
    user_id varchar,
    amount_usd numeric not null default 0,
    "month" varchar not null,
    "year" integer not null,
    updated_at timestamp without time zone,
    constraint fk_user_cost_user foreign key (user_id) references "user" (id),
    constraint unique_period_user unique (user_id, "month", "year")
);

