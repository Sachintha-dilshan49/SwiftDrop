--
-- PostgreSQL database dump
--

\restrict ijGMX3UeHr5Kj97nvYfWUD2DKE7qeThJH9mDKaFEnEH2iZKiz0LmXYFFm4dKQru

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

ALTER TABLE IF EXISTS ONLY public.delivery_proofs DROP CONSTRAINT IF EXISTS fk_delivery_proofs_assignment;
ALTER TABLE IF EXISTS ONLY public.daily_stats DROP CONSTRAINT IF EXISTS fk_daily_stats_courier;
ALTER TABLE IF EXISTS ONLY public.assignments DROP CONSTRAINT IF EXISTS fk_assignments_courier;
DROP INDEX IF EXISTS public.ux_assignments_active_parcel;
DROP INDEX IF EXISTS public.idx_processed_events_event_id;
DROP INDEX IF EXISTS public.idx_delivery_proofs_assignment_id;
DROP INDEX IF EXISTS public.idx_daily_stats_date;
DROP INDEX IF EXISTS public.idx_daily_stats_courier_id;
DROP INDEX IF EXISTS public.idx_couriers_user_id;
DROP INDEX IF EXISTS public.idx_couriers_status;
DROP INDEX IF EXISTS public.idx_assignments_status;
DROP INDEX IF EXISTS public.idx_assignments_parcel_id;
DROP INDEX IF EXISTS public.idx_assignments_courier_id;
DROP INDEX IF EXISTS public.flyway_schema_history_s_idx;
ALTER TABLE IF EXISTS ONLY public.processed_events DROP CONSTRAINT IF EXISTS processed_events_pkey;
ALTER TABLE IF EXISTS ONLY public.processed_events DROP CONSTRAINT IF EXISTS processed_events_event_id_unique;
ALTER TABLE IF EXISTS ONLY public.flyway_schema_history DROP CONSTRAINT IF EXISTS flyway_schema_history_pk;
ALTER TABLE IF EXISTS ONLY public.delivery_proofs DROP CONSTRAINT IF EXISTS delivery_proofs_pkey;
ALTER TABLE IF EXISTS ONLY public.daily_stats DROP CONSTRAINT IF EXISTS daily_stats_unique;
ALTER TABLE IF EXISTS ONLY public.daily_stats DROP CONSTRAINT IF EXISTS daily_stats_pkey;
ALTER TABLE IF EXISTS ONLY public.couriers DROP CONSTRAINT IF EXISTS couriers_user_id_unique;
ALTER TABLE IF EXISTS ONLY public.couriers DROP CONSTRAINT IF EXISTS couriers_pkey;
ALTER TABLE IF EXISTS ONLY public.assignments DROP CONSTRAINT IF EXISTS assignments_pkey;
DROP TABLE IF EXISTS public.processed_events;
DROP TABLE IF EXISTS public.flyway_schema_history;
DROP TABLE IF EXISTS public.delivery_proofs;
DROP TABLE IF EXISTS public.daily_stats;
DROP TABLE IF EXISTS public.couriers;
DROP TABLE IF EXISTS public.assignments;
SET default_tablespace = '';

SET default_table_access_method = heap;

--
-- Name: assignments; Type: TABLE; Schema: public; Owner: dispatch
--

CREATE TABLE public.assignments (
    id uuid DEFAULT gen_random_uuid() NOT NULL,
    parcel_id uuid NOT NULL,
    courier_id uuid NOT NULL,
    assigned_by_user_id uuid NOT NULL,
    status character varying(32) DEFAULT 'ASSIGNED'::character varying NOT NULL,
    assigned_at timestamp with time zone DEFAULT now() NOT NULL,
    created_at timestamp with time zone DEFAULT now() NOT NULL,
    updated_at timestamp with time zone,
    tracking_no character varying(20),
    pickup_address character varying(500),
    dropoff_address character varying(500),
    recipient_name character varying(150),
    parcel_size character varying(16),
    weight_kg numeric(6,2),
    parcel_status character varying(32),
    CONSTRAINT assignments_parcel_status_check CHECK (((parcel_status)::text = ANY ((ARRAY['CREATED'::character varying, 'PICKED_UP'::character varying, 'IN_TRANSIT'::character varying, 'OUT_FOR_DELIVERY'::character varying, 'DELIVERED'::character varying, 'FAILED'::character varying])::text[]))),
    CONSTRAINT assignments_status_check CHECK (((status)::text = ANY ((ARRAY['ASSIGNED'::character varying, 'IN_PROGRESS'::character varying, 'COMPLETED'::character varying, 'FAILED'::character varying])::text[])))
);


ALTER TABLE public.assignments OWNER TO dispatch;

--
-- Name: couriers; Type: TABLE; Schema: public; Owner: dispatch
--

CREATE TABLE public.couriers (
    id uuid DEFAULT gen_random_uuid() NOT NULL,
    user_id uuid NOT NULL,
    status character varying(32) DEFAULT 'AVAILABLE'::character varying NOT NULL,
    created_at timestamp with time zone DEFAULT now() NOT NULL,
    updated_at timestamp with time zone,
    CONSTRAINT couriers_status_check CHECK (((status)::text = ANY ((ARRAY['AVAILABLE'::character varying, 'BUSY'::character varying, 'OFFLINE'::character varying])::text[])))
);


ALTER TABLE public.couriers OWNER TO dispatch;

--
-- Name: daily_stats; Type: TABLE; Schema: public; Owner: dispatch
--

CREATE TABLE public.daily_stats (
    id uuid DEFAULT gen_random_uuid() NOT NULL,
    date date NOT NULL,
    courier_id uuid NOT NULL,
    assigned_count integer DEFAULT 0 NOT NULL,
    completed_count integer DEFAULT 0 NOT NULL,
    failed_count integer DEFAULT 0 NOT NULL,
    created_at timestamp with time zone DEFAULT now() NOT NULL,
    updated_at timestamp with time zone
);


ALTER TABLE public.daily_stats OWNER TO dispatch;

--
-- Name: delivery_proofs; Type: TABLE; Schema: public; Owner: dispatch
--

CREATE TABLE public.delivery_proofs (
    id uuid DEFAULT gen_random_uuid() NOT NULL,
    assignment_id uuid NOT NULL,
    object_key character varying(255) NOT NULL,
    file_name character varying(255),
    uploaded_at timestamp with time zone DEFAULT now() NOT NULL,
    created_at timestamp with time zone DEFAULT now() NOT NULL,
    updated_at timestamp with time zone
);


ALTER TABLE public.delivery_proofs OWNER TO dispatch;

--
-- Name: flyway_schema_history; Type: TABLE; Schema: public; Owner: dispatch
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


ALTER TABLE public.flyway_schema_history OWNER TO dispatch;

--
-- Name: processed_events; Type: TABLE; Schema: public; Owner: dispatch
--

CREATE TABLE public.processed_events (
    id uuid DEFAULT gen_random_uuid() NOT NULL,
    event_id character varying(255) NOT NULL,
    source character varying(128) NOT NULL,
    processed_at timestamp with time zone DEFAULT now() NOT NULL,
    created_at timestamp with time zone DEFAULT now() NOT NULL
);


ALTER TABLE public.processed_events OWNER TO dispatch;

--
-- Data for Name: assignments; Type: TABLE DATA; Schema: public; Owner: dispatch
--

