--
-- PostgreSQL database dump
--

\restrict JUWduxqYUp1doH9VRhayQnucKa3h1nghzNL2OWQfGCsNbgRFafN0vmHXmz6Z6BJ

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

ALTER TABLE IF EXISTS ONLY public.user_roles DROP CONSTRAINT IF EXISTS user_roles_user_id_fkey;
ALTER TABLE IF EXISTS ONLY public.user_roles DROP CONSTRAINT IF EXISTS user_roles_role_id_fkey;
ALTER TABLE IF EXISTS ONLY public.user_roles DROP CONSTRAINT IF EXISTS user_roles_reviewed_by_fkey;
ALTER TABLE IF EXISTS ONLY public.refresh_tokens DROP CONSTRAINT IF EXISTS refresh_tokens_user_id_fkey;
ALTER TABLE IF EXISTS ONLY public.refresh_tokens DROP CONSTRAINT IF EXISTS refresh_tokens_replaced_by_fkey;
ALTER TABLE IF EXISTS ONLY public.oauth_login_codes DROP CONSTRAINT IF EXISTS oauth_login_codes_user_id_fkey;
ALTER TABLE IF EXISTS ONLY public.oauth_accounts DROP CONSTRAINT IF EXISTS oauth_accounts_user_id_fkey;
ALTER TABLE IF EXISTS ONLY public.merchant_profiles DROP CONSTRAINT IF EXISTS merchant_profiles_user_id_fkey;
ALTER TABLE IF EXISTS ONLY public.merchant_profiles DROP CONSTRAINT IF EXISTS merchant_profiles_reviewed_by_fkey;
ALTER TABLE IF EXISTS ONLY public.courier_profiles DROP CONSTRAINT IF EXISTS courier_profiles_user_id_fkey;
ALTER TABLE IF EXISTS ONLY public.courier_profiles DROP CONSTRAINT IF EXISTS courier_profiles_reviewed_by_fkey;
DROP INDEX IF EXISTS public.uq_users_mobile_number;
DROP INDEX IF EXISTS public.uq_users_email_lower;
DROP INDEX IF EXISTS public.idx_user_roles_role_id;
DROP INDEX IF EXISTS public.idx_refresh_tokens_user_id;
DROP INDEX IF EXISTS public.idx_oauth_login_codes_expires_at;
DROP INDEX IF EXISTS public.idx_oauth_accounts_user_id;
DROP INDEX IF EXISTS public.flyway_schema_history_s_idx;
ALTER TABLE IF EXISTS ONLY public.users DROP CONSTRAINT IF EXISTS users_pkey;
ALTER TABLE IF EXISTS ONLY public.user_roles DROP CONSTRAINT IF EXISTS user_roles_pkey;
ALTER TABLE IF EXISTS ONLY public.oauth_accounts DROP CONSTRAINT IF EXISTS uq_oauth_provider_account;
ALTER TABLE IF EXISTS ONLY public.roles DROP CONSTRAINT IF EXISTS roles_pkey;
ALTER TABLE IF EXISTS ONLY public.roles DROP CONSTRAINT IF EXISTS roles_name_key;
ALTER TABLE IF EXISTS ONLY public.refresh_tokens DROP CONSTRAINT IF EXISTS refresh_tokens_token_hash_key;
ALTER TABLE IF EXISTS ONLY public.refresh_tokens DROP CONSTRAINT IF EXISTS refresh_tokens_pkey;
ALTER TABLE IF EXISTS ONLY public.oauth_login_codes DROP CONSTRAINT IF EXISTS oauth_login_codes_pkey;
ALTER TABLE IF EXISTS ONLY public.oauth_login_codes DROP CONSTRAINT IF EXISTS oauth_login_codes_code_hash_key;
ALTER TABLE IF EXISTS ONLY public.oauth_accounts DROP CONSTRAINT IF EXISTS oauth_accounts_pkey;
ALTER TABLE IF EXISTS ONLY public.merchant_profiles DROP CONSTRAINT IF EXISTS merchant_profiles_pkey;
ALTER TABLE IF EXISTS ONLY public.merchant_profiles DROP CONSTRAINT IF EXISTS merchant_profiles_business_registration_number_key;
ALTER TABLE IF EXISTS ONLY public.flyway_schema_history DROP CONSTRAINT IF EXISTS flyway_schema_history_pk;
ALTER TABLE IF EXISTS ONLY public.courier_profiles DROP CONSTRAINT IF EXISTS courier_profiles_pkey;
ALTER TABLE IF EXISTS ONLY public.courier_profiles DROP CONSTRAINT IF EXISTS courier_profiles_nic_number_key;
DROP TABLE IF EXISTS public.users;
DROP TABLE IF EXISTS public.user_roles;
DROP TABLE IF EXISTS public.roles;
DROP TABLE IF EXISTS public.refresh_tokens;
DROP TABLE IF EXISTS public.oauth_login_codes;
DROP TABLE IF EXISTS public.oauth_accounts;
DROP TABLE IF EXISTS public.merchant_profiles;
DROP TABLE IF EXISTS public.flyway_schema_history;
DROP TABLE IF EXISTS public.courier_profiles;
SET default_tablespace = '';

SET default_table_access_method = heap;

--
-- Name: courier_profiles; Type: TABLE; Schema: public; Owner: auth
--

CREATE TABLE public.courier_profiles (
    user_id uuid NOT NULL,
    nic_number character varying(30) NOT NULL,
    date_of_birth date NOT NULL,
    delivery_zones text NOT NULL,
    vehicle_type character varying(50) NOT NULL,
    nic_document_path text,
    licence_document_path text,
    approval_status character varying(20) DEFAULT 'PENDING'::character varying NOT NULL,
    reviewed_at timestamp with time zone,
    reviewed_by uuid,
    created_at timestamp with time zone DEFAULT CURRENT_TIMESTAMP NOT NULL,
    CONSTRAINT chk_courier_approval_status CHECK (((approval_status)::text = ANY ((ARRAY['PENDING'::character varying, 'APPROVED'::character varying, 'REJECTED'::character varying])::text[])))
);


ALTER TABLE public.courier_profiles OWNER TO auth;

--
-- Name: flyway_schema_history; Type: TABLE; Schema: public; Owner: auth
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


ALTER TABLE public.flyway_schema_history OWNER TO auth;

--
-- Name: merchant_profiles; Type: TABLE; Schema: public; Owner: auth
--

CREATE TABLE public.merchant_profiles (
    user_id uuid NOT NULL,
    business_name character varying(200) NOT NULL,
    business_registration_number character varying(100) NOT NULL,
    business_category character varying(100) NOT NULL,
    pickup_location text NOT NULL,
    contact_person character varying(150) NOT NULL,
    br_certificate_path text,
    owner_nic_path text,
    approval_status character varying(20) DEFAULT 'PENDING'::character varying NOT NULL,
    reviewed_at timestamp with time zone,
    reviewed_by uuid,
    created_at timestamp with time zone DEFAULT CURRENT_TIMESTAMP NOT NULL,
    CONSTRAINT chk_merchant_approval_status CHECK (((approval_status)::text = ANY ((ARRAY['PENDING'::character varying, 'APPROVED'::character varying, 'REJECTED'::character varying])::text[])))
);


ALTER TABLE public.merchant_profiles OWNER TO auth;

--
-- Name: oauth_accounts; Type: TABLE; Schema: public; Owner: auth
--

CREATE TABLE public.oauth_accounts (
    id uuid DEFAULT gen_random_uuid() NOT NULL,
    user_id uuid NOT NULL,
    provider character varying(30) NOT NULL,
    provider_user_id character varying(255) NOT NULL,
    created_at timestamp with time zone DEFAULT CURRENT_TIMESTAMP NOT NULL
);


ALTER TABLE public.oauth_accounts OWNER TO auth;

--
-- Name: oauth_login_codes; Type: TABLE; Schema: public; Owner: auth
--

CREATE TABLE public.oauth_login_codes (
    id uuid DEFAULT gen_random_uuid() NOT NULL,
    user_id uuid NOT NULL,
    code_hash character varying(64) NOT NULL,
    expires_at timestamp with time zone NOT NULL,
    consumed_at timestamp with time zone,
    created_at timestamp with time zone DEFAULT CURRENT_TIMESTAMP NOT NULL
);


