#!/usr/bin/env python3
"""Build the Dumb Launcher PWA: one screen, 100 famous apps, deep-linked.

Reads the APPS table below (name, category, deep-link URL) and writes
index.html, manifest.json and sw.js into this repo's root.

Rules (Shiva, 2026-11-14):
- ONE screen, no scroll. All 100 tiles in a single grid.
- NO top row of most-used apps: those apps sit in the grid with their peers.
- Fixed positions forever: categories in a set order, alphabetical inside
  each category, so a spot is predictable from the name.
- Tapping a tile opens the real app: https App Links for third-party apps
  (Android opens the installed app, else the website); Android intent://
  URLs for the system apps that have no website.
- Dead apps were swapped for live ones (see SWAPS below).
"""
import json
import os

HERE = os.path.dirname(os.path.abspath(__file__))
ROOT = os.path.dirname(HERE)

CATS = [
    ("social", "Social", "#e64980"),
    ("chat", "Chat", "#12b76a"),
    ("money", "Money", "#0e9f6e"),
    ("shopping", "Shopping", "#f59f00"),
    ("food", "Food & Drink", "#e8590c"),
    ("travel", "Travel & Maps", "#1c7ed6"),
    ("video", "Video", "#e03131"),
    ("music", "Music & Audio", "#7048e8"),
    ("work", "Work & Files", "#4263eb"),
    ("tools", "Utilities", "#868e96"),
]

