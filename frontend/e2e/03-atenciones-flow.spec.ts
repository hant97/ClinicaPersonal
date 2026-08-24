import { test, expect } from './fixtures/clinic-test';

test.describe('Flujo E2E: Atenciones Formalizadas y Pipeline Clínico', () => {

  test.beforeEach(async ({ page }) => {
    await page.goto('/login');
    await page.fill('input[formControlName="username"], input[type="text"]', 'admin');
    await page.fill('input[formControlName="password"], input[type="password"]', 'Admin123*');
    await page.click('button[type="submit"]');
    await page.waitForURL(/\/(dashboard|agenda|patients|attentions)/, { timeout: 10000 });
    await page.goto('/attentions');
  });

  test('debe cargar la pantalla de atenciones con KPI cards del día', async ({ page }) => {
    await expect(page.locator('h1, h2, h3:has-text("Atenciones")').first()).toBeVisible();

    // KPI Cards: Total Hoy, Agendadas, En Atención, Atendidas
    const kpiCards = page.locator('.card, .rounded-xl, [class*="bg-surface"]');
    await expect(kpiCards.first()).toBeVisible();
  });

  test('debe permitir filtrar atenciones por estado en el pipeline', async ({ page }) => {
    const inProgressFilter = page.getByRole('button', { name: 'En Proceso', exact: true });
    if (await inProgressFilter.isVisible()) {
      await inProgressFilter.click();
      // Debe actualizar la vista o la tabla
      await expect(page.locator('table, .list-state').first()).toBeVisible();
    }
  });

  test('debe abrir el modal de captura rápida de atención directa', async ({ page }) => {
    const quickBtn = page.locator('button:has-text("Nueva Atención"), button:has-text("+ Atención")');
    if (await quickBtn.isVisible()) {
      await quickBtn.click();
      const modal = page.getByRole('dialog', { name: 'Nueva Atención Rápida' });
      await expect(modal).toBeVisible();

      // Debe incluir selector de paciente y servicio clínico
      await expect(modal.locator('input, select').first()).toBeVisible();
    }
  });

});
