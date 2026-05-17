import ReactMarkdown from 'react-markdown'
import remarkGfm from 'remark-gfm'

// Tailwind-styled renderers (Tailwind v4 core has no `prose`, so we style
// each element explicitly).
const components = {
  h1: (p) => <h1 className="mt-6 mb-3 text-2xl font-bold text-slate-900" {...p} />,
  h2: (p) => <h2 className="mt-6 mb-2 text-xl font-semibold text-slate-900" {...p} />,
  h3: (p) => <h3 className="mt-4 mb-2 text-lg font-semibold text-slate-800" {...p} />,
  p: (p) => <p className="my-2 leading-relaxed text-slate-700" {...p} />,
  ul: (p) => <ul className="my-2 list-disc space-y-1 pl-6 text-slate-700" {...p} />,
  ol: (p) => <ol className="my-2 list-decimal space-y-1 pl-6 text-slate-700" {...p} />,
  li: (p) => <li className="leading-relaxed" {...p} />,
  strong: (p) => <strong className="font-semibold text-slate-900" {...p} />,
  a: (p) => <a className="text-blue-600 underline" target="_blank" rel="noreferrer" {...p} />,
  code: (p) => (
    <code className="rounded bg-slate-100 px-1.5 py-0.5 font-mono text-sm text-slate-800" {...p} />
  ),
  hr: () => <hr className="my-4 border-slate-200" />,
  table: (p) => (
    <table className="my-3 w-full border-collapse text-sm" {...p} />
  ),
  th: (p) => <th className="border border-slate-300 bg-slate-50 px-2 py-1 text-left" {...p} />,
  td: (p) => <td className="border border-slate-300 px-2 py-1" {...p} />,
  blockquote: (p) => (
    <blockquote className="my-3 border-l-4 border-slate-300 pl-4 text-slate-600 italic" {...p} />
  ),
}

export default function Markdown({ children }) {
  return (
    <ReactMarkdown remarkPlugins={[remarkGfm]} components={components}>
      {children || ''}
    </ReactMarkdown>
  )
}
