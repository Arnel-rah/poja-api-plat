do
$$
begin
  if
    not exists(select from pg_type where typname = 'app_installation_repository_selection') then
    create type app_installation_repository_selection as enum (
                  'ALL',
                  'SELECTED'
    );
    end if;
end
$$;

alter table "app_installation"
    add column repository_selection app_installation_repository_selection;
