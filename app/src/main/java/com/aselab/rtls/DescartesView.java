package com.aselab.rtls;

import android.annotation.SuppressLint;
import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Typeface;
import android.graphics.drawable.Drawable;
import android.os.Build;
import android.util.AttributeSet;
import android.util.Log;
import android.view.MotionEvent;
import android.view.ScaleGestureDetector;
import android.view.View;

import androidx.annotation.Nullable;
import androidx.annotation.RequiresApi;
import androidx.core.content.ContextCompat;
import androidx.core.graphics.drawable.DrawableCompat;

import com.aselab.rtls.model.Anchor;
import com.aselab.rtls.model.Fraction;
import com.aselab.rtls.model.Point;
import com.aselab.rtls.model.Tag;
import com.aselab.rtls.model.Target;

import java.util.ArrayList;
import java.util.LinkedList;
import java.util.Queue;

public class DescartesView extends View {

    private static final int POINT_OFFSET = 15; // dp

    // Variables for scroll and zooming
    private float mPosX;
    private float mPosY;
    private float mLastTouchX;
    private float mLastTouchY;
    private static final int INVALID_POINTER_ID = -1;
    private int mActivePointerId = INVALID_POINTER_ID;
    private ScaleGestureDetector mScaleDetector;
    private float mScaleFactor = 1f;

    //Paints
    Paint paintLineXY,paintGrid,paintText,paintTextR,paintLine,paintInterception;
    Paint paintAnchor;
    Paint paintAxisUint;
    Paint paintTarget;
    Paint paintTag;

    //Variables for the grid
    int ScFactor;
    int w2 = 768;
    int ScaleMultiplier = 1;

    float TextPlusX1 = 0;
    float TextPlusY1 = 0;

    int BottomBound,TopBound,LeftBound,RightBound;

    // width and height of the screen
    int w;
    int h;

    PointSelectorListener pointSelectorListener;

    Typeface FontSpecial;
    DescartesLogic logic;
    Fraction nu1,nu2,nu3,nu4,nu5,nu6;
    float XInterception,YInterception;
    //int kdivider = 1000;
    //String kletter = "k"
    ArrayList<Anchor> anchors;
    Target target;
    Tag tag;

    boolean showTag;
    boolean showAnchor;
    boolean showTagLabel;
    boolean showAnchorLabel;
    boolean showGrid;
    boolean showAxis;
    int numberTrailer;
    int maxTrailer;
    Queue<Tag> tagTrailer;

    class Range {
        private final double x;
        private final double y;
        public Range(double x, double y) {
            this.x = x;
            this.y = y;
        }
        public double getX() {
            return x;
        }
        public double getY() {
            return y;
        }
    }

    public DescartesView(Context context) {
        super(context);
        init(context);
    }

    public DescartesView(Context context, @Nullable AttributeSet attrs) {
        super(context, attrs);
        init(context);
    }

    public DescartesView(Context context, @Nullable AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        init(context);
    }

    @RequiresApi(api = Build.VERSION_CODES.LOLLIPOP)
    public DescartesView(Context context, @Nullable AttributeSet attrs, int defStyleAttr, int defStyleRes) {
        super(context, attrs, defStyleAttr, defStyleRes);
        init(context);
    }

    private void init(Context c){
        mScaleDetector = new ScaleGestureDetector(c, new ScaleListener());
        logic = new DescartesLogic();

//        FontSpecial = Typeface.createFromAsset(c.getAssets(),  "fonts/tahoma regular font.ttf");

        paintGrid  = new Paint();
        paintLineXY = new Paint();
        paintText = new Paint();
        paintTextR = new Paint();
        paintLine = new Paint();
        paintInterception = new Paint();
        paintAnchor = new Paint();
        paintAxisUint = new Paint();
        paintTarget = new Paint();
        paintTag = new Paint();

//        paintText.setTypeface(FontSpecial);
        paintText.setColor(Color.rgb(156,156,156));
//        paintTextR.setTypeface(FontSpecial);
        paintTextR.setColor(Color.rgb(156,156,156));
        paintTextR.setTextAlign(Paint.Align.RIGHT);

//        paintText.setTypeface(FontSpecial);
        paintText.setColor(Color.rgb(156,156,156));

        paintLineXY.setColor(Color.BLACK);//Color.rgb(86,86,86)
        paintLineXY.setAntiAlias(true);
        paintGrid.setColor(Color.rgb(191,191,191));
        paintGrid.setAntiAlias(true);

        paintLine.setColor(Color.rgb(255,82,82));
        paintGrid.setAntiAlias(true);
        paintInterception.setColor(Color.rgb(97,97,97));
        paintInterception.setAntiAlias(true);

        paintAnchor.setColor(Color.rgb(97,97,97));
        paintAnchor.setAntiAlias(true);

        paintTarget.setColor(Color.rgb(200,46,38));
        paintTarget.setAntiAlias(true);

        paintTag.setColor(Color.rgb(55,0,179));
        paintTag.setAntiAlias(true);

        tagTrailer = new LinkedList<>();

        // Here width and height are 0, we have to wait until onDraw Method be activated and call GridAttributes;
    }


