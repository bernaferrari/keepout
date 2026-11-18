#!/usr/bin/env python3
"""Build and sign manual release artifacts without publishing or exposing key material."""
from __future__ import annotations
import base64
import hashlib
import os
from pathlib import Path
import re
import subprocess
import sys
import tempfile

ROOT = Path(__file__).resolve().parents[1]
SECRETS = {"RELEASE_KEYSTORE_BASE64", "RELEASE_STORE_PASSWORD", "RELEASE_KEY_PASSWORD", "RELEASE_KEY_ALIAS"}


def tool(name: str) -> Path:
    sdk = Path(os.environ.get("ANDROID_HOME") or os.environ.get("ANDROID_SDK_ROOT") or "")
    options = list(sdk.glob(f"build-tools/*/{name}"))
    if not options:
        raise ValueError(f"Missing Android SDK tool: {name}")
    return max(options, key=lambda p: tuple(int(x) for x in re.findall(r"\d+", p.parent.name)))


def main() -> None:
    channel = os.environ.get("RELEASE_CHANNEL", "fdroid")
    version = os.environ.get("VERSION_CODE", "")
    certificate = os.environ.get("EXPECTED_CERT_SHA256", "").replace(":", "").lower()
    if channel not in {"fdroid", "play", "website"}:
        raise ValueError("Invalid release channel")
    if not re.fullmatch(r"[1-9][0-9]{0,6}", version):
        raise ValueError("VERSION_CODE must be between 1 and 9,999,999")
    if not re.fullmatch(r"[a-f0-9]{64}", certificate):
        raise ValueError("Expected certificate must be a SHA-256 digest")
    if any(not os.environ.get(name) for name in SECRETS):
        raise ValueError("All four release signing secrets must be provided")
    build_environment = {key: value for key, value in os.environ.items() if key not in SECRETS}
    with tempfile.TemporaryDirectory(prefix="keepout-signing-", dir=os.environ.get("RUNNER_TEMP")) as temporary:
        workspace = Path(temporary)
        keystore = workspace / "release.keystore"
        try:
            keystore.write_bytes(base64.b64decode("".join(os.environ["RELEASE_KEYSTORE_BASE64"].split()), validate=True))
        except ValueError:
            raise ValueError("Release keystore is not valid base64") from None
        keystore.chmod(0o600)
        certificate_result = subprocess.run([
            "keytool", "-exportcert", "-alias", os.environ["RELEASE_KEY_ALIAS"], "-keystore", str(keystore),
            "-storepass:env", "RELEASE_STORE_PASSWORD",
        ], capture_output=True)
        if certificate_result.returncode or hashlib.sha256(certificate_result.stdout).hexdigest() != certificate:
            raise ValueError("Keystore certificate does not match the expected signer")
        build_and_sign(channel, version, certificate, workspace, keystore, build_environment)
    print(f"Signed artifacts verified at app/build/outputs/signed-release/{channel}; nothing was published")


def build_and_sign(channel, version, certificate, workspace, keystore, build_environment):
    tasks = [f":app:assemble{channel.title()}Release"]
    if channel == "play":
        tasks.append(":app:bundlePlayRelease")
    subprocess.run([
        str(ROOT / "gradlew"), *tasks, f"-PVERSION_CODE={version}",
        "--no-daemon", "--console=plain",
    ], cwd=ROOT, env=build_environment, check=True)
    source = ROOT / f"app/build/outputs/apk/{channel}/release"
    output = ROOT / f"app/build/outputs/signed-release/{channel}"
    output.mkdir(parents=True, exist_ok=True)
    if any(artifact.suffix in {".apk", ".aab"} for artifact in output.iterdir()):
        raise ValueError("Signed output directory already contains release artifacts; use a clean checkout")
    for apk in sorted(source.glob("*-unsigned.apk")):
        aligned = workspace / apk.name
        subprocess.run([str(tool("zipalign")), "-P", "16", "-f", "4", str(apk), str(aligned)], check=True)
        signed = output / apk.name.replace("-unsigned", "")
        result = subprocess.run([
            str(tool("apksigner")), "sign", "--ks", str(keystore), "--ks-key-alias", os.environ["RELEASE_KEY_ALIAS"],
            "--ks-pass", "env:RELEASE_STORE_PASSWORD", "--key-pass", "env:RELEASE_KEY_PASSWORD",
            "--out", str(signed), str(aligned),
        ], capture_output=True)
        if result.returncode:
            raise ValueError("APK signing failed; check the supplied signing credentials")
    subprocess.run([
        sys.executable, str(ROOT / "scripts/verify-release-apks.py"), str(output),
        "--version-code", version, "--expected-cert", certificate,
    ], env=build_environment, check=True)


    if channel == "play":
        source_bundle = ROOT / "app/build/outputs/bundle/playRelease/app-play-release.aab"
        sign_play_bundle(source_bundle, output / "app-play-release.aab", keystore, certificate)


def sign_play_bundle(source: Path, output: Path, keystore: Path, certificate: str) -> None:
    if not source.is_file():
        raise ValueError("Play release bundle is missing")
    result = subprocess.run([
        "jarsigner", "-keystore", str(keystore),
        "-storepass:env", "RELEASE_STORE_PASSWORD", "-keypass:env", "RELEASE_KEY_PASSWORD",
        "-signedjar", str(output), str(source), os.environ["RELEASE_KEY_ALIAS"],
    ], capture_output=True)
    if result.returncode:
        raise ValueError("Bundle signing failed; check the supplied signing credentials")
    try:
        verify_play_bundle(output, certificate)
    except (ValueError, subprocess.CalledProcessError):
        output.unlink()
        raise


def verify_play_bundle(bundle: Path, certificate: str) -> None:
    verified = subprocess.check_output([
        "jarsigner", "-J-Duser.language=en", "-J-Duser.country=US", "-verify", str(bundle),
    ], text=True)
    if "jar verified." not in verified or "unsigned entries" in verified:
        raise ValueError("Play release bundle does not have a valid signature")
    signers = subprocess.check_output([
        "keytool", "-J-Duser.language=en", "-J-Duser.country=US", "-printcert", "-jarfile", str(bundle),
    ], text=True)
    fingerprints = re.findall(r"SHA256:\s*([0-9A-F:]+)", signers)
    if not fingerprints or fingerprints[0].replace(":", "").lower() != certificate:
        raise ValueError("Play release bundle signing certificate does not match the supplied certificate")


if __name__ == "__main__":
    main()