# (name, category, deep-link URL)
APPS = [
    # Social
    ("Facebook", "social", "https://www.facebook.com/"),
    ("Instagram", "social", "https://www.instagram.com/"),
    ("LinkedIn", "social", "https://www.linkedin.com/"),
    ("Mastodon", "social", "https://mastodon.social/"),
    ("Pinterest", "social", "https://www.pinterest.com/"),
    ("Quora", "social", "https://www.quora.com/"),
    ("Reddit", "social", "https://www.reddit.com/"),
    ("Snapchat", "social", "https://www.snapchat.com/"),
    ("Threads", "social", "https://www.threads.net/"),
    ("X", "social", "https://x.com/"),
    # Chat
    ("Discord", "chat", "https://discord.com/"),
    ("Line", "chat", "https://line.me/"),
    ("Messages", "chat", "https://messages.google.com/"),
    ("Messenger", "chat", "https://www.messenger.com/"),
    ("Signal", "chat", "https://signal.org/"),
    ("Slack", "chat", "https://slack.com/"),
    ("Telegram", "chat", "https://t.me/"),
    ("Viber", "chat", "https://www.viber.com/"),
    ("WeChat", "chat", "https://www.wechat.com/"),
    ("WhatsApp", "chat", "https://wa.me/"),
    # Money
    ("CRED", "money", "https://cred.club/"),
    ("Google Pay", "money", "https://pay.google.com/"),
    ("Groww", "money", "https://groww.in/"),
    ("HDFC Bank", "money", "https://www.hdfcbank.com/"),
    ("PayPal", "money", "https://www.paypal.com/"),
    ("Paytm", "money", "https://paytm.com/"),
    ("PhonePe", "money", "https://www.phonepe.com/"),
    ("SBI YONO", "money", "https://www.sbiyono.sbi/"),
    ("Wise", "money", "https://wise.com/"),
    ("Zerodha", "money", "https://kite.zerodha.com/"),
    # Shopping
    ("Ajio", "shopping", "https://www.ajio.com/"),
    ("Amazon", "shopping", "https://www.amazon.in/"),
    ("BigBasket", "shopping", "https://www.bigbasket.com/"),
    ("Blinkit", "shopping", "https://blinkit.com/"),
    ("eBay", "shopping", "https://www.ebay.com/"),
    ("Flipkart", "shopping", "https://www.flipkart.com/"),
    ("IKEA", "shopping", "https://www.ikea.com/in/en/"),
    ("Meesho", "shopping", "https://www.meesho.com/"),
    ("Myntra", "shopping", "https://www.myntra.com/"),
    ("Nykaa", "shopping", "https://www.nykaa.com/"),
    # Food & Drink
    ("Burger King", "food", "https://www.burgerking.in/"),
    ("Domino's", "food", "https://www.dominos.co.in/"),
    ("EatSure", "food", "https://www.eatsure.com/"),
    ("FreshMenu", "food", "https://www.freshmenu.com/"),
    ("Licious", "food", "https://www.licious.in/"),
    ("McDonald's", "food", "https://www.mcdonaldsindia.com/"),
    ("Pizza Hut", "food", "https://www.pizzahut.co.in/"),
    ("Starbucks", "food", "https://www.starbucks.in/"),
    ("Swiggy", "food", "https://www.swiggy.com/"),
    ("Zomato", "food", "https://www.zomato.com/"),
    # Travel & Maps
    ("Airbnb", "travel", "https://www.airbnb.co.in/"),
    ("Booking.com", "travel", "https://www.booking.com/"),
    ("Google Maps", "travel", "https://maps.google.com/"),
    ("IRCTC Rail", "travel", "https://www.irctc.co.in/"),
    ("ixigo", "travel", "https://www.ixigo.com/"),
    ("MakeMyTrip", "travel", "https://www.makemytrip.com/"),
    ("Ola", "travel", "https://www.olacabs.com/"),
    ("Rapido", "travel", "https://www.rapido.bike/"),
    ("redBus", "travel", "https://www.redbus.in/"),
    ("Uber", "travel", "https://www.uber.com/"),
    # Video
    ("Apple TV+", "video", "https://tv.apple.com/"),
    ("JioHotstar", "video", "https://www.jiohotstar.com/"),
    ("MX Player", "video", "https://www.mxplayer.in/"),
    ("Netflix", "video", "https://www.netflix.com/"),
    ("Prime Video", "video", "https://www.primevideo.com/"),
    ("SonyLIV", "video", "https://www.sonyliv.com/"),
    ("Twitch", "video", "https://www.twitch.tv/"),
    ("Vimeo", "video", "https://vimeo.com/"),
    ("YouTube", "video", "https://www.youtube.com/"),
    ("ZEE5", "video", "https://www.zee5.com/"),
    # Music & Audio
    ("Amazon Music", "music", "https://music.amazon.in/"),
    ("Apple Music", "music", "https://music.apple.com/"),
    ("Audible", "music", "https://www.audible.in/"),
    ("Gaana", "music", "https://gaana.com/"),
    ("Hungama Music", "music", "https://www.hungama.com/music/"),
    ("JioSaavn", "music", "https://www.jiosaavn.com/"),
    ("Shazam", "music", "https://www.shazam.com/"),
    ("SoundCloud", "music", "https://soundcloud.com/"),
    ("Spotify", "music", "https://open.spotify.com/"),
    ("YouTube Music", "music", "https://music.youtube.com/"),
    # Work & Files
    ("Calendar", "work", "https://calendar.google.com/"),
    ("Docs", "work", "https://docs.google.com/"),
    ("Dropbox", "work", "https://www.dropbox.com/"),
    ("Gmail", "work", "https://mail.google.com/"),
    ("Google Drive", "work", "https://drive.google.com/"),
    ("Google Meet", "work", "https://meet.google.com/"),
    ("Notion", "work", "https://www.notion.so/"),
    ("OneDrive", "work", "https://onedrive.live.com/"),
    ("Sheets", "work", "https://sheets.google.com/"),
    ("Zoom", "work", "https://zoom.us/"),
    # Utilities — system apps: intent:// where there is no website
    ("Calculator", "tools", "intent://#Intent;action=android.intent.action.MAIN;category=android.intent.category.APP_CALCULATOR;end"),
    ("Camera", "tools", "intent://#Intent;action=android.media.action.STILL_IMAGE_CAMERA;end"),
    ("Chrome", "tools", "https://www.google.com/"),
    ("Clock", "tools", "intent://#Intent;action=android.intent.action.SHOW_ALARMS;end"),
    ("Files", "tools", "intent://#Intent;package=com.google.android.apps.nbu.files;end"),
    ("Notes", "tools", "https://keep.google.com/"),
    ("Passwords", "tools", "https://passwords.google.com/"),
    ("Photos", "tools", "https://photos.google.com/"),
    ("Settings", "tools", "intent://#Intent;action=android.settings.SETTINGS;end"),
    ("Weather", "tools", "https://www.google.com/search?q=weather"),
]

