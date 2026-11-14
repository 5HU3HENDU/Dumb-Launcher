var CACHE = 'dumb-launcher-v1';
var SHELL = [
  './',
  'index.html',
  'manifest.json',
  'icon.svg',
  'icon-maskable.svg'
];

self.addEventListener('install', function (event) {
  event.waitUntil(
    caches.open(CACHE).then(function (cache) { return cache.addAll(SHELL); })
      .then(function () { return self.skipWaiting(); })
  );
});

self.addEventListener('activate', function (event) {
  event.waitUntil(
    caches.keys().then(function (keys) {
      return Promise.all(keys.filter(function (k) { return k !== CACHE; })
        .map(function (k) { return caches.delete(k); }));
    }).then(function () { return self.clients.claim(); })
  );
});

self.addEventListener('fetch', function (event) {
  var req = event.request;
  if (req.method !== 'GET' || new URL(req.url).origin !== self.location.origin) return;
  var isFresh = req.url.indexOf('index.html') !== -1 || new URL(req.url).pathname.endsWith('/');
  if (isFresh) {
    event.respondWith(
      fetch(req).then(function (res) {
        var copy = res.clone();
        caches.open(CACHE).then(function (cache) { cache.put(req, copy); });
        return res;
      }).catch(function () { return caches.match(req); })
    );
  } else {
    event.respondWith(
      caches.match(req).then(function (hit) { return hit || fetch(req); })
    );
  }
});
