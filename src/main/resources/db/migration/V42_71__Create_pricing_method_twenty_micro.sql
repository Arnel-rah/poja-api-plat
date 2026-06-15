do
$$
    begin
        if not exists(SELECT e.enumlabel
                      FROM pg_type t
                               JOIN pg_enum e ON t.oid = e.enumtypid
                      WHERE t.typname = 'pricing_method'
                        AND e.enumlabel = 'TWENTY_MICRO') then
            alter type "pricing_method" add value 'TWENTY_MICRO' after 'TEN_MICRO';
        end if;
    end
$$;
