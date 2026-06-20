'use client'

import { createContext, useContext, useState, type ReactNode } from 'react'

type SidebarCtx = {
  open: boolean
  setOpen: (v: boolean) => void
  toggle: () => void
}

const SidebarContext = createContext<SidebarCtx>({
  open: false,
  setOpen: () => {},
  toggle: () => {},
})

export function SidebarProvider({ children }: { children: ReactNode }) {
  const [open, setOpen] = useState(false)

  return (
    <SidebarContext.Provider value={{ open, setOpen, toggle: () => setOpen(v => !v) }}>
      {children}
    </SidebarContext.Provider>
  )
}

export function useSidebar() {
  return useContext(SidebarContext)
}
