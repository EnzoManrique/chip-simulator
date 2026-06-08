const CACHE_NAME = 'pokersim-v1';
const ASSETS = [
  '/',
  '/index.html',
  '/manifest.json',
  '/icons/icon-192.png',
  '/icons/icon-512.png',
  'https://fonts.googleapis.com/css2?family=Outfit:wght@300;400;500;600;700&display=swap',
  'https://cdn.jsdelivr.net/npm/sockjs-client@1.5.2/dist/sockjs.min.js',
  'https://cdn.jsdelivr.net/npm/stompjs@2.3.3/lib/stomp.min.js'
];

// Instalar Service Worker y almacenar recursos en caché
self.addEventListener('install', event => {
  event.waitUntil(
    caches.open(CACHE_NAME)
      .then(cache => {
        console.log('Cacheando recursos principales...');
        return cache.addAll(ASSETS);
      })
      .then(() => self.skipWaiting())
  );
});

// Activar Service Worker y limpiar cachés viejos
self.addEventListener('activate', event => {
  event.waitUntil(
    caches.keys().then(keys => {
      return Promise.all(
        keys.map(key => {
          if (key !== CACHE_NAME) {
            console.log('Borrando caché antigua:', key);
            return caches.delete(key);
          }
        })
      );
    }).then(() => self.clients.claim())
  );
});

// Estrategia Network-First con Fallback en Caché (adecuada para una App en desarrollo/tiempo real)
self.addEventListener('fetch', event => {
  // Evitar interceptar llamadas de WebSocket o API del Backend directamente
  if (event.request.url.includes('/ws') || event.request.url.includes('/api/')) {
    return;
  }

  event.respondWith(
    fetch(event.request)
      .then(response => {
        // Clonar la respuesta y guardarla en la caché si la llamada fue exitosa
        if (response.status === 200) {
          const responseClone = response.clone();
          caches.open(CACHE_NAME).then(cache => {
            cache.put(event.request, responseClone);
          });
        }
        return response;
      })
      .catch(() => {
        // Si no hay red, servir desde caché
        return caches.match(event.request);
      })
  );
});
