export interface InventoryTransaction {
  id?: number;
  supplyId: number;
  supplyName?: string;
  quantity: number;
  type: 'IN' | 'OUT' | 'ADJUSTMENT';
  reason: 'BILLING' | 'CLINICAL_USAGE' | 'EXPIRED' | 'DAMAGED' | 'RESTOCK';
  referenceId?: string;
  notes?: string;
  transactionDate?: string;
}
