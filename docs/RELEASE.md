# Release verification and manual signing

The security workflow builds a minified F-Droid release without signing secrets. It checks the
application ID, version code, launcher alias, VPN service permission, native ABI contents,
and 16 KB native-library alignment. CodeQL runs against the de-Googled Android build. Dependency
review rejects high/critical advisories; its resolved Gradle graph is submitted to GitHub only from
trusted pushes to `main`, with write permission confined to that job. No APK is sent to an external
scanning service.

Before running trusted dependency submission in a fresh fork, enable **Settings → Advanced Security
→ Dependency graph** for that repository. Otherwise the graph build can pass while submission is
rejected because the feature is disabled. See [GitHub's dependency graph setup](https://docs.github.com/en/code-security/how-tos/secure-your-supply-chain/secure-your-dependencies/enable-dependency-graph).

Local unsigned verification requires JDK 17 and an Android SDK with Build Tools. Export
`ANDROID_HOME` (or `ANDROID_SDK_ROOT`) to that SDK directory for the Python artifact tools.

```sh
./gradlew :app:assembleFdroidRelease
python3 scripts/verify-release-apks.py app/build/outputs/apk/fdroid/release --version-code 1
python3 -m unittest discover -s scripts/tests -v
```

Every release produces four ABI APKs (`armeabi-v7a`, `arm64-v8a`, `x86`, `x86_64`) and a universal
APK. Play also produces a signed Android App Bundle (`.aab`) for Play Console. All channels and ABIs
use the same version code. The default is `1`. `VERSION_CODE` can override it with a positive integer
below 10,000,000. Increase the version code for each published update.

The **Manual signed release artifacts** workflow is dispatch-only and accepts only the `main` ref.
Choose a channel, a version code, and the expected signing certificate SHA-256 digest.
Provide these repository secrets at runtime:

- `RELEASE_KEYSTORE_BASE64`: base64 encoding of the release keystore.
- `RELEASE_KEY_ALIAS`: alias in that keystore.
- `RELEASE_STORE_PASSWORD` and `RELEASE_KEY_PASSWORD`: keystore and private-key passwords.

Signing material is written to a private temporary directory and removed in `finally`. Passwords
are passed by environment-variable name, never in command arguments or log output. Gradle's build
environment excludes signing secrets. The supplied certificate is checked before building and again
on every signed APK and the Play bundle. Signed artifacts and R8 mappings are uploaded as workflow artifacts for review;
the workflow never publishes a GitHub release, deploys a site, or uploads to an app store.

For local signing, supply the same environment variables plus `RELEASE_CHANNEL`, `VERSION_CODE`, and
`EXPECTED_CERT_SHA256`, then run `python3 scripts/build-signed-release.py`. Use a clean generated
`app/build/outputs/signed-release/<channel>` directory; the script rejects stale APKs and bundles rather than
mixing releases. Never commit a keystore, its base64 encoding, or passwords.

# Translation maintenance

`python3 scripts/refresh-upstream-translations.py` previews changes against a locally fetched
`upstream/main`; add `--apply` to write them. Only resource keys with unchanged English contracts are
eligible. Non-translatable entries, wrong-brand text, unknown references, changed link targets and
incompatible format arguments are excluded. A second run should report no changed files. Translation
language quality still needs native-speaker review; matching the source contract does not certify
linguistic quality.

## Play Console data disclosures

Publish the [Keepout privacy policy](../PRIVACY.md) at the public URL used in About
before entering that URL in Play Console. Automatic crash and ANR reporting comes
from [Android vitals](https://support.google.com/googleplay/android-developer/answer/9859174). Keepout’s manual bug reports are shared at the user’s request.
For optional proxy subscriptions, review purchase history and device, account, and entitlement
identifiers sent to the subscription backend. Review resolver and proxy transport
data separately from the local Activity logs. Reports and backups users deliberately
export can contain information beyond what the app transfers automatically.

Use [Play's App content instructions](https://support.google.com/googleplay/android-developer/answer/9859455)
when completing the declarations for the production artifact.
