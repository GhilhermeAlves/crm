import { InvitationLanding } from "@/features/identity/invitations/components/InvitationLanding";

/** Convite (público): prévia, cadastro ou entrada com conta existente, e aceite. */
export default function InvitationPage({ params }: { params: { token: string } }) {
  return <InvitationLanding token={decodeURIComponent(params.token)} />;
}
