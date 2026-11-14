--
-- PostgreSQL database dump
--

\restrict TXA1XBYNkE8XpeRJ2WxeKHImKUCgy0FhRDs4sJtR6NqTrTn4AsEOgsgUX5ercaW

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
ALTER TABLE IF EXISTS ONLY public.processed_events DROP CONSTRAINT IF EXISTS uq_processed_events_event_consumer;
ALTER TABLE IF EXISTS ONLY public.processed_events DROP CONSTRAINT IF EXISTS processed_events_pkey;
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
DROP TABLE IF EXISTS public.processed_events;
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
-- Name: processed_events; Type: TABLE; Schema: public; Owner: merchant
--

CREATE TABLE public.processed_events (
    id uuid NOT NULL,
    event_id uuid NOT NULL,
    consumer_name character varying(50) NOT NULL,
    processed_at timestamp with time zone DEFAULT now() NOT NULL
);


ALTER TABLE public.processed_events OWNER TO merchant;

--
-- Data for Name: flyway_schema_history; Type: TABLE DATA; Schema: public; Owner: merchant
--

COPY public.flyway_schema_history (installed_rank, version, description, type, script, checksum, installed_by, installed_on, execution_time, success) FROM stdin;
1	1	create merchants	SQL	V1__create_merchants.sql	16135107	merchant	2026-11-14 14:11:40.304815	30	t
2	2	create merchant api keys	SQL	V2__create_merchant_api_keys.sql	1894479372	merchant	2026-11-14 14:11:40.377165	22	t
3	3	create merchant webhook endpoints	SQL	V3__create_merchant_webhook_endpoints.sql	-1702115928	merchant	2026-11-14 14:11:40.420946	31	t
4	4	create merchant orders	SQL	V4__create_merchant_orders.sql	28251337	merchant	2026-11-14 14:11:40.45992	34	t
5	5	create processed events	SQL	V5__create_processed_events.sql	-475888314	merchant	2026-11-14 14:11:40.507291	9	t
\.


--
-- Data for Name: merchant_api_keys; Type: TABLE DATA; Schema: public; Owner: merchant
--

COPY public.merchant_api_keys (id, merchant_id, name, key_prefix, key_hash, revoked, revoked_at, expires_at, created_at, updated_at) FROM stdin;
625ec3c3-d1cc-4318-87d2-140e408cee68	b696e59a-1df2-46bb-b4eb-7ed663dbb634	Online store	sd_live_VRYr6VU8	d5e30ddb4ec8012af409985e712df1b6c0e43498d883ab6d24f0ff2bbc684024	f	\N	\N	2026-09-05 15:35:00+00	\N
ee9ca95c-52ff-42a3-ba18-0ad73494e6d8	c47e35ec-77fd-4d96-acd3-32dd2c35f9c3	Online store	sd_live_O5PR9cQJ	376f0a9d8c83d5f51a523baf309a943f9ea2ee207ccc05e99d4f31ee118b2fe2	f	\N	\N	2026-09-20 15:13:00+00	\N
b80b68ab-ff4c-4e6c-ad74-c470acf7ac8f	048bd6b8-66d4-44c9-8d76-89568ca52320	Online store	sd_live_qi0HwIDu	9fcce7e5478d3bfc155d754af1f070d3afee19d760401946fbbf9a71c800205e	f	\N	\N	2026-09-15 15:35:00+00	\N
a4184127-acf4-4743-9e72-09d5f3a2ffe2	e36d57bd-94eb-445d-892f-b6e9d14f795d	Online store	sd_live_yZmr62OF	fb0a60f5bb0d31efd53da5383c25f4f4648671fb115c49371979aabdca561c38	f	\N	\N	2026-09-03 19:57:00+00	\N
d16585b2-b01f-4d8d-8401-4c646a692de7	7aaaa942-2283-4848-9835-2ac9ea183ca9	Online store	sd_live_YErIeiZc	fcd07f69f449c9635eb2e4cdb88653f50c8f8eef0b64cfb51b8913744d9f52f4	f	\N	\N	2026-09-10 14:31:00+00	\N
57dd50e6-1c63-4a48-92da-14ad179b7776	7aaaa942-2283-4848-9835-2ac9ea183ca9	Warehouse system	sd_live_NhhzF2pg	f2302339d832791f11a9af5df484b3646bdfec9004ffc1dfb469c18539428cfb	f	\N	\N	2026-09-10 14:31:00+00	\N
6805c6d1-4ca8-4777-a6dc-e2db9e24f174	a5d2760a-c8c5-4540-bb34-6bee9391d166	Online store	sd_live_qbGCR08k	909e726f4a7ed2b32ec8733ccec1e86c2bb3abc6b296fa0c1519dc26e8c833a6	f	\N	\N	2026-09-20 21:24:00+00	\N
\.


--
-- Data for Name: merchant_orders; Type: TABLE DATA; Schema: public; Owner: merchant
--

