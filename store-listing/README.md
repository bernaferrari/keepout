# Keepout · Play listing

Built with [Screenshot Studio](https://github.com/bernaferrari/screenshot-studio) (`npx screenshot-studio`); the app keeps no copy of it.

| Change | File |
| --- | --- |
| Cards, order, captures | `gallery.json` |
| Words per language | `<locale>/copy.json`, `<locale>/listing.json` |
| Look | `theme.css`, `theme.js` |
| Canvas and capture sizes | `profiles.json` |
| Seeded apps and traffic | `app/src/storeCapture/.../StoreFixtures.kt` |
| Scene tour | `capture.tour.mjs` (build and runs: `studio.json` → `capture`) |

```sh
npx screenshot-studio recapture                 # build, install, capture every size on emulator-5578, export
npx screenshot-studio tour --profile phone        # the scene tour alone
npx screenshot-studio studio                    # editor
npx screenshot-studio export --locale en-US
npx screenshot-studio check                     # fast check after each edit
```

The capture build (`app/src/storeCapture`) shows the real UI over the Google apps that ship on the emulator's
Play image (Chrome, Gmail, YouTube, Maps, Clock, ...), with rules and a week of traffic seeded against their real
UIDs and the hosts they really use. The VPN state is pinned to "protected"; the build has no VPN service and no
INTERNET permission. Nothing is downloaded or installed for the screenshots: the script only removes the stub
apps an older run may have left (`fixture-apps.json`). It sets the emulator clock to 9:41 so the seeded traffic
ends at the status-bar time, captures Home with its real animation running, and restores the clock, animations
and display size afterwards.
