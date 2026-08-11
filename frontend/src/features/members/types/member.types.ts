export type Member = {
  userId: string;
  name: string;
  email: string;
  role: string;
  status: string;
  joinedAt: string | null;
};

export type InviteMemberRequest = {
  firstName: string;
  lastName: string;
  email: string;
  department?: string;
  jobTitle?: string;
};
