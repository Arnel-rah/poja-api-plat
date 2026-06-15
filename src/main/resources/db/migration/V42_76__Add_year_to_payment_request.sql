alter table "payment_request"
    add column if not exists "year" integer;
update "payment_request" pr
set year=extract('year' from pr.request_instant)
where year is null;