alter table "user" alter column last_connection drop default;
alter table "user" alter column last_connection drop not null;