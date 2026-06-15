insert into "application_import"
    (id, app_name, github_repository_name, github_repository_http_url, user_id, org_id, app_installation_id)
values  ('import_1', 'to_import', 'dummy', 'https://github.com/user/dummy', 'joe-doe-id', 'org_1_id', 'gh_app_install_1_id'),
        ('import_2', 'to_import', 'dummy', 'https://github.com/user/dummy', 'jane_doe_id', 'org_2_id', 'gh_app_install_2_id'),
        ('import_3', 'to_import', 'dummy', 'https://github.com/user/dummy', 'denis_ritchie_id', 'org_3_id', 'gh_app_install_3_id'),
        ('import_4', 'to_import', 'dummy', 'https://github.com/user/dummy', 'noobie_id', 'org_4_id', 'gh_app_install_4_id');

insert into "application_import"
(id, app_name, github_repository_name, github_repository_http_url, user_id, org_id, app_installation_id)
values  ('import_5', 'to_import', 'dummy', 'https://github.com/user/dummy', 'joe-doe-id', 'org_5_id', 'gh_app_install_1_id'),
        ('import_6', 'to_import', 'dummy', 'https://github.com/user/dummy', 'joe-doe-id', 'org_1_id', 'gh_app_install_1_id');

insert into "application_import"
(id, app_name, github_repository_name, github_repository_http_url, user_id, org_id, app_installation_id, github_repository_id)
 values ('import_7', 'to_import', 'dummy', 'https://github.com/user/dummy', 'joe-doe-id', 'org_1_id', 'gh_app_install_1_id', 'gh_repository_1_id');