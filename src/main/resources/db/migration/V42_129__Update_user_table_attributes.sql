alter table if exists "user" add column if not exists latest_subscription_id varchar;
alter table if exists "user" add constraint fk_user_subscription foreign key (latest_subscription_id) references "user_subscription" (id)