COPY public.assignments (id, parcel_id, courier_id, assigned_by_user_id, status, assigned_at, created_at, updated_at, tracking_no, pickup_address, dropoff_address, recipient_name, parcel_size, weight_kg, parcel_status) FROM stdin;
aa640526-f565-4418-9ef5-5f5dea8b8a4b	cb4c89c8-e03c-4ed1-99e3-258ee583d4c4	0c248632-4e7b-4ade-893f-f0a7d8ae0cfb	cb4cd22c-d4cd-4f2f-ad43-c2e73f1b9cb5	COMPLETED	2026-10-16 06:04:59.999674+00	2026-10-16 06:04:59.999675+00	2026-10-17 06:13:00.005212+00	SD-TWCK-SK6K	310 Ward Place, Colombo 07	208 Galle Road, Colombo 04	Senuri de Silva	SMALL	0.56	DELIVERED
43293013-d935-46f3-9805-49f6af6ab46d	7efe7dd0-e98c-4c1f-98e3-9d9486aeff4e	72186f7d-7dd3-4e09-8551-045f309d0a7e	cb4cd22c-d4cd-4f2f-ad43-c2e73f1b9cb5	COMPLETED	2026-10-16 05:35:00.000131+00	2026-10-16 05:35:00.000131+00	2026-10-17 04:21:00.003904+00	SD-4H58-DZA7	292 Flower Road, Colombo 07	225 York Street, Colombo 01	Dinuka Silva	LARGE	10.97	DELIVERED
0391df31-ca48-4e49-b27b-fa6e8969f066	3bf1f8a9-ea53-4c5b-83bb-37abb5776990	0c248632-4e7b-4ade-893f-f0a7d8ae0cfb	3900455f-0834-4d57-85c5-c3de92f5ecee	FAILED	2026-10-16 06:26:00.000424+00	2026-10-16 06:26:00.000424+00	2026-10-17 06:35:00.004115+00	SD-63NF-QTNQ	302 Hotel Road, Mount Lavinia	285 Flower Road, Colombo 07	Kavindi Fonseka	MEDIUM	6.09	FAILED
d5644461-17dd-45b4-942d-b21046946cce	0f793e08-7f96-4004-8696-2d2d537fa471	86d11ba9-d0e8-4301-8403-9903afbd11a9	3900455f-0834-4d57-85c5-c3de92f5ecee	COMPLETED	2026-10-16 09:23:00.000168+00	2026-10-16 09:23:00.000168+00	2026-10-17 09:12:00.004082+00	SD-ZXZV-8P32	295 Hotel Road, Mount Lavinia	229 Matara Road, Galle	Nadeesha Senanayake	SMALL	2.43	DELIVERED
ac7b186f-fb07-422e-91bd-2504b972e900	ec6452c6-3f54-4c9b-b5c7-9cc86ad162e4	2c4bbfbc-96d6-4e98-b778-a375c6c2ab0d	3900455f-0834-4d57-85c5-c3de92f5ecee	COMPLETED	2026-10-17 03:15:59.999884+00	2026-10-17 03:15:59.999886+00	2026-10-18 04:21:00.00454+00	SD-68M4-QFTB	115 Galle Road, Colombo 04	18 Ward Place, Colombo 07	Hiruni Fernando	LARGE	15.63	DELIVERED
f4fd906f-2df9-4a36-be2b-124ffd0f6e49	6fcc04d5-88a4-4a36-98c0-505cc019a285	72186f7d-7dd3-4e09-8551-045f309d0a7e	7bf5a598-1908-4a34-9ac4-041d3ce99466	COMPLETED	2026-10-17 09:10:59.99992+00	2026-10-17 09:10:59.999921+00	2026-10-18 03:51:00.003404+00	SD-Q8B2-W2SF	52 Kotte Road, Kotte	118 Colombo Road, Negombo	Hiruni Gunasekara	SMALL	2.80	DELIVERED
de19d75b-a3bf-45a1-97d0-89a29b57b368	2df5c1c0-1479-444f-8de8-091489264053	64dd446f-7e03-421f-a2f0-05db7f2bc36c	7bf5a598-1908-4a34-9ac4-041d3ce99466	FAILED	2026-10-17 10:47:59.99995+00	2026-10-17 10:47:59.99995+00	2026-10-18 06:12:00.00282+00	SD-VHBW-7242	82 Colombo Road, Negombo	263 Galle Road, Colombo 04	Dimuth Cooray	EXTRA_LARGE	33.35	FAILED
b91d9b31-1bf3-4956-89fe-384c7f8cf7cb	5d36d7b4-d02b-42b1-9069-0be00b019294	72186f7d-7dd3-4e09-8551-045f309d0a7e	7bf5a598-1908-4a34-9ac4-041d3ce99466	COMPLETED	2026-10-18 04:38:59.999543+00	2026-10-18 04:38:59.999543+00	2026-10-19 03:57:00.003562+00	SD-RMK4-ALEA	246 Nawala Road, Rajagiriya	308 Main Street, Colombo 11	Dulani Mendis	SMALL	2.67	DELIVERED
3612272d-2f3f-4871-a2c8-4ab97fab2ffd	9d7622d3-bd8d-4582-9ec2-51126e098884	72186f7d-7dd3-4e09-8551-045f309d0a7e	7bf5a598-1908-4a34-9ac4-041d3ce99466	COMPLETED	2026-10-18 11:03:59.999644+00	2026-10-18 11:03:59.999644+00	2026-10-19 07:18:00.003794+00	SD-YNK8-VG8N	43 Thimbirigasyaya Road, Colombo 05	141 Matara Road, Galle	Hiruni Jayawardena	SMALL	1.78	DELIVERED
3c5eacdd-e228-470d-8d2c-c9536e0753a3	7eed64e3-d48d-4e46-8db1-43dfaed0aa19	8b77ede6-e12d-4a24-9795-f3c87c19d2ba	cb4cd22c-d4cd-4f2f-ad43-c2e73f1b9cb5	COMPLETED	2026-10-18 12:41:00.000253+00	2026-10-18 12:41:00.000254+00	2026-10-19 05:57:00.003277+00	SD-UBQD-Q6W5	106 Kotte Road, Kotte	132 Colombo Road, Negombo	Nimal Herath	MEDIUM	2.01	DELIVERED
f48494a1-d976-41ff-b431-fe2901913d59	e18c27a7-cb95-4940-be6f-332901913c5c	0c248632-4e7b-4ade-893f-f0a7d8ae0cfb	7bf5a598-1908-4a34-9ac4-041d3ce99466	COMPLETED	2026-10-18 12:29:00.000047+00	2026-10-18 12:29:00.000047+00	2026-10-19 08:40:00.004126+00	SD-4LVS-XSTC	263 Kotte Road, Kotte	52 Stanley Thilakaratne Mawatha, Nugegoda	Sajith Cooray	LARGE	6.15	DELIVERED
04217771-e9b1-4bd9-8737-8ae68203d1f7	ed0cb627-1361-42ea-92b9-7ff6f68c769e	d4d6a9d7-39f3-45d3-ab66-553e4aee8b84	7bf5a598-1908-4a34-9ac4-041d3ce99466	COMPLETED	2026-10-18 14:19:59.999849+00	2026-10-18 14:19:59.99985+00	2026-10-19 05:57:00.003379+00	SD-WDPF-PTEV	50 York Street, Colombo 01	138 Main Street, Negombo	Ayesha Peiris	LARGE	17.85	DELIVERED
9a720174-d947-4b37-9c7d-f1b9abe795e5	ec7baae6-4fbd-4d86-b8b8-f21d492e6bfb	0175f24c-4200-4492-84f2-030a2c0cc5ca	3900455f-0834-4d57-85c5-c3de92f5ecee	COMPLETED	2026-10-19 05:23:59.999567+00	2026-10-19 05:23:59.999567+00	2026-10-20 07:11:00.003087+00	SD-AWP6-FEBG	334 Galle Road, Dehiwala	284 Kollupitiya Road, Colombo 03	Shehan Perera	SMALL	1.33	DELIVERED
eb1d3466-15b6-4d7b-960d-c6fbbe731d6f	92b020ee-1f1f-4152-b0c3-3f66b9e218df	8b77ede6-e12d-4a24-9795-f3c87c19d2ba	7bf5a598-1908-4a34-9ac4-041d3ce99466	COMPLETED	2026-10-19 06:23:00.000485+00	2026-10-19 06:23:00.000485+00	2026-10-20 05:29:00.002964+00	SD-UJVG-J4HY	205 Maradana Road, Colombo 10	213 Parliament Road, Rajagiriya	Methmi Wickramasinghe	LARGE	7.88	DELIVERED
f4c7e9e5-a890-4215-bfe7-aa58f37d6440	050eaa90-8196-4af6-96ff-aa1ce2241ad3	64dd446f-7e03-421f-a2f0-05db7f2bc36c	cb4cd22c-d4cd-4f2f-ad43-c2e73f1b9cb5	COMPLETED	2026-10-19 10:14:00.000258+00	2026-10-19 10:14:00.000258+00	2026-10-20 08:52:00.003363+00	SD-BPLS-9ADX	340 Wakwella Road, Galle	344 Parliament Road, Rajagiriya	Dinuka Bandara	LARGE	5.37	DELIVERED
09d1671b-63c5-455d-b6e1-1e72ea009eb0	131cabab-560b-46ae-b617-137b6784e2f0	72186f7d-7dd3-4e09-8551-045f309d0a7e	cb4cd22c-d4cd-4f2f-ad43-c2e73f1b9cb5	COMPLETED	2026-10-19 13:28:59.999854+00	2026-10-19 13:28:59.999854+00	2026-10-20 03:41:00.002758+00	SD-7EQU-WLWB	108 Matara Road, Galle	140 Ramakrishna Road, Colombo 06	Chamari Ratnayake	MEDIUM	4.97	DELIVERED
321bce98-5336-4ea7-b657-b723a00bf2fc	f8a028dd-239d-444d-9ae5-965617e62eb5	2c4bbfbc-96d6-4e98-b778-a375c6c2ab0d	cb4cd22c-d4cd-4f2f-ad43-c2e73f1b9cb5	COMPLETED	2026-10-20 04:17:59.999672+00	2026-10-20 04:17:59.999672+00	2026-10-20 12:06:00.003143+00	SD-MZLE-HHY5	317 Thimbirigasyaya Road, Colombo 05	204 Prince of Wales Avenue, Colombo 14	Hashini Ratnayake	LARGE	9.51	DELIVERED
9185c901-1c15-477f-92b2-7fb9f6bd9647	cb75a194-1655-4b94-88ea-85794537614d	86d11ba9-d0e8-4301-8403-9903afbd11a9	3900455f-0834-4d57-85c5-c3de92f5ecee	COMPLETED	2026-10-20 03:59:59.999758+00	2026-10-20 03:59:59.999758+00	2026-10-20 11:35:00.002707+00	SD-5SCN-K7HB	341 Kollupitiya Road, Colombo 03	202 Nawala Road, Rajagiriya	Methmi Jayasinghe	LARGE	17.81	DELIVERED
1c95d998-c24e-41bd-9aac-a6714b3c247d	40fea8a4-1d58-4cd3-b4df-b1f6d0d5d906	d4d6a9d7-39f3-45d3-ab66-553e4aee8b84	cb4cd22c-d4cd-4f2f-ad43-c2e73f1b9cb5	COMPLETED	2026-10-20 09:25:59.999816+00	2026-10-20 09:25:59.999816+00	2026-10-21 07:55:00.003564+00	SD-AHDB-V3RD	76 Peradeniya Road, Kandy	16 Main Street, Colombo 11	Lahiru Fonseka	EXTRA_LARGE	19.40	DELIVERED
d7213795-94b9-42d9-aee7-7c12410994ad	b792a3cd-6df4-43a4-ac4f-c49ddeed3490	7fb36c3a-09c3-4c20-b5b6-d00d8d230cd5	7bf5a598-1908-4a34-9ac4-041d3ce99466	COMPLETED	2026-10-20 08:24:59.999574+00	2026-10-20 08:24:59.999575+00	2026-10-21 07:44:00.002922+00	SD-TESE-Z9MA	39 Prince of Wales Avenue, Colombo 14	95 De Saram Road, Mount Lavinia	Kamal Nawaz	MEDIUM	6.26	DELIVERED
f043b700-b402-417a-93e7-8bb8a8211281	e08139d0-ace6-4476-874f-935379d018cc	8b77ede6-e12d-4a24-9795-f3c87c19d2ba	7bf5a598-1908-4a34-9ac4-041d3ce99466	COMPLETED	2026-10-20 10:23:00.000495+00	2026-10-20 10:23:00.000495+00	2026-10-21 05:13:00.004267+00	SD-V3BQ-SBB7	59 Station Road, Nugegoda	259 High Level Road, Maharagama	Supuni Silva	SMALL	4.47	DELIVERED
e97995ff-049b-4d22-bd4d-b9ca90a3f65a	0508fbe9-83cb-48b5-afbf-794b9472c00a	72186f7d-7dd3-4e09-8551-045f309d0a7e	cb4cd22c-d4cd-4f2f-ad43-c2e73f1b9cb5	COMPLETED	2026-10-20 12:28:59.999627+00	2026-10-20 12:28:59.999628+00	2026-10-21 04:11:00.003599+00	SD-U2U4-8NBR	322 Pita Kotte Road, Kotte	286 Peradeniya Road, Kandy	Sahan Peiris	MEDIUM	6.90	DELIVERED
27f86cd6-c188-421d-a969-5fc0eb1101b7	444709b4-e510-4eef-98b4-d729f9522d79	0c248632-4e7b-4ade-893f-f0a7d8ae0cfb	cb4cd22c-d4cd-4f2f-ad43-c2e73f1b9cb5	COMPLETED	2026-10-20 13:01:00.000024+00	2026-10-20 13:01:00.000024+00	2026-10-21 04:30:00.003209+00	SD-SXWR-FAGT	203 Galle Road, Colombo 03	269 Colombo Road, Negombo	Yasiru Wickramasinghe	SMALL	4.29	DELIVERED
dffb67d1-00ee-4599-ba81-8af3f639c002	d1016b17-21b8-4225-bf3c-acf73a6c2a1f	7fb36c3a-09c3-4c20-b5b6-d00d8d230cd5	cb4cd22c-d4cd-4f2f-ad43-c2e73f1b9cb5	FAILED	2026-10-20 13:28:59.999656+00	2026-10-20 13:28:59.999656+00	2026-10-21 05:10:00.002588+00	SD-6EZN-QJFQ	173 Main Street, Negombo	100 Main Street, Negombo	Fathima Jayawardena	MEDIUM	8.59	FAILED
590c2326-27b7-45fc-a53f-76c23c07dca9	a7081212-3c58-47bf-9deb-82e39801cde0	8b77ede6-e12d-4a24-9795-f3c87c19d2ba	7bf5a598-1908-4a34-9ac4-041d3ce99466	COMPLETED	2026-10-21 03:57:59.999992+00	2026-10-21 03:57:59.999992+00	2026-10-22 06:37:00.004216+00	SD-BZYL-3XFF	189 Galle Road, Colombo 03	13 Baseline Road, Colombo 09	Anjali Rajapaksa	LARGE	13.24	DELIVERED
17ba8320-c76f-4f31-abfe-7edb8b38c023	be41e7a3-dadf-4b4a-a810-0707b3f25aee	0175f24c-4200-4492-84f2-030a2c0cc5ca	cb4cd22c-d4cd-4f2f-ad43-c2e73f1b9cb5	FAILED	2026-10-21 04:19:00.000346+00	2026-10-21 04:19:00.000346+00	2026-10-22 06:39:00.003414+00	SD-4J44-78XC	139 York Street, Colombo 01	222 Colombo Road, Negombo	Malith Rajendran	MEDIUM	3.06	FAILED
cc283b02-bddb-4710-970f-dd0c4af2623a	0a197510-2909-447a-ad06-ca8198998587	64dd446f-7e03-421f-a2f0-05db7f2bc36c	7bf5a598-1908-4a34-9ac4-041d3ce99466	COMPLETED	2026-10-21 05:38:00.000237+00	2026-10-21 05:38:00.000237+00	2026-10-22 06:51:00.002668+00	SD-AVCA-T8D4	75 Kotte Road, Kotte	209 Pita Kotte Road, Kotte	Malith Fonseka	SMALL	3.58	DELIVERED
4e947a7f-5bdf-4a62-9c31-97bfb6432815	5f377826-555f-4879-8944-cd6f0ed5c2ea	7fb36c3a-09c3-4c20-b5b6-d00d8d230cd5	cb4cd22c-d4cd-4f2f-ad43-c2e73f1b9cb5	COMPLETED	2026-10-21 07:08:00.000259+00	2026-10-21 07:08:00.00026+00	2026-10-22 04:21:00.002664+00	SD-F6HP-UPY4	278 De Saram Road, Mount Lavinia	95 Colombo Road, Negombo	Mohamed Fonseka	MEDIUM	2.14	DELIVERED
932c9894-136c-4da2-8dd4-f5538e0c301b	8f7513a7-351e-4eeb-8309-7e2b3bddaa43	d4d6a9d7-39f3-45d3-ab66-553e4aee8b84	3900455f-0834-4d57-85c5-c3de92f5ecee	COMPLETED	2026-10-22 04:18:59.999526+00	2026-10-22 04:18:59.999527+00	2026-10-22 09:30:00.002696+00	SD-8QAC-FJJF	338 Flower Road, Colombo 07	124 Hotel Road, Mount Lavinia	Nipuni Rathnayake	MEDIUM	3.76	DELIVERED
593c4dff-a6af-4117-b946-9f579c96402d	4d99312c-0f55-4fa5-ac56-819fcc497284	0c248632-4e7b-4ade-893f-f0a7d8ae0cfb	3900455f-0834-4d57-85c5-c3de92f5ecee	COMPLETED	2026-10-22 07:17:59.99995+00	2026-10-22 07:17:59.99995+00	2026-10-23 06:44:00.002934+00	SD-WU2L-J76D	325 Station Road, Nugegoda	8 Parliament Road, Rajagiriya	Gayani Rahman	SMALL	3.79	DELIVERED
c38a917e-96a9-4dd1-94f8-7ae8749e3938	2de73874-c077-46fb-8c7b-b497346324bb	0c248632-4e7b-4ade-893f-f0a7d8ae0cfb	7bf5a598-1908-4a34-9ac4-041d3ce99466	COMPLETED	2026-10-22 08:53:00.000235+00	2026-10-22 08:53:00.000235+00	2026-10-23 07:05:00.002816+00	SD-8G3U-ZS3C	274 Marine Drive, Colombo 06	26 Chatham Street, Colombo 01	Dinuka Peiris	SMALL	4.08	DELIVERED
d6e40554-6e13-4be6-a0d2-bd88fca225e7	1763c5b8-1cc3-4081-a5f5-fbd40fdb8d9a	2c4bbfbc-96d6-4e98-b778-a375c6c2ab0d	7bf5a598-1908-4a34-9ac4-041d3ce99466	COMPLETED	2026-10-23 02:57:00.000189+00	2026-10-23 02:57:00.000189+00	2026-10-24 06:19:00.003439+00	SD-CLRM-NDBM	125 Galle Road, Colombo 03	344 Marine Drive, Colombo 06	Sajith Mendis	LARGE	9.04	DELIVERED
5160891c-0242-480d-937e-d373a999b4f3	1c65f745-e5f5-42fa-add5-bc00333a7a95	64dd446f-7e03-421f-a2f0-05db7f2bc36c	7bf5a598-1908-4a34-9ac4-041d3ce99466	COMPLETED	2026-10-23 05:44:00.000407+00	2026-10-23 05:44:00.000408+00	2026-10-24 06:06:00.003054+00	SD-MH7G-DUR4	1 Old Kesbewa Road, Nugegoda	140 Old Kesbewa Road, Nugegoda	Dimuth Rahman	MEDIUM	6.91	DELIVERED
4fab4875-494b-4ab7-8958-2b3d255a280a	6cb54e55-8239-4bae-8532-ee3822bc2148	7fb36c3a-09c3-4c20-b5b6-d00d8d230cd5	7bf5a598-1908-4a34-9ac4-041d3ce99466	COMPLETED	2026-10-23 06:18:00.000467+00	2026-10-23 06:18:00.000468+00	2026-10-24 04:07:00.003517+00	SD-WH8P-7QJA	319 Lauries Road, Colombo 04	224 William Gopallawa Mawatha, Kandy	Kumar Ratnayake	SMALL	4.66	DELIVERED
22bb0e00-395e-4775-b952-fe496f9f8cdd	2bfeb860-2965-4abf-aedd-4fb80353a8ef	0c248632-4e7b-4ade-893f-f0a7d8ae0cfb	7bf5a598-1908-4a34-9ac4-041d3ce99466	COMPLETED	2026-10-23 07:26:59.999756+00	2026-10-23 07:26:59.999756+00	2026-10-24 07:20:00.003703+00	SD-H4F4-TSJA	174 Flower Road, Colombo 07	183 Hotel Road, Mount Lavinia	Dinuka Rajendran	MEDIUM	9.96	DELIVERED
a30ac4b2-b1ee-4421-8933-35e9c7027228	d2aaa9df-1d7f-4725-b50d-ceecd862bc0f	0c248632-4e7b-4ade-893f-f0a7d8ae0cfb	cb4cd22c-d4cd-4f2f-ad43-c2e73f1b9cb5	COMPLETED	2026-10-23 08:57:59.999975+00	2026-10-23 08:57:59.999975+00	2026-10-24 06:08:00.003013+00	SD-WT4V-NL4Z	48 Galle Road, Colombo 04	188 Ward Place, Colombo 07	Lakshmi Ratnayake	LARGE	19.57	DELIVERED
5bb3908f-84f5-42ca-a37f-b8aced2b2a71	2f498a02-e4bb-4e54-8592-d5e1e0ff7331	0175f24c-4200-4492-84f2-030a2c0cc5ca	cb4cd22c-d4cd-4f2f-ad43-c2e73f1b9cb5	COMPLETED	2026-10-23 11:14:00.00012+00	2026-10-23 11:14:00.00012+00	2026-10-24 06:21:00.003343+00	SD-MUHH-3KVA	81 Galle Road, Colombo 04	64 De Saram Road, Mount Lavinia	Malith Samarasinghe	SMALL	4.87	DELIVERED
3a395a08-b186-42b9-af9f-0c04ac2317f4	7773e622-b144-4efc-82a0-455755380182	7fb36c3a-09c3-4c20-b5b6-d00d8d230cd5	3900455f-0834-4d57-85c5-c3de92f5ecee	COMPLETED	2026-10-23 12:51:59.999908+00	2026-10-23 12:51:59.999908+00	2026-10-24 07:46:00.003481+00	SD-494R-ZJEA	68 Pita Kotte Road, Kotte	221 Flower Road, Colombo 07	Gayani Silva	MEDIUM	5.14	DELIVERED
b3829299-5cee-4bf2-a63d-cd2e29ae14a3	0c156b0b-4a0b-4202-a88e-3c105f21b266	86d11ba9-d0e8-4301-8403-9903afbd11a9	3900455f-0834-4d57-85c5-c3de92f5ecee	COMPLETED	2026-10-24 04:01:59.999867+00	2026-10-24 04:01:59.999867+00	2026-10-25 08:29:00.003132+00	SD-23VH-SF38	220 Stanley Thilakaratne Mawatha, Nugegoda	2 Prince of Wales Avenue, Colombo 14	Sachini Samarasinghe	SMALL	2.91	DELIVERED
dc380889-4306-4542-83af-9e926f3a9c0d	43a85fc3-c21f-42a0-a890-993fb94eded6	8b77ede6-e12d-4a24-9795-f3c87c19d2ba	7bf5a598-1908-4a34-9ac4-041d3ce99466	COMPLETED	2026-10-24 06:30:59.999673+00	2026-10-24 06:30:59.999674+00	2026-10-25 04:21:00.004066+00	SD-QSCK-3J6T	65 Flower Road, Colombo 07	321 Kollupitiya Road, Colombo 03	Chathura Gunasekara	SMALL	1.84	DELIVERED
8cb9c219-6bd9-434c-8034-c92a373c6e29	02b30293-e854-43eb-9891-f61c35126104	72186f7d-7dd3-4e09-8551-045f309d0a7e	7bf5a598-1908-4a34-9ac4-041d3ce99466	COMPLETED	2026-10-24 10:29:00.000176+00	2026-10-24 10:29:00.000176+00	2026-10-25 05:43:00.003928+00	SD-2WUQ-5CRJ	119 Pita Kotte Road, Kotte	290 Thimbirigasyaya Road, Colombo 05	Sachini Gunasekara	SMALL	1.94	DELIVERED
f8f2c648-600d-4c8f-a925-60796c6faad9	ba6f0635-a515-4e7c-994a-0b18d0845a6d	2c4bbfbc-96d6-4e98-b778-a375c6c2ab0d	cb4cd22c-d4cd-4f2f-ad43-c2e73f1b9cb5	COMPLETED	2026-10-25 04:01:00.000012+00	2026-10-25 04:01:00.000012+00	2026-10-26 05:46:00.003365+00	SD-VMHT-TEZ9	185 Hill Street, Dehiwala	290 Parliament Road, Rajagiriya	Kavindi Jayasinghe	SMALL	4.59	DELIVERED
c0564f29-d9c7-4de0-9224-92deaa5e1703	c95a9dce-e44a-460a-819f-6332e5d1c171	86d11ba9-d0e8-4301-8403-9903afbd11a9	cb4cd22c-d4cd-4f2f-ad43-c2e73f1b9cb5	COMPLETED	2026-10-25 06:33:59.999771+00	2026-10-25 06:33:59.999772+00	2026-10-26 05:54:00.002927+00	SD-4QLA-KZKL	190 Ward Place, Colombo 07	52 Galle Road, Colombo 03	Dimuth Abeysekera	SMALL	2.44	DELIVERED
b8e0a85f-5c4b-4dbe-be92-27cd6551f27b	8e9255fd-18ce-43c3-96d1-4e07c31fd2fb	0c248632-4e7b-4ade-893f-f0a7d8ae0cfb	cb4cd22c-d4cd-4f2f-ad43-c2e73f1b9cb5	COMPLETED	2026-10-25 08:25:00.000371+00	2026-10-25 08:25:00.000371+00	2026-10-26 06:32:00.003228+00	SD-YVUP-LTK9	337 Kotte Road, Kotte	130 Lewis Place, Negombo	Fathima Rajendran	MEDIUM	6.46	DELIVERED
8a67dfe3-f811-462f-94c5-11bc6d9e2e70	d32b0434-ee3c-4980-be83-a8a6d68203b6	72186f7d-7dd3-4e09-8551-045f309d0a7e	7bf5a598-1908-4a34-9ac4-041d3ce99466	COMPLETED	2026-10-25 10:17:59.999782+00	2026-10-25 10:17:59.999783+00	2026-10-26 05:20:00.002728+00	SD-3NDH-EB2R	299 Pita Kotte Road, Kotte	46 Old Kesbewa Road, Nugegoda	Rizwan Weerasinghe	MEDIUM	4.65	DELIVERED
798641cf-b763-45ac-bfb8-bf5517ef65dd	70e418dc-b6eb-4c90-b7fb-8f2a11852b3f	8b77ede6-e12d-4a24-9795-f3c87c19d2ba	7bf5a598-1908-4a34-9ac4-041d3ce99466	COMPLETED	2026-10-25 11:30:00.000298+00	2026-10-25 11:30:00.000298+00	2026-10-26 05:06:00.002196+00	SD-7LY4-746E	286 Union Place, Colombo 02	51 Baseline Road, Colombo 09	Shehan Dissanayake	SMALL	1.00	DELIVERED
06674bd7-6d22-4058-8c9f-5b9a1f9e10b6	1b1897ff-02b8-4c03-a8ed-5dca0cb800ad	7fb36c3a-09c3-4c20-b5b6-d00d8d230cd5	3900455f-0834-4d57-85c5-c3de92f5ecee	COMPLETED	2026-10-25 11:53:59.99989+00	2026-10-25 11:53:59.999891+00	2026-10-26 04:30:00.003079+00	SD-9H78-WFHA	344 De Saram Road, Mount Lavinia	143 De Saram Road, Mount Lavinia	Chathura Silva	MEDIUM	4.66	DELIVERED
9ba7a679-8dca-4969-89d6-e47d34cf4d12	d341eeeb-fafa-4cd9-85cf-70c87bdcd596	2c4bbfbc-96d6-4e98-b778-a375c6c2ab0d	7bf5a598-1908-4a34-9ac4-041d3ce99466	COMPLETED	2026-10-26 08:05:59.999855+00	2026-10-26 08:05:59.999855+00	2026-10-27 06:33:00.002856+00	SD-HRDF-J5ML	286 Old Kesbewa Road, Nugegoda	31 Galle Road, Colombo 03	Lakshmi Karunaratne	LARGE	5.70	DELIVERED
abee985a-d2ed-47e5-8fe9-3d4e314cf0d0	ca5bbd7b-e200-4e10-bc55-a5d3d4841463	0c248632-4e7b-4ade-893f-f0a7d8ae0cfb	3900455f-0834-4d57-85c5-c3de92f5ecee	COMPLETED	2026-10-26 11:53:00.000281+00	2026-10-26 11:53:00.000281+00	2026-10-27 07:38:00.003454+00	SD-66DN-8BAZ	270 De Saram Road, Mount Lavinia	275 Lewis Place, Negombo	Hiruni Peiris	SMALL	1.38	DELIVERED
ea01d846-c523-40dc-ba39-e4515ca7ea67	94b83c16-3dd0-4ee6-9291-6ed0a801d173	64dd446f-7e03-421f-a2f0-05db7f2bc36c	7bf5a598-1908-4a34-9ac4-041d3ce99466	COMPLETED	2026-10-26 13:21:00.000455+00	2026-10-26 13:21:00.000455+00	2026-10-27 07:20:00.002681+00	SD-2MUG-X59Q	91 Prince of Wales Avenue, Colombo 14	187 York Street, Colombo 01	Ravindu Amarasekara	SMALL	1.71	DELIVERED
7a4d50f6-2c12-4181-9efb-7baf725d3d4e	1e9ca30f-6e43-4d05-adcb-e6cb8ad934d6	8b77ede6-e12d-4a24-9795-f3c87c19d2ba	cb4cd22c-d4cd-4f2f-ad43-c2e73f1b9cb5	COMPLETED	2026-10-26 14:25:00.000254+00	2026-10-26 14:25:00.000254+00	2026-10-27 06:20:00.002738+00	SD-DNBM-52V8	324 Galle Road, Colombo 04	48 Union Place, Colombo 02	Nipuni Rahman	MEDIUM	9.48	DELIVERED
9b068b39-40c6-4cde-b0b5-a0a57ba9d975	004cd5c7-98cc-49d7-9870-8b9425a9f710	0c248632-4e7b-4ade-893f-f0a7d8ae0cfb	3900455f-0834-4d57-85c5-c3de92f5ecee	COMPLETED	2026-10-27 04:11:59.999592+00	2026-10-27 04:11:59.999592+00	2026-10-28 06:36:00.002689+00	SD-YCER-E83U	78 Peradeniya Road, Kandy	144 Prince of Wales Avenue, Colombo 14	Pradeep Jayawardena	MEDIUM	4.92	DELIVERED
1cb9b730-9435-4fa3-b889-8f02ee5bcc03	21ed97d5-f357-4d1c-99aa-dca05bb0b955	0175f24c-4200-4492-84f2-030a2c0cc5ca	3900455f-0834-4d57-85c5-c3de92f5ecee	FAILED	2026-10-27 05:08:59.999518+00	2026-10-27 05:08:59.999518+00	2026-10-27 11:12:00.002116+00	SD-F62P-XJ55	158 Hotel Road, Mount Lavinia	7 High Level Road, Maharagama	Lakshmi Nawaz	LARGE	14.38	FAILED
7c726660-84ef-4508-9708-c5184f912f4e	afa58c9e-b283-457e-a6d3-b1337765413c	8b77ede6-e12d-4a24-9795-f3c87c19d2ba	7bf5a598-1908-4a34-9ac4-041d3ce99466	COMPLETED	2026-10-27 06:04:59.999597+00	2026-10-27 06:04:59.999598+00	2026-10-28 07:20:00.002517+00	SD-PGYL-XUKF	237 Stanley Thilakaratne Mawatha, Nugegoda	107 Havelock Road, Colombo 05	Nadeesha Fonseka	SMALL	2.28	DELIVERED
623387ec-8168-4990-8c6f-66fa467055d7	b9555759-225f-462a-8ee9-f29f034368db	d4d6a9d7-39f3-45d3-ab66-553e4aee8b84	3900455f-0834-4d57-85c5-c3de92f5ecee	COMPLETED	2026-10-27 11:04:00.000353+00	2026-10-27 11:04:00.000353+00	2026-10-28 05:34:00.004222+00	SD-VGV5-HX4V	341 Old Kesbewa Road, Nugegoda	299 Old Kesbewa Road, Nugegoda	Kamal Gunasekara	SMALL	0.90	DELIVERED
2d1467b1-1fd4-4ca2-9ea7-74827ef13d5e	a4ac2588-9fe5-409d-be41-bcbe19e04599	0c248632-4e7b-4ade-893f-f0a7d8ae0cfb	7bf5a598-1908-4a34-9ac4-041d3ce99466	COMPLETED	2026-10-27 14:02:00.000131+00	2026-10-27 14:02:00.000131+00	2026-10-27 22:12:00.003769+00	SD-98XX-KXW9	206 Hill Street, Dehiwala	57 Maradana Road, Colombo 10	Senuri Senanayake	SMALL	4.78	DELIVERED
0389961a-ca07-45a3-9a7f-6fdea4af0775	2b41e29e-1bf0-48c4-897d-838f4d708c27	64dd446f-7e03-421f-a2f0-05db7f2bc36c	cb4cd22c-d4cd-4f2f-ad43-c2e73f1b9cb5	COMPLETED	2026-10-28 03:17:59.999951+00	2026-10-28 03:17:59.999952+00	2026-10-29 03:55:00.003752+00	SD-LKZ6-P3CH	334 Thimbirigasyaya Road, Colombo 05	96 Kollupitiya Road, Colombo 03	Sajith Gunasekara	SMALL	1.90	DELIVERED
11bb8cfa-4977-4dec-8865-70dcad7538f3	b70a0bbe-0ecf-461b-b8d1-c883a11c3296	0c248632-4e7b-4ade-893f-f0a7d8ae0cfb	cb4cd22c-d4cd-4f2f-ad43-c2e73f1b9cb5	COMPLETED	2026-10-28 07:26:59.999714+00	2026-10-28 07:26:59.999714+00	2026-10-29 06:13:00.003579+00	SD-3M2A-NC3Q	278 Galle Road, Dehiwala	330 Parliament Road, Rajagiriya	Rizwan Nawaz	LARGE	8.50	DELIVERED
3fdd598f-6d28-41f7-b731-1eccfea182e2	04dff3f0-25ba-4dfe-89af-f10d3b7502bf	2c4bbfbc-96d6-4e98-b778-a375c6c2ab0d	7bf5a598-1908-4a34-9ac4-041d3ce99466	COMPLETED	2026-10-28 10:41:00.00042+00	2026-10-28 10:41:00.00042+00	2026-10-29 04:56:00.003737+00	SD-LQF7-3NDY	232 Flower Road, Colombo 07	47 Thimbirigasyaya Road, Colombo 05	Senuri Dissanayake	SMALL	2.97	DELIVERED
7931c624-33f9-4a98-82b5-238813ff03d3	cf7bdbfe-1ac9-489c-956c-4ea20cdd1238	7fb36c3a-09c3-4c20-b5b6-d00d8d230cd5	cb4cd22c-d4cd-4f2f-ad43-c2e73f1b9cb5	COMPLETED	2026-10-28 12:24:59.999619+00	2026-10-28 12:24:59.999619+00	2026-10-29 07:20:00.002891+00	SD-WN9K-9H4D	307 De Saram Road, Mount Lavinia	288 Stanley Thilakaratne Mawatha, Nugegoda	Lakshmi Fonseka	SMALL	4.49	DELIVERED
191f4c1e-e280-4314-ac07-a345a2ef59b5	50932f3f-039d-4401-ac94-21fe57203e68	64dd446f-7e03-421f-a2f0-05db7f2bc36c	7bf5a598-1908-4a34-9ac4-041d3ce99466	FAILED	2026-10-28 13:34:59.9999+00	2026-10-28 13:34:59.9999+00	2026-10-29 04:26:00.002841+00	SD-YAN6-JVWB	86 De Saram Road, Mount Lavinia	27 Kotte Road, Kotte	Anjali Amarasekara	EXTRA_LARGE	17.07	FAILED
4a392377-d200-4bc1-aacb-1182b7f410a8	b17d31cd-7d1a-4180-845b-91890605a6a3	86d11ba9-d0e8-4301-8403-9903afbd11a9	3900455f-0834-4d57-85c5-c3de92f5ecee	COMPLETED	2026-10-28 13:07:00.000163+00	2026-10-28 13:07:00.000164+00	2026-10-29 07:44:00.002762+00	SD-ZKKW-ZV6M	38 Flower Road, Colombo 07	244 Station Road, Nugegoda	Nadeesha Karunaratne	MEDIUM	5.94	DELIVERED
87002e06-b4b7-45a9-b046-1beb855d24a2	0064c98a-e8c7-4409-abfd-9fcfd1afd81a	86d11ba9-d0e8-4301-8403-9903afbd11a9	cb4cd22c-d4cd-4f2f-ad43-c2e73f1b9cb5	COMPLETED	2026-10-29 07:53:00.000137+00	2026-10-29 07:53:00.000138+00	2026-10-30 07:11:00.002446+00	SD-WEA3-L7AS	251 De Saram Road, Mount Lavinia	303 Station Road, Nugegoda	Sanduni Dissanayake	MEDIUM	8.49	DELIVERED
76c04b61-bf48-4489-95c1-dbf225def8cd	b4c169f9-f1f4-495e-a97c-6aa076f92a4c	64dd446f-7e03-421f-a2f0-05db7f2bc36c	7bf5a598-1908-4a34-9ac4-041d3ce99466	COMPLETED	2026-10-29 07:50:59.999705+00	2026-10-29 07:50:59.999705+00	2026-10-30 09:05:00.00306+00	SD-A3G8-BT77	192 Station Road, Nugegoda	140 Thimbirigasyaya Road, Colombo 05	Chamari Liyanage	SMALL	3.28	DELIVERED
1831988e-456d-4c24-93f1-2f4199db84d6	f6eca05b-c7b7-4363-8bfc-f71de14c8940	0c248632-4e7b-4ade-893f-f0a7d8ae0cfb	7bf5a598-1908-4a34-9ac4-041d3ce99466	COMPLETED	2026-10-29 08:31:00.000185+00	2026-10-29 08:31:00.000185+00	2026-10-30 06:41:00.003351+00	SD-HF45-X2LP	120 Galle Road, Colombo 03	160 William Gopallawa Mawatha, Kandy	Amaya Weerasinghe	MEDIUM	9.67	DELIVERED
be3998fa-93dd-4756-9cc9-0a166cd01966	6aa91c49-e517-4b10-8768-646d31273bb2	8b77ede6-e12d-4a24-9795-f3c87c19d2ba	cb4cd22c-d4cd-4f2f-ad43-c2e73f1b9cb5	COMPLETED	2026-10-29 12:05:00.000223+00	2026-10-29 12:05:00.000223+00	2026-10-30 04:17:00.003537+00	SD-WB4E-76Z2	40 High Level Road, Maharagama	60 Galle Road, Dehiwala	Tharindu Gunasekara	MEDIUM	4.15	DELIVERED
f34716d9-5c57-4378-9a5a-b84f821cfea2	bebf00f2-7419-4ccb-a2ec-d90c1a0a69eb	72186f7d-7dd3-4e09-8551-045f309d0a7e	3900455f-0834-4d57-85c5-c3de92f5ecee	COMPLETED	2026-10-30 05:21:59.999761+00	2026-10-30 05:21:59.999761+00	2026-10-31 05:54:00.002405+00	SD-9JEK-RJ3X	210 Old Kesbewa Road, Nugegoda	312 Chatham Street, Colombo 01	Arun Jayawardena	SMALL	4.06	DELIVERED
04ba0473-a31b-4a0b-907d-0b4137c8ef88	7c4283b3-e8af-4476-a8a1-9a3787eec8af	7fb36c3a-09c3-4c20-b5b6-d00d8d230cd5	3900455f-0834-4d57-85c5-c3de92f5ecee	COMPLETED	2026-10-30 05:19:00.000302+00	2026-10-30 05:19:00.000302+00	2026-10-31 05:29:00.002984+00	SD-G2GQ-BKZZ	279 De Saram Road, Mount Lavinia	106 Maradana Road, Colombo 10	Shehan Liyanage	MEDIUM	9.06	DELIVERED
ef3570e0-fe57-4b07-8c24-01dfff79d2d9	b1e52b10-a127-4b71-801b-5e1ed987f3ab	64dd446f-7e03-421f-a2f0-05db7f2bc36c	3900455f-0834-4d57-85c5-c3de92f5ecee	COMPLETED	2026-10-30 08:16:00.000385+00	2026-10-30 08:16:00.000385+00	2026-10-31 04:46:00.002988+00	SD-QS67-BRPA	130 Lauries Road, Colombo 04	225 Stanley Thilakaratne Mawatha, Nugegoda	Mohamed Silva	SMALL	0.83	DELIVERED
fe08b028-b3fc-4ddc-bec8-8d44f9b8f197	a6443ff5-a591-4556-87f9-7ac872dd0dcb	d4d6a9d7-39f3-45d3-ab66-553e4aee8b84	cb4cd22c-d4cd-4f2f-ad43-c2e73f1b9cb5	COMPLETED	2026-10-30 10:09:59.999845+00	2026-10-30 10:09:59.999845+00	2026-10-31 07:18:00.002276+00	SD-5R5Y-DC5M	219 York Street, Colombo 01	130 Chatham Street, Colombo 01	Oshada Wijesinghe	MEDIUM	4.28	DELIVERED
b2a67ad2-9f39-451c-a8ba-ed4b5d6bf6bb	b50c78a0-2621-4b70-bf0d-beef45e53b9c	d4d6a9d7-39f3-45d3-ab66-553e4aee8b84	7bf5a598-1908-4a34-9ac4-041d3ce99466	COMPLETED	2026-10-30 10:08:59.999797+00	2026-10-30 10:08:59.999797+00	2026-10-31 05:58:00.003634+00	SD-SGRN-485X	59 Flower Road, Colombo 07	293 Kotte Road, Kotte	Gayani Nawaz	LARGE	11.24	DELIVERED
40392804-efee-49af-9390-f89482803274	3e2a2e67-eaf1-4c1c-b2ca-0fa319c4a2f6	2c4bbfbc-96d6-4e98-b778-a375c6c2ab0d	3900455f-0834-4d57-85c5-c3de92f5ecee	COMPLETED	2026-10-30 12:36:59.999596+00	2026-10-30 12:36:59.999597+00	2026-10-31 06:59:00.00239+00	SD-TC2W-86SG	229 Maradana Road, Colombo 10	217 Marine Drive, Colombo 06	Dulani Liyanage	LARGE	16.35	DELIVERED
a02374ac-a3f1-4628-843f-a1330659b307	1f105fcf-f890-44b2-871f-19127e948f1d	64dd446f-7e03-421f-a2f0-05db7f2bc36c	7bf5a598-1908-4a34-9ac4-041d3ce99466	COMPLETED	2026-10-31 07:14:00.000337+00	2026-10-31 07:14:00.000338+00	2026-11-01 04:29:00.00357+00	SD-XWP5-NSDP	120 Hill Street, Dehiwala	325 Havelock Road, Colombo 05	Arun Perera	SMALL	3.02	DELIVERED
0ee96b28-af14-43e9-9584-c379cd72f436	fe94d603-c28a-45b0-9205-48a64a9031d9	0c248632-4e7b-4ade-893f-f0a7d8ae0cfb	cb4cd22c-d4cd-4f2f-ad43-c2e73f1b9cb5	COMPLETED	2026-10-31 07:39:00.000382+00	2026-10-31 07:39:00.000382+00	2026-11-01 05:08:00.003336+00	SD-R8VP-M767	306 Galle Road, Dehiwala	175 De Saram Road, Mount Lavinia	Kumar Samarasinghe	SMALL	4.12	DELIVERED
b75d0256-7baf-4655-88cb-9e2e179a8e42	47904eaf-6777-4872-8837-ba922a7de5f5	72186f7d-7dd3-4e09-8551-045f309d0a7e	3900455f-0834-4d57-85c5-c3de92f5ecee	COMPLETED	2026-10-31 07:48:00.000464+00	2026-10-31 07:48:00.000464+00	2026-11-01 07:12:00.003195+00	SD-X4CW-4A8Q	188 Station Road, Nugegoda	14 Peradeniya Road, Kandy	Ishara Cooray	EXTRA_LARGE	34.60	DELIVERED
cb557e2b-2428-444a-a6f7-741de7ff099e	1fd30a73-5d6b-4b25-88b1-ccd8ac1c403e	64dd446f-7e03-421f-a2f0-05db7f2bc36c	7bf5a598-1908-4a34-9ac4-041d3ce99466	COMPLETED	2026-10-31 07:52:00.000338+00	2026-10-31 07:52:00.000339+00	2026-11-01 05:18:00.004105+00	SD-92PT-XHLN	168 Parliament Road, Rajagiriya	118 Maradana Road, Colombo 10	Ayesha Rajendran	SMALL	4.16	DELIVERED
b78d2e8f-c87b-4c53-8a56-da5ab41fc0b0	53010fcb-dfe0-4458-a8a9-b1ac8492d79b	0c248632-4e7b-4ade-893f-f0a7d8ae0cfb	3900455f-0834-4d57-85c5-c3de92f5ecee	COMPLETED	2026-10-31 08:55:00.000181+00	2026-10-31 08:55:00.000181+00	2026-11-01 04:30:00.003904+00	SD-3D9A-KSRK	28 Nawala Road, Rajagiriya	194 Ward Place, Colombo 07	Kumar Senanayake	SMALL	4.29	DELIVERED
cd173eaa-e876-4f8f-9c74-96c011a166eb	aaedd4ae-856d-4593-a878-c5ae4e62a809	2c4bbfbc-96d6-4e98-b778-a375c6c2ab0d	7bf5a598-1908-4a34-9ac4-041d3ce99466	COMPLETED	2026-10-31 09:16:59.999911+00	2026-10-31 09:16:59.999911+00	2026-11-01 07:25:00.002609+00	SD-9RMJ-HGQ2	10 High Level Road, Maharagama	270 Matara Road, Galle	Nadeesha de Silva	LARGE	11.90	DELIVERED
20495e3f-34a7-45cb-bf0d-f52dcbd8381c	71743040-3bd3-4bb3-b8cc-25a90c2b6bf9	0c248632-4e7b-4ade-893f-f0a7d8ae0cfb	cb4cd22c-d4cd-4f2f-ad43-c2e73f1b9cb5	COMPLETED	2026-10-31 12:56:59.999641+00	2026-10-31 12:56:59.999641+00	2026-11-01 07:00:00.002853+00	SD-SDG4-A9MM	335 Nawala Road, Rajagiriya	54 De Saram Road, Mount Lavinia	Ravindu Karunaratne	MEDIUM	3.65	DELIVERED
52c05ee6-2b02-476c-ba57-ba3ac8981dbd	19d25d61-ce89-4229-8223-a1388347b7a3	d4d6a9d7-39f3-45d3-ab66-553e4aee8b84	7bf5a598-1908-4a34-9ac4-041d3ce99466	COMPLETED	2026-10-31 14:58:59.999505+00	2026-10-31 14:58:59.999505+00	2026-11-02 09:08:00.003131+00	SD-FULS-P3G7	159 Old Kesbewa Road, Nugegoda	267 Dalada Veediya, Kandy	Oshada Wickramasinghe	LARGE	7.63	DELIVERED
25aa4432-a405-445b-a9af-ba5d99114bb1	56c0ce5b-cf69-4417-9776-2e1da9f95446	0c248632-4e7b-4ade-893f-f0a7d8ae0cfb	cb4cd22c-d4cd-4f2f-ad43-c2e73f1b9cb5	COMPLETED	2026-11-01 06:27:59.99964+00	2026-11-01 06:27:59.99964+00	2026-11-02 03:45:00.002056+00	SD-VCNB-FVM2	102 Station Road, Nugegoda	26 Chatham Street, Colombo 01	Nadeesha Rahman	MEDIUM	6.50	DELIVERED
a7f5bc95-086b-45d0-826f-d525c3d91c46	b00f9ac4-2e1f-4b00-b4ff-3a5966a2fb2b	2c4bbfbc-96d6-4e98-b778-a375c6c2ab0d	7bf5a598-1908-4a34-9ac4-041d3ce99466	COMPLETED	2026-11-01 07:40:59.999718+00	2026-11-01 07:40:59.999718+00	2026-11-02 04:52:00.003345+00	SD-HEB8-AANT	301 York Street, Colombo 01	293 High Level Road, Maharagama	Rizwan Amarasekara	MEDIUM	7.92	DELIVERED
8241dc4e-69c6-4503-a5d0-d47cf5eefcc4	c37a9a27-7fd7-4bc3-b486-a26c7914af53	0c248632-4e7b-4ade-893f-f0a7d8ae0cfb	cb4cd22c-d4cd-4f2f-ad43-c2e73f1b9cb5	COMPLETED	2026-11-01 09:35:00.000167+00	2026-11-01 09:35:00.000167+00	2026-11-02 05:55:00.002616+00	SD-TC22-JBAR	93 Galle Road, Colombo 03	277 Hotel Road, Mount Lavinia	Kavindi Cooray	SMALL	1.76	DELIVERED
86b25937-cab1-4cad-8441-71ab819fbdb8	c8b0ca2a-4c5a-43ab-b25a-ddd2986dd15d	2c4bbfbc-96d6-4e98-b778-a375c6c2ab0d	7bf5a598-1908-4a34-9ac4-041d3ce99466	COMPLETED	2026-11-01 08:47:59.999726+00	2026-11-01 08:47:59.999726+00	2026-11-02 04:22:00.002537+00	SD-SUZ4-R4NA	257 Stanley Thilakaratne Mawatha, Nugegoda	116 Hotel Road, Mount Lavinia	Sajith Rahman	MEDIUM	9.58	DELIVERED
1aeb83cd-ad37-4f46-94b0-34cf94ba9ff4	fdba7a88-2d12-4d1b-979c-8dba65aefad3	d4d6a9d7-39f3-45d3-ab66-553e4aee8b84	cb4cd22c-d4cd-4f2f-ad43-c2e73f1b9cb5	COMPLETED	2026-11-02 04:56:59.999797+00	2026-11-02 04:56:59.999797+00	2026-11-03 07:06:00.002154+00	SD-8RDY-DZS6	95 Dalada Veediya, Kandy	208 Maradana Road, Colombo 10	Ruwan Liyanage	SMALL	3.36	DELIVERED
e55984d2-0450-4c14-a126-a17ee7db14e5	131bef04-41da-47db-8142-e3a43f7f145e	2c4bbfbc-96d6-4e98-b778-a375c6c2ab0d	cb4cd22c-d4cd-4f2f-ad43-c2e73f1b9cb5	FAILED	2026-11-02 06:42:00.000409+00	2026-11-02 06:42:00.000409+00	2026-11-02 14:13:00.00183+00	SD-M4FS-KW3G	227 Flower Road, Colombo 07	199 Thimbirigasyaya Road, Colombo 05	Arun Herath	EXTRA_LARGE	10.34	FAILED
3a4b7461-8dcb-4bfc-a75b-49c48f8e9b44	230f1664-6f7c-40b2-929e-5f53e3f6d42c	72186f7d-7dd3-4e09-8551-045f309d0a7e	cb4cd22c-d4cd-4f2f-ad43-c2e73f1b9cb5	COMPLETED	2026-11-02 08:14:00.000251+00	2026-11-02 08:14:00.000251+00	2026-11-03 08:09:00.002342+00	SD-GW8C-RCQN	32 Duplication Road, Colombo 03	218 Galle Road, Colombo 03	Ishara Abeysekera	SMALL	0.61	DELIVERED
9dc4a845-b068-4d9c-a6fb-8a184c9aa4e5	f5200a40-bd35-42f9-9df7-5516f58e2a1b	86d11ba9-d0e8-4301-8403-9903afbd11a9	cb4cd22c-d4cd-4f2f-ad43-c2e73f1b9cb5	COMPLETED	2026-11-02 13:03:59.99955+00	2026-11-02 13:03:59.99955+00	2026-11-03 04:53:00.003005+00	SD-YP6R-6CAY	230 York Street, Colombo 01	108 Pita Kotte Road, Kotte	Chathura Fonseka	MEDIUM	5.33	DELIVERED
0a509ec9-5fb1-4466-8fbb-58efa350d520	5eea5cae-0d6c-4f25-8877-b4726e78eafb	0175f24c-4200-4492-84f2-030a2c0cc5ca	cb4cd22c-d4cd-4f2f-ad43-c2e73f1b9cb5	COMPLETED	2026-11-02 14:29:00.000497+00	2026-11-02 14:29:00.000497+00	2026-11-04 06:01:00.002333+00	SD-SBZR-ULZ5	82 Pita Kotte Road, Kotte	131 Church Street, Galle Fort	Kasun Perera	MEDIUM	5.56	DELIVERED
dd219a33-aa6c-4f90-983d-cf37a872709a	ecab594c-e3c1-4260-a789-4fe38e574905	64dd446f-7e03-421f-a2f0-05db7f2bc36c	3900455f-0834-4d57-85c5-c3de92f5ecee	COMPLETED	2026-11-03 04:40:59.999757+00	2026-11-03 04:40:59.999757+00	2026-11-03 12:41:00.002682+00	SD-V9DY-JSKK	150 Baseline Road, Colombo 09	36 Lauries Road, Colombo 04	Isuru Rahman	MEDIUM	5.67	DELIVERED
13dfe99a-93ab-4f36-99ff-92f9b384d5d5	4b9cf182-3371-43fa-9404-f880d66160a3	86d11ba9-d0e8-4301-8403-9903afbd11a9	7bf5a598-1908-4a34-9ac4-041d3ce99466	FAILED	2026-11-03 06:50:00.000101+00	2026-11-03 06:50:00.000101+00	2026-11-03 10:49:00.002209+00	SD-P8V3-FCGP	281 Station Road, Nugegoda	139 Havelock Road, Colombo 05	Sunil Liyanage	SMALL	0.83	FAILED
3395dd7b-1937-41e3-af87-4b62c236be53	1f259682-1a63-48d3-9666-be7ec0adb8af	8b77ede6-e12d-4a24-9795-f3c87c19d2ba	7bf5a598-1908-4a34-9ac4-041d3ce99466	COMPLETED	2026-11-03 09:23:00.000047+00	2026-11-03 09:23:00.000047+00	2026-11-04 05:16:00.002402+00	SD-4LYW-G47P	263 Kotte Road, Kotte	160 Galle Road, Colombo 03	Senuri Weerasinghe	SMALL	4.50	DELIVERED
4593a69b-1b04-4be3-88f5-2bf2dcd55036	fdcb6d35-bb5d-4f87-896f-37b2662841a8	86d11ba9-d0e8-4301-8403-9903afbd11a9	cb4cd22c-d4cd-4f2f-ad43-c2e73f1b9cb5	COMPLETED	2026-11-03 10:46:00.00004+00	2026-11-03 10:46:00.00004+00	2026-11-04 05:46:00.002917+00	SD-7DUG-UEZ5	198 High Level Road, Maharagama	183 Ramakrishna Road, Colombo 06	Shehan Peiris	MEDIUM	9.91	DELIVERED
11eca17d-9beb-4fb1-a9cc-be5abd4a8575	b3499683-fd80-486d-b9ef-dcf3a9af72ea	0175f24c-4200-4492-84f2-030a2c0cc5ca	7bf5a598-1908-4a34-9ac4-041d3ce99466	COMPLETED	2026-11-03 13:40:00.000277+00	2026-11-03 13:40:00.000278+00	2026-11-04 06:11:00.002943+00	SD-KWSR-RPQS	255 Pita Kotte Road, Kotte	113 Marine Drive, Colombo 06	Arun Wickramasinghe	SMALL	1.63	DELIVERED
37b1289b-979a-4e0f-9c1f-70d7db6ac3c2	e315e353-9a97-4b4e-94c4-b5f8577b5194	0175f24c-4200-4492-84f2-030a2c0cc5ca	cb4cd22c-d4cd-4f2f-ad43-c2e73f1b9cb5	COMPLETED	2026-11-03 14:22:59.99953+00	2026-11-03 14:22:59.99953+00	2026-11-05 08:50:00.002747+00	SD-WGG2-QNCZ	78 Hill Street, Dehiwala	43 Hotel Road, Mount Lavinia	Tharindu Karunaratne	LARGE	16.49	DELIVERED
f4012391-02fb-4938-bf8b-004a07f67ef1	d1295ba2-f82f-4f08-b96e-a4b4e329ddb8	0c248632-4e7b-4ade-893f-f0a7d8ae0cfb	3900455f-0834-4d57-85c5-c3de92f5ecee	COMPLETED	2026-11-03 14:53:59.999505+00	2026-11-03 14:53:59.999505+00	2026-11-04 04:42:00.003229+00	SD-DPTF-LSAU	316 Parliament Road, Rajagiriya	240 Kotte Road, Kotte	Fathima Rathnayake	SMALL	3.53	DELIVERED
1d2e0a95-2077-45a8-9bf6-1e14809f7fc0	21eb2515-589c-4e35-b82c-8e7011f1d135	2c4bbfbc-96d6-4e98-b778-a375c6c2ab0d	7bf5a598-1908-4a34-9ac4-041d3ce99466	COMPLETED	2026-11-04 06:08:59.999973+00	2026-11-04 06:08:59.999973+00	2026-11-05 06:54:00.002451+00	SD-3Z4G-8ZYZ	16 Galle Road, Colombo 03	342 York Street, Colombo 01	Gayani Samarasinghe	LARGE	19.13	DELIVERED
ba13bec4-fc20-4766-8073-79117ed6f12c	61cd2ea3-1434-447d-b127-089b1ab760f2	0c248632-4e7b-4ade-893f-f0a7d8ae0cfb	3900455f-0834-4d57-85c5-c3de92f5ecee	COMPLETED	2026-11-04 05:45:00.000132+00	2026-11-04 05:45:00.000132+00	2026-11-05 08:02:00.002842+00	SD-SN8H-CJKJ	185 Galle Road, Colombo 03	173 High Level Road, Maharagama	Malith Abeysekera	SMALL	1.56	DELIVERED
7ae900df-ad90-4595-bcb8-4334491bdf1e	0cbfe5a0-86e8-4640-a947-b19ae0212264	64dd446f-7e03-421f-a2f0-05db7f2bc36c	3900455f-0834-4d57-85c5-c3de92f5ecee	COMPLETED	2026-11-04 06:11:00.000301+00	2026-11-04 06:11:00.000301+00	2026-11-05 06:09:00.002924+00	SD-KHDP-Z6FL	16 Stanley Thilakaratne Mawatha, Nugegoda	171 Lauries Road, Colombo 04	Amaya Abeysekera	SMALL	1.38	DELIVERED
4c1f233a-2c0c-4c6e-8117-4d48edde8aa6	eaec91ca-ad92-4556-aad5-d8c2a40e21d3	2c4bbfbc-96d6-4e98-b778-a375c6c2ab0d	3900455f-0834-4d57-85c5-c3de92f5ecee	COMPLETED	2026-11-04 06:50:00.000323+00	2026-11-04 06:50:00.000323+00	2026-11-05 08:40:00.002522+00	SD-WU6B-PQRH	19 Galle Road, Colombo 04	278 Main Street, Negombo	Sachini Weerasinghe	SMALL	3.34	DELIVERED
2e6cec97-9b5c-4759-8f32-cb64606d4f78	1b09b4a8-6d73-4807-b509-5ac061b14797	d4d6a9d7-39f3-45d3-ab66-553e4aee8b84	3900455f-0834-4d57-85c5-c3de92f5ecee	COMPLETED	2026-11-04 11:37:59.999651+00	2026-11-04 11:37:59.999651+00	2026-11-05 06:38:00.002066+00	SD-LPYY-3RWQ	45 Hotel Road, Mount Lavinia	335 Old Kesbewa Road, Nugegoda	Mohamed Cooray	MEDIUM	2.03	DELIVERED
a9a51607-8c1a-4cba-8887-fdac21feff8a	c01869d2-93d9-4fb2-ab6b-7fb374a65b57	7fb36c3a-09c3-4c20-b5b6-d00d8d230cd5	3900455f-0834-4d57-85c5-c3de92f5ecee	COMPLETED	2026-11-04 14:49:59.999721+00	2026-11-04 14:49:59.999721+00	2026-11-05 05:25:00.001917+00	SD-BQLN-Y7HQ	29 Pita Kotte Road, Kotte	336 Stanley Thilakaratne Mawatha, Nugegoda	Hiruni Dissanayake	SMALL	2.31	DELIVERED
964d670c-924d-4159-9f72-5ca7a04bfbbe	90566894-0673-42cc-a998-4799f4d88b2b	2c4bbfbc-96d6-4e98-b778-a375c6c2ab0d	7bf5a598-1908-4a34-9ac4-041d3ce99466	COMPLETED	2026-11-05 05:05:59.999795+00	2026-11-05 05:05:59.999795+00	2026-11-06 06:38:00.001979+00	SD-AKWN-GRA5	174 Main Street, Colombo 11	131 Lewis Place, Negombo	Dinuka Fernando	MEDIUM	4.44	DELIVERED
b33a1ff6-e1f0-4a43-b3df-705dda5290e1	8c437d4a-330c-4024-bc85-ac7a95add447	8b77ede6-e12d-4a24-9795-f3c87c19d2ba	3900455f-0834-4d57-85c5-c3de92f5ecee	COMPLETED	2026-11-05 08:54:00.000167+00	2026-11-05 08:54:00.000167+00	2026-11-05 13:51:00.001853+00	SD-XWK6-D2TN	298 High Level Road, Maharagama	143 Thimbirigasyaya Road, Colombo 05	Amaya Wijesinghe	LARGE	8.10	DELIVERED
3bec4e79-2c69-4d46-a22a-2e626d284711	33895d12-23a2-482a-b64b-f581dfb4a36e	8b77ede6-e12d-4a24-9795-f3c87c19d2ba	7bf5a598-1908-4a34-9ac4-041d3ce99466	COMPLETED	2026-11-05 10:00:59.99951+00	2026-11-05 10:00:59.99951+00	2026-11-06 06:33:00.002385+00	SD-9GYG-N9C5	152 Kotte Road, Kotte	162 Ramakrishna Road, Colombo 06	Gayani Karunaratne	SMALL	3.47	DELIVERED
b36e37cc-4f52-4acb-ae1d-47fc448f22ee	0638d805-283a-4dc4-8902-2ccfecbe3873	d4d6a9d7-39f3-45d3-ab66-553e4aee8b84	cb4cd22c-d4cd-4f2f-ad43-c2e73f1b9cb5	FAILED	2026-11-06 06:26:59.99962+00	2026-11-06 06:26:59.99962+00	2026-11-06 12:29:00.001727+00	SD-FG4A-RJGT	76 Ward Place, Colombo 07	292 Nawala Road, Rajagiriya	Ishara Samarasinghe	MEDIUM	9.25	FAILED
ffa5afb8-7354-4c08-8e02-9aaff98914d9	25d6a22f-1ac2-481d-98a5-48a2df6f9a25	72186f7d-7dd3-4e09-8551-045f309d0a7e	7bf5a598-1908-4a34-9ac4-041d3ce99466	COMPLETED	2026-11-06 05:51:59.99958+00	2026-11-06 05:51:59.99958+00	2026-11-07 05:00:00.002114+00	SD-UKMC-ST78	51 Thimbirigasyaya Road, Colombo 05	139 York Street, Colombo 01	Sajith Ratnayake	SMALL	2.83	DELIVERED
7a062bda-68aa-4ad3-954d-3b400e951a60	70796530-5fd6-4dbd-98cb-1150fb510577	72186f7d-7dd3-4e09-8551-045f309d0a7e	3900455f-0834-4d57-85c5-c3de92f5ecee	COMPLETED	2026-11-06 08:14:00.000486+00	2026-11-06 08:14:00.000486+00	2026-11-07 07:18:00.002326+00	SD-LULG-UEBB	229 Hotel Road, Mount Lavinia	158 Parliament Road, Rajagiriya	Supuni Jayawardena	SMALL	2.26	DELIVERED
d5b66c9a-ac2d-4e0a-a519-b0d688761107	35b68344-9e75-4327-b0fc-628bfa9cb330	86d11ba9-d0e8-4301-8403-9903afbd11a9	3900455f-0834-4d57-85c5-c3de92f5ecee	COMPLETED	2026-11-06 09:51:59.999839+00	2026-11-06 09:51:59.999839+00	2026-11-07 06:56:00.003186+00	SD-X8QF-MS5T	90 Ward Place, Colombo 07	271 Hotel Road, Mount Lavinia	Lakshmi Mendis	MEDIUM	7.63	DELIVERED
c1e84e14-8b17-42e2-92da-712a8abbda61	bedfbc02-8f8a-4f4a-a458-e8944a6a9afe	0c248632-4e7b-4ade-893f-f0a7d8ae0cfb	7bf5a598-1908-4a34-9ac4-041d3ce99466	COMPLETED	2026-11-06 10:04:00.000454+00	2026-11-06 10:04:00.000454+00	2026-11-07 05:02:00.002604+00	SD-5GQG-666Y	78 Prince of Wales Avenue, Colombo 14	143 Flower Road, Colombo 07	Malith Senanayake	LARGE	17.91	DELIVERED
baaa513a-ee37-4525-bd8c-212af1017b43	05d547b2-a499-46ff-bea6-5caacdf1c6e0	0c248632-4e7b-4ade-893f-f0a7d8ae0cfb	7bf5a598-1908-4a34-9ac4-041d3ce99466	COMPLETED	2026-11-06 11:57:59.999629+00	2026-11-06 11:57:59.999629+00	2026-11-07 07:06:00.002976+00	SD-GZKB-2TZT	30 Ward Place, Colombo 07	260 Hill Street, Dehiwala	Ishara Perera	LARGE	8.37	DELIVERED
b9a9ea29-7571-45e8-a074-1e2e4b2ba360	565e6aa3-10a5-4cbe-a600-5d3aadae0bd5	2c4bbfbc-96d6-4e98-b778-a375c6c2ab0d	7bf5a598-1908-4a34-9ac4-041d3ce99466	COMPLETED	2026-11-06 14:02:00.000031+00	2026-11-06 14:02:00.000031+00	2026-11-07 07:28:00.00236+00	SD-JG97-7MFJ	300 Galle Road, Colombo 03	127 Nawala Road, Rajagiriya	Hashini Rahman	MEDIUM	2.63	DELIVERED
562d0df4-b1e2-4a18-b1d2-5688aaadba44	64bbc435-a6f0-4815-811d-742386914865	0c248632-4e7b-4ade-893f-f0a7d8ae0cfb	cb4cd22c-d4cd-4f2f-ad43-c2e73f1b9cb5	COMPLETED	2026-11-07 05:25:59.999694+00	2026-11-07 05:25:59.999694+00	2026-11-08 05:24:00.004327+00	SD-2C6S-8B6C	282 York Street, Colombo 01	326 Union Place, Colombo 02	Nipuni Jayawardena	LARGE	13.74	DELIVERED
2a7aa66a-338d-41a8-ab10-12b98bcc924b	e2b45329-6890-44c8-9951-7aa274ae337e	64dd446f-7e03-421f-a2f0-05db7f2bc36c	cb4cd22c-d4cd-4f2f-ad43-c2e73f1b9cb5	COMPLETED	2026-11-07 06:54:00.000017+00	2026-11-07 06:54:00.000017+00	2026-11-08 05:48:00.00259+00	SD-EG2B-V9C7	6 Stanley Thilakaratne Mawatha, Nugegoda	192 Station Road, Nugegoda	Amaya Samarasinghe	EXTRA_LARGE	32.19	DELIVERED
fe35afb2-e3cc-478d-81b2-378372a0f3a6	3dba59e9-c793-4fd1-8900-a1ea78043376	64dd446f-7e03-421f-a2f0-05db7f2bc36c	cb4cd22c-d4cd-4f2f-ad43-c2e73f1b9cb5	COMPLETED	2026-11-07 08:42:59.999865+00	2026-11-07 08:42:59.999865+00	2026-11-08 06:22:00.003269+00	SD-JD5T-VS6Y	271 Maradana Road, Colombo 10	188 De Saram Road, Mount Lavinia	Dimuth Perera	SMALL	4.12	DELIVERED
89928a74-4e58-422b-be1e-56fdaf0afd78	2f8c8560-e3f8-40c6-a5ae-acf3be54dcf0	7fb36c3a-09c3-4c20-b5b6-d00d8d230cd5	3900455f-0834-4d57-85c5-c3de92f5ecee	COMPLETED	2026-11-07 10:33:00.000496+00	2026-11-07 10:33:00.000496+00	2026-11-08 07:31:00.002528+00	SD-VP8Y-CLYB	269 High Level Road, Maharagama	250 Ramakrishna Road, Colombo 06	Dilani Rahman	SMALL	3.06	DELIVERED
060036d3-4e7f-4f10-a9da-20c6385f0784	14ded203-28cd-4a4a-867f-e95e102d39de	0175f24c-4200-4492-84f2-030a2c0cc5ca	7bf5a598-1908-4a34-9ac4-041d3ce99466	COMPLETED	2026-11-08 05:00:00.000024+00	2026-11-08 05:00:00.000024+00	2026-11-09 08:17:00.00232+00	SD-TWTU-GXJB	275 Flower Road, Colombo 07	74 Hotel Road, Mount Lavinia	Shehan Jayasinghe	LARGE	12.32	DELIVERED
4177b1a6-4789-4365-b7fb-8174ab4112ac	509a48c8-6c0a-4c3d-be1e-02ce8759562e	0175f24c-4200-4492-84f2-030a2c0cc5ca	7bf5a598-1908-4a34-9ac4-041d3ce99466	FAILED	2026-11-08 07:22:00.000085+00	2026-11-08 07:22:00.000085+00	2026-11-09 08:08:00.002823+00	SD-47YK-UELM	225 Chatham Street, Colombo 01	343 Thimbirigasyaya Road, Colombo 05	Ayesha Weerasinghe	SMALL	3.48	FAILED
d567b5cf-b3cd-4a79-83c4-8e595ec353f4	d00a7719-de2b-4cd2-80a6-53741cfbdee9	7fb36c3a-09c3-4c20-b5b6-d00d8d230cd5	7bf5a598-1908-4a34-9ac4-041d3ce99466	COMPLETED	2026-11-08 09:00:00.000291+00	2026-11-08 09:00:00.000292+00	2026-11-09 03:43:00.002363+00	SD-WL8T-DNS9	212 Stanley Thilakaratne Mawatha, Nugegoda	40 Kotte Road, Kotte	Methmi Liyanage	SMALL	4.42	DELIVERED
2da29572-a29a-4323-ad93-ecf8f5dbde04	ccf599c5-bae9-47ad-be04-711f9ee21d53	7fb36c3a-09c3-4c20-b5b6-d00d8d230cd5	cb4cd22c-d4cd-4f2f-ad43-c2e73f1b9cb5	COMPLETED	2026-11-08 11:51:00.000177+00	2026-11-08 11:51:00.000177+00	2026-11-09 06:59:00.002466+00	SD-TNRA-K8AV	319 Ward Place, Colombo 07	220 Galle Road, Dehiwala	Shehan Samarasinghe	SMALL	4.08	DELIVERED
9a0b6fea-d442-4ea8-8f10-32790e973031	9cca48fe-ec49-4ea4-927d-02b7458a4334	0c248632-4e7b-4ade-893f-f0a7d8ae0cfb	cb4cd22c-d4cd-4f2f-ad43-c2e73f1b9cb5	COMPLETED	2026-11-09 05:26:59.999791+00	2026-11-09 05:26:59.999791+00	2026-11-09 11:55:00.002713+00	SD-B7P8-A8C8	92 Galle Road, Colombo 03	13 York Street, Colombo 01	Dulani Ratnayake	MEDIUM	4.59	DELIVERED
078bb0e4-ece3-4a84-885d-403461c21927	bca538fc-3b41-4b1f-901a-ec28c2682dfe	64dd446f-7e03-421f-a2f0-05db7f2bc36c	cb4cd22c-d4cd-4f2f-ad43-c2e73f1b9cb5	COMPLETED	2026-11-09 07:21:00.000207+00	2026-11-09 07:21:00.000207+00	2026-11-10 06:22:00.002748+00	SD-2MRT-2S2K	34 Stanley Thilakaratne Mawatha, Nugegoda	136 De Saram Road, Mount Lavinia	Isuru Peiris	SMALL	1.65	DELIVERED
f9c27374-8666-43f1-829d-698c3615d56e	d38dfefc-8d81-4609-ac05-758941b92656	7fb36c3a-09c3-4c20-b5b6-d00d8d230cd5	cb4cd22c-d4cd-4f2f-ad43-c2e73f1b9cb5	COMPLETED	2026-11-09 09:40:59.999685+00	2026-11-09 09:40:59.999685+00	2026-11-10 04:31:00.001839+00	SD-6Y9H-L5T2	303 Marine Drive, Colombo 06	145 Colombo Road, Negombo	Anjali Perera	SMALL	0.90	DELIVERED
7aa5ecbd-4687-4b1e-bce0-dadc25ac2a84	77a20407-f16b-4fc3-9ae8-62037dc231c8	0175f24c-4200-4492-84f2-030a2c0cc5ca	3900455f-0834-4d57-85c5-c3de92f5ecee	COMPLETED	2026-11-09 10:27:59.999849+00	2026-11-09 10:27:59.999849+00	2026-11-10 08:07:00.002534+00	SD-3T93-2Q82	332 Stanley Thilakaratne Mawatha, Nugegoda	253 Galle Road, Dehiwala	Sahan Weerasinghe	SMALL	3.94	DELIVERED
a5a3f9ff-47ae-4feb-8fa3-7c778f3980d5	69dd739f-8fc7-4b93-a921-86b1d09cf6df	0175f24c-4200-4492-84f2-030a2c0cc5ca	7bf5a598-1908-4a34-9ac4-041d3ce99466	COMPLETED	2026-11-09 12:23:59.999946+00	2026-11-09 12:23:59.999946+00	2026-11-10 05:11:00.00276+00	SD-RFBC-TLV4	244 Chatham Street, Colombo 01	127 De Saram Road, Mount Lavinia	Fathima Fonseka	SMALL	1.80	DELIVERED
4b30b983-3c83-4776-9098-66051903f04a	dde66879-665d-49f7-be23-bde5270552da	2c4bbfbc-96d6-4e98-b778-a375c6c2ab0d	cb4cd22c-d4cd-4f2f-ad43-c2e73f1b9cb5	COMPLETED	2026-11-09 11:58:00.000351+00	2026-11-09 11:58:00.000351+00	2026-11-10 05:36:00.002747+00	SD-J6EK-UFFE	305 Parliament Road, Rajagiriya	102 De Saram Road, Mount Lavinia	Isuru Amarasekara	LARGE	11.33	DELIVERED
2e77755a-ebe6-4466-a157-25d541414f69	b9996eb4-48d9-47af-a1a8-51eeee14b369	7fb36c3a-09c3-4c20-b5b6-d00d8d230cd5	7bf5a598-1908-4a34-9ac4-041d3ce99466	COMPLETED	2026-11-10 04:40:00.00018+00	2026-11-10 04:40:00.00018+00	2026-11-11 05:12:00.002935+00	SD-F975-ZZX7	84 De Saram Road, Mount Lavinia	201 Pita Kotte Road, Kotte	Ravindu Rajendran	LARGE	16.01	DELIVERED
d5f61148-5cb2-4f04-9c2f-e14e309bf90d	e3b4b42b-7642-4d28-8dae-68e160e2af1d	86d11ba9-d0e8-4301-8403-9903afbd11a9	3900455f-0834-4d57-85c5-c3de92f5ecee	COMPLETED	2026-11-10 04:54:59.999521+00	2026-11-10 04:54:59.999521+00	2026-11-10 12:10:00.002305+00	SD-URYF-UBRU	253 Hotel Road, Mount Lavinia	112 Stanley Thilakaratne Mawatha, Nugegoda	Sajith Amarasekara	LARGE	10.00	DELIVERED
d62060a8-39ce-4436-ae51-5bedf32aebf7	840a08d5-4f16-4703-8384-4160246525d2	2c4bbfbc-96d6-4e98-b778-a375c6c2ab0d	cb4cd22c-d4cd-4f2f-ad43-c2e73f1b9cb5	FAILED	2026-11-10 07:50:59.999771+00	2026-11-10 07:50:59.999771+00	2026-11-11 05:45:00.002524+00	SD-9B6M-R8QQ	109 Chatham Street, Colombo 01	49 Hotel Road, Mount Lavinia	Methmi Karunaratne	SMALL	2.72	FAILED
64e81fcc-378b-486a-a92c-cc13f1341b69	a897b469-b63e-4238-8d75-963b4960d7f2	0175f24c-4200-4492-84f2-030a2c0cc5ca	cb4cd22c-d4cd-4f2f-ad43-c2e73f1b9cb5	COMPLETED	2026-11-10 07:30:59.999579+00	2026-11-10 07:30:59.999579+00	2026-11-11 07:23:00.003373+00	SD-SQXL-63HE	15 William Gopallawa Mawatha, Kandy	17 Union Place, Colombo 02	Mohamed Mendis	SMALL	4.83	DELIVERED
17d45e9c-f5b1-46cd-b3d8-0d44a30254cc	208e7e47-9fef-400e-842a-855997c7fb47	86d11ba9-d0e8-4301-8403-9903afbd11a9	7bf5a598-1908-4a34-9ac4-041d3ce99466	COMPLETED	2026-11-10 07:06:59.99985+00	2026-11-10 07:06:59.99985+00	2026-11-11 06:16:00.00264+00	SD-8DV3-K2FK	60 Hill Street, Dehiwala	223 Hill Street, Dehiwala	Priya Fernando	MEDIUM	4.87	DELIVERED
89073517-7529-45c0-a412-e9344b03b5c5	38d8044b-63b9-4f71-ba06-609bdf118f60	d4d6a9d7-39f3-45d3-ab66-553e4aee8b84	7bf5a598-1908-4a34-9ac4-041d3ce99466	COMPLETED	2026-11-10 08:31:59.999791+00	2026-11-10 08:31:59.999791+00	2026-11-11 06:29:00.00251+00	SD-VTDV-KDAM	88 Galle Road, Dehiwala	235 Main Street, Colombo 11	Sachini Rahman	SMALL	1.53	DELIVERED
0bac67dd-64c7-4900-80ae-ea1574e5ff5b	7d6e40c6-5aa6-4ca4-9cda-9547df196028	64dd446f-7e03-421f-a2f0-05db7f2bc36c	3900455f-0834-4d57-85c5-c3de92f5ecee	COMPLETED	2026-11-10 11:26:59.999984+00	2026-11-10 11:26:59.999984+00	2026-11-11 09:09:00.003257+00	SD-D8ZL-E2DE	206 Kotte Road, Kotte	244 Maradana Road, Colombo 10	Pradeep Karunaratne	LARGE	11.67	DELIVERED
0931e5c0-67f4-4c20-acff-da506cad1c57	64833476-4eab-44e8-a9c3-952a0213c694	8b77ede6-e12d-4a24-9795-f3c87c19d2ba	7bf5a598-1908-4a34-9ac4-041d3ce99466	FAILED	2026-11-10 11:53:00.000024+00	2026-11-10 11:53:00.000024+00	2026-11-11 06:49:00.002594+00	SD-L95J-DZEW	4 Hotel Road, Mount Lavinia	52 Chatham Street, Colombo 01	Arun Peiris	LARGE	13.61	FAILED
880d87c2-a977-4124-b92a-0ce6996799f0	65a2021e-d833-4abf-8a03-e6180aeade73	8b77ede6-e12d-4a24-9795-f3c87c19d2ba	3900455f-0834-4d57-85c5-c3de92f5ecee	COMPLETED	2026-11-10 13:07:59.999914+00	2026-11-10 13:07:59.999915+00	2026-11-11 08:39:00.00316+00	SD-P72R-2K6Z	217 Station Road, Nugegoda	139 Chatham Street, Colombo 01	Gayani Ratnayake	LARGE	5.71	DELIVERED
647a5dd6-b8b3-44e4-a2a6-c2cebb9dc9f5	e956d9db-b38c-45c2-92cb-47522eae1f14	64dd446f-7e03-421f-a2f0-05db7f2bc36c	cb4cd22c-d4cd-4f2f-ad43-c2e73f1b9cb5	COMPLETED	2026-11-10 12:57:59.99995+00	2026-11-10 12:57:59.99995+00	2026-11-11 04:44:00.002645+00	SD-W5M8-BUHU	92 Parliament Road, Rajagiriya	249 Kotte Road, Kotte	Ayesha Abeysekera	SMALL	4.42	DELIVERED
134677c7-64a0-40d7-81f5-d8ad9638e297	573b6d9f-f2bb-4236-950e-966d33de17eb	2c4bbfbc-96d6-4e98-b778-a375c6c2ab0d	3900455f-0834-4d57-85c5-c3de92f5ecee	COMPLETED	2026-11-10 13:22:00.000299+00	2026-11-10 13:22:00.000299+00	2026-11-11 07:02:00.003753+00	SD-CZVD-3F8S	236 Galle Road, Colombo 04	230 Main Street, Negombo	Kamal Jayawardena	SMALL	4.05	DELIVERED
4e3391cb-25d4-481d-a696-056da1d703d1	7ed2efc2-021e-4742-a780-1cc3cb631fc4	86d11ba9-d0e8-4301-8403-9903afbd11a9	7bf5a598-1908-4a34-9ac4-041d3ce99466	COMPLETED	2026-11-11 03:38:00.000113+00	2026-11-11 03:38:00.000113+00	2026-11-12 03:48:00.002742+00	SD-QCRX-PV42	286 Maradana Road, Colombo 10	35 Hill Street, Dehiwala	Rizwan Senanayake	MEDIUM	9.42	DELIVERED
4e7ac7f5-a164-4219-89f2-39a93a52ded3	9a5a3b4e-7332-46e1-be10-ff0bf9e3b0a7	d4d6a9d7-39f3-45d3-ab66-553e4aee8b84	7bf5a598-1908-4a34-9ac4-041d3ce99466	COMPLETED	2026-11-11 12:22:00.000168+00	2026-11-11 12:22:00.000168+00	2026-11-12 08:06:00.002038+00	SD-G835-NFBC	192 Pita Kotte Road, Kotte	189 Chatham Street, Colombo 01	Sunil Peiris	SMALL	4.69	DELIVERED
66611925-1271-4a01-aa8b-1288f7eafd0a	71072f9e-95c8-44ab-bb73-2af02cfafe91	64dd446f-7e03-421f-a2f0-05db7f2bc36c	cb4cd22c-d4cd-4f2f-ad43-c2e73f1b9cb5	COMPLETED	2026-11-11 12:41:59.99982+00	2026-11-11 12:41:59.99982+00	2026-11-12 07:06:00.002606+00	SD-K5EM-CQH3	29 High Level Road, Maharagama	83 Flower Road, Colombo 07	Tharindu Peiris	MEDIUM	7.11	DELIVERED
9a55bf5a-79e3-4007-b518-c5b3aeebeff7	c16abd19-54a2-4ce1-a3ad-f2e44154c702	72186f7d-7dd3-4e09-8551-045f309d0a7e	7bf5a598-1908-4a34-9ac4-041d3ce99466	COMPLETED	2026-11-12 04:39:00.000395+00	2026-11-12 04:39:00.000395+00	2026-11-13 07:35:00.002796+00	SD-VD7G-SHNT	90 Chatham Street, Colombo 01	10 Stanley Thilakaratne Mawatha, Nugegoda	Dulani Rajapaksa	SMALL	0.62	DELIVERED
f7d0cf22-b6e5-4103-8bd9-725a1c81d48a	e9b83438-d6cf-4121-9f48-ea64dea90cae	72186f7d-7dd3-4e09-8551-045f309d0a7e	7bf5a598-1908-4a34-9ac4-041d3ce99466	COMPLETED	2026-11-12 06:14:59.999763+00	2026-11-12 06:14:59.999763+00	2026-11-13 06:07:00.002433+00	SD-LC6P-Q26G	138 Union Place, Colombo 02	307 Duplication Road, Colombo 03	Methmi Abeysekera	SMALL	2.11	DELIVERED
e189e890-ef98-49da-b01a-0516636ea750	bf4d7095-6545-42d8-b5d7-ba224fa17df0	64dd446f-7e03-421f-a2f0-05db7f2bc36c	3900455f-0834-4d57-85c5-c3de92f5ecee	COMPLETED	2026-11-12 05:17:00.000272+00	2026-11-12 05:17:00.000272+00	2026-11-12 13:37:00.001791+00	SD-3XTD-GXG7	223 Havelock Road, Colombo 05	91 Station Road, Nugegoda	Kamal Samarasinghe	LARGE	12.44	DELIVERED
cebdaddb-0983-497b-be87-8887caf5bd5d	91225279-3d6b-435c-b5c4-befabb0d8c38	7fb36c3a-09c3-4c20-b5b6-d00d8d230cd5	3900455f-0834-4d57-85c5-c3de92f5ecee	COMPLETED	2026-11-12 07:58:00.000323+00	2026-11-12 07:58:00.000323+00	2026-11-13 04:29:00.002995+00	SD-5FXF-QEPX	302 Maradana Road, Colombo 10	282 Hill Street, Dehiwala	Arun Abeysekera	SMALL	1.06	DELIVERED
56b81c93-2441-443e-aaa4-f3374c5804ab	57e1eee0-de0c-4c7c-87a0-36a03079f1b2	86d11ba9-d0e8-4301-8403-9903afbd11a9	3900455f-0834-4d57-85c5-c3de92f5ecee	FAILED	2026-11-12 08:51:00.000069+00	2026-11-12 08:51:00.000069+00	2026-11-13 07:53:00.001865+00	SD-F8BH-VRTP	333 Old Kesbewa Road, Nugegoda	277 Lauries Road, Colombo 04	Ruwan Gunasekara	LARGE	10.24	FAILED
84668864-f413-4492-98aa-4ac50db9687a	100686c1-1b27-4a67-b199-e1e85f686282	64dd446f-7e03-421f-a2f0-05db7f2bc36c	7bf5a598-1908-4a34-9ac4-041d3ce99466	COMPLETED	2026-11-12 08:36:00.000108+00	2026-11-12 08:36:00.000108+00	2026-11-13 07:23:00.001739+00	SD-WK4H-AF3M	234 Main Street, Colombo 11	10 Pita Kotte Road, Kotte	Ruwan Silva	LARGE	16.03	DELIVERED
90d214cf-2844-43e1-af27-aa9c20e56365	c6272b68-c609-46e3-a83f-10dfb077ceb7	8b77ede6-e12d-4a24-9795-f3c87c19d2ba	7bf5a598-1908-4a34-9ac4-041d3ce99466	COMPLETED	2026-11-12 08:49:00.000133+00	2026-11-12 08:49:00.000133+00	2026-11-13 05:07:00.002414+00	SD-JC54-RPS9	45 De Saram Road, Mount Lavinia	237 William Gopallawa Mawatha, Kandy	Kasun Wijesinghe	EXTRA_LARGE	17.33	DELIVERED
aea90bb3-9f42-4188-82a5-88257362a873	edb98b76-7745-4f92-9dac-13895a2e9966	8b77ede6-e12d-4a24-9795-f3c87c19d2ba	cb4cd22c-d4cd-4f2f-ad43-c2e73f1b9cb5	COMPLETED	2026-11-12 11:49:00.000161+00	2026-11-12 11:49:00.000161+00	2026-11-13 06:52:00.002139+00	SD-NQLL-WYC7	153 Ramakrishna Road, Colombo 06	106 Church Street, Galle Fort	Gayani Weerasinghe	MEDIUM	3.36	DELIVERED
b10a0842-a82b-480a-812a-d7e4b922ae6f	0332bf7c-dbe7-45d5-a948-3952ec0eebb0	d4d6a9d7-39f3-45d3-ab66-553e4aee8b84	3900455f-0834-4d57-85c5-c3de92f5ecee	COMPLETED	2026-11-13 03:23:59.999676+00	2026-11-13 03:23:59.999676+00	2026-11-14 06:21:00.002237+00	SD-4R5G-9FUH	55 Pita Kotte Road, Kotte	304 York Street, Colombo 01	Nadeesha Ratnayake	SMALL	0.85	DELIVERED
6dc9e55e-f93b-41a8-9882-4d391580b136	60d4d86a-1b2a-47fc-9e99-18b0dad6e052	2c4bbfbc-96d6-4e98-b778-a375c6c2ab0d	7bf5a598-1908-4a34-9ac4-041d3ce99466	COMPLETED	2026-11-13 05:10:59.999943+00	2026-11-13 05:10:59.999943+00	2026-11-14 04:41:00.002721+00	SD-TEKR-4KBQ	274 Kotte Road, Kotte	117 Union Place, Colombo 02	Tharushi Nawaz	LARGE	10.38	DELIVERED
b8c3917a-c138-4866-8f5e-0c39b229badc	ddb3195f-f3b8-4294-8de3-414929485b15	64dd446f-7e03-421f-a2f0-05db7f2bc36c	cb4cd22c-d4cd-4f2f-ad43-c2e73f1b9cb5	COMPLETED	2026-11-13 08:06:59.999573+00	2026-11-13 08:06:59.999573+00	2026-11-14 07:57:00.002545+00	SD-AF8Q-LBWM	254 Colombo Road, Negombo	276 William Gopallawa Mawatha, Kandy	Kasun Dissanayake	MEDIUM	3.63	DELIVERED
ee5706d8-5f11-4610-9771-a19fc8b2d7bb	7b927c0b-65f6-4e33-bcc0-bbf4e51411a7	86d11ba9-d0e8-4301-8403-9903afbd11a9	cb4cd22c-d4cd-4f2f-ad43-c2e73f1b9cb5	FAILED	2026-11-13 10:13:59.999553+00	2026-11-13 10:13:59.999553+00	2026-11-14 06:54:00.002279+00	SD-BKQY-7SGL	161 Maradana Road, Colombo 10	187 Main Street, Negombo	Pradeep Peiris	EXTRA_LARGE	31.43	FAILED
555817e3-59e7-4daa-b856-00bb6d1cdb4a	5519eb2c-3c0c-4a0e-aa91-4d760d9bf819	0c248632-4e7b-4ade-893f-f0a7d8ae0cfb	3900455f-0834-4d57-85c5-c3de92f5ecee	COMPLETED	2026-11-13 10:39:00.000275+00	2026-11-13 10:39:00.000275+00	2026-11-14 04:01:00.007632+00	SD-BVMQ-6G7Z	313 Station Road, Nugegoda	296 Matara Road, Galle	Sunil Rajendran	MEDIUM	4.21	DELIVERED
7853c958-6b38-4030-89dd-4728610e849c	eb20624e-51f4-4727-9852-3a6a88b77d7b	64dd446f-7e03-421f-a2f0-05db7f2bc36c	3900455f-0834-4d57-85c5-c3de92f5ecee	COMPLETED	2026-11-13 12:15:59.999631+00	2026-11-13 12:15:59.999631+00	2026-11-14 03:26:00.002007+00	SD-GMP5-Y4ZU	117 Matara Road, Galle	53 Duplication Road, Colombo 03	Tharushi Mendis	LARGE	19.48	DELIVERED
61897b73-1cf0-47ee-bc70-123aa6eb8e19	320b7a40-1c89-4ad1-9c2b-df1538109881	8b77ede6-e12d-4a24-9795-f3c87c19d2ba	7bf5a598-1908-4a34-9ac4-041d3ce99466	COMPLETED	2026-11-13 13:12:59.999837+00	2026-11-13 13:12:59.999837+00	2026-11-14 07:47:00.002046+00	SD-5U22-DZ57	120 York Street, Colombo 01	178 York Street, Colombo 01	Nimal Sivakumar	SMALL	4.01	DELIVERED
1db694c0-c1d1-40c4-9386-d71dae7903f0	b0817bf3-b24a-48e1-9ea5-160a32b96662	d4d6a9d7-39f3-45d3-ab66-553e4aee8b84	cb4cd22c-d4cd-4f2f-ad43-c2e73f1b9cb5	COMPLETED	2026-11-13 14:02:59.999931+00	2026-11-13 14:02:59.999931+00	2026-11-14 06:51:00.002439+00	SD-CR6W-8SLF	339 Havelock Road, Colombo 05	4 Matara Road, Galle	Senuri Perera	LARGE	13.61	DELIVERED
ac5eeb37-dc8f-4c5a-9757-45baa874932b	a359755f-6e1f-4eac-b786-3f3105cee17a	64dd446f-7e03-421f-a2f0-05db7f2bc36c	3900455f-0834-4d57-85c5-c3de92f5ecee	COMPLETED	2026-11-14 03:53:00.000075+00	2026-11-14 03:53:00.000076+00	2026-11-14 09:39:00.002814+00	SD-82DY-ZY8Q	139 Ward Place, Colombo 07	259 Hill Street, Dehiwala	Arun de Silva	SMALL	1.27	DELIVERED
51d778aa-fa08-45e6-9902-3b9d520e5ba2	117105bb-7aa5-4fd3-ae80-56cd30bb17dc	72186f7d-7dd3-4e09-8551-045f309d0a7e	3900455f-0834-4d57-85c5-c3de92f5ecee	IN_PROGRESS	2026-11-14 04:47:59.999696+00	2026-11-14 04:47:59.999696+00	2026-11-14 09:16:00.00235+00	SD-2DZN-AWQC	211 Hill Street, Dehiwala	149 Maradana Road, Colombo 10	Oshada Silva	MEDIUM	4.36	IN_TRANSIT
2d6d72f7-843c-4c7e-be3a-da722cdafc69	87009dd8-07a1-4526-bcfb-b99c6667db08	7fb36c3a-09c3-4c20-b5b6-d00d8d230cd5	7bf5a598-1908-4a34-9ac4-041d3ce99466	IN_PROGRESS	2026-11-14 08:08:59.999829+00	2026-11-14 08:08:59.999829+00	2026-11-14 10:48:00.002635+00	SD-52R2-T9ZT	138 Ramakrishna Road, Colombo 06	134 Dalada Veediya, Kandy	Priya Nawaz	SMALL	1.96	IN_TRANSIT
ba46ecee-5328-4ed2-9e6b-be2dc89bfe9a	0d93e4d1-2ada-4cd6-950f-1b932e01f5db	64dd446f-7e03-421f-a2f0-05db7f2bc36c	3900455f-0834-4d57-85c5-c3de92f5ecee	IN_PROGRESS	2026-11-14 08:51:00.000271+00	2026-11-14 08:51:00.000271+00	2026-11-14 13:28:00.002671+00	SD-EZSH-M5XY	251 Marine Drive, Colombo 06	25 Galle Road, Colombo 03	Lakshmi Weerasinghe	LARGE	8.31	IN_TRANSIT
3497695c-c1cf-49fd-9376-4d6aaa74fb5a	20496736-26ff-4024-bb9a-8fed67582047	8b77ede6-e12d-4a24-9795-f3c87c19d2ba	cb4cd22c-d4cd-4f2f-ad43-c2e73f1b9cb5	IN_PROGRESS	2026-11-14 09:02:00.000193+00	2026-11-14 09:02:00.000193+00	2026-11-14 12:50:00.001856+00	SD-SQSA-32XM	107 Station Road, Nugegoda	339 Main Street, Negombo	Sahan Fernando	SMALL	4.23	IN_TRANSIT
53c99bc5-392f-47c1-bdb3-1a5f6d60bf74	a06a0356-5fc6-4f96-9d98-38ac5d1ba316	8b77ede6-e12d-4a24-9795-f3c87c19d2ba	3900455f-0834-4d57-85c5-c3de92f5ecee	IN_PROGRESS	2026-11-14 11:41:59.999881+00	2026-11-14 11:41:59.999881+00	2026-11-14 13:41:00.002868+00	SD-P6DV-Q4ZZ	321 Station Road, Nugegoda	19 Nawala Road, Rajagiriya	Malith Fernando	SMALL	1.67	IN_TRANSIT
d1129d2f-7b42-4b61-ae24-9d88954c22a8	fa6a64d4-2f8c-4470-8526-30cb336ddf53	86d11ba9-d0e8-4301-8403-9903afbd11a9	3900455f-0834-4d57-85c5-c3de92f5ecee	IN_PROGRESS	2026-11-14 11:07:00.000269+00	2026-11-14 11:07:00.000269+00	2026-11-14 12:45:00.00235+00	SD-UNDL-ED2M	327 Galle Road, Dehiwala	321 Prince of Wales Avenue, Colombo 14	Hiruni Karunaratne	SMALL	1.88	PICKED_UP
2c6dc9e1-a3d5-4818-884f-701d49fd3853	1904626f-987a-4bab-80da-6fc487b3eda6	72186f7d-7dd3-4e09-8551-045f309d0a7e	cb4cd22c-d4cd-4f2f-ad43-c2e73f1b9cb5	IN_PROGRESS	2026-11-14 11:55:59.999628+00	2026-11-14 11:55:59.999628+00	2026-11-14 14:00:00.002348+00	SD-QQ26-H6LQ	63 Baseline Road, Colombo 09	18 Dalada Veediya, Kandy	Kasun Jayawardena	MEDIUM	7.12	PICKED_UP
a6eeed9b-55dd-43c4-a5a3-8cdb2f6c9211	90037bb1-b7fc-4e5d-97d9-391ed328c237	64dd446f-7e03-421f-a2f0-05db7f2bc36c	3900455f-0834-4d57-85c5-c3de92f5ecee	ASSIGNED	2026-11-14 13:04:59.999921+00	2026-11-14 13:04:59.999922+00	2026-11-14 11:16:14.119922+00	SD-LQ39-NMXP	171 Galle Road, Colombo 04	324 Old Kesbewa Road, Nugegoda	Arun Karunaratne	MEDIUM	9.31	CREATED
3525a704-77f8-46bc-a87b-478ed2c1c10e	7e9911c4-865b-4b7d-ad54-06a3a70696e5	72186f7d-7dd3-4e09-8551-045f309d0a7e	3900455f-0834-4d57-85c5-c3de92f5ecee	ASSIGNED	2026-11-14 13:53:00.000387+00	2026-11-14 13:53:00.000387+00	2026-11-14 12:09:14.116387+00	SD-B6CB-DGV8	146 Havelock Road, Colombo 05	167 Union Place, Colombo 02	Sunil Silva	SMALL	4.62	CREATED
e5bc47b8-da43-4832-bf79-422387a33d2b	67fabc21-5dd0-418b-b4d0-065b4f4c3580	0175f24c-4200-4492-84f2-030a2c0cc5ca	3900455f-0834-4d57-85c5-c3de92f5ecee	ASSIGNED	2026-11-14 14:00:59.999909+00	2026-11-14 14:00:59.999909+00	2026-11-14 12:38:14.101909+00	SD-3M9Z-TX2G	330 Pita Kotte Road, Kotte	61 York Street, Colombo 01	Dimuth Rajapaksa	LARGE	17.24	CREATED
0c013bee-cee2-4593-8037-d8669b843cdb	14bf7e5b-e512-4fac-80e1-5df786ffb9ac	72186f7d-7dd3-4e09-8551-045f309d0a7e	3900455f-0834-4d57-85c5-c3de92f5ecee	ASSIGNED	2026-11-14 13:39:59.999523+00	2026-11-14 13:39:59.999523+00	2026-11-14 12:51:14.099523+00	SD-5PE9-CDXD	82 Ward Place, Colombo 07	318 Church Street, Galle Fort	Mohamed Rajendran	SMALL	1.33	CREATED
659fe5e5-a63c-477c-8626-b18df91918d7	a7165bf4-820a-45c7-988a-78aa8f157502	72186f7d-7dd3-4e09-8551-045f309d0a7e	cb4cd22c-d4cd-4f2f-ad43-c2e73f1b9cb5	ASSIGNED	2026-11-14 13:57:59.999723+00	2026-11-14 13:57:59.999723+00	2026-11-14 13:13:14.096723+00	SD-RTN3-BFBV	162 High Level Road, Maharagama	37 Kollupitiya Road, Colombo 03	Dilani Jayasinghe	SMALL	3.21	CREATED
8788ce70-c28f-480d-a870-5eb68a004cf4	fe2c3f20-9e75-460c-b1f5-2d2dcc64b823	64dd446f-7e03-421f-a2f0-05db7f2bc36c	cb4cd22c-d4cd-4f2f-ad43-c2e73f1b9cb5	COMPLETED	2026-10-16 08:03:59.999994+00	2026-10-16 08:03:59.999995+00	2026-10-17 05:16:00.001651+00	SD-SESA-M65D	51 Peradeniya Road, Kandy	124 Flower Road, Colombo 07	Kumar Jayawardena	SMALL	2.93	DELIVERED
817feb55-f22a-4aa7-9652-c488885966b4	d4b01bbf-7822-4a71-a00b-0981c775eee5	7fb36c3a-09c3-4c20-b5b6-d00d8d230cd5	cb4cd22c-d4cd-4f2f-ad43-c2e73f1b9cb5	COMPLETED	2026-10-17 06:16:59.999584+00	2026-10-17 06:16:59.999585+00	2026-10-18 05:51:00.002222+00	SD-PUHU-K6HA	105 Maradana Road, Colombo 10	30 Hill Street, Dehiwala	Dinuka Perera	LARGE	17.39	DELIVERED
b66a7d28-bd11-44ac-9ebf-89dabe94de55	ddb2296e-be6e-4324-b41b-3cbb7c94f00e	2c4bbfbc-96d6-4e98-b778-a375c6c2ab0d	cb4cd22c-d4cd-4f2f-ad43-c2e73f1b9cb5	COMPLETED	2026-10-17 07:34:00.000068+00	2026-10-17 07:34:00.000068+00	2026-10-18 07:13:00.0023+00	SD-V8JJ-PJMR	180 Kollupitiya Road, Colombo 03	289 Lewis Place, Negombo	Rizwan Gunasekara	SMALL	2.95	DELIVERED
e074282e-6fc8-4c32-a9f4-0374ff701614	19342e31-ec55-4052-b870-c94846ce7669	d4d6a9d7-39f3-45d3-ab66-553e4aee8b84	7bf5a598-1908-4a34-9ac4-041d3ce99466	COMPLETED	2026-10-17 13:59:59.999947+00	2026-10-17 13:59:59.999947+00	2026-10-18 06:35:00.003472+00	SD-XHK8-RVC8	225 High Level Road, Maharagama	258 Chatham Street, Colombo 01	Ayesha Jayawardena	SMALL	3.75	DELIVERED
48c61cef-4bfe-4a99-8408-35548a3aef5b	b179e9e2-1ce3-4e22-ba43-97fb342cff79	8b77ede6-e12d-4a24-9795-f3c87c19d2ba	cb4cd22c-d4cd-4f2f-ad43-c2e73f1b9cb5	COMPLETED	2026-10-18 12:29:00.000064+00	2026-10-18 12:29:00.000064+00	2026-10-19 05:28:00.002108+00	SD-6JZF-XMRA	92 Havelock Road, Colombo 05	225 Maradana Road, Colombo 10	Dulani Herath	SMALL	3.51	DELIVERED
a8d9be82-5c1f-428c-a642-34dc5260187f	d469c568-fd20-48e2-9849-a59320621231	72186f7d-7dd3-4e09-8551-045f309d0a7e	cb4cd22c-d4cd-4f2f-ad43-c2e73f1b9cb5	COMPLETED	2026-10-20 07:38:00.000496+00	2026-10-20 07:38:00.000496+00	2026-10-21 05:30:00.002609+00	SD-JGNM-QBJ3	229 Church Street, Galle Fort	41 High Level Road, Maharagama	Dinuka Mendis	MEDIUM	9.94	DELIVERED
eb2309cb-05d4-414a-9a43-829ec817d9f6	bdfffb72-f48a-42da-9a26-b8723321444b	72186f7d-7dd3-4e09-8551-045f309d0a7e	cb4cd22c-d4cd-4f2f-ad43-c2e73f1b9cb5	COMPLETED	2026-10-20 14:25:00.000235+00	2026-10-20 14:25:00.000235+00	2026-10-21 08:08:00.002177+00	SD-VGKE-WSJZ	51 Peradeniya Road, Kandy	339 Prince of Wales Avenue, Colombo 14	Methmi Gunasekara	MEDIUM	9.24	DELIVERED
e47781d9-bc62-4baf-95db-a0b8acfc6396	e9a26e2f-cf1f-4b78-a7af-de0045830185	7fb36c3a-09c3-4c20-b5b6-d00d8d230cd5	3900455f-0834-4d57-85c5-c3de92f5ecee	COMPLETED	2026-10-21 09:43:59.999732+00	2026-10-21 09:43:59.999733+00	2026-10-22 06:28:00.001642+00	SD-W8AM-PJCX	105 Maradana Road, Colombo 10	202 De Saram Road, Mount Lavinia	Tharindu Jayawardena	MEDIUM	5.00	DELIVERED
7d0eaf38-c4e1-49a0-94e1-7db73920f6cc	d64dff65-8795-4896-92f5-032cd4425083	0175f24c-4200-4492-84f2-030a2c0cc5ca	cb4cd22c-d4cd-4f2f-ad43-c2e73f1b9cb5	COMPLETED	2026-10-21 09:51:59.999687+00	2026-10-21 09:51:59.999687+00	2026-10-22 05:17:00.00254+00	SD-EXNH-72W9	180 Kollupitiya Road, Colombo 03	64 Marine Drive, Colombo 06	Hiruni Jayasinghe	LARGE	12.03	DELIVERED
7ecaae05-bc90-4d7a-abb6-3f777543c101	41497e83-1823-4a69-a831-69649128c588	0c248632-4e7b-4ade-893f-f0a7d8ae0cfb	7bf5a598-1908-4a34-9ac4-041d3ce99466	FAILED	2026-10-21 10:42:59.999911+00	2026-10-21 10:42:59.999911+00	2026-10-22 06:57:00.001448+00	SD-4HML-GUZ8	225 High Level Road, Maharagama	219 Ward Place, Colombo 07	Dinuka Wijesinghe	MEDIUM	9.94	FAILED
b93754f1-2178-48e1-8abf-e2c61b13f54c	332ceee7-b0f5-443e-b29f-ff623e156268	86d11ba9-d0e8-4301-8403-9903afbd11a9	7bf5a598-1908-4a34-9ac4-041d3ce99466	COMPLETED	2026-10-21 12:25:59.999925+00	2026-10-21 12:25:59.999925+00	2026-10-22 06:05:00.001354+00	SD-99AG-ETA9	92 Havelock Road, Colombo 05	25 Old Kesbewa Road, Nugegoda	Ishara Dissanayake	SMALL	3.79	DELIVERED
2b453a84-c2d2-42a2-8345-3cd0ddf6c85e	a0db27d3-edaa-49ff-aead-7a465b9212bc	0175f24c-4200-4492-84f2-030a2c0cc5ca	7bf5a598-1908-4a34-9ac4-041d3ce99466	COMPLETED	2026-10-22 09:52:59.999895+00	2026-10-22 09:52:59.999895+00	2026-10-23 07:41:00.001755+00	SD-NH3H-GNQR	229 Church Street, Galle Fort	73 Matara Road, Galle	Pradeep Rathnayake	SMALL	4.53	DELIVERED
c3fe0e84-8161-4abd-b361-c7062d575aa9	32ace6bc-3a97-40d9-9e28-8e789d0e279e	0c248632-4e7b-4ade-893f-f0a7d8ae0cfb	7bf5a598-1908-4a34-9ac4-041d3ce99466	COMPLETED	2026-10-22 10:09:59.999757+00	2026-10-22 10:09:59.999757+00	2026-10-23 08:04:00.002405+00	SD-ZXVL-RQWJ	51 Peradeniya Road, Kandy	287 Church Street, Galle Fort	Anjali Dissanayake	MEDIUM	3.81	DELIVERED
c63fcd2f-30c2-46a5-80db-a1a43bf2cc1d	5d46e4bf-3bdd-4f1a-8931-d2434d62d124	86d11ba9-d0e8-4301-8403-9903afbd11a9	7bf5a598-1908-4a34-9ac4-041d3ce99466	COMPLETED	2026-10-22 14:16:59.999678+00	2026-10-22 14:16:59.999678+00	2026-10-23 06:58:00.002412+00	SD-TNTS-UM6B	105 Maradana Road, Colombo 10	261 Prince of Wales Avenue, Colombo 14	Anjali Silva	SMALL	0.67	DELIVERED
7bee691a-7b76-46cd-b254-455bfcbab934	908066ce-981c-4588-8eee-f6bcb2d75add	86d11ba9-d0e8-4301-8403-9903afbd11a9	7bf5a598-1908-4a34-9ac4-041d3ce99466	COMPLETED	2026-10-22 14:30:00.000179+00	2026-10-22 14:30:00.00018+00	2026-10-23 08:41:00.002668+00	SD-HXTE-9S7R	180 Kollupitiya Road, Colombo 03	81 Maradana Road, Colombo 10	Sunil Amarasekara	LARGE	8.91	DELIVERED
7622cad8-3465-4df1-b3cc-7510ac020224	1bb394d1-5319-45b8-88ad-e58c663544bb	86d11ba9-d0e8-4301-8403-9903afbd11a9	cb4cd22c-d4cd-4f2f-ad43-c2e73f1b9cb5	COMPLETED	2026-10-23 05:00:00.000083+00	2026-10-23 05:00:00.000083+00	2026-10-24 06:55:00.003008+00	SD-P8CS-VETV	225 High Level Road, Maharagama	3 Marine Drive, Colombo 06	Priya Wijesinghe	LARGE	6.07	DELIVERED
327a7970-7c62-4322-9821-692bce7aef5f	0cd449a5-78ac-49f9-a531-ec67e81c6f9e	8b77ede6-e12d-4a24-9795-f3c87c19d2ba	cb4cd22c-d4cd-4f2f-ad43-c2e73f1b9cb5	COMPLETED	2026-10-23 08:24:00.000212+00	2026-10-23 08:24:00.000212+00	2026-10-24 07:19:00.003365+00	SD-5HC8-K45N	92 Havelock Road, Colombo 05	331 Main Street, Negombo	Hiruni Mendis	SMALL	4.99	DELIVERED
272d615f-e149-4298-b696-12300a53e831	7f5b34f3-1ff3-42f1-9302-09dfce48e49a	86d11ba9-d0e8-4301-8403-9903afbd11a9	cb4cd22c-d4cd-4f2f-ad43-c2e73f1b9cb5	COMPLETED	2026-10-23 07:57:59.999984+00	2026-10-23 07:57:59.999984+00	2026-10-24 05:13:00.002035+00	SD-UYHZ-KMU3	229 Church Street, Galle Fort	187 York Street, Colombo 01	Tharindu Liyanage	EXTRA_LARGE	16.25	DELIVERED
45098314-50cf-4e1e-b18b-c94d03a51ecc	f6fcd3b4-24a2-4132-975d-ad784eeb2c87	86d11ba9-d0e8-4301-8403-9903afbd11a9	7bf5a598-1908-4a34-9ac4-041d3ce99466	COMPLETED	2026-10-23 13:48:00.000279+00	2026-10-23 13:48:00.000279+00	2026-10-24 05:00:00.002641+00	SD-6KML-JUDF	51 Peradeniya Road, Kandy	30 Church Street, Galle Fort	Kasun Liyanage	MEDIUM	2.64	DELIVERED
d6611e30-908d-431e-bcb2-24c2f6eb2c82	a41c2f77-793a-49db-b44c-69e4f354734c	2c4bbfbc-96d6-4e98-b778-a375c6c2ab0d	7bf5a598-1908-4a34-9ac4-041d3ce99466	COMPLETED	2026-10-24 03:45:00.00011+00	2026-10-24 03:45:00.00011+00	2026-10-24 11:46:00.003245+00	SD-XKYC-Z7KZ	105 Maradana Road, Colombo 10	265 Galle Road, Colombo 04	Malith Cooray	EXTRA_LARGE	26.29	DELIVERED
64c369fa-985d-4b6c-a897-8981873bbd1b	00dd5adb-0164-46c1-8985-3521b4225d60	2c4bbfbc-96d6-4e98-b778-a375c6c2ab0d	7bf5a598-1908-4a34-9ac4-041d3ce99466	COMPLETED	2026-10-24 05:24:59.999892+00	2026-10-24 05:24:59.999892+00	2026-10-25 08:18:00.002519+00	SD-BEQ8-VT4U	180 Kollupitiya Road, Colombo 03	223 Main Street, Negombo	Chathura Rajapaksa	MEDIUM	3.36	DELIVERED
9e2565c3-87fc-4885-aca7-5b0f2319fd67	af33a0d2-502d-40db-a579-9a9643de95ed	2c4bbfbc-96d6-4e98-b778-a375c6c2ab0d	cb4cd22c-d4cd-4f2f-ad43-c2e73f1b9cb5	COMPLETED	2026-10-24 07:47:00.000166+00	2026-10-24 07:47:00.000166+00	2026-10-25 05:54:00.002125+00	SD-UJ5H-MYJ5	225 High Level Road, Maharagama	201 Main Street, Negombo	Nipuni Samarasinghe	MEDIUM	3.25	DELIVERED
6af2da8d-5dbf-4868-a7f9-f70cdcbfc0a1	c678a650-a95e-412c-b709-f79ff8ab3f3a	2c4bbfbc-96d6-4e98-b778-a375c6c2ab0d	3900455f-0834-4d57-85c5-c3de92f5ecee	COMPLETED	2026-10-24 11:06:59.999556+00	2026-10-24 11:06:59.999556+00	2026-10-25 06:06:00.002846+00	SD-XDL5-PPY7	92 Havelock Road, Colombo 05	186 Hotel Road, Mount Lavinia	Anjali Liyanage	MEDIUM	2.80	DELIVERED
6de83d1a-4d54-426b-9197-354b3c08cc79	ccb4457b-105d-49e1-a3be-3c09364b87a2	7fb36c3a-09c3-4c20-b5b6-d00d8d230cd5	cb4cd22c-d4cd-4f2f-ad43-c2e73f1b9cb5	COMPLETED	2026-10-25 04:54:59.999987+00	2026-10-25 04:54:59.999987+00	2026-10-25 10:03:00.002654+00	SD-Q6SQ-3E32	229 Church Street, Galle Fort	214 Church Street, Galle Fort	Sahan Ratnayake	MEDIUM	4.01	DELIVERED
868775b1-8df6-475f-b3eb-65c852c850d3	3be21c6e-d0ad-4c8e-b1a6-49939ccef259	72186f7d-7dd3-4e09-8551-045f309d0a7e	7bf5a598-1908-4a34-9ac4-041d3ce99466	COMPLETED	2026-10-25 05:19:00.000282+00	2026-10-25 05:19:00.000282+00	2026-10-26 07:23:00.002165+00	SD-8BX8-NG5N	51 Peradeniya Road, Kandy	338 Peradeniya Road, Kandy	Nipuni Rajendran	LARGE	13.34	DELIVERED
7ecfd356-577e-43c4-bc53-2d4973d20c46	01d70cc4-f0c0-4923-b33e-0766c1422a58	86d11ba9-d0e8-4301-8403-9903afbd11a9	7bf5a598-1908-4a34-9ac4-041d3ce99466	COMPLETED	2026-10-25 08:53:59.999683+00	2026-10-25 08:53:59.999684+00	2026-10-26 05:49:00.002778+00	SD-TTBN-PZCZ	105 Maradana Road, Colombo 10	142 Pita Kotte Road, Kotte	Dulani Sivakumar	MEDIUM	9.42	DELIVERED
6ce843d3-8d65-4211-944a-4151403bc422	06953d4c-fa58-40b6-9ad7-2af29679740c	72186f7d-7dd3-4e09-8551-045f309d0a7e	cb4cd22c-d4cd-4f2f-ad43-c2e73f1b9cb5	COMPLETED	2026-10-25 10:15:59.999652+00	2026-10-25 10:15:59.999652+00	2026-10-26 06:45:00.00166+00	SD-VWKQ-Y8MW	180 Kollupitiya Road, Colombo 03	289 Stanley Thilakaratne Mawatha, Nugegoda	Kavindi Mendis	EXTRA_LARGE	24.83	DELIVERED
747973a6-171a-4a98-8e99-6b673ff29121	cb43e333-5bcb-4310-9897-05feba028af4	0175f24c-4200-4492-84f2-030a2c0cc5ca	3900455f-0834-4d57-85c5-c3de92f5ecee	COMPLETED	2026-10-25 13:48:00.000404+00	2026-10-25 13:48:00.000404+00	2026-10-26 06:13:00.001996+00	SD-TD9Q-YZQZ	225 High Level Road, Maharagama	328 Prince of Wales Avenue, Colombo 14	Sunil Mendis	SMALL	3.04	DELIVERED
75ace23e-926b-4d5d-a5a7-3ff9cd6169d0	0f9dd69f-ad77-489c-afb6-7eb48100a761	0175f24c-4200-4492-84f2-030a2c0cc5ca	cb4cd22c-d4cd-4f2f-ad43-c2e73f1b9cb5	COMPLETED	2026-10-26 08:27:00.000438+00	2026-10-26 08:27:00.000438+00	2026-10-27 04:37:00.001706+00	SD-ZMBW-UCFV	92 Havelock Road, Colombo 05	119 Marine Drive, Colombo 06	Ishara Fernando	MEDIUM	4.49	DELIVERED
5dc3dfa5-85f0-43ad-b350-b05a050c251d	19ddc3e9-6381-4a0e-9216-3f689a542fa3	d4d6a9d7-39f3-45d3-ab66-553e4aee8b84	3900455f-0834-4d57-85c5-c3de92f5ecee	COMPLETED	2026-10-26 10:25:59.999788+00	2026-10-26 10:25:59.999789+00	2026-10-27 04:32:00.002378+00	SD-UWA5-RKP3	229 Church Street, Galle Fort	43 Old Kesbewa Road, Nugegoda	Dinuka Jayawardena	LARGE	11.83	DELIVERED
9bcef941-1afb-43a8-a21a-5e3a49b37511	528fc99f-926c-4071-bc21-aa49e94d472a	0c248632-4e7b-4ade-893f-f0a7d8ae0cfb	cb4cd22c-d4cd-4f2f-ad43-c2e73f1b9cb5	COMPLETED	2026-10-27 12:19:59.999697+00	2026-10-27 12:19:59.999697+00	2026-10-28 05:56:00.002189+00	SD-S6AZ-8M34	51 Peradeniya Road, Kandy	157 De Saram Road, Mount Lavinia	Anjali Nawaz	SMALL	1.63	DELIVERED
fecb294a-a4e0-4040-be14-6036e3a91db8	59fe7a84-1af9-431b-ae09-d59c58b8c506	72186f7d-7dd3-4e09-8551-045f309d0a7e	7bf5a598-1908-4a34-9ac4-041d3ce99466	COMPLETED	2026-10-28 08:25:00.000259+00	2026-10-28 08:25:00.00026+00	2026-10-28 11:24:00.002219+00	SD-GYQN-74ZX	105 Maradana Road, Colombo 10	339 Flower Road, Colombo 07	Dilani Dissanayake	LARGE	9.19	DELIVERED
76add8e9-bb4a-4acd-9a35-e34a2e68126c	4db772c3-cd9f-42d8-92a3-a24441cba66c	86d11ba9-d0e8-4301-8403-9903afbd11a9	cb4cd22c-d4cd-4f2f-ad43-c2e73f1b9cb5	COMPLETED	2026-10-28 08:37:00.000047+00	2026-10-28 08:37:00.000047+00	2026-10-29 06:46:00.003038+00	SD-A789-G989	180 Kollupitiya Road, Colombo 03	160 Ward Place, Colombo 07	Priya Dissanayake	LARGE	16.62	DELIVERED
5e716482-dac9-4edf-9bf4-9ae3435804e3	d1909a37-24c6-4751-8f40-138cf8870506	86d11ba9-d0e8-4301-8403-9903afbd11a9	cb4cd22c-d4cd-4f2f-ad43-c2e73f1b9cb5	COMPLETED	2026-10-29 08:28:00.00003+00	2026-10-29 08:28:00.00003+00	2026-10-30 08:00:00.002349+00	SD-TLPS-B3JZ	225 High Level Road, Maharagama	161 Peradeniya Road, Kandy	Supuni Sivakumar	SMALL	2.05	DELIVERED
6518b210-f51b-4bf1-a886-6d3d31835f9c	a24cd3d8-b457-42b3-a832-23ac64e36d48	2c4bbfbc-96d6-4e98-b778-a375c6c2ab0d	cb4cd22c-d4cd-4f2f-ad43-c2e73f1b9cb5	COMPLETED	2026-10-29 08:34:00.00002+00	2026-10-29 08:34:00.000021+00	2026-10-30 04:52:00.001706+00	SD-E39J-BNZ6	92 Havelock Road, Colombo 05	36 Prince of Wales Avenue, Colombo 14	Dilani Rajendran	SMALL	1.34	DELIVERED
3b1decc4-8901-449b-85ca-bd6e5ba2e51e	a63cb0ff-ddbf-4f2b-8359-8d1edb3a9e51	7fb36c3a-09c3-4c20-b5b6-d00d8d230cd5	cb4cd22c-d4cd-4f2f-ad43-c2e73f1b9cb5	COMPLETED	2026-10-29 12:44:59.999962+00	2026-10-29 12:44:59.999962+00	2026-10-30 04:46:00.002172+00	SD-ZUN5-F3DJ	229 Church Street, Galle Fort	240 Parliament Road, Rajagiriya	Mohamed Herath	SMALL	4.30	DELIVERED
f30dac8c-371e-4336-b70c-a0acab03144b	8f55cfc5-a5d8-48c0-8af3-fab66487b40d	d4d6a9d7-39f3-45d3-ab66-553e4aee8b84	7bf5a598-1908-4a34-9ac4-041d3ce99466	COMPLETED	2026-10-30 07:03:00.000446+00	2026-10-30 07:03:00.000446+00	2026-10-31 06:20:00.00238+00	SD-MJSL-4HNV	51 Peradeniya Road, Kandy	266 Ramakrishna Road, Colombo 06	Hashini Samarasinghe	SMALL	1.49	DELIVERED
20abd13f-b6a1-42f6-b0d3-b410417e057c	c38fd52c-d6b0-4697-b452-1f6985500841	2c4bbfbc-96d6-4e98-b778-a375c6c2ab0d	cb4cd22c-d4cd-4f2f-ad43-c2e73f1b9cb5	FAILED	2026-10-30 10:22:00.000266+00	2026-10-30 10:22:00.000267+00	2026-10-31 08:33:00.002265+00	SD-E7G2-PV6U	105 Maradana Road, Colombo 10	258 Duplication Road, Colombo 03	Ruwan Jayasinghe	SMALL	1.21	FAILED
de28093d-2eb7-4768-bd5c-6b51d708cdda	1611654b-7391-4819-bb9a-749efaf76799	8b77ede6-e12d-4a24-9795-f3c87c19d2ba	cb4cd22c-d4cd-4f2f-ad43-c2e73f1b9cb5	COMPLETED	2026-10-30 11:45:00.000295+00	2026-10-30 11:45:00.000296+00	2026-10-31 06:36:00.002456+00	SD-6X3C-NQKM	180 Kollupitiya Road, Colombo 03	110 Hotel Road, Mount Lavinia	Nipuni Rajapaksa	LARGE	16.74	DELIVERED
7555f932-e06a-40fc-88d8-7bda7431cbe0	2e369813-cb50-42bc-abfb-6a92062e879a	7fb36c3a-09c3-4c20-b5b6-d00d8d230cd5	3900455f-0834-4d57-85c5-c3de92f5ecee	FAILED	2026-10-31 06:15:59.999859+00	2026-10-31 06:15:59.999859+00	2026-11-01 04:57:00.002545+00	SD-RFR2-GUZ9	225 High Level Road, Maharagama	30 High Level Road, Maharagama	Lakshmi Senanayake	LARGE	15.60	FAILED
40a257c9-aef1-42a8-b3bd-21983b5c83a9	06e8fa20-d3e4-4c0e-80ff-d6279c8733f1	7fb36c3a-09c3-4c20-b5b6-d00d8d230cd5	7bf5a598-1908-4a34-9ac4-041d3ce99466	COMPLETED	2026-10-31 08:57:59.999569+00	2026-10-31 08:57:59.999569+00	2026-11-01 05:54:00.001771+00	SD-7A37-M8N9	92 Havelock Road, Colombo 05	278 De Saram Road, Mount Lavinia	Sajith Jayasinghe	LARGE	13.84	DELIVERED
2346dc8e-57a0-4c3d-b03d-d9e8cdc52cc9	d7846298-e4ca-4d1c-8318-837b36328301	7fb36c3a-09c3-4c20-b5b6-d00d8d230cd5	3900455f-0834-4d57-85c5-c3de92f5ecee	COMPLETED	2026-11-01 05:41:00.000103+00	2026-11-01 05:41:00.000103+00	2026-11-02 03:31:00.00231+00	SD-F9CN-TX7Q	229 Church Street, Galle Fort	25 Dalada Veediya, Kandy	Thisaru Silva	SMALL	0.30	DELIVERED
1154f6ed-fb21-4e89-a949-c90b573cd664	9a4b5e25-dfff-4edc-91b1-a05f2832a67b	86d11ba9-d0e8-4301-8403-9903afbd11a9	3900455f-0834-4d57-85c5-c3de92f5ecee	COMPLETED	2026-11-01 10:11:59.999799+00	2026-11-01 10:11:59.999799+00	2026-11-02 05:47:00.003053+00	SD-L9VQ-EGNH	51 Peradeniya Road, Kandy	12 Nawala Road, Rajagiriya	Mohamed Abeysekera	LARGE	6.40	DELIVERED
027f7418-3bd9-4b38-baf5-acea63972e79	05b4344c-e074-4021-93fb-a371765831f9	8b77ede6-e12d-4a24-9795-f3c87c19d2ba	3900455f-0834-4d57-85c5-c3de92f5ecee	COMPLETED	2026-11-01 11:59:00.000245+00	2026-11-01 11:59:00.000245+00	2026-11-02 07:00:00.002676+00	SD-YTP2-FKXD	105 Maradana Road, Colombo 10	128 Galle Road, Dehiwala	Malith Silva	MEDIUM	8.46	DELIVERED
5d653d09-17ca-4ff8-a20a-5926155e7969	f0a6bc09-e868-4dc3-8f17-b05d0390fa83	72186f7d-7dd3-4e09-8551-045f309d0a7e	cb4cd22c-d4cd-4f2f-ad43-c2e73f1b9cb5	COMPLETED	2026-11-01 13:48:59.999936+00	2026-11-01 13:48:59.999936+00	2026-11-02 07:11:00.002929+00	SD-WNRY-WYLP	180 Kollupitiya Road, Colombo 03	13 Hill Street, Dehiwala	Pradeep Silva	MEDIUM	7.31	DELIVERED
fb2c138b-795a-4da6-80a4-5a32c8b6b093	c6e7b5e4-b0fe-4573-a0c0-d94898d2df8b	8b77ede6-e12d-4a24-9795-f3c87c19d2ba	7bf5a598-1908-4a34-9ac4-041d3ce99466	COMPLETED	2026-11-01 13:43:00.000366+00	2026-11-01 13:43:00.000366+00	2026-11-02 06:50:00.002613+00	SD-J7EG-JHKG	225 High Level Road, Maharagama	42 Galle Road, Colombo 04	Senuri Rajapaksa	MEDIUM	8.86	DELIVERED
1b7b380c-3e40-4c91-aa4c-45e3d728049c	e86a30fa-35b9-4a40-91b4-f7fa18266198	72186f7d-7dd3-4e09-8551-045f309d0a7e	7bf5a598-1908-4a34-9ac4-041d3ce99466	COMPLETED	2026-11-01 13:50:00.000295+00	2026-11-01 13:50:00.000295+00	2026-11-02 06:23:00.001551+00	SD-MN22-4N2N	92 Havelock Road, Colombo 05	47 De Saram Road, Mount Lavinia	Yasiru Senanayake	SMALL	0.30	DELIVERED
7c5c89db-b1a2-40ec-84d9-9e499b52fe44	566b897b-ca92-4026-8756-e20a065dbacf	86d11ba9-d0e8-4301-8403-9903afbd11a9	cb4cd22c-d4cd-4f2f-ad43-c2e73f1b9cb5	COMPLETED	2026-11-02 13:42:59.99985+00	2026-11-02 13:42:59.99985+00	2026-11-03 04:06:00.002429+00	SD-MDPW-G9MX	229 Church Street, Galle Fort	288 Lewis Place, Negombo	Kamal Dissanayake	SMALL	3.46	DELIVERED
b0bb3490-4dc8-4507-8d57-a92437f0e6f8	5eebf527-d6b1-425f-a998-09a21d88a3ea	72186f7d-7dd3-4e09-8551-045f309d0a7e	7bf5a598-1908-4a34-9ac4-041d3ce99466	COMPLETED	2026-11-03 04:22:59.999824+00	2026-11-03 04:22:59.999824+00	2026-11-04 04:19:00.002042+00	SD-7494-2297	51 Peradeniya Road, Kandy	62 Main Street, Colombo 11	Kamal Liyanage	SMALL	4.79	DELIVERED
f65fc1bf-e0a0-44cc-88ec-0b2ea7fde6e6	6a0ffa21-c149-4a91-b4c2-187a44e4fb45	72186f7d-7dd3-4e09-8551-045f309d0a7e	3900455f-0834-4d57-85c5-c3de92f5ecee	COMPLETED	2026-11-05 04:28:59.999705+00	2026-11-05 04:28:59.999705+00	2026-11-06 05:03:00.002307+00	SD-8QEY-Z5PM	105 Maradana Road, Colombo 10	22 Colombo Road, Negombo	Tharindu Amarasekara	MEDIUM	6.90	DELIVERED
e06622b7-5e6a-4b8f-be36-ba758deff387	79601ad6-c87f-4c10-afee-dbb79f31c8d2	0175f24c-4200-4492-84f2-030a2c0cc5ca	cb4cd22c-d4cd-4f2f-ad43-c2e73f1b9cb5	COMPLETED	2026-11-07 11:12:59.999731+00	2026-11-07 11:12:59.999731+00	2026-11-08 08:20:00.002003+00	SD-DUG4-56K8	180 Kollupitiya Road, Colombo 03	177 De Saram Road, Mount Lavinia	Dinuka Dissanayake	SMALL	3.82	DELIVERED
4a72d4b5-85fd-404f-83b7-13f1fd6b4bc5	d7ad2fd9-f1d2-42eb-9238-dca139cf4877	2c4bbfbc-96d6-4e98-b778-a375c6c2ab0d	3900455f-0834-4d57-85c5-c3de92f5ecee	COMPLETED	2026-11-07 11:18:00.000398+00	2026-11-07 11:18:00.000398+00	2026-11-08 03:57:00.002611+00	SD-9E4S-SFBA	225 High Level Road, Maharagama	214 Pita Kotte Road, Kotte	Arun Liyanage	SMALL	0.81	DELIVERED
4aa2d15b-8422-42d0-a74e-f10164006c1a	c0e8575b-3d0f-4c81-b1ac-b357a77c8d40	8b77ede6-e12d-4a24-9795-f3c87c19d2ba	7bf5a598-1908-4a34-9ac4-041d3ce99466	COMPLETED	2026-11-07 12:44:59.999899+00	2026-11-07 12:44:59.9999+00	2026-11-08 04:51:00.002093+00	SD-KXHS-6SRX	92 Havelock Road, Colombo 05	63 Ramakrishna Road, Colombo 06	Kasun Amarasekara	LARGE	16.05	DELIVERED
cf3f9396-1cdb-465b-a84f-633b29018547	108efbb7-3487-44d6-8cd0-d042ae95821d	64dd446f-7e03-421f-a2f0-05db7f2bc36c	cb4cd22c-d4cd-4f2f-ad43-c2e73f1b9cb5	COMPLETED	2026-11-08 08:21:00.00038+00	2026-11-08 08:21:00.00038+00	2026-11-09 06:38:00.002769+00	SD-LLPZ-SLCZ	229 Church Street, Galle Fort	71 High Level Road, Maharagama	Kavindi Rajendran	SMALL	4.11	DELIVERED
335acee3-fa77-4d09-9d8c-1ffd59d250b9	411ce78f-b85b-4c67-8062-7642ed48d8a3	2c4bbfbc-96d6-4e98-b778-a375c6c2ab0d	cb4cd22c-d4cd-4f2f-ad43-c2e73f1b9cb5	COMPLETED	2026-11-09 02:54:00.00033+00	2026-11-09 02:54:00.00033+00	2026-11-10 06:51:00.002002+00	SD-6YEU-YHDW	51 Peradeniya Road, Kandy	295 Havelock Road, Colombo 05	Kumar Dissanayake	LARGE	19.36	DELIVERED
73cf6143-6746-482a-9d12-ef3ce40921da	1c69adda-384d-4440-bbff-c29d5e839c24	0175f24c-4200-4492-84f2-030a2c0cc5ca	7bf5a598-1908-4a34-9ac4-041d3ce99466	COMPLETED	2026-11-09 04:26:59.999964+00	2026-11-09 04:26:59.999964+00	2026-11-09 11:38:00.002146+00	SD-WDLM-SWK7	105 Maradana Road, Colombo 10	176 Old Kesbewa Road, Nugegoda	Senuri Silva	MEDIUM	8.81	DELIVERED
8af32062-2d07-4744-920f-fb2fa638ef85	e9160c13-1fa8-433a-b0f2-2531eed818f7	72186f7d-7dd3-4e09-8551-045f309d0a7e	cb4cd22c-d4cd-4f2f-ad43-c2e73f1b9cb5	COMPLETED	2026-11-09 07:00:00.000493+00	2026-11-09 07:00:00.000493+00	2026-11-10 07:20:00.002334+00	SD-PNMV-72MT	180 Kollupitiya Road, Colombo 03	256 High Level Road, Maharagama	Sahan Bandara	MEDIUM	3.24	DELIVERED
54bb134d-e8ab-47ba-b2f3-32b49f9def7f	30bcf46b-f722-41ba-8028-952a94dd7637	7fb36c3a-09c3-4c20-b5b6-d00d8d230cd5	3900455f-0834-4d57-85c5-c3de92f5ecee	COMPLETED	2026-11-09 08:14:00.000106+00	2026-11-09 08:14:00.000106+00	2026-11-10 06:01:00.001959+00	SD-EEYS-7D48	225 High Level Road, Maharagama	303 Flower Road, Colombo 07	Sahan Silva	EXTRA_LARGE	33.69	DELIVERED
c01f9bca-34f5-4ae0-a60b-13181a54a51f	8886a196-2f7d-453b-bee2-8646a67323ce	d4d6a9d7-39f3-45d3-ab66-553e4aee8b84	3900455f-0834-4d57-85c5-c3de92f5ecee	COMPLETED	2026-11-09 07:43:59.999787+00	2026-11-09 07:43:59.999787+00	2026-11-10 07:05:00.002303+00	SD-F69D-MG2Z	92 Havelock Road, Colombo 05	85 High Level Road, Maharagama	Malith Dissanayake	MEDIUM	2.64	DELIVERED
7dd6cf24-b2d2-4541-b813-d00b14c9256f	c8bbbf0b-28b5-4d39-84f2-0cab9a297379	86d11ba9-d0e8-4301-8403-9903afbd11a9	3900455f-0834-4d57-85c5-c3de92f5ecee	COMPLETED	2026-11-09 11:35:00.000346+00	2026-11-09 11:35:00.000346+00	2026-11-10 06:46:00.001733+00	SD-JLGL-8WNF	229 Church Street, Galle Fort	193 Main Street, Colombo 11	Dimuth Jayawardena	MEDIUM	3.09	DELIVERED
543c9533-3716-4548-aa8e-2657a9cdb61c	24c56c10-80a3-4dbc-9ff2-65d251ec1bfa	7fb36c3a-09c3-4c20-b5b6-d00d8d230cd5	7bf5a598-1908-4a34-9ac4-041d3ce99466	COMPLETED	2026-11-10 05:16:00.000066+00	2026-11-10 05:16:00.000066+00	2026-11-10 13:06:00.001739+00	SD-UG7G-W4N9	51 Peradeniya Road, Kandy	128 Colombo Road, Negombo	Ayesha Cooray	MEDIUM	6.11	DELIVERED
06cd25fd-8240-4eaa-971f-f350ed21ffe2	832f0466-ef0f-4885-b959-228ac6fe0a2e	0c248632-4e7b-4ade-893f-f0a7d8ae0cfb	7bf5a598-1908-4a34-9ac4-041d3ce99466	FAILED	2026-11-10 10:03:00.00042+00	2026-11-10 10:03:00.00042+00	2026-11-11 05:17:00.001907+00	SD-N2PQ-8Q5K	105 Maradana Road, Colombo 10	27 Old Kesbewa Road, Nugegoda	Anjali Bandara	SMALL	3.18	FAILED
1f33af0a-9274-4937-931f-3a7730285b5c	12a107c5-9ef1-40d4-bb55-c104c8037b0b	0175f24c-4200-4492-84f2-030a2c0cc5ca	cb4cd22c-d4cd-4f2f-ad43-c2e73f1b9cb5	COMPLETED	2026-11-10 12:16:00.000427+00	2026-11-10 12:16:00.000427+00	2026-11-11 08:45:00.002437+00	SD-RTZM-4RRL	180 Kollupitiya Road, Colombo 03	300 York Street, Colombo 01	Chamari Rajapaksa	SMALL	3.24	DELIVERED
f0163113-0ee2-4d18-a854-829f2baf2ade	a461794b-a39d-4397-af2e-7e5e58e52976	7fb36c3a-09c3-4c20-b5b6-d00d8d230cd5	3900455f-0834-4d57-85c5-c3de92f5ecee	COMPLETED	2026-11-10 12:42:59.999771+00	2026-11-10 12:42:59.999771+00	2026-11-11 07:34:00.001896+00	SD-QLA9-N578	225 High Level Road, Maharagama	26 Station Road, Nugegoda	Sahan Senanayake	LARGE	19.30	DELIVERED
5c2fcb5e-0b83-4b2d-b700-ac41e51f0621	a625183e-ee80-4261-bf29-f7013f3f1a80	8b77ede6-e12d-4a24-9795-f3c87c19d2ba	7bf5a598-1908-4a34-9ac4-041d3ce99466	COMPLETED	2026-11-10 12:47:00.00003+00	2026-11-10 12:47:00.00003+00	2026-11-11 05:19:00.00242+00	SD-KQFQ-2PU3	92 Havelock Road, Colombo 05	43 Dalada Veediya, Kandy	Pradeep Abeysekera	MEDIUM	3.85	DELIVERED
06ad9290-b637-4ca5-8d37-c347eefd9586	7dc8dc48-7f34-4a6a-900c-af00461bd037	2c4bbfbc-96d6-4e98-b778-a375c6c2ab0d	3900455f-0834-4d57-85c5-c3de92f5ecee	COMPLETED	2026-11-11 03:44:00.000226+00	2026-11-11 03:44:00.000226+00	2026-11-12 08:25:00.002302+00	SD-P3HR-6NT8	229 Church Street, Galle Fort	50 Havelock Road, Colombo 05	Rizwan Karunaratne	MEDIUM	7.18	DELIVERED
04ee1ebb-75e0-4f71-89a0-4b4ab9a93afd	e8ec1d31-46d2-4727-baad-3f8abdda52b5	d4d6a9d7-39f3-45d3-ab66-553e4aee8b84	3900455f-0834-4d57-85c5-c3de92f5ecee	COMPLETED	2026-11-11 05:34:59.999634+00	2026-11-11 05:34:59.999634+00	2026-11-12 06:20:00.001934+00	SD-EUR5-B8ZQ	51 Peradeniya Road, Kandy	160 De Saram Road, Mount Lavinia	Chamari Sivakumar	SMALL	4.87	DELIVERED
28d36a64-e7c9-4120-abe6-dc9054014f9f	bb7b183e-59e6-4dff-961b-9ba22cfbf8ef	0c248632-4e7b-4ade-893f-f0a7d8ae0cfb	7bf5a598-1908-4a34-9ac4-041d3ce99466	COMPLETED	2026-11-11 13:04:59.999899+00	2026-11-11 13:04:59.999899+00	2026-11-12 07:39:00.002413+00	SD-4X7S-MCJY	105 Maradana Road, Colombo 10	178 Galle Road, Colombo 03	Nipuni Wickramasinghe	SMALL	0.38	DELIVERED
e9c020f0-b5cb-4c1d-8fc8-85bfb42fc2a2	2ac4bc9b-c256-4624-94cb-586287dab302	64dd446f-7e03-421f-a2f0-05db7f2bc36c	cb4cd22c-d4cd-4f2f-ad43-c2e73f1b9cb5	FAILED	2026-11-12 03:28:59.999579+00	2026-11-12 03:28:59.99958+00	2026-11-13 05:39:00.002916+00	SD-JRTA-DN6Z	180 Kollupitiya Road, Colombo 03	224 Havelock Road, Colombo 05	Sanduni Rajendran	MEDIUM	3.85	FAILED
7fc15bbf-99b4-4f5a-afdb-964d665978ae	fa0a73d0-9bf6-4ce2-aae4-6303ce7aa3eb	64dd446f-7e03-421f-a2f0-05db7f2bc36c	7bf5a598-1908-4a34-9ac4-041d3ce99466	COMPLETED	2026-11-12 09:12:59.999964+00	2026-11-12 09:12:59.999964+00	2026-11-13 07:38:00.003447+00	SD-M7ZS-4246	225 High Level Road, Maharagama	275 Galle Road, Dehiwala	Sachini Rathnayake	SMALL	2.17	DELIVERED
22ca1be6-7d0d-4c33-bd26-1ca7396db5e9	62d179e6-7752-4058-a412-5f8849d3e08d	0c248632-4e7b-4ade-893f-f0a7d8ae0cfb	7bf5a598-1908-4a34-9ac4-041d3ce99466	FAILED	2026-11-12 10:16:00.000179+00	2026-11-12 10:16:00.00018+00	2026-11-13 07:01:00.001785+00	SD-J58S-AEJC	92 Havelock Road, Colombo 05	223 William Gopallawa Mawatha, Kandy	Dinuka Senanayake	SMALL	0.30	FAILED
8dcf07b7-8b2b-4abb-82f1-8200514d94b3	d07d7a58-1f08-4821-a51d-1896473f5f03	0175f24c-4200-4492-84f2-030a2c0cc5ca	cb4cd22c-d4cd-4f2f-ad43-c2e73f1b9cb5	COMPLETED	2026-11-13 07:30:59.999707+00	2026-11-13 07:30:59.999708+00	2026-11-14 05:05:00.002208+00	SD-GNHA-23AQ	229 Church Street, Galle Fort	109 High Level Road, Maharagama	Priya Rathnayake	LARGE	15.13	DELIVERED
4dc94717-5480-40a4-89c5-d24d2807016a	389a7263-491d-40d0-80f7-d4a12e442e39	0c248632-4e7b-4ade-893f-f0a7d8ae0cfb	cb4cd22c-d4cd-4f2f-ad43-c2e73f1b9cb5	COMPLETED	2026-11-13 09:10:59.999729+00	2026-11-13 09:10:59.999729+00	2026-11-14 06:50:00.002955+00	SD-268W-7UF3	51 Peradeniya Road, Kandy	252 Lewis Place, Negombo	Nadeesha Cooray	SMALL	1.40	DELIVERED
8c8f3631-c5aa-4431-8372-90bed118348a	c9a261ef-bd5c-48e6-80b3-d8cd74cf7ef3	0175f24c-4200-4492-84f2-030a2c0cc5ca	3900455f-0834-4d57-85c5-c3de92f5ecee	IN_PROGRESS	2026-11-14 03:25:59.999556+00	2026-11-14 03:25:59.999556+00	2026-11-14 05:25:00.001756+00	SD-8GPU-UK9A	105 Maradana Road, Colombo 10	314 Galle Road, Colombo 04	Lahiru Perera	MEDIUM	6.51	IN_TRANSIT
2e336e14-4d32-4de5-b60b-b6e3c31ce134	1fe88d4c-8ada-4052-b597-2d31ebd9d086	2c4bbfbc-96d6-4e98-b778-a375c6c2ab0d	7bf5a598-1908-4a34-9ac4-041d3ce99466	IN_PROGRESS	2026-11-14 05:19:00.000469+00	2026-11-14 05:19:00.000469+00	2026-11-14 07:53:00.002039+00	SD-EUPJ-FMVF	180 Kollupitiya Road, Colombo 03	18 High Level Road, Maharagama	Priya Samarasinghe	LARGE	18.96	IN_TRANSIT
14093a80-5023-426f-8fcc-edf36e59042d	c9736aac-0bdf-4cc9-887a-232f2e8ec46e	0c248632-4e7b-4ade-893f-f0a7d8ae0cfb	cb4cd22c-d4cd-4f2f-ad43-c2e73f1b9cb5	COMPLETED	2026-11-14 07:06:59.999593+00	2026-11-14 07:06:59.999593+00	2026-11-14 13:49:00.002303+00	SD-ZCXL-D77C	225 High Level Road, Maharagama	192 Pita Kotte Road, Kotte	Shehan Herath	MEDIUM	3.33	DELIVERED
1ca40360-b69e-4ab4-ab01-892640afa97c	ade03cc5-c9ce-41b4-b154-fdc57b329c01	7fb36c3a-09c3-4c20-b5b6-d00d8d230cd5	7bf5a598-1908-4a34-9ac4-041d3ce99466	IN_PROGRESS	2026-11-14 07:10:00.00036+00	2026-11-14 07:10:00.00036+00	2026-11-14 08:29:00.001816+00	SD-Z3VH-XBD6	92 Havelock Road, Colombo 05	7 Old Kesbewa Road, Nugegoda	Kumar Fonseka	EXTRA_LARGE	27.78	IN_TRANSIT
0c541a8f-6347-4f55-af4f-5f31f0384089	e4548b77-2a75-4044-869d-e9fe3281caef	2c4bbfbc-96d6-4e98-b778-a375c6c2ab0d	7bf5a598-1908-4a34-9ac4-041d3ce99466	IN_PROGRESS	2026-11-14 08:19:59.999669+00	2026-11-14 08:19:59.999669+00	2026-11-14 12:05:00.001585+00	SD-M9JH-E5EA	229 Church Street, Galle Fort	208 Parliament Road, Rajagiriya	Kumar Gunasekara	LARGE	14.40	IN_TRANSIT
17d02aca-591a-42ac-be15-c01728dda057	200664de-108e-4bf6-986c-0e2d760b7aea	0175f24c-4200-4492-84f2-030a2c0cc5ca	cb4cd22c-d4cd-4f2f-ad43-c2e73f1b9cb5	IN_PROGRESS	2026-11-14 08:59:00.00007+00	2026-11-14 08:59:00.00007+00	2026-11-14 13:55:00.001937+00	SD-9PYR-PG8C	51 Peradeniya Road, Kandy	182 Stanley Thilakaratne Mawatha, Nugegoda	Chathura Herath	MEDIUM	3.49	IN_TRANSIT
7be3dfa1-cc51-4b4e-85d3-4075fa1a12a0	fc35546b-ea0b-4d24-b268-2873dc0b87d8	d4d6a9d7-39f3-45d3-ab66-553e4aee8b84	cb4cd22c-d4cd-4f2f-ad43-c2e73f1b9cb5	ASSIGNED	2026-11-14 12:34:59.99957+00	2026-11-14 12:34:59.99957+00	2026-11-14 12:17:15.79957+00	SD-YQYU-BVZ4	105 Maradana Road, Colombo 10	321 Pita Kotte Road, Kotte	Sanduni Karunaratne	SMALL	1.85	CREATED
\.


