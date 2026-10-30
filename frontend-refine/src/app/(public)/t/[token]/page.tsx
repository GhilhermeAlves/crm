import { LiveViewScreen } from "@/features/clinical/trichoscopy/components/LiveViewScreen";

/** Tela do paciente (tablet): mostra ao vivo a câmera de tricoscopia, sem login. */
export default function TrichoscopyLiveViewPage({ params }: { params: { token: string } }) {
  return <LiveViewScreen token={decodeURIComponent(params.token)} />;
}
