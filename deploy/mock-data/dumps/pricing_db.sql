--
-- PostgreSQL database dump
--

\restrict oujlGUTRdyX8DRiDIuPy8Afku5ifrAy7TefLtlqeqZe5B93yrccHlWO9VOiUzF7

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
1	1	create zones and zone districts	SQL	V1__create_zones_and_zone_districts.sql	1335606024	pricing	2026-11-14 10:16:56.675191	20	t
2	2	create pricing rules	SQL	V2__create_pricing_rules.sql	1917403634	pricing	2026-11-14 10:16:56.724643	10	t
3	3	create promo codes	SQL	V3__create_promo_codes.sql	-1131332078	pricing	2026-11-14 10:16:56.753604	10	t
4	4	seed zones	SQL	V4__seed_zones.sql	-1812363596	pricing	2026-11-14 10:16:56.772581	6	t
5	5	seed pricing rules and promo codes	SQL	V5__seed_pricing_rules_and_promo_codes.sql	-1824865431	pricing	2026-11-14 10:16:56.789036	3	t
\.


--
-- Data for Name: pricing_rules; Type: TABLE DATA; Schema: public; Owner: pricing
--

COPY public.pricing_rules (id, rule_type, rule_key, amount, currency, max_weight_kg, active, created_at, updated_at) FROM stdin;
7137c3c1-dcaf-41e4-9033-e7eba0b132ed	ROUTE_BASE	INTER_CITY	650.00	LKR	\N	t	2026-11-14 10:16:56.797621+00	\N
07eb31b2-9c10-4901-b955-d4b3cf015999	ROUTE_BASE	SAME_ZONE	300.00	LKR	\N	t	2026-11-14 10:16:56.797621+00	\N
db545de8-6d7a-4b57-b763-6c8ed31c3ef5	ROUTE_BASE	NEIGHBOURING	350.00	LKR	\N	t	2026-11-14 10:16:56.797621+00	\N
f0a56c4b-9c82-443d-99a5-f9a456fecbde	ROUTE_BASE	COLOMBO_SUBURBS	400.00	LKR	\N	t	2026-11-14 10:16:56.797621+00	\N
091d304f-ebfe-4ce6-951c-486563dbfde0	EXTRA_KG	PER_KG	40.00	LKR	\N	t	2026-11-14 10:16:56.797621+00	\N
094fc12c-d108-4685-8796-1c0e5e9ef53c	SIZE	SMALL	0.00	LKR	5.00	t	2026-11-14 10:16:56.797621+00	\N
fa61916e-ddbd-4af7-bc9c-4304e2320c3a	SIZE	MEDIUM	100.00	LKR	10.00	t	2026-11-14 10:16:56.797621+00	\N
39bff0f6-aed6-45d8-9093-4e2bbd7f3891	SIZE	LARGE	250.00	LKR	20.00	t	2026-11-14 10:16:56.797621+00	\N
03834c96-2646-4eda-a732-28cf31438f46	SIZE	EXTRA_LARGE	500.00	LKR	40.00	t	2026-11-14 10:16:56.797621+00	\N
1196fb4a-28ae-46b3-b529-bf6b387ad659	SPEED	EXPRESS	100.00	LKR	\N	t	2026-11-14 10:16:56.797621+00	\N
dfa68a72-30aa-452c-9a45-a51a9e140283	SPEED	STANDARD	0.00	LKR	\N	t	2026-11-14 10:16:56.797621+00	\N
69b89e9e-528e-434f-9734-fd8e1b974021	SPEED	NEXT_DAY	0.00	LKR	\N	t	2026-11-14 10:16:56.797621+00	\N
\.


--
-- Data for Name: promo_codes; Type: TABLE DATA; Schema: public; Owner: pricing
--

