CREATE TABLE website_settings (
    id BIGSERIAL PRIMARY KEY,
    singleton_key VARCHAR(1) NOT NULL DEFAULT 'S',
    commercial_name VARCHAR(120) NOT NULL,
    tagline VARCHAR(180),
    description VARCHAR(500),
    logo_external_image_url VARCHAR(500),
    logo_asset_key VARCHAR(255),
    hero_eyebrow VARCHAR(120),
    hero_title VARCHAR(180) NOT NULL,
    hero_highlight VARCHAR(120),
    hero_description VARCHAR(500),
    hero_primary_button_text VARCHAR(80),
    hero_secondary_button_text VARCHAR(80),
    hero_external_image_url VARCHAR(500),
    hero_asset_key VARCHAR(255),
    approach_title VARCHAR(180),
    approach_highlight VARCHAR(120),
    approach_description VARCHAR(1000),
    approach_secondary_description VARCHAR(1000),
    approach_cta_text VARCHAR(80),
    approach_external_image_url VARCHAR(500),
    approach_asset_key VARCHAR(255),
    contact_heading VARCHAR(180),
    contact_description VARCHAR(500),
    contact_phone VARCHAR(40),
    contact_whatsapp VARCHAR(40),
    contact_email VARCHAR(180),
    contact_address VARCHAR(300),
    contact_hours VARCHAR(300),
    map_url VARCHAR(500),
    facebook_url VARCHAR(500),
    instagram_url VARCHAR(500),
    tiktok_url VARCHAR(500),
    linkedin_url VARCHAR(500),
    seo_title VARCHAR(180),
    seo_description VARCHAR(300),
    seo_site_name VARCHAR(120),
    seo_external_image_url VARCHAR(500),
    seo_asset_key VARCHAR(255),
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT uq_website_settings_singleton UNIQUE (singleton_key),
    CONSTRAINT ck_website_settings_singleton CHECK (singleton_key = 'S')
);

CREATE TABLE website_specialties (
    id BIGSERIAL PRIMARY KEY,
    code VARCHAR(50) NOT NULL UNIQUE,
    label VARCHAR(120) NOT NULL,
    title VARCHAR(180) NOT NULL,
    subtitle VARCHAR(500),
    icon_code VARCHAR(40) NOT NULL DEFAULT 'STETHOSCOPE',
    display_order INTEGER NOT NULL DEFAULT 0,
    is_visible BOOLEAN NOT NULL DEFAULT TRUE
);

CREATE TABLE website_specialty_services (
    id BIGSERIAL PRIMARY KEY,
    specialty_id BIGINT NOT NULL REFERENCES website_specialties(id) ON DELETE CASCADE,
    name VARCHAR(180) NOT NULL,
    display_order INTEGER NOT NULL DEFAULT 0,
    is_active BOOLEAN NOT NULL DEFAULT TRUE
);

CREATE TABLE website_benefits (
    id BIGSERIAL PRIMARY KEY,
    title VARCHAR(180) NOT NULL,
    description VARCHAR(500),
    icon_code VARCHAR(40) NOT NULL DEFAULT 'SPARKLES',
    display_order INTEGER NOT NULL DEFAULT 0,
    is_active BOOLEAN NOT NULL DEFAULT TRUE
);

CREATE TABLE website_process_steps (
    id BIGSERIAL PRIMARY KEY,
    step_number INTEGER NOT NULL,
    title VARCHAR(180) NOT NULL,
    description VARCHAR(500),
    display_order INTEGER NOT NULL DEFAULT 0,
    is_active BOOLEAN NOT NULL DEFAULT TRUE
);

CREATE TABLE website_professionals (
    id BIGSERIAL PRIMARY KEY,
    name VARCHAR(180) NOT NULL,
    specialty VARCHAR(120),
    license_number VARCHAR(80),
    description VARCHAR(1000),
    experience VARCHAR(300),
    care_areas VARCHAR(500),
    photo_external_url VARCHAR(500),
    photo_asset_key VARCHAR(255),
    display_order INTEGER NOT NULL DEFAULT 0,
    is_active BOOLEAN NOT NULL DEFAULT TRUE
);

