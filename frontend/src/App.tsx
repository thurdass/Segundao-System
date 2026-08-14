import { BrowserRouter, Navigate, Route, Routes } from 'react-router-dom'
import './App.css'
import { AppLayout } from './layouts/AppLayout'
import { ActivitiesPage } from './pages/ActivitiesPage'
import { AnnouncementsPage } from './pages/AnnouncementsPage'
import { ChangePasswordPage } from './pages/ChangePasswordPage'
import { DashboardPage } from './pages/DashboardPage'
import { LoginPage } from './pages/LoginPage'
import { ProfilePage } from './pages/ProfilePage'
import { SchedulePage } from './pages/SchedulePage'
import { SubjectsPage } from './pages/SubjectsPage'
import { TeachersPage } from './pages/TeachersPage'
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
          <Route element={<AppLayout />}>
            <Route path="/" element={<HomeRedirect />} />
            <Route path="/dashboard" element={<DashboardPage />} />
            <Route path="/activities" element={<ActivitiesPage />} />
            <Route path="/announcements" element={<AnnouncementsPage />} />
            <Route path="/schedule" element={<SchedulePage />} />
            <Route path="/subjects" element={<SubjectsPage />} />
            <Route path="/teachers" element={<TeachersPage />} />
            <Route path="/profile" element={<ProfilePage />} />
          </Route>
        </Route>

        <Route path="*" element={<Navigate to="/" replace />} />
      </Routes>
    </BrowserRouter>
  )
}

export default App
