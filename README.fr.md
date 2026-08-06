# MaxReader - Lecteur rapide RSVP pour EPUB

[English](README.md) · **Français** · [Español](README.es.md) · [简体中文](README.zh-CN.md)

Un lecteur EPUB Android rapide et épuré, fondé sur la **RSVP** (Rapid Serial Visual Presentation, présentation visuelle sérielle rapide), avec une lettre en gras centrée sur l’ORP (Optimal Recognition Point, point de reconnaissance optimal).

## Fonctionnalités

- **Lecture RSVP** : les mots défilent un à un à la vitesse de votre choix
- **Mise en évidence de l’ORP** : le caractère du point de reconnaissance optimal est en gras et en rouge, centré à l’écran
- **Affichage du mot précédent** : le mot précédent reste visible en haut, pour un coup d’œil en arrière
- **Phrase de contexte** : les N derniers mots (configurable) sont affichés en dessous pour situer le paragraphe
- **Vitesse réglable** : de 50 à 1500 mots par minute, au curseur ou avec les boutons +/-
- **Pauses de ponctuation** : pause distincte et configurable (en ms) pour :
  - la virgule, le point-virgule et les deux-points
  - le point, le point d’exclamation et le point d’interrogation
  - les sauts de paragraphe
- **Thème sombre** : interface sombre reposante pour les yeux
- **Prise en charge de l’EPUB** : ouvre les fichiers `.epub` standard via le sélecteur de fichiers Android
- **Interface multilingue** : anglais, français, espagnol et chinois simplifié

## Compilation

1. Ouvrez le projet dans Android Studio (Hedgehog ou plus récent)
2. Synchronisez Gradle
3. Lancez l’application sur un appareil ou un émulateur (API 26 et plus)

## Architecture

```
app/
├── epub/           # Analyse des EPUB (epublib + Jsoup)
├── model/          # Classes de données (BookData, RsvpWord, etc.)
├── rsvp/           # Moteur de lecture RSVP (basé sur les coroutines)
├── settings/       # Préférences DataStore
├── ui/
│   ├── components/ # RsvpWordDisplay, ContextDisplay
│   ├── screens/    # HomeScreen, ReaderScreen, SettingsScreen
│   └── theme/      # Couleurs du thème sombre
└── viewmodel/      # ReaderViewModel
```

## Commandes

- **Appui sur l’écran** : lecture / pause
- **Boutons +/-** : ajustent la vitesse par pas de 25 mots/min
- **⏪ / ⏩** : reculent ou avancent de 10 mots
- **Paramètres** : toutes les options de rythme et d’affichage

## Langues

L’interface est traduite en anglais, français, espagnol et chinois simplifié. L’application suit la langue du système ; sous Android 13 et plus, vous pouvez aussi lui attribuer une langue distincte depuis *Paramètres → Applications → MaxReader → Langue*.

## Licence

Distribué sous la licence Apache, version 2.0. Voir [LICENSE](LICENSE) pour le texte intégral.

Une traduction française non officielle est fournie à titre indicatif dans [LICENSE.fr](LICENSE.fr) ; **seule la version anglaise fait foi**.

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
