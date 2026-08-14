import { draftToViewLanding } from './landing-editor.mapper';
import { WebsiteDraft } from '../../../core/models/website-editor.model';

describe('draftToViewLanding', () => {
  const assetBase = '/api/v1/public/website-assets';

  function sampleDraft(): WebsiteDraft {
    return {
      commercialName: 'Clínica Demo',
      heroTitle: 'Tu bienestar',
      heroAssetKey: 'draft/hero/abc.png',
      logoAssetKey: 'logo/xyz.png',
      approachAssetKey: 'approach/def.png',
      specialties: [
        { draftKey: 's1', code: 'DERMATOLOGIA', label: '01', title: 'Dermatología', iconCode: 'MICROSCOPE', displayOrder: 1, visible: true },
        { draftKey: 's2', code: 'PSICOLOGIA', label: '02', title: 'Psicología', iconCode: 'BRAIN', displayOrder: 2, visible: false }
      ],
      benefits: [
        { draftKey: 'b1', title: 'Confianza', iconCode: 'SHIELD_CHECK', displayOrder: 1, active: true },
        { draftKey: 'b2', title: 'Oculto', iconCode: 'SPARKLES', displayOrder: 2, active: false }
      ],
      processSteps: [
        { draftKey: 'p1', stepNumber: 1, title: 'Paso uno', displayOrder: 1, active: true }
      ],
      professionals: [
        { draftKey: 'pr1', name: 'Dra. Pérez', photoAssetKey: 'draft/professionals/photo.png', displayOrder: 1, active: true },
        { draftKey: 'pr2', name: 'Dra. Oculto', displayOrder: 2, active: false }
      ]
    };
  }

  it('resuelve las claves de imagen a la URL pública y conserva las URLs externas', () => {
    const draft = sampleDraft();
    draft.heroExternalImageUrl = 'https://example.com/hero.jpg';
    draft.heroAssetKey = undefined;

    const view = draftToViewLanding(draft, assetBase);

    expect(view.hero.imageUrl).toBe('https://example.com/hero.jpg');
    expect(view.general.logoUrl).toBe(`${assetBase}/logo/xyz.png`);
    expect(view.approach.imageUrl).toBe(`${assetBase}/approach/def.png`);
  });

  it('filtra los elementos ocultos o inactivos de las colecciones', () => {
    const view = draftToViewLanding(sampleDraft(), assetBase);

    expect(view.specialties.map(s => s.code)).toEqual(['DERMATOLOGIA']);
    expect(view.benefits.map(b => b.title)).toEqual(['Confianza']);
    expect(view.processSteps.map(p => p.title)).toEqual(['Paso uno']);
    expect(view.professionals.map(p => p.name)).toEqual(['Dra. Pérez']);
  });

  it('resuelve la foto del profesional desde su asset de borrador', () => {
    const view = draftToViewLanding(sampleDraft(), assetBase);

    expect(view.professionals[0].photoUrl).toBe(`${assetBase}/draft/professionals/photo.png`);
  });

  it('tolera colecciones ausentes', () => {
    const draft = sampleDraft();
    draft.specialties = [];
    draft.benefits = undefined as unknown as WebsiteDraft['benefits'];
    draft.processSteps = undefined as unknown as WebsiteDraft['processSteps'];
    draft.professionals = undefined as unknown as WebsiteDraft['professionals'];

    const view = draftToViewLanding(draft, assetBase);

    expect(view.specialties).toEqual([]);
    expect(view.benefits).toEqual([]);
    expect(view.processSteps).toEqual([]);
    expect(view.professionals).toEqual([]);
  });
});
