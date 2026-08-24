UPDATE clinic_settings
SET logo_url = REPLACE(
    logo_url,
    '/api/settings/clinic/logo/',
    '/api/v1/settings/clinic/logo/'
)
WHERE logo_url LIKE '/api/settings/clinic/logo/%';
