#!/usr/bin/env python3
"""Create a local screenshot viewer from actual instrumentation evidence only."""
import argparse,html,json,pathlib

TITLES={
    'welcome':'شاشة الترحيب','welcome-motion':'الترحيب — إطار آخر للحركة',
    'local-sign-up':'إنشاء ملف محلي','dashboard':'لوحة المشاريع بعد الدخول المحلي',
    'local-sign-in-sheet':'لوحة الدخول المحلي','text-size-increased':'زيادة حجم النص',
    'project-imported':'استيراد الصورة الطويلة','project-gallery':'صفحات المشروع',
    'editor-long-image':'المحرر والصورة الطويلة','editor-pinch-zoom':'التكبير بإصبعين',
    'text-added':'إضافة النص','arabic-text':'تعديل الحوار العربي',
    'drawing-tools':'أدوات الرسم','drawing-stroke':'رسم خط باللمس',
    'layers':'لوحة الطبقات','shapes-picker':'اختيار الأشكال',
    'image-drafts':'مسودات الصور','canvas-resize-dialog':'تغيير أبعاد مساحة العمل','background-crop':'قص الخلفية',
    'undo':'التراجع','redo':'الإعادة','project-reopened':'إعادة فتح المشروع المحفوظ',
    'export-studio':'استوديو التصدير','reader':'القارئ','font-library':'مكتبة الخطوط',
    'export-selected':'تحديد صفحات التصدير','png-export-finished':'اكتمال تصدير PNG','tag-mini-editor':'محرر الوسوم المصغر',
    'arabic-fonts':'الخطوط العربية','english-fonts':'الخطوط الإنجليزية','imported-fonts':'الخطوط المخصصة',
    'floating-assistant':'المساعد العائم','settings-local-profile':'الإعدادات والملف المحلي',
    'store-disabled-preview':'المتجر — خدمة معطلة','community-disabled-preview':'المجتمع — خدمة معطلة',
    'create-post-disabled-preview':'إنشاء منشور — خدمة معطلة','webtoon-disabled-preview':'تنزيل ويبتون — خدمة معطلة',
    'guest-dashboard':'فتح المشاريع دون حساب',
}
PANELS={'font':'الخط','format':'التنسيق','color':'اللون','stroke':'الحدود','background':'الخلفية','shadow':'الظل','position':'الموضع','spacing':'المسافات','3d':'الدوران ثلاثي الأبعاد','perspective':'المنظور','effects':'التأثيرات','texture':'الخامة','opacity':'الشفافية','styles':'الأنماط','eraser':'الممحاة'}
def title(name):
    if name.startswith('text-panel-'):return 'النص — '+PANELS.get(name[11:],name[11:])
    if name.startswith('failure-'):return 'لقطة تشخيص فشل — '+name[8:]
    return TITLES.get(name,name)

def create(directory):
    p=pathlib.Path(directory);report=json.loads((p/'ui-verification.json').read_text());cards=[]
    for i,s in enumerate(report['steps']):
        assert (p/s['file']).is_file(),s['file']
        label=title(s['screen']);disabled='disabled-preview' in s['screen']
        badge='خدمة معطلة' if disabled else 'لقطة فعلية'
        cards.append(f'<article><a href="{html.escape(s["file"])}" target="_blank"><img loading="lazy" src="{html.escape(s["file"])}" alt="{html.escape(label)}"></a><div class="caption"><span class="badge">{badge}</span><h2>{i+1:02d}. {html.escape(label)}</h2><details><summary>تفاصيل الخطوة</summary><p dir="ltr">{html.escape(s["operation"])}</p><p dir="ltr">{html.escape(s["activity"])}</p><a href="{html.escape(s["hierarchy"])}">عناصر الواجهة الملتقطة</a></details></div></article>')
    passed=sum(c['status']=='pass' for c in report['checks']);failed=sum(c['status']=='fail' for c in report['checks'])
    rows=''.join(f'<tr><td dir="ltr">{html.escape(c["name"])}</td><td class="{c["status"]}">{"نجح" if c["status"]=="pass" else "فشل"}</td><td dir="ltr">{html.escape(c.get("error",""))}</td></tr>' for c in report['checks'])
    document='''<!doctype html><html lang="ar" dir="rtl"><meta charset="utf-8"><meta name="viewport" content="width=device-width,initial-scale=1"><title>Cookies Editor — صور الاختبار الفعلية</title><style>
    *{box-sizing:border-box}body{margin:0;background:#0b0c0f;color:#f3eedf;font:16px/1.7 system-ui,sans-serif}header,main{max-width:1400px;margin:auto;padding:32px 24px}header{border-bottom:1px solid #443b25}small,.badge{color:#e5c576}h1{font-size:clamp(27px,4vw,46px);line-height:1.4;margin:8px 0}p{color:#b7b4a9}a{color:#e5c576}.stats{display:flex;gap:12px;flex-wrap:wrap}.stats span{padding:7px 14px;background:#202019;border:1px solid #403927;border-radius:30px}.gallery{display:grid;grid-template-columns:repeat(auto-fill,minmax(260px,1fr));gap:22px}article{background:#17191f;border:1px solid #383429;border-radius:18px;overflow:hidden}img{display:block;width:100%;height:500px;object-fit:contain;background:#060709}.caption{padding:16px}h2{font-size:17px;margin:8px 0}.badge{font-size:12px;border:1px solid #66512d;padding:2px 8px;border-radius:30px}details{font-size:12px;color:#aaa}table{width:100%;border-collapse:collapse;font-size:13px}td{padding:10px;border-bottom:1px solid #333}td.pass{color:#9bd3ac}td.fail{color:#ffb095}.scroll{overflow:auto;margin:24px 0}.notice{border-right:3px solid #d4af37;padding:2px 18px;margin:20px 0}button{background:#24221a;color:#e5c576;border:1px solid #6a5530;border-radius:10px;padding:12px;font:inherit;cursor:pointer}body.compact img{height:270px}</style>
    <header><small>COOKIES EDITOR / ANDROID</small><h1>الواجهات والخطوات الملتقطة فعلًا</h1><p>هذا الملف يعرض صور التطبيق أثناء الاختبار، وليس تصاميم مقترحة أو صورًا مولدة.</p>STATS<div class="notice"><p>صور الخدمات السحابية تعرض واجهات معطلة. فتح لوحة أداة لا يثبت اختبار جميع خياراتها؛ تفاصيل الفحوص مثبتة في الجدول والتقرير.</p></div><button onclick="document.body.classList.toggle('compact')">تغيير حجم الصور</button> <a href="ui-verification.json">التقرير الكامل</a></header>
    <main><div class="gallery">CARDS</div><div class="scroll"><table><tbody>ROWS</tbody></table></div></main></html>'''
    document=document.replace('STATS',f'<div class="stats"><span>{len(cards)} لقطة</span><span>{passed} فحص ناجح</span><span>{failed} فحص فاشل</span></div>').replace('CARDS',''.join(cards)).replace('ROWS',rows)
    (p/'index.html').write_text(document)
    print('Gallery created:',p/'index.html')

if __name__=='__main__':
    parser=argparse.ArgumentParser();parser.add_argument('directory');create(parser.parse_args().directory)
