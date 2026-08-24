import { Locator, Page } from '@playwright/test';
import { expect, test } from './fixtures/clinic-test';

interface SmokeRoute {
  path: string;
  heading: RegExp;
  primaryAction?: RegExp;
}

const smokeRoutes: SmokeRoute[] = [
  { path: '/dashboard', heading: /Panel|Dashboard/i, primaryAction: /Nuevo Paciente/i },
  { path: '/agenda', heading: /Agenda Clínica/i, primaryAction: /Agendar Cita/i },
  { path: '/attentions', heading: /Atenciones Clínicas/i, primaryAction: /Nueva Atención Rápida/i },
  { path: '/patients', heading: /Directorio de Pacientes/i, primaryAction: /Nuevo Paciente/i },
  { path: '/billing', heading: /Cobros y Facturación/i, primaryAction: /Registrar Cobro/i },
  { path: '/services', heading: /Servicios Clínicos/i, primaryAction: /Nuevo Servicio/i },
  { path: '/inventory', heading: /Inventario de Insumos/i, primaryAction: /Nuevo Insumo/i },
  { path: '/tests-catalog', heading: /Pruebas Psicométricas/i, primaryAction: /Nueva prueba/i },
  { path: '/settings/catalogs', heading: /Administración de Catálogos/i, primaryAction: /Nuevo Catálogo/i },
  { path: '/settings/users', heading: /Personal y Cuentas/i, primaryAction: /Nuevo Usuario/i },
  { path: '/settings/audit', heading: /Registro de Auditoría/i },
  { path: '/profile', heading: /Mi Perfil y Cuenta/i },
];

const allowedHorizontalScrollSelectors = [
  '.scroll-region',
  '.table-responsive',
  '.scroll-touch',
];

async function expectNoGlobalOverflow(page: Page): Promise<void> {
  const overflow = await page.evaluate(() => ({
    viewportWidth: document.documentElement.clientWidth,
    documentWidth: document.documentElement.scrollWidth,
  }));

  expect(
    overflow.documentWidth - overflow.viewportWidth,
    `Overflow global: documento ${overflow.documentWidth}px, viewport ${overflow.viewportWidth}px`,
  ).toBeLessThanOrEqual(1);
}

async function expectOnlyAllowedHorizontalScroll(page: Page): Promise<void> {
  const unexpected = await page.evaluate((allowedSelectors) => {
    const selector = allowedSelectors.join(',');
    return Array.from(document.querySelectorAll<HTMLElement>('main *'))
      .filter((element) => {
        const style = getComputedStyle(element);
        const scrollable = style.overflowX === 'auto' || style.overflowX === 'scroll';
        return scrollable && element.scrollWidth - element.clientWidth > 1;
      })
      .filter((element) => !element.matches(selector))
      .map((element) => ({
        tag: element.tagName.toLowerCase(),
        classes: element.className.toString().slice(0, 160),
        clientWidth: element.clientWidth,
        scrollWidth: element.scrollWidth,
      }));
  }, allowedHorizontalScrollSelectors);

  expect(unexpected, 'Aparecieron contenedores desplazables fuera de la lista autorizada').toEqual([]);
}

async function expectSelectedView(button: Locator): Promise<void> {
  await expect(button).toBeVisible();
  await expect(button).toHaveClass(/bg-surface/);
}

