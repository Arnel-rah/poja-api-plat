alter table application
    add constraint fk_organization
        foreign key (org_id)
            references organization(id);
create index idx_application_org_id on application(org_id);
create index idx_application_user_id on application(id_user);
create index idx_application_installation_id on application(installation_id);