    @SuppressLint("DrawAllocation")
    @Override
    public void onDraw(Canvas canvas) {
        super.onDraw(canvas);
        canvas.drawColor(ContextCompat.getColor(getContext(), R.color.map_background));
        canvas.save();

        ConditionsForDrawing();

        canvas.translate(mPosX, mPosY);
        canvas.scale(mScaleFactor, mScaleFactor, (float) (canvas.getClipBounds().left + canvas.getClipBounds().right) / 2, (float) (canvas.getClipBounds().top + canvas.getClipBounds().bottom) / 2);

        DrawGrid(canvas);
        if (showAnchor) DrawAnchor(canvas);
        if (target != null) DrawTarget(canvas);
        if (tag != null) DrawTag(canvas);
        if (tagTrailer.size() > 0) DrawTagTrailer(canvas);

        canvas.restore();

    }

    private void ConditionsForDrawing(){

        // According to the size of the screen we put a specific values to ensure that ScFactor is an int
        if(w>=1536){
            w2 = 1536;
        }else if(w>=1344){
            w2 = 1344;
        }else if(w>=1152){
            w2 = 1152;
        }else if(w>=960){
            w2 = 960;
        }else if(w>=768){
            w2 = 768;
        }else if(w>=576){
            w2 = 576;
        }else{
            w2 = 576;
        }

        // According to the zoom of the screen, the measure between each line will change
        if(mScaleFactor>1.3){
            ScFactor = (w2*28)/768;
            ScaleMultiplier = 1;
        }else if(mScaleFactor>0.75){
            ScFactor = (w2*56)/768;
            ScaleMultiplier = 2;
        }else if(mScaleFactor>0.4){
            ScFactor = (w2*140)/768;
            ScaleMultiplier = 5;
        }else if (mScaleFactor>0.15){
            ScFactor = (w2*280)/768;
            ScaleMultiplier = 10;
        }else if (mScaleFactor>0.07){
            ScFactor = (w2*560)/768;
            ScaleMultiplier = 20;
        }else if (mScaleFactor>0.03){
            ScFactor = (w2*1120)/768;
            ScaleMultiplier = 50;
        }else{
            ScFactor = (w2*2240)/768;
            ScaleMultiplier = 100;
        }
        GridAttributes();
    }

    private void GridAttributes(){

        paintLineXY.setStrokeWidth((float)w*(2.2f/mScaleFactor)/768);
        paintGrid.setStrokeWidth((float)w*(1f/mScaleFactor)/768);
        paintText.setTextSize((float)w*(17f/mScaleFactor)/768);
        paintTextR.setTextSize((float)w*(17f/mScaleFactor)/768);
        paintAxisUint.setTextSize((float)w*(20f/mScaleFactor)/768);
        paintLine.setStrokeWidth((float)w*(3.7f/mScaleFactor)/768);
        TextPlusX1 = w*(8/mScaleFactor)/768;
        TextPlusY1 = w*(-8/mScaleFactor)/768;
    }

