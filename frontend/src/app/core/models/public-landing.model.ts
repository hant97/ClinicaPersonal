export interface PublicLanding {
  general: { commercialName?: string; tagline?: string; description?: string; logoUrl?: string };
  hero: { eyebrow?: string; title?: string; highlight?: string; description?: string; primaryButtonText?: string; secondaryButtonText?: string; imageUrl?: string };
  approach: { title?: string; highlight?: string; description?: string; secondaryDescription?: string; ctaText?: string; imageUrl?: string };
  contact: { heading?: string; description?: string; phone?: string; whatsapp?: string; email?: string; address?: string; hours?: string; mapUrl?: string };
  social: { facebook?: string; instagram?: string; tiktok?: string; linkedin?: string };
  seo: { title?: string; description?: string; siteName?: string; imageUrl?: string };
  specialties: PublicSpecialty[];
  benefits: PublicBenefit[];
  processSteps: PublicProcessStep[];
  professionals: PublicProfessional[];
}

export interface PublicSpecialty { code: string; label?: string; title?: string; subtitle?: string; iconCode?: string; services: string[]; }
export interface PublicBenefit { title?: string; description?: string; iconCode?: string; }
export interface PublicProcessStep { stepNumber: number; title?: string; description?: string; }
export interface PublicProfessional { name?: string; specialty?: string; licenseNumber?: string; description?: string; experience?: string; careAreas?: string; photoUrl?: string; }
