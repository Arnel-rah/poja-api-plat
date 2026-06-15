do
$$
    begin
        if not exists(select from pg_type where typname = 'user_status') then
            create type user_status as enum ('ACTIVE', 'SUSPENDED');
        end if;
    end
$$;
