create table "organization_invite"
(
    id           varchar
        constraint pk_organization_invite primary key default uuid_generate_v4(),
    inviter_org  varchar,
    invited_user varchar,
    status organization_invite_status default 'PENDING',
    creation_datetime                 timestamp without time zone default now(),
    constraint fk_invite_invited_user foreign key (invited_user) references "user" ("id"),
    constraint fk_invite_inviter_org foreign key (inviter_org) references "organization" ("id")
);