COPY public.merchant_orders (id, merchant_id, parcel_id, tracking_no, recipient_name, recipient_phone, dropoff_address, status, fee_amount, fee_currency, idempotency_key, created_at, updated_at) FROM stdin;
efc05242-12b6-4de3-84a3-7e0f9c99f334	7aaaa942-2283-4848-9835-2ac9ea183ca9	b179e9e2-1ce3-4e22-ba43-97fb342cff79	SD-6JZF-XMRA	Dulani Herath	0784039225	225 Maradana Road, Colombo 10	DELIVERED	470.00	LKR	order-c53d6866-ecaa-2774-5760-38f239d7e48b	2026-10-18 12:03:00.002662+00	2026-10-19 05:28:07.789631+00
91d07e97-3418-4153-90b7-48e02b8acc74	a5d2760a-c8c5-4540-bb34-6bee9391d166	d469c568-fd20-48e2-9849-a59320621231	SD-JGNM-QBJ3	Dinuka Mendis	0783923669	41 High Level Road, Maharagama	DELIVERED	1110.00	LKR	order-081a9cdb-a6fa-46ca-6017-f813a51cd540	2026-10-20 06:03:00.002362+00	2026-10-21 05:30:07.714703+00
8295bc1d-d148-455e-a751-ae29a9630774	b696e59a-1df2-46bb-b4eb-7ed663dbb634	bdfffb72-f48a-42da-9a26-b8723321444b	SD-VGKE-WSJZ	Methmi Gunasekara	0766503720	339 Prince of Wales Avenue, Colombo 14	DELIVERED	1110.00	LKR	order-d4b9d5f2-7116-4144-9c48-dfa0be0288b2	2026-10-20 13:02:00.00226+00	2026-10-21 08:08:07.694753+00
1c19c630-31b3-4769-a806-ed6fbffb95ae	b696e59a-1df2-46bb-b4eb-7ed663dbb634	8f55cfc5-a5d8-48c0-8af3-fab66487b40d	SD-MJSL-4HNV	Hashini Samarasinghe	0703373622	266 Ramakrishna Road, Colombo 06	DELIVERED	690.00	LKR	order-81a12067-dd67-afd2-b224-79c70ecac43c	2026-10-30 06:14:00.002033+00	2026-10-31 06:20:08.472767+00
e26091ab-05a4-45e4-b043-9c1efbf01e7c	048bd6b8-66d4-44c9-8d76-89568ca52320	1611654b-7391-4819-bb9a-749efaf76799	SD-6X3C-NQKM	Nipuni Rajapaksa	0710936099	110 Hotel Road, Mount Lavinia	DELIVERED	1290.00	LKR	order-b2b86dba-f579-d3fb-eee8-de83e9b0a277	2026-10-30 10:06:00.00263+00	2026-10-31 06:36:08.380464+00
d26f691a-9b6f-4a41-bf6c-cd824a2668e6	e36d57bd-94eb-445d-892f-b6e9d14f795d	2e369813-cb50-42bc-abfb-6a92062e879a	SD-RFR2-GUZ9	Lakshmi Senanayake	0742387913	30 High Level Road, Maharagama	FAILED	1150.00	LKR	order-f05e00c9-8158-6a03-3a84-671b4517268e	2026-10-31 05:37:00.002075+00	2026-11-01 04:57:08.336143+00
abdb5723-7263-4c34-88b7-f8e2a21d7b72	e36d57bd-94eb-445d-892f-b6e9d14f795d	41497e83-1823-4a69-a831-69649128c588	SD-4HML-GUZ8	Dinuka Wijesinghe	0706141472	219 Ward Place, Colombo 07	FAILED	860.00	LKR	order-d221b044-ee42-8a0b-6e11-a620694604af	2026-10-21 09:31:00.002359+00	2026-10-22 06:57:08.58518+00
0aba4acb-85dd-4eb7-898c-0fe132018b4b	b696e59a-1df2-46bb-b4eb-7ed663dbb634	fe2c3f20-9e75-460c-b1f5-2d2dcc64b823	SD-SESA-M65D	Kumar Jayawardena	0769462037	124 Flower Road, Colombo 07	DELIVERED	730.00	LKR	order-a0a5a8f6-3398-52b0-031f-f57e2f7660ee	2026-10-16 07:14:00.007286+00	2026-10-17 05:16:07.943589+00
1d76d635-58d2-4925-9665-29259e94c11a	a5d2760a-c8c5-4540-bb34-6bee9391d166	ccb4457b-105d-49e1-a3be-3c09364b87a2	SD-Q6SQ-3E32	Sahan Ratnayake	0787582224	214 Church Street, Galle Fort	DELIVERED	1010.00	LKR	order-c65eb134-4726-9cd5-e539-2f5bcbbb459d	2026-10-25 02:57:00.003158+00	2026-10-25 10:03:07.951689+00
6b683cec-6aec-45fd-b1b2-3fd8e99715eb	c47e35ec-77fd-4d96-acd3-32dd2c35f9c3	d4b01bbf-7822-4a71-a00b-0981c775eee5	SD-PUHU-K6HA	Dinuka Perera	0714596522	30 Hill Street, Dehiwala	DELIVERED	1430.00	LKR	order-bf21ed87-7de9-d602-3122-0192e7d0f941	2026-10-17 04:36:00.002372+00	2026-10-18 05:51:07.906804+00
dcb02ad7-245c-4f1d-9ce4-57f9835bbde1	048bd6b8-66d4-44c9-8d76-89568ca52320	ddb2296e-be6e-4324-b41b-3cbb7c94f00e	SD-V8JJ-PJMR	Rizwan Gunasekara	0789551969	289 Lewis Place, Negombo	DELIVERED	830.00	LKR	order-95ad7af0-b3b9-4d10-a91d-07a45df6fa7e	2026-10-17 07:08:00.003037+00	2026-10-18 07:13:07.870581+00
55848354-0b63-478f-a40e-36adf0fe9116	e36d57bd-94eb-445d-892f-b6e9d14f795d	19342e31-ec55-4052-b870-c94846ce7669	SD-XHK8-RVC8	Ayesha Jayawardena	0776327798	258 Chatham Street, Colombo 01	DELIVERED	520.00	LKR	order-71656d9b-eacf-5512-e2ff-afefe6c80967	2026-10-17 13:12:00.002572+00	2026-10-18 06:35:07.828303+00
31536596-10b2-4a07-b54f-c2b25cca8b2b	048bd6b8-66d4-44c9-8d76-89568ca52320	d64dff65-8795-4896-92f5-032cd4425083	SD-EXNH-72W9	Hiruni Jayasinghe	0779588539	64 Marine Drive, Colombo 06	DELIVERED	1080.00	LKR	order-b5a1416e-6616-15ea-a60c-73dadc942076	2026-10-21 09:06:00.0031+00	2026-10-22 05:17:08.63511+00
176519c2-159b-4563-be19-42f96fce93a5	b696e59a-1df2-46bb-b4eb-7ed663dbb634	3be21c6e-d0ad-4c8e-b1a6-49939ccef259	SD-8BX8-NG5N	Nipuni Rajendran	0728436360	338 Peradeniya Road, Kandy	DELIVERED	1420.00	LKR	order-b4bfd820-9351-fd72-5047-89f44b8b2655	2026-10-25 04:03:00.002617+00	2026-10-26 07:23:07.91214+00
96652b97-d7b6-49d7-aa50-f988370b4c06	048bd6b8-66d4-44c9-8d76-89568ca52320	1fe88d4c-8ada-4052-b597-2d31ebd9d086	SD-EUPJ-FMVF	Priya Samarasinghe	0712092465	18 High Level Road, Maharagama	IN_TRANSIT	1370.00	LKR	order-42e23fd2-3106-4c45-9b15-d17ea4f6c7d8	2026-11-14 03:50:00.002309+00	2026-11-14 07:53:07.935061+00
f97e03b4-7b64-4712-85f5-2b41a17708bb	e36d57bd-94eb-445d-892f-b6e9d14f795d	c9736aac-0bdf-4cc9-887a-232f2e8ec46e	SD-ZCXL-D77C	Shehan Herath	0718130346	192 Pita Kotte Road, Kotte	DELIVERED	670.00	LKR	order-21257e51-efbc-2148-58cf-dda441eeef88	2026-11-14 05:16:00.002839+00	2026-11-14 13:49:10.927579+00
04c1efb2-3ece-4bfc-b581-1ed16c11344f	7aaaa942-2283-4848-9835-2ac9ea183ca9	ade03cc5-c9ce-41b4-b154-fdc57b329c01	SD-Z3VH-XBD6	Kumar Fonseka	0728906850	7 Old Kesbewa Road, Nugegoda	IN_TRANSIT	1980.00	LKR	order-dec5a8d7-c3ca-63c7-6454-4ea088df831b	2026-11-14 06:21:00.002321+00	2026-11-14 08:29:08.898934+00
61f69b51-bb65-4613-8b32-6c05e657afb5	c47e35ec-77fd-4d96-acd3-32dd2c35f9c3	01d70cc4-f0c0-4923-b33e-0766c1422a58	SD-TTBN-PZCZ	Dulani Sivakumar	0777460837	142 Pita Kotte Road, Kotte	DELIVERED	860.00	LKR	order-c9d58b53-5226-8296-cc54-57bb1e8187e9	2026-10-25 07:06:00.002516+00	2026-10-26 05:49:08.899841+00
33bb52c6-7201-4ee8-85c6-4f31658f7adc	c47e35ec-77fd-4d96-acd3-32dd2c35f9c3	e9a26e2f-cf1f-4b78-a7af-de0045830185	SD-W8AM-PJCX	Tharindu Jayawardena	0747109092	202 De Saram Road, Mount Lavinia	DELIVERED	760.00	LKR	order-3dfa34fe-52d3-c0d6-3569-b04435927208	2026-10-21 08:46:00.002106+00	2026-10-22 06:28:08.656736+00
e2fe3a62-b2de-4ef8-a585-e86a5942d0b8	7aaaa942-2283-4848-9835-2ac9ea183ca9	332ceee7-b0f5-443e-b29f-ff623e156268	SD-99AG-ETA9	Ishara Dissanayake	0715591053	25 Old Kesbewa Road, Nugegoda	DELIVERED	520.00	LKR	order-0bb343cf-7800-2b99-655f-f380a9f379c1	2026-10-21 11:00:00.002384+00	2026-10-22 06:05:08.57137+00
f4688aa7-e4ce-41c1-b6eb-48c777ab6ae3	a5d2760a-c8c5-4540-bb34-6bee9391d166	a0db27d3-edaa-49ff-aead-7a465b9212bc	SD-NH3H-GNQR	Pradeep Rathnayake	0718485830	73 Matara Road, Galle	DELIVERED	810.00	LKR	order-1898462e-8e04-e358-4d76-76f89c68452a	2026-10-22 08:17:00.00224+00	2026-10-23 07:41:08.515734+00
2ea39916-d5ce-42d4-b7fc-a3bd6342c1f8	b696e59a-1df2-46bb-b4eb-7ed663dbb634	32ace6bc-3a97-40d9-9e28-8e789d0e279e	SD-ZXVL-RQWJ	Anjali Dissanayake	0719348440	287 Church Street, Galle Fort	DELIVERED	870.00	LKR	order-095c5fe9-3f6c-555c-511a-f8407d401674	2026-10-22 08:30:00.002157+00	2026-10-23 08:04:08.491791+00
27b2cdc6-35b1-401e-8be5-415faf76235d	c47e35ec-77fd-4d96-acd3-32dd2c35f9c3	5d46e4bf-3bdd-4f1a-8931-d2434d62d124	SD-TNTS-UM6B	Anjali Silva	0780820881	261 Prince of Wales Avenue, Colombo 14	DELIVERED	300.00	LKR	order-0936ff1b-71d1-9424-593d-1a2b806a9bbc	2026-10-22 13:13:00.002888+00	2026-10-23 06:58:08.441477+00
0e299810-ad01-4389-864f-4526828ec054	048bd6b8-66d4-44c9-8d76-89568ca52320	908066ce-981c-4588-8eee-f6bcb2d75add	SD-HXTE-9S7R	Sunil Amarasekara	0782746654	81 Maradana Road, Colombo 10	DELIVERED	920.00	LKR	order-ba42553c-44cb-09a8-9f28-13be1edc1a84	2026-10-22 13:27:00.003093+00	2026-10-23 08:41:08.400004+00
2f288994-a5d7-4464-904d-ec3cdbaa79f7	e36d57bd-94eb-445d-892f-b6e9d14f795d	1bb394d1-5319-45b8-88ad-e58c663544bb	SD-P8CS-VETV	Priya Wijesinghe	0760468591	3 Marine Drive, Colombo 06	DELIVERED	890.00	LKR	order-96f4dfbb-0787-3eaa-a2c6-d8bea28b2942	2026-10-23 03:10:00.002867+00	2026-10-24 06:55:08.365974+00
8bcbaf00-54f7-4cea-9a17-78d028a70e6e	7aaaa942-2283-4848-9835-2ac9ea183ca9	0cd449a5-78ac-49f9-a531-ec67e81c6f9e	SD-5HC8-K45N	Hiruni Mendis	0788906201	331 Main Street, Negombo	DELIVERED	810.00	LKR	order-f9f38a57-2f23-f0da-00b6-3f89c3721601	2026-10-23 06:30:00.006691+00	2026-10-24 07:19:08.324364+00
e90e7a1a-433b-412f-9f9a-16ec94b89e89	a5d2760a-c8c5-4540-bb34-6bee9391d166	7f5b34f3-1ff3-42f1-9302-09dfce48e49a	SD-UYHZ-KMU3	Tharindu Liyanage	0728510636	187 York Street, Colombo 01	DELIVERED	1790.00	LKR	order-27f1db64-daf7-fc14-e046-8eebbeaa6af1	2026-10-23 06:47:00.003196+00	2026-10-24 05:13:08.263226+00
f5f9da6a-7f9f-4ad1-9570-af1c2fa46411	b696e59a-1df2-46bb-b4eb-7ed663dbb634	f6fcd3b4-24a2-4132-975d-ad784eeb2c87	SD-6KML-JUDF	Kasun Liyanage	0762726452	30 Church Street, Galle Fort	DELIVERED	830.00	LKR	order-a69cd690-9ff9-eb46-7cd5-53a89722bba6	2026-10-23 12:54:00.002975+00	2026-10-24 05:00:08.232228+00
77818c4c-4082-4e4a-8482-f615a08e9190	c47e35ec-77fd-4d96-acd3-32dd2c35f9c3	a41c2f77-793a-49db-b44c-69e4f354734c	SD-XKYC-Z7KZ	Malith Cooray	0724785757	265 Galle Road, Colombo 04	DELIVERED	1990.00	LKR	order-4af74d82-206c-5e31-012c-47ae321f1dcc	2026-10-24 03:07:00.002975+00	2026-10-24 11:46:08.173239+00
3e9b2e82-0c5a-423f-9b28-25cd8f094b1e	048bd6b8-66d4-44c9-8d76-89568ca52320	00dd5adb-0164-46c1-8985-3521b4225d60	SD-BEQ8-VT4U	Chathura Rajapaksa	0768870530	223 Main Street, Negombo	DELIVERED	870.00	LKR	order-dad65cae-1dad-4ec8-c1a8-0d8e6e040837	2026-10-24 04:30:00.002369+00	2026-10-25 08:18:08.137243+00
f9acca69-750d-4c69-a0b1-83933e4e838d	e36d57bd-94eb-445d-892f-b6e9d14f795d	af33a0d2-502d-40db-a579-9a9643de95ed	SD-UJ5H-MYJ5	Nipuni Samarasinghe	0725172179	201 Main Street, Negombo	DELIVERED	970.00	LKR	order-346ae13c-b968-a129-3db5-c86a43263982	2026-10-24 07:09:00.003714+00	2026-10-25 05:54:08.079498+00
0137c99b-01d6-4a94-b7fa-144107d46419	7aaaa942-2283-4848-9835-2ac9ea183ca9	c678a650-a95e-412c-b709-f79ff8ab3f3a	SD-XDL5-PPY7	Anjali Liyanage	0769500812	186 Hotel Road, Mount Lavinia	DELIVERED	580.00	LKR	order-917e9d15-89be-17c1-0acd-7a8a9d194328	2026-10-24 09:07:00.003238+00	2026-10-25 06:06:07.990864+00
6e27ed99-161f-43de-9142-61b6e2e3fc03	048bd6b8-66d4-44c9-8d76-89568ca52320	06953d4c-fa58-40b6-9ad7-2af29679740c	SD-VWKQ-Y8MW	Kavindi Mendis	0719011504	289 Stanley Thilakaratne Mawatha, Nugegoda	DELIVERED	1860.00	LKR	order-bed31bc2-665d-6db9-d40d-5305785fac61	2026-10-25 09:47:00.002858+00	2026-10-26 06:45:08.853847+00
feb2e09d-db45-4fa5-9ea5-3e0000b4721d	e36d57bd-94eb-445d-892f-b6e9d14f795d	cb43e333-5bcb-4310-9897-05feba028af4	SD-TD9Q-YZQZ	Sunil Mendis	0705098179	328 Prince of Wales Avenue, Colombo 14	DELIVERED	620.00	LKR	order-b052d170-e492-ca64-05fa-3dc916bbceb6	2026-10-25 12:39:00.003002+00	2026-10-26 06:13:08.83573+00
775f7108-849e-4fbd-a323-f8884e167b12	7aaaa942-2283-4848-9835-2ac9ea183ca9	0f9dd69f-ad77-489c-afb6-7eb48100a761	SD-ZMBW-UCFV	Ishara Fernando	0723533502	119 Marine Drive, Colombo 06	DELIVERED	560.00	LKR	order-bc6ada50-c0e1-156b-af99-d01e9c411244	2026-10-26 08:08:00.002622+00	2026-10-27 04:37:08.789028+00
cfa1e3b1-4b62-4f28-a840-74134b575986	a5d2760a-c8c5-4540-bb34-6bee9391d166	19ddc3e9-6381-4a0e-9216-3f689a542fa3	SD-UWA5-RKP3	Dinuka Jayawardena	0708909025	43 Old Kesbewa Road, Nugegoda	DELIVERED	1340.00	LKR	order-1c2a4a31-5e1f-2491-012f-54d1d8d4f340	2026-10-26 10:09:00.002935+00	2026-10-27 04:32:08.752451+00
8d594e9c-152d-4962-9d7a-a3e70e09dbbb	b696e59a-1df2-46bb-b4eb-7ed663dbb634	528fc99f-926c-4071-bc21-aa49e94d472a	SD-S6AZ-8M34	Anjali Nawaz	0784284161	157 De Saram Road, Mount Lavinia	DELIVERED	790.00	LKR	order-66d0c303-2f65-9ead-2b49-a2daee8fc4d9	2026-10-27 11:31:00.002467+00	2026-10-28 05:56:08.699408+00
3f9cce42-b349-47c5-912e-c1bfac1b2df8	c47e35ec-77fd-4d96-acd3-32dd2c35f9c3	59fe7a84-1af9-431b-ae09-d59c58b8c506	SD-GYQN-74ZX	Dilani Dissanayake	0742758272	339 Flower Road, Colombo 07	DELIVERED	1010.00	LKR	order-1fb3059d-e53e-7448-97bc-2261023eaf42	2026-10-28 06:58:00.002921+00	2026-10-28 11:24:08.673297+00
62eaa980-68f7-40b6-8fc2-9e0af9c678ff	048bd6b8-66d4-44c9-8d76-89568ca52320	4db772c3-cd9f-42d8-92a3-a24441cba66c	SD-A789-G989	Priya Dissanayake	0728124534	160 Ward Place, Colombo 07	DELIVERED	1340.00	LKR	order-9dba3264-ddb4-a562-fa85-6ef985e6a6c6	2026-10-28 07:15:00.003168+00	2026-10-29 06:46:08.645512+00
d75c0f35-e89c-4595-af5d-81df3f9245c9	e36d57bd-94eb-445d-892f-b6e9d14f795d	d1909a37-24c6-4751-8f40-138cf8870506	SD-TLPS-B3JZ	Supuni Sivakumar	0748242024	161 Peradeniya Road, Kandy	DELIVERED	730.00	LKR	order-9898bcc9-ca34-db97-5f2a-7d72d04e8be8	2026-10-29 06:49:00.002629+00	2026-10-30 08:00:08.610338+00
6465eb68-bb26-4f9d-86d8-f6a73942cac8	7aaaa942-2283-4848-9835-2ac9ea183ca9	a24cd3d8-b457-42b3-a832-23ac64e36d48	SD-E39J-BNZ6	Dilani Rajendran	0743784340	36 Prince of Wales Avenue, Colombo 14	DELIVERED	390.00	LKR	order-5ad58393-1885-c677-345e-775f07794e7b	2026-10-29 06:51:00.002687+00	2026-10-30 04:52:08.571459+00
ef2a16a7-5954-4daf-87d5-a84da9a67d0d	a5d2760a-c8c5-4540-bb34-6bee9391d166	a63cb0ff-ddbf-4f2b-8359-8d1edb3a9e51	SD-ZUN5-F3DJ	Mohamed Herath	0770828152	240 Parliament Road, Rajagiriya	DELIVERED	910.00	LKR	order-b7bd48a2-fccd-268e-8311-ddb81d2c4460	2026-10-29 11:16:00.00255+00	2026-10-30 04:46:08.50793+00
36c1cc2f-2a88-48ab-94b2-d583ec2462c0	c47e35ec-77fd-4d96-acd3-32dd2c35f9c3	c38fd52c-d6b0-4697-b452-1f6985500841	SD-E7G2-PV6U	Ruwan Jayasinghe	0742230345	258 Duplication Road, Colombo 03	FAILED	490.00	LKR	order-2cc2d58c-9356-6ef8-f097-832c3b144e8f	2026-10-30 09:02:00.002929+00	2026-10-31 08:33:08.419184+00
bbb4af1e-bcfa-4009-8f01-c9da474d7f90	7aaaa942-2283-4848-9835-2ac9ea183ca9	06e8fa20-d3e4-4c0e-80ff-d6279c8733f1	SD-7A37-M8N9	Sajith Jayasinghe	0709843157	278 De Saram Road, Mount Lavinia	DELIVERED	1170.00	LKR	order-406cd677-17e5-fbe4-92cd-d30092f845e9	2026-10-31 07:17:00.001847+00	2026-11-01 05:54:08.28058+00
430f556e-2d89-4e21-9135-13daa72256eb	a5d2760a-c8c5-4540-bb34-6bee9391d166	d7846298-e4ca-4d1c-8318-837b36328301	SD-F9CN-TX7Q	Thisaru Silva	0725061099	25 Dalada Veediya, Kandy	DELIVERED	650.00	LKR	order-f8aa0905-88b0-1293-d4c8-aecf19f4cb38	2026-11-01 03:44:00.002215+00	2026-11-02 03:31:08.249451+00
a6dbdf2b-3d69-4251-9ce6-fb56759ffaff	b696e59a-1df2-46bb-b4eb-7ed663dbb634	9a4b5e25-dfff-4edc-91b1-a05f2832a67b	SD-L9VQ-EGNH	Mohamed Abeysekera	0748714842	12 Nawala Road, Rajagiriya	DELIVERED	1140.00	LKR	order-63948cd2-b31e-a727-2bdb-a6f30e9165ee	2026-11-01 08:50:00.002853+00	2026-11-02 05:47:08.212387+00
96b2f093-ecda-4b80-ad83-5b25b86c6a3d	c47e35ec-77fd-4d96-acd3-32dd2c35f9c3	05b4344c-e074-4021-93fb-a371765831f9	SD-YTP2-FKXD	Malith Silva	0778512003	128 Galle Road, Dehiwala	DELIVERED	920.00	LKR	order-8b90ada9-9014-20ad-db14-5608e31430f8	2026-11-01 10:28:00.002646+00	2026-11-02 07:00:09.155314+00
712a755f-1f7d-4a80-ab3d-7f90cfd74283	048bd6b8-66d4-44c9-8d76-89568ca52320	f0a6bc09-e868-4dc3-8f17-b05d0390fa83	SD-WNRY-WYLP	Pradeep Silva	0764256402	13 Hill Street, Dehiwala	DELIVERED	780.00	LKR	order-d9fb0ec5-01a1-e20d-aaea-20e6044ff6a9	2026-11-01 12:16:00.002073+00	2026-11-02 07:11:09.113155+00
e8398471-5d0f-4a3e-9347-eb67c94f28fb	e36d57bd-94eb-445d-892f-b6e9d14f795d	c6e7b5e4-b0fe-4573-a0c0-d94898d2df8b	SD-J7EG-JHKG	Senuri Rajapaksa	0742654922	42 Galle Road, Colombo 04	DELIVERED	820.00	LKR	order-337cface-c901-313d-0daf-a0466914104b	2026-11-01 12:33:00.002254+00	2026-11-02 06:50:09.069196+00
65b0edab-827e-4518-98f1-09efb28cfe84	7aaaa942-2283-4848-9835-2ac9ea183ca9	e86a30fa-35b9-4a40-91b4-f7fa18266198	SD-MN22-4N2N	Yasiru Senanayake	0707266328	47 De Saram Road, Mount Lavinia	DELIVERED	400.00	LKR	order-dbd122ca-23f2-8e6f-dedd-8c6cd174cafe	2026-11-01 12:41:00.003033+00	2026-11-02 06:23:09.029483+00
2ca70cd6-eda5-4dbf-9b67-0d21fc800c42	a5d2760a-c8c5-4540-bb34-6bee9391d166	566b897b-ca92-4026-8756-e20a065dbacf	SD-MDPW-G9MX	Kamal Dissanayake	0784349135	288 Lewis Place, Negombo	DELIVERED	770.00	LKR	order-a6ad77a6-8b37-894d-15af-a68f5ceda489	2026-11-02 12:27:00.002602+00	2026-11-03 04:06:09.000552+00
0aa337e0-7b91-48a9-b32f-015ed371fc17	b696e59a-1df2-46bb-b4eb-7ed663dbb634	5eebf527-d6b1-425f-a998-09a21d88a3ea	SD-7494-2297	Kamal Liyanage	0765935827	62 Main Street, Colombo 11	DELIVERED	810.00	LKR	order-7368c959-3129-f3ef-7bd4-80c4dafbc550	2026-11-03 03:16:00.002531+00	2026-11-04 04:19:09.980142+00
820b6401-ddbe-4e3b-9f41-14e8423d516a	c47e35ec-77fd-4d96-acd3-32dd2c35f9c3	6a0ffa21-c149-4a91-b4c2-187a44e4fb45	SD-8QEY-Z5PM	Tharindu Amarasekara	0775851101	22 Colombo Road, Negombo	DELIVERED	990.00	LKR	order-e32b4186-9ef6-4c83-f269-9b41518165f4	2026-11-05 03:20:00.002321+00	2026-11-06 05:03:09.943118+00
d85d1d64-838c-49f5-9fc8-e5a31e58031c	048bd6b8-66d4-44c9-8d76-89568ca52320	79601ad6-c87f-4c10-afee-dbb79f31c8d2	SD-DUG4-56K8	Dinuka Dissanayake	0714191495	177 De Saram Road, Mount Lavinia	DELIVERED	520.00	LKR	order-5240a11f-abcd-d9f4-bcd5-f12562b61c59	2026-11-07 10:23:00.002545+00	2026-11-08 08:20:09.91174+00
f6cf0a63-341b-421c-89e4-049dc9c76868	e36d57bd-94eb-445d-892f-b6e9d14f795d	d7ad2fd9-f1d2-42eb-9238-dca139cf4877	SD-9E4S-SFBA	Arun Liyanage	0727639426	214 Pita Kotte Road, Kotte	DELIVERED	350.00	LKR	order-c0973cdc-0f96-124b-65cb-c6e459e54feb	2026-11-07 11:03:00.002313+00	2026-11-08 03:57:09.864638+00
f7bdeaa8-3040-4ad6-ac61-91c0c50647c2	7aaaa942-2283-4848-9835-2ac9ea183ca9	c0e8575b-3d0f-4c81-b1ac-b357a77c8d40	SD-KXHS-6SRX	Kasun Amarasekara	0782988159	63 Ramakrishna Road, Colombo 06	DELIVERED	1190.00	LKR	order-4d124abe-8baa-cc16-67f4-f121cbb7f4fe	2026-11-07 11:24:00.002757+00	2026-11-08 04:51:09.826579+00
511a3343-eadc-4684-bde9-8ac42da86a62	a5d2760a-c8c5-4540-bb34-6bee9391d166	108efbb7-3487-44d6-8cd0-d042ae95821d	SD-LLPZ-SLCZ	Kavindi Rajendran	0726148765	71 High Level Road, Maharagama	DELIVERED	810.00	LKR	order-58ea1743-c08e-ae07-b891-a392f005368c	2026-11-08 06:30:00.00275+00	2026-11-09 06:38:09.796632+00
1bfb1d29-8f59-4590-a6b0-cb51df0faafe	b696e59a-1df2-46bb-b4eb-7ed663dbb634	411ce78f-b85b-4c67-8062-7642ed48d8a3	SD-6YEU-YHDW	Kumar Dissanayake	0749245321	295 Havelock Road, Colombo 05	DELIVERED	1660.00	LKR	order-49d138ad-f834-a890-d895-2d09aa9ca7c8	2026-11-09 02:35:00.002235+00	2026-11-10 06:51:09.750604+00
0453ea7a-8005-4805-92e9-5ee81a62539a	c47e35ec-77fd-4d96-acd3-32dd2c35f9c3	1c69adda-384d-4440-bbff-c29d5e839c24	SD-WDLM-SWK7	Senuri Silva	0710828238	176 Old Kesbewa Road, Nugegoda	DELIVERED	920.00	LKR	order-a8ed2e83-3fc2-f16d-a2d4-d33a490b9e54	2026-11-09 03:22:00.002535+00	2026-11-09 11:38:09.706728+00
24144596-500c-43e0-b7b7-efb0081e18be	048bd6b8-66d4-44c9-8d76-89568ca52320	e9160c13-1fa8-433a-b0f2-2531eed818f7	SD-PNMV-72MT	Sahan Bandara	0748549742	256 High Level Road, Maharagama	DELIVERED	620.00	LKR	order-907e70d7-18d9-e3f7-63ff-3d2b1a853c76	2026-11-09 05:16:00.002415+00	2026-11-10 07:20:09.673652+00
2c966b4b-dba0-4e15-8c47-96ca75377bcf	e36d57bd-94eb-445d-892f-b6e9d14f795d	30bcf46b-f722-41ba-8028-952a94dd7637	SD-EEYS-7D48	Sahan Silva	0740623452	303 Flower Road, Colombo 07	DELIVERED	2320.00	LKR	order-849b6be9-a155-219e-7fe2-98a03fa5f3b8	2026-11-09 06:29:00.002993+00	2026-11-10 06:01:10.649686+00
275f6afd-6c96-49eb-82f9-ed8d12de63ee	7aaaa942-2283-4848-9835-2ac9ea183ca9	8886a196-2f7d-453b-bee2-8646a67323ce	SD-F69D-MG2Z	Malith Dissanayake	0728322488	85 High Level Road, Maharagama	DELIVERED	580.00	LKR	order-64cbc718-5d3d-0cd7-f9c1-691af2dc310b	2026-11-09 06:36:00.003076+00	2026-11-10 07:05:10.604698+00
58cacf98-67de-4767-8b9d-7b3418e831b4	a5d2760a-c8c5-4540-bb34-6bee9391d166	c8bbbf0b-28b5-4d39-84f2-0cab9a297379	SD-JLGL-8WNF	Dimuth Jayawardena	0788076243	193 Main Street, Colombo 11	DELIVERED	870.00	LKR	order-1c3a7051-0362-337c-35e0-ee5b3218eb85	2026-11-09 11:09:00.004292+00	2026-11-10 06:46:10.574374+00
8628c16e-794e-46d0-876d-b299a4966bd7	b696e59a-1df2-46bb-b4eb-7ed663dbb634	24c56c10-80a3-4dbc-9ff2-65d251ec1bfa	SD-UG7G-W4N9	Ayesha Cooray	0774193038	128 Colombo Road, Negombo	DELIVERED	1090.00	LKR	order-b7d7ba8f-398d-6481-3281-c301e4e9f4c5	2026-11-10 03:19:00.002115+00	2026-11-10 13:06:10.530146+00
e8bf79e7-74cd-4af9-b081-6c9e1b418c19	c47e35ec-77fd-4d96-acd3-32dd2c35f9c3	832f0466-ef0f-4885-b959-228ac6fe0a2e	SD-N2PQ-8Q5K	Anjali Bandara	0702339414	27 Old Kesbewa Road, Nugegoda	FAILED	520.00	LKR	order-8fcce0f9-98e2-7fa9-a373-ec1b502f7ad1	2026-11-10 09:44:00.001991+00	2026-11-11 05:17:10.495997+00
dc2400be-c151-4409-8c40-dc85b6cc2629	048bd6b8-66d4-44c9-8d76-89568ca52320	12a107c5-9ef1-40d4-bb55-c104c8037b0b	SD-RTZM-4RRL	Chamari Rajapaksa	0709264143	300 York Street, Colombo 01	DELIVERED	420.00	LKR	order-07f33066-556f-a366-b80c-8ca4cc30565f	2026-11-10 10:20:00.002947+00	2026-11-11 08:45:10.456464+00
7e0eef90-cdd4-4b5c-a319-058abe87edf6	e36d57bd-94eb-445d-892f-b6e9d14f795d	a461794b-a39d-4397-af2e-7e5e58e52976	SD-QLA9-N578	Sahan Senanayake	0785145987	26 Station Road, Nugegoda	DELIVERED	1310.00	LKR	order-20eef4c6-03bf-e426-a9c8-50b8b1dc6c00	2026-11-10 10:45:00.003186+00	2026-11-11 07:34:10.413621+00
5d1b939f-5e0d-4c81-9c3e-f49b85f0dd57	7aaaa942-2283-4848-9835-2ac9ea183ca9	a625183e-ee80-4261-bf29-f7013f3f1a80	SD-KQFQ-2PU3	Pradeep Abeysekera	0764679215	43 Dalada Veediya, Kandy	DELIVERED	870.00	LKR	order-47a9e222-0725-ff12-7d9b-64d60330c275	2026-11-10 11:35:00.002941+00	2026-11-11 05:19:10.367392+00
ee2e12d2-9309-40fa-9fa3-8984bbe3eb1a	a5d2760a-c8c5-4540-bb34-6bee9391d166	7dc8dc48-7f34-4a6a-900c-af00461bd037	SD-P3HR-6NT8	Rizwan Karunaratne	0746177174	50 Havelock Road, Colombo 05	DELIVERED	1030.00	LKR	order-4e6e3296-2c88-7998-758b-5ec66d00d5ad	2026-11-11 02:39:00.002613+00	2026-11-12 08:25:10.328505+00
f6c45473-a11f-46bd-abb6-2a5bebd01ad2	b696e59a-1df2-46bb-b4eb-7ed663dbb634	e8ec1d31-46d2-4727-baad-3f8abdda52b5	SD-EUR5-B8ZQ	Chamari Sivakumar	0722170891	160 De Saram Road, Mount Lavinia	DELIVERED	810.00	LKR	order-d3549a52-05b7-92a8-fcca-a182fece40fd	2026-11-11 04:59:00.003114+00	2026-11-12 06:20:10.290339+00
c6531b20-d5f3-493f-8047-cb4e5e33712d	c47e35ec-77fd-4d96-acd3-32dd2c35f9c3	bb7b183e-59e6-4dff-961b-9ba22cfbf8ef	SD-4X7S-MCJY	Nipuni Wickramasinghe	0771682242	178 Galle Road, Colombo 03	DELIVERED	350.00	LKR	order-4aea20cd-9604-7c89-802d-13e4bdeee796	2026-11-11 11:32:00.002058+00	2026-11-12 07:39:10.242173+00
376c0862-ac03-4631-aca5-4b9feb109265	048bd6b8-66d4-44c9-8d76-89568ca52320	2ac4bc9b-c256-4624-94cb-586287dab302	SD-JRTA-DN6Z	Sanduni Rajendran	0763450437	224 Havelock Road, Colombo 05	FAILED	570.00	LKR	order-c3fcfc4b-1654-3eb6-3dfe-bb841bb9b777	2026-11-12 02:34:00.005598+00	2026-11-13 05:39:10.201484+00
097634dd-a976-4c89-8493-1d2f304ab924	e36d57bd-94eb-445d-892f-b6e9d14f795d	fa0a73d0-9bf6-4ce2-aae4-6303ce7aa3eb	SD-M7ZS-4246	Sachini Rathnayake	0763239439	275 Galle Road, Dehiwala	DELIVERED	530.00	LKR	order-975d6c86-60bf-3873-9c95-61e3e0b71865	2026-11-12 08:43:00.00287+00	2026-11-13 07:38:10.152993+00
c2fc12a0-47e6-430e-8076-2c4024f96b90	7aaaa942-2283-4848-9835-2ac9ea183ca9	62d179e6-7752-4058-a412-5f8849d3e08d	SD-J58S-AEJC	Dinuka Senanayake	0772156162	223 William Gopallawa Mawatha, Kandy	FAILED	650.00	LKR	order-9329f57b-da7a-15d2-747a-efc4ee3b2b10	2026-11-12 09:50:00.002982+00	2026-11-13 07:01:10.115299+00
fd3a28cd-b06d-4762-a64f-3b058337c1ff	a5d2760a-c8c5-4540-bb34-6bee9391d166	d07d7a58-1f08-4821-a51d-1896473f5f03	SD-GNHA-23AQ	Priya Rathnayake	0781422584	109 High Level Road, Maharagama	DELIVERED	1500.00	LKR	order-540faaf6-e4c0-90a4-4d9f-cb4a06e4cb0d	2026-11-13 06:10:00.002824+00	2026-11-14 05:05:10.079861+00
ab3ae1eb-e06b-4693-b5b6-1087153337c9	b696e59a-1df2-46bb-b4eb-7ed663dbb634	389a7263-491d-40d0-80f7-d4a12e442e39	SD-268W-7UF3	Nadeesha Cooray	0785293326	252 Lewis Place, Negombo	DELIVERED	690.00	LKR	order-0086d2c5-4a56-3622-14a5-14da0929e180	2026-11-13 08:29:00.002314+00	2026-11-14 06:50:10.028976+00
c62b5757-b11c-49a1-ae50-2096214e776f	c47e35ec-77fd-4d96-acd3-32dd2c35f9c3	c9a261ef-bd5c-48e6-80b3-d8cd74cf7ef3	SD-8GPU-UK9A	Lahiru Perera	0704128436	314 Galle Road, Colombo 04	IN_TRANSIT	690.00	LKR	order-2ecd3a68-2930-bb26-60fa-a433053a6e39	2026-11-14 02:38:00.002535+00	2026-11-14 05:25:07.957725+00
b0c17160-edb4-4d75-b7fb-18b7406b7d24	a5d2760a-c8c5-4540-bb34-6bee9391d166	e4548b77-2a75-4044-869d-e9fe3281caef	SD-M9JH-E5EA	Kumar Gunasekara	0700482031	208 Parliament Road, Rajagiriya	IN_TRANSIT	1460.00	LKR	order-eb05ca26-758b-08e8-17ac-73560bae2674	2026-11-14 07:09:00.002843+00	2026-11-14 12:05:08.874441+00
1a74ae51-093f-4308-90cd-c4dae5ef74bc	b696e59a-1df2-46bb-b4eb-7ed663dbb634	200664de-108e-4bf6-986c-0e2d760b7aea	SD-9PYR-PG8C	Chathura Herath	0708654467	182 Stanley Thilakaratne Mawatha, Nugegoda	IN_TRANSIT	870.00	LKR	order-b3dc0260-c8e2-b2f5-9238-79b4080772c8	2026-11-14 08:09:00.003278+00	2026-11-14 13:55:08.853811+00
a13b5107-7b4b-4438-81a2-370d9cd8c246	c47e35ec-77fd-4d96-acd3-32dd2c35f9c3	fc35546b-ea0b-4d24-b268-2873dc0b87d8	SD-YQYU-BVZ4	Sanduni Karunaratne	0717517692	321 Pita Kotte Road, Kotte	CREATED	440.00	LKR	order-f17381e9-f543-db2b-8e6c-51aacd80aca9	2026-11-14 12:17:00.003063+00	\N
\.


