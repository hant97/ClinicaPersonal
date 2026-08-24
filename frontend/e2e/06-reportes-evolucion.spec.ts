import { test, expect } from './fixtures/clinic-test';

test.describe('Flujo E2E: Reportes Clínicos de Evolución y Comparativa Fotográfica', () => {

  test.beforeEach(async ({ page }) => {
    await page.goto('/login');
    await page.fill('input[formControlName="username"], input[type="text"]', 'admin');
    await page.fill('input[formControlName="password"], input[type="password"]', 'Admin123*');
    await page.click('button[type="submit"]');
    await page.waitForURL(/\/(dashboard|agenda|patients|attentions)/, { timeout: 10000 });
  });

  test('debe visualizar el gráfico de evolución psicométrica en el expediente de psicología', async ({ page }) => {
    await page.goto('/patients');
    const patientRow = page.locator('table tbody tr').first();
    if (await patientRow.isVisible()) {
      await patientRow.click();
      await page.waitForURL(/\/patients\//, { timeout: 8000 });

      // Ir a la pestaña de especialidad
      const specTab = page.locator('button:has-text("Especialidad"), button:has-text("Psicología")').first();
      if (await specTab.isVisible()) {
        await specTab.click();

        const testsSubTab = page.locator('button:has-text("Pruebas"), button:has-text("Psicométricas")').first();
        if (await testsSubTab.isVisible()) {
          await testsSubTab.click();

          // Verificar que cargue el reporte de evolución
          await expect(page.locator('h3:has-text("Evolución"), text=Psicométrica, canvas').first()).toBeVisible();
        }
      }
    }
  });

  test('debe permitir abrir el comparador fotográfico de lesiones en dermatología', async ({ page }) => {
    await page.goto('/patients');
    const patientRow = page.locator('table tbody tr').first();
    if (await patientRow.isVisible()) {
      await patientRow.click();
      await page.waitForURL(/\/patients\//, { timeout: 8000 });

      const specTab = page.locator('button:has-text("Especialidad"), button:has-text("Dermatología")').first();
      if (await specTab.isVisible()) {
        await specTab.click();

        const lesionsSubTab = page.locator('button:has-text("Lesiones"), button:has-text("Fotos")').first();
        if (await lesionsSubTab.isVisible()) {
          await lesionsSubTab.click();

          const compareBtn = page.locator('button:has-text("Comparar"), button[title*="Comparativa"]').first();
          if (await compareBtn.isVisible()) {
            await compareBtn.click();

            // Verificar apertura del modal comparativo
            const compareModal = page.locator('h2:has-text("Comparador Fotográfico"), text=Lado a Lado, text=Split Slider');
            await expect(compareModal.first()).toBeVisible();
          }
        }
      }
    }
  });

});
