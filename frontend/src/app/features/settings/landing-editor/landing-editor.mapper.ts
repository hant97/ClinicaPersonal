import { PublicLanding } from '../../../core/models/public-landing.model';
import { WebsiteDraft } from '../../../core/models/website-editor.model';

export function draftToViewLanding(draft: WebsiteDraft, assetBase: string): PublicLanding {
  const resolve = (key?: string, external?: string): string | undefined =>
    key ? `${assetBase}/${key}` : external;

  return {
    general: {
      commercialName: draft.commercialName,
      tagline: draft.tagline,
      description: draft.description,
      logoUrl: resolve(draft.logoAssetKey, draft.logoExternalImageUrl)
    },
    hero: {
      eyebrow: draft.heroEyebrow,
      title: draft.heroTitle,
      highlight: draft.heroHighlight,
      description: draft.heroDescription,
      primaryButtonText: draft.heroPrimaryButtonText,
      secondaryButtonText: draft.heroSecondaryButtonText,
      imageUrl: resolve(draft.heroAssetKey, draft.heroExternalImageUrl)
    },
    approach: {
      title: draft.approachTitle,
      highlight: draft.approachHighlight,
      description: draft.approachDescription,
      secondaryDescription: draft.approachSecondaryDescription,
      ctaText: draft.approachCtaText,
      imageUrl: resolve(draft.approachAssetKey, draft.approachExternalImageUrl)
    },
    contact: {
      heading: draft.contactHeading,
      description: draft.contactDescription,
      phone: draft.contactPhone,
      whatsapp: draft.contactWhatsapp,
      email: draft.contactEmail,
      address: draft.contactAddress,
      hours: draft.contactHours,
      mapUrl: draft.mapUrl
    },
    social: {
      facebook: draft.facebookUrl,
      instagram: draft.instagramUrl,
      tiktok: draft.tiktokUrl,
      linkedin: draft.linkedinUrl
    },
    seo: {
      title: draft.seoTitle,
      description: draft.seoDescription,
      siteName: draft.seoSiteName,
      imageUrl: resolve(draft.seoAssetKey, draft.seoExternalImageUrl)
    },
    specialties: (draft.specialties || [])
      .filter(specialty => specialty.visible)
      .map(specialty => ({
        code: specialty.code,
        label: specialty.label,
        title: specialty.title,
        subtitle: specialty.subtitle,
        iconCode: specialty.iconCode,
        services: []
      })),
    benefits: (draft.benefits || [])
      .filter(benefit => benefit.active)
      .map(benefit => ({ title: benefit.title, description: benefit.description, iconCode: benefit.iconCode })),
    processSteps: (draft.processSteps || [])
      .filter(step => step.active)
      .map(step => ({ stepNumber: step.stepNumber, title: step.title, description: step.description })),
    professionals: (draft.professionals || [])
      .filter(professional => professional.active)
      .map(professional => ({
        name: professional.name,
        specialty: professional.specialty,
        licenseNumber: professional.licenseNumber,
        description: professional.description,
        experience: professional.experience,
        careAreas: professional.careAreas,
        photoUrl: resolve(professional.photoAssetKey, professional.photoExternalUrl)
      }))
  };
}
