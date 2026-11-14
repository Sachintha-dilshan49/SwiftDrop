--
-- PostgreSQL database dump
--

\restrict Bf6WjGu76tp2yj9PdvMudEp91WVbEnzkwmUUGR1N5vlzxClUE6EepAarBqNMHAh

-- Dumped from database version 16.16
-- Dumped by pg_dump version 16.16

SET statement_timeout = 0;
SET lock_timeout = 0;
SET idle_in_transaction_session_timeout = 0;
SET client_encoding = 'UTF8';
SET standard_conforming_strings = on;
SELECT pg_catalog.set_config('search_path', '', false);
SET check_function_bodies = false;
SET xmloption = content;
SET client_min_messages = warning;
SET row_security = off;

ALTER TABLE IF EXISTS ONLY public.zone_districts DROP CONSTRAINT IF EXISTS zone_districts_zone_id_fkey;
DROP INDEX IF EXISTS public.idx_zones_active;
DROP INDEX IF EXISTS public.flyway_schema_history_s_idx;
ALTER TABLE IF EXISTS ONLY public.zones DROP CONSTRAINT IF EXISTS zones_pkey;
ALTER TABLE IF EXISTS ONLY public.zone_districts DROP CONSTRAINT IF EXISTS zone_districts_pkey;
ALTER TABLE IF EXISTS ONLY public.zones DROP CONSTRAINT IF EXISTS uq_zones_code;
ALTER TABLE IF EXISTS ONLY public.zone_districts DROP CONSTRAINT IF EXISTS uq_zone_districts_zone_district;
ALTER TABLE IF EXISTS ONLY public.promo_codes DROP CONSTRAINT IF EXISTS uq_promo_codes_code;
ALTER TABLE IF EXISTS ONLY public.pricing_rules DROP CONSTRAINT IF EXISTS uq_pricing_rules_type_key;
ALTER TABLE IF EXISTS ONLY public.promo_codes DROP CONSTRAINT IF EXISTS promo_codes_pkey;
ALTER TABLE IF EXISTS ONLY public.pricing_rules DROP CONSTRAINT IF EXISTS pricing_rules_pkey;
ALTER TABLE IF EXISTS ONLY public.flyway_schema_history DROP CONSTRAINT IF EXISTS flyway_schema_history_pk;
DROP TABLE IF EXISTS public.zones;
DROP TABLE IF EXISTS public.zone_districts;
DROP TABLE IF EXISTS public.promo_codes;
DROP TABLE IF EXISTS public.pricing_rules;
DROP TABLE IF EXISTS public.flyway_schema_history;
SET default_tablespace = '';

SET default_table_access_method = heap;

--
-- Name: flyway_schema_history; Type: TABLE; Schema: public; Owner: pricing
--

CREATE TABLE public.flyway_schema_history (
    installed_rank integer NOT NULL,
    version character varying(50),
    description character varying(200) NOT NULL,
    type character varying(20) NOT NULL,
    script character varying(1000) NOT NULL,
    checksum integer,
    installed_by character varying(100) NOT NULL,
    installed_on timestamp without time zone DEFAULT now() NOT NULL,
    execution_time integer NOT NULL,
    success boolean NOT NULL
);


ALTER TABLE public.flyway_schema_history OWNER TO pricing;

--
-- Name: pricing_rules; Type: TABLE; Schema: public; Owner: pricing
--

