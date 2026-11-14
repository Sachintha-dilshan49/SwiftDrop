--
-- PostgreSQL database dump
--

\restrict M8A0GphJEGnqfR0EQGfUlcBbUHcD6DAkojNcjdHOFbo3sd3WopP4nmGy7wC7X7X

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

ALTER TABLE IF EXISTS ONLY public.merchant_webhook_endpoints DROP CONSTRAINT IF EXISTS merchant_webhook_endpoints_merchant_id_fkey;
ALTER TABLE IF EXISTS ONLY public.merchant_orders DROP CONSTRAINT IF EXISTS merchant_orders_merchant_id_fkey;
ALTER TABLE IF EXISTS ONLY public.merchant_api_keys DROP CONSTRAINT IF EXISTS merchant_api_keys_merchant_id_fkey;
DROP INDEX IF EXISTS public.idx_webhooks_merchant;
DROP INDEX IF EXISTS public.idx_merchants_user_id;
DROP INDEX IF EXISTS public.idx_merchants_status;
DROP INDEX IF EXISTS public.idx_merchants_reg_no;
DROP INDEX IF EXISTS public.idx_merchants_email;
DROP INDEX IF EXISTS public.idx_merchant_orders_tracking;
DROP INDEX IF EXISTS public.idx_merchant_orders_parcel;
DROP INDEX IF EXISTS public.idx_merchant_orders_merchant;
DROP INDEX IF EXISTS public.idx_merchant_orders_idemp;
DROP INDEX IF EXISTS public.idx_api_keys_merchant;
DROP INDEX IF EXISTS public.idx_api_keys_hash;
DROP INDEX IF EXISTS public.flyway_schema_history_s_idx;
ALTER TABLE IF EXISTS ONLY public.merchants DROP CONSTRAINT IF EXISTS merchants_user_id_key;
ALTER TABLE IF EXISTS ONLY public.merchants DROP CONSTRAINT IF EXISTS merchants_pkey;
ALTER TABLE IF EXISTS ONLY public.merchants DROP CONSTRAINT IF EXISTS merchants_email_key;
ALTER TABLE IF EXISTS ONLY public.merchants DROP CONSTRAINT IF EXISTS merchants_business_reg_no_key;
ALTER TABLE IF EXISTS ONLY public.merchant_webhook_endpoints DROP CONSTRAINT IF EXISTS merchant_webhook_endpoints_pkey;
ALTER TABLE IF EXISTS ONLY public.merchant_webhook_endpoints DROP CONSTRAINT IF EXISTS merchant_webhook_endpoints_merchant_id_key;
ALTER TABLE IF EXISTS ONLY public.merchant_orders DROP CONSTRAINT IF EXISTS merchant_orders_pkey;
ALTER TABLE IF EXISTS ONLY public.merchant_api_keys DROP CONSTRAINT IF EXISTS merchant_api_keys_pkey;
ALTER TABLE IF EXISTS ONLY public.merchant_api_keys DROP CONSTRAINT IF EXISTS merchant_api_keys_key_hash_key;
ALTER TABLE IF EXISTS ONLY public.flyway_schema_history DROP CONSTRAINT IF EXISTS flyway_schema_history_pk;
DROP TABLE IF EXISTS public.merchants;
DROP TABLE IF EXISTS public.merchant_webhook_endpoints;
DROP TABLE IF EXISTS public.merchant_orders;
DROP TABLE IF EXISTS public.merchant_api_keys;
DROP TABLE IF EXISTS public.flyway_schema_history;
SET default_tablespace = '';

SET default_table_access_method = heap;

--
-- Name: flyway_schema_history; Type: TABLE; Schema: public; Owner: merchant
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


ALTER TABLE public.flyway_schema_history OWNER TO merchant;

--
-- Name: merchant_api_keys; Type: TABLE; Schema: public; Owner: merchant
--

CREATE TABLE public.merchant_api_keys (
    id uuid NOT NULL,
    merchant_id uuid NOT NULL,
    name character varying(100) NOT NULL,
    key_prefix character varying(20) NOT NULL,
    key_hash character varying(64) NOT NULL,
    revoked boolean DEFAULT false NOT NULL,
    revoked_at timestamp with time zone,
    expires_at timestamp with time zone,
    created_at timestamp with time zone DEFAULT now() NOT NULL,
    updated_at timestamp with time zone
);


ALTER TABLE public.merchant_api_keys OWNER TO merchant;

--
-- Name: merchant_orders; Type: TABLE; Schema: public; Owner: merchant
--

CREATE TABLE public.merchant_orders (
    id uuid NOT NULL,
    merchant_id uuid NOT NULL,
    parcel_id uuid NOT NULL,
    tracking_no character varying(50) NOT NULL,
    recipient_name character varying(150) NOT NULL,
    recipient_phone character varying(30) NOT NULL,
    dropoff_address character varying(500) NOT NULL,
    status character varying(30) DEFAULT 'CREATED'::character varying NOT NULL,
    fee_amount numeric(10,2),
    fee_currency character(3) DEFAULT 'LKR'::bpchar NOT NULL,
    idempotency_key character varying(100),
    created_at timestamp with time zone DEFAULT now() NOT NULL,
    updated_at timestamp with time zone
);


ALTER TABLE public.merchant_orders OWNER TO merchant;

--
-- Name: merchant_webhook_endpoints; Type: TABLE; Schema: public; Owner: merchant
--

CREATE TABLE public.merchant_webhook_endpoints (
    id uuid NOT NULL,
    merchant_id uuid NOT NULL,
    webhook_url character varying(1000) NOT NULL,
    secret character varying(255) NOT NULL,
    is_active boolean DEFAULT true NOT NULL,
    created_at timestamp with time zone DEFAULT now() NOT NULL,
    updated_at timestamp with time zone
);


ALTER TABLE public.merchant_webhook_endpoints OWNER TO merchant;

--
-- Name: merchants; Type: TABLE; Schema: public; Owner: merchant
--

CREATE TABLE public.merchants (
    id uuid NOT NULL,
    user_id uuid,
    business_name character varying(200) NOT NULL,
    business_reg_no character varying(100) NOT NULL,
    contact_name character varying(150) NOT NULL,
    email character varying(255) NOT NULL,
    phone character varying(30) NOT NULL,
    address character varying(500) NOT NULL,
    status character varying(20) DEFAULT 'PENDING'::character varying NOT NULL,
    rejection_reason character varying(500),
    created_at timestamp with time zone DEFAULT now() NOT NULL,
    updated_at timestamp with time zone,
    CONSTRAINT merchants_status_check CHECK (((status)::text = ANY ((ARRAY['PENDING'::character varying, 'APPROVED'::character varying, 'REJECTED'::character varying])::text[])))
);


ALTER TABLE public.merchants OWNER TO merchant;

--
-- Data for Name: flyway_schema_history; Type: TABLE DATA; Schema: public; Owner: merchant
--

COPY public.flyway_schema_history (installed_rank, version, description, type, script, checksum, installed_by, installed_on, execution_time, success) FROM stdin;
1	1	create merchants	SQL	V1__create_merchants.sql	16135107	merchant	2026-11-14 10:17:00.146395	23	t
2	2	create merchant api keys	SQL	V2__create_merchant_api_keys.sql	1894479372	merchant	2026-11-14 10:17:00.187643	9	t
3	3	create merchant webhook endpoints	SQL	V3__create_merchant_webhook_endpoints.sql	-1702115928	merchant	2026-11-14 10:17:00.21141	8	t
4	4	create merchant orders	SQL	V4__create_merchant_orders.sql	28251337	merchant	2026-11-14 10:17:00.22933	10	t
\.


--
-- Data for Name: merchant_api_keys; Type: TABLE DATA; Schema: public; Owner: merchant
--

