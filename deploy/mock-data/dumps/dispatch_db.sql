--
-- PostgreSQL database dump
--

\restrict OUGpiIndkXLR0FYUYhSGweU348X6T4t1BIsKodjsLgsHv9nZV4z9ER1NVdDilxZ

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
d6eb5c63-502a-4544-bbb8-37c7c75c2d17	77cd5de2-1f8d-4a36-8ada-5a8b2768d94f	ff655d35-a0cb-4ec7-a11c-25e7cb868ddf	80d23eb8-1156-49dc-b5c3-42cdedfab75d	FAILED	2026-10-16 05:03:59.999739+00	2026-10-16 05:03:59.999741+00	2026-10-17 06:19:00.003819+00	SD-AG45-YZ8K	21 Hill Street, Dehiwala	47 Prince of Wales Avenue, Colombo 14	Thisaru Abeysekera	MEDIUM	5.26	FAILED
46f99904-39d7-4ab1-9391-2657d28ca0b5	93f9477a-9014-4754-a193-4e99f3c6fdfe	ff655d35-a0cb-4ec7-a11c-25e7cb868ddf	7d19bfe5-daf8-4879-8f88-7fcc9bff34c5	COMPLETED	2026-10-16 05:39:59.999657+00	2026-10-16 05:39:59.999657+00	2026-10-17 05:36:00.004805+00	SD-6BEK-39QX	142 High Level Road, Maharagama	150 William Gopallawa Mawatha, Kandy	Anjali Mendis	SMALL	3.64	DELIVERED
a375580b-aeb1-4383-a551-e39917c1760e	cf10fc95-559f-4604-b769-cf5c31cdf965	5ba3a5c8-4911-45c9-9155-84e05b5dae36	7d19bfe5-daf8-4879-8f88-7fcc9bff34c5	COMPLETED	2026-10-16 06:19:00.00045+00	2026-10-16 06:19:00.000451+00	2026-10-17 07:22:00.004531+00	SD-KP4C-CLYN	269 Kotte Road, Kotte	329 Wakwella Road, Galle	Arun Gunasekara	SMALL	2.53	DELIVERED
94762905-62f2-421e-93a6-d6417aef949f	d098173f-8e4c-435f-9239-d08137460ab9	0692e9c1-ff92-4d13-ae68-a7c2168947c3	80d23eb8-1156-49dc-b5c3-42cdedfab75d	COMPLETED	2026-10-16 06:23:59.999777+00	2026-10-16 06:23:59.999778+00	2026-10-17 04:21:00.004065+00	SD-2LH4-3KJJ	238 Chatham Street, Colombo 01	340 Marine Drive, Colombo 06	Methmi Senanayake	LARGE	8.94	DELIVERED
c5ae4485-6544-43e9-bfb3-916deb8e96e3	1257c29a-a284-4c3a-b418-b8fbd7c0fd9f	2a4e4e19-00b3-4d93-b3f5-185ca3701d3d	7d19bfe5-daf8-4879-8f88-7fcc9bff34c5	COMPLETED	2026-10-17 04:33:59.99969+00	2026-10-17 04:33:59.99969+00	2026-10-18 03:51:00.003461+00	SD-3G9B-U22X	202 Lauries Road, Colombo 04	73 Galle Road, Colombo 04	Kavindi Silva	MEDIUM	7.57	DELIVERED
4ca7adf3-6e74-4257-96fd-31b8f014b166	bd766846-3990-4e28-b7df-ac9e540905fb	16d13c48-6ea3-48b5-b0b5-fbe7fcefec4d	7d19bfe5-daf8-4879-8f88-7fcc9bff34c5	FAILED	2026-10-17 06:28:59.999576+00	2026-10-17 06:28:59.999576+00	2026-10-18 06:12:00.002802+00	SD-SSN5-89MZ	231 Old Kesbewa Road, Nugegoda	216 Church Street, Galle Fort	Ruwan Rahman	MEDIUM	6.73	FAILED
6391389b-c04c-4a2c-b964-4c1219415ad0	09c22a9e-4fa6-4749-85b7-d010e247568c	2a4e4e19-00b3-4d93-b3f5-185ca3701d3d	7d19bfe5-daf8-4879-8f88-7fcc9bff34c5	COMPLETED	2026-10-17 09:15:59.999534+00	2026-10-17 09:15:59.999534+00	2026-10-18 03:57:00.003658+00	SD-DX6W-7SUJ	143 Galle Road, Dehiwala	260 Hotel Road, Mount Lavinia	Sanduni Karunaratne	MEDIUM	9.76	DELIVERED
046c2174-f759-4eb2-935a-80e038e05a87	264a7b99-8035-4175-8fe0-7c295e69fff9	2a4e4e19-00b3-4d93-b3f5-185ca3701d3d	7d19bfe5-daf8-4879-8f88-7fcc9bff34c5	COMPLETED	2026-10-18 04:42:59.99974+00	2026-10-18 04:42:59.999741+00	2026-10-19 07:18:00.004147+00	SD-EAZC-9SDE	81 Nawala Road, Rajagiriya	264 Lewis Place, Negombo	Lakshmi Mendis	SMALL	1.29	DELIVERED
caf929ba-392b-4359-bbe5-92e26c1c1063	b45fffea-4e34-47fe-a284-6758c66d61e5	03706d2d-c424-4359-8d98-702189d2a120	49c139c8-8007-4199-80b6-e68a928a59fb	COMPLETED	2026-10-18 05:06:59.999769+00	2026-10-18 05:06:59.999769+00	2026-10-19 05:57:00.003322+00	SD-WHY9-S223	57 Union Place, Colombo 02	325 Galle Road, Dehiwala	Oshada Karunaratne	EXTRA_LARGE	33.54	DELIVERED
633e779f-bccd-4dd1-a976-151d935a75e2	a10523c8-ff0e-4b16-b7aa-aea3dbfe9376	ff655d35-a0cb-4ec7-a11c-25e7cb868ddf	7d19bfe5-daf8-4879-8f88-7fcc9bff34c5	COMPLETED	2026-10-18 10:02:59.999643+00	2026-10-18 10:02:59.999643+00	2026-10-19 08:40:00.004073+00	SD-PPFR-9ZBN	270 Station Road, Nugegoda	324 William Gopallawa Mawatha, Kandy	Kamal Ratnayake	SMALL	1.73	DELIVERED
06a8c0fa-3de5-4786-a382-b3b38abb6540	4926d6b8-8464-4ef0-b64d-d2b90f58dad4	2afe8e87-d5cd-41ea-99b4-db04a7f48d25	7d19bfe5-daf8-4879-8f88-7fcc9bff34c5	COMPLETED	2026-10-18 12:04:00.000381+00	2026-10-18 12:04:00.000381+00	2026-10-19 05:57:00.003987+00	SD-8BXG-US34	12 Station Road, Nugegoda	176 Havelock Road, Colombo 05	Hiruni Dissanayake	SMALL	0.39	DELIVERED
c12c7cd3-9504-422c-98bf-07a205f66928	32c3be58-2758-432f-a30c-b17dd060a0e2	1cda4767-b26a-4a9e-aa6d-d52cf46c2dc3	80d23eb8-1156-49dc-b5c3-42cdedfab75d	COMPLETED	2026-10-18 13:27:00.00029+00	2026-10-18 13:27:00.00029+00	2026-10-19 07:11:00.00288+00	SD-KCP3-22H2	213 Parliament Road, Rajagiriya	249 William Gopallawa Mawatha, Kandy	Methmi Wickramasinghe	SMALL	4.63	DELIVERED
87ef3a07-cbfe-4c77-acc3-0283e5332f7d	4715e0ea-76ef-463d-8e83-d05d2353b0dd	03706d2d-c424-4359-8d98-702189d2a120	7d19bfe5-daf8-4879-8f88-7fcc9bff34c5	COMPLETED	2026-10-19 04:00:59.999597+00	2026-10-19 04:00:59.999598+00	2026-10-20 05:29:00.003905+00	SD-4NX8-FGHF	340 Wakwella Road, Galle	344 Parliament Road, Rajagiriya	Dinuka Bandara	LARGE	5.37	DELIVERED
faf60aec-153a-4760-9572-0334b60f1d9b	3ca7d22d-0927-46b3-ad2b-8a0b228c0610	16d13c48-6ea3-48b5-b0b5-fbe7fcefec4d	49c139c8-8007-4199-80b6-e68a928a59fb	COMPLETED	2026-10-19 04:56:59.999557+00	2026-10-19 04:56:59.999558+00	2026-10-19 10:14:00.003223+00	SD-SAJE-GPFW	108 Matara Road, Galle	140 Ramakrishna Road, Colombo 06	Chamari Ratnayake	MEDIUM	4.97	DELIVERED
f200513b-689a-4dac-bc13-223f74a8d913	d8fa34e7-11ee-49b9-b3c1-f1b262f3a9ec	f8264489-f505-49cf-817d-d4373523e808	49c139c8-8007-4199-80b6-e68a928a59fb	COMPLETED	2026-10-19 05:39:59.999971+00	2026-10-19 05:39:59.999972+00	2026-10-19 10:34:00.003048+00	SD-EHU9-M26L	317 Thimbirigasyaya Road, Colombo 05	204 Prince of Wales Avenue, Colombo 14	Sajith Dissanayake	LARGE	9.51	DELIVERED
34841185-4b1a-46fe-b12c-799eca471300	cbbcdbbe-f230-4bc7-b26e-8dcda1daa3f0	f8264489-f505-49cf-817d-d4373523e808	7d19bfe5-daf8-4879-8f88-7fcc9bff34c5	COMPLETED	2026-10-19 10:19:00.000013+00	2026-10-19 10:19:00.000013+00	2026-10-20 07:04:00.002879+00	SD-4BBR-F88T	341 Kollupitiya Road, Colombo 03	202 Nawala Road, Rajagiriya	Methmi Jayasinghe	LARGE	17.81	DELIVERED
acded89e-4bdd-4a41-a997-9be1c2e1448a	983484fc-e9ae-464f-9d2a-c8cde5909a94	0692e9c1-ff92-4d13-ae68-a7c2168947c3	7d19bfe5-daf8-4879-8f88-7fcc9bff34c5	COMPLETED	2026-10-19 10:48:00.000127+00	2026-10-19 10:48:00.000127+00	2026-10-20 06:56:00.003025+00	SD-T9DN-WCXZ	76 Peradeniya Road, Kandy	16 Main Street, Colombo 11	Lahiru Fonseka	EXTRA_LARGE	19.40	DELIVERED
d356963f-7c14-4147-a38f-c771dead51fb	0619ed67-a4c9-4b48-924b-3875a6050408	1cda4767-b26a-4a9e-aa6d-d52cf46c2dc3	80d23eb8-1156-49dc-b5c3-42cdedfab75d	COMPLETED	2026-10-20 02:59:59.999516+00	2026-10-20 02:59:59.999517+00	2026-10-20 10:53:00.003148+00	SD-55W6-KET6	39 Prince of Wales Avenue, Colombo 14	95 De Saram Road, Mount Lavinia	Kamal Nawaz	MEDIUM	6.26	DELIVERED
727462b8-d350-4e29-954f-624a83cb6db7	55f09d8e-5b23-463a-b319-0738a010d832	5ba3a5c8-4911-45c9-9155-84e05b5dae36	7d19bfe5-daf8-4879-8f88-7fcc9bff34c5	COMPLETED	2026-10-20 08:40:59.999516+00	2026-10-20 08:40:59.999516+00	2026-10-21 06:28:00.003803+00	SD-GMGZ-YXYK	59 Station Road, Nugegoda	259 High Level Road, Maharagama	Supuni Silva	SMALL	4.47	DELIVERED
60f56c11-2dde-4c6c-8580-181ca2b50db0	eeedbe72-1230-4b14-8e9b-d05955707919	2afe8e87-d5cd-41ea-99b4-db04a7f48d25	7d19bfe5-daf8-4879-8f88-7fcc9bff34c5	COMPLETED	2026-10-20 13:25:00.000317+00	2026-10-20 13:25:00.000317+00	2026-10-21 05:33:00.003322+00	SD-M656-J5QX	322 Pita Kotte Road, Kotte	286 Peradeniya Road, Kandy	Sahan Peiris	MEDIUM	6.90	DELIVERED
d74f2ee9-9946-4897-8cc4-d52699f27d0a	100430b7-1441-4029-bb23-a672c473a848	2a4e4e19-00b3-4d93-b3f5-185ca3701d3d	80d23eb8-1156-49dc-b5c3-42cdedfab75d	COMPLETED	2026-10-20 13:37:59.9997+00	2026-10-20 13:37:59.9997+00	2026-10-21 06:26:00.002723+00	SD-XJDQ-2W37	203 Galle Road, Colombo 03	269 Colombo Road, Negombo	Yasiru Wickramasinghe	SMALL	4.29	DELIVERED
350193c7-d419-41c6-8551-4f07a461d7ec	265cf5ac-5aa0-4da0-ad39-c7c7f1d84ec3	16d13c48-6ea3-48b5-b0b5-fbe7fcefec4d	49c139c8-8007-4199-80b6-e68a928a59fb	COMPLETED	2026-10-21 04:00:00.000028+00	2026-10-21 04:00:00.000028+00	2026-10-22 05:03:00.003707+00	SD-TYRL-PL42	173 Main Street, Negombo	100 Main Street, Negombo	Fathima Jayawardena	MEDIUM	8.59	DELIVERED
54a8c8fe-ed62-4b8a-a1bb-f9261cfc0d79	92ac4dca-4634-47d4-84bc-871c1d324371	5ba3a5c8-4911-45c9-9155-84e05b5dae36	7d19bfe5-daf8-4879-8f88-7fcc9bff34c5	COMPLETED	2026-10-21 08:29:00.000031+00	2026-10-21 08:29:00.000031+00	2026-10-22 06:29:00.003215+00	SD-JP9E-TESU	189 Galle Road, Colombo 03	13 Baseline Road, Colombo 09	Anjali Rajapaksa	LARGE	13.24	DELIVERED
ff2663ac-0ec3-4ed6-819f-f611b6fd676a	274a1dea-8b3d-48fc-806f-70dde0e4b26c	03706d2d-c424-4359-8d98-702189d2a120	7d19bfe5-daf8-4879-8f88-7fcc9bff34c5	COMPLETED	2026-10-21 13:12:59.999816+00	2026-10-21 13:12:59.999817+00	2026-10-22 06:37:00.003082+00	SD-7KEZ-ULPD	139 York Street, Colombo 01	222 Colombo Road, Negombo	Malith Rajendran	MEDIUM	3.06	DELIVERED
e2c415fd-ee74-417f-956b-947a7abc63e5	3f9161f0-7a8c-400d-b563-b4c42c29a0ce	1cda4767-b26a-4a9e-aa6d-d52cf46c2dc3	49c139c8-8007-4199-80b6-e68a928a59fb	FAILED	2026-10-22 06:09:00.000261+00	2026-10-22 06:09:00.000262+00	2026-10-23 06:39:00.003916+00	SD-J9D9-QV8R	75 Kotte Road, Kotte	209 Pita Kotte Road, Kotte	Malith Fonseka	SMALL	3.58	FAILED
ee31ad5f-4d02-476a-9bc0-1f98ad33aab3	f5579fdb-ee46-4763-80f2-b98f651da483	16d13c48-6ea3-48b5-b0b5-fbe7fcefec4d	7d19bfe5-daf8-4879-8f88-7fcc9bff34c5	COMPLETED	2026-10-22 07:10:59.999873+00	2026-10-22 07:10:59.999874+00	2026-10-23 06:51:00.003682+00	SD-J2SH-9KTV	278 De Saram Road, Mount Lavinia	95 Colombo Road, Negombo	Mohamed Fonseka	MEDIUM	2.14	DELIVERED
f07cd848-694a-4b91-bee3-a40dff16cce8	259702bd-95f9-4fe3-a4e7-3bb844bcb584	f8264489-f505-49cf-817d-d4373523e808	49c139c8-8007-4199-80b6-e68a928a59fb	COMPLETED	2026-10-22 08:18:00.000334+00	2026-10-22 08:18:00.000335+00	2026-10-23 04:21:00.003364+00	SD-5RTY-44PK	338 Flower Road, Colombo 07	124 Hotel Road, Mount Lavinia	Nipuni Rathnayake	MEDIUM	3.76	DELIVERED
897696ea-a219-4dce-b9d6-4dc1fdc43e0d	4771a473-3c20-42fb-aa83-5130f32ca950	16d13c48-6ea3-48b5-b0b5-fbe7fcefec4d	80d23eb8-1156-49dc-b5c3-42cdedfab75d	COMPLETED	2026-10-23 03:01:59.999899+00	2026-10-23 03:01:59.9999+00	2026-10-24 05:03:00.003097+00	SD-FVPP-JH8U	325 Station Road, Nugegoda	8 Parliament Road, Rajagiriya	Gayani Rahman	SMALL	3.79	DELIVERED
351a51c5-55f2-4450-9bd0-c007d9e58b92	c8f0d779-1d12-4540-beb8-61bcdca2ba2e	ff655d35-a0cb-4ec7-a11c-25e7cb868ddf	80d23eb8-1156-49dc-b5c3-42cdedfab75d	COMPLETED	2026-10-23 05:42:00.000465+00	2026-10-23 05:42:00.000466+00	2026-10-24 06:44:00.00286+00	SD-6V97-M8EK	274 Marine Drive, Colombo 06	26 Chatham Street, Colombo 01	Dinuka Peiris	SMALL	4.08	DELIVERED
9340b600-4aee-4e66-831d-02e416b161da	13eb7150-229b-46b2-b173-36e8bb11baf8	ff655d35-a0cb-4ec7-a11c-25e7cb868ddf	7d19bfe5-daf8-4879-8f88-7fcc9bff34c5	COMPLETED	2026-10-23 09:10:59.999623+00	2026-10-23 09:10:59.999624+00	2026-10-24 07:05:00.00303+00	SD-KDC3-UXBP	125 Galle Road, Colombo 03	344 Marine Drive, Colombo 06	Sajith Mendis	LARGE	9.04	DELIVERED
67e96926-8ef3-4bdb-bce8-6b1120bb627a	cbea84fd-c8ed-4d73-9bd5-df479039bd8f	0692e9c1-ff92-4d13-ae68-a7c2168947c3	7d19bfe5-daf8-4879-8f88-7fcc9bff34c5	COMPLETED	2026-10-23 10:44:59.999884+00	2026-10-23 10:44:59.999884+00	2026-10-24 06:19:00.00247+00	SD-E8WS-B8EP	1 Old Kesbewa Road, Nugegoda	140 Old Kesbewa Road, Nugegoda	Dimuth Rahman	MEDIUM	6.91	DELIVERED
426f1866-873e-4bdf-9491-d31ee86961c8	ead4e17d-9a13-4c43-8881-85dbf3b19dfc	16d13c48-6ea3-48b5-b0b5-fbe7fcefec4d	7d19bfe5-daf8-4879-8f88-7fcc9bff34c5	COMPLETED	2026-10-23 13:08:59.999573+00	2026-10-23 13:08:59.999573+00	2026-10-24 06:06:00.002835+00	SD-SGMM-9AUS	319 Lauries Road, Colombo 04	224 William Gopallawa Mawatha, Kandy	Kumar Ratnayake	SMALL	4.66	DELIVERED
c7de9b09-233d-4c46-961b-c85c97f37d36	985083b0-3bae-4f53-8de7-079bebddd131	f8264489-f505-49cf-817d-d4373523e808	7d19bfe5-daf8-4879-8f88-7fcc9bff34c5	COMPLETED	2026-10-24 04:09:00.000496+00	2026-10-24 04:09:00.000496+00	2026-10-25 04:07:00.002714+00	SD-QJEY-F5FC	174 Flower Road, Colombo 07	183 Hotel Road, Mount Lavinia	Dinuka Rajendran	MEDIUM	9.96	DELIVERED
86806dca-45c2-40b9-93d4-635af3bc1210	c6c405f0-dacd-4762-bfc0-f7d3ab81b970	ff655d35-a0cb-4ec7-a11c-25e7cb868ddf	7d19bfe5-daf8-4879-8f88-7fcc9bff34c5	COMPLETED	2026-10-24 05:59:00.000082+00	2026-10-24 05:59:00.000082+00	2026-10-25 07:20:00.00298+00	SD-DBTB-6DD2	48 Galle Road, Colombo 04	188 Ward Place, Colombo 07	Lakshmi Ratnayake	LARGE	19.57	DELIVERED
35ba6e19-9e32-46e2-8bbe-4de22b24a929	2936c052-61e5-47c5-b409-a701e9213b28	ff655d35-a0cb-4ec7-a11c-25e7cb868ddf	49c139c8-8007-4199-80b6-e68a928a59fb	COMPLETED	2026-10-24 07:19:59.999771+00	2026-10-24 07:19:59.999771+00	2026-10-25 06:08:00.003144+00	SD-M9Q9-63N9	81 Galle Road, Colombo 04	64 De Saram Road, Mount Lavinia	Malith Samarasinghe	SMALL	4.87	DELIVERED
8f9bac46-bdbf-4a4c-ad9f-520aa9a3c6c3	475f8fbe-a8f7-4b0c-9a30-4a28371c45e4	1cda4767-b26a-4a9e-aa6d-d52cf46c2dc3	49c139c8-8007-4199-80b6-e68a928a59fb	COMPLETED	2026-10-24 07:02:59.999871+00	2026-10-24 07:02:59.999871+00	2026-10-25 06:21:00.002989+00	SD-5VUB-V6FQ	68 Pita Kotte Road, Kotte	221 Flower Road, Colombo 07	Gayani Silva	MEDIUM	5.14	DELIVERED
781a409e-bb49-41e5-9ef5-c31b82e60c1a	7b429928-4702-4e61-8bf1-2f4130b6c012	f8264489-f505-49cf-817d-d4373523e808	80d23eb8-1156-49dc-b5c3-42cdedfab75d	COMPLETED	2026-10-25 07:44:00.000474+00	2026-10-25 07:44:00.000474+00	2026-10-26 07:46:00.003088+00	SD-RZE8-TNMX	220 Stanley Thilakaratne Mawatha, Nugegoda	2 Prince of Wales Avenue, Colombo 14	Sachini Samarasinghe	SMALL	2.91	DELIVERED
4607fb11-d3fa-4019-aa0a-2ffe3a48098d	f2711868-85e6-45fb-8a40-98b9ef284ca8	5ba3a5c8-4911-45c9-9155-84e05b5dae36	80d23eb8-1156-49dc-b5c3-42cdedfab75d	COMPLETED	2026-10-25 08:00:59.999835+00	2026-10-25 08:00:59.999835+00	2026-10-26 08:29:00.003354+00	SD-9M7Z-EWV8	65 Flower Road, Colombo 07	321 Kollupitiya Road, Colombo 03	Chathura Gunasekara	SMALL	1.84	DELIVERED
2e3abb9c-966f-4ccb-8a4c-c8c803dbbd0c	702a1742-f297-4314-9264-295aea208afc	03706d2d-c424-4359-8d98-702189d2a120	7d19bfe5-daf8-4879-8f88-7fcc9bff34c5	COMPLETED	2026-10-25 10:50:59.999988+00	2026-10-25 10:50:59.999989+00	2026-10-26 04:21:00.002621+00	SD-LUPK-7HND	119 Pita Kotte Road, Kotte	290 Thimbirigasyaya Road, Colombo 05	Sachini Gunasekara	SMALL	1.94	DELIVERED
511270ff-a015-40d5-af1d-c6c07bc2efd4	9d873dfd-c8dd-4c80-8969-e8770b78bf1a	2a4e4e19-00b3-4d93-b3f5-185ca3701d3d	7d19bfe5-daf8-4879-8f88-7fcc9bff34c5	COMPLETED	2026-10-25 11:29:00.000351+00	2026-10-25 11:29:00.000351+00	2026-10-26 05:43:00.003477+00	SD-ZEBP-NUYA	185 Hill Street, Dehiwala	290 Parliament Road, Rajagiriya	Kavindi Jayasinghe	SMALL	4.59	DELIVERED
30b699e7-7c1d-48e6-b33b-b9e77790f7ed	f6303826-52b5-450c-bbb6-feb45b9c3983	0692e9c1-ff92-4d13-ae68-a7c2168947c3	49c139c8-8007-4199-80b6-e68a928a59fb	COMPLETED	2026-10-25 11:43:00.000427+00	2026-10-25 11:43:00.000428+00	2026-10-26 05:46:00.003505+00	SD-LF9Z-LLGC	190 Ward Place, Colombo 07	52 Galle Road, Colombo 03	Dimuth Abeysekera	SMALL	2.44	DELIVERED
b5155c7c-a5eb-4ea4-a8bb-06a74fea1017	508f3e52-d482-4365-b38c-809e52433c5e	5ba3a5c8-4911-45c9-9155-84e05b5dae36	49c139c8-8007-4199-80b6-e68a928a59fb	COMPLETED	2026-10-26 03:47:59.999872+00	2026-10-26 03:47:59.999872+00	2026-10-27 05:54:00.003174+00	SD-T2K4-RB7Y	337 Kotte Road, Kotte	130 Lewis Place, Negombo	Fathima Rajendran	MEDIUM	6.46	DELIVERED
e1dde7e9-56f0-4e3e-a608-4207f790f4fe	761feb66-f668-496f-9c09-bf3dc6bcaa7c	ff655d35-a0cb-4ec7-a11c-25e7cb868ddf	49c139c8-8007-4199-80b6-e68a928a59fb	COMPLETED	2026-10-26 07:59:59.99977+00	2026-10-26 07:59:59.999771+00	2026-10-27 06:32:00.003348+00	SD-8YB3-ZZNY	299 Pita Kotte Road, Kotte	46 Old Kesbewa Road, Nugegoda	Rizwan Weerasinghe	MEDIUM	4.65	DELIVERED
9e00f2ff-8552-4113-b19d-3f04a2210b87	bd308ec9-68de-477d-b6c2-a8dc2439684a	2a4e4e19-00b3-4d93-b3f5-185ca3701d3d	7d19bfe5-daf8-4879-8f88-7fcc9bff34c5	COMPLETED	2026-10-26 08:19:00.000434+00	2026-10-26 08:19:00.000434+00	2026-10-27 05:20:00.003054+00	SD-X4XB-ZWRS	286 Union Place, Colombo 02	51 Baseline Road, Colombo 09	Shehan Dissanayake	SMALL	1.00	DELIVERED
7636bfb2-b413-4eea-868b-4ba17067a0cf	7c0305b9-6083-4470-9ab9-93e380bcf4a0	03706d2d-c424-4359-8d98-702189d2a120	7d19bfe5-daf8-4879-8f88-7fcc9bff34c5	COMPLETED	2026-10-26 08:17:00.000183+00	2026-10-26 08:17:00.000183+00	2026-10-27 05:06:00.003311+00	SD-EZMT-YAL6	344 De Saram Road, Mount Lavinia	143 De Saram Road, Mount Lavinia	Chathura Silva	MEDIUM	4.66	DELIVERED
2af6eb17-418c-4d33-81ec-912221889f58	1a1881cc-5245-4b0a-a0a8-fcd97f70ab6c	f8264489-f505-49cf-817d-d4373523e808	80d23eb8-1156-49dc-b5c3-42cdedfab75d	COMPLETED	2026-10-26 08:22:00.000175+00	2026-10-26 08:22:00.000175+00	2026-10-27 04:30:00.004353+00	SD-AR6L-J2CR	286 Old Kesbewa Road, Nugegoda	31 Galle Road, Colombo 03	Lakshmi Karunaratne	LARGE	5.70	DELIVERED
23e922d7-38a5-4a87-bafa-b94ab3567e95	90dd6e3d-e745-4863-9c64-11e57e57e63b	0692e9c1-ff92-4d13-ae68-a7c2168947c3	7d19bfe5-daf8-4879-8f88-7fcc9bff34c5	COMPLETED	2026-10-26 08:45:59.999983+00	2026-10-26 08:45:59.999984+00	2026-10-27 06:33:00.002786+00	SD-5G8H-9B4S	270 De Saram Road, Mount Lavinia	275 Lewis Place, Negombo	Hiruni Peiris	SMALL	1.38	DELIVERED
87015d47-7130-4533-8653-5408a531f634	e16f9df5-567a-4ddf-92a7-d680d5a3aabd	ff655d35-a0cb-4ec7-a11c-25e7cb868ddf	80d23eb8-1156-49dc-b5c3-42cdedfab75d	COMPLETED	2026-10-26 11:53:00.000337+00	2026-10-26 11:53:00.000337+00	2026-10-27 07:38:00.002964+00	SD-MTCH-WSDS	91 Prince of Wales Avenue, Colombo 14	187 York Street, Colombo 01	Ravindu Amarasekara	SMALL	1.71	DELIVERED
671bb8f5-6063-4cce-be77-b44c07bfaeec	53f9067c-501b-427e-8b56-c2a9b30675ef	16d13c48-6ea3-48b5-b0b5-fbe7fcefec4d	7d19bfe5-daf8-4879-8f88-7fcc9bff34c5	COMPLETED	2026-10-26 13:21:00.000101+00	2026-10-26 13:21:00.000101+00	2026-10-27 07:20:00.002286+00	SD-QC37-E8VZ	324 Galle Road, Colombo 04	48 Union Place, Colombo 02	Ravindu Rathnayake	MEDIUM	9.48	DELIVERED
8319a756-f9dd-46f3-a23f-809087b1e47d	1bd724b1-50d3-4677-a700-a3ea5893082f	03706d2d-c424-4359-8d98-702189d2a120	49c139c8-8007-4199-80b6-e68a928a59fb	COMPLETED	2026-10-27 03:54:00.000295+00	2026-10-27 03:54:00.000296+00	2026-10-28 06:20:00.00273+00	SD-JM3D-J4FD	132 Pita Kotte Road, Kotte	103 Colombo Road, Negombo	Ravindu Perera	MEDIUM	7.71	DELIVERED
8bc8b72d-ac7a-4b51-8fc8-0c357a6adf45	54f45592-9a09-42fd-b7b9-e86f81cd98e4	0692e9c1-ff92-4d13-ae68-a7c2168947c3	80d23eb8-1156-49dc-b5c3-42cdedfab75d	COMPLETED	2026-10-27 04:10:59.999694+00	2026-10-27 04:10:59.999694+00	2026-10-28 05:37:00.002596+00	SD-UED9-5DP3	76 Ward Place, Colombo 07	83 William Gopallawa Mawatha, Kandy	Sajith Liyanage	MEDIUM	2.61	DELIVERED
aa00e1cb-d75b-46a8-b527-ee74fef2739b	bc41a5af-e07a-4e9b-a7f2-bfd77dcb48c9	03706d2d-c424-4359-8d98-702189d2a120	7d19bfe5-daf8-4879-8f88-7fcc9bff34c5	COMPLETED	2026-10-27 05:52:00.000075+00	2026-10-27 05:52:00.000075+00	2026-10-28 06:00:00.002554+00	SD-8MNG-5VUK	22 Duplication Road, Colombo 03	127 Dalada Veediya, Kandy	Shehan Jayawardena	MEDIUM	2.06	DELIVERED
6b60f403-06a4-4f20-a88a-c0298c5a46ff	84c6f04e-f4b3-408d-9ac8-faa69a0fa913	5ba3a5c8-4911-45c9-9155-84e05b5dae36	49c139c8-8007-4199-80b6-e68a928a59fb	COMPLETED	2026-10-27 10:42:00.000113+00	2026-10-27 10:42:00.000113+00	2026-10-28 07:21:00.0026+00	SD-7ECB-J2LU	273 High Level Road, Maharagama	235 Stanley Thilakaratne Mawatha, Nugegoda	Sanduni Samarasinghe	SMALL	1.79	DELIVERED
db110d6d-c0fe-4cff-b667-dbec4915a3ab	26c4cf13-02eb-403f-b97f-9694cfd30cfa	1cda4767-b26a-4a9e-aa6d-d52cf46c2dc3	80d23eb8-1156-49dc-b5c3-42cdedfab75d	COMPLETED	2026-10-27 09:51:59.999542+00	2026-10-27 09:51:59.999542+00	2026-10-28 07:56:00.003023+00	SD-W7X7-VNE7	184 Havelock Road, Colombo 05	270 Old Kesbewa Road, Nugegoda	Senuri de Silva	MEDIUM	5.21	DELIVERED
fa832a0f-4250-4b27-8e70-3299e458ec0f	d452e272-1457-4b15-a829-a9d41f879b63	5ba3a5c8-4911-45c9-9155-84e05b5dae36	7d19bfe5-daf8-4879-8f88-7fcc9bff34c5	COMPLETED	2026-10-28 04:10:59.999931+00	2026-10-28 04:10:59.999931+00	2026-10-29 08:23:00.003105+00	SD-53Y2-CLUJ	191 Galle Road, Dehiwala	189 Dalada Veediya, Kandy	Sanduni Wickramasinghe	SMALL	4.77	DELIVERED
7072fefb-774e-4a93-b9e1-fa72456750ed	da576c86-4629-47eb-b26c-f19c59317de3	2afe8e87-d5cd-41ea-99b4-db04a7f48d25	49c139c8-8007-4199-80b6-e68a928a59fb	COMPLETED	2026-10-28 07:07:59.999884+00	2026-10-28 07:07:59.999885+00	2026-10-29 06:35:00.002178+00	SD-GHGW-2JLG	236 High Level Road, Maharagama	278 Galle Road, Colombo 03	Nimal Rathnayake	LARGE	10.86	DELIVERED
cdcbea39-f909-44ce-95c2-6bb61b153d52	a664effa-34e1-464e-9cfa-ae465c88ce80	16d13c48-6ea3-48b5-b0b5-fbe7fcefec4d	7d19bfe5-daf8-4879-8f88-7fcc9bff34c5	COMPLETED	2026-10-28 09:19:59.999989+00	2026-10-28 09:19:59.999989+00	2026-10-29 08:07:00.003493+00	SD-Z2Z7-D3VS	279 Ramakrishna Road, Colombo 06	286 Baseline Road, Colombo 09	Kasun Rahman	MEDIUM	9.89	DELIVERED
9a742200-4824-4ba5-92dc-532166ef5dda	3846546d-0373-4cda-831e-7527e5396e1a	f8264489-f505-49cf-817d-d4373523e808	49c139c8-8007-4199-80b6-e68a928a59fb	COMPLETED	2026-10-28 10:24:00.000197+00	2026-10-28 10:24:00.000197+00	2026-10-29 07:20:00.002849+00	SD-Q2A4-JBGM	128 Hotel Road, Mount Lavinia	234 Duplication Road, Colombo 03	Dimuth Mendis	SMALL	3.18	DELIVERED
3cd38393-9df4-4002-8e8a-961824cec45d	e03c0235-3aaa-4f97-8525-dea1deb9f838	16d13c48-6ea3-48b5-b0b5-fbe7fcefec4d	7d19bfe5-daf8-4879-8f88-7fcc9bff34c5	COMPLETED	2026-10-28 10:14:59.999915+00	2026-10-28 10:14:59.999915+00	2026-10-29 04:26:00.002987+00	SD-LEWP-QLDK	145 Colombo Road, Negombo	188 Hotel Road, Mount Lavinia	Pradeep Amarasekara	MEDIUM	8.12	DELIVERED
1ce465ab-ba6f-4a67-95d5-4fd98e0b6104	8c44d18e-7fbb-472d-bbe0-1fc9532473e6	0692e9c1-ff92-4d13-ae68-a7c2168947c3	7d19bfe5-daf8-4879-8f88-7fcc9bff34c5	COMPLETED	2026-10-28 11:54:59.999632+00	2026-10-28 11:54:59.999632+00	2026-10-29 07:22:00.002584+00	SD-C6XT-3GPG	279 Hotel Road, Mount Lavinia	330 Colombo Road, Negombo	Fathima Peiris	MEDIUM	5.83	DELIVERED
99a5991d-8ba6-458d-abbe-29ef339eb54a	cbbaf6c9-6ddf-4797-ac38-0406007b4d71	2afe8e87-d5cd-41ea-99b4-db04a7f48d25	7d19bfe5-daf8-4879-8f88-7fcc9bff34c5	COMPLETED	2026-10-29 07:50:00.00038+00	2026-10-29 07:50:00.00038+00	2026-10-30 08:23:00.002573+00	SD-HLU6-XPPR	306 Kotte Road, Kotte	135 Stanley Thilakaratne Mawatha, Nugegoda	Fathima Rahman	LARGE	16.42	DELIVERED
1636e736-0749-4351-af97-38de26b4825b	4b7350b0-e74d-4735-9998-51aefe940325	2afe8e87-d5cd-41ea-99b4-db04a7f48d25	80d23eb8-1156-49dc-b5c3-42cdedfab75d	COMPLETED	2026-10-29 12:05:00.000369+00	2026-10-29 12:05:00.00037+00	2026-10-30 07:31:00.002614+00	SD-ZCSE-9JFM	333 High Level Road, Maharagama	205 Wakwella Road, Galle	Thisaru Gunasekara	LARGE	6.47	DELIVERED
9d54121d-f1de-4026-8a9b-bdc0c2365f73	0a4da429-2aae-4bce-8760-d4bc3fb5c619	2afe8e87-d5cd-41ea-99b4-db04a7f48d25	49c139c8-8007-4199-80b6-e68a928a59fb	COMPLETED	2026-10-30 04:45:59.999566+00	2026-10-30 04:45:59.999566+00	2026-10-31 06:25:00.003565+00	SD-7T73-6WBB	33 Stanley Thilakaratne Mawatha, Nugegoda	11 Galle Road, Dehiwala	Kumar Rajapaksa	LARGE	19.71	DELIVERED
8c78b183-1928-4b5c-893d-972a51865f6c	50448a37-8739-4800-b2df-6cf2538188cc	2a4e4e19-00b3-4d93-b3f5-185ca3701d3d	80d23eb8-1156-49dc-b5c3-42cdedfab75d	COMPLETED	2026-10-30 08:41:59.9997+00	2026-10-30 08:41:59.9997+00	2026-10-31 05:54:00.003775+00	SD-XXF6-5ECC	341 Chatham Street, Colombo 01	48 Marine Drive, Colombo 06	Senuri Wijesinghe	LARGE	5.30	DELIVERED
4c1b84fa-72b6-4e55-bf7e-16b042578fa6	3d3b84eb-d948-4821-b0ad-6debaa8eb288	f8264489-f505-49cf-817d-d4373523e808	80d23eb8-1156-49dc-b5c3-42cdedfab75d	COMPLETED	2026-10-30 12:35:59.999792+00	2026-10-30 12:35:59.999792+00	2026-10-31 05:29:00.003563+00	SD-DQK9-4XKU	27 Station Road, Nugegoda	172 Main Street, Negombo	Thisaru Amarasekara	MEDIUM	2.28	DELIVERED
1dbd034a-ba0e-45a4-96fc-c22eb685a215	c1419eee-1e07-4070-b4ea-a2fe89eec2bd	16d13c48-6ea3-48b5-b0b5-fbe7fcefec4d	80d23eb8-1156-49dc-b5c3-42cdedfab75d	COMPLETED	2026-10-30 12:32:59.999861+00	2026-10-30 12:32:59.999861+00	2026-10-31 04:46:00.003229+00	SD-P2SG-9HT3	135 Galle Road, Dehiwala	174 Nawala Road, Rajagiriya	Shehan Amarasekara	LARGE	12.28	DELIVERED
94beb42a-da24-485a-82e6-fc32b42a6451	bb9ca9b2-8efb-4b91-9abb-7547f1ffd3b3	2afe8e87-d5cd-41ea-99b4-db04a7f48d25	49c139c8-8007-4199-80b6-e68a928a59fb	COMPLETED	2026-10-30 13:16:59.99954+00	2026-10-30 13:16:59.99954+00	2026-10-31 07:18:00.002998+00	SD-SEK3-B3VS	127 Kollupitiya Road, Colombo 03	131 William Gopallawa Mawatha, Kandy	Kumar Perera	MEDIUM	6.54	DELIVERED
70cc21aa-425d-4a62-9e3f-a3a52cee8d24	ef1721cb-e74b-49a6-af84-cf00837f7d37	2afe8e87-d5cd-41ea-99b4-db04a7f48d25	7d19bfe5-daf8-4879-8f88-7fcc9bff34c5	COMPLETED	2026-10-31 07:31:00.000158+00	2026-10-31 07:31:00.000158+00	2026-11-01 05:58:00.003104+00	SD-V8Z8-TWA3	136 Stanley Thilakaratne Mawatha, Nugegoda	27 Lauries Road, Colombo 04	Tharindu Abeysekera	SMALL	3.91	DELIVERED
fec79f00-10e9-4077-8021-689cf8fc99d7	43f72cd7-e07c-4f46-be71-a1bf1d3836d3	0692e9c1-ff92-4d13-ae68-a7c2168947c3	80d23eb8-1156-49dc-b5c3-42cdedfab75d	COMPLETED	2026-10-31 07:30:00.000027+00	2026-10-31 07:30:00.000027+00	2026-11-01 06:59:00.002194+00	SD-GWCS-USVN	185 Main Street, Colombo 11	211 High Level Road, Maharagama	Isuru Wickramasinghe	MEDIUM	4.73	DELIVERED
719739d7-0098-4188-87af-50ac40cbe1b3	2f35b695-ad79-4fe0-ab86-24e21344c7d7	16d13c48-6ea3-48b5-b0b5-fbe7fcefec4d	7d19bfe5-daf8-4879-8f88-7fcc9bff34c5	COMPLETED	2026-10-31 08:41:59.999806+00	2026-10-31 08:41:59.999806+00	2026-11-01 04:29:00.003228+00	SD-NG49-R5WT	166 High Level Road, Maharagama	112 Galle Road, Dehiwala	Sahan Nawaz	MEDIUM	9.21	DELIVERED
3d3161d8-c1e8-45bf-8893-635454918dc8	c970ea88-952f-45e7-af36-81320c41bb3a	ff655d35-a0cb-4ec7-a11c-25e7cb868ddf	49c139c8-8007-4199-80b6-e68a928a59fb	COMPLETED	2026-10-31 09:41:59.999951+00	2026-10-31 09:41:59.999951+00	2026-11-01 05:08:00.002603+00	SD-2GEU-HYGG	175 Galle Road, Dehiwala	244 Ward Place, Colombo 07	Nadeesha Gunasekara	LARGE	13.45	DELIVERED
887f94a5-4c0a-4d3f-8cd8-e23b2f536303	2cf24291-90fd-4e07-a30b-da93d867cb5f	2a4e4e19-00b3-4d93-b3f5-185ca3701d3d	80d23eb8-1156-49dc-b5c3-42cdedfab75d	COMPLETED	2026-10-31 09:40:00.00001+00	2026-10-31 09:40:00.00001+00	2026-11-01 07:12:00.003208+00	SD-39AC-699G	285 Pita Kotte Road, Kotte	210 Hotel Road, Mount Lavinia	Kavindi Rathnayake	MEDIUM	5.10	DELIVERED
87b5bc3b-ff2e-4cde-999f-c1cc3416b1aa	a6fb9868-8c93-4449-abd7-9ca318c7d52b	16d13c48-6ea3-48b5-b0b5-fbe7fcefec4d	7d19bfe5-daf8-4879-8f88-7fcc9bff34c5	COMPLETED	2026-10-31 11:13:00.000354+00	2026-10-31 11:13:00.000354+00	2026-11-01 05:18:00.002545+00	SD-3CAE-PNMA	311 Hotel Road, Mount Lavinia	45 Union Place, Colombo 02	Kamal Mendis	SMALL	4.46	DELIVERED
54302b37-069b-427b-b459-9f95646a75cf	ab423278-d8b7-439d-ac95-b0883520d0b1	ff655d35-a0cb-4ec7-a11c-25e7cb868ddf	80d23eb8-1156-49dc-b5c3-42cdedfab75d	COMPLETED	2026-10-31 13:44:00.000469+00	2026-10-31 13:44:00.000469+00	2026-11-01 04:30:00.003092+00	SD-ALS3-JZPW	183 Matara Road, Galle	126 Peradeniya Road, Kandy	Chathura Abeysekera	MEDIUM	3.16	DELIVERED
70807807-27fe-4927-8195-a1f1676222cc	4d33d968-9693-4dd2-8135-40bf71636df1	0692e9c1-ff92-4d13-ae68-a7c2168947c3	7d19bfe5-daf8-4879-8f88-7fcc9bff34c5	COMPLETED	2026-10-31 13:23:59.999816+00	2026-10-31 13:23:59.999816+00	2026-11-01 07:25:00.003313+00	SD-2GT3-J42F	222 Ward Place, Colombo 07	284 Ward Place, Colombo 07	Gayani Herath	LARGE	19.80	DELIVERED
1bae2b30-3623-45e4-b069-a7082e9ada4e	500aa574-bab8-45e5-b3d3-522062843214	ff655d35-a0cb-4ec7-a11c-25e7cb868ddf	49c139c8-8007-4199-80b6-e68a928a59fb	COMPLETED	2026-11-01 06:17:59.999545+00	2026-11-01 06:17:59.999545+00	2026-11-02 07:00:00.002485+00	SD-L7BM-3TGC	268 Hill Street, Dehiwala	138 Parliament Road, Rajagiriya	Ishara Fonseka	MEDIUM	8.50	DELIVERED
089d7be6-6302-42aa-a909-6e4000a5b486	b846c0b3-5018-4270-a07e-c7469c4fdee3	2afe8e87-d5cd-41ea-99b4-db04a7f48d25	7d19bfe5-daf8-4879-8f88-7fcc9bff34c5	COMPLETED	2026-11-01 08:35:00.000242+00	2026-11-01 08:35:00.000242+00	2026-11-02 09:08:00.002562+00	SD-RTC5-NURE	286 Hill Street, Dehiwala	159 Union Place, Colombo 02	Yasiru Weerasinghe	LARGE	17.98	DELIVERED
07c13299-fc40-4167-8d45-08d48af97e32	f37b7ae7-588d-418b-b2bf-3187b8eb1b50	ff655d35-a0cb-4ec7-a11c-25e7cb868ddf	49c139c8-8007-4199-80b6-e68a928a59fb	COMPLETED	2026-11-01 08:17:00.000286+00	2026-11-01 08:17:00.000286+00	2026-11-02 03:45:00.002969+00	SD-LQCX-DBEE	11 Kotte Road, Kotte	264 Parliament Road, Rajagiriya	Supuni Karunaratne	SMALL	4.18	DELIVERED
5711d0eb-b8fc-41b6-9d2f-e617d9c0a940	b8b6d8b3-dfde-4e6c-ae87-8cc25aed604c	0692e9c1-ff92-4d13-ae68-a7c2168947c3	7d19bfe5-daf8-4879-8f88-7fcc9bff34c5	COMPLETED	2026-11-01 10:11:59.999573+00	2026-11-01 10:11:59.999573+00	2026-11-02 04:52:00.00255+00	SD-Y3RS-VTBV	211 Union Place, Colombo 02	327 Peradeniya Road, Kandy	Sanduni Jayawardena	SMALL	3.96	DELIVERED
cea8bbd2-b2b1-4d0c-83a0-bbb466f4c32f	f9389473-cb21-4895-bb99-2722a29d9a14	ff655d35-a0cb-4ec7-a11c-25e7cb868ddf	49c139c8-8007-4199-80b6-e68a928a59fb	COMPLETED	2026-11-01 13:05:00.0001+00	2026-11-01 13:05:00.0001+00	2026-11-02 05:55:00.002701+00	SD-TGPK-TTMT	309 Kotte Road, Kotte	190 Galle Road, Colombo 03	Lakshmi Bandara	MEDIUM	9.76	DELIVERED
d543f745-e2ba-429a-bf9b-675162a70b4a	9bc6597f-9598-49d4-8fb0-086557d8cf16	0692e9c1-ff92-4d13-ae68-a7c2168947c3	7d19bfe5-daf8-4879-8f88-7fcc9bff34c5	COMPLETED	2026-11-02 06:17:59.999556+00	2026-11-02 06:17:59.999556+00	2026-11-03 04:22:00.003053+00	SD-RDLJ-PE62	160 Marine Drive, Colombo 06	63 Union Place, Colombo 02	Sunil Wijesinghe	MEDIUM	9.11	DELIVERED
6358f8b4-f9f2-4f9a-bba5-5a214db553af	81544f0c-8114-46b7-aee3-457ee27a0c16	2afe8e87-d5cd-41ea-99b4-db04a7f48d25	49c139c8-8007-4199-80b6-e68a928a59fb	COMPLETED	2026-11-02 07:48:00.000064+00	2026-11-02 07:48:00.000064+00	2026-11-03 07:06:00.0025+00	SD-XVS5-47HW	34 Ramakrishna Road, Colombo 06	110 William Gopallawa Mawatha, Kandy	Malith Rathnayake	MEDIUM	7.87	DELIVERED
68bfe0c2-cf46-40c0-b506-f94d008b630c	ec87b368-16b6-425d-a73a-781b09a917d1	0692e9c1-ff92-4d13-ae68-a7c2168947c3	49c139c8-8007-4199-80b6-e68a928a59fb	FAILED	2026-11-02 09:11:59.999669+00	2026-11-02 09:11:59.999669+00	2026-11-03 07:42:00.003249+00	SD-3245-6RV8	6 Duplication Road, Colombo 03	160 Church Street, Galle Fort	Supuni Fonseka	MEDIUM	4.38	FAILED
0941e14d-80d7-4f2c-b6bd-d091ec3c8f6f	995cee5f-cbda-459a-84ed-b102ab99156b	2a4e4e19-00b3-4d93-b3f5-185ca3701d3d	49c139c8-8007-4199-80b6-e68a928a59fb	COMPLETED	2026-11-02 14:49:00.000155+00	2026-11-02 14:49:00.000155+00	2026-11-04 08:09:00.002959+00	SD-YJGX-YC7J	122 Old Kesbewa Road, Nugegoda	227 Wakwella Road, Galle	Isuru Rajendran	SMALL	1.04	DELIVERED
5a8cd0b4-3c0b-4359-a912-4b615dfdf3af	40fd09af-f7a6-414a-80a1-657cbba7b761	5ba3a5c8-4911-45c9-9155-84e05b5dae36	49c139c8-8007-4199-80b6-e68a928a59fb	COMPLETED	2026-11-02 14:16:00.000469+00	2026-11-02 14:16:00.000469+00	2026-11-03 04:53:00.002476+00	SD-M6W2-2542	77 Kotte Road, Kotte	11 Havelock Road, Colombo 05	Dulani Abeysekera	SMALL	3.98	DELIVERED
d254776e-fc57-4ad8-a225-8c668cce64f1	fec48c34-326f-4fcf-a1fc-652efee84ac8	1cda4767-b26a-4a9e-aa6d-d52cf46c2dc3	49c139c8-8007-4199-80b6-e68a928a59fb	COMPLETED	2026-11-03 06:16:00.000032+00	2026-11-03 06:16:00.000032+00	2026-11-04 06:01:00.003148+00	SD-32ZC-ETL4	337 Prince of Wales Avenue, Colombo 14	139 Dalada Veediya, Kandy	Sachini Jayasinghe	SMALL	2.35	DELIVERED
263db9a8-a715-4c93-b9d6-12886b714b75	29789d49-0c07-41ac-9e37-5ebb82f5a427	16d13c48-6ea3-48b5-b0b5-fbe7fcefec4d	80d23eb8-1156-49dc-b5c3-42cdedfab75d	COMPLETED	2026-11-03 07:54:59.999553+00	2026-11-03 07:54:59.999554+00	2026-11-04 07:48:00.003144+00	SD-LZS8-4P2D	192 Maradana Road, Colombo 10	264 Pita Kotte Road, Kotte	Chathura Rajendran	EXTRA_LARGE	17.81	DELIVERED
892fd24b-a486-47ef-8702-d8a995b5fccb	ebc23827-53fe-4197-9c98-3fd3f5753bb7	5ba3a5c8-4911-45c9-9155-84e05b5dae36	7d19bfe5-daf8-4879-8f88-7fcc9bff34c5	COMPLETED	2026-11-03 09:21:59.999663+00	2026-11-03 09:21:59.999663+00	2026-11-04 05:32:00.002144+00	SD-XBBR-KQTJ	342 Old Kesbewa Road, Nugegoda	150 Peradeniya Road, Kandy	Dilani Ratnayake	MEDIUM	3.22	DELIVERED
eb50528a-afd0-4004-9b92-551ac27cd26e	91a1f196-c66f-4936-9239-46fca897988b	f8264489-f505-49cf-817d-d4373523e808	49c139c8-8007-4199-80b6-e68a928a59fb	COMPLETED	2026-11-03 10:26:00.000031+00	2026-11-03 10:26:00.000032+00	2026-11-04 07:58:00.002359+00	SD-RJUT-ZU4L	281 Station Road, Nugegoda	139 Havelock Road, Colombo 05	Sunil Liyanage	SMALL	0.83	DELIVERED
b8255987-a285-407e-bc66-7b072ddeba42	e859265a-9bb0-46a5-a720-298b7ccb3f03	5ba3a5c8-4911-45c9-9155-84e05b5dae36	49c139c8-8007-4199-80b6-e68a928a59fb	COMPLETED	2026-11-03 11:45:00.000026+00	2026-11-03 11:45:00.000026+00	2026-11-04 05:46:00.002319+00	SD-CMJV-PMJG	263 Kotte Road, Kotte	160 Galle Road, Colombo 03	Senuri Weerasinghe	SMALL	4.50	DELIVERED
b0bb373c-ddd1-4eda-acf3-58b18c274348	5347cb83-0705-4c6f-a702-e2e976ac95e8	1cda4767-b26a-4a9e-aa6d-d52cf46c2dc3	7d19bfe5-daf8-4879-8f88-7fcc9bff34c5	COMPLETED	2026-11-03 13:40:00.000189+00	2026-11-03 13:40:00.000189+00	2026-11-04 06:11:00.002948+00	SD-8KYG-7VH3	198 High Level Road, Maharagama	183 Ramakrishna Road, Colombo 06	Shehan Peiris	MEDIUM	9.91	DELIVERED
fe5bbeba-5351-4527-b91f-a93ac044fd32	ba4abce7-774f-4b44-abf1-4594020602da	1cda4767-b26a-4a9e-aa6d-d52cf46c2dc3	49c139c8-8007-4199-80b6-e68a928a59fb	COMPLETED	2026-11-03 14:23:00.000199+00	2026-11-03 14:23:00.000199+00	2026-11-05 08:50:00.002664+00	SD-RMRQ-KNNF	255 Pita Kotte Road, Kotte	113 Marine Drive, Colombo 06	Arun Wickramasinghe	SMALL	1.63	DELIVERED
b3655508-4159-4da9-b953-344e52c106c0	b14eb3ee-6d36-4429-ac64-0d4300f8cb28	ff655d35-a0cb-4ec7-a11c-25e7cb868ddf	80d23eb8-1156-49dc-b5c3-42cdedfab75d	COMPLETED	2026-11-03 14:54:00.000328+00	2026-11-03 14:54:00.000329+00	2026-11-04 04:42:00.002187+00	SD-N62D-HLZR	78 Hill Street, Dehiwala	43 Hotel Road, Mount Lavinia	Tharindu Karunaratne	LARGE	16.49	DELIVERED
7ca27227-25cd-40d0-80d7-1996c96cd686	46f77593-51d5-4be9-a077-a3c1dced6899	0692e9c1-ff92-4d13-ae68-a7c2168947c3	7d19bfe5-daf8-4879-8f88-7fcc9bff34c5	COMPLETED	2026-11-04 06:08:59.999508+00	2026-11-04 06:08:59.999508+00	2026-11-05 06:54:00.002062+00	SD-L8EM-7BQN	316 Parliament Road, Rajagiriya	240 Kotte Road, Kotte	Fathima Rathnayake	SMALL	3.53	DELIVERED
a703e02d-e351-4c3a-8c3e-7d8c300d3d90	40a1d40c-739b-4a8d-9150-6ed9d34cb9f3	ff655d35-a0cb-4ec7-a11c-25e7cb868ddf	80d23eb8-1156-49dc-b5c3-42cdedfab75d	COMPLETED	2026-11-04 05:45:00.000422+00	2026-11-04 05:45:00.000423+00	2026-11-05 08:02:00.002298+00	SD-V2VX-99CH	16 Galle Road, Colombo 03	342 York Street, Colombo 01	Gayani Samarasinghe	LARGE	19.13	DELIVERED
a577e667-d29a-48b2-8263-11ed23ad9e1f	6dba8eae-6eab-423b-8706-8364ca0d869b	16d13c48-6ea3-48b5-b0b5-fbe7fcefec4d	80d23eb8-1156-49dc-b5c3-42cdedfab75d	COMPLETED	2026-11-04 07:26:00.000337+00	2026-11-04 07:26:00.000337+00	2026-11-05 06:09:00.003063+00	SD-VYSA-GCCK	185 Galle Road, Colombo 03	173 High Level Road, Maharagama	Malith Abeysekera	SMALL	1.56	DELIVERED
4945d7d4-11b5-43fc-8054-97394e688b00	7f148b3c-4657-426d-a18c-2fc11899a370	0692e9c1-ff92-4d13-ae68-a7c2168947c3	80d23eb8-1156-49dc-b5c3-42cdedfab75d	COMPLETED	2026-11-04 13:18:00.000248+00	2026-11-04 13:18:00.000249+00	2026-11-05 08:40:00.002712+00	SD-UB4W-GKDB	16 Stanley Thilakaratne Mawatha, Nugegoda	171 Lauries Road, Colombo 04	Oshada Wijesinghe	SMALL	1.38	DELIVERED
883cfcff-0f1e-4988-a2aa-180ee702de0b	52fad28c-76fc-40d7-ba01-830b91b79c66	2afe8e87-d5cd-41ea-99b4-db04a7f48d25	80d23eb8-1156-49dc-b5c3-42cdedfab75d	COMPLETED	2026-11-04 14:31:00.000185+00	2026-11-04 14:31:00.000185+00	2026-11-05 06:38:00.002617+00	SD-8F8K-93AV	278 Main Street, Negombo	266 Hotel Road, Mount Lavinia	Nadeesha Samarasinghe	SMALL	1.98	DELIVERED
2fc330b3-c400-45e5-a183-5fd2016f36b8	8a6f1325-8cef-4c13-a545-e64637d37711	f8264489-f505-49cf-817d-d4373523e808	80d23eb8-1156-49dc-b5c3-42cdedfab75d	COMPLETED	2026-11-05 05:12:59.99983+00	2026-11-05 05:12:59.99983+00	2026-11-06 05:25:00.002352+00	SD-CTA6-KMK8	45 Hotel Road, Mount Lavinia	335 Old Kesbewa Road, Nugegoda	Mohamed Cooray	MEDIUM	2.03	DELIVERED
55e565e6-b5cf-460f-9f63-2beb7b3f5ed6	166fc649-190a-440a-ad29-dc6d89d73bbe	0692e9c1-ff92-4d13-ae68-a7c2168947c3	7d19bfe5-daf8-4879-8f88-7fcc9bff34c5	COMPLETED	2026-11-05 09:52:00.000189+00	2026-11-05 09:52:00.000189+00	2026-11-06 06:38:00.004879+00	SD-7LXK-N6DJ	29 Pita Kotte Road, Kotte	336 Stanley Thilakaratne Mawatha, Nugegoda	Priya Jayasinghe	SMALL	2.31	DELIVERED
0020f9d0-b315-435a-95b1-55eb979c4532	8aacec1d-76c7-41a9-a672-08fd4653b531	2a4e4e19-00b3-4d93-b3f5-185ca3701d3d	7d19bfe5-daf8-4879-8f88-7fcc9bff34c5	COMPLETED	2026-11-05 12:48:59.999611+00	2026-11-05 12:48:59.999611+00	2026-11-06 06:30:00.002511+00	SD-2LYW-UZFP	131 Stanley Thilakaratne Mawatha, Nugegoda	161 Prince of Wales Avenue, Colombo 14	Sunil de Silva	SMALL	4.68	DELIVERED
744c45ce-c57b-4823-a717-dd8ab3b93b34	52151e16-e672-4561-b12d-c46672e6f087	03706d2d-c424-4359-8d98-702189d2a120	7d19bfe5-daf8-4879-8f88-7fcc9bff34c5	COMPLETED	2026-11-06 05:27:00.000436+00	2026-11-06 05:27:00.000436+00	2026-11-07 06:33:00.00319+00	SD-X6RW-ES8Q	143 Lauries Road, Colombo 04	91 Marine Drive, Colombo 06	Hashini Peiris	MEDIUM	3.57	DELIVERED
a229d888-6f8e-4f04-bb71-bd41b544cbf1	e97f667d-9729-449d-95af-5ef50c985742	2afe8e87-d5cd-41ea-99b4-db04a7f48d25	49c139c8-8007-4199-80b6-e68a928a59fb	FAILED	2026-11-06 07:10:59.99977+00	2026-11-06 07:10:59.99977+00	2026-11-07 08:22:00.003645+00	SD-WFFV-EJEG	152 Kotte Road, Kotte	162 Ramakrishna Road, Colombo 06	Gayani Karunaratne	SMALL	4.25	FAILED
5a3e2a50-6c24-4bd9-9dab-5a7801edab43	71162e7d-384f-4fb6-8a6d-47814433723d	2a4e4e19-00b3-4d93-b3f5-185ca3701d3d	7d19bfe5-daf8-4879-8f88-7fcc9bff34c5	COMPLETED	2026-11-06 07:54:00.000394+00	2026-11-06 07:54:00.000394+00	2026-11-07 05:00:00.003558+00	SD-Z7EK-QL5N	76 Ward Place, Colombo 07	292 Nawala Road, Rajagiriya	Ishara Samarasinghe	MEDIUM	9.25	DELIVERED
245c1539-5e99-4236-8e58-daccfa83d46e	904cb740-6727-435f-9a13-dcf71cb2d027	2a4e4e19-00b3-4d93-b3f5-185ca3701d3d	80d23eb8-1156-49dc-b5c3-42cdedfab75d	COMPLETED	2026-11-06 09:51:00.000105+00	2026-11-06 09:51:00.000105+00	2026-11-07 07:18:00.005695+00	SD-Y2D3-AZY4	51 Thimbirigasyaya Road, Colombo 05	139 York Street, Colombo 01	Ayesha Mendis	SMALL	2.83	DELIVERED
6bcc0524-1a29-4895-8318-341b5496cf91	25ae8c7d-cdbc-4a36-b249-7b48bd9564d2	5ba3a5c8-4911-45c9-9155-84e05b5dae36	80d23eb8-1156-49dc-b5c3-42cdedfab75d	COMPLETED	2026-11-06 11:27:59.999653+00	2026-11-06 11:27:59.999653+00	2026-11-07 06:56:00.003028+00	SD-MEQK-X6DQ	30 Prince of Wales Avenue, Colombo 14	95 Union Place, Colombo 02	Isuru Rathnayake	LARGE	11.80	DELIVERED
324ee24a-b711-48f7-a994-1492ee57187d	11ac596c-c1c7-4f17-be17-684ace945181	ff655d35-a0cb-4ec7-a11c-25e7cb868ddf	7d19bfe5-daf8-4879-8f88-7fcc9bff34c5	COMPLETED	2026-11-06 10:44:00.000232+00	2026-11-06 10:44:00.000232+00	2026-11-07 05:02:00.002778+00	SD-JUBQ-KAAS	181 Lauries Road, Colombo 04	178 Main Street, Negombo	Fathima Wijesinghe	SMALL	1.94	DELIVERED
c821b664-7312-4419-9ffe-08d41933e4b0	300eb92e-1e21-486b-8b40-8bd07d41c65b	ff655d35-a0cb-4ec7-a11c-25e7cb868ddf	7d19bfe5-daf8-4879-8f88-7fcc9bff34c5	COMPLETED	2026-11-06 11:58:00.00024+00	2026-11-06 11:58:00.00024+00	2026-11-07 07:06:00.002301+00	SD-B7K4-F3HY	296 Hill Street, Dehiwala	182 Thimbirigasyaya Road, Colombo 05	Lakshmi Rathnayake	MEDIUM	4.00	DELIVERED
49a0d114-8398-4a52-962b-8e20c9c36a92	a5b7d466-bdcd-4d0f-8f6e-9449508e0a80	0692e9c1-ff92-4d13-ae68-a7c2168947c3	7d19bfe5-daf8-4879-8f88-7fcc9bff34c5	COMPLETED	2026-11-06 14:01:59.999553+00	2026-11-06 14:01:59.999553+00	2026-11-07 07:28:00.002151+00	SD-NSYM-YTEJ	78 Station Road, Nugegoda	116 Galle Road, Dehiwala	Mohamed Samarasinghe	LARGE	6.90	DELIVERED
4add9a40-7f67-42db-9cbe-200a5733f320	f19c6e7e-d7ff-4203-9d05-9167441a924e	ff655d35-a0cb-4ec7-a11c-25e7cb868ddf	49c139c8-8007-4199-80b6-e68a928a59fb	COMPLETED	2026-11-07 05:25:59.999533+00	2026-11-07 05:25:59.999533+00	2026-11-08 05:24:00.002597+00	SD-W7Q5-4SN8	204 Galle Road, Dehiwala	236 Maradana Road, Colombo 10	Dimuth Samarasinghe	SMALL	3.22	DELIVERED
ff86c1d6-b0e2-426d-a472-72362b628343	35f1a41d-53aa-4d14-bcf7-a13b8cefaec3	16d13c48-6ea3-48b5-b0b5-fbe7fcefec4d	49c139c8-8007-4199-80b6-e68a928a59fb	COMPLETED	2026-11-07 06:54:00.000168+00	2026-11-07 06:54:00.000168+00	2026-11-08 05:48:00.002231+00	SD-HPWH-LJDA	45 Chatham Street, Colombo 01	199 Peradeniya Road, Kandy	Dilani Rahman	EXTRA_LARGE	28.36	DELIVERED
5329dbde-2dd6-46e7-8cd1-4ff21a1f3e1b	e877b4d4-c857-4b51-8f1f-6bc1b2533665	16d13c48-6ea3-48b5-b0b5-fbe7fcefec4d	49c139c8-8007-4199-80b6-e68a928a59fb	COMPLETED	2026-11-07 08:42:59.99997+00	2026-11-07 08:42:59.99997+00	2026-11-08 06:22:00.002836+00	SD-C5FZ-NQ5C	297 Baseline Road, Colombo 09	68 Flower Road, Colombo 07	Lahiru Silva	MEDIUM	8.89	DELIVERED
11fa5ad8-ff5e-4e12-9711-38289c4fd43d	fd0dd3b7-cbe0-4e72-adb8-34a9ea7b0ec7	f8264489-f505-49cf-817d-d4373523e808	80d23eb8-1156-49dc-b5c3-42cdedfab75d	COMPLETED	2026-11-07 10:32:59.999928+00	2026-11-07 10:32:59.999928+00	2026-11-08 07:31:00.002882+00	SD-387V-QMNP	7 Hill Street, Dehiwala	26 York Street, Colombo 01	Dilani Mendis	SMALL	1.39	DELIVERED
3a7d773b-84f9-431a-a6e6-a141612cf129	0f1e5377-5165-4f6f-a34a-adc6a8784ba1	1cda4767-b26a-4a9e-aa6d-d52cf46c2dc3	7d19bfe5-daf8-4879-8f88-7fcc9bff34c5	COMPLETED	2026-11-07 14:41:00.000264+00	2026-11-07 14:41:00.000264+00	2026-11-07 23:01:00.003042+00	SD-ET3Y-JA5E	137 High Level Road, Maharagama	230 Prince of Wales Avenue, Colombo 14	Mohamed Abeysekera	MEDIUM	4.07	DELIVERED
42d61479-0cef-475b-9062-9a8743a5cbf3	cfbcb587-f351-449a-b763-6dcd37bbaf95	1cda4767-b26a-4a9e-aa6d-d52cf46c2dc3	7d19bfe5-daf8-4879-8f88-7fcc9bff34c5	FAILED	2026-11-08 03:53:59.999854+00	2026-11-08 03:53:59.999855+00	2026-11-08 12:06:00.002274+00	SD-E527-VGNN	36 Old Kesbewa Road, Nugegoda	209 Matara Road, Galle	Oshada Perera	LARGE	16.74	FAILED
821eb0ea-3a95-4326-b4a5-12c49c9938b1	5340fc77-d353-485d-a723-3e0e54f4a941	f8264489-f505-49cf-817d-d4373523e808	7d19bfe5-daf8-4879-8f88-7fcc9bff34c5	COMPLETED	2026-11-08 07:55:59.999562+00	2026-11-08 07:55:59.999562+00	2026-11-09 03:43:00.001801+00	SD-JQVF-7ED6	74 Baseline Road, Colombo 09	68 Main Street, Colombo 11	Kasun Jayasinghe	MEDIUM	6.72	DELIVERED
85af3ae4-9288-458e-8256-4eb50a981455	0fdc2e71-d520-4322-b247-fac921051c0e	f8264489-f505-49cf-817d-d4373523e808	49c139c8-8007-4199-80b6-e68a928a59fb	COMPLETED	2026-11-08 09:03:00.000281+00	2026-11-08 09:03:00.000281+00	2026-11-09 06:59:00.002908+00	SD-S2V2-QUCZ	223 Thimbirigasyaya Road, Colombo 05	113 Galle Road, Dehiwala	Kamal Samarasinghe	LARGE	13.98	DELIVERED
aa422e43-0be6-45f7-97d2-818e8017d472	4ad40d3e-fb78-4f6a-8e3f-e5c27a792975	1cda4767-b26a-4a9e-aa6d-d52cf46c2dc3	80d23eb8-1156-49dc-b5c3-42cdedfab75d	COMPLETED	2026-11-08 12:56:59.999505+00	2026-11-08 12:56:59.999505+00	2026-11-09 04:28:00.002561+00	SD-JZWL-4KWW	272 Parliament Road, Rajagiriya	191 Kotte Road, Kotte	Nadeesha Karunaratne	SMALL	0.11	DELIVERED
4a8bf17f-2dcb-4ce2-95e2-1ceeabbfe519	d53d153c-8545-4d5f-990c-b213d445f704	2a4e4e19-00b3-4d93-b3f5-185ca3701d3d	80d23eb8-1156-49dc-b5c3-42cdedfab75d	COMPLETED	2026-11-09 06:32:00.000306+00	2026-11-09 06:32:00.000306+00	2026-11-10 05:08:00.002816+00	SD-6BYN-2Y86	126 Chatham Street, Colombo 01	163 Pita Kotte Road, Kotte	Tharushi Gunasekara	MEDIUM	6.95	DELIVERED
0d23581c-8fd0-4e76-8249-a151d37689a6	2d8605d8-95a4-4349-80a6-c16f121a9d4e	f8264489-f505-49cf-817d-d4373523e808	7d19bfe5-daf8-4879-8f88-7fcc9bff34c5	COMPLETED	2026-11-09 07:50:59.999792+00	2026-11-09 07:50:59.999792+00	2026-11-10 04:33:00.005366+00	SD-6VWA-SA6Q	283 Pita Kotte Road, Kotte	233 Chatham Street, Colombo 01	Anjali Senanayake	SMALL	0.22	DELIVERED
cc3f79c0-dfad-417b-8a30-0ba43edc3809	41d14aef-9ad0-4f90-9ce4-8f06b6fe6c3a	1cda4767-b26a-4a9e-aa6d-d52cf46c2dc3	80d23eb8-1156-49dc-b5c3-42cdedfab75d	COMPLETED	2026-11-09 11:14:00.000455+00	2026-11-09 11:14:00.000455+00	2026-11-10 08:07:00.002147+00	SD-LTE8-VFUK	339 Lauries Road, Colombo 04	155 Wakwella Road, Galle	Ayesha Dissanayake	LARGE	11.75	DELIVERED
11305675-a592-4a67-a268-7fea9b01e0e7	3ce88e20-01e5-4511-a0ac-c0b1367b7bd0	1cda4767-b26a-4a9e-aa6d-d52cf46c2dc3	7d19bfe5-daf8-4879-8f88-7fcc9bff34c5	COMPLETED	2026-11-09 12:23:59.999513+00	2026-11-09 12:23:59.999514+00	2026-11-10 05:11:00.00256+00	SD-8GUV-7WW5	338 Stanley Thilakaratne Mawatha, Nugegoda	271 High Level Road, Maharagama	Pradeep Herath	SMALL	3.00	DELIVERED
42d621d5-4f2a-41d6-80f8-9142f2a78c38	adfaebfb-b0b9-4e4b-bc1c-d0d4b267978c	0692e9c1-ff92-4d13-ae68-a7c2168947c3	49c139c8-8007-4199-80b6-e68a928a59fb	COMPLETED	2026-11-09 11:58:00.000227+00	2026-11-09 11:58:00.000227+00	2026-11-10 05:36:00.003477+00	SD-CDHK-EQL8	189 Kollupitiya Road, Colombo 03	230 Hill Street, Dehiwala	Ruwan Bandara	MEDIUM	5.30	DELIVERED
320e44da-9982-4d41-b92d-a8d99d520d47	1fb2ba0a-fb40-4d94-b5e9-b50dd6b02444	f8264489-f505-49cf-817d-d4373523e808	7d19bfe5-daf8-4879-8f88-7fcc9bff34c5	COMPLETED	2026-11-10 03:42:59.999528+00	2026-11-10 03:42:59.999529+00	2026-11-11 05:12:00.003266+00	SD-KEYE-2MKC	255 Kotte Road, Kotte	333 Wakwella Road, Galle	Rizwan Liyanage	LARGE	8.69	DELIVERED
eea039c9-bfa6-41d0-9711-18647709c608	df106740-601f-492b-a961-fa7a1b28df66	16d13c48-6ea3-48b5-b0b5-fbe7fcefec4d	49c139c8-8007-4199-80b6-e68a928a59fb	COMPLETED	2026-11-10 04:32:00.000134+00	2026-11-10 04:32:00.000134+00	2026-11-11 05:46:00.004011+00	SD-8M7R-4SDS	102 Pita Kotte Road, Kotte	196 De Saram Road, Mount Lavinia	Tharushi Rathnayake	MEDIUM	6.76	DELIVERED
26a8f3f1-d009-425d-92d4-76611c39d062	36a19647-4851-44a2-9908-6136ce79cb57	0692e9c1-ff92-4d13-ae68-a7c2168947c3	49c139c8-8007-4199-80b6-e68a928a59fb	COMPLETED	2026-11-10 06:23:59.999695+00	2026-11-10 06:23:59.999695+00	2026-11-11 05:45:00.002554+00	SD-C4Z7-NYFX	270 Duplication Road, Colombo 03	201 Main Street, Negombo	Ravindu Rajendran	MEDIUM	2.73	DELIVERED
ef0ad32d-adcb-472c-a56a-126f399322cf	16e4ae12-dcbd-4f7a-867f-bcee9ab899df	ff655d35-a0cb-4ec7-a11c-25e7cb868ddf	49c139c8-8007-4199-80b6-e68a928a59fb	COMPLETED	2026-11-10 06:42:00.000171+00	2026-11-10 06:42:00.000171+00	2026-11-10 12:28:00.002762+00	SD-3XLY-Z37Y	253 Hotel Road, Mount Lavinia	112 Stanley Thilakaratne Mawatha, Nugegoda	Sajith Amarasekara	LARGE	10.00	DELIVERED
79a2b6ca-c31b-4592-8888-e648893c93a0	8f747046-f991-4496-895b-9afb7fb5f590	16d13c48-6ea3-48b5-b0b5-fbe7fcefec4d	49c139c8-8007-4199-80b6-e68a928a59fb	COMPLETED	2026-11-10 09:50:00.000052+00	2026-11-10 09:50:00.000052+00	2026-11-11 05:03:00.002387+00	SD-9ZAW-HGXY	109 Chatham Street, Colombo 01	49 Hotel Road, Mount Lavinia	Methmi Karunaratne	SMALL	2.72	DELIVERED
25306103-e929-44e0-a9c1-5f0b14d10644	a48f602c-0f5e-4c03-9890-f45902d8771a	2afe8e87-d5cd-41ea-99b4-db04a7f48d25	7d19bfe5-daf8-4879-8f88-7fcc9bff34c5	COMPLETED	2026-11-10 11:16:59.999835+00	2026-11-10 11:16:59.999835+00	2026-11-11 06:55:00.002196+00	SD-DESB-MFNC	15 William Gopallawa Mawatha, Kandy	17 Union Place, Colombo 02	Mohamed Mendis	SMALL	4.83	DELIVERED
5bbf2fe9-542a-4826-a4f5-2050b0d83fa4	9e023210-0858-4e7f-a17a-bc39f6755fd8	ff655d35-a0cb-4ec7-a11c-25e7cb868ddf	7d19bfe5-daf8-4879-8f88-7fcc9bff34c5	COMPLETED	2026-11-10 12:09:00.000098+00	2026-11-10 12:09:00.000098+00	2026-11-11 04:40:00.002396+00	SD-6RUA-L9T2	60 Hill Street, Dehiwala	223 Hill Street, Dehiwala	Priya Fernando	MEDIUM	4.87	DELIVERED
ca866be9-1d84-41c4-9008-bf74d2e4ed14	a2404812-1530-4280-8a4e-abea42c42f6e	16d13c48-6ea3-48b5-b0b5-fbe7fcefec4d	7d19bfe5-daf8-4879-8f88-7fcc9bff34c5	COMPLETED	2026-11-10 12:24:59.999533+00	2026-11-10 12:24:59.999533+00	2026-11-11 07:14:00.002375+00	SD-K7U8-MCPX	88 Galle Road, Dehiwala	235 Main Street, Colombo 11	Sachini Rahman	SMALL	1.53	DELIVERED
f93458d6-398a-4af8-893b-3c0d127614ea	fb0dc0ab-2a73-4637-82fc-1e5026adeb82	2a4e4e19-00b3-4d93-b3f5-185ca3701d3d	80d23eb8-1156-49dc-b5c3-42cdedfab75d	COMPLETED	2026-11-10 12:31:59.999808+00	2026-11-10 12:31:59.999808+00	2026-11-11 04:37:00.002006+00	SD-TGE6-HTLG	206 Kotte Road, Kotte	244 Maradana Road, Colombo 10	Pradeep Karunaratne	LARGE	11.67	DELIVERED
9b4bc778-9d64-4b5a-a9f0-6f75932692ee	72c13ca4-8622-4d4d-ae96-854a1e720a49	2afe8e87-d5cd-41ea-99b4-db04a7f48d25	49c139c8-8007-4199-80b6-e68a928a59fb	COMPLETED	2026-11-11 03:50:59.999948+00	2026-11-11 03:50:59.999948+00	2026-11-12 03:54:00.002795+00	SD-HCFV-WJ4P	4 Hotel Road, Mount Lavinia	52 Chatham Street, Colombo 01	Arun Peiris	LARGE	13.61	DELIVERED
1c89a980-ea29-4dbf-8fa7-5a7111aaf845	f6fc7f4e-de6f-444e-9498-bab20740131b	03706d2d-c424-4359-8d98-702189d2a120	49c139c8-8007-4199-80b6-e68a928a59fb	COMPLETED	2026-11-11 04:22:00.000403+00	2026-11-11 04:22:00.000403+00	2026-11-12 06:55:00.002572+00	SD-C2U4-Y78D	217 Station Road, Nugegoda	139 Chatham Street, Colombo 01	Gayani Ratnayake	LARGE	5.71	DELIVERED
5434bd3d-3a2d-41f7-9684-c4880efd764e	4f9ebf48-bbf6-4d2d-93b9-7b5e410f0482	03706d2d-c424-4359-8d98-702189d2a120	80d23eb8-1156-49dc-b5c3-42cdedfab75d	FAILED	2026-11-11 04:57:00.000407+00	2026-11-11 04:57:00.000407+00	2026-11-12 06:44:00.002426+00	SD-A7PB-VEMM	92 Parliament Road, Rajagiriya	249 Kotte Road, Kotte	Ayesha Abeysekera	SMALL	4.42	FAILED
8c557cad-9302-4b8f-803c-2168426ae741	b2ba5dfd-6ecd-443e-8145-abbd5ab0486e	2a4e4e19-00b3-4d93-b3f5-185ca3701d3d	80d23eb8-1156-49dc-b5c3-42cdedfab75d	COMPLETED	2026-11-11 04:55:00.000144+00	2026-11-11 04:55:00.000145+00	2026-11-11 12:44:00.002602+00	SD-3GCV-T4RH	236 Galle Road, Colombo 04	230 Main Street, Negombo	Kamal Jayawardena	SMALL	4.05	DELIVERED
a253d578-e21b-4029-9974-d15746f03bc8	9ed1070a-8df0-48fd-a24a-e556a861650c	16d13c48-6ea3-48b5-b0b5-fbe7fcefec4d	7d19bfe5-daf8-4879-8f88-7fcc9bff34c5	COMPLETED	2026-11-11 08:02:59.999622+00	2026-11-11 08:02:59.999622+00	2026-11-12 05:23:00.00249+00	SD-HMXC-T8XS	286 Maradana Road, Colombo 10	35 Hill Street, Dehiwala	Rizwan Senanayake	MEDIUM	9.42	DELIVERED
6f2717ea-85c4-4bea-8541-223d42bb0497	3fca0d50-d690-4979-b6d5-dc2df3e8379a	2afe8e87-d5cd-41ea-99b4-db04a7f48d25	7d19bfe5-daf8-4879-8f88-7fcc9bff34c5	COMPLETED	2026-11-11 09:28:00.000201+00	2026-11-11 09:28:00.000201+00	2026-11-12 08:13:00.001886+00	SD-JPL4-T7JZ	192 Pita Kotte Road, Kotte	189 Chatham Street, Colombo 01	Sunil Peiris	SMALL	4.69	DELIVERED
3f767242-8ca5-4bd1-94db-e1ab9ba903a6	8483dcec-f2cb-4114-892b-6cfe79fac714	16d13c48-6ea3-48b5-b0b5-fbe7fcefec4d	7d19bfe5-daf8-4879-8f88-7fcc9bff34c5	FAILED	2026-11-11 12:08:59.999562+00	2026-11-11 12:08:59.999562+00	2026-11-12 07:05:00.002374+00	SD-RVH6-Y5X9	29 High Level Road, Maharagama	83 Flower Road, Colombo 07	Tharindu Peiris	MEDIUM	7.11	FAILED
aeae20c8-eab6-4fb0-9f43-7e401319bc8f	33bbd2ea-d17b-4fba-b7fa-1b301c4b7b6f	2a4e4e19-00b3-4d93-b3f5-185ca3701d3d	49c139c8-8007-4199-80b6-e68a928a59fb	COMPLETED	2026-11-11 12:10:00.000056+00	2026-11-11 12:10:00.000056+00	2026-11-12 06:34:00.001902+00	SD-9KH5-KN3R	90 Chatham Street, Colombo 01	10 Stanley Thilakaratne Mawatha, Nugegoda	Dulani Rajapaksa	SMALL	0.62	DELIVERED
a10ca3ec-102f-4a2b-ab49-0f157c72d310	361a9949-891a-4213-b73f-54233de9eb58	5ba3a5c8-4911-45c9-9155-84e05b5dae36	7d19bfe5-daf8-4879-8f88-7fcc9bff34c5	FAILED	2026-11-11 12:49:00.000373+00	2026-11-11 12:49:00.000373+00	2026-11-12 06:33:00.00211+00	SD-JQXD-AER8	138 Union Place, Colombo 02	307 Duplication Road, Colombo 03	Methmi Abeysekera	SMALL	2.11	FAILED
1b2b9ec4-e2ce-4abc-8f12-4291d6661805	b0810285-192c-48f7-b40b-4ab5a8f9a07c	0692e9c1-ff92-4d13-ae68-a7c2168947c3	49c139c8-8007-4199-80b6-e68a928a59fb	COMPLETED	2026-11-12 04:33:00.000421+00	2026-11-12 04:33:00.000421+00	2026-11-12 10:19:00.002205+00	SD-7EZ4-YVPN	223 Havelock Road, Colombo 05	91 Station Road, Nugegoda	Chathura Weerasinghe	LARGE	12.44	DELIVERED
b9993c3c-c688-4293-ad38-e84b414b3ab2	1ec71d67-4c00-48b3-81a1-f8a8d5be9a39	1cda4767-b26a-4a9e-aa6d-d52cf46c2dc3	49c139c8-8007-4199-80b6-e68a928a59fb	COMPLETED	2026-11-12 04:39:59.999916+00	2026-11-12 04:39:59.999916+00	2026-11-13 05:38:00.002146+00	SD-NMAV-EWRY	302 Ramakrishna Road, Colombo 06	282 Dalada Veediya, Kandy	Arun Abeysekera	MEDIUM	4.71	DELIVERED
8b67ad31-556f-41ac-9156-fa9073f02264	cf174afb-50dd-4095-92f2-929b743d0070	16d13c48-6ea3-48b5-b0b5-fbe7fcefec4d	49c139c8-8007-4199-80b6-e68a928a59fb	COMPLETED	2026-11-12 06:18:59.999689+00	2026-11-12 06:18:59.999689+00	2026-11-13 07:09:00.002304+00	SD-UEXP-TMCA	333 Old Kesbewa Road, Nugegoda	277 Lauries Road, Colombo 04	Ruwan Gunasekara	LARGE	10.24	DELIVERED
3257c2fe-f334-443f-8061-84daa269f35d	02ad924c-726e-4d53-8ae4-66d49901850e	2a4e4e19-00b3-4d93-b3f5-185ca3701d3d	80d23eb8-1156-49dc-b5c3-42cdedfab75d	COMPLETED	2026-11-12 09:41:59.999749+00	2026-11-12 09:41:59.999749+00	2026-11-13 05:47:00.00276+00	SD-M5TJ-C8BD	234 Main Street, Colombo 11	10 Pita Kotte Road, Kotte	Ruwan Silva	LARGE	16.03	DELIVERED
0b0070e2-b653-4d01-9b6a-1cdd7d281410	d020723c-fa44-4817-bedf-9e7397843a43	0692e9c1-ff92-4d13-ae68-a7c2168947c3	80d23eb8-1156-49dc-b5c3-42cdedfab75d	COMPLETED	2026-11-12 08:41:00.00016+00	2026-11-12 08:41:00.00016+00	2026-11-13 04:22:00.002445+00	SD-2SNS-L4GC	45 De Saram Road, Mount Lavinia	237 William Gopallawa Mawatha, Kandy	Kasun Wijesinghe	EXTRA_LARGE	17.33	DELIVERED
01dee7a5-8c2f-44e8-a588-1bba0a3d5186	85116c1f-f955-4fed-9d0b-651cd4fc8492	f8264489-f505-49cf-817d-d4373523e808	7d19bfe5-daf8-4879-8f88-7fcc9bff34c5	COMPLETED	2026-11-12 14:36:00.000375+00	2026-11-12 14:36:00.000375+00	2026-11-13 05:34:00.002438+00	SD-H5JE-BXE5	153 Ramakrishna Road, Colombo 06	106 Church Street, Galle Fort	Gayani Weerasinghe	MEDIUM	3.36	DELIVERED
0b392f10-6bcd-41f2-a5e9-d66f184e1dbe	a2e47ec4-a465-4450-aa6e-b66e664327ce	2afe8e87-d5cd-41ea-99b4-db04a7f48d25	49c139c8-8007-4199-80b6-e68a928a59fb	COMPLETED	2026-11-13 03:51:59.999893+00	2026-11-13 03:51:59.999893+00	2026-11-14 06:49:00.00486+00	SD-ST7M-EGRD	55 Pita Kotte Road, Kotte	304 York Street, Colombo 01	Nadeesha Ratnayake	SMALL	0.85	DELIVERED
9ee0c750-5d11-4ca1-8995-ea1818f55209	32bf0def-f60f-4371-bb8e-1e1b736e61bf	0692e9c1-ff92-4d13-ae68-a7c2168947c3	7d19bfe5-daf8-4879-8f88-7fcc9bff34c5	COMPLETED	2026-11-13 07:14:00.000043+00	2026-11-13 07:14:00.000043+00	2026-11-14 03:45:00.00204+00	SD-2XYX-X7LL	274 Kotte Road, Kotte	117 Union Place, Colombo 02	Chathura Fonseka	LARGE	10.38	DELIVERED
e1a50b90-1098-4dea-a646-1daeee4b84bd	05240ab4-479d-432e-bf21-356174a68daf	2a4e4e19-00b3-4d93-b3f5-185ca3701d3d	80d23eb8-1156-49dc-b5c3-42cdedfab75d	COMPLETED	2026-11-13 06:50:00.000171+00	2026-11-13 06:50:00.000171+00	2026-11-14 04:54:00.001953+00	SD-KDBC-K656	4 Hill Street, Dehiwala	253 Old Kesbewa Road, Nugegoda	Sachini Perera	SMALL	4.22	DELIVERED
b9e69f4b-8c59-4506-9c4d-3d33aa2b400b	8edf3ec5-f0a1-415a-aba5-f13bf14782cc	2afe8e87-d5cd-41ea-99b4-db04a7f48d25	49c139c8-8007-4199-80b6-e68a928a59fb	COMPLETED	2026-11-13 09:44:59.999615+00	2026-11-13 09:44:59.999615+00	2026-11-14 08:28:00.00255+00	SD-793G-BBQ3	336 York Street, Colombo 01	215 High Level Road, Maharagama	Ravindu Mendis	SMALL	4.98	DELIVERED
c62587e9-5440-4fdc-926a-8d7f8f74b823	797a642c-241e-4eb4-8936-2442618c6c6b	03706d2d-c424-4359-8d98-702189d2a120	80d23eb8-1156-49dc-b5c3-42cdedfab75d	FAILED	2026-11-13 12:08:00.000452+00	2026-11-13 12:08:00.000452+00	2026-11-14 03:39:00.001593+00	SD-E7ND-9EBS	146 Kollupitiya Road, Colombo 03	291 Nawala Road, Rajagiriya	Lakshmi Samarasinghe	MEDIUM	8.97	FAILED
08f343e8-9522-40ce-8cd4-6a90e2354f5e	a20071ea-5db0-4732-ac9e-11e2ca87e48f	2afe8e87-d5cd-41ea-99b4-db04a7f48d25	49c139c8-8007-4199-80b6-e68a928a59fb	COMPLETED	2026-11-13 14:52:59.999825+00	2026-11-13 14:52:59.999825+00	2026-11-14 05:37:00.002745+00	SD-NDZX-AHL4	284 Pita Kotte Road, Kotte	273 Parliament Road, Rajagiriya	Mohamed Silva	SMALL	4.93	DELIVERED
7135d6ca-cd53-449d-8f30-4c64f7797e40	0b3fa768-278b-4389-93a2-55d1e55f5a1c	16d13c48-6ea3-48b5-b0b5-fbe7fcefec4d	80d23eb8-1156-49dc-b5c3-42cdedfab75d	COMPLETED	2026-11-14 03:52:59.999861+00	2026-11-14 03:52:59.999861+00	2026-11-14 09:39:00.002717+00	SD-RHCL-8G4H	168 Wakwella Road, Galle	315 Chatham Street, Colombo 01	Isuru Rajapaksa	SMALL	4.93	DELIVERED
4f1f8028-778d-47b0-aaf2-7478cacc9de7	1fa0e3f5-27bb-4e6c-9264-c91a22f60485	2a4e4e19-00b3-4d93-b3f5-185ca3701d3d	80d23eb8-1156-49dc-b5c3-42cdedfab75d	IN_PROGRESS	2026-11-14 05:00:59.999725+00	2026-11-14 05:00:59.999725+00	2026-11-14 09:29:00.002069+00	SD-VT99-RX2K	203 Main Street, Colombo 11	11 Hill Street, Dehiwala	Yasiru Jayawardena	LARGE	15.27	IN_TRANSIT
e94062ca-c866-4682-a5bf-ed2a76fdb151	fb64489c-0bc2-436f-a8a0-a7ccd6adf212	f8264489-f505-49cf-817d-d4373523e808	7d19bfe5-daf8-4879-8f88-7fcc9bff34c5	IN_PROGRESS	2026-11-14 06:16:00.000252+00	2026-11-14 06:16:00.000252+00	2026-11-14 08:55:00.001998+00	SD-4HEF-C9LZ	216 Prince of Wales Avenue, Colombo 14	139 Pita Kotte Road, Kotte	Rizwan Peiris	MEDIUM	4.26	IN_TRANSIT
49db35a7-f6d7-448e-b75d-e53b05b8c955	e8dd67c5-80ff-4fe0-8953-94f5114cae44	16d13c48-6ea3-48b5-b0b5-fbe7fcefec4d	80d23eb8-1156-49dc-b5c3-42cdedfab75d	IN_PROGRESS	2026-11-14 07:04:08.999653+00	2026-11-14 07:04:08.999653+00	2026-11-14 09:58:09.002375+00	SD-2REW-RV3K	35 Hill Street, Dehiwala	189 Station Road, Nugegoda	Sahan Fonseka	EXTRA_LARGE	29.80	PICKED_UP
48f860f3-36d1-4104-9bb4-01d69ce664c0	d35bd31e-50ff-4a6d-8c8d-e90deac843ef	03706d2d-c424-4359-8d98-702189d2a120	49c139c8-8007-4199-80b6-e68a928a59fb	IN_PROGRESS	2026-11-14 06:45:08.999525+00	2026-11-14 06:45:08.999525+00	2026-11-14 08:12:09.001898+00	SD-BQQT-GRMW	222 Nawala Road, Rajagiriya	106 Dalada Veediya, Kandy	Priya Nawaz	SMALL	1.96	PICKED_UP
58eb5969-3fd9-44aa-9226-706b33e01579	2997d24e-c3c4-44fe-b24b-1395a618f278	03706d2d-c424-4359-8d98-702189d2a120	80d23eb8-1156-49dc-b5c3-42cdedfab75d	IN_PROGRESS	2026-11-14 08:17:59.999577+00	2026-11-14 08:17:59.999577+00	2026-11-14 09:10:00.002331+00	SD-KPVM-WYR5	251 Marine Drive, Colombo 06	25 Galle Road, Colombo 03	Sahan Weerasinghe	LARGE	8.31	PICKED_UP
7bdf894b-13a2-4689-a416-e8af23dd316c	7d5338fb-9cf1-40ae-8e00-6e345a25a18e	5ba3a5c8-4911-45c9-9155-84e05b5dae36	80d23eb8-1156-49dc-b5c3-42cdedfab75d	IN_PROGRESS	2026-11-14 07:51:08.999936+00	2026-11-14 07:51:08.999936+00	2026-11-14 09:29:09.002686+00	SD-K9WD-7VKZ	107 Hill Street, Dehiwala	339 De Saram Road, Mount Lavinia	Sahan Fernando	SMALL	4.23	PICKED_UP
de5d3ab9-4e6d-41a6-96ee-6508463f403d	1ca7da93-e28f-473c-b5a8-7c30a05d5571	2a4e4e19-00b3-4d93-b3f5-185ca3701d3d	49c139c8-8007-4199-80b6-e68a928a59fb	ASSIGNED	2026-11-14 08:28:09.000406+00	2026-11-14 08:28:09.000406+00	2026-11-14 06:58:22.662406+00	SD-CTDT-YMZJ	321 Station Road, Nugegoda	19 Nawala Road, Rajagiriya	Malith Fernando	SMALL	1.67	CREATED
f2498c89-805b-4402-9828-32fbcebd836c	2fbdcb84-06fd-49a2-aec9-6a631b6eab06	16d13c48-6ea3-48b5-b0b5-fbe7fcefec4d	80d23eb8-1156-49dc-b5c3-42cdedfab75d	ASSIGNED	2026-11-14 09:11:08.999895+00	2026-11-14 09:11:08.999895+00	2026-11-14 07:22:22.657895+00	SD-9NYG-N9YW	327 Galle Road, Dehiwala	321 Prince of Wales Avenue, Colombo 14	Hiruni Karunaratne	SMALL	1.88	CREATED
f334d885-a3ab-42e4-ac33-22b3b917697b	43d0acf8-4e56-4f44-9504-df162cf768ef	2a4e4e19-00b3-4d93-b3f5-185ca3701d3d	80d23eb8-1156-49dc-b5c3-42cdedfab75d	ASSIGNED	2026-11-14 09:14:09.000397+00	2026-11-14 09:14:09.000397+00	2026-11-14 07:30:22.653397+00	SD-YCRE-YG7C	63 Baseline Road, Colombo 09	18 Dalada Veediya, Kandy	Kasun Jayawardena	MEDIUM	7.12	CREATED
7415bb25-7740-4df9-afa7-c810668342b1	23f0781e-d399-407b-90e3-26f320ef74e5	03706d2d-c424-4359-8d98-702189d2a120	7d19bfe5-daf8-4879-8f88-7fcc9bff34c5	ASSIGNED	2026-11-14 09:57:59.999569+00	2026-11-14 09:57:59.999569+00	2026-11-14 08:08:13.648569+00	SD-44HZ-PVJQ	171 Galle Road, Colombo 04	324 Old Kesbewa Road, Nugegoda	Arun Karunaratne	MEDIUM	9.31	CREATED
6296c836-bff5-438e-97f8-e3910f517b20	4dfada0a-ebc0-4f19-b7c8-10a3f3a92777	ff655d35-a0cb-4ec7-a11c-25e7cb868ddf	80d23eb8-1156-49dc-b5c3-42cdedfab75d	ASSIGNED	2026-11-14 09:04:08.999943+00	2026-11-14 09:04:08.999943+00	2026-11-14 08:18:22.642943+00	SD-P2CQ-NG9H	146 Havelock Road, Colombo 05	167 Union Place, Colombo 02	Sunil Silva	SMALL	4.62	CREATED
f0bea048-51e2-4b1d-be95-833f8c8951b2	7f75eaff-12db-4955-abe1-9fdff905915b	2a4e4e19-00b3-4d93-b3f5-185ca3701d3d	80d23eb8-1156-49dc-b5c3-42cdedfab75d	IN_PROGRESS	2026-11-14 08:54:09.000021+00	2026-11-14 08:54:09.000021+00	2026-11-14 09:54:09.002009+00	SD-RA9K-6926	141 Parliament Road, Rajagiriya	270 Union Place, Colombo 02	Dulani Nawaz	MEDIUM	6.73	PICKED_UP
f7ec844c-06ad-4d61-9a85-a1291c673d38	0bec12f3-8e01-4188-b4a1-6b0b229c1bc0	2a4e4e19-00b3-4d93-b3f5-185ca3701d3d	49c139c8-8007-4199-80b6-e68a928a59fb	ASSIGNED	2026-11-14 09:25:00.000063+00	2026-11-14 09:25:00.000063+00	2026-11-14 08:40:13.642063+00	SD-3QRL-BEL2	330 Pita Kotte Road, Kotte	61 York Street, Colombo 01	Dimuth Rajapaksa	LARGE	17.24	CREATED
b2f84dd6-884d-406d-aeb1-d1af6969a0e7	508ea919-3824-42cc-bc62-d111f44fc752	16d13c48-6ea3-48b5-b0b5-fbe7fcefec4d	49c139c8-8007-4199-80b6-e68a928a59fb	ASSIGNED	2026-11-14 09:30:59.999988+00	2026-11-14 09:30:59.999988+00	2026-11-14 08:41:13.635988+00	SD-4Y3N-7W4N	82 Ward Place, Colombo 07	318 Church Street, Galle Fort	Mohamed Rajendran	SMALL	1.33	CREATED
e7e93925-526d-41cb-9e20-43945defba14	6a136f34-e312-41e8-85d1-40b5b1bb8033	2a4e4e19-00b3-4d93-b3f5-185ca3701d3d	80d23eb8-1156-49dc-b5c3-42cdedfab75d	ASSIGNED	2026-11-14 10:07:09.00034+00	2026-11-14 10:07:09.00034+00	2026-11-14 09:51:22.61734+00	SD-762E-UCET	272 Kotte Road, Kotte	4 William Gopallawa Mawatha, Kandy	Ravindu Rajapaksa	SMALL	4.38	CREATED
1328a478-fc37-41e6-b678-2a61a2245a7f	97dbd3bd-cfd5-4b69-8bf7-4a3a02e536d0	2a4e4e19-00b3-4d93-b3f5-185ca3701d3d	49c139c8-8007-4199-80b6-e68a928a59fb	COMPLETED	2026-10-16 08:01:00.000354+00	2026-10-16 08:01:00.000354+00	2026-10-17 04:34:00.001756+00	SD-E8FD-BH6Q	59 William Gopallawa Mawatha, Kandy	196 Chatham Street, Colombo 01	Shehan Rahman	SMALL	1.22	DELIVERED
f8b1f5c8-33f4-4364-89d1-c9f9280220ab	847b8a16-3a0a-48ae-b93d-ebbb7a7a09bf	5ba3a5c8-4911-45c9-9155-84e05b5dae36	7d19bfe5-daf8-4879-8f88-7fcc9bff34c5	COMPLETED	2026-10-17 05:50:00.000158+00	2026-10-17 05:50:00.000158+00	2026-10-18 04:45:00.002597+00	SD-N8ZH-TKMW	243 Prince of Wales Avenue, Colombo 14	160 Stanley Thilakaratne Mawatha, Nugegoda	Anjali Rajendran	SMALL	3.03	DELIVERED
221c4ab3-90c4-4b7e-bf02-e415bddd01c3	ae4c9d47-686d-4390-8c73-9960b15539ae	f8264489-f505-49cf-817d-d4373523e808	80d23eb8-1156-49dc-b5c3-42cdedfab75d	COMPLETED	2026-10-17 07:51:00.000124+00	2026-10-17 07:51:00.000124+00	2026-10-18 05:55:00.001975+00	SD-3M6X-Y92H	188 Kollupitiya Road, Colombo 03	20 Main Street, Negombo	Supuni Liyanage	MEDIUM	5.01	DELIVERED
d9dd3a0d-9637-48c1-a42c-7b8e9ede73b8	b2bd86a0-b000-491e-b893-cdd88c1ba858	f8264489-f505-49cf-817d-d4373523e808	7d19bfe5-daf8-4879-8f88-7fcc9bff34c5	COMPLETED	2026-10-17 13:52:00.000476+00	2026-10-17 13:52:00.000476+00	2026-10-19 05:55:00.002057+00	SD-PZMS-HY9G	85 High Level Road, Maharagama	157 Thimbirigasyaya Road, Colombo 05	Anjali Wijesinghe	LARGE	5.24	DELIVERED
b46f556f-9044-4d8c-8eac-efce027e091c	3bddfe60-25f9-47ab-8925-3e2ebddff83d	f8264489-f505-49cf-817d-d4373523e808	80d23eb8-1156-49dc-b5c3-42cdedfab75d	COMPLETED	2026-10-17 14:42:00.000044+00	2026-10-17 14:42:00.000044+00	2026-10-18 06:03:00.002652+00	SD-26ZV-LE8J	184 Thimbirigasyaya Road, Colombo 05	327 Main Street, Colombo 11	Supuni Bandara	MEDIUM	9.81	DELIVERED
44c3a408-2f06-4484-8eeb-0a35d23d2a0b	e18c8423-831b-44a9-aa94-2870198d1d70	1cda4767-b26a-4a9e-aa6d-d52cf46c2dc3	49c139c8-8007-4199-80b6-e68a928a59fb	COMPLETED	2026-10-18 14:01:00.000004+00	2026-10-18 14:01:00.000005+00	2026-10-19 05:17:00.002356+00	SD-K8N4-PT6Q	59 Church Street, Galle Fort	324 Matara Road, Galle	Mohamed Wijesinghe	MEDIUM	4.98	DELIVERED
94c7a4cd-5714-4670-8991-a2b23e600396	cf41bce5-2258-4004-b52c-81b77c572b8e	ff655d35-a0cb-4ec7-a11c-25e7cb868ddf	7d19bfe5-daf8-4879-8f88-7fcc9bff34c5	FAILED	2026-10-19 12:17:00.00019+00	2026-10-19 12:17:00.00019+00	2026-10-20 06:57:00.002749+00	SD-4Z52-HPHE	59 William Gopallawa Mawatha, Kandy	344 Main Street, Negombo	Lakshmi Fernando	SMALL	4.44	FAILED
96cb03bc-b4c5-4b2f-a30b-3c001f7fefc6	028d6342-4b77-4a66-929c-d86b040c5e80	5ba3a5c8-4911-45c9-9155-84e05b5dae36	7d19bfe5-daf8-4879-8f88-7fcc9bff34c5	COMPLETED	2026-10-20 05:56:00.000258+00	2026-10-20 05:56:00.000258+00	2026-10-20 12:40:00.002567+00	SD-AG88-Y7AW	243 Prince of Wales Avenue, Colombo 14	162 Galle Road, Dehiwala	Ravindu Peiris	LARGE	7.00	DELIVERED
b241f5c3-c09a-40d1-8c77-90dbae91e51a	aa4a02c7-177f-413a-807a-8d5dc6822638	1cda4767-b26a-4a9e-aa6d-d52cf46c2dc3	7d19bfe5-daf8-4879-8f88-7fcc9bff34c5	COMPLETED	2026-10-20 07:39:00.000186+00	2026-10-20 07:39:00.000186+00	2026-10-21 07:41:00.00293+00	SD-V5RH-BAAK	188 Kollupitiya Road, Colombo 03	221 Dalada Veediya, Kandy	Sajith Sivakumar	SMALL	4.52	DELIVERED
a83184a9-286a-44b7-bd91-87bfe920b728	dc99d383-3e36-4e52-b1f6-53aa613492dd	ff655d35-a0cb-4ec7-a11c-25e7cb868ddf	7d19bfe5-daf8-4879-8f88-7fcc9bff34c5	COMPLETED	2026-10-20 14:42:00.000458+00	2026-10-20 14:42:00.000458+00	2026-10-21 08:04:00.003408+00	SD-27WC-YA6G	85 High Level Road, Maharagama	340 Peradeniya Road, Kandy	Shehan Herath	SMALL	4.10	DELIVERED
7f040a01-2b31-4754-8f88-ac7e082b58fe	6f4f17fc-432f-4485-b1bf-f580c24d7a5f	5ba3a5c8-4911-45c9-9155-84e05b5dae36	7d19bfe5-daf8-4879-8f88-7fcc9bff34c5	COMPLETED	2026-10-21 09:49:59.999587+00	2026-10-21 09:49:59.999587+00	2026-10-22 06:58:00.003495+00	SD-2DTV-QP7Z	184 Thimbirigasyaya Road, Colombo 05	73 Prince of Wales Avenue, Colombo 14	Pradeep Rathnayake	SMALL	4.53	DELIVERED
6e14b4d8-6a18-46b9-ade0-13b999f1b76a	04549e3f-d5dc-41ba-abd9-902269515fad	5ba3a5c8-4911-45c9-9155-84e05b5dae36	7d19bfe5-daf8-4879-8f88-7fcc9bff34c5	COMPLETED	2026-10-21 10:08:59.999686+00	2026-10-21 10:08:59.999686+00	2026-10-22 08:41:00.002028+00	SD-TGDS-ZE7A	59 Church Street, Galle Fort	287 Church Street, Galle Fort	Anjali Dissanayake	MEDIUM	3.81	DELIVERED
409dec9f-afd0-4d91-bf63-5f8d07242715	c57aac0a-1e88-4b7c-8d9d-ef12725a9333	5ba3a5c8-4911-45c9-9155-84e05b5dae36	49c139c8-8007-4199-80b6-e68a928a59fb	COMPLETED	2026-10-21 11:20:59.999981+00	2026-10-21 11:20:59.999981+00	2026-10-22 06:55:00.002002+00	SD-LG5D-3KDW	59 William Gopallawa Mawatha, Kandy	261 Prince of Wales Avenue, Colombo 14	Anjali Silva	SMALL	0.67	DELIVERED
ef4b7a25-b819-4de5-8833-e176551111cc	99b6d8a8-35e9-43eb-a138-5877433fcc2f	03706d2d-c424-4359-8d98-702189d2a120	49c139c8-8007-4199-80b6-e68a928a59fb	COMPLETED	2026-10-21 12:54:00.000042+00	2026-10-21 12:54:00.000042+00	2026-10-22 07:19:00.001805+00	SD-ZRUV-QCCR	243 Prince of Wales Avenue, Colombo 14	81 Maradana Road, Colombo 10	Sunil Amarasekara	LARGE	8.91	DELIVERED
67714d4d-d065-4902-b1e7-6f0148c077b6	abb4a93d-b380-4f6b-bbc3-99501e28899f	5ba3a5c8-4911-45c9-9155-84e05b5dae36	49c139c8-8007-4199-80b6-e68a928a59fb	COMPLETED	2026-10-22 09:41:00.000459+00	2026-10-22 09:41:00.000459+00	2026-10-23 05:13:00.002626+00	SD-3YUN-X8T7	188 Kollupitiya Road, Colombo 03	3 Marine Drive, Colombo 06	Priya Wijesinghe	LARGE	6.07	DELIVERED
e28db3f4-6e27-4967-b409-653d8fb55c05	ba826b8e-1037-4a8f-891b-601836f6b623	5ba3a5c8-4911-45c9-9155-84e05b5dae36	7d19bfe5-daf8-4879-8f88-7fcc9bff34c5	COMPLETED	2026-10-22 14:21:00.00011+00	2026-10-22 14:21:00.00011+00	2026-10-23 05:00:00.002013+00	SD-PM9T-6SHR	85 High Level Road, Maharagama	331 Main Street, Negombo	Hiruni Mendis	SMALL	4.99	DELIVERED
a2e53980-8b54-4cff-8bb6-559e2d1460eb	e5ef7136-68c8-4d46-9f28-e9e2d92c1efd	1cda4767-b26a-4a9e-aa6d-d52cf46c2dc3	80d23eb8-1156-49dc-b5c3-42cdedfab75d	COMPLETED	2026-10-23 03:26:59.999956+00	2026-10-23 03:26:59.999956+00	2026-10-24 08:14:00.002824+00	SD-6GP8-XUA8	184 Thimbirigasyaya Road, Colombo 05	187 York Street, Colombo 01	Tharindu Liyanage	EXTRA_LARGE	16.25	DELIVERED
115ed4ac-43d3-4414-b5d3-65e65d94f6c8	4cd97983-2378-4921-84ce-0a540ad93fe4	0692e9c1-ff92-4d13-ae68-a7c2168947c3	7d19bfe5-daf8-4879-8f88-7fcc9bff34c5	COMPLETED	2026-10-23 04:04:59.999683+00	2026-10-23 04:04:59.999683+00	2026-10-24 08:18:00.002376+00	SD-LHR2-6CNT	59 Church Street, Galle Fort	30 Church Street, Galle Fort	Kasun Liyanage	MEDIUM	2.64	DELIVERED
1fd3fe72-5207-4f4a-bbc8-47af3dd728ad	97e61206-0c55-4d6f-85ff-1d40aa8e5a5e	0692e9c1-ff92-4d13-ae68-a7c2168947c3	49c139c8-8007-4199-80b6-e68a928a59fb	COMPLETED	2026-10-23 07:07:59.999785+00	2026-10-23 07:07:59.999785+00	2026-10-24 05:54:00.002004+00	SD-JWN9-SH5Z	59 William Gopallawa Mawatha, Kandy	265 Galle Road, Colombo 04	Malith Cooray	EXTRA_LARGE	26.29	DELIVERED
cf8d173d-9775-416e-a04e-43f73cb49805	bdb43a56-a17a-433f-9d71-f97208be93bf	0692e9c1-ff92-4d13-ae68-a7c2168947c3	80d23eb8-1156-49dc-b5c3-42cdedfab75d	COMPLETED	2026-10-23 08:46:59.999906+00	2026-10-23 08:46:59.999906+00	2026-10-24 06:06:00.002326+00	SD-TD68-J2GG	243 Prince of Wales Avenue, Colombo 14	223 Main Street, Negombo	Chathura Rajapaksa	MEDIUM	3.36	DELIVERED
52e85093-4761-463f-b8e1-a200cefe427f	679e0f91-c539-42dd-b249-518cc2cc7ca2	f8264489-f505-49cf-817d-d4373523e808	49c139c8-8007-4199-80b6-e68a928a59fb	COMPLETED	2026-10-23 14:39:59.999743+00	2026-10-23 14:39:59.999743+00	2026-10-24 04:23:00.002675+00	SD-9YVA-C9FV	188 Kollupitiya Road, Colombo 03	201 Main Street, Negombo	Nipuni Samarasinghe	MEDIUM	3.25	DELIVERED
530e138b-6ba8-4960-bf6a-96fa105ebee2	defae5af-d535-4407-b5d9-5b96e58fccce	2a4e4e19-00b3-4d93-b3f5-185ca3701d3d	7d19bfe5-daf8-4879-8f88-7fcc9bff34c5	COMPLETED	2026-10-24 04:22:59.999893+00	2026-10-24 04:22:59.999893+00	2026-10-25 07:23:00.001857+00	SD-GDKD-6WCX	85 High Level Road, Maharagama	186 Hotel Road, Mount Lavinia	Anjali Liyanage	MEDIUM	2.80	DELIVERED
f1d6bd52-4e26-4838-ba7a-9b1b0e404077	219a886a-4e85-4e61-a9a0-319131ff32fc	5ba3a5c8-4911-45c9-9155-84e05b5dae36	7d19bfe5-daf8-4879-8f88-7fcc9bff34c5	COMPLETED	2026-10-24 06:18:00.000164+00	2026-10-24 06:18:00.000164+00	2026-10-25 05:49:00.002691+00	SD-RPVT-2LUH	184 Thimbirigasyaya Road, Colombo 05	214 Church Street, Galle Fort	Sahan Ratnayake	MEDIUM	4.01	DELIVERED
38664095-19b5-4ab9-aceb-8d75db27bee1	31994151-4619-491a-9693-4ebccf8e38b4	2a4e4e19-00b3-4d93-b3f5-185ca3701d3d	49c139c8-8007-4199-80b6-e68a928a59fb	COMPLETED	2026-10-24 07:38:00.000353+00	2026-10-24 07:38:00.000353+00	2026-10-25 06:45:00.001965+00	SD-UTFH-HMAA	59 Church Street, Galle Fort	338 Peradeniya Road, Kandy	Nipuni Rajendran	LARGE	13.34	DELIVERED
112a9779-e1bb-400b-a321-848b1aa4a123	2e90cb60-5ad4-4166-8ea0-4b4150f48255	1cda4767-b26a-4a9e-aa6d-d52cf46c2dc3	80d23eb8-1156-49dc-b5c3-42cdedfab75d	COMPLETED	2026-10-24 09:21:00.00049+00	2026-10-24 09:21:00.00049+00	2026-10-25 06:13:00.002078+00	SD-DYWL-J6EL	59 William Gopallawa Mawatha, Kandy	142 Pita Kotte Road, Kotte	Dulani Sivakumar	MEDIUM	9.42	DELIVERED
697cb626-d4fe-4b67-9a65-04a7388309a1	706c6b4c-eafa-4106-80c2-ec0f2f7de13d	1cda4767-b26a-4a9e-aa6d-d52cf46c2dc3	49c139c8-8007-4199-80b6-e68a928a59fb	COMPLETED	2026-10-24 09:26:00.000413+00	2026-10-24 09:26:00.000413+00	2026-10-25 04:37:00.001763+00	SD-UW7V-ZMPG	243 Prince of Wales Avenue, Colombo 14	289 Stanley Thilakaratne Mawatha, Nugegoda	Kavindi Mendis	EXTRA_LARGE	24.83	DELIVERED
6ae1896d-d662-496f-b93c-c52876cdbd00	b0e9bc2b-79e4-4451-8d19-f3da58a41bcf	f8264489-f505-49cf-817d-d4373523e808	49c139c8-8007-4199-80b6-e68a928a59fb	COMPLETED	2026-10-25 04:38:00.000203+00	2026-10-25 04:38:00.000203+00	2026-10-25 07:59:00.002327+00	SD-3QYE-8E3R	188 Kollupitiya Road, Colombo 03	328 Prince of Wales Avenue, Colombo 14	Sunil Mendis	SMALL	3.04	DELIVERED
8481c832-f930-42f7-9d62-e29adc1e0007	b81c9da6-95f6-42df-a7d7-e5df91bec9da	2a4e4e19-00b3-4d93-b3f5-185ca3701d3d	49c139c8-8007-4199-80b6-e68a928a59fb	COMPLETED	2026-10-25 06:38:00.000183+00	2026-10-25 06:38:00.000183+00	2026-10-26 07:10:00.002538+00	SD-CHTG-TCJQ	85 High Level Road, Maharagama	119 Marine Drive, Colombo 06	Ishara Fernando	MEDIUM	4.49	DELIVERED
27fc3d11-b6e4-4983-a082-fb43079302b4	a29887b4-09f0-4408-acb3-1fec493b1cde	2a4e4e19-00b3-4d93-b3f5-185ca3701d3d	7d19bfe5-daf8-4879-8f88-7fcc9bff34c5	COMPLETED	2026-10-25 07:57:00.000485+00	2026-10-25 07:57:00.000485+00	2026-10-26 06:13:00.002038+00	SD-H5LA-AMF2	184 Thimbirigasyaya Road, Colombo 05	43 Old Kesbewa Road, Nugegoda	Dinuka Jayawardena	LARGE	11.83	DELIVERED
1250a667-6b45-4bde-8024-3e55a994b922	163ef59e-a983-46f0-a6b9-4dd445217687	1cda4767-b26a-4a9e-aa6d-d52cf46c2dc3	7d19bfe5-daf8-4879-8f88-7fcc9bff34c5	COMPLETED	2026-10-25 11:04:00.000248+00	2026-10-25 11:04:00.000248+00	2026-10-26 07:24:00.002337+00	SD-3HSQ-Z2VN	59 Church Street, Galle Fort	157 De Saram Road, Mount Lavinia	Anjali Nawaz	SMALL	1.63	DELIVERED
2a0cd009-8b7c-45a4-affd-77434cf86a03	6cc9c460-9d18-448f-b090-b925aa4f4616	03706d2d-c424-4359-8d98-702189d2a120	7d19bfe5-daf8-4879-8f88-7fcc9bff34c5	COMPLETED	2026-10-25 14:30:59.999914+00	2026-10-25 14:30:59.999914+00	2026-10-26 06:35:00.003025+00	SD-RPJ5-8F4A	59 William Gopallawa Mawatha, Kandy	339 Flower Road, Colombo 07	Dilani Dissanayake	LARGE	9.19	DELIVERED
51e50060-90af-42c5-878b-636bf619f517	d5b97f72-f54c-4c23-b057-e4c0823f97bb	0692e9c1-ff92-4d13-ae68-a7c2168947c3	80d23eb8-1156-49dc-b5c3-42cdedfab75d	COMPLETED	2026-10-26 09:13:00.000448+00	2026-10-26 09:13:00.000448+00	2026-10-27 04:22:00.003051+00	SD-EZ4D-E8WH	243 Prince of Wales Avenue, Colombo 14	160 Ward Place, Colombo 07	Priya Dissanayake	LARGE	16.62	DELIVERED
134eb802-d376-42b9-82c7-3e070cba85c4	252adb9d-77a1-40ad-b861-0e9842fc570e	2afe8e87-d5cd-41ea-99b4-db04a7f48d25	7d19bfe5-daf8-4879-8f88-7fcc9bff34c5	COMPLETED	2026-10-26 11:38:00.00022+00	2026-10-26 11:38:00.000221+00	2026-10-27 05:01:00.002574+00	SD-2FB4-9X9C	188 Kollupitiya Road, Colombo 03	161 Peradeniya Road, Kandy	Supuni Sivakumar	SMALL	2.05	DELIVERED
993719a8-e42e-4fc9-bf26-34ff67ddc7b7	be176bb6-bb39-483b-a768-e7decc318e03	2afe8e87-d5cd-41ea-99b4-db04a7f48d25	7d19bfe5-daf8-4879-8f88-7fcc9bff34c5	COMPLETED	2026-10-27 12:42:00.000213+00	2026-10-27 12:42:00.000213+00	2026-10-28 05:48:00.003075+00	SD-NTMV-PK6P	85 High Level Road, Maharagama	36 Prince of Wales Avenue, Colombo 14	Dilani Rajendran	SMALL	1.34	DELIVERED
0751d0e8-27f6-48df-bad6-d60865323716	c9233b02-c3cd-4898-92af-c5ac1ff204a6	2a4e4e19-00b3-4d93-b3f5-185ca3701d3d	7d19bfe5-daf8-4879-8f88-7fcc9bff34c5	COMPLETED	2026-10-27 13:18:59.999846+00	2026-10-27 13:18:59.999846+00	2026-10-28 07:18:00.002558+00	SD-4Q5F-DJZT	184 Thimbirigasyaya Road, Colombo 05	240 Parliament Road, Rajagiriya	Mohamed Herath	SMALL	4.30	DELIVERED
1f7dea90-22cf-445f-a000-2b5e3f299b54	8eeb8d57-fe1b-4b10-9126-98f3e39d9878	0692e9c1-ff92-4d13-ae68-a7c2168947c3	49c139c8-8007-4199-80b6-e68a928a59fb	COMPLETED	2026-10-28 07:29:59.999894+00	2026-10-28 07:29:59.999894+00	2026-10-29 05:12:00.001977+00	SD-U5WY-QG6Y	59 Church Street, Galle Fort	266 Ramakrishna Road, Colombo 06	Hashini Samarasinghe	SMALL	1.49	DELIVERED
9d931d0f-aec4-46dc-8b83-ea3056bca450	1095aaed-0a2a-4647-8d1a-db30edefd318	f8264489-f505-49cf-817d-d4373523e808	7d19bfe5-daf8-4879-8f88-7fcc9bff34c5	COMPLETED	2026-10-28 08:27:59.999776+00	2026-10-28 08:27:59.999776+00	2026-10-29 06:33:00.002401+00	SD-XXUE-XYST	59 William Gopallawa Mawatha, Kandy	258 Duplication Road, Colombo 03	Ruwan Jayasinghe	SMALL	1.21	DELIVERED
83e40dc2-f5dc-453f-8e45-685e19784912	25f30799-0863-4ffb-bd89-7cf8fd72f0fd	1cda4767-b26a-4a9e-aa6d-d52cf46c2dc3	7d19bfe5-daf8-4879-8f88-7fcc9bff34c5	COMPLETED	2026-10-28 13:49:00.000465+00	2026-10-28 13:49:00.000465+00	2026-10-29 07:00:00.002215+00	SD-W99M-ZC83	243 Prince of Wales Avenue, Colombo 14	110 Hotel Road, Mount Lavinia	Nipuni Rajapaksa	LARGE	16.74	DELIVERED
f0ad9687-d310-4bb2-8f90-1474204e3a26	72e7dd74-df2a-4a93-8447-26a0a73fa925	03706d2d-c424-4359-8d98-702189d2a120	80d23eb8-1156-49dc-b5c3-42cdedfab75d	FAILED	2026-10-29 07:10:59.999678+00	2026-10-29 07:10:59.999679+00	2026-10-30 04:28:00.001395+00	SD-J6J4-S8S8	188 Kollupitiya Road, Colombo 03	30 High Level Road, Maharagama	Lakshmi Senanayake	LARGE	15.60	FAILED
883d6f35-1c68-4c30-bb27-97d4100fe9a2	e022dd0f-dc40-47e4-9e9d-a25357488108	ff655d35-a0cb-4ec7-a11c-25e7cb868ddf	7d19bfe5-daf8-4879-8f88-7fcc9bff34c5	COMPLETED	2026-10-29 12:10:59.999695+00	2026-10-29 12:10:59.999695+00	2026-10-30 05:47:00.00178+00	SD-UKAU-J26C	85 High Level Road, Maharagama	278 De Saram Road, Mount Lavinia	Sajith Jayasinghe	LARGE	13.84	DELIVERED
f5cd3933-735b-4e88-8f8d-6d3b39a4fd86	1ee4bcf7-bb7b-4c29-952d-55fbab53bf5e	ff655d35-a0cb-4ec7-a11c-25e7cb868ddf	80d23eb8-1156-49dc-b5c3-42cdedfab75d	FAILED	2026-10-30 06:42:00.000276+00	2026-10-30 06:42:00.000276+00	2026-10-31 04:52:00.002591+00	SD-H97K-DM5W	184 Thimbirigasyaya Road, Colombo 05	25 Dalada Veediya, Kandy	Thisaru Silva	SMALL	0.30	FAILED
b4055baa-8c88-4fc8-bd1c-32fa67eb1da6	1b752b1a-26a1-4cce-9db9-2194376fae67	0692e9c1-ff92-4d13-ae68-a7c2168947c3	7d19bfe5-daf8-4879-8f88-7fcc9bff34c5	COMPLETED	2026-10-30 09:18:59.999761+00	2026-10-30 09:18:59.999761+00	2026-10-31 05:45:00.001697+00	SD-NNVU-A9ES	59 Church Street, Galle Fort	88 Parliament Road, Rajagiriya	Hiruni Ratnayake	LARGE	6.40	DELIVERED
bb6257bf-8e12-4cb0-943c-778590c294ba	6fbc6a71-94a9-4f30-b4bd-2004e6887044	03706d2d-c424-4359-8d98-702189d2a120	49c139c8-8007-4199-80b6-e68a928a59fb	COMPLETED	2026-10-30 10:51:00.000114+00	2026-10-30 10:51:00.000114+00	2026-10-31 07:37:00.001973+00	SD-RRDU-GLAS	59 William Gopallawa Mawatha, Kandy	128 Lewis Place, Negombo	Malith Silva	MEDIUM	8.46	DELIVERED
2f88667d-3d04-4915-8a42-7c56c41e7775	b1674497-9c17-495a-a3ac-2b8b9a4288e3	03706d2d-c424-4359-8d98-702189d2a120	49c139c8-8007-4199-80b6-e68a928a59fb	COMPLETED	2026-10-31 06:24:59.999561+00	2026-10-31 06:24:59.999561+00	2026-11-01 05:14:00.001527+00	SD-Q9YZ-YFG4	243 Prince of Wales Avenue, Colombo 14	13 Hill Street, Dehiwala	Pradeep Silva	MEDIUM	7.31	DELIVERED
946cc0cc-952f-415d-9259-70fff6e60249	cb320019-0e44-41c1-8c9b-bf9157f8ba4b	5ba3a5c8-4911-45c9-9155-84e05b5dae36	49c139c8-8007-4199-80b6-e68a928a59fb	COMPLETED	2026-11-01 04:51:59.999662+00	2026-11-01 04:51:59.999662+00	2026-11-02 06:22:00.002254+00	SD-PTJL-K4PP	188 Kollupitiya Road, Colombo 03	42 Galle Road, Colombo 04	Senuri Rajapaksa	MEDIUM	8.86	DELIVERED
9e7c346e-5e08-40d4-a4ca-bc1402ea93ac	d3fbcca3-5c8e-41d4-9ad5-cf2f5aa0d985	2a4e4e19-00b3-4d93-b3f5-185ca3701d3d	7d19bfe5-daf8-4879-8f88-7fcc9bff34c5	COMPLETED	2026-11-01 09:56:59.999708+00	2026-11-01 09:56:59.999708+00	2026-11-02 04:19:00.002366+00	SD-JLXC-MJ76	85 High Level Road, Maharagama	47 De Saram Road, Mount Lavinia	Yasiru Senanayake	SMALL	0.30	DELIVERED
75d3b028-f62d-4cad-9a47-f48267232a59	4748b8ce-e100-475f-ad19-1765212a2510	2a4e4e19-00b3-4d93-b3f5-185ca3701d3d	80d23eb8-1156-49dc-b5c3-42cdedfab75d	COMPLETED	2026-11-01 11:33:00.000327+00	2026-11-01 11:33:00.000328+00	2026-11-02 05:03:00.00253+00	SD-P87W-M5PJ	184 Thimbirigasyaya Road, Colombo 05	288 Lewis Place, Negombo	Kamal Dissanayake	SMALL	3.46	DELIVERED
de5ac3f2-cf77-4671-8243-0d49cf37d25a	040ddcbc-5280-47f1-825c-a59275874a76	1cda4767-b26a-4a9e-aa6d-d52cf46c2dc3	49c139c8-8007-4199-80b6-e68a928a59fb	COMPLETED	2026-11-01 11:18:00.000023+00	2026-11-01 11:18:00.000023+00	2026-11-02 08:20:00.002089+00	SD-L87J-BQ3M	59 Church Street, Galle Fort	62 Main Street, Colombo 11	Kamal Liyanage	SMALL	4.79	DELIVERED
5716e283-5c5a-432d-a000-00da43b93100	95d934f3-63d5-4cb7-8da3-df658f3a98b0	0692e9c1-ff92-4d13-ae68-a7c2168947c3	80d23eb8-1156-49dc-b5c3-42cdedfab75d	COMPLETED	2026-11-01 12:48:00.000009+00	2026-11-01 12:48:00.000009+00	2026-11-02 03:57:00.002184+00	SD-XWKP-3T5W	59 William Gopallawa Mawatha, Kandy	22 Colombo Road, Negombo	Tharindu Amarasekara	MEDIUM	6.90	DELIVERED
243acb35-0a94-48f4-be50-73fe43ceba8c	8f4a5b4c-0f58-4543-96d8-f003c414e220	03706d2d-c424-4359-8d98-702189d2a120	7d19bfe5-daf8-4879-8f88-7fcc9bff34c5	COMPLETED	2026-11-01 14:02:00.000311+00	2026-11-01 14:02:00.000311+00	2026-11-02 04:51:00.001674+00	SD-258B-L37Z	243 Prince of Wales Avenue, Colombo 14	196 Hill Street, Dehiwala	Dinuka Perera	SMALL	3.82	DELIVERED
6aa18f9d-07dc-449e-acb8-a6e91a27df45	401480cf-2d8e-42b7-ae22-20ab7a099e28	16d13c48-6ea3-48b5-b0b5-fbe7fcefec4d	49c139c8-8007-4199-80b6-e68a928a59fb	COMPLETED	2026-11-02 14:18:00.000029+00	2026-11-02 14:18:00.000029+00	2026-11-03 06:38:00.004063+00	SD-GQCF-HMSD	188 Kollupitiya Road, Colombo 03	214 Pita Kotte Road, Kotte	Mohamed Jayawardena	LARGE	7.78	DELIVERED
00ae9f3d-8831-46ac-bd5f-28720b0dff1b	c08e4280-3066-4190-b59f-95b7e6e4a330	0692e9c1-ff92-4d13-ae68-a7c2168947c3	49c139c8-8007-4199-80b6-e68a928a59fb	COMPLETED	2026-11-03 03:35:59.999718+00	2026-11-03 03:35:59.999718+00	2026-11-04 06:51:00.003108+00	SD-XNQJ-GSYS	85 High Level Road, Maharagama	63 Ramakrishna Road, Colombo 06	Kasun Amarasekara	LARGE	16.05	DELIVERED
902d846e-35c9-4692-bad5-de32cb223d98	f5eaa1a6-a4e4-4410-9c1f-813b1d750b32	5ba3a5c8-4911-45c9-9155-84e05b5dae36	49c139c8-8007-4199-80b6-e68a928a59fb	COMPLETED	2026-11-03 11:35:00.000041+00	2026-11-03 11:35:00.000041+00	2026-11-04 06:33:00.002377+00	SD-2BD4-EESS	184 Thimbirigasyaya Road, Colombo 05	71 High Level Road, Maharagama	Kavindi Rajendran	SMALL	4.11	DELIVERED
41022151-bd76-4a6b-b3e3-04ff004439fc	b25e6d6f-6a49-438f-bf6e-ccf8dfe1cf5c	2a4e4e19-00b3-4d93-b3f5-185ca3701d3d	49c139c8-8007-4199-80b6-e68a928a59fb	COMPLETED	2026-11-05 05:03:59.999569+00	2026-11-05 05:03:59.999569+00	2026-11-06 07:20:00.002652+00	SD-CN9P-AFX5	59 Church Street, Galle Fort	295 Havelock Road, Colombo 05	Kumar Dissanayake	LARGE	19.36	DELIVERED
c217480a-ce2a-4154-9548-8d0e6a5f094e	154924a3-8730-419e-a343-13706d0c1058	f8264489-f505-49cf-817d-d4373523e808	80d23eb8-1156-49dc-b5c3-42cdedfab75d	COMPLETED	2026-11-06 09:36:00.000148+00	2026-11-06 09:36:00.000148+00	2026-11-07 06:01:00.002488+00	SD-GSKL-H4C2	59 William Gopallawa Mawatha, Kandy	176 Old Kesbewa Road, Nugegoda	Senuri Silva	MEDIUM	8.81	DELIVERED
30385b0d-a8c7-4a5f-a80c-8ce3116c28e6	f7793586-0040-4ff4-8b89-1b95b14a8b4c	2afe8e87-d5cd-41ea-99b4-db04a7f48d25	80d23eb8-1156-49dc-b5c3-42cdedfab75d	COMPLETED	2026-11-07 05:49:59.999525+00	2026-11-07 05:49:59.999525+00	2026-11-08 07:05:00.002597+00	SD-CSTT-Z5GR	243 Prince of Wales Avenue, Colombo 14	256 High Level Road, Maharagama	Sahan Bandara	MEDIUM	3.24	DELIVERED
9880fe5e-4221-4f94-9eef-add92f6026aa	a0ad2836-2f19-4941-8245-a58d49b505c8	5ba3a5c8-4911-45c9-9155-84e05b5dae36	80d23eb8-1156-49dc-b5c3-42cdedfab75d	COMPLETED	2026-11-07 11:28:59.999887+00	2026-11-07 11:28:59.999887+00	2026-11-08 06:46:00.002722+00	SD-SNLA-VS3N	188 Kollupitiya Road, Colombo 03	303 Flower Road, Colombo 07	Sahan Silva	EXTRA_LARGE	33.69	DELIVERED
7c0696cb-2028-4d2b-b1d0-a8826862ea8c	688bba91-5c24-4cd1-a4fb-3d27eaf95136	f8264489-f505-49cf-817d-d4373523e808	49c139c8-8007-4199-80b6-e68a928a59fb	COMPLETED	2026-11-07 13:21:00.000463+00	2026-11-07 13:21:00.000463+00	2026-11-08 07:28:00.001828+00	SD-G9UN-J8FH	85 High Level Road, Maharagama	85 High Level Road, Maharagama	Malith Dissanayake	MEDIUM	2.64	DELIVERED
1fc3b69d-98ba-46ec-9d75-70ee82516726	ec86b9cc-0a2d-4262-8fed-9078abbea755	2afe8e87-d5cd-41ea-99b4-db04a7f48d25	49c139c8-8007-4199-80b6-e68a928a59fb	COMPLETED	2026-11-08 04:37:00.000498+00	2026-11-08 04:37:00.000498+00	2026-11-09 03:38:00.00222+00	SD-VFH8-TGRQ	184 Thimbirigasyaya Road, Colombo 05	193 Main Street, Colombo 11	Dimuth Jayawardena	MEDIUM	3.09	DELIVERED
0d1c0b55-fda9-4c67-bffe-f4dddf3e945a	4ff8d9bd-1c7c-45dc-9b7b-a64446eab5d4	1cda4767-b26a-4a9e-aa6d-d52cf46c2dc3	49c139c8-8007-4199-80b6-e68a928a59fb	FAILED	2026-11-08 07:25:00.000491+00	2026-11-08 07:25:00.000491+00	2026-11-09 08:45:00.001855+00	SD-6APB-2YDD	59 Church Street, Galle Fort	128 Colombo Road, Negombo	Ayesha Cooray	MEDIUM	6.11	FAILED
ab1a350b-307d-4b62-8645-1104660b1f55	85e4a87c-812b-4c27-b5c1-bc7c980d255d	f8264489-f505-49cf-817d-d4373523e808	80d23eb8-1156-49dc-b5c3-42cdedfab75d	COMPLETED	2026-11-09 03:49:00.000267+00	2026-11-09 03:49:00.000267+00	2026-11-10 07:34:00.002025+00	SD-MBXZ-QQ8Z	59 William Gopallawa Mawatha, Kandy	27 Old Kesbewa Road, Nugegoda	Anjali Bandara	SMALL	3.18	DELIVERED
bcee654b-4164-4713-b684-1436e52f198c	c240f03e-4d7d-40ea-815d-80bcf9746f07	03706d2d-c424-4359-8d98-702189d2a120	7d19bfe5-daf8-4879-8f88-7fcc9bff34c5	COMPLETED	2026-11-09 04:33:59.999961+00	2026-11-09 04:33:59.999961+00	2026-11-10 05:19:00.001981+00	SD-5Y5U-CKUE	243 Prince of Wales Avenue, Colombo 14	300 York Street, Colombo 01	Chamari Rajapaksa	SMALL	3.24	DELIVERED
b6c4d74c-a0de-4a96-829c-99b44db73897	9d8c82e1-dd8c-4c2c-88fd-c1e6915def6a	0692e9c1-ff92-4d13-ae68-a7c2168947c3	80d23eb8-1156-49dc-b5c3-42cdedfab75d	COMPLETED	2026-11-09 06:20:59.999522+00	2026-11-09 06:20:59.999522+00	2026-11-10 08:25:00.001436+00	SD-ES8F-54XH	188 Kollupitiya Road, Colombo 03	26 Station Road, Nugegoda	Sahan Senanayake	LARGE	19.30	DELIVERED
59716fa6-389a-4138-a63a-6b02e1337433	ff9e2078-d372-4467-a80c-cc8a15646694	2afe8e87-d5cd-41ea-99b4-db04a7f48d25	80d23eb8-1156-49dc-b5c3-42cdedfab75d	COMPLETED	2026-11-09 07:04:59.999877+00	2026-11-09 07:04:59.999877+00	2026-11-10 06:20:00.001302+00	SD-4DAS-592D	85 High Level Road, Maharagama	43 Dalada Veediya, Kandy	Pradeep Abeysekera	MEDIUM	3.85	DELIVERED
c3b1f546-2dc6-41de-afb1-4bed89fe39bc	caa06da7-08da-4b1a-b3bc-5c7177a55a01	ff655d35-a0cb-4ec7-a11c-25e7cb868ddf	7d19bfe5-daf8-4879-8f88-7fcc9bff34c5	COMPLETED	2026-11-09 10:14:00.000457+00	2026-11-09 10:14:00.000457+00	2026-11-10 07:39:00.002266+00	SD-BY4S-FYCD	184 Thimbirigasyaya Road, Colombo 05	50 Havelock Road, Colombo 05	Rizwan Karunaratne	MEDIUM	7.18	DELIVERED
849df54d-8bcd-4754-9f8c-f9be0c89234a	b1f029fb-5b57-4ac3-bd27-de28ccff4f47	16d13c48-6ea3-48b5-b0b5-fbe7fcefec4d	49c139c8-8007-4199-80b6-e68a928a59fb	FAILED	2026-11-10 10:38:59.999968+00	2026-11-10 10:38:59.999968+00	2026-11-11 05:39:00.002217+00	SD-PEN8-3JKS	59 Church Street, Galle Fort	160 De Saram Road, Mount Lavinia	Chamari Sivakumar	SMALL	4.87	FAILED
84fe6a14-3b12-4603-89a4-d50f5198a3a4	d6b559e9-6a79-4d00-bac3-4eb32cc612c9	16d13c48-6ea3-48b5-b0b5-fbe7fcefec4d	7d19bfe5-daf8-4879-8f88-7fcc9bff34c5	COMPLETED	2026-11-10 10:49:59.999527+00	2026-11-10 10:49:59.999527+00	2026-11-11 07:38:00.001701+00	SD-D6GF-YCUN	59 William Gopallawa Mawatha, Kandy	178 Galle Road, Colombo 03	Nipuni Wickramasinghe	SMALL	0.38	DELIVERED
6567d2ae-55eb-49f4-b994-6aa76ae4c5bb	4484943d-e478-4ef4-a596-55c48a5ff860	ff655d35-a0cb-4ec7-a11c-25e7cb868ddf	7d19bfe5-daf8-4879-8f88-7fcc9bff34c5	FAILED	2026-11-11 04:31:00.000236+00	2026-11-11 04:31:00.000236+00	2026-11-12 07:01:00.001895+00	SD-UVAK-9DZH	243 Prince of Wales Avenue, Colombo 14	224 Havelock Road, Colombo 05	Sanduni Rajendran	MEDIUM	3.85	FAILED
eac860b6-890c-4274-8072-587c0151fa4b	90213101-b437-47ac-b550-56a539a42728	1cda4767-b26a-4a9e-aa6d-d52cf46c2dc3	49c139c8-8007-4199-80b6-e68a928a59fb	COMPLETED	2026-11-11 12:52:59.999993+00	2026-11-11 12:52:59.999993+00	2026-11-12 05:05:00.002547+00	SD-B32B-YZU2	188 Kollupitiya Road, Colombo 03	275 Galle Road, Dehiwala	Sachini Rathnayake	SMALL	2.17	DELIVERED
800743bf-020b-4520-aef7-cfc4e6c9ad33	14362f56-a35a-47eb-b2ae-0366b9e71ac6	ff655d35-a0cb-4ec7-a11c-25e7cb868ddf	49c139c8-8007-4199-80b6-e68a928a59fb	COMPLETED	2026-11-12 07:04:59.9996+00	2026-11-12 07:04:59.9996+00	2026-11-13 06:50:00.001621+00	SD-U7WJ-FETA	85 High Level Road, Maharagama	223 William Gopallawa Mawatha, Kandy	Dinuka Senanayake	SMALL	0.30	DELIVERED
a2922fb7-5afd-43e9-9c31-a040166eb9d6	2417c5aa-da80-4151-8029-587bdaceea64	1cda4767-b26a-4a9e-aa6d-d52cf46c2dc3	80d23eb8-1156-49dc-b5c3-42cdedfab75d	COMPLETED	2026-11-12 09:30:59.999873+00	2026-11-12 09:30:59.999873+00	2026-11-13 06:33:00.00162+00	SD-BJPD-AWVD	184 Thimbirigasyaya Road, Colombo 05	109 High Level Road, Maharagama	Priya Rathnayake	LARGE	15.13	DELIVERED
a7ba0036-4ecc-49e9-b359-31048ceb6213	783227ad-3712-4cfc-aad3-3edb0a29911f	0692e9c1-ff92-4d13-ae68-a7c2168947c3	49c139c8-8007-4199-80b6-e68a928a59fb	COMPLETED	2026-11-12 10:33:59.99951+00	2026-11-12 10:33:59.99951+00	2026-11-13 06:49:00.002171+00	SD-5MQB-GJ74	59 Church Street, Galle Fort	252 Lewis Place, Negombo	Nadeesha Cooray	SMALL	1.40	DELIVERED
3871323c-e3d9-4814-be0b-d3745fe8281a	00dbf2cd-b81c-4f80-bc78-2909a2fa178b	5ba3a5c8-4911-45c9-9155-84e05b5dae36	49c139c8-8007-4199-80b6-e68a928a59fb	COMPLETED	2026-11-13 06:29:00.000078+00	2026-11-13 06:29:00.000078+00	2026-11-14 06:46:00.001799+00	SD-5NDC-S7E8	59 William Gopallawa Mawatha, Kandy	314 Galle Road, Colombo 04	Lahiru Perera	MEDIUM	6.51	DELIVERED
227d5152-1738-4162-b5a1-54ccd1fe5fa3	a486c9af-0166-418f-93dc-23aa7e7ff015	03706d2d-c424-4359-8d98-702189d2a120	80d23eb8-1156-49dc-b5c3-42cdedfab75d	COMPLETED	2026-11-13 06:45:59.999628+00	2026-11-13 06:45:59.999628+00	2026-11-14 07:05:00.004391+00	SD-2J73-D7AX	243 Prince of Wales Avenue, Colombo 14	18 High Level Road, Maharagama	Priya Samarasinghe	LARGE	18.96	DELIVERED
77768888-7ebc-400c-8e95-4e76676bd6a5	e483ca82-9218-4bd4-91bf-de55195353e8	16d13c48-6ea3-48b5-b0b5-fbe7fcefec4d	49c139c8-8007-4199-80b6-e68a928a59fb	COMPLETED	2026-11-13 09:43:00.000431+00	2026-11-13 09:43:00.000431+00	2026-11-14 06:52:00.002366+00	SD-6SNX-3XGV	188 Kollupitiya Road, Colombo 03	192 Pita Kotte Road, Kotte	Ruwan Rathnayake	MEDIUM	3.33	DELIVERED
219abfd0-a366-45f2-bc15-9952a55b1ebb	32277336-20d2-4f64-919c-b3cdcdcc8c2e	2afe8e87-d5cd-41ea-99b4-db04a7f48d25	49c139c8-8007-4199-80b6-e68a928a59fb	IN_PROGRESS	2026-11-14 03:52:00.000459+00	2026-11-14 03:52:00.000459+00	2026-11-14 05:54:00.002312+00	SD-6MMF-PF4L	85 High Level Road, Maharagama	7 Old Kesbewa Road, Nugegoda	Kumar Fonseka	EXTRA_LARGE	27.78	IN_TRANSIT
35b82f02-f10b-4139-84f9-11e73031fac5	cfcdc245-1ee8-41ae-8a4f-7562132355dc	f8264489-f505-49cf-817d-d4373523e808	80d23eb8-1156-49dc-b5c3-42cdedfab75d	IN_PROGRESS	2026-11-14 05:23:59.999827+00	2026-11-14 05:23:59.999827+00	2026-11-14 09:04:00.001955+00	SD-DBK4-HPTK	184 Thimbirigasyaya Road, Colombo 05	208 Parliament Road, Rajagiriya	Kumar Gunasekara	LARGE	14.40	IN_TRANSIT
66765321-065b-44fb-949e-5560b304ce24	35f6d117-0326-4c47-ae13-9d4573c8e97d	f8264489-f505-49cf-817d-d4373523e808	7d19bfe5-daf8-4879-8f88-7fcc9bff34c5	IN_PROGRESS	2026-11-14 07:43:00.00043+00	2026-11-14 07:43:00.00043+00	2026-11-14 09:33:00.002658+00	SD-A4NM-UKNG	59 Church Street, Galle Fort	182 Stanley Thilakaratne Mawatha, Nugegoda	Chathura Herath	MEDIUM	3.49	PICKED_UP
ed1ccc2a-3e3f-4ac5-8ab7-4880e7a84547	d4d5a8c2-fa2f-458b-b197-7103c6e01381	1cda4767-b26a-4a9e-aa6d-d52cf46c2dc3	7d19bfe5-daf8-4879-8f88-7fcc9bff34c5	ASSIGNED	2026-11-14 07:51:08.999727+00	2026-11-14 07:51:08.999727+00	2026-11-14 07:35:24.391727+00	SD-8CJA-3BEA	59 William Gopallawa Mawatha, Kandy	321 Pita Kotte Road, Kotte	Hashini Liyanage	SMALL	1.85	CREATED
ad57b3ea-a1ea-462f-a307-cadec5682ed2	c32fb930-0f69-421c-9fef-998c096ef091	16d13c48-6ea3-48b5-b0b5-fbe7fcefec4d	49c139c8-8007-4199-80b6-e68a928a59fb	ASSIGNED	2026-11-14 10:08:59.999747+00	2026-11-14 10:08:59.999747+00	2026-11-14 08:09:15.377747+00	SD-PH2H-4FRV	243 Prince of Wales Avenue, Colombo 14	6 De Saram Road, Mount Lavinia	Arun de Silva	SMALL	0.59	CREATED
\.


