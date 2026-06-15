insert into "application_template" (id, name, description, repository_url, demo_url, with_custom_config)
values ('at_1', 'hello-world', 'deploy hello-world template', 'https://github.com/poja/hello-world-template',
        'https://hello-world-template-demo.on.aws', true),
       ('at_2', 'thymeleaf', 'deploy thymeleaf template', 'https://github.com/poja/thymeleaf-template', null, false);