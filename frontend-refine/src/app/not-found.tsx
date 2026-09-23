import { ErrorPage } from "@/components/feedback/ErrorPage";

export default function NotFound() {
  return (
    <ErrorPage
      code="404"
      title="Página não encontrada"
      description="A página que você procura não existe ou foi movida para outro endereço."
    />
  );
}