ALTER TABLE public.oauth_login_codes OWNER TO auth;

--
-- Name: refresh_tokens; Type: TABLE; Schema: public; Owner: auth
--

CREATE TABLE public.refresh_tokens (
    id uuid DEFAULT gen_random_uuid() NOT NULL,
    user_id uuid NOT NULL,
    token_hash character varying(255) NOT NULL,
    expires_at timestamp with time zone NOT NULL,
    revoked_at timestamp with time zone,
    replaced_by uuid,
    created_at timestamp with time zone DEFAULT CURRENT_TIMESTAMP NOT NULL
);


ALTER TABLE public.refresh_tokens OWNER TO auth;

--
-- Name: roles; Type: TABLE; Schema: public; Owner: auth
--

CREATE TABLE public.roles (
    id bigint NOT NULL,
    name character varying(30) NOT NULL,
    CONSTRAINT chk_roles_name CHECK (((name)::text = ANY ((ARRAY['CUSTOMER'::character varying, 'MERCHANT'::character varying, 'COURIER'::character varying, 'DISPATCHER'::character varying, 'ADMIN'::character varying])::text[])))
);


ALTER TABLE public.roles OWNER TO auth;

--
-- Name: roles_id_seq; Type: SEQUENCE; Schema: public; Owner: auth
--

ALTER TABLE public.roles ALTER COLUMN id ADD GENERATED ALWAYS AS IDENTITY (
    SEQUENCE NAME public.roles_id_seq
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1
);


--
-- Name: user_roles; Type: TABLE; Schema: public; Owner: auth
--

CREATE TABLE public.user_roles (
    user_id uuid NOT NULL,
    role_id bigint NOT NULL,
    assignment_status character varying(20) DEFAULT 'ACTIVE'::character varying NOT NULL,
    assigned_at timestamp with time zone DEFAULT CURRENT_TIMESTAMP NOT NULL,
    reviewed_at timestamp with time zone,
    reviewed_by uuid,
    CONSTRAINT chk_user_roles_status CHECK (((assignment_status)::text = ANY ((ARRAY['PENDING'::character varying, 'ACTIVE'::character varying, 'REJECTED'::character varying, 'SUSPENDED'::character varying])::text[])))
);


ALTER TABLE public.user_roles OWNER TO auth;

--
-- Name: users; Type: TABLE; Schema: public; Owner: auth
--

CREATE TABLE public.users (
    id uuid DEFAULT gen_random_uuid() NOT NULL,
    full_name character varying(150) NOT NULL,
    email character varying(255) NOT NULL,
    mobile_number character varying(30),
    password_hash character varying(255),
    account_status character varying(20) DEFAULT 'ACTIVE'::character varying NOT NULL,
    email_verified boolean DEFAULT false NOT NULL,
    created_at timestamp with time zone DEFAULT CURRENT_TIMESTAMP NOT NULL,
    updated_at timestamp with time zone DEFAULT CURRENT_TIMESTAMP NOT NULL,
    CONSTRAINT chk_users_account_status CHECK (((account_status)::text = ANY ((ARRAY['ACTIVE'::character varying, 'SUSPENDED'::character varying, 'DISABLED'::character varying])::text[])))
);


ALTER TABLE public.users OWNER TO auth;

--
-- Data for Name: courier_profiles; Type: TABLE DATA; Schema: public; Owner: auth
--

COPY public.courier_profiles (user_id, nic_number, date_of_birth, delivery_zones, vehicle_type, nic_document_path, licence_document_path, approval_status, reviewed_at, reviewed_by, created_at) FROM stdin;
c5592d9f-a72a-4822-9080-fd0ccc511add	197684979167	1976-07-09	COLOMBO_CENTRAL,COLOMBO_SOUTH	Three-wheeler	\N	\N	APPROVED	2026-08-10 11:42:00+00	679fce35-4d90-404d-abdf-3208533d5c24	2026-08-10 09:42:00+00
a1452cf6-4d4b-4a28-860e-518d81821912	200293904619	2002-12-26	COLOMBO_NORTH,COLOMBO_CENTRAL	Van	\N	\N	APPROVED	2026-07-10 04:40:00+00	679fce35-4d90-404d-abdf-3208533d5c24	2026-07-09 02:40:00+00
42b44f66-66c4-44b8-af94-031065494ba7	200229364897	2002-10-09	KOTTE_RAJAGIRIYA,NUGEGODA_MAHARAGAMA	Motorbike	\N	\N	APPROVED	2026-08-08 01:35:00+00	679fce35-4d90-404d-abdf-3208533d5c24	2026-08-07 12:35:00+00
73623c45-6a11-4080-98da-1a004bacbf48	198670871702	1986-03-16	DEHIWALA_MT_LAVINIA,COLOMBO_SOUTH	Motorbike	\N	\N	APPROVED	2026-08-21 18:26:00+00	679fce35-4d90-404d-abdf-3208533d5c24	2026-08-21 11:26:00+00
28e8368b-8a0e-4fa6-8a8f-db7172b8c354	199365881301	1993-12-14	NUGEGODA_MAHARAGAMA,DEHIWALA_MT_LAVINIA	Motorbike	\N	\N	APPROVED	2026-09-02 04:54:00+00	679fce35-4d90-404d-abdf-3208533d5c24	2026-09-01 05:54:00+00
88670f51-c181-48ba-b6ff-1342422fa974	197578137055	1975-11-04	COLOMBO_NORTH,NEGOMBO	Motorbike	\N	\N	APPROVED	2026-08-18 11:35:00+00	679fce35-4d90-404d-abdf-3208533d5c24	2026-08-18 03:35:00+00
8a9f6979-f1cc-4539-a393-1baa9bb70bad	200094589930	2000-08-23	KANDY	Three-wheeler	\N	\N	APPROVED	2026-09-03 05:40:00+00	679fce35-4d90-404d-abdf-3208533d5c24	2026-09-02 06:40:00+00
2312f6f5-ecd5-4e40-8ed1-8bfbfe15c53a	199031822359	1990-11-06	GALLE	Motorbike	\N	\N	APPROVED	2026-07-16 12:43:00+00	679fce35-4d90-404d-abdf-3208533d5c24	2026-07-16 05:43:00+00
175873ad-65ef-45e0-91ea-d52af596bf9e	199126255693	1991-03-04	COLOMBO_SOUTH,KOTTE_RAJAGIRIYA	Motorbike	\N	\N	APPROVED	2026-07-23 20:27:00+00	679fce35-4d90-404d-abdf-3208533d5c24	2026-07-23 04:27:00+00
9875ccd7-f7f9-4a43-bbbd-c1fd75a9407e	200140861450	2001-07-27	COLOMBO_CENTRAL,COLOMBO_NORTH	Motorbike	\N	\N	APPROVED	2026-07-15 00:37:00+00	679fce35-4d90-404d-abdf-3208533d5c24	2026-07-14 05:37:00+00
\.


--
-- Data for Name: flyway_schema_history; Type: TABLE DATA; Schema: public; Owner: auth
--

COPY public.flyway_schema_history (installed_rank, version, description, type, script, checksum, installed_by, installed_on, execution_time, success) FROM stdin;
1	1	create auth tables	SQL	V1__create_auth_tables.sql	427391598	auth	2026-11-14 10:16:56.023329	128	t
2	2	create oauth login codes	SQL	V2__create_oauth_login_codes.sql	-2005132465	auth	2026-11-14 10:16:56.212664	8	t
3	3	unique user mobile numbers	SQL	V3__unique_user_mobile_numbers.sql	1535160245	auth	2026-11-14 10:16:56.236336	6	t
\.


--
-- Data for Name: merchant_profiles; Type: TABLE DATA; Schema: public; Owner: auth
--

