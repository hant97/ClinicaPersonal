import { Injectable } from '@angular/core';
import { Observable } from 'rxjs';
import { catchError, of } from 'rxjs';
import { CatalogService } from '../../../core/services/catalog.service';
import { SpecialtyService } from '../../../core/services/specialty.service';
import { Catalog, CatalogItem } from '../../../core/models/catalog.model';
import { SpecialtyItem } from '../../../core/models/specialty.model';

const FALLBACK_SPECIALTIES: SpecialtyItem[] = [
  { id: 1, code: 'PSICOLOGIA', name: 'Psicología', active: true, displayOrder: 1 },
  { id: 2, code: 'DERMATOLOGIA', name: 'Dermatología', active: true, displayOrder: 2 }
];

/**
 * Orquesta las llamadas HTTP que necesita la vista de administración de
 * catálogos (CatalogManagementComponent): especialidades disponibles,
 * catálogos accesibles y el CRUD/reordenamiento de sus opciones.
 */
@Injectable({
  providedIn: 'root'
})
export class CatalogManagementDataService {
  constructor(
    private catalogService: CatalogService,
    private specialtyService: SpecialtyService
  ) {}

  /** Si el backend falla, se ofrecen las dos especialidades base como respaldo. */
  loadSpecialties(): Observable<SpecialtyItem[]> {
    return this.specialtyService.getActiveSpecialties().pipe(
      catchError(() => of(FALLBACK_SPECIALTIES))
    );
  }

  loadCatalogs(isGlobalAdmin: boolean): Observable<Catalog[]> {
    return this.catalogService.getAllAccessibleCatalogs(isGlobalAdmin ? 'ALL' : undefined);
  }

  addCatalogItem(catalogCode: string, item: CatalogItem): Observable<CatalogItem> {
    return this.catalogService.addCatalogItem(catalogCode, item);
  }

  updateCatalogItem(itemId: number, item: CatalogItem, catalogCode?: string): Observable<CatalogItem> {
    return this.catalogService.updateCatalogItem(itemId, item, catalogCode);
  }

  reorderCatalogItems(catalogCode: string, itemIds: number[]): Observable<CatalogItem[]> {
    return this.catalogService.reorderCatalogItems(catalogCode, itemIds);
  }

  deleteCatalogItem(itemId: number, catalogCode?: string): Observable<void> {
    return this.catalogService.deleteCatalogItem(itemId, catalogCode);
  }

  createCatalog(catalog: Catalog): Observable<Catalog> {
    return this.catalogService.createCatalog(catalog);
  }

  updateCatalog(id: number, catalog: Partial<Catalog>): Observable<Catalog> {
    return this.catalogService.updateCatalog(id, catalog);
  }
}
