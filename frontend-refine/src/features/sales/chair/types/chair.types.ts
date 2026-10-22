export type Chair = {
  id: string;
  companyId: string;
  name: string;
  color: string | null;
  active: boolean;
  sortOrder: number;
  createdAt: string;
  updatedAt: string;
};

export type CreateChairRequest = {
  name: string;
  color?: string | null;
  active?: boolean;
  sortOrder?: number;
};

export type UpdateChairRequest = Partial<CreateChairRequest>;