COPY public.merchant_api_keys (id, merchant_id, name, key_prefix, key_hash, revoked, revoked_at, expires_at, created_at, updated_at) FROM stdin;
18815204-fa05-4116-ab0a-ce161e154157	31a28baa-be0f-4565-8862-ff126e1cd4a9	Online store	sd_live_6A4OZBQw	3d65c8c734995f2947ca5bc1ffabc3d1ad2f7aaa4b1f8410c33c872c660e0dfe	f	\N	\N	2026-09-21 02:06:00+00	\N
cb12a9c2-b816-4bf5-bb22-581e9668fdad	f4d76c8c-22f9-4947-a7a5-a4469d18e781	Online store	sd_live_jI_eAh0w	fcecdc4d2c11c1d0e18af4f1540d232fcd8c9fbe80daec94a2fc398e958b1fa1	f	\N	\N	2026-09-03 04:06:00+00	\N
b80ad451-dac0-49e1-9e00-5b55363568cc	8eef88b2-09ae-4482-a9af-c755adcc9c2b	Online store	sd_live_M4ES-tTu	61940f957bf78daec28627a18fed8e985666a2485f50062fda425442e0f2e02e	f	\N	\N	2026-09-18 20:49:00+00	\N
eddf5a2a-4e68-4b5a-9b8f-3d945f814d3d	8eef88b2-09ae-4482-a9af-c755adcc9c2b	Warehouse system	sd_live_ecH2-WMh	b92238fe8d9c9cfbd29674a36e67f0a00f3d7bc76a8bda678eb0de5bd51c64e6	f	\N	\N	2026-09-18 20:49:00+00	\N
61f3ae0c-cab0-4cf4-8138-94b5be1ea3c4	9a6cef18-d765-487a-bf61-4d62a9a02714	Online store	sd_live_OlEqoW8C	58f178f9c3a9441d158a4e055bc9fe9a5bad3c3f3dca4a7193c0fcb0727cdc59	f	\N	\N	2026-09-18 21:19:00+00	\N
3d43310e-fb03-4145-a557-0145b1bf4e25	c15f4f15-fcc6-4bd4-8d2f-a310b3ef706a	Online store	sd_live_1xGRpP6U	37b659560538e69652e54a2777a5db542be8f41be0890c07c718d8d5406bde06	f	\N	\N	2026-09-22 02:26:00+00	\N
f5c804ef-b905-43c5-a7de-11bdb5da5f84	3a839b3d-f0a3-46e7-b6ea-eaac1647dc36	Online store	sd_live_DpMG1WeF	b591d630fcba54c1d5d37bac357d25a21f05a0d750c9a1bb08ed2dbca50e9d4f	f	\N	\N	2026-09-16 02:08:00+00	\N
\.


--
-- Data for Name: merchant_orders; Type: TABLE DATA; Schema: public; Owner: merchant
--

