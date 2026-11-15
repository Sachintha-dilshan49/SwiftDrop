import { expect } from '@playwright/test';
import { createHmac } from 'node:crypto';
import { execFileSync } from 'node:child_process';
import { env } from './env';

/** Helpers for things outside the gateway: Mailpit, the webhook receiver, the SSE stream, RabbitMQ and Docker. */

// ─── Mailpit ───────────────────────────────────────────────────────────────────

export interface Mail {
  ID: string;
  Subject: string;
  To: { Address: string }[];
  Snippet: string;
}

export async function mailsTo(address: string): Promise<Mail[]> {
  const url = `${env.mailpitUrl}/api/v1/search?query=${encodeURIComponent(`to:"${address}"`)}&limit=50`;
  const response = await fetch(url);
  if (!response.ok) throw new Error(`Mailpit search failed: HTTP ${response.status}`);
  return ((await response.json()) as { messages: Mail[] }).messages;
}

/** Waits until at least `count` emails have reached `address`, then returns them. */
export async function waitForMails(address: string, count = 1, timeout = 45_000) {
  await expect
    .poll(async () => (await mailsTo(address)).length, { timeout, message: `emails to ${address}` })
    .toBeGreaterThanOrEqual(count);
  return mailsTo(address);
}

// ─── Webhook receiver (WireMock) ───────────────────────────────────────────────

export interface ReceivedWebhook {
  body: string;
  headers: Record<string, string>;
}

/** Makes the receiver answer `path` with the given statuses in order, then 200 for every later call. */
export async function stubWebhook(path: string, statuses: number[] = []) {
  const scenario = `webhook-${path}`;
  const stubs = [...statuses, 200].map((status, i) => ({
    scenarioName: statuses.length ? scenario : undefined,
    requiredScenarioState: statuses.length ? (i === 0 ? 'Started' : `step-${i}`) : undefined,
    newScenarioState: statuses.length && i < statuses.length ? `step-${i + 1}` : undefined,
    request: { method: 'POST', url: path },
    response: { status },
  }));
  for (const stub of stubs) {
    const response = await fetch(`${env.webhookReceiverAdminUrl}/__admin/mappings`, {
      method: 'POST',
      headers: { 'Content-Type': 'application/json' },
      body: JSON.stringify(stub),
    });
    if (!response.ok) throw new Error(`WireMock stub failed: HTTP ${response.status}`);
  }
}

export async function webhooksAt(path: string): Promise<ReceivedWebhook[]> {
  const response = await fetch(`${env.webhookReceiverAdminUrl}/__admin/requests/find`, {
    method: 'POST',
    headers: { 'Content-Type': 'application/json' },
    body: JSON.stringify({ method: 'POST', url: path }),
  });
  if (!response.ok) throw new Error(`WireMock journal failed: HTTP ${response.status}`);
  const { requests } = (await response.json()) as {
    requests: { body: string; headers: Record<string, string | string[]> }[];
  };
  return requests.map((r) => ({
    body: r.body,
    headers: Object.fromEntries(
      Object.entries(r.headers).map(([k, v]) => [k.toLowerCase(), Array.isArray(v) ? v.join(',') : v]),
    ),
  }));
}

export async function waitForWebhooks(path: string, count = 1, timeout = 60_000) {
  await expect
    .poll(async () => (await webhooksAt(path)).length, { timeout, message: `webhooks at ${path}` })
    .toBeGreaterThanOrEqual(count);
  return webhooksAt(path);
}

/** Same scheme as notification-service's WebhookSigningService: "sha256=" + hex HMAC-SHA256 of the raw body. */
export const signature = (body: string, secret: string) =>
  `sha256=${createHmac('sha256', secret).update(body, 'utf8').digest('hex')}`;

// ─── Server-sent events ─────────────────────────────────────────────────────────

export interface SseEvent {
  event: string;
  data: string;
}

/**
 * Opens an SSE stream and collects events until it is closed. Call `close()` when done; `events` fills as they
 * arrive, so poll it with expect.poll.
 */
export function openSse(url: string) {
  const controller = new AbortController();
  const events: SseEvent[] = [];
  const ready = (async () => {
    const response = await fetch(url, { headers: { Accept: 'text/event-stream' }, signal: controller.signal });
    if (!response.ok || !response.body) throw new Error(`SSE ${url}: HTTP ${response.status}`);
    return response.body;
  })();
  const done = ready
    .then(async (body) => {
      const decoder = new TextDecoder();
      let buffer = '';
      for await (const chunk of body as unknown as AsyncIterable<Uint8Array>) {
        buffer += decoder.decode(chunk, { stream: true }).replace(/\r\n/g, '\n');
        let end: number;
        while ((end = buffer.indexOf('\n\n')) >= 0) {
          const block = buffer.slice(0, end);
          buffer = buffer.slice(end + 2);
          let event = 'message';
          const data: string[] = [];
          for (const line of block.split('\n')) {
            if (line.startsWith('event:')) event = line.slice(6).trim();
            else if (line.startsWith('data:')) data.push(line.slice(5).trimStart());
          }
          if (data.length) events.push({ event, data: data.join('\n') });
        }
      }
    })
    .catch((error: unknown) => {
      if (!controller.signal.aborted) throw error;
    });
  return { events, ready, close: () => controller.abort(), done };
}

// ─── RabbitMQ management API ────────────────────────────────────────────────────

const rabbitAuth = () =>
  `Basic ${Buffer.from(`${env.rabbitUser}:${env.rabbitPassword}`).toString('base64')}`;

/** Publishes a message to an exchange through the management HTTP API. Returns whether it was routed. */
export async function rabbitPublish(
  exchange: string,
  routingKey: string,
  payload: unknown,
  properties: Record<string, unknown> = {},
) {
  const response = await fetch(`${env.rabbitMgmtUrl}/api/exchanges/%2F/${encodeURIComponent(exchange)}/publish`, {
    method: 'POST',
    headers: { Authorization: rabbitAuth(), 'Content-Type': 'application/json' },
    body: JSON.stringify({
      properties: { content_type: 'application/json', delivery_mode: 2, ...properties },
      routing_key: routingKey,
      payload: JSON.stringify(payload),
      payload_encoding: 'string',
    }),
  });
  if (!response.ok) throw new Error(`RabbitMQ publish failed: HTTP ${response.status}`);
  return ((await response.json()) as { routed: boolean }).routed;
}

// ─── Docker Compose ─────────────────────────────────────────────────────────────

export function compose(...args: string[]) {
  const files = env.composeFiles.flatMap((file) => ['-f', file]);
  return execFileSync('docker', ['compose', ...files, ...args], { encoding: 'utf8', stdio: 'pipe' });
}

export async function waitHealthy(url: string, timeout = 180_000) {
  await expect
    .poll(
      async () => {
        try {
          const response = await fetch(url);
          return response.ok && (await response.text()).includes('"UP"');
        } catch {
          return false;
        }
      },
      { timeout, intervals: [2_000], message: `${url} healthy` },
    )
    .toBe(true);
}
