-- Hesabyar central license registry (Supabase/PostgreSQL)
-- One license may be bound once to one normalized email address.
-- Every activated license is valid for exactly 365 days from first activation.
-- Reinstall/reactivation with the same email + same license remains valid only
-- until the original expiry date; reinstall does not reset the one-year term.

create table if not exists public.licenses (
  serial integer primary key check (serial between 1 and 1800),
  license_key_hash text not null unique,
  status text not null default 'unused' check (status in ('unused','active','revoked')),
  bound_email text,
  activated_at timestamptz,
  expires_at timestamptz,
  last_seen_at timestamptz,
  activation_count integer not null default 0,
  last_app_version text,
  created_at timestamptz not null default now(),
  updated_at timestamptz not null default now(),
  constraint active_requires_email check (status <> 'active' or bound_email is not null),
  constraint active_requires_expiry check (status <> 'active' or expires_at is not null)
);

-- Safe migration for projects where the table already exists.
alter table public.licenses add column if not exists expires_at timestamptz;

create index if not exists licenses_bound_email_idx on public.licenses (lower(bound_email));
create index if not exists licenses_status_idx on public.licenses (status);
create index if not exists licenses_expires_at_idx on public.licenses (expires_at);

-- The mobile app must never query this table directly.
-- Only the Edge Function uses the service-role key.
alter table public.licenses enable row level security;

-- Intentionally no anon/authenticated policies.
-- This leaves direct public table access blocked while service_role still works.

create or replace function public.touch_updated_at()
returns trigger
language plpgsql
as $$
begin
  new.updated_at = now();
  return new;
end;
$$;

drop trigger if exists licenses_touch_updated_at on public.licenses;
create trigger licenses_touch_updated_at
before update on public.licenses
for each row execute function public.touch_updated_at();