--
-- Data for Name: couriers; Type: TABLE DATA; Schema: public; Owner: dispatch
--

COPY public.couriers (id, user_id, status, created_at, updated_at) FROM stdin;
86d11ba9-d0e8-4301-8403-9903afbd11a9	9e3b8266-acf9-4126-91bb-369d59ad25a7	AVAILABLE	2026-08-13 12:07:00+00	\N
72186f7d-7dd3-4e09-8551-045f309d0a7e	69f3dbf3-6a51-4f41-8634-af3d608c2527	AVAILABLE	2026-08-29 07:37:00+00	\N
7fb36c3a-09c3-4c20-b5b6-d00d8d230cd5	2296db41-b3a5-4443-a906-df1b2a23b26c	AVAILABLE	2026-08-02 10:06:00+00	\N
0175f24c-4200-4492-84f2-030a2c0cc5ca	5b32b607-0ba6-4ff9-8b45-e7e15baf395b	AVAILABLE	2026-08-30 13:19:00+00	\N
0c248632-4e7b-4ade-893f-f0a7d8ae0cfb	fd5016f3-f6ab-462f-a5ed-0a2fb7ee2491	AVAILABLE	2026-07-31 06:02:00+00	\N
d4d6a9d7-39f3-45d3-ab66-553e4aee8b84	2064432d-0cca-4c76-ac2b-47cd07270ec7	AVAILABLE	2026-07-14 11:52:00+00	\N
64dd446f-7e03-421f-a2f0-05db7f2bc36c	c4750ad6-4949-4e9e-b59c-1c8af8f71294	AVAILABLE	2026-07-24 10:30:00+00	\N
2c4bbfbc-96d6-4e98-b778-a375c6c2ab0d	d28074b5-475a-4e20-b5d3-63080c9f1b7f	AVAILABLE	2026-08-12 07:32:00+00	\N
8b77ede6-e12d-4a24-9795-f3c87c19d2ba	854d0d01-8a43-4f8c-b742-ee0202e4e1b8	AVAILABLE	2026-08-18 13:04:00+00	\N
adc7e28d-fe54-4d27-9d7c-98a98b07040f	3be525c0-b776-4ec1-b343-38831e36255d	OFFLINE	2026-09-02 04:40:00+00	\N
\.


