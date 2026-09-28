import { DeferBlockState, TestBed } from '@angular/core/testing';
import { AppComponent } from './app.component';
import { ToastService } from './shared/services/toast/toast.service';

describe('AppComponent', () => {
  it('creates the app and loads the toast view when a message arrives', async () => {
    await TestBed.configureTestingModule({ imports: [AppComponent] }).compileComponents();
    const fixture = TestBed.createComponent(AppComponent);
    const toastService = TestBed.inject(ToastService);
    expect(fixture.componentInstance.title).toBe('frontend');
    fixture.detectChanges();
    expect(fixture.componentInstance.hasToasts()).toBe(false);

    toastService.info('Aviso de prueba');
    fixture.detectChanges();
    expect(fixture.componentInstance.hasToasts()).toBe(true);

    const [toastBlock] = await fixture.getDeferBlocks();
    await toastBlock.render(DeferBlockState.Complete);
    fixture.detectChanges();
    expect(fixture.nativeElement.querySelector('[role="status"]')?.textContent).toContain('Aviso de prueba');

    toastService.dismiss(1);
  }, 30000);
});