COPY public.merchant_profiles (user_id, business_name, business_registration_number, business_category, pickup_location, contact_person, br_certificate_path, owner_nic_path, approval_status, reviewed_at, reviewed_by, created_at) FROM stdin;
5bbfdaf0-3d8d-4ceb-bd93-00c91573ac23	Kandy Spice House	PV467611	Food & groceries	220 Peradeniya Road, Kandy	Ruwan Fernando	\N	\N	APPROVED	2026-09-21 01:06:00+00	679fce35-4d90-404d-abdf-3208533d5c24	2026-09-20 06:06:00+00
30056f25-6559-438c-9980-4e2a84365b06	Colombo Threads	PV603196	Clothing	10 Flower Road, Colombo 07	Sajith Karunaratne	\N	\N	APPROVED	2026-09-01 17:06:00+00	679fce35-4d90-404d-abdf-3208533d5c24	2026-09-01 10:06:00+00
cc91bacd-b679-443d-8d92-da68e6bdc2be	TechLanka Gadgets	PV405641	Electronics	216 Galle Road, Colombo 03	Lahiru Senanayake	\N	\N	APPROVED	2026-09-19 09:49:00+00	679fce35-4d90-404d-abdf-3208533d5c24	2026-09-18 03:49:00+00
9e6c3a6a-7c87-4f1d-a0b1-6509fb5a96d3	Ceylon Tea Corner	PV924020	Food & groceries	21 Stanley Thilakaratne Mawatha, Nugegoda	Lahiru Wijesinghe	\N	\N	APPROVED	2026-09-18 04:19:00+00	679fce35-4d90-404d-abdf-3208533d5c24	2026-09-17 06:19:00+00
8131f094-5b5f-4331-8afd-7d7f917074e5	BookNook Lanka	PV542663	Books	144 Havelock Road, Colombo 05	Chathura Perera	\N	\N	APPROVED	2026-09-22 10:26:00+00	679fce35-4d90-404d-abdf-3208533d5c24	2026-09-21 13:26:00+00
7635a541-77c2-47a8-a362-e854d888fcfc	Galle Fort Crafts	PV500681	Handicrafts	79 Wakwella Road, Galle	Pradeep Wickramasinghe	\N	\N	APPROVED	2026-09-15 02:08:00+00	679fce35-4d90-404d-abdf-3208533d5c24	2026-09-14 11:08:00+00
b498d433-f9af-4096-a491-d8d4097b64ed	Negombo Fresh Fish	PV470497	Food & groceries	170 Lewis Place, Negombo	Pradeep Rajapaksa	\N	\N	PENDING	\N	\N	2026-09-06 03:27:00+00
25d7fcf0-1032-4973-a56d-0a547777f402	QuickFix Mobile Parts	PV616649	Electronics	216 Parliament Road, Rajagiriya	Oshada Weerasinghe	\N	\N	REJECTED	2026-09-18 03:25:00+00	679fce35-4d90-404d-abdf-3208533d5c24	2026-09-17 11:25:00+00
\.


--
-- Data for Name: oauth_accounts; Type: TABLE DATA; Schema: public; Owner: auth
--

COPY public.oauth_accounts (id, user_id, provider, provider_user_id, created_at) FROM stdin;
\.


--
-- Data for Name: oauth_login_codes; Type: TABLE DATA; Schema: public; Owner: auth
--

COPY public.oauth_login_codes (id, user_id, code_hash, expires_at, consumed_at, created_at) FROM stdin;
\.


--
-- Data for Name: refresh_tokens; Type: TABLE DATA; Schema: public; Owner: auth
--

COPY public.refresh_tokens (id, user_id, token_hash, expires_at, revoked_at, replaced_by, created_at) FROM stdin;
\.


--
-- Data for Name: roles; Type: TABLE DATA; Schema: public; Owner: auth
--

COPY public.roles (id, name) FROM stdin;
1	CUSTOMER
2	MERCHANT
3	COURIER
4	DISPATCHER
5	ADMIN
\.


--
-- Data for Name: user_roles; Type: TABLE DATA; Schema: public; Owner: auth
--

