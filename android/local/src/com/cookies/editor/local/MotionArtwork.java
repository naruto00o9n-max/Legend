package com.cookies.editor.local;

import android.animation.ValueAnimator;
import android.content.Context;
import android.graphics.*;
import android.view.View;

/** Procedural welcome artwork. No dependency on network or editor models. */
public final class MotionArtwork extends View {
    private final Paint p=new Paint(3);
    private final boolean hero;
    private ValueAnimator animation;
    private float phase;
    private final Typeface arabic;
    public MotionArtwork(Context context,boolean hero){super(context);this.hero=hero;Typeface f;try{f=Typeface.createFromAsset(context.getAssets(),"fonts/bein_normal.ttf");}catch(Exception e){f=Typeface.DEFAULT;}arabic=f;setImportantForAccessibility(IMPORTANT_FOR_ACCESSIBILITY_NO);}
    public void start(){if(animation!=null)return;animation=ValueAnimator.ofFloat(0,1);animation.setDuration(12000);animation.setRepeatCount(ValueAnimator.INFINITE);animation.setInterpolator(new android.view.animation.LinearInterpolator());animation.addUpdateListener(a->{phase=(float)a.getAnimatedValue();invalidate();});animation.start();}
    public void stop(){if(animation!=null){animation.cancel();animation=null;}}
    private void rect(Canvas c,RectF r,int color,float radius){p.setShader(null);p.setStyle(Paint.Style.FILL);p.setColor(color);c.drawRoundRect(r,radius,radius,p);}
    private void edge(Canvas c,RectF r,int color,float radius){p.setShader(null);p.setStyle(Paint.Style.STROKE);p.setStrokeWidth(1);p.setColor(color);c.drawRoundRect(r,radius,radius,p);p.setStyle(Paint.Style.FILL);}
    private void label(Canvas c,String s,float x,float y,float size,int color,boolean rtl){p.setShader(null);p.setTypeface(rtl?arabic:Typeface.create("sans-serif-medium",0));p.setTextSize(size);p.setColor(color);p.setTextAlign(Paint.Align.CENTER);c.drawText(s,x,y,p);}
    @Override protected void onDraw(Canvas canvas){
        super.onDraw(canvas);Canvas c=canvas;float w=getWidth(),h=getHeight();if(w==0||h==0)return;
        if(!hero){
            float x=w*(.7f+.12f*(float)Math.sin(phase*6.28)),y=h*.22f;
            p.setShader(new RadialGradient(x,y,w*.9f,new int[]{0x32ba8d31,0x102d2418,0x00090a0c},null,Shader.TileMode.CLAMP));c.drawRect(0,0,w,h,p);p.setShader(null);
            p.setColor(0x087e744f);p.setStrokeWidth(1);for(float i=0;i<w;i+=42)c.drawLine(i,0,i,h,p);for(float i=0;i<h;i+=42)c.drawLine(0,i,w,i,p);
            for(int i=0;i<34;i++){float sx=(i*73.7f)%w,sy=((i*127.3f)+phase*35)%h;p.setColor(Color.argb(20+(i%4)*9,229,194,103));c.drawCircle(sx,sy,1+(i%3)*.4f,p);}return;
        }
        c.save();c.scale(w/340,h/210);float drift=(float)Math.sin(phase*6.28)*3;
        p.setShader(new RadialGradient(170,102,145,new int[]{0x43d7aa49,0x122e2315,0x00000000},null,Shader.TileMode.CLAMP));c.drawRect(0,0,340,210,p);p.setShader(null);
        p.setColor(0x36c8a34b);p.setStyle(Paint.Style.STROKE);p.setStrokeWidth(.8f);c.save();c.rotate(-15,170,105);c.drawOval(new RectF(15,52,325,166),p);c.restore();p.setStyle(Paint.Style.FILL);
        c.save();c.translate(0,drift);c.rotate(-8,162,101);rect(c,new RectF(89,22,241,181),0xff0e1014,18);edge(c,new RectF(89,22,241,181),0xff716044,18);
        RectF art=new RectF(99,32,231,150);p.setShader(new LinearGradient(99,32,231,150,new int[]{0xff292c34,0xff726246,0xff171a20},null,Shader.TileMode.CLAMP));c.drawRoundRect(art,11,11,p);p.setShader(null);
        p.setColor(0x55dfc386);Path rays=new Path();rays.moveTo(101,147);rays.lineTo(177,38);rays.lineTo(213,36);rays.close();c.drawPath(rays,p);
        p.setColor(0xff101317);c.drawCircle(174,75,15,p);RectF character=new RectF(150,88,198,157);c.drawRoundRect(character,22,22,p);
        rect(c,new RectF(106,47,164,70),0xfff2e9d0,10);label(c,"حكايتك",135,62,10,0xff29251c,true);
        rect(c,new RectF(166,115,222,139),0xfff2e9d0,10);label(c,"تبدأ هنا",194,131,9,0xff29251c,true);
        p.setColor(0xffd7bb76);p.setStrokeWidth(2);c.drawLine(113,163,166,163,p);p.setColor(0xff34352f);c.drawLine(174,163,217,163,p);c.restore();
        c.save();c.translate(0,-drift);c.rotate(6,266,88);rect(c,new RectF(229,52,331,126),0xf01b1d23,13);edge(c,new RectF(229,52,331,126),0x65e0c06c,13);label(c,"LAYERS",280,69,7,0xffa49c8d,false);
        String[] layers={"نص عربي","صورة","رسم"};for(int i=0;i<3;i++){float yy=76+i*14;rect(c,new RectF(237,yy,323,yy+12),i==0?0xff504633:0xff24262c,4);label(c,layers[i],279,yy+9,7,0xffe9dfc8,true);p.setColor(i==0?0xffe6c568:0xff6d706e);c.drawCircle(244,yy+6,2,p);}c.restore();
        c.save();c.translate(0,drift);c.rotate(-4,75,142);rect(c,new RectF(7,118,112,174),0xf0191b21,12);edge(c,new RectF(7,118,112,174),0x4dc8aa61,12);label(c,"Aa",34,145,21,0xffe8c879,false);label(c,"خطوط عربية",80,139,8,0xffefe7d5,true);label(c,"تصنع الفرق",80,154,7,0xffa9a396,true);c.restore();
        p.setColor(0xffd6b764);for(int i=0;i<5;i++){float xx=23+i*72,yy=31+(i%3)*60;p.setStrokeWidth(1);c.drawLine(xx-3,yy,xx+3,yy,p);c.drawLine(xx,yy-3,xx,yy+3,p);}c.restore();
    }
}
