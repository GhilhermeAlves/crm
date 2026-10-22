export type ContactNote = {
  id: string;
  contactId: string;
  body: string;
  createdBy: string;
  createdByName: string;
  createdAt: string;
  updatedAt: string;
};

export type CreateContactNoteRequest = {
  body: string;
};

export type UpdateContactNoteRequest = {
  body: string;
};
