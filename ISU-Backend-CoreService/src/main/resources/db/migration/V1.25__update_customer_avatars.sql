-- =============================================
-- Fortune Telling Application - Database Schema
-- Version: 1.25
-- Description: Update customer avatars with random distribution
-- =============================================

-- =============================================
-- UPDATE CUSTOMER AVATARS (ROLE 4) - RANDOM DISTRIBUTION
-- =============================================

UPDATE "user"
SET avatar_url = CASE MOD(ABS(HASHTEXT(user_id::text)), 7)
    WHEN 0 THEN 'https://res.cloudinary.com/dzpv3mfjt/image/upload/v1765724410/customer1_u13w9m.jpg'
    WHEN 1 THEN 'https://res.cloudinary.com/dzpv3mfjt/image/upload/v1765724409/customer2_zd8xqj.avif'
    WHEN 2 THEN 'https://res.cloudinary.com/dzpv3mfjt/image/upload/v1765724409/customer3_inmwbr.avif'
    WHEN 3 THEN 'https://res.cloudinary.com/dzpv3mfjt/image/upload/v1765724408/customer4_z8b5hc.jpg'
    WHEN 4 THEN 'https://res.cloudinary.com/dzpv3mfjt/image/upload/v1765724410/customer5_srdvbs.avif'
    WHEN 5 THEN 'https://res.cloudinary.com/dzpv3mfjt/image/upload/v1765724410/customer6_khivf9.jpg'
    WHEN 6 THEN 'https://res.cloudinary.com/dzpv3mfjt/image/upload/v1765724244/15d86c07-2482-4c1a-a731-2e6065aa2b70.png'
END
WHERE role = '4';
