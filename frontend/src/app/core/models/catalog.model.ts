export interface CatalogItem {
  id?: number;
  catalogId?: number;
  itemCode: string;
  itemName: string;
  isActive: boolean;
  orderIndex: number;
}

export interface Catalog {
  id?: number;
  code: string;
  name: string;
  description?: string;
  items?: CatalogItem[];
}