COPY public.user_roles (user_id, role_id, assignment_status, assigned_at, reviewed_at, reviewed_by) FROM stdin;
679fce35-4d90-404d-abdf-3208533d5c24	5	ACTIVE	2026-06-17 02:33:00+00	2026-06-17 13:33:00+00	\N
8b792576-f64c-48bc-9eaa-1865563cfdb5	5	ACTIVE	2026-06-11 04:27:00+00	2026-06-11 10:27:00+00	679fce35-4d90-404d-abdf-3208533d5c24
49c139c8-8007-4199-80b6-e68a928a59fb	4	ACTIVE	2026-07-30 12:47:00+00	2026-07-31 15:47:00+00	679fce35-4d90-404d-abdf-3208533d5c24
80d23eb8-1156-49dc-b5c3-42cdedfab75d	4	ACTIVE	2026-07-11 03:05:00+00	2026-07-12 02:05:00+00	679fce35-4d90-404d-abdf-3208533d5c24
7d19bfe5-daf8-4879-8f88-7fcc9bff34c5	4	ACTIVE	2026-07-21 10:35:00+00	2026-07-22 16:35:00+00	679fce35-4d90-404d-abdf-3208533d5c24
c5592d9f-a72a-4822-9080-fd0ccc511add	3	ACTIVE	2026-08-10 09:42:00+00	2026-08-11 04:42:00+00	679fce35-4d90-404d-abdf-3208533d5c24
a1452cf6-4d4b-4a28-860e-518d81821912	3	ACTIVE	2026-07-09 02:40:00+00	2026-07-10 07:40:00+00	679fce35-4d90-404d-abdf-3208533d5c24
42b44f66-66c4-44b8-af94-031065494ba7	3	ACTIVE	2026-08-07 12:35:00+00	2026-08-07 23:35:00+00	679fce35-4d90-404d-abdf-3208533d5c24
73623c45-6a11-4080-98da-1a004bacbf48	3	ACTIVE	2026-08-21 11:26:00+00	2026-08-21 19:26:00+00	679fce35-4d90-404d-abdf-3208533d5c24
28e8368b-8a0e-4fa6-8a8f-db7172b8c354	3	ACTIVE	2026-09-01 05:54:00+00	2026-09-02 00:54:00+00	679fce35-4d90-404d-abdf-3208533d5c24
88670f51-c181-48ba-b6ff-1342422fa974	3	ACTIVE	2026-08-18 03:35:00+00	2026-08-19 05:35:00+00	679fce35-4d90-404d-abdf-3208533d5c24
8a9f6979-f1cc-4539-a393-1baa9bb70bad	3	ACTIVE	2026-09-02 06:40:00+00	2026-09-02 13:40:00+00	679fce35-4d90-404d-abdf-3208533d5c24
2312f6f5-ecd5-4e40-8ed1-8bfbfe15c53a	3	ACTIVE	2026-07-16 05:43:00+00	2026-07-16 09:43:00+00	679fce35-4d90-404d-abdf-3208533d5c24
175873ad-65ef-45e0-91ea-d52af596bf9e	3	ACTIVE	2026-07-23 04:27:00+00	2026-07-23 14:27:00+00	679fce35-4d90-404d-abdf-3208533d5c24
9875ccd7-f7f9-4a43-bbbd-c1fd75a9407e	3	ACTIVE	2026-07-14 05:37:00+00	2026-07-15 05:37:00+00	679fce35-4d90-404d-abdf-3208533d5c24
5bbfdaf0-3d8d-4ceb-bd93-00c91573ac23	1	ACTIVE	2026-09-20 06:06:00+00	\N	\N
5bbfdaf0-3d8d-4ceb-bd93-00c91573ac23	2	ACTIVE	2026-09-20 06:06:00+00	\N	\N
30056f25-6559-438c-9980-4e2a84365b06	1	ACTIVE	2026-09-01 10:06:00+00	\N	\N
30056f25-6559-438c-9980-4e2a84365b06	2	ACTIVE	2026-09-01 10:06:00+00	\N	\N
cc91bacd-b679-443d-8d92-da68e6bdc2be	1	ACTIVE	2026-09-18 03:49:00+00	\N	\N
cc91bacd-b679-443d-8d92-da68e6bdc2be	2	ACTIVE	2026-09-18 03:49:00+00	\N	\N
9e6c3a6a-7c87-4f1d-a0b1-6509fb5a96d3	1	ACTIVE	2026-09-17 06:19:00+00	\N	\N
9e6c3a6a-7c87-4f1d-a0b1-6509fb5a96d3	2	ACTIVE	2026-09-17 06:19:00+00	\N	\N
8131f094-5b5f-4331-8afd-7d7f917074e5	1	ACTIVE	2026-09-21 13:26:00+00	\N	\N
8131f094-5b5f-4331-8afd-7d7f917074e5	2	ACTIVE	2026-09-21 13:26:00+00	\N	\N
7635a541-77c2-47a8-a362-e854d888fcfc	1	ACTIVE	2026-09-14 11:08:00+00	\N	\N
7635a541-77c2-47a8-a362-e854d888fcfc	2	ACTIVE	2026-09-14 11:08:00+00	\N	\N
b498d433-f9af-4096-a491-d8d4097b64ed	1	ACTIVE	2026-09-06 03:27:00+00	\N	\N
25d7fcf0-1032-4973-a56d-0a547777f402	1	ACTIVE	2026-09-17 11:25:00+00	\N	\N
abb1016e-35d4-40b4-a1d9-e5a308e17a5d	1	ACTIVE	2026-08-03 12:05:00+00	\N	\N
643fdc94-f3a5-4cbb-ab82-67cb56405d8e	1	ACTIVE	2026-09-11 05:53:00+00	\N	\N
56bccd5c-356a-4e87-8932-16022d0f1c67	1	ACTIVE	2026-08-09 09:14:00+00	\N	\N
35af507f-40f9-4179-aa18-ad67cc90e3c6	1	ACTIVE	2026-09-19 06:23:00+00	\N	\N
4562ae10-2335-4237-8eb9-25880e883a30	1	ACTIVE	2026-08-25 10:33:00+00	\N	\N
b91119eb-4767-4860-b6e1-b5ee93593957	1	ACTIVE	2026-09-23 06:24:00+00	\N	\N
fd7c2e41-eded-41e7-b0d2-98353000fa90	1	ACTIVE	2026-08-06 10:59:00+00	\N	\N
a69f40d7-9950-455b-b2db-9a1a35f6b4c2	1	ACTIVE	2026-09-08 12:01:00+00	\N	\N
ba7a69cd-815c-448c-ada9-f7a49ac6bb50	1	ACTIVE	2026-10-02 03:57:00+00	\N	\N
ba860191-4254-4f32-ab5d-03b2adbee345	1	ACTIVE	2026-09-22 05:01:00+00	\N	\N
38a8b458-db4b-4a49-9866-91e79ed8cb4c	1	ACTIVE	2026-10-06 02:54:00+00	\N	\N
9f76138f-3fc1-4b8f-bb9f-5df28b84cd2c	1	ACTIVE	2026-08-26 09:38:00+00	\N	\N
18dbe225-6150-4e1b-b41c-e7109ccbd18e	1	ACTIVE	2026-08-15 06:47:00+00	\N	\N
b4f1804b-2b88-4a98-b6ba-0913cd259045	1	ACTIVE	2026-09-08 08:09:00+00	\N	\N
d643c832-0366-4e10-b444-63f2cc479424	1	ACTIVE	2026-09-05 10:15:00+00	\N	\N
c7a61ae5-05fc-49d7-9bd1-f8093be4f0e3	1	ACTIVE	2026-08-15 10:22:00+00	\N	\N
7cbce484-9762-48e8-8fda-891d5d088cee	1	ACTIVE	2026-08-15 10:44:00+00	\N	\N
f1c98d80-55e5-4e75-8f22-56fc86bfc894	1	ACTIVE	2026-08-09 12:41:00+00	\N	\N
bc89ad84-c579-4c07-9452-41aff3eade38	1	ACTIVE	2026-09-10 07:17:00+00	\N	\N
ab8979a9-f86a-4114-a57a-05c1bbb047a0	1	ACTIVE	2026-08-27 04:36:00+00	\N	\N
35e35258-a722-428c-bf10-8318c9542f33	1	ACTIVE	2026-09-20 12:39:00+00	\N	\N
b01faa8b-97a4-4fec-b8e6-83570b20abe1	1	ACTIVE	2026-10-11 09:28:00+00	\N	\N
7837a8c9-5849-4620-8032-4c6db4bd64b9	1	ACTIVE	2026-09-23 03:43:00+00	\N	\N
894da928-9d42-4091-a210-f889650603cf	1	ACTIVE	2026-08-29 11:03:00+00	\N	\N
ca560003-b29d-4548-8e8a-db0e365ffacb	1	ACTIVE	2026-08-02 07:06:00+00	\N	\N
0e48a1cf-a0d4-46ab-9355-bff3bd5b058c	1	ACTIVE	2026-09-03 07:46:00+00	\N	\N
cbf74fc9-af4a-4124-8fc3-8e6aa92a6b4f	1	ACTIVE	2026-10-13 03:36:00+00	\N	\N
41e925fa-e3cf-4ad8-b97b-590dddb8eb7e	1	ACTIVE	2026-09-30 06:14:00+00	\N	\N
da3a2f45-be06-4092-bcc9-dba48859fd08	1	ACTIVE	2026-10-09 05:52:00+00	\N	\N
7854b03a-8c06-47a6-8c46-971d35592326	1	ACTIVE	2026-10-07 07:39:00+00	\N	\N
4a070202-5651-4fc2-8a83-9453e5948ea0	1	ACTIVE	2026-08-10 08:48:00+00	\N	\N
99e69915-044d-46d1-ac17-0dec6d89ef9e	1	ACTIVE	2026-08-02 12:00:00+00	\N	\N
5e62844d-8f4d-489e-96d9-44e67ff6da2c	1	ACTIVE	2026-08-02 13:21:00+00	\N	\N
47bc625f-72d4-4c14-b74e-ae6bb4ff01ff	1	ACTIVE	2026-10-12 09:27:00+00	\N	\N
69efc098-9961-44e4-9608-e4401147d661	1	ACTIVE	2026-09-13 13:16:00+00	\N	\N
d4db9fa2-3261-4580-820e-9174ddc27975	1	ACTIVE	2026-08-12 07:21:00+00	\N	\N
893ba1fd-f5b8-406d-a7d8-846f7b7370eb	1	ACTIVE	2026-07-28 04:08:00+00	\N	\N
926acd4b-b097-4ed8-b69e-f6ef48e5dec3	1	ACTIVE	2026-08-06 04:19:00+00	\N	\N
403f89f3-daab-43e4-a095-8c5382a79c41	1	ACTIVE	2026-09-23 08:19:00+00	\N	\N
faf30155-d50f-4fd5-a24a-c85f69826132	1	ACTIVE	2026-08-10 05:29:00+00	\N	\N
\.


--
-- Data for Name: users; Type: TABLE DATA; Schema: public; Owner: auth
--

