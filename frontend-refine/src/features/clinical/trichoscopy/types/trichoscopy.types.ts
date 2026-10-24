export type TrichoscopyImageResponse = {
  id: string;
  contactId: string;
  storageObjectId: string;
  takenBy: string | null;
  takenAt: string;
  notes: string | null;
  downloadUrl: string;
};
