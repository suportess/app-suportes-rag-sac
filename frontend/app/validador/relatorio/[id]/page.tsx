import { RelatorioView } from './_components/relatorio-view'

export default async function RelatorioPage({ params }: { params: Promise<{ id: string }> }) {
  const { id } = await params
  return <RelatorioView reportId={id} />
}
