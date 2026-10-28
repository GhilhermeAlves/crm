import { PublicPatientForm } from "@/features/clinical/anamnesis/components/PublicPatientForm";

/**
 * Formulário respondido pelo paciente (público, sem login): pelo link recebido
 * ou no tablet da recepção (?modo=recepcao).
 */
export default function PublicPatientFormPage({
  params,
  searchParams,
}: {
  params: { token: string };
  searchParams: { modo?: string };
}) {
  return (
    <PublicPatientForm
      token={decodeURIComponent(params.token)}
      reception={searchParams.modo === "recepcao"}
    />
  );
}
