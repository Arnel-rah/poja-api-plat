alter table if exists "application"
    add column "imported" boolean not null default false;