--
-- Data for Name: merchant_webhook_endpoints; Type: TABLE DATA; Schema: public; Owner: merchant
--

COPY public.merchant_webhook_endpoints (id, merchant_id, webhook_url, secret, is_active, created_at, updated_at) FROM stdin;
6c0e6f04-2104-4b80-bdc9-7cae3d10a53c	b696e59a-1df2-46bb-b4eb-7ed663dbb634	https://kandy-spice-house.example.com/swiftdrop/webhook	whsec_8FO52LJVE0epSxtv32iz5opTWm5R1UWXL2eXtPFyVpU	t	2026-09-05 16:35:00+00	\N
3805bfa6-c871-4e8b-b2a9-7384fb3bc542	c47e35ec-77fd-4d96-acd3-32dd2c35f9c3	https://colombo-threads.example.com/swiftdrop/webhook	whsec_3uiAqDhZZ6FoSp9JTJxR--RUTZ2EWfoZbsVtMQeuq8E	t	2026-09-20 16:13:00+00	\N
d93bfcea-e3e1-4211-99e9-ec913b8499ea	048bd6b8-66d4-44c9-8d76-89568ca52320	https://techlanka-gadgets.example.com/swiftdrop/webhook	whsec_xICLCglANxOrJZqJQyb-yKl8liqZ2KOQajz47NnB8Xc	t	2026-09-15 16:35:00+00	\N
43494378-6a7a-4283-85e7-f842cf0d937a	e36d57bd-94eb-445d-892f-b6e9d14f795d	https://ceylon-tea-corner.example.com/swiftdrop/webhook	whsec_vlMhuQFrp6sCuEmKPkLAa9ep3Vl8KcisLBGX76aezkQ	t	2026-09-03 20:57:00+00	\N
7e017dae-c4d9-43d2-9334-add785754209	7aaaa942-2283-4848-9835-2ac9ea183ca9	https://booknook-lanka.example.com/swiftdrop/webhook	whsec_nArMaU03P0AnXHAc1N8s8fQ6AT_mks-QXJjnH718eK4	t	2026-09-10 15:31:00+00	\N
7a45cd25-dd0a-4145-9e71-a3e2474af0ff	a5d2760a-c8c5-4540-bb34-6bee9391d166	https://galle-fort-crafts.example.com/swiftdrop/webhook	whsec_-I1vZTly1ueJHXKBug9b_35lzeVSmi0EQX_hc9Do5dI	t	2026-09-20 22:24:00+00	\N
\.