--
-- Data for Name: daily_stats; Type: TABLE DATA; Schema: public; Owner: dispatch
--

COPY public.daily_stats (id, date, courier_id, assigned_count, completed_count, failed_count, created_at, updated_at) FROM stdin;
8b9abfef-06d0-4463-9549-607d5425eab2	2026-10-30	d4d6a9d7-39f3-45d3-ab66-553e4aee8b84	3	3	0	2026-10-30 10:09:59.999845+00	2026-10-31 07:18:00.002276+00
4acfed8d-06b6-4979-9579-13d78cb0840b	2026-10-25	86d11ba9-d0e8-4301-8403-9903afbd11a9	2	2	0	2026-10-25 08:53:59.999683+00	2026-10-26 05:54:00.002927+00
8f8597c3-1384-45a5-89eb-5798b23909d6	2026-10-21	64dd446f-7e03-421f-a2f0-05db7f2bc36c	1	1	0	2026-10-21 05:38:00.000237+00	2026-10-22 06:51:00.002668+00
7b3d42d0-c523-4885-8801-a4fd32002d63	2026-11-14	7fb36c3a-09c3-4c20-b5b6-d00d8d230cd5	2	0	0	2026-11-14 08:08:59.999829+00	2026-11-14 10:48:00.002635+00
c2ac90e3-0e66-411e-bc61-ca1972c82b3b	2026-10-26	64dd446f-7e03-421f-a2f0-05db7f2bc36c	1	1	0	2026-10-26 13:21:00.000455+00	2026-10-27 07:20:00.002681+00
37a2ae53-764c-4b91-ba1b-816fbcaa1228	2026-11-11	86d11ba9-d0e8-4301-8403-9903afbd11a9	1	1	0	2026-11-11 03:38:00.000113+00	2026-11-12 03:48:00.002742+00
766916e7-fec9-4253-8877-6f22ed1032bf	2026-11-14	2c4bbfbc-96d6-4e98-b778-a375c6c2ab0d	2	0	0	2026-11-14 08:19:59.999669+00	2026-11-14 12:05:00.001585+00
fbccee84-c66b-4737-985c-5a87c460a68f	2026-11-03	8b77ede6-e12d-4a24-9795-f3c87c19d2ba	1	1	0	2026-11-03 09:23:00.000047+00	2026-11-04 05:16:00.002402+00
5cb581a9-52d5-4ab0-956d-67bdf1a27155	2026-11-05	72186f7d-7dd3-4e09-8551-045f309d0a7e	1	1	0	2026-11-05 04:28:59.999705+00	2026-11-06 05:03:00.002307+00
8e339e91-a4ea-4fc8-9e9a-959598b47b3b	2026-11-04	d4d6a9d7-39f3-45d3-ab66-553e4aee8b84	1	1	0	2026-11-04 11:37:59.999651+00	2026-11-05 06:38:00.002066+00
52e42714-0fbb-428e-a0fe-fdce8c5e09c7	2026-11-10	7fb36c3a-09c3-4c20-b5b6-d00d8d230cd5	3	3	0	2026-11-10 12:42:59.999771+00	2026-11-11 07:34:00.001896+00
bc2bd9af-90cf-41c7-9c8a-75c339af6bc7	2026-11-06	0c248632-4e7b-4ade-893f-f0a7d8ae0cfb	2	2	0	2026-11-06 11:57:59.999629+00	2026-11-07 07:06:00.002976+00
2e120415-2e2b-4d48-8f01-41f739cdaba8	2026-10-23	0c248632-4e7b-4ade-893f-f0a7d8ae0cfb	2	2	0	2026-10-23 08:57:59.999975+00	2026-10-24 07:20:00.003703+00
ebc45bb0-de8b-46fe-9841-27c667431079	2026-10-28	86d11ba9-d0e8-4301-8403-9903afbd11a9	2	2	0	2026-10-28 13:07:00.000163+00	2026-10-29 07:44:00.002762+00
a820eb09-1110-4944-81de-596df297f7b6	2026-11-08	0175f24c-4200-4492-84f2-030a2c0cc5ca	2	1	1	2026-11-08 07:22:00.000085+00	2026-11-09 08:17:00.00232+00
582b0f5c-9444-4bb6-9c16-5468936fbe04	2026-10-23	2c4bbfbc-96d6-4e98-b778-a375c6c2ab0d	1	1	0	2026-10-23 02:57:00.000189+00	2026-10-24 06:19:00.003439+00
4e0acf61-5a9a-4256-a2ca-6dfa43bfcdff	2026-10-16	0c248632-4e7b-4ade-893f-f0a7d8ae0cfb	2	1	1	2026-10-16 06:26:00.000424+00	2026-10-17 06:35:00.004115+00
d75648c9-ede0-4adc-a24e-c3e245fa84aa	2026-11-13	d4d6a9d7-39f3-45d3-ab66-553e4aee8b84	2	2	0	2026-11-13 14:02:59.999931+00	2026-11-14 06:51:00.002439+00
fbd4b80b-4188-4082-bb7e-7576389bdfdc	2026-10-16	72186f7d-7dd3-4e09-8551-045f309d0a7e	1	1	0	2026-10-16 05:35:00.000131+00	2026-10-17 04:21:00.003904+00
1e5e182a-f349-4d68-9d9f-c950ddeabc13	2026-11-10	8b77ede6-e12d-4a24-9795-f3c87c19d2ba	3	2	1	2026-11-10 13:07:59.999914+00	2026-11-11 08:39:00.00316+00
0f6df887-878e-4b60-917d-76b59903092e	2026-10-31	64dd446f-7e03-421f-a2f0-05db7f2bc36c	2	2	0	2026-10-31 07:52:00.000338+00	2026-11-01 05:18:00.004105+00
8018259c-ffc6-44de-a3c1-68ddaafe7d50	2026-10-20	72186f7d-7dd3-4e09-8551-045f309d0a7e	3	3	0	2026-10-20 14:25:00.000235+00	2026-10-21 08:08:00.002177+00
a84c89f4-1be1-429a-bbb8-fbe682d3d06a	2026-10-28	64dd446f-7e03-421f-a2f0-05db7f2bc36c	2	1	1	2026-10-28 13:34:59.9999+00	2026-10-29 04:26:00.002841+00
98208930-89d1-41ee-99cb-434340723351	2026-10-25	2c4bbfbc-96d6-4e98-b778-a375c6c2ab0d	1	1	0	2026-10-25 04:01:00.000012+00	2026-10-26 05:46:00.003365+00
4c36f6f4-d3a2-487a-92ba-da3b7bc0d37a	2026-10-20	7fb36c3a-09c3-4c20-b5b6-d00d8d230cd5	2	1	1	2026-10-20 13:28:59.999656+00	2026-10-21 07:44:00.002922+00
094fb517-c922-4de1-a16e-480b0abadba0	2026-11-09	0c248632-4e7b-4ade-893f-f0a7d8ae0cfb	1	1	0	2026-11-09 05:26:59.999791+00	2026-11-09 11:55:00.002713+00
0abda992-9449-43f9-9ae6-b3ac72711151	2026-10-21	8b77ede6-e12d-4a24-9795-f3c87c19d2ba	1	1	0	2026-10-21 03:57:59.999992+00	2026-10-22 06:37:00.004216+00
f6ed8f5d-d6a7-434d-90bf-3711ecac940b	2026-10-17	2c4bbfbc-96d6-4e98-b778-a375c6c2ab0d	2	2	0	2026-10-17 07:34:00.000068+00	2026-10-18 07:13:00.0023+00
888f8095-4bb1-42d0-8f4d-9ed4a04f6e39	2026-11-02	2c4bbfbc-96d6-4e98-b778-a375c6c2ab0d	1	0	1	2026-11-02 06:42:00.000409+00	2026-11-02 14:13:00.00183+00
603417b3-754a-4f1b-81a1-97cbfc86d60b	2026-11-12	86d11ba9-d0e8-4301-8403-9903afbd11a9	1	0	1	2026-11-12 08:51:00.000069+00	2026-11-13 07:53:00.001865+00
b7376a29-d01e-4cfb-811f-39a9f4788690	2026-10-19	64dd446f-7e03-421f-a2f0-05db7f2bc36c	1	1	0	2026-10-19 10:14:00.000258+00	2026-10-20 08:52:00.003363+00
65a83cd7-319c-4deb-8b0f-b91daedbbc5c	2026-11-03	0c248632-4e7b-4ade-893f-f0a7d8ae0cfb	1	1	0	2026-11-03 14:53:59.999505+00	2026-11-04 04:42:00.003229+00
d773f42c-c6d5-4ee2-8829-ab5d499827bc	2026-10-21	0175f24c-4200-4492-84f2-030a2c0cc5ca	2	1	1	2026-10-21 09:51:59.999687+00	2026-10-22 06:39:00.003414+00
9df2fc98-892a-4c13-bd03-bf40fc57b345	2026-11-06	86d11ba9-d0e8-4301-8403-9903afbd11a9	1	1	0	2026-11-06 09:51:59.999839+00	2026-11-07 06:56:00.003186+00
ece875aa-5263-43d2-834c-0831efca2597	2026-11-13	0175f24c-4200-4492-84f2-030a2c0cc5ca	1	1	0	2026-11-13 07:30:59.999707+00	2026-11-14 05:05:00.002208+00
e3d7c453-0374-46d2-bfa6-de3e91594497	2026-10-17	72186f7d-7dd3-4e09-8551-045f309d0a7e	1	1	0	2026-10-17 09:10:59.99992+00	2026-10-18 03:51:00.003404+00
0858ce07-c82a-4a88-8a45-077dd28b296e	2026-10-26	d4d6a9d7-39f3-45d3-ab66-553e4aee8b84	1	1	0	2026-10-26 10:25:59.999788+00	2026-10-27 04:32:00.002378+00
9dd90f44-e7d4-4289-86a4-c83b2dbc2e2c	2026-10-18	d4d6a9d7-39f3-45d3-ab66-553e4aee8b84	1	1	0	2026-10-18 14:19:59.999849+00	2026-10-19 05:57:00.003379+00
86c85f3d-458c-4f52-b877-46ff3786d748	2026-11-02	0175f24c-4200-4492-84f2-030a2c0cc5ca	1	1	0	2026-11-02 14:29:00.000497+00	2026-11-04 06:01:00.002333+00
5b1ee322-1a40-4d81-a6d6-30a89329f5c2	2026-11-07	64dd446f-7e03-421f-a2f0-05db7f2bc36c	2	2	0	2026-11-07 08:42:59.999865+00	2026-11-08 06:22:00.003269+00
34201402-e352-4660-8593-34609a391c48	2026-10-25	72186f7d-7dd3-4e09-8551-045f309d0a7e	3	3	0	2026-10-25 10:17:59.999782+00	2026-10-26 07:23:00.002165+00
a7ce0132-966f-4a58-8e2e-c8ae72dbfb05	2026-11-06	d4d6a9d7-39f3-45d3-ab66-553e4aee8b84	1	0	1	2026-11-06 06:26:59.99962+00	2026-11-06 12:29:00.001727+00
c1bcc80e-e262-46b5-80ec-65c7cc74bc26	2026-10-24	2c4bbfbc-96d6-4e98-b778-a375c6c2ab0d	4	4	0	2026-10-24 11:06:59.999556+00	2026-10-25 08:18:00.002519+00
c42b3de5-eaba-45f0-b366-2809e47698ad	2026-10-27	d4d6a9d7-39f3-45d3-ab66-553e4aee8b84	1	1	0	2026-10-27 11:04:00.000353+00	2026-10-28 05:34:00.004222+00
d543f16c-9789-4e78-aac8-e1defda5d9b9	2026-11-07	0175f24c-4200-4492-84f2-030a2c0cc5ca	1	1	0	2026-11-07 11:12:59.999731+00	2026-11-08 08:20:00.002003+00
4b47846a-e3a9-4b8b-ae10-f6654f952af0	2026-11-12	72186f7d-7dd3-4e09-8551-045f309d0a7e	2	2	0	2026-11-12 06:14:59.999763+00	2026-11-13 07:35:00.002796+00
2c542348-6e9f-47fe-854c-89196d5d8907	2026-11-14	8b77ede6-e12d-4a24-9795-f3c87c19d2ba	2	0	0	2026-11-14 11:41:59.999881+00	2026-11-14 13:41:00.002868+00
2192cfc4-6390-4160-9f38-ae4b6f94f87f	2026-11-03	86d11ba9-d0e8-4301-8403-9903afbd11a9	2	1	1	2026-11-03 10:46:00.00004+00	2026-11-04 05:46:00.002917+00
8dac10d6-1cbc-4e7d-a3a6-a96afb810536	2026-11-09	0175f24c-4200-4492-84f2-030a2c0cc5ca	3	3	0	2026-11-09 12:23:59.999946+00	2026-11-10 08:07:00.002534+00
038f31f7-f6ca-4f16-a7f6-369656d12568	2026-10-18	8b77ede6-e12d-4a24-9795-f3c87c19d2ba	2	2	0	2026-10-18 12:41:00.000253+00	2026-10-19 05:57:00.003277+00
33839963-b32d-4b20-b0f5-1f2b8bcf2de3	2026-11-02	72186f7d-7dd3-4e09-8551-045f309d0a7e	1	1	0	2026-11-02 08:14:00.000251+00	2026-11-03 08:09:00.002342+00
54f51198-05a6-401b-98aa-27e16df2b10a	2026-10-16	64dd446f-7e03-421f-a2f0-05db7f2bc36c	1	1	0	2026-10-16 08:03:59.999994+00	2026-10-17 05:16:00.001651+00
c49032d8-8b1d-40be-a375-b21321658677	2026-10-25	7fb36c3a-09c3-4c20-b5b6-d00d8d230cd5	2	2	0	2026-10-25 11:53:59.99989+00	2026-10-26 04:30:00.003079+00
a20299f9-c8c4-4215-b2fa-c8741a09ffe1	2026-11-09	2c4bbfbc-96d6-4e98-b778-a375c6c2ab0d	2	2	0	2026-11-09 11:58:00.000351+00	2026-11-10 06:51:00.002002+00
b669c41b-f0e7-419b-82f4-424465eebbba	2026-11-04	7fb36c3a-09c3-4c20-b5b6-d00d8d230cd5	1	1	0	2026-11-04 14:49:59.999721+00	2026-11-05 05:25:00.001917+00
c55e66f4-b93d-4a02-b906-7156b7248fef	2026-11-03	64dd446f-7e03-421f-a2f0-05db7f2bc36c	1	1	0	2026-11-03 04:40:59.999757+00	2026-11-03 12:41:00.002682+00
f6ac4e29-63db-48c7-8440-af70a6e5a9c9	2026-11-10	2c4bbfbc-96d6-4e98-b778-a375c6c2ab0d	2	1	1	2026-11-10 13:22:00.000299+00	2026-11-11 07:02:00.003753+00
66e71efc-bace-4fbc-9e84-5adcb0ac4a5f	2026-11-07	0c248632-4e7b-4ade-893f-f0a7d8ae0cfb	1	1	0	2026-11-07 05:25:59.999694+00	2026-11-08 05:24:00.004327+00
ef10fa2e-9d5e-4629-8a25-4b11046dd8cc	2026-10-23	7fb36c3a-09c3-4c20-b5b6-d00d8d230cd5	2	2	0	2026-10-23 12:51:59.999908+00	2026-10-24 07:46:00.003481+00
7d332a96-d3a4-429c-9024-a1f02d374ba6	2026-10-20	8b77ede6-e12d-4a24-9795-f3c87c19d2ba	1	1	0	2026-10-20 10:23:00.000495+00	2026-10-21 05:13:00.004267+00
9b5473a6-1796-49de-8256-3a7e5204b3c0	2026-10-31	2c4bbfbc-96d6-4e98-b778-a375c6c2ab0d	1	1	0	2026-10-31 09:16:59.999911+00	2026-11-01 07:25:00.002609+00
7dff0ff6-49d0-4665-b6b6-d08e53fdaa20	2026-11-10	86d11ba9-d0e8-4301-8403-9903afbd11a9	2	2	0	2026-11-10 07:06:59.99985+00	2026-11-11 06:16:00.00264+00
0a278aaf-7c68-4a5b-bd39-40a641d34145	2026-10-29	8b77ede6-e12d-4a24-9795-f3c87c19d2ba	1	1	0	2026-10-29 12:05:00.000223+00	2026-10-30 04:17:00.003537+00
ef44cfa1-fa6e-4da7-a6b7-ce45ca3dcf42	2026-10-26	0175f24c-4200-4492-84f2-030a2c0cc5ca	1	1	0	2026-10-26 08:27:00.000438+00	2026-10-27 04:37:00.001706+00
e16a629e-d45b-41e3-a62f-45d1d39d3d03	2026-10-30	64dd446f-7e03-421f-a2f0-05db7f2bc36c	1	1	0	2026-10-30 08:16:00.000385+00	2026-10-31 04:46:00.002988+00
ad25a7f6-2b68-483b-a2c7-a24d08126ffa	2026-10-31	0c248632-4e7b-4ade-893f-f0a7d8ae0cfb	3	3	0	2026-10-31 12:56:59.999641+00	2026-11-01 07:00:00.002853+00
9d2ba416-395d-4c07-9201-ba4d367b1bb7	2026-10-29	0c248632-4e7b-4ade-893f-f0a7d8ae0cfb	1	1	0	2026-10-29 08:31:00.000185+00	2026-10-30 06:41:00.003351+00
5a230c23-0270-4472-b9ca-a757842f30e3	2026-11-01	7fb36c3a-09c3-4c20-b5b6-d00d8d230cd5	1	1	0	2026-11-01 05:41:00.000103+00	2026-11-02 03:31:00.00231+00
0151acf0-7511-4dbd-8a66-9615c00efc14	2026-10-28	2c4bbfbc-96d6-4e98-b778-a375c6c2ab0d	1	1	0	2026-10-28 10:41:00.00042+00	2026-10-29 04:56:00.003737+00
adf2b44d-98c1-445a-9063-2ee2a7b5d927	2026-10-16	86d11ba9-d0e8-4301-8403-9903afbd11a9	1	1	0	2026-10-16 09:23:00.000168+00	2026-10-17 09:12:00.004082+00
ec35d2ee-afa8-4299-9a5f-e42bb349da7a	2026-11-07	7fb36c3a-09c3-4c20-b5b6-d00d8d230cd5	1	1	0	2026-11-07 10:33:00.000496+00	2026-11-08 07:31:00.002528+00
6cc61a6f-5805-4be5-9314-da85abb209e1	2026-10-27	8b77ede6-e12d-4a24-9795-f3c87c19d2ba	1	1	0	2026-10-27 06:04:59.999597+00	2026-10-28 07:20:00.002517+00
69f7f6e5-1519-4cee-9ee9-399d7581bd17	2026-10-20	0c248632-4e7b-4ade-893f-f0a7d8ae0cfb	1	1	0	2026-10-20 13:01:00.000024+00	2026-10-21 04:30:00.003209+00
2a5a25f1-d491-438e-a16d-6c84f7cafef1	2026-11-06	2c4bbfbc-96d6-4e98-b778-a375c6c2ab0d	1	1	0	2026-11-06 14:02:00.000031+00	2026-11-07 07:28:00.00236+00
c630377e-563a-494d-92d1-4ba6c9c9261d	2026-11-11	64dd446f-7e03-421f-a2f0-05db7f2bc36c	1	1	0	2026-11-11 12:41:59.99982+00	2026-11-12 07:06:00.002606+00
79ab9e9e-a008-440e-84ab-492c46f7f5ae	2026-10-19	72186f7d-7dd3-4e09-8551-045f309d0a7e	1	1	0	2026-10-19 13:28:59.999854+00	2026-10-20 03:41:00.002758+00
bd8eedc5-926c-47a6-924a-fce6c050dd3a	2026-10-28	72186f7d-7dd3-4e09-8551-045f309d0a7e	1	1	0	2026-10-28 08:25:00.000259+00	2026-10-28 11:24:00.002219+00
2ed6d6c2-9489-40c0-bff1-5bf0b537cb0c	2026-11-10	0c248632-4e7b-4ade-893f-f0a7d8ae0cfb	1	0	1	2026-11-10 10:03:00.00042+00	2026-11-11 05:17:00.001907+00
8a52eec7-82d1-43bb-be95-b8e2c43294b1	2026-11-14	0175f24c-4200-4492-84f2-030a2c0cc5ca	3	0	0	2026-11-14 14:00:59.999909+00	2026-11-14 13:55:00.001937+00
2e505a2f-4010-4afc-b066-2b4070338ea1	2026-11-09	72186f7d-7dd3-4e09-8551-045f309d0a7e	1	1	0	2026-11-09 07:00:00.000493+00	2026-11-10 07:20:00.002334+00
356c21c2-0378-4df7-b075-3ac2c62278bf	2026-10-24	72186f7d-7dd3-4e09-8551-045f309d0a7e	1	1	0	2026-10-24 10:29:00.000176+00	2026-10-25 05:43:00.003928+00
5a8d52d8-9a8d-4839-af07-fa36849cb759	2026-11-14	86d11ba9-d0e8-4301-8403-9903afbd11a9	1	0	0	2026-11-14 11:07:00.000269+00	2026-11-14 12:45:00.00235+00
e180a3d7-c019-4e93-9bc7-3efb042706bd	2026-11-12	7fb36c3a-09c3-4c20-b5b6-d00d8d230cd5	1	1	0	2026-11-12 07:58:00.000323+00	2026-11-13 04:29:00.002995+00
648170b8-2511-4978-87be-e5ccb61d24b5	2026-10-25	0c248632-4e7b-4ade-893f-f0a7d8ae0cfb	1	1	0	2026-10-25 08:25:00.000371+00	2026-10-26 06:32:00.003228+00
1bed94c3-a7c2-4582-bd3f-26adfaf6cfcb	2026-10-29	64dd446f-7e03-421f-a2f0-05db7f2bc36c	1	1	0	2026-10-29 07:50:59.999705+00	2026-10-30 09:05:00.00306+00
3d7f0b9d-6908-428a-abe1-95baad2ed737	2026-11-03	72186f7d-7dd3-4e09-8551-045f309d0a7e	1	1	0	2026-11-03 04:22:59.999824+00	2026-11-04 04:19:00.002042+00
7a067068-8352-4109-9c5b-5be85d71e134	2026-11-11	2c4bbfbc-96d6-4e98-b778-a375c6c2ab0d	1	1	0	2026-11-11 03:44:00.000226+00	2026-11-12 08:25:00.002302+00
88949fba-8c7a-4db2-89be-a964635da254	2026-10-23	8b77ede6-e12d-4a24-9795-f3c87c19d2ba	1	1	0	2026-10-23 08:24:00.000212+00	2026-10-24 07:19:00.003365+00
dda3ea58-df86-4d45-9ebb-80a81e7d3b60	2026-10-31	72186f7d-7dd3-4e09-8551-045f309d0a7e	1	1	0	2026-10-31 07:48:00.000464+00	2026-11-01 07:12:00.003195+00
354dddfe-7654-4a49-b1fd-87ba267b0342	2026-10-21	86d11ba9-d0e8-4301-8403-9903afbd11a9	1	1	0	2026-10-21 12:25:59.999925+00	2026-10-22 06:05:00.001354+00
31819839-132f-4ef6-9d0b-f0997bb6c70f	2026-10-20	d4d6a9d7-39f3-45d3-ab66-553e4aee8b84	1	1	0	2026-10-20 09:25:59.999816+00	2026-10-21 07:55:00.003564+00
53008f1d-0f12-415d-862d-b2be1a561704	2026-11-11	d4d6a9d7-39f3-45d3-ab66-553e4aee8b84	2	2	0	2026-11-11 12:22:00.000168+00	2026-11-12 08:06:00.002038+00
a1a4f69c-2b4a-45fb-95e2-223bf0126d4f	2026-10-22	86d11ba9-d0e8-4301-8403-9903afbd11a9	2	2	0	2026-10-22 14:30:00.000179+00	2026-10-23 08:41:00.002668+00
4aa8f632-b499-40b1-b3e9-99b9d08877fa	2026-11-01	8b77ede6-e12d-4a24-9795-f3c87c19d2ba	2	2	0	2026-11-01 13:43:00.000366+00	2026-11-02 07:00:00.002676+00
f7f427c7-e729-4d81-9189-d8473fff8d55	2026-10-17	7fb36c3a-09c3-4c20-b5b6-d00d8d230cd5	1	1	0	2026-10-17 06:16:59.999584+00	2026-10-18 05:51:00.002222+00
a8dfb0e4-70fe-43fc-a02c-6a8891f9aca4	2026-10-31	d4d6a9d7-39f3-45d3-ab66-553e4aee8b84	1	1	0	2026-10-31 14:58:59.999505+00	2026-11-02 09:08:00.003131+00
d61e1e10-f562-495a-bbab-d8fa2abc5998	2026-10-30	72186f7d-7dd3-4e09-8551-045f309d0a7e	1	1	0	2026-10-30 05:21:59.999761+00	2026-10-31 05:54:00.002405+00
860a6062-1e12-4d86-af54-8bf654f98cb8	2026-11-10	0175f24c-4200-4492-84f2-030a2c0cc5ca	2	2	0	2026-11-10 12:16:00.000427+00	2026-11-11 08:45:00.002437+00
5dbbbc36-d650-4b80-975c-4fef97f7a7f7	2026-10-31	7fb36c3a-09c3-4c20-b5b6-d00d8d230cd5	2	1	1	2026-10-31 08:57:59.999569+00	2026-11-01 05:54:00.001771+00
0a3a3b32-59fd-4677-a342-c738858073f6	2026-10-30	2c4bbfbc-96d6-4e98-b778-a375c6c2ab0d	2	1	1	2026-10-30 12:36:59.999596+00	2026-10-31 08:33:00.002265+00
6cc44780-95e9-48ce-b9e3-b080eef4b101	2026-10-23	0175f24c-4200-4492-84f2-030a2c0cc5ca	1	1	0	2026-10-23 11:14:00.00012+00	2026-10-24 06:21:00.003343+00
53794610-8129-4fab-b911-cf73930f4a1d	2026-11-05	2c4bbfbc-96d6-4e98-b778-a375c6c2ab0d	1	1	0	2026-11-05 05:05:59.999795+00	2026-11-06 06:38:00.001979+00
024c86b6-4bcb-4995-856a-f3052434b047	2026-10-29	86d11ba9-d0e8-4301-8403-9903afbd11a9	2	2	0	2026-10-29 08:28:00.00003+00	2026-10-30 08:00:00.002349+00
4706f51e-61c3-4b93-b1dd-42ec78cb4143	2026-11-08	64dd446f-7e03-421f-a2f0-05db7f2bc36c	1	1	0	2026-11-08 08:21:00.00038+00	2026-11-09 06:38:00.002769+00
fcf96abb-9dbf-4a71-af35-e9c73e14b0d0	2026-11-14	64dd446f-7e03-421f-a2f0-05db7f2bc36c	3	1	0	2026-11-14 13:04:59.999921+00	2026-11-14 13:28:00.002671+00
666ef68c-75c0-4a5a-a813-318ac9b4c0e5	2026-11-13	86d11ba9-d0e8-4301-8403-9903afbd11a9	1	0	1	2026-11-13 10:13:59.999553+00	2026-11-14 06:54:00.002279+00
387c42a8-a48a-47c4-89f8-342da0184787	2026-11-10	64dd446f-7e03-421f-a2f0-05db7f2bc36c	2	2	0	2026-11-10 12:57:59.99995+00	2026-11-11 09:09:00.003257+00
a569a60e-5a9a-495b-ad89-c8fdbdebbac3	2026-10-19	0175f24c-4200-4492-84f2-030a2c0cc5ca	1	1	0	2026-10-19 05:23:59.999567+00	2026-10-20 07:11:00.003087+00
b5baaa82-f134-4033-818a-f860218e6488	2026-10-26	2c4bbfbc-96d6-4e98-b778-a375c6c2ab0d	1	1	0	2026-10-26 08:05:59.999855+00	2026-10-27 06:33:00.002856+00
a0e10189-7831-4800-ac3e-5051f28348d1	2026-10-23	64dd446f-7e03-421f-a2f0-05db7f2bc36c	1	1	0	2026-10-23 05:44:00.000407+00	2026-10-24 06:06:00.003054+00
c2c3091a-330b-4673-b236-96b8dcdef4ba	2026-11-07	2c4bbfbc-96d6-4e98-b778-a375c6c2ab0d	1	1	0	2026-11-07 11:18:00.000398+00	2026-11-08 03:57:00.002611+00
c04da291-680a-47d4-852a-7aa997597198	2026-10-24	86d11ba9-d0e8-4301-8403-9903afbd11a9	1	1	0	2026-10-24 04:01:59.999867+00	2026-10-25 08:29:00.003132+00
aafbb9e1-f011-4f51-9e8e-622b1fbadb46	2026-10-20	2c4bbfbc-96d6-4e98-b778-a375c6c2ab0d	1	1	0	2026-10-20 04:17:59.999672+00	2026-10-20 12:06:00.003143+00
a4060b69-ffe3-42c4-941b-62a2d761cb87	2026-11-04	0c248632-4e7b-4ade-893f-f0a7d8ae0cfb	1	1	0	2026-11-04 05:45:00.000132+00	2026-11-05 08:02:00.002842+00
18ccd7f6-1abb-44ce-b986-0242383eff66	2026-10-27	0c248632-4e7b-4ade-893f-f0a7d8ae0cfb	3	3	0	2026-10-27 14:02:00.000131+00	2026-10-28 06:36:00.002689+00
73b0e657-f0f1-4555-b415-3309f4d82d69	2026-10-22	0c248632-4e7b-4ade-893f-f0a7d8ae0cfb	3	3	0	2026-10-22 10:09:59.999757+00	2026-10-23 08:04:00.002405+00
3dee0dac-6f1f-4d24-a154-1eaea9086f46	2026-11-11	0c248632-4e7b-4ade-893f-f0a7d8ae0cfb	1	1	0	2026-11-11 13:04:59.999899+00	2026-11-12 07:39:00.002413+00
1d72dd19-21d6-4dd5-bc09-e839cbf2b686	2026-10-30	8b77ede6-e12d-4a24-9795-f3c87c19d2ba	1	1	0	2026-10-30 11:45:00.000295+00	2026-10-31 06:36:00.002456+00
7a48b81b-e063-4277-9d73-8d5557995c73	2026-11-09	7fb36c3a-09c3-4c20-b5b6-d00d8d230cd5	2	2	0	2026-11-09 09:40:59.999685+00	2026-11-10 06:01:00.001959+00
c05101a2-d47e-4fc2-a24c-66901318c910	2026-11-04	64dd446f-7e03-421f-a2f0-05db7f2bc36c	1	1	0	2026-11-04 06:11:00.000301+00	2026-11-05 06:09:00.002924+00
b1a522aa-47a7-4473-b538-c3b70bbda874	2026-11-12	8b77ede6-e12d-4a24-9795-f3c87c19d2ba	2	2	0	2026-11-12 11:49:00.000161+00	2026-11-13 06:52:00.002139+00
6e56e2ff-7526-418c-854b-7e6f89998188	2026-10-29	7fb36c3a-09c3-4c20-b5b6-d00d8d230cd5	1	1	0	2026-10-29 12:44:59.999962+00	2026-10-30 04:46:00.002172+00
9f92ace2-b7d8-434c-9946-39751a66aded	2026-11-13	8b77ede6-e12d-4a24-9795-f3c87c19d2ba	1	1	0	2026-11-13 13:12:59.999837+00	2026-11-14 07:47:00.002046+00
70cf1fe5-f9e9-43e6-9ba7-e238d8d35547	2026-11-02	86d11ba9-d0e8-4301-8403-9903afbd11a9	2	2	0	2026-11-02 13:42:59.99985+00	2026-11-03 04:53:00.003005+00
14f5e1bb-101a-4643-8802-53d27fcce3b1	2026-10-18	72186f7d-7dd3-4e09-8551-045f309d0a7e	2	2	0	2026-10-18 11:03:59.999644+00	2026-10-19 07:18:00.003794+00
a5bf59c7-86ff-41fb-9238-560b8725a96f	2026-10-21	0c248632-4e7b-4ade-893f-f0a7d8ae0cfb	1	0	1	2026-10-21 10:42:59.999911+00	2026-10-22 06:57:00.001448+00
2c2cf972-44bc-4fa8-ac6a-9b50821959f8	2026-10-28	0c248632-4e7b-4ade-893f-f0a7d8ae0cfb	1	1	0	2026-10-28 07:26:59.999714+00	2026-10-29 06:13:00.003579+00
883a2c2e-2d3d-43e6-b0cf-fc916d7f6979	2026-10-24	8b77ede6-e12d-4a24-9795-f3c87c19d2ba	1	1	0	2026-10-24 06:30:59.999673+00	2026-10-25 04:21:00.004066+00
4bcc1b4b-7199-4be5-bbd2-9c20db4495bb	2026-11-14	d4d6a9d7-39f3-45d3-ab66-553e4aee8b84	1	0	0	2026-11-14 12:34:59.99957+00	2026-11-14 12:17:15.79957+00
31c9e944-813e-4833-b114-8896fd781614	2026-11-03	0175f24c-4200-4492-84f2-030a2c0cc5ca	2	2	0	2026-11-03 14:22:59.99953+00	2026-11-05 08:50:00.002747+00
97ac89ec-747f-44fc-b98a-dc108ef3a008	2026-10-19	8b77ede6-e12d-4a24-9795-f3c87c19d2ba	1	1	0	2026-10-19 06:23:00.000485+00	2026-10-20 05:29:00.002964+00
c1010b1a-5223-4e52-9cf7-5d11d379ef8f	2026-10-17	d4d6a9d7-39f3-45d3-ab66-553e4aee8b84	1	1	0	2026-10-17 13:59:59.999947+00	2026-10-18 06:35:00.003472+00
2164db43-beaf-4783-8e74-f03c804f5c12	2026-11-09	64dd446f-7e03-421f-a2f0-05db7f2bc36c	1	1	0	2026-11-09 07:21:00.000207+00	2026-11-10 06:22:00.002748+00
73d65102-0c61-4a22-ae3a-8d74971eeaac	2026-10-22	d4d6a9d7-39f3-45d3-ab66-553e4aee8b84	1	1	0	2026-10-22 04:18:59.999526+00	2026-10-22 09:30:00.002696+00
278ed61a-1428-4ac4-bf7b-b14ecf0f1072	2026-11-04	2c4bbfbc-96d6-4e98-b778-a375c6c2ab0d	2	2	0	2026-11-04 06:50:00.000323+00	2026-11-05 08:40:00.002522+00
66866dbb-26cc-498b-8151-c27ccbc40d8b	2026-10-25	0175f24c-4200-4492-84f2-030a2c0cc5ca	1	1	0	2026-10-25 13:48:00.000404+00	2026-10-26 06:13:00.001996+00
26f9b704-c4ea-4950-94a4-9efd3c4e87e7	2026-10-23	86d11ba9-d0e8-4301-8403-9903afbd11a9	3	3	0	2026-10-23 13:48:00.000279+00	2026-10-24 06:55:00.003008+00
34978b05-10b0-49ae-b290-e6e97bd429a3	2026-10-26	0c248632-4e7b-4ade-893f-f0a7d8ae0cfb	1	1	0	2026-10-26 11:53:00.000281+00	2026-10-27 07:38:00.003454+00
9362968e-bef1-40e8-b727-a47157fb5ad8	2026-11-01	86d11ba9-d0e8-4301-8403-9903afbd11a9	1	1	0	2026-11-01 10:11:59.999799+00	2026-11-02 05:47:00.003053+00
4b3af46b-0ed7-4c42-a97e-14658e2ee55f	2026-11-13	0c248632-4e7b-4ade-893f-f0a7d8ae0cfb	2	2	0	2026-11-13 10:39:00.000275+00	2026-11-14 06:50:00.002955+00
d5d35682-b150-4419-b1d8-fa3bd221619b	2026-11-01	72186f7d-7dd3-4e09-8551-045f309d0a7e	2	2	0	2026-11-01 13:50:00.000295+00	2026-11-02 07:11:00.002929+00
d1ca73bb-6823-4d26-a1cb-663bc64afecd	2026-11-10	d4d6a9d7-39f3-45d3-ab66-553e4aee8b84	1	1	0	2026-11-10 08:31:59.999791+00	2026-11-11 06:29:00.00251+00
36b47caf-1396-4efc-9b24-0f1a92c7b5cb	2026-11-02	d4d6a9d7-39f3-45d3-ab66-553e4aee8b84	1	1	0	2026-11-02 04:56:59.999797+00	2026-11-03 07:06:00.002154+00
3dbce7b3-d3a2-4c5d-83c5-2f1f8f955200	2026-11-07	8b77ede6-e12d-4a24-9795-f3c87c19d2ba	1	1	0	2026-11-07 12:44:59.999899+00	2026-11-08 04:51:00.002093+00
e6b73714-5ab8-4e34-91f3-520be4456a44	2026-11-01	2c4bbfbc-96d6-4e98-b778-a375c6c2ab0d	2	2	0	2026-11-01 08:47:59.999726+00	2026-11-02 04:52:00.003345+00
b408810e-e686-4f57-8c06-095cf3e48ec7	2026-10-30	7fb36c3a-09c3-4c20-b5b6-d00d8d230cd5	1	1	0	2026-10-30 05:19:00.000302+00	2026-10-31 05:29:00.002984+00
f3087a69-d778-49b7-9ac5-f5029a27f4c2	2026-11-06	72186f7d-7dd3-4e09-8551-045f309d0a7e	2	2	0	2026-11-06 08:14:00.000486+00	2026-11-07 07:18:00.002326+00
4f94742b-9d85-4154-8e42-0351a34d92c0	2026-11-09	86d11ba9-d0e8-4301-8403-9903afbd11a9	1	1	0	2026-11-09 11:35:00.000346+00	2026-11-10 06:46:00.001733+00
0c0222c1-d4eb-4fd9-9cd6-de36cfc894fc	2026-11-08	7fb36c3a-09c3-4c20-b5b6-d00d8d230cd5	2	2	0	2026-11-08 11:51:00.000177+00	2026-11-09 06:59:00.002466+00
d6306050-3e90-4da3-ae8d-6166ecfc308b	2026-11-14	0c248632-4e7b-4ade-893f-f0a7d8ae0cfb	1	1	0	2026-11-14 07:06:59.999593+00	2026-11-14 13:49:00.002303+00
da40ae19-bf33-4c55-942a-c0d7d60ed426	2026-10-26	8b77ede6-e12d-4a24-9795-f3c87c19d2ba	1	1	0	2026-10-26 14:25:00.000254+00	2026-10-27 06:20:00.002738+00
916f840d-902d-4c6c-bcbe-68ee4972cd18	2026-10-18	0c248632-4e7b-4ade-893f-f0a7d8ae0cfb	1	1	0	2026-10-18 12:29:00.000047+00	2026-10-19 08:40:00.004126+00
e5b62ac5-f873-46a8-add5-c44246005fe0	2026-10-22	0175f24c-4200-4492-84f2-030a2c0cc5ca	1	1	0	2026-10-22 09:52:59.999895+00	2026-10-23 07:41:00.001755+00
9fb5edaf-9a4b-48c1-b02e-f8905b311171	2026-11-09	d4d6a9d7-39f3-45d3-ab66-553e4aee8b84	1	1	0	2026-11-09 07:43:59.999787+00	2026-11-10 07:05:00.002303+00
2febfcb1-4785-44a2-a5c9-6bb876998e02	2026-11-14	72186f7d-7dd3-4e09-8551-045f309d0a7e	5	0	0	2026-11-14 13:57:59.999723+00	2026-11-14 14:00:00.002348+00
b78e98eb-169b-4009-991a-dcb88ea113a0	2026-10-17	64dd446f-7e03-421f-a2f0-05db7f2bc36c	1	0	1	2026-10-17 10:47:59.99995+00	2026-10-18 06:12:00.00282+00
278b29ce-85a1-4468-b937-85e5c309a038	2026-11-12	64dd446f-7e03-421f-a2f0-05db7f2bc36c	4	3	1	2026-11-12 09:12:59.999964+00	2026-11-13 07:38:00.003447+00
0c599e78-1619-4fa9-80c1-d6c1d1c6f5e4	2026-10-25	8b77ede6-e12d-4a24-9795-f3c87c19d2ba	1	1	0	2026-10-25 11:30:00.000298+00	2026-10-26 05:06:00.002196+00
29911b82-a495-4b26-80c6-72f74485c711	2026-10-20	86d11ba9-d0e8-4301-8403-9903afbd11a9	1	1	0	2026-10-20 03:59:59.999758+00	2026-10-20 11:35:00.002707+00
003e0b48-b75c-4c86-b309-848b5898ff88	2026-11-01	0c248632-4e7b-4ade-893f-f0a7d8ae0cfb	2	2	0	2026-11-01 09:35:00.000167+00	2026-11-02 05:55:00.002616+00
0afe6707-39b5-468c-8957-b66cc9ad4bfa	2026-11-13	2c4bbfbc-96d6-4e98-b778-a375c6c2ab0d	1	1	0	2026-11-13 05:10:59.999943+00	2026-11-14 04:41:00.002721+00
a2faa411-976b-4759-8de3-e3849b3326e6	2026-11-05	8b77ede6-e12d-4a24-9795-f3c87c19d2ba	2	2	0	2026-11-05 10:00:59.99951+00	2026-11-06 06:33:00.002385+00
c1489487-8c75-4760-afff-4e6492a4a7ea	2026-10-29	2c4bbfbc-96d6-4e98-b778-a375c6c2ab0d	1	1	0	2026-10-29 08:34:00.00002+00	2026-10-30 04:52:00.001706+00
c6a7d101-4531-4527-9227-66e870ef49d6	2026-11-12	0c248632-4e7b-4ade-893f-f0a7d8ae0cfb	1	0	1	2026-11-12 10:16:00.000179+00	2026-11-13 07:01:00.001785+00
88890a4a-f349-4c00-840c-58057324d935	2026-10-21	7fb36c3a-09c3-4c20-b5b6-d00d8d230cd5	2	2	0	2026-10-21 09:43:59.999732+00	2026-10-22 06:28:00.001642+00
19ab8595-1c53-4c59-b7f6-883ae6f1dd4d	2026-11-13	64dd446f-7e03-421f-a2f0-05db7f2bc36c	2	2	0	2026-11-13 12:15:59.999631+00	2026-11-14 07:57:00.002545+00
fb54766c-9f20-4599-9e28-2ef31c79b5c6	2026-10-28	7fb36c3a-09c3-4c20-b5b6-d00d8d230cd5	1	1	0	2026-10-28 12:24:59.999619+00	2026-10-29 07:20:00.002891+00
51a4037a-d991-48fd-aa86-6b0833d3177e	2026-10-27	0175f24c-4200-4492-84f2-030a2c0cc5ca	1	0	1	2026-10-27 05:08:59.999518+00	2026-10-27 11:12:00.002116+00
\.


