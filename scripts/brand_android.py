#!/usr/bin/env python3
"""Resource-only branding and scoped defaults. Editor instructions remain intact.

This is a reference rebuild, not a recovered Gradle/Kotlin project. Authentication
and original feature entitlement checks are retained. Do not publish it as a
standalone functional Cookies release before configuring independent services.
"""
import argparse, hashlib, json, pathlib, re, shutil, subprocess
ROOT=pathlib.Path(__file__).resolve().parent.parent
ANDROID='{http://schemas.android.com/apk/res/android}'
def brand(decoded):
    decoded=pathlib.Path(decoded); identity=json.loads((ROOT/'branding/identity.json').read_text())
    changed=[]
    for file in sorted((decoded/'res').rglob('*.xml')):
        original=file.read_text()
        text=re.sub(r'>YTyper<','>Cookies Editor<',original)
        # Names in resources are display text, not protocol keys or class names.
        text=text.replace('YTyper Plus','Cookies Editor Plus').replace('YTyper VIP','Cookies Editor VIP')
        text=text.replace('in YTyper','in Cookies Editor').replace('to YTyper','to Cookies Editor').replace('of YTyper','of Cookies Editor')
        text=text.replace('في YTyper','في Cookies Editor').replace('بك في YTyper','بك في Cookies Editor').replace('من YTyper','من Cookies Editor')
        text=text.replace('android:text="YTyper"','android:text="Cookies Editor"')
        text=re.sub(r'(?i)#(ff)?64ffda', lambda m:'#'+(m.group(1) or '')+'d4af37',text)
        if file.name=='activity_editor.xml': text=text.replace('android:text="Y"','android:text="🍪"')
        if text!=original: file.write_text(text);changed.append(str(file.relative_to(decoded)))
    # Keep fully qualified Android classes: they reference the restored smali.
    manifest=decoded/'AndroidManifest.xml';s=manifest.read_text()
    s=s.replace('package="com.oneguystudio.ytyper"','package="com.cookies.editor"')
    for suffix in ['.provider','.cropper.fileprovider','.mlkitinitprovider','.firebaseinitprovider','.androidx-startup','.DYNAMIC_RECEIVER_NOT_EXPORTED_PERMISSION']:
        s=s.replace('com.oneguystudio.ytyper'+suffix,'com.cookies.editor'+suffix)
    manifest.write_text(s);changed.append('AndroidManifest.xml')
    # Provider authorities appear as string constants too; model/class FQNs do not change.
    for file in decoded.glob('smali*/**/*.smali'):
        original=file.read_text();text=original
        for suffix in ['.provider','.cropper.fileprovider']:
            text=text.replace('"com.oneguystudio.ytyper'+suffix+'"','"com.cookies.editor'+suffix+'"')
        if file.as_posix().endswith('/data/Prefs.smali') or file.as_posix().endswith('/utils/ThemeUtils.smali'):
            text=text.replace('"#64FFDA"','"#D4AF37"')
        if file.as_posix().endswith('/data/Prefs.smali'):
            # Only this method's fallback language changes. User preferences still win.
            start=text.index('.method public final getLanguage()Ljava/lang/String;');end=text.index('.end method',start)
            text=text[:start]+text[start:end].replace('const-string v1, "en"','const-string v1, "ar"')+text[end:]
        if file.as_posix().endswith('/ui/dashboard/SplashActivityKt.smali'):
            # Compose's title is not in XML resources; change display literals only.
            text=text.replace('const-string v6, "Y"','const-string v6, "🍪"')
            text=text.replace('const-string v6, "Typer"','const-string v6, "Cookies"')
            text=text.replace('"Login with Google"','"تسجيل الدخول باستخدام Google"')
            text=text.replace('"Please sign in to continue."','"سجّل الدخول للمتابعة."')
            text=text.replace('0xff64ffdaL','0xffd4af37L')
        if text!=original:file.write_text(text);changed.append(str(file.relative_to(decoded)))
    for density,size,foreground in [('mdpi',48,108),('hdpi',72,162),('xhdpi',96,216),('xxhdpi',144,324),('xxxhdpi',192,432)]:
        folder=decoded/'res'/('mipmap-'+density);folder.mkdir(exist_ok=True)
        for name in ['ic_launcher','ic_launcher_round','ic_launcher_foreground']:
            for existing in folder.glob(name+'.*'): existing.unlink()
            destination=folder/(name+'.png')
            subprocess.run(['magick',str(ROOT/'branding/cookies-logo.png'),'-resize',str(foreground if name.endswith('foreground') else size)+'x'+str(foreground if name.endswith('foreground') else size),str(destination)],check=True)
            changed.append(str(destination.relative_to(decoded)))
    (decoded/'cookies-brand-changes.json').write_text(json.dumps({'identity':identity,'changed_files':changed,'auth_modified':False,'entitlements_modified':False,'editor_engine_modified':False},indent=2,ensure_ascii=False))
    print(f'Branding changed {len(changed)} files; editor, auth and entitlements retained.')
if __name__=='__main__':
    parser=argparse.ArgumentParser();parser.add_argument('decoded');brand(parser.parse_args().decoded)
