create table if not exists user_billing_discount
(
    id             varchar
    constraint pk_user_billing_discount primary key default uuid_generate_v4(),
    creation_datetime timestamp without time zone default current_timestamp,
    amount_in_usd        numeric,
    description varchar,
    month varchar,
    year integer,
    user_id        varchar not null,
    constraint fk_discounted_user foreign key (user_id) references "user" (id)
    );