COPY public.merchant_orders (id, merchant_id, parcel_id, tracking_no, recipient_name, recipient_phone, dropoff_address, status, fee_amount, fee_currency, idempotency_key, created_at, updated_at) FROM stdin;
143a007c-cf2a-4d3f-9504-5e460b5168a4	31a28baa-be0f-4565-8862-ff126e1cd4a9	97dbd3bd-cfd5-4b69-8bf7-4a3a02e536d0	SD-E8FD-BH6Q	Shehan Rahman	0766522503	196 Chatham Street, Colombo 01	DELIVERED	690.00	LKR	order-94f1e339-ff21-8b58-2e28-0a80a887890a	2026-10-16 07:26:00.007624+00	\N
dca7434a-89d7-44b8-8fe2-247cf9cdf4c2	f4d76c8c-22f9-4947-a7a5-a4469d18e781	847b8a16-3a0a-48ae-b93d-ebbb7a7a09bf	SD-N8ZH-TKMW	Anjali Rajendran	0786929591	160 Stanley Thilakaratne Mawatha, Nugegoda	DELIVERED	520.00	LKR	order-fcf3c3c0-7c9c-0590-bedb-8e76dc3a2e51	2026-10-17 04:36:00.002988+00	\N
873cc915-87d3-41d5-bad0-03109ac7bf9e	8eef88b2-09ae-4482-a9af-c755adcc9c2b	ae4c9d47-686d-4390-8c73-9960b15539ae	SD-3M6X-Y92H	Supuni Liyanage	0747987879	20 Main Street, Negombo	DELIVERED	950.00	LKR	order-f19ce2c5-225f-63df-b216-8f096b5fd6ab	2026-10-17 07:08:00.00284+00	\N
37fa3d3b-1909-42d9-aed3-2d6b77c33f59	9a6cef18-d765-487a-bf61-4d62a9a02714	b2bd86a0-b000-491e-b893-cdd88c1ba858	SD-PZMS-HY9G	Anjali Wijesinghe	0715873559	157 Thimbirigasyaya Road, Colombo 05	DELIVERED	850.00	LKR	order-8e4cdd9c-64fe-2319-6817-6ac9fdf8a74e	2026-10-17 12:18:00.002748+00	\N
54d5d8dc-c7f3-4b78-8288-20a7df2f04d5	c15f4f15-fcc6-4bd4-8d2f-a310b3ef706a	3bddfe60-25f9-47ab-8925-3e2ebddff83d	SD-26ZV-LE8J	Supuni Bandara	0746921603	327 Main Street, Colombo 11	DELIVERED	810.00	LKR	order-4a69438b-1169-489e-e799-f1cf6e437c23	2026-10-17 13:12:00.002793+00	\N
925bfcb7-36e0-4e26-ac22-1e62059742a9	3a839b3d-f0a3-46e7-b6ea-eaac1647dc36	e18c8423-831b-44a9-aa94-2870198d1d70	SD-K8N4-PT6Q	Mohamed Wijesinghe	0709796350	324 Matara Road, Galle	DELIVERED	910.00	LKR	order-af301316-4745-9c87-c094-f3168d60986a	2026-10-18 12:03:00.00194+00	\N
9daa34ff-185e-42e1-8c24-7f4dda8580c2	31a28baa-be0f-4565-8862-ff126e1cd4a9	cf41bce5-2258-4004-b52c-81b77c572b8e	SD-4Z52-HPHE	Lakshmi Fernando	0762663353	344 Main Street, Negombo	FAILED	810.00	LKR	order-77fea59e-8e11-b064-d8cc-7a822bfa4460	2026-10-19 11:05:00.002633+00	\N
7f143dc3-9e63-4cb6-885f-782b0432bdff	f4d76c8c-22f9-4947-a7a5-a4469d18e781	028d6342-4b77-4a66-929c-d86b040c5e80	SD-AG88-Y7AW	Ravindu Peiris	0768539516	162 Galle Road, Dehiwala	DELIVERED	990.00	LKR	order-fe16f84b-8d62-0532-97a7-20f752051208	2026-10-20 04:30:00.002555+00	\N
02d2ba0a-c512-4c18-801c-252f47dac2b9	8eef88b2-09ae-4482-a9af-c755adcc9c2b	aa4a02c7-177f-413a-807a-8d5dc6822638	SD-V5RH-BAAK	Sajith Sivakumar	0784147266	221 Dalada Veediya, Kandy	DELIVERED	810.00	LKR	order-3713e218-b8cd-ed73-d221-b044ee428a0b	2026-10-20 06:03:00.003096+00	\N
b112bc82-a124-4965-8a2f-178831ad0239	9a6cef18-d765-487a-bf61-4d62a9a02714	dc99d383-3e36-4e52-b1f6-53aa613492dd	SD-27WC-YA6G	Shehan Herath	0725910537	340 Peradeniya Road, Kandy	DELIVERED	810.00	LKR	order-d7a02113-0bb3-43cf-7800-2b99655ff380	2026-10-20 13:02:00.003379+00	\N
f6e67884-8eec-43fa-ada3-531db1943f3f	c15f4f15-fcc6-4bd4-8d2f-a310b3ef706a	6f4f17fc-432f-4485-b1bf-f580c24d7a5f	SD-2DTV-QP7Z	Pradeep Rathnayake	0718485830	73 Prince of Wales Avenue, Colombo 14	DELIVERED	510.00	LKR	order-1898462e-8e04-e358-4d76-76f89c68452a	2026-10-21 08:46:00.002872+00	\N
f0a0dae5-08e8-4eef-a36a-56f2187bb5eb	3a839b3d-f0a3-46e7-b6ea-eaac1647dc36	04549e3f-d5dc-41ba-abd9-902269515fad	SD-TGDS-ZE7A	Anjali Dissanayake	0719348440	287 Church Street, Galle Fort	DELIVERED	870.00	LKR	order-095c5fe9-3f6c-555c-511a-f8407d401674	2026-10-21 09:06:00.00273+00	\N
fbe5c4d0-affc-4d4e-bb35-3cc5ab073405	31a28baa-be0f-4565-8862-ff126e1cd4a9	c57aac0a-1e88-4b7c-8d9d-ef12725a9333	SD-LG5D-3KDW	Anjali Silva	0780820881	261 Prince of Wales Avenue, Colombo 14	DELIVERED	650.00	LKR	order-0936ff1b-71d1-9424-593d-1a2b806a9bbc	2026-10-21 09:31:00.002541+00	\N
4aa1dec1-04a5-4b60-96c7-9c1da1bcffda	f4d76c8c-22f9-4947-a7a5-a4469d18e781	99b6d8a8-35e9-43eb-a138-5877433fcc2f	SD-ZRUV-QCCR	Sunil Amarasekara	0782746654	81 Maradana Road, Colombo 10	DELIVERED	870.00	LKR	order-ba42553c-44cb-09a8-9f28-13be1edc1a84	2026-10-21 11:00:00.00267+00	\N
7df2a444-40b5-46d4-969b-a6323914c543	8eef88b2-09ae-4482-a9af-c755adcc9c2b	abb4a93d-b380-4f6b-bbc3-99501e28899f	SD-3YUN-X8T7	Priya Wijesinghe	0760468591	3 Marine Drive, Colombo 06	DELIVERED	840.00	LKR	order-96f4dfbb-0787-3eaa-a2c6-d8bea28b2942	2026-10-22 08:30:00.003049+00	\N
8bd7d520-5eab-4bdc-ab37-64c9764806c1	9a6cef18-d765-487a-bf61-4d62a9a02714	ba826b8e-1037-4a8f-891b-601836f6b623	SD-PM9T-6SHR	Hiruni Mendis	0788906201	331 Main Street, Negombo	DELIVERED	810.00	LKR	order-f9f38a57-2f23-f0da-00b6-3f89c3721601	2026-10-22 13:27:00.002154+00	\N
3bb0ddef-c48f-47b9-b00e-cf28e699f18f	c15f4f15-fcc6-4bd4-8d2f-a310b3ef706a	e5ef7136-68c8-4d46-9f28-e9e2d92c1efd	SD-6GP8-XUA8	Tharindu Liyanage	0728510636	187 York Street, Colombo 01	DELIVERED	1490.00	LKR	order-27f1db64-daf7-fc14-e046-8eebbeaa6af1	2026-10-23 02:34:00.002684+00	\N
1595db83-fc56-43be-a59f-9b1325e7d156	3a839b3d-f0a3-46e7-b6ea-eaac1647dc36	4cd97983-2378-4921-84ce-0a540ad93fe4	SD-LHR2-6CNT	Kasun Liyanage	0762726452	30 Church Street, Galle Fort	DELIVERED	830.00	LKR	order-a69cd690-9ff9-eb46-7cd5-53a89722bba6	2026-10-23 03:10:00.002335+00	\N
7346cfe7-fbb6-4a50-8c1c-be8dd407e32c	31a28baa-be0f-4565-8862-ff126e1cd4a9	97e61206-0c55-4d6f-85ff-1d40aa8e5a5e	SD-JWN9-SH5Z	Malith Cooray	0724785757	265 Galle Road, Colombo 04	DELIVERED	2290.00	LKR	order-4af74d82-206c-5e31-012c-47ae321f1dcc	2026-10-23 06:30:00.00296+00	\N
03ae7035-8756-45ab-94f4-0279995afb99	f4d76c8c-22f9-4947-a7a5-a4469d18e781	bdb43a56-a17a-433f-9d71-f97208be93bf	SD-TD68-J2GG	Chathura Rajapaksa	0768870530	223 Main Street, Negombo	DELIVERED	870.00	LKR	order-dad65cae-1dad-4ec8-c1a8-0d8e6e040837	2026-10-23 06:47:00.003649+00	\N
b39a1e18-df7b-4549-8b76-879cd1255c8e	8eef88b2-09ae-4482-a9af-c755adcc9c2b	679e0f91-c539-42dd-b249-518cc2cc7ca2	SD-9YVA-C9FV	Nipuni Samarasinghe	0725172179	201 Main Street, Negombo	DELIVERED	970.00	LKR	order-346ae13c-b968-a129-3db5-c86a43263982	2026-10-23 12:42:00.002782+00	\N
aa8933f8-d8b7-4d18-9afb-3585a647a87a	9a6cef18-d765-487a-bf61-4d62a9a02714	defae5af-d535-4407-b5d9-5b96e58fccce	SD-GDKD-6WCX	Anjali Liyanage	0769500812	186 Hotel Road, Mount Lavinia	DELIVERED	530.00	LKR	order-917e9d15-89be-17c1-0acd-7a8a9d194328	2026-10-24 03:07:00.003782+00	\N
fb198b2c-f757-466a-8d58-655de0984401	c15f4f15-fcc6-4bd4-8d2f-a310b3ef706a	219a886a-4e85-4e61-a9a0-319131ff32fc	SD-RPVT-2LUH	Sahan Ratnayake	0787582224	214 Church Street, Galle Fort	DELIVERED	1010.00	LKR	order-c65eb134-4726-9cd5-e539-2f5bcbbb459d	2026-10-24 04:30:00.003795+00	\N
c971ae42-1e4d-479f-958f-53514192c154	3a839b3d-f0a3-46e7-b6ea-eaac1647dc36	31994151-4619-491a-9693-4ebccf8e38b4	SD-UTFH-HMAA	Nipuni Rajendran	0728436360	338 Peradeniya Road, Kandy	DELIVERED	1420.00	LKR	order-b4bfd820-9351-fd72-5047-89f44b8b2655	2026-10-24 07:09:00.003206+00	\N
c7dd712c-7f6e-406b-a107-d23dc464f641	31a28baa-be0f-4565-8862-ff126e1cd4a9	2e90cb60-5ad4-4166-8ea0-4b4150f48255	SD-DYWL-J6EL	Dulani Sivakumar	0777460837	142 Pita Kotte Road, Kotte	DELIVERED	1110.00	LKR	order-c9d58b53-5226-8296-cc54-57bb1e8187e9	2026-10-24 08:12:00.003285+00	\N
68ca5c8c-3247-4397-b2e2-e9e8f1b324b5	f4d76c8c-22f9-4947-a7a5-a4469d18e781	706c6b4c-eafa-4106-80c2-ec0f2f7de13d	SD-UW7V-ZMPG	Kavindi Mendis	0719011504	289 Stanley Thilakaratne Mawatha, Nugegoda	DELIVERED	1860.00	LKR	order-bed31bc2-665d-6db9-d40d-5305785fac61	2026-10-24 09:07:00.00296+00	\N
60e4e7ac-50bd-444d-a827-c478e0225010	8eef88b2-09ae-4482-a9af-c755adcc9c2b	b0e9bc2b-79e4-4451-8d19-f3da58a41bcf	SD-3QYE-8E3R	Sunil Mendis	0705098179	328 Prince of Wales Avenue, Colombo 14	DELIVERED	570.00	LKR	order-b052d170-e492-ca64-05fa-3dc916bbceb6	2026-10-25 02:57:00.002419+00	\N
41723e6c-a851-4ee2-b5d9-ff4ecb30b4b1	9a6cef18-d765-487a-bf61-4d62a9a02714	b81c9da6-95f6-42df-a7d7-e5df91bec9da	SD-CHTG-TCJQ	Ishara Fernando	0723533502	119 Marine Drive, Colombo 06	DELIVERED	660.00	LKR	order-bc6ada50-c0e1-156b-af99-d01e9c411244	2026-10-25 05:47:00.003213+00	\N
25e3528a-80f1-4a26-afc0-9215fad5d6fb	c15f4f15-fcc6-4bd4-8d2f-a310b3ef706a	a29887b4-09f0-4408-acb3-1fec493b1cde	SD-H5LA-AMF2	Dinuka Jayawardena	0708909025	43 Old Kesbewa Road, Nugegoda	DELIVERED	1090.00	LKR	order-1c2a4a31-5e1f-2491-012f-54d1d8d4f340	2026-10-25 07:06:00.003326+00	\N
c652e9b6-48c6-42a8-96de-bc3e6ddd8eed	3a839b3d-f0a3-46e7-b6ea-eaac1647dc36	163ef59e-a983-46f0-a6b9-4dd445217687	SD-3HSQ-Z2VN	Anjali Nawaz	0784284161	157 De Saram Road, Mount Lavinia	DELIVERED	790.00	LKR	order-66d0c303-2f65-9ead-2b49-a2daee8fc4d9	2026-10-25 09:47:00.002628+00	\N
1ef85723-4f5f-4b70-adc9-3de5bcf53cf1	31a28baa-be0f-4565-8862-ff126e1cd4a9	6cc9c460-9d18-448f-b090-b925aa4f4616	SD-RPJ5-8F4A	Dilani Dissanayake	0742758272	339 Flower Road, Colombo 07	DELIVERED	1360.00	LKR	order-1fb3059d-e53e-7448-97bc-2261023eaf42	2026-10-25 12:39:00.002584+00	\N
e9d87c6b-0efe-43bc-a907-b78c03f39d06	f4d76c8c-22f9-4947-a7a5-a4469d18e781	d5b97f72-f54c-4c23-b057-e4c0823f97bb	SD-EZ4D-E8WH	Priya Dissanayake	0728124534	160 Ward Place, Colombo 07	DELIVERED	1290.00	LKR	order-9dba3264-ddb4-a562-fa85-6ef985e6a6c6	2026-10-26 08:08:00.003244+00	\N
481ae2d6-2e16-4d93-9fbe-d36eeb8f8ad8	8eef88b2-09ae-4482-a9af-c755adcc9c2b	252adb9d-77a1-40ad-b861-0e9842fc570e	SD-2FB4-9X9C	Supuni Sivakumar	0748242024	161 Peradeniya Road, Kandy	DELIVERED	730.00	LKR	order-9898bcc9-ca34-db97-5f2a-7d72d04e8be8	2026-10-26 10:09:00.003282+00	\N
78433faa-0db2-4b94-a5bf-f453d3097427	9a6cef18-d765-487a-bf61-4d62a9a02714	be176bb6-bb39-483b-a768-e7decc318e03	SD-NTMV-PK6P	Dilani Rajendran	0743784340	36 Prince of Wales Avenue, Colombo 14	DELIVERED	440.00	LKR	order-5ad58393-1885-c677-345e-775f07794e7b	2026-10-27 11:31:00.003209+00	\N
eb537f5d-1afa-447e-8cc0-c7de3d2ba595	c15f4f15-fcc6-4bd4-8d2f-a310b3ef706a	c9233b02-c3cd-4898-92af-c5ac1ff204a6	SD-4Q5F-DJZT	Mohamed Herath	0770828152	240 Parliament Road, Rajagiriya	DELIVERED	660.00	LKR	order-b7bd48a2-fccd-268e-8311-ddb81d2c4460	2026-10-27 11:53:00.002403+00	\N
09611325-68eb-43b8-8aed-8597a0e62bb9	3a839b3d-f0a3-46e7-b6ea-eaac1647dc36	8eeb8d57-fe1b-4b10-9126-98f3e39d9878	SD-U5WY-QG6Y	Hashini Samarasinghe	0703373622	266 Ramakrishna Road, Colombo 06	DELIVERED	690.00	LKR	order-81a12067-dd67-afd2-b224-79c70ecac43c	2026-10-28 06:58:00.003606+00	\N
ef269103-8cb8-49f8-a327-a8ebc696ad3a	31a28baa-be0f-4565-8862-ff126e1cd4a9	1095aaed-0a2a-4647-8d1a-db30edefd318	SD-XXUE-XYST	Ruwan Jayasinghe	0742230345	258 Duplication Road, Colombo 03	DELIVERED	790.00	LKR	order-2cc2d58c-9356-6ef8-f097-832c3b144e8f	2026-10-28 07:15:00.002824+00	\N
03a33308-234f-4c0d-8cb4-09d0d6c0de69	f4d76c8c-22f9-4947-a7a5-a4469d18e781	25f30799-0863-4ffb-bd89-7cf8fd72f0fd	SD-W99M-ZC83	Nipuni Rajapaksa	0710936099	110 Hotel Road, Mount Lavinia	DELIVERED	1290.00	LKR	order-b2b86dba-f579-d3fb-eee8-de83e9b0a277	2026-10-28 12:22:00.00213+00	\N
82b7f398-2d5b-4407-8664-e7f58c888239	8eef88b2-09ae-4482-a9af-c755adcc9c2b	72e7dd74-df2a-4a93-8447-26a0a73fa925	SD-J6J4-S8S8	Lakshmi Senanayake	0742387913	30 High Level Road, Maharagama	FAILED	1250.00	LKR	order-f05e00c9-8158-6a03-3a84-671b4517268e	2026-10-29 06:49:00.003017+00	\N
78de9c32-dc13-4447-bb83-32e8716878a5	9a6cef18-d765-487a-bf61-4d62a9a02714	e022dd0f-dc40-47e4-9e9d-a25357488108	SD-UKAU-J26C	Sajith Jayasinghe	0709843157	278 De Saram Road, Mount Lavinia	DELIVERED	1120.00	LKR	order-406cd677-17e5-fbe4-92cd-d30092f845e9	2026-10-29 11:16:00.003183+00	\N
77778f23-ed34-4d0c-a1cf-c5758a984dda	c15f4f15-fcc6-4bd4-8d2f-a310b3ef706a	1ee4bcf7-bb7b-4c29-952d-55fbab53bf5e	SD-H97K-DM5W	Thisaru Silva	0725061099	25 Dalada Veediya, Kandy	FAILED	650.00	LKR	order-f8aa0905-88b0-1293-d4c8-aecf19f4cb38	2026-10-30 06:14:00.002071+00	\N
1bfcf3a5-482d-4c32-80d1-ea351f2d8b83	3a839b3d-f0a3-46e7-b6ea-eaac1647dc36	1b752b1a-26a1-4cce-9db9-2194376fae67	SD-NNVU-A9ES	Hiruni Ratnayake	0771484250	88 Parliament Road, Rajagiriya	DELIVERED	1140.00	LKR	order-35cb671d-0046-05e8-6394-8cd2b31ea727	2026-10-30 09:02:00.00273+00	\N
3064e98b-9ede-4d81-830f-956b5f654628	31a28baa-be0f-4565-8862-ff126e1cd4a9	6fbc6a71-94a9-4f30-b4bd-2004e6887044	SD-RRDU-GLAS	Malith Silva	0778512003	128 Lewis Place, Negombo	DELIVERED	1170.00	LKR	order-8b90ada9-9014-20ad-db14-5608e31430f8	2026-10-30 10:06:00.003384+00	\N
8675da0c-b000-4bab-93b5-3202b0197729	f4d76c8c-22f9-4947-a7a5-a4469d18e781	b1674497-9c17-495a-a3ac-2b8b9a4288e3	SD-Q9YZ-YFG4	Pradeep Silva	0764256402	13 Hill Street, Dehiwala	DELIVERED	780.00	LKR	order-d9fb0ec5-01a1-e20d-aaea-20e6044ff6a9	2026-10-31 05:37:00.002797+00	\N
733c154c-5ce8-487c-8bb1-976e14b362c5	8eef88b2-09ae-4482-a9af-c755adcc9c2b	cb320019-0e44-41c1-8c9b-bf9157f8ba4b	SD-PTJL-K4PP	Senuri Rajapaksa	0742654922	42 Galle Road, Colombo 04	DELIVERED	770.00	LKR	order-337cface-c901-313d-0daf-a0466914104b	2026-11-01 03:44:00.003174+00	\N
526ae256-9cb0-4906-a749-427aad66ade2	9a6cef18-d765-487a-bf61-4d62a9a02714	d3fbcca3-5c8e-41d4-9ad5-cf2f5aa0d985	SD-JLXC-MJ76	Yasiru Senanayake	0707266328	47 De Saram Road, Mount Lavinia	DELIVERED	350.00	LKR	order-dbd122ca-23f2-8e6f-dedd-8c6cd174cafe	2026-11-01 08:50:00.002786+00	\N
a29617f3-512b-49e8-b8a9-792b5af076bc	c15f4f15-fcc6-4bd4-8d2f-a310b3ef706a	4748b8ce-e100-475f-ad19-1765212a2510	SD-P87W-M5PJ	Kamal Dissanayake	0784349135	288 Lewis Place, Negombo	DELIVERED	770.00	LKR	order-a6ad77a6-8b37-894d-15af-a68f5ceda489	2026-11-01 10:24:00.003885+00	\N
a1519e1f-12cf-4cc2-8bf1-248c30196195	3a839b3d-f0a3-46e7-b6ea-eaac1647dc36	040ddcbc-5280-47f1-825c-a59275874a76	SD-L87J-BQ3M	Kamal Liyanage	0765935827	62 Main Street, Colombo 11	DELIVERED	810.00	LKR	order-7368c959-3129-f3ef-7bd4-80c4dafbc550	2026-11-01 10:28:00.001933+00	\N
c35b729b-b1d6-4953-955d-e469ef55acab	31a28baa-be0f-4565-8862-ff126e1cd4a9	95d934f3-63d5-4cb7-8da3-df658f3a98b0	SD-XWKP-3T5W	Tharindu Amarasekara	0775851101	22 Colombo Road, Negombo	DELIVERED	990.00	LKR	order-e32b4186-9ef6-4c83-f269-9b41518165f4	2026-11-01 12:33:00.002175+00	\N
b2b30575-e088-46dc-b80d-e3409d8888e7	f4d76c8c-22f9-4947-a7a5-a4469d18e781	8f4a5b4c-0f58-4543-96d8-f003c414e220	SD-258B-L37Z	Dinuka Perera	0725341914	196 Hill Street, Dehiwala	DELIVERED	520.00	LKR	order-abcdd9f4-bcd5-f125-62b6-1c59583c8ae7	2026-11-01 12:41:00.002082+00	\N
628e60ce-99f2-405c-a10a-94cd8db86667	8eef88b2-09ae-4482-a9af-c755adcc9c2b	401480cf-2d8e-42b7-ae22-20ab7a099e28	SD-GQCF-HMSD	Mohamed Jayawardena	0727639426	214 Pita Kotte Road, Kotte	DELIVERED	930.00	LKR	order-c0973cdc-0f96-124b-65cb-c6e459e54feb	2026-11-02 12:27:00.002583+00	\N
8ed73956-438c-4b40-9ffb-bfd532c71912	9a6cef18-d765-487a-bf61-4d62a9a02714	c08e4280-3066-4190-b59f-95b7e6e4a330	SD-XNQJ-GSYS	Kasun Amarasekara	0782988159	63 Ramakrishna Road, Colombo 06	DELIVERED	1290.00	LKR	order-4d124abe-8baa-cc16-67f4-f121cbb7f4fe	2026-11-03 03:17:00.002588+00	\N
99e648f0-629a-4c61-beb0-a461a5b03659	c15f4f15-fcc6-4bd4-8d2f-a310b3ef706a	f5eaa1a6-a4e4-4410-9c1f-813b1d750b32	SD-2BD4-EESS	Kavindi Rajendran	0726148765	71 High Level Road, Maharagama	DELIVERED	560.00	LKR	order-58ea1743-c08e-ae07-b891-a392f005368c	2026-11-03 09:51:00.002606+00	\N
707a8a40-0cf7-4fa3-b34f-d1ff0749671c	3a839b3d-f0a3-46e7-b6ea-eaac1647dc36	b25e6d6f-6a49-438f-bf6e-ccf8dfe1cf5c	SD-CN9P-AFX5	Kumar Dissanayake	0749245321	295 Havelock Road, Colombo 05	DELIVERED	1660.00	LKR	order-49d138ad-f834-a890-d895-2d09aa9ca7c8	2026-11-05 03:20:00.001918+00	\N
4c154fd6-0be3-4c3f-a3f3-4f01e2a04622	31a28baa-be0f-4565-8862-ff126e1cd4a9	154924a3-8730-419e-a343-13706d0c1058	SD-GSKL-H4C2	Senuri Silva	0710828238	176 Old Kesbewa Road, Nugegoda	DELIVERED	1170.00	LKR	order-a8ed2e83-3fc2-f16d-a2d4-d33a490b9e54	2026-11-06 07:51:00.002138+00	\N
64003347-2c82-48e1-beb2-cd6a8f390e20	f4d76c8c-22f9-4947-a7a5-a4469d18e781	f7793586-0040-4ff4-8b89-1b95b14a8b4c	SD-CSTT-Z5GR	Sahan Bandara	0748549742	256 High Level Road, Maharagama	DELIVERED	620.00	LKR	order-907e70d7-18d9-e3f7-63ff-3d2b1a853c76	2026-11-07 04:42:00.002297+00	\N
bb5d84ac-ff14-4744-9736-d30ef0a47ab5	8eef88b2-09ae-4482-a9af-c755adcc9c2b	a0ad2836-2f19-4941-8245-a58d49b505c8	SD-SNLA-VS3N	Sahan Silva	0740623452	303 Flower Road, Colombo 07	DELIVERED	2270.00	LKR	order-849b6be9-a155-219e-7fe2-98a03fa5f3b8	2026-11-07 11:03:00.002033+00	\N
013006f4-05af-487b-98ba-d70c3b2d7b70	9a6cef18-d765-487a-bf61-4d62a9a02714	688bba91-5c24-4cd1-a4fb-3d27eaf95136	SD-G9UN-J8FH	Malith Dissanayake	0728322488	85 High Level Road, Maharagama	DELIVERED	480.00	LKR	order-64cbc718-5d3d-0cd7-f9c1-691af2dc310b	2026-11-07 11:24:00.002905+00	\N
c517ae02-265b-41a1-90fa-309b7d80b18d	c15f4f15-fcc6-4bd4-8d2f-a310b3ef706a	ec86b9cc-0a2d-4262-8fed-9078abbea755	SD-VFH8-TGRQ	Dimuth Jayawardena	0788076243	193 Main Street, Colombo 11	DELIVERED	570.00	LKR	order-1c3a7051-0362-337c-35e0-ee5b3218eb85	2026-11-08 02:48:00.002395+00	\N
71997064-4835-4a93-832a-f47166468c1c	3a839b3d-f0a3-46e7-b6ea-eaac1647dc36	4ff8d9bd-1c7c-45dc-9b7b-a64446eab5d4	SD-6APB-2YDD	Ayesha Cooray	0774193038	128 Colombo Road, Negombo	FAILED	1090.00	LKR	order-b7d7ba8f-398d-6481-3281-c301e4e9f4c5	2026-11-08 06:30:00.001821+00	\N
073cbca5-d8fa-4943-aec8-b527534f0a15	31a28baa-be0f-4565-8862-ff126e1cd4a9	85e4a87c-812b-4c27-b5c1-bc7c980d255d	SD-MBXZ-QQ8Z	Anjali Bandara	0702339414	27 Old Kesbewa Road, Nugegoda	DELIVERED	770.00	LKR	order-8fcce0f9-98e2-7fa9-a373-ec1b502f7ad1	2026-11-09 02:35:00.001875+00	\N
2c27adfa-1b92-4a2d-9ba3-94d6aed33778	f4d76c8c-22f9-4947-a7a5-a4469d18e781	c240f03e-4d7d-40ea-815d-80bcf9746f07	SD-5Y5U-CKUE	Chamari Rajapaksa	0709264143	300 York Street, Colombo 01	DELIVERED	470.00	LKR	order-07f33066-556f-a366-b80c-8ca4cc30565f	2026-11-09 03:22:00.002029+00	\N
38887439-67b0-4f1f-b5df-a4baa3427c19	8eef88b2-09ae-4482-a9af-c755adcc9c2b	9d8c82e1-dd8c-4c2c-88fd-c1e6915def6a	SD-ES8F-54XH	Sahan Senanayake	0785145987	26 Station Road, Nugegoda	DELIVERED	1410.00	LKR	order-20eef4c6-03bf-e426-a9c8-50b8b1dc6c00	2026-11-09 05:16:00.002488+00	\N
f9110f38-b8c5-45b6-ae0e-6730efa39af3	9a6cef18-d765-487a-bf61-4d62a9a02714	ff9e2078-d372-4467-a80c-cc8a15646694	SD-4DAS-592D	Pradeep Abeysekera	0764679215	43 Dalada Veediya, Kandy	DELIVERED	870.00	LKR	order-47a9e222-0725-ff12-7d9b-64d60330c275	2026-11-09 06:29:00.002883+00	\N
a035e5c0-1647-4fea-b0de-141546a45fc5	c15f4f15-fcc6-4bd4-8d2f-a310b3ef706a	caa06da7-08da-4b1a-b3bc-5c7177a55a01	SD-BY4S-FYCD	Rizwan Karunaratne	0746177174	50 Havelock Road, Colombo 05	DELIVERED	680.00	LKR	order-4e6e3296-2c88-7998-758b-5ec66d00d5ad	2026-11-09 08:41:00.002457+00	\N
ec4533db-42df-4d6a-9705-344228debc7c	3a839b3d-f0a3-46e7-b6ea-eaac1647dc36	b1f029fb-5b57-4ac3-bd27-de28ccff4f47	SD-PEN8-3JKS	Chamari Sivakumar	0722170891	160 De Saram Road, Mount Lavinia	FAILED	810.00	LKR	order-d3549a52-05b7-92a8-fcca-a182fece40fd	2026-11-10 09:44:00.002345+00	\N
2d3d7290-1c7c-4b74-b716-71e8c1c8e685	31a28baa-be0f-4565-8862-ff126e1cd4a9	d6b559e9-6a79-4d00-bac3-4eb32cc612c9	SD-D6GF-YCUN	Nipuni Wickramasinghe	0771682242	178 Galle Road, Colombo 03	DELIVERED	650.00	LKR	order-4aea20cd-9604-7c89-802d-13e4bdeee796	2026-11-10 10:20:00.00251+00	\N
305c77c8-ab3a-4847-ad96-4c34fdb7928c	f4d76c8c-22f9-4947-a7a5-a4469d18e781	4484943d-e478-4ef4-a596-55c48a5ff860	SD-UVAK-9DZH	Sanduni Rajendran	0763450437	224 Havelock Road, Colombo 05	FAILED	570.00	LKR	order-c3fcfc4b-1654-3eb6-3dfe-bb841bb9b777	2026-11-11 04:05:00.002472+00	\N
c36036f9-ecc1-48fd-8df3-9d7bad6006cb	8eef88b2-09ae-4482-a9af-c755adcc9c2b	90213101-b437-47ac-b550-56a539a42728	SD-B32B-YZU2	Sachini Rathnayake	0763239439	275 Galle Road, Dehiwala	DELIVERED	580.00	LKR	order-975d6c86-60bf-3873-9c95-61e3e0b71865	2026-11-11 11:32:00.002666+00	\N
bbb13bed-b492-4262-aa4e-64c3265f13af	9a6cef18-d765-487a-bf61-4d62a9a02714	14362f56-a35a-47eb-b2ae-0366b9e71ac6	SD-U7WJ-FETA	Dinuka Senanayake	0772156162	223 William Gopallawa Mawatha, Kandy	DELIVERED	650.00	LKR	order-9329f57b-da7a-15d2-747a-efc4ee3b2b10	2026-11-12 06:23:00.00247+00	\N
448cb36d-168c-4d6a-83dd-b884d06436bf	c15f4f15-fcc6-4bd4-8d2f-a310b3ef706a	2417c5aa-da80-4151-8029-587bdaceea64	SD-BJPD-AWVD	Priya Rathnayake	0781422584	109 High Level Road, Maharagama	DELIVERED	1250.00	LKR	order-540faaf6-e4c0-90a4-4d9f-cb4a06e4cb0d	2026-11-12 08:43:00.00264+00	\N
f5cf0010-0b12-4551-a31f-e6a73f8a9137	3a839b3d-f0a3-46e7-b6ea-eaac1647dc36	783227ad-3712-4cfc-aad3-3edb0a29911f	SD-5MQB-GJ74	Nadeesha Cooray	0785293326	252 Lewis Place, Negombo	DELIVERED	690.00	LKR	order-0086d2c5-4a56-3622-14a5-14da0929e180	2026-11-12 09:50:00.001983+00	\N
0eb0a706-9d0b-41b5-966e-079071c5691c	31a28baa-be0f-4565-8862-ff126e1cd4a9	00dbf2cd-b81c-4f80-bc78-2909a2fa178b	SD-5NDC-S7E8	Lahiru Perera	0704128436	314 Galle Road, Colombo 04	DELIVERED	990.00	LKR	order-2ecd3a68-2930-bb26-60fa-a433053a6e39	2026-11-13 05:49:00.002204+00	\N
3fa6ff4f-7a8b-48d6-9d04-a82848f57a8d	f4d76c8c-22f9-4947-a7a5-a4469d18e781	a486c9af-0166-418f-93dc-23aa7e7ff015	SD-2J73-D7AX	Priya Samarasinghe	0712092465	18 High Level Road, Maharagama	DELIVERED	1370.00	LKR	order-42e23fd2-3106-4c45-9b15-d17ea4f6c7d8	2026-11-13 06:10:00.002797+00	\N
491710a0-7724-4478-8a25-bb607ce66646	8eef88b2-09ae-4482-a9af-c755adcc9c2b	e483ca82-9218-4bd4-91bf-de55195353e8	SD-6SNX-3XGV	Ruwan Rathnayake	0761303469	192 Pita Kotte Road, Kotte	DELIVERED	720.00	LKR	order-21257e51-efbc-2148-58cf-dda441eeef88	2026-11-13 08:29:00.003176+00	\N
78fead35-4d83-45b2-b7cf-b1a698152ab6	9a6cef18-d765-487a-bf61-4d62a9a02714	32277336-20d2-4f64-919c-b3cdcdcc8c2e	SD-6MMF-PF4L	Kumar Fonseka	0728906850	7 Old Kesbewa Road, Nugegoda	IN_TRANSIT	1880.00	LKR	order-dec5a8d7-c3ca-63c7-6454-4ea088df831b	2026-11-14 02:38:00.002793+00	\N
483377f0-45ee-4e3e-a55f-f5ab93285a41	c15f4f15-fcc6-4bd4-8d2f-a310b3ef706a	cfcdc245-1ee8-41ae-8a4f-7562132355dc	SD-DBK4-HPTK	Kumar Gunasekara	0700482031	208 Parliament Road, Rajagiriya	IN_TRANSIT	1210.00	LKR	order-eb05ca26-758b-08e8-17ac-73560bae2674	2026-11-14 03:50:00.002601+00	\N
ac91aa4a-a8e6-4af2-9133-22ab1ac43909	3a839b3d-f0a3-46e7-b6ea-eaac1647dc36	35f6d117-0326-4c47-ae13-9d4573c8e97d	SD-A4NM-UKNG	Chathura Herath	0708654467	182 Stanley Thilakaratne Mawatha, Nugegoda	PICKED_UP	870.00	LKR	order-b3dc0260-c8e2-b2f5-9238-79b4080772c8	2026-11-14 07:09:00.003114+00	\N
2225746a-05d5-44ae-863e-7e01e24d98a5	31a28baa-be0f-4565-8862-ff126e1cd4a9	d4d5a8c2-fa2f-458b-b197-7103c6e01381	SD-8CJA-3BEA	Hashini Liyanage	0745176929	321 Pita Kotte Road, Kotte	CREATED	690.00	LKR	order-f17381e9-f543-db2b-8e6c-51aacd80aca9	2026-11-14 07:35:09.003049+00	\N
ee701a6d-ef45-4ac2-a493-f9098e6a54c5	f4d76c8c-22f9-4947-a7a5-a4469d18e781	c32fb930-0f69-421c-9fef-998c096ef091	SD-PH2H-4FRV	Arun de Silva	0784258553	6 De Saram Road, Mount Lavinia	CREATED	500.00	LKR	order-c06c5648-9201-b61e-e8c8-d79bb617bd58	2026-11-14 08:09:00.0021+00	\N
\.


