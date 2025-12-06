-- =============================================
-- Fortune Telling Application - Database Schema
-- Version: 1.24
-- Description: Update images for knowledge items, service packages, users, and certificates
-- =============================================

-- =============================================
-- 1. UPDATE KNOWLEDGE ITEM IMAGES BY CATEGORY
-- =============================================

-- Chỉ Tay Category (4d4d3003-cad8-4805-8e04-2170d12e5bcf)
UPDATE knowledge_item ki
SET image_url = CASE
    WHEN ki.item_id = '7524950d-2ecd-45a5-8994-b1e7ae744fe3' THEN 'https://res.cloudinary.com/dzpv3mfjt/image/upload/v1764996341/cach-coi-chi-tay-1_li6vk3.jpg'
    WHEN ki.item_id = '1227a134-9048-4377-8856-062c5530d595' THEN 'https://res.cloudinary.com/dzpv3mfjt/image/upload/v1764996340/g%C3%B2_ai4i85.png'
    WHEN ki.item_id = 'ae80ac40-69ba-480c-b541-ca096fa947f2' THEN 'https://res.cloudinary.com/dzpv3mfjt/image/upload/v1764996339/3_%C4%91%C6%B0%E1%BB%9Dng_cwmoxf.jpg'
    ELSE ki.image_url
END
WHERE ki.item_id IN (
    SELECT ic.item_id FROM item_category ic WHERE ic.category_id = '4d4d3003-cad8-4805-8e04-2170d12e5bcf'
);

-- Cung Hoàng Đạo Category (1a1d3003-cad8-4805-8e04-2170d12e5bcf)
UPDATE knowledge_item ki
SET image_url = CASE
    WHEN ki.item_id = '1e664360-9d6f-48ad-b680-f0d4cda9b7de' THEN 'https://res.cloudinary.com/dzpv3mfjt/image/upload/v1764996337/ky-hieu-12-cung-hoang-dao_h9tbmu.jpg'
    WHEN ki.item_id = '35db3c89-1474-4697-8eb2-82a14d59b65c' THEN 'https://res.cloudinary.com/dzpv3mfjt/image/upload/v1764996336/cung-lua-gom-cung-nao-thumb_hg7ku3.jpg'
    WHEN ki.item_id = 'a9b54c47-4175-46ef-ba1a-f8e3d0f5e6c1' THEN 'https://res.cloudinary.com/dzpv3mfjt/image/upload/v1764996335/cung-dat-gom-cung-nao-3_nfpmu5.jpg'
    WHEN ki.item_id = '2afeeb7c-5ae5-49e6-87db-6d850d026710' THEN 'https://res.cloudinary.com/dzpv3mfjt/image/upload/v1764996335/cung-khi_pfcmnh.jpg'
    WHEN ki.item_id = '9c06e0f2-2c54-477d-b0f0-848df4bc6821' THEN 'https://res.cloudinary.com/dzpv3mfjt/image/upload/v1764996333/cung-nuoc_tlmu3i.jpg'
    ELSE ki.image_url
END
WHERE ki.item_id IN (
    SELECT ic.item_id FROM item_category ic WHERE ic.category_id = '1a1d3003-cad8-4805-8e04-2170d12e5bcf'
);

-- Ngũ Hành Category (3c3d3003-cad8-4805-8e04-2170d12e5bcf)
UPDATE knowledge_item ki
SET image_url = CASE
    WHEN ki.item_id = 'c2fe3a0c-0c2e-40a5-a38d-3e83820d90b2' THEN 'https://res.cloudinary.com/dzpv3mfjt/image/upload/v1764996343/kim-moc-thuy-hoa-thuy_nptqc0.jpg'
    WHEN ki.item_id = '9f72a042-f7c6-474d-8e25-2e7cc5bc138d' THEN 'https://res.cloudinary.com/dzpv3mfjt/image/upload/v1764996342/tuongsinhtuongkhac_geww9i.jpg'
    WHEN ki.item_id = '026eeee3-372e-43a2-9797-816459818d56' THEN 'https://res.cloudinary.com/dzpv3mfjt/image/upload/v1764996342/ngu-hanh-phan-khac_h1s8l8.jpg'
    ELSE ki.image_url
