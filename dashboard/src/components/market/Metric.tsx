
interface MetricProps {
    label: string
    value: string
}


export function Metric({ label, value }: MetricProps) {
    return (
        <div className="rounded-lg border border-border bg-bg2 px-3 py-2.5">
            <div className="text-[10px] uppercase tracking-wider text-text-muted">{label}</div>
            <div className="mt-1 font-mono text-[13px] text-text-primary">{value}</div>
        </div>
    )
}