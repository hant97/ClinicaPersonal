import { buildCsv } from './export.service';

describe('buildCsv', () => {
  it('conserva columnas, caracteres españoles, fechas y montos', () => {
    const csv = buildCsv([{
      Paciente: 'José Núñez',
      Fecha: '2026-08-23',
      Monto: 125.5
    }]);

    expect(csv.charCodeAt(0)).toBe(0xFEFF);
    expect(csv).toContain('"Paciente";"Fecha";"Monto"');
    expect(csv).toContain('"José Núñez";"2026-08-23";"125.5"');
  });

  it('escapa comillas, separadores y fórmulas de hoja de cálculo', () => {
    const csv = buildCsv([{ Nota: 'Texto; "citado"', Código: '=1+1' }]);

    expect(csv).toContain('"Texto; ""citado"""');
    expect(csv).toContain('"\'=1+1"');
  });
});
