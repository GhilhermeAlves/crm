import { PublicRegistrationForm } from "@/features/masterdata/contacts/registration/components/PublicRegistrationForm";

/** Cadastro do paciente por link (público): o paciente completa os próprios dados. */
export default function PublicRegistrationPage({ params }: { params: { token: string } }) {
  return <PublicRegistrationForm token={decodeURIComponent(params.token)} />;
}