--
-- Data for Name: delivery_proofs; Type: TABLE DATA; Schema: public; Owner: dispatch
--

COPY public.delivery_proofs (id, assignment_id, object_key, file_name, uploaded_at, created_at, updated_at) FROM stdin;
f5dfc494-d1b3-4319-9fb6-7549f42ad716	aa640526-f565-4418-9ef5-5f5dea8b8a4b	assignments/aa640526-f565-4418-9ef5-5f5dea8b8a4b/00208fd5-a1d5-4efa-9efe-9e6a29ecbf9f.png	\N	2026-10-17 06:13:00.004256+00	2026-10-17 06:13:00.004257+00	2026-10-17 06:13:00.004257+00
7f4a025a-8053-4373-bdd7-c1bb9c336b26	43293013-d935-46f3-9805-49f6af6ab46d	assignments/43293013-d935-46f3-9805-49f6af6ab46d/ca6cd36f-2dda-4bfe-9de2-c18bfb3d1541.png	\N	2026-10-17 04:21:00.003047+00	2026-10-17 04:21:00.003048+00	2026-10-17 04:21:00.003048+00
82c19f84-23c2-484b-8035-da9e5efa2bac	d5644461-17dd-45b4-942d-b21046946cce	assignments/d5644461-17dd-45b4-942d-b21046946cce/e5595bdd-af51-46c6-8fdb-a751709149e1.png	\N	2026-10-17 09:12:00.003424+00	2026-10-17 09:12:00.003424+00	2026-10-17 09:12:00.003424+00
bfe7f975-6897-4906-aad3-0477c3c5f070	ac7b186f-fb07-422e-91bd-2504b972e900	assignments/ac7b186f-fb07-422e-91bd-2504b972e900/4a1f0804-1d9b-4c0e-9ad2-068da67f7462.png	\N	2026-10-18 04:21:00.003677+00	2026-10-18 04:21:00.003677+00	2026-10-18 04:21:00.003677+00
4b4df078-e60f-48b7-b368-3d412e2abf4d	b91d9b31-1bf3-4956-89fe-384c7f8cf7cb	assignments/b91d9b31-1bf3-4956-89fe-384c7f8cf7cb/4cbc7091-a03f-49c3-a490-33310aabf0ef.png	\N	2026-10-19 03:57:00.002665+00	2026-10-19 03:57:00.002665+00	2026-10-19 03:57:00.002665+00
1120d7e7-f756-4f62-a3bd-b2e74bc5216e	3612272d-2f3f-4871-a2c8-4ab97fab2ffd	assignments/3612272d-2f3f-4871-a2c8-4ab97fab2ffd/c97873bb-8797-40e7-9477-cadf6f05fbe8.png	\N	2026-10-19 07:18:00.003196+00	2026-10-19 07:18:00.003197+00	2026-10-19 07:18:00.003197+00
abcc2697-f4e0-4ba0-a452-b3146e8e2e37	f48494a1-d976-41ff-b431-fe2901913d59	assignments/f48494a1-d976-41ff-b431-fe2901913d59/5bf4695f-9302-4c97-9563-82aa7afcfb2a.png	\N	2026-10-19 08:40:00.003443+00	2026-10-19 08:40:00.003443+00	2026-10-19 08:40:00.003443+00
ff8b6a8a-6a63-476d-a63f-c95dcd5b2bfb	04217771-e9b1-4bd9-8737-8ae68203d1f7	assignments/04217771-e9b1-4bd9-8737-8ae68203d1f7/ac04aae2-19b9-496e-8d1c-b14ab4469117.png	\N	2026-10-19 05:57:00.002731+00	2026-10-19 05:57:00.002732+00	2026-10-19 05:57:00.002732+00
30d80b0e-2d6c-489f-8492-46d0a48aa0c2	eb1d3466-15b6-4d7b-960d-c6fbbe731d6f	assignments/eb1d3466-15b6-4d7b-960d-c6fbbe731d6f/cda5899d-23c2-4645-af16-d485c167d0a2.png	\N	2026-10-20 05:29:00.002473+00	2026-10-20 05:29:00.002474+00	2026-10-20 05:29:00.002474+00
ecddbb04-82dd-4ab7-929c-8b5c9ee1f65a	f043b700-b402-417a-93e7-8bb8a8211281	assignments/f043b700-b402-417a-93e7-8bb8a8211281/6b322636-5095-448f-aaf6-473b039f7f5c.png	\N	2026-10-21 05:13:00.003611+00	2026-10-21 05:13:00.003611+00	2026-10-21 05:13:00.003611+00
ccb01795-4f66-48e6-ad33-7e7cd671934f	590c2326-27b7-45fc-a53f-76c23c07dca9	assignments/590c2326-27b7-45fc-a53f-76c23c07dca9/5f78fa42-fcfc-4368-9037-6e9988a18fc9.png	\N	2026-10-22 06:37:00.003689+00	2026-10-22 06:37:00.00369+00	2026-10-22 06:37:00.00369+00
df8c1688-2f4e-43d9-a291-25cb3198127e	cc283b02-bddb-4710-970f-dd0c4af2623a	assignments/cc283b02-bddb-4710-970f-dd0c4af2623a/2786199d-51d5-4917-82b2-43843f987d63.png	\N	2026-10-22 06:51:00.002228+00	2026-10-22 06:51:00.002228+00	2026-10-22 06:51:00.002228+00
51786934-1163-4f03-8143-e6394d4b0e77	4e947a7f-5bdf-4a62-9c31-97bfb6432815	assignments/4e947a7f-5bdf-4a62-9c31-97bfb6432815/d8d6179b-371d-4722-90e5-e021a825469f.png	\N	2026-10-22 04:21:00.002075+00	2026-10-22 04:21:00.002075+00	2026-10-22 04:21:00.002075+00
2297da0d-1111-40dc-9d85-0d8f4564a376	932c9894-136c-4da2-8dd4-f5538e0c301b	assignments/932c9894-136c-4da2-8dd4-f5538e0c301b/eea43258-2352-4c13-9794-6c5a7967adf8.png	\N	2026-10-22 09:30:00.002267+00	2026-10-22 09:30:00.002268+00	2026-10-22 09:30:00.002268+00
93aaa15d-d6e0-4891-866c-7519fcf35be7	593c4dff-a6af-4117-b946-9f579c96402d	assignments/593c4dff-a6af-4117-b946-9f579c96402d/c08bf11a-fe70-4f46-ae5f-506a6c6cd009.png	\N	2026-10-23 06:44:00.002469+00	2026-10-23 06:44:00.002474+00	2026-10-23 06:44:00.002475+00
77fa2b34-fe2b-41d0-831f-c339c4e59212	5160891c-0242-480d-937e-d373a999b4f3	assignments/5160891c-0242-480d-937e-d373a999b4f3/79216d3c-1497-47a4-be5e-6b97c81bca66.png	\N	2026-10-24 06:06:00.002463+00	2026-10-24 06:06:00.002463+00	2026-10-24 06:06:00.002463+00
adf42edf-105b-41df-b5a5-1c019cce60be	4fab4875-494b-4ab7-8958-2b3d255a280a	assignments/4fab4875-494b-4ab7-8958-2b3d255a280a/dcbc0385-cbd4-4b33-a296-bf199ef2fb4e.png	\N	2026-10-24 04:07:00.00298+00	2026-10-24 04:07:00.00298+00	2026-10-24 04:07:00.00298+00
a5ac6556-452b-4c8f-9cd3-1fbabd05a50f	22bb0e00-395e-4775-b952-fe496f9f8cdd	assignments/22bb0e00-395e-4775-b952-fe496f9f8cdd/413ea29e-61d8-4d1f-8d98-af3d923aa5de.png	\N	2026-10-24 07:20:00.00314+00	2026-10-24 07:20:00.00314+00	2026-10-24 07:20:00.00314+00
ba6fe0cf-a746-4cce-8eec-2911a3342a4f	5bb3908f-84f5-42ca-a37f-b8aced2b2a71	assignments/5bb3908f-84f5-42ca-a37f-b8aced2b2a71/7f9e061e-6f8b-4677-a79a-e064bade180f.png	\N	2026-10-24 06:21:00.002898+00	2026-10-24 06:21:00.002899+00	2026-10-24 06:21:00.002899+00
e74f32bc-971f-4435-b0af-5b459a465338	3a395a08-b186-42b9-af9f-0c04ac2317f4	assignments/3a395a08-b186-42b9-af9f-0c04ac2317f4/3214b0c5-c27f-4ae4-8026-07e4eeca2265.png	\N	2026-10-24 07:46:00.002827+00	2026-10-24 07:46:00.002828+00	2026-10-24 07:46:00.002828+00
454aeacd-346c-4905-9884-8307940768dd	dc380889-4306-4542-83af-9e926f3a9c0d	assignments/dc380889-4306-4542-83af-9e926f3a9c0d/31b50bbd-1937-4c89-8521-6f678e1d108b.png	\N	2026-10-25 04:21:00.003429+00	2026-10-25 04:21:00.003429+00	2026-10-25 04:21:00.003429+00
cd145b19-28ab-4fff-9241-0b0a6f9916c0	8cb9c219-6bd9-434c-8034-c92a373c6e29	assignments/8cb9c219-6bd9-434c-8034-c92a373c6e29/4de89246-6b8b-4071-a697-aa8271f98d25.png	\N	2026-10-25 05:43:00.003371+00	2026-10-25 05:43:00.003371+00	2026-10-25 05:43:00.003371+00
678c8ac6-6525-4fca-8e50-27e803d3fbfe	f8f2c648-600d-4c8f-a925-60796c6faad9	assignments/f8f2c648-600d-4c8f-a925-60796c6faad9/25ad116b-cf57-454b-ac5e-03cbb66744d2.png	\N	2026-10-26 05:46:00.002801+00	2026-10-26 05:46:00.002801+00	2026-10-26 05:46:00.002802+00
9095f493-b748-404c-8f13-5d9da1d0660c	c0564f29-d9c7-4de0-9224-92deaa5e1703	assignments/c0564f29-d9c7-4de0-9224-92deaa5e1703/2bf39615-f22e-460c-b276-5003935b1021.png	\N	2026-10-26 05:54:00.002338+00	2026-10-26 05:54:00.002338+00	2026-10-26 05:54:00.002338+00
4573c971-ea6f-46e9-808d-094071bd8976	b8e0a85f-5c4b-4dbe-be92-27cd6551f27b	assignments/b8e0a85f-5c4b-4dbe-be92-27cd6551f27b/261a0084-c404-4552-ad02-7e7c1ce731e5.png	\N	2026-10-26 06:32:00.002761+00	2026-10-26 06:32:00.002761+00	2026-10-26 06:32:00.002762+00
cb19bd06-c56c-4562-a8ff-5cf8afb7df37	8a67dfe3-f811-462f-94c5-11bc6d9e2e70	assignments/8a67dfe3-f811-462f-94c5-11bc6d9e2e70/b3e24b58-a354-4c7b-b5b6-87956ae2a6e4.png	\N	2026-10-26 05:20:00.002312+00	2026-10-26 05:20:00.002313+00	2026-10-26 05:20:00.002313+00
62355410-f03b-4fa5-bf0c-c01a9b6921b2	06674bd7-6d22-4058-8c9f-5b9a1f9e10b6	assignments/06674bd7-6d22-4058-8c9f-5b9a1f9e10b6/ece60218-52a2-47f0-9595-691eb5fbc5cc.png	\N	2026-10-26 04:30:00.002655+00	2026-10-26 04:30:00.002655+00	2026-10-26 04:30:00.002655+00
cb48734f-c6cb-407e-993c-4653724c2061	abee985a-d2ed-47e5-8fe9-3d4e314cf0d0	assignments/abee985a-d2ed-47e5-8fe9-3d4e314cf0d0/5af194b8-a992-4a48-88ad-a91082f03774.png	\N	2026-10-27 07:38:00.002923+00	2026-10-27 07:38:00.002923+00	2026-10-27 07:38:00.002923+00
c96f85a3-f0eb-4c36-a47f-76339e084c58	9b068b39-40c6-4cde-b0b5-a0a57ba9d975	assignments/9b068b39-40c6-4cde-b0b5-a0a57ba9d975/11becfe4-cfc2-430b-a7e1-a8ecab3707df.png	\N	2026-10-28 06:36:00.00232+00	2026-10-28 06:36:00.00232+00	2026-10-28 06:36:00.00232+00
a86f1561-2d8a-4b6f-9ac4-04415eab771b	623387ec-8168-4990-8c6f-66fa467055d7	assignments/623387ec-8168-4990-8c6f-66fa467055d7/5ac45f2e-87e5-4125-90d8-2bf8d61fdfdd.png	\N	2026-10-28 05:34:00.003607+00	2026-10-28 05:34:00.003608+00	2026-10-28 05:34:00.003608+00
f38a63d8-294e-40e9-a097-43abe564b578	2d1467b1-1fd4-4ca2-9ea7-74827ef13d5e	assignments/2d1467b1-1fd4-4ca2-9ea7-74827ef13d5e/5c3296ae-e61b-4546-af2e-416904733d7e.png	\N	2026-10-27 22:12:00.003221+00	2026-10-27 22:12:00.003222+00	2026-10-27 22:12:00.003222+00
f2e4fbc6-3f50-4437-8335-d17cf92e7eb4	0389961a-ca07-45a3-9a7f-6fdea4af0775	assignments/0389961a-ca07-45a3-9a7f-6fdea4af0775/70caf56e-8e12-4c3f-95c6-acf4a928cea6.png	\N	2026-10-29 03:55:00.003162+00	2026-10-29 03:55:00.003162+00	2026-10-29 03:55:00.003162+00
5a6880e7-1418-46d0-aa5c-22965ed2f2a0	11bb8cfa-4977-4dec-8865-70dcad7538f3	assignments/11bb8cfa-4977-4dec-8865-70dcad7538f3/d91ee998-f1dc-437c-b5d1-80d6c608a6f7.png	\N	2026-10-29 06:13:00.002985+00	2026-10-29 06:13:00.002985+00	2026-10-29 06:13:00.002985+00
59f82062-ff7c-4d90-a8f1-cdaaaaa93a91	3fdd598f-6d28-41f7-b731-1eccfea182e2	assignments/3fdd598f-6d28-41f7-b731-1eccfea182e2/b338b3ca-4695-4ebe-a697-b50c63338285.png	\N	2026-10-29 04:56:00.003165+00	2026-10-29 04:56:00.003166+00	2026-10-29 04:56:00.003166+00
1a142e84-739f-42ea-a803-42dec4c39a8f	76c04b61-bf48-4489-95c1-dbf225def8cd	assignments/76c04b61-bf48-4489-95c1-dbf225def8cd/e31dff3b-332f-4ba6-9332-9a91a37cb212.png	\N	2026-10-30 09:05:00.002616+00	2026-10-30 09:05:00.002617+00	2026-10-30 09:05:00.002617+00
d2fbff3d-3d4e-4e03-811d-9376569eb49f	1831988e-456d-4c24-93f1-2f4199db84d6	assignments/1831988e-456d-4c24-93f1-2f4199db84d6/2319b070-4b6b-4324-9f9f-2541be75e778.png	\N	2026-10-30 06:41:00.002971+00	2026-10-30 06:41:00.002971+00	2026-10-30 06:41:00.002971+00
332b3548-af3e-4b49-b31f-1175aeb3a1ee	be3998fa-93dd-4756-9cc9-0a166cd01966	assignments/be3998fa-93dd-4756-9cc9-0a166cd01966/824fb403-18db-4a8d-b28d-d009d0fe86f8.png	\N	2026-10-30 04:17:00.002965+00	2026-10-30 04:17:00.002965+00	2026-10-30 04:17:00.002965+00
f4e370b0-4c1c-419f-815f-a213503eb53f	f34716d9-5c57-4378-9a5a-b84f821cfea2	assignments/f34716d9-5c57-4378-9a5a-b84f821cfea2/9fc553ed-2abc-4f44-b131-9aa59cadb711.png	\N	2026-10-31 05:54:00.00202+00	2026-10-31 05:54:00.00202+00	2026-10-31 05:54:00.002021+00
b4a73a67-5982-4149-ac60-53b5d2957955	04ba0473-a31b-4a0b-907d-0b4137c8ef88	assignments/04ba0473-a31b-4a0b-907d-0b4137c8ef88/b96d2777-294e-46ca-a0e6-dfd7a9153df2.png	\N	2026-10-31 05:29:00.002453+00	2026-10-31 05:29:00.002453+00	2026-10-31 05:29:00.002453+00
d39beb77-2269-47ae-bd7a-ff7bd5fb9f34	ef3570e0-fe57-4b07-8c24-01dfff79d2d9	assignments/ef3570e0-fe57-4b07-8c24-01dfff79d2d9/065752e2-3de7-4b12-9805-669e01d3938f.png	\N	2026-10-31 04:46:00.002409+00	2026-10-31 04:46:00.002409+00	2026-10-31 04:46:00.00241+00
e02cff57-b132-47a7-8d64-7d9c60da8a86	b2a67ad2-9f39-451c-a8ba-ed4b5d6bf6bb	assignments/b2a67ad2-9f39-451c-a8ba-ed4b5d6bf6bb/b748f15e-f011-45c8-a01c-f7e59b4d4d29.png	\N	2026-10-31 05:58:00.003163+00	2026-10-31 05:58:00.003163+00	2026-10-31 05:58:00.003163+00
b077c26b-0eb8-424d-90e2-400881ae5b26	a02374ac-a3f1-4628-843f-a1330659b307	assignments/a02374ac-a3f1-4628-843f-a1330659b307/457eadf5-d078-4774-85e8-acb91e39c2ed.png	\N	2026-11-01 04:29:00.002967+00	2026-11-01 04:29:00.002967+00	2026-11-01 04:29:00.002967+00
817e94fd-fc81-48a3-9e0a-009e557da351	b75d0256-7baf-4655-88cb-9e2e179a8e42	assignments/b75d0256-7baf-4655-88cb-9e2e179a8e42/3dbb0f0c-f6d0-49cb-b346-0765e42cc8c4.png	\N	2026-11-01 07:12:00.00271+00	2026-11-01 07:12:00.00271+00	2026-11-01 07:12:00.00271+00
233b17a5-8f82-4eee-962a-b7e8f352cdf2	cb557e2b-2428-444a-a6f7-741de7ff099e	assignments/cb557e2b-2428-444a-a6f7-741de7ff099e/b6bf87a7-338d-4ae8-8003-0b3bed63ea52.png	\N	2026-11-01 05:18:00.003502+00	2026-11-01 05:18:00.003502+00	2026-11-01 05:18:00.003503+00
ddebd343-1b79-481e-99b4-ac87f7bc920a	b78d2e8f-c87b-4c53-8a56-da5ab41fc0b0	assignments/b78d2e8f-c87b-4c53-8a56-da5ab41fc0b0/6a41f0ef-0d27-4912-816a-f11afb4e5011.png	\N	2026-11-01 04:30:00.003431+00	2026-11-01 04:30:00.003431+00	2026-11-01 04:30:00.003431+00
da9695c8-ceef-49e9-94a1-c959ba1e744c	25aa4432-a405-445b-a9af-ba5d99114bb1	assignments/25aa4432-a405-445b-a9af-ba5d99114bb1/3d875082-f210-4133-9124-7052e1e6ac18.png	\N	2026-11-02 03:45:00.001709+00	2026-11-02 03:45:00.00171+00	2026-11-02 03:45:00.00171+00
3e078693-e2fc-4529-aa1c-3b39a14b4064	3a4b7461-8dcb-4bfc-a75b-49c48f8e9b44	assignments/3a4b7461-8dcb-4bfc-a75b-49c48f8e9b44/01802c2b-2c1d-4d62-92d4-3f1e537fb232.png	\N	2026-11-03 08:09:00.002054+00	2026-11-03 08:09:00.002054+00	2026-11-03 08:09:00.002054+00
656a634f-505d-45e2-8b3a-5fe6eb6feb5a	dd219a33-aa6c-4f90-983d-cf37a872709a	assignments/dd219a33-aa6c-4f90-983d-cf37a872709a/1332ed13-68b0-4927-9e36-1ec54968d740.png	\N	2026-11-03 12:41:00.002342+00	2026-11-03 12:41:00.002342+00	2026-11-03 12:41:00.002342+00
c82827cd-6719-4664-a791-487e212c588f	11eca17d-9beb-4fb1-a9cc-be5abd4a8575	assignments/11eca17d-9beb-4fb1-a9cc-be5abd4a8575/3a4ab6fb-fb9e-46b5-93db-f6c69f025b73.png	\N	2026-11-04 06:11:00.002549+00	2026-11-04 06:11:00.002549+00	2026-11-04 06:11:00.002549+00
83fcbcaa-99a7-4841-9d4c-b8137402734c	37b1289b-979a-4e0f-9c1f-70d7db6ac3c2	assignments/37b1289b-979a-4e0f-9c1f-70d7db6ac3c2/9aab1bde-751b-40ab-9305-abe8f0e2e136.png	\N	2026-11-05 08:50:00.002376+00	2026-11-05 08:50:00.002377+00	2026-11-05 08:50:00.002377+00
99dca7f7-fa45-4862-8ce5-34d1466b9dc7	7ae900df-ad90-4595-bcb8-4334491bdf1e	assignments/7ae900df-ad90-4595-bcb8-4334491bdf1e/8b6aa79c-3781-4421-b0b2-24270528aa35.png	\N	2026-11-05 06:09:00.002579+00	2026-11-05 06:09:00.002579+00	2026-11-05 06:09:00.002579+00
9e55a13c-55a1-40d0-8faf-7d8647645101	4c1f233a-2c0c-4c6e-8117-4d48edde8aa6	assignments/4c1f233a-2c0c-4c6e-8117-4d48edde8aa6/dffa4ed5-f5d0-47e7-8dc1-e9674bff2045.png	\N	2026-11-05 08:40:00.00223+00	2026-11-05 08:40:00.00223+00	2026-11-05 08:40:00.00223+00
baf0e1ac-94c5-491d-94fe-8d47ced1dc27	2e6cec97-9b5c-4759-8f32-cb64606d4f78	assignments/2e6cec97-9b5c-4759-8f32-cb64606d4f78/87cb1c44-38b9-4abf-9891-3e7e09a23b82.png	\N	2026-11-05 06:38:00.001723+00	2026-11-05 06:38:00.001723+00	2026-11-05 06:38:00.001723+00
290fb3a1-5275-44bc-8250-dcfb28f32340	7a062bda-68aa-4ad3-954d-3b400e951a60	assignments/7a062bda-68aa-4ad3-954d-3b400e951a60/4476e506-0ea5-4ea2-a745-0fd2afa8c918.png	\N	2026-11-07 07:18:00.001993+00	2026-11-07 07:18:00.001993+00	2026-11-07 07:18:00.001993+00
681eec66-2667-4ff4-88d1-5c709bc83564	d5b66c9a-ac2d-4e0a-a519-b0d688761107	assignments/d5b66c9a-ac2d-4e0a-a519-b0d688761107/1cf1e98e-e785-42ec-8e9b-ea171c7ef9f7.png	\N	2026-11-07 06:56:00.002837+00	2026-11-07 06:56:00.002837+00	2026-11-07 06:56:00.002837+00
34746978-ff62-465a-83c1-4ca54513ca4e	baaa513a-ee37-4525-bd8c-212af1017b43	assignments/baaa513a-ee37-4525-bd8c-212af1017b43/ee3dab0d-7faf-4320-9df8-440757ef060c.png	\N	2026-11-07 07:06:00.002609+00	2026-11-07 07:06:00.002609+00	2026-11-07 07:06:00.002609+00
0b58594a-c6e6-493d-ac40-65af6a4c92c0	562d0df4-b1e2-4a18-b1d2-5688aaadba44	assignments/562d0df4-b1e2-4a18-b1d2-5688aaadba44/130126f7-a3c2-435e-9cd8-054f2e0aa156.png	\N	2026-11-08 05:24:00.003652+00	2026-11-08 05:24:00.003652+00	2026-11-08 05:24:00.003652+00
aff4d73f-4697-4f80-9a9e-b79e6500491f	2a7aa66a-338d-41a8-ab10-12b98bcc924b	assignments/2a7aa66a-338d-41a8-ab10-12b98bcc924b/ac0bd003-06b4-4a76-afd0-cdfca5b5d0e2.png	\N	2026-11-08 05:48:00.00219+00	2026-11-08 05:48:00.00219+00	2026-11-08 05:48:00.00219+00
c9d18776-fd2e-44e0-bd38-69dd55192b91	fe35afb2-e3cc-478d-81b2-378372a0f3a6	assignments/fe35afb2-e3cc-478d-81b2-378372a0f3a6/70bcd94d-2bb2-4db6-aa1a-13669c3c7522.png	\N	2026-11-08 06:22:00.00285+00	2026-11-08 06:22:00.00285+00	2026-11-08 06:22:00.00285+00
062383c3-fb5f-41b8-8b44-2d9874e4545e	89928a74-4e58-422b-be1e-56fdaf0afd78	assignments/89928a74-4e58-422b-be1e-56fdaf0afd78/e0dd1eac-39bb-45ca-9101-cf2d4345c966.png	\N	2026-11-08 07:31:00.002203+00	2026-11-08 07:31:00.002203+00	2026-11-08 07:31:00.002203+00
07959579-b573-4887-bb04-40f8d473b6bf	060036d3-4e7f-4f10-a9da-20c6385f0784	assignments/060036d3-4e7f-4f10-a9da-20c6385f0784/1cb578c3-fc27-415b-917a-8a29f9a69c86.png	\N	2026-11-09 08:17:00.001937+00	2026-11-09 08:17:00.001937+00	2026-11-09 08:17:00.001937+00
7632e326-c5d5-4b3c-b21e-365dd2b83be0	2da29572-a29a-4323-ad93-ecf8f5dbde04	assignments/2da29572-a29a-4323-ad93-ecf8f5dbde04/5eba1a90-c07a-472e-bf43-3463cfd2e1ef.png	\N	2026-11-09 06:59:00.002175+00	2026-11-09 06:59:00.002175+00	2026-11-09 06:59:00.002175+00
cbfc85e8-335e-446f-9651-c6d34c455b06	4b30b983-3c83-4776-9098-66051903f04a	assignments/4b30b983-3c83-4776-9098-66051903f04a/539e8728-6945-4905-8448-884f875f0de7.png	\N	2026-11-10 05:36:00.002355+00	2026-11-10 05:36:00.002355+00	2026-11-10 05:36:00.002355+00
cf937aab-f7d9-4c3c-a59e-98be3cca6ce9	2e77755a-ebe6-4466-a157-25d541414f69	assignments/2e77755a-ebe6-4466-a157-25d541414f69/810b23db-cf93-4b99-a29b-19afcb9a2379.png	\N	2026-11-11 05:12:00.002238+00	2026-11-11 05:12:00.002238+00	2026-11-11 05:12:00.002238+00
bdd7e8ea-0f92-4c6f-b989-dd909d564691	64e81fcc-378b-486a-a92c-cc13f1341b69	assignments/64e81fcc-378b-486a-a92c-cc13f1341b69/8fda5796-7ffe-4beb-8528-e6e6476c6f58.png	\N	2026-11-11 07:23:00.003004+00	2026-11-11 07:23:00.003004+00	2026-11-11 07:23:00.003004+00
8f8329cb-8e08-46f7-a312-0b06827f6a33	89073517-7529-45c0-a412-e9344b03b5c5	assignments/89073517-7529-45c0-a412-e9344b03b5c5/9a757a92-3399-448f-9b21-ce6e6beb3010.png	\N	2026-11-11 06:29:00.002243+00	2026-11-11 06:29:00.002243+00	2026-11-11 06:29:00.002243+00
afdc4b94-f58d-4e56-8a62-99d608ffdc90	0bac67dd-64c7-4900-80ae-ea1574e5ff5b	assignments/0bac67dd-64c7-4900-80ae-ea1574e5ff5b/67cf9990-7261-4270-b000-83236db07b2b.png	\N	2026-11-11 09:09:00.002839+00	2026-11-11 09:09:00.00284+00	2026-11-11 09:09:00.00284+00
072590fc-de85-4888-ad33-a285a542a055	880d87c2-a977-4124-b92a-0ce6996799f0	assignments/880d87c2-a977-4124-b92a-0ce6996799f0/8243ca95-14ab-4804-9e97-451378112d45.png	\N	2026-11-11 08:39:00.002732+00	2026-11-11 08:39:00.002732+00	2026-11-11 08:39:00.002732+00
16c94352-4fe5-434b-8c4f-b94f74c2fbf1	647a5dd6-b8b3-44e4-a2a6-c2cebb9dc9f5	assignments/647a5dd6-b8b3-44e4-a2a6-c2cebb9dc9f5/34665d9d-8874-4c96-942e-2bacb1659538.png	\N	2026-11-11 04:44:00.002257+00	2026-11-11 04:44:00.002258+00	2026-11-11 04:44:00.002258+00
1c05d5d8-ab9c-4dea-99ad-c1d5ab47bc92	134677c7-64a0-40d7-81f5-d8ad9638e297	assignments/134677c7-64a0-40d7-81f5-d8ad9638e297/6d662634-a1a3-45cd-8220-195027cbb248.png	\N	2026-11-11 07:02:00.003395+00	2026-11-11 07:02:00.003395+00	2026-11-11 07:02:00.003395+00
ab852921-bf4c-445a-89d8-8df465902c83	4e3391cb-25d4-481d-a696-056da1d703d1	assignments/4e3391cb-25d4-481d-a696-056da1d703d1/0a64f11d-1959-4a9a-8d88-63b646018264.png	\N	2026-11-12 03:48:00.002329+00	2026-11-12 03:48:00.002329+00	2026-11-12 03:48:00.002329+00
42a0b408-c389-4e0c-a420-f1507bd51f9d	66611925-1271-4a01-aa8b-1288f7eafd0a	assignments/66611925-1271-4a01-aa8b-1288f7eafd0a/ca979b88-b4af-47b1-b5ed-aded4c22990a.png	\N	2026-11-12 07:06:00.002234+00	2026-11-12 07:06:00.002234+00	2026-11-12 07:06:00.002234+00
55d37b00-ad18-4036-a81f-6a006d02b781	9a55bf5a-79e3-4007-b518-c5b3aeebeff7	assignments/9a55bf5a-79e3-4007-b518-c5b3aeebeff7/b16735c5-caf8-4e62-a863-42d2004341cc.png	\N	2026-11-13 07:35:00.002456+00	2026-11-13 07:35:00.002456+00	2026-11-13 07:35:00.002457+00
c4af0735-c8c6-456d-ae21-a0af1b43dd77	b10a0842-a82b-480a-812a-d7e4b922ae6f	assignments/b10a0842-a82b-480a-812a-d7e4b922ae6f/f07ac0b2-993a-4d02-8716-f337cc01f9fb.png	\N	2026-11-14 06:21:00.001961+00	2026-11-14 06:21:00.001961+00	2026-11-14 06:21:00.001961+00
8d75f4ba-d536-431a-99d7-d8b6f202c6cd	6dc9e55e-f93b-41a8-9882-4d391580b136	assignments/6dc9e55e-f93b-41a8-9882-4d391580b136/0309dca5-230f-4c86-a7e1-1e52ebd5c943.png	\N	2026-11-14 04:41:00.00232+00	2026-11-14 04:41:00.00232+00	2026-11-14 04:41:00.00232+00
b1e7a0bd-21c3-42df-b5c6-ab27cc0af3d7	b8c3917a-c138-4866-8f5e-0c39b229badc	assignments/b8c3917a-c138-4866-8f5e-0c39b229badc/32b8405b-8b79-4608-a67f-774537c47435.png	\N	2026-11-14 07:57:00.002222+00	2026-11-14 07:57:00.002223+00	2026-11-14 07:57:00.002223+00
3a5b7fb0-7871-470b-a05b-2676f6f79dbe	1db694c0-c1d1-40c4-9386-d71dae7903f0	assignments/1db694c0-c1d1-40c4-9386-d71dae7903f0/c40881d1-4e09-4cb6-8e12-cbc978c46ad5.png	\N	2026-11-14 06:51:00.002088+00	2026-11-14 06:51:00.002088+00	2026-11-14 06:51:00.002088+00
88272db7-c60a-4849-be1e-2a4c7082194e	ac5eeb37-dc8f-4c5a-9757-45baa874932b	assignments/ac5eeb37-dc8f-4c5a-9757-45baa874932b/3043906a-85f5-4443-9aae-f3304e31c38c.png	\N	2026-11-14 09:39:00.002487+00	2026-11-14 09:39:00.002487+00	2026-11-14 09:39:00.002487+00
47e9d9c4-f21f-40cf-9bc8-fb0acbf3bf86	b66a7d28-bd11-44ac-9ebf-89dabe94de55	assignments/b66a7d28-bd11-44ac-9ebf-89dabe94de55/1381564f-e892-4be3-a9f7-ea682554a190.png	\N	2026-10-18 07:13:00.002014+00	2026-10-18 07:13:00.002014+00	2026-10-18 07:13:00.002014+00
afb7fc7e-d6c1-4979-9838-9fd383c14268	e074282e-6fc8-4c32-a9f4-0374ff701614	assignments/e074282e-6fc8-4c32-a9f4-0374ff701614/8b7b5dee-5ee6-48ed-96be-95c4708a5fae.png	\N	2026-10-18 06:35:00.00301+00	2026-10-18 06:35:00.00301+00	2026-10-18 06:35:00.00301+00
d1fe0ad6-a573-4987-a822-f0c349c5070e	a8d9be82-5c1f-428c-a642-34dc5260187f	assignments/a8d9be82-5c1f-428c-a642-34dc5260187f/34da5802-0968-467d-ae7e-68f372dbf355.png	\N	2026-10-21 05:30:00.002308+00	2026-10-21 05:30:00.002309+00	2026-10-21 05:30:00.002309+00
c6d3a69a-2d00-4927-bfa4-a9808f3022ae	eb2309cb-05d4-414a-9a43-829ec817d9f6	assignments/eb2309cb-05d4-414a-9a43-829ec817d9f6/66811e36-7a1e-40ad-980e-0961561bde56.png	\N	2026-10-21 08:08:00.001934+00	2026-10-21 08:08:00.001934+00	2026-10-21 08:08:00.001934+00
5897c24f-e0bc-4722-9ba1-5d530e8a6114	e47781d9-bc62-4baf-95db-a0b8acfc6396	assignments/e47781d9-bc62-4baf-95db-a0b8acfc6396/a46ca503-49f3-432c-b4ea-be902ac7d6b3.png	\N	2026-10-22 06:28:00.001421+00	2026-10-22 06:28:00.001421+00	2026-10-22 06:28:00.001421+00
2e5ae852-f423-4e75-9d75-086b25515300	2b453a84-c2d2-42a2-8345-3cd0ddf6c85e	assignments/2b453a84-c2d2-42a2-8345-3cd0ddf6c85e/e7551c27-0c2e-436c-bb26-c7c42342a614.png	\N	2026-10-23 07:41:00.00146+00	2026-10-23 07:41:00.00146+00	2026-10-23 07:41:00.00146+00
7cd4658e-ffe8-4a31-aacd-a5855b3462c1	c3fe0e84-8161-4abd-b361-c7062d575aa9	assignments/c3fe0e84-8161-4abd-b361-c7062d575aa9/98515591-e10c-472c-a2fe-b2429561d738.png	\N	2026-10-23 08:04:00.002146+00	2026-10-23 08:04:00.002146+00	2026-10-23 08:04:00.002146+00
802ac91f-ea9f-4bec-a46f-e826b1242a91	c63fcd2f-30c2-46a5-80db-a1a43bf2cc1d	assignments/c63fcd2f-30c2-46a5-80db-a1a43bf2cc1d/b1b7a0b3-5b88-482a-800c-d6f978db9e9a.png	\N	2026-10-23 06:58:00.002148+00	2026-10-23 06:58:00.002148+00	2026-10-23 06:58:00.002148+00
11325d23-678c-414f-a64d-779b6a3f4f94	327a7970-7c62-4322-9821-692bce7aef5f	assignments/327a7970-7c62-4322-9821-692bce7aef5f/27092344-7be4-4e76-a8e5-3600a9c56233.png	\N	2026-10-24 07:19:00.003038+00	2026-10-24 07:19:00.003038+00	2026-10-24 07:19:00.003038+00
8284f020-2cc5-41cb-8ef8-67e1bd315be2	272d615f-e149-4298-b696-12300a53e831	assignments/272d615f-e149-4298-b696-12300a53e831/bce5acac-2844-4ae0-9aa3-ca3569778b37.png	\N	2026-10-24 05:13:00.001668+00	2026-10-24 05:13:00.001668+00	2026-10-24 05:13:00.001668+00
20f1f050-4206-41cb-bf00-ef9e98df619b	d6611e30-908d-431e-bcb2-24c2f6eb2c82	assignments/d6611e30-908d-431e-bcb2-24c2f6eb2c82/a3d72e09-6e9f-48b7-a432-e218cbba94d9.png	\N	2026-10-24 11:46:00.002948+00	2026-10-24 11:46:00.002948+00	2026-10-24 11:46:00.002948+00
66507a07-c524-4a28-9961-291f0b9eb10a	9e2565c3-87fc-4885-aca7-5b0f2319fd67	assignments/9e2565c3-87fc-4885-aca7-5b0f2319fd67/c5cfe0e6-b486-4dae-96a9-0ae2f4e23325.png	\N	2026-10-25 05:54:00.001848+00	2026-10-25 05:54:00.001848+00	2026-10-25 05:54:00.001848+00
eec154fc-8b3c-4d6e-991d-3d5b491ccba6	6de83d1a-4d54-426b-9197-354b3c08cc79	assignments/6de83d1a-4d54-426b-9197-354b3c08cc79/e2c43ed6-4a49-4415-a7b3-2951bd7ca967.png	\N	2026-10-25 10:03:00.002395+00	2026-10-25 10:03:00.002395+00	2026-10-25 10:03:00.002395+00
319c6613-e75b-4ba1-8af9-35ea3c6b2d26	868775b1-8df6-475f-b3eb-65c852c850d3	assignments/868775b1-8df6-475f-b3eb-65c852c850d3/3a148e22-0b96-4322-89af-42e114df331f.png	\N	2026-10-26 07:23:00.001905+00	2026-10-26 07:23:00.001905+00	2026-10-26 07:23:00.001905+00
432efb79-fbf2-4c6d-aa1b-fecad8ce612d	7ecfd356-577e-43c4-bc53-2d4973d20c46	assignments/7ecfd356-577e-43c4-bc53-2d4973d20c46/9d4a3990-e9b0-4c1f-ba00-c76a7feeeba8.png	\N	2026-10-26 05:49:00.00252+00	2026-10-26 05:49:00.002521+00	2026-10-26 05:49:00.002521+00
390dd2c7-1356-440f-b383-f0c31e765041	6ce843d3-8d65-4211-944a-4151403bc422	assignments/6ce843d3-8d65-4211-944a-4151403bc422/db5b363d-c55d-4483-a377-00da6136000d.png	\N	2026-10-26 06:45:00.00145+00	2026-10-26 06:45:00.00145+00	2026-10-26 06:45:00.00145+00
18dad4bf-eda3-4632-a443-5cd07c18de30	5dc3dfa5-85f0-43ad-b350-b05a050c251d	assignments/5dc3dfa5-85f0-43ad-b350-b05a050c251d/1ba1c505-c66f-45d4-bb2b-32396914de41.png	\N	2026-10-27 04:32:00.002163+00	2026-10-27 04:32:00.002163+00	2026-10-27 04:32:00.002163+00
e823bb8a-5486-46f5-a22d-f9f167552ed2	9bcef941-1afb-43a8-a21a-5e3a49b37511	assignments/9bcef941-1afb-43a8-a21a-5e3a49b37511/a80f0e23-9811-498c-8e7e-feb2ee663790.png	\N	2026-10-28 05:56:00.001873+00	2026-10-28 05:56:00.001873+00	2026-10-28 05:56:00.001873+00
380092f7-51a0-48ba-8ac2-3006c3a36621	76add8e9-bb4a-4acd-9a35-e34a2e68126c	assignments/76add8e9-bb4a-4acd-9a35-e34a2e68126c/4ec9b8fb-3eb6-4f3e-bdcf-80a0e0f54f73.png	\N	2026-10-29 06:46:00.002756+00	2026-10-29 06:46:00.002756+00	2026-10-29 06:46:00.002756+00
b0d112fb-8fce-463d-9721-cd9c57bb71ff	5e716482-dac9-4edf-9bf4-9ae3435804e3	assignments/5e716482-dac9-4edf-9bf4-9ae3435804e3/80ccc515-5af5-4795-8b34-299462ffceef.png	\N	2026-10-30 08:00:00.002125+00	2026-10-30 08:00:00.002125+00	2026-10-30 08:00:00.002125+00
98737d42-9d31-42a2-a0c7-d829cf601e51	3b1decc4-8901-449b-85ca-bd6e5ba2e51e	assignments/3b1decc4-8901-449b-85ca-bd6e5ba2e51e/f81f94ff-d863-46d4-8a44-7df4eed40574.png	\N	2026-10-30 04:46:00.001781+00	2026-10-30 04:46:00.001781+00	2026-10-30 04:46:00.001781+00
5cd68682-6bad-4e6e-b89e-a09b8721cad7	de28093d-2eb7-4768-bd5c-6b51d708cdda	assignments/de28093d-2eb7-4768-bd5c-6b51d708cdda/3f138849-e7d5-4896-a357-d6bf1045471f.png	\N	2026-10-31 06:36:00.002227+00	2026-10-31 06:36:00.002227+00	2026-10-31 06:36:00.002227+00
091c52e8-2def-4dd6-8caa-f2a0e59e3ff8	40a257c9-aef1-42a8-b3bd-21983b5c83a9	assignments/40a257c9-aef1-42a8-b3bd-21983b5c83a9/c29a1ad2-faa4-429f-9d9b-dd1539beae36.png	\N	2026-11-01 05:54:00.001462+00	2026-11-01 05:54:00.001462+00	2026-11-01 05:54:00.001462+00
b1f16dd6-4cc3-43f7-9f33-a72cf1f01771	5d653d09-17ca-4ff8-a20a-5926155e7969	assignments/5d653d09-17ca-4ff8-a20a-5926155e7969/43f0a8cc-5de3-430e-9cf2-5623555f6943.png	\N	2026-11-02 07:11:00.002562+00	2026-11-02 07:11:00.002562+00	2026-11-02 07:11:00.002562+00
edc3bd77-704b-4a6d-9921-611bbe44724e	fb2c138b-795a-4da6-80a4-5a32c8b6b093	assignments/fb2c138b-795a-4da6-80a4-5a32c8b6b093/88e777cb-b4b0-499d-a2f7-4b878ab765fc.png	\N	2026-11-02 06:50:00.002289+00	2026-11-02 06:50:00.002289+00	2026-11-02 06:50:00.002289+00
cf588607-ceeb-4229-aef0-80c6019fe6f9	1b7b380c-3e40-4c91-aa4c-45e3d728049c	assignments/1b7b380c-3e40-4c91-aa4c-45e3d728049c/b483fded-81ae-49f7-b816-38a507806961.png	\N	2026-11-02 06:23:00.001324+00	2026-11-02 06:23:00.001324+00	2026-11-02 06:23:00.001324+00
48c280c4-583e-4bca-bf75-cae0b8c9a8a3	f65fc1bf-e0a0-44cc-88ec-0b2ea7fde6e6	assignments/f65fc1bf-e0a0-44cc-88ec-0b2ea7fde6e6/ac009e2d-9693-43f7-b62a-6afae3bb2138.png	\N	2026-11-06 05:03:00.002028+00	2026-11-06 05:03:00.002028+00	2026-11-06 05:03:00.002028+00
1d75b335-3c3b-4b4f-98ed-88e1812a656b	e06622b7-5e6a-4b8f-be36-ba758deff387	assignments/e06622b7-5e6a-4b8f-be36-ba758deff387/219e82f5-d7e6-42ee-a007-fca44ee5664c.png	\N	2026-11-08 08:20:00.00172+00	2026-11-08 08:20:00.00172+00	2026-11-08 08:20:00.00172+00
709e51aa-080e-44eb-ac58-10479958f085	4a72d4b5-85fd-404f-83b7-13f1fd6b4bc5	assignments/4a72d4b5-85fd-404f-83b7-13f1fd6b4bc5/eda98e09-1d90-4936-999b-29f90888ad89.png	\N	2026-11-08 03:57:00.002368+00	2026-11-08 03:57:00.002368+00	2026-11-08 03:57:00.002368+00
c5b98b55-27c0-46f8-b67a-227466bc5b74	4aa2d15b-8422-42d0-a74e-f10164006c1a	assignments/4aa2d15b-8422-42d0-a74e-f10164006c1a/64d9555e-05ea-46c4-8895-214d1e005e8d.png	\N	2026-11-08 04:51:00.001843+00	2026-11-08 04:51:00.001843+00	2026-11-08 04:51:00.001843+00
f90dd373-033e-40b7-ac24-7dadf448bb8e	cf3f9396-1cdb-465b-a84f-633b29018547	assignments/cf3f9396-1cdb-465b-a84f-633b29018547/07c39bb5-e6c3-4b80-819e-41e291198046.png	\N	2026-11-09 06:38:00.002503+00	2026-11-09 06:38:00.002504+00	2026-11-09 06:38:00.002504+00
e57950c3-5258-4b47-b2bf-e608ccbbe17c	335acee3-fa77-4d09-9d8c-1ffd59d250b9	assignments/335acee3-fa77-4d09-9d8c-1ffd59d250b9/0bbfd002-8d9d-4696-ad19-f1c790091ba7.png	\N	2026-11-10 06:51:00.001738+00	2026-11-10 06:51:00.001738+00	2026-11-10 06:51:00.001738+00
c7dc6f7f-218c-4763-afe6-c4d018a30d8b	73cf6143-6746-482a-9d12-ef3ce40921da	assignments/73cf6143-6746-482a-9d12-ef3ce40921da/bca60e5e-1878-406b-8996-08c950643909.png	\N	2026-11-09 11:38:00.001937+00	2026-11-09 11:38:00.001937+00	2026-11-09 11:38:00.001937+00
ac052fca-cd08-46fd-a208-1169f4e2e710	8af32062-2d07-4744-920f-fb2fa638ef85	assignments/8af32062-2d07-4744-920f-fb2fa638ef85/cb597b74-eada-45ba-ab14-6d96dda79666.png	\N	2026-11-10 07:20:00.002115+00	2026-11-10 07:20:00.002115+00	2026-11-10 07:20:00.002115+00
176b43b2-8fa0-4066-9707-3c23f3855177	c01f9bca-34f5-4ae0-a60b-13181a54a51f	assignments/c01f9bca-34f5-4ae0-a60b-13181a54a51f/47ddc14f-4c9e-4d35-a336-13562afb3396.png	\N	2026-11-10 07:05:00.002064+00	2026-11-10 07:05:00.002064+00	2026-11-10 07:05:00.002064+00
cc7d6075-e037-41b3-9d07-305d25e706fc	7dd6cf24-b2d2-4541-b813-d00b14c9256f	assignments/7dd6cf24-b2d2-4541-b813-d00b14c9256f/f66d693c-229a-47a9-8553-3d26bee5ca6a.png	\N	2026-11-10 06:46:00.001474+00	2026-11-10 06:46:00.001474+00	2026-11-10 06:46:00.001474+00
320032f5-19ee-4020-b71e-5e4eca5334d6	543c9533-3716-4548-aa8e-2657a9cdb61c	assignments/543c9533-3716-4548-aa8e-2657a9cdb61c/166a7234-457c-40aa-bad9-7c4012477919.png	\N	2026-11-10 13:06:00.001471+00	2026-11-10 13:06:00.001471+00	2026-11-10 13:06:00.001471+00
3513bb3c-735b-44d4-9031-fe13b31c22ab	1f33af0a-9274-4937-931f-3a7730285b5c	assignments/1f33af0a-9274-4937-931f-3a7730285b5c/ff1f9ce7-2d37-4eb1-810a-be8451831137.png	\N	2026-11-11 08:45:00.002181+00	2026-11-11 08:45:00.002181+00	2026-11-11 08:45:00.002181+00
bbebeb60-ca63-4287-83da-75f08bae7435	28d36a64-e7c9-4120-abe6-dc9054014f9f	assignments/28d36a64-e7c9-4120-abe6-dc9054014f9f/f5234c37-2cdb-4ae2-ad6d-e69cbe1d893e.png	\N	2026-11-12 07:39:00.002079+00	2026-11-12 07:39:00.002079+00	2026-11-12 07:39:00.002079+00
600eaf50-1618-4072-8d30-b2b4df9e1af8	7fc15bbf-99b4-4f5a-afdb-964d665978ae	assignments/7fc15bbf-99b4-4f5a-afdb-964d665978ae/48426526-3838-4c28-95cb-10845a08311d.png	\N	2026-11-13 07:38:00.003137+00	2026-11-13 07:38:00.003137+00	2026-11-13 07:38:00.003137+00
05db5fbc-1230-465d-9ab9-988cea60426f	8dcf07b7-8b2b-4abb-82f1-8200514d94b3	assignments/8dcf07b7-8b2b-4abb-82f1-8200514d94b3/74d7d412-9e91-4ee7-b3a0-c274d7e08693.png	\N	2026-11-14 05:05:00.001932+00	2026-11-14 05:05:00.001932+00	2026-11-14 05:05:00.001932+00
3a939428-25f3-49db-832b-a4e7d7a778e8	14093a80-5023-426f-8fcc-edf36e59042d	assignments/14093a80-5023-426f-8fcc-edf36e59042d/4779672c-a9c7-41cc-90df-88d9e6c6e344.png	\N	2026-11-14 13:49:00.002107+00	2026-11-14 13:49:00.002108+00	2026-11-14 13:49:00.002108+00
\.


