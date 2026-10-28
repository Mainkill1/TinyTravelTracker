#!/usr/bin/env python3
"""Compile the source-level font XML fix and replace only its original class."""
import hashlib
import json
import pathlib
import subprocess
import sys
import zipfile

source, official, annotations, sdk, jdk, output = map(pathlib.Path, sys.argv[1:])
root = pathlib.Path(__file__).resolve().parent.parent
patch = root / 'third_party/tangram/patches/font-axis.patch'
android = sdk / 'platforms/android-36/android.jar'
sha = lambda path: hashlib.sha256(path.read_bytes()).hexdigest()
if sha(android) != 'd9eb9da824d9e247a352f570f01e1169e725b2954bca9e283a71786c59b59f9a':
    sys.exit('Use the pinned Android 36 platform revision 2 android.jar.')
compiler = subprocess.check_output([str(jdk / 'bin/javac'), '-version'], text=True).strip()
release = dict(line.split('=', 1) for line in (jdk / 'release').read_text().splitlines() if '=' in line)
if (compiler != 'javac 17.0.17' or release.get('IMPLEMENTOR') != '"Eclipse Adoptium"'
        or release.get('JAVA_RUNTIME_VERSION') != '"17.0.17+10"'):
    sys.exit('Use the pinned Temurin JDK 17.0.17+10.')
prepared = output / 'source'
java = prepared / 'com/mapzen/tangram/FontFileParser.java'
java.parent.mkdir(parents=True, exist_ok=True)
java.write_bytes((source / 'platforms/android/tangram/src/main/java/com/mapzen/tangram/FontFileParser.java').read_bytes())
subprocess.run(['git', '-C', str(prepared), 'apply', '--unidiff-zero', str(patch)], check=True)
with zipfile.ZipFile(official) as aar:
    original_jar = aar.read('classes.jar')
original = output / 'original-classes.jar'
original.write_bytes(original_jar)
classes = output / 'classes'
classes.mkdir(exist_ok=True)
subprocess.run([str(jdk / 'bin/javac'), '-source', '8', '-target', '8', '-g:none',
                '-encoding', 'UTF-8', '-Xlint:-options', '-implicit:none',
                '-cp', ':'.join(map(str, (original, annotations, android))),
                '-d', str(classes), str(java)], check=True)
replacement_name = 'com/mapzen/tangram/FontFileParser.class'
replacement = (classes / replacement_name).read_bytes()
with zipfile.ZipFile(original) as archive:
    entries = {name: archive.read(name) for name in archive.namelist() if not name.endswith('/')}
entries[replacement_name] = replacement
with zipfile.ZipFile(output / 'classes.jar', 'w', compression=zipfile.ZIP_STORED) as archive:
    for name, data in sorted(entries.items()):
        info = zipfile.ZipInfo(name, (1980, 1, 1, 0, 0, 0))
        info.create_system = 3
        info.external_attr = 0o100644 << 16
        archive.writestr(info, data)
(output / 'provenance.json').write_text(json.dumps({
    'compiler': compiler, 'jdk_distribution': 'Temurin 17.0.17+10',
    'source_target': 8, 'debug_information': False,
    'android_jar_sha256': sha(android), 'annotations_sha256': sha(annotations),
    'patch': 'font-axis.patch', 'patch_sha256': sha(patch),
    'patched_source_sha256': sha(java), 'replaced_class': replacement_name,
    'original_class_sha256': hashlib.sha256(zipfile.ZipFile(original).read(replacement_name)).hexdigest(),
    'replacement_class_sha256': hashlib.sha256(replacement).hexdigest(),
}, indent=2, sort_keys=True) + '\n')
