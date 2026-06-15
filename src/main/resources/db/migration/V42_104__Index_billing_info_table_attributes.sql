create index idx_billing_app on billing_info(app_id);
create index idx_billing_env on billing_info(env_id);
create index idx_billing_user on billing_info(user_id);
alter table billing_info
    add constraint fk_billing_org
        foreign key (org_id)
            references organization(id);
create index idx_billing_org on billing_info(org_id);
