-- Demo courier for the end-to-end demo (dispatcher assigns → courier delivers → customer notified).
-- user_id is the auth-service user ID of the demo COURIER account; auth-service seeds that user with the same
-- fixed ID. Without this row the courier screens answer 404 "courier profile not found".
INSERT INTO couriers (id, user_id, status)
VALUES ('00000000-0000-4000-8000-00000000d001', '00000000-0000-4000-8000-00000000c001', 'AVAILABLE')
ON CONFLICT (user_id) DO NOTHING;
