import { PublicQuiz } from "@/features/clinical/trichology/components/PublicPages";

/** Quiz capilar que o paciente responde pelo link enviado pela clínica (sem login). */
export default function PublicTrichologyQuizPage({ params }: { params: { token: string } }) {
  return <PublicQuiz token={decodeURIComponent(params.token)} />;
}