CREATE TABLE public.pricing_rules (
    id uuid DEFAULT gen_random_uuid() NOT NULL,
    rule_type character varying(20) NOT NULL,
    rule_key character varying(30) NOT NULL,
    amount numeric(10,2) NOT NULL,
    currency character(3) DEFAULT 'LKR'::bpchar NOT NULL,
    max_weight_kg numeric(6,2),
    active boolean DEFAULT true NOT NULL,
    created_at timestamp with time zone DEFAULT now() NOT NULL,
    updated_at timestamp with time zone,
    CONSTRAINT ck_pricing_rules_amount CHECK ((amount >= (0)::numeric)),
    CONSTRAINT ck_pricing_rules_currency CHECK ((currency = 'LKR'::bpchar)),
    CONSTRAINT ck_pricing_rules_key CHECK (((((rule_type)::text = 'ROUTE_BASE'::text) AND ((rule_key)::text = ANY ((ARRAY['INTER_CITY'::character varying, 'SAME_ZONE'::character varying, 'NEIGHBOURING'::character varying, 'COLOMBO_SUBURBS'::character varying])::text[]))) OR (((rule_type)::text = 'EXTRA_KG'::text) AND ((rule_key)::text = 'PER_KG'::text)) OR (((rule_type)::text = 'SIZE'::text) AND ((rule_key)::text = ANY ((ARRAY['SMALL'::character varying, 'MEDIUM'::character varying, 'LARGE'::character varying, 'EXTRA_LARGE'::character varying])::text[]))) OR (((rule_type)::text = 'SPEED'::text) AND ((rule_key)::text = ANY ((ARRAY['EXPRESS'::character varying, 'STANDARD'::character varying, 'NEXT_DAY'::character varying])::text[]))))),
    CONSTRAINT ck_pricing_rules_max_weight CHECK (((((rule_type)::text = 'SIZE'::text) AND (max_weight_kg IS NOT NULL) AND (max_weight_kg > (0)::numeric)) OR (((rule_type)::text <> 'SIZE'::text) AND (max_weight_kg IS NULL)))),
    CONSTRAINT ck_pricing_rules_type CHECK (((rule_type)::text = ANY ((ARRAY['ROUTE_BASE'::character varying, 'EXTRA_KG'::character varying, 'SIZE'::character varying, 'SPEED'::character varying])::text[])))
);


ALTER TABLE public.pricing_rules OWNER TO pricing;

--
-- Name: promo_codes; Type: TABLE; Schema: public; Owner: pricing
--

CREATE TABLE public.promo_codes (
    id uuid DEFAULT gen_random_uuid() NOT NULL,
    code character varying(30) NOT NULL,
    discount_amount numeric(10,2) NOT NULL,
    currency character(3) DEFAULT 'LKR'::bpchar NOT NULL,
    valid_from timestamp with time zone NOT NULL,
    valid_to timestamp with time zone,
    active boolean DEFAULT true NOT NULL,
    created_at timestamp with time zone DEFAULT now() NOT NULL,
    updated_at timestamp with time zone,
    CONSTRAINT ck_promo_codes_amount CHECK ((discount_amount > (0)::numeric)),
    CONSTRAINT ck_promo_codes_code CHECK (((code)::text ~ '^[A-Z0-9_-]{1,30}$'::text)),
    CONSTRAINT ck_promo_codes_currency CHECK ((currency = 'LKR'::bpchar)),
    CONSTRAINT ck_promo_codes_validity CHECK (((valid_to IS NULL) OR (valid_to > valid_from)))
);


ALTER TABLE public.promo_codes OWNER TO pricing;

--
-- Name: zone_districts; Type: TABLE; Schema: public; Owner: pricing
--

CREATE TABLE public.zone_districts (
    id uuid DEFAULT gen_random_uuid() NOT NULL,
    zone_id uuid NOT NULL,
    district_name character varying(100) NOT NULL,
    created_at timestamp with time zone DEFAULT now() NOT NULL,
    updated_at timestamp with time zone
);


ALTER TABLE public.zone_districts OWNER TO pricing;

--
-- Name: zones; Type: TABLE; Schema: public; Owner: pricing
--

CREATE TABLE public.zones (
    id uuid DEFAULT gen_random_uuid() NOT NULL,
    code character varying(30) NOT NULL,
    name character varying(100) NOT NULL,
    zone_group character varying(20) NOT NULL,
    active boolean DEFAULT true NOT NULL,
    created_at timestamp with time zone DEFAULT now() NOT NULL,
    updated_at timestamp with time zone,
    CONSTRAINT ck_zones_code CHECK (((code)::text ~ '^[A-Z][A-Z_]{0,29}$'::text)),
    CONSTRAINT ck_zones_group CHECK (((zone_group)::text = ANY ((ARRAY['COLOMBO'::character varying, 'SUBURBS'::character varying, 'INTER_CITY'::character varying])::text[])))
);


