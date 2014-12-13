package com.example.johnandjai.sunshine.app;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.drawable.ShapeDrawable;
import android.graphics.drawable.shapes.PathShape;
import android.graphics.drawable.shapes.RoundRectShape;
import android.util.AttributeSet;
import android.view.View;

/**
 * Created by John and Jai on 12/4/2014.
 */
public class DirectionSpeedView extends View {

    private static final int W_SIZE = 100;          // width of the graphic
    private static final int H_SIZE = 100;          // height of the graphic

    private static final float COMPASS_RADIUS = 30;
    private static final float LARGE_COMPASS_POINT_WIDTH = 16;
    private static final float LARGE_COMPASS_POINT_HEIGHT = LARGE_COMPASS_POINT_WIDTH/2;
    private static final int LARGE_COMPASS_POINT_COLOR = Color.MAGENTA;
    private static final float SMALL_COMPASS_POINT_WIDTH = 10;
    private static final float SMALL_COMPASS_POINT_HEIGHT = SMALL_COMPASS_POINT_WIDTH/2;
    private static final int SMALL_COMPASS_POINT_COLOR = Color.BLUE;
    private static final float COMPASS_LABEL_OFFSET = LARGE_COMPASS_POINT_HEIGHT + 15;
    private static final int COMPASS_LABEL_TEXTCOLOR = Color.BLACK;
    private static final float COMPASS_LABEL_TEXTSIZE = 16;

    private Paint mPaint;
    private Paint mPaintText;
    private ShapeDrawable mLargeCompassPoint;
    private ShapeDrawable mSmallCompassPoint;

    private double mWindSpeed;
    private double mWindDirection;

    public DirectionSpeedView(Context context) {
        // Created from code.
        super(context);
        init();
    }

    public DirectionSpeedView(Context context, AttributeSet attrs) {
        // Created from resource.
        super(context, attrs);
        init();
    }

    public DirectionSpeedView(Context context, AttributeSet attrs, int defaultStyle) {
        // Created through inflation.
        super(context, attrs, defaultStyle);
        init();
    }

    public void setSpeed(double windSpeed) {
        mWindSpeed = windSpeed;
    }

    public void setDirection(double windDirection) {
        mWindDirection = windDirection;
    }

    private void init() {
        // called from each of the three constructors
        mPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        mPaintText = new Paint(Paint.ANTI_ALIAS_FLAG);
        mPaintText.setColor(COMPASS_LABEL_TEXTCOLOR);
        mPaintText.setTextSize(COMPASS_LABEL_TEXTSIZE);
        mPaintText.setTextAlign(Paint.Align.CENTER);
        mLargeCompassPoint = createCompassPoint(LARGE_COMPASS_POINT_WIDTH,
                                                LARGE_COMPASS_POINT_HEIGHT, LARGE_COMPASS_POINT_COLOR);
        mSmallCompassPoint = createCompassPoint(SMALL_COMPASS_POINT_WIDTH,
                                                SMALL_COMPASS_POINT_HEIGHT, SMALL_COMPASS_POINT_COLOR);
    }

    private ShapeDrawable createWindDirection(int color) {
        // Create a wind direction graphic which will be drawn on the compass circle at the
        // appropriate angle.
        float[] outerRadii = {3, 3, 3, 3, 3, 3, 3, 3};
        RoundRectShape roundRectShape = new RoundRectShape(outerRadii, null, null);
    }

    private ShapeDrawable createCompassPoint(float width, float height, int color) {
        // Create a compass direction graphic; default orientation is pointing South.
        Path path = new Path();
        path.moveTo(0,0);
        path.lineTo(width, 0);
        path.lineTo(width/2, height);
        path.lineTo(0,0);
        path.close();
        path.setFillType(Path.FillType.EVEN_ODD);

        // Draw the path onto a PathShape that is width units wide x height units high
        PathShape pathShape = new PathShape(path, width, height);
        ShapeDrawable compassPoint = new ShapeDrawable(pathShape);
        compassPoint.getPaint().setStyle(Paint.Style.FILL_AND_STROKE);
        compassPoint.getPaint().setColor(color);
        // Draw the PathShape into the bounding rectangle located according to the setBounds
        // arguments.  Scale the PathShape width and height to fill the width and height of the
        // bounding rectangle in setBounds.
        int rBound = Math.round(width/2);
        int lBound = Math.round(rBound - width);
        int tBound = Math.round(height);
        compassPoint.setBounds(lBound, 0, rBound, tBound);
        return compassPoint;
    }

    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        // Called when this view's parent is laying out its children.  Passes in how much space
        // is available and whether it will use exactly that much space or at most that much
        // space.

