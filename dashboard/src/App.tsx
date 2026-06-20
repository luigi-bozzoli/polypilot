import { useEffect, useState } from 'react'

type ServiceStatus = {
  service: string
  status: string
  [key: string]: unknown
}

type HealthResponse = {
  service: string
  status: string
  'auth-service': ServiceStatus
  'ai-agent': ServiceStatus
}

const dot = (ok: boolean) => ({
  display: 'inline-block',
  width: 10,
  height: 10,
  borderRadius: '50%',
  background: ok ? '#22c55e' : '#ef4444',
  marginRight: 8,
})

const card: React.CSSProperties = {
  border: '1px solid #e5e7eb',
  borderRadius: 8,
  padding: '16px 20px',
  marginBottom: 12,
  background: '#fff',
  minWidth: 280,
}

export default function App() {
  const [data, setData] = useState<HealthResponse | null>(null)
  const [error, setError] = useState<string | null>(null)
  const [loading, setLoading] = useState(true)

  const fetchHealth = async () => {
    try {
      const res = await fetch('/api/health')
      if (!res.ok) throw new Error(`HTTP ${res.status}`)
      setData(await res.json())
      setError(null)
    } catch (e) {
      setError((e as Error).message)
    } finally {
      setLoading(false)
    }
  }

  useEffect(() => {
    fetchHealth()
    const id = setInterval(fetchHealth, 5000) // poll every 5 s
    return () => clearInterval(id)
  }, [])

  return (
    <div style={{ fontFamily: 'system-ui, sans-serif', padding: 40, background: '#f9fafb', minHeight: '100vh' }}>
      <h1 style={{ fontSize: 28, fontWeight: 700, marginBottom: 4 }}>🤖 PolyPilot</h1>
      <p style={{ color: '#6b7280', marginBottom: 32 }}>Service health — refreshes every 5 s</p>

      {loading && <p style={{ color: '#6b7280' }}>Contacting orchestrator…</p>}

      {error && (
        <div style={{ ...card, borderColor: '#fca5a5', background: '#fff1f2' }}>
          <strong style={{ color: '#dc2626' }}>Orchestrator unreachable</strong>
          <p style={{ margin: '8px 0 0', color: '#dc2626', fontSize: 14 }}>{error}</p>
        </div>
      )}

      {data && (
        <>
          {/* Orchestrator */}
          <div style={card}>
            <div style={{ fontWeight: 600, marginBottom: 6 }}>
              <span style={dot(data.status === 'ok')} />
              orchestrator
            </div>
            <pre style={{ margin: 0, fontSize: 12, color: '#374151' }}>
              {JSON.stringify({ service: data.service, status: data.status }, null, 2)}
            </pre>
          </div>

          {/* auth-service */}
          <div style={card}>
            <div style={{ fontWeight: 600, marginBottom: 6 }}>
              <span style={dot(data['auth-service']?.status === 'ok')} />
              auth-service
            </div>
            <pre style={{ margin: 0, fontSize: 12, color: '#374151' }}>
              {JSON.stringify(data['auth-service'], null, 2)}
            </pre>
          </div>

          {/* ai-agent */}
          <div style={card}>
            <div style={{ fontWeight: 600, marginBottom: 6 }}>
              <span style={dot(data['ai-agent']?.status === 'ok')} />
              ai-agent
            </div>
            <pre style={{ margin: 0, fontSize: 12, color: '#374151' }}>
              {JSON.stringify(data['ai-agent'], null, 2)}
            </pre>
          </div>
        </>
      )}
    </div>
  )
}