--
-- Data for Name: flyway_schema_history; Type: TABLE DATA; Schema: public; Owner: dispatch
--

COPY public.flyway_schema_history (installed_rank, version, description, type, script, checksum, installed_by, installed_on, execution_time, success) FROM stdin;
1	1	create dispatch tables	SQL	V1__create_dispatch_tables.sql	622169515	dispatch	2026-11-14 14:11:39.027453	113	t
2	2	add parcel details to assignments	SQL	V2__add_parcel_details_to_assignments.sql	-174379231	dispatch	2026-11-14 14:11:39.208583	8	t
3	3	add parcel status to assignments	SQL	V3__add_parcel_status_to_assignments.sql	1408546475	dispatch	2026-11-14 14:11:39.237027	3	t
4	4	seed demo courier	SQL	V4__seed_demo_courier.sql	-357519094	dispatch	2026-11-14 14:11:39.25171	4	t
5	5	remove orphan demo courier	SQL	V5__remove_orphan_demo_courier.sql	-1224014215	dispatch	2026-11-14 14:11:39.266942	2	t
\.


--
-- Data for Name: processed_events; Type: TABLE DATA; Schema: public; Owner: dispatch
--

COPY public.processed_events (id, event_id, source, processed_at, created_at) FROM stdin;
\.


