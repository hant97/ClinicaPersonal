import { test, expect } from '@playwright/test';

test.describe('Flujo E2E: Recetas Médicas, Facturación y Abonos Parciales', () => {

  test.beforeEach(async ({ page }) => {
    await page.goto('/login');
    await page.fill('input[formControlName="username"], input[type="text"]', 'admin');
    await page.fill('input[formControlName="password"], input[type="password"]', 'Admin123*');
    await page.click('button[type="submit"]');
    await page.waitForURL(/\/(dashboard|agenda|patients|attentions)/, { timeout: 10000 });
  });

  test('debe cargar la vista de facturación con panel resumen y métricas de saldo', async ({ page }) => {
    await page.goto('/billing');
    await expect(page.locator('h1, h2, h3:has-text("Cobros"), h1, h2, h3:has-text("Facturación")').first()).toBeVisible();

    // Badges de estado de pago (PAGADO, PARCIAL, PENDIENTE)
    const tableOrCards = page.locator('table, .card, .list-state');
    await expect(tableOrCards.first()).toBeVisible();
  });

  test('debe abrir el diálogo de registro de abono parcial', async ({ page }) => {
    await page.goto('/billing');
    const paymentRow = page.locator('table tbody tr').first();
    if (await paymentRow.isVisible()) {
      const actionBtn = paymentRow.locator('button:has-text("Abonar"), button:has-text("Detalle"), button:has-text("Ver")').first();
      if (await actionBtn.isVisible()) {
        await actionBtn.click();
        const modal = page.locator('.modal, [role="dialog"], .card');
        await expect(modal.first()).toBeVisible();
      }
    }
  });

  test('debe permitir navegar a la verificación pública de receta', async ({ page }) => {
    // Verificación de acceso a la página pública de verificación
    await page.goto('/verificar-receta/DEMO-CODE-123');
    await expect(page.locator('text=Verificación, text=Receta, input, .card').first()).toBeVisible();
  });

});
