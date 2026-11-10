-- Fixed-amount promo codes (docs/architecture.md §8). Codes are stored in upper case; valid_to null means
-- the code does not expire.
create table promo_codes (
    id               uuid           primary key default gen_random_uuid(),
    code             varchar(30)    not null,
    discount_amount  numeric(10, 2) not null,
    currency         char(3)        not null default 'LKR',
    valid_from       timestamptz    not null,
    valid_to         timestamptz,
    active           boolean        not null default true,
    created_at       timestamptz    not null default now(),
    updated_at       timestamptz,
    constraint uq_promo_codes_code unique (code),
    constraint ck_promo_codes_code check (code ~ '^[A-Z0-9_-]{1,30}$'),
    constraint ck_promo_codes_amount check (discount_amount > 0),
    constraint ck_promo_codes_currency check (currency = 'LKR'),
    constraint ck_promo_codes_validity check (valid_to is null or valid_to > valid_from)
);
