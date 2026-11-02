// Keepout's store screens on the capture emulator. The capture build shows the real UI over the Google apps that ship
// on the emulator's Play image, with a seeded week of traffic; it has no VPN service and no INTERNET permission, and
// never touches real data. `npx screenshot-studio recapture` installs it, runs this for every language and device
// size, and exports. Alone: npx screenshot-studio tour [--profile phone] [--locale de-DE].
import fs from 'node:fs';

const tap = (d, label, {wait = 2200, textOnly = false, above} = {}) =>
  d.tap(label, {wait, textOnly, attempts: 5, ...(above !== undefined && {within: b => b[1] < d.height * above})});
/** A bottom-bar (or rail) tab: Home, Apps, Activity, Settings. Home's "Activity" card lands on the same tab. */
const tab = (d, label) => tap(d, label, {wait: 2500, textOnly: true});

/** @type {import('screenshot-studio').Tour} */
export default {
  app: 'com.bernaferrari.bravedns.storecapture',
  activity: 'com.bernaferrari.bravedns.ui.activity.LauncherAliasAppLock',
  animations: false,  // the Home beacon animates forever; still screens read reliably
  animated: ['01-home', '11-home-dark'],  // …and these let it move for their frame
  settle: 1500,
  // The screens show the real Google apps of the Play image; stub apps from earlier runs would mix in.
  setup(d) {
    for (const stub of JSON.parse(fs.readFileSync(new URL('fixture-apps.json', import.meta.url), 'utf8')).apps || []) {
      const id = stub.package || stub.packageName;
      if (id && d.shell('pm', 'list', 'packages', id).includes(id)) d.adb('uninstall', id);
    }
  },
  async start(d) {
    d.setClock('09:41');  // the app seeds its week of traffic up to now
    await d.launch({clear: true, wait: 0});
    // The first start lists the apps and seeds their rules and traffic.
    for (let i = 0; !d.appFiles().includes('storecapture-seeded'); i++) {
      if (i === 60) throw new Error('The capture build did not seed its fixtures');
      await d.sleep(2000);
    }
    // Start again on the seeded data, so every screen reads it from the beginning.
    for (let i = 0; i < 3; i++) { await d.launch({wait: 5000}); if (d.topActivity().includes(d.app)) return; }
    throw new Error('The capture build did not come to the foreground');
  },
  scenes: {
    light: {
      '01-home': null,
      // Stats is the Insights view of the Activity tab (the Log · Insights header is its title).
      async '05-stats'(d) { await tab(d, 'Activity'); await tap(d, 'Insights'); await tap(d, '24h'); },
      '06-blocked-apps': d => d.scrollTo('Most blocked apps', .12),
      // The log is the Activity tab's other view, on All: connections and DNS lookups as one timeline.
      '02-logs': d => tap(d, 'Log'),
      async '04-dns-blocked'(d) {
        await tap(d, 'Filters', {above: .3});
        await tap(d, 'DNS lookups');
        await tap(d, 'Close sheet', {wait: 1800});  // the scrim: the "DNS lookups" chip now shows under the controls
        await tap(d, 'Blocked', {above: .35});
      },
      '03-apps': d => tab(d, 'Apps'),
      async '07-app-detail'(d) { await d.waitFor('Clock'); await tap(d, 'Clock', {wait: 7000}); },
      async '08-settings'(d) { await d.back(1800); await tab(d, 'Settings'); },
      '09-dns': d => tap(d, 'DNS', {wait: 2500}),
    },
    dark: {
      async '11-home-dark'(d) {},
      '12-apps-dark': d => tab(d, 'Apps'),
      '13-settings-dark': d => tab(d, 'Settings'),
    },
  },
  profiles: {
    // README only: a 7" tablet in landscape (960dp, list beside details) shows the log at a readable size.
    slate7: {light: {async '02-logs'(d) { await tab(d, 'Activity'); await tap(d, 'Log'); }}},
  },
};
