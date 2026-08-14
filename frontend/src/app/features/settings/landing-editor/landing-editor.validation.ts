import { WebsiteDraft } from '../../../core/models/website-editor.model';

export const ICON_CODES = [
  'HEART_HANDSHAKE',
  'SHIELD_CHECK',
  'STETHOSCOPE',
  'SPARKLES',
  'MICROSCOPE',
  'BRAIN'
] as const;

export const ICON_LABELS: Record<string, string> = {
  HEART_HANDSHAKE: 'Cuidado personalizado',
  SHIELD_CHECK: 'Confianza y privacidad',
  STETHOSCOPE: 'Atención profesional',
  SPARKLES: 'Bienestar',
  MICROSCOPE: 'Piel y diagnóstico',
  BRAIN: 'Mente y emociones'
};

export const FIELD_LIMITS: Record<string, number> = {
  commercialName: 120,
  tagline: 180,
  description: 500,
  logoExternalImageUrl: 500,
  heroEyebrow: 120,
  heroTitle: 180,
  heroHighlight: 120,
  heroDescription: 500,
  heroPrimaryButtonText: 80,
  heroSecondaryButtonText: 80,
  heroExternalImageUrl: 500,
  approachTitle: 180,
  approachHighlight: 120,
  approachDescription: 1000,
  approachSecondaryDescription: 1000,
  approachCtaText: 80,
  approachExternalImageUrl: 500,
  contactHeading: 180,
  contactDescription: 500,
  contactPhone: 40,
  contactWhatsapp: 40,
  contactEmail: 180,
  contactAddress: 300,
  contactHours: 300,
  mapUrl: 500,
  facebookUrl: 500,
  instagramUrl: 500,
  tiktokUrl: 500,
  linkedinUrl: 500,
  seoTitle: 180,
  seoDescription: 300,
  seoSiteName: 120,
  seoExternalImageUrl: 500
};

export const COLLECTION_LIMITS = {
  specialties: 20,
  benefits: 30,
  processSteps: 30,
  professionals: 50
} as const;

const EMAIL_REGEX = /^[^@\s]+@[^@\s]+\.[^@\s]+$/;

const URL_FIELDS = [
  'logoExternalImageUrl',
  'heroExternalImageUrl',
  'approachExternalImageUrl',
  'seoExternalImageUrl',
  'mapUrl',
  'facebookUrl',
  'instagramUrl',
  'tiktokUrl',
  'linkedinUrl'
];

export function isHttpUrl(value?: string): boolean {
  if (!value || value.trim() === '') return true;
  try {
    const url = new URL(value.trim());
    return url.protocol === 'http:' || url.protocol === 'https:';
  } catch {
    return false;
  }
}

export function validateDraft(draft: WebsiteDraft): Record<string, string> {
  const errors: Record<string, string> = {};

  if (!draft.commercialName?.trim()) errors['commercialName'] = 'Obligatorio';
  if (!draft.heroTitle?.trim()) errors['heroTitle'] = 'Obligatorio';

  for (const [field, max] of Object.entries(FIELD_LIMITS)) {
    const value = (draft as unknown as Record<string, unknown>)[field];
    if (typeof value === 'string' && value.length > max) {
      errors[field] = `Máximo ${max} caracteres`;
    }
  }

  for (const field of URL_FIELDS) {
    const value = (draft as unknown as Record<string, unknown>)[field];
    if (typeof value === 'string' && !isHttpUrl(value)) {
      errors[field] = 'Debe ser una URL http(s)';
    }
  }

  if (draft.contactEmail && !EMAIL_REGEX.test(draft.contactEmail)) {
    errors['contactEmail'] = 'Correo no válido';
  }

  if (draft.specialties.length > COLLECTION_LIMITS.specialties) {
    errors['specialties'] = `Máximo ${COLLECTION_LIMITS.specialties} especialidades`;
  }
  if (draft.benefits.length > COLLECTION_LIMITS.benefits) {
    errors['benefits'] = `Máximo ${COLLECTION_LIMITS.benefits} beneficios`;
  }
  if (draft.processSteps.length > COLLECTION_LIMITS.processSteps) {
    errors['processSteps'] = `Máximo ${COLLECTION_LIMITS.processSteps} pasos`;
  }
  if (draft.professionals.length > COLLECTION_LIMITS.professionals) {
    errors['professionals'] = `Máximo ${COLLECTION_LIMITS.professionals} profesionales`;
  }

  draft.specialties.forEach((item, index) => {
    if (!item.code?.trim()) errors[`specialties.${index}.code`] = 'Obligatorio';
    if (!item.label?.trim()) errors[`specialties.${index}.label`] = 'Obligatorio';
    if (!item.title?.trim()) errors[`specialties.${index}.title`] = 'Obligatorio';
    if (!(ICON_CODES as readonly string[]).includes(item.iconCode)) {
      errors[`specialties.${index}.iconCode`] = 'Icono no permitido';
    }
  });

  draft.benefits.forEach((item, index) => {
    if (!item.title?.trim()) errors[`benefits.${index}.title`] = 'Obligatorio';
    if (!(ICON_CODES as readonly string[]).includes(item.iconCode)) {
      errors[`benefits.${index}.iconCode`] = 'Icono no permitido';
    }
  });

  draft.processSteps.forEach((item, index) => {
    if (!item.title?.trim()) errors[`processSteps.${index}.title`] = 'Obligatorio';
  });

  draft.professionals.forEach((item, index) => {
    if (!item.name?.trim()) errors[`professionals.${index}.name`] = 'Obligatorio';
    if (!isHttpUrl(item.photoExternalUrl)) errors[`professionals.${index}.photoExternalUrl`] = 'Debe ser una URL http(s)';
  });

  return errors;
}
