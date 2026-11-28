import { VerifyReport } from "@/features/clinical/trichology/components/PublicPages";

/** Verificação pública do relatório de tricologia pelo código impresso no PDF. */
export default function VerifyTrichologyReportPage({ params }: { params: { code: string } }) {
  return <VerifyReport code={decodeURIComponent(params.code)} />;
}