--
-- Data for Name: merchant_webhook_endpoints; Type: TABLE DATA; Schema: public; Owner: merchant
--

COPY public.merchant_webhook_endpoints (id, merchant_id, webhook_url, secret, is_active, created_at, updated_at) FROM stdin;
f6b4778d-cc0c-4e36-b25b-46f860e3a767	31a28baa-be0f-4565-8862-ff126e1cd4a9	https://kandy-spice-house.example.com/swiftdrop/webhook	whsec_xO9gNjqwbFRhiHEQiphqX2if6Oxs9fbuym74jmEf-Mw	t	2026-09-21 03:06:00+00	\N
da06e9d3-0eba-4d97-9ce9-1129deb81fa0	f4d76c8c-22f9-4947-a7a5-a4469d18e781	https://colombo-threads.example.com/swiftdrop/webhook	whsec_tctKLOGEMDNZgzD6cvHDWgzjGvZv2RJ5rY2G7twvEf4	t	2026-09-03 05:06:00+00	\N
c1b8ec34-5ab6-401b-a812-97bb83320ce4	8eef88b2-09ae-4482-a9af-c755adcc9c2b	https://techlanka-gadgets.example.com/swiftdrop/webhook	whsec_ZqT09iXtuyMkrtOqc83T6oUEMUeYyq8a4IYzP88PCJI	t	2026-09-18 21:49:00+00	\N
e71118e5-ba76-42bd-939d-55c7fdbcb7e4	9a6cef18-d765-487a-bf61-4d62a9a02714	https://ceylon-tea-corner.example.com/swiftdrop/webhook	whsec_d00m0MX4u-hRa6es2ov06wMXb9hf6betQZCYHQWRyF0	t	2026-09-18 22:19:00+00	\N
531ab7fa-bfaf-4f9d-9465-b664cfe4dfdd	c15f4f15-fcc6-4bd4-8d2f-a310b3ef706a	https://booknook-lanka.example.com/swiftdrop/webhook	whsec_-CPmj9pdKabX4tDj27mb_HjVHR_W-8NdJ7eFIO8Z5vQ	t	2026-09-22 03:26:00+00	\N
fe4617d1-e376-46b8-a51f-9831c416e807	3a839b3d-f0a3-46e7-b6ea-eaac1647dc36	https://galle-fort-crafts.example.com/swiftdrop/webhook	whsec_sA5quTKH_uiAkVFxmO_qaJV8ffWtrTvP5aim4sUYSjs	t	2026-09-16 03:08:00+00	\N
\.


