# Keepout UX audit: flows (2026-10-30)

Build audited: `com.bernaferrari.bravedns.storecapture` at HEAD `334e0f42f`, on emulator-5578 in the phone profile
(1080×2340 @420; the display was set back to its earlier 1200×1920 @320 afterwards). Flows the capture build
can't show (first run, VPN consent, always-on/Private DNS conflicts, pause, notifications, app lock) were
read from code. The physical phone was not touched.

Screenshots are 400 px wide. They're in `design/ux-audit/screens/` (copied from `/tmp/keepout-ux/`). `s*.png` files are contact sheets.

The visual layer is in good shape: icons, shapes, radio groups and filter pills are consistent. What keeps
Keepout from a 9/10 is flow:

1. **The most common question ("why doesn't this app work?") has no path to an answer.** Log rows can't be acted on, stats rows can't be tapped, and App detail puts the app's traffic at the very bottom.
2. **The structure follows the engine, not the user's tasks.** DNS asks for a protocol before a provider. Rules are split across three screens with icon-only toggles. Apps and Logs, which people use every day, sit inside Settings, while a whole tab goes to Stats.
3. **Each screen uses its own words for the same concept.** "Unmetered", "Data", "Isolated", "Bypassed", "RDNS", "Sky/Max", "Per-app IP and domain rules".

---

## 1. Executive summary: the 10 changes that would most raise the rating

