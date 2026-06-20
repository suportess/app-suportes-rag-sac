'use client'

import { usePathname } from 'next/navigation'
import Link from 'next/link'
import { navigation } from './nav-config'
import { useSidebar } from './sidebar-context'
import { X, Sun, Moon, ShieldCheck } from 'lucide-react'
import { useState, useEffect } from 'react'

export function Sidebar() {
  const pathname = usePathname()
  const { open, setOpen } = useSidebar()
  const [isDark, setIsDark] = useState(false)

  useEffect(() => {
    setIsDark(document.documentElement.classList.contains('dark'))
  }, [])

  function toggleTheme() {
    const newDark = !isDark
    setIsDark(newDark)
    document.body.classList.add('theme-transition')
    if (newDark) {
      document.documentElement.classList.add('dark')
    } else {
      document.documentElement.classList.remove('dark')
    }
    localStorage.setItem('theme', newDark ? 'dark' : 'light')
    setTimeout(() => document.body.classList.remove('theme-transition'), 300)
  }

  const isActive = (href: string) =>
    href === '/validador' ? pathname === href : pathname.startsWith(href)

  const inner = (
    <div className="flex flex-col h-full">
      {/* Header */}
      <div
        className="flex items-center h-14 px-4 flex-shrink-0 border-b gap-3"
        style={{ borderColor: 'var(--d2b-border)' }}
      >
        <div
          className="w-8 h-8 rounded-lg flex items-center justify-center flex-shrink-0"
          style={{ background: 'var(--gradient-brand)' }}
        >
          <ShieldCheck size={18} color="#fff" />
        </div>
        <div className="flex-1 min-w-0">
          <p
            className="text-sm font-bold truncate"
            style={{ color: 'var(--d2b-text-primary)' }}
          >
            SAP Spec Validator
          </p>
          <p
            className="text-[10px] truncate"
            style={{ color: 'var(--d2b-text-muted)' }}
          >
            Validador ABAP
          </p>
        </div>

        {/* Close button (mobile only) */}
        <button
          onClick={() => setOpen(false)}
          className="md:hidden p-1.5 rounded-lg transition-colors"
          style={{ color: 'var(--d2b-text-secondary)' }}
          aria-label="Fechar menu"
        >
          <X size={16} />
        </button>
      </div>

      {/* Navigation */}
      <nav className="flex-1 overflow-y-auto overflow-x-hidden py-3 px-2">
        <ul className="space-y-0.5">
          {navigation.map((item) => {
            const active = isActive(item.href)
            const Icon = item.icon
            return (
              <li key={item.href}>
                <Link
                  href={item.href}
                  onClick={() => setOpen(false)}
                  className="group relative flex items-center gap-2.5 rounded-lg px-2.5 py-2 text-sm font-medium transition-all duration-150"
                  style={{
                    color: active ? 'var(--d2b-text-primary)' : 'var(--d2b-text-secondary)',
                    background: active ? 'var(--d2b-active)' : 'transparent',
                  }}
                  onMouseEnter={e => {
                    if (!active) {
                      ;(e.currentTarget as HTMLElement).style.background = 'var(--d2b-hover)'
                      ;(e.currentTarget as HTMLElement).style.color = 'var(--d2b-text-primary)'
                    }
                  }}
                  onMouseLeave={e => {
                    if (!active) {
                      ;(e.currentTarget as HTMLElement).style.background = 'transparent'
                      ;(e.currentTarget as HTMLElement).style.color = 'var(--d2b-text-secondary)'
                    }
                  }}
                >
                  {active && (
                    <span className="absolute left-0 top-1/2 -translate-y-1/2 w-0.5 h-5 rounded-r-full bg-[var(--brand)]" />
                  )}
                  <Icon
                    size={18}
                    className="flex-shrink-0"
                    style={{ color: active ? 'var(--brand)' : 'currentColor' }}
                  />
                  <span className="flex-1 truncate">{item.label}</span>
                </Link>
              </li>
            )
          })}
        </ul>
      </nav>

      {/* Theme toggle */}
      <div
        className="flex items-center justify-between px-4 py-3 border-t flex-shrink-0"
        style={{ borderColor: 'var(--d2b-border)' }}
      >
        <span
          className="text-xs font-medium"
          style={{ color: 'var(--d2b-text-muted)' }}
        >
          {isDark ? 'Modo escuro' : 'Modo claro'}
        </span>
        <button
          onClick={toggleTheme}
          className="p-1.5 rounded-lg transition-colors"
          style={{ color: 'var(--d2b-text-secondary)' }}
          onMouseEnter={e => {
            ;(e.currentTarget as HTMLElement).style.background = 'var(--d2b-hover)'
            ;(e.currentTarget as HTMLElement).style.color = 'var(--d2b-text-primary)'
          }}
          onMouseLeave={e => {
            ;(e.currentTarget as HTMLElement).style.background = 'transparent'
            ;(e.currentTarget as HTMLElement).style.color = 'var(--d2b-text-secondary)'
          }}
          aria-label="Alternar tema"
          title={isDark ? 'Modo claro' : 'Modo escuro'}
        >
          {isDark ? <Sun size={16} /> : <Moon size={16} />}
        </button>
      </div>
    </div>
  )

  return (
    <>
      {/* Desktop sidebar */}
      <aside
        className="hidden md:flex flex-col flex-shrink-0 h-screen border-r fixed left-0 top-0 z-30"
        style={{
          width: '15rem',
          background: 'var(--d2b-bg-surface)',
          borderColor: 'var(--d2b-border)',
        }}
      >
        {inner}
      </aside>

      {/* Mobile overlay */}
      {open && (
        <div
          className="fixed inset-0 z-40 md:hidden"
          onClick={() => setOpen(false)}
          style={{ background: 'rgba(0,0,0,0.5)' }}
        />
      )}

      {/* Mobile drawer */}
      <aside
        className={`fixed inset-y-0 left-0 z-50 flex flex-col md:hidden transition-transform duration-200 ${
          open ? 'translate-x-0' : '-translate-x-full'
        }`}
        style={{
          width: '15rem',
          background: 'var(--d2b-bg-surface)',
          borderRight: '1px solid var(--d2b-border)',
        }}
      >
        {inner}
      </aside>
    </>
  )
}
