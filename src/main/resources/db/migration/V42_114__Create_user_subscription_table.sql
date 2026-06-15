create table if not exists user_subscription
(
    id                          varchar
        constraint pk_user_subscription primary key                  default uuid_generate_v4(),
    user_id                     varchar                     not null,
    offer_id                    varchar                     not null,
    constraint fk_subscribed_user foreign key ("user_id") references "user" ("id"),
    constraint fk_subscribed_subscription foreign key (offer_id) references "offer" ("id"),
    subscription_begin_datetime timestamp without time zone not null default now(),
    subscription_end_datetime   timestamp without time zone,
    invoice_id varchar,
    constraint fk_subscription_invoice foreign key ("invoice_id") references "invoice"("id")
);

create index if not exists idx_monthly_subscription_user on user_subscription (user_id);
create index if not exists idx_monthly_subscription_subscription on user_subscription (offer_id);
