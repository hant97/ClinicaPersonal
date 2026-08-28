import { Catalog, CatalogItem } from '../../../core/models/catalog.model';
import { SpecialtyItem } from '../../../core/models/specialty.model';

/**
 * Funciones puras usadas por la vista de administración de catálogos:
 * generación de códigos, filtrado de catálogos/opciones y resolución de
 * etiquetas/colores por especialidad.
 * Extraídas del componente para poder probarlas de forma aislada.
 */

export function slugify(text: string): string {
  if (!text) return '';
  return text
    .trim()
    .toUpperCase()
    .normalize('NFD')
    .replace(/[\u0300-\u036f]/g, '')
    .replace(/[^A-Z0-9_]/g, '_')
    .replace(/_+/g, '_')
    .replace(/^_|_$/g, '');
}

export function getSpecialtyBadgeClass(specialty?: string): string {
  switch (specialty?.toUpperCase()) {
    case 'DERMATOLOGIA':
      return 'bg-emerald-50 text-emerald-700 border-emerald-200';
    case 'PSICOLOGIA':
      return 'bg-purple-50 text-purple-700 border-purple-200';
    case 'GENERAL':
    case undefined:
      return 'bg-blue-50 text-blue-700 border-blue-200';
    default:
      return 'bg-indigo-50 text-indigo-700 border-indigo-200';
  }
}

export function getSpecialtyLabel(specialty: string | undefined, specialties: SpecialtyItem[]): string {
  if (!specialty || specialty.toUpperCase() === 'GENERAL') return 'General';
  const found = specialties.find(s => s.code.toUpperCase() === specialty.toUpperCase());
  if (found) return found.name;
  if (specialty === 'DERMATOLOGIA') return 'Dermatología';
  if (specialty === 'PSICOLOGIA') return 'Psicología';
  return specialty.charAt(0) + specialty.slice(1).toLowerCase();
}

export function filterCatalogs(catalogs: Catalog[], specialtyFilter: string, searchQuery: string): Catalog[] {
  return catalogs.filter((catalog) => {
    const matchSpecialty =
      specialtyFilter === 'ALL' ||
      (specialtyFilter === 'GENERAL' && (catalog.specialty === 'GENERAL' || !catalog.specialty)) ||
      catalog.specialty === specialtyFilter;

    const query = searchQuery.toLowerCase().trim();
    const matchQuery =
      !query ||
      catalog.name.toLowerCase().includes(query) ||
      catalog.code.toLowerCase().includes(query) ||
      (catalog.description && catalog.description.toLowerCase().includes(query));

    return matchSpecialty && matchQuery;
  });
}

export function filterItems(items: CatalogItem[], statusFilter: 'ALL' | 'ACTIVE' | 'INACTIVE', searchQuery: string): CatalogItem[] {
  return items.filter((item) => {
    const matchStatus =
      statusFilter === 'ALL' ||
      (statusFilter === 'ACTIVE' && item.isActive) ||
      (statusFilter === 'INACTIVE' && !item.isActive);

    const query = searchQuery.toLowerCase().trim();
    const matchQuery =
      !query ||
      item.itemName.toLowerCase().includes(query) ||
      item.itemCode.toLowerCase().includes(query);

    return matchStatus && matchQuery;
  });
}
