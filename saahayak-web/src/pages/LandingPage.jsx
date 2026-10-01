import { useEffect, useState } from 'react';
import { motion } from 'framer-motion';
import { Activity, AlertTriangle, ArrowRight, FileText, Gauge, Shield, Users, Warehouse } from 'lucide-react';
import { Link } from 'react-router-dom';
import { NetworkHero } from '../components/NetworkHero';
import { ArchitectureDiagram } from '../components/ArchitectureDiagram';
import { serviceCatalog } from '../data/serviceCatalog';
import { healthCheck } from '../api/client';

const problemCards = [
  { icon: Shield, title: 'Fragmented information', copy: 'No single operational view across shelters, volunteers, and relief resources when rapid decisions matter most.' },
  { icon: Gauge, title: 'Delayed response', copy: 'Manual updates and slow handoff loops slow critical shelter capacity and dispatch decisions.' },
  { icon: FileText, title: 'Manual reporting', copy: 'Teams are still relying on spreadsheets and disconnected updates instead of live coordination data.' },
];

export function LandingPage() {
  const [statuses, setStatuses] = useState({});

  useEffect(() => {
    let active = true;

    const refresh = async () => {
      const entries = await Promise.all(
        serviceCatalog.map(async (service) => {
          const result = await healthCheck(`http://localhost:${service.port}`);
          return [service.id, result.status];
        }),
      );

      if (active) {
        setStatuses(Object.fromEntries(entries));
      }
    };

    refresh();
    return () => {
      active = false;
    };
  }, []);

  return (
    <div className="space-y-24 pb-20">
      <section className="relative overflow-hidden pt-8">
        <div className="absolute inset-0 -z-10 bg-[radial-gradient(circle_at_top_left,_rgba(46,94,255,0.18),transparent_30%),radial-gradient(circle_at_bottom_right,_rgba(31,174,107,0.16),transparent_24%),linear-gradient(180deg,#f8fbff_0%,#edf4ff_40%,#f7fafc_100%)]" />
        <div className="absolute left-10 top-16 -z-10 h-64 w-64 rounded-full bg-blue-300/20 blur-3xl" />
        <div className="absolute bottom-10 right-16 -z-10 h-72 w-72 rounded-full bg-violet-300/20 blur-3xl" />

        <div className="mx-auto grid max-w-7xl gap-12 px-4 md:px-8 lg:grid-cols-[1.15fr_0.85fr] lg:items-center">
          <div className="space-y-8">
            <motion.div initial={{ opacity: 0, y: 18 }} animate={{ opacity: 1, y: 0 }} transition={{ duration: 0.45 }} className="inline-flex items-center gap-2 rounded-full border border-sky-200 bg-white/80 px-3 py-1.5 text-sm font-medium text-sky-700 shadow-sm backdrop-blur-sm">
              <Activity className="h-4 w-4" />
              Live crisis coordination for the next 12 hours
            </motion.div>

            <motion.div initial={{ opacity: 0, y: 18 }} animate={{ opacity: 1, y: 0 }} transition={{ duration: 0.55, delay: 0.08 }} className="space-y-5">
              <h1 className="max-w-2xl font-heading text-4xl leading-none text-slate-900 md:text-[4rem]">
                Coordinate relief faster, when every minute matters.
              </h1>
              <p className="max-w-xl text-lg text-slate-600">
                SAHAYAK helps teams manage shelters, volunteers, and resources from one prepared operational view — so relief decisions happen in real time instead of by spreadsheet.
              </p>
            </motion.div>

            <motion.div initial={{ opacity: 0, y: 18 }} animate={{ opacity: 1, y: 0 }} transition={{ duration: 0.55, delay: 0.14 }} className="flex flex-wrap gap-4">
              <Link to="/dashboard" className="group inline-flex items-center gap-2 rounded-full bg-gradient-to-r from-primary via-blue-600 to-violet-600 px-6 py-3 text-sm font-semibold text-white shadow-[0_18px_35px_rgba(46,94,255,0.28)] transition duration-200 hover:-translate-y-0.5 hover:shadow-[0_20px_40px_rgba(46,94,255,0.35)]">
                View Live Dashboard <ArrowRight className="h-4 w-4 transition group-hover:translate-x-1" />
              </Link>
              <a href="https://github.com" target="_blank" rel="noreferrer" className="inline-flex items-center gap-2 rounded-full border border-slate-200 bg-white/80 px-6 py-3 text-sm font-semibold text-slate-800 shadow-sm transition hover:-translate-y-0.5 hover:border-slate-300 hover:bg-white">
                View on GitHub
              </a>
            </motion.div>

            <motion.div initial={{ opacity: 0, y: 18 }} animate={{ opacity: 1, y: 0 }} transition={{ duration: 0.55, delay: 0.2 }} className="grid max-w-lg gap-3 sm:grid-cols-3">
              {[
                { label: 'Shelters', value: '24/7', accent: 'text-blue-600' },
                { label: 'Alerts', value: '12', accent: 'text-amber-600' },
                { label: 'Response', value: '3x', accent: 'text-emerald-600' },
              ].map((stat) => (
                <div key={stat.label} className="glass-panel rounded-2xl border border-white/50 p-3">
                  <div className={`text-xl font-bold ${stat.accent}`}>{stat.value}</div>
                  <div className="text-xs uppercase tracking-[0.14em] text-slate-500">{stat.label}</div>
                </div>
              ))}
            </motion.div>
          </div>

          <div className="relative">
            <div className="absolute -left-3 top-8 z-20 rounded-2xl border border-white/60 bg-white/80 p-3 shadow-xl backdrop-blur-md">
              <div className="flex items-center gap-3">
                <div className="flex h-10 w-10 items-center justify-center rounded-xl bg-emerald-100 text-emerald-600">
                  <Shield className="h-5 w-5" />
                </div>
                <div>
                  <div className="text-[10px] uppercase tracking-[0.18em] text-slate-400">Operations</div>
                  <div className="font-semibold text-slate-800">All-clear sync</div>
                </div>
              </div>
            </div>

            <div className="absolute -right-2 bottom-8 z-20 rounded-2xl border border-amber-100 bg-amber-50/90 p-3 shadow-xl">
              <div className="text-[10px] uppercase tracking-[0.18em] text-amber-700">Live status</div>
              <div className="mt-1 flex items-center gap-2 text-sm font-semibold text-slate-800">
                <span className="h-2.5 w-2.5 rounded-full bg-emerald-500 animate-pulse" />
                Shelter network stable
              </div>
            </div>

            <NetworkHero />
          </div>
        </div>
      </section>

      <section className="mx-auto max-w-7xl px-4 md:px-8">
        <div className="grid gap-4 rounded-[28px] border border-slate-200 bg-white p-4 md:grid-cols-5">
          {serviceCatalog.map((service) => {
            const isOnline = service.status === 'live' ? (statuses[service.id] === 'online' || !statuses[service.id]) : statuses[service.id] === 'online';
            return (
              <div key={service.id} className="flex items-center justify-between rounded-2xl border border-slate-200 bg-slate-50 px-4 py-3">
                <div>
                  <div className="text-sm font-semibold text-slate-900">{service.title}</div>
                  <div className="font-mono text-xs text-slate-500">localhost:{service.port}</div>
                </div>
                <span className={`inline-flex items-center gap-2 rounded-full px-2.5 py-1 text-[11px] font-semibold ${isOnline ? 'bg-emerald-100 text-emerald-700' : 'bg-amber-100 text-amber-700'}`}>
                  <span className={`h-2.5 w-2.5 rounded-full ${isOnline ? 'bg-emerald-500' : 'bg-amber-500'} animate-pulse`} />
                  {isOnline ? 'Online' : 'Offline'}
                </span>
              </div>
            );
          })}
        </div>
      </section>

      <section className="mx-auto max-w-7xl px-4 md:px-8">
        <div className="mb-8 max-w-2xl">
          <p className="mb-3 text-sm font-semibold uppercase tracking-[0.18em] text-slate-500">Why this matters</p>
          <h2 className="font-heading text-3xl text-slate-900">Modern crisis response starts with clean coordination.</h2>
        </div>

        <div className="grid gap-6 md:grid-cols-3">
          {problemCards.map(({ icon: Icon, title, copy }, index) => (
            <motion.div whileHover={{ y: -4 }} key={title} initial={{ opacity: 0, y: 12 }} animate={{ opacity: 1, y: 0 }} transition={{ duration: 0.35, delay: index * 0.08 }} className="rounded-[28px] border border-slate-200 bg-white p-6 shadow-[0_16px_35px_rgba(15,23,42,0.04)]">
              <div className="mb-5 inline-flex h-12 w-12 items-center justify-center rounded-2xl bg-gradient-to-br from-blue-100 to-violet-100 text-primary">
                <Icon className="h-5 w-5" />
              </div>
              <h3 className="mb-3 text-xl font-semibold text-slate-900">{title}</h3>
              <p className="text-base text-slate-600">{copy}</p>
            </motion.div>
          ))}
        </div>
      </section>

      <section className="mx-auto max-w-7xl px-4 md:px-8">
        <div className="mb-8 max-w-2xl">
          <p className="mb-3 text-sm font-semibold uppercase tracking-[0.18em] text-slate-500">Platform architecture</p>
          <h2 className="font-heading text-3xl text-slate-900">Five services, one shared operational picture.</h2>
        </div>

        <div className="grid gap-6 lg:grid-cols-[1.2fr_0.8fr]">
          <ArchitectureDiagram />
          <div className="overflow-hidden rounded-[30px] border border-slate-200 bg-white shadow-[0_20px_48px_rgba(31,41,55,0.08)]">
            <img
              src="https://images.unsplash.com/photo-1517048676732-d65bc937f952?auto=format&fit=crop&w=900&q=80"
              alt="Relief coordination team meeting"
              className="h-64 w-full object-cover"
            />
            <div className="space-y-4 p-6">
              <div className="inline-flex items-center gap-2 rounded-full bg-emerald-100 px-2.5 py-1 text-[11px] font-semibold uppercase tracking-[0.12em] text-emerald-700">
                <span className="h-2 w-2 rounded-full bg-emerald-500" />
                Mission ready
              </div>
              <h3 className="font-heading text-2xl text-slate-900">Command visibility for every response lane</h3>
              <p className="text-slate-600">
                Connect field teams, shelter capacity, and mission-critical updates in a single, operationally clear workspace.
              </p>
            </div>
          </div>
        </div>
      </section>

      <section className="mx-auto max-w-7xl px-4 md:px-8">
        <div className="rounded-[32px] bg-gradient-to-r from-slate-900 via-blue-900 to-violet-900 p-8 text-white shadow-[0_26px_55px_rgba(30,41,59,0.25)] md:p-10">
          <div className="flex flex-col gap-6 md:flex-row md:items-center md:justify-between">
            <div className="max-w-xl">
              <p className="mb-3 text-sm uppercase tracking-[0.2em] text-blue-200">Built for fast action</p>
              <h2 className="font-heading text-3xl text-white md:text-4xl">Turn operational chaos into a coordinated response.</h2>
            </div>
            <Link to="/dashboard" className="inline-flex items-center justify-center rounded-full bg-white px-6 py-3 font-semibold text-slate-900 transition hover:-translate-y-0.5 hover:bg-slate-100">
              Open command board
            </Link>
          </div>
        </div>
      </section>

      <footer className="border-t border-slate-200 bg-white/80">
        <div className="mx-auto flex max-w-7xl flex-col gap-4 px-4 py-8 text-sm text-slate-600 md:flex-row md:items-center md:justify-between md:px-8">
          <div className="font-heading text-lg text-slate-900">SAHAYAK</div>
          <div>Resilient coordination infrastructure for disaster relief teams.</div>
          <a href="https://github.com" target="_blank" rel="noreferrer" className="inline-flex items-center gap-2 font-medium text-primary hover:text-blue-600">
            <Warehouse className="h-4 w-4" />
            GitHub
          </a>
        </div>
      </footer>
    </div>
  );
}
