import { TestBed } from '@angular/core/testing';

import { IconPickerComponent } from './icon-picker.component';

describe('IconPickerComponent', () => {
  it('renders the available icons and emits the selected code', async () => {
    await TestBed.configureTestingModule({ imports: [IconPickerComponent] }).compileComponents();
    const fixture = TestBed.createComponent(IconPickerComponent);
    const selected = vi.fn();
    fixture.componentInstance.valueChange.subscribe(selected);
    fixture.detectChanges();

    const buttons = fixture.nativeElement.querySelectorAll('.icon-option') as NodeListOf<HTMLButtonElement>;
    expect(buttons).toHaveLength(6);
    for (const button of buttons) {
      expect(button.querySelector('svg path, svg circle, svg rect')).not.toBeNull();
    }

    buttons[1].click();
    expect(selected).toHaveBeenCalledWith('SHIELD_CHECK');
  });
});
