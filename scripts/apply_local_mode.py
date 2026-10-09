#!/usr/bin/env python3
"""Explicit, reversible offline hooks; original editor/entitlement code is preserved."""
import argparse, hashlib, json, pathlib, re, xml.etree.ElementTree as ET
ANDROID = '{http://schemas.android.com/apk/res/android}'

def apply(decoded):
    decoded=pathlib.Path(decoded); changes=[]
    protected={str(p.relative_to(decoded)):hashlib.sha256(p.read_bytes()).hexdigest()
               for p in decoded.glob('smali*/com/oneguystudio/ytyper/ui/editor/**/*.smali')}
    protected.update({str(p.relative_to(decoded)):hashlib.sha256(p.read_bytes()).hexdigest()
                      for p in decoded.glob('smali*/com/oneguystudio/ytyper/data/model/**/*.smali')})
    def source(name):
        matches=list(decoded.glob('smali*/com/oneguystudio/ytyper/'+name+'.smali'))
        assert len(matches)==1,(name,len(matches))
        return matches[0]
    def replace(name,signature,body):
        p=source(name);old=p.read_text()
        pattern=r'(^\.method[^\n]* '+re.escape(signature)+r'\n).*?^\.end method'
        text,count=re.subn(pattern,lambda m:m[1]+body+'\n.end method',old,flags=re.M|re.S)
        assert count==1,(name,signature,count)
        p.write_text(text);changes.append({'file':str(p.relative_to(decoded)),'method':signature})
    noop='    .locals 0\n    return-void'
    for method in ['forceSyncFcmToken()V','trackActivityAndScheduleWorkers()V']:
        replace('ui/dashboard/ProjectsActivity',method,noop)
    for method in ['executeBackup(Lcom/oneguystudio/ytyper/data/model/ProjectMeta;)V','showElegantCloudProjectsDialog()V']:
        replace('ui/dashboard/ProjectsActivity',method,'    .locals 0\n    invoke-static {p0}, Lcom/cookies/editor/local/OfflineBridge;->unavailable(Landroid/content/Context;)V\n    return-void')
    replace('data/UpdateChecker','checkForUpdates(Landroid/app/Activity;)V',noop)
    for method in ['pushSettingsJava(Landroid/content/Context;)V','syncPendingBubblesJava(Landroid/content/Context;)V','updateFcmTokenJava(Ljava/lang/String;)V']:
        replace('utils/CloudSyncManager',method,noop)
    replace('ui/settings/SettingsActivity','loadUserProfile()V','    .locals 0\n    invoke-static {p0}, Lcom/cookies/editor/local/OfflineBridge;->profile(Landroid/app/Activity;)V\n    return-void')
    replace('ui/settings/SettingsActivity','performSignOut()V','    .locals 0\n    invoke-static {p0}, Lcom/cookies/editor/local/OfflineBridge;->signOut(Landroid/app/Activity;)V\n    return-void')
    replace('ui/dashboard/SplashActivity','onCreate(Landroid/os/Bundle;)V','    .locals 0\n    invoke-super {p0, p1}, Landroidx/fragment/app/k0;->onCreate(Landroid/os/Bundle;)V\n    invoke-static {p0}, Lcom/cookies/editor/local/OfflineBridge;->openWelcome(Landroid/app/Activity;)V\n    return-void')
    tutorial=source('utils/TutorialHelper');text=tutorial.read_text()
    # The reference passes a resource name to Color.parseColor, which throws
    # on the first assistant tutorial. Correct the literal, retaining its UI.
    assert '"@color/grid_center_line_color"' in text
    tutorial.write_text(text.replace('"@color/grid_center_line_color"','"#D4AF37"'))
    changes.append({'file':str(tutorial.relative_to(decoded)),'method':'showNextStep: invalid color literal corrected'})
    for name,layout in [('ui/dashboard/StoreActivity','activity_store'),('ui/community/CommunityActivity','activity_community'),('ui/community/CreatePostActivity','activity_create_post'),('ui/dashboard/WebtoonScraperActivity','activity_webtoon_scraper')]:
        replace(name,'onCreate(Landroid/os/Bundle;)V',f'    .locals 1\n    invoke-super {{p0, p1}}, Landroidx/fragment/app/k0;->onCreate(Landroid/os/Bundle;)V\n    const-string v0, "{layout}"\n    invoke-static {{p0, v0}}, Lcom/cookies/editor/local/OfflineBridge;->servicePreview(Landroid/app/Activity;Ljava/lang/String;)V\n    return-void')
        # Original service lifecycle expects fields initialized by its online onCreate.
        p=source(name)
        for lifecycle in ['onResume()V','onPause()V','onDestroy()V']:
            text=p.read_text()
            m=re.search(r'^\.method[^\n]* '+re.escape(lifecycle)+r'\n(.*?)^\.end method',text,re.M|re.S)
            if m:
                super_call=re.search(r'    invoke-super[^\n]+',m[1]);assert super_call,(name,lifecycle)
                replace(name,lifecycle,'    .locals 0\n'+super_call[0]+'\n    return-void')
    app=source('data/YTyperApp');text=app.read_text()
    marker='    invoke-super {p0}, Landroid/app/Application;->onCreate()V'
    assert text.count(marker)==1
    text=text.replace(marker,marker+'\n\n    invoke-static {p0}, Lcom/cookies/editor/local/OfflineBridge;->install(Landroid/app/Application;)V')
    app.write_text(text);changes.append({'file':str(app.relative_to(decoded)),'method':'onCreate()V'})
    manifest=decoded/'AndroidManifest.xml';tree=ET.parse(manifest);root=tree.getroot()
    # Permission removal enforces offline isolation even if an overlooked library tries a request.
    for el in list(root):
        if el.tag=='uses-permission' and el.get(ANDROID+'name') in ['android.permission.INTERNET','com.google.android.gms.permission.AD_ID','com.google.android.c2dm.permission.RECEIVE']:
            root.remove(el)
    application=root.find('application');application.set(ANDROID+'allowBackup','false')
    activity=ET.SubElement(application,'activity',{ANDROID+'name':'com.cookies.editor.local.WelcomeActivity',ANDROID+'exported':'true',ANDROID+'theme':'@android:style/Theme.Material.NoActionBar',ANDROID+'windowSoftInputMode':'adjustResize'})
    original=next(el for el in application.findall('activity') if el.get(ANDROID+'name')=='com.oneguystudio.ytyper.ui.dashboard.SplashActivity')
    for intent in list(original.findall('intent-filter')):
        original.remove(intent);activity.append(intent)
    original.set(ANDROID+'exported','false')
    for el in list(application):
        name=el.get(ANDROID+'name','')
        if 'firebase' in name.lower() or 'c2dm' in name.lower() or 'ads.AdActivity' in name:
            application.remove(el)
    ET.register_namespace('android',ANDROID[1:-1]);tree.write(manifest,encoding='utf-8',xml_declaration=True)
    # Ensure editor, drawing, transform, fonts, and purchase entitlement instructions are untouched by this script.
    assert all(hashlib.sha256((decoded/path).read_bytes()).hexdigest()==digest for path,digest in protected.items()),'Protected editor/model instructions changed'
    report={'mode':'device-local','network_permission':False,'google_login_enabled':False,'password_storage':'salted PBKDF2-HMAC-SHA256; API24-25 HMAC-SHA1 fallback','backend_credentials_used':False,'entitlements_modified':False,'editor_engine_modified':False,'protected_editor_model_files_checked':len(protected),'online_screens':'disabled original-layout previews','changes':changes}
    (decoded/'cookies-local-mode.json').write_text(json.dumps(report,ensure_ascii=False,indent=2))
    print(f'Offline mode: {len(changes)} narrow method hooks; networking removed; editor and entitlements unchanged.')

if __name__=='__main__':
    p=argparse.ArgumentParser();p.add_argument('decoded');apply(p.parse_args().decoded)
