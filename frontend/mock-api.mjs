/**
 * Dev-only stand-in for the Spring Boot API (no JVM, no AWS credentials needed).
 *
 *   npm run mock:api        # listens on :8080, the same port the Vite proxy targets
 *
 * It implements exactly the endpoints the dashboard calls, with the same JSON shapes as the
 * backend DTOs. Delete it whenever the real backend is running; `make backend-run` is preferred.
 */
import { createServer } from 'node:http';

const PORT = Number(process.env.PORT || 8080);
const DEMO_USERNAME = process.env.DEMO_USERNAME || 'admin';
const DEMO_PASSWORD = process.env.DEMO_PASSWORD || 'admin123';
const TOKEN_TTL_MS = 8 * 60 * 60 * 1000;

const INSTANCES = [
  {
    instanceId: 'i-0a1b2c3d4e5f60710',
    name: 'api-server',
    instanceType: 't3.micro',
    state: 'running',
    privateIp: '10.0.1.10',
    publicIp: '54.10.10.10',
    launchTime: daysAgo(18),
    baseLoad: 14,
  },
  {
    instanceId: 'i-0a1b2c3d4e5f60711',
    name: 'worker-node',
    instanceType: 't3.small',
    state: 'running',
    privateIp: '10.0.1.11',
    publicIp: '54.10.10.11',
    launchTime: daysAgo(11),
    baseLoad: 55,
  },
  {
    instanceId: 'i-0a1b2c3d4e5f60712',
    name: 'staging-server',
    instanceType: 't3.micro',
    state: 'stopped',
    privateIp: '10.0.1.12',
    publicIp: '54.10.10.12',
    launchTime: daysAgo(30),
    baseLoad: 4,
  },
];

function daysAgo(days) {
  return new Date(Date.now() - days * 86400000).toISOString();
}

function send(response, status, body) {
  const payload = JSON.stringify(body);
  response.writeHead(status, {
    'content-type': 'application/json',
    'content-length': Buffer.byteLength(payload),
  });
  response.end(payload);
}

function readBody(request) {
  return new Promise((resolve) => {
    let raw = '';
    request.on('data', (chunk) => {
      raw += chunk;
      if (raw.length > 1e6) request.destroy();
    });
    request.on('end', () => {
      try {
        resolve(raw ? JSON.parse(raw) : {});
      } catch {
        resolve({});
      }
    });
  });
}

// Unsigned, base64url JSON: just enough for the dashboard to exercise the authenticated paths.
function issueToken(username) {
  const payload = { sub: username, role: 'ADMIN', exp: Date.now() + TOKEN_TTL_MS };
  return `mock.${Buffer.from(JSON.stringify(payload)).toString('base64url')}`;
}

function bearerValid(request) {
  const header = request.headers.authorization ?? '';
  if (!header.startsWith('Bearer ')) return false;
  try {
    const payload = JSON.parse(Buffer.from(header.slice(7).split('.')[1] ?? '', 'base64url').toString());
    return typeof payload.exp === 'number' && payload.exp > Date.now();
  } catch {
    return false;
  }
}

function periodFor(hours) {
  return hours <= 1 ? 60 : hours <= 12 ? 300 : 900;
}

function emptySeries(instanceId) {
  return { instanceId, metric: 'CPUUtilization', unit: 'percent', source: 'DEMO', periodSeconds: 300, points: [] };
}

function cpuSeries(instance, hours) {
  const periodSeconds = periodFor(hours);
  const count = Math.max(6, Math.min(2000, Math.round((hours * 3600) / periodSeconds)));
  if (instance.state !== 'running') {
    return emptySeries(instance.instanceId);
  }
  const end = Date.now();
  const points = Array.from({ length: count }, (_, index) => {
    const step = count - index;
    const wave = 14 * Math.sin(step * 0.55) + 6 * Math.sin(step * 0.17);
    const value = Math.max(2, Math.min(97, instance.baseLoad + wave + Math.random() * 6));
    return {
      timestamp: new Date(end - (count - 1 - index) * periodSeconds * 1000).toISOString(),
      value: Number(value.toFixed(2)),
    };
  });
  return { instanceId: instance.instanceId, metric: 'CPUUtilization', unit: 'percent', source: 'DEMO', periodSeconds, points };
}

const server = createServer(async (request, response) => {
  const url = new URL(request.url, `http://${request.headers.host}`);
  const path = url.pathname.replace(/\/+$/, '') || '/';
  console.log(`${request.method} ${url.pathname}`);

  if (request.method === 'GET' && path === '/api/health') {
    return send(response, 200, { status: 'UP', service: 'cloud-monitor-mock', region: 'ap-south-1', demoMode: true });
  }

  if (request.method === 'POST' && path === '/api/auth/login') {
    const body = await readBody(request);
    if (body.username !== DEMO_USERNAME || body.password !== DEMO_PASSWORD) {
      return send(response, 401, { status: 401, error: 'Unauthorized', message: 'Invalid username or password', timestamp: new Date().toISOString() });
    }
    return send(response, 200, { token: issueToken(body.username), tokenType: 'Bearer', role: 'ADMIN', expiresAt: new Date(Date.now() + TOKEN_TTL_MS).toISOString() });
  }

  if (!bearerValid(request)) {
    return send(response, 401, { status: 401, error: 'Unauthorized', message: 'A valid bearer token is required', timestamp: new Date().toISOString() });
  }

  const cpuMatch = path.match(/^\/api\/monitoring\/instances\/([^/]+)\/cpu$/);
  if (request.method === 'GET' && cpuMatch) {
    const instance = INSTANCES.find((candidate) => candidate.instanceId === decodeURIComponent(cpuMatch[1]));
    const hours = Number(url.searchParams.get('hours') ?? 3);
    if (!Number.isInteger(hours) || hours < 1 || hours > 24) {
      return send(response, 400, { status: 400, error: 'Bad Request', message: 'hours must be between 1 and 24', timestamp: new Date().toISOString() });
    }
    // Unknown ids answer like CloudWatch does for an id with no data: an empty series.
    return send(response, 200, instance ? cpuSeries(instance, hours) : emptySeries(decodeURIComponent(cpuMatch[1]), hours));
  }

  if (request.method === 'GET' && path === '/api/monitoring/instances') {
    return send(
      response,
      200,
      INSTANCES.map(({ baseLoad, ...instance }) => instance),
    );
  }

  return send(response, 404, { status: 404, error: 'Not Found', message: `No mock route for ${request.method} ${path}`, timestamp: new Date().toISOString() });
});

server.listen(PORT, '0.0.0.0', () => {
  console.log(`mock API on http://localhost:${PORT}/api (user "${DEMO_USERNAME}", password "${DEMO_PASSWORD}")`);
});
