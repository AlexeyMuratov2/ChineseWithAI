import { useEffect } from 'react'
import { Outlet } from 'react-router-dom'

import { Header } from '@/widgets/layout/Header'
import { MainContainer } from '@/widgets/layout/MainContainer'
import { useUiStore } from '@/shared/store/ui-store'

export const AppLayout = () => {
  const theme = useUiStore((state) => state.theme)

  useEffect(() => {
    document.documentElement.setAttribute('data-theme', theme)
  }, [theme])

  return (
    <div className="min-h-screen bg-app-bg text-app-fg">
      <Header />
      <MainContainer>
        <Outlet />
      </MainContainer>
    </div>
  )
}
