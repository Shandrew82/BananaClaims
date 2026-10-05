#!/usr/bin/env python3
"""Prepare pinned, licensed Foxvale mod forks. Never connects to a game server."""
from pathlib import Path
import json
import re
import shutil
import subprocess

ROOT = Path.cwd() / 'foxvale-build'
PINS = {
    'itemflexer': ('SilverAndro/itemflexer', 'a7f806d1c20b4e25694346458a8c6aa7150a950f'),
    'player-ladder': ('justbecauseph/modern-player-ladder', '26a2f784fe379ad7ec1f31af33defa066e21f921'),
}

def run(*args, cwd=None):
    subprocess.run(args, cwd=cwd, check=True)

def replace_once(path, old, new):
    text = path.read_text()
    if text.count(old) != 1:
        raise RuntimeError(f'{path}: expected exactly one occurrence of {old!r}')
    path.write_text(text.replace(old, new))

def write(path, text):
    path.parent.mkdir(parents=True, exist_ok=True)
    path.write_text(text)

ROOT.mkdir(exist_ok=True)
for name, (repo, sha) in PINS.items():
    dest = ROOT / name
    if dest.exists():
        raise RuntimeError(f'Refusing to overwrite {dest}; use a fresh working directory')
    run('git', 'init', str(dest))
    run('git', 'remote', 'add', 'origin', f'https://github.com/{repo}.git', cwd=dest)
    run('git', 'fetch', '--depth', '1', 'origin', sha, cwd=dest)
    run('git', 'checkout', '--detach', 'FETCH_HEAD', cwd=dest)
    actual = subprocess.check_output(['git', 'rev-parse', 'HEAD'], cwd=dest, text=True).strip()
    if actual != sha:
        raise RuntimeError(f'Unexpected source revision for {name}: {actual}')

item = ROOT / 'itemflexer'
ladder = ROOT / 'player-ladder'
java_path = item / 'src/main/java/dev/silverandro/itemflexer/ItemFlexer.java'
text = java_path.read_text()
replacements = {
    'net.minecraft.item.ItemStack': 'net.minecraft.world.item.ItemStack',
    'net.minecraft.item.Items': 'net.minecraft.world.item.Items',
    'net.minecraft.server.command.CommandManager': 'net.minecraft.commands.Commands',
    'net.minecraft.server.command.ServerCommandSource': 'net.minecraft.commands.CommandSourceStack',
    'net.minecraft.server.network.ServerPlayerEntity': 'net.minecraft.server.level.ServerPlayer',
    'net.minecraft.text.Text': 'net.minecraft.network.chat.Component',
    'net.minecraft.util.Identifier': 'net.minecraft.resources.Identifier',
    'ServerPlayerEntity': 'ServerPlayer',
    'ServerCommandSource': 'CommandSourceStack',
    'CommandManager.': 'Commands.',
    'Identifier.of(': 'Identifier.fromNamespaceAndPath(',
    'stack.toHoverableText()': 'stack.getDisplayName()',
    'Text.empty()': 'Component.empty()',
    'Text.of(': 'Component.literal(',
    'Text text;': 'Component text;',
    'Text message =': 'Component message =',
    '.getPlayerOrThrow()': '.getPlayerOrException()',
    '.getMainHandStack()': '.getMainHandItem()',
    '.getInventory().getStack(': '.getInventory().getItem(',
    '.sendError(': '.sendFailure(',
    '.getPlayerManager().getPlayerList()': '.getPlayerList().getPlayers()',
    'other.sendMessage(message, false)': 'other.sendSystemMessage(message)',
    'public static ItemStack stack;': 'public static ItemStack stack = ItemStack.EMPTY;',
    'cooldowns.get(ctx.player()) / 20f': 'cooldowns.getOrDefault(ctx.player(), 0) / 20f',
}
for old, new in replacements.items():
    if old not in text:
        raise RuntimeError(f'ItemFlexer source changed: missing {old!r}')
    text = text.replace(old, new)
