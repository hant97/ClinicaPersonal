import { clearClinicalDrafts, clinicalDraftKey } from './clinical-draft.util';

describe('clinical-draft.util', () => {
  beforeEach(() => localStorage.clear());
  afterEach(() => localStorage.clear());

  it('should scope the draft key by user and patient', () => {
    expect(clinicalDraftKey('dra.perez', 42)).not.toBe(clinicalDraftKey('dr.gomez', 42));
    expect(clinicalDraftKey('dra.perez', 42)).not.toBe(clinicalDraftKey('dra.perez', 43));
  });

  it('should remove current and legacy clinical drafts only', () => {
    localStorage.setItem(clinicalDraftKey('dra.perez', 42), '{"subjective":"nota"}');
    localStorage.setItem('flowgrid_draft_session_42', '{"subjective":"nota antigua"}');
    localStorage.setItem('sidebar_expanded', 'true');

    clearClinicalDrafts();

    expect(localStorage.getItem(clinicalDraftKey('dra.perez', 42))).toBeNull();
    expect(localStorage.getItem('flowgrid_draft_session_42')).toBeNull();
    expect(localStorage.getItem('sidebar_expanded')).toBe('true');
  });
});
