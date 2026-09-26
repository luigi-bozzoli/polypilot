import { Navigate, Route, Routes } from 'react-router-dom'
import { AuthPage } from './pages/AuthPage'
import { AppShell } from './components/shell/AppShell'
import { OverviewPage } from './pages/OverviewPage'
import { HealthPage } from './pages/HealthPage'
import { SeriesListPage } from './pages/SeriesListPage'
import { SeriesDetailPage } from './pages/SeriesDetailPage'
import { MarketDetailPage } from './pages/MarketDetailPage'
import { StrategiesListPage } from './pages/StrategiesListPage'
import { StrategyDetailPage } from './pages/StrategyDetailPage'
import { CreateStrategyPage } from './pages/CreateStrategyPage'
import { EditStrategyPage } from './pages/EditStrategyPage'
import { ProtectedRoute } from './routes/ProtectedRoute'
import { useAuth } from './features/auth/AuthContext'

/**
 * Unknown URL → send authenticated users to the app, everyone else to Auth.
 * No dedicated 404 screen (the project has no such convention).
 */
function NotFoundRedirect() {
  const { isAuthenticated } = useAuth()
  return <Navigate to={isAuthenticated ? '/overview' : '/'} replace />
}

export default function App() {
  return (
    <Routes>
      {/* Public — the landing page and the only unauthenticated route */}
      <Route path="/" element={<AuthPage />} />

      {/* Everything below requires a valid session and renders inside the shell */}
      <Route element={<ProtectedRoute />}>
        <Route element={<AppShell />}>
          <Route path="/overview" element={<OverviewPage />} />
          <Route path="/health" element={<HealthPage />} />
          <Route path="/series" element={<SeriesListPage />} />
          <Route path="/series/:id" element={<SeriesDetailPage />} />
          <Route path="/series/:seriesId/markets/:marketId" element={<MarketDetailPage />} />
          <Route path="/strategies" element={<StrategiesListPage />} />
          <Route path="/strategies/new" element={<CreateStrategyPage />} />
          <Route path="/strategies/:id" element={<StrategyDetailPage />} />
          <Route path="/strategies/:id/edit" element={<EditStrategyPage />} />
        </Route>
      </Route>

      <Route path="*" element={<NotFoundRedirect />} />
    </Routes>
  )
}
