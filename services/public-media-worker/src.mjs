const PUBLIC_BUCKETS = new Set(['shop-profile-images', 'menu-images', 'event-images']);
const CLIENT_CACHE = 'private,max-age=86400,immutable';

function responseWithoutCache(status, method = 'GET') {
  return new Response(method === 'HEAD' ? null : String(status), {
    status, headers: { 'Cache-Control': 'no-store', 'Cloudflare-CDN-Cache-Control': 'no-store', 'X-Content-Type-Options': 'nosniff' },
  });
}
function objectKey(url) {
  if (url.search || /%2f|%5c/i.test(url.pathname)) return null;
  let key;
  try { key = decodeURIComponent(url.pathname.slice(1)); } catch { return null; }
  const [bucket, ...path] = key.split('/');
  if (!PUBLIC_BUCKETS.has(bucket) || path.length === 0 || !path.join('/') || path.join('/').length > 512
    || path.some((segment) => !segment) || key.includes('..') || /[\\%?#\x00-\x1f\x7f]/.test(key)) return null;
  return key;
}
function notModified(request, object) {
  const etags = request.headers.get('If-None-Match');
  if (etags !== null) return etags.split(',').some((tag) => tag.trim() === '*' || tag.trim().replace(/^W\//, '') === object.httpEtag);
  const since = Date.parse(request.headers.get('If-Modified-Since') ?? '');
  return Number.isFinite(since) && Math.floor(object.uploaded.getTime() / 1000) <= Math.floor(since / 1000);
}

export default {
  async fetch(request, env) {
    const method = request.method;
    if (!['GET', 'HEAD'].includes(method)) {
      const response = responseWithoutCache(405, method);
      response.headers.set('Allow', 'GET, HEAD');
      return response;
    }
    const url = new URL(request.url);
    if (url.pathname === '/health' && !url.search) return new Response(method === 'HEAD' ? null : 'ok', { headers: { 'Cache-Control': 'no-store' } });
    const key = objectKey(url);
    if (!key) return responseWithoutCache(404, method);
    try {
      // Only the public bucket is bound. No private bucket or external URL can be selected by a request.
      const object = method === 'HEAD' ? await env.PUBLIC_IMAGES.head(key) : await env.PUBLIC_IMAGES.get(key);
      if (!object) return responseWithoutCache(404, method);
      const headers = new Headers();
      object.writeHttpMetadata(headers);
      headers.set('ETag', object.httpEtag);
      headers.set('Last-Modified', object.uploaded.toUTCString());
      headers.set('Content-Type', object.httpMetadata?.contentType || 'application/octet-stream');
      headers.set('Content-Length', String(object.size));
      headers.set('X-Content-Type-Options', 'nosniff');
      headers.set('Content-Security-Policy', "default-src 'none'; sandbox");
      // Cache only on the device. workers.dev has no owned zone for global purge.
      // Every network request reads R2, so a deleted object is never served from an edge cache.
      headers.set('Cache-Control', CLIENT_CACHE);
      headers.set('CDN-Cache-Control', 'no-store');
      headers.set('Cloudflare-CDN-Cache-Control', 'no-store');
      if (notModified(request, object)) {
        await object.body?.cancel();
        headers.delete('Content-Length');
        return new Response(null, { status: 304, headers });
      }
      return new Response(method === 'HEAD' ? null : object.body, { headers });
    } catch { return responseWithoutCache(503, method); }
  },
};
