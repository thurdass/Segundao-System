import { LoaderCircle } from 'lucide-react'

interface LoadingScreenProps {
  label?: string
}

export function LoadingScreen({ label = 'Carregando...' }: LoadingScreenProps) {
  return (
    <main className="loading-screen" aria-live="polite">
      <LoaderCircle className="spin" size={24} aria-hidden="true" />
      <span>{label}</span>
    </main>
  )
}
