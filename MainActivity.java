
package com.danil.acornhunter;

import android.app.Activity;
import android.os.Bundle;
import android.graphics.*;
import android.view.*;
import android.content.*;
import java.util.*;

public class MainActivity extends Activity {
    @Override public void onCreate(Bundle b) {
        super.onCreate(b);
        getWindow().setStatusBarColor(Color.rgb(23,20,33));
        getWindow().setNavigationBarColor(Color.rgb(23,20,33));
        setContentView(new GameView(this));
    }

    static class GameView extends View {
        final Paint p = new Paint(Paint.ANTI_ALIAS_FLAG);
        final Random rnd = new Random();

        ArrayList<PointF> acorns = new ArrayList<>();
        ArrayList<PointF> leaves = new ArrayList<>();
        ArrayList<PointF> squirrels = new ArrayList<>();
        ArrayList<PointF> fakeAcorns = new ArrayList<>();

        float playerX, playerY;
        int level = 0, found = 0, total = 3, secrets = 0;
        boolean started = false, finished = false, victory = false;
        String toast = "";
        long toastUntil = 0;
        long lastSquirrelMove = 0;
        int lives = 3;

        GameView(Context c) { super(c); setFocusable(true); }

        float d(float a,float b,float c,float e) {
            return (float)Math.hypot(a-c,b-e);
        }

        void startGame() {
            started = true;
            finished = false;
            victory = false;
            level = 1;
            setupLevel();
        }

        void setupLevel() {
            found = 0;
            lives = 3;
            acorns.clear();
            fakeAcorns.clear();
            squirrels.clear();
            leaves.clear();

            playerX = getWidth()/2f;
            playerY = getHeight()-180;

            int real = 3 + level;
            total = real;

            for (int i=0;i<48;i++)
                leaves.add(new PointF(
                    30+rnd.nextFloat()*(getWidth()-60),
                    155+rnd.nextFloat()*(getHeight()-330)));

            for (int i=0;i<real;i++) acorns.add(randomItem(170));
            for (int i=0;i<level+2;i++) fakeAcorns.add(randomItem(130));

            // Squirrels become active from level 2.
            if (level >= 2) {
                for (int i=0;i<Math.min(4, level);i++)
                    squirrels.add(new PointF(
                        60+rnd.nextFloat()*(getWidth()-120),
                        190+rnd.nextFloat()*(getHeight()-390)));
            }

            toast = level == 1
                    ? "Уровень 1: парк. Тут всё ещё нет желудей."
                    : "Уровень " + level + ": белки активированы.";
            toastUntil = System.currentTimeMillis()+1800;
            invalidate();
        }

        PointF randomItem(float minFromPlayer) {
            PointF q;
            do {
                q = new PointF(
                    55+rnd.nextFloat()*(getWidth()-110),
                    185+rnd.nextFloat()*(getHeight()-390));
            } while (d(q.x,q.y,playerX,playerY) < minFromPlayer);
            return q;
        }

        void showToast(String s) {
            toast = s;
            toastUntil = System.currentTimeMillis()+1400;
        }

        void moveToward(float tx,float ty) {
            if (!started || finished) return;

            float dx=tx-playerX, dy=ty-playerY;
            float len=(float)Math.hypot(dx,dy);
            if (len<2) return;

            playerX=Math.max(44,Math.min(getWidth()-44,playerX+dx/len*30));
            playerY=Math.max(150,Math.min(getHeight()-75,playerY+dy/len*30));

            // Real acorns.
            for (int i=acorns.size()-1;i>=0;i--) {
                PointF a=acorns.get(i);
                if (d(playerX,playerY,a.x,a.y)<48) {
                    acorns.remove(i);
                    found++;
                    if (found == 1 && secrets == 0) {
                        secrets++;
                        showToast("Котёнок, один есть. Я в тебя верила.");
                    } else if (found == 2 && secrets == 1) {
                        secrets++;
                        showToast("Данил сообщает: прогресс исторический.");
                    }
                    if (found==total) {
                        if (level < 3) {
                            showToast("Уровень пройден. Это было подозрительно легко.");
                        } else {
                            victory = true;
                            finished = true;
                        }
                    } else {
                        showToast("ЖЁЛУДЬ! Дарина одобряет.");
                    }
                }
            }

            // Fake acorns.
            for (int i=fakeAcorns.size()-1;i>=0;i--) {
                PointF a=fakeAcorns.get(i);
                if (d(playerX,playerY,a.x,a.y)<45) {
                    fakeAcorns.remove(i);
                    lives--;
                    showToast("ЭТО БЫЛ НЕ ЖЁЛУДЬ. Удача: "+lives+"/3");
                    if (lives<=0) {
                        finished=true;
                        victory=false;
                    }
                }
            }

            // Squirrels steal a real acorn if they get close.
            if (level >= 2 && System.currentTimeMillis()-lastSquirrelMove>900) {
                lastSquirrelMove=System.currentTimeMillis();
                for (PointF s:squirrels) {
                    float ang=rnd.nextFloat()*(float)Math.PI*2;
                    s.x=Math.max(40,Math.min(getWidth()-40,s.x+(float)Math.cos(ang)*70));
                    s.y=Math.max(160,Math.min(getHeight()-90,s.y+(float)Math.sin(ang)*70));
                }
                for (int i=acorns.size()-1;i>=0;i--) {
                    PointF a=acorns.get(i);
                    for (PointF s:squirrels) {
                        if (d(a.x,a.y,s.x,s.y)<42) {
                            acorns.remove(i);
                            total--;
                            showToast("БЕЛКА УКРАЛА ЖЁЛУДЬ. Я ВИДЕЛА.");
                            break;
                        }
                    }
                }
            }

            invalidate();
        }

