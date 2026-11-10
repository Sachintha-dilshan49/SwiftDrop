-- Reference data: every amount in docs/architecture.md §8, and the DROP100 promo code.
insert into pricing_rules (rule_type, rule_key, amount, max_weight_kg) values
    ('ROUTE_BASE', 'INTER_CITY',      650.00, null),
    ('ROUTE_BASE', 'SAME_ZONE',       300.00, null),
    ('ROUTE_BASE', 'NEIGHBOURING',    350.00, null),
    ('ROUTE_BASE', 'COLOMBO_SUBURBS', 400.00, null),
    ('EXTRA_KG',   'PER_KG',           40.00, null),
    ('SIZE',       'SMALL',             0.00,  5.00),
    ('SIZE',       'MEDIUM',          100.00, 10.00),
    ('SIZE',       'LARGE',           250.00, 20.00),
    ('SIZE',       'EXTRA_LARGE',     500.00, 40.00),
    ('SPEED',      'EXPRESS',         100.00, null),
    ('SPEED',      'STANDARD',          0.00, null),
    ('SPEED',      'NEXT_DAY',          0.00, null);

insert into promo_codes (code, discount_amount, valid_from, valid_to) values
    ('DROP100', 100.00, timestamptz '2026-01-01 00:00:00+00', null);
