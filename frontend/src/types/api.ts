export type ApiError = {
  message: string;
  code: string;
  details?: Record<string, string[]>;
};