--
-- Data for Name: merchants; Type: TABLE DATA; Schema: public; Owner: merchant
--

COPY public.merchants (id, user_id, business_name, business_reg_no, contact_name, email, phone, address, status, rejection_reason, created_at, updated_at) FROM stdin;
31a28baa-be0f-4565-8862-ff126e1cd4a9	5bbfdaf0-3d8d-4ceb-bd93-00c91573ac23	Kandy Spice House	PV564525	Ruwan Fernando	ruwan.fernando@merchant.example.com	0772401774	59 William Gopallawa Mawatha, Kandy	APPROVED	\N	2026-09-20 10:06:00+00	2026-09-21 01:06:00+00
f4d76c8c-22f9-4947-a7a5-a4469d18e781	30056f25-6559-438c-9980-4e2a84365b06	Colombo Threads	PV222294	Sajith Karunaratne	sajith.karunaratne@merchant.example.com	0774891635	243 Prince of Wales Avenue, Colombo 14	APPROVED	\N	2026-09-01 11:06:00+00	2026-09-03 03:06:00+00
8eef88b2-09ae-4482-a9af-c755adcc9c2b	cc91bacd-b679-443d-8d92-da68e6bdc2be	TechLanka Gadgets	PV251830	Lahiru Senanayake	lahiru.senanayake@merchant.example.com	0770480984	188 Kollupitiya Road, Colombo 03	APPROVED	\N	2026-09-18 09:49:00+00	2026-09-18 19:49:00+00
9a6cef18-d765-487a-bf61-4d62a9a02714	9e6c3a6a-7c87-4f1d-a0b1-6509fb5a96d3	Ceylon Tea Corner	PV743448	Lahiru Wijesinghe	lahiru.wijesinghe@merchant.example.com	0782262943	85 High Level Road, Maharagama	APPROVED	\N	2026-09-17 12:19:00+00	2026-09-18 20:19:00+00
c15f4f15-fcc6-4bd4-8d2f-a310b3ef706a	8131f094-5b5f-4331-8afd-7d7f917074e5	BookNook Lanka	PV371962	Chathura Perera	chathura.perera@merchant.example.com	0770943404	184 Thimbirigasyaya Road, Colombo 05	APPROVED	\N	2026-09-21 16:26:00+00	2026-09-22 01:26:00+00
3a839b3d-f0a3-46e7-b6ea-eaac1647dc36	7635a541-77c2-47a8-a362-e854d888fcfc	Galle Fort Crafts	PV249709	Pradeep Wickramasinghe	pradeep.wickramasinghe@merchant.example.com	0760290556	59 Church Street, Galle Fort	APPROVED	\N	2026-09-14 14:08:00+00	2026-09-16 01:08:00+00
f98744ae-7a97-4c3c-adb8-15465af0de06	b498d433-f9af-4096-a491-d8d4097b64ed	Negombo Fresh Fish	PV743446	Pradeep Rajapaksa	pradeep.rajapaksa@merchant.example.com	0726996524	87 Lewis Place, Negombo	PENDING	\N	2026-09-06 08:27:00+00	\N
843688d0-2418-468b-9b43-3c501e381115	25d7fcf0-1032-4973-a56d-0a547777f402	QuickFix Mobile Parts	PV456476	Oshada Weerasinghe	oshada.weerasinghe@merchant.example.com	0787692766	228 Parliament Road, Rajagiriya	REJECTED	Business registration certificate could not be verified	2026-09-17 15:25:00+00	2026-09-19 04:25:00+00
\.