        @Override protected void onDraw(Canvas c) {
            int w=getWidth(),h=getHeight();
            p.setStyle(Paint.Style.FILL);
            p.setColor(Color.rgb(23,20,33));
            c.drawRect(0,0,w,h,p);

            if (!started) { drawStart(c,w,h); return; }

            // Forest.
            p.setColor(Color.rgb(39,57,43)); c.drawRect(0,115,w,h,p);
            p.setColor(Color.rgb(27,42,33));
            for(int i=0;i<11;i++) {
                float x=18+i*w/10f;
                c.drawRect(x-8,145,x+8,h,p);
            }

            for(PointF q:leaves) {
                p.setColor(Color.rgb(83,110,69));
                c.drawCircle(q.x,q.y,15,p);
                p.setColor(Color.rgb(112,86,52));
                c.drawCircle(q.x+11,q.y+5,4,p);
            }

            // HUD.
            p.setColor(Color.rgb(23,20,33)); c.drawRect(0,0,w,115,p);
            txt(c,"ОПЕРАЦИЯ «ЖЁЛУДЬ»",20,35,19,Color.WHITE,true);
            txt(c,"УР. "+level+"   "+found+" / "+total,20,75,17,Color.rgb(240,216,165),true);
            txt(c,"❤ "+lives,w-75,75,16,Color.rgb(255,174,183),true);

            for(PointF a:fakeAcorns) drawFake(c,a.x,a.y);
            for(PointF a:acorns) drawAcorn(c,a.x,a.y);
            for(PointF s:squirrels) drawSquirrel(c,s.x,s.y);
            drawPlayer(c,playerX,playerY);

            // Bottom controls.
            p.setColor(Color.argb(75,255,255,255));
            c.drawCircle(70,h-65,43,p);
            c.drawCircle(w-70,h-65,43,p);
            txt(c,"←",51,h-53,27,Color.DKGRAY,true);
            txt(c,"→",w-89,h-53,27,Color.DKGRAY,true);

            if(toastUntil>System.currentTimeMillis()) {
                p.setColor(Color.argb(238,255,248,235));
                c.drawRoundRect(20,127,w-20,185,27,27,p);
                txt(c,toast,37,162,14,Color.rgb(55,43,47),true);
                postInvalidateDelayed(100);
            }

            if (finished) drawFinish(c,w,h);
        }

        void drawStart(Canvas c,int w,int h) {
            txtCenter(c,"ОПЕРАЦИЯ",w,150,29,Color.rgb(255,185,190),true);
            txtCenter(c,"«ЖЁЛУДЬ»",w,208,44,Color.WHITE,true);
            txtCenter(c,"Игра, которую пришлось сделать после",w,267,16,Color.LTGRAY,false);
            txtCenter(c,"того, как ты слишком долго искала жёлуди.",w,294,16,Color.LTGRAY,false);

            p.setColor(Color.rgb(255,174,183));
            c.drawRoundRect(50,500,w-50,580,38,38,p);
            txtCenter(c,"НАЧАТЬ ОПЕРАЦИЮ",w,550,18,Color.rgb(35,28,35),true);

            txtCenter(c,"Цель: найти хотя бы один.",w,h-95,14,Color.GRAY,false);
            txtCenter(c,"Побочный квест: пережить белок.",w,h-70,14,Color.GRAY,false);
        }

