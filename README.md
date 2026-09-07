# SmartCanales

IPTV para Android TV: importa playlists **M3U**, reproduce HTTP/HLS y AceStream, y permite **subir el .m3u por la red local**.

## Canales por USB

1. Copia tu `.m3u` a la **raiz del pendrive** o a una carpeta `SmartCanales/`.
2. Inserta el USB en la TV.
3. Abre la app y pulsa **Importar M3U desde USB** (con el mando).
4. Si Android pide permiso de archivos, acéptalo.

## Canales por defecto

Edita `app/src/main/assets/default_channels.m3u` y vuelve a generar el APK.
Se cargan solo si la base de datos está vacía (primera instalación / datos borrados).


## Qué acepta el M3U

- Entradas `#EXTINF` + URL (`http://`, `https://`, `acestream://HASH`, hash de 40 hex, o URLs del motor AceStream con `id=` / `infohash=`).
- Atributos opcionales `group-title` y `tvg-name`.

## Stack

- Kotlin, Compose for TV, Media3, Room, Retrofit (AceStream local), servidor HTTP embebido (puerto **8765**).

## Build

Abre el proyecto en Android Studio → **Build APK**.
Salida: `app/build/outputs/apk/debug/app-debug.apk`.

## Notas

- TV y PC deben estar en la misma red.
- Si la IP no aparece bien, mira la IP de la TV en Ajustes de red y usa `http://ESA_IP:8765`.
- Cada subida **reemplaza** la lista de canales.
