-- Every amount the fee is made of (docs/architecture.md §8), one row per rule. Admins edit the amount and
-- the active flag; the set of rules itself only changes through a migration.
--   ROUTE_BASE  key = route type                  amount = base fee
--   EXTRA_KG    key = PER_KG                      amount = fee per started kg after the first
--   SIZE        key = parcel size                 amount = surcharge, max_weight_kg = heaviest parcel
--   SPEED       key = delivery speed              amount = surcharge
create table pricing_rules (
    id             uuid           primary key default gen_random_uuid(),
    rule_type      varchar(20)    not null,
    rule_key       varchar(30)    not null,
    amount         numeric(10, 2) not null,
    currency       char(3)        not null default 'LKR',
    max_weight_kg  numeric(6, 2),
    active         boolean        not null default true,
    created_at     timestamptz    not null default now(),
    updated_at     timestamptz,
    constraint uq_pricing_rules_type_key unique (rule_type, rule_key),
    constraint ck_pricing_rules_type check (rule_type in ('ROUTE_BASE', 'EXTRA_KG', 'SIZE', 'SPEED')),
    constraint ck_pricing_rules_key check (
        (rule_type = 'ROUTE_BASE' and rule_key in ('INTER_CITY', 'SAME_ZONE', 'NEIGHBOURING', 'COLOMBO_SUBURBS'))
        or (rule_type = 'EXTRA_KG' and rule_key = 'PER_KG')
        or (rule_type = 'SIZE' and rule_key in ('SMALL', 'MEDIUM', 'LARGE', 'EXTRA_LARGE'))
        or (rule_type = 'SPEED' and rule_key in ('EXPRESS', 'STANDARD', 'NEXT_DAY'))),
    constraint ck_pricing_rules_amount check (amount >= 0),
    constraint ck_pricing_rules_currency check (currency = 'LKR'),
    constraint ck_pricing_rules_max_weight check (
        (rule_type = 'SIZE' and max_weight_kg is not null and max_weight_kg > 0)
        or (rule_type <> 'SIZE' and max_weight_kg is null))
);