java_path.write_text(text)
old_version = re.search(r'^\s*mod_version\s*=\s*(.+)$', (item/'gradle.properties').read_text(), re.M).group(1).strip().split('+')[0]
write(item/'gradle.properties', f'''org.gradle.jvmargs=-Xmx2G
org.gradle.parallel=true
minecraft_version=26.3
loader_version=0.19.5
loom_version=1.17.21
fabric_api_version=0.161.0+26.3
mod_version={old_version}+foxvale.26.3.1
maven_group=dev.silverandro
archives_base_name=itemflexer
''')
write(item/'build.gradle', '''plugins {
    id 'net.fabricmc.fabric-loom' version "${loom_version}"
}
version = project.mod_version
group = project.maven_group
base { archivesName = project.archives_base_name }
repositories {
    mavenCentral()
    maven { url = 'https://maven.fabricmc.net/' }
    maven { url = 'https://maven.nucleoid.xyz/' }
    maven { url = 'https://jitpack.io' }
}
dependencies {
    minecraft "com.mojang:minecraft:${project.minecraft_version}"
    implementation "net.fabricmc:fabric-loader:${project.loader_version}"
    implementation "net.fabricmc.fabric-api:fabric-api:${project.fabric_api_version}"
    implementation 'com.github.P03W:Microconfig:2.2.1'
    include 'com.github.P03W:Microconfig:2.2.1'
    implementation 'eu.pb4:placeholder-api:3.2.0+26.3'
    include 'eu.pb4:placeholder-api:3.2.0+26.3'
}
processResources {
    inputs.property 'version', project.version
    filesMatching('fabric.mod.json') { expand 'version': project.version }
}
tasks.withType(JavaCompile).configureEach {
    options.encoding = 'UTF-8'
    options.release = 25
}
java {
    withSourcesJar()
    sourceCompatibility = JavaVersion.VERSION_25
    targetCompatibility = JavaVersion.VERSION_25
}
jar { from('LICENSE'); from('FOXVALE-NOTICE.md') }
''')
for filename in ['gradlew', 'gradlew.bat', 'gradle/wrapper/gradle-wrapper.jar', 'gradle/wrapper/gradle-wrapper.properties']:
    destination = item/filename
    destination.parent.mkdir(parents=True, exist_ok=True)
    shutil.copy2(ladder/filename, destination)
(item/'gradlew').chmod(0o755)
(ladder/'gradlew').chmod(0o755)
metadata_path = item/'src/main/resources/fabric.mod.json'
meta = json.loads(metadata_path.read_text())
meta.update({
    'name': 'ItemFlexer (Foxvale 26.3)',
    'description': 'Unofficial Foxvale port of ItemFlexer for Minecraft 26.3. Flex items in chat with /flex.',
    'environment': '*',
    'depends': {'minecraft': '26.3', 'fabricloader': '>=0.19.5', 'fabric-api': '*', 'java': '>=25'},
})
metadata_path.write_text(json.dumps(meta, indent=2)+'\n')

