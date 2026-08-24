import { Component, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import {
  LucideAngularModule,
  FolderTree,
  BookOpen,
  Plus,
  Search,
  Edit2,
  Trash2,
  Check,
  X,
  ArrowUp,
  ArrowDown,
  Layers,
  Sparkles,
  Copy,
  CheckCheck,
  Globe,
  Brain,
  Stethoscope,
  Tag,
  SlidersHorizontal,
  RefreshCw,
  AlertCircle
} from 'lucide-angular';
import { CatalogService } from '../../../core/services/catalog.service';
import { SpecialtyService } from '../../../core/services/specialty.service';
import { Catalog, CatalogItem } from '../../../core/models/catalog.model';
import { SpecialtyItem } from '../../../core/models/specialty.model';
import { ToastService } from '../../../shared/services/toast/toast.service';
import { AuthService } from '../../../core/services/auth.service';

@Component({
  selector: 'app-catalog-management',
  standalone: true,
  imports: [CommonModule, FormsModule, LucideAngularModule],
  templateUrl: './catalog-management.component.html',
  styleUrls: ['./catalog-management.component.css'],
})
export class CatalogManagementComponent implements OnInit {
  // Iconos
  readonly FolderTree = FolderTree;
  readonly BookOpen = BookOpen;
  readonly Plus = Plus;
  readonly Search = Search;
  readonly Edit2 = Edit2;
  readonly Trash2 = Trash2;
  readonly Check = Check;
  readonly X = X;
  readonly ArrowUp = ArrowUp;
  readonly ArrowDown = ArrowDown;
  readonly Layers = Layers;
  readonly Sparkles = Sparkles;
  readonly Copy = Copy;
  readonly CheckCheck = CheckCheck;
  readonly Globe = Globe;
  readonly Brain = Brain;
  readonly Stethoscope = Stethoscope;
  readonly Tag = Tag;
  readonly SlidersHorizontal = SlidersHorizontal;
  readonly RefreshCw = RefreshCw;
  readonly AlertCircle = AlertCircle;

  // Estado general
  catalogs: Catalog[] = [];
  specialties: SpecialtyItem[] = [];
  selectedCatalog: Catalog | null = null;
  items: CatalogItem[] = [];
  loadingCatalogs: boolean = false;
  loadingItems: boolean = false;
  savingItem: boolean = false;
  copiedCode: boolean = false;

  // Filtros de catálogos
  catalogSearchQuery: string = '';
  specialtyFilter: string = 'ALL'; // 'ALL' | 'GENERAL' | 'PSICOLOGIA' | 'DERMATOLOGIA' | custom

  // Filtros de items
  itemSearchQuery: string = '';
  itemStatusFilter: 'ALL' | 'ACTIVE' | 'INACTIVE' = 'ALL';

  // Formulario de agregar item
  newItemName: string = '';
  newItemCode: string = '';
  isCustomItemCodeExpanded: boolean = false;

  // Edición inline de item
  editingItemId: number | null = null;
  editingItemName: string = '';

  // Modal de Catálogo (Crear / Editar)
  showCatalogModal: boolean = false;
  isEditingCatalog: boolean = false;
  catalogFormId?: number;
  catalogFormName: string = '';
  catalogFormCode: string = '';
  catalogFormDescription: string = '';
  catalogFormSpecialty: string = 'GENERAL';
  isCustomCatalogCodeExpanded: boolean = false;
  savingCatalog: boolean = false;

  // Modal de Eliminación de Item
  showDeleteModal: boolean = false;
  itemToDelete: CatalogItem | null = null;
  isDeletingItem: boolean = false;

  constructor(
    private catalogService: CatalogService,
    private specialtyService: SpecialtyService,
    private toastService: ToastService,
    private authService: AuthService
  ) {}

  ngOnInit(): void {
    this.loadSpecialties();
    this.loadCatalogs();
  }

  loadSpecialties(): void {
    this.specialtyService.getActiveSpecialties().subscribe({
      next: (list) => {
        this.specialties = list || [];
      },
      error: () => {
        this.specialties = [
          { id: 1, code: 'PSICOLOGIA', name: 'Psicología', active: true, displayOrder: 1 },
          { id: 2, code: 'DERMATOLOGIA', name: 'Dermatología', active: true, displayOrder: 2 }
        ];
      }
    });
  }

  loadCatalogs(selectedCodeToKeep?: string): void {
    this.loadingCatalogs = true;
    const requestedSpecialty = this.authService.hasRole('ROLE_SITE_ADMIN') ? 'ALL' : undefined;
    this.catalogService.getAllAccessibleCatalogs(requestedSpecialty).subscribe({
      next: (catalogs) => {
        this.catalogs = catalogs || [];
        this.loadingCatalogs = false;
        
        if (selectedCodeToKeep) {
          const found = this.catalogs.find(c => c.code === selectedCodeToKeep);
          if (found) {
            this.selectCatalog(found);
            return;
          }
        }

        if (this.selectedCatalog) {
          const stillExists = this.catalogs.find(c => c.id === this.selectedCatalog?.id || c.code === this.selectedCatalog?.code);
          if (stillExists) {
            this.selectCatalog(stillExists);
            return;
          }
        }

        if (this.filteredCatalogs.length > 0) {
          this.selectCatalog(this.filteredCatalogs[0]);
        } else if (this.catalogs.length > 0) {
          this.selectCatalog(this.catalogs[0]);
        } else {
          this.selectedCatalog = null;
          this.items = [];
        }
      },
      error: (err) => {
        console.error(err);
        this.loadingCatalogs = false;
        this.toastService.error('Error al cargar los catálogos del sistema');
      }
    });
  }

  get filteredCatalogs(): Catalog[] {
    return this.catalogs.filter((catalog) => {
      const matchSpecialty =
        this.specialtyFilter === 'ALL' ||
        (this.specialtyFilter === 'GENERAL' && (catalog.specialty === 'GENERAL' || !catalog.specialty)) ||
        catalog.specialty === this.specialtyFilter;

      const query = this.catalogSearchQuery.toLowerCase().trim();
      const matchQuery =
        !query ||
        catalog.name.toLowerCase().includes(query) ||
        catalog.code.toLowerCase().includes(query) ||
        (catalog.description && catalog.description.toLowerCase().includes(query));

      return matchSpecialty && matchQuery;
    });
  }

  get filteredItems(): CatalogItem[] {
    return this.items.filter((item) => {
      const matchStatus =
        this.itemStatusFilter === 'ALL' ||
        (this.itemStatusFilter === 'ACTIVE' && item.isActive) ||
        (this.itemStatusFilter === 'INACTIVE' && !item.isActive);

      const query = this.itemSearchQuery.toLowerCase().trim();
      const matchQuery =
        !query ||
        item.itemName.toLowerCase().includes(query) ||
        item.itemCode.toLowerCase().includes(query);

      return matchStatus && matchQuery;
    });
  }

  get activeItemsCount(): number {
    return this.items.filter(i => i.isActive).length;
  }

  get totalItemsAcrossAllCatalogs(): number {
    return this.catalogs.reduce((acc, cat) => acc + (cat.items?.length || 0), 0);
  }

  selectCatalog(catalog: Catalog): void {
    this.selectedCatalog = catalog;
    this.items = (catalog.items || []).slice().sort((a, b) => (a.orderIndex ?? 0) - (b.orderIndex ?? 0));
    this.resetItemForm();
    this.cancelEditItem();
  }

  setSpecialtyFilter(specialty: string): void {
    this.specialtyFilter = specialty;
    const available = this.filteredCatalogs;
    if (available.length > 0 && (!this.selectedCatalog || !available.some(c => c.id === this.selectedCatalog?.id))) {
      this.selectCatalog(available[0]);
    }
  }

  // --- Manejo de Items ---

  onNewItemNameChange(): void {
    if (!this.isCustomItemCodeExpanded) {
      this.newItemCode = this.slugify(this.newItemName);
    }
  }

  toggleCustomItemCode(): void {
    this.isCustomItemCodeExpanded = !this.isCustomItemCodeExpanded;
    if (!this.isCustomItemCodeExpanded) {
      this.newItemCode = this.slugify(this.newItemName);
    }
  }

  addItem(): void {
    if (!this.selectedCatalog || !this.newItemName.trim()) {
      return;
    }

    const code = this.newItemCode.trim() ? this.newItemCode.trim().toUpperCase() : this.slugify(this.newItemName);
    
    // Validar duplicado local
    const exists = this.items.some(i => i.itemCode.toUpperCase() === code.toUpperCase());
    if (exists) {
      this.toastService.warning(`Ya existe una opción con el código "${code}" en este catálogo`);
      return;
    }

    const newItem: CatalogItem = {
      itemCode: code,
      itemName: this.newItemName.trim(),
      isActive: true,
      orderIndex: this.items.length
    };

    this.savingItem = true;
    this.catalogService.addCatalogItem(this.selectedCatalog.code, newItem).subscribe({
      next: (savedItem) => {
        this.items.push(savedItem);
        if (this.selectedCatalog && this.selectedCatalog.items) {
          this.selectedCatalog.items.push(savedItem);
        }
        this.resetItemForm();
        this.savingItem = false;
        this.toastService.success(`Opción "${savedItem.itemName}" agregada correctamente`);
      },
      error: (err) => {
        console.error(err);
        this.savingItem = false;
        const msg = err.error?.message || 'Error al agregar la opción';
        this.toastService.error(msg);
      }
    });
  }

  resetItemForm(): void {
    this.newItemName = '';
    this.newItemCode = '';
    this.isCustomItemCodeExpanded = false;
  }

  startEditItem(item: CatalogItem): void {
    this.editingItemId = item.id!;
    this.editingItemName = item.itemName;
  }

  cancelEditItem(): void {
    this.editingItemId = null;
    this.editingItemName = '';
  }

  saveEditItem(item: CatalogItem): void {
    if (!this.editingItemName.trim() || !item.id) {
      return;
    }

    const updatedItem: CatalogItem = {
      ...item,
      itemName: this.editingItemName.trim()
    };

    this.catalogService.updateCatalogItem(item.id, updatedItem, this.selectedCatalog?.code).subscribe({
      next: (saved) => {
        item.itemName = saved.itemName;
        this.cancelEditItem();
        this.toastService.success('Opción actualizada correctamente');
      },
      error: (err) => {
        console.error(err);
        this.toastService.error('No se pudo actualizar la opción');
      }
    });
  }

  toggleActive(item: CatalogItem): void {
    if (!item.id) return;
    const updatedItem: CatalogItem = {
      ...item,
      isActive: item.isActive
    };

    this.catalogService.updateCatalogItem(item.id, updatedItem, this.selectedCatalog?.code).subscribe({
      next: (saved) => {
        item.isActive = saved.isActive;
        this.toastService.success(`Opción ${item.isActive ? 'activada' : 'desactivada'} correctamente`);
      },
      error: (err) => {
        console.error(err);
        item.isActive = !item.isActive;
        this.toastService.error('No se pudo actualizar el estado de la opción');
      }
    });
  }

  moveItemUp(index: number): void {
    if (index <= 0 || !this.selectedCatalog) return;
    const currentList = [...this.items];
    const temp = currentList[index];
    currentList[index] = currentList[index - 1];
    currentList[index - 1] = temp;

    this.applyAndPersistOrder(currentList);
  }

  moveItemDown(index: number): void {
    if (index >= this.items.length - 1 || !this.selectedCatalog) return;
    const currentList = [...this.items];
    const temp = currentList[index];
    currentList[index] = currentList[index + 1];
    currentList[index + 1] = temp;

    this.applyAndPersistOrder(currentList);
  }

  private applyAndPersistOrder(orderedItems: CatalogItem[]): void {
    if (!this.selectedCatalog) return;
    this.items = orderedItems;
    const ids = orderedItems.map(i => i.id!).filter(id => !!id);

    this.catalogService.reorderCatalogItems(this.selectedCatalog.code, ids).subscribe({
      next: (savedItems) => {
        this.items = savedItems;
        if (this.selectedCatalog) {
          this.selectedCatalog.items = savedItems;
        }
        this.toastService.success('Orden actualizado correctamente');
      },
      error: (err) => {
        console.error(err);
        this.toastService.error('Error al guardar el nuevo orden');
      }
    });
  }

  confirmDeleteItem(item: CatalogItem): void {
    this.itemToDelete = item;
    this.showDeleteModal = true;
  }

  closeDeleteModal(): void {
    this.showDeleteModal = false;
    this.itemToDelete = null;
  }

  executeDeleteItem(): void {
    if (!this.itemToDelete || !this.itemToDelete.id || !this.selectedCatalog) return;

    this.isDeletingItem = true;
    const id = this.itemToDelete.id;
    this.catalogService.deleteCatalogItem(id, this.selectedCatalog.code).subscribe({
      next: () => {
        this.items = this.items.filter(i => i.id !== id);
        if (this.selectedCatalog && this.selectedCatalog.items) {
          this.selectedCatalog.items = this.selectedCatalog.items.filter(i => i.id !== id);
        }
        this.isDeletingItem = false;
        this.closeDeleteModal();
        this.toastService.success('Opción eliminada del catálogo');
      },
      error: (err) => {
        console.error(err);
        this.isDeletingItem = false;
        this.toastService.error('No se pudo eliminar la opción');
      }
    });
  }

  // --- Modal de Catálogo (Crear / Editar) ---

  openCreateCatalogModal(): void {
    this.isEditingCatalog = false;
    this.catalogFormId = undefined;
    this.catalogFormName = '';
    this.catalogFormCode = '';
    this.catalogFormDescription = '';
    this.catalogFormSpecialty = 'GENERAL';
    this.isCustomCatalogCodeExpanded = false;
    this.showCatalogModal = true;
  }

  openEditCatalogModal(catalog: Catalog): void {
    this.isEditingCatalog = true;
    this.catalogFormId = catalog.id;
    this.catalogFormName = catalog.name;
    this.catalogFormCode = catalog.code;
    this.catalogFormDescription = catalog.description || '';
    this.catalogFormSpecialty = catalog.specialty || 'GENERAL';
    this.isCustomCatalogCodeExpanded = true;
    this.showCatalogModal = true;
  }

  closeCatalogModal(): void {
    this.showCatalogModal = false;
  }

  onCatalogFormNameChange(): void {
    if (!this.isEditingCatalog && !this.isCustomCatalogCodeExpanded) {
      this.catalogFormCode = this.slugify(this.catalogFormName);
    }
  }

  toggleCustomCatalogCode(): void {
    this.isCustomCatalogCodeExpanded = !this.isCustomCatalogCodeExpanded;
    if (!this.isCustomCatalogCodeExpanded && !this.isEditingCatalog) {
      this.catalogFormCode = this.slugify(this.catalogFormName);
    }
  }

  saveCatalog(): void {
    if (!this.catalogFormName.trim() || (!this.isEditingCatalog && !this.catalogFormCode.trim())) {
      return;
    }

    this.savingCatalog = true;
    const code = this.catalogFormCode.trim() ? this.catalogFormCode.trim().toUpperCase() : this.slugify(this.catalogFormName);

    if (this.isEditingCatalog && this.catalogFormId) {
      const updatePayload: Partial<Catalog> = {
        name: this.catalogFormName.trim(),
        description: this.catalogFormDescription.trim(),
        specialty: this.catalogFormSpecialty
      };

      this.catalogService.updateCatalog(this.catalogFormId, updatePayload).subscribe({
        next: (updated) => {
          this.savingCatalog = false;
          this.closeCatalogModal();
          this.loadCatalogs(updated.code);
          this.toastService.success(`Catálogo "${updated.name}" actualizado correctamente`);
        },
        error: (err) => {
          console.error(err);
          this.savingCatalog = false;
          this.toastService.error('No se pudo actualizar el catálogo');
        }
      });
    } else {
      const newCatalog: Catalog = {
        name: this.catalogFormName.trim(),
        code: code,
        description: this.catalogFormDescription.trim(),
        specialty: this.catalogFormSpecialty
      };

      this.catalogService.createCatalog(newCatalog).subscribe({
        next: (created) => {
          this.savingCatalog = false;
          this.closeCatalogModal();
          this.loadCatalogs(created.code);
          this.toastService.success(`Catálogo "${created.name}" creado con éxito`);
        },
        error: (err) => {
          console.error(err);
          this.savingCatalog = false;
          const msg = err.error?.message || 'Error al crear el catálogo';
          this.toastService.error(msg);
        }
      });
    }
  }

  // --- Utilidades ---

  copyCatalogCode(code: string): void {
    navigator.clipboard.writeText(code);
    this.copiedCode = true;
    this.toastService.info(`Código "${code}" copiado al portapapeles`);
    setTimeout(() => {
      this.copiedCode = false;
    }, 2000);
  }

  slugify(text: string): string {
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

  getSpecialtyBadgeClass(specialty?: string): string {
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

  getSpecialtyLabel(specialty?: string): string {
    if (!specialty || specialty.toUpperCase() === 'GENERAL') return 'General';
    const found = this.specialties.find(s => s.code.toUpperCase() === specialty.toUpperCase());
    if (found) return found.name;
    if (specialty === 'DERMATOLOGIA') return 'Dermatología';
    if (specialty === 'PSICOLOGIA') return 'Psicología';
    return specialty.charAt(0) + specialty.slice(1).toLowerCase();
  }
}
