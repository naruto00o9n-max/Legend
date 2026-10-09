#!/usr/bin/env python3
"""Extract a complete screen/control inventory without exporting server credentials."""
import argparse, json, pathlib, re, xml.etree.ElementTree as ET
ANDROID='{http://schemas.android.com/apk/res/android}'
def inventory(decoded, java, output):
    decoded=pathlib.Path(decoded);java=pathlib.Path(java);output=pathlib.Path(output);output.mkdir(parents=True,exist_ok=True)
    strings={}
    for lang in ['values','values-ar']:
        strings[lang]={el.get('name'):''.join(el.itertext()) for el in ET.parse(decoded/'res'/lang/'strings.xml').getroot() if el.tag=='string'}
    layouts=[]
    for file in sorted((decoded/'res/layout').glob('*.xml')):
        root=ET.parse(file).getroot();controls=[]
        for node in root.iter():
            id=node.get(ANDROID+'id');text=node.get(ANDROID+'text','')
            if not id and not text:continue
            key=text.removeprefix('@string/') if text.startswith('@string/') else None
            controls.append({'id':id.removeprefix('@id/') if id else None,'view':node.tag,'text_ar':strings['values-ar'].get(key,strings['values'].get(key,text)) if key else text,'text_en':strings['values'].get(key,text) if key else text,'width':node.get(ANDROID+'layout_width'),'height':node.get(ANDROID+'layout_height'),'max':node.get(ANDROID+'max'),'progress':node.get(ANDROID+'progress'),'layout':node.get('layout')})
        layouts.append({'name':file.stem,'file':'res/layout/'+file.name,'controls':controls})
    models={}
    for file in sorted((java/'data/model').glob('*.java')):
        source=file.read_text(); fields=[]
        for type,name in re.findall(r'^    private (?:final )?(?!transient)([\w<>\[\], ]+) (\w+);',source,re.M):
            fields.append({'name':name,'type':type.strip()})
        if fields:models[file.stem]=fields
    failures=[]
    for file in sorted(java.rglob('*.java')):
        for n,line in enumerate(file.read_text().splitlines(),1):
            if 'throw new UnsupportedOperationException("Method not decompiled:' in line:
                failures.append({'file':str(file.relative_to(java)),'line':n,'method':line.split('Method not decompiled: ')[-1].split('"')[0]})
    result={'reference_version':'3.8','layouts':layouts,'models':models,'java_methods_not_reconstructed':failures,'port_status':'not an iOS implementation','backend_credentials_exported':False}
    (output/'reference-inventory.json').write_text(json.dumps(result,ensure_ascii=False,indent=2))
    print(json.dumps({'layouts':len(layouts),'controls':sum(len(x['controls']) for x in layouts),'models':len(models),'unreconstructed_methods':len(failures)},indent=2))
if __name__=='__main__':
    p=argparse.ArgumentParser();p.add_argument('decoded');p.add_argument('java');p.add_argument('output');a=p.parse_args();inventory(a.decoded,a.java,a.output)