END
WHERE ki.item_id IN (
    SELECT ic.item_id FROM item_category ic WHERE ic.category_id = '3c3d3003-cad8-4805-8e04-2170d12e5bcf'
);

-- Nhân Tướng Category (2b2d3003-cad8-4805-8e04-2170d12e5bcf)
UPDATE knowledge_item ki
SET image_url = CASE
    WHEN ki.item_id = '6e11568b-5fcb-4b82-b815-60018c46e28e' THEN 'https://res.cloudinary.com/dzpv3mfjt/image/upload/v1764996333/m%E1%BA%B7t_h1ponu.jpg'
    WHEN ki.item_id = 'afd07a12-0f85-43bf-ad22-ae0023e08005' THEN 'https://res.cloudinary.com/dzpv3mfjt/image/upload/v1764996332/t%C6%B0%E1%BB%9Bng_ibbu3l.jpg'
    ELSE ki.image_url
END
WHERE ki.item_id IN (
    SELECT ic.item_id FROM item_category ic WHERE ic.category_id = '2b2d3003-cad8-4805-8e04-2170d12e5bcf'
);

-- Tarot Category (5e5d3003-cad8-4805-8e04-2170d12e5bcf)
UPDATE knowledge_item ki
SET image_url = CASE
    WHEN ki.item_id = 'd5c5ab58-2480-41da-825f-3c01efe7d640' THEN 'https://res.cloudinary.com/dzpv3mfjt/image/upload/v1764996331/%E1%BA%A9n_ch%C3%ADnh_drdvhs.jpg'
    WHEN ki.item_id = '820f6465-c535-41d5-bc0c-02a745792513' THEN 'https://res.cloudinary.com/dzpv3mfjt/image/upload/v1764996330/wands_cwtyvw.png'
    WHEN ki.item_id = '224c7fd6-5098-4397-996e-9e2229a977a4' THEN 'https://res.cloudinary.com/dzpv3mfjt/image/upload/v1764996329/swords_t94sst.png'
    WHEN ki.item_id = '2ad61ee6-8f2c-4058-a5e3-be5a8f5dff42' THEN 'https://res.cloudinary.com/dzpv3mfjt/image/upload/v1764996329/cups_denyde.png'
    WHEN ki.item_id = '554eb231-1085-471b-a681-612fa1819295' THEN 'https://res.cloudinary.com/dzpv3mfjt/image/upload/v1764996328/Pentacles_sdp455.png'
    ELSE ki.image_url
END
WHERE ki.item_id IN (
    SELECT ic.item_id FROM item_category ic WHERE ic.category_id = '5e5d3003-cad8-4805-8e04-2170d12e5bcf'
);

-- =============================================
-- 2. UPDATE SERVICE PACKAGE IMAGES BY CATEGORY
-- =============================================

-- Chỉ Tay Packages
UPDATE service_package sp
SET image_url = CASE
    WHEN MOD(ABS(('x' || substring(sp.package_id::text, 1, 8))::bit(32)::int), 2) = 0
        THEN 'https://res.cloudinary.com/dzpv3mfjt/image/upload/v1765001337/chitay1_zhv7zl.jpg'
    ELSE 'https://res.cloudinary.com/dzpv3mfjt/image/upload/v1765001336/chitay2_jw7qzj.jpg'
END
WHERE sp.package_id IN (
    SELECT pc.package_id FROM package_category pc WHERE pc.category_id = '4d4d3003-cad8-4805-8e04-2170d12e5bcf'
);

