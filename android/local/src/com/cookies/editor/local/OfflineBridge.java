package com.cookies.editor.local;

import android.app.*;
import android.content.*;
import android.graphics.Color;
import android.os.Bundle;
import android.view.*;
import android.widget.*;

/** Narrow local hooks: no subscription unlocks, credentials, or renderer changes. */
public final class OfflineBridge {
    private OfflineBridge() {}
    public static int id(Context c,String name){return c.getResources().getIdentifier(name,"id",c.getPackageName());}
    private static void text(Activity a,String name,String value){View v=a.findViewById(id(a,name));if(v instanceof TextView)((TextView)v).setText(value);}
    public static void unavailable(Context c){Toast.makeText(c,"الخدمة السحابية معطلة مؤقتًا في نسخة التجربة.",Toast.LENGTH_LONG).show();}
    private static void disable(Activity a,String name){View v=a.findViewById(id(a,name));if(v!=null)v.setOnClickListener(view->unavailable(a));}
    public static void install(Application app){
        app.registerActivityLifecycleCallbacks(new Application.ActivityLifecycleCallbacks(){
            @Override public void onActivityCreated(Activity a,Bundle b){}
            @Override public void onActivityStarted(Activity a){}
            @Override public void onActivityResumed(Activity a){
                String name=a.getClass().getName();
                if(name.endsWith("ProjectsActivity"))for(String button:new String[]{"btnDriveSync","btnLeaderboard","btnAnnounce","btnDiscord"})disable(a,button);
                if(name.endsWith("SettingsActivity")){profile(a);for(String button:new String[]{"btn_privacy_policy","btn_contact_us"})disable(a,button);View logout=a.findViewById(id(a,"btn_sign_out"));if(logout!=null)logout.setOnClickListener(v->signOut(a));}
            }
            @Override public void onActivityPaused(Activity a){}
            @Override public void onActivityStopped(Activity a){}
            @Override public void onActivitySaveInstanceState(Activity a,Bundle b){}
            @Override public void onActivityDestroyed(Activity a){}
        });
    }
    public static void profile(Activity a){
        String address=LocalAccounts.email(a);
        text(a,"tv_profile_name","guest".equals(address)?"مساحة التجربة":"ملف محلي");
        text(a,"tvUserEmail","guest".equals(address)?"دون حساب":address);
        text(a,"tvUserAlias","محفوظ على هذا الجهاز");text(a,"tvUserProfileTitle","Cookies Editor");
        text(a,"tv_profile_tier","محلي · مجاني");text(a,"tv_profile_rank","—");text(a,"tv_profile_points","—");
        text(a,"tv_profile_bubbles",String.valueOf(a.getSharedPreferences("DashboardStats",0).getInt("total_bubbles",0)));
    }
    public static void signOut(Activity a){new AlertDialog.Builder(a).setTitle("تسجيل الخروج").setMessage("ستبقى مشاريعك محفوظة على الجهاز.").setNegativeButton("إلغاء",null).setPositiveButton("تسجيل الخروج",(d,which)->{LocalAccounts.signOut(a);Intent i=new Intent(a,WelcomeActivity.class);i.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK|Intent.FLAG_ACTIVITY_CLEAR_TASK);a.startActivity(i);a.finish();}).show();}
    public static void openWelcome(Activity a){a.startActivity(new Intent(a,WelcomeActivity.class));a.finish();}
    public static void servicePreview(Activity a,String layout){
        int resource=a.getResources().getIdentifier(layout,"layout",a.getPackageName());
        FrameLayout frame=new FrameLayout(a);frame.setBackgroundColor(0xff111218);
        if(resource!=0){View original=a.getLayoutInflater().inflate(resource,frame,false);frame.addView(original);disableTree(original);}
        LinearLayout banner=new LinearLayout(a);banner.setOrientation(LinearLayout.VERTICAL);banner.setGravity(Gravity.CENTER);banner.setPadding(24,20,24,20);banner.setBackgroundColor(0xf5111218);
        TextView title=new TextView(a);title.setText("الخدمة معطلة مؤقتًا");title.setTextColor(0xffe7c66b);title.setTextSize(18);title.setGravity(Gravity.CENTER);banner.addView(title);
        TextView note=new TextView(a);note.setText("هذه معاينة الواجهة دون اتصال بخوادم التطبيق الأصلي.");note.setTextColor(0xffc9c4b8);note.setGravity(Gravity.CENTER);banner.addView(note);
        Button back=new Button(a);back.setText("العودة إلى مشاريعي");back.setOnClickListener(v->a.finish());banner.addView(back);
        FrameLayout.LayoutParams p=new FrameLayout.LayoutParams(-1,-2,Gravity.BOTTOM);frame.addView(banner,p);a.setContentView(frame);
    }
    private static void disableTree(View v){v.setEnabled(false);if(v instanceof ProgressBar)v.setVisibility(View.GONE);if(v instanceof ViewGroup)for(int i=0;i<((ViewGroup)v).getChildCount();i++)disableTree(((ViewGroup)v).getChildAt(i));}
}
