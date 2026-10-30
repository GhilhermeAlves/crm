export type ContactNote = {
  id: string;
  contactId: string;
  body: string;
  /** Dente (FDI) da anotação; null = anotação geral. Só usado em Odontologia. */
  tooth: number | null;
  createdBy: string;
  createdByName: string;
  createdAt: string;
  updatedAt: string;
};

export type CreateContactNoteRequest = {
  body: string;
  tooth?: number | null;
};

export type UpdateContactNoteRequest = {
  body: string;
  tooth?: number | null;
};
