import { PublicPatientForm } from "@/features/clinical/anamnesis/components/PublicPatientForm";

/**
 * Link aberto do formulário (público, sem login): o mesmo endereço serve para
 * qualquer pessoa, que se identifica com nome e WhatsApp antes de responder.
 */
export default function OpenPatientFormPage({ params }: { params: { token: string } }) {
  return <PublicPatientForm token={decodeURIComponent(params.token)} reception={false} open />;
}
