-- V29: Borrador independiente del contenido publicado de la landing.
--
-- Separa el documento editable (borrador) de la versión pública. La versión
-- pública sigue viviendo en las tablas normalizadas (website_settings,
-- website_specialties, website_benefits, website_process_steps y
-- website_professionals). El borrador se persiste como un único documento JSON
-- que solo se traslada a las tablas públicas al ejecutar la acción "Publicar".

-- 1. Metadatos de la última publicación sobre website_settings.
ALTER TABLE website_settings
    ADD COLUMN IF NOT EXISTS published_at TIMESTAMP,
    ADD COLUMN IF NOT EXISTS published_by BIGINT,
    ADD COLUMN IF NOT EXISTS published_revision BIGINT NOT NULL DEFAULT 0;

-- 2. Tabla singleton del borrador.
CREATE TABLE website_landing_drafts (
    id BIGSERIAL PRIMARY KEY,
    singleton_key VARCHAR(1) NOT NULL DEFAULT 'S',
    content JSONB NOT NULL,
    revision BIGINT NOT NULL DEFAULT 0,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_by BIGINT,
    CONSTRAINT uq_website_landing_drafts_singleton UNIQUE (singleton_key),
    CONSTRAINT ck_website_landing_drafts_singleton CHECK (singleton_key = 'S')
);

-- 3. Inicializar el primer borrador a partir de la configuración publicada
--    existente. La revisión inicial (0) coincide con la versión pública, de
--    modo que el editor arranca sin cambios pendientes.
INSERT INTO website_landing_drafts (singleton_key, content, revision)
SELECT
    'S',
    jsonb_build_object(
        'commercialName', s.commercial_name,
        'tagline', s.tagline,
        'description', s.description,
        'logoExternalImageUrl', s.logo_external_image_url,
        'logoAssetKey', s.logo_asset_key,
        'heroEyebrow', s.hero_eyebrow,
        'heroTitle', s.hero_title,
        'heroHighlight', s.hero_highlight,
        'heroDescription', s.hero_description,
        'heroPrimaryButtonText', s.hero_primary_button_text,
        'heroSecondaryButtonText', s.hero_secondary_button_text,
        'heroExternalImageUrl', s.hero_external_image_url,
        'heroAssetKey', s.hero_asset_key,
        'approachTitle', s.approach_title,
        'approachHighlight', s.approach_highlight,
        'approachDescription', s.approach_description,
        'approachSecondaryDescription', s.approach_secondary_description,
        'approachCtaText', s.approach_cta_text,
        'approachExternalImageUrl', s.approach_external_image_url,
        'approachAssetKey', s.approach_asset_key,
        'contactHeading', s.contact_heading,
        'contactDescription', s.contact_description,
        'contactPhone', s.contact_phone,
        'contactWhatsapp', s.contact_whatsapp,
        'contactEmail', s.contact_email,
        'contactAddress', s.contact_address,
        'contactHours', s.contact_hours,
        'mapUrl', s.map_url,
        'facebookUrl', s.facebook_url,
        'instagramUrl', s.instagram_url,
        'tiktokUrl', s.tiktok_url,
        'linkedinUrl', s.linkedin_url,
        'seoTitle', s.seo_title,
        'seoDescription', s.seo_description,
        'seoSiteName', s.seo_site_name,
        'seoExternalImageUrl', s.seo_external_image_url,
        'seoAssetKey', s.seo_asset_key,
        'specialties', COALESCE((
            SELECT jsonb_agg(jsonb_build_object(
                'draftKey', gen_random_uuid()::text,
                'id', sp.id,
                'code', sp.code,
                'label', sp.label,
                'title', sp.title,
                'subtitle', sp.subtitle,
                'iconCode', sp.icon_code,
                'displayOrder', sp.display_order,
                'visible', sp.is_visible
            ) ORDER BY sp.display_order, sp.id)
            FROM website_specialties sp
        ), '[]'::jsonb),
        'benefits', COALESCE((
            SELECT jsonb_agg(jsonb_build_object(
                'draftKey', gen_random_uuid()::text,
                'id', b.id,
                'title', b.title,
                'description', b.description,
                'iconCode', b.icon_code,
                'displayOrder', b.display_order,
                'active', b.is_active
            ) ORDER BY b.display_order, b.id)
            FROM website_benefits b
        ), '[]'::jsonb),
        'processSteps', COALESCE((
            SELECT jsonb_agg(jsonb_build_object(
                'draftKey', gen_random_uuid()::text,
                'id', p.id,
                'stepNumber', p.step_number,
                'title', p.title,
                'description', p.description,
                'displayOrder', p.display_order,
                'active', p.is_active
            ) ORDER BY p.display_order, p.id)
            FROM website_process_steps p
        ), '[]'::jsonb),
        'professionals', COALESCE((
            SELECT jsonb_agg(jsonb_build_object(
                'draftKey', gen_random_uuid()::text,
                'id', pr.id,
                'name', pr.name,
                'specialty', pr.specialty,
                'licenseNumber', pr.license_number,
                'description', pr.description,
                'experience', pr.experience,
                'careAreas', pr.care_areas,
                'photoExternalUrl', pr.photo_external_url,
                'photoAssetKey', pr.photo_asset_key,
                'displayOrder', pr.display_order,
                'active', pr.is_active
            ) ORDER BY pr.display_order, pr.id)
            FROM website_professionals pr
        ), '[]'::jsonb)
    ),
    0
FROM website_settings s
WHERE s.singleton_key = 'S'
ON CONFLICT (singleton_key) DO NOTHING;
