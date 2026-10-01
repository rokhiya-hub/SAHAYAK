import axios from 'axios';

const baseConfig = {
  shelter: import.meta.env.VITE_SHELTER_SERVICE_URL || 'http://localhost:8082',
  resource: import.meta.env.VITE_RESOURCE_SERVICE_URL || 'http://localhost:8083',
  reporting: import.meta.env.VITE_REPORTING_SERVICE_URL || 'http://localhost:8084',
  victim: import.meta.env.VITE_VICTIM_SERVICE_URL || 'http://localhost:8085',
  volunteer: import.meta.env.VITE_VOLUNTEER_SERVICE_URL || 'http://localhost:8081',
};

let authToken = null;

export const setAuthToken = (token) => {
  authToken = token;
};

const normalizeApiError = (error) => {
  const payload = error?.response?.data || {};
  const nextError = new Error(payload.message || error?.message || 'Request failed');
  nextError.status = error?.response?.status || 0;
  nextError.code = payload.error || 'REQUEST_FAILED';
  nextError.path = payload.path || error?.config?.url || '';
  nextError.fieldErrors = payload.fieldErrors || [];
  nextError.details = payload;
  return nextError;
};

const createClient = (serviceUrl) => {
  const client = axios.create({
    baseURL: serviceUrl,
    timeout: 15000,
  });

  client.interceptors.request.use((config) => {
    if (authToken) {
      config.headers = config.headers || {};
      config.headers.Authorization = `Bearer ${authToken}`;
    }
    return config;
  });

  client.interceptors.response.use(
    (response) => response,
    (error) => {
      const normalized = normalizeApiError(error);
      return Promise.reject(normalized);
    },
  );

  return client;
};

export const apiClients = Object.fromEntries(
  Object.entries(baseConfig).map(([key, url]) => [key, createClient(url)]),
);

export const healthCheck = async (serviceUrl) => {
  const url = serviceUrl.replace(/\/$/, '');
  const controller = new AbortController();
  const timeoutId = setTimeout(() => controller.abort(), 2000);

  try {
    const response = await axios.get(`${url}/actuator/health`, {
      signal: controller.signal,
      timeout: 2000,
    });
    clearTimeout(timeoutId);
    return {
      status: response?.data?.status === 'UP' ? 'online' : 'degraded',
      detail: response?.data,
      serviceUrl: url,
    };
  } catch (error) {
    clearTimeout(timeoutId);
    return {
      status: 'offline',
      detail: null,
      serviceUrl: url,
    };
  }
};

export default apiClients;