| # | Change | Why it moves the rating |
|---|---|---|
| 1 | **Make every log row actionable.** Tapping a row opens a *Connection sheet*: one plain sentence saying why it was blocked or allowed (the `firewall_rule_*_desc` strings already exist), one primary fix ("Allow Clock on Wi‑Fi"), "Allow for 15 minutes", and Block/Trust for the domain or IP, for this app or for all apps. | Right now a log row expands to a read-only table and stops there (`screens/07b-log-detail.png`). This is the #1 journey for a firewall app. |
| 2 | **Change the navigation to Home · Apps · Activity · Settings.** Stats moves into Activity as an "Insights" view. Apps and Logs stop living inside Settings. Fix the tab highlight (see #10). | People open Apps and Logs constantly. Today each is either a below-the-fold tile on Home or a Settings entry (`s-home.png`, `05-settings.png`). Stats has a full tab, yet none of its rows can be tapped. |
| 3 | **Rebuild App detail around "Can it use the internet?".** Show a status sentence, then an **Internet access** pair of **Wi‑Fi / Mobile data** tiles, with no "Firewall" heading and no duplicate chip. "Allow for 15 minutes" becomes an inline button whenever something is blocked. Next comes **Recent activity**, blocked first, with Block/Trust on each row. The five-option mode radio collapses into a "Special handling" row. Rules and notes go at the end. | Today the screen opens with a heading, a chip and two tiles that all say "Blocked". The fix for a blocked app (temporary allow) is buried in "Advanced", and activity is three scrolls down (`s-appinfo.png`, `s4.png`). |
| 4 | **Make DNS provider-first.** One DNS screen with three choices: **Keepout DNS (blocks ads & trackers)**, **Choose a provider** (Cloudflare, Google, Quad9, AdGuard, NextDNS…, with the protocol as a detail), and **Your network's DNS**. Below that, a **Blocklists** card ("3 lists · on this device"). About 15 expert toggles move to "DNS · Advanced". | The current IA asks "DoH or DoT or DNSCrypt or ODoH?" first (`s7.png`). "Rethink DNS" shows up both as a type and as a protocol. "RDNS", "Sky/Max" and "stub resolver running on cloudflare.com" are jargon (`s6.png`). |
| 5 | **Make first run a single path to "Protected".** The Welcome CTA becomes **Turn on protection**. It shows a one-sentence explanation and goes straight to the system VPN consent, with no extra "VPN permission / Proceed" dialog. Before starting, check for always-on VPN and Private DNS conflicts and resolve them inline. Notification permission is requested later, once, with a reason. | Today: Welcome → "Finish" → Home → Start → in-app "VPN permission" dialog → system consent → the notification prompt stacks on top. The Private DNS conflict is only detected when you *open the DNS screen*. |
| 6 | **Make the Home hero honest and controllable.** Use a connected button group **[Pause ▾] [Stop]**: pause offers 15 min / 1 h / Until I resume, and today it only exists as a notification action. Add a health banner for "DNS isn't answering", "Private DNS is overriding Keepout" and "Another VPN is always-on", each with its fix. Replace the "RDNS Default" chip with a tappable "DNS: Keepout · 3 blocklists" line. | The capture shows "Protected" on Home while DNS shows "Failed: using fallback DNS" (`01-home.png` vs `s6.png`). Pause can't be found in the app. |
| 7 | **Merge rules into one screen.** **Rules** has labelled tabs **Domains / IPs**, a scope chip (All apps · Universal only · one app), search, and a FAB. The Add sheet asks for **Block / Trust** and **Applies to: All apps / Choose app…**. Universal toggles are split into **Everyday** and **Strict** groups, and Lockdown gets a confirm. | Today: four icon-only toggle buttons, a title that stays "IP & port rules" on the Domain tab, an Add sheet that always creates a *Block* rule, and a per-app view with no Add (`s8.png`, `09g-add-domain.png`). |
| 8 | **Trim the Settings IA.** Settings keeps *Protection* (DNS, Firewall rules, Proxy & VPN), *App* (Appearance, App lock, Backup & restore, Notifications, About) and *Advanced* (Network, Anti-censorship, Logging, Diagnostics). "Logs" leaves Settings because it becomes a tab. **Restore the App lock setting**, which currently has no UI at all. | Settings mixes daily tools (Apps, Logs) with expert engines (Anti-censorship, Network with ~18 toggles). App lock is an orphaned feature: nothing in the UI sets `biometricAuthType`. |
| 9 | **Use one vocabulary everywhere.** Wi‑Fi / Mobile data, Trusted only, Skips filtering, Skips universal rules, Excluded. Use the same words in App detail, the Apps filter, bulk actions, notifications and logs (exact strings in §4). | The filter says "Isolated" where App detail says "Allow only trusted". The bulk sheet says "Bypassed"/"Disabled" (`06d-rules-menu.png`). Section headers say "Most contacted ips". |
| 10 | **Fix five correctness/flow bugs that undermine trust.** (a) A loading state flashes error/empty copy ("App information missing. Perhaps this app was uninstalled?", "Search 0 apps · No apps found"). (b) The highlighted tab doesn't match the stack (App detail opened from Home highlights *Settings*). (c) Selecting a resolver moves it to the top of the list under your finger. (d) Stats rows are dead ends. (e) The per-app rules screen doesn't name the app. | Each is small, but each makes the app feel broken or untrustworthy, which hurts more in a security product. |

---

## 2. Journeys

Tap counts start from Home with the app open.

### a. First run → understand → protection on

**Current (code: `HomeScreenActivity.kt` 305, 927–1110; `WelcomeScreen.kt`; `HomeDialogComponents.kt`)**
1. Welcome: app name, a long sentence (`slide_2_desc`: "…monitor network activity, bypass Internet censorship, and firewall apps…"), three features, CTA **Finish**.
2. Home "Not active · Turn on protection to secure your traffic", then **Start**.
3. In-app dialog "VPN permission" (a 3-line paragraph plus "Keepout does not collect any information…"), then **Proceed**.
4. System VPN consent, then **OK**.
5. `getNotificationPermissionIfNeeded()` fires during `startVpnService()`, so a second system dialog appears right after the first.
6. Always-on conflict: "Disable always-on VPN… Go to settings" is a dead end, because you leave and have to find your way back.
7. Private DNS active: **not checked on start**. Only `navigateToDnsDetailIfAllowed()` warns, when you open DNS. Until then Home says "Protected" while Android's Private DNS overrides Keepout's DNS.

Total: 4 taps and 2–3 dialogs before "Protected". The user never chooses what "protected" means (block ads? which DNS?).

**Problems:** "Finish" ends onboarding before anything is set up. The Proceed dialog duplicates the system dialog's purpose. Two OS prompts arrive back to back. Conflicts are found late or not at all. There's no moment of success ("You're protected. Here's what's happening").

**Proposed flow**
- **Welcome** (one screen; keep the existing layout and expressive shape): headline "Keep unwanted traffic out", subhead "Block ads and trackers for every app, and decide which apps can use the internet." Keep two features, firewall and DNS blocking, and move routing into an "Also in Keepout" line. Primary button **Turn on protection**, secondary text button **Not now**.
- Tapping **Turn on protection** runs the pre-flight checks. If another app owns always-on VPN, show an inline sheet "Another VPN is set to always on" with the body "Android lets only one VPN run. Turn off always-on for the other VPN, then come back." and the button **Open VPN settings**. On return (`onResume`), re-check and continue automatically. Otherwise call `VpnService.prepare` → system consent directly. Put the privacy reassurance as one line under the Welcome CTA ("Keepout filters traffic on this device. Nothing is sent to us.") instead of a separate dialog.
- **Success state** on Home (first time only): the hero animates to Protected, and a dismissible card says "You're protected. Ads and trackers are blocked with Keepout DNS." with buttons **Review apps** and **Choose blocklists**.
- **Private DNS check runs on every start and on resume.** If it's active, Home shows a health banner (see journey i) instead of a silent wrong "Protected".
- **Notifications:** ask after the first successful start, on Home, with a card "Get alerts when a new app is installed or protection stops", **Allow notifications** / **No thanks**. Never ask in the same second as VPN consent.

**Gain:** from 4 taps and 3 dialogs to 2 taps and 1 dialog. Conflicts get resolved instead of discovered. First-week retention should improve because the user sees value immediately.

### b. "An app isn't working" → find out it's blocked and why → allow it

**Current**
- *From Home:* works only if the app is in the top 3 of "Stopped most" (`s-home.png`). Tap the row → App detail, which flashes "App information missing" while loading (`s-appinfo.png` left) → two tiles to toggle. Opening App detail from Home highlights the **Settings** tab (bug: `AppInfo.rootDestination()` is always Configure even when pushed on the Home stack).
- *From Logs:* Settings tab → Logs → find the row → tap → read-only details (`07b-log-detail.png`), and the flow ends there. You have to remember the app name, go back twice, then Settings → Apps → search → app → tiles: **about 9 taps plus memory**.
- *From Apps:* Settings → Apps → scroll or search → toggle the row's Wi‑Fi/data icons, or tap in for detail. The row icons' accessibility labels describe the *action* rather than the state ("Blocked: Unmetered" on an allowed app), so TalkBack reads the opposite of what's true.
- *From Stats:* "Most blocked apps" rows **are not tappable** (`RethinkSummaryStatisticsScreen.kt` 296 sets `onClick` only for countries).

**Proposed flow**
- **Connection sheet** (new shared component, used by Logs, App detail activity, Stats and DomainConnections):
  - Header: app icon + domain (or IP), "Clock · 09:41 · HTTPS".
  - Verdict card (error container when blocked, as the log pill is today): the plain sentence from `FirewallRuleset.desc`, e.g. "Blocked because this app is blocked on Wi‑Fi."
  - **Primary fix**, derived from the rule:
    - App blocked or metered/unmetered rule → **Allow Clock on Wi‑Fi** (or "on mobile data" / "everywhere"), with **Allow for 15 minutes** as the tonal secondary.
    - Domain or IP rule → **Remove this rule**.
    - Blocklist (DNS) → **Allow this domain** (creates an app-scoped Trust domain rule) plus a caption: "Blocked by Hagezi Multi Pro, 1Hosts Pro".
    - Universal rule (device locked, not in use, UDP, port 80, DNS bypass…) → **Open universal rules**, with that row highlighted (`FirewallSettings(focusKey)` already exists).
  - "This domain" / "This IP": a connected button group **Block · Trust · No rule** and a scope switch **Only Clock / All apps**.
  - Footer links: **Open Clock**, **Copy address**, **Details** (the current key/value table, collapsed by default).
- **App detail** puts "Allow for 15 minutes" next to the access tiles whenever any network is blocked (journey c).
- **Stats rows** become tappable: apps → App detail; domains and IPs → Connection sheet in aggregate mode, or `DomainConnections`.
- **Apps list**: fix the accessibility labels to state the state ("Wi‑Fi: blocked. Double-tap to allow").

**Gain:** Logs → fixed in 2 taps (row → **Allow Clock on Wi‑Fi**), down from about 9. This is the change most likely to appear in reviews ("finally a firewall that tells me *why*").

### c. Block an app entirely / only on mobile data; temporary allow

**Current (`s-appinfo.png`, `03-appinfo-tap-unmetered.png`)**
- App detail shows "Firewall", then a chip "Blocked", then tiles "Unmetered · Blocked" and "Data · Blocked". The same fact appears three times, and "Unmetered"/"Data" are jargon.
- Tapping a tile toggles it immediately with no feedback or undo. The chip changes to "Blocked on mobile data".
- "How Keepout treats this app": five radios (Normal, Allow only trusted, Bypass DNS and firewall, Bypass universal rules, Exclude from Keepout). These are expert modes, yet they're the first big block on the page.
- "Allow for 15 minutes" is a *switch* under **Advanced**, three scrolls down, next to "Bypass all proxies" and "App info".
- Apps list: two icon buttons per row, which works for experts. The bulk **Rules** sheet (`06d-rules-menu.png`) applies to *all apps currently filtered* ("16 apps"), but its rows say "Disabled", which is the state of the bulk toggle, not of your apps. Dangerous and confusing.

**Proposed (App detail, top to bottom)**
1. Large flexible top bar: icon + name, with the package as subtitle, collapsing as today.
2. **Status line** in `titleMedium`, colored: "Can't use the internet" / "Blocked on mobile data" / "Blocked on Wi‑Fi" / "Can use the internet" / "Allowed for 12 more minutes" (live countdown). No chip.
3. **Internet access** section: two expressive tiles, **Wi‑Fi** and **Mobile data**, each showing Allowed/Blocked, toggled on tap with a haptic and a snackbar ("Clock blocked on Wi‑Fi · Undo"). When either is blocked, a tonal button **Allow for 15 minutes** appears under the tiles; while active it reads **Stop temporary access · 12 min left**. Footnote in `bodySmall`: "Metered Wi‑Fi counts as mobile data." (semantics; see Risks).
4. **Recent activity**: blocked connections first (the last 24 h), each row opening the Connection sheet, then "See all activity" → Activity tab filtered to this app (`NetworkLogs(query=…)` already takes a query). This replaces the three cards "Top active connections / Most contacted domains / Most contacted ips".
5. **Rules for this app**: "Domain rules · 2", "IP rules · 0". The per-app rules screen title becomes "Clock · rules".
6. **Special handling** row showing the current value ("Normal"). It opens a sheet with the five modes as the existing radio group (keep the copy from commit `5e3dadfe8`) and a warning line on Exclude and Skip filtering.
7. **More**: Bypass all proxies (only when a proxy is configured), Notes, Android app info.

**Apps list:** keep the per-row toggles. Replace the "Rules" bulk sheet with **Select** mode: long-press or a top-bar "Select" → checkboxes → a bottom action bar **Block Wi‑Fi · Block mobile data · Allow all · More**, with a confirm sheet "Block Wi‑Fi for 6 apps?". Only selected apps are affected, never "whatever the filter currently shows". Rename "Sort apps" to a sort icon with a menu, and put Refresh in the overflow.

**Gain:** blocking or allowing becomes a 1-tap, self-explanatory action, and temporary allow goes from 4 scrolls + a switch to 1 tap. Bulk actions stop being a foot-gun.

### d. See what got blocked today and why; act on a log row

**Current (`s3.png`, `07c-logs-dns.png`)**
- Logs sit in Settings → System → Logs. The top bar has a connected group "Network | [DNS icon]" (the DNS half is icon-only). The filter row has an "All" pill plus icon-only Allowed, Blocked, Rules and Apps.
- Rows look good (verdict pill, reason, protocol, size), but tapping only expands details. Blocked DNS rows show the blocklist names, which is good. The DNS row tap appeared to do nothing on the capture build. Verify.
- Home's "Last 24 hours" counts (454/122) and the Stats tab (which defaults to **1h**, `SummaryStatisticsViewModel` fallback `ONE_HOUR`) disagree at first glance.

**Proposed: Activity tab**
- Top: a connected button group **Log · Insights** (Insights is today's Stats content). Keep the time-range group 1h/24h/7d, defaulting to **24h** to match Home.
- **Log** merges network and DNS into one stream by default: one row per connection, with the DNS answer folded in as "Looked up via Keepout DNS". A filter chip **Type: All / Connections / Lookups** replaces the top-bar toggle. Filters get text labels when space allows (they already grow on selection): **All · Blocked · Allowed · App ▾**. "Rules" becomes **Reason ▾**.
- Each row opens the Connection sheet (journey b).
- An empty Blocked filter shows "Nothing blocked in the last 24 hours" instead of a generic empty state.
- Home's "Stopped most" rows and "Statistics ›" link go to Activity → Insights.

**Gain:** one place answers "what happened and why", with a fix one tap away.

### e. Choose DNS, blocklists; understand what's active

**Current (`s5.png`, `s6.png`, `s7.png`, `08j-rdns-edit.png`)**
- DNS screen: hero "In use · RDNS Default · 33 ms" (good), then **Type** as four cards: System DNS / Other DNS / Rethink DNS / Smart DNS. The descriptions are vague ("Smart DNS picks from a rotating set of resolvers").
- *Rethink DNS* → "⛅ Sky | 🪂 Max" plus "280+ locations; higher uptime; stub resolver running on cloudflare.com" plus "RDNS Default / Adult / Piracy / Social Media / Security / Privacy / Plus". Each row has a "…" that opens an info dialog with the URL and a "Copy" button. **Selecting a row moves it to the top of the list**, so the row you tapped jumps away. "RDNS Plus" has a pencil; Edit opens "Download blocklists (around 60MB) to use this feature" with a progress bar and only **Cancel**.
- *Other DNS* → "DNS servers" with six protocol cards (DoH, DoT, DNSCrypt, DNS Proxy, ODoH, **Rethink DNS** again, marked "Failing") → a provider list per protocol.
- Then about 20 settings on the same scroll: On-device blocklists (Disabled), in-app downloader, prompt on updates, Advanced DNS filtering (experimental), Split DNS (experimental), Allowed record types, trusted-domain resolution (3 radios + endpoint), captive portal, website icons, DNS booster, Never proxy DNS…
- "Fallback DNS" lives in **Network › Advanced**, separate from DNS.
- Home and DNS disagree on health (Protected vs "Failed: using fallback DNS").

**Proposed: DNS screen**
- Hero (keep): "**Keepout DNS** · Blocking ads, trackers & malware · 33 ms". When failing: error container "Keepout DNS isn't answering. Using your network's DNS for now." and a **Try another server** button.
- **Who answers your lookups** (radio cards, 3 options):
  1. **Keepout DNS**: "Encrypted. Blocks ads, trackers and malware." (today's Rethink DNS). Below it, inline: **Blocklists: Default** ›, which opens the existing list-picker screen. The presets become *Default, Security, Privacy, Family (adult content), Social media, Piracy* and *Custom…* (today's "Plus"). The **Sky/Max** choice moves to "DNS · Advanced › Keepout DNS network" as "Fastest (global edge)" / "Most private (fewer logs)". Verify the exact privacy properties with the Rethink docs before shipping that copy.
  2. **Another provider**: "Cloudflare, Google, Quad9, AdGuard, NextDNS or your own". Opens a **provider-first** list grouped by provider, protocol shown as a pill on each row (DoH/DoT/DNSCrypt/ODoH), plus **Add custom server** (FAB) where the protocol is chosen in the form. "Rethink DNS" disappears from this list.
  3. **Your network's DNS**: "Use whatever Wi‑Fi or mobile network provides. Not encrypted."
  - **Smart DNS** moves into Advanced as "Automatic server selection", or gets dropped if usage is low. As a top-level card its value can't be explained.
- **Blocking on this device** card: "On-device blocklists · Off" with **Set up** (download size stated on the button: "Download lists (≈60 MB)"). It's explicit that this works with *any* provider. The downloader and update prompts move into that screen's overflow.
- **DNS · Advanced** (one row → sub-screen): filtering experiments, record types, trusted-domain resolution, captive portal, website icons, DNS booster, never proxy DNS, *fallback DNS (moved from Network)*.
- Selection never reorders a list while it's on screen. Sort the "in use" item first only on the next entry.
- Remote blocklist editing must not demand a 60 MB download. If it really needs the file-tag metadata, say "Getting the list of blocklists (2 MB)…" and show progress with a retry, not just Cancel. Investigate `ConfigureRethinkBasicScreen.kt`; the copy may come from the on-device path.

**Gain:** someone who knows "I want Cloudflare" or "I want ad blocking" picks in 2 taps without learning protocol names. What's active shows on Home and in the hero.

### f. Firewall rules: universal, IP & domain (per-app and universal)

**Current (`09-firewall.png`, `s8.png`, `09g-add-domain.png`)**
- Firewall hub: Universal firewall rules · IP & port rules ("Block or trust domains, IPs, and ports") · Per-app IP and domain rules. The last two open the *same* screen with different preselected icon toggles.
- Rules screen: two connected groups of icon-only buttons (globe/grid = universal/per app; 123/link = IP/domain). The title stays "IP & port rules" on the domain tab and in per-app mode.
- **Add** sheet: a single text field. Every rule is created as **Block** (`CustomRulesScreen.kt` 112, 129); to trust you add it and then edit it. Per-app mode with nothing selected has no Add.
- Universal: 9 unlabelled-risk toggles in one list. "Block all except bypassed apps and IPs" (a lockdown that can cut off the device) sits next to "Block port 80". "Block on metered (mobile) network" duplicates per-app Mobile data, applied globally, which is confusing.

**Proposed**
- **Firewall** (Settings → Protection → Firewall) becomes a single screen with three sections:
  - **Everyday**: Block new apps until I allow them · Block apps when the screen is locked · Block apps I'm not using. Each has a one-line explanation.
  - **Strict**: Block UDP except DNS and NTP · Block insecure HTTP (port 80) · Block when the app is unknown · Block apps that skip Keepout DNS · Block all apps on mobile data.
  - **Lockdown**: "Block everything except apps and IPs you've allowed", behind a confirm sheet stating the consequence and how to undo it.
  - Then **Rules · 12** › (one entry).
- **Rules screen:** a large top bar titled "Rules" (or "Clock · rules" when scoped). Tabs as a labelled connected group **Domains · IPs** with counts. A scope **filter chip**: "All rules ▾" → All / Every app (universal) / one app (picker). Search, rows as today, and a FAB **Add rule**.
- **Add rule sheet:** field ("Domain, e.g. ads.example.com or *.example.com" / "IP or range, e.g. 1.2.3.4 or 1.2.3.0/24, optional :port"), **Action: Block · Trust** (connected group, Block preselected), and **Applies to: All apps · Choose app…** (prefilled when scoped). The confirm label follows the action: "Block domain" / "Trust domain".
- Import stays in the overflow, with the preview dialog as today.

**Gain:** one mental model ("a rule is a target + an action + a scope"), no hidden icon meanings, and Trust becomes a one-step action.

### g. Proxy / WireGuard / Orbot

**Current (`10-proxy.png`, `10c-wg.png`, `10d-socks.png`)**: "Proxy servers: Set up WireGuard / Set up SOCKS5 proxy (switch) / Set up HTTP(S) CONNECT proxy (switch)", then "Tor: Orbot (switch) · Apps · Open Orbot · Info". The rows say "Set up" but are switches with no stated configured state. WireGuard opens an empty list with a FAB. The SOCKS sheet is dense but clear.

**Proposed:** rename the section **Proxy & VPN**. Each row states its state: "WireGuard · 2 tunnels, 1 on", "SOCKS5 · Off · Not set up", "HTTP proxy · Off", "Tor via Orbot · Off". Tap opens the setup or detail; the switch appears only once configured. Before the first config, the WireGuard empty state offers **Import file · Scan QR · Create** (the actions exist in `onWgImportClick`/`onWgQrScanClick`) instead of a lone FAB. "Info" becomes an inline sentence: "Routes chosen apps through Tor. Requires Orbot." Gain: no ambiguous switches, and fewer steps to the first tunnel.

### h. Settings hub

**Current (`05-settings.png`)**: Protection (Apps, DNS, Firewall, Proxy) · System (Network, General, Logs, Anti-censorship, Advanced[debug]) · Appearance (theme + 11 color swatches inline). General mixes Backup, logging, auto-start, uninstalled-app memory, the bubble, provider info, log retention, packet capture, "Website: https://www.rethinkdns.com/" and About. Network is about 18 expert toggles. "Logs" in Settings opens the log *viewer*, while log *settings* live in General.

**Proposed IA**
- **Protection:** DNS · Firewall (universal + rules) · Proxy & VPN.
- **App:** Appearance (row → screen; the swatch grid doesn't belong on the hub) · App lock · Notifications · Backup & restore · Start on boot.
- **Advanced:** Network · Anti-censorship · Logging (on-device logging, retention, provider info, packet capture, the bubble) · Diagnostics (event logs, console logs, ping test, DB inspector, flight recorder, today scattered in About) · Experimental (debug builds only).
- **About Keepout** as the last row. Drop the "Website: rethinkdns.com" row from General (it's in About, and the brand mismatch confuses).
- Keep the search; it's good. Add synonyms ("wifi", "mobile", "pause", "lock").

### i. Pause/stop, app lock, backup/restore, about

- **Pause** only exists as a notification action (`VpnNotificationManager.kt` PAUSE_STOP). In-app, the only control is Stop. Proposed: a hero connected group **[Pause ▾] [Stop]**. The Pause menu offers 15 minutes, 1 hour and Until I resume, and the existing `PauseScreen` countdown becomes a hero state ("Paused · 14:32 left · Resume"), not a full-screen takeover.
- **Stop** with always-on enabled shows "Always-on VPN… Are you sure you want to stop Keepout anyway?". Keep it, but rename the buttons **Stop anyway** / **Open VPN settings** / **Cancel**.
- **App lock:** `AppLockScreen` and the launcher alias exist, but **no UI sets `persistentState.biometricAuthType`** (the setter disappeared in `e9fab90da`). Add Settings → App → **App lock** with the radio group *Off · Every time · After 5 minutes · After 15 minutes* (match `BioMetricType`). Explain that notification actions are hidden while locked (`VpnNotificationManager` already does this).
- **Backup & restore:** OK as a sheet. Move it to Settings → App. After a restore, show "Restored 214 rules and settings · Restart protection to apply" with a **Restart** button.
- **Health banners** on Home (new, one at a time, in priority order): Another VPN is always-on · Private DNS overrides Keepout DNS ("Turn off Private DNS in Android settings so Keepout can block ads." **Open settings**) · DNS failing · Notifications blocked (only if the user enabled alerts).

---

## 3. Trim list

| Item | Action | Justification |
|---|---|---|
| **Stats tab** | Merge into Activity ("Insights") | Its rows were dead ends, and it duplicates Home's 24h summary. The tab slot is worth more as Apps. |
| **Home tiles Apps / Logs / DNS** | Remove (they become tabs or a hero line) | Below the fold. They duplicate navigation. |
| **Logs entry in Settings** | Remove | It's a tab now. |
| **"Firewall" heading + status chip on App detail** | Remove | Replaced by the single status line. |
| **Mode radio group on App detail** | Move behind "Special handling" | Expert; five options at the top intimidate. |
| **"Top active connections / Most contacted domains / Most contacted ips" cards** | Merge into "Recent activity" | Three near-identical cards. Blocked-first is what users need. |
| **In-app "VPN permission / Proceed" dialog** | Remove | Duplicates the system consent. Reassurance moves under the CTA. |
| **"Firewall › IP & port rules" and "Firewall › Per-app IP and domain rules"** | Merge into one "Rules" | Same screen reached two ways with different names. |
| **"Rethink DNS" in the protocol grid** | Remove | Already a top-level choice. |
| **Smart DNS card** | Move to DNS › Advanced (or drop) | Can't be explained in a line. Few users need it. |
| **Sky / Max** | Move to DNS › Advanced, renamed | Jargon with emoji. |
| **~15 DNS toggles** | Move to DNS › Advanced | Keep power, cut scroll by about 70%. |
| **Fallback DNS (in Network)** | Move to DNS › Advanced | It's a DNS setting. |
| **Anti-censorship (top-level card)** | Move to Advanced | Niche; packet-level jargon. |
| **Apps bulk "Rules" sheet** | Replace with Select mode | Applies to "whatever is filtered". "Disabled" labels mislead. |
| **"Sort apps" text button** | Sort icon + menu | It's the widest thing in the top bar, for a rare action. |
| **Appearance swatch grid on the Settings hub** | Move to an Appearance screen | The hub should be navigation, not a form. |
| **"Website: rethinkdns.com" row in General** | Remove (About has links) | Brand confusion. |
| **Advanced ("Random panic", "Set auto dial")** | Keep debug-only, rename **Developer** | Already gated by `isDebug`; the name should say so. |

---

## 4. Copy changes (exact strings)

Vocabulary to lock in `values/strings.xml`, edited in place so translations keep their keys:

| Key | Current | New |
|---|---|---|
| `ada_app_unmetered` | Unmetered | Wi‑Fi |
| `lbl_mobile_data` | Data | Mobile data |
| `ada_app_isolate` | Allow only trusted | Trusted only |
| (isolate desc) | Blocks everything except the domains and IPs you trust | Only reaches domains and IPs you trust |
| `ada_app_bypass_dns_firewall` | Bypass DNS and firewall | Skip all filtering |
| (desc) | Skips DNS blocking and firewall rules | No DNS blocking or firewall rules. Still logged. |
| `ada_app_bypass_univ` | Bypass universal rules | Skip universal rules |
| `ada_app_exclude` | Exclude from Keepout | Exclude from Keepout (unchanged) |
| (exclude desc) | Its traffic goes around Keepout entirely | Goes around Keepout. Not filtered or logged. |
| Apps filter chips | Bypassed · Excluded · Isolated · Blocked: Wi‑Fi (unmetered) · Blocked: Data | Skips filtering · Skips universal rules · Excluded · Trusted only · Blocked on Wi‑Fi · Blocked on mobile data |
| Apps filter "View" | Installed · System · All | Your apps · System apps · All |
| Apps filter "Categories · 0" | Categories · 0 | Category (hide the section when there is only "Other"/"System apps") |
| `temp_allow_label` | Allow for 15 minutes | Allow for 15 minutes (now a button) |
| `temp_allow_desc` | Temporarily allow this app. Auto-reverts after 15 minutes. | (delete; the button and countdown say it) |
| new | — | Allowed for %1$d more minutes |
| new | — | Stop temporary access |
| `ada_noapp_dialog_message` | App information missing. Perhaps this app was uninstalled? | This app is no longer installed (shown only after loading finishes) |
| App info section headers | Most contacted domains / Most contacted ips / Top active connections | Recent activity |
| `app_ip_domain_rules` | Per-app IP and domain rules | Rules (or "%1$s · rules" when scoped) |
| IP rules hub row | IP & port rules · Block or trust domains, IPs, and ports. | Rules · Block or trust domains and IPs, for one app or all |
| `custom_rules_add_domain_title` | Add domain rule | Add rule |
| Add rule field (domain) | Domain | Domain · e.g. ads.example.com or *.example.com |
| Add rule confirm | Add | Block domain / Trust domain / Block IP / Trust IP |
| `univ_firewall_heading` | Universal firewall rules | Firewall |
| universal intro | Firewall all apps based on device events, like a locked screen. | Rules that apply to every app. |
| "Block all except bypassed apps and IPs" | — | Lockdown: block everything except apps and IPs you've allowed |
| "Block when DNS is bypassed" | — | Block apps that skip Keepout DNS |
| "Block any app not in use" | — | Block apps I'm not using |
| "Block on metered (mobile) network" | — | Block all apps on mobile data |
| DNS type "System DNS · Use the DNS server configured by the system." | | Your network's DNS · Whatever Wi‑Fi or mobile provides. Not encrypted. |
| "Other DNS · Use a user-selected DNS server." | | Another provider · Cloudflare, Google, Quad9, AdGuard or your own |
| "Rethink DNS · Use Rethink DNS with in-app blocklists and policies." | | Keepout DNS · Encrypted. Blocks ads, trackers and malware. |
| "RDNS Default / Adult / Piracy / Social Media / Security / Privacy / Plus" | | Default / Family / Piracy / Social media / Security / Privacy / Custom |
| "Failed: using fallback DNS" | | Not answering · using your network's DNS |
| "DNS servers · Choose how domain names are resolved" | | Choose a provider |
| "Download blocklists (around 60MB) to use this feature." | | Getting the list of blocklists… (with the real size, plus Retry) |
| Home chip "RDNS Default" | | DNS: Keepout · Default |
| `hsf_vpn_dialog_*` | VPN permission / … / Proceed | (removed; under the Welcome CTA:) Keepout filters traffic on this device. Nothing is sent to us. |
| `slide_2_desc` | Keepout is the easiest way to monitor network activity, bypass Internet censorship, and firewall apps on your Android device. | Block ads and trackers for every app, and decide which apps can use the internet. |
| `finish` (Welcome CTA) | Finish | Turn on protection |
| `always_on_dialog_heading` | Disable always-on VPN | Another VPN is set to always on |
| `always_on_dialog` | Another app is set as the always-on VPN. To start Keepout, turn off always-on VPN in Android settings. | Android lets only one VPN run. Turn off always-on for the other VPN, then come back. |
| `always_on_dialog_positive_btn` | Go to settings | Open VPN settings |
| `always_on_dialog_positive` (stop) | Stop | Stop anyway |
| `private_dns_dialog_heading` | Turn off private DNS | Private DNS is overriding Keepout |
| `private_dns_dialog_desc` | Keepout's DNS settings are overridden by Android's Private DNS.\n\nTo use Keepout's DNS go to Android Settings and turn off "Private DNS". | Android's Private DNS is on, so Keepout can't block ads and trackers. Turn it off in Android settings. |
| Home health: DNS failing | Keepout is trying to restore your connection | Keepout DNS isn't answering. Using your network's DNS for now. |
| Pause | — | Pause · 15 minutes · 1 hour · Until I resume · Paused · %1$s left · Resume |
| Apps row accessibility labels | Blocked: Unmetered (on an allowed app) | Wi‑Fi: allowed / Wi‑Fi: blocked (state), with action label "Block on Wi‑Fi" / "Allow on Wi‑Fi" |
| Log filter "Rules" | Rules | Reason |
| Stats/section headers | Most blocked ips · Most contacted ips | Most blocked IPs · Most contacted IPs |
| General "Show provider information" desc | …powered by IPinfo Inc.. | …powered by IPinfo Inc. |
| Proxy rows | Set up WireGuard / Set up SOCKS5 proxy / Set up HTTP(S) CONNECT proxy | WireGuard / SOCKS5 proxy / HTTP proxy (+ state line) |
| Settings section | System | Advanced |

---

## 5. Implementation workstreams

**File-ownership rule:** each file below has exactly one owner. New strings go in a per-workstream file (`app/src/main/res/values/strings_<ws>.xml`). Only WS0 edits existing values in `strings.xml`. Only WS1 edits `HomeNavigation.kt` and the route/navigation files. Other streams expose new callbacks with no-op defaults, and WS1 wires them, or the stream sends a one-line follow-up PR to WS1.

### WS0: Foundation (must land first; small)
- **Vocabulary pass**: all §4 renames of *existing* keys in `app/src/main/res/values/strings.xml` (English only; translations marked for refresh).
- **Block-reason model**: `app/src/main/kotlin/com/bernaferrari/bravedns/ui/compose/logs/BlockReason.kt` (new) maps `FirewallRuleset` (+ DNS blocklist hit) to `{sentence, primaryFix, secondaryFix}`, as a pure function with unit tests.
- **Connection sheet**: `shared/src/commonMain/kotlin/com/bernaferrari/bravedns/ui/compose/logs/RethinkConnectionSheet.kt` (new; shared, stateless: header, verdict card, primary/secondary buttons, Block·Trust·No rule group, scope switch, details) and `app/src/main/kotlin/com/bernaferrari/bravedns/ui/compose/logs/ConnectionSheetHost.kt` (new; Android adapter calling `FirewallManager`, `DomainRulesManager`, `IpRulesManager`).
- **Rule editor sheet**: extend `shared/.../firewall/RethinkRuleSheetComponents.kt` with `RethinkAddRuleSheet(target, action, scope)`, used by WS4 and the Connection sheet.
- Loading-state fix in shared: `shared/.../common/RethinkEmptyState.kt` / `RethinkLoadingState` gets a documented "loading vs empty" contract (no API change).

### WS1: Navigation + Activity (logs & insights), highest impact
Files: `shared/.../navigation/RethinkRoute.kt`, `RethinkAppNavigation.kt`, `RethinkNavShell.kt`, `app/.../navigation/HomeNavigation.kt` (incl. `HomeDestination`), `app/.../logs/NetworkLogsScreen.kt`, `ConnectionTrackerRow.kt`, `app/.../dns/DnsLogRow.kt`, `shared/.../logs/RethinkLogRow.kt`, `RethinkLogDetailPane.kt`, `RethinkLogFilterComponents.kt`, `RethinkLogsList.kt`, `app/.../statistics/SummaryStatisticsScreen.kt`, `shared/.../statistics/RethinkSummaryStatisticsScreen.kt`, `RethinkDetailedStatisticsScreen.kt`, `app/.../logs/DomainConnectionsScreen.kt`, `DomainConnectionsRow.kt`, `app/.../viewmodel/SummaryStatisticsViewModel` (default 24h only).
- Tabs become Home · Apps · Activity · Settings. `RethinkRootDestination` gets `Apps` and `Activity`. Make `rootDestination()` reflect the stack a route was pushed on, so App detail from Home highlights Home.
- Activity: Log | Insights group; merged log stream with a Type chip; labelled filters; row → Connection sheet; stats rows tappable.
- Wire the new callbacks from WS2–WS5.

### WS2: App detail + Apps list
Files: `shared/.../apps/RethinkAppInfoScreen.kt`, `app/.../app/AppInfoScreen.kt`, `app/.../apps/AppListScreen.kt`, `shared/.../firewall/RethinkFirewallAppListScreen.kt`, `FirewallAppGrouping.kt`, `RethinkAppSortMenu.kt`, `app/.../firewall/AppIpRulesDialog.kt`, `AppDomainRulesDialog.kt`, `app/.../logs/AppWiseIpLogsScreen.kt`, `AppWiseDomainLogsScreen.kt`, `AppWiseLogsShared.kt`, `shared/.../logs/RethinkAppWiseLogs.kt`.
- Status line, Internet access tiles (Wi‑Fi / Mobile data) with snackbar undo, inline temporary allow + countdown, Recent activity (blocked first) via the Connection sheet, Rules section, Special handling sheet, More.
- Loading state (no "missing" flash; same for "Search 0 apps").
- Apps list: accessibility state labels, Select mode replacing the bulk sheet, sort icon.

### WS3: Home, first run, protection lifecycle
Files: `shared/.../home/RethinkHomeScreen.kt`, `ProtectionFlowCanvas.kt`, `WelcomeScreenShared.kt`, `PauseScreenShared.kt`, `app/.../home/HomeScreen.kt`, `HomeScreenViewModel.kt`, `HomeVpnPresentation.kt`, `WelcomeScreen.kt`, `PauseScreen.kt`, `app/.../ui/HomeScreenActivity.kt` (start/stop/pause/conflict/permission functions only), `HomeDialog.kt`, `HomeDialogComponents.kt`, `app/.../service/VpnNotificationManager.kt` (copy only).
- Welcome → Turn on protection → pre-flight conflicts → system consent → success card. Deferred notification ask.
- Hero: Pause ▾ / Stop connected group, paused state with countdown, health banners (always-on, Private DNS checked on start and resume, DNS failing), DNS line replacing the chip, removal of the Apps/Logs/DNS tiles.

### WS4: DNS
Files: `app/.../dns/DnsDetailScreen.kt`, `DnsSettingsScreen.kt`, `DnsSettingsViewModel.kt`, `DnsListScreen.kt`, `ConfigureOtherDnsScreen.kt`, `ConfigureRethinkBasicScreen.kt`, `ConfigureRethinkListSection.kt`, `ConfigureRethinkBlocklistSection.kt`, `ConfigureRethinkBlocklistFeeds.kt`, `ConfigureRethinkBlocklistEffects.kt`, `DnsEndpointShared.kt`, `*EndpointRow.kt`, `BlockFreeDnsScreen.kt`, `CaptivePortalSetting.kt`, `shared/.../dns/RethinkDnsSettingsScreen.kt`, `RethinkDnsListScreen.kt`, `RethinkEndpointList.kt`, `RethinkEndpointEditors.kt`, `RethinkDnsDetailSheets.kt`, `RethinkBlocklistEditor.kt`; plus the fallback-DNS row moved out of `app/.../settings/TunnelSettingsScreen.kt` (coordinate: WS4 adds it, WS5 removes it in the same release).
- Three-choice DNS screen, provider-first list, blocklist presets renamed, Blocking-on-device card, DNS · Advanced sub-screen, no reorder-on-select, honest download copy and retry.

### WS5: Rules, Firewall, Settings IA, App lock
Files: `app/.../firewall/FirewallSettingsScreen.kt`, `UniversalFirewallSettingsScreen.kt`, `CustomRulesScreen.kt`, `RulesImportAction.kt`, `RulesImportPreviewDialog.kt`, `shared/.../firewall/RethinkFirewallSettingsScreen.kt`, `RethinkUniversalFirewallSettingsScreen.kt`, `RethinkCustomRulesScreen.kt`, `RethinkRuleEditor.kt`, `RethinkRuleScreenActions.kt`, `app/.../configure/ConfigureScreen.kt`, `SettingsSearchIndex.kt`, `shared/.../configure/RethinkConfigureScreen.kt`, `app/.../settings/MiscSettingsScreen.kt`, `shared/.../settings/RethinkMiscSettingsScreen.kt`, `AppLockScreen.kt` (+ new `AppLockSetting.kt`), `TunnelSettingsScreen.kt`, `AntiCensorshipScreen.kt`, `AdvancedSettingsScreen.kt`, `ProxySettingsScreen.kt` + `Proxy*Section.kt`, `shared/.../settings/RethinkProxySettings.kt`, `app/.../settings/AppearanceSettingsCard.kt`, `shared/.../settings/RethinkAppearanceSettingsCard.kt`, `app/.../about/AboutScreen.kt`, `shared/.../about/RethinkAboutScreen.kt`.
- Firewall screen (Everyday/Strict/Lockdown + Rules entry), unified Rules screen with labelled tabs and scope chip, Add-rule with action and scope, Settings regrouped (Protection / App / Advanced / About), App lock setting restored, Diagnostics gathered, Proxy & VPN state lines, Appearance moved to its own screen.

**Order:** WS0, then WS1, then WS2, WS3, WS4 and WS5 in parallel (WS2 and WS3 first if serialised). WS1 merges last-mile wiring for the others.

---

## 6. Risks (behaviour and semantics)

1. **Wi‑Fi / Mobile data relabel.** The firewall decides by *metered vs unmetered*, not by radio type. Metered Wi‑Fi (hotspots, user-marked) counts as "mobile data", and Ethernet counts as "Wi‑Fi". Keep the stored flags unchanged and show the footnote "Metered Wi‑Fi counts as mobile data". "Treat mobile data as metered" in Network stays.
2. **Connection-sheet quick fixes write rules.** "Allow Clock on Wi‑Fi" must change exactly the flag that blocked the connection (unmetered vs metered), not reset the app to fully allowed. "Allow this domain" must create an *app-scoped* Trust rule unless the user switches the scope. Each write shows a snackbar with Undo. Unit-test `BlockReason` for every `FirewallRuleset` value: universal rules (device locked, UDP, port 80, lockdown, Private DNS, proxy errors) must route to settings, never to a silent rule write.
3. **Temporary allow** already exists as a toggle. Moving it to a button must keep the same expiry mechanism and must not survive a restart differently. Check `FirewallManager` temp-allow persistence, and that the countdown reads from the stored expiry, not a UI timer.
4. **Bulk Select mode** must call the same `FirewallManager` batch APIs as the current bulk sheet, scoped to selected UIDs only. Shared UIDs (several packages under one UID) need a "Also affects: …" line, as the log row's "+N other apps" does.
5. **Pause in the hero** uses the existing `VpnController.pauseApp()` path. App lock hides notification actions, so the hero becomes the only pause UI when locked. Make sure the lock screen gates it.
6. **Private DNS / always-on pre-flight** reads `Utilities.isPrivateDnsActive`. Never auto-start the VPN when returning from Android settings unless the user had pressed Turn on protection in this session.
7. **DNS restructure** must not change resolver IDs or stored selections. Rename labels only. Preset renames ("RDNS Adult" → "Family") are display strings over the same stamps/URLs. Moving Smart DNS and Sky/Max must keep the current selection working for users who already chose them (show it as selected even from Advanced).
8. **Add-rule default.** Today Add means Block. The new sheet preselects Block, so typing and confirming does the same thing; nothing else changes silently.
9. **Navigation change.** Deep links and notification intents (`HomeIntentRouter.kt`, `homeNavRequest` routes such as UniversalFirewall, DnsList, Events, Database, RpnAccount, WireGuard) must land in the new tabs. Saved back-stacks serialized with `rethinkNavSavedStateConfiguration` must tolerate the new root keys (add them as subclasses; don't rename existing ones).
10. **Restored App lock**, if it's mis-set, can lock the user out. Require a successful biometric/credential check before enabling, and keep the launcher alias behaviour unchanged.
11. **Emulator note:** returning from per-app IP rules triggered an ANR ("Keepout isn't responding", `screens/03c-app-domain-rules.png`). Logcat shows `WaitForGcToComplete blocked … 295s` on a memory-starved emulator (≈240 MB free), so it's probably environmental. Profile once on a real device before shipping WS1/WS2, since both add work to App detail and Logs.

---

## 7. Screenshot index (`design/ux-audit/screens/`)

- Home: `01-home.png`, `01c-home-scrolled2.png`, `01d-home-bottom.png`, sheet `s-home.png`
- App detail: `02-home-tap-camera.png` (missing-app flash), `02b…02f` (modes, advanced, activity, domain trust sheet), sheets `s-appinfo.png`, `s4.png`; toggle `03-appinfo-tap-unmetered.png`; per-app rules `03b-app-ip-rules.png`; ANR `03c-app-domain-rules.png`
- Settings and Apps: `05-settings.png`, `06-apps.png`, `06b-apps-filter.png`, `06c-sort.png`, `06d-rules-menu.png`, sheets `s-settings-apps.png`, `s2.png`
- Logs: `07-logs.png`, `07b-log-detail.png`, `07c-logs-dns.png`, `07f-logs-apps.png`, sheet `s3.png`
- DNS: `08-dns.png` … `08d-dns-4.png` (sheet `s5.png`), Rethink list `08e/08f` (sheet `s6.png`), `08j-rdns-edit.png`, protocol grid `08k/08l` (sheet `s7.png`), `08m-doh.png`
- Firewall: `09-firewall.png`, `09b-universal.png`, `09d-ip-rules.png`, `09e-perapp-rules.png`, `09f-domain-rules.png`, `09g-add-domain.png`, sheet `s8.png`
- Proxy: `10-proxy.png`, `10c-wg.png`, `10d-socks.png`; Anti-censorship `11-anticensor.png`; Advanced `12-advanced.png`; Stats `13-stats.png`, `13b-stats-bottom.png`
