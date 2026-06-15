insert into "offer" (id, name, max_apps, price_in_usd)
values ('cb038529-dea0-43ab-b9bc-262ab668f150', 'premium', 10, 1) on conflict (id) do nothing;