    private void DrawGrid(Canvas canvas){

        LeftBound = canvas.getClipBounds().left; // max
        RightBound = canvas.getClipBounds().right; // min
        TopBound = canvas.getClipBounds().top; //max
        BottomBound = canvas.getClipBounds().bottom; //min

        //Get the number of the point for drawing the first vertical and horizontal line of the grid
        int StNumberX = logic.GetFirstTextNumber(LeftBound,RightBound,ScFactor);
        int StNumberY = logic.GetFirstTextNumber(TopBound,BottomBound,ScFactor);

        if (showAxis) {
            // X axis unit
            if (TopBound < 0 && BottomBound > 0) {
                canvas.drawText(" x (cm)", RightBound - (w * (75f / mScaleFactor)) / 768, TextPlusY1 + (w * (30f / mScaleFactor)) / 768, paintAxisUint);
            } else if (TopBound > 0 && BottomBound > 0) {
                canvas.drawText(" x (cm)", RightBound - (w * (75f / mScaleFactor)) / 768, TopBound + (w * (55f / mScaleFactor)) / 768, paintAxisUint);
            } else if (TopBound < 0 && BottomBound < 0) {
                canvas.drawText(" x (cm)", RightBound - (w * (75f / mScaleFactor)) / 768, BottomBound - (w * (30f / mScaleFactor)) / 768, paintAxisUint);
            }

            // Y axis unit
            if (LeftBound < 0 && RightBound > 0) {
                canvas.drawText("y (cm)", TextPlusX1 - (w * (75f / mScaleFactor)) / 768, TopBound + (w * (30f / mScaleFactor)) / 768, paintAxisUint);
            } else if (LeftBound > 0 && RightBound > 0) {
                canvas.drawText("y (cm)", LeftBound + (w * (55f / mScaleFactor)) / 768, TopBound + (w * (30f / mScaleFactor)) / 768, paintAxisUint);
            } else if (LeftBound < 0 && RightBound < 0) {
                canvas.drawText("y (cm)", RightBound - (w * (85f / mScaleFactor)) / 768, TopBound + (w * (30f / mScaleFactor)) / 768, paintAxisUint);
            }
        }

        //Draw the horizontal text and vertical grid lines
        for(int i = StNumberX;i<RightBound;i+=ScFactor){
            if (showAxis) {
                if (i == StNumberX) {
                    canvas.drawLine(0, TopBound, 0, BottomBound, paintLineXY); // Y axis
                    //canvas.drawRect(0,TopBound,20,BottomBound,paintRect);
                }

                if((i/ScFactor)*ScaleMultiplier!=0) {
                    if(Math.abs(((i/ScFactor)*ScaleMultiplier))>=10000 && ScaleMultiplier>=10){
                        if (TopBound < 0 && BottomBound > 0) {
                            canvas.drawText(String.valueOf((float)((i/(ScFactor))*ScaleMultiplier)/1000f)+"k", i+TextPlusX1, TextPlusY1, paintText);
                        } else if (TopBound > 0 && BottomBound > 0) {
                            canvas.drawText(String.valueOf((float)((i/(ScFactor))*ScaleMultiplier)/1000f)+"k", i+TextPlusX1, (float) TopBound + (float)(w*(25f/mScaleFactor))/768, paintText);
                        } else if (TopBound < 0 && BottomBound < 0) {
                            canvas.drawText(String.valueOf((float)((i/(ScFactor))*ScaleMultiplier)/1000f)+"k", i+TextPlusX1, (float) BottomBound+TextPlusY1, paintText);
                        }
                    }else{
                        if (TopBound < 0 && BottomBound > 0) {
                            // Normal
                            canvas.drawText(String.valueOf((i / ScFactor) * ScaleMultiplier), i+TextPlusX1, TextPlusY1, paintText);
                        } else if (TopBound > 0 && BottomBound > 0) {
                            // Axis top
                            canvas.drawText(String.valueOf((i / ScFactor) * ScaleMultiplier), i+TextPlusX1, (float) TopBound + (float)(w*(25f/mScaleFactor))/768, paintText);
                        } else if (TopBound < 0 && BottomBound < 0) {
                            // Axis bottom
                            canvas.drawText(String.valueOf((i / ScFactor) * ScaleMultiplier), i+TextPlusX1, (float) BottomBound+TextPlusY1, paintText);
                        }
                    }
                }
            }

            if (showGrid) {
                canvas.drawLine(i, TopBound, i, BottomBound, paintGrid);
            }
        }

        //Draw the vertical text and horizontal grid lines
        for(int i = StNumberY;i<BottomBound;i+=ScFactor){
            if (showAxis) {
                if (i == StNumberY) {
                    canvas.drawLine(LeftBound, 0, RightBound, 0, paintLineXY); // X axis
                    //canvas.drawRect(LeftBound,-20,RightBound,0,paintRect);
                }

                if((i/ScFactor)*ScaleMultiplier!=0){
                    if(Math.abs(((i/ScFactor)*ScaleMultiplier))>=10000 && ScaleMultiplier>=10){
                        if(LeftBound<0 && RightBound>0){
                            canvas.drawText(String.valueOf((float)((i/(ScFactor*-1))*ScaleMultiplier)/1000f)+"k",TextPlusX1,i+TextPlusY1,paintText);
                        }else if(LeftBound>0 && RightBound>0){
                            canvas.drawText(String.valueOf((float)((i/(ScFactor*-1))*ScaleMultiplier)/1000f)+"k",(float)LeftBound+TextPlusX1,i+TextPlusY1,paintText);
                        }else if(LeftBound<0 && RightBound<0){
                            canvas.drawText(String.valueOf((float)((i/(ScFactor*-1))*ScaleMultiplier)/1000f)+"k",(float)RightBound-TextPlusX1,i,paintTextR);
                        }
                    }else{
                        if(LeftBound<0 && RightBound>0){
                            canvas.drawText(String.valueOf((i/(ScFactor*-1))*ScaleMultiplier),TextPlusX1,i+TextPlusY1,paintText);
                        }else if(LeftBound>0 && RightBound>0){
                            canvas.drawText(String.valueOf((i/(ScFactor*-1))*ScaleMultiplier),(float)LeftBound+TextPlusX1,i+TextPlusY1,paintText);
                        }else if(LeftBound<0 && RightBound<0){
                            canvas.drawText(String.valueOf((i/(ScFactor*-1))*ScaleMultiplier),(float)RightBound-TextPlusX1,i,paintTextR);
                        }
                    }
                }
            }

            if (showGrid) {
                canvas.drawLine(LeftBound, i, RightBound, i, paintGrid);
            }
        }
    }

