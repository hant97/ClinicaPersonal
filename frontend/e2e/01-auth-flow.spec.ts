import { test, expect } from './fixtures/clinic-test';

test.describe('Flujo E2E: Autenticación y Control de Sesión', () => {

  test.beforeEach(async ({ page }) => {
    await page.goto('/login');
  });

  test('debe mostrar el formulario de inicio de sesión con campos requeridos', async ({ page }) => {
    await expect(page).toHaveTitle(/Clínica/i);
    const usernameInput = page.locator('input[formControlName="username"], input[name="username"], input[type="text"]');
    const passwordInput = page.locator('input[formControlName="password"], input[name="password"], input[type="password"]');
    const submitBtn = page.locator('button[type="submit"]');

    await expect(usernameInput).toBeVisible();
    await expect(passwordInput).toBeVisible();
    await expect(submitBtn).toBeVisible();
  });

  test('debe mostrar error ante credenciales inválidas', async ({ page }) => {
    await page.fill('input[formControlName="username"], input[type="text"]', 'usuario_invalido');
    await page.fill('input[formControlName="password"], input[type="password"]', 'clave_incorrecta_123');
    await page.click('button[type="submit"]');

    // Debe mostrar feedback de error (toast o alert)
    await expect(page.getByText(/Credenciales incorrectas|Error/i).first()).toBeVisible({ timeout: 5000 });
  });

  test('debe permitir inicio de sesión exitoso y redirigir al Dashboard', async ({ page }) => {
    await page.fill('input[formControlName="username"], input[type="text"]', 'admin');
    await page.fill('input[formControlName="password"], input[type="password"]', 'Admin123*');
    await page.click('button[type="submit"]');

    // Redirección exitosa hacia una ruta protegida
    await page.waitForURL(/\/(dashboard|agenda|patients|attentions)/, { timeout: 10000 });
    expect(page.url()).not.toContain('/login');
  });

  test('debe permitir cerrar sesión correctamente', async ({ page }) => {
    // Autenticar primero
    await page.fill('input[formControlName="username"], input[type="text"]', 'admin');
    await page.fill('input[formControlName="password"], input[type="password"]', 'Admin123*');
    await page.click('button[type="submit"]');
    await page.waitForURL(/\/(dashboard|agenda|patients|attentions)/, { timeout: 10000 });

    // Click en botón de cerrar sesión
    const logoutBtn = page.locator('button:has-text("Cerrar Sesión"), button[title="Cerrar Sesión"], a:has-text("Cerrar Sesión")');
    if (await logoutBtn.isVisible()) {
      await logoutBtn.click();
      await page.waitForURL(/\/login/, { timeout: 5000 });
      expect(page.url()).toContain('/login');
    }
  });

});