ALTER TABLE public.zones OWNER TO pricing;

--
-- Data for Name: flyway_schema_history; Type: TABLE DATA; Schema: public; Owner: pricing
--

COPY public.flyway_schema_history (installed_rank, version, description, type, script, checksum, installed_by, installed_on, execution_time, success) FROM stdin;
1	1	create zones and zone districts	SQL	V1__create_zones_and_zone_districts.sql	1335606024	pricing	2026-11-14 14:11:37.427179	54	t
2	2	create pricing rules	SQL	V2__create_pricing_rules.sql	1917403634	pricing	2026-11-14 14:11:37.522253	18	t
3	3	create promo codes	SQL	V3__create_promo_codes.sql	-1131332078	pricing	2026-11-14 14:11:37.564262	18	t
4	4	seed zones	SQL	V4__seed_zones.sql	-1812363596	pricing	2026-11-14 14:11:37.59234	2	t
5	5	seed pricing rules and promo codes	SQL	V5__seed_pricing_rules_and_promo_codes.sql	-1824865431	pricing	2026-11-14 14:11:37.600249	1	t
\.


--
-- Data for Name: pricing_rules; Type: TABLE DATA; Schema: public; Owner: pricing
--

COPY public.pricing_rules (id, rule_type, rule_key, amount, currency, max_weight_kg, active, created_at, updated_at) FROM stdin;
45f8ca46-bd14-414a-a5a7-72872ead66a0	ROUTE_BASE	INTER_CITY	650.00	LKR	\N	t	2026-11-14 14:11:37.601845+00	\N
8cb0742b-648d-4061-85fa-d7b83c03e710	ROUTE_BASE	SAME_ZONE	300.00	LKR	\N	t	2026-11-14 14:11:37.601845+00	\N
a0363053-c5b4-4a8d-af77-c3d843f818fe	ROUTE_BASE	NEIGHBOURING	350.00	LKR	\N	t	2026-11-14 14:11:37.601845+00	\N
e10cb075-b063-43b8-9107-56ebe32c1cc1	ROUTE_BASE	COLOMBO_SUBURBS	400.00	LKR	\N	t	2026-11-14 14:11:37.601845+00	\N
70597cf0-6d97-401f-a89e-fe0180b0ef03	EXTRA_KG	PER_KG	40.00	LKR	\N	t	2026-11-14 14:11:37.601845+00	\N
50f96265-a545-4753-b38f-508d4aaaf16b	SIZE	SMALL	0.00	LKR	5.00	t	2026-11-14 14:11:37.601845+00	\N
4d8aa53d-a878-41a8-a169-03b5b09b766e	SIZE	MEDIUM	100.00	LKR	10.00	t	2026-11-14 14:11:37.601845+00	\N
91ee508d-c59f-436f-8852-5eebcea0dea7	SIZE	LARGE	250.00	LKR	20.00	t	2026-11-14 14:11:37.601845+00	\N
25e11bfc-60ac-4dac-90fc-cb4b9137f570	SIZE	EXTRA_LARGE	500.00	LKR	40.00	t	2026-11-14 14:11:37.601845+00	\N
50f76a4b-16e2-4543-abd3-f0d669185c55	SPEED	EXPRESS	100.00	LKR	\N	t	2026-11-14 14:11:37.601845+00	\N
63e1a07b-4812-4e74-bbf4-21d0362a3d89	SPEED	STANDARD	0.00	LKR	\N	t	2026-11-14 14:11:37.601845+00	\N
3b8c1905-1208-492c-aa0a-b413762b481a	SPEED	NEXT_DAY	0.00	LKR	\N	t	2026-11-14 14:11:37.601845+00	\N
\.


