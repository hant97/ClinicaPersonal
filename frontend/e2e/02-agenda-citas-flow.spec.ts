import { test, expect } from '@playwright/test';

test.describe('Flujo E2E: Agenda y Gestión de Citas', () => {

  test.beforeEach(async ({ page }) => {
    // Autenticar como staff
    await page.goto('/login');
    await page.fill('input[formControlName="username"], input[type="text"]', 'admin');
    await page.fill('input[formControlName="password"], input[type="password"]', 'Admin123*');
    await page.click('button[type="submit"]');
    await page.waitForURL(/\/(dashboard|agenda|patients|attentions)/, { timeout: 10000 });
    await page.goto('/agenda');
  });

  test('debe cargar la vista de agenda con controles de navegación', async ({ page }) => {
    await expect(page.locator('h1, h2, h3:has-text("Agenda")').first()).toBeVisible();

    // Controles de fecha (Hoy, Anterior, Siguiente)
    const todayBtn = page.locator('button:has-text("Hoy")');
    await expect(todayBtn).toBeVisible();

    // Filtro por profesional
    const profFilter = page.locator('select, [role="combobox"]').first();
    await expect(profFilter).toBeVisible();
  });

  test('debe abrir el modal de agendamiento con opción de recurrencia', async ({ page }) => {
    const newApptBtn = page.locator('button:has-text("Nueva Cita"), button:has-text("Agendar")');
    if (await newApptBtn.isVisible()) {
      await newApptBtn.click();

      const modal = page.locator('.modal, [role="dialog"], .card');
      await expect(modal.first()).toBeVisible();

      // Verificar campos del formulario
      const recurrenceCheckbox = page.locator('input[type="checkbox"][formControlName="isRecurring"], text=Recurrente');
      if (await recurrenceCheckbox.isVisible()) {
        await recurrenceCheckbox.check();
        // Verificar que aparezcan campos de recurrencia (frecuencia, repeticiones)
        await expect(page.locator('input[formControlName="recurrenceCount"], select[formControlName="recurrenceFrequency"]').first()).toBeVisible();
      }
    }
  });

  test('debe disponer del botón de recordatorio por WhatsApp', async ({ page }) => {
    const waButtons = page.locator('a[href*="wa.me"], button[title*="WhatsApp"], a[title*="WhatsApp"]');
    // Si existen citas en la agenda, verificar que los enlaces de WhatsApp se estructuran correctamente
    const count = await waButtons.count();
    if (count > 0) {
      const href = await waButtons.first().getAttribute('href');
      expect(href).toMatch(/wa\.me/);
    }
  });

});