test.describe('Protección responsive de rutas privadas', () => {
  test('mantiene título, acción primaria y overflow bajo control', async ({ page }) => {
    const pageErrors: string[] = [];
    page.on('pageerror', (error) => pageErrors.push(error.message));

    for (const route of smokeRoutes) {
      await test.step(route.path, async () => {
        const previousErrorCount = pageErrors.length;
        await page.goto(route.path);

        const heading = page.locator('main h1').first();
        await expect(heading).toBeVisible();
        await expect(heading).toContainText(route.heading);

        if (route.primaryAction) {
          const primaryAction = page
            .locator('main button, main a')
            .filter({ hasText: route.primaryAction })
            .first();
          await expect(primaryAction).toBeVisible();
        }

        await expectNoGlobalOverflow(page);
        await expectOnlyAllowedHorizontalScroll(page);
        expect(pageErrors.slice(previousErrorCount), `Errores de página en ${route.path}`).toEqual([]);
      });
    }
  });

  test('aplica el ancho de navegación previsto para cada perfil', async ({ page }, testInfo) => {
    await page.goto('/dashboard');
    const navigation = page.locator('#main-navigation');
    await expect(navigation).toBeVisible();

    const width = (await navigation.boundingBox())?.width ?? 0;
    const expectedWidth = testInfo.project.name.startsWith('tablet-') ? 64 : 256;
    expect(Math.abs(width - expectedWidth), `Sidebar de ${width}px; se esperaban ${expectedWidth}px`).toBeLessThanOrEqual(1);
  });

  test('selecciona vistas compactas solo cuando el contenido lo requiere', async ({ page }, testInfo) => {
    const isPortraitTablet = testInfo.project.name === 'tablet-portrait';

    await page.goto('/agenda');
    await expectSelectedView(page.getByRole('button', { name: isPortraitTablet ? 'Lista' : 'Semana', exact: true }));
    if (!isPortraitTablet) {
      await expect(page.getByLabel('Calendario semanal desplazable')).toBeVisible();
    }

    for (const path of ['/billing', '/inventory']) {
      await page.goto(path);
      await expectSelectedView(
        page.locator(`button[title="Vista de ${isPortraitTablet ? 'tarjetas' : 'tabla'}"]`),
      );
      await expectNoGlobalOverflow(page);
    }
  });

  test('mantiene los KPI dentro de sus tarjetas', async ({ page }) => {
    for (const path of ['/dashboard', '/attentions', '/billing', '/inventory']) {
      await test.step(path, async () => {
        await page.goto(path);
        const kpiGrid = page.getByTestId('kpi-grid');
        await expect(kpiGrid).toBeVisible();

        const overflowingElements = await kpiGrid.evaluate((grid) =>
          [grid, ...Array.from(grid.children)].flatMap((element, index) => {
            const htmlElement = element as HTMLElement;
            const overflow = htmlElement.scrollWidth - htmlElement.clientWidth;
            return overflow > 1
              ? [{ index, clientWidth: htmlElement.clientWidth, scrollWidth: htmlElement.scrollWidth }]
              : [];
          }),
        );

        expect(overflowingElements, `KPI fuera de su contenedor en ${path}`).toEqual([]);
      });
    }
  });

  test('adapta maestro-detalle y columnas de auditoría al espacio disponible', async ({ page }, testInfo) => {
    await page.goto('/settings/catalogs');
    const catalogColumns = await page.locator('.catalog-master-detail').evaluate((element) =>
      getComputedStyle(element).gridTemplateColumns.split(' ').filter(Boolean).length,
    );
    const expectsMasterDetail = testInfo.project.name.startsWith('desktop-');
    expect(catalogColumns).toBe(expectsMasterDetail ? 2 : 1);

    await page.goto('/settings/audit');
    const secondaryColumnDisplay = await page.locator('.audit-secondary-column').first().evaluate(
      (element) => getComputedStyle(element).display,
    );
    expect(secondaryColumnDisplay).toBe(testInfo.project.name === 'desktop-wide' ? 'table-cell' : 'none');
    await expectNoGlobalOverflow(page);
  });

  test('recalcula sidebar, vista activa y overflow al cambiar orientación', async ({ page }, testInfo) => {
    test.skip(testInfo.project.name !== 'tablet-portrait', 'La rotación se cubre desde el perfil tablet vertical.');

    await page.goto('/agenda');
    await expectSelectedView(page.getByRole('button', { name: 'Lista' }));

    await page.setViewportSize({ width: 1024, height: 768 });
    await expect(page.locator('#main-navigation')).toHaveCSS('width', '64px');
    await expectNoGlobalOverflow(page);

    await page.getByRole('button', { name: 'Semana' }).click();
    await expect(page.getByLabel('Calendario semanal desplazable')).toBeVisible();
    await expectNoGlobalOverflow(page);
  });
});