-- Cung Hoàng Đạo Packages
UPDATE service_package sp
SET image_url = CASE
    WHEN MOD(ABS(('x' || substring(sp.package_id::text, 1, 8))::bit(32)::int), 3) = 0
        THEN 'https://res.cloudinary.com/dzpv3mfjt/image/upload/v1765001342/cunghoangdao1_lyor9z.jpg'
    WHEN MOD(ABS(('x' || substring(sp.package_id::text, 1, 8))::bit(32)::int), 3) = 1
        THEN 'https://res.cloudinary.com/dzpv3mfjt/image/upload/v1765001341/cunghoangdao2_utaaiw.jpg'
    ELSE 'https://res.cloudinary.com/dzpv3mfjt/image/upload/v1765001340/cunghoangdao3_rv1x4o.jpg'
END
WHERE sp.package_id IN (
    SELECT pc.package_id FROM package_category pc WHERE pc.category_id = '1a1d3003-cad8-4805-8e04-2170d12e5bcf'
);

-- Nhân Tướng Học Packages
UPDATE service_package sp
SET image_url = CASE
    WHEN MOD(ABS(('x' || substring(sp.package_id::text, 1, 8))::bit(32)::int), 5) = 0
        THEN 'https://res.cloudinary.com/dzpv3mfjt/image/upload/v1765001340/nhantuonghoc1_tntr7g.jpg'
    WHEN MOD(ABS(('x' || substring(sp.package_id::text, 1, 8))::bit(32)::int), 5) = 1
        THEN 'https://res.cloudinary.com/dzpv3mfjt/image/upload/v1765001339/nhantuonghoc2_zvbxyi.png'
    WHEN MOD(ABS(('x' || substring(sp.package_id::text, 1, 8))::bit(32)::int), 5) = 2
        THEN 'https://res.cloudinary.com/dzpv3mfjt/image/upload/v1765001339/nhantuonghoc3_mmzqnn.jpg'
    WHEN MOD(ABS(('x' || substring(sp.package_id::text, 1, 8))::bit(32)::int), 5) = 3
        THEN 'https://res.cloudinary.com/dzpv3mfjt/image/upload/v1765001338/nhantuonghoc4_qw31nx.jpg'
    ELSE 'https://res.cloudinary.com/dzpv3mfjt/image/upload/v1765001338/nhantuonghoc5_ke2j8h.jpg'
END
WHERE sp.package_id IN (
    SELECT pc.package_id FROM package_category pc WHERE pc.category_id = '2b2d3003-cad8-4805-8e04-2170d12e5bcf'
);

-- Phong Thủy Ngũ Hành Packages
UPDATE service_package sp
SET image_url = CASE
    WHEN MOD(ABS(('x' || substring(sp.package_id::text, 1, 8))::bit(32)::int), 5) = 0
        THEN 'https://res.cloudinary.com/dzpv3mfjt/image/upload/v1765001399/phongthuy1_wdud47.jpg'
    WHEN MOD(ABS(('x' || substring(sp.package_id::text, 1, 8))::bit(32)::int), 5) = 1
        THEN 'https://res.cloudinary.com/dzpv3mfjt/image/upload/v1765001398/phongthuy2_ssasrt.jpg'
    WHEN MOD(ABS(('x' || substring(sp.package_id::text, 1, 8))::bit(32)::int), 5) = 2
        THEN 'https://res.cloudinary.com/dzpv3mfjt/image/upload/v1765001398/phongthuy3_cr2hxb.png'
    WHEN MOD(ABS(('x' || substring(sp.package_id::text, 1, 8))::bit(32)::int), 5) = 3
        THEN 'https://res.cloudinary.com/dzpv3mfjt/image/upload/v1765001397/phongthuy4_r0hhjs.png'
    ELSE 'https://res.cloudinary.com/dzpv3mfjt/image/upload/v1765001396/phongthuy5_b9lusm.jpg'
END
WHERE sp.package_id IN (
    SELECT pc.package_id FROM package_category pc WHERE pc.category_id = '3c3d3003-cad8-4805-8e04-2170d12e5bcf'
);