--
-- Data for Name: merchants; Type: TABLE DATA; Schema: public; Owner: merchant
--

COPY public.merchants (id, user_id, business_name, business_reg_no, contact_name, email, phone, address, status, rejection_reason, created_at, updated_at) FROM stdin;
b696e59a-1df2-46bb-b4eb-7ed663dbb634	c50a9935-b0a4-42a7-8b67-834c582c0b41	Kandy Spice House	PV252555	Chamari Peiris	chamari.peiris@merchant.example.com	0782401774	51 Peradeniya Road, Kandy	APPROVED	\N	2026-09-04 07:35:00+00	2026-09-05 14:35:00+00
c47e35ec-77fd-4d96-acd3-32dd2c35f9c3	71ac489c-40e8-4057-bd8d-22645457f877	Colombo Threads	PV389562	Sajith Karunaratne	sajith.karunaratne@merchant.example.com	0774891635	105 Maradana Road, Colombo 10	APPROVED	\N	2026-09-19 17:13:00+00	2026-09-20 14:13:00+00
048bd6b8-66d4-44c9-8d76-89568ca52320	ac219f99-85fb-43a8-90b2-786b06a4d80b	TechLanka Gadgets	PV932564	Lahiru Senanayake	lahiru.senanayake@merchant.example.com	0770480984	180 Kollupitiya Road, Colombo 03	APPROVED	\N	2026-09-14 12:35:00+00	2026-09-15 14:35:00+00
e36d57bd-94eb-445d-892f-b6e9d14f795d	c8b97194-64f2-4ced-bc12-b5c4b7d4d9ee	Ceylon Tea Corner	PV702007	Lahiru Wijesinghe	lahiru.wijesinghe@merchant.example.com	0782262943	225 High Level Road, Maharagama	APPROVED	\N	2026-09-02 08:57:00+00	2026-09-03 18:57:00+00
7aaaa942-2283-4848-9835-2ac9ea183ca9	9b13b98c-873f-4749-958b-2f913cc7e712	BookNook Lanka	PV575560	Chathura Perera	chathura.perera@merchant.example.com	0770943404	92 Havelock Road, Colombo 05	APPROVED	\N	2026-09-09 12:31:00+00	2026-09-10 13:31:00+00
a5d2760a-c8c5-4540-bb34-6bee9391d166	fff7e75b-4ded-4f40-8614-627687e7ec32	Galle Fort Crafts	PV473435	Pradeep Wickramasinghe	pradeep.wickramasinghe@merchant.example.com	0760290556	229 Church Street, Galle Fort	APPROVED	\N	2026-09-19 09:24:00+00	2026-09-20 20:24:00+00
572e901c-9a6b-4ce7-8a56-09e6104db169	533bff2a-8e8b-4b3e-91f3-9a06ff07c37a	Negombo Fresh Fish	PV550730	Pradeep Rajapaksa	pradeep.rajapaksa@merchant.example.com	0726996524	231 Colombo Road, Negombo	PENDING	\N	2026-08-29 12:12:00+00	\N
3b8b27ee-8acf-4391-96f2-4f3d14859a7a	73e39455-d811-45cf-a238-602646b5422b	QuickFix Mobile Parts	PV632320	Oshada Weerasinghe	oshada.weerasinghe@merchant.example.com	0787692766	5 Parliament Road, Rajagiriya	REJECTED	Business registration certificate could not be verified	2026-09-02 09:02:00+00	2026-09-02 18:02:00+00
\.


--
-- Data for Name: processed_events; Type: TABLE DATA; Schema: public; Owner: merchant
--

