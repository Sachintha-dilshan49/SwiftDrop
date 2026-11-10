-- Reference data: the nine delivery zones (docs/architecture.md §8). The codes must match
-- frontend/src/features/booking/bookingOptions.ts.
insert into zones (code, name, zone_group) values
    ('COLOMBO_CENTRAL',     'Colombo Central',       'COLOMBO'),
    ('COLOMBO_SOUTH',       'Colombo South',         'COLOMBO'),
    ('COLOMBO_NORTH',       'Colombo North',         'COLOMBO'),
    ('KOTTE_RAJAGIRIYA',    'Kotte & Rajagiriya',    'SUBURBS'),
    ('NUGEGODA_MAHARAGAMA', 'Nugegoda & Maharagama', 'SUBURBS'),
    ('DEHIWALA_MT_LAVINIA', 'Dehiwala & Mt Lavinia', 'SUBURBS'),
    ('KANDY',               'Kandy',                 'INTER_CITY'),
    ('GALLE',               'Galle',                 'INTER_CITY'),
    ('NEGOMBO',             'Negombo',               'INTER_CITY');

insert into zone_districts (zone_id, district_name)
select z.id, d.district_name
from (values
    ('COLOMBO_CENTRAL',     'Colombo 01'),
    ('COLOMBO_CENTRAL',     'Colombo 02'),
    ('COLOMBO_CENTRAL',     'Colombo 03'),
    ('COLOMBO_SOUTH',       'Colombo 04'),
    ('COLOMBO_SOUTH',       'Colombo 05'),
    ('COLOMBO_SOUTH',       'Colombo 06'),
    ('COLOMBO_NORTH',       'Colombo 07'),
    ('COLOMBO_NORTH',       'Colombo 08'),
    ('COLOMBO_NORTH',       'Colombo 09'),
    ('COLOMBO_NORTH',       'Colombo 10'),
    ('COLOMBO_NORTH',       'Colombo 11'),
    ('COLOMBO_NORTH',       'Colombo 12'),
    ('COLOMBO_NORTH',       'Colombo 13'),
    ('COLOMBO_NORTH',       'Colombo 14'),
    ('COLOMBO_NORTH',       'Colombo 15'),
    ('KOTTE_RAJAGIRIYA',    'Kotte'),
    ('KOTTE_RAJAGIRIYA',    'Rajagiriya'),
    ('NUGEGODA_MAHARAGAMA', 'Nugegoda'),
    ('NUGEGODA_MAHARAGAMA', 'Maharagama'),
    ('DEHIWALA_MT_LAVINIA', 'Dehiwala'),
    ('DEHIWALA_MT_LAVINIA', 'Mount Lavinia'),
    ('KANDY',               'Kandy'),
    ('GALLE',               'Galle'),
    ('NEGOMBO',             'Negombo')
) as d (zone_code, district_name)
join zones z on z.code = d.zone_code;