-- Tarot Packages
UPDATE service_package sp
SET image_url = CASE
    WHEN MOD(ABS(('x' || substring(sp.package_id::text, 1, 8))::bit(32)::int), 15) = 0
        THEN 'https://res.cloudinary.com/dzpv3mfjt/image/upload/v1765001375/tarot1_ikbizb.jpg'
    WHEN MOD(ABS(('x' || substring(sp.package_id::text, 1, 8))::bit(32)::int), 15) = 1
        THEN 'https://res.cloudinary.com/dzpv3mfjt/image/upload/v1765001374/tarot2_na7u6y.jpg'
    WHEN MOD(ABS(('x' || substring(sp.package_id::text, 1, 8))::bit(32)::int), 15) = 2
        THEN 'https://res.cloudinary.com/dzpv3mfjt/image/upload/v1765001374/tarot3_zzszzr.jpg'
    WHEN MOD(ABS(('x' || substring(sp.package_id::text, 1, 8))::bit(32)::int), 15) = 3
        THEN 'https://res.cloudinary.com/dzpv3mfjt/image/upload/v1765001373/tarot4_owouuy.jpg'
    WHEN MOD(ABS(('x' || substring(sp.package_id::text, 1, 8))::bit(32)::int), 15) = 4
        THEN 'https://res.cloudinary.com/dzpv3mfjt/image/upload/v1765001373/tarot5_clnb3g.jpg'
    WHEN MOD(ABS(('x' || substring(sp.package_id::text, 1, 8))::bit(32)::int), 15) = 5
        THEN 'https://res.cloudinary.com/dzpv3mfjt/image/upload/v1765001372/tarot6_xy2lxg.jpg'
    WHEN MOD(ABS(('x' || substring(sp.package_id::text, 1, 8))::bit(32)::int), 15) = 6
        THEN 'https://res.cloudinary.com/dzpv3mfjt/image/upload/v1765001371/tarot7_fqcowe.jpg'
    WHEN MOD(ABS(('x' || substring(sp.package_id::text, 1, 8))::bit(32)::int), 15) = 7
        THEN 'https://res.cloudinary.com/dzpv3mfjt/image/upload/v1765001371/tarot8_xok4fw.jpg'
    WHEN MOD(ABS(('x' || substring(sp.package_id::text, 1, 8))::bit(32)::int), 15) = 8
        THEN 'https://res.cloudinary.com/dzpv3mfjt/image/upload/v1765001370/tarot9_rulihw.jpg'
    WHEN MOD(ABS(('x' || substring(sp.package_id::text, 1, 8))::bit(32)::int), 15) = 9
        THEN 'https://res.cloudinary.com/dzpv3mfjt/image/upload/v1765001370/tarot10_mnuprl.jpg'
    WHEN MOD(ABS(('x' || substring(sp.package_id::text, 1, 8))::bit(32)::int), 15) = 10
        THEN 'https://res.cloudinary.com/dzpv3mfjt/image/upload/v1765001369/tarot11_jxlqkj.jpg'
    WHEN MOD(ABS(('x' || substring(sp.package_id::text, 1, 8))::bit(32)::int), 15) = 11
        THEN 'https://res.cloudinary.com/dzpv3mfjt/image/upload/v1765001369/tarot12_nlqsbw.jpg'
    WHEN MOD(ABS(('x' || substring(sp.package_id::text, 1, 8))::bit(32)::int), 15) = 12
        THEN 'https://res.cloudinary.com/dzpv3mfjt/image/upload/v1765001368/tarot13_gog48x.png'
    WHEN MOD(ABS(('x' || substring(sp.package_id::text, 1, 8))::bit(32)::int), 15) = 13
        THEN 'https://res.cloudinary.com/dzpv3mfjt/image/upload/v1765001364/tarot14_udbcm5.jpg'
    ELSE 'https://res.cloudinary.com/dzpv3mfjt/image/upload/v1765001342/tarot15_r8zszh.jpg'
END
WHERE sp.package_id IN (
    SELECT pc.package_id FROM package_category pc WHERE pc.category_id = '5e5d3003-cad8-4805-8e04-2170d12e5bcf'
);

