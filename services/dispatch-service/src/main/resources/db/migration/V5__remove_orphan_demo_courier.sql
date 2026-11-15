-- V4 seeded a demo courier for a user ID that no auth-service account has, so nobody could sign in as it, yet it
-- showed in the workload and could be given parcels. Couriers are now registered when a courier account first opens
-- its screens, so the seed is not needed. Removed only if no parcel was ever assigned to it.
DELETE FROM couriers c
WHERE c.id = '00000000-0000-4000-8000-00000000d001'
  AND NOT EXISTS (SELECT 1 FROM assignments a WHERE a.courier_id = c.id);