        void drawFinish(Canvas c,int w,int h) {
            p.setColor(Color.argb(244,23,20,33)); c.drawRect(0,0,w,h,p);
            if (victory) {
                txtCenter(c,"🏆",w,175,52,Color.WHITE,false);
                txtCenter(c,"ОН СУЩЕСТВУЕТ.",w,255,30,Color.rgb(255,205,120),true);
                txtCenter(c,"Каролина нашла легендарный жёлудь.",w,305,17,Color.WHITE,false);
                txtCenter(c,"Дарина может делать поделку.",w,338,17,Color.WHITE,false);
                txtCenter(c,"Белки официально проиграли.",w,385,17,Color.rgb(255,174,183),true);
            } else if (lives<=0) {
                txtCenter(c,"ОПЕРАЦИЯ ПРОВАЛЕНА",w,235,27,Color.rgb(255,174,183),true);
                txtCenter(c,"Ты приняла подозрительный объект",w,285,16,Color.WHITE,false);
                txtCenter(c,"за жёлудь. Это была шишка.",w,313,16,Color.WHITE,false);
            } else {
                txtCenter(c,"БЕЛКИ НАНЕСЛИ УДАР",w,235,27,Color.rgb(255,174,183),true);
                txtCenter(c,"Они украли слишком много улик.",w,285,16,Color.WHITE,false);
            }
            p.setColor(Color.rgb(255,174,183));
            c.drawRoundRect(50,490,w-50,560,35,35,p);
            txtCenter(c,"НАЧАТЬ ЗАНОВО",w,535,18,Color.rgb(35,28,35),true);
        }

        void drawAcorn(Canvas c,float x,float y) {
            p.setColor(Color.rgb(180,116,63));
            c.drawOval(x-12,y-7,x+12,y+19,p);
            p.setColor(Color.rgb(89,61,42));
            c.drawArc(x-14,y-17,x+14,y+3,180,-180,true,p);
            p.setColor(Color.rgb(230,178,110));
            c.drawCircle(x-4,y+6,3,p);
        }

        void drawFake(Canvas c,float x,float y) {
            p.setColor(Color.rgb(117,83,53));
            c.drawOval(x-13,y-6,x+13,y+20,p);
            p.setColor(Color.rgb(78,60,48));
            c.drawArc(x-15,y-16,x+15,y+4,180,-180,true,p);
            p.setColor(Color.rgb(150,115,78));
            c.drawLine(x-10,y+5,x+9,y+13,p);
        }

        void drawSquirrel(Canvas c,float x,float y) {
            p.setColor(Color.rgb(166,96,55));
            c.drawCircle(x,y,17,p);
            c.drawCircle(x-13,y-13,8,p);
            c.drawCircle(x+13,y-13,8,p);
            p.setColor(Color.rgb(60,42,42));
            c.drawCircle(x-6,y-3,2.5f,p);
            c.drawCircle(x+6,y-3,2.5f,p);
            p.setStyle(Paint.Style.STROKE); p.setStrokeWidth(3);
            c.drawArc(x-7,y+1,x+7,y+11,10,160,false,p);
            p.setStyle(Paint.Style.FILL);
        }

        void drawPlayer(Canvas c,float x,float y) {
            p.setColor(Color.rgb(255,185,190)); c.drawCircle(x,y,22,p);
            p.setColor(Color.rgb(48,37,55));
            c.drawCircle(x-7,y-4,3,p); c.drawCircle(x+7,y-4,3,p);
            p.setStyle(Paint.Style.STROKE); p.setStrokeWidth(3);
            c.drawArc(x-8,y,x+8,y+12,10,160,false,p);
            p.setStyle(Paint.Style.FILL);
        }

        void txt(Canvas c,String s,float x,float y,float size,int color,boolean bold) {
            p.setTextSize(size);
            p.setColor(color);
            p.setTypeface(Typeface.create("sans",bold?Typeface.BOLD:Typeface.NORMAL));
            c.drawText(s,x,y,p);
        }

        void txtCenter(Canvas c,String s,float w,float y,float size,int color,boolean bold) {
            p.setTextSize(size);
            p.setTypeface(Typeface.create("sans",bold?Typeface.BOLD:Typeface.NORMAL));
            txt(c,s,(w-p.measureText(s))/2,y,size,color,bold);
        }

        @Override public boolean onTouchEvent(MotionEvent e) {
            if(e.getAction()!=MotionEvent.ACTION_DOWN &&
               e.getAction()!=MotionEvent.ACTION_MOVE) return true;

            float x=e.getX(), y=e.getY();
            int w=getWidth(), h=getHeight();

            if(!started || finished) {
                if(y>455 && y<625) {
                    if(finished && victory && level<3) level++;
                    else if(finished) level=1;
                    startGame();
                }
                return true;
            }

            if(y>h-145) {
                moveToward(x<w/2?playerX-120:playerX+120,playerY);
            } else {
                moveToward(x,y);
            }
            return true;
        }
    }
}
