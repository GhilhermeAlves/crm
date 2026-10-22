export type Tag = {
  id: string;
  companyId: string;
  name: string;
  color: string | null;
  createdAt: string;
  updatedAt: string;
};

export type CreateTagRequest = {
  name: string;
  color?: string | null;
};

export type UpdateTagRequest = Partial<CreateTagRequest>;
