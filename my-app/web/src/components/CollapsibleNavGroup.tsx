import { useEffect, useState } from 'react'
import { NavLink, useLocation } from 'react-router-dom'
import type { NavSection } from '../navigation/sections'

type Props = {
  id: string
  label: string
  basePath: string
  sections: NavSection[]
}

function readStoredOpen(id: string): boolean | null {
  const stored = localStorage.getItem(`nav-expanded-${id}`)
  if (stored === 'true') return true
  if (stored === 'false') return false
  return null
}

export default function CollapsibleNavGroup({ id, label, basePath, sections }: Props) {
  const location = useLocation()
  const isActiveGroup = location.pathname.startsWith(basePath)

  const [open, setOpen] = useState(() => {
    const stored = readStoredOpen(id)
    if (stored !== null) return stored
    return isActiveGroup
  })

  useEffect(() => {
    if (isActiveGroup) setOpen(true)
  }, [isActiveGroup])

  const toggle = () => {
    setOpen((prev) => {
      const next = !prev
      localStorage.setItem(`nav-expanded-${id}`, next ? 'true' : 'false')
      return next
    })
  }

  return (
    <div className="sidebar-group">
      <button
        type="button"
        className={`sidebar-group-header${isActiveGroup ? ' active' : ''}`}
        onClick={toggle}
        aria-expanded={open}
      >
        <span>{label}</span>
        <span className="sidebar-group-chevron" aria-hidden>{open ? '▾' : '▸'}</span>
      </button>
      {open && (
        <div className="sidebar-subnav">
          {sections.map((section) => (
            <NavLink
              key={section.path}
              to={`${basePath}/${section.path}`}
              className={({ isActive }) => (isActive ? 'sidebar-sub-link active' : 'sidebar-sub-link')}
            >
              {section.label}
            </NavLink>
          ))}
        </div>
      )}
    </div>
  )
}
