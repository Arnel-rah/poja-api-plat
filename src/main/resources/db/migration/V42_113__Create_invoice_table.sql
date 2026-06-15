create table if not exists "invoice"
(
    id             varchar
        constraint pk_invoice primary key default uuid_generate_v4(),
    amount_in_usd        numeric,
    invoice_id     varchar,
    status invoice_status         default 'OPEN'::invoice_status,
    invoice_url    varchar,
    user_id        varchar not null,
    constraint fk_invoiced_user foreign key (user_id) references "user" (id)
);