import { Upload, List, type LucideIcon } from 'lucide-react'

export type NavItem = {
  label: string
  href: string
  icon: LucideIcon
  children?: NavItem[]
}

export const navigation: NavItem[] = [
  { label: 'Validar Documento', href: '/validador', icon: Upload },
  { label: 'Documentos', href: '/validador/documentos', icon: List },
]
