# T-063 Live TV: switch for the automatic genre categories

- **Issue:** #96 (request in the comments: a setting to turn off the automatic genre categories, so the provider's categories follow "All channels")
- **Status:** implemented, not built, not device tested

## Problem

In Live TV the six automatic genre categories (Sport, Movies, Documentaries, Kids, News, Music) always come before the provider's categories. They could only be hidden one by one.

## Change

- `LiveSettings.genres` (default on, so nothing changes for existing users), stored as key `genres` in the existing file `ipvio_livetv` (additive key).
- `LiveTab.kt`: the category list uses `LiveGenreLogic.visible(...)`. With the switch off there are no genre rows; the provider's categories (favorite categories first, then the saved or provider order) follow "All channels". If a genre category was selected when the switch is turned off, the view goes back to "All channels".
- Settings > Live TV: new switch "Genre categories in Live TV". The page reset sets it back to on.
- English dictionary rows in `DictEn1.kt` (other languages fall back to English).

Movies and Series already have the switch "Automatic genres" (Settings > Genres & categories). It is off by default, so the provider's categories are shown. The reporter wrote that Movies and Series have the same problem; this was not reproduced. It is possible they switched the setting on.

## Risks

Low. One boolean, default unchanged. Hidden genre categories stay hidden when the switch is turned on again.

## Tests

- JVM: `LiveGenreLogicTest` 3/3 and `DictEn1Test` 5/5, run here with the Kotlin compiler bundled in Gradle. Syntax check of the changed Android files: no syntax errors. Not built, not device tested.

### Manual test steps (maintainer)

1. Settings > Live TV: turn "Genre categories in Live TV" off. Open Live TV: after "All channels" come the provider's categories; there is no Sport, Movies, Documentaries, Kids, News or Music row.
2. Turn it on again: the six rows are back (the ones you hid earlier stay hidden).
3. Select "Sport" (genre), go to the settings, turn it off, come back: Live TV shows "All channels".
4. "Reset this page" on Settings > Live TV turns it on again.