--
-- Name: flyway_schema_history flyway_schema_history_pk; Type: CONSTRAINT; Schema: public; Owner: merchant
--

ALTER TABLE ONLY public.flyway_schema_history
    ADD CONSTRAINT flyway_schema_history_pk PRIMARY KEY (installed_rank);


--
-- Name: merchant_api_keys merchant_api_keys_key_hash_key; Type: CONSTRAINT; Schema: public; Owner: merchant
--

ALTER TABLE ONLY public.merchant_api_keys
    ADD CONSTRAINT merchant_api_keys_key_hash_key UNIQUE (key_hash);


--
-- Name: merchant_api_keys merchant_api_keys_pkey; Type: CONSTRAINT; Schema: public; Owner: merchant
--

ALTER TABLE ONLY public.merchant_api_keys
    ADD CONSTRAINT merchant_api_keys_pkey PRIMARY KEY (id);


--
-- Name: merchant_orders merchant_orders_pkey; Type: CONSTRAINT; Schema: public; Owner: merchant
--

ALTER TABLE ONLY public.merchant_orders
    ADD CONSTRAINT merchant_orders_pkey PRIMARY KEY (id);


--
-- Name: merchant_webhook_endpoints merchant_webhook_endpoints_merchant_id_key; Type: CONSTRAINT; Schema: public; Owner: merchant
--

