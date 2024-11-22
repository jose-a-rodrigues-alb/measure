-- migrate:up
create table if not exists public.allowed_email_domains (
    id uuid primary key not null default gen_random_uuid(),
    domain varchar(256) not null
);

comment on column public.allowed_email_domains.id IS 'unique id for each domain';
comment on column public.allowed_email_domains.domain IS 'email fqd';

-- migrate:down
drop table if exists public.allowed_email_domains;