# MaxReader - Lector rápido RSVP para EPUB

[English](README.md) · [Français](README.fr.md) · **Español** · [简体中文](README.zh-CN.md)

Un lector de EPUB para Android rápido y sin distracciones, basado en **RSVP** (Rapid Serial Visual Presentation, presentación visual serial rápida), con una letra en negrita centrada en el ORP (Optimal Recognition Point, punto óptimo de reconocimiento).

## Características

- **Lectura RSVP**: las palabras aparecen una a una a la velocidad que elijas
- **Resalte del ORP**: el carácter del punto óptimo de reconocimiento se muestra en negrita y rojo, centrado en la pantalla
- **Palabra anterior**: la palabra previa permanece visible arriba, para echar un vistazo atrás
- **Frase de contexto**: las últimas N palabras (configurable) se muestran debajo para situar el párrafo
- **Velocidad ajustable**: de 50 a 1500 palabras por minuto, con el deslizador o los botones +/-
- **Pausas de puntuación**: pausa independiente y configurable (en ms) para:
  - la coma, el punto y coma y los dos puntos
  - el punto, la exclamación y la interrogación
  - los saltos de párrafo
- **Tema oscuro**: interfaz oscura y cómoda para la vista
- **Compatibilidad con EPUB**: abre archivos `.epub` estándar mediante el selector de archivos de Android
- **Interfaz multilingüe**: inglés, francés, español y chino simplificado

## Compilación

1. Abre el proyecto en Android Studio (Hedgehog o posterior)
2. Sincroniza Gradle
3. Ejecútalo en un dispositivo o emulador (API 26 o superior)

## Arquitectura

```
app/
├── epub/           # Análisis de EPUB (epublib + Jsoup)
├── model/          # Clases de datos (BookData, RsvpWord, etc.)
├── rsvp/           # Motor de reproducción RSVP (basado en corrutinas)
├── settings/       # Preferencias con DataStore
├── ui/
│   ├── components/ # RsvpWordDisplay, ContextDisplay
│   ├── screens/    # HomeScreen, ReaderScreen, SettingsScreen
│   └── theme/      # Colores del tema oscuro
└── viewmodel/      # ReaderViewModel
```

## Controles

- **Tocar la pantalla**: reproducir / pausar
- **Botones +/-**: ajustan la velocidad en pasos de 25 ppm
- **⏪ / ⏩**: retroceden o avanzan 10 palabras
- **Ajustes**: todas las opciones de ritmo y presentación

## Idiomas

La interfaz está traducida al inglés, francés, español y chino simplificado. La aplicación sigue el idioma del sistema; en Android 13 y posteriores también puedes asignarle un idioma propio desde *Ajustes → Aplicaciones → MaxReader → Idioma*.

## Licencia

Distribuido bajo la Licencia Apache, versión 2.0. Consulta [LICENSE](LICENSE) para el texto completo.

En [LICENSE.es](LICENSE.es) se incluye una traducción al español no oficial, únicamente a título informativo; **solo la versión en inglés es vinculante**.

```
Copyright 2026 Maxime Pinard

Licensed under the Apache License, Version 2.0 (the "License");
you may not use this file except in compliance with the License.
You may obtain a copy of the License at

    http://www.apache.org/licenses/LICENSE-2.0

Unless required by applicable law or agreed to in writing, software
distributed under the License is distributed on an "AS IS" BASIS,
WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
See the License for the specific language governing permissions and
limitations under the License.
```
