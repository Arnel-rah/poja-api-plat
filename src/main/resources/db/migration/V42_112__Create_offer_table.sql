create table if not exists "offer"
(
    id                   varchar
        constraint pk_offer primary key default uuid_generate_v4(),
    name                 varchar not null,
    price_in_usd numeric,
    max_subscribers      integer not null,
    max_apps             integer not null
);