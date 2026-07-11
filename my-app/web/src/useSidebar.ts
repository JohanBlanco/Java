import { useEffect, useState } from 'react'

const STORAGE_KEY = 'sidebarOpen'

export function useSidebar() {
  const [sidebarOpen, setSidebarOpen] = useState(() => localStorage.getItem(STORAGE_KEY) !== 'false')

  useEffect(() => {
    localStorage.setItem(STORAGE_KEY, sidebarOpen ? 'true' : 'false')
  }, [sidebarOpen])

  const toggleSidebar = () => setSidebarOpen((open) => !open)

  return { sidebarOpen, setSidebarOpen, toggleSidebar }
}
