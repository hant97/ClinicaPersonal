import { FormBuilder } from '@angular/forms';
import { InventoryFormComponent } from './inventory-form.component';
import { InventoryService } from '../../../core/services/inventory.service';
import { CatalogService } from '../../../core/services/catalog.service';
import { ToastService } from '../../../shared/services/toast/toast.service';

describe('InventoryFormComponent', () => {
  let component: InventoryFormComponent;

  beforeEach(() => {
    component = new InventoryFormComponent(new FormBuilder(), {} as unknown as InventoryService, {} as unknown as CatalogService, {
      show: vi.fn().mockName('ToastService.show')
    } as unknown as ToastService);
    component.initForm();
  });

  it('revoca la vista previa anterior al seleccionar otra imagen y al destruirse', () => {
    const createObjectUrlSpy = vi.spyOn(URL, 'createObjectURL').mockReturnValue('blob:nueva');
    const revokeObjectUrlSpy = vi.spyOn(URL, 'revokeObjectURL');
    component.imagePreviewUrl = 'blob:anterior';
    const file = new File(['image'], 'foto.png', { type: 'image/png' });

    component.onImageSelected({ target: { files: [file], value: '' } } as unknown as Event);

    expect(revokeObjectUrlSpy).toHaveBeenCalledWith('blob:anterior');
    expect(createObjectUrlSpy).toHaveBeenCalledWith(file);
    expect(component.imagePreviewUrl).toBe('blob:nueva');

    component.ngOnDestroy();
    expect(revokeObjectUrlSpy).toHaveBeenCalledWith('blob:nueva');
    expect(component.imagePreviewUrl).toBeNull();
  });
});
