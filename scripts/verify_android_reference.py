#!/usr/bin/env python3
"""Validate source hash, every original font/PSD engine/native library, identity."""
import hashlib,json,pathlib,subprocess,sys,zipfile
source,destination,report=map(pathlib.Path,sys.argv[1:])
expected='56ed425f56d0d8614c39ad3e0a50f4eb3528e35676b1f4dc49bbfced4b033e3b'
assert hashlib.file_digest(source.open('rb'),'sha256').hexdigest()==expected,'Unexpected source APK'
with zipfile.ZipFile(source) as a,zipfile.ZipFile(destination) as b:
    assert b.testzip() is None,'Corrupt APK'
    entries=[n for n in a.namelist() if n.startswith(('assets/','lib/'))]
    different=[n for n in entries if n not in b.namelist() or a.read(n)!=b.read(n)]
    assert not different,'Rendering assets or native code changed: '+str(different)
    result={'reference_sha256':expected,'rebuilt_sha256':hashlib.file_digest(destination.open('rb'),'sha256').hexdigest(),'font_psd_and_native_entries_checked':len(entries),'changed_rendering_assets':different,'archive_integrity':'pass','runtime_editor_parity':'not yet verified','google_login_with_new_signature':'not verified; original OAuth service is not controlled by Cookies','ios_status':'not built from this Android artifact'}
report.parent.mkdir(parents=True,exist_ok=True);report.write_text(json.dumps(result,indent=2));print(json.dumps(result,indent=2))
