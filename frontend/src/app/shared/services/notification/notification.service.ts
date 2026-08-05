import { Injectable } from '@angular/core';
import Swal, { SweetAlertIcon } from 'sweetalert2';

@Injectable({
  providedIn: 'root'
})
export class NotificationService {

  // Colores extraídos del styles.css para mantener coherencia visual
  constructor() { }

  /**
   * Muestra un modal de confirmación con dos botones.
   * @param title Título del modal
   * @param text Texto o pregunta del modal
   * @param confirmButtonText Texto del botón de confirmación
   * @param cancelButtonText Texto del botón de cancelación
   * @returns Promesa que se resuelve a true si el usuario confirmó, o false en caso contrario.
   */
  async confirm(
    title: string,
    text: string,
    confirmButtonText: string = 'Aceptar',
    cancelButtonText: string = 'Cancelar'
  ): Promise<boolean> {
    const result = await Swal.fire({
      title,
      text,
      icon: 'warning',
      showCancelButton: true,
      confirmButtonText,
      cancelButtonText,
      focusCancel: true,
      buttonsStyling: false,
      customClass: {
        popup: 'rounded-xl border border-line bg-surface p-6 shadow-raised',
        title: 'font-display text-xl font-semibold text-clinic-900',
        htmlContainer: 'text-sm text-muted',
        actions: 'gap-3',
        confirmButton: 'btn !bg-clinic-600 hover:!bg-clinic-700',
        cancelButton: 'btn-secondary'
      }
    });

    return result.isConfirmed;
  }

  /**
   * Muestra un modal de alerta simple bloqueante.
   * @param title Título del modal
   * @param text Texto del modal
   * @param icon Tipo de icono (success, error, warning, info, question)
   */
  async alert(title: string, text: string, icon: SweetAlertIcon = 'info'): Promise<void> {
    await Swal.fire({
      title,
      text,
      icon,
      confirmButtonText: 'Aceptar',
      buttonsStyling: false,
      customClass: {
        popup: 'rounded-xl border border-line bg-surface p-6 shadow-raised',
        title: 'font-display text-xl font-semibold text-clinic-900',
        htmlContainer: 'text-sm text-muted',
        confirmButton: 'btn !bg-clinic-600 hover:!bg-clinic-700'
      }
    });
  }
}
