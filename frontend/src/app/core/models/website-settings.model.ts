export interface WebsiteSettingsAdmin {
  id?: number;
  commercialName: string;
  tagline?: string;
  description?: string;
  logoExternalImageUrl?: string;
  logoAssetKey?: string;
  heroEyebrow?: string;
  heroTitle: string;
  heroHighlight?: string;
  heroDescription?: string;
  heroPrimaryButtonText?: string;
  heroSecondaryButtonText?: string;
  heroExternalImageUrl?: string;
  heroAssetKey?: string;
  approachTitle?: string;
  approachHighlight?: string;
  approachDescription?: string;
  approachSecondaryDescription?: string;
  approachCtaText?: string;
  approachExternalImageUrl?: string;
  approachAssetKey?: string;
  contactHeading?: string;
  contactDescription?: string;
  contactPhone?: string;
  contactWhatsapp?: string;
  contactEmail?: string;
  contactAddress?: string;
  contactHours?: string;
  mapUrl?: string;
  facebookUrl?: string;
  instagramUrl?: string;
  tiktokUrl?: string;
  linkedinUrl?: string;
  seoTitle?: string;
  seoDescription?: string;
  seoSiteName?: string;
  seoExternalImageUrl?: string;
  seoAssetKey?: string;
  specialties: WebsiteSpecialtyAdmin[];
  benefits: WebsiteBenefitAdmin[];
  processSteps: WebsiteProcessStepAdmin[];
  professionals: WebsiteProfessionalAdmin[];
}

export interface WebsiteSpecialtyAdmin {
  id?: number;
  code: string;
  label: string;
  title: string;
  subtitle?: string;
  iconCode: string;
  displayOrder: number;
  visible: boolean;
  services: WebsiteSpecialtyServiceAdmin[];
}

export interface WebsiteSpecialtyServiceAdmin { id?: number; name: string; displayOrder: number; active: boolean; }
export interface WebsiteBenefitAdmin { id?: number; title: string; description?: string; iconCode: string; displayOrder: number; active: boolean; }
export interface WebsiteProcessStepAdmin { id?: number; stepNumber: number; title: string; description?: string; displayOrder: number; active: boolean; }
export interface WebsiteProfessionalAdmin { id?: number; name: string; specialty?: string; licenseNumber?: string; description?: string; experience?: string; careAreas?: string; photoExternalUrl?: string; photoAssetKey?: string; displayOrder: number; active: boolean; }
