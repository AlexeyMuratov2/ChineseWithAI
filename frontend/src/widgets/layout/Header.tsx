import { Link } from 'react-router-dom'

import { Button } from '@/shared/ui/Button'
import { Container } from '@/shared/ui/Container'
import { useUiStore } from '@/shared/store/ui-store'

export const Header = () => {
  const theme = useUiStore((state) => state.theme)
  const toggleTheme = useUiStore((state) => state.toggleTheme)

  return (
    <header className="border-b border-app-border bg-app-bg/90 backdrop-blur">
      <Container className="flex h-16 items-center justify-between gap-4">
        <Link to="/" className="text-lg font-semibold text-app-fg">
          ChineseWithAI Frontend
        </Link>
        <Button variant="ghost" size="sm" onClick={toggleTheme}>
          Theme: {theme}
        </Button>
      </Container>
    </header>
  )
}