ALTER TABLE ONLY public.merchant_webhook_endpoints
    ADD CONSTRAINT merchant_webhook_endpoints_merchant_id_key UNIQUE (merchant_id);


--
-- Name: merchant_webhook_endpoints merchant_webhook_endpoints_pkey; Type: CONSTRAINT; Schema: public; Owner: merchant
--

ALTER TABLE ONLY public.merchant_webhook_endpoints
    ADD CONSTRAINT merchant_webhook_endpoints_pkey PRIMARY KEY (id);


--
-- Name: merchants merchants_business_reg_no_key; Type: CONSTRAINT; Schema: public; Owner: merchant
--

ALTER TABLE ONLY public.merchants
    ADD CONSTRAINT merchants_business_reg_no_key UNIQUE (business_reg_no);


--
-- Name: merchants merchants_email_key; Type: CONSTRAINT; Schema: public; Owner: merchant
--

ALTER TABLE ONLY public.merchants
    ADD CONSTRAINT merchants_email_key UNIQUE (email);


--
-- Name: merchants merchants_pkey; Type: CONSTRAINT; Schema: public; Owner: merchant
--

ALTER TABLE ONLY public.merchants
    ADD CONSTRAINT merchants_pkey PRIMARY KEY (id);


--
-- Name: merchants merchants_user_id_key; Type: CONSTRAINT; Schema: public; Owner: merchant
--

