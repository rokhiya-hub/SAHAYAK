import { useEffect, useState } from 'react';
import { useNavigate, useParams } from 'react-router-dom';
import apiClients from '../api/client';

const emptyForm = {
  name: '',
  address: '',
  latitude: '',
  longitude: '',
  totalCapacity: '',
  resourcesAvailable: '',
};

export function ShelterFormPage() {
  const navigate = useNavigate();
  const { id } = useParams();
  const isEdit = Boolean(id);
  const [form, setForm] = useState(emptyForm);
  const [errors, setErrors] = useState({});
  const [loading, setLoading] = useState(isEdit);
  const [saving, setSaving] = useState(false);

  useEffect(() => {
    if (!isEdit) return;

    const loadShelter = async () => {
      try {
        const response = await apiClients.shelter.get(`/api/shelters/${id}`);
        const shelter = response.data;
        setForm({
          name: shelter.name || '',
          address: shelter.address || '',
          latitude: String(shelter.latitude ?? ''),
          longitude: String(shelter.longitude ?? ''),
          totalCapacity: String(shelter.totalCapacity ?? ''),
          resourcesAvailable: (shelter.resourcesAvailable || []).join(', '),
        });
      } catch (error) {
        setErrors({ form: error.message || 'Unable to load shelter.' });
      } finally {
        setLoading(false);
      }
    };

    loadShelter();
  }, [id, isEdit]);

  const handleChange = (event) => {
    const { name, value } = event.target;
    setForm((current) => ({ ...current, [name]: value }));
    setErrors((current) => ({ ...current, [name]: '' }));
  };

  const validate = () => {
    const nextErrors = {};

    if (!form.name.trim()) nextErrors.name = 'Shelter name is required.';
    if (!form.address.trim()) nextErrors.address = 'Address is required.';
    if (form.latitude === '' || Number.isNaN(Number(form.latitude))) nextErrors.latitude = 'Latitude is required.';
    if (form.longitude === '' || Number.isNaN(Number(form.longitude))) nextErrors.longitude = 'Longitude is required.';
    if (!form.totalCapacity || Number(form.totalCapacity) < 1) nextErrors.totalCapacity = 'Capacity must be at least 1.';

    return nextErrors;
  };

  const handleSubmit = async (event) => {
    event.preventDefault();
    const nextErrors = validate();
    setErrors(nextErrors);
    if (Object.keys(nextErrors).length) return;

    setSaving(true);
    try {
      const payload = {
        name: form.name.trim(),
        address: form.address.trim(),
        latitude: Number(form.latitude),
        longitude: Number(form.longitude),
        totalCapacity: Number(form.totalCapacity),
        resourcesAvailable: form.resourcesAvailable
          .split(',')
          .map((item) => item.trim())
          .filter(Boolean),
      };

      if (isEdit) {
        await apiClients.shelter.put(`/api/shelters/${id}`, payload);
      } else {
        await apiClients.shelter.post('/api/shelters', payload);
      }
      navigate('/dashboard');
    } catch (error) {
      const backendErrors = error?.details?.fieldErrors || {};
      setErrors({ ...nextErrors, ...backendErrors });
    } finally {
      setSaving(false);
    }
  };

  if (loading) {
    return <div className="h-48 animate-pulse rounded-[28px] border border-slate-200 bg-slate-100" />;
  }

  return (
    <div className="mx-auto max-w-3xl rounded-[30px] border border-slate-200 bg-white p-8">
      <div className="mb-6">
        <p className="text-sm font-semibold uppercase tracking-[0.18em] text-slate-500">{isEdit ? 'Edit shelter' : 'New shelter'}</p>
        <h1 className="mt-2 font-heading text-4xl text-slate-900">{isEdit ? 'Update shelter details' : 'Register a new shelter'}</h1>
      </div>

      <form onSubmit={handleSubmit} className="space-y-5">
        <div className="grid gap-5 md:grid-cols-2">
          <div className="md:col-span-2">
            <label className="mb-1 block text-sm font-medium text-slate-700">Shelter name</label>
            <input name="name" value={form.name} onChange={handleChange} className="w-full rounded-2xl border border-slate-200 bg-slate-50 px-4 py-3 outline-none transition focus:border-primary focus:bg-white" />
            {errors.name && <div className="mt-1 text-sm text-red-600">{errors.name}</div>}
          </div>

          <div className="md:col-span-2">
            <label className="mb-1 block text-sm font-medium text-slate-700">Address</label>
            <textarea name="address" value={form.address} onChange={handleChange} rows="3" className="w-full rounded-2xl border border-slate-200 bg-slate-50 px-4 py-3 outline-none transition focus:border-primary focus:bg-white" />
            {errors.address && <div className="mt-1 text-sm text-red-600">{errors.address}</div>}
          </div>

          <div>
            <label className="mb-1 block text-sm font-medium text-slate-700">Latitude</label>
            <input type="number" step="0.000001" name="latitude" value={form.latitude} onChange={handleChange} className="w-full rounded-2xl border border-slate-200 bg-slate-50 px-4 py-3 outline-none transition focus:border-primary focus:bg-white" />
            {errors.latitude && <div className="mt-1 text-sm text-red-600">{errors.latitude}</div>}
          </div>

          <div>
            <label className="mb-1 block text-sm font-medium text-slate-700">Longitude</label>
            <input type="number" step="0.000001" name="longitude" value={form.longitude} onChange={handleChange} className="w-full rounded-2xl border border-slate-200 bg-slate-50 px-4 py-3 outline-none transition focus:border-primary focus:bg-white" />
            {errors.longitude && <div className="mt-1 text-sm text-red-600">{errors.longitude}</div>}
          </div>

          <div>
            <label className="mb-1 block text-sm font-medium text-slate-700">Capacity</label>
            <input type="number" min="1" name="totalCapacity" value={form.totalCapacity} onChange={handleChange} className="w-full rounded-2xl border border-slate-200 bg-slate-50 px-4 py-3 outline-none transition focus:border-primary focus:bg-white" />
            {errors.totalCapacity && <div className="mt-1 text-sm text-red-600">{errors.totalCapacity}</div>}
          </div>

          <div>
            <label className="mb-1 block text-sm font-medium text-slate-700">Resources available</label>
            <input name="resourcesAvailable" value={form.resourcesAvailable} onChange={handleChange} placeholder="Food, Water, Blankets" className="w-full rounded-2xl border border-slate-200 bg-slate-50 px-4 py-3 outline-none transition focus:border-primary focus:bg-white" />
          </div>
        </div>

        {errors.form && <div className="rounded-2xl border border-red-200 bg-red-50 p-3 text-sm text-red-700">{errors.form}</div>}

        <div className="flex justify-end gap-3">
          <button type="button" onClick={() => navigate('/dashboard')} className="rounded-full border border-slate-200 bg-white px-5 py-2.5 text-sm font-semibold text-slate-700 hover:bg-slate-50">Cancel</button>
          <button type="submit" disabled={saving} className="rounded-full bg-primary px-5 py-2.5 text-sm font-semibold text-white hover:bg-blue-600 disabled:opacity-60">
            {saving ? (isEdit ? 'Saving...' : 'Creating...') : (isEdit ? 'Save changes' : 'Create shelter')}
          </button>
        </div>
      </form>
    </div>
  );
}
