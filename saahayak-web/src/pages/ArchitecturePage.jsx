import { useEffect, useState } from 'react';
import { ArrowRight, CalendarClock, CloudCog, Database, ShieldCheck } from 'lucide-react';
import { ArchitectureDiagram } from '../components/ArchitectureDiagram';
import { serviceCatalog } from '../data/serviceCatalog';
import { healthCheck } from '../api/client';

const roadmap = [
  { phase: 'Shipped', items: ['Shelter management API', 'JWT auth flows', 'Capacity tracking and concurrency protections'], tone: 'emerald' },
  { phase: 'In progress', items: ['Volunteer dispatch workflow', 'Resource inventory sync', 'Victim registry integrations'], tone: 'amber' },
  { phase: 'Planned', items: ['Reporting UI and analytics', 'Cross-service operator dashboards', 'Public deployment pipeline'], tone: 'blue' },
];

export function ArchitecturePage() {
  const [statuses, setStatuses] = useState({});

  useEffect(() => {
    let active = true;

    const refresh = async () => {
      const result = await Promise.all(
        serviceCatalog.map(async (service) => [service.id, (await healthCheck(`http://localhost:${service.port}`)).status]),
      );
      if (active) setStatuses(Object.fromEntries(result));
    };

    refresh();
    return () => {
      active = false;
    };
  }, []);

  return (
    <div className="space-y-10 pb-12">
      <div className="max-w-3xl">
        <p className="text-sm font-semibold uppercase tracking-[0.18em] text-slate-500">System map</p>
        <h1 className="mt-3 font-heading text-4xl text-slate-900">Operational architecture</h1>
      </div>

      <ArchitectureDiagram />

      <div className="grid gap-6 md:grid-cols-2 xl:grid-cols-5">
        {serviceCatalog.map((service) => {
          const online = service.status === 'live' ? (statuses[service.id] === 'online' || !statuses[service.id]) : statuses[service.id] === 'online';
          return (
            <div key={service.id} className="rounded-[24px] border border-slate-200 bg-white p-4">
              <div className="mb-4 flex items-center justify-between">
                <span className={`inline-flex items-center gap-2 rounded-full px-2.5 py-1 text-[11px] font-semibold ${online ? 'bg-emerald-100 text-emerald-700' : 'bg-amber-100 text-amber-700'}`}>
                  <span className={`h-2 w-2 rounded-full ${online ? 'bg-emerald-500' : 'bg-amber-500'}`} />
                  {online ? 'Online' : 'Offline'}
                </span>
                <ShieldCheck className="h-4 w-4 text-primary" />
              </div>
              <div className="font-heading text-xl text-slate-900">{service.title}</div>
              <div className="mt-2 font-mono text-xs text-slate-500">localhost:{service.port}</div>
              <div className="mt-4 flex flex-wrap gap-2">
                {service.stack.map((tech) => (
                  <span key={tech} className="rounded-full border border-slate-200 bg-slate-50 px-2 py-1 text-[10px] font-medium text-slate-700">{tech}</span>
                ))}
              </div>
            </div>
          );
        })}
      </div>

      <div className="rounded-[30px] border border-slate-200 bg-white p-6">
        <div className="mb-6 flex items-center gap-3">
          <CalendarClock className="h-6 w-6 text-primary" />
          <h2 className="font-heading text-2xl text-slate-900">Roadmap</h2>
        </div>

        <div className="grid gap-5 lg:grid-cols-3">
          {roadmap.map((step) => (
            <div key={step.phase} className="rounded-[26px] border border-slate-200 bg-slate-50 p-4">
              <div className="mb-3 flex items-center gap-2">
                <span className={`h-2.5 w-2.5 rounded-full ${step.tone === 'emerald' ? 'bg-emerald-500' : step.tone === 'amber' ? 'bg-amber-500' : 'bg-primary'}`} />
                <div className="font-heading text-xl text-slate-900">{step.phase}</div>
              </div>
              <ul className="space-y-2 text-sm text-slate-600">
                {step.items.map((item) => (
                  <li key={item} className="flex items-start gap-2">
                    <ArrowRight className="mt-0.5 h-4 w-4 text-primary" />
                    {item}
                  </li>
                ))}
              </ul>
            </div>
          ))}
        </div>
      </div>
    </div>
  );
}