COPY public.promo_codes (id, code, discount_amount, currency, valid_from, valid_to, active, created_at, updated_at) FROM stdin;
1d24b17e-d18d-4465-b4bc-7875b0d90915	DROP100	100.00	LKR	2026-01-01 00:00:00+00	\N	t	2026-11-14 10:16:56.797621+00	\N
b3d052bb-556d-4c7e-9021-014ae9ba0ab7	WELCOME50	50.00	LKR	2026-08-16 10:17:09+00	\N	t	2026-08-09 10:17:09+00	\N
8774bea2-79cb-405b-9ca5-ecc0c8e2f5a3	AVURUDU2026	150.00	LKR	2026-04-13 10:17:09+00	2026-05-08 10:17:09+00	t	2026-04-06 10:17:09+00	\N
0eccbe32-65b4-46ec-9c44-20a81330d46c	XMAS250	250.00	LKR	2026-12-14 10:17:09+00	2026-12-29 10:17:09+00	t	2026-12-07 10:17:09+00	\N
b0e6b0ae-cd44-4b3c-a27a-875ba47e994b	STAFFTEST	500.00	LKR	2026-09-15 10:17:09+00	\N	f	2026-09-08 10:17:09+00	\N
\.


--
-- Data for Name: zone_districts; Type: TABLE DATA; Schema: public; Owner: pricing
--

COPY public.zone_districts (id, zone_id, district_name, created_at, updated_at) FROM stdin;
230423e7-e7e8-4b5f-b1e1-a5b344b163aa	fbf812ed-c47b-4b66-88a2-1dc90ffff260	Colombo 03	2026-11-14 10:16:56.77841+00	\N
fca8022c-a28d-44e2-a326-c13fe838a5fe	fbf812ed-c47b-4b66-88a2-1dc90ffff260	Colombo 02	2026-11-14 10:16:56.77841+00	\N
0caf043b-4add-4531-b9dd-96dd110a9d92	fbf812ed-c47b-4b66-88a2-1dc90ffff260	Colombo 01	2026-11-14 10:16:56.77841+00	\N
f241c27b-d841-4703-87ff-d71fa5c25f74	11d719b9-c253-4cad-a0cb-a97d639b2881	Colombo 06	2026-11-14 10:16:56.77841+00	\N
5af2ef13-ca6b-4048-bda5-29fd049357ce	11d719b9-c253-4cad-a0cb-a97d639b2881	Colombo 05	2026-11-14 10:16:56.77841+00	\N
b5d470e4-78d9-4861-8851-5b30f09408e2	11d719b9-c253-4cad-a0cb-a97d639b2881	Colombo 04	2026-11-14 10:16:56.77841+00	\N
a97758fe-7c5f-4377-b719-0dcd35bd493a	6c69cd22-f554-4d46-8aa6-9ba085af707f	Colombo 15	2026-11-14 10:16:56.77841+00	\N
f85233b0-48b8-468c-b308-c4ad605daa78	6c69cd22-f554-4d46-8aa6-9ba085af707f	Colombo 14	2026-11-14 10:16:56.77841+00	\N
9ca39e00-0ee5-454e-9745-a66cfbea3091	6c69cd22-f554-4d46-8aa6-9ba085af707f	Colombo 13	2026-11-14 10:16:56.77841+00	\N
0b7bab23-6e28-4088-92fa-8fd3fcb83ac2	6c69cd22-f554-4d46-8aa6-9ba085af707f	Colombo 12	2026-11-14 10:16:56.77841+00	\N
265421ec-4f04-43f4-981c-e8f089ee6923	6c69cd22-f554-4d46-8aa6-9ba085af707f	Colombo 11	2026-11-14 10:16:56.77841+00	\N
41ceb422-48cd-4d95-9da7-8ec586a546a3	6c69cd22-f554-4d46-8aa6-9ba085af707f	Colombo 10	2026-11-14 10:16:56.77841+00	\N
e222dbe2-9a07-4686-aa57-ee3959cd1839	6c69cd22-f554-4d46-8aa6-9ba085af707f	Colombo 09	2026-11-14 10:16:56.77841+00	\N
f73c0bd7-2786-4fda-a442-ea3396757597	6c69cd22-f554-4d46-8aa6-9ba085af707f	Colombo 08	2026-11-14 10:16:56.77841+00	\N
e77d8a9b-abcf-4440-81fe-26c49a4da838	6c69cd22-f554-4d46-8aa6-9ba085af707f	Colombo 07	2026-11-14 10:16:56.77841+00	\N
a92fdc21-787a-4529-b7aa-8c3ccbf22161	236007fa-4899-4dd2-9e08-620cb6c03f9d	Rajagiriya	2026-11-14 10:16:56.77841+00	\N
9bc86f34-3e54-4b99-a6ef-b58f724db07a	236007fa-4899-4dd2-9e08-620cb6c03f9d	Kotte	2026-11-14 10:16:56.77841+00	\N
e021b5b0-b583-4e6a-a49b-e16c57c44f75	1e82de10-aabd-40c8-b154-09fef779391b	Maharagama	2026-11-14 10:16:56.77841+00	\N
6d8d9449-4c59-4c71-af81-a9828edf10c3	1e82de10-aabd-40c8-b154-09fef779391b	Nugegoda	2026-11-14 10:16:56.77841+00	\N
e3f7b978-7403-460d-a66d-46d49c39df91	dfd16933-61bf-4080-aae3-4713de96a601	Mount Lavinia	2026-11-14 10:16:56.77841+00	\N
ff32a927-ebb8-4baf-aadc-6a258ec66c08	dfd16933-61bf-4080-aae3-4713de96a601	Dehiwala	2026-11-14 10:16:56.77841+00	\N
aed4e31d-c085-4b13-a874-4f4954d2a6b5	ed9c9c34-87fe-4892-a6d0-a482f1c17114	Kandy	2026-11-14 10:16:56.77841+00	\N
56133d18-9112-49e3-a649-3ea9493192a4	c7f3e39f-6abe-43f7-8b0a-8bb3930abcd6	Galle	2026-11-14 10:16:56.77841+00	\N
8114fe09-730d-4bbd-aa05-08048a01f7df	510ba456-0132-46e0-8808-f5cbfa013cfe	Negombo	2026-11-14 10:16:56.77841+00	\N
\.


