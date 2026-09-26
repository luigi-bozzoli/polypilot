import { AlertCircle } from 'lucide-react'

type FormErrorProps = {
  message: string
}

export function FormError({ message }: FormErrorProps) {
  return (
    <div
      role="alert"
      className="mb-4 flex items-start gap-2 rounded-md border border-red-border bg-red-dim px-3 py-2.5 text-[12px] text-red"
    >
      <AlertCircle className="mt-0.5 h-3.5 w-3.5 flex-shrink-0" aria-hidden="true" />
      <span>{message}</span>
    </div>
  )
}
