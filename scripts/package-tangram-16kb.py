#!/usr/bin/env python3
"""Preserve the official AAR Java API and package rebuilt JNI deterministically."""
import hashlib
import json
import pathlib
import subprocess
import sys
import zipfile

source, ndk, build, official, output = map(pathlib.Path, sys.argv[1:])
abis = {'armeabi-v7a': 'arm-linux-androideabi', 'arm64-v8a': 'aarch64-linux-android',
        'x86': 'i686-linux-android', 'x86_64': 'x86_64-linux-android'}
with zipfile.ZipFile(official) as archive:
    entries = {name: archive.read(name) for name in archive.namelist()
               if not name.startswith('jni/') and not name.endswith('/')}
for abi, triple in abis.items():
    entries[f'jni/{abi}/libtangram.so'] = (build / abi / 'libtangram.so').read_bytes()
    entries[f'jni/{abi}/libc++_shared.so'] = (build / abi / 'libc++_shared.so').read_bytes()
tracked = subprocess.check_output(['git', '-C', str(source), 'ls-files', '--recurse-submodules', '-z']).split(b'\0')
for raw in tracked:
    if not raw:
        continue
    name = raw.decode()
    leaf = pathlib.PurePosixPath(name).name.upper()
    if leaf.startswith(('LICENSE', 'COPYING', 'COPYRIGHT', 'NOTICE')):
        entries[f'META-INF/tangram-licenses/{name}'] = (source / name).read_bytes()
entries['META-INF/tangram-licenses/NDK-NOTICE'] = (ndk / 'NOTICE').read_bytes()
entries['META-INF/tangram-licenses/NDK-NOTICE.toolchain'] = (ndk / 'NOTICE.toolchain').read_bytes()
for license_file in (pathlib.Path(__file__).resolve().parent.parent / 'third_party/tangram').iterdir():
    if license_file.is_file():
        entries[f'META-INF/tangram-licenses/supplemental/{license_file.name}'] = license_file.read_bytes()
submodules = subprocess.check_output(['git', '-C', str(source), 'submodule', 'status', '--recursive']).decode()
submodule_revisions = {}
for line in submodules.splitlines():
    revision, name = line[1:].split()[:2]
    submodule_revisions[name] = revision
entries['META-INF/tangram-rebuild.json'] = (json.dumps({
    'source': 'https://github.com/tangrams/tangram-es',
    'revision': subprocess.check_output(['git', '-C', str(source), 'rev-parse', 'HEAD']).decode().strip(),
    'upstream_aar_sha256': hashlib.sha256(official.read_bytes()).hexdigest(),
    'ndk': '28.2.13676358', 'android_api': 21, 'cxx_standard': 14,
    'page_size': 16384, 'abis': list(abis), 'submodules': submodule_revisions,
}, indent=2, sort_keys=True) + '\n').encode()
# Stored entries avoid host zlib variation. AGP controls final APK compression/alignment.
temporary = output.with_suffix('.aar.tmp')
with zipfile.ZipFile(temporary, 'w', compression=zipfile.ZIP_STORED) as archive:
    for name, data in sorted(entries.items()):
        entry = zipfile.ZipInfo(name, date_time=(1980, 1, 1, 0, 0, 0))
        entry.create_system = 3
        entry.external_attr = 0o100644 << 16
        archive.writestr(entry, data)
temporary.replace(output)