    public void DrawAnchor(Canvas canvas) {
        int ScFactorLine = (w2*28)/768;

        paintAnchor.setStrokeWidth((float) w * (16f / mScaleFactor) / 768);

        for (Anchor anchor:anchors) {
            canvas.drawPoint((float)anchor.getX() * ScFactorLine, (float)anchor.getY() * ScFactorLine * -1, paintAnchor);
            if (showAnchorLabel) {
                canvas.drawText(anchor.getTitle(),
                        (float) anchor.getX() * ScFactorLine + (anchor.getHorizontalAlignment() < 0 ? -6 : 1) * (w * (15f / mScaleFactor)) / 768,
                        (float) anchor.getY() * ScFactorLine * -1 + anchor.getVerticalAlignment() * (w * (30f / mScaleFactor)) / 768,
                        paintAxisUint);
            }
        }

    }

    public static Bitmap getBitmapFromVectorDrawable(Drawable drawable) {
        Bitmap bitmap = Bitmap.createBitmap(drawable.getIntrinsicWidth(),
                drawable.getIntrinsicHeight(), Bitmap.Config.ARGB_8888);
        Canvas canvas = new Canvas(bitmap);
        drawable.setBounds(0, 0, canvas.getWidth(), canvas.getHeight());
        drawable.draw(canvas);
        return bitmap;
    }

    public void DrawTarget(Canvas canvas) {
        int ScFactorLine = (w2*28)/768;
        canvas.drawCircle(((float)target.getX() * ScFactorLine),
                ((float)target.getY() * ScFactorLine * -1),
                (float) w * (8f / mScaleFactor) / 768,
                paintTarget);
    }

    public void DrawTag(Canvas canvas) {
        paintTag.setColor(Color.argb(100,55,0,179));
        int ScFactorLine = (w2*28)/768;
        canvas.drawCircle(((float)tag.getX() * ScFactorLine),
                ((float)tag.getY() * ScFactorLine * -1),
                (float) w * (8f / mScaleFactor) / 768,
                paintTag);
    }