COPY public.users (id, full_name, email, mobile_number, password_hash, account_status, email_verified, created_at, updated_at) FROM stdin;
7635a541-77c2-47a8-a362-e854d888fcfc	Pradeep Wickramasinghe	pradeep.wickramasinghe@merchant.example.com	0760290556	$2a$12$VU.TiXFiowKfs5mL7pG8C.w/sakPZ/MmlytJAykyvLFzlyMB14gMS	ACTIVE	t	2026-09-14 11:08:00+00	2026-09-14 11:08:00+00
b498d433-f9af-4096-a491-d8d4097b64ed	Pradeep Rajapaksa	pradeep.rajapaksa@merchant.example.com	0726996524	$2a$12$mIa2LxluEV.Qceg3XNxx0ee.5dshqQZ1rd14pVCpPXsJfsi35tgiG	ACTIVE	t	2026-09-06 03:27:00+00	2026-09-06 03:27:00+00
25d7fcf0-1032-4973-a56d-0a547777f402	Oshada Weerasinghe	oshada.weerasinghe@merchant.example.com	0787692766	$2a$12$FP1zL3NAXUY/9QlR2Uz9VuSuXK6QhJe1uky/cexJ4A0IyCs1aMmnO	ACTIVE	t	2026-09-17 11:25:00+00	2026-09-17 11:25:00+00
643fdc94-f3a5-4cbb-ab82-67cb56405d8e	Anjali Fernando	anjali.fernando@example.com	0761737195	$2a$12$.CqNq6CaAPsYMfhMuwrCC.dfvWHrzhxc1pza/rFiTf92jt5iSxE0q	ACTIVE	t	2026-09-11 05:53:00+00	2026-09-11 05:53:00+00
56bccd5c-356a-4e87-8932-16022d0f1c67	Thisaru Senanayake	thisaru.senanayake@example.com	0703171384	$2a$12$dYlFGCx1DnDhtjhP8ssXSuhVPzT8PF8dfvvFWTwycEIwba4c/ODWy	ACTIVE	t	2026-08-09 09:14:00+00	2026-08-09 09:14:00+00
35af507f-40f9-4179-aa18-ad67cc90e3c6	Ishara Gunasekara	ishara.gunasekara@example.com	0774426117	$2a$12$19bZOUTu3INipkGKXC36jO7eSwNQ0aarbKDJlYoOVof.ZrPr8j//G	ACTIVE	t	2026-09-19 06:23:00+00	2026-09-19 06:23:00+00
4562ae10-2335-4237-8eb9-25880e883a30	Tharushi de Silva	tharushi.de.silva@example.com	0706451756	$2a$12$DOcBwmzwdx4QXJKhc5Yptu55Qe2mZMBGnp2IV50D9OXhxhumB32QG	ACTIVE	t	2026-08-25 10:33:00+00	2026-08-25 10:33:00+00
b91119eb-4767-4860-b6e1-b5ee93593957	Nipuni Liyanage	nipuni.liyanage@example.com	0767872240	$2a$12$ZRQpqWCQOJCBPlj5kf8BDuRtZom2h8LiIFie4GB911J9XWCn7Jzp6	ACTIVE	t	2026-09-23 06:24:00+00	2026-09-23 06:24:00+00
fd7c2e41-eded-41e7-b0d2-98353000fa90	Malith de Silva	malith.de.silva@example.com	0701418621	$2a$12$7kcws3BsiDl5D.qUpNkzr.kl5YIt3UXiE1/QR3HdCboymQ5Tr5IWm	ACTIVE	t	2026-08-06 10:59:00+00	2026-08-06 10:59:00+00
a69f40d7-9950-455b-b2db-9a1a35f6b4c2	Thisaru Wickramasinghe	thisaru.wickramasinghe@example.com	0721896886	$2a$12$r2YOVPZocEf/aLlR2lurreXiBMlq7K4y1bw.8X3H6mjP530uIJSuK	ACTIVE	t	2026-09-08 12:01:00+00	2026-09-08 12:01:00+00
ba7a69cd-815c-448c-ada9-f7a49ac6bb50	Lahiru Nawaz	lahiru.nawaz@example.com	0700341717	$2a$12$94dlZr5AHqnSv/TRAVuDEOwnSWtfIn0cYBAFd77RWU5w0aQAoSsj2	ACTIVE	t	2026-10-02 03:57:00+00	2026-10-02 03:57:00+00
ba860191-4254-4f32-ab5d-03b2adbee345	Tharindu Rahman	tharindu.rahman@example.com	0786177595	$2a$12$KXCPgGxUCuYIwWkGKSCgOO7srlMV4iu2Z3J9nBNEsq15JqkcwBHRu	ACTIVE	t	2026-09-22 05:01:00+00	2026-09-22 05:01:00+00
38a8b458-db4b-4a49-9866-91e79ed8cb4c	Methmi Peiris	methmi.peiris@example.com	0717295928	$2a$12$DF8j7Ly5qVrN0Zv6ATMmHOYf/H/NjVXAbBTgKhUTaRFwy9Bka1cyW	ACTIVE	t	2026-10-06 02:54:00+00	2026-10-06 02:54:00+00
9f76138f-3fc1-4b8f-bb9f-5df28b84cd2c	Hashini Weerasinghe	hashini.weerasinghe@example.com	0785757443	$2a$12$rSKZmz4zkGlquYFWQVs2Ge4LNkYUkZ4PRtiWc6mDzhmX9t6c0LrAG	ACTIVE	t	2026-08-26 09:38:00+00	2026-08-26 09:38:00+00
18dbe225-6150-4e1b-b41c-e7109ccbd18e	Methmi Cooray	methmi.cooray@example.com	0765663118	$2a$12$uQiE2NpVMLOYWQ/1tTTFqu2NK7WoJXpIcf.gtHzWjHSyYN84eheK.	ACTIVE	t	2026-08-15 06:47:00+00	2026-08-15 06:47:00+00
b4f1804b-2b88-4a98-b6ba-0913cd259045	Sanduni Silva	sanduni.silva@example.com	0727631910	$2a$12$J8t7G/i0piQV8NcF6qSn4emvRFr.UU1lyolrVuYkL3fAr2/LmThpu	ACTIVE	t	2026-09-08 08:09:00+00	2026-09-08 08:09:00+00
d643c832-0366-4e10-b444-63f2cc479424	Amaya Senanayake	amaya.senanayake@example.com	0721398560	$2a$12$V.WdjR/io4jXbLymDRAf9uLxlETAHSMuHYSJwby.suAPy5PnRe3ji	ACTIVE	t	2026-09-05 10:15:00+00	2026-09-05 10:15:00+00
c7a61ae5-05fc-49d7-9bd1-f8093be4f0e3	Sanduni Perera	sanduni.perera@example.com	0789265864	$2a$12$xJSDWf5cGrvraw/XLLMUVed64LHcS4Ip0GdU.HwgfNQqK4jodu1em	ACTIVE	t	2026-08-15 10:22:00+00	2026-08-15 10:22:00+00
7cbce484-9762-48e8-8fda-891d5d088cee	Dulani Jayawardena	dulani.jayawardena@example.com	0767207213	$2a$12$jnCXV95j50JFUnMXewTlReAuiM1Q8sooNxs0F5rCNeKUW3Ed6kMgi	ACTIVE	t	2026-08-15 10:44:00+00	2026-08-15 10:44:00+00
f1c98d80-55e5-4e75-8f22-56fc86bfc894	Dinuka Ratnayake	dinuka.ratnayake@example.com	0709621201	$2a$12$siC.Io9K3F9vYzw7pYy20ukNM84//C3M5jU3rCzfdjGUt476ugszu	ACTIVE	t	2026-08-09 12:41:00+00	2026-08-09 12:41:00+00
bc89ad84-c579-4c07-9452-41aff3eade38	Rizwan Jayawardena	rizwan.jayawardena@example.com	0746337605	$2a$12$g4xbuw7sBd8iPxf9QacKIu5pb02Q38ZbDXavkS8BtLm6Yqlc3ZOUO	ACTIVE	t	2026-09-10 07:17:00+00	2026-09-10 07:17:00+00
ab8979a9-f86a-4114-a57a-05c1bbb047a0	Senuri Gunasekara	senuri.gunasekara@example.com	0769629453	$2a$12$b9aVhdzoGih0gJVWMpxbUO66q7JV7MRVdZ2tOZo3IUSYUoHOsx9ve	ACTIVE	t	2026-08-27 04:36:00+00	2026-08-27 04:36:00+00
35e35258-a722-428c-bf10-8318c9542f33	Yasiru Amarasekara	yasiru.amarasekara@example.com	0788162462	$2a$12$3GH0EtuEmmM0vL5lQnJBZObqM8smgjNkyOWYfcOMsaD0KtzKSfIbS	ACTIVE	t	2026-09-20 12:39:00+00	2026-09-20 12:39:00+00
b01faa8b-97a4-4fec-b8e6-83570b20abe1	Senuri Karunaratne	senuri.karunaratne@example.com	0705100214	$2a$12$P0PlwjqqSQHDiFR9YpE3Gukzao18vBMeIsYitd2Vwc19v6eYBvg1q	ACTIVE	t	2026-10-11 09:28:00+00	2026-10-11 09:28:00+00
7837a8c9-5849-4620-8032-4c6db4bd64b9	Ayesha Amarasekara	ayesha.amarasekara@example.com	0712374484	$2a$12$qDrs5zwbjmLVG9kwMPs8VeWhrbttjNZUdOX8BtygFarasWd1uZHbO	ACTIVE	t	2026-09-23 03:43:00+00	2026-09-23 03:43:00+00
894da928-9d42-4091-a210-f889650603cf	Ishara Karunaratne	ishara.karunaratne@example.com	0761346321	$2a$12$9uq5SVeQtqXYC1.hW.ElUuWQ3UJ0L6VHhGh8anU4rR4xmHHgvN4my	ACTIVE	t	2026-08-29 11:03:00+00	2026-08-29 11:03:00+00
ca560003-b29d-4548-8e8a-db0e365ffacb	Ayesha Herath	ayesha.herath@example.com	0770296518	$2a$12$FlyhTUJ3d3yCjMgVd3QufeiqLoWUZv2PIWfJFFK/HOroq4Dy/hmv6	ACTIVE	t	2026-08-02 07:06:00+00	2026-08-02 07:06:00+00
0e48a1cf-a0d4-46ab-9355-bff3bd5b058c	Isuru Senanayake	isuru.senanayake@example.com	0769558037	$2a$12$Chr1ugme9RYU.OrfxSS0AOyuNABkf.oH.8ALw7K08/r8gDpQkap1m	ACTIVE	t	2026-09-03 07:46:00+00	2026-09-03 07:46:00+00
cbf74fc9-af4a-4124-8fc3-8e6aa92a6b4f	Tharindu Silva	tharindu.silva@example.com	0788424029	$2a$12$6adYYlyY2r6OlCmjgeXJqO/rMHOglgzRuudNMvooboXGT1M8eOnNW	ACTIVE	t	2026-10-13 03:36:00+00	2026-10-13 03:36:00+00
41e925fa-e3cf-4ad8-b97b-590dddb8eb7e	Kavindi Nawaz	kavindi.nawaz@example.com	0710147275	$2a$12$yvP1z5IRqYkAya0bcULYmutQSXvSbPG/rQIO8Q5aAyeWZkabDix4S	ACTIVE	t	2026-09-30 06:14:00+00	2026-09-30 06:14:00+00
da3a2f45-be06-4092-bcc9-dba48859fd08	Hashini Wijesinghe	hashini.wijesinghe@example.com	0727662295	$2a$12$pGI7inO1VoI.T9heEmTqeOyijBtxxWnok0cqLxioo.4GoV4yuJJa.	ACTIVE	t	2026-10-09 05:52:00+00	2026-10-09 05:52:00+00
7854b03a-8c06-47a6-8c46-971d35592326	Pradeep Nawaz	pradeep.nawaz@example.com	0704877974	$2a$12$oXpgLHCMXuj636L5YYd3j.ZEQO8HOLA0jOCP4r81jGCEahvf1r6tm	ACTIVE	t	2026-10-07 07:39:00+00	2026-10-07 07:39:00+00
4a070202-5651-4fc2-8a83-9453e5948ea0	Nipuni Karunaratne	nipuni.karunaratne@example.com	0760873486	$2a$12$4ZxENLQ1CEhQZ9xwGnYuOO/R5xF9P.5c2ZmITC86ZpVm53vBV6osi	ACTIVE	t	2026-08-10 08:48:00+00	2026-08-10 08:48:00+00
99e69915-044d-46d1-ac17-0dec6d89ef9e	Tharushi Sivakumar	tharushi.sivakumar@example.com	0764929388	$2a$12$luLiw5LNdIyxDBAk9XD0c.TMi/hGUKld4FD82unlTP0dpZN7eZORe	ACTIVE	t	2026-08-02 12:00:00+00	2026-08-02 12:00:00+00
5e62844d-8f4d-489e-96d9-44e67ff6da2c	Isuru Perera	isuru.perera@example.com	0707429766	$2a$12$zjVqhIouUjOILzpVSMj3WeWC72mJx0XRgP2s9UKZgociO9SzlLpVq	ACTIVE	t	2026-08-02 13:21:00+00	2026-08-02 13:21:00+00
47bc625f-72d4-4c14-b74e-ae6bb4ff01ff	Ishara de Silva	ishara.de.silva@example.com	0768566616	$2a$12$LHwVg3AJhTsWE0yoXPF.Q.Qfu3LB1JldLbySbIDjzXXDsOBslgu4i	ACTIVE	t	2026-10-12 09:27:00+00	2026-10-12 09:27:00+00
69efc098-9961-44e4-9608-e4401147d661	Kamal Wickramasinghe	kamal.wickramasinghe@example.com	0721585856	$2a$12$e19DZZ3o.q2e4mzDeuYiCedXmyDcT.UjS7N03knghFkxPNkcQW0aK	ACTIVE	t	2026-09-13 13:16:00+00	2026-09-13 13:16:00+00
d4db9fa2-3261-4580-820e-9174ddc27975	Chamari Perera	chamari.perera@example.com	0706725897	$2a$12$aW//vS04eU0GCMT6cOZy/OV8/Z1ait7LNG6C6qW1af8LLDN3QPKNa	ACTIVE	t	2026-08-12 07:21:00+00	2026-08-12 07:21:00+00
893ba1fd-f5b8-406d-a7d8-846f7b7370eb	Senuri Jayasinghe	senuri.jayasinghe@example.com	0743647515	$2a$12$.ZEPTQeK9yGFKzsq/SWEV.d9lcT6VQ53DS0I0KXdKl66SzeTGWW9e	ACTIVE	t	2026-07-28 04:08:00+00	2026-07-28 04:08:00+00
926acd4b-b097-4ed8-b69e-f6ef48e5dec3	Yasiru Dissanayake	yasiru.dissanayake@example.com	0760580653	$2a$12$7E8FJcuIXNtVu/uVmc4rUeZzCV9y8XV7cLsPWfsU90p3q9mZQxK/C	ACTIVE	t	2026-08-06 04:19:00+00	2026-08-06 04:19:00+00
403f89f3-daab-43e4-a095-8c5382a79c41	Supuni Abeysekera	supuni.abeysekera@example.com	0744471904	$2a$12$fz1zMN7DhSU3AB4rGnayQ.PesvtNqU.yG44YKByrPpgcosJLLwcYy	ACTIVE	t	2026-09-23 08:19:00+00	2026-09-23 08:19:00+00
faf30155-d50f-4fd5-a24a-c85f69826132	Dinuka Rajapaksa	dinuka.rajapaksa@example.com	0745802325	$2a$12$dw/OYCf2e8cLfII0MXX1v.3N1bq2FzehNzg6pnKG.kbWeQfeD/3UW	ACTIVE	t	2026-08-10 05:29:00+00	2026-08-10 05:29:00+00
679fce35-4d90-404d-abdf-3208533d5c24	Sachintha Dilshan	admin@swiftdrop.example.com	0703897827	$2a$12$pgiisgnUZDVbsAWl/gmcoOsWSKJprZ6V/EKvULxWOq4cVHJN.zngq	ACTIVE	t	2026-06-17 02:33:00+00	2026-06-17 02:33:00+00
8b792576-f64c-48bc-9eaa-1865563cfdb5	Ops Admin	ops.admin@swiftdrop.example.com	0707362069	$2a$12$iNL6Dtwh5rephdsZrXlvg.DHJyQZGBcW194e5IVzNkeuD3Ne64Uwi	ACTIVE	t	2026-06-11 04:27:00+00	2026-06-11 04:27:00+00
49c139c8-8007-4199-80b6-e68a928a59fb	Hiruni Nawaz	dispatcher1@swiftdrop.example.com	0766643372	$2a$12$W7cWqHjBsd9yfGqWvOzhae3DatdSuSczqOcQlRaiPG6ezXukYoheq	ACTIVE	t	2026-07-30 12:47:00+00	2026-07-30 12:47:00+00
80d23eb8-1156-49dc-b5c3-42cdedfab75d	Hiruni Weerasinghe	dispatcher2@swiftdrop.example.com	0763536741	$2a$12$BW8SmN5AsRXQvy.uezf01.IIM3E6j4j0e6T1ny3qICzQFsU1v620q	ACTIVE	t	2026-07-11 03:05:00+00	2026-07-11 03:05:00+00
7d19bfe5-daf8-4879-8f88-7fcc9bff34c5	Sajith Wijesinghe	dispatcher3@swiftdrop.example.com	0765276291	$2a$12$K6rerjFDW./u1rItF9gWjeXp3tfnuNg0T.YAlIYv0EETV38lx6j2S	ACTIVE	t	2026-07-21 10:35:00+00	2026-07-21 10:35:00+00
c5592d9f-a72a-4822-9080-fd0ccc511add	Malith Wijesinghe	courier01@swiftdrop.example.com	0705825274	$2a$12$p97VWRrnXIcUUEf99Nml4Okz1LwHCMbg8GiPYz4yAShMJBHRcWuMW	ACTIVE	t	2026-08-10 09:42:00+00	2026-08-10 09:42:00+00
a1452cf6-4d4b-4a28-860e-518d81821912	Supuni Jayasinghe	courier02@swiftdrop.example.com	0744232026	$2a$12$16.HD2RHMUJlF1SIQb5z7utQIAXIw0M133Flzn0sgsBp7LjJuMwGq	ACTIVE	t	2026-07-09 02:40:00+00	2026-07-09 02:40:00+00
42b44f66-66c4-44b8-af94-031065494ba7	Mohamed Weerasinghe	courier03@swiftdrop.example.com	0745692595	$2a$12$lmB76lgoYr4ZsAJ4497o/.Drl/ku4Slo4pZoDpHZGY6cTlu3lX8eC	ACTIVE	t	2026-08-07 12:35:00+00	2026-08-07 12:35:00+00
73623c45-6a11-4080-98da-1a004bacbf48	Dimuth Nawaz	courier04@swiftdrop.example.com	0728377257	$2a$12$1HdMrNVYBWLgHAPmbDWRB.4B3/fjQCB41I9bo2HcTouUFT1Ci4sw.	ACTIVE	t	2026-08-21 11:26:00+00	2026-08-21 11:26:00+00
28e8368b-8a0e-4fa6-8a8f-db7172b8c354	Ishara Rahman	courier05@swiftdrop.example.com	0778737854	$2a$12$bhpbGU0svMNu1htHPwsXaeYJY5N09jBzNqlxYvvp3PeoG6Y1wIPvC	ACTIVE	t	2026-09-01 05:54:00+00	2026-09-01 05:54:00+00
88670f51-c181-48ba-b6ff-1342422fa974	Sachini Fonseka	courier06@swiftdrop.example.com	0767793065	$2a$12$C0fRXDC3j6YIC4kC8rdEquUTJ8BDUddyJYseisXNGRXjnppt8iALK	ACTIVE	t	2026-08-18 03:35:00+00	2026-08-18 03:35:00+00
8a9f6979-f1cc-4539-a393-1baa9bb70bad	Thisaru Liyanage	courier07@swiftdrop.example.com	0746284855	$2a$12$.e2R1NImIrgZp0gHori5geqLsF1cZeQbjmpy.2TAT3wQMkQIRv2Fm	ACTIVE	t	2026-09-02 06:40:00+00	2026-09-02 06:40:00+00
2312f6f5-ecd5-4e40-8ed1-8bfbfe15c53a	Supuni Nawaz	courier08@swiftdrop.example.com	0701627611	$2a$12$.vJxDBTjLC06LQCWc91gQuU5Eljo1M0JA2S1YPP45LHu4drnXAyiy	ACTIVE	t	2026-07-16 05:43:00+00	2026-07-16 05:43:00+00
175873ad-65ef-45e0-91ea-d52af596bf9e	Hashini Ratnayake	courier09@swiftdrop.example.com	0782832894	$2a$12$mWLKuLtVYnO0KZXuewkEneVavVqUH4iD75eFVykJydlC0qdfefaya	ACTIVE	t	2026-07-23 04:27:00+00	2026-07-23 04:27:00+00
9875ccd7-f7f9-4a43-bbbd-c1fd75a9407e	Pradeep Ratnayake	courier10@swiftdrop.example.com	0700045175	$2a$12$oX/eBuXbkU1qkEyOJedqCO.BgqJprg1Fy.W2LHeBfq6OPkcIZ0cfm	ACTIVE	t	2026-07-14 05:37:00+00	2026-07-14 05:37:00+00
5bbfdaf0-3d8d-4ceb-bd93-00c91573ac23	Ruwan Fernando	ruwan.fernando@merchant.example.com	0772401774	$2a$12$blJtB7eO22Sz7V9r.3vk5.C1YkyhiLKsL.OSSkkOYAYThh22/RE4G	ACTIVE	t	2026-09-20 06:06:00+00	2026-09-20 06:06:00+00
30056f25-6559-438c-9980-4e2a84365b06	Sajith Karunaratne	sajith.karunaratne@merchant.example.com	0774891635	$2a$12$ZlYp4ibByg12/27us6Hx7O9DJIcubcBYA5LcIp66e4RHXAWzGg2Be	ACTIVE	t	2026-09-01 10:06:00+00	2026-09-01 10:06:00+00
cc91bacd-b679-443d-8d92-da68e6bdc2be	Lahiru Senanayake	lahiru.senanayake@merchant.example.com	0770480984	$2a$12$Ycw4NK4wS6kcwiDUEynxEezELC.9kzMGSBI9YDhXso3nrLP29MrzS	ACTIVE	t	2026-09-18 03:49:00+00	2026-09-18 03:49:00+00
9e6c3a6a-7c87-4f1d-a0b1-6509fb5a96d3	Lahiru Wijesinghe	lahiru.wijesinghe@merchant.example.com	0782262943	$2a$12$nQfzpW4yKiyyb8DwBgsz9.KyYFYQH/50lsaNcDBb8IVXZw2ArixBW	ACTIVE	t	2026-09-17 06:19:00+00	2026-09-17 06:19:00+00
8131f094-5b5f-4331-8afd-7d7f917074e5	Chathura Perera	chathura.perera@merchant.example.com	0770943404	$2a$12$hb41xG2YlF1TtK/EG9TvuO1/Bw8jeqK0It.ibNvL5XfFRQudDlv7i	ACTIVE	t	2026-09-21 13:26:00+00	2026-09-21 13:26:00+00
abb1016e-35d4-40b4-a1d9-e5a308e17a5d	Demo Customer	customer@example.com	0702392760	$2a$12$KVnyn7t8gHLyIUEllPJgYez6.lOgY0dz2wNLghu43cCxaP115I1xm	ACTIVE	t	2026-08-03 12:05:00+00	2026-08-03 12:05:00+00
\.


