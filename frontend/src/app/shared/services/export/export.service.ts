import { Injectable } from '@angular/core';
import * as XLSX from 'xlsx';

@Injectable({
  providedIn: 'root'
})
export class ExportService {

  constructor() { }

  /**
   * Exports an array of objects to an Excel file (.xlsx)
   * @param data Array of objects representing the rows
   * @param fileName The desired name for the downloaded file (without extension)
   */
  exportToExcel(data: any[], fileName: string): void {
    const worksheet: XLSX.WorkSheet = XLSX.utils.json_to_sheet(data);
    const workbook: XLSX.WorkBook = { Sheets: { 'data': worksheet }, SheetNames: ['data'] };
    
    // Generar buffer y descargar archivo
    XLSX.writeFile(workbook, `${fileName}.xlsx`);
  }
}