    public void DrawTagTrailer(Canvas canvas) {
        int ScFactorLine = (w2*28)/768;
        int alpha = 50;
        float alphaStep = 1f * maxTrailer / (100 - alpha);
        for (Tag trailer : tagTrailer) {
            alpha = (int)(alpha + alphaStep);
            if (alpha > 100) alpha = 100;
            paintTag.setColor(Color.argb(alpha,55,0,179));
            canvas.drawCircle(((float)trailer.getX() * ScFactorLine),
                    ((float)trailer.getY() * ScFactorLine * -1),
                    (float) w * (8f / mScaleFactor) / 768,
                    paintTag);
        }
    }

    public void DrawLines(Canvas canvas){

        int ScFactorLine = (w2*28)/768;
        int ScaleMultiplierLine = 1;

        //Get the coordinates of the line according to the equation and the maximum and minimum
        //coordinates which the cartesian plane provide us
        float[] MaxValues = logic.getMaxandMinPoints(ScaleMultiplierLine,ScFactorLine,1,nu1,nu2,nu3,
                (float)RightBound/ScFactorLine,(float)TopBound/ScFactorLine);
        float[] MinValues = logic.getMaxandMinPoints(ScaleMultiplierLine,ScFactorLine,2,nu1,nu2,nu3,
                (float)LeftBound/ScFactorLine,(float)BottomBound/ScFactorLine);

        paintLine.setColor(Color.rgb(255,82,82));//red
        canvas.drawLine(MaxValues[0],MaxValues[1],MinValues[0],MinValues[1],paintLine);

        float[] MaxValues2 = logic.getMaxandMinPoints(ScaleMultiplierLine,ScFactorLine,1,nu4,nu5,nu6,
                (float)RightBound/ScFactorLine,(float)TopBound/ScFactorLine);
        float[] MinValues2 = logic.getMaxandMinPoints(ScaleMultiplierLine,ScFactorLine,2,nu4,nu5,nu6,
                (float)LeftBound/ScFactorLine,(float)BottomBound/ScFactorLine);

        paintLine.setColor(Color.rgb(26,82,118));
        canvas.drawLine(MaxValues2[0],MaxValues2[1],MinValues2[0],MinValues2[1],paintLine);

        // if the equation is parallel it will not have point of intersection
        if(!logic.isparallel(nu1,nu2,nu3,nu4,nu5,nu6)) {
            float[] points = logic.Solve2x2(nu1, nu2, nu3, nu4, nu5, nu6);
            XInterception = points[0];
            YInterception = points[1] * -1;
            canvas.drawCircle((points[0] * ScFactorLine), (points[1] * ScFactorLine * -1), (float) w * (8f / mScaleFactor) / 768, paintInterception);
        }

        // test for draw anchor
        // canvas.drawCircle((100 * ScFactorLine), (100 * ScFactorLine * -1), (float) w * (8f / mScaleFactor) / 768, paintInterception);
    }


