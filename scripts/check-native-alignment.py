#!/usr/bin/env python3
"""Check ELF LOAD and RELRO alignment in a shared library, AAR, or APK."""
import pathlib
import struct
import sys
import zipfile

PAGE = 16384


def check(name, data):
    if data[:4] != b"\x7fELF" or data[5] != 1:
        raise ValueError(f"{name}: expected little-endian ELF")
    elf64 = data[4] == 2
    if data[4] not in (1, 2):
        raise ValueError(f"{name}: unsupported ELF class")
    if elf64:
        offset = struct.unpack_from("<Q", data, 32)[0]
        size, count = struct.unpack_from("<HH", data, 54)
    else:
        offset = struct.unpack_from("<I", data, 28)[0]
        size, count = struct.unpack_from("<HH", data, 42)
    loads = []
    relros = []
    for index in range(count):
        header = offset + index * size
        if elf64:
            kind, flags, fileoff, addr, _, _, memsize, align = struct.unpack_from("<IIQQQQQQ", data, header)
        else:
            kind, fileoff, addr, _, _, memsize, flags, align = struct.unpack_from("<IIIIIIII", data, header)
        if kind == 1:
            loads.append((addr, addr + memsize, flags))
            if elf64 and (align < PAGE or (fileoff - addr) % PAGE):
                raise ValueError(f"{name}: LOAD is not 16 KB aligned")
        if kind == 0x6474E552:
            relros.append((addr, addr + memsize))
    if not loads:
        raise ValueError(f"{name}: no LOAD segments")
    if elf64:
        for start, end in relros:
            # A RELRO segment may end inside a page if it occupies its own LOAD
            # segment: the gap before the next writable LOAD is harmless.
            rounded_start = start // PAGE * PAGE
            rounded_end = (end + PAGE - 1) // PAGE * PAGE
            for load_start, load_end, flags in loads:
                if flags & 2 and (max(load_start, rounded_start) < min(load_end, start)
                                  or max(load_start, end) < min(load_end, rounded_end)):
                    raise ValueError(f"{name}: RELRO shares a 16 KB page with writable data")
    print(f"{name}: {'16 KB aligned' if elf64 else '32-bit ELF (16 KB devices use 64-bit ABIs)'}")


def main():
    for filename in sys.argv[1:]:
        path = pathlib.Path(filename)
        if zipfile.is_zipfile(path):
            with zipfile.ZipFile(path) as archive:
                libs = [name for name in archive.namelist() if name.endswith('.so')]
                if not libs:
                    raise ValueError(f"{path}: no native libraries")
                for name in sorted(libs):
                    check(name, archive.read(name))
        else:
            check(str(path), path.read_bytes())


if __name__ == '__main__':
    if len(sys.argv) < 2:
        sys.exit('Usage: check-native-alignment.py LIBRARY.so|ARTIFACT.aar|APP.apk [...]')
    try:
        main()
    except (ValueError, OSError, struct.error, zipfile.BadZipFile) as error:
        sys.exit(str(error))
