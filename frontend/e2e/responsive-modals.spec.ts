import { Locator, Page } from '@playwright/test';
import { expect, test } from './fixtures/clinic-test';

async function expectNoGlobalOverflow(page: Page): Promise<void> {
  const overflow = await page.evaluate(() =>
    document.documentElement.scrollWidth - document.documentElement.clientWidth,
  );
  expect(overflow, 'El modal produjo overflow horizontal global').toBeLessThanOrEqual(1);
}

async function expectDialogUsable(
  page: Page,
  dialog: Locator,
  headingName: RegExp,
  finalActionName: RegExp,
): Promise<void> {
  await expect(dialog).toBeVisible();
  await expect(dialog.getByRole('heading', { name: headingName })).toBeVisible();

  const viewport = page.viewportSize();
  const bounds = await dialog.boundingBox();
  expect(bounds).not.toBeNull();
  expect(bounds!.width).toBeLessThanOrEqual(viewport!.width + 1);
  expect(bounds!.height).toBeLessThanOrEqual(viewport!.height + 1);

  const finalAction = dialog.getByRole('button', { name: finalActionName }).last();
  await finalAction.scrollIntoViewIfNeeded();
  await expect(finalAction).toBeVisible();
  await expectNoGlobalOverflow(page);
}

test.describe('Modales responsive y gestión de foco', () => {
  test('panel de cita mantiene cabecera, acciones y foco al rotar tablet', async ({ page }, testInfo) => {
    await page.goto('/agenda');
    const trigger = page.getByRole('button', { name: 'Agendar Cita', exact: true });
    await trigger.click();

    const dialog = page.getByRole('dialog');
    await expectDialogUsable(page, dialog, /Agendar Nueva Cita/i, /Confirmar Cita/i);

    if (testInfo.project.name === 'tablet-portrait') {
      await page.setViewportSize({ width: 1024, height: 768 });
      await expectDialogUsable(page, dialog, /Agendar Nueva Cita/i, /Confirmar Cita/i);
    }

    await page.keyboard.press('Escape');
    await expect(dialog).toBeHidden();
    await expect(trigger).toBeFocused();
  });

  test('modal de disponibilidad es navegable y restaura el foco', async ({ page }) => {
    await page.goto('/agenda');
    const trigger = page.getByRole('button', { name: 'Disponibilidad y Bloqueos', exact: true });
    await trigger.click();

    const dialog = page.getByRole('dialog', { name: 'Disponibilidad y bloqueos de agenda' });
    await expectDialogUsable(page, dialog, /Disponibilidad y Bloqueos de Agenda/i, /Guardar Horario Semanal/i);

    await page.keyboard.press('Escape');
    await expect(dialog).toBeHidden();
    await expect(trigger).toBeFocused();
  });

  test('modal de cobro conserva accesibles la cabecera y la acción final', async ({ page }) => {
    await page.goto('/billing');
    const trigger = page.getByRole('button', { name: 'Registrar Cobro', exact: true });
    await trigger.click();

    const dialog = page.getByRole('dialog');
    await expectDialogUsable(page, dialog, /Registrar Cobro de Atención/i, /Confirmar S\//i);

    await page.keyboard.press('Escape');
    await expect(dialog).toBeHidden();
    await expect(trigger).toBeFocused();
  });
});
