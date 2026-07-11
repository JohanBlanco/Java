import { useEffect, useRef, useState } from 'react'

export type MultiSelectOption<T extends string | number> = {
  value: T
  label: string
}

type Props<T extends string | number> = {
  options: MultiSelectOption<T>[]
  value: T[]
  onChange: (value: T[]) => void
  placeholder?: string
  emptyLabel?: string
}

export default function MultiSelect<T extends string | number>({
  options,
  value,
  onChange,
  placeholder = 'Seleccionar...',
  emptyLabel = 'Sin seleccionar',
}: Props<T>) {
  const [open, setOpen] = useState(false)
  const rootRef = useRef<HTMLDivElement>(null)

  useEffect(() => {
    const onDocClick = (e: MouseEvent) => {
      if (rootRef.current && !rootRef.current.contains(e.target as Node)) {
        setOpen(false)
      }
    }
    document.addEventListener('mousedown', onDocClick)
    return () => document.removeEventListener('mousedown', onDocClick)
  }, [])

  const toggle = (optionValue: T) => {
    if (value.includes(optionValue)) {
      onChange(value.filter((v) => v !== optionValue))
    } else {
      onChange([...value, optionValue])
    }
  }

  const selectedLabels = options
    .filter((o) => value.includes(o.value))
    .map((o) => o.label)

  const display = selectedLabels.length > 0 ? selectedLabels.join(', ') : emptyLabel

  return (
    <div className="multi-select" ref={rootRef}>
      <button
        type="button"
        className="multi-select-trigger"
        onClick={() => setOpen((prev) => !prev)}
        aria-haspopup="listbox"
        aria-expanded={open}
      >
        <span className={selectedLabels.length === 0 ? 'multi-select-placeholder' : undefined}>
          {display}
        </span>
        <span className="multi-select-chevron">{open ? '▴' : '▾'}</span>
      </button>
      {open && (
        <div className="multi-select-menu" role="listbox">
          {options.length === 0 ? (
            <p className="multi-select-empty">{placeholder}</p>
          ) : (
            options.map((option) => (
              <label key={String(option.value)} className="multi-select-option">
                <input
                  type="checkbox"
                  checked={value.includes(option.value)}
                  onChange={() => toggle(option.value)}
                />
                <span>{option.label}</span>
              </label>
            ))
          )}
        </div>
      )}
    </div>
  )
}
