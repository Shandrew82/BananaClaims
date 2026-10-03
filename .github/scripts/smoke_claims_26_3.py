#!/usr/bin/env python3
"""Boot the built JAR twice in an isolated, loopback-only Fabric test server.

No production files or credentials are used. This is a startup/console smoke
check, not a substitute for player, permission, protection or BlueMap testing.
"""
from __future__ import annotations

import hashlib
import json
from pathlib import Path
import shutil
import subprocess
import time
import urllib.request
import zipfile

ROOT = Path(__file__).resolve().parents[2]
PROJECT = ROOT / "Banana Claims Mod"
WORK = PROJECT / "build" / "smoke-26.3"
RELEASE = PROJECT / "build" / "release-candidate"
MC = "26.3"
LOADER = "0.19.5"
API = "0.161.0+26.3"
INSTALLER = "1.1.2"


def download(url: str, destination: Path) -> None:
    request = urllib.request.Request(url, headers={"User-Agent": "BananaClaims-CI-smoke-test"})
    with urllib.request.urlopen(request, timeout=120) as response:
        with destination.open("wb") as output:
            shutil.copyfileobj(response, output)


def read_log(path: Path) -> str:
    return path.read_text(encoding="utf-8", errors="replace") if path.exists() else ""


def wait_for(process: subprocess.Popen, path: Path, marker: str, timeout: int) -> None:
    deadline = time.monotonic() + timeout
    while time.monotonic() < deadline:
        if marker in read_log(path):
            return
        if process.poll() is not None:
            raise RuntimeError(f"Server exited {process.returncode} before {marker!r}")
        time.sleep(0.5)
    raise TimeoutError(f"Server did not reach {marker!r} within {timeout}s")


def boot(label: str) -> None:
    log_path = WORK / f"{label}.log"
    with log_path.open("w", encoding="utf-8") as log:
        process = subprocess.Popen(
            ["java", "-Xms256M", "-Xmx1536M", "-jar", "fabric-server-launch.jar", "nogui"],
            cwd=WORK, stdin=subprocess.PIPE, stdout=log,
            stderr=subprocess.STDOUT, text=True,
        )
        try:
            wait_for(process, log_path, "Done (", 240)
            wait_for(process, log_path, "Banana Claims initialized with 0 claim(s)", 10)
            assert process.stdin is not None
            for command in (
                "claim admin diagnostics", "claim admin reload config",
                "claim admin reload preview", "claim admin reload claims",
                "save-all flush", "say BANANA_CLAIMS_SMOKE_COMMANDS_DONE",
            ):
                process.stdin.write(command + "\n")
                process.stdin.flush()
                time.sleep(0.5)
            wait_for(process, log_path, "BANANA_CLAIMS_SMOKE_COMMANDS_DONE", 30)
            time.sleep(1)
            process.stdin.write("stop\n")
            process.stdin.flush()
            return_code = process.wait(timeout=60)
            if return_code != 0:
                raise RuntimeError(f"Server shutdown returned {return_code}")
        finally:
            if process.poll() is None:
                process.terminate()
                try:
                    process.wait(timeout=15)
                except subprocess.TimeoutExpired:
                    process.kill()
                    process.wait(timeout=10)
    text = read_log(log_path)
    print(f"--- {label} server log ---\n{text}", flush=True)
    forbidden = (
        "Mixin apply failed", "MixinApplyError", "InvalidInjectionException",
        "NoSuchMethodError", "NoClassDefFoundError", "ClassNotFoundException",
        "An unexpected error occurred", "Unknown or incomplete command",
        "Encountered an unexpected exception", "ERROR", "FATAL",
    )
    for marker in forbidden:
        if marker in text:
            raise RuntimeError(f"{label} log contains failure marker: {marker}")
    print(f"PASS: {label} startup, initialization, console checks and graceful shutdown", flush=True)