--
-- Data for Name: promo_codes; Type: TABLE DATA; Schema: public; Owner: pricing
--

COPY public.promo_codes (id, code, discount_amount, currency, valid_from, valid_to, active, created_at, updated_at) FROM stdin;
c728915f-7a6b-4bd9-94d5-8285efc28ac0	DROP100	100.00	LKR	2026-01-01 00:00:00+00	\N	t	2026-11-14 14:11:37.601845+00	\N
0c15a419-f5b8-41d6-ad30-d19154b0bcdf	WELCOME50	50.00	LKR	2026-08-16 14:11:48+00	\N	t	2026-08-09 14:11:48+00	\N
71c60446-2e83-415b-b366-3ea4e55e70cf	AVURUDU2026	150.00	LKR	2026-04-13 14:11:48+00	2026-05-08 14:11:48+00	t	2026-04-06 14:11:48+00	\N
6576a48b-e7df-4291-8410-60b1b78b6502	XMAS250	250.00	LKR	2026-12-14 14:11:48+00	2026-12-29 14:11:48+00	t	2026-12-07 14:11:48+00	\N
11bcfec6-939b-46ea-8729-af5d5313fe90	STAFFTEST	500.00	LKR	2026-09-15 14:11:48+00	\N	f	2026-09-08 14:11:48+00	\N
\.


--
-- Data for Name: zone_districts; Type: TABLE DATA; Schema: public; Owner: pricing
--

