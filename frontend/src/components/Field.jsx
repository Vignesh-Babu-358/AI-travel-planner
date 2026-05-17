// Labeled input/textarea used across the forms.
export default function Field({
  label,
  name,
  value,
  onChange,
  type = 'text',
  required = false,
  textarea = false,
  rows = 8,
  placeholder,
}) {
  const shared =
    'w-full rounded-md border border-slate-300 px-3 py-2 text-sm outline-none focus:border-slate-500 focus:ring-1 focus:ring-slate-500'
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