COPY public.processed_events (id, event_id, consumer_name, processed_at) FROM stdin;
3367140b-4b49-468f-8653-a0c8e600503e	8cd28de5-f0d2-435e-bf45-a4ebe5b163ad	merchant-orders	2026-11-14 14:12:25.634697+00
f5687109-e6be-4ef6-9247-69a971244482	f6c1c551-88f5-4db4-8b0f-8f9ab3aabe9e	merchant-orders	2026-11-14 14:12:25.640792+00
fa03eb9d-7ffd-4b17-af90-ce9685c5c4dd	e78040d6-e489-4513-bcf6-6e84751fe718	merchant-orders	2026-11-14 14:12:25.644491+00
f73240b1-3571-409e-9e49-4342762bff81	1c30a05c-e7ae-4826-a82d-7affcbcc51f7	merchant-orders	2026-11-14 14:12:25.648496+00
4a4d56d0-e8be-41f6-af15-efa1b2d28154	74e599f0-d7bb-4de5-88e7-90ba56211a3d	merchant-orders	2026-11-14 14:12:25.652473+00
ce6afdbd-1144-4a75-bd33-f47830f52d9f	e14ebae7-351d-467c-a875-5bc878ee3452	merchant-orders	2026-11-14 14:12:25.655915+00
261505c8-2276-4197-a7bd-10106c3a9a5a	91f5ffd6-2700-4545-b640-b39e04cd3398	merchant-orders	2026-11-14 14:12:25.659038+00
41704d26-bad0-406f-b61c-da9c9c4a51ac	faed8f2a-c7ec-4888-b60e-955972f94c5b	merchant-orders	2026-11-14 14:12:25.662203+00
95d6df4a-a204-442a-a13d-c5e2c7e8197c	bda622ad-fecc-468d-a747-4429ea2ccaac	merchant-orders	2026-11-14 14:12:25.665323+00
e8b161ac-9164-4cd4-bcc8-40c00210d88d	1d18b320-3ea9-4118-b567-648ff4387fe9	merchant-orders	2026-11-14 14:12:25.668082+00
694fcc08-7305-4c59-9c21-d3c132edf1d3	1dd6e4df-eb3a-46ac-993d-1c256c4e7cf2	merchant-orders	2026-11-14 14:12:25.671062+00
815f0632-fb70-4c25-a2c4-b3054bd123da	dc7baef9-bacb-4253-8653-553ad643c6b3	merchant-orders	2026-11-14 14:12:25.673553+00
15a63957-878b-452b-9d9d-6e9d9a06bb55	f2ac2a4d-e33e-44e4-80ee-0b6a9a258b1e	merchant-orders	2026-11-14 14:12:25.676091+00
d642ee5e-3fbc-4f4c-859c-e79e99adb7e4	62f1f76a-1a7a-4b96-b06c-cd6fd72dbc49	merchant-orders	2026-11-14 14:12:25.678929+00
9a7a9153-3c10-40bc-92e1-4486276552e4	6bfe094c-a05f-4155-855b-d051d070a5c4	merchant-orders	2026-11-14 14:12:25.681454+00
e9b29fee-017a-4fae-9be7-23518d301de7	bc07419e-1328-4661-9aec-8cc5c0eb2414	merchant-orders	2026-11-14 14:12:25.683958+00
00836a8f-4db6-4e2e-8de9-30a021622020	c25a0ba1-27cf-4269-9b55-be57b46731de	merchant-orders	2026-11-14 14:12:25.686352+00
cde13f23-0e94-411d-9a2b-d5b5d8728f16	1d06199f-537c-4e3e-9aad-04afa8621f71	merchant-orders	2026-11-14 14:12:25.689532+00
8e8169ac-927f-45ab-ab60-f0b9521f6f3a	05a9bac9-eeb2-4b04-98d4-2a7c2ca83245	merchant-orders	2026-11-14 14:12:25.692673+00
0ee95efd-eee9-45df-b921-e2242c795e18	140a9fd2-2234-422a-995c-ad11cf30de59	merchant-orders	2026-11-14 14:12:25.695448+00
77f444ff-ee32-424b-b00b-9d10b3849805	85acfb87-5368-4569-a638-b14499059675	merchant-orders	2026-11-14 14:12:26.667976+00
348736a7-6a42-4d04-b844-54269259c5cb	60d296aa-d8d8-4d95-bcb0-ed6b70c7ed3b	merchant-orders	2026-11-14 14:12:26.672785+00
abb8e5c6-37cf-4b50-abdf-375742df21ee	21f9a135-2f16-40f2-9971-eef819bb6fa2	merchant-orders	2026-11-14 14:12:26.678897+00
27e89ebf-7c33-4e51-ac47-cda7fb834861	9a2316f9-87fe-4c0f-9085-2a3153dcfd8a	merchant-orders	2026-11-14 14:12:26.684991+00
50d8b8dd-a030-47d3-90de-f55c1baf5003	fbbbe604-6add-44c9-9aba-7e8321f9e1a5	merchant-orders	2026-11-14 14:12:26.690056+00
a81a9b40-ea5f-4203-86fc-49ad877c9480	260c29bc-c1ad-4631-a9d0-6b1137e1fa79	merchant-orders	2026-11-14 14:12:26.693189+00
da5372f9-75d8-44de-b8b6-810f9fed6fdd	e2f545e2-030c-471e-ab31-1179aaa3f039	merchant-orders	2026-11-14 14:12:26.697555+00
5bebd140-105f-4668-af03-d1f56e3724bc	30493137-5e39-4c4e-b990-1c782e22c25d	merchant-orders	2026-11-14 14:12:26.700866+00
f55943b0-3068-46c5-8167-7e86fe1babfe	7632ac12-36dc-4517-aa6a-721d7a5151b2	merchant-orders	2026-11-14 14:12:26.703736+00
dde83bb7-82cc-42fc-a3c1-6cca03978d15	3aad228c-6d28-4530-9243-d3c0d2441ac1	merchant-orders	2026-11-14 14:12:26.706487+00
89f0a512-4714-401c-b071-e75c72bdc372	04074b8d-f7a3-4605-b8f7-b6c807f1637e	merchant-orders	2026-11-14 14:12:26.709003+00
acfbe0c6-aea5-45aa-a5cf-0ca11792bf1b	b5b48ba1-75ac-4956-a5f3-a1b8da1c2fc4	merchant-orders	2026-11-14 14:12:26.711143+00
c877bbfa-70c0-4d62-af61-130fdca1d7f4	95cf2e5a-c191-49b7-b489-4de92ff03e46	merchant-orders	2026-11-14 14:12:26.71355+00
643e394b-5d27-4fa4-9538-88f8e64b1fd6	7e2aac32-4923-4248-be1f-27abb5e520d9	merchant-orders	2026-11-14 14:12:26.716355+00
118b582b-ba04-4ac4-a64c-0a5c49e7356f	89ce2d05-ae88-40b7-bff2-dc2aac43194a	merchant-orders	2026-11-14 14:12:26.719427+00
13fb18dd-b572-4888-b0c6-2c0ece56dac3	991205cf-8aa4-4b2c-9d4c-fc9265a3c1b9	merchant-orders	2026-11-14 14:12:26.723231+00
c030c13e-4034-4a09-9d68-e48993f017b4	89917597-aee7-4df7-b51a-f2245647ddf2	merchant-orders	2026-11-14 14:12:26.725769+00
6a5c91aa-77d5-435d-bbbd-482f46404523	1e610600-4ad9-40f4-9e34-61815a27f7c2	merchant-orders	2026-11-14 14:12:26.72829+00
f26230b6-6125-41a3-9a94-f82381792b11	eae28701-8a2d-4ec8-b837-ddd4294af16d	merchant-orders	2026-11-14 14:12:26.731208+00
068c1a62-ea90-459f-9c75-c8a622fd6d13	51605a05-2e36-4fe4-b391-f024fcaf03e6	merchant-orders	2026-11-14 14:12:26.733588+00
6c16c442-45c2-4794-af7d-409c5a18a8fc	644ab1d8-1baa-42f8-904e-bec635955b8f	merchant-orders	2026-11-14 14:12:26.735878+00
1a5417a6-c281-48cb-bd5e-d75059524a22	fa3f30e8-9699-43ae-be00-eb0c13b11d78	merchant-orders	2026-11-14 14:12:26.738501+00
f19ac013-8826-4941-92f1-457b5898cf36	e5fd90ed-e845-4320-9206-ced09e690131	merchant-orders	2026-11-14 14:12:26.741066+00
527934a6-08c2-4a59-8a7a-5474a843e110	e8208319-95de-406d-a8f5-8d51d7ae8a1c	merchant-orders	2026-11-14 14:12:26.743161+00
e5b03992-3be7-42e6-8995-ebd86484eea4	72f58eb6-1cb9-4125-a794-7891b93a4810	merchant-orders	2026-11-14 14:12:26.745463+00
de1eb3d4-f686-4379-be57-2d03699f5d00	ca2e5663-6ffe-48a2-ba29-70e76be763b8	merchant-orders	2026-11-14 14:12:26.747965+00
ac74097c-ac6a-4230-8fee-157c516d03a3	929d3305-4a90-40f2-8ba8-1876fcd544b9	merchant-orders	2026-11-14 14:12:26.750391+00
c48f3f8f-3bbf-4d69-be58-ee00a7d00d14	f6503e5d-ca82-4c06-b87d-b246c6cf62e7	merchant-orders	2026-11-14 14:12:26.752605+00
03a2a9f5-4e52-49be-bf09-b5cdab4876de	fd82c148-e293-4bf0-9152-381351f8b276	merchant-orders	2026-11-14 14:12:26.755025+00
0e86f4c7-c1bf-4fe6-b93d-dd2fe0374a65	946840f5-5192-439f-86ba-79fc340d6dbb	merchant-orders	2026-11-14 14:12:26.757203+00
5cbda554-cef2-4b36-998d-0c0e9704a3c1	f198d64a-9b43-43bc-ba82-dd058d51b624	merchant-orders	2026-11-14 14:12:26.759186+00
35799cb4-1593-4adf-87ec-0307d45e1de2	fa52c7ad-4eff-4a59-be70-bd1f76de7216	merchant-orders	2026-11-14 14:12:26.761138+00
797ed307-fef9-40af-bcec-d01ed2dad1c8	e622aa30-a381-4928-be21-9a38c4a7e7df	merchant-orders	2026-11-14 14:12:26.763194+00
8bd052f9-564c-4966-a673-a69edb947234	0d4cc2f3-2e8a-45f4-8393-a645bfdc4fa2	merchant-orders	2026-11-14 14:12:26.765128+00
04ac2234-7b37-4daa-a767-3d818085f782	8593b70a-f505-4d52-bdaa-a11921322820	merchant-orders	2026-11-14 14:12:26.767211+00
cddd8b39-5436-49d8-b512-b49f7f76d0ff	d25835e2-c75f-4e25-a7b6-8b3613a5faff	merchant-orders	2026-11-14 14:12:26.769171+00
238eac05-b8df-4a03-9843-36059af8b12f	059df520-ba46-4d34-a32d-2bd8cb44605a	merchant-orders	2026-11-14 14:12:26.771631+00
923c04a8-689f-47d0-839b-87556f35cb2b	8915960e-137d-48af-a42e-0fc3800ba69d	merchant-orders	2026-11-14 14:12:26.773886+00
f0ad8a49-4c44-4f26-a210-75bc334cb0e6	c93852cf-d128-4e8f-a5ea-a88b4019646a	merchant-orders	2026-11-14 14:12:26.775726+00
c4253365-b4e1-4c8b-b290-2f7a0c43aedf	9c349512-6b23-4bbf-98ad-ec4c9413bcc3	merchant-orders	2026-11-14 14:12:26.778018+00
1710acd3-9519-403d-bc73-1c531d244f6a	a46712df-ba03-483f-bb4a-7c91e42eff99	merchant-orders	2026-11-14 14:12:26.780299+00
53c20f2e-c025-411e-851f-4593b1f81a52	6b5734bc-f170-4b80-bf6f-4f7e5a2be724	merchant-orders	2026-11-14 14:12:26.798999+00
ac39b3fb-fc47-443e-9bb5-a474366593e6	27e66a6a-67c9-4445-b039-ba22337c2e6b	merchant-orders	2026-11-14 14:12:26.805347+00
cdd08bfa-3883-4d8d-8502-6342c6697349	3f29782e-c976-46d3-941a-80d3f040b101	merchant-orders	2026-11-14 14:12:26.809143+00
4adb1646-5062-40f8-8d14-411cb476fcc1	d2371686-6234-44b8-862b-1aaaa1ae4fda	merchant-orders	2026-11-14 14:12:26.812164+00
9b0ac748-cf29-4e76-a8f2-d6851c4fdfbd	f4958129-3886-418d-8657-6f8b825664a6	merchant-orders	2026-11-14 14:12:26.814822+00
c98dcf33-c282-44ef-8fcb-a7ecef0febee	f616f895-cf25-446d-bcdb-c0ae724895db	merchant-orders	2026-11-14 14:12:26.81803+00
27b64084-2d16-448d-9795-0ef2cfa50557	e536f70a-188e-41a5-b966-e4026d06891f	merchant-orders	2026-11-14 14:12:26.820655+00
d8f7f1be-40ce-4c14-bcf4-4f7f5c7f844e	4404046a-7f7a-45ba-b06d-d4b7ddc2fde1	merchant-orders	2026-11-14 14:12:26.823202+00
7073a98f-9f0a-4257-9394-a929bdc10ba2	f4e816db-10a8-42fe-aa00-cabb5d76d9af	merchant-orders	2026-11-14 14:12:26.82574+00
a7e88f25-826c-424c-9dc8-842d1796883b	8b4ccc66-99af-4970-beb1-c74bb35f2615	merchant-orders	2026-11-14 14:12:27.709585+00
3171d533-64ab-4b8b-a694-4aa1f6dcec89	fba36aea-296e-4e69-ba12-18caea369b00	merchant-orders	2026-11-14 14:12:27.7135+00
d8e75e15-a24f-4549-8a5a-46e9f7070ca8	521c43cd-bb5b-4609-9834-d493a10b0a06	merchant-orders	2026-11-14 14:12:27.717395+00
a3ab392d-4735-467c-9087-6f31f69cdd57	ce0df305-7fad-49c5-9de9-3d4e80cbd76a	merchant-orders	2026-11-14 14:12:27.720454+00
9518de3d-db7a-46ad-bc1a-724cbd72956a	777f46b6-e446-4e51-936b-17b2dd2209b3	merchant-orders	2026-11-14 14:12:27.723004+00
1af4ae2a-1199-41f7-8691-7cdad3c03f83	82ab4d52-afa3-40fc-bfef-cfe0a4ab6e70	merchant-orders	2026-11-14 14:12:27.724956+00
6ac3d0ba-cbe6-40d8-8f26-54f99ba113da	5f020e40-0f69-4fc4-8e00-9f06709f361e	merchant-orders	2026-11-14 14:12:27.727118+00
9fec20f5-0e01-4cb7-b439-cd0f966d6585	f8704076-c270-40ef-9c0a-6c20f5ea7e77	merchant-orders	2026-11-14 14:12:27.729014+00
7155ce45-0a0c-4f86-ade1-9f4f78bb4bf8	a5ec97a9-5872-4893-ae09-74f5df6992d5	merchant-orders	2026-11-14 14:12:27.730949+00
2d56d42b-b9a0-4cf5-b2bd-76ee0ac18cb6	5e1582d5-f07c-495a-b4e2-9ea078103b11	merchant-orders	2026-11-14 14:12:27.732585+00
0d8e60d5-778e-4ee1-bd79-8971bb43b63a	e08a4af0-c17b-412a-93f0-17e13242a89b	merchant-orders	2026-11-14 14:12:43.171511+00
f376d114-9be9-4213-9d22-931a4f82bb68	12ddda6b-4593-4fa7-9232-78cfaff8d76f	merchant-orders	2026-11-14 14:12:43.177255+00
afaf0a92-1f50-4abf-adfb-60ace220e858	920770d9-2a75-4005-a70d-15efd7ded023	merchant-orders	2026-11-14 14:12:43.180961+00
968a510c-a3b3-4d0d-9689-73d43f7b6383	650ff5c3-4ec2-44b6-97cc-5e8a746f103f	merchant-orders	2026-11-14 14:12:43.184552+00
2e952f59-7400-44d6-b5fa-c76fa39408a1	a820df52-efdc-470e-ba41-43e5dd64ff5a	merchant-orders	2026-11-14 14:12:43.18784+00
8adbb60c-5991-4b14-8b7c-b044980f7faf	245f33a0-0293-455b-bf48-2ae30aae0e9f	merchant-orders	2026-11-14 14:12:43.190845+00
0264656a-712b-483e-bdfe-d0f5028dd4b5	de3428e1-1fad-4512-8cfe-035fc36630bd	merchant-orders	2026-11-14 14:12:43.193613+00
49f3a4b9-e0d3-4b0d-8eee-e309ba8ac26c	e5fbe7ff-a193-4981-9153-297f66c9cec3	merchant-orders	2026-11-14 14:12:44.204923+00
fe20b719-b53e-4950-b9aa-22f9cd61dbac	0e05a525-4f86-4642-b944-1cf282f55394	merchant-orders	2026-11-14 14:12:44.208596+00
2f3db6c7-09ca-488e-a0b5-101d425a8307	ba4e6278-f701-469a-b0fa-39ff76e00ffd	merchant-orders	2026-11-14 14:12:44.212004+00
d0799708-a787-4a9e-8eae-a8e102fc62d0	f905945f-e649-4930-a847-8f1a81378569	merchant-orders	2026-11-14 14:12:44.214854+00
39a2055b-fc6f-4165-92cd-7ea3b132ff4b	cb85d36d-d579-4073-b67d-cb114d5885b6	merchant-orders	2026-11-14 14:12:44.217652+00
b80cf350-90a9-4b41-8ff8-61b77e737dc8	3f3d51bf-ac2a-42d8-b758-672223a1e911	merchant-orders	2026-11-14 14:12:44.220683+00
ebabd93a-04dd-4a39-884e-7592768c8aff	5469f72c-58c4-4dfa-bdfa-d49d757e3ee3	merchant-orders	2026-11-14 14:12:44.223326+00
4e7b78e7-c6c6-4f2d-96be-979179651ae8	71666c15-c9ce-4b86-a3a5-efcb6ce1bf50	merchant-orders	2026-11-14 14:12:44.226238+00
85adf732-19ff-446b-a86b-504fb02fa376	3c06491d-f1df-4130-af68-2b5068dbe1d2	merchant-orders	2026-11-14 14:12:44.22921+00
0260e0ec-fb93-4cc3-94be-ced811c43f8e	0e9dcc81-f910-466a-9331-70601ef5261b	merchant-orders	2026-11-14 14:12:44.231941+00
bcb1b760-8646-4bb8-a6f8-86fdc69ffc80	3f5f7e15-958a-4e08-a870-239534197305	merchant-orders	2026-11-14 14:12:44.234744+00
20383a65-bd70-49c8-8372-3c75d45cc15b	9e1fe786-cc02-4007-8462-4d67d3aa1306	merchant-orders	2026-11-14 14:12:44.237879+00
1672ceb1-252c-4606-9d5d-b3d26d949c85	7fdc47e9-e216-457c-9665-b4f8408477c6	merchant-orders	2026-11-14 14:12:44.240558+00
b84863a3-fb00-483f-97db-5f1aa70c7308	f36fe0f1-80d9-4678-9ffb-4cfd360476ba	merchant-orders	2026-11-14 14:12:44.243037+00
a35117b2-7913-4051-8313-d187d87b985a	bc427250-eaed-4c6b-b4a1-ef4714b6ed43	merchant-orders	2026-11-14 14:12:44.245281+00
e6637904-0e38-40fd-9c95-77f5b8174b70	604c3f1d-5050-460b-a363-a86452071a6a	merchant-orders	2026-11-14 14:12:44.24777+00
03186d67-71a8-43b9-bf87-3a6a27e1a976	1dffced4-95a5-4a34-bba2-003ca9907b61	merchant-orders	2026-11-14 14:12:44.250203+00
0a88e45a-c21f-4a6e-ba4d-4795f5473112	322197e9-b351-447e-aafb-0c6a0964c92a	merchant-orders	2026-11-14 14:12:44.252216+00
1098c5d3-d3e6-4f38-b2d0-c8752950d852	8d04350b-604c-4376-92f3-dad5b0db3aeb	merchant-orders	2026-11-14 14:12:44.254488+00
c02d9a26-e22b-442f-a8e8-db3b16e858f0	5454cc46-2e10-40ca-b1b1-791d4aced4f2	merchant-orders	2026-11-14 14:12:44.256959+00
24396a5c-14c2-42d1-a5ee-db32e27927bf	73878a46-8439-43a4-b386-2b1fb27df287	merchant-orders	2026-11-14 14:12:44.259049+00
955a9737-9e6c-47dc-a1e0-150a32ddd499	bf07b84c-d257-4998-901d-c1856ba05a68	merchant-orders	2026-11-14 14:12:44.261191+00
8419d09c-0781-4415-9f6c-ac9614637485	c2696416-fd78-4a8d-9e7d-bceef183b577	merchant-orders	2026-11-14 14:12:44.263519+00
58becc65-f2b9-4ee7-a57f-e3bc3a6b5ed5	55671d7b-abd8-4b43-a09b-e834633ae2b4	merchant-orders	2026-11-14 14:12:44.265539+00
248854e3-8eeb-4b23-862a-267f88aa3c93	1229cd8a-c2a9-42ac-8d4d-81fc7a33b9ef	merchant-orders	2026-11-14 14:12:44.267569+00
2be1d754-b42f-4413-acd3-3a5aacb9dd6d	1ccef7c7-cb81-4b40-af76-7f54f37d39da	merchant-orders	2026-11-14 14:12:45.251056+00
22a1bf0a-5eab-48a3-b709-04e7f6d9d3c7	b3a11d55-32c2-4e3c-9898-4d4c37c1f589	merchant-orders	2026-11-14 14:12:45.257288+00
dbd8e262-a064-4b1b-b2f3-a3da9f34bf93	d94d1efe-122e-45d8-af96-0f9ebd56d621	merchant-orders	2026-11-14 14:12:45.261715+00
924cd945-cb34-4095-a93a-948d4371387a	cb29cd5b-3096-4153-96c9-e86c09ac07cc	merchant-orders	2026-11-14 14:12:45.265983+00
b7703c15-aeb5-4885-b039-10622679ea67	07e741bd-b1a6-4026-93af-7b53b5051858	merchant-orders	2026-11-14 14:12:45.270436+00
f5817579-862b-4065-8d07-2033341240b7	20d8c824-c095-49c3-96aa-218de5c5c243	merchant-orders	2026-11-14 14:12:45.275297+00
38c02056-c3ba-4fa0-8252-8a5635f5bf54	08ec5f70-62e4-49a5-ad67-e437491ed3f6	merchant-orders	2026-11-14 14:12:45.27825+00
842d488d-ec13-481a-9a4f-007e53747c42	957fb7ba-09ce-4ccc-bfda-60591cc1642f	merchant-orders	2026-11-14 14:12:45.280985+00
321064f5-4f9b-4a97-a896-2faa003e1098	681bda5e-9954-4dde-832d-775a9e99260b	merchant-orders	2026-11-14 14:12:45.283315+00
aba1277c-e7aa-4263-b385-260c638d7bb6	23a268aa-76d7-446c-b660-39578e06ed72	merchant-orders	2026-11-14 14:12:45.285325+00
52a764cb-8921-4023-8273-e789b42c2e1c	6a1b8b27-5bbb-453d-a45f-4b2dc826d5f8	merchant-orders	2026-11-14 14:12:45.288076+00
14ab8f56-65a1-4980-bbdd-218b11704bcc	5425b1f9-286f-4fcf-8bf5-0cb2c3a5a1ab	merchant-orders	2026-11-14 14:12:45.290449+00
c2e12ff8-8e05-4880-a27c-1bb705d674a4	bf8d106d-73b5-4afc-9958-e4c2bb217791	merchant-orders	2026-11-14 14:12:45.29279+00
6227a105-3e1d-43eb-8148-32bfa66f402d	e355c8f4-721d-4a5f-920a-d19c5206bcb2	merchant-orders	2026-11-14 14:12:45.295389+00
19408428-ae47-4f89-a1dd-e26b336bafad	90e17138-6c50-4b6b-bf2f-003bdad10ef3	merchant-orders	2026-11-14 14:12:45.297654+00
550c9ab9-7770-43b6-b676-0aecd0706f5a	c5cb94dd-c14d-4e7a-8169-e5162f2c4ab0	merchant-orders	2026-11-14 14:12:45.300268+00
6c938ef9-68a6-48c6-8997-8409bfe8287f	da77d9ed-eb42-4e05-a582-0297990ded67	merchant-orders	2026-11-14 14:12:45.302575+00
02bc9a22-b9c2-4740-bf78-56e143af48be	0d26d010-cdc2-42d9-88ea-5352570d0b0f	merchant-orders	2026-11-14 14:12:45.30479+00
d1eae143-6055-403b-8a50-4a4d68e84698	f1f0b052-c140-4089-b3d6-c5bbfaf39429	merchant-orders	2026-11-14 14:12:45.306785+00
ab83d2dc-12a1-4916-be77-0e2567057335	d2cd0af9-d03f-46b5-8672-6c54a277c886	merchant-orders	2026-11-14 14:12:45.308713+00
b53a6223-34c4-4045-b075-130fc12fad13	9dbfeb92-4956-4325-9b41-961182be44c1	merchant-orders	2026-11-14 14:12:45.310683+00
29f9ecd9-42d7-44c0-9a88-4d50eb0cea0e	f025a034-721d-4ed4-a421-14a8f923fa01	merchant-orders	2026-11-14 14:12:45.312771+00
4f92b209-aa55-4f4e-b9bb-b52e22f1a4aa	ba7a75ae-c9f8-4db3-9c4a-37ae8b203b9b	merchant-orders	2026-11-14 14:12:45.314688+00
266a0feb-5b60-4cf4-9c08-bce7dbce4650	3fef999a-a326-44ca-ba37-d75c772a9c1d	merchant-orders	2026-11-14 14:12:45.316743+00
34eb2901-4be0-48c8-97b2-d0fcf77ffe28	cdaab7bb-3161-489b-b330-afe53d08b78d	merchant-orders	2026-11-14 14:12:45.318844+00
d8e24ae7-8a6d-410a-acd4-cf58ad5dc780	09c2de86-4ae1-40fe-8611-d53c1883410c	merchant-orders	2026-11-14 14:12:45.320525+00
23378b86-af23-4320-a093-62325dce7cfe	471d7a28-a23e-4318-8bd4-63f0e0890c48	merchant-orders	2026-11-14 14:12:45.322691+00
d2ef69b5-f20f-4d41-96cc-c9331af3d993	a767a93a-7382-4c1f-9381-0ed2029de96b	merchant-orders	2026-11-14 14:12:45.324633+00
015cef97-8bed-415f-b42d-ebaa19f03bfd	2f568021-592b-4834-96a2-27ef08481d29	merchant-orders	2026-11-14 14:12:45.326868+00
6cd2c72d-86c5-40dc-a94d-418eb0c434ee	fad179b4-cb90-4a9e-b712-43dc1850a48f	merchant-orders	2026-11-14 14:12:45.32874+00
87da6fe5-0262-44f1-9775-5f44147b205c	6b4be964-75ae-4e2b-9af2-07618072362d	merchant-orders	2026-11-14 14:12:45.330687+00
a3751f8d-f1f6-499a-9eed-881f301327a0	13013cd8-f54a-46ea-b237-d2ce81ff2401	merchant-orders	2026-11-14 14:12:45.332389+00
0e2bc30b-1f40-42ca-9f89-d3dc76f3127b	a7187b94-b2cf-40b6-9ea3-f3b00e7e23df	merchant-orders	2026-11-14 14:12:45.334316+00
f7c0ad3f-5fac-4cf1-a52b-554de7fc31a5	ed5a1be6-e85e-4748-aa49-adb798b195b7	merchant-orders	2026-11-14 14:12:45.336077+00
011d8d65-cd86-4cc0-8677-882fa18a1f5c	463ceeeb-42de-48fa-9de3-b4823d563ff3	merchant-orders	2026-11-14 14:12:45.338205+00
da8a8b43-8d39-4ec5-aad7-22fe08b62c89	e02a46ea-ee7a-4c81-8d2e-094ea75133be	merchant-orders	2026-11-14 14:12:45.340272+00
4d1173a7-f967-465b-b0c0-79718590c2fa	f5d51135-ca6a-4c9e-a6f3-cf1063366a8d	merchant-orders	2026-11-14 14:12:45.341996+00
3b315a18-06de-433a-bea4-a3f432b86126	3dfc9645-4676-47b8-8d44-553be1ae82f6	merchant-orders	2026-11-14 14:12:45.344171+00
0b8f286c-2034-4c94-ba76-deb9bfe57ff5	6a1693f6-7d2f-4bbc-a278-305fc0a1b53a	merchant-orders	2026-11-14 14:12:45.346703+00
3039a7bb-0e32-4ab1-b844-bd8083264399	d3dc291e-a1f3-4637-b287-06b179f2e2fd	merchant-orders	2026-11-14 14:12:45.348803+00
33473a3e-46a7-4da8-818a-1089a963e3b7	cb13380b-72bc-4f36-8630-895922468d13	merchant-orders	2026-11-14 14:12:45.350717+00
c1d8a242-5369-4da1-9212-02eb40afdecb	2ff7acf1-06b2-492d-94a6-c50d4dab6435	merchant-orders	2026-11-14 14:12:45.352749+00
b67bb1d4-2e34-4f22-8735-0ff82f7bbda5	5e3ec97e-d148-4760-9a5e-8f1ae670bc04	merchant-orders	2026-11-14 14:12:45.35462+00
4fde12eb-89fb-4990-995a-beb1538559d2	239f0b23-06df-4323-a2eb-40d49ada6178	merchant-orders	2026-11-14 14:12:46.287632+00
7cb5c4c2-f5f9-4b1a-80d7-d86947e04d0b	a78d287d-322d-4981-99dc-6170de261ca7	merchant-orders	2026-11-14 14:12:46.292315+00
f1c10bc3-0f1f-47b5-a691-192b31f5e759	624e3f42-c854-4982-9270-8e4b5c920056	merchant-orders	2026-11-14 14:12:46.29615+00
cdbd45f2-1533-48c4-b184-c564e7e482cb	aca36ed1-da50-4903-9629-122c33625b40	merchant-orders	2026-11-14 14:12:46.299165+00
501b353c-6c7d-4fb4-bc27-8eeb1c1436e0	e998983b-0f63-4ec1-8dc7-4e4818136b86	merchant-orders	2026-11-14 14:12:46.302459+00
440f9edb-6040-4bed-a2de-23525c017b4d	7889590e-ce9c-4e4e-978a-c921fba3bf00	merchant-orders	2026-11-14 14:12:46.306378+00
be6299b0-f744-400d-a738-cc0cb9ab1d6d	2bb98af2-93da-4a42-a0d1-4d534e3c69c1	merchant-orders	2026-11-14 14:12:46.311253+00
052c647e-90a1-480c-bf85-7997baba6d0e	77d9a9be-d042-4612-ab95-b09f981365c0	merchant-orders	2026-11-14 14:12:46.314093+00
66c57e51-b752-4b82-b3d8-4b4fbd4abc87	5a2a6a0b-93cf-406a-983b-be063f360a71	merchant-orders	2026-11-14 14:12:46.316576+00
0fa2a588-5530-4547-80ba-8a50e76d3580	6748a385-02ed-43d2-83ec-3ba57182b8a2	merchant-orders	2026-11-14 14:12:46.319179+00
3fcc33b4-d6c3-4cd4-9a94-f391561a15e0	76566281-f94f-43c1-93f5-9961062bab79	merchant-orders	2026-11-14 14:12:46.322326+00
e0ebc939-80ec-4a1a-ae37-90c39080eccd	8d9d0bb0-010a-42e1-a96e-575db5756b47	merchant-orders	2026-11-14 14:12:46.325015+00
254ea1fb-4cea-44c6-badb-b49da7b34820	22164548-f772-424b-9fb7-a145a4383e4a	merchant-orders	2026-11-14 14:12:46.328222+00
0493ac2d-d88b-44a8-aa7e-b2eb6750026a	d3de8ab0-eed8-4491-a838-ab12d356c3cb	merchant-orders	2026-11-14 14:12:46.332295+00
4e9fb2e5-4763-48d1-92db-3588877e6715	e0dd158c-740b-4bf0-b56f-0facd393e172	merchant-orders	2026-11-14 14:12:46.335596+00
8effe115-cf7a-4e53-9153-04dffbd8f399	89ccabab-573a-4b75-9ce5-49b723654947	merchant-orders	2026-11-14 14:12:46.339552+00
a36cc04f-3c53-4089-86dd-b763e36b3651	f88b1b48-1da9-4775-8db1-1f8acc2e7e1e	merchant-orders	2026-11-14 14:12:46.343269+00
7ecd65d6-3aee-4cd9-8dbe-7d695fc44ce7	83ca9a25-37b4-4ac9-9072-c0984bb23042	merchant-orders	2026-11-14 14:12:46.345762+00
4c4109d5-fec5-47fd-9a28-ad486d272a61	bb55ef97-15f9-4dd8-832b-d01a48e60188	merchant-orders	2026-11-14 14:12:46.348369+00
f4541cd8-b107-4ffd-a69c-0458b4e07221	ca5d1290-616d-4470-b967-5733d031e077	merchant-orders	2026-11-14 14:12:46.35056+00
090511f4-109f-448a-8efe-d7e572496430	89d6d587-04b2-4644-8410-e47d3412763e	merchant-orders	2026-11-14 14:12:46.35269+00
0550f05f-ff7e-4b34-9f7f-34fffab4dcd4	341fee12-620a-462c-8966-7d2e9bd8500b	merchant-orders	2026-11-14 14:12:46.355354+00
cf4233f3-2dad-4d0c-8519-99a82c32739f	35421030-2c37-4bdc-8cfb-6dafffde7f52	merchant-orders	2026-11-14 14:12:46.357423+00
799a5394-f845-4a00-9d20-20eb695aa829	be369f41-c63e-4d91-be8f-b3f44c4d981a	merchant-orders	2026-11-14 14:12:46.359372+00
cdcf979c-38bd-4a9f-a034-49a981f7e360	60a2e58d-7e97-481e-8498-41c02d595c85	merchant-orders	2026-11-14 14:12:46.361237+00
d0350c5c-fdaa-4d11-bc7d-1297f5bd0e88	8fc2da48-dad3-4f30-a2e7-949ba45a031b	merchant-orders	2026-11-14 14:12:46.362958+00
66a9b1af-dd3e-47ab-aa6b-9b7764d25c9d	285fc5f3-269b-4298-b6e8-8aadd496e625	merchant-orders	2026-11-14 14:12:46.365443+00
08cfed01-fb0e-459e-ba47-147135f55bc4	6c544156-0bd1-496a-9a7f-a7b17183a2d0	merchant-orders	2026-11-14 14:12:46.367594+00
832b9616-abb2-475d-9e09-5f4910397384	7b9470a7-721b-479d-817f-b6f4d51d225d	merchant-orders	2026-11-14 14:12:46.370162+00
76619a1c-b813-483c-aba6-559f454bddea	5e8ca648-60da-41c7-b07f-3b4ad58d7c37	merchant-orders	2026-11-14 14:12:46.372581+00
19163848-105f-4e03-8ea0-f7d896fcc663	6291f0f2-d399-4334-91a3-23f38239bf94	merchant-orders	2026-11-14 14:12:46.376224+00
8c7e4f91-b372-4774-87f6-59afd6bb9e02	7e1c0cf9-d170-40db-8f98-41913f0ee1b7	merchant-orders	2026-11-14 14:12:46.379973+00
29262281-290b-42a5-a4c4-e37a74ee51c9	8aa37883-a876-47f1-8380-128224c4f238	merchant-orders	2026-11-14 14:12:46.382056+00
6665b48f-4e87-4987-8cf3-71bb5d247972	7c4d84d9-795c-4fa8-adcc-1e3f1534e79e	merchant-orders	2026-11-14 14:12:46.384061+00
35c1f46a-8bad-4aa6-916a-efeab7df15f6	bc672289-8fd2-40bb-8702-c6565f702874	merchant-orders	2026-11-14 14:12:46.385924+00
432cfabb-7a8d-423b-a404-58c9871515ca	90b987f1-2ef9-48e6-894d-03c918616a22	merchant-orders	2026-11-14 14:12:46.388022+00
a2d464bd-8c70-42bc-ba6c-e840f3b0a7b9	b28ffaaf-e3cb-40c1-a669-4e0913301f31	merchant-orders	2026-11-14 14:12:46.389961+00
c904fc58-2e79-4a86-b535-f0051c4c30de	2ea8ab69-3715-405e-942c-04c2a6a689b1	merchant-orders	2026-11-14 14:12:46.39204+00
600841a6-5e11-41df-a839-56b48569c493	ec40bf58-fc63-4ca5-8eb0-ba4aba8c92cd	merchant-orders	2026-11-14 14:12:46.394027+00
dfe87ee1-2b9f-4865-af2f-b7a0e334fb18	54eb974e-5d1f-4803-82c1-a9d9e7cd0e9d	merchant-orders	2026-11-14 14:12:46.395671+00
099cb9d9-b7c5-41c7-85f9-a5b98d130c48	b26705a7-66b4-4caf-9179-dc4505d74647	merchant-orders	2026-11-14 14:12:46.397655+00
9c64cb34-80fb-4f54-b9cf-71da8677c37d	2b2e07cc-af9a-4898-bde2-e4b0630d0065	merchant-orders	2026-11-14 14:12:46.399744+00
e1a6eb7f-cd68-42f9-9c66-60600fb2cc28	5849294e-33b6-4980-9c56-3809a997de30	merchant-orders	2026-11-14 14:12:46.401618+00
2f0d32c4-052f-44d8-bf60-bbc2bdb78a33	86fc9857-5d46-4028-8865-3b9d37294f5e	merchant-orders	2026-11-14 14:12:46.403372+00
05e380fb-593a-4843-831a-8d34ae5fd83d	9ec806a6-ceac-4731-b56a-ec0f0b31398d	merchant-orders	2026-11-14 14:12:46.405466+00
26c05f5a-f6d1-46d7-9bf1-81e3b8fe19d6	42b82418-248d-41ad-91b6-0b31777d9269	merchant-orders	2026-11-14 14:12:46.407624+00
733f2cf2-899a-4e6d-adc0-b9a9b0b46a87	8c33bad0-5242-48c9-9be8-5362ffd48544	merchant-orders	2026-11-14 14:12:46.409567+00
14daaca1-1b86-4d61-b18e-e516b0b5d6ad	72e96061-278c-42b8-ae8e-f1cb0c706334	merchant-orders	2026-11-14 14:12:46.411355+00
bcb250ed-5065-4ffb-9c69-e6c8e046f6d3	e251138c-6924-4649-aaff-e4e79947c479	merchant-orders	2026-11-14 14:12:47.32702+00
a1296cae-b43b-4e6d-b9d3-b69651df07a5	0015a650-e47f-4698-89bb-5ddc1091b97e	merchant-orders	2026-11-14 14:12:47.331466+00
fcdc9bb6-2e2c-43c3-bc8c-b2320b2e86e4	611917d4-6320-427f-b33e-77178ba0afd1	merchant-orders	2026-11-14 14:12:47.335363+00
466a8131-54c6-4b6e-abcc-d312fbc0aa53	1127649c-5071-4a8b-b316-0ffefe44546c	merchant-orders	2026-11-14 14:12:47.339212+00
02cc47d0-daac-43bc-b562-90f52eec1d40	df7f687b-9493-4662-a867-c9653e547ec1	merchant-orders	2026-11-14 14:12:47.342429+00
0d54cc51-08bf-4ffe-b43f-01b833146b11	b68bb2c4-c889-4fb1-8243-4ef99e2bfb90	merchant-orders	2026-11-14 14:12:47.345701+00
1e160b71-b5a8-4fce-a138-d01487a04d49	620c727d-3546-4370-83e7-5e3732551cb5	merchant-orders	2026-11-14 14:12:47.348415+00
d0547285-046a-4856-8acd-3b439b80e6fc	c8a748a1-9ca1-4806-9c0c-42cefb052b90	merchant-orders	2026-11-14 14:12:47.350557+00
29830051-d4dd-403f-b331-51736cf108a2	7d71bfe2-2337-416e-bddf-4a2ad1a3fee6	merchant-orders	2026-11-14 14:12:47.35261+00
2b6a2756-a662-445d-915f-341c9829e46c	00d77745-0973-4374-9e9f-c03b30944412	merchant-orders	2026-11-14 14:12:47.354598+00
9ce35299-5c77-4fd5-84ff-b6817ec57e9e	8cb1e845-27f1-4627-b7fe-c2704a023128	merchant-orders	2026-11-14 14:12:47.357059+00
4b780e83-64c7-4998-b1b9-96656793a3e2	c404e5e7-1f13-4a99-be82-836ee2576817	merchant-orders	2026-11-14 14:12:47.360007+00
5ce40a42-b1d1-468e-9541-913d1280b98d	50e436e2-a261-417b-bfb1-10666a762ced	merchant-orders	2026-11-14 14:12:47.362514+00
3e4b2eb3-5973-4c6f-9fe9-218d3da9ebe1	889bac7e-f3f8-4e6e-b67c-a1040b6ab17f	merchant-orders	2026-11-14 14:12:47.364696+00
afda137a-8478-4c9d-a7e6-8883c051058b	0d97bea8-0d91-41a4-90fa-62d732a8476e	merchant-orders	2026-11-14 14:12:47.36685+00
1589788c-f05f-4c62-9d08-7d66a629f22e	3d59f157-2ab0-4688-9886-ea8893de4e61	merchant-orders	2026-11-14 14:12:47.368725+00
f6538bba-5eb5-418f-9361-83851a9ee748	fc0a31ad-0659-408f-8e2c-0cae50f0aef4	merchant-orders	2026-11-14 14:12:47.370912+00
c081439e-5930-4e8c-b4e3-50a5bcd0143f	5ddd64c4-7351-4579-9fc6-fd4b86242981	merchant-orders	2026-11-14 14:12:47.373213+00
0f260e50-c214-42c4-aa6b-a7831d63f659	7e97db7c-a9d5-4f70-908e-b4db44875cc5	merchant-orders	2026-11-14 14:12:47.375274+00
0b35a19c-17ca-4eb5-84fc-46f63ae69c4f	baa0dcb7-a976-4df6-b802-f97a7194e6b6	merchant-orders	2026-11-14 14:12:47.377063+00
161cb23a-2577-49db-8683-5bdd9acbd419	c7385a33-3673-4a67-aa2b-191b67d6ce74	merchant-orders	2026-11-14 14:12:47.379033+00
75ed54ed-0e5c-4fdc-87aa-2324e805e793	ecc991de-6d1c-45f0-92b7-dff489fee9f6	merchant-orders	2026-11-14 14:12:47.380966+00
a0fe075c-4760-4f5d-bd04-61549660caef	3dec8ad0-8dcc-4813-93e3-dcbf8ca9fcf7	merchant-orders	2026-11-14 14:12:47.382975+00
6feef9de-0c49-4903-a71c-7202488669ad	103a8f43-8670-4bad-9ee8-79bedc2dda43	merchant-orders	2026-11-14 14:12:47.384739+00
61fdb923-b15d-49f0-8b75-bae1ae094b19	cd1977e6-84a9-4442-9c07-613023d84e57	merchant-orders	2026-11-14 14:12:47.386582+00
90314969-e7c4-4aba-aeaf-b45b7f139fca	84178913-53ae-40b6-9a98-573a8f71bb47	merchant-orders	2026-11-14 14:12:47.388728+00
cbec78af-64e5-4ce3-a3c5-4cb045e9d4ec	5b1fb333-dd3e-4e57-b467-f90f006124ef	merchant-orders	2026-11-14 14:12:47.390451+00
239d95a7-de24-4bdc-ab1e-3c671bab09a4	a04f3a4c-b339-4c58-bfc6-61321fb6ad0d	merchant-orders	2026-11-14 14:12:47.392153+00
b0bb302b-1dba-4c79-be56-077c9fecbcc9	aceb0eeb-3ba4-4dc9-ac9b-5bb8eaf7d852	merchant-orders	2026-11-14 14:12:47.393764+00
54ac5590-7e62-47ea-b781-a4ba1580b28a	676b2d25-1f19-49d3-acfb-8165e1ced3ce	merchant-orders	2026-11-14 14:12:47.395645+00
bb0019e3-debd-40bd-a899-bed78d50af8e	ce786d9e-1780-4ddd-8b13-af129b79c474	merchant-orders	2026-11-14 14:12:47.397515+00
5c6bc641-9ca4-45c9-b5f2-42ec359b274f	6c287f68-6d4e-4852-aab7-f7a5d10aa3b2	merchant-orders	2026-11-14 14:12:47.399396+00
6dcc4b83-3f85-4893-9c74-039f53899bb3	cdb3b922-46e4-4f0d-8a40-f802e0dc80cc	merchant-orders	2026-11-14 14:12:47.401068+00
35d9a8b6-7cb3-4cd0-8b46-a3f9870244cf	94bea6ff-dab4-4554-bda6-cb700023686f	merchant-orders	2026-11-14 14:12:47.402998+00
181e61b5-a091-4eff-b32c-71e0494546ac	88b79f20-30fe-43a8-a796-fe3b1655a446	merchant-orders	2026-11-14 14:12:47.404699+00
23dd3f2b-8c1d-4acf-a1cf-0e64ca6e8857	59c28a60-2a35-41fa-8f5b-c58b488e88c5	merchant-orders	2026-11-14 14:12:47.406581+00
7a1ffab4-8973-402f-a750-d870500d1f3f	0bbeb0fd-6496-4812-b89e-f0690bdfee18	merchant-orders	2026-11-14 14:12:47.40869+00
5b38b54f-6e75-438e-9863-a98b467f6c33	052c9679-f183-4f3e-b8e8-c7f6041fdca1	merchant-orders	2026-11-14 14:12:47.410387+00
7073aab2-eee5-4823-88a6-ff218a938d25	5ff6c19e-6cce-4e24-b829-5d24bbd3c2f2	merchant-orders	2026-11-14 14:12:47.412178+00
506c441d-a44f-4806-a4d5-1ca491496e41	0bfffe49-1a56-427a-92c8-86a96ebf7b0f	merchant-orders	2026-11-14 14:12:47.414271+00
0eff5a88-9eec-4e7b-abbe-1f5e2fa7fc79	00a2a80f-22bc-4e88-b633-89d6b1b9fc8e	merchant-orders	2026-11-14 14:12:47.41638+00
5b891206-d7e1-4bdd-80ad-b5a87c7d7666	1708dea0-41e2-446b-9231-c62e6d642d52	merchant-orders	2026-11-14 14:12:47.418431+00
949d3ab9-f505-42cb-bc9c-824796139e1f	5aba458f-18f0-479e-a9f0-e62d0704247e	merchant-orders	2026-11-14 14:12:47.420149+00
b2983a05-4d8f-4bd1-b89c-773ac0da1a3a	6c211521-514b-4746-8006-a5010d5dc69b	merchant-orders	2026-11-14 14:12:47.421917+00
80e722c2-53e2-4a38-9eb0-60a5416bda99	2617a5d9-a2e4-4221-8cf7-aab4186accc8	merchant-orders	2026-11-14 14:12:47.42375+00
3b14aa61-f4bc-41d9-b047-1b1a942fe2bb	d8722207-e788-4c9f-b575-156e2a3871f2	merchant-orders	2026-11-14 14:12:47.425384+00
37c13a4f-6572-4b08-be6b-ac6053ec1fd2	95b73644-027a-4edd-93bd-dfd76dea40dd	merchant-orders	2026-11-14 14:12:47.427288+00
a4df2f75-2dc4-4a2b-9a68-8c9aefa66dfb	6a970bbc-34aa-45bd-8551-06694a3405fa	merchant-orders	2026-11-14 14:12:47.429128+00
a59b5bf6-6ec4-41a4-aa2f-fff84edcc835	6db2469e-0404-40f1-870a-0c2d15090833	merchant-orders	2026-11-14 14:12:47.431052+00
e62c4bf5-fca8-4c4d-9977-5af1670aad83	c5a201f3-4802-4d80-b387-a6b97a93bc06	merchant-orders	2026-11-14 14:12:47.432863+00
11aa9458-c0b0-4b52-9add-e203d19b9672	87f68e26-f0cb-4d6e-a95f-606f73fd5e0a	merchant-orders	2026-11-14 14:12:48.367803+00
384dcc65-8bee-4b4a-9151-b44f04e0e084	4522413a-6da8-4da5-822e-3b4ca21bccfe	merchant-orders	2026-11-14 14:12:48.37324+00
3bab20cd-6088-4062-9398-68fe4ebbfc35	d9d09a4a-ccbe-4c8f-99a5-758bd52c2b6e	merchant-orders	2026-11-14 14:12:48.377622+00
082a8703-b57c-45b7-820d-da58e523f4b6	81954384-8b98-4eb0-aad9-27037a83af69	merchant-orders	2026-11-14 14:12:48.382375+00
01942480-ab85-4cdf-91a1-dbc7293241b5	2cf58569-3661-4c2a-9776-cfcc91f5c05c	merchant-orders	2026-11-14 14:12:48.386006+00
49572388-05b3-4366-93ec-01b034133ecf	630cc2ce-4725-4e93-9f36-e22075bf7077	merchant-orders	2026-11-14 14:12:48.389197+00
c1147482-b138-42f5-90a2-e600f85117d8	3131b3fc-ab0d-4c7b-873d-9ec9fda3dfa0	merchant-orders	2026-11-14 14:12:48.392635+00
038b4507-0980-41cf-9ab2-81164d0b4bb5	eb74c89c-34f1-4d72-81c8-07d30f9232c9	merchant-orders	2026-11-14 14:12:48.395503+00
70c74cb6-7091-4237-adbf-7ecbe6a2f290	0b3a679a-94ec-4ea0-bb1b-4e3fdd54406b	merchant-orders	2026-11-14 14:12:48.397865+00
bb092650-e775-4049-9a2e-7b8fe7e50152	daf8eb9e-fa30-43ce-8121-104742f077c9	merchant-orders	2026-11-14 14:12:48.400037+00
322ea838-b13c-49d5-915f-a615aa8ec282	b01e3462-32e2-47b7-a895-76f44582468d	merchant-orders	2026-11-14 14:12:48.402242+00
13947217-6266-4b52-bb35-d0efdbd143f8	6c7d8bd3-796f-455d-a409-c751a7abf190	merchant-orders	2026-11-14 14:12:48.404399+00
5f7eece3-bf93-4ae1-ac1b-b429d3ce5b6b	1fab303c-0d92-4e8f-868e-eb5949c7a87e	merchant-orders	2026-11-14 14:12:48.406882+00
5a4f24d1-86f4-4b25-977e-eeca4c45e986	52fd4cd6-42a1-4131-9b0c-d749017cf5b6	merchant-orders	2026-11-14 14:12:48.409793+00
9fbd46d8-b519-449d-b189-884958117355	98f64caa-6a68-49dc-8b38-1a41b946f473	merchant-orders	2026-11-14 14:12:48.412139+00
f66aa230-376b-40d7-aa36-eb831592b59e	b896357b-e86f-4be0-863c-bb506713d6c8	merchant-orders	2026-11-14 14:12:48.413997+00
3c5c197b-4009-429e-89cf-7c56d2216a18	6a0df613-c735-482b-9be6-1a857995bf08	merchant-orders	2026-11-14 14:12:48.416136+00
431195cf-8451-4940-bd5c-4eccb8f5af0d	49ee8d26-b9ba-4253-ae8b-0d9e3683fba8	merchant-orders	2026-11-14 14:12:48.418405+00
00ac0347-e483-4425-8e21-2fa5f97473c0	cd53a1fd-3c44-4c05-bde0-697613a3998c	merchant-orders	2026-11-14 14:12:48.420969+00
abc4904e-d086-4071-9295-0fa15b5838a1	0df79144-7f65-4557-b689-fdaa04031894	merchant-orders	2026-11-14 14:12:48.423173+00
fecd995a-7d65-4aba-90d2-4146cd0865e5	a8e1bdd8-a96a-44e7-9d8a-37da3fb41f26	merchant-orders	2026-11-14 14:12:48.425112+00
d94239bf-f01c-45a9-ab05-bffd3e87275d	8dc7ff28-fda5-465d-887a-044493dcf2da	merchant-orders	2026-11-14 14:12:48.427226+00
fc8df0e7-358e-436d-9db5-ace3b6c1a93b	f4eeec4e-86e5-48ea-b04a-35a974b8c37c	merchant-orders	2026-11-14 14:12:48.42963+00
37c3f1d1-9780-4eea-8651-afe4d6ea238a	94f2bc2e-e99b-4266-80fa-cf53ebbbde5f	merchant-orders	2026-11-14 14:12:48.431946+00
c84aeeec-214a-4a3c-b512-0499153e4ed5	439111a9-8b0d-4717-8cba-2b2925e68dd6	merchant-orders	2026-11-14 14:12:48.434971+00
4e57c054-5440-4476-811a-0e1acae951ba	a5e9b1f4-484f-48e4-862c-587c3171b5b9	merchant-orders	2026-11-14 14:12:48.437436+00
111ced29-a0f3-4668-98ea-57b7cca6a29e	05da7bc8-b1bb-4dc2-879e-bdd8f82caab6	merchant-orders	2026-11-14 14:12:48.439648+00
331c8ac1-d374-42ad-86ac-825eb5169758	775ed2c4-644f-4bd7-91a2-b58c5606aa0a	merchant-orders	2026-11-14 14:12:48.442081+00
42ed30bf-fe55-425b-b54c-7c4fefb4be47	ca887147-3ae6-4e56-9879-7acfd9eca033	merchant-orders	2026-11-14 14:12:48.44395+00
298fdc30-eae3-4c83-8e64-ab920a447595	fbf2f16c-0a24-44db-bd04-4b11cd4ce904	merchant-orders	2026-11-14 14:12:48.445786+00
fd492c96-412e-4082-9f65-05c6e29448b5	1f614c4c-9bc6-447d-b4e8-ea47382783c0	merchant-orders	2026-11-14 14:12:48.4478+00
664b73d2-ac75-4dc7-a4f2-63ed725c6047	a068712b-3aac-402d-b25a-74b990396731	merchant-orders	2026-11-14 14:12:48.44956+00
5ca64002-e557-4e18-869e-ea06b8f7df2f	5b34a635-4dc5-454d-a708-76921dc5315f	merchant-orders	2026-11-14 14:12:48.452047+00
a4f38fe6-3569-48af-ba92-798df151707d	175af317-f8c8-48f0-a160-3021ce5108b8	merchant-orders	2026-11-14 14:12:48.453923+00
cadd8c60-8c0d-42c6-b644-3a4b59a44c07	da9da7d4-e1c3-4649-86a1-59a2a7a25473	merchant-orders	2026-11-14 14:12:48.455897+00
0d830b58-6b8e-4bb9-93dc-4a2fd6ecc372	00d16832-c7ee-49e4-abd9-fb667d550884	merchant-orders	2026-11-14 14:12:48.457745+00
e3d0893b-1d10-4b14-ae08-80745e74c435	c4023cc5-3b44-4723-9a65-79448bc13692	merchant-orders	2026-11-14 14:12:48.460702+00
386a2ad5-7661-4ebb-a0f3-7d803e750f21	84bd38d4-0bf1-4412-80df-7662c24423a2	merchant-orders	2026-11-14 14:12:48.463222+00
ec1e7851-b116-4438-bc71-a407f4c55926	2b9ad04a-4374-4d34-902f-974476b66340	merchant-orders	2026-11-14 14:12:48.465969+00
bf091864-16e2-422d-864b-d76c4f460ddc	e37435ab-280e-4e03-a40d-f6fd0214f387	merchant-orders	2026-11-14 14:12:48.468751+00
b98e067c-bcfc-46b6-a1a4-b15b54da8eb1	bf95d7be-27c7-409e-8954-8f684a434c84	merchant-orders	2026-11-14 14:12:48.472049+00
a887d668-a405-4c2d-af55-d3f69051d9a0	0e891d75-950e-4a32-b45e-58123ce1610f	merchant-orders	2026-11-14 14:12:48.476049+00
d2c13e22-4801-4a13-8424-ca6ceb60cb63	ecfc250d-26dc-4982-939c-23f388d0fae7	merchant-orders	2026-11-14 14:12:48.47923+00
964fd1b2-4763-47e9-9f3e-40f7ac8cc698	2859ab31-3fac-4d88-a473-53d34929ce3c	merchant-orders	2026-11-14 14:12:48.48316+00
8efb0a2b-2373-4d1a-8a29-afe1ccc9b4ff	066a2fb7-5694-4ddf-a05e-d61aa0db1762	merchant-orders	2026-11-14 14:12:48.487337+00
36795189-7e9d-4a48-b1f9-be797a6fecd5	b8fee499-dee8-4279-95cf-f6c28ef68eb5	merchant-orders	2026-11-14 14:12:48.490906+00
d5c6f202-78f4-4fd3-a1d3-fbcac1288269	a26d9b5e-45a1-4cfc-a3f4-5cc02a74472c	merchant-orders	2026-11-14 14:12:48.494181+00
28f99d7a-4726-41f6-b1ce-8cdf4db9f562	cfaad84d-3e56-4758-ac35-5e422e0afda6	merchant-orders	2026-11-14 14:12:48.497694+00
e4e132e2-92fd-4d95-9e8b-c02c36ee5bd6	58f1ee9e-8cf2-4c32-8d17-89a689a9bba1	merchant-orders	2026-11-14 14:12:48.50063+00
3a2e18f2-bc9b-46db-97e7-69ce8d224a7e	6dfb9c4a-9da2-4c33-9757-c87253d8042f	merchant-orders	2026-11-14 14:12:48.503301+00
ac894149-1ce7-42e5-af04-a634e62c777d	f53d3c2d-3a0b-4035-938e-a51a7da7680d	merchant-orders	2026-11-14 14:12:49.412204+00
348e2001-1aed-4557-bc85-c694c9a2f1a5	80da5157-98bc-4ac5-bd0a-8b145ebabe6a	merchant-orders	2026-11-14 14:12:49.419457+00
8c290434-041d-432d-9140-bc2b3255a186	d6cae3f0-3e28-413b-80dd-0e24b93aec3c	merchant-orders	2026-11-14 14:12:49.423605+00
69af0f75-4968-436d-9f56-9796ba5cb48b	9438e273-d88f-459f-bf26-597e2330b71e	merchant-orders	2026-11-14 14:12:49.426992+00
f72c9ff6-f64f-4700-b3e7-fc77d3f9269e	7bc456b3-0c29-4188-80c0-7b4d16c98a24	merchant-orders	2026-11-14 14:12:49.430384+00
7738a857-9b65-4a94-8756-9946ca0d7284	289e0a90-04d0-487d-979f-265a846a0c5c	merchant-orders	2026-11-14 14:12:49.433185+00
a99493b9-04cd-4402-b31e-b9950239575a	67713d88-f4a2-447f-86d1-10de7ea9603f	merchant-orders	2026-11-14 14:12:49.435875+00
f4dcde9d-fdca-4f6e-b011-6ea1406c4b68	e1b25c8c-5052-450d-925f-6997e363b071	merchant-orders	2026-11-14 14:12:49.438588+00
090437c3-d8f6-4b4f-8b94-8096989c95bd	27521873-581d-46c4-a890-a0199f122f77	merchant-orders	2026-11-14 14:12:49.441035+00
0627aa04-73d9-48e8-a22d-01e2837d84cd	ce789c0a-1628-4ac6-a0db-1444ac64d3c9	merchant-orders	2026-11-14 14:12:49.44314+00
12ab700f-cfec-45f8-861d-d3389bc9934f	878f2b17-5cd9-4edf-8a2c-d8762b29df84	merchant-orders	2026-11-14 14:12:49.445091+00
690beba2-f349-4ad1-8edb-c5c5a1c609c0	6e2b3840-6401-44ff-a751-eedf9957370c	merchant-orders	2026-11-14 14:12:49.447392+00
1ae105f4-a8c3-4e8f-934b-ac392fb3f43d	7d503963-e33b-4f53-af9b-beffcb7b6b92	merchant-orders	2026-11-14 14:12:49.449275+00
0611072e-a09a-4906-930e-267df2d19ed0	ca0c6fe5-68d1-4181-b82a-34617e434656	merchant-orders	2026-11-14 14:12:49.451408+00
dbb3346a-c93c-4ec6-b2d2-b6c00d60d8ea	c3cc377a-83e9-4ef2-8aca-79365a9e54a3	merchant-orders	2026-11-14 14:12:49.453748+00
ef98d160-cbff-40fb-aad4-ad0260a9fbb8	bbf5e241-f86b-4c5d-96b5-3af1b8b43da6	merchant-orders	2026-11-14 14:12:49.455723+00
dfd179b2-660c-4585-9525-d5fa6644685f	03045c2c-b043-4388-8856-946a8a217747	merchant-orders	2026-11-14 14:12:49.457489+00
64bc31f0-e3ea-4e6c-9113-53774a1eac05	7c75cbcb-3cb7-4b1b-b73b-77e06a5d9ef9	merchant-orders	2026-11-14 14:12:49.459386+00
0ced400b-f855-4528-a31e-ff9984b8ce96	06850a8f-abbf-4059-a137-572aa51f06aa	merchant-orders	2026-11-14 14:12:49.461238+00
e2d052a1-1c91-48bd-83d1-ec87ce85ea46	fdb7f315-2134-4dc1-903c-cfacee2f0ce8	merchant-orders	2026-11-14 14:12:49.463157+00
e1ee0560-a5f6-472e-b6d4-ebe378a5c047	6477eb88-4a58-4722-8e44-43444b466695	merchant-orders	2026-11-14 14:12:49.464782+00
1f9a80c5-250e-4968-82cd-9600c7ff5065	f022f558-06bf-40d2-aec2-a935e9fa0536	merchant-orders	2026-11-14 14:12:49.46623+00
1acbd170-304c-479b-8986-03b623a779a5	3927ca3c-7a68-4ede-af40-e9146adaa484	merchant-orders	2026-11-14 14:12:49.467888+00
a2afe7ed-fe40-4ea7-a0b0-f82be17fee78	acf8c95a-69da-4cdf-9db5-5026ff5609bc	merchant-orders	2026-11-14 14:12:49.469632+00
9c9584f2-55e1-4a9b-bc56-815f85592725	f796b7a4-cf81-47db-a1c9-ac5b5c4a110a	merchant-orders	2026-11-14 14:12:49.471101+00
0f577a8b-b1dd-4c16-af28-f8eb42bced6f	732d18f5-3632-4f5b-995d-0a9583f0ec79	merchant-orders	2026-11-14 14:12:49.472611+00
35f0556d-9e19-4fd5-8230-f17d75a9ed59	5c2c1f7c-913a-436e-9ad4-9d0ed7d31b3f	merchant-orders	2026-11-14 14:12:49.474177+00
c45071cf-e4da-4847-99c2-4024a01b4bae	9196b3c3-d8bd-41f4-a978-7ea899382f1d	merchant-orders	2026-11-14 14:12:49.47595+00
a83378eb-3ecf-4dbf-8325-86b9b30125e5	7f0fb481-4022-41c3-9b36-103d57f445d6	merchant-orders	2026-11-14 14:12:49.477914+00
1afc916b-087b-413d-8b26-d84573570abe	e7d08d94-4da9-4c02-a246-b798633debfa	merchant-orders	2026-11-14 14:12:49.479827+00
6bd002c7-ba65-4e34-bb2a-60601524ff3d	e52db7d0-590e-436b-bdb1-772073701fd5	merchant-orders	2026-11-14 14:12:49.481443+00
8ad1de7e-31ef-494e-b447-37a12e009904	bbc92b51-965d-4ce2-9544-4413cae5cf14	merchant-orders	2026-11-14 14:12:49.48321+00
0da322dc-581d-420d-8ff2-e0676b8f4a7f	50fff81b-556e-4370-b3e2-6bb6cdfac9eb	merchant-orders	2026-11-14 14:12:49.484756+00
38abac30-dd26-4a11-ae56-e836c0dadc74	fbd3ea43-d469-4ef0-bb4e-4135d8c13961	merchant-orders	2026-11-14 14:12:49.486509+00
dd7e83c8-f108-4a6a-bc42-246bfc73e434	82514ee4-2a39-4b74-abb4-d8f94687b815	merchant-orders	2026-11-14 14:12:49.488254+00
5656a6cc-1101-4696-8e83-2920b02bb6d9	4d315648-4cb6-4e95-9cde-e4a90151cc35	merchant-orders	2026-11-14 14:12:49.489824+00
10ed1a4c-caed-4e20-9edb-d040ddf80e7d	640dd0b1-e763-49bb-9aeb-f2a8d65270e1	merchant-orders	2026-11-14 14:12:50.450998+00
b0fb9fbd-baeb-42fc-ba1f-4e733061a826	03f115f6-6a36-47ee-9d56-d80742ce7fad	merchant-orders	2026-11-14 14:12:50.45648+00
6b516f80-05c9-46cd-80ba-3141a228d0c6	21ccad15-37a8-4f45-ac03-02b7d33e7824	merchant-orders	2026-11-14 14:12:50.462183+00
a7e003bf-96b0-41f4-a201-a3d6fc3ec747	db7007f0-7dc2-4963-bb91-bbfce6b6af59	merchant-orders	2026-11-14 14:12:50.465185+00
3abf6f94-4097-48ce-b076-88a41a3920ad	f3117996-e0bc-4508-a90e-5758ebd267e3	merchant-orders	2026-11-14 14:12:50.4684+00
91ca1f4d-f5d2-4267-9116-64d5a4035ad1	105fdd40-e343-4867-93e9-6c2c342fb157	merchant-orders	2026-11-14 14:12:50.471368+00
9e506d85-0281-4abe-a32c-aad2040fcc30	7b65d445-4ed5-4726-8d1a-112e3a8093fa	merchant-orders	2026-11-14 14:12:50.47427+00
97e4d928-29d0-49c8-a00e-e9369fc98741	5221428c-6bbd-4187-b9db-addac8422523	merchant-orders	2026-11-14 14:12:50.476331+00
f4815494-ce6c-44ca-856a-ecad086bbfef	9ca90e89-e309-486e-9927-98149efd1e97	merchant-orders	2026-11-14 14:12:50.478269+00
da5485a8-92d5-420c-8164-7e3d71f26a60	96debd4a-afec-4468-8152-def74f712252	merchant-orders	2026-11-14 14:12:50.480284+00
7eed9067-1914-471a-9945-4e26253c84bc	e3ab9475-1e17-4500-bec9-26f6b3ea0687	merchant-orders	2026-11-14 14:12:50.48239+00
3cb5171d-1562-46a2-b48c-80effa6f66c1	b9195053-28fe-487c-a302-7a3dc0dcaf5c	merchant-orders	2026-11-14 14:12:50.484118+00
4f47fa6e-3a22-48a5-94b8-031f5ac9f1a5	86a72915-fad0-4564-b676-e74706fdbc44	merchant-orders	2026-11-14 14:12:50.485727+00
08e4f5b8-9110-444a-9b1b-382234d8bd67	78293f31-6cf1-463d-8700-34e6168e24dc	merchant-orders	2026-11-14 14:12:50.487189+00
5ad365ad-adb9-4ade-8d42-3b60e49202ab	1f6aabf8-d701-41ae-aeae-585afea992bd	merchant-orders	2026-11-14 14:12:50.488827+00
4500d9ad-5d9e-4537-9787-7258514d9a18	2d0741d1-75df-4742-aa57-3a66897d3899	merchant-orders	2026-11-14 14:12:50.49085+00
f7f9ee5d-0711-4c61-8e0b-2ca5267db303	93b75ba8-c6e1-415f-bdcb-85806eebac5f	merchant-orders	2026-11-14 14:12:50.492275+00
70f1bbbb-fed4-4937-8692-546c118d0f24	2f80b1a4-c3ab-46a6-88c9-c76167638b13	merchant-orders	2026-11-14 14:12:50.494022+00
bb7e9658-9d06-4822-a910-f3f7e1967db4	ee23772b-15a6-4570-9bed-9174dc0d0d5a	merchant-orders	2026-11-14 14:12:50.4954+00
a9e041f5-561b-442f-a64f-42f66707ca0b	ec8f7f8b-793e-4f8a-b7d6-0c74893058de	merchant-orders	2026-11-14 14:12:50.497142+00
24ead45a-32f0-4b56-9612-79cc2d2c60f3	a4fbc84c-907a-48a8-9f5a-aa3c708bdd38	merchant-orders	2026-11-14 14:12:50.498838+00
54266aa6-9944-4496-a9c0-adc14ccfcc5f	45999175-fd8d-4333-ae10-bd130724c808	merchant-orders	2026-11-14 14:12:50.500493+00
079b4f07-63e5-47b8-b55a-2ffac81a3c7e	464e4498-ba24-48c7-b87b-d12c75b55c59	merchant-orders	2026-11-14 14:12:50.501983+00
7b26904d-2137-4c22-a172-633712813d1a	f95f3e1a-77bc-4562-b2ad-3deb0b5d5b70	merchant-orders	2026-11-14 14:12:50.503482+00
78e5f364-fa2c-4e4a-b89a-3aa5d176b9f8	e71c9bbf-6ffe-40bd-ab1a-c951ad542b61	merchant-orders	2026-11-14 14:12:50.504893+00
477eeff8-af81-46c9-a9c0-08af3972cdda	e411e882-a728-4a70-81fd-af74ac47fe6b	merchant-orders	2026-11-14 14:12:50.506179+00
a6d47c4a-7c92-4c96-b3d1-12f2842f78d8	1d0c9216-b5dd-4985-be97-5d07f6291d9b	merchant-orders	2026-11-14 14:12:50.507652+00
1f43a546-08ea-4df3-b1d8-81f63b32d9c9	81d0c46a-10bb-4e1c-b5de-72a101cc8471	merchant-orders	2026-11-14 14:12:50.509189+00
4689fab8-b5ec-479c-9a47-78f34249b30c	67e485d8-b0ab-4fc7-8be7-0d74f0fc1a35	merchant-orders	2026-11-14 14:12:50.510568+00
06ccda02-72d6-4d8b-a5c8-3a6eba25abaa	b7b3d45f-4be0-4b61-ba33-317b2a05f71e	merchant-orders	2026-11-14 14:12:51.487011+00
f20a4e3d-709d-4054-9371-09c8c4cd81ae	2df14161-2772-487d-b425-2cd39160b14d	merchant-orders	2026-11-14 14:12:51.492949+00
5a83ee4f-0920-4529-9715-f06f9f7c2bec	fa90a349-74d5-4679-a5f6-efd4a4b49ef5	merchant-orders	2026-11-14 14:12:51.497594+00
51e5fa74-9b6e-4e77-bf33-731cefb1ebec	3cfdd310-c84d-4847-acc0-1f53095f6021	merchant-orders	2026-11-14 14:12:51.500955+00
584a90d3-0340-4334-bc29-261ed72a2d3a	0fc88bf2-fe0c-40b1-b051-897e2271406b	merchant-orders	2026-11-14 14:12:51.503546+00
31bb0668-06d1-4a20-8dee-209dc09af21e	de5931c4-cbfe-4e86-89dd-ea6182179552	merchant-orders	2026-11-14 14:12:51.506072+00
d7a8762c-4889-40b8-b36e-a32050b06162	aba68dd7-91c9-4f95-9477-2004365cb6d2	merchant-orders	2026-11-14 14:12:51.509012+00
373b94a9-d47d-45b9-8283-c105442ba64d	78d8a356-2a86-4cf9-923a-1290d97207da	merchant-orders	2026-11-14 14:12:51.511301+00
dee7a858-eeb3-48cd-81e7-ac140a8d23bd	746d5f71-95ee-44ae-b66e-f4cf549169cc	merchant-orders	2026-11-14 14:12:51.513251+00
a1fe56f7-fcd7-4e86-a0a8-dddccf1488f2	a5b54a56-09c4-4b04-aad8-eb18dc53279d	merchant-orders	2026-11-14 14:12:51.514926+00
507e211a-ebc5-4382-8f73-26a129666d0f	11d59bbb-c88b-4d49-be41-226b8e4faa3e	merchant-orders	2026-11-14 14:12:51.517068+00
652e2b31-c09e-4d59-8b4e-1b1b852f9d7f	680469e6-d3a7-4e7c-b8c8-f624e9457d58	merchant-orders	2026-11-14 14:12:51.518723+00
31b7289a-50c1-4804-a29d-73da9f60936a	d5293a41-0791-44ee-bf46-477d2dc6075a	merchant-orders	2026-11-14 14:12:51.520266+00
2e0bd0e5-d642-4078-a6ab-cbea7c530c16	08d00d3e-ed93-4398-ae4e-197642e7b432	merchant-orders	2026-11-14 14:12:51.522047+00
6150429c-5d9a-43a9-9c60-3f3ce517350a	06bed6eb-fd7b-430c-a311-c8ddcb184ffd	merchant-orders	2026-11-14 14:12:51.523588+00
3ad81482-aaf3-4c1c-9f28-7269f2b6d4d6	20bd75a2-7118-4133-ac05-ee9bfe97cc48	merchant-orders	2026-11-14 14:12:51.525599+00
67a64a8c-931e-4d27-84d8-5181c195e3b7	66338ad3-0679-422a-a997-33326110130d	merchant-orders	2026-11-14 14:12:51.528707+00
bc35becb-8188-4855-9c07-22e6e37bb2ad	166cb0e1-242a-4e2e-916e-f78e85a31b61	merchant-orders	2026-11-14 14:12:52.511532+00
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
-- Name: processed_events processed_events_pkey; Type: CONSTRAINT; Schema: public; Owner: merchant
--

ALTER TABLE ONLY public.processed_events
    ADD CONSTRAINT processed_events_pkey PRIMARY KEY (id);


--
-- Name: processed_events uq_processed_events_event_consumer; Type: CONSTRAINT; Schema: public; Owner: merchant
--

ALTER TABLE ONLY public.processed_events
    ADD CONSTRAINT uq_processed_events_event_consumer UNIQUE (event_id, consumer_name);


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

\unrestrict TXA1XBYNkE8XpeRJ2WxeKHImKUCgy0FhRDs4sJtR6NqTrTn4AsEOgsgUX5ercaW

