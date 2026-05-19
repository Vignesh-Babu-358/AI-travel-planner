// Labeled input/select/textarea/checkbox used across the forms.
export default function Field({
  label,
  name,
  value,
  onChange,
  type = 'text',
  required = false,
  textarea = false,
  options,
  checkbox = false,
  rows = 8,
  placeholder,
}) {
  const shared =
    'w-full rounded-md border border-slate-300 px-3 py-2 text-sm outline-none focus:border-slate-500 focus:ring-1 focus:ring-slate-500'

  if (checkbox) {
    return (
      <label className="flex items-center gap-2 py-2">
        <input
          name={name}
          type="checkbox"
          checked={!!value}
          onChange={onChange}
          className="h-4 w-4 rounded border-slate-300"
        />
        <span className="text-sm font-medium text-slate-700">{label}</span>
      </label>
    )
  }

  return (
    <label className="block">
      <span className="mb-1 block text-sm font-medium text-slate-700">
        {label}
        {required && <span className="text-red-500"> *</span>}
      </span>
      {textarea ? (
        <textarea
          name={name}
          value={value}
          onChange={onChange}
          rows={rows}
          placeholder={placeholder}
          className={`${shared} font-mono`}
        />
      ) : options ? (
        <select name={name} value={value} onChange={onChange} className={shared}>
          {options.map((o) => (
            <option key={o.value} value={o.value}>
              {o.label}
            </option>
          ))}
        </select>
      ) : (
        <input
          name={name}
          type={type}
          value={value}
          onChange={onChange}
          required={required}
          placeholder={placeholder}
          className={shared}
        />
      )}
    </label>
  )
}