--
-- Name: roles_id_seq; Type: SEQUENCE SET; Schema: public; Owner: auth
--

SELECT pg_catalog.setval('public.roles_id_seq', 5, true);


--
-- Name: courier_profiles courier_profiles_nic_number_key; Type: CONSTRAINT; Schema: public; Owner: auth
--

ALTER TABLE ONLY public.courier_profiles
    ADD CONSTRAINT courier_profiles_nic_number_key UNIQUE (nic_number);


--
-- Name: courier_profiles courier_profiles_pkey; Type: CONSTRAINT; Schema: public; Owner: auth
--

ALTER TABLE ONLY public.courier_profiles
    ADD CONSTRAINT courier_profiles_pkey PRIMARY KEY (user_id);


--
-- Name: flyway_schema_history flyway_schema_history_pk; Type: CONSTRAINT; Schema: public; Owner: auth
--

ALTER TABLE ONLY public.flyway_schema_history
    ADD CONSTRAINT flyway_schema_history_pk PRIMARY KEY (installed_rank);


--
-- Name: merchant_profiles merchant_profiles_business_registration_number_key; Type: CONSTRAINT; Schema: public; Owner: auth
--

ALTER TABLE ONLY public.merchant_profiles
    ADD CONSTRAINT merchant_profiles_business_registration_number_key UNIQUE (business_registration_number);


