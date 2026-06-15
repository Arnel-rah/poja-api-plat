do
$$
    begin
        if not exists(select from pg_type where typname = 'env_status') then
            create type env_status as enum ('ACTIVE', 'SUSPENDED');
        end if;
    end
$$;
