package com.cookies.editor.fixtures;

import android.app.*;
import android.content.*;
import android.content.pm.PackageManager;
import android.graphics.*;
import android.net.Uri;
import android.os.*;
import android.provider.MediaStore;
import android.view.*;
import android.view.inputmethod.InputMethodManager;
import android.widget.*;
import java.io.*;
import java.lang.reflect.*;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.zip.*;
import org.json.*;

/** Real activities and UI actions. Each capture is evidence, not an inferred pass. */
public final class UiRunner extends Instrumentation {
    private static final String BASE="com.oneguystudio.ytyper.";
    private Context target; private ClassLoader loader; private File out;
    private volatile Activity top;
    private volatile String stage="starting";
    private final JSONArray steps=new JSONArray(); private final JSONArray checks=new JSONArray();
    private String projectId,pageId; private int sequence;
    interface Task {void run()throws Exception;}
    @Override public void onCreate(Bundle args){super.onCreate(args);start();}
    private Class<?> type(String name)throws Exception{return Class.forName(name,true,loader);}
    private int id(String name){return target.getResources().getIdentifier(name,"id",target.getPackageName());}
    private Object field(Object value,String name)throws Exception{Field f=value.getClass().getDeclaredField(name);f.setAccessible(true);return f.get(value);}
    private Object call(Object value,String name)throws Exception{return value.getClass().getMethod(name).invoke(value);}
    private void main(Runnable r){final Throwable[] failure={null};runOnMainSync(()->{try{r.run();}catch(Throwable e){failure[0]=e;}});if(failure[0]!=null)throw new RuntimeException(failure[0]);SystemClock.sleep(80);}
    private void waitActivity(String suffix)throws Exception{long deadline=SystemClock.uptimeMillis()+20000;while(SystemClock.uptimeMillis()<deadline){if(top!=null&&top.getClass().getName().endsWith(suffix))return;SystemClock.sleep(150);}throw new AssertionError("Expected "+suffix+"; current="+(top==null?"none":top.getClass().getName()));}
    private Activity launch(String name,Intent extras)throws Exception{
        Intent i=extras==null?new Intent():extras;i.setClassName(target,name);i.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        // startActivitySync waits for a globally idle UI; animated welcome and
        // original shimmer widgets keep posting frames. Wait for lifecycle instead.
        stage="launch:"+name;main(()->target.startActivity(i));waitActivity(name.substring(name.lastIndexOf('.')+1));SystemClock.sleep(600);return top;
    }
    private View view(String name){return top.findViewById(id(name));}
    private void click(String name)throws Exception{
        View v=view(name);if(v==null)throw new AssertionError("Missing control "+name);
        main(()->{v.requestRectangleOnScreen(new Rect(0,0,v.getWidth(),v.getHeight()),true);if(!v.performClick())throw new AssertionError("No click action "+name);});SystemClock.sleep(450);
    }
    private void set(String name,String value)throws Exception{View v=view(name);if(!(v instanceof EditText))throw new AssertionError("Missing input "+name);main(()->((EditText)v).setText(value));}
    private void number(String name,String value)throws Exception{set(name,value);main(()->((EditText)view(name)).onEditorAction(android.view.inputmethod.EditorInfo.IME_ACTION_DONE));}
    private void keyboard(){main(()->{View v=top.getCurrentFocus();if(v!=null)((InputMethodManager)top.getSystemService(Context.INPUT_METHOD_SERVICE)).hideSoftInputFromWindow(v.getWindowToken(),0);});SystemClock.sleep(200);}
    private void back(){sendKeyDownUpSync(KeyEvent.KEYCODE_BACK);SystemClock.sleep(350);}
    private void write(String name,String value)throws Exception{try(FileOutputStream f=new FileOutputStream(new File(out,name))){f.write(value.getBytes(StandardCharsets.UTF_8));}}
    private void publish(File file)throws Exception{
        ContentValues values=new ContentValues();values.put(MediaStore.Downloads.DISPLAY_NAME,file.getName());values.put(MediaStore.Downloads.MIME_TYPE,file.getName().endsWith(".png")?"image/png":file.getName().endsWith(".zip")?"application/zip":"application/json");values.put(MediaStore.Downloads.RELATIVE_PATH,"Download/Cookies-ui");values.put(MediaStore.Downloads.IS_PENDING,1);
        Uri uri=target.getContentResolver().insert(MediaStore.Downloads.EXTERNAL_CONTENT_URI,values);if(uri==null)throw new IOException("Cannot publish UI evidence");
        try(InputStream input=new FileInputStream(file);OutputStream output=target.getContentResolver().openOutputStream(uri)){byte[] bytes=new byte[32768];int n;while((n=input.read(bytes))>0)output.write(bytes,0,n);}
        values.clear();values.put(MediaStore.Downloads.IS_PENDING,0);target.getContentResolver().update(uri,values,null,null);
    }
    private JSONObject tree(View v)throws Exception{
        JSONObject j=new JSONObject().put("class",v.getClass().getSimpleName()).put("visible",v.getVisibility()).put("enabled",v.isEnabled());
        try{j.put("id",v.getResources().getResourceEntryName(v.getId()));}catch(Exception ignored){}
        if(v instanceof TextView){TextView t=(TextView)v;boolean secret=(t.getInputType()&0x80)!=0;j.put("text",secret?"[password redacted]":t.getText().toString());}
        if(v instanceof ViewGroup){JSONArray a=new JSONArray();for(int i=0;i<((ViewGroup)v).getChildCount();i++)a.put(tree(((ViewGroup)v).getChildAt(i)));j.put("children",a);}return j;
    }
    private void capture(String name,String operation)throws Exception{
        stage="capture:"+name;SystemClock.sleep(350);String stem=String.format(Locale.ROOT,"%03d-%s",++sequence,name);
        Bitmap b=getUiAutomation().takeScreenshot();if(b==null)throw new AssertionError("Screenshot unavailable");
        try(FileOutputStream f=new FileOutputStream(new File(out,stem+".png"))){if(!b.compress(Bitmap.CompressFormat.PNG,100,f))throw new AssertionError("Screenshot write");}finally{b.recycle();}
        final JSONObject[] hierarchy={null};main(()->{try{hierarchy[0]=tree(top.getWindow().getDecorView());}catch(Exception e){throw new RuntimeException(e);}});write(stem+".json",hierarchy[0].toString(2));
        steps.put(new JSONObject().put("screen",name).put("activity",top.getClass().getName()).put("operation",operation).put("file",stem+".png").put("hierarchy",stem+".json").put("status","captured"));
        publish(new File(out,stem+".png"));publish(new File(out,stem+".json"));
        android.util.Log.i("CookiesUi",stage);write("ui-progress.json",new JSONObject().put("stage",stage).put("steps",steps).put("checks",checks).toString(2));
    }
    private void attempt(String name,Task task)throws Exception{
        stage="check:"+name;android.util.Log.i("CookiesUi",stage);
        try{task.run();checks.put(new JSONObject().put("name",name).put("status","pass"));}
        catch(Throwable e){Throwable cause=e;while(cause.getCause()!=null)cause=cause.getCause();checks.put(new JSONObject().put("name",name).put("status","fail").put("error",cause.toString()));try{capture("failure-"+name,"Diagnostic after failure");}catch(Throwable ignored){} }
    }
    private void accountChecks()throws Exception{
        Class<?> auth=type("com.cookies.editor.local.LocalAccounts");Method m=auth.getMethod("authenticate",Context.class,String.class,char[].class,boolean.class);
        String address="auth-fixture@example.invalid",secret="local-fixture-3481";
        if(m.invoke(null,target,address,secret.toCharArray(),true)!=null)throw new AssertionError("Create local profile");
        if(m.invoke(null,target,address,"wrong-fixture".toCharArray(),false)==null)throw new AssertionError("Incorrect password accepted");
        if(m.invoke(null,target,address,secret.toCharArray(),false)!=null)throw new AssertionError("Correct password rejected");
        if(m.invoke(null,target,address,secret.toCharArray(),true)==null)throw new AssertionError("Duplicate profile accepted");
        if(m.invoke(null,target,"invalid","12345678".toCharArray(),true)==null)throw new AssertionError("Invalid email accepted");
        Map<String,?> stored=target.getSharedPreferences("CookiesLocalAccounts",0).getAll();for(Object v:stored.values())if(String.valueOf(v).contains(secret))throw new AssertionError("Plain password stored");
        auth.getMethod("signOut",Context.class).invoke(null,target);
        if(!"guest".equals(auth.getMethod("email",Context.class).invoke(null,target)))throw new AssertionError("Session not cleared");
    }
    private void image(String name,Bitmap bitmap)throws Exception{File f=new File(out,name);try(FileOutputStream stream=new FileOutputStream(f)){if(!bitmap.compress(Bitmap.CompressFormat.PNG,100,stream))throw new IOException("PNG fixture");}publish(f);}
    private void cleanerCheck()throws Exception{
        if(!Boolean.TRUE.equals(type("org.opencv.android.OpenCVLoader").getMethod("initDebug").invoke(null)))throw new AssertionError("Original native OpenCV not loaded");
        Bitmap source=Bitmap.createBitmap(400,400,Bitmap.Config.ARGB_8888),mask=Bitmap.createBitmap(400,400,Bitmap.Config.ARGB_8888),patch=null,composite=null;
        try{Canvas c=new Canvas(source);c.drawColor(Color.WHITE);Paint p=new Paint(3);p.setColor(Color.BLACK);p.setTextAlign(Paint.Align.CENTER);p.setTextSize(40);c.drawText("COOKIES",200,220,p);
            Canvas m=new Canvas(mask);m.drawColor(Color.BLACK);p.setColor(Color.WHITE);m.drawRect(75,165,325,235,p);
            Object engine=type(BASE+"engine.OpenCVEngine").getConstructor().newInstance();patch=(Bitmap)engine.getClass().getMethod("removeTextAndInpaint",Bitmap.class,Bitmap.class,double.class).invoke(engine,source,mask,3d);
            if(patch==null||patch.getWidth()!=400||patch.getHeight()!=400)throw new AssertionError("Native cleaner returned invalid patch");
            int erased=0;for(int y=0;y<400;y++)for(int x=0;x<400;x++){int original=source.getPixel(x,y),pixel=patch.getPixel(x,y);if(Color.red(original)<80&&Color.alpha(pixel)>240&&Color.red(pixel)>230)erased++;if((x<70||x>330||y<160||y>240)&&Color.alpha(pixel)!=0)throw new AssertionError("Cleaner patch changed unmasked area");}
            if(erased<300)throw new AssertionError("Native cleaner did not remove fixture lettering");
            composite=source.copy(Bitmap.Config.ARGB_8888,true);new Canvas(composite).drawBitmap(patch,0,0,null);image("native-cleaner-before.png",source);image("native-cleaner-mask.png",mask);image("native-cleaner-after.png",composite);
            write("native-cleaner-verification.json",new JSONObject().put("status","pass").put("erasedDarkPixels",erased).put("outsidePatchTransparent",true).put("scope","Actual original OpenCV patch engine; not every inpainting scenario").toString(2));
        }finally{source.recycle();mask.recycle();if(patch!=null)patch.recycle();if(composite!=null)composite.recycle();}
    }
    private void overlayCheck()throws Exception{
        // Foreground overlay permission is granted by the emulator script only.
        target.getSharedPreferences("AshtyperPrefs",0).edit().putInt("TUTORIAL_MAIN_SHOWN",1).putBoolean("TUTORIAL_FLOATING",true).apply();
        launch(BASE+"ui.floatingwidget.FolatingWidgetDashboard",null);set("editText","هذا حوار عربي تجريبي\nوهذه فقاعة ثانية");capture("assistant-dialogue-input","Entered local dialogue text for the floating assistant");click("button");waitActivity("ProjectsActivity");SystemClock.sleep(1300);
        boolean running=false;for(ActivityManager.RunningServiceInfo service:((ActivityManager)target.getSystemService(Context.ACTIVITY_SERVICE)).getRunningServices(100))if(service.service.getClassName().endsWith("FloatingWidgetService"))running=true;
        Intent stop=new Intent().setClassName(target,BASE+"ui.floatingwidget.FloatingWidgetService");
        try{
            if(!running)throw new AssertionError("Original floating service did not start");capture("floating-window","Original floating service running above Cookies dashboard");
            // Android Settings deliberately hides third-party overlays. Test the
            // actual launcher instead, and use UiAutomation for system input:
            // Instrumentation.sendKeyDownUpSync only injects into our own UID.
            android.accessibilityservice.AccessibilityServiceInfo info=getUiAutomation().getServiceInfo();
            info.flags|=android.accessibilityservice.AccessibilityServiceInfo.FLAG_RETRIEVE_INTERACTIVE_WINDOWS;getUiAutomation().setServiceInfo(info);
            long now=SystemClock.uptimeMillis();
            if(!getUiAutomation().injectInputEvent(new KeyEvent(now,now,KeyEvent.ACTION_DOWN,KeyEvent.KEYCODE_HOME,0),true)||!getUiAutomation().injectInputEvent(new KeyEvent(now,SystemClock.uptimeMillis(),KeyEvent.ACTION_UP,KeyEvent.KEYCODE_HOME,0),true))throw new AssertionError("Cannot open Android home");
            SystemClock.sleep(1400);boolean externalWindow=false,overlayText=false;
            for(android.view.accessibility.AccessibilityWindowInfo window:getUiAutomation().getWindows()){
                android.view.accessibility.AccessibilityNodeInfo root=window.getRoot();if(root==null)continue;
                String owner=String.valueOf(root.getPackageName());
                if(!owner.equals(target.getPackageName())&&!owner.equals("com.android.systemui"))externalWindow=true;
                if(owner.equals(target.getPackageName())&&!root.findAccessibilityNodeInfosByText("هذا حوار عربي").isEmpty())overlayText=true;
                root.recycle();
            }
            if(!externalWindow||!overlayText)throw new AssertionError("Expected external launcher and visible dialogue overlay; external="+externalWindow+", overlay="+overlayText);
            capture("floating-above-home","Verified separate launcher window and visible original Arabic dialogue overlay through accessibility, then captured the actual screen");
        }finally{main(()->target.stopService(stop));SystemClock.sleep(400);launch(BASE+"ui.dashboard.ProjectsActivity",null);}
    }
    private Uri sourceImage()throws Exception{
        Bitmap b=Bitmap.createBitmap(800,15000,Bitmap.Config.ARGB_8888);Canvas c=new Canvas(b);c.drawColor(Color.WHITE);Paint p=new Paint(3);
        for(int y=0;y<15000;y+=750){p.setColor(y%1500==0?0xff161c25:0xff2c3137);c.drawRect(20,y+20,780,y+580,p);p.setColor(0xffa78e56);c.drawCircle(400,y+235,135,p);p.setColor(0xff141920);c.drawCircle(400,y+196,43,p);c.drawRoundRect(new RectF(325,y+239,475,y+480),60,60,p);p.setColor(Color.WHITE);c.drawOval(new RectF(480,y+55,735,y+190),p);p.setColor(Color.BLACK);p.setTextSize(25);p.setTextAlign(Paint.Align.CENTER);c.drawText("PAGE "+(y/750+1),610,y+128,p);}
        ContentValues values=new ContentValues();values.put(MediaStore.Images.Media.DISPLAY_NAME,"Cookies-800x15000.png");values.put(MediaStore.Images.Media.MIME_TYPE,"image/png");values.put(MediaStore.Images.Media.RELATIVE_PATH,"Pictures/CookiesFixtures");
        Uri uri=target.getContentResolver().insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI,values);if(uri==null)throw new AssertionError("Cannot create fixture image");
        try(OutputStream stream=target.getContentResolver().openOutputStream(uri)){if(!b.compress(Bitmap.CompressFormat.PNG,100,stream))throw new AssertionError("Fixture PNG");}finally{b.recycle();}return uri;
    }
    private void pinch(View v)throws Exception{
        int[] pos=new int[2];main(()->v.getLocationOnScreen(pos));float cx=pos[0]+v.getWidth()*.5f,cy=pos[1]+v.getHeight()*.5f;
        MotionEvent.PointerProperties[] props={new MotionEvent.PointerProperties(),new MotionEvent.PointerProperties()};for(int i=0;i<2;i++){props[i].id=i;props[i].toolType=MotionEvent.TOOL_TYPE_FINGER;}
        long start=SystemClock.uptimeMillis();
        for(int step=0;step<20;step++){int count=step==0?1:2;int action=step==0?MotionEvent.ACTION_DOWN:step==1?(MotionEvent.ACTION_POINTER_DOWN|(1<<MotionEvent.ACTION_POINTER_INDEX_SHIFT)):step==18?(MotionEvent.ACTION_POINTER_UP|(1<<MotionEvent.ACTION_POINTER_INDEX_SHIFT)):step==19?MotionEvent.ACTION_UP:MotionEvent.ACTION_MOVE;if(step==19)count=1;
            MotionEvent.PointerCoords[] coords=new MotionEvent.PointerCoords[count];float radius=35+Math.min(step,17)*4;for(int i=0;i<count;i++){coords[i]=new MotionEvent.PointerCoords();coords[i].x=cx+(i==0?-radius:radius);coords[i].y=cy;coords[i].pressure=1;coords[i].size=1;}
            MotionEvent e=MotionEvent.obtain(start,SystemClock.uptimeMillis(),action,count,props,coords,0,0,1,1,0,0,InputDevice.SOURCE_TOUCHSCREEN,0);sendPointerSync(e);e.recycle();SystemClock.sleep(18);
        }SystemClock.sleep(200);
    }
    private void drawStroke(View v)throws Exception{
        int[] pos=new int[2];main(()->v.getLocationOnScreen(pos));float x=pos[0]+v.getWidth()*.35f,y=pos[1]+v.getHeight()*.4f;long start=SystemClock.uptimeMillis();
        for(int i=0;i<20;i++){int action=i==0?MotionEvent.ACTION_DOWN:i==19?MotionEvent.ACTION_UP:MotionEvent.ACTION_MOVE;MotionEvent e=MotionEvent.obtain(start,SystemClock.uptimeMillis(),action,x+i*5,y+(float)Math.sin(i*.22)*25,0);sendPointerSync(e);e.recycle();SystemClock.sleep(12);}SystemClock.sleep(200);
    }
    private float[] matrix(Object canvas)throws Exception{Matrix m=(Matrix)call(canvas,"getCurrentMatrix");float[] result=new float[9];m.getValues(result);return result;}
    private void closePanels(Activity editor)throws Exception{Object panels=field(editor,"panelsController");main(()->{try{call(panels,"closeAllPanels");}catch(Exception e){throw new RuntimeException(e);}});}
    private void editorWorkflow()throws Exception{
        Uri image=sourceImage();Intent importIntent=new Intent(Intent.ACTION_SEND);importIntent.setType("image/png");importIntent.putExtra(Intent.EXTRA_STREAM,image);launch(BASE+"ui.dashboard.ProjectsActivity",importIntent);
        long until=SystemClock.uptimeMillis()+25000;View thumb=null;while(SystemClock.uptimeMillis()<until){if(top.getClass().getName().endsWith("ProjectGalleryActivity"))break;thumb=view("iv_project_thumb");if(thumb!=null)break;SystemClock.sleep(200);}capture("project-imported","Imported synthetic 800x15000 PNG through ACTION_SEND");
        if(!top.getClass().getName().endsWith("ProjectGalleryActivity")){if(thumb==null)throw new AssertionError("Imported project absent");final View card=thumb;main(()->{View x=card;while(x!=null&&!x.isClickable())x=x.getParent() instanceof View?(View)x.getParent():null;if(x==null)throw new AssertionError("Project card not clickable");x.performClick();});}
        waitActivity("ProjectGalleryActivity");SystemClock.sleep(700);capture("project-gallery","Project page thumbnails and operations");
        View page=view("iv_page_thumb");if(page==null)throw new AssertionError("Imported page absent");main(()->{View x=page;while(x!=null&&!x.isClickable())x=x.getParent() instanceof View?(View)x.getParent():null;if(x==null)throw new AssertionError("Page card not clickable");x.performClick();});waitActivity("EditorActivity");SystemClock.sleep(1400);
        Activity editor=top;Object manager=field(editor,"pageManager");projectId=(String)call(manager,"getProjectId");pageId=(String)call(manager,"getPageId");capture("editor-long-image","Long source loaded in original canvas");
        View canvas=view("editorCanvas");float[] before=matrix(canvas);pinch(canvas);float[] after=matrix(canvas);if(Arrays.equals(before,after))throw new AssertionError("Pinch did not change canvas matrix");capture("editor-pinch-zoom","Two-finger pointer sequence changed canvas transform");
        float fitWidth=(canvas.getWidth()-24f)/((Number)call(canvas,"getActualBgWidth")).floatValue();
        for(int n=0;n<6&&Math.abs(matrix(canvas)[Matrix.MSCALE_X])<fitWidth;n++)pinch(canvas);
        if(Math.abs(matrix(canvas)[Matrix.MSCALE_X])<fitWidth*.8f)throw new AssertionError("Pinch did not reach page reading width");
        capture("editor-reading-zoom","Repeated real pinch gestures to inspect a section of the long page");
        click("btnToolText");capture("text-added","Clicked original add-text action");
        if(view("etInlineInput")==null)throw new AssertionError("Inline text editor missing");set("etInlineInput","كوكيز إيدتور\nاختبار حوار عربي");keyboard();capture("arabic-text","Entered Arabic text in original editor");
        attempt("text-size-control",()->{Object layer=call(canvas,"getActiveLayer");float size=((Number)call(layer,"getFontSize")).floatValue();click("btnToolFormat");click("btnSizePlus");float changed=((Number)call(layer,"getFontSize")).floatValue();if(changed<=size)throw new AssertionError("Font size control did not change model");capture("text-size-increased","Increased font size through original UI and verified model change");number("etSize","48");number("etZoom","100");number("etWidth","400");if(Math.abs(((Number)call(layer,"getFontSize")).floatValue()-48)>0.1f||Math.abs(((Number)call(layer,"getScaleX")).floatValue()-1)>0.01f)throw new AssertionError("Numeric format fields did not commit to the model");capture("text-readable-size","Committed size 48, scale 100 percent and text box width through original IME Done actions; verified model");closePanels(editor);});
        for(String[] panel:new String[][]{{"btnToolFont","font"},{"btnToolFormat","format"},{"btnToolColor","color"},{"btnToolStroke","stroke"},{"btnToolBackground","background"},{"btnToolShadow","shadow"},{"btnToolPosition","position"},{"btnToolSpacing","spacing"},{"btnTool3DRotate","3d"},{"btnToolPerspective","perspective"},{"btnToolEffects","effects"},{"btnToolTexture","texture"},{"btnToolOpacity","opacity"},{"btnToolStyles","styles"},{"btnToolEraser","eraser"}}){
            attempt("panel-"+panel[1],()->{top=editor;click(panel[0]);capture("text-panel-"+panel[1],"Opened original text control panel; panel capture is not exhaustive parameter verification");closePanels(editor);});
        }
        attempt("drawing-stroke",()->{top=editor;click("btnInlineDrawMode");capture("drawing-tools","Entered original drawing mode");drawStroke(view("editorCanvas"));capture("drawing-stroke","Injected stroke pointer sequence");click("btnDrawDone");});
        attempt("layers",()->{top=editor;click("btnLayers");capture("layers","Opened original layers dialog");back();});
        attempt("image-drafts",()->{top=editor;click("btnToolImageDrafts");capture("image-drafts","Opened original local image drafts dialog");back();});
        attempt("canvas-resize-dialog",()->{top=editor;click("btnToolResizeBg");capture("canvas-resize-dialog","Opened original resize settings without modifying source dimensions");back();});
        attempt("background-crop",()->{top=editor;click("btnToolCropBg");waitActivity("ImageCropActivity");capture("background-crop","Opened actual crop activity for the long source");back();waitActivity("EditorActivity");});
        attempt("shapes-picker",()->{top=editor;click("btnToolShapes");capture("shapes-picker","Opened original shape picker");back();});
        attempt("undo-redo",()->{top=editor;click("btnUndo");capture("undo","Original undo button");click("btnRedo");capture("redo","Original redo button");});
        attempt("project-round-trip",()->{
            top=editor;main(()->{try{call(manager,"saveCurrentPage");}catch(Exception e){throw new RuntimeException(e);}});SystemClock.sleep(1000);
            Intent i=new Intent().putExtra("PROJECT_ID",projectId).putExtra("PAGE_ID",pageId);main(()->editor.finish());launch(BASE+"ui.editor.EditorActivity",i);SystemClock.sleep(1400);
            Object reopened=view("editorCanvas");Object layers=call(reopened,"getLayersList");if(!(layers instanceof List)||((List<?>)layers).isEmpty())throw new AssertionError("Saved layers absent after reopening");
            boolean arabic=false;for(Object l:(List<?>)layers)if(l.getClass().getName().endsWith("TextLayer")&&String.valueOf(call(l,"getTextContent")).contains("كوكيز"))arabic=true;if(!arabic)throw new AssertionError("Arabic text absent after reopening");capture("project-reopened","Saved project reopened with Arabic text layers");
        });
        attempt("export-studio",()->{Intent i=new Intent().putExtra("PROJECT_ID",projectId);launch(BASE+"ui.editor.ExportStudioActivity",i);capture("export-studio","Original page selection and export settings");});
        attempt("png-export-through-ui",()->{
            click("btnSelectAll");capture("export-selected","Selected project pages for lossless export");
            long previous=0;try(android.database.Cursor c=target.getContentResolver().query(MediaStore.Images.Media.EXTERNAL_CONTENT_URI,new String[]{"_id"},null,null,"_id DESC")){if(c!=null&&c.moveToFirst())previous=c.getLong(0);}
            click("btnStartExport");long deadline=SystemClock.uptimeMillis()+35000;Uri exported=null;
            while(SystemClock.uptimeMillis()<deadline){try(android.database.Cursor c=target.getContentResolver().query(MediaStore.Images.Media.EXTERNAL_CONTENT_URI,new String[]{"_id","mime_type"},"_id > ?",new String[]{String.valueOf(previous)},"_id DESC")){if(c!=null)while(c.moveToNext())if("image/png".equals(c.getString(1))){exported=ContentUris.withAppendedId(MediaStore.Images.Media.EXTERNAL_CONTENT_URI,c.getLong(0));break;}}if(exported!=null)break;SystemClock.sleep(200);}
            if(exported==null)throw new AssertionError("No exported PNG appeared in MediaStore");SystemClock.sleep(900);BitmapFactory.Options options=new BitmapFactory.Options();options.inJustDecodeBounds=true;try(InputStream stream=target.getContentResolver().openInputStream(exported)){BitmapFactory.decodeStream(stream,null,options);}if(options.outWidth!=800||options.outHeight!=15000)throw new AssertionError("UI export changed dimensions: "+options.outWidth+"x"+options.outHeight);capture("png-export-finished","Actual PNG export to gallery retained 800x15000 dimensions");
        });
        attempt("reader",()->{ArrayList<Uri> images=new ArrayList<>();images.add(image);Intent i=new Intent().putParcelableArrayListExtra("IMAGES_URIS",images);launch(BASE+"ui.editor.ReaderActivity",i);capture("reader","Original reader with fixture image");});
    }
    private void screen(String suffix,String name)throws Exception{attempt(name,()->{launch(BASE+suffix,null);capture(name,"Opened activity with local services mode");});}
    private void zip()throws Exception{
        File archive=new File(out.getParentFile(),"cookies-ui-evidence.zip");try(ZipOutputStream z=new ZipOutputStream(new FileOutputStream(archive))){File[] files=out.listFiles();if(files==null)return;Arrays.sort(files);byte[] buffer=new byte[32768];for(File f:files){if(!f.isFile())continue;z.putNextEntry(new ZipEntry(f.getName()));try(FileInputStream s=new FileInputStream(f)){int n;while((n=s.read(buffer))>0)z.write(buffer,0,n);}z.closeEntry();}}
        publish(archive);
    }
    @Override public void onStart(){
        Bundle result=new Bundle();int code=Activity.RESULT_OK;
        try{
            target=getTargetContext();loader=target.getClassLoader();out=new File(target.getExternalFilesDir(null),"ui-evidence");if(!out.mkdirs()&&!out.isDirectory())throw new IOException("Evidence directory");
            ((Application)target.getApplicationContext()).registerActivityLifecycleCallbacks(new Application.ActivityLifecycleCallbacks(){public void onActivityCreated(Activity a,Bundle b){}public void onActivityStarted(Activity a){}public void onActivityResumed(Activity a){top=a;}public void onActivityPaused(Activity a){}public void onActivityStopped(Activity a){}public void onActivitySaveInstanceState(Activity a,Bundle b){}public void onActivityDestroyed(Activity a){}});
            // Suppress tutorial popovers only in the test fixture to expose tools reliably.
            target.getSharedPreferences("AshtyperPrefs",0).edit().putBoolean("TUTORIAL_DASHBOARD",true).putBoolean("TUTORIAL_FLOATING",true).apply();
            attempt("network-isolated",()->{if(target.getPackageManager().checkPermission("android.permission.INTERNET",target.getPackageName())!=PackageManager.PERMISSION_DENIED)throw new AssertionError("Network permission still granted");});
            attempt("local-account-authentication",()->accountChecks());
            launch("com.cookies.editor.local.WelcomeActivity",null);SystemClock.sleep(1200);capture("welcome","Actual redesigned welcome screen after entrance animation");SystemClock.sleep(900);capture("welcome-motion","Second actual animation frame");
            attempt("welcome-email-entry",()->{final Activity a=top;main(()->a.findViewById(0xc005).performClick());Dialog dialog=(Dialog)field(a,"authDialog");SystemClock.sleep(350);capture("local-sign-in-sheet","Opened the local email/password bottom sheet");main(()->{((EditText)dialog.findViewById(0xc001)).setText("ui-fixture@example.invalid");((EditText)dialog.findViewById(0xc002)).setText("cookies-ui-3481");dialog.findViewById(0xc003).requestRectangleOnScreen(new Rect(0,0,300,60),true);});capture("local-sign-up","Local email/password form; password masked");main(()->dialog.findViewById(0xc003).performClick());waitActivity("ProjectsActivity");capture("dashboard","Reached original dashboard through local profile");});
            attempt("editor-workflow",()->editorWorkflow());
            attempt("native-smart-cleaner",()->cleanerCheck());
            screen("ui.library.FontLibraryActivity","font-library");screen("ui.library.ArabicFontsActivity","arabic-fonts");screen("ui.library.EnglishFontsActivity","english-fonts");screen("ui.library.ImportedFontsActivity","imported-fonts");screen("ui.floatingwidget.FolatingWidgetDashboard","floating-assistant");screen("ui.settings.SettingsActivity","settings-local-profile");
            attempt("tag-mini-editor",()->{Object tag=type(BASE+"ui.settings.TagItem").getConstructor(String.class,String.class,String.class,String.class).newInstance("حوار عربي","#حوار","#FFFFFF","bein_normal.ttf");Intent i=new Intent().putExtra("TAG_ITEM_DATA",(Serializable)tag);launch(BASE+"ui.editor.TagMiniEditorActivity",i);capture("tag-mini-editor","Opened original mini editor with a local tag");});
            attempt("floating-window-service",()->overlayCheck());
            screen("ui.dashboard.StoreActivity","store-disabled-preview");screen("ui.community.CommunityActivity","community-disabled-preview");screen("ui.community.CreatePostActivity","create-post-disabled-preview");screen("ui.dashboard.WebtoonScraperActivity","webtoon-disabled-preview");
            attempt("guest-entry",()->{Intent reset=new Intent().addFlags(Intent.FLAG_ACTIVITY_CLEAR_TASK);launch("com.cookies.editor.local.WelcomeActivity",reset);Activity a=top;main(()->a.findViewById(0xc004).performClick());waitActivity("ProjectsActivity");capture("guest-dashboard","Guest opened original local dashboard after resetting navigation task");});
        }catch(Throwable e){try{checks.put(new JSONObject().put("name","runner").put("status","fail").put("error",e.toString()));}catch(Exception ignored){}code=Activity.RESULT_CANCELED;}
        finally{
            try{int failures=0;for(int i=0;i<checks.length();i++)if("fail".equals(checks.getJSONObject(i).getString("status")))failures++;if(failures>0)code=Activity.RESULT_CANCELED;
                JSONObject report=new JSONObject().put("status",failures==0?"pass":"fail").put("screenshots",steps.length()).put("steps",steps).put("checks",checks).put("limitations",new JSONArray(Arrays.asList("Offline service screens are original-layout previews, not functioning cloud services","UI actions plus selected end-to-end flows; not every combination of properties","Paid feature entitlement checks retained","Original ARM OpenCV cleaner tested through emulator native translation; not every native operation","No iPhone application tested")));
                write("ui-verification.json",report.toString(2));zip();result.putString("report",new File(out,"ui-verification.json").getAbsolutePath());result.putInt("screenshots",steps.length());result.putInt("failures",failures);
            }catch(Exception e){result.putString("error",e.toString());code=Activity.RESULT_CANCELED;}finish(code,result);
        }
    }
}
