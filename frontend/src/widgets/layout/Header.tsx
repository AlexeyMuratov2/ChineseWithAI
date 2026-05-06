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
        <Link to="/" className="flex items-center gap-3 text-lg font-black text-app-fg">
          <span className="grid h-9 w-9 place-items-center rounded-lg bg-[#173b4b] font-display text-xl text-[#ffcf5a]">
            中
          </span>
          ChineseWithAI
        </Link>
        <Button variant="ghost" size="sm" onClick={toggleTheme}>
          {theme === 'light' ? 'Светлая' : 'Темная'}
        </Button>
      </Container>
    </header>
  )
}
