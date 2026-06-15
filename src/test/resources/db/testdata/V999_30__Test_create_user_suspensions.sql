insert into "user_suspension" (id, user_id, suspension_reason, suspended_at)
values ('sus1_id', 'recsus_id', 'admin: first suspension', now() + interval '10 minutes' - interval '2 days');