--
-- Data for Name: zones; Type: TABLE DATA; Schema: public; Owner: pricing
--

COPY public.zones (id, code, name, zone_group, active, created_at, updated_at) FROM stdin;
fbf812ed-c47b-4b66-88a2-1dc90ffff260	COLOMBO_CENTRAL	Colombo Central	COLOMBO	t	2026-11-14 10:16:56.77841+00	\N
11d719b9-c253-4cad-a0cb-a97d639b2881	COLOMBO_SOUTH	Colombo South	COLOMBO	t	2026-11-14 10:16:56.77841+00	\N
6c69cd22-f554-4d46-8aa6-9ba085af707f	COLOMBO_NORTH	Colombo North	COLOMBO	t	2026-11-14 10:16:56.77841+00	\N
236007fa-4899-4dd2-9e08-620cb6c03f9d	KOTTE_RAJAGIRIYA	Kotte & Rajagiriya	SUBURBS	t	2026-11-14 10:16:56.77841+00	\N
1e82de10-aabd-40c8-b154-09fef779391b	NUGEGODA_MAHARAGAMA	Nugegoda & Maharagama	SUBURBS	t	2026-11-14 10:16:56.77841+00	\N
dfd16933-61bf-4080-aae3-4713de96a601	DEHIWALA_MT_LAVINIA	Dehiwala & Mt Lavinia	SUBURBS	t	2026-11-14 10:16:56.77841+00	\N
ed9c9c34-87fe-4892-a6d0-a482f1c17114	KANDY	Kandy	INTER_CITY	t	2026-11-14 10:16:56.77841+00	\N
c7f3e39f-6abe-43f7-8b0a-8bb3930abcd6	GALLE	Galle	INTER_CITY	t	2026-11-14 10:16:56.77841+00	\N
510ba456-0132-46e0-8808-f5cbfa013cfe	NEGOMBO	Negombo	INTER_CITY	t	2026-11-14 10:16:56.77841+00	\N
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

\unrestrict oujlGUTRdyX8DRiDIuPy8Afku5ifrAy7TefLtlqeqZe5B93yrccHlWO9VOiUzF7

