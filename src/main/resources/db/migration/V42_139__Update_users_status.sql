begin;

update "user"
set status            = 'ACTIVE',
    status_updated_at = now(),
    status_checked_at = now()
where status = 'UNDER_MODIFICATION';

commit;