-- =============================================
-- 3. UPDATE USER AVATARS AND COVERS BY ROLE
-- =============================================

-- Update ADMIN avatar
UPDATE "user"
SET avatar_url = 'https://res.cloudinary.com/dzpv3mfjt/image/upload/v1765001298/admin_j74vin.avif'
WHERE role = '0';

-- Update CUSTOMER avatars (shared avatar)
UPDATE "user"
SET avatar_url = 'https://res.cloudinary.com/dzpv3mfjt/image/upload/v1765001291/customer_e5mzhs.avif'
WHERE role = '4';

-- Update SEER avatars (each seer gets unique avatar)
WITH seer_avatars AS (
    SELECT
        user_id,
        ROW_NUMBER() OVER (ORDER BY user_id) as rn
    FROM "user"
    WHERE role = '1'
)
UPDATE "user" u
SET avatar_url = CASE sa.rn
    WHEN 1 THEN 'https://res.cloudinary.com/dzpv3mfjt/image/upload/v1765001303/seer1_nzu7ep.avif'
    WHEN 2 THEN 'https://res.cloudinary.com/dzpv3mfjt/image/upload/v1765001303/seer2_j1inbg.jpg'
    WHEN 3 THEN 'https://res.cloudinary.com/dzpv3mfjt/image/upload/v1765001303/seer3_g6rtbb.jpg'
    WHEN 4 THEN 'https://res.cloudinary.com/dzpv3mfjt/image/upload/v1765001302/seer4_m2nusg.jpg'
    WHEN 5 THEN 'https://res.cloudinary.com/dzpv3mfjt/image/upload/v1765001302/seer5_hqr5yg.jpg'
    WHEN 6 THEN 'https://res.cloudinary.com/dzpv3mfjt/image/upload/v1765001301/seer6_jnammi.jpg'
    WHEN 7 THEN 'https://res.cloudinary.com/dzpv3mfjt/image/upload/v1765001301/seer7_m9qfpv.avif'
    WHEN 8 THEN 'https://res.cloudinary.com/dzpv3mfjt/image/upload/v1765001299/seer8_r5vjvm.avif'
    WHEN 9 THEN 'https://res.cloudinary.com/dzpv3mfjt/image/upload/v1765001301/seer9_umx5hx.png'
    WHEN 10 THEN 'https://res.cloudinary.com/dzpv3mfjt/image/upload/v1765001299/seer10_tnlybn.jpg'
    WHEN 11 THEN 'https://res.cloudinary.com/dzpv3mfjt/image/upload/v1765001299/seer11_vnudpp.jpg'
    ELSE 'https://res.cloudinary.com/dzpv3mfjt/image/upload/v1765001303/seer1_nzu7ep.avif'
END
FROM seer_avatars sa
WHERE u.user_id = sa.user_id AND u.role = '1';

-- Update CUSTOMER covers (shared cover)
UPDATE "user"
SET cover_url = 'https://res.cloudinary.com/dzpv3mfjt/image/upload/v1765001291/cover2_ngknal.jpg'
WHERE role = '4';

