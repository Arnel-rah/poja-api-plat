insert into "app_installation"(id, user_id, gh_id, owner_github_login, "type", "avatar_url", "repository_selection")
values ('gh_app_install_1_id', 'joe-doe-id', 12344, 'joedoelogin1', 'User', 'http://testimage.com', 'ALL'),
       ('gh_app_install_2_id', 'jane_doe_id', 12346, 'janedoelogin1', 'Organization', 'http://testimage.com',
        'SELECTED'),
       ('gh_app_install_3_id', 'joe-doe-id', 123445, 'joedoelogin1', 'User', 'http://testimage.com', 'ALL'),
       ('gh_app_install_4_id', 'denis_ritchie_id', 123455, 'denisritchielogin1', 'User', 'http://testimage.com', 'ALL'),
       ('gh_app_install_5_id', 'recsus_id', 123456, 'recsuslogin1', 'User', 'http://testimage.com', 'ALL');
