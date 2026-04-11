import { Link, Outlet, useLocation } from 'react-router-dom'

import { APP_ROUTES } from '@/shared/config/constants'
import { cn } from '@/shared/lib/cn'

const navLinks = [
  {
    label: 'Вход',
    path: APP_ROUTES.login,
  },
  {
    label: 'Регистрация',
    path: APP_ROUTES.register,
  },
]

export const AuthLayout = () => {
  const location = useLocation()

  return (
    <div className="game-auth-background relative min-h-screen overflow-hidden">
      <div className="game-orb game-orb--one" />
      <div className="game-orb game-orb--two" />
      <div className="game-orb game-orb--three" />
      <div className="game-grid-overlay" />

      <main className="relative z-10 mx-auto grid min-h-screen w-full max-w-6xl gap-10 px-4 py-8 sm:px-6 lg:grid-cols-[1.05fr_0.95fr] lg:items-center lg:px-8">
        <section className="space-y-7 rounded-[2rem] bg-[#1f2a88]/82 p-6 text-[#f7f8ff] backdrop-blur sm:p-8 lg:shadow-[0_18px_50px_rgba(8,17,69,0.4)]">
          <p className="inline-flex rounded-full bg-[#f8ff8d] px-3 py-1 text-xs font-bold uppercase tracking-[0.14em] text-[#312b72]">
            ChineseWithAI
          </p>

          <h2 className="font-display text-4xl font-extrabold leading-tight text-white sm:text-5xl">
            Учись как в игре.
            <br />
            Прокачивайся каждый день.
          </h2>

          <p className="max-w-lg text-base leading-relaxed text-[#dbe1ff]">
            Яркие интерактивные уроки, челленджи и понятный прогресс. Войди в портал и продолжай
            свое языковое приключение.
          </p>

          <ul className="space-y-2 text-sm text-[#dbe1ff] sm:text-base">
            <li>Комбо из грамматики, слов и практики</li>
            <li>Миссии, которые приятно закрывать по шагам</li>
            <li>Темп обучения подстраивается под тебя</li>
          </ul>
        </section>

        <section className="flex flex-col items-center lg:items-end">
          <nav className="mb-4 inline-flex rounded-full bg-[#1d2378]/30 p-1 backdrop-blur">
            {navLinks.map((link) => {
              const isActive = location.pathname === link.path

              return (
                <Link
                  key={link.path}
                  className={cn(
                    'rounded-full px-4 py-2 text-sm font-semibold transition',
                    isActive
                      ? 'bg-[#fff8a8] text-[#1f2572]'
                      : 'text-white/90 hover:bg-white/20 hover:text-white',
                  )}
                  to={link.path}
                >
                  {link.label}
                </Link>
              )
            })}
          </nav>

          <Outlet />
        </section>
      </main>
    </div>
  )
}