COPY public.zone_districts (id, zone_id, district_name, created_at, updated_at) FROM stdin;
259bef39-be68-4ff3-86c0-6697303a2fbb	c38c801b-039d-4cd4-a23c-a2acbb165ef0	Colombo 03	2026-11-14 14:11:37.596187+00	\N
4f1c85bf-2597-4a31-9d1f-4e6f0c622bd4	c38c801b-039d-4cd4-a23c-a2acbb165ef0	Colombo 02	2026-11-14 14:11:37.596187+00	\N
30f56233-ac00-4cac-b174-2ff8b26442a2	c38c801b-039d-4cd4-a23c-a2acbb165ef0	Colombo 01	2026-11-14 14:11:37.596187+00	\N
4aaec64b-1f9f-49eb-8e64-bd14a8c264e8	fc200b66-f004-4c05-8c3e-5badfa6ce18f	Colombo 06	2026-11-14 14:11:37.596187+00	\N
a60377c4-d4c9-4cc7-912e-58659ad54ef7	fc200b66-f004-4c05-8c3e-5badfa6ce18f	Colombo 05	2026-11-14 14:11:37.596187+00	\N
144c8572-0463-4b6d-838e-9ca5b6df54a4	fc200b66-f004-4c05-8c3e-5badfa6ce18f	Colombo 04	2026-11-14 14:11:37.596187+00	\N
65280306-79c1-4b6b-ab19-dc090e090d86	6c48b329-dd5f-48e2-a259-712d127c157c	Colombo 15	2026-11-14 14:11:37.596187+00	\N
b30b8e37-8305-45b4-a54a-0c32ec2468a4	6c48b329-dd5f-48e2-a259-712d127c157c	Colombo 14	2026-11-14 14:11:37.596187+00	\N
509304bb-7ab7-42e0-9a6c-74c2bc79cb74	6c48b329-dd5f-48e2-a259-712d127c157c	Colombo 13	2026-11-14 14:11:37.596187+00	\N
b942c7cd-ab77-4053-bea7-00dc08fc5104	6c48b329-dd5f-48e2-a259-712d127c157c	Colombo 12	2026-11-14 14:11:37.596187+00	\N
5d89f5c4-6ef6-46c1-815a-594b4b8ad71a	6c48b329-dd5f-48e2-a259-712d127c157c	Colombo 11	2026-11-14 14:11:37.596187+00	\N
40d6a637-8d13-4b93-8fba-df22efd64f72	6c48b329-dd5f-48e2-a259-712d127c157c	Colombo 10	2026-11-14 14:11:37.596187+00	\N
73b13f37-e0f7-4746-9c2e-08a0e659213b	6c48b329-dd5f-48e2-a259-712d127c157c	Colombo 09	2026-11-14 14:11:37.596187+00	\N
6b4e129c-cda9-4483-afec-2d23ccefbf3c	6c48b329-dd5f-48e2-a259-712d127c157c	Colombo 08	2026-11-14 14:11:37.596187+00	\N
1b6d52ab-127f-47d0-a232-332d607d0609	6c48b329-dd5f-48e2-a259-712d127c157c	Colombo 07	2026-11-14 14:11:37.596187+00	\N
7ef0105a-0bcc-49be-bf55-b94b988ee121	d11c3bc4-3a16-4e3e-ae7b-743cac82ecff	Rajagiriya	2026-11-14 14:11:37.596187+00	\N
88f6f8e9-e866-48a8-866d-7e77bb74ef22	d11c3bc4-3a16-4e3e-ae7b-743cac82ecff	Kotte	2026-11-14 14:11:37.596187+00	\N
9c84171b-5453-41df-b323-5b67a5b4cc50	f807342d-8f10-404b-bc06-f7220dd90762	Maharagama	2026-11-14 14:11:37.596187+00	\N
5e73c0aa-397c-47f0-9660-6501381a1fc7	f807342d-8f10-404b-bc06-f7220dd90762	Nugegoda	2026-11-14 14:11:37.596187+00	\N
c5f95e1c-a51b-465b-8f90-cbd1d60b55a6	13a2c53c-d82f-4da0-8685-39c86628e236	Mount Lavinia	2026-11-14 14:11:37.596187+00	\N
bef257b9-a432-44ba-aae8-51395fc788f2	13a2c53c-d82f-4da0-8685-39c86628e236	Dehiwala	2026-11-14 14:11:37.596187+00	\N
45ececad-d74e-428f-866c-5d138f9c22ce	29f32d0f-626d-44a6-a22a-5d714ffe3fe1	Kandy	2026-11-14 14:11:37.596187+00	\N
3b70dddd-b24e-45af-9ab1-217bd8738dfd	128e705f-4cc4-49fa-8e3a-e1af4dd4e734	Galle	2026-11-14 14:11:37.596187+00	\N
5822281e-e322-4c06-9a13-b5b8b382c724	5dda6082-9e47-49f3-87b3-2bd6d04006b9	Negombo	2026-11-14 14:11:37.596187+00	\N
\.


--
-- Data for Name: zones; Type: TABLE DATA; Schema: public; Owner: pricing
--

COPY public.zones (id, code, name, zone_group, active, created_at, updated_at) FROM stdin;
c38c801b-039d-4cd4-a23c-a2acbb165ef0	COLOMBO_CENTRAL	Colombo Central	COLOMBO	t	2026-11-14 14:11:37.596187+00	\N
fc200b66-f004-4c05-8c3e-5badfa6ce18f	COLOMBO_SOUTH	Colombo South	COLOMBO	t	2026-11-14 14:11:37.596187+00	\N
6c48b329-dd5f-48e2-a259-712d127c157c	COLOMBO_NORTH	Colombo North	COLOMBO	t	2026-11-14 14:11:37.596187+00	\N
d11c3bc4-3a16-4e3e-ae7b-743cac82ecff	KOTTE_RAJAGIRIYA	Kotte & Rajagiriya	SUBURBS	t	2026-11-14 14:11:37.596187+00	\N
f807342d-8f10-404b-bc06-f7220dd90762	NUGEGODA_MAHARAGAMA	Nugegoda & Maharagama	SUBURBS	t	2026-11-14 14:11:37.596187+00	\N
13a2c53c-d82f-4da0-8685-39c86628e236	DEHIWALA_MT_LAVINIA	Dehiwala & Mt Lavinia	SUBURBS	t	2026-11-14 14:11:37.596187+00	\N
29f32d0f-626d-44a6-a22a-5d714ffe3fe1	KANDY	Kandy	INTER_CITY	t	2026-11-14 14:11:37.596187+00	\N
128e705f-4cc4-49fa-8e3a-e1af4dd4e734	GALLE	Galle	INTER_CITY	t	2026-11-14 14:11:37.596187+00	\N
5dda6082-9e47-49f3-87b3-2bd6d04006b9	NEGOMBO	Negombo	INTER_CITY	t	2026-11-14 14:11:37.596187+00	\N
\.


