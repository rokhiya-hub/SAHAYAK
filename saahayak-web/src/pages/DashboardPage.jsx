import { useEffect, useMemo, useState } from 'react';
import { AlertTriangle, ArrowUpRight, CheckCircle2, LoaderCircle, Plus, RefreshCcw, ShieldAlert, Users } from 'lucide-react';
import { PieChart, Pie, Cell, ResponsiveContainer } from 'recharts';
import { useNavigate } from 'react-router-dom';
import apiClients from '../api/client';

const statusColors = {
  OPEN: '#1FAE6B',
  FULL: '#FF6B4A',
  CLOSED: '#F5A623',
};

export function DashboardPage() {
  const navigate = useNavigate();
  const [shelters, setShelters] = useState([]);
  const [loading, setLoading] = useState(true);
  const [notice, setNotice] = useState('');
  const [busyIds, setBusyIds] = useState([]);
  const [demoResult, setDemoResult] = useState(null);
  const [demoLoading, setDemoLoading] = useState(false);

  const fetchShelters = async () => {
    setLoading(true);
    try {
      const response = await apiClients.shelter.get('/api/shelters');
      setShelters(response.data.content || []);
      setNotice('');
    } catch (error) {
      if (error.status === 401) {
        navigate('/login');
        return;
      }
      setNotice(error.message || "Can't reach shelter-management-service at localhost:8082 — is it running?");
    } finally {
      setLoading(false);
    }
  };

  useEffect(() => {
    fetchShelters();
  }, []);

  const summary = useMemo(() => {
    const totalCapacity = shelters.reduce((sum, item) => sum + Number(item.totalCapacity || 0), 0);
    const totalOccupancy = shelters.reduce((sum, item) => sum + Number(item.currentOccupancy || 0), 0);
    const fullPercent = totalCapacity ? Math.round((totalOccupancy / totalCapacity) * 100) : 0;

    return {
      totalShelters: shelters.length,
      totalCapacity,
      totalOccupancy,
      fullPercent,
    };
  }, [shelters]);

  const chartData = [
    { name: 'Occupied', value: summary.totalOccupancy, color: '#2E5EFF' },
    { name: 'Available', value: Math.max(summary.totalCapacity - summary.totalOccupancy, 0), color: '#1FAE6B' },
  ];

  const updateShelterInList = (id, transform) => {
    setShelters((current) =>
      current.map((shelter) => {
        if (shelter.id !== id) return shelter;
        return transform(shelter);
      }),
    );
  };

  const handleCheckIn = async (id) => {
    setBusyIds((current) => [...current, id]);
    const before = shelters.find((item) => item.id === id);
    if (before) {
      updateShelterInList(id, (shelter) => ({
        ...shelter,
        currentOccupancy: Math.min(shelter.currentOccupancy + 1, shelter.totalCapacity),
      }));
    }

    try {
      const response = await apiClients.shelter.post(`/api/shelters/${id}/checkin`);
      setNotice(`Checked in successfully at ${response.data.name}.`);
      await fetchShelters();
    } catch (error) {
      if (error.status === 409) {
        setNotice('Shelter is full — the second request was rejected with a 409 conflict, matching the backend concurrency rule.');
      } else {
        setNotice(error.message || 'Check-in failed.');
      }
      await fetchShelters();
    } finally {
      setBusyIds((current) => current.filter((item) => item !== id));
    }
  };

  const handleCheckOut = async (id) => {
    setBusyIds((current) => [...current, id]);
    const before = shelters.find((item) => item.id === id);
    if (before) {
      updateShelterInList(id, (shelter) => ({
        ...shelter,
        currentOccupancy: Math.max(shelter.currentOccupancy - 1, 0),
      }));
    }

    try {
      const response = await apiClients.shelter.post(`/api/shelters/${id}/checkout`);
      setNotice(`Checked out successfully at ${response.data.name}.`);
      await fetchShelters();
    } catch (error) {
      setNotice(error.message || 'Check-out failed.');
      await fetchShelters();
    } finally {
      setBusyIds((current) => current.filter((item) => item !== id));
    }
  };

  const handleRunConcurrencyDemo = async () => {
    setDemoLoading(true);
    setDemoResult(null);

    try {
      let target = shelters.find((item) => item.availableCapacity === 1);

      if (!target) {
        const created = await apiClients.shelter.post('/api/shelters', {
          name: 'Demo Capacity Shelf',
          address: 'Demo Street, Bengaluru',
          latitude: 12.9716,
          longitude: 77.5946,
          totalCapacity: 1,
          resourcesAvailable: ['Demo Kit'],
        });
        target = created.data;
      }

      if (target.availableCapacity !== 1) {
        const adjustUntilOneSeat = await Promise.all(
          Array.from({ length: Math.max(target.totalCapacity - 1 - target.currentOccupancy, 0) }, () =>
            apiClients.shelter.post(`/api/shelters/${target.id}/checkin`),
          ),
        );
        if (adjustUntilOneSeat.length) {
          const refreshed = await apiClients.shelter.get(`/api/shelters/${target.id}`);
          target = refreshed.data;
        }
      }

      if (target.availableCapacity !== 1) {
        throw new Error('No shelter in the exact one-seat-left state was available for the concurrency demo.');
      }

      const results = await Promise.allSettled([
        apiClients.shelter.post(`/api/shelters/${target.id}/checkin`),
        apiClients.shelter.post(`/api/shelters/${target.id}/checkin`),
      ]);

      setDemoResult({
        target,
        results: results.map((result) => ({
          status: result.status,
          payload: result.status === 'fulfilled' ? result.value.data : (result.reason?.response?.data || result.reason?.details || { error: result.reason?.message || 'Unknown error' }),
        })),
      });

      await fetchShelters();
    } catch (error) {
      setNotice(error.message || 'Concurrency demo failed.');
    } finally {
      setDemoLoading(false);
    }
  };

  if (loading) {
    return (
      <div className="space-y-6">
        <div className="grid gap-4 md:grid-cols-4">
          {Array.from({ length: 4 }).map((_, i) => (
            <div key={i} className="h-28 animate-pulse rounded-[26px] border border-slate-200 bg-slate-100" />
          ))}
        </div>
        <div className="grid gap-6 lg:grid-cols-2">
          {Array.from({ length: 4 }).map((_, i) => (
            <div key={i} className="h-48 animate-pulse rounded-[26px] border border-slate-200 bg-slate-100" />
          ))}
        </div>
      </div>
    );
  }

  return (
    <div className="space-y-8 pb-12">
      <div className="flex flex-col gap-4 lg:flex-row lg:items-center lg:justify-between">
        <div>
          <p className="text-sm font-semibold uppercase tracking-[0.18em] text-slate-500">Live operations</p>
          <h1 className="font-heading text-4xl text-slate-900">Shelter dashboard</h1>
        </div>
        <div className="flex gap-3">
          <button onClick={fetchShelters} className="inline-flex items-center gap-2 rounded-full border border-slate-200 bg-white px-4 py-2 text-sm font-medium text-slate-700 hover:bg-slate-50">
            <RefreshCcw className="h-4 w-4" /> Refresh
          </button>
          <button onClick={handleRunConcurrencyDemo} disabled={demoLoading} className="inline-flex items-center gap-2 rounded-full bg-primary px-4 py-2 text-sm font-semibold text-white hover:bg-blue-600 disabled:opacity-60">
            {demoLoading ? <LoaderCircle className="h-4 w-4 animate-spin" /> : <ArrowUpRight className="h-4 w-4" />}
            Run Concurrency Demo
          </button>
        </div>
      </div>

      {notice && (
        <div className="rounded-2xl border border-amber-200 bg-amber-50 p-4 text-sm text-amber-900">
          <div className="flex items-start gap-2">
            <AlertTriangle className="mt-0.5 h-4 w-4 text-amber-600" />
            <span>{notice}</span>
          </div>
        </div>
      )}

      <div className="grid gap-4 md:grid-cols-4">
        <div className="rounded-[26px] border border-slate-200 bg-white p-5">
          <div className="text-sm text-slate-500">Total shelters</div>
          <div className="mt-3 flex items-end justify-between">
            <span className="font-heading text-3xl text-slate-900">{summary.totalShelters}</span>
            <ShieldAlert className="h-6 w-6 text-primary" />
          </div>
        </div>
        <div className="rounded-[26px] border border-slate-200 bg-white p-5">
          <div className="text-sm text-slate-500">Total capacity</div>
          <div className="mt-3 flex items-end justify-between">
            <span className="font-heading text-3xl text-slate-900">{summary.totalCapacity}</span>
            <Users className="h-6 w-6 text-success" />
          </div>
        </div>
        <div className="rounded-[26px] border border-slate-200 bg-white p-5">
          <div className="text-sm text-slate-500">Current occupancy</div>
          <div className="mt-3 flex items-end justify-between">
            <span className="font-heading text-3xl text-slate-900">{summary.totalOccupancy}</span>
            <CheckCircle2 className="h-6 w-6 text-success" />
          </div>
        </div>
        <div className="rounded-[26px] border border-slate-200 bg-white p-5">
          <div className="text-sm text-slate-500">% full</div>
          <div className="mt-3 flex items-end justify-between">
            <span className="font-heading text-3xl text-slate-900">{summary.fullPercent}%</span>
            <div className="h-3 w-12 overflow-hidden rounded-full bg-slate-200">
              <div className="h-full rounded-full bg-primary" style={{ width: `${Math.min(summary.fullPercent, 100)}%` }} />
            </div>
          </div>
        </div>
      </div>

      <div className="grid gap-6 lg:grid-cols-[1.15fr_0.85fr]">
        <div className="rounded-[28px] border border-slate-200 bg-white p-5">
          <div className="mb-4 flex items-center justify-between">
            <h2 className="font-heading text-2xl text-slate-900">Shelter occupancy</h2>
            <button onClick={() => navigate('/shelters/new')} className="inline-flex items-center gap-2 rounded-full bg-primary px-3 py-2 text-sm font-semibold text-white hover:bg-blue-600">
              <Plus className="h-4 w-4" /> Add shelter
            </button>
          </div>

          {shelters.length === 0 ? (
            <div className="rounded-[24px] border border-dashed border-slate-300 bg-slate-50 p-8 text-center">
              <div className="mx-auto mb-4 inline-flex h-12 w-12 items-center justify-center rounded-full bg-white text-slate-500">
                <ShieldAlert className="h-5 w-5" />
              </div>
              <h3 className="text-xl font-semibold text-slate-900">No shelters registered yet</h3>
              <p className="mt-2 text-slate-600">Add your first shelter to begin operational tracking.</p>
              <button onClick={() => navigate('/shelters/new')} className="mt-5 rounded-full bg-primary px-5 py-2.5 text-sm font-semibold text-white hover:bg-blue-600">
                Create shelter
              </button>
            </div>
          ) : (
            <div className="grid gap-4 md:grid-cols-2">
              {shelters.map((shelter) => {
                const percent = Math.min(Math.round((shelter.currentOccupancy / shelter.totalCapacity) * 100), 100);
                const fillColor = percent >= 95 ? '#FF6B4A' : percent >= 70 ? '#F5A623' : '#1FAE6B';
                const isBusy = busyIds.includes(shelter.id);

                return (
                  <div key={shelter.id} className="rounded-[24px] border border-slate-200 bg-slate-50 p-4">
                    <div className="mb-3 flex items-start justify-between gap-3">
                      <div>
                        <h3 className="text-lg font-semibold text-slate-900">{shelter.name}</h3>
                        <p className="text-sm text-slate-600">{shelter.address}</p>
                      </div>
                      <span className="inline-flex items-center rounded-full px-2 py-1 text-xs font-semibold" style={{ backgroundColor: `${statusColors[shelter.status] || '#2E5EFF'}22`, color: statusColors[shelter.status] || '#2E5EFF' }}>
                        {shelter.status}
                      </span>
                    </div>

                    <div className="mb-2 flex items-center justify-between text-sm text-slate-600">
                      <span>{shelter.currentOccupancy} / {shelter.totalCapacity}</span>
                      <span>{percent}%</span>
                    </div>
                    <div className="h-2.5 overflow-hidden rounded-full bg-slate-200">
                      <div className="h-full rounded-full" style={{ width: `${percent}%`, backgroundColor: fillColor }} />
                    </div>

                    <div className="mt-4 flex gap-2">
                      <button onClick={() => handleCheckIn(shelter.id)} disabled={isBusy || shelter.currentOccupancy >= shelter.totalCapacity} className="flex-1 rounded-full bg-primary px-3 py-2 text-sm font-semibold text-white hover:bg-blue-600 disabled:cursor-not-allowed disabled:opacity-50">
                        {isBusy ? 'Working...' : 'Check In'}
                      </button>
                      <button onClick={() => handleCheckOut(shelter.id)} disabled={isBusy || shelter.currentOccupancy <= 0} className="flex-1 rounded-full border border-slate-200 bg-white px-3 py-2 text-sm font-semibold text-slate-700 hover:bg-slate-50 disabled:cursor-not-allowed disabled:opacity-50">
                        {isBusy ? 'Working...' : 'Check Out'}
                      </button>
                    </div>
                  </div>
                );
              })}
            </div>
          )}
        </div>

        <div className="rounded-[28px] border border-slate-200 bg-white p-5">
          <h2 className="mb-4 font-heading text-2xl text-slate-900">Utilization</h2>
          <div className="h-64">
            <ResponsiveContainer width="100%" height="100%">
              <PieChart>
                <Pie data={chartData} dataKey="value" nameKey="name" innerRadius={50} outerRadius={80} paddingAngle={4}>
                  {chartData.map((entry) => (
                    <Cell key={entry.name} fill={entry.color} />
                  ))}
                </Pie>
              </PieChart>
            </ResponsiveContainer>
          </div>
          <div className="mt-2 space-y-2">
            {chartData.map((entry) => (
              <div key={entry.name} className="flex items-center justify-between text-sm text-slate-600">
                <span className="inline-flex items-center gap-2"><span className="h-2.5 w-2.5 rounded-full" style={{ backgroundColor: entry.color }} /> {entry.name}</span>
                <span className="font-semibold text-slate-900">{entry.value}</span>
              </div>
            ))}
          </div>
        </div>
      </div>

      {demoResult && (
        <div className="rounded-[28px] border border-slate-200 bg-white p-5">
          <h2 className="mb-4 font-heading text-2xl text-slate-900">Concurrency demo</h2>
          <div className="rounded-2xl border border-slate-200 bg-slate-50 p-4">
            <div className="mb-3 text-sm text-slate-600">Target shelter: <span className="font-semibold text-slate-900">{demoResult.target.name}</span></div>
            <div className="grid gap-4 md:grid-cols-2">
              {demoResult.results.map((result, index) => (
                <div key={index} className="rounded-2xl border border-slate-200 bg-white p-4">
                  <div className="mb-3 flex items-center justify-between">
                    <span className="text-sm font-semibold text-slate-700">Request {index + 1}</span>
                    <span className={`rounded-full px-2 py-1 text-xs font-semibold ${result.status === 'fulfilled' ? 'bg-emerald-100 text-emerald-700' : 'bg-rose-100 text-rose-700'}`}>
                      {result.status === 'fulfilled' ? 'Success' : 'Conflict'}
                    </span>
                  </div>
                  <pre className="overflow-x-auto rounded-xl bg-slate-900 p-3 font-mono text-xs text-slate-100">
                    {JSON.stringify(result.payload, null, 2)}
                  </pre>
                </div>
              ))}
            </div>
          </div>
        </div>
      )}
    </div>
  );
}