INSERT INTO website_settings (
    commercial_name, tagline, description, hero_eyebrow, hero_title, hero_highlight,
    hero_description, hero_primary_button_text, hero_secondary_button_text, hero_external_image_url,
    approach_title, approach_highlight, approach_description, approach_secondary_description,
    approach_cta_text, approach_external_image_url, contact_heading, contact_description,
    seo_title, seo_description, seo_site_name
) VALUES (
    'Clínica Personal',
    'Dermatología y Psicología para tu bienestar integral.',
    'Dos especialidades, un mismo propósito: acompañarte con conocimiento, respeto y calidez.',
    'Cuidado que empieza por escucharte',
    'Tu bienestar,',
    'en buenas manos.',
    'Atención profesional en Dermatología y Psicología, con una mirada integral, cercana y pensada para tu momento.',
    'Agendar una cita',
    'Conocer nuestros servicios',
    'https://images.unsplash.com/photo-1576091160399-112ba8d25d1d?auto=format&fit=crop&w=1400&h=1750&q=82',
    'Atenderte también es',
    'conocerte.',
    'Creemos que la atención profesional puede sentirse humana. Por eso construimos un espacio donde puedas hablar con confianza, resolver tus dudas y tomar decisiones informadas sobre tu salud.',
    'Cada consulta comienza escuchándote y continúa con un acompañamiento respetuoso, sin juicios y a tu ritmo.',
    'Conversemos',
    'https://images.unsplash.com/photo-1544168190-79c17527004f?auto=format&fit=crop&w=1200&h=900&q=80',
    'Tu bienestar merece atención profesional.',
    'Déjanos tus datos o escríbenos por nuestros canales de contacto.',
    'Clínica Personal | Dermatología y Psicología',
    'Atención profesional en Dermatología y Psicología, con una mirada integral y cercana.',
    'Clínica Personal'
);

INSERT INTO website_specialties (code, label, title, subtitle, icon_code, display_order)
VALUES
    ('DERMATOLOGIA', '01 / Piel y cabello', 'Dermatología', 'Conoce y cuida tu piel con una evaluación profesional, clara y adaptada a tus necesidades.', 'MICROSCOPE', 1),
    ('PSICOLOGIA', '02 / Mente y emociones', 'Psicología', 'Un espacio seguro para comprender lo que sientes y encontrar herramientas para tu bienestar.', 'BRAIN', 2);

INSERT INTO website_specialty_services (specialty_id, name, display_order)
SELECT id, service_name, service_order
FROM website_specialties s
JOIN (VALUES
    ('DERMATOLOGIA', 'Consulta dermatológica', 1),
    ('DERMATOLOGIA', 'Acné, dermatitis y manchas', 2),
    ('DERMATOLOGIA', 'Caída del cabello y cuidado de la piel', 3),
    ('DERMATOLOGIA', 'Evaluación de lesiones de piel', 4),
    ('PSICOLOGIA', 'Consulta y evaluación psicológica', 1),
    ('PSICOLOGIA', 'Terapia individual', 2),
    ('PSICOLOGIA', 'Manejo del estrés y la ansiedad', 3),
    ('PSICOLOGIA', 'Autoestima y bienestar emocional', 4)
) AS services(specialty_code, service_name, service_order) ON services.specialty_code = s.code;

INSERT INTO website_benefits (title, description, icon_code, display_order)
VALUES
    ('Atención personalizada', 'Escuchamos tu historia para ofrecerte una atención acorde a lo que necesitas.', 'HEART_HANDSHAKE', 1),
    ('Confianza y confidencialidad', 'Cuidamos tu privacidad y respetamos cada proceso personal.', 'SHIELD_CHECK', 2),
    ('Profesionalismo cercano', 'Conocimiento y experiencia comunicados de forma clara y humana.', 'STETHOSCOPE', 3),
    ('Reserva sencilla', 'Da el primer paso de manera simple, a tu ritmo y sin complicaciones.', 'SPARKLES', 4);

INSERT INTO website_process_steps (step_number, title, description, display_order)
VALUES
    (1, 'Elige tu especialidad', 'Dermatología o Psicología, según lo que hoy necesitas.', 1),
    (2, 'Solicita una cita', 'Escríbenos por el canal que prefieras.', 2),
    (3, 'Confirma tu horario', 'Coordinamos contigo el día y la hora más conveniente.', 3),
    (4, 'Recibe atención profesional', 'Encontrarás un espacio seguro y pensado para ti.', 4);

INSERT INTO user_roles (user_id, role)
SELECT u.id, 'ROLE_SITE_ADMIN'
FROM users u
WHERE u.username = 'admin'
  AND NOT EXISTS (
      SELECT 1 FROM user_roles ur
      WHERE ur.user_id = u.id AND ur.role = 'ROLE_SITE_ADMIN'
  );
