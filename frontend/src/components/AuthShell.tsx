import type { ReactNode } from 'react'

interface AuthShellProps {
  eyebrow: string
  title: string
  description: string
  children: ReactNode
}

export function AuthShell({
  eyebrow,
  title,
  description,
  children,
}: AuthShellProps) {
  return (
    <main className="auth-page">
      <section className="auth-brand-panel" aria-label="Identidade do sistema">
        <div className="auth-brand-mark">
          <img src="/img.png" alt="Identidade visual da turma 2º Informática A" />
        </div>
        <div>
          <p className="auth-kicker">2º Informática A</p>
          <h1>Segundão System</h1>
          <p className="auth-brand-copy">
            A rotina da turma, organizada em um só lugar.
          </p>
        </div>
        <span className="auth-year">2026 · Informática</span>
      </section>

      <section className="auth-form-panel">
        <div className="auth-card">
          <div className="auth-card-heading">
            <p className="eyebrow">{eyebrow}</p>
            <h2>{title}</h2>
            <p>{description}</p>
          </div>
          {children}
        </div>
      </section>
    </main>
  )
}