-- Update SEER covers (each seer gets unique cover)
WITH seer_covers AS (
    SELECT
        user_id,
        ROW_NUMBER() OVER (ORDER BY user_id) as rn
    FROM "user"
    WHERE role = '1'
)
UPDATE "user" u
SET cover_url = CASE sc.rn
    WHEN 1 THEN 'https://res.cloudinary.com/dzpv3mfjt/image/upload/v1765001288/cover4_npswm7.jpg'
    WHEN 2 THEN 'https://res.cloudinary.com/dzpv3mfjt/image/upload/v1765001288/cover5_ne1hgh.jpg'
    WHEN 3 THEN 'https://res.cloudinary.com/dzpv3mfjt/image/upload/v1765001287/cover6_smiiye.jpg'
    WHEN 4 THEN 'https://res.cloudinary.com/dzpv3mfjt/image/upload/v1765001286/cover7_fvdzac.avif'
    WHEN 5 THEN 'https://res.cloudinary.com/dzpv3mfjt/image/upload/v1765001286/cover8_nrlaot.jpg'
    WHEN 6 THEN 'https://res.cloudinary.com/dzpv3mfjt/image/upload/v1765001285/cover9_o90vwl.jpg'
    WHEN 7 THEN 'https://res.cloudinary.com/dzpv3mfjt/image/upload/v1765001285/cover10_ivho8b.jpg'
    WHEN 8 THEN 'https://res.cloudinary.com/dzpv3mfjt/image/upload/v1765001284/cover11_t4ervg.jpg'
    WHEN 9 THEN 'https://res.cloudinary.com/dzpv3mfjt/image/upload/v1765001288/cover4_npswm7.jpg'
    WHEN 10 THEN 'https://res.cloudinary.com/dzpv3mfjt/image/upload/v1765001288/cover5_ne1hgh.jpg'
    WHEN 11 THEN 'https://res.cloudinary.com/dzpv3mfjt/image/upload/v1765001287/cover6_smiiye.jpg'
    ELSE 'https://res.cloudinary.com/dzpv3mfjt/image/upload/v1765001288/cover4_npswm7.jpg'
END
FROM seer_covers sc
WHERE u.user_id = sc.user_id AND u.role = '1';

-- =============================================
-- 4. UPDATE CERTIFICATE IMAGES (RANDOM DISTRIBUTION)
-- =============================================

UPDATE certificate
SET certificate_url = CASE
    WHEN MOD(ABS(('x' || substring(certificate_id::text, 1, 8))::bit(32)::int), 9) = 0
        THEN 'https://res.cloudinary.com/dzpv3mfjt/image/upload/v1764996327/Screenshot_2025-12-06_111602_gi1jty.png'
    WHEN MOD(ABS(('x' || substring(certificate_id::text, 1, 8))::bit(32)::int), 9) = 1
        THEN 'https://res.cloudinary.com/dzpv3mfjt/image/upload/v1764996326/Screenshot_2025-12-06_111656_lzq5ad.png'
    WHEN MOD(ABS(('x' || substring(certificate_id::text, 1, 8))::bit(32)::int), 9) = 2
        THEN 'https://res.cloudinary.com/dzpv3mfjt/image/upload/v1764996325/Screenshot_2025-12-06_111730_ehyhw3.png'
    WHEN MOD(ABS(('x' || substring(certificate_id::text, 1, 8))::bit(32)::int), 9) = 3
        THEN 'https://res.cloudinary.com/dzpv3mfjt/image/upload/v1764996324/Screenshot_2025-12-06_111754_qfmsvp.png'
    WHEN MOD(ABS(('x' || substring(certificate_id::text, 1, 8))::bit(32)::int), 9) = 4
        THEN 'https://res.cloudinary.com/dzpv3mfjt/image/upload/v1764996322/Screenshot_2025-12-06_111834_qtxnnv.png'
    WHEN MOD(ABS(('x' || substring(certificate_id::text, 1, 8))::bit(32)::int), 9) = 5
        THEN 'https://res.cloudinary.com/dzpv3mfjt/image/upload/v1764996322/Screenshot_2025-12-06_111913_euzolb.png'
    WHEN MOD(ABS(('x' || substring(sp.package_id::text, 1, 8))::bit(32)::int), 9) = 6
        THEN 'https://res.cloudinary.com/dzpv3mfjt/image/upload/v1764996322/Screenshot_2025-12-06_111937_dwdq0t.png'
    WHEN MOD(ABS(('x' || substring(certificate_id::text, 1, 8))::bit(32)::int), 9) = 7
        THEN 'https://res.cloudinary.com/dzpv3mfjt/image/upload/v1764996322/Screenshot_2025-12-06_111956_sgejst.png'
    ELSE 'https://res.cloudinary.com/dzpv3mfjt/image/upload/v1764996321/Screenshot_2025-12-06_112014_x4js3s.png'
END;