    @Override
    public boolean onTouchEvent(MotionEvent ev) {
        // Let the ScaleGestureDetector inspect all events.
        mScaleDetector.onTouchEvent(ev);
        pointSelectorListener.onTouchEvent(ev);

        final int action = ev.getAction();
        switch (action & MotionEvent.ACTION_MASK) {
            case MotionEvent.ACTION_DOWN: {
                final float x = (ev.getX())/mScaleFactor;
                final float y = (ev.getY()/mScaleFactor);

                int ScFactor = (w2*28)/768;

                double originX = (LeftBound+x+w*8f/mScaleFactor/768)/ScFactor;
                double originY = (TopBound+y)/ScFactor*-1;

                Log.e("TAGTOUCH", "LeftBound " + LeftBound + " ScaleMultiplier " + ScaleMultiplier
                        + " mScaleFactor " + mScaleFactor + " ScFactor " + ScFactor
                        + " w " + w + " w*16f/768 " + w*16f/768);
                Log.d("TAGTOUCH","You touched on: ("+x+" ; "+y+") --> ("+originX+" ; "+originY + ")");

                pointSelectorListener.onSelectPoint(getPointObject(originX, originY));

                mLastTouchX = x;
                mLastTouchY = y;
                mActivePointerId = ev.getPointerId(0);
                break;
            }

            case MotionEvent.ACTION_MOVE: {
                final int pointerIndex = ev.findPointerIndex(mActivePointerId);
//                Log.d("TEST", "mActivePointerId=" + mActivePointerId+" pointerIndex="+pointerIndex);
                final float x = (ev.getX(pointerIndex)/mScaleFactor);
                final float y = (ev.getY(pointerIndex)/mScaleFactor);

                // Only move if the ScaleGestureDetector isn't processing a gesture.
                if (!mScaleDetector.isInProgress()) {
                    final float dx = x - mLastTouchX;
                    final float dy = y - mLastTouchY;

                    mPosX += dx;
                    mPosY += dy;


                    invalidate();
                }

                mLastTouchX = x;
                mLastTouchY = y;

                break;
            }

            case MotionEvent.ACTION_UP: {
                mActivePointerId = INVALID_POINTER_ID;
                break;
            }

            case MotionEvent.ACTION_CANCEL: {
                mActivePointerId = INVALID_POINTER_ID;
                break;
            }

            case MotionEvent.ACTION_POINTER_UP: {
                final int pointerIndex = (ev.getAction() & MotionEvent.ACTION_POINTER_INDEX_MASK)
                        >> MotionEvent.ACTION_POINTER_INDEX_SHIFT;
                final int pointerId = ev.getPointerId(pointerIndex);
                if (pointerId == mActivePointerId) {
                    // This was our active pointer going up. Choose a new
                    // active pointer and adjust accordingly.
                    final int newPointerIndex = pointerIndex == 0 ? 1 : 0;
                    mLastTouchX = ev.getX(newPointerIndex)/mScaleFactor;
                    mLastTouchY = ev.getY(newPointerIndex)/mScaleFactor;
                    mActivePointerId = ev.getPointerId(newPointerIndex);
                }
                break;
            }
        }

        return true;
    }

    private class ScaleListener extends ScaleGestureDetector.SimpleOnScaleGestureListener {
        @Override
        public boolean onScale(ScaleGestureDetector detector) {
            mScaleFactor *= detector.getScaleFactor();

            // Don't let the object get too small or too large. 0.8-3.4, 1 escala 0.7-1.3, 1.3-2.5
            //0.2-0.37
            mScaleFactor = Math.max(0.015f, Math.min(mScaleFactor,1.5f));
            Log.d("TAGB"," "+mScaleFactor+"  zoom 1?"+"    "+(float)((-2.55*mScaleFactor)+7.895));
            invalidate();
            return true;
        }
    }

    public void setHeightAndWidth(int w, int h){
        this.w = w;
        this.h = h;
    }

    public void setEquation(Fraction nu1, Fraction nu2, Fraction nu3, Fraction nu4, Fraction nu5, Fraction nu6){
        this.nu1 = nu1;
        this.nu2 = nu2;
        this.nu3 = nu3;
        this.nu4 = nu4;
        this.nu5 = nu5;
        this.nu6 = nu6;
    }

    public void setAnchors(ArrayList<Anchor> anchors) {
        this.anchors = anchors;
    }

    public void setTarget(Target target) {
        this.target = target;
    }

    public void setTag(Tag tag) {
        this.tag = tag;
        while (tagTrailer.size() > 0 && tagTrailer.size() >= maxTrailer) {
            tagTrailer.remove();
        }
        tagTrailer.add(tag);
    }

    public void deleteAllTags() {
        tag = null;
        tagTrailer.clear();
        invalidate();
    }

    public void centerPlane(){
        ConditionsForDrawing();
        float ScFactor = 1f * (w2 * 28) / 768;
        Range viewRange = getViewRange();
        mScaleFactor = Math.min(h/ScFactor/2f/(float)viewRange.getY(), w/ScFactor/2f/(float)viewRange.getX());
        mPosX = 150 / 2f * -1 * ScFactor + w / 2f; //- = x positives y su += a x negatives
        mPosY = 250 / 2f * ScFactor + h / 2f; //- = x negatives y su += a x positives
        Log.d("CENTER", "h = " + h + ", w = " + w + ", w2 = " + w2);
        Log.d("CENTER", "mScaleFactor = " + mScaleFactor + ", mPosX = " + mPosX + ", mPosY = " + mPosY);
        invalidate();
    }