--
-- Name: merchant_profiles merchant_profiles_pkey; Type: CONSTRAINT; Schema: public; Owner: auth
--

ALTER TABLE ONLY public.merchant_profiles
    ADD CONSTRAINT merchant_profiles_pkey PRIMARY KEY (user_id);


--
-- Name: oauth_accounts oauth_accounts_pkey; Type: CONSTRAINT; Schema: public; Owner: auth
--

ALTER TABLE ONLY public.oauth_accounts
    ADD CONSTRAINT oauth_accounts_pkey PRIMARY KEY (id);


--
-- Name: oauth_login_codes oauth_login_codes_code_hash_key; Type: CONSTRAINT; Schema: public; Owner: auth
--

ALTER TABLE ONLY public.oauth_login_codes
    ADD CONSTRAINT oauth_login_codes_code_hash_key UNIQUE (code_hash);


--
-- Name: oauth_login_codes oauth_login_codes_pkey; Type: CONSTRAINT; Schema: public; Owner: auth
--

ALTER TABLE ONLY public.oauth_login_codes
    ADD CONSTRAINT oauth_login_codes_pkey PRIMARY KEY (id);


--
-- Name: refresh_tokens refresh_tokens_pkey; Type: CONSTRAINT; Schema: public; Owner: auth
--

