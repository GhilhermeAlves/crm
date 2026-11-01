import { CompanyInvitationLanding } from "@/features/identity/company-invitations/components/CompanyInvitationLanding";

/** Convite de criação de empresa (público): conta + dados da empresa. */
export default function CompanyInvitationPage({ params }: { params: { token: string } }) {
  return <CompanyInvitationLanding token={decodeURIComponent(params.token)} />;
}
