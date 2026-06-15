alter table if exists "user_payment_request"
    add column if not exists "discount_amount" numeric default 0 not null;