--
-- Data for Name: couriers; Type: TABLE DATA; Schema: public; Owner: dispatch
--

COPY public.couriers (id, user_id, status, created_at, updated_at) FROM stdin;
00000000-0000-4000-8000-00000000d001	00000000-0000-4000-8000-00000000c001	AVAILABLE	2026-11-14 10:16:59.86814+00	\N
5ba3a5c8-4911-45c9-9155-84e05b5dae36	c5592d9f-a72a-4822-9080-fd0ccc511add	AVAILABLE	2026-08-10 09:42:00+00	\N
2a4e4e19-00b3-4d93-b3f5-185ca3701d3d	a1452cf6-4d4b-4a28-860e-518d81821912	AVAILABLE	2026-07-09 02:40:00+00	\N
f8264489-f505-49cf-817d-d4373523e808	42b44f66-66c4-44b8-af94-031065494ba7	AVAILABLE	2026-08-07 12:35:00+00	\N
1cda4767-b26a-4a9e-aa6d-d52cf46c2dc3	73623c45-6a11-4080-98da-1a004bacbf48	AVAILABLE	2026-08-21 11:26:00+00	\N
ff655d35-a0cb-4ec7-a11c-25e7cb868ddf	28e8368b-8a0e-4fa6-8a8f-db7172b8c354	AVAILABLE	2026-09-01 05:54:00+00	\N
2afe8e87-d5cd-41ea-99b4-db04a7f48d25	88670f51-c181-48ba-b6ff-1342422fa974	AVAILABLE	2026-08-18 03:35:00+00	\N
16d13c48-6ea3-48b5-b0b5-fbe7fcefec4d	8a9f6979-f1cc-4539-a393-1baa9bb70bad	AVAILABLE	2026-09-02 06:40:00+00	\N
0692e9c1-ff92-4d13-ae68-a7c2168947c3	2312f6f5-ecd5-4e40-8ed1-8bfbfe15c53a	AVAILABLE	2026-07-16 05:43:00+00	\N
03706d2d-c424-4359-8d98-702189d2a120	175873ad-65ef-45e0-91ea-d52af596bf9e	AVAILABLE	2026-07-23 04:27:00+00	\N
e26b2e9d-5b3c-455d-a3c1-bb348b126efd	9875ccd7-f7f9-4a43-bbbd-c1fd75a9407e	OFFLINE	2026-07-14 05:37:00+00	\N
\.


