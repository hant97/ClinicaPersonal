import { validateDraft, isHttpUrl } from './landing-editor.validation';
import { WebsiteDraft } from '../../../core/models/website-editor.model';

describe('landing-editor.validation', () => {
  function validDraft(): WebsiteDraft {
    return {
      commercialName: 'Clínica Demo',
      heroTitle: 'Tu bienestar',
      specialties: [
        { draftKey: 's1', code: 'DERMATOLOGIA', label: '01', title: 'Dermatología', iconCode: 'MICROSCOPE', displayOrder: 1, visible: true }
      ],
      benefits: [{ draftKey: 'b1', title: 'Confianza', iconCode: 'SHIELD_CHECK', displayOrder: 1, active: true }],
      processSteps: [{ draftKey: 'p1', stepNumber: 1, title: 'Paso uno', displayOrder: 1, active: true }],
      professionals: [{ draftKey: 'pr1', name: 'Dra. Pérez', displayOrder: 1, active: true }]
    };
  }

  describe('isHttpUrl', () => {
    it('acepta URLs http y https', () => {
      expect(isHttpUrl('https://example.com')).toBe(true);
      expect(isHttpUrl('http://example.com/path')).toBe(true);
    });

    it('rechaza esquemas no http y textos inválidos', () => {
      expect(isHttpUrl('ftp://example.com')).toBe(false);
      expect(isHttpUrl('javascript:alert(1)')).toBe(false);
      expect(isHttpUrl('no es una url')).toBe(false);
    });

    it('permite valores vacíos', () => {
      expect(isHttpUrl(undefined)).toBe(true);
      expect(isHttpUrl('')).toBe(true);
    });
  });

  describe('validateDraft', () => {
    it('no reporta errores para un borrador válido', () => {
      expect(validateDraft(validDraft())).toEqual({});
    });

    it('reporta campos obligatorios', () => {
      const draft = validDraft();
      draft.commercialName = '';
      draft.heroTitle = '   ';
      const errors = validateDraft(draft);
      expect(errors['commercialName']).toBeTruthy();
      expect(errors['heroTitle']).toBeTruthy();
    });

    it('reporta longitud máxima excedida', () => {
      const draft = validDraft();
      draft.commercialName = 'x'.repeat(121);
      expect(validateDraft(draft)['commercialName']).toContain('120');
    });

    it('reporta URLs inválidas', () => {
      const draft = validDraft();
      draft.facebookUrl = 'not-a-url';
      expect(validateDraft(draft)['facebookUrl']).toBeTruthy();
    });

    it('reporta correo inválido', () => {
      const draft = validDraft();
      draft.contactEmail = 'correo-invalido';
      expect(validateDraft(draft)['contactEmail']).toBeTruthy();
    });

    it('reporta iconos no permitidos en colecciones', () => {
      const draft = validDraft();
      draft.benefits[0].iconCode = 'INVALIDO';
      expect(validateDraft(draft)['benefits.0.iconCode']).toBeTruthy();
    });
  });
});
