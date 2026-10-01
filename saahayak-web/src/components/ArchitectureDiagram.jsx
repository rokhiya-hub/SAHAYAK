import { useState } from 'react';
import { serviceCatalog } from '../data/serviceCatalog';

export function ArchitectureDiagram() {
  const [selected, setSelected] = useState(serviceCatalog[0]);

  return (
    <div className="grid gap-7 lg:grid-cols-[1.5fr_0.8fr]">
      <div className="rounded-[30px] border border-slate-200 bg-white p-5">
        <svg viewBox="0 0 680 360" className="h-[330px] w-full">
          <rect x="18" y="18" width="644" height="324" rx="28" fill="#FBFBFE" stroke="#E7E9F2" />

          {[{ x: 125, y: 90, id: 'shelter' }, { x: 515, y: 90, id: 'volunteer' }, { x: 325, y: 190, id: 'victim' }, { x: 125, y: 260, id: 'resource' }, { x: 515, y: 260, id: 'reporting' }].map((node) => {
            const item = serviceCatalog.find((entry) => entry.id === node.id);
            const active = selected.id === node.id;
            return (
              <g key={node.id} onClick={() => setSelected(item)} className="cursor-pointer">
                <rect
                  x={node.x - 80}
                  y={node.y - 32}
                  width="160"
                  height="64"
                  rx="18"
                  fill={active ? '#EEF3FF' : '#FFFFFF'}
                  stroke={active ? '#2E5EFF' : '#E7E9F2'}
                  strokeWidth={active ? 2 : 1}
                />
                <circle cx={node.x} cy={node.y - 8} r="7" fill={item.status === 'live' ? '#1FAE6B' : '#F5A623'} />
                <text x={node.x} y={node.y + 18} textAnchor="middle" fontSize="14" fill="#141B2D" fontWeight="600">
                  {item.title.replace(' ', '\n')}
                </text>
              </g>
            );
          })}

          <path d="M205 90H435" stroke="#DDE4FF" strokeWidth="2" />
          <path d="M415 135L350 170" stroke="#DDE4FF" strokeWidth="2" />
          <path d="M205 260H435" stroke="#DDE4FF" strokeWidth="2" />
          <path d="M200 125L320 195" stroke="#DDE4FF" strokeWidth="2" />
          <path d="M430 235L350 195" stroke="#DDE4FF" strokeWidth="2" />
        </svg>
      </div>

      <div className="rounded-[30px] border border-slate-200 bg-white p-5">
        <div className="mb-4 flex items-center justify-between">
          <span className="text-sm font-semibold uppercase tracking-[0.15em] text-slate-500">Service</span>
          <span className={`inline-flex items-center rounded-full px-2.5 py-1 text-xs font-semibold ${selected.status === 'live' ? 'bg-emerald-100 text-emerald-700' : 'bg-amber-100 text-amber-700'}`}>
            {selected.statusLabel}
          </span>
        </div>

        <div className="space-y-3">
          <h3 className="font-heading text-2xl text-slate-900">{selected.title}</h3>
          <p className="text-sm text-slate-600">{selected.summary}</p>

          <div className="rounded-2xl border border-slate-200 bg-slate-50 p-3">
            <div className="mb-2 text-xs font-semibold uppercase tracking-[0.18em] text-slate-500">Port</div>
            <div className="font-mono text-sm text-slate-800">localhost:{selected.port}</div>
          </div>

          <div>
            <div className="mb-2 text-xs font-semibold uppercase tracking-[0.18em] text-slate-500">Tech stack</div>
            <div className="flex flex-wrap gap-2">
              {selected.stack.map((entry) => (
                <span key={entry} className="rounded-full border border-slate-200 bg-white px-2.5 py-1 text-xs font-medium text-slate-700">
                  {entry}
                </span>
              ))}
            </div>
          </div>
        </div>
      </div>
    </div>
  );
}