ALTER TABLE ONLY public.refresh_tokens
    ADD CONSTRAINT refresh_tokens_pkey PRIMARY KEY (id);


--
-- Name: refresh_tokens refresh_tokens_token_hash_key; Type: CONSTRAINT; Schema: public; Owner: auth
--

ALTER TABLE ONLY public.refresh_tokens
    ADD CONSTRAINT refresh_tokens_token_hash_key UNIQUE (token_hash);


--
-- Name: roles roles_name_key; Type: CONSTRAINT; Schema: public; Owner: auth
--

ALTER TABLE ONLY public.roles
    ADD CONSTRAINT roles_name_key UNIQUE (name);


--
-- Name: roles roles_pkey; Type: CONSTRAINT; Schema: public; Owner: auth
--

ALTER TABLE ONLY public.roles
    ADD CONSTRAINT roles_pkey PRIMARY KEY (id);


--
-- Name: oauth_accounts uq_oauth_provider_account; Type: CONSTRAINT; Schema: public; Owner: auth
--

ALTER TABLE ONLY public.oauth_accounts
    ADD CONSTRAINT uq_oauth_provider_account UNIQUE (provider, provider_user_id);


--
-- Name: user_roles user_roles_pkey; Type: CONSTRAINT; Schema: public; Owner: auth
--

ALTER TABLE ONLY public.user_roles
    ADD CONSTRAINT user_roles_pkey PRIMARY KEY (user_id, role_id);


--
-- Name: users users_pkey; Type: CONSTRAINT; Schema: public; Owner: auth
--

ALTER TABLE ONLY public.users
    ADD CONSTRAINT users_pkey PRIMARY KEY (id);


--
-- Name: flyway_schema_history_s_idx; Type: INDEX; Schema: public; Owner: auth
--

CREATE INDEX flyway_schema_history_s_idx ON public.flyway_schema_history USING btree (success);


--
-- Name: idx_oauth_accounts_user_id; Type: INDEX; Schema: public; Owner: auth
--

CREATE INDEX idx_oauth_accounts_user_id ON public.oauth_accounts USING btree (user_id);


--
-- Name: idx_oauth_login_codes_expires_at; Type: INDEX; Schema: public; Owner: auth
--

CREATE INDEX idx_oauth_login_codes_expires_at ON public.oauth_login_codes USING btree (expires_at);


--
-- Name: idx_refresh_tokens_user_id; Type: INDEX; Schema: public; Owner: auth
--

CREATE INDEX idx_refresh_tokens_user_id ON public.refresh_tokens USING btree (user_id);


--
-- Name: idx_user_roles_role_id; Type: INDEX; Schema: public; Owner: auth
--

CREATE INDEX idx_user_roles_role_id ON public.user_roles USING btree (role_id);


--
-- Name: uq_users_email_lower; Type: INDEX; Schema: public; Owner: auth
--

CREATE UNIQUE INDEX uq_users_email_lower ON public.users USING btree (lower((email)::text));


--
-- Name: uq_users_mobile_number; Type: INDEX; Schema: public; Owner: auth
--

CREATE UNIQUE INDEX uq_users_mobile_number ON public.users USING btree (mobile_number) WHERE ((mobile_number IS NOT NULL) AND (btrim((mobile_number)::text) <> ''::text));


--
-- Name: courier_profiles courier_profiles_reviewed_by_fkey; Type: FK CONSTRAINT; Schema: public; Owner: auth
--

ALTER TABLE ONLY public.courier_profiles
    ADD CONSTRAINT courier_profiles_reviewed_by_fkey FOREIGN KEY (reviewed_by) REFERENCES public.users(id);


--
-- Name: courier_profiles courier_profiles_user_id_fkey; Type: FK CONSTRAINT; Schema: public; Owner: auth
--

ALTER TABLE ONLY public.courier_profiles
    ADD CONSTRAINT courier_profiles_user_id_fkey FOREIGN KEY (user_id) REFERENCES public.users(id) ON DELETE CASCADE;


--
-- Name: merchant_profiles merchant_profiles_reviewed_by_fkey; Type: FK CONSTRAINT; Schema: public; Owner: auth
--

ALTER TABLE ONLY public.merchant_profiles
    ADD CONSTRAINT merchant_profiles_reviewed_by_fkey FOREIGN KEY (reviewed_by) REFERENCES public.users(id);


--
-- Name: merchant_profiles merchant_profiles_user_id_fkey; Type: FK CONSTRAINT; Schema: public; Owner: auth
--

ALTER TABLE ONLY public.merchant_profiles
    ADD CONSTRAINT merchant_profiles_user_id_fkey FOREIGN KEY (user_id) REFERENCES public.users(id) ON DELETE CASCADE;


--
-- Name: oauth_accounts oauth_accounts_user_id_fkey; Type: FK CONSTRAINT; Schema: public; Owner: auth
--

ALTER TABLE ONLY public.oauth_accounts
    ADD CONSTRAINT oauth_accounts_user_id_fkey FOREIGN KEY (user_id) REFERENCES public.users(id) ON DELETE CASCADE;


--
-- Name: oauth_login_codes oauth_login_codes_user_id_fkey; Type: FK CONSTRAINT; Schema: public; Owner: auth
--

ALTER TABLE ONLY public.oauth_login_codes
    ADD CONSTRAINT oauth_login_codes_user_id_fkey FOREIGN KEY (user_id) REFERENCES public.users(id) ON DELETE CASCADE;


--
-- Name: refresh_tokens refresh_tokens_replaced_by_fkey; Type: FK CONSTRAINT; Schema: public; Owner: auth
--

ALTER TABLE ONLY public.refresh_tokens
    ADD CONSTRAINT refresh_tokens_replaced_by_fkey FOREIGN KEY (replaced_by) REFERENCES public.refresh_tokens(id);


--
-- Name: refresh_tokens refresh_tokens_user_id_fkey; Type: FK CONSTRAINT; Schema: public; Owner: auth
--

ALTER TABLE ONLY public.refresh_tokens
    ADD CONSTRAINT refresh_tokens_user_id_fkey FOREIGN KEY (user_id) REFERENCES public.users(id) ON DELETE CASCADE;


--
-- Name: user_roles user_roles_reviewed_by_fkey; Type: FK CONSTRAINT; Schema: public; Owner: auth
--

ALTER TABLE ONLY public.user_roles
    ADD CONSTRAINT user_roles_reviewed_by_fkey FOREIGN KEY (reviewed_by) REFERENCES public.users(id);


--
-- Name: user_roles user_roles_role_id_fkey; Type: FK CONSTRAINT; Schema: public; Owner: auth
--

ALTER TABLE ONLY public.user_roles
    ADD CONSTRAINT user_roles_role_id_fkey FOREIGN KEY (role_id) REFERENCES public.roles(id);


--
-- Name: user_roles user_roles_user_id_fkey; Type: FK CONSTRAINT; Schema: public; Owner: auth
--

ALTER TABLE ONLY public.user_roles
    ADD CONSTRAINT user_roles_user_id_fkey FOREIGN KEY (user_id) REFERENCES public.users(id) ON DELETE CASCADE;


--
-- PostgreSQL database dump complete
--

\unrestrict JUWduxqYUp1doH9VRhayQnucKa3h1nghzNL2OWQfGCsNbgRFafN0vmHXmz6Z6BJ

