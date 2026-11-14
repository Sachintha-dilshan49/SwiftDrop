--
-- PostgreSQL database dump
--

\restrict WcQfvp6YkBzsZg78fW2tx53DWdUJtt9gjInzGMgJoFFpoNC5k1GPdeAdY9toJoK

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
ALTER TABLE IF EXISTS ONLY public.flyway_schema_history DROP CONSTRAINT IF EXISTS flyway_schema_history_pk;
DROP TABLE IF EXISTS public.users;
DROP TABLE IF EXISTS public.user_roles;
DROP TABLE IF EXISTS public.roles;
DROP TABLE IF EXISTS public.refresh_tokens;
DROP TABLE IF EXISTS public.oauth_login_codes;
DROP TABLE IF EXISTS public.oauth_accounts;
DROP TABLE IF EXISTS public.flyway_schema_history;
SET default_tablespace = '';

SET default_table_access_method = heap;

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
-- Data for Name: flyway_schema_history; Type: TABLE DATA; Schema: public; Owner: auth
--

COPY public.flyway_schema_history (installed_rank, version, description, type, script, checksum, installed_by, installed_on, execution_time, success) FROM stdin;
1	1	create auth tables	SQL	V1__create_auth_tables.sql	427391598	auth	2026-11-14 14:11:36.752002	64	t
2	2	create oauth login codes	SQL	V2__create_oauth_login_codes.sql	-2005132465	auth	2026-11-14 14:11:36.873384	11	t
3	3	unique user mobile numbers	SQL	V3__unique_user_mobile_numbers.sql	1535160245	auth	2026-11-14 14:11:36.897776	5	t
4	4	drop profile tables	SQL	V4__drop_profile_tables.sql	934928446	auth	2026-11-14 14:11:36.910325	17	t
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
eeab7ea5-20bc-4d80-b757-447ed3fa06f0	5	ACTIVE	2026-06-17 09:21:00+00	2026-06-17 15:21:00+00	\N
2aea47d3-a06d-4dba-b302-a2a6c6c25e37	5	ACTIVE	2026-06-17 10:09:00+00	2026-06-18 03:09:00+00	eeab7ea5-20bc-4d80-b757-447ed3fa06f0
cb4cd22c-d4cd-4f2f-ad43-c2e73f1b9cb5	4	ACTIVE	2026-08-21 04:02:00+00	2026-08-21 19:02:00+00	eeab7ea5-20bc-4d80-b757-447ed3fa06f0
3900455f-0834-4d57-85c5-c3de92f5ecee	4	ACTIVE	2026-08-07 04:35:00+00	2026-08-08 08:35:00+00	eeab7ea5-20bc-4d80-b757-447ed3fa06f0
7bf5a598-1908-4a34-9ac4-041d3ce99466	4	ACTIVE	2026-07-08 04:50:00+00	2026-07-08 22:50:00+00	eeab7ea5-20bc-4d80-b757-447ed3fa06f0
9e3b8266-acf9-4126-91bb-369d59ad25a7	3	ACTIVE	2026-08-13 12:07:00+00	2026-08-14 12:07:00+00	eeab7ea5-20bc-4d80-b757-447ed3fa06f0
69f3dbf3-6a51-4f41-8634-af3d608c2527	3	ACTIVE	2026-08-29 07:37:00+00	2026-08-29 09:37:00+00	eeab7ea5-20bc-4d80-b757-447ed3fa06f0
2296db41-b3a5-4443-a906-df1b2a23b26c	3	ACTIVE	2026-08-02 10:06:00+00	2026-08-02 15:06:00+00	eeab7ea5-20bc-4d80-b757-447ed3fa06f0
5b32b607-0ba6-4ff9-8b45-e7e15baf395b	3	ACTIVE	2026-08-30 13:19:00+00	2026-08-31 18:19:00+00	eeab7ea5-20bc-4d80-b757-447ed3fa06f0
fd5016f3-f6ab-462f-a5ed-0a2fb7ee2491	3	ACTIVE	2026-07-31 06:02:00+00	2026-08-01 03:02:00+00	eeab7ea5-20bc-4d80-b757-447ed3fa06f0
2064432d-0cca-4c76-ac2b-47cd07270ec7	3	ACTIVE	2026-07-14 11:52:00+00	2026-07-14 23:52:00+00	eeab7ea5-20bc-4d80-b757-447ed3fa06f0
c4750ad6-4949-4e9e-b59c-1c8af8f71294	3	ACTIVE	2026-07-24 10:30:00+00	2026-07-24 15:30:00+00	eeab7ea5-20bc-4d80-b757-447ed3fa06f0
d28074b5-475a-4e20-b5d3-63080c9f1b7f	3	ACTIVE	2026-08-12 07:32:00+00	2026-08-12 10:32:00+00	eeab7ea5-20bc-4d80-b757-447ed3fa06f0
854d0d01-8a43-4f8c-b742-ee0202e4e1b8	3	ACTIVE	2026-08-18 13:04:00+00	2026-08-19 08:04:00+00	eeab7ea5-20bc-4d80-b757-447ed3fa06f0
3be525c0-b776-4ec1-b343-38831e36255d	3	ACTIVE	2026-09-02 04:40:00+00	2026-09-02 19:40:00+00	eeab7ea5-20bc-4d80-b757-447ed3fa06f0
c50a9935-b0a4-42a7-8b67-834c582c0b41	1	ACTIVE	2026-09-04 04:35:00+00	\N	\N
c50a9935-b0a4-42a7-8b67-834c582c0b41	2	ACTIVE	2026-09-04 04:35:00+00	\N	\N
71ac489c-40e8-4057-bd8d-22645457f877	1	ACTIVE	2026-09-19 11:13:00+00	\N	\N
71ac489c-40e8-4057-bd8d-22645457f877	2	ACTIVE	2026-09-19 11:13:00+00	\N	\N
ac219f99-85fb-43a8-90b2-786b06a4d80b	1	ACTIVE	2026-09-14 10:35:00+00	\N	\N
ac219f99-85fb-43a8-90b2-786b06a4d80b	2	ACTIVE	2026-09-14 10:35:00+00	\N	\N
c8b97194-64f2-4ced-bc12-b5c4b7d4d9ee	1	ACTIVE	2026-09-02 04:57:00+00	\N	\N
c8b97194-64f2-4ced-bc12-b5c4b7d4d9ee	2	ACTIVE	2026-09-02 04:57:00+00	\N	\N
9b13b98c-873f-4749-958b-2f913cc7e712	1	ACTIVE	2026-09-09 10:31:00+00	\N	\N
9b13b98c-873f-4749-958b-2f913cc7e712	2	ACTIVE	2026-09-09 10:31:00+00	\N	\N
fff7e75b-4ded-4f40-8614-627687e7ec32	1	ACTIVE	2026-09-19 05:24:00+00	\N	\N
fff7e75b-4ded-4f40-8614-627687e7ec32	2	ACTIVE	2026-09-19 05:24:00+00	\N	\N
533bff2a-8e8b-4b3e-91f3-9a06ff07c37a	1	ACTIVE	2026-08-29 10:12:00+00	\N	\N
73e39455-d811-45cf-a238-602646b5422b	1	ACTIVE	2026-09-02 08:02:00+00	\N	\N
b2691eac-bac7-474e-831a-394cf0a74ad4	1	ACTIVE	2026-08-10 05:40:00+00	\N	\N
5ed4919f-41d3-4113-b146-c0e1fde1d5e1	1	ACTIVE	2026-10-05 05:54:00+00	\N	\N
09113e73-190a-4d24-b29d-7e954c8046f3	1	ACTIVE	2026-08-16 07:20:00+00	\N	\N
fb91c4b1-ee0b-420d-a871-afd90ef87175	1	ACTIVE	2026-10-04 05:56:00+00	\N	\N
ef2804b1-d8ab-4ff3-adfa-286e6cabaa3c	1	ACTIVE	2026-09-12 04:40:00+00	\N	\N
d6f7d004-a3f0-4c13-b832-9a5d2a190277	1	ACTIVE	2026-10-07 05:43:00+00	\N	\N
3c71c5d3-fb79-4b87-9cc2-208259f8bd5c	1	ACTIVE	2026-09-23 08:15:00+00	\N	\N
4a7ec423-fde4-463d-b720-dc701fa6cb67	1	ACTIVE	2026-09-11 10:21:00+00	\N	\N
2d5e78c7-b018-44fa-919a-c88f6dec4d5a	1	ACTIVE	2026-09-14 04:14:00+00	\N	\N
2a67b293-8450-4f8c-9974-de5f22cba511	1	ACTIVE	2026-10-05 02:45:00+00	\N	\N
be990ef6-292c-4890-a29d-f87648582c13	1	ACTIVE	2026-09-10 10:50:00+00	\N	\N
a658ff88-1274-44e4-810f-8c7075b7c364	1	ACTIVE	2026-08-03 12:36:00+00	\N	\N
76176177-992e-488a-8a2c-932dcc5140f1	1	ACTIVE	2026-10-08 03:49:00+00	\N	\N
1bee39c3-f064-4fa6-a337-4c2cc080e2e9	1	ACTIVE	2026-10-06 06:19:00+00	\N	\N
bcb82745-4e74-4a93-b988-080e4ebe656e	1	ACTIVE	2026-10-15 13:26:00+00	\N	\N
09d62189-9682-4b25-84ed-cc7ab1e87651	1	ACTIVE	2026-09-27 11:37:00+00	\N	\N
9e7d6f92-a22a-4978-b95e-6a18c452a849	1	ACTIVE	2026-07-30 11:30:00+00	\N	\N
d5677db6-7def-46a6-a51e-dad63366f591	1	ACTIVE	2026-09-14 03:27:00+00	\N	\N
8884150d-9195-43bf-b123-69dc672f3488	1	ACTIVE	2026-08-19 10:34:00+00	\N	\N
ef5bff6c-992b-4a08-811d-91be3cd0e645	1	ACTIVE	2026-08-08 08:51:00+00	\N	\N
9c8c5f30-6fcd-4f27-a77b-2bb855526690	1	ACTIVE	2026-08-03 12:05:00+00	\N	\N
0d266b85-d4b2-46b2-aafe-5f0ab0b02c0d	1	ACTIVE	2026-09-14 05:47:00+00	\N	\N
e6a07fa6-7259-4d1b-ac3f-8a432f428b8d	1	ACTIVE	2026-08-29 10:55:00+00	\N	\N
d1b1fc3f-f55d-4e46-99ea-704b2d1ef8ff	1	ACTIVE	2026-08-09 09:14:00+00	\N	\N
356e5a5a-ba92-4d21-8017-b5649aa65425	1	ACTIVE	2026-08-16 09:43:00+00	\N	\N
c1423ca5-efb7-4421-9901-4d70ae2163d0	1	ACTIVE	2026-10-09 11:55:00+00	\N	\N
db3b1189-edff-403b-a1e7-09a061a2d405	1	ACTIVE	2026-08-09 02:32:00+00	\N	\N
a5b66fa4-196a-41a2-a15b-dc7bbc57430f	1	ACTIVE	2026-08-24 05:18:00+00	\N	\N
74d0028c-0695-44d9-b8e1-8a522a6e0e34	1	ACTIVE	2026-08-10 13:23:00+00	\N	\N
500636b3-1973-4548-adf1-8666733731b8	1	ACTIVE	2026-08-05 10:07:00+00	\N	\N
fdd7ae27-35e5-417e-b4cd-df85a5fe6cd1	1	ACTIVE	2026-09-08 12:01:00+00	\N	\N
f8668c6d-3bc0-48b3-8ead-474d9523a7b7	1	ACTIVE	2026-09-25 03:34:00+00	\N	\N
4b30e56a-3840-429c-b968-3e885023d0b2	1	ACTIVE	2026-09-12 10:10:00+00	\N	\N
08ce854a-6db5-4855-a2b7-d68e5c8305cf	1	ACTIVE	2026-09-26 10:06:00+00	\N	\N
af229567-0ca1-4397-bf5b-1d8f8b6ffe81	1	ACTIVE	2026-08-19 03:30:00+00	\N	\N
4cecee6d-ec13-4ac2-803f-b9a696d6c972	1	ACTIVE	2026-08-10 12:55:00+00	\N	\N
ceacbbdb-fde0-4a2c-95ed-e1d7ee74c98d	1	ACTIVE	2026-09-29 07:34:00+00	\N	\N
b7e01598-766c-4b81-a1d2-55c4f2fbb709	1	ACTIVE	2026-08-15 06:47:00+00	\N	\N
b02190b7-45c7-4967-baf0-5581d728dd35	1	ACTIVE	2026-07-30 07:15:00+00	\N	\N
2da89375-52f5-47f7-859b-2ac948151468	1	ACTIVE	2026-07-28 04:11:00+00	\N	\N
\.


