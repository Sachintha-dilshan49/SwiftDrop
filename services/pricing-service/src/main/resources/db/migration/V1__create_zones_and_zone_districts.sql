-- Delivery zones (docs/architecture.md §8). parcel-service stores the zone code of each parcel, so codes
-- are never reused: a zone that is no longer served is set inactive instead of being deleted.
create table zones (
    id          uuid          primary key default gen_random_uuid(),
    code        varchar(30)   not null,
    name        varchar(100)  not null,
    zone_group  varchar(20)   not null,
    active      boolean       not null default true,
    created_at  timestamptz   not null default now(),
    updated_at  timestamptz,
    constraint uq_zones_code unique (code),
    constraint ck_zones_code check (code ~ '^[A-Z][A-Z_]{0,29}$'),
    constraint ck_zones_group check (zone_group in ('COLOMBO', 'SUBURBS', 'INTER_CITY'))
);

create index idx_zones_active on zones (active);

-- Areas a zone covers, shown to customers. The unique constraint also indexes zone_id (leading column).
create table zone_districts (
    id             uuid          primary key default gen_random_uuid(),
    zone_id        uuid          not null references zones (id) on delete cascade,
    district_name  varchar(100)  not null,
    created_at     timestamptz   not null default now(),
    updated_at     timestamptz,
    constraint uq_zone_districts_zone_district unique (zone_id, district_name)
);
