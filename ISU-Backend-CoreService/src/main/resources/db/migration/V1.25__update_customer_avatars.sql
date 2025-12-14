-- =============================================
-- Fortune Telling Application - Database Schema
-- Version: 1.25
-- Description: Update customer avatars with random distribution
-- =============================================

-- =============================================
-- UPDATE CUSTOMER AVATARS (ROLE 4) - RANDOM DISTRIBUTION
-- =============================================

UPDATE "user"
SET avatar_url = CASE
    WHEN MOD(ABS(('x' || substring(user_id::text, 1, 8))::bit(32)::int), 7) = 0
        THEN 'https://res.cloudinary.com/dzpv3mfjt/image/upload/v1765724410/customer1_u13w9m.jpg'
    WHEN MOD(ABS(('x' || substring(user_id::text, 1, 8))::bit(32)::int), 7) = 1
        THEN 'https://res.cloudinary.com/dzpv3mfjt/image/upload/v1765724409/customer2_zd8xqj.avif'
    WHEN MOD(ABS(('x' || substring(user_id::text, 1, 8))::bit(32)::int), 7) = 2
        THEN 'https://res.cloudinary.com/dzpv3mfjt/image/upload/v1765724409/customer3_inmwbr.avif'
    WHEN MOD(ABS(('x' || substring(user_id::text, 1, 8))::bit(32)::int), 7) = 3
        THEN 'https://res.cloudinary.com/dzpv3mfjt/image/upload/v1765724408/customer4_z8b5hc.jpg'
    WHEN MOD(ABS(('x' || substring(user_id::text, 1, 8))::bit(32)::int), 7) = 4
        THEN 'https://res.cloudinary.com/dzpv3mfjt/image/upload/v1765724410/customer5_srdvbs.avif'
    WHEN MOD(ABS(('x' || substring(user_id::text, 1, 8))::bit(32)::int), 7) = 5
        THEN 'https://res.cloudinary.com/dzpv3mfjt/image/upload/v1765724410/customer6_khivf9.jpg'
    ELSE 'https://res.cloudinary.com/dzpv3mfjt/image/upload/v1765724244/15d86c07-2482-4c1a-a731-2e6065aa2b70.png'
END
WHERE role = '4';