--
-- Name: assignments assignments_pkey; Type: CONSTRAINT; Schema: public; Owner: dispatch
--

ALTER TABLE ONLY public.assignments
    ADD CONSTRAINT assignments_pkey PRIMARY KEY (id);


--
-- Name: couriers couriers_pkey; Type: CONSTRAINT; Schema: public; Owner: dispatch
--

ALTER TABLE ONLY public.couriers
    ADD CONSTRAINT couriers_pkey PRIMARY KEY (id);


--
-- Name: couriers couriers_user_id_unique; Type: CONSTRAINT; Schema: public; Owner: dispatch
--

ALTER TABLE ONLY public.couriers
    ADD CONSTRAINT couriers_user_id_unique UNIQUE (user_id);


--
-- Name: daily_stats daily_stats_pkey; Type: CONSTRAINT; Schema: public; Owner: dispatch
--

ALTER TABLE ONLY public.daily_stats
    ADD CONSTRAINT daily_stats_pkey PRIMARY KEY (id);


--
-- Name: daily_stats daily_stats_unique; Type: CONSTRAINT; Schema: public; Owner: dispatch
--

ALTER TABLE ONLY public.daily_stats
    ADD CONSTRAINT daily_stats_unique UNIQUE (date, courier_id);


--
-- Name: delivery_proofs delivery_proofs_pkey; Type: CONSTRAINT; Schema: public; Owner: dispatch
--

