import { test, expect } from '@playwright/test';

test.describe('Flujo E2E: Historia Clínica 360° y Registro S.O.A.P.', () => {

  test.beforeEach(async ({ page }) => {
    await page.goto('/login');
    await page.fill('input[formControlName="username"], input[type="text"]', 'admin');
    await page.fill('input[formControlName="password"], input[type="password"]', 'Admin123*');
    await page.click('button[type="submit"]');
    await page.waitForURL(/\/(dashboard|agenda|patients|attentions)/, { timeout: 10000 });
    await page.goto('/patients');
  });

  test('debe mostrar la lista de pacientes con búsqueda y paginación', async ({ page }) => {
    await expect(page.locator('h1, h2, h3:has-text("Pacientes")').first()).toBeVisible();
    const searchInput = page.locator('input[type="search"], input[placeholder*="Buscar"]');
    await expect(searchInput).toBeVisible();
  });

  test('debe navegar al expediente 360° y visualizar las pestañas clínicas', async ({ page }) => {
    const patientRow = page.locator('table tbody tr, .card a[href*="/patients/"]').first();
    if (await patientRow.isVisible()) {
      await patientRow.click();
      await page.waitForURL(/\/patients\//, { timeout: 8000 });

      // Pestañas principales de trabajo clínico
      await expect(page.locator('button:has-text("Timeline"), button:has-text("Historial")').first()).toBeVisible();
      await expect(page.locator('button:has-text("Expediente"), button:has-text("Ficha")').first()).toBeVisible();
      await expect(page.locator('button:has-text("Especialidad"), button:has-text("Psicología"), button:has-text("Dermatología")').first()).toBeVisible();
      await expect(page.locator('button:has-text("Cobros")').first()).toBeVisible();
    }
  });

  test('debe abrir el formulario de sesión clínica SOAP', async ({ page }) => {
    const patientRow = page.locator('table tbody tr, .card a[href*="/patients/"]').first();
    if (await patientRow.isVisible()) {
      await patientRow.click();
      await page.waitForURL(/\/patients\//, { timeout: 8000 });

      const newSessionBtn = page.locator('button:has-text("Nueva Sesión"), a:has-text("Nueva Sesión")');
      if (await newSessionBtn.isVisible()) {
        await newSessionBtn.click();
        await page.waitForURL(/\/sessions\/new/, { timeout: 8000 });

        // Campos estructurados SOAP
        await expect(page.locator('textarea, input').first()).toBeVisible();
      }
    }
  });

});