--
-- Data for Name: daily_stats; Type: TABLE DATA; Schema: public; Owner: dispatch
--

COPY public.daily_stats (id, date, courier_id, assigned_count, completed_count, failed_count, created_at, updated_at) FROM stdin;
7fe77bd9-d168-48de-b5b4-0fae9f77e87c	2026-10-18	03706d2d-c424-4359-8d98-702189d2a120	1	1	0	2026-10-18 05:06:59.999769+00	2026-10-19 05:57:00.003322+00
10c4dab5-42c5-4d55-b0ad-8e0ff8a96e86	2026-10-31	16d13c48-6ea3-48b5-b0b5-fbe7fcefec4d	2	2	0	2026-10-31 11:13:00.000354+00	2026-11-01 05:18:00.002545+00
33b711a4-80cf-428e-9c68-9781f7c75033	2026-11-02	0692e9c1-ff92-4d13-ae68-a7c2168947c3	2	1	1	2026-11-02 09:11:59.999669+00	2026-11-03 07:42:00.003249+00
08cc24fc-73fd-477b-aa62-017165f2cdf4	2026-11-14	03706d2d-c424-4359-8d98-702189d2a120	3	0	0	2026-11-14 09:57:59.999569+00	2026-11-14 09:10:00.002331+00
55cc3b5d-dba6-4409-aeed-d2a50028364c	2026-11-12	ff655d35-a0cb-4ec7-a11c-25e7cb868ddf	1	1	0	2026-11-12 07:04:59.9996+00	2026-11-13 06:50:00.001621+00
30b09612-be1e-49ab-8f73-71174931fc7f	2026-11-01	1cda4767-b26a-4a9e-aa6d-d52cf46c2dc3	1	1	0	2026-11-01 11:18:00.000023+00	2026-11-02 08:20:00.002089+00
c5ded904-4e18-4f13-8abd-6a4846d0e600	2026-11-09	ff655d35-a0cb-4ec7-a11c-25e7cb868ddf	1	1	0	2026-11-09 10:14:00.000457+00	2026-11-10 07:39:00.002266+00
49ce2996-a5c4-40ec-b069-2e398239c9cd	2026-11-02	2a4e4e19-00b3-4d93-b3f5-185ca3701d3d	1	1	0	2026-11-02 14:49:00.000155+00	2026-11-04 08:09:00.002959+00
ff55061f-6291-4bb4-a91b-18bcea9aa70e	2026-11-10	f8264489-f505-49cf-817d-d4373523e808	1	1	0	2026-11-10 03:42:59.999528+00	2026-11-11 05:12:00.003266+00
97d97fa9-911f-42c2-adfc-0b13822935eb	2026-10-26	0692e9c1-ff92-4d13-ae68-a7c2168947c3	2	2	0	2026-10-26 09:13:00.000448+00	2026-10-27 06:33:00.002786+00
25025420-4ba0-4365-a0ce-933c357c57f0	2026-11-13	2a4e4e19-00b3-4d93-b3f5-185ca3701d3d	1	1	0	2026-11-13 06:50:00.000171+00	2026-11-14 04:54:00.001953+00
797740a7-0df3-4bcd-8b99-626d859aba1b	2026-11-11	ff655d35-a0cb-4ec7-a11c-25e7cb868ddf	1	0	1	2026-11-11 04:31:00.000236+00	2026-11-12 07:01:00.001895+00
3b3f2057-1270-41cf-b291-c4a84feb706a	2026-10-24	ff655d35-a0cb-4ec7-a11c-25e7cb868ddf	2	2	0	2026-10-24 07:19:59.999771+00	2026-10-25 07:20:00.00298+00
7667d7d1-a295-4c93-8887-ec6863a5c418	2026-11-07	f8264489-f505-49cf-817d-d4373523e808	2	2	0	2026-11-07 13:21:00.000463+00	2026-11-08 07:31:00.002882+00
83d40a68-433e-466e-a36f-2b99cc9b34d5	2026-11-07	1cda4767-b26a-4a9e-aa6d-d52cf46c2dc3	1	1	0	2026-11-07 14:41:00.000264+00	2026-11-07 23:01:00.003042+00
a0fb1be9-3b48-4723-97ad-5b2fbbd145e0	2026-10-26	2a4e4e19-00b3-4d93-b3f5-185ca3701d3d	1	1	0	2026-10-26 08:19:00.000434+00	2026-10-27 05:20:00.003054+00
4d0f3065-7830-41f3-9b44-d51db84639d7	2026-11-03	1cda4767-b26a-4a9e-aa6d-d52cf46c2dc3	3	3	0	2026-11-03 14:23:00.000199+00	2026-11-05 08:50:00.002664+00
40a2d6ff-5a62-4c7c-8a12-4af01faf6636	2026-10-30	16d13c48-6ea3-48b5-b0b5-fbe7fcefec4d	1	1	0	2026-10-30 12:32:59.999861+00	2026-10-31 04:46:00.003229+00
62caa1d2-d906-4221-b87a-c8f63b4ad387	2026-11-11	2afe8e87-d5cd-41ea-99b4-db04a7f48d25	2	2	0	2026-11-11 09:28:00.000201+00	2026-11-12 08:13:00.001886+00
faed6b1d-c0fb-4592-b08b-e73499be876e	2026-11-01	0692e9c1-ff92-4d13-ae68-a7c2168947c3	2	2	0	2026-11-01 12:48:00.000009+00	2026-11-02 04:52:00.00255+00
9d8b447b-6dfd-4b6b-b390-c7297189b009	2026-10-28	0692e9c1-ff92-4d13-ae68-a7c2168947c3	2	2	0	2026-10-28 11:54:59.999632+00	2026-10-29 07:22:00.002584+00
398e5854-e99b-496f-a451-eacda1f997f3	2026-10-22	16d13c48-6ea3-48b5-b0b5-fbe7fcefec4d	1	1	0	2026-10-22 07:10:59.999873+00	2026-10-23 06:51:00.003682+00
49e93fa6-584a-4fe9-b373-d8c82e5bfb0e	2026-11-06	f8264489-f505-49cf-817d-d4373523e808	1	1	0	2026-11-06 09:36:00.000148+00	2026-11-07 06:01:00.002488+00
99fef679-f9ba-4949-8893-1955c85f42c4	2026-11-14	f8264489-f505-49cf-817d-d4373523e808	3	0	0	2026-11-14 07:43:00.00043+00	2026-11-14 09:33:00.002658+00
349f6e25-718d-470d-8441-a45c4664fab3	2026-10-24	1cda4767-b26a-4a9e-aa6d-d52cf46c2dc3	3	3	0	2026-10-24 09:26:00.000413+00	2026-10-25 06:21:00.002989+00
008bed28-4caa-4f5b-a401-20598a3b994f	2026-11-01	2a4e4e19-00b3-4d93-b3f5-185ca3701d3d	2	2	0	2026-11-01 11:33:00.000327+00	2026-11-02 05:03:00.00253+00
59991507-628e-4a19-8db9-807dcddf903a	2026-10-18	1cda4767-b26a-4a9e-aa6d-d52cf46c2dc3	2	2	0	2026-10-18 14:01:00.000004+00	2026-10-19 07:11:00.00288+00
dd571b90-79d3-4d8e-a229-21272233d498	2026-10-30	0692e9c1-ff92-4d13-ae68-a7c2168947c3	1	1	0	2026-10-30 09:18:59.999761+00	2026-10-31 05:45:00.001697+00
40f5b625-9a24-4e0b-bae2-72659f26849a	2026-11-14	16d13c48-6ea3-48b5-b0b5-fbe7fcefec4d	5	1	0	2026-11-14 10:08:59.999747+00	2026-11-14 09:58:09.002375+00
33cabf20-4760-41e6-944e-43c5ceab6b63	2026-11-06	5ba3a5c8-4911-45c9-9155-84e05b5dae36	1	1	0	2026-11-06 11:27:59.999653+00	2026-11-07 06:56:00.003028+00
7ba24c64-b6bc-41d1-a37e-003cdcd95f53	2026-11-13	5ba3a5c8-4911-45c9-9155-84e05b5dae36	1	1	0	2026-11-13 06:29:00.000078+00	2026-11-14 06:46:00.001799+00
386d0bc6-eff1-401b-8cbe-84d9dc7444a4	2026-10-31	ff655d35-a0cb-4ec7-a11c-25e7cb868ddf	2	2	0	2026-10-31 13:44:00.000469+00	2026-11-01 05:08:00.002603+00
d9f7ebb0-3537-489a-88f3-b9dd07006e23	2026-10-27	1cda4767-b26a-4a9e-aa6d-d52cf46c2dc3	1	1	0	2026-10-27 09:51:59.999542+00	2026-10-28 07:56:00.003023+00
17dc3cfe-7ca3-4173-b478-5bd77edff4fb	2026-10-30	2a4e4e19-00b3-4d93-b3f5-185ca3701d3d	1	1	0	2026-10-30 08:41:59.9997+00	2026-10-31 05:54:00.003775+00
1e95ef3c-652e-4aac-90ce-e5cbb6f44229	2026-10-27	0692e9c1-ff92-4d13-ae68-a7c2168947c3	1	1	0	2026-10-27 04:10:59.999694+00	2026-10-28 05:37:00.002596+00
db560414-085e-4798-a77f-72788e59d67c	2026-11-14	ff655d35-a0cb-4ec7-a11c-25e7cb868ddf	1	0	0	2026-11-14 09:04:08.999943+00	2026-11-14 08:18:22.642943+00
32dc78b4-2563-40a7-8214-e99c2510e8d7	2026-10-17	16d13c48-6ea3-48b5-b0b5-fbe7fcefec4d	1	0	1	2026-10-17 06:28:59.999576+00	2026-10-18 06:12:00.002802+00
29bbe1c4-d7c2-4308-94df-a86f94a7c321	2026-11-14	5ba3a5c8-4911-45c9-9155-84e05b5dae36	1	0	0	2026-11-14 07:51:08.999936+00	2026-11-14 09:29:09.002686+00
02ca50e5-476b-4ee4-bb48-644d9a7f3ef2	2026-10-28	1cda4767-b26a-4a9e-aa6d-d52cf46c2dc3	1	1	0	2026-10-28 13:49:00.000465+00	2026-10-29 07:00:00.002215+00
200a0b3a-44b2-42af-9d61-bf4aa880650f	2026-10-19	03706d2d-c424-4359-8d98-702189d2a120	1	1	0	2026-10-19 04:00:59.999597+00	2026-10-20 05:29:00.003905+00
c1e603ba-a801-47e2-81a4-18c5a97909fa	2026-10-23	ff655d35-a0cb-4ec7-a11c-25e7cb868ddf	2	2	0	2026-10-23 09:10:59.999623+00	2026-10-24 07:05:00.00303+00
cf4453a6-e3b7-4ba6-92b0-032a5637c0b0	2026-10-27	5ba3a5c8-4911-45c9-9155-84e05b5dae36	1	1	0	2026-10-27 10:42:00.000113+00	2026-10-28 07:21:00.0026+00
46cd905d-62b9-4de7-9070-536a3bf63da5	2026-10-18	ff655d35-a0cb-4ec7-a11c-25e7cb868ddf	1	1	0	2026-10-18 10:02:59.999643+00	2026-10-19 08:40:00.004073+00
0082bfd5-843c-4290-b59a-db6f882dd316	2026-11-01	ff655d35-a0cb-4ec7-a11c-25e7cb868ddf	3	3	0	2026-11-01 13:05:00.0001+00	2026-11-02 07:00:00.002485+00
1dbc4325-f2c8-4fb3-911c-d85054afe89a	2026-10-18	2afe8e87-d5cd-41ea-99b4-db04a7f48d25	1	1	0	2026-10-18 12:04:00.000381+00	2026-10-19 05:57:00.003987+00
b50f981e-c79f-40b3-a172-61009b31981e	2026-11-06	0692e9c1-ff92-4d13-ae68-a7c2168947c3	1	1	0	2026-11-06 14:01:59.999553+00	2026-11-07 07:28:00.002151+00
b6c16522-8524-49af-a877-3c7782a89658	2026-11-13	16d13c48-6ea3-48b5-b0b5-fbe7fcefec4d	1	1	0	2026-11-13 09:43:00.000431+00	2026-11-14 06:52:00.002366+00
f03815ef-af72-4221-92ec-dd17287f7c2d	2026-10-23	1cda4767-b26a-4a9e-aa6d-d52cf46c2dc3	1	1	0	2026-10-23 03:26:59.999956+00	2026-10-24 08:14:00.002824+00
039292fc-ad66-415c-bbf2-8875ddce45cb	2026-11-09	03706d2d-c424-4359-8d98-702189d2a120	1	1	0	2026-11-09 04:33:59.999961+00	2026-11-10 05:19:00.001981+00
c52daf1c-7b10-441b-a207-fc27054b315a	2026-10-31	2afe8e87-d5cd-41ea-99b4-db04a7f48d25	1	1	0	2026-10-31 07:31:00.000158+00	2026-11-01 05:58:00.003104+00
06f9e239-c8f6-476b-ac4e-74a5b13f8fb1	2026-11-09	f8264489-f505-49cf-817d-d4373523e808	2	2	0	2026-11-09 07:50:59.999792+00	2026-11-10 07:34:00.002025+00
1b2dffc4-b956-4a49-a01b-25c876d8141f	2026-11-03	ff655d35-a0cb-4ec7-a11c-25e7cb868ddf	1	1	0	2026-11-03 14:54:00.000328+00	2026-11-04 04:42:00.002187+00
e2d68f8e-66a1-41a3-9713-61ac10e0eb2c	2026-10-16	0692e9c1-ff92-4d13-ae68-a7c2168947c3	1	1	0	2026-10-16 06:23:59.999777+00	2026-10-17 04:21:00.004065+00
ad920ecf-951f-4ac6-9687-b10a9a933997	2026-11-02	2afe8e87-d5cd-41ea-99b4-db04a7f48d25	1	1	0	2026-11-02 07:48:00.000064+00	2026-11-03 07:06:00.0025+00
a9ffdb57-1a73-445e-bce3-b418a0064755	2026-11-09	0692e9c1-ff92-4d13-ae68-a7c2168947c3	2	2	0	2026-11-09 11:58:00.000227+00	2026-11-10 08:25:00.001436+00
7fbcb4f5-7f28-4225-8210-4e138b0c4e80	2026-11-13	03706d2d-c424-4359-8d98-702189d2a120	2	1	1	2026-11-13 12:08:00.000452+00	2026-11-14 07:05:00.004391+00
ad97ca10-7199-4990-82c2-e539eeb43db6	2026-10-23	f8264489-f505-49cf-817d-d4373523e808	1	1	0	2026-10-23 14:39:59.999743+00	2026-10-24 04:23:00.002675+00
3df53498-d14c-4f6c-add4-74ada4139085	2026-10-31	2a4e4e19-00b3-4d93-b3f5-185ca3701d3d	1	1	0	2026-10-31 09:40:00.00001+00	2026-11-01 07:12:00.003208+00
40c44e00-e5b6-4aad-80e7-bce16820152e	2026-11-11	2a4e4e19-00b3-4d93-b3f5-185ca3701d3d	2	2	0	2026-11-11 12:10:00.000056+00	2026-11-12 06:34:00.001902+00
ba9d1ebe-527e-4acb-a3b1-dd1bcb0b092b	2026-10-16	2a4e4e19-00b3-4d93-b3f5-185ca3701d3d	1	1	0	2026-10-16 08:01:00.000354+00	2026-10-17 04:34:00.001756+00
584c0f3e-263c-4c98-b6b7-9263f7b66bc2	2026-11-08	1cda4767-b26a-4a9e-aa6d-d52cf46c2dc3	3	1	2	2026-11-08 12:56:59.999505+00	2026-11-09 08:45:00.001855+00
b1da1ca9-6b5a-4f69-bb44-af5bcb11fac8	2026-11-04	ff655d35-a0cb-4ec7-a11c-25e7cb868ddf	1	1	0	2026-11-04 05:45:00.000422+00	2026-11-05 08:02:00.002298+00
e330b3ee-2809-4fd7-9ffe-ac310ad1fba0	2026-11-06	2afe8e87-d5cd-41ea-99b4-db04a7f48d25	1	0	1	2026-11-06 07:10:59.99977+00	2026-11-07 08:22:00.003645+00
b7e9c545-6ce6-4462-9b61-d1beb6c1cb67	2026-11-07	5ba3a5c8-4911-45c9-9155-84e05b5dae36	1	1	0	2026-11-07 11:28:59.999887+00	2026-11-08 06:46:00.002722+00
1050927f-3935-4bff-85cc-7922520710c7	2026-10-21	5ba3a5c8-4911-45c9-9155-84e05b5dae36	4	4	0	2026-10-21 11:20:59.999981+00	2026-10-22 08:41:00.002028+00
10b10c8c-615c-46f4-9d70-272372134e05	2026-10-30	f8264489-f505-49cf-817d-d4373523e808	1	1	0	2026-10-30 12:35:59.999792+00	2026-10-31 05:29:00.003563+00
4936d9a1-fe90-4b13-9ac1-593e8042b621	2026-10-20	2a4e4e19-00b3-4d93-b3f5-185ca3701d3d	1	1	0	2026-10-20 13:37:59.9997+00	2026-10-21 06:26:00.002723+00
fbe8896b-0e69-4432-bbb1-f852b348b7cb	2026-11-07	ff655d35-a0cb-4ec7-a11c-25e7cb868ddf	1	1	0	2026-11-07 05:25:59.999533+00	2026-11-08 05:24:00.002597+00
d810b343-0c9e-41a8-957b-bd9ddbd673a5	2026-10-21	16d13c48-6ea3-48b5-b0b5-fbe7fcefec4d	1	1	0	2026-10-21 04:00:00.000028+00	2026-10-22 05:03:00.003707+00
abaabda1-9418-4a82-9f56-80ec4fed6d5b	2026-10-29	ff655d35-a0cb-4ec7-a11c-25e7cb868ddf	1	1	0	2026-10-29 12:10:59.999695+00	2026-10-30 05:47:00.00178+00
89336179-0c6b-4458-bc3e-9503a3ef0a9a	2026-10-28	2afe8e87-d5cd-41ea-99b4-db04a7f48d25	1	1	0	2026-10-28 07:07:59.999884+00	2026-10-29 06:35:00.002178+00
c2a05297-d57f-404a-9586-32b4b6d5b4cf	2026-11-11	03706d2d-c424-4359-8d98-702189d2a120	2	1	1	2026-11-11 04:57:00.000407+00	2026-11-12 06:55:00.002572+00
8b2d5efd-68be-4b6e-938a-1b81addfe29f	2026-10-25	03706d2d-c424-4359-8d98-702189d2a120	2	2	0	2026-10-25 14:30:59.999914+00	2026-10-26 06:35:00.003025+00
33d8811c-e53e-48af-b1fa-e76d3c865e85	2026-11-06	ff655d35-a0cb-4ec7-a11c-25e7cb868ddf	2	2	0	2026-11-06 11:58:00.00024+00	2026-11-07 07:06:00.002301+00
f8ee57e2-0169-4f1f-98e9-322cd0618c04	2026-11-10	ff655d35-a0cb-4ec7-a11c-25e7cb868ddf	2	2	0	2026-11-10 12:09:00.000098+00	2026-11-11 04:40:00.002396+00
92e10fe5-9f91-4c01-9959-25ca6238aec9	2026-10-31	03706d2d-c424-4359-8d98-702189d2a120	1	1	0	2026-10-31 06:24:59.999561+00	2026-11-01 05:14:00.001527+00
71421fed-3ed8-4afd-b7e5-7a8be61b6c06	2026-11-14	1cda4767-b26a-4a9e-aa6d-d52cf46c2dc3	1	0	0	2026-11-14 07:51:08.999727+00	2026-11-14 07:35:24.391727+00
b85746cf-dcb1-427a-9d17-e15843b694a9	2026-11-10	16d13c48-6ea3-48b5-b0b5-fbe7fcefec4d	5	4	1	2026-11-10 12:24:59.999533+00	2026-11-11 07:38:00.001701+00
94f50ea3-4d3c-4fd8-ad90-e063cbe83684	2026-11-14	2afe8e87-d5cd-41ea-99b4-db04a7f48d25	1	0	0	2026-11-14 03:52:00.000459+00	2026-11-14 05:54:00.002312+00
9c50a1a5-53ba-4e62-91ee-5653d523d1bc	2026-10-24	5ba3a5c8-4911-45c9-9155-84e05b5dae36	1	1	0	2026-10-24 06:18:00.000164+00	2026-10-25 05:49:00.002691+00
2280f4d7-aab2-456a-97fb-56e516cb17fd	2026-10-23	16d13c48-6ea3-48b5-b0b5-fbe7fcefec4d	2	2	0	2026-10-23 13:08:59.999573+00	2026-10-24 06:06:00.002835+00
c9c052f3-4f6f-4672-a09d-a8f93ddbd932	2026-10-27	03706d2d-c424-4359-8d98-702189d2a120	2	2	0	2026-10-27 05:52:00.000075+00	2026-10-28 06:20:00.00273+00
bfb36af1-bc7d-40f8-a02a-cd23f170162a	2026-10-16	ff655d35-a0cb-4ec7-a11c-25e7cb868ddf	2	1	1	2026-10-16 05:39:59.999657+00	2026-10-17 06:19:00.003819+00
c9e62093-24ec-4c41-9ae1-fc9032627548	2026-11-03	0692e9c1-ff92-4d13-ae68-a7c2168947c3	1	1	0	2026-11-03 03:35:59.999718+00	2026-11-04 06:51:00.003108+00
6867d292-3581-4ee2-8c33-1d75de692bf9	2026-10-22	f8264489-f505-49cf-817d-d4373523e808	1	1	0	2026-10-22 08:18:00.000334+00	2026-10-23 04:21:00.003364+00
5c503942-87a7-4e83-9ef7-181c34ea97f6	2026-10-29	03706d2d-c424-4359-8d98-702189d2a120	1	0	1	2026-10-29 07:10:59.999678+00	2026-10-30 04:28:00.001395+00
cc4dc44b-353c-43b5-8723-2269fa97c0ea	2026-11-08	2afe8e87-d5cd-41ea-99b4-db04a7f48d25	1	1	0	2026-11-08 04:37:00.000498+00	2026-11-09 03:38:00.00222+00
2b70f37a-e9a6-48e5-a62e-fd66c3420865	2026-11-02	16d13c48-6ea3-48b5-b0b5-fbe7fcefec4d	1	1	0	2026-11-02 14:18:00.000029+00	2026-11-03 06:38:00.004063+00
eb3a64ca-5292-4953-9682-49093369ebb9	2026-10-27	2afe8e87-d5cd-41ea-99b4-db04a7f48d25	1	1	0	2026-10-27 12:42:00.000213+00	2026-10-28 05:48:00.003075+00
3f420d03-203a-4a02-9e52-23e12d9f7834	2026-11-12	2a4e4e19-00b3-4d93-b3f5-185ca3701d3d	1	1	0	2026-11-12 09:41:59.999749+00	2026-11-13 05:47:00.00276+00
f360abce-506b-401c-a068-4b78036daa51	2026-10-21	03706d2d-c424-4359-8d98-702189d2a120	2	2	0	2026-10-21 13:12:59.999816+00	2026-10-22 07:19:00.001805+00
e527c534-b2d1-4a7d-bc02-c57e5371e1d5	2026-10-25	1cda4767-b26a-4a9e-aa6d-d52cf46c2dc3	1	1	0	2026-10-25 11:04:00.000248+00	2026-10-26 07:24:00.002337+00
a221b927-38eb-44f9-9c1d-ba0754606165	2026-10-19	0692e9c1-ff92-4d13-ae68-a7c2168947c3	1	1	0	2026-10-19 10:48:00.000127+00	2026-10-20 06:56:00.003025+00
8e378a39-a1d7-4483-b531-e43f9773aef9	2026-11-01	2afe8e87-d5cd-41ea-99b4-db04a7f48d25	1	1	0	2026-11-01 08:35:00.000242+00	2026-11-02 09:08:00.002562+00
dd609b55-6db5-4492-857f-ad0c93428798	2026-11-13	0692e9c1-ff92-4d13-ae68-a7c2168947c3	1	1	0	2026-11-13 07:14:00.000043+00	2026-11-14 03:45:00.00204+00
2803d078-d3f6-41c3-a5d3-fbc3b32d779b	2026-10-19	f8264489-f505-49cf-817d-d4373523e808	2	2	0	2026-10-19 10:19:00.000013+00	2026-10-20 07:04:00.002879+00
48c6284b-5924-4484-9636-49742855de9b	2026-10-26	2afe8e87-d5cd-41ea-99b4-db04a7f48d25	1	1	0	2026-10-26 11:38:00.00022+00	2026-10-27 05:01:00.002574+00
06717abf-6302-4f11-b2be-f8ab8d2993e7	2026-11-05	0692e9c1-ff92-4d13-ae68-a7c2168947c3	1	1	0	2026-11-05 09:52:00.000189+00	2026-11-06 06:38:00.004879+00
e6e0e987-4838-4aed-8e7f-289178589c9e	2026-11-03	5ba3a5c8-4911-45c9-9155-84e05b5dae36	3	3	0	2026-11-03 11:45:00.000026+00	2026-11-04 06:33:00.002377+00
cd122468-d331-4a6b-9a11-30944a84bb83	2026-10-30	ff655d35-a0cb-4ec7-a11c-25e7cb868ddf	1	0	1	2026-10-30 06:42:00.000276+00	2026-10-31 04:52:00.002591+00
d40a1f9e-3916-48cd-8a1a-d37cb0ba089f	2026-11-09	2a4e4e19-00b3-4d93-b3f5-185ca3701d3d	1	1	0	2026-11-09 06:32:00.000306+00	2026-11-10 05:08:00.002816+00
80fab6c3-c24a-4174-b85a-3950a84075b7	2026-11-09	1cda4767-b26a-4a9e-aa6d-d52cf46c2dc3	2	2	0	2026-11-09 12:23:59.999513+00	2026-11-10 08:07:00.002147+00
16812b79-c099-47e9-9016-5b8131e47b20	2026-10-20	ff655d35-a0cb-4ec7-a11c-25e7cb868ddf	1	1	0	2026-10-20 14:42:00.000458+00	2026-10-21 08:04:00.003408+00
33a050b2-ba90-40a0-be88-61c3cd52a632	2026-11-03	f8264489-f505-49cf-817d-d4373523e808	1	1	0	2026-11-03 10:26:00.000031+00	2026-11-04 07:58:00.002359+00
0d48857b-e574-4593-bff3-6243821c1d65	2026-10-28	5ba3a5c8-4911-45c9-9155-84e05b5dae36	1	1	0	2026-10-28 04:10:59.999931+00	2026-10-29 08:23:00.003105+00
1b70cdfd-5c46-4afc-a1b2-e5c5e91daff2	2026-10-23	0692e9c1-ff92-4d13-ae68-a7c2168947c3	4	4	0	2026-10-23 10:44:59.999884+00	2026-10-24 08:18:00.002376+00
028ad3ac-4f34-4ca3-a76f-6958f29cc0c3	2026-10-19	16d13c48-6ea3-48b5-b0b5-fbe7fcefec4d	1	1	0	2026-10-19 04:56:59.999557+00	2026-10-19 10:14:00.003223+00
ad9d8845-a9cd-4317-8864-b221b1b0b5c8	2026-10-17	5ba3a5c8-4911-45c9-9155-84e05b5dae36	1	1	0	2026-10-17 05:50:00.000158+00	2026-10-18 04:45:00.002597+00
657cdc0c-8e51-43f9-88c5-c7e6c99e4283	2026-10-18	2a4e4e19-00b3-4d93-b3f5-185ca3701d3d	1	1	0	2026-10-18 04:42:59.99974+00	2026-10-19 07:18:00.004147+00
804ed5fb-80ba-40f0-b7a6-cca3df3dea01	2026-10-17	2a4e4e19-00b3-4d93-b3f5-185ca3701d3d	2	2	0	2026-10-17 09:15:59.999534+00	2026-10-18 03:57:00.003658+00
3d56937f-b4dd-4e3c-931d-65f46a03c206	2026-11-05	2a4e4e19-00b3-4d93-b3f5-185ca3701d3d	2	2	0	2026-11-05 12:48:59.999611+00	2026-11-06 07:20:00.002652+00
8ff1976f-8952-4b5b-aef8-ebf2ffa12a43	2026-11-14	2a4e4e19-00b3-4d93-b3f5-185ca3701d3d	6	0	0	2026-11-14 10:07:09.00034+00	2026-11-14 09:54:09.002009+00
36913a18-3390-4060-b27b-23bf6dcfe1aa	2026-11-11	5ba3a5c8-4911-45c9-9155-84e05b5dae36	1	0	1	2026-11-11 12:49:00.000373+00	2026-11-12 06:33:00.00211+00
68e224b1-fe19-42d3-b134-468daccbee42	2026-10-20	2afe8e87-d5cd-41ea-99b4-db04a7f48d25	1	1	0	2026-10-20 13:25:00.000317+00	2026-10-21 05:33:00.003322+00
d9f5188d-3e00-474c-af24-368c5a2209d2	2026-11-10	2afe8e87-d5cd-41ea-99b4-db04a7f48d25	1	1	0	2026-11-10 11:16:59.999835+00	2026-11-11 06:55:00.002196+00
3c750695-0dec-4be9-bf4a-e4836041e596	2026-10-26	f8264489-f505-49cf-817d-d4373523e808	1	1	0	2026-10-26 08:22:00.000175+00	2026-10-27 04:30:00.004353+00
2dfa960c-6f8a-4328-8bdd-75f1dd3b81b2	2026-11-12	f8264489-f505-49cf-817d-d4373523e808	1	1	0	2026-11-12 14:36:00.000375+00	2026-11-13 05:34:00.002438+00
2e3a930d-521c-4894-9f97-d88de3b2b7bb	2026-10-31	0692e9c1-ff92-4d13-ae68-a7c2168947c3	2	2	0	2026-10-31 13:23:59.999816+00	2026-11-01 07:25:00.003313+00
4c2fd994-ad2a-4b68-9eb2-febe3ebc68ce	2026-11-03	16d13c48-6ea3-48b5-b0b5-fbe7fcefec4d	1	1	0	2026-11-03 07:54:59.999553+00	2026-11-04 07:48:00.003144+00
eddffab8-3c69-4ddd-84ae-cd5a8c61ab6d	2026-10-28	16d13c48-6ea3-48b5-b0b5-fbe7fcefec4d	2	2	0	2026-10-28 10:14:59.999915+00	2026-10-29 08:07:00.003493+00
6aa8a164-ddda-4fe6-a474-e70486ae8a90	2026-10-25	2a4e4e19-00b3-4d93-b3f5-185ca3701d3d	3	3	0	2026-10-25 11:29:00.000351+00	2026-10-26 07:10:00.002538+00
971ebad5-8063-406a-84e6-feb088274351	2026-11-08	f8264489-f505-49cf-817d-d4373523e808	2	2	0	2026-11-08 09:03:00.000281+00	2026-11-09 06:59:00.002908+00
42f41820-d845-4072-b755-8b706abda1d2	2026-10-26	5ba3a5c8-4911-45c9-9155-84e05b5dae36	1	1	0	2026-10-26 03:47:59.999872+00	2026-10-27 05:54:00.003174+00
98c6d3e3-cca9-435b-b668-27a5e9b6d0ec	2026-11-04	16d13c48-6ea3-48b5-b0b5-fbe7fcefec4d	1	1	0	2026-11-04 07:26:00.000337+00	2026-11-05 06:09:00.003063+00
e18ad45e-a528-4ea3-9a5c-f0565a02ded0	2026-10-27	2a4e4e19-00b3-4d93-b3f5-185ca3701d3d	1	1	0	2026-10-27 13:18:59.999846+00	2026-10-28 07:18:00.002558+00
49ca8d87-18bd-42fd-a273-b199802274f9	2026-10-28	f8264489-f505-49cf-817d-d4373523e808	2	2	0	2026-10-28 10:24:00.000197+00	2026-10-29 07:20:00.002849+00
a22a5a48-f381-42bb-8b7a-b8cf3aed4c38	2026-10-16	5ba3a5c8-4911-45c9-9155-84e05b5dae36	1	1	0	2026-10-16 06:19:00.00045+00	2026-10-17 07:22:00.004531+00
6349aa97-33d0-4ccf-a634-8d0ae112f4e8	2026-11-12	16d13c48-6ea3-48b5-b0b5-fbe7fcefec4d	1	1	0	2026-11-12 06:18:59.999689+00	2026-11-13 07:09:00.002304+00
68fd41a3-cb6c-4901-b45e-f96e4b09798d	2026-10-20	1cda4767-b26a-4a9e-aa6d-d52cf46c2dc3	2	2	0	2026-10-20 07:39:00.000186+00	2026-10-21 07:41:00.00293+00
63efe779-7bff-45f7-8408-cfdc6f5ad75e	2026-10-17	f8264489-f505-49cf-817d-d4373523e808	3	3	0	2026-10-17 14:42:00.000044+00	2026-10-19 05:55:00.002057+00
f22cc334-4bf2-4ac3-8280-d15f0ff75af6	2026-10-30	03706d2d-c424-4359-8d98-702189d2a120	1	1	0	2026-10-30 10:51:00.000114+00	2026-10-31 07:37:00.001973+00
f78afaf2-fa5e-425f-b4de-c71b2e96ed6f	2026-10-22	1cda4767-b26a-4a9e-aa6d-d52cf46c2dc3	1	0	1	2026-10-22 06:09:00.000261+00	2026-10-23 06:39:00.003916+00
a06c3056-410b-4bbd-9bac-164d77f98495	2026-10-22	5ba3a5c8-4911-45c9-9155-84e05b5dae36	2	2	0	2026-10-22 14:21:00.00011+00	2026-10-23 05:13:00.002626+00
aed0e778-af2e-479c-96f1-830af451ff4e	2026-10-24	f8264489-f505-49cf-817d-d4373523e808	1	1	0	2026-10-24 04:09:00.000496+00	2026-10-25 04:07:00.002714+00
58ab3431-fbc0-4812-81f4-d95cc8c0285e	2026-10-26	ff655d35-a0cb-4ec7-a11c-25e7cb868ddf	2	2	0	2026-10-26 11:53:00.000337+00	2026-10-27 07:38:00.002964+00
6b309c47-cc56-44e6-9a25-bd72e0e97bba	2026-11-13	2afe8e87-d5cd-41ea-99b4-db04a7f48d25	3	3	0	2026-11-13 14:52:59.999825+00	2026-11-14 08:28:00.00255+00
8d9f740c-b9f3-494e-82f6-fa7a602a87a4	2026-10-29	2afe8e87-d5cd-41ea-99b4-db04a7f48d25	2	2	0	2026-10-29 12:05:00.000369+00	2026-10-30 08:23:00.002573+00
18ec3c72-6223-49ec-8673-be8b62f24a8f	2026-11-06	2a4e4e19-00b3-4d93-b3f5-185ca3701d3d	2	2	0	2026-11-06 09:51:00.000105+00	2026-11-07 07:18:00.005695+00
5ccd925d-8548-4ba1-9b22-41be19129355	2026-11-11	1cda4767-b26a-4a9e-aa6d-d52cf46c2dc3	1	1	0	2026-11-11 12:52:59.999993+00	2026-11-12 05:05:00.002547+00
a9001852-eba2-4b2a-a669-82e5c33a8689	2026-10-25	f8264489-f505-49cf-817d-d4373523e808	2	2	0	2026-10-25 07:44:00.000474+00	2026-10-26 07:46:00.003088+00
ed5547d4-f7ba-446e-8e36-fcdbdd2a18d1	2026-11-07	2afe8e87-d5cd-41ea-99b4-db04a7f48d25	1	1	0	2026-11-07 05:49:59.999525+00	2026-11-08 07:05:00.002597+00
90bad63a-be7d-4f9f-8601-a732386b5a55	2026-11-06	03706d2d-c424-4359-8d98-702189d2a120	1	1	0	2026-11-06 05:27:00.000436+00	2026-11-07 06:33:00.00319+00
54338853-0def-4c79-aaa6-7a3216429f21	2026-11-04	0692e9c1-ff92-4d13-ae68-a7c2168947c3	2	2	0	2026-11-04 13:18:00.000248+00	2026-11-05 08:40:00.002712+00
e633f58b-919a-4a16-9dad-47604eb63ff7	2026-11-12	1cda4767-b26a-4a9e-aa6d-d52cf46c2dc3	2	2	0	2026-11-12 09:30:59.999873+00	2026-11-13 06:33:00.00162+00
27abbbbe-6853-40c6-8721-c27519cc09c1	2026-11-10	2a4e4e19-00b3-4d93-b3f5-185ca3701d3d	1	1	0	2026-11-10 12:31:59.999808+00	2026-11-11 04:37:00.002006+00
5a632741-3f92-4b7e-8fa3-354dca95d7ac	2026-11-01	5ba3a5c8-4911-45c9-9155-84e05b5dae36	1	1	0	2026-11-01 04:51:59.999662+00	2026-11-02 06:22:00.002254+00
54b21007-243a-434d-a2f2-99eaa88239f7	2026-11-02	5ba3a5c8-4911-45c9-9155-84e05b5dae36	1	1	0	2026-11-02 14:16:00.000469+00	2026-11-03 04:53:00.002476+00
6bf69813-0661-48af-9682-1ec9aed3ff17	2026-11-11	16d13c48-6ea3-48b5-b0b5-fbe7fcefec4d	2	1	1	2026-11-11 12:08:59.999562+00	2026-11-12 07:05:00.002374+00
044b76b1-002b-4b86-9f7e-040b2f022274	2026-10-26	03706d2d-c424-4359-8d98-702189d2a120	1	1	0	2026-10-26 08:17:00.000183+00	2026-10-27 05:06:00.003311+00
555ea8b9-ea10-4ab7-ac5f-d9faa734ab8b	2026-10-30	2afe8e87-d5cd-41ea-99b4-db04a7f48d25	2	2	0	2026-10-30 13:16:59.99954+00	2026-10-31 07:18:00.002998+00
789a46ca-f59b-40ed-abd7-7bd45a71f50a	2026-10-26	16d13c48-6ea3-48b5-b0b5-fbe7fcefec4d	1	1	0	2026-10-26 13:21:00.000101+00	2026-10-27 07:20:00.002286+00
6e0692c9-19ee-4efb-93bf-c72cd9937395	2026-10-24	2a4e4e19-00b3-4d93-b3f5-185ca3701d3d	2	2	0	2026-10-24 07:38:00.000353+00	2026-10-25 07:23:00.001857+00
180cfa19-8fb1-4089-8bf2-b131ccdb7763	2026-10-19	ff655d35-a0cb-4ec7-a11c-25e7cb868ddf	1	0	1	2026-10-19 12:17:00.00019+00	2026-10-20 06:57:00.002749+00
4e1e8908-dfee-40e7-ae70-81e96bab3db8	2026-11-01	03706d2d-c424-4359-8d98-702189d2a120	1	1	0	2026-11-01 14:02:00.000311+00	2026-11-02 04:51:00.001674+00
38f1b715-e325-433f-ade3-9a8b6f497996	2026-10-20	5ba3a5c8-4911-45c9-9155-84e05b5dae36	2	2	0	2026-10-20 08:40:59.999516+00	2026-10-21 06:28:00.003803+00
3942bb1d-3ebb-45e8-a31d-15e5efc59c06	2026-11-05	f8264489-f505-49cf-817d-d4373523e808	1	1	0	2026-11-05 05:12:59.99983+00	2026-11-06 05:25:00.002352+00
4e252960-2ed3-407c-b39b-02cf6cd72663	2026-11-07	16d13c48-6ea3-48b5-b0b5-fbe7fcefec4d	2	2	0	2026-11-07 08:42:59.99997+00	2026-11-08 06:22:00.002836+00
aa13ed02-3e81-4923-aa7d-67c5eca49669	2026-10-25	0692e9c1-ff92-4d13-ae68-a7c2168947c3	1	1	0	2026-10-25 11:43:00.000427+00	2026-10-26 05:46:00.003505+00
5affc50c-3de9-4e06-91e6-7c0df0059925	2026-11-09	2afe8e87-d5cd-41ea-99b4-db04a7f48d25	1	1	0	2026-11-09 07:04:59.999877+00	2026-11-10 06:20:00.001302+00
a34fc312-1dc0-4c4d-a15d-417f2db42ca5	2026-11-04	2afe8e87-d5cd-41ea-99b4-db04a7f48d25	1	1	0	2026-11-04 14:31:00.000185+00	2026-11-05 06:38:00.002617+00
430795bd-de01-422e-ab54-47b9a4d7093c	2026-11-12	0692e9c1-ff92-4d13-ae68-a7c2168947c3	3	3	0	2026-11-12 10:33:59.99951+00	2026-11-13 06:49:00.002171+00
5565ec8a-6df5-4707-aa9f-b2b78b5ee99a	2026-11-10	0692e9c1-ff92-4d13-ae68-a7c2168947c3	1	1	0	2026-11-10 06:23:59.999695+00	2026-11-11 05:45:00.002554+00
a1cb95f3-c3e0-448d-93ff-6ed365926445	2026-10-25	5ba3a5c8-4911-45c9-9155-84e05b5dae36	1	1	0	2026-10-25 08:00:59.999835+00	2026-10-26 08:29:00.003354+00
\.