def main() -> None:
    jars = [p for p in (PROJECT / "build" / "libs").glob("*.jar") if not p.name.endswith("-sources.jar")]
    if len(jars) != 1:
        raise RuntimeError(f"Expected one production JAR, found {jars}")
    jar = jars[0]
    with zipfile.ZipFile(jar) as archive:
        metadata = json.loads(archive.read("fabric.mod.json"))
        if metadata["id"] != "bananaclaims" or metadata["depends"]["minecraft"] != "~26.3":
            raise RuntimeError("Unexpected mod identity or Minecraft dependency")
        if metadata["version"] != "1.0.2-rc.1+mc26.3":
            raise RuntimeError("Unexpected candidate version")
    WORK.mkdir(parents=True, exist_ok=False)
    (WORK / "mods").mkdir()
    shutil.copy2(jar, WORK / "mods" / jar.name)
    api_name = f"fabric-api-{API}.jar"
    api_url = f"https://maven.fabricmc.net/net/fabricmc/fabric-api/fabric-api/{API}/{api_name}"
    download(api_url, WORK / "mods" / api_name)
    download(api_url + ".sha256", WORK / "fabric-api.sha256")
    expected_hash = (WORK / "fabric-api.sha256").read_text().split()[0].lower()
    actual_hash = hashlib.sha256((WORK / "mods" / api_name).read_bytes()).hexdigest()
    if actual_hash != expected_hash:
        raise RuntimeError("Fabric API checksum mismatch")
    download(
        f"https://meta.fabricmc.net/v2/versions/loader/{MC}/{LOADER}/{INSTALLER}/server/jar",
        WORK / "fabric-server-launch.jar",
    )
    # This EULA setting applies only to this ephemeral CI test world.
    (WORK / "eula.txt").write_text("eula=true\n", encoding="utf-8")
    (WORK / "server.properties").write_text(
        "server-ip=127.0.0.1\nserver-port=25595\nonline-mode=true\n"
        "white-list=true\nenforce-whitelist=true\nmax-players=1\n"
        "enable-rcon=false\nenable-query=false\nview-distance=2\nsimulation-distance=2\n"
        "level-name=smoke-world\nlevel-seed=1\ngenerate-structures=false\n"
        "motd=Temporary Banana Claims CI smoke test\n", encoding="utf-8",
    )
    boot("fresh")
    configs = list((WORK / "config").rglob("*.json"))
    for name in ("bananaclaims.json", "bananaclaims-preview.json"):
        matches = [p for p in configs if p.name == name]
        if len(matches) != 1:
            raise RuntimeError(f"Expected generated {name}, found {matches}")
        json.loads(matches[0].read_text(encoding="utf-8"))
    boot("restart")
    RELEASE.mkdir()
    shutil.copy2(jar, RELEASE / jar.name)
    digest = hashlib.sha256(jar.read_bytes()).hexdigest()
    (RELEASE / "SHA256SUMS.txt").write_text(f"{digest}  {jar.name}\n", encoding="utf-8")
    (RELEASE / "TEST-STATUS.txt").write_text(
        f"Banana Claims {metadata['version']}\nMinecraft {MC}; Fabric Loader {LOADER}; Fabric API {API}; Java 25.\n"
        "PASSED: clean compilation; built-JAR metadata; fresh dedicated server startup;\n"
        "empty-claim initialization; console diagnostics/config/preview/claims reload commands;\n"
        "generated config JSON parsing; graceful shutdown and restart.\n"
        "Test uses only Fabric API and Banana Claims on loopback with an empty whitelist.\n\n"
        "NOT TESTED: player claims, invitations, book clicks, boundary previews, protections,\n"
        "LuckPerms grants/denials, BlueMap integration, existing claim migration or Foxvale's mod combination.\n"
        "Candidate only. Back up server data and test in game before production approval.\n"
        "No production server was accessed by this workflow.\n", encoding="utf-8",
    )
    print(f"SMOKE TEST PASSED; candidate {jar.name}; SHA256 {digest}", flush=True)


if __name__ == "__main__":
    try:
        main()
    except Exception:
        for log_file in sorted(WORK.glob("*.log")) if WORK.exists() else []:
            print(f"--- failure evidence: {log_file.name} ---\n{read_log(log_file)[-20000:]}", flush=True)
        raise