--
-- Name: flyway_schema_history flyway_schema_history_pk; Type: CONSTRAINT; Schema: public; Owner: pricing
--

ALTER TABLE ONLY public.flyway_schema_history
    ADD CONSTRAINT flyway_schema_history_pk PRIMARY KEY (installed_rank);


--
-- Name: pricing_rules pricing_rules_pkey; Type: CONSTRAINT; Schema: public; Owner: pricing
--

ALTER TABLE ONLY public.pricing_rules
    ADD CONSTRAINT pricing_rules_pkey PRIMARY KEY (id);


--
-- Name: promo_codes promo_codes_pkey; Type: CONSTRAINT; Schema: public; Owner: pricing
--

ALTER TABLE ONLY public.promo_codes
    ADD CONSTRAINT promo_codes_pkey PRIMARY KEY (id);


--
-- Name: pricing_rules uq_pricing_rules_type_key; Type: CONSTRAINT; Schema: public; Owner: pricing
--

ALTER TABLE ONLY public.pricing_rules
    ADD CONSTRAINT uq_pricing_rules_type_key UNIQUE (rule_type, rule_key);


--
-- Name: promo_codes uq_promo_codes_code; Type: CONSTRAINT; Schema: public; Owner: pricing
--

ALTER TABLE ONLY public.promo_codes
    ADD CONSTRAINT uq_promo_codes_code UNIQUE (code);


--
-- Name: zone_districts uq_zone_districts_zone_district; Type: CONSTRAINT; Schema: public; Owner: pricing
--

ALTER TABLE ONLY public.zone_districts
    ADD CONSTRAINT uq_zone_districts_zone_district UNIQUE (zone_id, district_name);


--
-- Name: zones uq_zones_code; Type: CONSTRAINT; Schema: public; Owner: pricing
--

ALTER TABLE ONLY public.zones
    ADD CONSTRAINT uq_zones_code UNIQUE (code);


--
-- Name: zone_districts zone_districts_pkey; Type: CONSTRAINT; Schema: public; Owner: pricing
--

ALTER TABLE ONLY public.zone_districts
    ADD CONSTRAINT zone_districts_pkey PRIMARY KEY (id);


--
-- Name: zones zones_pkey; Type: CONSTRAINT; Schema: public; Owner: pricing
--

ALTER TABLE ONLY public.zones
    ADD CONSTRAINT zones_pkey PRIMARY KEY (id);


--
-- Name: flyway_schema_history_s_idx; Type: INDEX; Schema: public; Owner: pricing
--

CREATE INDEX flyway_schema_history_s_idx ON public.flyway_schema_history USING btree (success);


--
-- Name: idx_zones_active; Type: INDEX; Schema: public; Owner: pricing
--

CREATE INDEX idx_zones_active ON public.zones USING btree (active);


--
-- Name: zone_districts zone_districts_zone_id_fkey; Type: FK CONSTRAINT; Schema: public; Owner: pricing
--

ALTER TABLE ONLY public.zone_districts
    ADD CONSTRAINT zone_districts_zone_id_fkey FOREIGN KEY (zone_id) REFERENCES public.zones(id) ON DELETE CASCADE;


--
-- PostgreSQL database dump complete
--

\unrestrict Bf6WjGu76tp2yj9PdvMudEp91WVbEnzkwmUUGR1N5vlzxClUE6EepAarBqNMHAh

