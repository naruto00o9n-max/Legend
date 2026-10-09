package com.cookies.editor.fixtures;

import android.app.Instrumentation;
import android.content.Context;
import android.content.ContentValues;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Typeface;
import android.os.Bundle;
import android.os.Build;
import android.net.Uri;
import android.provider.MediaStore;
import android.text.TextPaint;
import java.io.File;
import java.io.FileOutputStream;
import java.io.FileInputStream;
import java.io.OutputStream;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.Locale;
import java.util.Random;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;
import org.json.JSONArray;
import org.json.JSONObject;

/** Calls the APK's actual recovered engine, not JADX's incomplete Java stubs.
 * No login, entitlement or server methods are modified or called by this test.
 * It records golden renderings and actual font metrics for the iOS port.
 */
public final class FixtureRunner extends Instrumentation {
    private static final String BASE="com.oneguystudio.ytyper.";
    private Context target;
    private File output;
    private ClassLoader loader;
    private Class<?> textClass, layerClass;
    private Object renderer;
    private Method draw;
    private final JSONArray renderCases=new JSONArray();

    @Override public void onCreate(Bundle arguments) { super.onCreate(arguments); start(); }
    private Class<?> type(String name)throws Exception {return Class.forName(BASE+name,true,loader);}
    private void property(Object value,String name,Object field)throws Exception {
        Field f=value.getClass().getDeclaredField(name);f.setAccessible(true);f.set(value,field);
    }
    private Object enumValue(String name,String value)throws Exception {
        Class<?> cls=type("data.model."+name);
        for(Object item:cls.getEnumConstants())if(((Enum<?>)item).name().equals(value))return item;
        throw new IllegalArgumentException("Unknown reference enum: "+name+":"+value);
    }
    private JSONObject properties(Object model)throws Exception {
        JSONObject result=new JSONObject();
        for(Field field:model.getClass().getDeclaredFields()){
            if(Modifier.isStatic(field.getModifiers())||Modifier.isTransient(field.getModifiers()))continue;
            field.setAccessible(true);Object value=field.get(model);
            result.put(field.getName(),value instanceof Enum<?>?((Enum<?>)value).name():JSONObject.wrap(value));
        }
        return result;
    }
    private void saveJson(String name,Object value)throws Exception {
        try(FileOutputStream stream=new FileOutputStream(new File(output,name))){stream.write(value.toString().getBytes(StandardCharsets.UTF_8));}
    }
    private void render(String name,Object layer,boolean requireNonempty)throws Exception {
        for(Field field:renderer.getClass().getDeclaredFields()){
            if(field.getType()==Random.class){field.setAccessible(true);((Random)field.get(renderer)).setSeed(1337L);}
        }
        Bitmap bitmap=Bitmap.createBitmap(512,512,Bitmap.Config.ARGB_8888);
        try {
            Canvas canvas=new Canvas(bitmap);canvas.drawColor(Color.WHITE);
            draw.invoke(renderer,canvas,layer);
            if(requireNonempty){
                int[] pixels=new int[512*512];bitmap.getPixels(pixels,0,512,0,0,512,512);
                boolean changed=false;for(int pixel:pixels)if(pixel!=Color.WHITE){changed=true;break;}
                if(!changed)throw new AssertionError("Reference renderer produced an empty baseline: "+name);
            }
            try(FileOutputStream stream=new FileOutputStream(new File(output,name+".png"))){
                if(!bitmap.compress(Bitmap.CompressFormat.PNG,100,stream))throw new AssertionError("PNG write failed");
            }
            renderCases.put(new JSONObject().put("file",name+".png").put("noiseSeed",1337).put("model",properties(layer)));
        }finally{bitmap.recycle();}
    }
    private Object text(String font)throws Exception {
        Object value=textClass.getConstructor().newInstance();
        property(value,"textContent","هذا حوار عربي\nداخل فقاعة المانهوا");
        property(value,"fontPath",font);property(value,"typeface",Typeface.createFromAsset(target.getAssets(),"fonts/"+font));
        property(value,"fontSize",32f);property(value,"boxWidth",260f);
        property(value,"x",256f);property(value,"y",256f);
        property(value,"color",Color.BLACK);property(value,"opacity",255);
        property(value,"isVisible",true);property(value,"scaleX",1f);property(value,"scaleY",1f);
        return value;
    }
    private void fontFixtures()throws Exception {
        String[] names=target.getAssets().list("fonts");if(names==null||names.length==0)throw new AssertionError("Original fonts absent");
        Arrays.sort(names);JSONArray fixtures=new JSONArray();
        Class<?> engine=type("engine.TextLayoutEngine");
        Method box=engine.getMethod("formatForBox",String.class,Typeface.class,float.class,int.class,boolean.class);
        Method circle=engine.getMethod("formatForCircle",String.class,Typeface.class,float.class,int.class);
        String[] texts={"هذا حوار عربي داخل فقاعة طويلة للتحقق من توزيع الكلمات","براءة وتآزر وتوازن الكلمات","one two three four five six seven eight nine ten","زيوس","بِسْمِ اللهِ الرَّحْمٰنِ الرَّحِيمِ"};
        for(String name:names){
            Typeface face=Typeface.createFromAsset(target.getAssets(),"fonts/"+name);
            TextPaint paint=new TextPaint(TextPaint.ANTI_ALIAS_FLAG);paint.setTextSize(32f);paint.setTypeface(face);
            for(String content:texts){
                JSONObject item=new JSONObject().put("font",name).put("text",content).put("fontSize",32).put("width",240)
                    .put("androidMeasuredWidth",paint.measureText(content))
                    .put("box",box.invoke(null,content,face,32f,240,false))
                    .put("boxTatweel",box.invoke(null,content,face,32f,240,true))
                    .put("circle",circle.invoke(null,content,face,32f,240));
                fixtures.put(item);
            }
        }
        saveJson("typography-android.json",fixtures);
        for(String name:new String[]{"hayah.ttf","hacen_samra_lt.ttf","hsn_sadiyah.ttf","boahmed_alhour.ttf"}) {
            if(Arrays.asList(names).contains(name))render("text-font-"+name.replace('.','-'),text(name),true);
        }
    }
    private void styleFixtures()throws Exception {
        String[] effects={"NONE","BLUR","GLITCH","SLICE","NEON","FADE","WARP","SHADOW","ERROR"};
        for(String effect:effects){
            Object value=text("hayah.ttf");property(value,"effectType",enumValue("TextEffect",effect));
            property(value,"effectValue",4f);property(value,"effectColor",0xFFD4AF37);
            render("effect-"+effect.toLowerCase(Locale.ROOT),value,effect.equals("NONE"));
        }
        Object stroke=text("hayah.ttf");property(stroke,"strokeWidth",3f);property(stroke,"strokeColor",0xFFFF7043);render("text-stroke",stroke,true);
        Object shadow=text("hayah.ttf");property(shadow,"shadowRadius",4f);property(shadow,"shadowDx",7f);property(shadow,"shadowDy",8f);property(shadow,"shadowColor",0xFF283593);property(shadow,"shadowAlpha",180);render("text-shadow",shadow,true);
        Object background=text("hayah.ttf");property(background,"backgroundColor",0xFFFFE082);property(background,"backgroundAlpha",255);property(background,"backgroundCornerRadius",10f);property(background,"backgroundPaddingX",8f);property(background,"backgroundPaddingY",8f);render("text-background",background,true);
        Object spacing=text("hayah.ttf");property(spacing,"letterSpacing",0.1f);property(spacing,"lineSpacing",10f);render("text-spacing",spacing,true);
        Object rotated=text("hayah.ttf");property(rotated,"rotation",25f);property(rotated,"scaleX",1.4f);property(rotated,"scaleY",0.8f);render("text-rotated",rotated,true);
        Object gradient=text("hayah.ttf");property(gradient,"textGradient",Arrays.asList(0xFF1565C0,0xFFD4AF37));property(gradient,"textGradientAngle",45f);render("text-gradient",gradient,true);
    }
    private void shapeFixtures()throws Exception {
        Class<?> cls=type("data.model.ShapeLayer"),shapes=type("data.model.ShapeType");
        for(Object shape:shapes.getEnumConstants()){
            Object value=cls.getConstructor().newInstance();property(value,"shapeType",shape);
            property(value,"baseWidth",180f);property(value,"baseHeight",140f);property(value,"x",256f);property(value,"y",256f);
            property(value,"color",0xFFD4AF37);property(value,"opacity",255);property(value,"isVisible",true);property(value,"scaleX",1f);property(value,"scaleY",1f);
            render("shape-"+((Enum<?>)shape).name().toLowerCase(Locale.ROOT),value,true);
        }
    }
    private int sourcePixel(int x,int y) {
        return Color.rgb((x*17+y*31)&255,(x*7+y*13)&255,(x*3+y*19)&255);
    }
    private void saveBitmap(String name,Bitmap bitmap)throws Exception {
        try(FileOutputStream stream=new FileOutputStream(new File(output,name))){
            if(!bitmap.compress(Bitmap.CompressFormat.PNG,100,stream))throw new AssertionError("PNG export failed: "+name);
        }
    }
    private void verifyLongImage(Bitmap bitmap,boolean edited)throws Exception {
        if(bitmap==null)throw new AssertionError("Original export returned null for 800x15000");
        if(bitmap.getWidth()!=800||bitmap.getHeight()!=15000)throw new AssertionError("Original export changed source dimensions");
        int[] row=new int[800];long changed=0;
        for(int y=0;y<15000;y++){
            bitmap.getPixels(row,0,800,0,y,800,1);
            for(int x=0;x<800;x++)if(row[x]!=sourcePixel(x,y)){
                if(!edited||y<14000)throw new AssertionError("Original export changed an unedited pixel at "+x+","+y);
                changed++;
            }
        }
        if(edited&&changed==0)throw new AssertionError("Text near the bottom was lost during long-image export");
    }
    private void longImageFixtures()throws Exception {
        File source=new File(output,"long-source.png");
        Bitmap original=Bitmap.createBitmap(800,15000,Bitmap.Config.ARGB_8888);
        try {
            int[] row=new int[800];
            for(int y=0;y<15000;y++){
                for(int x=0;x<800;x++)row[x]=sourcePixel(x,y);
                original.setPixels(row,0,800,0,y,800,1);
            }
            saveBitmap(source.getName(),original);
        }finally{original.recycle();}
        Class<?> configClass=type("data.model.CanvasConfig"),pageClass=type("data.model.PageState"),exportClass=type("engine.YTyperExportEngine");
        Object config=configClass.getConstructor().newInstance();
        property(config,"width",800);property(config,"height",15000);property(config,"backgroundImagePath",source.getAbsolutePath());
        Object page=pageClass.getConstructor().newInstance();property(page,"canvasConfig",config);
        Object exporter=exportClass.getConstructor(Context.class).newInstance(target);
        Method export=exportClass.getMethod("renderPageToBitmap",pageClass,String.class);
        property(page,"layers",java.util.Collections.emptyList());
        Bitmap unchanged=(Bitmap)export.invoke(exporter,page,"PNG");
        try{verifyLongImage(unchanged,false);saveBitmap("long-unchanged.png",unchanged);}finally{if(unchanged!=null)unchanged.recycle();}
        Bitmap decoded=BitmapFactory.decodeFile(new File(output,"long-unchanged.png").getAbsolutePath());
        try{verifyLongImage(decoded,false);}finally{if(decoded!=null)decoded.recycle();}
        Object bottom=text("hayah.ttf");property(bottom,"textContent","آخر الصورة — اختبار عربي");property(bottom,"x",400f);property(bottom,"y",14740f);property(bottom,"boxWidth",500f);
        property(page,"layers",java.util.Collections.singletonList(bottom));
        Bitmap edited=(Bitmap)export.invoke(exporter,page,"PNG");
        try{verifyLongImage(edited,true);saveBitmap("long-bottom-text.png",edited);}finally{if(edited!=null)edited.recycle();}
        saveJson("long-image-verification.json",new JSONObject().put("result","pass").put("width",800).put("height",15000)
            .put("unchangedPixelsCompared",12000000).put("opaqueRGB",true).put("decodedPNGVerified",true)
            .put("bottomTextVerified",true).put("renderer","actual original renderPageToBitmap")
            .put("doesNotVerify","Transparent hidden RGB, JPEG, PSD, editor gestures or iPhone runtime"));
    }
    private void publishFixtures()throws Exception {
        // Android scoped storage prevents the adb shell from reading app-private
        // external files. Publish our own test output through the supported API.
        ContentValues values=new ContentValues();
        values.put(MediaStore.MediaColumns.DISPLAY_NAME,"cookies-reference-fixtures.zip");
        values.put(MediaStore.MediaColumns.MIME_TYPE,"application/zip");
        values.put(MediaStore.MediaColumns.RELATIVE_PATH,"Download/Cookies-reference");
        values.put(MediaStore.MediaColumns.IS_PENDING,1);
        Uri uri=target.getContentResolver().insert(MediaStore.Downloads.EXTERNAL_CONTENT_URI,values);
        if(uri==null)throw new IllegalStateException("Cannot publish test fixtures");
        boolean complete=false;
        try {
            try(OutputStream stream=target.getContentResolver().openOutputStream(uri);ZipOutputStream zip=new ZipOutputStream(stream)){
                File[] files=output.listFiles();if(files==null)throw new IllegalStateException("Fixtures missing");
                byte[] buffer=new byte[16384];
                for(File file:files){
                    if(!file.isFile())continue;
                    zip.putNextEntry(new ZipEntry(file.getName()));
                    try(FileInputStream input=new FileInputStream(file)){
                        int count;while((count=input.read(buffer))!=-1)zip.write(buffer,0,count);
                    }
                    zip.closeEntry();
                }
            }
            values.clear();values.put(MediaStore.MediaColumns.IS_PENDING,0);target.getContentResolver().update(uri,values,null,null);complete=true;
        }finally{if(!complete)target.getContentResolver().delete(uri,null,null);}
    }
    @Override public void onStart() {
        Bundle result=new Bundle();
        try {
            Locale.setDefault(new Locale("ar"));target=getTargetContext();loader=target.getClassLoader();
            output=new File(target.getExternalFilesDir(null),"reference-fixtures");if(!output.mkdirs()&&!output.isDirectory())throw new IllegalStateException("Fixture storage unavailable");
            textClass=type("data.model.TextLayer");layerClass=type("data.model.LayerData");Class<?> rendering=type("engine.LayerRendererEngine");
            renderer=rendering.getConstructor().newInstance();draw=rendering.getMethod("renderLayer",Canvas.class,layerClass);
            fontFixtures();styleFixtures();shapeFixtures();saveJson("render-manifest.json",renderCases);longImageFixtures();
            saveJson("verification.json",new JSONObject().put("result","pass").put("renderer","actual YTyper 3.8 smali in rebranded reference APK").put("androidApi",Build.VERSION.SDK_INT).put("renderCases",renderCases.length()).put("iosParity","not yet evaluated").put("backendAccess",false));
            publishFixtures();
            result.putString("stream","Reference rendering and font fixtures captured: "+renderCases.length());finish(-1,result);
        }catch(Throwable failure){
            failure.printStackTrace();
            result.putString("stream","REFERENCE FIXTURE FAILURE: "+failure.toString());finish(0,result);
        }
    }
}