ALTER TABLE ONLY public.merchants
    ADD CONSTRAINT merchants_user_id_key UNIQUE (user_id);


--
-- Name: flyway_schema_history_s_idx; Type: INDEX; Schema: public; Owner: merchant
--

CREATE INDEX flyway_schema_history_s_idx ON public.flyway_schema_history USING btree (success);


--
-- Name: idx_api_keys_hash; Type: INDEX; Schema: public; Owner: merchant
--

CREATE INDEX idx_api_keys_hash ON public.merchant_api_keys USING btree (key_hash);


--
-- Name: idx_api_keys_merchant; Type: INDEX; Schema: public; Owner: merchant
--

CREATE INDEX idx_api_keys_merchant ON public.merchant_api_keys USING btree (merchant_id);


--
-- Name: idx_merchant_orders_idemp; Type: INDEX; Schema: public; Owner: merchant
--

CREATE INDEX idx_merchant_orders_idemp ON public.merchant_orders USING btree (merchant_id, idempotency_key);


--
-- Name: idx_merchant_orders_merchant; Type: INDEX; Schema: public; Owner: merchant
--

CREATE INDEX idx_merchant_orders_merchant ON public.merchant_orders USING btree (merchant_id);


--
-- Name: idx_merchant_orders_parcel; Type: INDEX; Schema: public; Owner: merchant
--

CREATE INDEX idx_merchant_orders_parcel ON public.merchant_orders USING btree (parcel_id);


--
-- Name: idx_merchant_orders_tracking; Type: INDEX; Schema: public; Owner: merchant
--

CREATE INDEX idx_merchant_orders_tracking ON public.merchant_orders USING btree (tracking_no);


--
-- Name: idx_merchants_email; Type: INDEX; Schema: public; Owner: merchant
--

CREATE INDEX idx_merchants_email ON public.merchants USING btree (email);


--
-- Name: idx_merchants_reg_no; Type: INDEX; Schema: public; Owner: merchant
--

CREATE INDEX idx_merchants_reg_no ON public.merchants USING btree (business_reg_no);


--
-- Name: idx_merchants_status; Type: INDEX; Schema: public; Owner: merchant
--

CREATE INDEX idx_merchants_status ON public.merchants USING btree (status);


--
-- Name: idx_merchants_user_id; Type: INDEX; Schema: public; Owner: merchant
--

CREATE INDEX idx_merchants_user_id ON public.merchants USING btree (user_id);


--
-- Name: idx_webhooks_merchant; Type: INDEX; Schema: public; Owner: merchant
--

CREATE INDEX idx_webhooks_merchant ON public.merchant_webhook_endpoints USING btree (merchant_id);


--
-- Name: merchant_api_keys merchant_api_keys_merchant_id_fkey; Type: FK CONSTRAINT; Schema: public; Owner: merchant
--

ALTER TABLE ONLY public.merchant_api_keys
    ADD CONSTRAINT merchant_api_keys_merchant_id_fkey FOREIGN KEY (merchant_id) REFERENCES public.merchants(id) ON DELETE CASCADE;


--
-- Name: merchant_orders merchant_orders_merchant_id_fkey; Type: FK CONSTRAINT; Schema: public; Owner: merchant
--

ALTER TABLE ONLY public.merchant_orders
    ADD CONSTRAINT merchant_orders_merchant_id_fkey FOREIGN KEY (merchant_id) REFERENCES public.merchants(id) ON DELETE CASCADE;


--
-- Name: merchant_webhook_endpoints merchant_webhook_endpoints_merchant_id_fkey; Type: FK CONSTRAINT; Schema: public; Owner: merchant
--

ALTER TABLE ONLY public.merchant_webhook_endpoints
    ADD CONSTRAINT merchant_webhook_endpoints_merchant_id_fkey FOREIGN KEY (merchant_id) REFERENCES public.merchants(id) ON DELETE CASCADE;


--
-- PostgreSQL database dump complete
--

\unrestrict M8A0GphJEGnqfR0EQGfUlcBbUHcD6DAkojNcjdHOFbo3sd3WopP4nmGy7wC7X7X

