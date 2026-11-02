// Keepout's store screens on the capture emulator. The capture build shows the real UI over the Google apps that ship
// on the emulator's Play image, with a seeded week of traffic; it has no VPN service and no INTERNET permission, and
// never touches real data. `npx screenshot-studio recapture` installs it, runs this for every language and device
// size, and exports. Alone: npx screenshot-studio tour [--profile phone] [--locale de-DE].
import fs from 'node:fs';

/** A bottom-bar (or rail) tab: Home, Apps, Activity, Settings, by its text (Home's "Activity" card is described alike). */
const tab = (d, label) => d.tap(label, {textOnly: true});

/** @type {import('screenshot-studio').Tour} */
export default {
  app: 'com.bernaferrari.bravedns.storecapture',
  activity: 'com.bernaferrari.bravedns.ui.activity.LauncherAliasAppLock',
  animations: false,                       // the Home beacon animates forever; still screens settle
  animated: ['01-home', '11-home-dark'],   // …and these let it move for their frame
  // The screens show the real Google apps of the Play image; stub apps from earlier runs would mix in.
  setup(d) {
    for (const stub of JSON.parse(fs.readFileSync(new URL('fixture-apps.json', import.meta.url), 'utf8')).apps || []) {
      const id = stub.package || stub.packageName;
      if (id && d.shell('pm', 'list', 'packages', id).includes(id)) d.adb('uninstall', id);
    }
  },
  async start(d) {
    d.setClock('09:41');  // the app seeds its week of traffic up to now
    await d.launch({clear: true});
    await d.waitUntil(() => d.appFiles().includes('storecapture-seeded'), {timeout: 120_000, message: 'The capture build did not seed its fixtures'});
    await d.launch();     // start again on the seeded data, so every screen reads it from the beginning
  },
  scenes: {
    light: {
      '01-home': null,
      // Stats is the Insights view of the Activity tab (the Log · Insights header is its title).
      async '05-stats'(d) { await tab(d, 'Activity'); await d.tap('Insights'); await d.tap('24h'); },
      '06-blocked-apps': d => d.scrollTo('Most blocked apps', .12),
      // The log is the Activity tab's other view, on All: connections and DNS lookups as one timeline.
      '02-logs': d => d.tap('Log'),
      async '04-dns-blocked'(d) {
        await d.tap('Filters', {above: .3});
        await d.tap('DNS lookups');
        await d.tap('Close sheet');           // the scrim: the "DNS lookups" chip now shows under the controls
        await d.tap('Blocked', {above: .35});
      },
      '03-apps': d => tab(d, 'Apps'),
      '07-app-detail': d => d.tap('Clock', {wait: 3000}),  // its traffic chart draws in after the page settles
      async '08-settings'(d) { await d.back(); await tab(d, 'Settings'); },
      '09-dns': d => d.tap('DNS'),
    },
    dark: {
      '11-home-dark': null,
      '12-apps-dark': d => tab(d, 'Apps'),
      '13-settings-dark': d => tab(d, 'Settings'),
    },
  },
  profiles: {
    // README only: a 7" tablet in landscape (960dp, list beside details) shows the log at a readable size.
    slate7: {light: {async '02-logs'(d) { await tab(d, 'Activity'); await d.tap('Log'); }}},
  },
};
