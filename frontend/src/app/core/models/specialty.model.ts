export interface SpecialtyItem {
  id: number;
  code: string;
  name: string;
  description?: string;
  icon?: string;
  active: boolean;
  displayOrder: number;
  createdAt?: string;
  updatedAt?: string;
}
