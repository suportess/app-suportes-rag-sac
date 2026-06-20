import { SidebarProvider } from '@/components/shell/sidebar-context'
import { Sidebar } from '@/components/shell/sidebar'
import { Topbar } from '@/components/shell/topbar'
import { ThemeInit } from '@/components/providers/theme-init'

export default function ValidadorLayout({ children }: { children: React.ReactNode }) {
  return (
    <SidebarProvider>
      <ThemeInit />
      <div className="flex min-h-screen" style={{ background: 'var(--bg-base)' }}>
        <Sidebar />
        {/* Desktop: offset by sidebar width. Mobile: full width (sidebar is overlay). */}
        <div className="flex-1 flex flex-col md:ml-[15rem]">
          <Topbar />
          <main>{children}</main>
        </div>
      </div>
    </SidebarProvider>
  )
}
