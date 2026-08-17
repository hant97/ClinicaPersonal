export interface ClinicalService {
  id?: number;
  name: string;
  description?: string;
  price: number;
  category?: string;
  durationMinutes?: number;
  imageUrl?: string;
  active?: boolean;
  specialty?: string;
}

export interface ServicePerformance {
  serviceId: number;
  name: string;
  quantity: number;
  total: number;
}

export interface ClinicalServiceStats {
  totalServices: number;
  activeCount: number;
  averagePrice: number;
  topByRevenue: ServicePerformance[];
  topByQuantity: ServicePerformance[];
}
