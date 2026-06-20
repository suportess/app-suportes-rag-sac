'use client'

import { useSidebar } from './sidebar-context'
import { Menu, ShieldCheck } from 'lucide-react'

export function Topbar() {
  const { setOpen } = useSidebar()

  return (
    <header
      className="sticky top-0 z-30 flex items-center gap-3 px-4 border-b flex-shrink-0"
      style={{
        height: '3.5rem',
        background: 'var(--d2b-topbar-bg)',
        borderColor: 'var(--d2b-border)',
        backdropFilter: 'blur(12px)',
        WebkitBackdropFilter: 'blur(12px)',
      }}
    >
      {/* Mobile menu button */}
      <button
        onClick={() => setOpen(true)}
        className="md:hidden p-2 rounded-lg transition-colors flex-shrink-0"
        style={{ color: 'var(--d2b-text-secondary)' }}
        aria-label="Abrir menu"
      >
        <Menu size={18} />
      </button>

      {/* App title */}
      <div className="flex items-center gap-2 flex-1 min-w-0">
        <ShieldCheck
          size={18}
          className="flex-shrink-0 md:hidden"
          style={{ color: 'var(--brand)' }}
        />
        <div className="min-w-0">
          <h1
            className="text-sm font-semibold truncate"
            style={{ color: 'var(--d2b-text-primary)' }}
          >
            SAP Spec Validator
          </h1>
          <p
            className="text-[10px] truncate hidden sm:block"
            style={{ color: 'var(--d2b-text-muted)' }}
          >
            Validador de Especificacoes SAP ABAP
          </p>
        </div>
      </div>
    </header>
  )
}
