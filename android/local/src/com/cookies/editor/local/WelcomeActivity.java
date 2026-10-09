package com.cookies.editor.local;

import android.app.Activity;
import android.app.Dialog;
import android.content.Intent;
import android.graphics.Color;
import android.graphics.Typeface;
import android.graphics.drawable.GradientDrawable;
import android.graphics.drawable.RippleDrawable;
import android.content.res.ColorStateList;
import android.os.Bundle;
import android.text.InputType;
import android.view.Gravity;
import android.view.View;
import android.view.WindowManager;
import android.view.inputmethod.InputMethodManager;
import android.widget.*;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/** Original editor follows this screen; this is the only redesigned screen. */
public final class WelcomeActivity extends Activity {
    public static final int EMAIL_ID = 0xc001, PASSWORD_ID = 0xc002, SUBMIT_ID = 0xc003, GUEST_ID = 0xc004, ENTRY_ID = 0xc005;
    private final int gold = 0xffe7c66b, muted = 0xffa9a69d;
    private EditText email, password;
    private TextView submit, switchMode, error, cardTitle;
    private boolean register = true, busy;
    private MotionArtwork backdrop, hero;
    private ExecutorService worker;
    private Typeface arabic;
    private Dialog authDialog;
    private int dp(float value) { return Math.round(value * getResources().getDisplayMetrics().density); }
    private LinearLayout.LayoutParams lp(int width, int height) { return new LinearLayout.LayoutParams(width < 0 ? width : dp(width), height < 0 ? height : dp(height)); }
    private GradientDrawable box(int fill, int stroke, int radius) {
        GradientDrawable d = new GradientDrawable(); d.setColor(fill); d.setCornerRadius(dp(radius));
        if (stroke != 0) d.setStroke(dp(1), stroke); return d;
    }
    private TextView text(String s, int size, int color, boolean bold) {
        TextView t = new TextView(this); t.setText(s); t.setTextSize(size); t.setTextColor(color);
        t.setIncludeFontPadding(false);
        t.setTypeface(arabic, bold ? Typeface.BOLD : Typeface.NORMAL); t.setGravity(Gravity.CENTER);
        t.setLayoutDirection(View.LAYOUT_DIRECTION_RTL); return t;
    }
    private TextView button(String label, boolean primary) {
        TextView t = text(label, 15, primary ? 0xff16120a : gold, true);
        GradientDrawable d = primary ? new GradientDrawable(GradientDrawable.Orientation.LEFT_RIGHT,
                new int[] {0xffc49c3c, 0xfff6dfa0, 0xffd8b452}) : box(0x161e1c15, 0x4dba9b52, 18);
        d.setCornerRadius(dp(18));
        t.setBackground(new RippleDrawable(ColorStateList.valueOf(primary ? 0x44ffffff : 0x44d4af37), d, null));
        t.setElevation(dp(primary ? 7 : 0)); t.setClickable(true); t.setFocusable(true);
        t.setContentDescription(label); return t;
    }
    @Override public void onCreate(Bundle saved) {
        super.onCreate(saved);
        getWindow().setStatusBarColor(0xff090a0c); getWindow().setNavigationBarColor(0xff090a0c);
        getWindow().setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_ADJUST_RESIZE);
        try { arabic = Typeface.createFromAsset(getAssets(), "fonts/bein_normal.ttf"); } catch (Exception e) { arabic = Typeface.DEFAULT; }
        worker = Executors.newSingleThreadExecutor();
        FrameLayout root = new FrameLayout(this); root.setBackgroundColor(0xff090a0c);
        backdrop = new MotionArtwork(this, false); root.addView(backdrop, new FrameLayout.LayoutParams(-1, -1));
        ScrollView scroll = new ScrollView(this); scroll.setFillViewport(true); scroll.setClipToPadding(false); scroll.setVerticalScrollBarEnabled(false);
        root.addView(scroll, new FrameLayout.LayoutParams(-1, -1));
        LinearLayout body = new LinearLayout(this); body.setOrientation(LinearLayout.VERTICAL); body.setGravity(Gravity.CENTER_HORIZONTAL);
        body.setPadding(dp(24), dp(16), dp(24), dp(16)); scroll.addView(body, new ScrollView.LayoutParams(-1, -2));
        LinearLayout brand = new LinearLayout(this); brand.setGravity(Gravity.CENTER_VERTICAL); brand.setLayoutDirection(View.LAYOUT_DIRECTION_LTR);
        ImageView logo = new ImageView(this);
        try { logo.setImageDrawable(android.graphics.drawable.Drawable.createFromStream(getAssets().open("cookies/welcome-logo.png"), null)); } catch (Exception ignored) {}
        logo.setContentDescription("شعار كوكيز إيدتور"); brand.addView(logo, lp(46, 46));
        LinearLayout names = new LinearLayout(this); names.setOrientation(LinearLayout.VERTICAL); names.setPadding(dp(9),0,0,0);
        TextView wordmark = text("COOKIES", 18, 0xfff8f1df, true); wordmark.setTypeface(Typeface.create("sans-serif-medium", Typeface.NORMAL)); wordmark.setLetterSpacing(.15f); wordmark.setGravity(Gravity.LEFT);
        names.addView(wordmark); TextView edition = text("EDITOR", 10, gold, false); edition.setLetterSpacing(.35f); edition.setGravity(Gravity.LEFT); names.addView(edition); brand.addView(names);
        TextView badge = text("نسخة التجربة", 10, gold, false); badge.setPadding(dp(10),dp(5),dp(10),dp(5)); badge.setBackground(box(0x20d4af37,0x3dc7a347,30));
        LinearLayout.LayoutParams badgeLp=lp(-2,-2); badgeLp.leftMargin=dp(20); brand.addView(badge,badgeLp); body.addView(brand,lp(-2,46));
        hero = new MotionArtwork(this, true); hero.setContentDescription("معاينة فنية متحركة لصفحة مانهوا وطبقات النص والرسم");
        float screenDp=getResources().getDisplayMetrics().heightPixels/getResources().getDisplayMetrics().density;
        int heroHeight=Math.max(136,Math.min(220,(int)screenDp-510));
        LinearLayout.LayoutParams heroLp=lp(-1,heroHeight); heroLp.topMargin=dp(10); body.addView(hero,heroLp);
        TextView headline = text("امنح كل حوار\nبصمتك الخاصة.", 27, 0xfff7f1e5, true); headline.setLineSpacing(36*getResources().getDisplayMetrics().scaledDensity-headline.getPaint().getFontSpacing(),1f); body.addView(headline,lp(-1,-2));
        TextView subtitle=text("مساحتك للنصوص والطبقات والإبداع",13,muted,false); LinearLayout.LayoutParams subLp=lp(-1,-2);subLp.topMargin=dp(4);body.addView(subtitle,subLp);
        LinearLayout chips = new LinearLayout(this); chips.setGravity(Gravity.CENTER); chips.setLayoutDirection(View.LAYOUT_DIRECTION_RTL);
        for(String s:new String[]{"صور طويلة","خطوط عربية","طبقات"}) {
            TextView chip=text(s,10,0xffc6bfae,false); chip.setPadding(dp(10),dp(5),dp(10),dp(5));chip.setBackground(box(0x80191b20,0x28ffffff,20));
            LinearLayout.LayoutParams cl=lp(-2,-2);cl.setMargins(dp(3),0,dp(3),0);chips.addView(chip,cl);
        }
        LinearLayout.LayoutParams chipsLp=lp(-1,-2);chipsLp.topMargin=dp(12);chipsLp.bottomMargin=dp(18);body.addView(chips,chipsLp);
        LinearLayout card=new LinearLayout(this);card.setOrientation(LinearLayout.VERTICAL);card.setPadding(dp(18),dp(18),dp(18),dp(16));
        card.setBackground(box(0xf515161a,0x65bf9d50,24));card.setElevation(dp(10));
        authDialog=new Dialog(this);authDialog.requestWindowFeature(android.view.Window.FEATURE_NO_TITLE);
        ScrollView authScroll=new ScrollView(this);authScroll.setFillViewport(false);authScroll.setPadding(dp(16),dp(12),dp(16),dp(16));authScroll.addView(card,new ScrollView.LayoutParams(-1,-2));authDialog.setContentView(authScroll);
        authDialog.getWindow().setBackgroundDrawableResource(android.R.color.transparent);authDialog.getWindow().setGravity(Gravity.BOTTOM);authDialog.getWindow().setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_ADJUST_RESIZE);authDialog.getWindow().setNavigationBarColor(0xff090a0c);
        cardTitle=text("أنشئ ملفك المحلي",18,0xfff8f1df,true);cardTitle.setGravity(Gravity.RIGHT);card.addView(cardTitle,lp(-1,-2));
        TextView note=text("على هذا الجهاز · دون اتصال",11,muted,false);note.setGravity(Gravity.RIGHT);card.addView(note,lp(-1,-2));
        TextView el=text("البريد الإلكتروني",11,0xffc9c4b8,false);el.setGravity(Gravity.RIGHT);LinearLayout.LayoutParams label=lp(-1,-2);label.topMargin=dp(12);card.addView(el,label);
        email=new EditText(this);email.setId(EMAIL_ID);email.setHint("you@example.com");email.setInputType(InputType.TYPE_CLASS_TEXT|InputType.TYPE_TEXT_VARIATION_EMAIL_ADDRESS);email.setSingleLine(true);styleInput(email);email.setContentDescription("البريد الإلكتروني");card.addView(email,lp(-1,48));
        TextView pl=text("كلمة المرور",11,0xffc9c4b8,false);pl.setGravity(Gravity.RIGHT);LinearLayout.LayoutParams pll=lp(-1,-2);pll.topMargin=dp(10);card.addView(pl,pll);
        FrameLayout passwordBox=new FrameLayout(this);password=new EditText(this);password.setId(PASSWORD_ID);password.setHint("8 أحرف على الأقل");password.setInputType(InputType.TYPE_CLASS_TEXT|InputType.TYPE_TEXT_VARIATION_PASSWORD);password.setSingleLine(true);styleInput(password);password.setPadding(dp(60),0,dp(14),0);password.setGravity(Gravity.RIGHT|Gravity.CENTER_VERTICAL);password.setContentDescription("كلمة المرور");passwordBox.addView(password,new FrameLayout.LayoutParams(-1,dp(48)));
        TextView reveal=text("إظهار",11,gold,false);FrameLayout.LayoutParams rl=new FrameLayout.LayoutParams(dp(56),dp(48),Gravity.LEFT);passwordBox.addView(reveal,rl);reveal.setOnClickListener(v->{boolean hidden=password.getTransformationMethod()!=null;password.setTransformationMethod(hidden?null:android.text.method.PasswordTransformationMethod.getInstance());reveal.setText(hidden?"إخفاء":"إظهار");password.setSelection(password.length());});card.addView(passwordBox,lp(-1,48));
        error=text("",11,0xffffab91,false);error.setAccessibilityLiveRegion(View.ACCESSIBILITY_LIVE_REGION_POLITE);error.setVisibility(View.GONE);card.addView(error,lp(-1,-2));
        submit=button("إنشاء الملف والدخول",true);submit.setId(SUBMIT_ID);LinearLayout.LayoutParams sl=lp(-1,52);sl.topMargin=dp(18);card.addView(submit,sl);submit.setOnClickListener(v->authenticate());
        switchMode=text("لديك ملف محلي؟ تسجيل الدخول",12,gold,false);LinearLayout.LayoutParams sw=lp(-1,40);sw.topMargin=dp(3);card.addView(switchMode,sw);switchMode.setOnClickListener(v->{if(busy)return;register=!register;cardTitle.setText(register?"أنشئ ملفك المحلي":"مرحبًا بعودتك");submit.setText(register?"إنشاء الملف والدخول":"تسجيل الدخول");submit.setContentDescription(submit.getText());switchMode.setText(register?"لديك ملف محلي؟ تسجيل الدخول":"مستخدم جديد؟ إنشاء ملف محلي");error.setVisibility(View.GONE);});
        TextView entry=button("ابدأ مساحتك  ←",true);entry.setId(ENTRY_ID);entry.setContentDescription("الدخول بالبريد وكلمة المرور");body.addView(entry,lp(-1,52));entry.setOnClickListener(v->{authDialog.show();authDialog.getWindow().setLayout(-1,-2);authScroll.setAlpha(0);authScroll.setTranslationY(dp(24));authScroll.animate().alpha(1).translationY(0).setDuration(260).start();});
        TextView guest=button("تجربة المحرر دون حساب",false);guest.setId(GUEST_ID);LinearLayout.LayoutParams gl=lp(-1,48);gl.topMargin=dp(14);body.addView(guest,gl);guest.setOnClickListener(v->{if(!busy){LocalAccounts.guest(this);openEditor();}});
        TextView footer=text("محفوظ محليًا. الخدمات السحابية معطلة مؤقتًا.",10,0xff858578,false);LinearLayout.LayoutParams fl=lp(-1,-2);fl.topMargin=dp(12);body.addView(footer,fl);
        setContentView(root);
        if(saved!=null)email.setText(saved.getString("email",""));
        body.setAlpha(0);body.setTranslationY(dp(16));body.animate().alpha(1).translationY(0).setDuration(650).start();
    }
    private void styleInput(EditText field){field.setTextColor(0xfff3eedf);field.setHintTextColor(0xff74776f);field.setTextSize(14);field.setTypeface(Typeface.create("sans-serif",0));field.setPadding(dp(14),0,dp(14),0);field.setBackground(box(0xff101115,0xff33332d,14));field.setOnFocusChangeListener((v,focus)->field.setBackground(box(0xff101115,focus?gold:0xff33332d,14)));}
    private void authenticate(){
        if(busy)return;busy=true;submit.setEnabled(false);submit.setText("جارٍ فتح مساحتك…");error.setVisibility(View.GONE);
        final String address=email.getText().toString();final char[] secret=password.getText().toString().toCharArray();password.setText("");final boolean creating=register;
        worker.execute(()->{String message=LocalAccounts.authenticate(getApplicationContext(),address,secret,creating);runOnUiThread(()->{if(isFinishing()||isDestroyed())return;busy=false;submit.setEnabled(true);submit.setText(register?"إنشاء الملف والدخول":"تسجيل الدخول");if(message==null)openEditor();else{error.setText(message);error.setVisibility(View.VISIBLE);}});});
    }
    private void openEditor(){
        ((InputMethodManager)getSystemService(INPUT_METHOD_SERVICE)).hideSoftInputFromWindow(email.getWindowToken(),0);
        if(authDialog!=null&&authDialog.isShowing())authDialog.dismiss();
        Intent intent=new Intent();intent.setClassName(this,"com.oneguystudio.ytyper.ui.dashboard.ProjectsActivity");startActivity(intent);overridePendingTransition(android.R.anim.fade_in,android.R.anim.fade_out);finish();
    }
    @Override protected void onSaveInstanceState(Bundle out){super.onSaveInstanceState(out);out.putString("email",email.getText().toString());}
    @Override protected void onResume(){super.onResume();if(backdrop!=null)backdrop.start();if(hero!=null)hero.start();}
    @Override protected void onPause(){if(backdrop!=null)backdrop.stop();if(hero!=null)hero.stop();super.onPause();}
    @Override protected void onDestroy(){if(authDialog!=null)authDialog.dismiss();if(worker!=null)worker.shutdown();super.onDestroy();}
}