# Retain the attachment identity, Boolean codec, copy-on-death, and set/toggle flow.
state = ladder/'src/main/java/town/lampas/modernplayerladder/ladder/PlayerLadderState.java'
replace_once(state, '.initializer(() -> false)', '.initializer(() -> PlayerLadderPreference.DEFAULT_ENABLED)')
replace_once(state, 'return player.getAttachedOrElse(ENABLED_ATTACHMENT, false);', 'return PlayerLadderPreference.resolve(player.getAttached(ENABLED_ATTACHMENT));')
write(state.with_name('PlayerLadderPreference.java'), '''package town.lampas.modernplayerladder.ladder;

/** Foxvale default-on policy. A saved false is an explicit opt-out, never a missing value. */
public final class PlayerLadderPreference {
    public static final boolean DEFAULT_ENABLED = true;
    private PlayerLadderPreference() {}
    public static boolean resolve(Boolean savedPreference) {
        return savedPreference == null ? DEFAULT_ENABLED : savedPreference;
    }
}
''')
write(ladder/'src/test/java/town/lampas/modernplayerladder/ladder/PlayerLadderPreferenceTest.java', '''package town.lampas.modernplayerladder.ladder;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class PlayerLadderPreferenceTest {
    @Test void anUnsetPreferenceIsEnabled() {
        assertTrue(PlayerLadderPreference.resolve(null));
    }
    @Test void savedOptOutWinsOverTheDefault() {
        assertFalse(PlayerLadderPreference.resolve(Boolean.FALSE));
    }
    @Test void savedOptInStaysEnabled() {
        assertTrue(PlayerLadderPreference.resolve(Boolean.TRUE));
    }
    @Test void togglingAnUnsetPreferenceProducesAnOptOut() {
        Boolean saved = !PlayerLadderPreference.resolve(null);
        assertFalse(saved);
        assertFalse(PlayerLadderPreference.resolve(saved));
    }
    @Test void togglingAnOptOutProducesAnOptIn() {
        Boolean saved = !PlayerLadderPreference.resolve(Boolean.FALSE);
        assertTrue(saved);
        assertTrue(PlayerLadderPreference.resolve(saved));
    }
}
''')
replace_once(ladder/'gradle.properties', 'mod_version=2.0.0', 'mod_version=2.0.0+foxvale.1')
metadata_path = ladder/'src/main/resources/fabric.mod.json'
meta = json.loads(metadata_path.read_text())
meta['name'] = 'Modern Player Ladder (Foxvale default-on)'
meta['description'] = 'Unofficial Foxvale build: head riding enabled for unset preferences; saved opt-outs always win. /ladder toggle changes your preference.'
metadata_path.write_text(json.dumps(meta, indent=2)+'\n')

for name, (repo, sha) in PINS.items():
    folder = ROOT/name
    notice = f'''# Unofficial Foxvale build — 2026-10-04

Upstream: https://github.com/{repo}
Pinned revision: {sha}
Target: Minecraft Java 26.3, Fabric Loader 0.19.5, Fabric API 0.161.0+26.3, Java 25.

Original authors and license are retained. This is not an official upstream release.
'''
    if name == 'player-ladder':
        notice += '''\nChanges: missing preferences default to enabled. An existing false remains disabled.
The attachment ID, Boolean persistence codec, copy-on-death flag and toggle behavior are unchanged.
Use /ladder toggle or /playerladder toggle to opt out or back in.
The original README describes the original opt-in behavior; this notice describes this fork.
'''
    else:
        notice += '''\nChanges: ported Yarn names to the 26.3 unobfuscated API, updated Fabric/Loom and
Placeholder API, retained Microconfig and the existing configuration and command structure.
Commands: /flex, /flex <1-9>, /flex showCount, /flex showCount <1-9>.
The displayed held-item placeholder is initialized to EMPTY, and querying cooldown before
using /flex returns zero rather than throwing a null-pointer exception.
'''
    notice += '''\nInstall only the distributable JAR, not the sources JAR. Do not keep two JARs with the
same mod ID. Back up old JARs outside mods. Source archives accompany this build.
No server restart, stop, command, or live upload is performed by this build recipe.
'''
    write(folder/'FOXVALE-NOTICE.md', notice)
    run('git', 'add', '-N', '.', cwd=folder)
    run('git', '-c', 'core.whitespace=blank-at-eol,blank-at-eof,space-before-tab,cr-at-eol', 'diff', '--check', cwd=folder)
    patch = subprocess.check_output(['git', 'diff', '--binary'], cwd=folder)
    (ROOT/f'{name}.patch').write_bytes(patch)

manifest = {'target': '26.3', 'source_pins': PINS, 'server_changed': False, 'server_restarted': False}
write(ROOT/'build-manifest.json', json.dumps(manifest, indent=2)+'\n')
print('Prepared both source trees. Compilation and test results must be checked separately.')