        // MeasureSpec is a sub-class of the View class
        int hSpecMode = MeasureSpec.getMode(heightMeasureSpec);
        int hSpecSize = MeasureSpec.getSize(heightMeasureSpec);
        int mHeight;

        // Decode the mode parameter for the height
        if (hSpecMode == MeasureSpec.EXACTLY) {
            // Size has been specified or 'fill_parent'
            mHeight = hSpecSize;
        } else {
            // wSpecMode == MeasureSpec.AT_MOST or wSpecMode == MeasureSpec.UNSPECIFIED
            // wrap_content
            // wSpecMode == MeasureSpec.AT_MOST or wSpecMode == MeasureSpec.UNSPECIFIED
            // Try for a width based on the minimum
            int minH = getPaddingTop() + H_SIZE + getPaddingBottom();
            mHeight = resolveSizeAndState(minH, heightMeasureSpec, 0);
        }

        int wSpecMode = MeasureSpec.getMode(widthMeasureSpec);
        int wSpecSize = MeasureSpec.getSize(widthMeasureSpec);
        int mWidth;

        // Decode the mode parameter for the width
        if (wSpecMode == MeasureSpec.EXACTLY) {
            // Size has been specified or 'fill_parent'
            mWidth = wSpecSize;
        } else {
            // wSpecMode == MeasureSpec.AT_MOST or wSpecMode == MeasureSpec.UNSPECIFIED
            // wrap_content
            // Try for a width based on the minimum
            int minW = getPaddingLeft() + W_SIZE + getPaddingRight();
            mWidth = resolveSizeAndState(minW, widthMeasureSpec, 0);
        }

        // must call this method of View to set the dimensions
        setMeasuredDimension(mWidth, mHeight);


        super.onMeasure(widthMeasureSpec, heightMeasureSpec);
    }

    @Override
    protected void onDraw(Canvas canvas) {
        // The origin (0,0) of the canvas is at the upper left corner of the graphic; the positive
        // x and y directions are to the right and down.
        super.onDraw(canvas);
        mPaint.setStyle(Paint.Style.STROKE);
        mPaint.setColor(Color.GREEN);
        canvas.save();
        canvas.translate(COMPASS_RADIUS + 30, COMPASS_RADIUS + 30);                 // origin (0,0) of the canvas is now at (30,30)
        canvas.drawCircle(0, 0, COMPASS_RADIUS, mPaint);
        String[] largeLabels = {"S", "W", "N", "E"};
        drawCompassPoints(canvas, mLargeCompassPoint, COMPASS_RADIUS, 0, largeLabels);
        drawCompassPoints(canvas, mSmallCompassPoint, COMPASS_RADIUS, 45, new String[]{});
        canvas.restore();            // restore the canvas position, orientation to the saved state
//        mPaint.setColor(Color.RED);
//        canvas.save();
//        canvas.translate(0, 0);
//        canvas.drawCircle(0, 0, 25, mPaint);
//        canvas.restore();
    }

    private void drawCompassPoints(Canvas canvas, ShapeDrawable compassPoint, float radius,
                                        float angleOffset, String[] labels) {
        // Draw and label the main compass points S, W, N, E.  Canvas must already be translated
        // to the center of the compass.  Use compassPoint as the graphic.
        float angle = angleOffset;
        for (int i = 0; i < 4; i++) {
            canvas.save();
            canvas.rotate(angle);         // rotate canvas angle deg ccw (graphic rotates angle deg cw)
            canvas.translate(0, radius);
            compassPoint.draw(canvas);
            if (labels.length > 0) {
                canvas.translate(0, COMPASS_LABEL_OFFSET);
                canvas.rotate(-angle);
                canvas.translate(0, COMPASS_LABEL_TEXTSIZE / 2 - 2);
                canvas.drawText(labels[i], 0, 0, mPaintText);
            }
            canvas.restore();
            angle += 90;
        }
    }
}