# Dead apps in the original dummy list, swapped for live ones:
# Skype (retired May 2025) -> WeChat; Wynk Music (shut Aug 2024) -> Hungama Music;
# JioCinema (merged into JioHotstar Feb 2025) -> Apple TV+; Hotstar -> JioHotstar;
# Dunzo (shut down) -> Burger King; Uber Eats India (left India 2020) -> Pizza Hut.

CATMAP = {cid: (name, color) for cid, name, color in CATS}


def initials(name):
    clean = "".join(ch for ch in name if ch.isalnum() or ch == " ").strip()
    parts = clean.split()
    if len(parts) > 1:
        return (parts[0][0] + parts[1][0]).upper()
    return clean[:2].upper()


def shade(hexcolor, amt):
    n = int(hexcolor[1:], 16)
    r = min(255, max(0, (n >> 16) + amt))
    g = min(255, max(0, ((n >> 8) & 255) + amt))
    b = min(255, max(0, (n & 255) + amt))
    return "rgb(%d,%d,%d)" % (r, g, b)


def build_index():
    assert len(APPS) == 100, "expected 100 apps, got %d" % len(APPS)
    tiles = []
    for cid, _, _ in CATS:
        group = sorted([a for a in APPS if a[1] == cid], key=lambda a: a[0].lower())
        assert len(group) == 10, "category %s has %d apps" % (cid, len(group))
        for name, _, url in group:
            _, color = CATMAP[cid]
            bg = "linear-gradient(145deg,%s,%s)" % (shade(color, 26), shade(color, -22))
            tiles.append(
                '<a class="tile" href="{url}" target="_blank" rel="noopener" '
                'aria-label="Open {name}" title="{name}">'
                '<span class="icon" style="background:{bg}">{ini}</span></a>'.format(
                    url=url.replace("&", "&amp;").replace('"', "&quot;"),
                    name=name.replace('"', "&quot;"),
                    bg=bg, ini=initials(name)))
    legend = "".join(
        '<span class="leg"><span class="dot" style="background:{c}"></span>{n}</span>'.format(c=color, n=name)
        for _, name, color in CATS)
    html = """<!doctype html>
<html lang="en">
<head>
<meta charset="utf-8">
<meta name="viewport" content="width=device-width, initial-scale=1, viewport-fit=cover">
<meta name="theme-color" content="#0f0f0f">
<meta name="description" content="Dumb Launcher — all 100 apps on one screen. Tap any tile to open the app. No scroll, fixed spots.">
<meta property="og:type" content="website">
<meta property="og:url" content="https://5hu3hendu.github.io/Dumb-Launcher/">
<meta property="og:title" content="Dumb Launcher">
<meta property="og:description" content="All 100 apps on one screen. Tap a tile to open the app.">
<meta property="og:image" content="https://dummyimage.com/1200x630/ff0000/ffffff&amp;text=Dumb+Launcher">
<meta property="og:image:width" content="1200">
<meta property="og:image:height" content="630">
<meta property="og:image:type" content="image/png">
<meta name="twitter:card" content="summary_large_image">
<meta name="twitter:title" content="Dumb Launcher">
<meta name="twitter:description" content="All 100 apps on one screen. Tap a tile to open the app.">
<meta name="twitter:image" content="https://dummyimage.com/1200x630/ff0000/ffffff&amp;text=Dumb+Launcher">
<link rel="manifest" href="manifest.json">
<link rel="icon" href="icon.svg" type="image/svg+xml">
<title>Dumb Launcher</title>
<style>
  * { box-sizing: border-box; margin: 0; padding: 0; }
  html, body { height: 100%; }
  body {
    background: #0f0f0f; color: #f1f1f1;
    font-family: system-ui, -apple-system, "Segoe UI", Roboto, sans-serif;
    height: 100dvh; display: flex; flex-direction: column; overflow: hidden;
    -webkit-tap-highlight-color: transparent;
  }
  header { padding: 14px 14px 6px; }
  #clock { font-size: 34px; font-weight: 700; line-height: 1.05; letter-spacing: -0.5px; }
  #date { font-size: 13px; color: #a1a1a1; margin-top: 2px; }
  main { flex: 1; min-height: 0; padding: 6px 10px; }
  .grid {
    display: grid; grid-template-columns: repeat(8, 1fr); grid-auto-rows: 1fr;
    gap: 6px; height: 100%;
  }
  .tile { display: block; text-decoration: none; min-width: 0; min-height: 0; border-radius: 12px; }
  .tile:active { transform: scale(0.92); }
  .icon {
    display: flex; align-items: center; justify-content: center;
    width: 100%; height: 100%; border-radius: 12px;
    color: #fff; font-weight: 700; font-size: 19px; letter-spacing: 0.5px;
  }
  footer { padding: 8px 12px 12px; }
  .legend {
    display: flex; gap: 12px; overflow-x: auto; scrollbar-width: none;
    font-size: 11px; color: #a1a1a1; white-space: nowrap;
  }
  .legend::-webkit-scrollbar { display: none; }
  .leg { display: inline-flex; align-items: center; gap: 4px; }
  .dot { width: 8px; height: 8px; border-radius: 50%; display: inline-block; }
  @media (prefers-color-scheme: light) {
    body { background: #f5f5f4; color: #111; }
    #date { color: #666; }
    .legend { color: #666; }
  }
</style>
</head>
<body>
<header>
  <div id="clock">--:--</div>
  <div id="date"></div>
</header>
<main>
  <div class="grid">
""" + "\n".join(tiles) + """
  </div>
</main>
<footer><div class="legend">""" + legend + """</div></footer>
<script>
  function tick() {
    var d = new Date();
    var hh = String(d.getHours()).padStart(2, "0");
    var mm = String(d.getMinutes()).padStart(2, "0");
    document.getElementById("clock").textContent = hh + ":" + mm;
    document.getElementById("date").textContent = d.toLocaleDateString("en-IN", {
      weekday: "long", day: "numeric", month: "long"
    });
  }
  tick();
  setInterval(tick, 15000);
  if ("serviceWorker" in navigator) {
    window.addEventListener("load", function () {
      navigator.serviceWorker.register("sw.js").catch(function () {});
    });
  }
</script>
</body>
</html>
"""
    with open(os.path.join(ROOT, "index.html"), "w") as f:
        f.write(html)
    print("wrote index.html (%d tiles)" % len(tiles))


def build_manifest():
    manifest = {
        "name": "Dumb Launcher",
        "short_name": "Launcher",
        "description": "All 100 apps on one screen. Tap a tile to open the app.",
        "display": "standalone",
        "orientation": "portrait",
        "theme_color": "#0f0f0f",
        "background_color": "#0f0f0f",
        "start_url": ".",
        "scope": ".",
        "icons": [
            {"src": "icon.svg", "sizes": "any", "type": "image/svg+xml", "purpose": "any"},
            {"src": "icon-maskable.svg", "sizes": "any", "type": "image/svg+xml", "purpose": "maskable"},
        ],
    }
    with open(os.path.join(ROOT, "manifest.json"), "w") as f:
        json.dump(manifest, f, indent=2)
    print("wrote manifest.json")


def build_sw():
    sw = """var CACHE = 'dumb-launcher-v1';
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
"""
    with open(os.path.join(ROOT, "sw.js"), "w") as f:
        f.write(sw)
    print("wrote sw.js")


if __name__ == "__main__":
    build_index()
    build_manifest()
    build_sw()
