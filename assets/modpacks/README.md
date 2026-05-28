Modpack JSON format

- Location: assets/modpacks/*.json for sample files.
- To import into the app, copy the JSON into the app's `theme-mods.json` or use the Theme Editor.

Fields:
- id (string): unique id
- name (string)
- author (string)
- lightSchemes (array): each is {id, name, tokens}
- darkSchemes (array): same shape
- style (object): {shapeStyle, buttonHeightDp, textureRule, wallpaperUri}

Wallpaper URIs
- Use the document picker for correct URIs and persistable permissions.
- Hand-editing: `wallpaperUri` should be a content:// or file:// URI accessible by the app. If the app lacks permission, the wallpaper won't load.

Example: see `stone_temple_sample.json`