ALTER TABLE ONLY public.delivery_proofs
    ADD CONSTRAINT delivery_proofs_pkey PRIMARY KEY (id);


--
-- Name: flyway_schema_history flyway_schema_history_pk; Type: CONSTRAINT; Schema: public; Owner: dispatch
--

ALTER TABLE ONLY public.flyway_schema_history
    ADD CONSTRAINT flyway_schema_history_pk PRIMARY KEY (installed_rank);


--
-- Name: processed_events processed_events_event_id_unique; Type: CONSTRAINT; Schema: public; Owner: dispatch
--

ALTER TABLE ONLY public.processed_events
    ADD CONSTRAINT processed_events_event_id_unique UNIQUE (event_id);


--
-- Name: processed_events processed_events_pkey; Type: CONSTRAINT; Schema: public; Owner: dispatch
--

ALTER TABLE ONLY public.processed_events
    ADD CONSTRAINT processed_events_pkey PRIMARY KEY (id);


--
-- Name: flyway_schema_history_s_idx; Type: INDEX; Schema: public; Owner: dispatch
--

CREATE INDEX flyway_schema_history_s_idx ON public.flyway_schema_history USING btree (success);


--
-- Name: idx_assignments_courier_id; Type: INDEX; Schema: public; Owner: dispatch
--

CREATE INDEX idx_assignments_courier_id ON public.assignments USING btree (courier_id);


--
-- Name: idx_assignments_parcel_id; Type: INDEX; Schema: public; Owner: dispatch
--

CREATE INDEX idx_assignments_parcel_id ON public.assignments USING btree (parcel_id);


--
-- Name: idx_assignments_status; Type: INDEX; Schema: public; Owner: dispatch
--

CREATE INDEX idx_assignments_status ON public.assignments USING btree (status);


--
-- Name: idx_couriers_status; Type: INDEX; Schema: public; Owner: dispatch
--

CREATE INDEX idx_couriers_status ON public.couriers USING btree (status);


--
-- Name: idx_couriers_user_id; Type: INDEX; Schema: public; Owner: dispatch
--

CREATE INDEX idx_couriers_user_id ON public.couriers USING btree (user_id);


--
-- Name: idx_daily_stats_courier_id; Type: INDEX; Schema: public; Owner: dispatch
--

CREATE INDEX idx_daily_stats_courier_id ON public.daily_stats USING btree (courier_id);


--
-- Name: idx_daily_stats_date; Type: INDEX; Schema: public; Owner: dispatch
--

CREATE INDEX idx_daily_stats_date ON public.daily_stats USING btree (date);


--
-- Name: idx_delivery_proofs_assignment_id; Type: INDEX; Schema: public; Owner: dispatch
--

CREATE INDEX idx_delivery_proofs_assignment_id ON public.delivery_proofs USING btree (assignment_id);


--
-- Name: idx_processed_events_event_id; Type: INDEX; Schema: public; Owner: dispatch
--

CREATE INDEX idx_processed_events_event_id ON public.processed_events USING btree (event_id);


--
-- Name: ux_assignments_active_parcel; Type: INDEX; Schema: public; Owner: dispatch
--

CREATE UNIQUE INDEX ux_assignments_active_parcel ON public.assignments USING btree (parcel_id) WHERE ((status)::text = ANY ((ARRAY['ASSIGNED'::character varying, 'IN_PROGRESS'::character varying])::text[]));


--
-- Name: assignments fk_assignments_courier; Type: FK CONSTRAINT; Schema: public; Owner: dispatch
--

ALTER TABLE ONLY public.assignments
    ADD CONSTRAINT fk_assignments_courier FOREIGN KEY (courier_id) REFERENCES public.couriers(id) ON DELETE RESTRICT;


--
-- Name: daily_stats fk_daily_stats_courier; Type: FK CONSTRAINT; Schema: public; Owner: dispatch
--

ALTER TABLE ONLY public.daily_stats
    ADD CONSTRAINT fk_daily_stats_courier FOREIGN KEY (courier_id) REFERENCES public.couriers(id) ON DELETE CASCADE;


--
-- Name: delivery_proofs fk_delivery_proofs_assignment; Type: FK CONSTRAINT; Schema: public; Owner: dispatch
--

ALTER TABLE ONLY public.delivery_proofs
    ADD CONSTRAINT fk_delivery_proofs_assignment FOREIGN KEY (assignment_id) REFERENCES public.assignments(id) ON DELETE CASCADE;


--
-- PostgreSQL database dump complete
--

\unrestrict ijGMX3UeHr5Kj97nvYfWUD2DKE7qeThJH9mDKaFEnEH2iZKiz0LmXYFFm4dKQru

