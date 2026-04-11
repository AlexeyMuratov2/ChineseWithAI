import { Outlet } from 'react-router-dom'

export const ProtectedLayout = () => {
  return (
    <div className="min-h-screen bg-[#f9fbff]">
      <Outlet />
    </div>
  )
}
