import { BrowserRouter, Navigate, Route, Routes } from 'react-router-dom'
import './App.css'
import { ChangePasswordPage } from './pages/ChangePasswordPage'
import { InitialDashboardPage } from './pages/InitialDashboardPage'
import { LoginPage } from './pages/LoginPage'
import { FirstAccessRoute, ProtectedRoute } from './routes/ProtectedRoute'

function HomeRedirect() {
  return <Navigate to="/dashboard" replace />
}

function App() {
  return (
    <BrowserRouter>
      <Routes>
        <Route path="/login" element={<LoginPage />} />

        <Route element={<FirstAccessRoute />}>
          <Route path="/change-password" element={<ChangePasswordPage />} />
        </Route>

        <Route element={<ProtectedRoute />}>
          <Route path="/" element={<HomeRedirect />} />
          <Route path="/dashboard" element={<InitialDashboardPage />} />
        </Route>

        <Route path="*" element={<Navigate to="/" replace />} />
      </Routes>
    </BrowserRouter>
  )
}

export default App