--
-- Data for Name: delivery_proofs; Type: TABLE DATA; Schema: public; Owner: dispatch
--

COPY public.delivery_proofs (id, assignment_id, object_key, file_name, uploaded_at, created_at, updated_at) FROM stdin;
e15593ce-9b4d-413f-b4e3-b17194d76f00	46f99904-39d7-4ab1-9391-2657d28ca0b5	assignments/46f99904-39d7-4ab1-9391-2657d28ca0b5/d1818b92-9ead-40ac-91db-ae5dba5cc553.png	\N	2026-10-17 05:36:00.003924+00	2026-10-17 05:36:00.003925+00	2026-10-17 05:36:00.003925+00
b756ce3a-ceb8-4e68-b5dc-3c7e09852441	a375580b-aeb1-4383-a551-e39917c1760e	assignments/a375580b-aeb1-4383-a551-e39917c1760e/a825558b-e725-40b9-946b-2c7bee7ca58c.png	\N	2026-10-17 07:22:00.003711+00	2026-10-17 07:22:00.003711+00	2026-10-17 07:22:00.003711+00
5a80a9a8-b415-4de5-9d2f-24be7eaa96d5	94762905-62f2-421e-93a6-d6417aef949f	assignments/94762905-62f2-421e-93a6-d6417aef949f/4bee54ae-6ca2-4e08-a256-d618b3671e29.png	\N	2026-10-17 04:21:00.003282+00	2026-10-17 04:21:00.003282+00	2026-10-17 04:21:00.003282+00
75a65e3e-dfc2-46d1-a025-d9fdd394457a	6391389b-c04c-4a2c-b964-4c1219415ad0	assignments/6391389b-c04c-4a2c-b964-4c1219415ad0/c3be2ab0-6289-419a-b30b-5a05c11c7bb6.png	\N	2026-10-18 03:57:00.003099+00	2026-10-18 03:57:00.003099+00	2026-10-18 03:57:00.003099+00
bc4aa977-422c-4c09-bcff-40294e41c65e	046c2174-f759-4eb2-935a-80e038e05a87	assignments/046c2174-f759-4eb2-935a-80e038e05a87/fbd1dbed-730d-4a4b-a492-7dd69db0fa8d.png	\N	2026-10-19 07:18:00.003557+00	2026-10-19 07:18:00.003557+00	2026-10-19 07:18:00.003557+00
74cbd503-1f89-4be3-8abb-612fef17ef15	633e779f-bccd-4dd1-a976-151d935a75e2	assignments/633e779f-bccd-4dd1-a976-151d935a75e2/5fed3b60-f5a4-444d-bb58-4954653cbcf2.png	\N	2026-10-19 08:40:00.003465+00	2026-10-19 08:40:00.003465+00	2026-10-19 08:40:00.003465+00
9efd8f0d-3681-47a0-be84-8f34b1f73602	06a8c0fa-3de5-4786-a382-b3b38abb6540	assignments/06a8c0fa-3de5-4786-a382-b3b38abb6540/51baf783-410b-4ecc-8ccd-87c73c3b831d.png	\N	2026-10-19 05:57:00.003287+00	2026-10-19 05:57:00.003287+00	2026-10-19 05:57:00.003287+00
038a9e17-d592-46b0-8bae-19bf9743ad30	87ef3a07-cbfe-4c77-acc3-0283e5332f7d	assignments/87ef3a07-cbfe-4c77-acc3-0283e5332f7d/f4565201-8588-409c-b420-a0eed0d5c794.png	\N	2026-10-20 05:29:00.003393+00	2026-10-20 05:29:00.003393+00	2026-10-20 05:29:00.003393+00
cbbc50cb-f788-405f-92df-9c9e6655bb79	34841185-4b1a-46fe-b12c-799eca471300	assignments/34841185-4b1a-46fe-b12c-799eca471300/980f3675-c5f1-45bb-ba30-5949daef0e6e.png	\N	2026-10-20 07:04:00.002332+00	2026-10-20 07:04:00.002332+00	2026-10-20 07:04:00.002332+00
59710fab-4746-4735-ad84-da7aafcbe234	acded89e-4bdd-4a41-a997-9be1c2e1448a	assignments/acded89e-4bdd-4a41-a997-9be1c2e1448a/580068cc-0215-4e39-b952-40b8a3874d62.png	\N	2026-10-20 06:56:00.002483+00	2026-10-20 06:56:00.002483+00	2026-10-20 06:56:00.002483+00
3839c359-1867-46a7-9b98-3fd122d67ca9	d74f2ee9-9946-4897-8cc4-d52699f27d0a	assignments/d74f2ee9-9946-4897-8cc4-d52699f27d0a/ae11056a-1c83-47ef-b72d-a49b5556bf4f.png	\N	2026-10-21 06:26:00.002303+00	2026-10-21 06:26:00.002303+00	2026-10-21 06:26:00.002303+00
c98ae1aa-2ba0-4e95-886e-5311d62b370d	350193c7-d419-41c6-8551-4f07a461d7ec	assignments/350193c7-d419-41c6-8551-4f07a461d7ec/682e4e26-7683-4dbb-82de-5f8d5bc2ffb5.png	\N	2026-10-22 05:03:00.003209+00	2026-10-22 05:03:00.00321+00	2026-10-22 05:03:00.00321+00
9b03de76-16ff-49e0-a983-185091c1434c	ff2663ac-0ec3-4ed6-819f-f611b6fd676a	assignments/ff2663ac-0ec3-4ed6-819f-f611b6fd676a/7f7204e2-4f73-4ea9-9360-d43b24a909f8.png	\N	2026-10-22 06:37:00.002453+00	2026-10-22 06:37:00.002454+00	2026-10-22 06:37:00.002454+00
d1dfed72-9860-4264-a6e9-6c1a2340e9c8	ee31ad5f-4d02-476a-9bc0-1f98ad33aab3	assignments/ee31ad5f-4d02-476a-9bc0-1f98ad33aab3/0e636fa9-fcf8-4b2f-8d05-bdcc94a9b32b.png	\N	2026-10-23 06:51:00.003166+00	2026-10-23 06:51:00.003167+00	2026-10-23 06:51:00.003167+00
f4975f7c-f922-4988-86b6-dbbae3990576	f07cd848-694a-4b91-bee3-a40dff16cce8	assignments/f07cd848-694a-4b91-bee3-a40dff16cce8/53ed9346-24f2-4e13-9555-94e78a3c6823.png	\N	2026-10-23 04:21:00.002748+00	2026-10-23 04:21:00.002748+00	2026-10-23 04:21:00.002748+00
6fff7397-f717-4ce4-a388-608c585dc3ab	897696ea-a219-4dce-b9d6-4dc1fdc43e0d	assignments/897696ea-a219-4dce-b9d6-4dc1fdc43e0d/3143b97f-51a5-4b1b-a022-a4129dcec2d0.png	\N	2026-10-24 05:03:00.002389+00	2026-10-24 05:03:00.002389+00	2026-10-24 05:03:00.002389+00
1a112fb2-a072-481e-9ce0-c1953b2ba014	351a51c5-55f2-4450-9bd0-c007d9e58b92	assignments/351a51c5-55f2-4450-9bd0-c007d9e58b92/b0a655d7-489d-4296-92dd-d32489fbb2a3.png	\N	2026-10-24 06:44:00.002431+00	2026-10-24 06:44:00.002432+00	2026-10-24 06:44:00.002432+00
1776111e-4483-4221-ba83-5fe135c604e0	426f1866-873e-4bdf-9491-d31ee86961c8	assignments/426f1866-873e-4bdf-9491-d31ee86961c8/0e6b6cb8-e27b-4f9d-be76-7d1a5c71effe.png	\N	2026-10-24 06:06:00.002314+00	2026-10-24 06:06:00.002314+00	2026-10-24 06:06:00.002314+00
4c594dd8-61f9-499a-a3bb-6ab68117dad9	c7de9b09-233d-4c46-961b-c85c97f37d36	assignments/c7de9b09-233d-4c46-961b-c85c97f37d36/8d82c83b-101b-4457-adad-c49b17530502.png	\N	2026-10-25 04:07:00.002161+00	2026-10-25 04:07:00.002161+00	2026-10-25 04:07:00.002161+00
f9ce05d7-d8bc-4ffc-a0db-fddcda1e0591	86806dca-45c2-40b9-93d4-635af3bc1210	assignments/86806dca-45c2-40b9-93d4-635af3bc1210/80329653-2b2a-46d9-8f15-3942ed2550b6.png	\N	2026-10-25 07:20:00.002483+00	2026-10-25 07:20:00.002484+00	2026-10-25 07:20:00.002484+00
3b7ba6eb-4e8c-408b-a3ea-b88a00dfce9b	8f9bac46-bdbf-4a4c-ad9f-520aa9a3c6c3	assignments/8f9bac46-bdbf-4a4c-ad9f-520aa9a3c6c3/a314aba4-3bb4-4b56-a714-0b8ca89d702a.png	\N	2026-10-25 06:21:00.002533+00	2026-10-25 06:21:00.002533+00	2026-10-25 06:21:00.002533+00
df9a6e71-4ced-4ff6-8f30-83703e2cd362	781a409e-bb49-41e5-9ef5-c31b82e60c1a	assignments/781a409e-bb49-41e5-9ef5-c31b82e60c1a/860f356e-50cd-44de-bb6f-4a211fb472cc.png	\N	2026-10-26 07:46:00.002614+00	2026-10-26 07:46:00.002614+00	2026-10-26 07:46:00.002614+00
c7030bef-b8ed-4620-b1dd-47232ab3b10f	2e3abb9c-966f-4ccb-8a4c-c8c803dbbd0c	assignments/2e3abb9c-966f-4ccb-8a4c-c8c803dbbd0c/ed54cf63-4b13-41ce-8ba5-b78950a031c6.png	\N	2026-10-26 04:21:00.002166+00	2026-10-26 04:21:00.002166+00	2026-10-26 04:21:00.002166+00
7e8f1563-efc3-4c0c-b18d-c09409a47e7e	511270ff-a015-40d5-af1d-c6c07bc2efd4	assignments/511270ff-a015-40d5-af1d-c6c07bc2efd4/8510e6ad-23f2-4fa8-8e49-77470934ea2a.png	\N	2026-10-26 05:43:00.002914+00	2026-10-26 05:43:00.002914+00	2026-10-26 05:43:00.002914+00
09ac9d8c-02c9-4413-b011-ca55fd51e6e2	30b699e7-7c1d-48e6-b33b-b9e77790f7ed	assignments/30b699e7-7c1d-48e6-b33b-b9e77790f7ed/33be14d2-c09d-4541-b5ff-a5d6c6c268b2.png	\N	2026-10-26 05:46:00.0029+00	2026-10-26 05:46:00.0029+00	2026-10-26 05:46:00.0029+00
ca458cb9-405c-4820-adc2-cb2b5a8a62ef	b5155c7c-a5eb-4ea4-a8bb-06a74fea1017	assignments/b5155c7c-a5eb-4ea4-a8bb-06a74fea1017/d5630611-e209-4925-bc94-d0700b51581a.png	\N	2026-10-27 05:54:00.002778+00	2026-10-27 05:54:00.002778+00	2026-10-27 05:54:00.002778+00
1d5ee53a-4f80-490e-a821-8e4974d26ab9	e1dde7e9-56f0-4e3e-a608-4207f790f4fe	assignments/e1dde7e9-56f0-4e3e-a608-4207f790f4fe/3c8a3167-689b-45de-b84b-b943f3f50ec2.png	\N	2026-10-27 06:32:00.002896+00	2026-10-27 06:32:00.002897+00	2026-10-27 06:32:00.002897+00
e1eaa016-865a-4cd6-a50d-c37bd87b23d6	9e00f2ff-8552-4113-b19d-3f04a2210b87	assignments/9e00f2ff-8552-4113-b19d-3f04a2210b87/f92a11d0-8352-49ee-9b1e-bcb932b95874.png	\N	2026-10-27 05:20:00.00242+00	2026-10-27 05:20:00.00242+00	2026-10-27 05:20:00.00242+00
9a05b440-062a-4abb-babf-afbbde02e71c	2af6eb17-418c-4d33-81ec-912221889f58	assignments/2af6eb17-418c-4d33-81ec-912221889f58/d1f59737-28f3-4d7b-be91-03ec1263f06f.png	\N	2026-10-27 04:30:00.003791+00	2026-10-27 04:30:00.003791+00	2026-10-27 04:30:00.003791+00
4864232e-33ea-461c-bf34-b1dcea547d5e	87015d47-7130-4533-8653-5408a531f634	assignments/87015d47-7130-4533-8653-5408a531f634/6c1f5ab5-cd28-4a83-92dd-6065165131ea.png	\N	2026-10-27 07:38:00.002498+00	2026-10-27 07:38:00.002498+00	2026-10-27 07:38:00.002498+00
e9ce0e54-ce02-4049-88e5-87b854ae0907	8bc8b72d-ac7a-4b51-8fc8-0c357a6adf45	assignments/8bc8b72d-ac7a-4b51-8fc8-0c357a6adf45/398ca3cb-51ba-434d-a704-c498002bd963.png	\N	2026-10-28 05:37:00.002088+00	2026-10-28 05:37:00.002088+00	2026-10-28 05:37:00.002088+00
2ad5bdce-3d67-43f1-a695-b382c8d83bf7	aa00e1cb-d75b-46a8-b527-ee74fef2739b	assignments/aa00e1cb-d75b-46a8-b527-ee74fef2739b/ffc7ba40-4a3d-41bc-b056-4eead3cabcd0.png	\N	2026-10-28 06:00:00.002173+00	2026-10-28 06:00:00.002173+00	2026-10-28 06:00:00.002173+00
40b100ed-3a0d-485f-9717-08a0ebd2df4c	6b60f403-06a4-4f20-a88a-c0298c5a46ff	assignments/6b60f403-06a4-4f20-a88a-c0298c5a46ff/0138632d-e12e-4864-8884-d5ed23f4d6de.png	\N	2026-10-28 07:21:00.002109+00	2026-10-28 07:21:00.002109+00	2026-10-28 07:21:00.002109+00
d5ef3bcb-b4cd-403c-a70c-74b18c9d81ce	cdcbea39-f909-44ce-95c2-6bb61b153d52	assignments/cdcbea39-f909-44ce-95c2-6bb61b153d52/f5df8108-3214-4b54-b873-83c918e83b92.png	\N	2026-10-29 08:07:00.002997+00	2026-10-29 08:07:00.002997+00	2026-10-29 08:07:00.002997+00
b6487ed4-2164-4e3b-aa41-8a9c434c23c6	9a742200-4824-4ba5-92dc-532166ef5dda	assignments/9a742200-4824-4ba5-92dc-532166ef5dda/96965f10-96ab-4ecc-874d-bf265d4ad421.png	\N	2026-10-29 07:20:00.002429+00	2026-10-29 07:20:00.002429+00	2026-10-29 07:20:00.002429+00
0ef4e935-8a72-42ee-9a87-8c853b378de9	3cd38393-9df4-4002-8e8a-961824cec45d	assignments/3cd38393-9df4-4002-8e8a-961824cec45d/15853277-c437-41a1-9882-97e83eb15e84.png	\N	2026-10-29 04:26:00.002546+00	2026-10-29 04:26:00.002546+00	2026-10-29 04:26:00.002546+00
cf976d98-8117-4f15-8367-8027c10e670a	1636e736-0749-4351-af97-38de26b4825b	assignments/1636e736-0749-4351-af97-38de26b4825b/c04b7e79-74be-465b-adac-7d0d62b9fe23.png	\N	2026-10-30 07:31:00.002156+00	2026-10-30 07:31:00.002156+00	2026-10-30 07:31:00.002156+00
b680b564-ae2b-4590-9d7b-dcbe37d94f7b	9d54121d-f1de-4026-8a9b-bdc0c2365f73	assignments/9d54121d-f1de-4026-8a9b-bdc0c2365f73/924402a5-1030-4290-a5c5-4d853cd7ece1.png	\N	2026-10-31 06:25:00.003008+00	2026-10-31 06:25:00.003008+00	2026-10-31 06:25:00.003008+00
02474698-de91-4db1-9618-8373fb720038	8c78b183-1928-4b5c-893d-972a51865f6c	assignments/8c78b183-1928-4b5c-893d-972a51865f6c/833a0027-8ad0-4920-8701-a52e81dc1195.png	\N	2026-10-31 05:54:00.003218+00	2026-10-31 05:54:00.003219+00	2026-10-31 05:54:00.003219+00
c522e3c2-35e5-4ace-bce3-f17a34f0b7ba	4c1b84fa-72b6-4e55-bf7e-16b042578fa6	assignments/4c1b84fa-72b6-4e55-bf7e-16b042578fa6/793d1f49-e388-433a-9609-7da01bad991d.png	\N	2026-10-31 05:29:00.003152+00	2026-10-31 05:29:00.003152+00	2026-10-31 05:29:00.003152+00
8e4a9609-ac75-4902-9c99-e164dd861f88	1dbd034a-ba0e-45a4-96fc-c22eb685a215	assignments/1dbd034a-ba0e-45a4-96fc-c22eb685a215/bb1fef53-389c-4f8b-b52b-09bcacc8ebf1.png	\N	2026-10-31 04:46:00.002735+00	2026-10-31 04:46:00.002735+00	2026-10-31 04:46:00.002735+00
9bb64779-a707-44dc-9bd4-d7da55bf43b8	70cc21aa-425d-4a62-9e3f-a3a52cee8d24	assignments/70cc21aa-425d-4a62-9e3f-a3a52cee8d24/abe872d5-e5b3-434b-8c08-c93683faf5c2.png	\N	2026-11-01 05:58:00.002656+00	2026-11-01 05:58:00.002656+00	2026-11-01 05:58:00.002656+00
1639136d-d44d-495e-b598-9df2a4cc92b5	719739d7-0098-4188-87af-50ac40cbe1b3	assignments/719739d7-0098-4188-87af-50ac40cbe1b3/823b34ab-b291-4050-8efa-6796cb64cb1d.png	\N	2026-11-01 04:29:00.002726+00	2026-11-01 04:29:00.002726+00	2026-11-01 04:29:00.002726+00
9d849445-baa5-49c9-90bf-7dc424682087	887f94a5-4c0a-4d3f-8cd8-e23b2f536303	assignments/887f94a5-4c0a-4d3f-8cd8-e23b2f536303/8abb3a26-061b-4a24-9a62-735698693a22.png	\N	2026-11-01 07:12:00.002812+00	2026-11-01 07:12:00.002812+00	2026-11-01 07:12:00.002812+00
932f0d78-6c32-4fba-b0f6-0a65a382a8c5	87b5bc3b-ff2e-4cde-999f-c1cc3416b1aa	assignments/87b5bc3b-ff2e-4cde-999f-c1cc3416b1aa/9cbd1a6b-26b2-4d7d-bc4b-d02afa702b98.png	\N	2026-11-01 05:18:00.002157+00	2026-11-01 05:18:00.002157+00	2026-11-01 05:18:00.002157+00
146bc99b-e680-41fc-883f-ab6e5a9f5091	54302b37-069b-427b-b459-9f95646a75cf	assignments/54302b37-069b-427b-b459-9f95646a75cf/b6aacba1-00aa-48f2-b974-76e6b2f27fab.png	\N	2026-11-01 04:30:00.002664+00	2026-11-01 04:30:00.002664+00	2026-11-01 04:30:00.002664+00
1f0d5757-5197-4783-8570-3b147b308203	07c13299-fc40-4167-8d45-08d48af97e32	assignments/07c13299-fc40-4167-8d45-08d48af97e32/49ed7701-33d5-4a78-8cd9-b29a1b0db546.png	\N	2026-11-02 03:45:00.002574+00	2026-11-02 03:45:00.002574+00	2026-11-02 03:45:00.002574+00
973cbe8b-4c87-48d4-a9a6-0b2678329b9c	0941e14d-80d7-4f2c-b6bd-d091ec3c8f6f	assignments/0941e14d-80d7-4f2c-b6bd-d091ec3c8f6f/ae6e7fa8-e4a6-4ac7-ab29-7a6cf13a2ad1.png	\N	2026-11-04 08:09:00.002473+00	2026-11-04 08:09:00.002473+00	2026-11-04 08:09:00.002473+00
ca39bbd4-276b-4ff0-ad5f-b73f989c719a	892fd24b-a486-47ef-8702-d8a995b5fccb	assignments/892fd24b-a486-47ef-8702-d8a995b5fccb/f29ceb7c-087d-48ea-b532-129895967dbe.png	\N	2026-11-04 05:32:00.00182+00	2026-11-04 05:32:00.00182+00	2026-11-04 05:32:00.001821+00
32b67c2e-b697-446f-a5ee-20542173e394	b0bb373c-ddd1-4eda-acf3-58b18c274348	assignments/b0bb373c-ddd1-4eda-acf3-58b18c274348/28edb491-7ada-4c43-a3bf-5440b99e21ce.png	\N	2026-11-04 06:11:00.002601+00	2026-11-04 06:11:00.002601+00	2026-11-04 06:11:00.002601+00
43e58a36-7ad6-4c07-b506-8f8cf8d2b94a	fe5bbeba-5351-4527-b91f-a93ac044fd32	assignments/fe5bbeba-5351-4527-b91f-a93ac044fd32/58c46427-02b0-4e29-b46b-55a2500d5bd1.png	\N	2026-11-05 08:50:00.002257+00	2026-11-05 08:50:00.002257+00	2026-11-05 08:50:00.002257+00
6ecb2dc8-16f1-49fa-b9aa-182e9d68727c	a577e667-d29a-48b2-8263-11ed23ad9e1f	assignments/a577e667-d29a-48b2-8263-11ed23ad9e1f/ba8274f6-d63c-49fc-82cb-1d80a173580e.png	\N	2026-11-05 06:09:00.002673+00	2026-11-05 06:09:00.002673+00	2026-11-05 06:09:00.002673+00
61596abe-fa70-455a-b984-b9efab8debfb	4945d7d4-11b5-43fc-8054-97394e688b00	assignments/4945d7d4-11b5-43fc-8054-97394e688b00/4b45c09f-96a0-47f3-8062-e0c31c9acafa.png	\N	2026-11-05 08:40:00.002426+00	2026-11-05 08:40:00.002426+00	2026-11-05 08:40:00.002426+00
d4d0dbde-9154-435d-ba59-2c7a570695b8	883cfcff-0f1e-4988-a2aa-180ee702de0b	assignments/883cfcff-0f1e-4988-a2aa-180ee702de0b/dd5f3cb5-8e76-4646-9441-53f99ba37f61.png	\N	2026-11-05 06:38:00.002351+00	2026-11-05 06:38:00.002351+00	2026-11-05 06:38:00.002351+00
7dd430e2-0b79-421b-bd17-9951c6a54d5c	0020f9d0-b315-435a-95b1-55eb979c4532	assignments/0020f9d0-b315-435a-95b1-55eb979c4532/247979a1-83da-459e-9c01-23a23ee5d0f9.png	\N	2026-11-06 06:30:00.002056+00	2026-11-06 06:30:00.002056+00	2026-11-06 06:30:00.002056+00
b12c8b54-ed05-4a45-92ed-4aba144ccfcc	245c1539-5e99-4236-8e58-daccfa83d46e	assignments/245c1539-5e99-4236-8e58-daccfa83d46e/307daca4-6f19-4324-bef9-e9e1008e830a.png	\N	2026-11-07 07:18:00.005021+00	2026-11-07 07:18:00.005021+00	2026-11-07 07:18:00.005021+00
2147ffb1-ca6b-4294-9115-1002ff1c37b9	6bcc0524-1a29-4895-8318-341b5496cf91	assignments/6bcc0524-1a29-4895-8318-341b5496cf91/a4f88b08-78e9-49d9-a973-6ecaff23f308.png	\N	2026-11-07 06:56:00.002677+00	2026-11-07 06:56:00.002677+00	2026-11-07 06:56:00.002677+00
a9fa3dc0-e1ed-40b2-b816-b8f190647222	c821b664-7312-4419-9ffe-08d41933e4b0	assignments/c821b664-7312-4419-9ffe-08d41933e4b0/f45293ff-1daa-4f65-8242-dd04df31bb7b.png	\N	2026-11-07 07:06:00.00196+00	2026-11-07 07:06:00.00196+00	2026-11-07 07:06:00.00196+00
4e02d045-c350-43ec-89fa-8a06e5c0d943	4add9a40-7f67-42db-9cbe-200a5733f320	assignments/4add9a40-7f67-42db-9cbe-200a5733f320/40ea44b5-93db-4a5b-a522-ef1a16507da5.png	\N	2026-11-08 05:24:00.002288+00	2026-11-08 05:24:00.002288+00	2026-11-08 05:24:00.002288+00
e3a043b9-822a-4df9-9bc5-7b53cc9b1672	ff86c1d6-b0e2-426d-a472-72362b628343	assignments/ff86c1d6-b0e2-426d-a472-72362b628343/4dee4b8e-e243-4153-b7f7-0fc9d98ea162.png	\N	2026-11-08 05:48:00.001909+00	2026-11-08 05:48:00.001909+00	2026-11-08 05:48:00.001909+00
afb4bc46-b7f5-4bd6-b88c-1ae613bb0f18	5329dbde-2dd6-46e7-8cd1-4ff21a1f3e1b	assignments/5329dbde-2dd6-46e7-8cd1-4ff21a1f3e1b/b112f615-206b-4c2b-9181-f2c8f664121f.png	\N	2026-11-08 06:22:00.002515+00	2026-11-08 06:22:00.002515+00	2026-11-08 06:22:00.002515+00
7bc32db5-db33-47fc-8227-46fe6b1e45e1	11fa5ad8-ff5e-4e12-9711-38289c4fd43d	assignments/11fa5ad8-ff5e-4e12-9711-38289c4fd43d/7dae9bad-7bf1-4d68-bf03-7e99a9a63dbb.png	\N	2026-11-08 07:31:00.00258+00	2026-11-08 07:31:00.00258+00	2026-11-08 07:31:00.00258+00
5ea6337a-14eb-4c82-bac6-2f6f00c2d421	3a7d773b-84f9-431a-a6e6-a141612cf129	assignments/3a7d773b-84f9-431a-a6e6-a141612cf129/88cdfc66-eeb2-4123-b844-fbaed4acf07a.png	\N	2026-11-07 23:01:00.002719+00	2026-11-07 23:01:00.00272+00	2026-11-07 23:01:00.00272+00
6b65b6af-f09e-41f7-b415-229a3d528c15	85af3ae4-9288-458e-8256-4eb50a981455	assignments/85af3ae4-9288-458e-8256-4eb50a981455/d1d4b6bf-2450-4420-b82e-2cef8e81b9f2.png	\N	2026-11-09 06:59:00.002273+00	2026-11-09 06:59:00.002273+00	2026-11-09 06:59:00.002273+00
5dcda382-cb55-429d-9dbd-54293303e4cc	aa422e43-0be6-45f7-97d2-818e8017d472	assignments/aa422e43-0be6-45f7-97d2-818e8017d472/bbca90cd-abd1-427e-9cc4-feac9a953dd3.png	\N	2026-11-09 04:28:00.002273+00	2026-11-09 04:28:00.002273+00	2026-11-09 04:28:00.002273+00
a2faf8ac-6e28-403b-acaf-99ea1d864bf4	4a8bf17f-2dcb-4ce2-95e2-1ceeabbfe519	assignments/4a8bf17f-2dcb-4ce2-95e2-1ceeabbfe519/ea45899e-1c1f-4355-9a01-b12ee6fb6396.png	\N	2026-11-10 05:08:00.002408+00	2026-11-10 05:08:00.002408+00	2026-11-10 05:08:00.002408+00
b1b9d458-3aba-4870-ae7f-473e6a248a4f	0d23581c-8fd0-4e76-8249-a151d37689a6	assignments/0d23581c-8fd0-4e76-8249-a151d37689a6/d80df588-2a54-46e8-80f7-9defaee7914d.png	\N	2026-11-10 04:33:00.004081+00	2026-11-10 04:33:00.004081+00	2026-11-10 04:33:00.004081+00
8f2fc8d7-3b91-4a13-8a7b-a0e21923526b	42d621d5-4f2a-41d6-80f8-9142f2a78c38	assignments/42d621d5-4f2a-41d6-80f8-9142f2a78c38/b0b2e3f2-8db5-4d1a-ac10-9ca1bca957a4.png	\N	2026-11-10 05:36:00.003124+00	2026-11-10 05:36:00.003124+00	2026-11-10 05:36:00.003124+00
dd653e6a-edd2-434c-86e3-6d65a141a976	320e44da-9982-4d41-b92d-a8d99d520d47	assignments/320e44da-9982-4d41-b92d-a8d99d520d47/bf08f085-3303-405a-9a8d-ec2af11cfe46.png	\N	2026-11-11 05:12:00.002802+00	2026-11-11 05:12:00.002802+00	2026-11-11 05:12:00.002802+00
6eeabc15-2c75-40dd-8d71-ab6aaaed1971	eea039c9-bfa6-41d0-9711-18647709c608	assignments/eea039c9-bfa6-41d0-9711-18647709c608/4691bebb-f1c8-4ff7-9927-0428cbff9044.png	\N	2026-11-11 05:46:00.003368+00	2026-11-11 05:46:00.003368+00	2026-11-11 05:46:00.003368+00
4c7758fe-2c9a-4535-8432-a64e06feb4cc	26a8f3f1-d009-425d-92d4-76611c39d062	assignments/26a8f3f1-d009-425d-92d4-76611c39d062/94e7458f-2272-489a-9e5c-a78ac01476b9.png	\N	2026-11-11 05:45:00.002243+00	2026-11-11 05:45:00.002243+00	2026-11-11 05:45:00.002243+00
c9dc09c0-91ef-41cf-91a3-1191f7b890c5	ef0ad32d-adcb-472c-a56a-126f399322cf	assignments/ef0ad32d-adcb-472c-a56a-126f399322cf/84006640-d118-46ff-850f-8b845b80e665.png	\N	2026-11-10 12:28:00.002364+00	2026-11-10 12:28:00.002364+00	2026-11-10 12:28:00.002364+00
051b26d9-224a-4884-8ba2-481929d78c0e	79a2b6ca-c31b-4592-8888-e648893c93a0	assignments/79a2b6ca-c31b-4592-8888-e648893c93a0/14777371-cfd7-4fc6-93a5-cb9f567ae43e.png	\N	2026-11-11 05:03:00.002006+00	2026-11-11 05:03:00.002006+00	2026-11-11 05:03:00.002006+00
4bd9e05e-6c95-4ef5-a21f-7e64de899484	5bbf2fe9-542a-4826-a4f5-2050b0d83fa4	assignments/5bbf2fe9-542a-4826-a4f5-2050b0d83fa4/171da91f-ce9a-423a-9891-c3f1eafa47a7.png	\N	2026-11-11 04:40:00.002086+00	2026-11-11 04:40:00.002086+00	2026-11-11 04:40:00.002086+00
e46c3cb1-cf4a-4a96-9f56-4b01d196aaa1	ca866be9-1d84-41c4-9008-bf74d2e4ed14	assignments/ca866be9-1d84-41c4-9008-bf74d2e4ed14/fdc69ef4-d35d-4d67-a8a4-eed0e2d3d241.png	\N	2026-11-11 07:14:00.002003+00	2026-11-11 07:14:00.002003+00	2026-11-11 07:14:00.002003+00
60ab16ea-e201-4596-abb8-a1735a3fee26	f93458d6-398a-4af8-893b-3c0d127614ea	assignments/f93458d6-398a-4af8-893b-3c0d127614ea/c2cb76c9-43cb-48bd-9334-0ecf394ce385.png	\N	2026-11-11 04:37:00.001668+00	2026-11-11 04:37:00.001668+00	2026-11-11 04:37:00.001668+00
a5d0d929-7905-4716-b0a7-3cf632bcacd9	9b4bc778-9d64-4b5a-a9f0-6f75932692ee	assignments/9b4bc778-9d64-4b5a-a9f0-6f75932692ee/ed5fb0ae-d24f-4c3d-b992-fb3f7c180a91.png	\N	2026-11-12 03:54:00.002552+00	2026-11-12 03:54:00.002552+00	2026-11-12 03:54:00.002552+00
f214a3d7-99c8-4a45-98be-f41aa4045b9e	1c89a980-ea29-4dbf-8fa7-5a7111aaf845	assignments/1c89a980-ea29-4dbf-8fa7-5a7111aaf845/bf867d44-fb7d-489c-b2d7-8840d3ec6602.png	\N	2026-11-12 06:55:00.002305+00	2026-11-12 06:55:00.002305+00	2026-11-12 06:55:00.002305+00
cf173745-2147-4b73-83f9-f368ff3957a7	8c557cad-9302-4b8f-803c-2168426ae741	assignments/8c557cad-9302-4b8f-803c-2168426ae741/34bab6c1-8c87-4d25-b3c8-d4fd3729b716.png	\N	2026-11-11 12:44:00.002291+00	2026-11-11 12:44:00.002291+00	2026-11-11 12:44:00.002291+00
69d7b3a9-3d4a-4bf4-b23e-6b19299161f9	a253d578-e21b-4029-9974-d15746f03bc8	assignments/a253d578-e21b-4029-9974-d15746f03bc8/fb06d950-b547-4006-a99d-2c66c43a4ae1.png	\N	2026-11-12 05:23:00.002252+00	2026-11-12 05:23:00.002252+00	2026-11-12 05:23:00.002252+00
a4c06123-7eca-421e-8a1b-25aaed89abeb	8b67ad31-556f-41ac-9156-fa9073f02264	assignments/8b67ad31-556f-41ac-9156-fa9073f02264/df28af61-3691-4f1a-a676-bca04719f0b4.png	\N	2026-11-13 07:09:00.001951+00	2026-11-13 07:09:00.001951+00	2026-11-13 07:09:00.001951+00
8189a784-2a60-4a95-a8e7-eea4d62df166	3257c2fe-f334-443f-8061-84daa269f35d	assignments/3257c2fe-f334-443f-8061-84daa269f35d/9ad45656-bf21-4976-b017-08116652e3bd.png	\N	2026-11-13 05:47:00.00237+00	2026-11-13 05:47:00.00237+00	2026-11-13 05:47:00.00237+00
ce42a3d5-e628-4924-8b96-c3d63f46a5a8	01dee7a5-8c2f-44e8-a588-1bba0a3d5186	assignments/01dee7a5-8c2f-44e8-a588-1bba0a3d5186/244fc15c-bdaf-4f4d-9e82-e308f02e26c7.png	\N	2026-11-13 05:34:00.002095+00	2026-11-13 05:34:00.002095+00	2026-11-13 05:34:00.002095+00
ab8d97db-3d49-4715-9f88-af8d5020b78b	e1a50b90-1098-4dea-a646-1daeee4b84bd	assignments/e1a50b90-1098-4dea-a646-1daeee4b84bd/010b3550-40aa-4e00-af95-d288237ffccf.png	\N	2026-11-14 04:54:00.001714+00	2026-11-14 04:54:00.001714+00	2026-11-14 04:54:00.001714+00
338d799e-71f5-47b5-b7f2-b299a16621be	b9e69f4b-8c59-4506-9c4d-3d33aa2b400b	assignments/b9e69f4b-8c59-4506-9c4d-3d33aa2b400b/3b508165-968d-4c88-aac1-9b182c900f8e.png	\N	2026-11-14 08:28:00.002286+00	2026-11-14 08:28:00.002286+00	2026-11-14 08:28:00.002287+00
65f4a680-d27c-48a7-a1ff-6fc974e9064b	08f343e8-9522-40ce-8cd4-6a90e2354f5e	assignments/08f343e8-9522-40ce-8cd4-6a90e2354f5e/1b5392bb-8f11-4b64-a9fd-6d36dd49c9eb.png	\N	2026-11-14 05:37:00.002316+00	2026-11-14 05:37:00.002316+00	2026-11-14 05:37:00.002316+00
7a126298-23f8-4dc5-9bee-b34ab7a6ffaf	7135d6ca-cd53-449d-8f30-4c64f7797e40	assignments/7135d6ca-cd53-449d-8f30-4c64f7797e40/51b67977-7963-4484-b80a-a65c6472102e.png	\N	2026-11-14 09:39:00.002341+00	2026-11-14 09:39:00.002341+00	2026-11-14 09:39:00.002341+00
ad8d9ffb-5095-4ead-8d7a-bbc4254edfbf	221c4ab3-90c4-4b7e-bf02-e415bddd01c3	assignments/221c4ab3-90c4-4b7e-bf02-e415bddd01c3/71725d68-5c2f-4ca5-b2dc-c10abe696a30.png	\N	2026-10-18 05:55:00.00171+00	2026-10-18 05:55:00.001711+00	2026-10-18 05:55:00.001711+00
9a6ac13b-eb56-48e8-a722-921531dae058	b241f5c3-c09a-40d1-8c77-90dbae91e51a	assignments/b241f5c3-c09a-40d1-8c77-90dbae91e51a/1f35cd73-a2a0-4119-8bfe-1aaed74f5114.png	\N	2026-10-21 07:41:00.002496+00	2026-10-21 07:41:00.002496+00	2026-10-21 07:41:00.002496+00
d32feb0b-c075-44a5-b767-b5f5c6ef5df9	a83184a9-286a-44b7-bd91-87bfe920b728	assignments/a83184a9-286a-44b7-bd91-87bfe920b728/529d6d9f-9489-41f6-9af7-6aef6fe54c8c.png	\N	2026-10-21 08:04:00.002997+00	2026-10-21 08:04:00.002997+00	2026-10-21 08:04:00.002997+00
bea1bb22-3e2f-4b33-98ab-a462732b4d3e	7f040a01-2b31-4754-8f88-ac7e082b58fe	assignments/7f040a01-2b31-4754-8f88-ac7e082b58fe/0003c8c2-26db-49c8-bbe4-a3ed2e30053f.png	\N	2026-10-22 06:58:00.00317+00	2026-10-22 06:58:00.00317+00	2026-10-22 06:58:00.00317+00
f69634f9-b2e5-49da-9e6c-2dd682d8d533	ef4b7a25-b819-4de5-8833-e176551111cc	assignments/ef4b7a25-b819-4de5-8833-e176551111cc/27468363-4fc4-4766-8a89-11624210c572.png	\N	2026-10-22 07:19:00.001549+00	2026-10-22 07:19:00.00155+00	2026-10-22 07:19:00.00155+00
632841e8-cfce-4bdb-99ae-eff8f1989b25	67714d4d-d065-4902-b1e7-6f0148c077b6	assignments/67714d4d-d065-4902-b1e7-6f0148c077b6/607bf3d3-bf41-47a3-b0be-bffd1902860f.png	\N	2026-10-23 05:13:00.002354+00	2026-10-23 05:13:00.002354+00	2026-10-23 05:13:00.002354+00
c20c9f27-9cef-450f-915f-3ee1041f71c3	e28db3f4-6e27-4967-b409-653d8fb55c05	assignments/e28db3f4-6e27-4967-b409-653d8fb55c05/e6470d50-ed05-44d3-8444-a0f82a2dcb67.png	\N	2026-10-23 05:00:00.001752+00	2026-10-23 05:00:00.001752+00	2026-10-23 05:00:00.001752+00
092a070f-e851-4214-b674-b5eb31c3f665	a2e53980-8b54-4cff-8bb6-559e2d1460eb	assignments/a2e53980-8b54-4cff-8bb6-559e2d1460eb/2b0f8b50-54e2-4ca9-8ede-5cfea3199878.png	\N	2026-10-24 08:14:00.002543+00	2026-10-24 08:14:00.002543+00	2026-10-24 08:14:00.002543+00
0bb16c70-564e-4714-ba3e-19aeb39bf602	1fd3fe72-5207-4f4a-bbc8-47af3dd728ad	assignments/1fd3fe72-5207-4f4a-bbc8-47af3dd728ad/b0bd24e9-7f66-48c1-8669-e66dfd6e040d.png	\N	2026-10-24 05:54:00.001722+00	2026-10-24 05:54:00.001722+00	2026-10-24 05:54:00.001722+00
e7bebfc2-879b-4eec-8229-3a5922cf9485	52e85093-4761-463f-b8e1-a200cefe427f	assignments/52e85093-4761-463f-b8e1-a200cefe427f/dd18f7cd-16e6-410b-b231-88774e421c1a.png	\N	2026-10-24 04:23:00.00236+00	2026-10-24 04:23:00.00236+00	2026-10-24 04:23:00.00236+00
0a8d951f-1672-4e46-aeb7-21c69fb91d27	530e138b-6ba8-4960-bf6a-96fa105ebee2	assignments/530e138b-6ba8-4960-bf6a-96fa105ebee2/c835f236-1c0d-4592-8891-f812ab942cd1.png	\N	2026-10-25 07:23:00.001611+00	2026-10-25 07:23:00.001611+00	2026-10-25 07:23:00.001611+00
09d8f812-ec86-4d47-ae19-a53de209ab3d	f1d6bd52-4e26-4838-ba7a-9b1b0e404077	assignments/f1d6bd52-4e26-4838-ba7a-9b1b0e404077/96229fac-ef69-4bf1-89ad-3810ad1b4068.png	\N	2026-10-25 05:49:00.002383+00	2026-10-25 05:49:00.002383+00	2026-10-25 05:49:00.002383+00
58def8be-d4aa-4cee-b193-ff3b32b7e2d8	38664095-19b5-4ab9-aceb-8d75db27bee1	assignments/38664095-19b5-4ab9-aceb-8d75db27bee1/feaaaf6f-e330-4cc1-b624-2825a5fa9cf2.png	\N	2026-10-25 06:45:00.001706+00	2026-10-25 06:45:00.001706+00	2026-10-25 06:45:00.001706+00
6fb9e27e-9d3e-456e-bd40-c6adf14ff6ca	6ae1896d-d662-496f-b93c-c52876cdbd00	assignments/6ae1896d-d662-496f-b93c-c52876cdbd00/a34e2955-42a2-4922-b1f7-c613748f781a.png	\N	2026-10-25 07:59:00.002057+00	2026-10-25 07:59:00.002057+00	2026-10-25 07:59:00.002057+00
ed8fb6c9-fb17-40c2-920e-36e7c2a36683	8481c832-f930-42f7-9d62-e29adc1e0007	assignments/8481c832-f930-42f7-9d62-e29adc1e0007/745f2454-62fa-44bf-b26e-b7d991eb5586.png	\N	2026-10-26 07:10:00.002154+00	2026-10-26 07:10:00.002154+00	2026-10-26 07:10:00.002154+00
3f02e196-aacf-49a2-ac4a-918e0a4697c9	27fc3d11-b6e4-4983-a082-fb43079302b4	assignments/27fc3d11-b6e4-4983-a082-fb43079302b4/a7d56f75-a44a-40a8-9c6b-b5052ce772a6.png	\N	2026-10-26 06:13:00.001677+00	2026-10-26 06:13:00.001677+00	2026-10-26 06:13:00.001677+00
7a5f26ad-104e-4f4b-9557-a7950787a281	1250a667-6b45-4bde-8024-3e55a994b922	assignments/1250a667-6b45-4bde-8024-3e55a994b922/154b0011-be52-4f86-a9b2-bfffdcb771a5.png	\N	2026-10-26 07:24:00.001992+00	2026-10-26 07:24:00.001992+00	2026-10-26 07:24:00.001992+00
baf71b50-9fe7-4bfd-bc28-0e92b911671b	51e50060-90af-42c5-878b-636bf619f517	assignments/51e50060-90af-42c5-878b-636bf619f517/f87c7e2d-a0c3-4db8-bc45-4b51b02de19e.png	\N	2026-10-27 04:22:00.00276+00	2026-10-27 04:22:00.00276+00	2026-10-27 04:22:00.00276+00
b1264dfa-4cdc-4312-bfa6-eb194616b1a1	134eb802-d376-42b9-82c7-3e070cba85c4	assignments/134eb802-d376-42b9-82c7-3e070cba85c4/f059e205-021a-40f1-8572-52a0bebbf43c.png	\N	2026-10-27 05:01:00.002216+00	2026-10-27 05:01:00.002216+00	2026-10-27 05:01:00.002216+00
a23df305-6097-4c7d-ae8f-2ce1e680b09c	993719a8-e42e-4fc9-bf26-34ff67ddc7b7	assignments/993719a8-e42e-4fc9-bf26-34ff67ddc7b7/80025d5a-3c46-4191-a13d-173d76a44a2d.png	\N	2026-10-28 05:48:00.002759+00	2026-10-28 05:48:00.002759+00	2026-10-28 05:48:00.002759+00
6faf9622-31bc-4d82-8269-9dbf52b2e70d	83e40dc2-f5dc-453f-8e45-685e19784912	assignments/83e40dc2-f5dc-453f-8e45-685e19784912/1730b4cf-b211-4201-ae09-b49b082e64a3.png	\N	2026-10-29 07:00:00.001901+00	2026-10-29 07:00:00.001901+00	2026-10-29 07:00:00.001901+00
1d4c1edd-8e83-4c9d-8232-63bf9a1a0ee2	883d6f35-1c68-4c30-bb27-97d4100fe9a2	assignments/883d6f35-1c68-4c30-bb27-97d4100fe9a2/b0322aba-2ba5-49ac-960c-e8493a53de53.png	\N	2026-10-30 05:47:00.001538+00	2026-10-30 05:47:00.001538+00	2026-10-30 05:47:00.001538+00
be06bc0d-f5a4-4302-b66f-7dd8677c3b2d	bb6257bf-8e12-4cb0-943c-778590c294ba	assignments/bb6257bf-8e12-4cb0-943c-778590c294ba/3b1968a1-9b54-4ef3-a4d5-4b8307b663c7.png	\N	2026-10-31 07:37:00.001748+00	2026-10-31 07:37:00.001748+00	2026-10-31 07:37:00.001748+00
cb5fb3c7-77af-4dcb-9129-3739a557c6aa	75d3b028-f62d-4cad-9a47-f48267232a59	assignments/75d3b028-f62d-4cad-9a47-f48267232a59/555c4895-6d54-4465-ad71-f48d8cd68f27.png	\N	2026-11-02 05:03:00.00228+00	2026-11-02 05:03:00.00228+00	2026-11-02 05:03:00.00228+00
9d0de0bc-9d3d-4496-b8cc-27c2abd38edf	de5ac3f2-cf77-4671-8243-0d49cf37d25a	assignments/de5ac3f2-cf77-4671-8243-0d49cf37d25a/1bbb3683-4a6f-4dc0-948c-60a7a309d651.png	\N	2026-11-02 08:20:00.001856+00	2026-11-02 08:20:00.001857+00	2026-11-02 08:20:00.001857+00
58699da5-5465-46d0-a79f-bf23852847f1	5716e283-5c5a-432d-a000-00da43b93100	assignments/5716e283-5c5a-432d-a000-00da43b93100/35c1169c-5fec-4ea2-8aa2-c795779cedc1.png	\N	2026-11-02 03:57:00.001951+00	2026-11-02 03:57:00.001951+00	2026-11-02 03:57:00.001951+00
766a8d81-33f9-4dd3-ac87-64247ceec768	243acb35-0a94-48f4-be50-73fe43ceba8c	assignments/243acb35-0a94-48f4-be50-73fe43ceba8c/dc6bdaa2-ce50-499b-a146-5be8ee882897.png	\N	2026-11-02 04:51:00.001438+00	2026-11-02 04:51:00.001438+00	2026-11-02 04:51:00.001438+00
a573a339-097a-4e22-a4d2-4f42f97ba343	6aa18f9d-07dc-449e-acb8-a6e91a27df45	assignments/6aa18f9d-07dc-449e-acb8-a6e91a27df45/774feffe-7508-4c66-aa14-9d978d9a844b.png	\N	2026-11-03 06:38:00.00366+00	2026-11-03 06:38:00.003661+00	2026-11-03 06:38:00.003661+00
21d9b958-11aa-4c3b-8c83-f43505cc6ea7	00ae9f3d-8831-46ac-bd5f-28720b0dff1b	assignments/00ae9f3d-8831-46ac-bd5f-28720b0dff1b/58272420-dc71-41fd-91df-3562c3dd316f.png	\N	2026-11-04 06:51:00.002676+00	2026-11-04 06:51:00.002676+00	2026-11-04 06:51:00.002676+00
ea1fdfff-0609-4eec-b341-c27b5d216331	902d846e-35c9-4692-bad5-de32cb223d98	assignments/902d846e-35c9-4692-bad5-de32cb223d98/23fc4a0b-f947-4cfc-b3f6-a4d9221d3a63.png	\N	2026-11-04 06:33:00.002062+00	2026-11-04 06:33:00.002062+00	2026-11-04 06:33:00.002062+00
2bcb42ad-65cd-42a9-a55d-7f636c016d88	41022151-bd76-4a6b-b3e3-04ff004439fc	assignments/41022151-bd76-4a6b-b3e3-04ff004439fc/0cf42b88-f545-4f15-b476-d61ef2eab52b.png	\N	2026-11-06 07:20:00.002348+00	2026-11-06 07:20:00.002348+00	2026-11-06 07:20:00.002348+00
e53ab633-2c87-414d-ad55-c5e379c896ee	30385b0d-a8c7-4a5f-a80c-8ce3116c28e6	assignments/30385b0d-a8c7-4a5f-a80c-8ce3116c28e6/83210211-729f-4edd-a513-ef4f4773c8be.png	\N	2026-11-08 07:05:00.002233+00	2026-11-08 07:05:00.002233+00	2026-11-08 07:05:00.002233+00
18feaca5-a757-4dbb-aee3-bfeca74b1739	9880fe5e-4221-4f94-9eef-add92f6026aa	assignments/9880fe5e-4221-4f94-9eef-add92f6026aa/1925a3d6-45db-4450-80ce-b146e34b5777.png	\N	2026-11-08 06:46:00.002396+00	2026-11-08 06:46:00.002396+00	2026-11-08 06:46:00.002396+00
1bbcd2d0-93e7-4b24-a875-acf619c4a9c1	1fc3b69d-98ba-46ec-9d75-70ee82516726	assignments/1fc3b69d-98ba-46ec-9d75-70ee82516726/d056ec8a-517c-437c-aabe-31cb49300c39.png	\N	2026-11-09 03:38:00.001889+00	2026-11-09 03:38:00.00189+00	2026-11-09 03:38:00.00189+00
3810c40e-51a5-4934-abf5-c0ef51dfce64	c3b1f546-2dc6-41de-afb1-4bed89fe39bc	assignments/c3b1f546-2dc6-41de-afb1-4bed89fe39bc/3ef99830-911c-42a7-83ee-9e6a6c8079e6.png	\N	2026-11-10 07:39:00.002035+00	2026-11-10 07:39:00.002035+00	2026-11-10 07:39:00.002035+00
6c5ca03a-35ee-468c-83ab-c26b32b324f9	84fe6a14-3b12-4603-89a4-d50f5198a3a4	assignments/84fe6a14-3b12-4603-89a4-d50f5198a3a4/7913744f-1789-4f2e-bd55-2d9323e60f7a.png	\N	2026-11-11 07:38:00.001456+00	2026-11-11 07:38:00.001456+00	2026-11-11 07:38:00.001456+00
afd3be15-6565-4be0-9676-e8d33cbc4c27	eac860b6-890c-4274-8072-587c0151fa4b	assignments/eac860b6-890c-4274-8072-587c0151fa4b/3a303dc0-3ca3-4e00-85cc-58f30bda496b.png	\N	2026-11-12 05:05:00.002198+00	2026-11-12 05:05:00.002198+00	2026-11-12 05:05:00.002198+00
a996b2c0-9e0a-4981-bd68-321488dbd888	227d5152-1738-4162-b5a1-54ccd1fe5fa3	assignments/227d5152-1738-4162-b5a1-54ccd1fe5fa3/847d5266-460e-44e8-a7b3-fc8fa8705b3a.png	\N	2026-11-14 07:05:00.004009+00	2026-11-14 07:05:00.004009+00	2026-11-14 07:05:00.004009+00
dc8601ea-6327-4a97-9ffc-8365ee18b574	77768888-7ebc-400c-8e95-4e76676bd6a5	assignments/77768888-7ebc-400c-8e95-4e76676bd6a5/99963f74-6ee0-4cd6-8430-8440fc2d0d44.png	\N	2026-11-14 06:52:00.002042+00	2026-11-14 06:52:00.002042+00	2026-11-14 06:52:00.002042+00
\.


--
-- Data for Name: flyway_schema_history; Type: TABLE DATA; Schema: public; Owner: dispatch
--

COPY public.flyway_schema_history (installed_rank, version, description, type, script, checksum, installed_by, installed_on, execution_time, success) FROM stdin;
1	1	create dispatch tables	SQL	V1__create_dispatch_tables.sql	622169515	dispatch	2026-11-14 10:16:59.743083	68	t
2	2	add parcel details to assignments	SQL	V2__add_parcel_details_to_assignments.sql	-174379231	dispatch	2026-11-14 10:16:59.84134	5	t
3	3	add parcel status to assignments	SQL	V3__add_parcel_status_to_assignments.sql	1408546475	dispatch	2026-11-14 10:16:59.858426	1	t
4	4	seed demo courier	SQL	V4__seed_demo_courier.sql	-357519094	dispatch	2026-11-14 10:16:59.865101	2	t
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

\unrestrict OUGpiIndkXLR0FYUYhSGweU348X6T4t1BIsKodjsLgsHv9nZV4z9ER1NVdDilxZ