--
-- Data for Name: users; Type: TABLE DATA; Schema: public; Owner: auth
--

COPY public.users (id, full_name, email, mobile_number, password_hash, account_status, email_verified, created_at, updated_at) FROM stdin;
fff7e75b-4ded-4f40-8614-627687e7ec32	Pradeep Wickramasinghe	pradeep.wickramasinghe@merchant.example.com	0760290556	$2a$12$Bfp1q5XV6S5wjCzwO2/qGeQSR7wHuWh0xMWFvaQS5WqH4jLqaBgP6	ACTIVE	t	2026-09-19 05:24:00+00	2026-09-19 05:24:00+00
533bff2a-8e8b-4b3e-91f3-9a06ff07c37a	Pradeep Rajapaksa	pradeep.rajapaksa@merchant.example.com	0726996524	$2a$12$7/3MyQ25DGJPdhsVivimFuB8UE1NhoZk/DTjYaHGFmd5pg663x2BG	ACTIVE	t	2026-08-29 10:12:00+00	2026-08-29 10:12:00+00
73e39455-d811-45cf-a238-602646b5422b	Oshada Weerasinghe	oshada.weerasinghe@merchant.example.com	0787692766	$2a$12$HVki7RhTfJNQoVFE/fakM.UqvPWLuO9ZT.EGOhwHxtznMZL2ytYc.	ACTIVE	t	2026-09-02 08:02:00+00	2026-09-02 08:02:00+00
5ed4919f-41d3-4113-b146-c0e1fde1d5e1	Anjali Fernando	anjali.fernando@example.com	0761737195	$2a$12$7mHD2bk/PityTMexdebRbOGPqNGbkwKcoS8Gsds0zIErhz8WCQwV6	ACTIVE	t	2026-10-05 05:54:00+00	2026-10-05 05:54:00+00
09113e73-190a-4d24-b29d-7e954c8046f3	Thisaru Senanayake	thisaru.senanayake@example.com	0703171384	$2a$12$Zlxyam5IIyxHFWFvESjKVOieGgt1jAsXZz7Wn6R454z75CuiLygv2	ACTIVE	t	2026-08-16 07:20:00+00	2026-08-16 07:20:00+00
fb91c4b1-ee0b-420d-a871-afd90ef87175	Ishara Gunasekara	ishara.gunasekara@example.com	0774426117	$2a$12$9m91r27e4OvS.hQObu0uCOZZkkzMHKDvtOUF4ZeibYGjiNTXyGA4e	ACTIVE	t	2026-10-04 05:56:00+00	2026-10-04 05:56:00+00
ef2804b1-d8ab-4ff3-adfa-286e6cabaa3c	Tharushi de Silva	tharushi.de.silva@example.com	0706451756	$2a$12$g/9r2kJrTy/.bNv2A2mqjeWYHyowFplt2tyRWMv0.mleicfm6CcWu	ACTIVE	t	2026-09-12 04:40:00+00	2026-09-12 04:40:00+00
d6f7d004-a3f0-4c13-b832-9a5d2a190277	Nipuni Liyanage	nipuni.liyanage@example.com	0767872240	$2a$12$RfdsljPiulLQ2XyTe0ADt.psHY98ThWUmCAqBGa84HI403NVfSiti	ACTIVE	t	2026-10-07 05:43:00+00	2026-10-07 05:43:00+00
3c71c5d3-fb79-4b87-9cc2-208259f8bd5c	Malith de Silva	malith.de.silva@example.com	0701418621	$2a$12$u6y/NrGrxfrPfHyZ48pgZOJ2yVG9.Vn.xCkmKlpLirBv0FjTf.wSe	ACTIVE	t	2026-09-23 08:15:00+00	2026-09-23 08:15:00+00
4a7ec423-fde4-463d-b720-dc701fa6cb67	Thisaru Wickramasinghe	thisaru.wickramasinghe@example.com	0721896886	$2a$12$Sf2U6VrFfTTDiB4L6k/KHOV9mdWOMjjd5V33g74XDrRiTY6Xq3pGa	ACTIVE	t	2026-09-11 10:21:00+00	2026-09-11 10:21:00+00
2d5e78c7-b018-44fa-919a-c88f6dec4d5a	Lahiru Nawaz	lahiru.nawaz@example.com	0700341717	$2a$12$Yv/1sEm.oIcQQTu8tn8SjuENRfBKRe3Bs7eNFnI/nhuPsSJ.YyCqu	ACTIVE	t	2026-09-14 04:14:00+00	2026-09-14 04:14:00+00
2a67b293-8450-4f8c-9974-de5f22cba511	Tharindu Rahman	tharindu.rahman@example.com	0786177595	$2a$12$ZGWQvWoU8CYfeo/vszEVtuRF3GFOdHT40hYL2a7WqFa5jEakeT.HS	ACTIVE	t	2026-10-05 02:45:00+00	2026-10-05 02:45:00+00
be990ef6-292c-4890-a29d-f87648582c13	Methmi Peiris	methmi.peiris@example.com	0717295928	$2a$12$uJauHfp0frfhtFLn7DeJN.vPDuaCqAeOllr3An3M2ShWNkyJI0NWu	ACTIVE	t	2026-09-10 10:50:00+00	2026-09-10 10:50:00+00
a658ff88-1274-44e4-810f-8c7075b7c364	Hashini Weerasinghe	hashini.weerasinghe@example.com	0785757443	$2a$12$CGzvAndeJVxrUMRAjVqyEeAqdZWBH7v8s/E74.VH0DtTBgK9YrsTO	ACTIVE	t	2026-08-03 12:36:00+00	2026-08-03 12:36:00+00
76176177-992e-488a-8a2c-932dcc5140f1	Methmi Cooray	methmi.cooray@example.com	0765663118	$2a$12$/ySxONXa7RA2qWlNqgWjoeucmi4ciTuKt4dK7bOtrherKrvEYIKf.	ACTIVE	t	2026-10-08 03:49:00+00	2026-10-08 03:49:00+00
1bee39c3-f064-4fa6-a337-4c2cc080e2e9	Sanduni Silva	sanduni.silva@example.com	0727631910	$2a$12$vICoTHg4cFG2CiIfNhmrCeh.Qu5mpIHwyDRIFRUJIpwmeh7s2dR1O	ACTIVE	t	2026-10-06 06:19:00+00	2026-10-06 06:19:00+00
bcb82745-4e74-4a93-b988-080e4ebe656e	Amaya Senanayake	amaya.senanayake@example.com	0721398560	$2a$12$mOptKsHbP0Z/6/l3SGUhuuGQVImz2k975XTzW1mBfdE1aANQn2HGu	ACTIVE	t	2026-10-15 13:26:00+00	2026-10-15 13:26:00+00
09d62189-9682-4b25-84ed-cc7ab1e87651	Sanduni Perera	sanduni.perera@example.com	0789265864	$2a$12$ZC48CQogWtd53ZbbgJbs4..jlebd/5OI9U7.ehdYsoCwML.qUAOla	ACTIVE	t	2026-09-27 11:37:00+00	2026-09-27 11:37:00+00
9e7d6f92-a22a-4978-b95e-6a18c452a849	Dulani Jayawardena	dulani.jayawardena@example.com	0767207213	$2a$12$hJ3WaLoBAZL8mVsN3B0/1eOzN1n1FgiFmy7ayW2wV90QJ9I7RX91i	ACTIVE	t	2026-07-30 11:30:00+00	2026-07-30 11:30:00+00
d5677db6-7def-46a6-a51e-dad63366f591	Dinuka Ratnayake	dinuka.ratnayake@example.com	0709621201	$2a$12$DReW43u6wIYC9lBAltTa8.hBIvtzwcKkM79KMVLv1i3caLJqIuy9u	ACTIVE	t	2026-09-14 03:27:00+00	2026-09-14 03:27:00+00
8884150d-9195-43bf-b123-69dc672f3488	Rizwan Jayawardena	rizwan.jayawardena@example.com	0746337605	$2a$12$Mi10wD5sh4damPSCIe4iHurdQ7xYXwrEevL/xD3vQLMvD/mL54dxm	ACTIVE	t	2026-08-19 10:34:00+00	2026-08-19 10:34:00+00
ef5bff6c-992b-4a08-811d-91be3cd0e645	Senuri Gunasekara	senuri.gunasekara@example.com	0769629453	$2a$12$j6QxXJKqOovpFREVM6l1CuoV00OApew01Q7aMXLF/VdCVxMF3BLGS	ACTIVE	t	2026-08-08 08:51:00+00	2026-08-08 08:51:00+00
9c8c5f30-6fcd-4f27-a77b-2bb855526690	Yasiru Amarasekara	yasiru.amarasekara@example.com	0788162462	$2a$12$HwxXpcOTNWzb5hVumGI7lO8vn/kxKNqBK0Z9uP7hxgGM/aLtJcPmq	ACTIVE	t	2026-08-03 12:05:00+00	2026-08-03 12:05:00+00
0d266b85-d4b2-46b2-aafe-5f0ab0b02c0d	Senuri Karunaratne	senuri.karunaratne@example.com	0705100214	$2a$12$rB1GPZrcqwXDn2VIf0nhie6qHuSji9ozwCYgo.cNynB2fD9i6I5V6	ACTIVE	t	2026-09-14 05:47:00+00	2026-09-14 05:47:00+00
e6a07fa6-7259-4d1b-ac3f-8a432f428b8d	Ayesha Amarasekara	ayesha.amarasekara@example.com	0712374484	$2a$12$YJQO/maIaJP/lIHbc8.LwOFxbD73om60O86VNMYL6xWtucBluM1b6	ACTIVE	t	2026-08-29 10:55:00+00	2026-08-29 10:55:00+00
d1b1fc3f-f55d-4e46-99ea-704b2d1ef8ff	Ishara Karunaratne	ishara.karunaratne@example.com	0761346321	$2a$12$92ZG0635F34w1Bbimn2AjeeuoFvlrY7DDby11yQZR8LKINZw8vd.O	ACTIVE	t	2026-08-09 09:14:00+00	2026-08-09 09:14:00+00
356e5a5a-ba92-4d21-8017-b5649aa65425	Ayesha Herath	ayesha.herath@example.com	0770296518	$2a$12$9D7P8gswy88SLHi3wizpGuUDMloxYwRwkhIeODA5InVCfEdwGbWPS	ACTIVE	t	2026-08-16 09:43:00+00	2026-08-16 09:43:00+00
c1423ca5-efb7-4421-9901-4d70ae2163d0	Isuru Senanayake	isuru.senanayake@example.com	0769558037	$2a$12$/VxeGhOIDV5BEz8AKEBqx.9GxDIFghV/cVxs2B7BjZgJU0Q3ysg/e	ACTIVE	t	2026-10-09 11:55:00+00	2026-10-09 11:55:00+00
db3b1189-edff-403b-a1e7-09a061a2d405	Tharindu Silva	tharindu.silva@example.com	0788424029	$2a$12$ZI.t50V83Sw9VtVKMpWJP.I8UBKy3oLgbSSpXoxvCAEbRgUaakVwa	ACTIVE	t	2026-08-09 02:32:00+00	2026-08-09 02:32:00+00
a5b66fa4-196a-41a2-a15b-dc7bbc57430f	Kavindi Nawaz	kavindi.nawaz@example.com	0710147275	$2a$12$78AcleZCDZb.FYNM6kzLpegQNmfJkmU2/NcQif5n1QLzEQxB1l.B.	ACTIVE	t	2026-08-24 05:18:00+00	2026-08-24 05:18:00+00
74d0028c-0695-44d9-b8e1-8a522a6e0e34	Hashini Wijesinghe	hashini.wijesinghe@example.com	0727662295	$2a$12$9zaxfapQR1PZi0WXhPHsD.pda3xth6dkp/cFlBSQrVdivFEvhgw.u	ACTIVE	t	2026-08-10 13:23:00+00	2026-08-10 13:23:00+00
500636b3-1973-4548-adf1-8666733731b8	Pradeep Nawaz	pradeep.nawaz@example.com	0704877974	$2a$12$2FomsyvreTbqBrSbNZ51je8.NNDb4fDXwtOi19T6EPLp38.Y2.Nn6	ACTIVE	t	2026-08-05 10:07:00+00	2026-08-05 10:07:00+00
fdd7ae27-35e5-417e-b4cd-df85a5fe6cd1	Nipuni Karunaratne	nipuni.karunaratne@example.com	0760873486	$2a$12$fYl9hwmkur7QyiOAIler/ek3cqasKK6TE4wHWFLS00HsDy.3fWYCC	ACTIVE	t	2026-09-08 12:01:00+00	2026-09-08 12:01:00+00
f8668c6d-3bc0-48b3-8ead-474d9523a7b7	Tharushi Sivakumar	tharushi.sivakumar@example.com	0764929388	$2a$12$nQKMD2z7kTJk7JNDWompF.IKq.hFZfKJAWKHmJW9bjzMy97mMeNW2	ACTIVE	t	2026-09-25 03:34:00+00	2026-09-25 03:34:00+00
4b30e56a-3840-429c-b968-3e885023d0b2	Isuru Perera	isuru.perera@example.com	0707429766	$2a$12$KreZDojeQhi5rw97WihGnubHUeqdSr3X0FweEHMOV9cgxbNpGJBwy	ACTIVE	t	2026-09-12 10:10:00+00	2026-09-12 10:10:00+00
08ce854a-6db5-4855-a2b7-d68e5c8305cf	Ishara de Silva	ishara.de.silva@example.com	0768566616	$2a$12$EkWEu.G/rv0EWlyCNIosIueFE./hjMdSkYOrvLOAB6CV4sN9HsG/C	ACTIVE	t	2026-09-26 10:06:00+00	2026-09-26 10:06:00+00
af229567-0ca1-4397-bf5b-1d8f8b6ffe81	Pradeep Ratnayake	pradeep.ratnayake@example.com	0706415858	$2a$12$r6z3S3pI7gKb8x30PB3ltuijZ6tQmCick6Efjozv9uC5/PXeAVb7O	ACTIVE	t	2026-08-19 03:30:00+00	2026-08-19 03:30:00+00
4cecee6d-ec13-4ac2-803f-b9a696d6c972	Ravindu Rathnayake	ravindu.rathnayake@example.com	0740016725	$2a$12$/MhWd5Tnjf0qxP65/ANbVeLG8TJloO87JUw4RSuZnn6PoXP5T0.Ja	ACTIVE	t	2026-08-10 12:55:00+00	2026-08-10 12:55:00+00
ceacbbdb-fde0-4a2c-95ed-e1d7ee74c98d	Ayesha Mendis	ayesha.mendis@example.com	0747126364	$2a$12$1CalwAnS2s50L//RKhEn4elIWhPDZayg1SN.H5quiz1sEH.AKLw.i	ACTIVE	t	2026-09-29 07:34:00+00	2026-09-29 07:34:00+00
b7e01598-766c-4b81-a1d2-55c4f2fbb709	Fathima Dissanayake	fathima.dissanayake@example.com	0705590580	$2a$12$WcnQdXFO3sj7IGavDPwXCu9I.Vt.bx.GOIa2X6FQJ47Zojuo2SQh2	ACTIVE	t	2026-08-15 06:47:00+00	2026-08-15 06:47:00+00
b02190b7-45c7-4967-baf0-5581d728dd35	Hiruni Senanayake	hiruni.senanayake@example.com	0712744719	$2a$12$O9hBJqjVyzqedh3Nfs455.cjhHFhQo/sFXciE/OfTEG0KBtOGeWiW	ACTIVE	t	2026-07-30 07:15:00+00	2026-07-30 07:15:00+00
2da89375-52f5-47f7-859b-2ac948151468	Chamari Samarasinghe	chamari.samarasinghe@example.com	0784546580	$2a$12$ulu.xZV.HzjBvzGO180CF.gDBN9A65sDfnBMY/FTx4LgWirxdx9W2	ACTIVE	t	2026-07-28 04:11:00+00	2026-07-28 04:11:00+00
eeab7ea5-20bc-4d80-b757-447ed3fa06f0	Sachintha Dilshan	admin@swiftdrop.example.com	0703897827	$2a$12$Nh97tsfjmXtKdfLUzyLJV.Ow/Ls3HLQbsY4AW.6yLldonyiUwIi4i	ACTIVE	t	2026-06-17 09:21:00+00	2026-06-17 09:21:00+00
2aea47d3-a06d-4dba-b302-a2a6c6c25e37	Ops Admin	ops.admin@swiftdrop.example.com	0707362069	$2a$12$9ztBdCv8khNfXxyO6unxoORKaaEHFmEg3FeeSNcpXEQIXtcL4cmLm	ACTIVE	t	2026-06-17 10:09:00+00	2026-06-17 10:09:00+00
cb4cd22c-d4cd-4f2f-ad43-c2e73f1b9cb5	Hiruni Nawaz	dispatcher1@swiftdrop.example.com	0766643372	$2a$12$wETxdeKFMf92cLNXVJPDeeFuc61t6/jhkkKE/.T4wIC7T7kKain3.	ACTIVE	t	2026-08-21 04:02:00+00	2026-08-21 04:02:00+00
3900455f-0834-4d57-85c5-c3de92f5ecee	Hiruni Weerasinghe	dispatcher2@swiftdrop.example.com	0763536741	$2a$12$gSgMfOZTtZGgdJc/5OVeBe2xIPR2VQGuk17YVGx8TQDqQgvDRa97u	ACTIVE	t	2026-08-07 04:35:00+00	2026-08-07 04:35:00+00
7bf5a598-1908-4a34-9ac4-041d3ce99466	Sajith Wijesinghe	dispatcher3@swiftdrop.example.com	0765276291	$2a$12$S7E6lBCmfenHxEZHJQ2iGeVuP65eddVzTXUyN64JbqJ.Z4Kao0MBC	ACTIVE	t	2026-07-08 04:50:00+00	2026-07-08 04:50:00+00
9e3b8266-acf9-4126-91bb-369d59ad25a7	Malith Wijesinghe	courier01@swiftdrop.example.com	0705825274	$2a$12$chgzBjzIXi0YuVrqDPHxcu8.I8TUEHb9O99hdWOXqy11vRa9Jti1e	ACTIVE	t	2026-08-13 12:07:00+00	2026-08-13 12:07:00+00
69f3dbf3-6a51-4f41-8634-af3d608c2527	Supuni Jayasinghe	courier02@swiftdrop.example.com	0744232026	$2a$12$6hbLQeJZiDIBY6QBocfj0eO1Z3CcVpMXIGTe4Npw9GjXc1s9xzMf6	ACTIVE	t	2026-08-29 07:37:00+00	2026-08-29 07:37:00+00
2296db41-b3a5-4443-a906-df1b2a23b26c	Mohamed Weerasinghe	courier03@swiftdrop.example.com	0745692595	$2a$12$W1PiUoPAlBuQ3mLkXYxi8uPfZkf7qKNnHPrF6GdmMcYthnspLkzhu	ACTIVE	t	2026-08-02 10:06:00+00	2026-08-02 10:06:00+00
5b32b607-0ba6-4ff9-8b45-e7e15baf395b	Dimuth Nawaz	courier04@swiftdrop.example.com	0728377257	$2a$12$wFNEscmWmxVAx.IPBQlR.eohWZkdpYCSd0UHAGHSQfUqTB7W2HvTK	ACTIVE	t	2026-08-30 13:19:00+00	2026-08-30 13:19:00+00
fd5016f3-f6ab-462f-a5ed-0a2fb7ee2491	Ishara Rahman	courier05@swiftdrop.example.com	0778737854	$2a$12$9INcPOZLQBJ3GBAwQZ3oCuhGT07CfUFOmfkidEyJv4ZJX/oHDiidu	ACTIVE	t	2026-07-31 06:02:00+00	2026-07-31 06:02:00+00
2064432d-0cca-4c76-ac2b-47cd07270ec7	Sachini Fonseka	courier06@swiftdrop.example.com	0767793065	$2a$12$fPLxg7/kmH/Q7vX5Qk.mdOTtqD.vE884hPIz03QpzR3fVE92zqYMS	ACTIVE	t	2026-07-14 11:52:00+00	2026-07-14 11:52:00+00
c4750ad6-4949-4e9e-b59c-1c8af8f71294	Thisaru Liyanage	courier07@swiftdrop.example.com	0746284855	$2a$12$nwTp2F0z8/0lqu8RSmZ2pOdn2byRo3rpFB3p3t.wP212mXW76KWdK	ACTIVE	t	2026-07-24 10:30:00+00	2026-07-24 10:30:00+00
d28074b5-475a-4e20-b5d3-63080c9f1b7f	Nimal de Silva	courier08@swiftdrop.example.com	0742761137	$2a$12$Mp42MmCDoySfYw4ueZCGyuIv0WUGbXeEdtyKWdnJd9fPn../xZtsi	ACTIVE	t	2026-08-12 07:32:00+00	2026-08-12 07:32:00+00
854d0d01-8a43-4f8c-b742-ee0202e4e1b8	Ruwan Weerasinghe	courier09@swiftdrop.example.com	0712894471	$2a$12$9IKM2T0NyGwqyrbq7/7T4ufZWpdwc5OVVCIhvLpFjHWOxb0RrJ9gO	ACTIVE	t	2026-08-18 13:04:00+00	2026-08-18 13:04:00+00
3be525c0-b776-4ec1-b343-38831e36255d	Hiruni Liyanage	courier10@swiftdrop.example.com	0700451752	$2a$12$tri2F7UsLbKByWlfuN2Z2e88tBO68ExMg4GBCU6TiXv63QWPPf4h.	ACTIVE	t	2026-09-02 04:40:00+00	2026-09-02 04:40:00+00
c50a9935-b0a4-42a7-8b67-834c582c0b41	Chamari Peiris	chamari.peiris@merchant.example.com	0782401774	$2a$12$.7PfHINiFNUv1Y92goMd4uPbFTJ6tCy4jvkGasUqPTBkEJqpG8MqG	ACTIVE	t	2026-09-04 04:35:00+00	2026-09-04 04:35:00+00
71ac489c-40e8-4057-bd8d-22645457f877	Sajith Karunaratne	sajith.karunaratne@merchant.example.com	0774891635	$2a$12$ApuhMMlCm7mWEYz8clbnLOS7lk236TPqle38kPH0zNkdAUcih2iVe	ACTIVE	t	2026-09-19 11:13:00+00	2026-09-19 11:13:00+00
ac219f99-85fb-43a8-90b2-786b06a4d80b	Lahiru Senanayake	lahiru.senanayake@merchant.example.com	0770480984	$2a$12$w/T1oRUofX1P8uRbn7YyrescAbrLdBcCgwWrQXL2PSL8ww1QE4bWC	ACTIVE	t	2026-09-14 10:35:00+00	2026-09-14 10:35:00+00
c8b97194-64f2-4ced-bc12-b5c4b7d4d9ee	Lahiru Wijesinghe	lahiru.wijesinghe@merchant.example.com	0782262943	$2a$12$OLJ43i10Tt3dALFln2ylqe7nFEGMKA7OveYuwS33EhV9OPtsF212W	ACTIVE	t	2026-09-02 04:57:00+00	2026-09-02 04:57:00+00
9b13b98c-873f-4749-958b-2f913cc7e712	Chathura Perera	chathura.perera@merchant.example.com	0770943404	$2a$12$TzbYeh6lJYG.Jxo/69LTOeTzJZg4jQB6o9FHwid2aIzOHJnnIxAge	ACTIVE	t	2026-09-09 10:31:00+00	2026-09-09 10:31:00+00
b2691eac-bac7-474e-831a-394cf0a74ad4	Demo Customer	customer@example.com	0702392760	$2a$12$etAKRSodCio3q.l66qShzewZxNJYwsK9vX6jjIVUBoxaYTAEHSHMu	ACTIVE	t	2026-08-10 05:40:00+00	2026-08-10 05:40:00+00
\.


--
-- Name: roles_id_seq; Type: SEQUENCE SET; Schema: public; Owner: auth
--

SELECT pg_catalog.setval('public.roles_id_seq', 5, true);


--
-- Name: flyway_schema_history flyway_schema_history_pk; Type: CONSTRAINT; Schema: public; Owner: auth
--

ALTER TABLE ONLY public.flyway_schema_history
    ADD CONSTRAINT flyway_schema_history_pk PRIMARY KEY (installed_rank);


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

\unrestrict WcQfvp6YkBzsZg78fW2tx53DWdUJtt9gjInzGMgJoFFpoNC5k1GPdeAdY9toJoK

