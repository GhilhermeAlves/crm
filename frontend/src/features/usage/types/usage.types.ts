export interface UsageItem {
  current: number;
  limit: number;
}

export interface StorageUsage {
  currentMb: number;
  limitMb: number;
}

export interface CompanyUsage {
  users: UsageItem;
  contacts: UsageItem;
  storage: StorageUsage;
}