    private Range getViewRange() {
        double minX = Double.MAX_VALUE, maxX = Double.MIN_VALUE, minY = Double.MAX_VALUE, maxY = Double.MIN_VALUE;
        if (tag != null) {
            if (tag.getX() < minX) minX = tag.getX();
            if (tag.getX() > maxX) maxX = tag.getX();
            if (tag.getY() < minY) minY = tag.getY();
            if (tag.getY() > maxY) maxY = tag.getY();
        }
        if (target != null) {
            if (target.getX() < minX) minX = target.getX();
            if (target.getX() > maxX) maxX = target.getX();
            if (target.getY() < minY) minY = target.getY();
            if (target.getY() > maxY) maxY = target.getY();
        }
        for (Anchor anchor : anchors) {
            if (anchor.getX() < minX) minX = anchor.getX();
            if (anchor.getX() > maxX) maxX = anchor.getX();
            if (anchor.getY() < minY) minY = anchor.getY();
            if (anchor.getY() > maxY) maxY = anchor.getY();
        }
        return new Range(maxX-minX, maxY-minY);
    }

    public void center0(){
        mScaleFactor = 1f;
        mPosX = (float) (w / 2);
        mPosY = (float) (h / 2);
        invalidate();
    }

    public Point getPointObject(double x, double y) {
        int ScFactor = (w2*28)/768;
        double scaleOffset = 1f*POINT_OFFSET/ScFactor/mScaleFactor*getResources().getDisplayMetrics().density;
        Point p = new Point(x,y);
        double minDist = scaleOffset;
        double tmpDist;
        Point.PointType type = Point.PointType.POINT;
        int id = -1;

        if (tag != null) {
            tmpDist = calcDistance(p, tag);
            if (tmpDist < scaleOffset && tmpDist < minDist) {
                minDist = tmpDist;
                type = Point.PointType.TAG;
            }
        }

        if (target != null) {
            tmpDist = calcDistance(p, target);
            if (tmpDist < scaleOffset && tmpDist < minDist) {
                minDist = tmpDist;
                type = Point.PointType.TARGET;
            }
        }

        for (Anchor anchor:anchors) {
            tmpDist = calcDistance(p, anchor);
            if (tmpDist < scaleOffset && tmpDist < minDist) {
                minDist = tmpDist;
                type = Point.PointType.ANCHOR;
                id = Character.getNumericValue(anchor.getTitle().charAt(anchor.getTitle().length() - 1)) - 1;
            }
        }
        switch (type) {
            case TAG: return tag;
            case TARGET: return target;
            case ANCHOR: return anchors.get(id) != null ? anchors.get(id) : p;
            default: return p;
        }
    }

    private double calcDistance(Point p1, Point p2) {
        return Math.sqrt(Math.pow(p2.getX()-p1.getX(),2) + Math.pow(p2.getY()-p1.getY(),2));
    }

    public void setListener(PointSelectorListener listener){
        this.pointSelectorListener = listener;
    }

    public void setShowTag(boolean showTag) {
        this.showTag = showTag;
    }

    public void setShowAnchor(boolean showAnchor) {
        this.showAnchor = showAnchor;
    }

    public void setShowTagLabel(boolean showTagLabel) {
        this.showTagLabel = showTagLabel;
    }

    public void setShowAnchorLabel(boolean showAnchorLabel) {
        this.showAnchorLabel = showAnchorLabel;
    }

    public void setShowGrid(boolean showGrid) {
        this.showGrid = showGrid;
    }

    public void setShowAxis(boolean showAxis) {
        this.showAxis = showAxis;
    }

    public void setNumberTrailer(int numberTrailer) {
        this.numberTrailer = numberTrailer;
        switch (numberTrailer) {
            case -2:
                maxTrailer = Integer.MAX_VALUE;
                break;
            case -1:
                maxTrailer = 0;
                break;
            default:
                maxTrailer = numberTrailer;
        }
        while (tagTrailer.size() > 0 && tagTrailer.size() > maxTrailer) {
            tagTrailer.remove();
        }
    }
}

