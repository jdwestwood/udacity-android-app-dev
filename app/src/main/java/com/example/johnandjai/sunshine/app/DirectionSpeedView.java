package com.example.johnandjai.sunshine.app;

import android.content.Context;
import android.content.res.TypedArray;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.drawable.ShapeDrawable;
import android.graphics.drawable.shapes.PathShape;
import android.graphics.drawable.shapes.RoundRectShape;
import android.util.AttributeSet;
import android.util.Log;
import android.view.View;

/**
 * Created by John and Jai on 12/4/2014.
 */
public class DirectionSpeedView extends View {

    private final String LOG_TAG = getClass().getSimpleName();

    private float mSize = 0;
    private int W_SIZE;             // width of the graphic
    private int H_SIZE;             // height of the graphic

    private float COMPASS_RADIUS;
    private float COMPASS_CIRCLE_STROKE_WIDTH;
    private float LARGE_COMPASS_POINT_WIDTH;
    private float LARGE_COMPASS_POINT_HEIGHT;
    private float SMALL_COMPASS_POINT_WIDTH;
    private float SMALL_COMPASS_POINT_HEIGHT;
    private float COMPASS_LABEL_TEXTSIZE;
    private float COMPASS_LABEL_POSITION;
    private float COMPASS_LABEL_OFFSET;

    private float SPEED_RADIUS;
    private float SPEED_TICK_LENGTH;
    private float SPEED_LABEL_TEXTSIZE;
    private float SPEED_UNITS_TEXTSIZE;
    private float SPEED_START_ANGLE = 10;                  // angle from horizontal on left side
    private float SPEED_END_ANGLE = 180 - SPEED_START_ANGLE;

    private float WIND_MARKER_HEIGHT;
    private float WIND_MARKER_WIDTH = WIND_MARKER_HEIGHT/4;

    private int COMPASS_CIRCLE_STROKE_COLOR;
    private int COMPASS_CIRCLE_FILL_COLOR;
    private int LARGE_COMPASS_POINT_COLOR;
    private int SMALL_COMPASS_POINT_COLOR;
    private int COMPASS_LABEL_TEXTCOLOR;

    private int SPEED_CIRCLE_STROKE_COLOR;
    private int SPEED_CIRCLE_FILL_COLOR;
    private int SPEED_LABEL_TEXTCOLOR;

    private int WIND_MARKER_COLOR;

    private Paint mPaint;
    private Paint mPaintText;
    private ShapeDrawable mLargeCompassPoint;
    private ShapeDrawable mSmallCompassPoint;
    private ShapeDrawable mWindDirectionMarker;
    private ShapeDrawable mWindSpeedMarker;

    private double mWindSpeed;
    private double mWindDirection;
    String mSpeedLabels[] = new String[]{"0", "15", "30", "45", "60"};
    String mSpeedUnits = "km/h";

    private Context mContext;

    public DirectionSpeedView(Context context) {
        // Created from code.
        super(context);
        mContext = context;
        initGraphics();
    }

    public DirectionSpeedView(Context context, AttributeSet attrSet) {
        // Created from resource.
        super(context, attrSet);
        mContext = context;
        TypedArray attrArr = context.getTheme()
                              .obtainStyledAttributes(attrSet, R.styleable.DirectionSpeedView, 0, 0);
        try {
            initDimensions(attrArr);
            initColors(attrArr);
         } finally {
            attrArr.recycle();
        }
        initGraphics();
    }

    public DirectionSpeedView(Context context, AttributeSet attrSet, int defaultStyle) {
        // Created through inflation.
        super(context, attrSet, defaultStyle);
        mContext = context;
        TypedArray attrArr = context.getTheme()
                .obtainStyledAttributes(attrSet, R.styleable.DirectionSpeedView, 0, 0);
        try {
            initDimensions(attrArr);
            initColors(attrArr);
        } finally {
            attrArr.recycle();
        }
        initGraphics();
    }

    public void setDirectionAndSpeed(double windDirection, double windSpeed) {
        // Set the wind direction, which is an angle in degrees with 0 = North, 90 = East,
        // -90 or 270 = West, and 180 or -180 = South; windSpeed is in m/sec.
        mWindDirection = windDirection;
        if (Utility.isMetric(mContext)) {
            mSpeedLabels = new String[]{"0", "15", "30", "45", "60"};
            mWindSpeed = 3.6 * windSpeed;               // 1 km/h = 3.6 meters/sec
            mSpeedUnits = "km/h";
        } else {
            mSpeedLabels = new String[]{"0", "10", "20", "30"};
            mWindSpeed = 2.237 * windSpeed;             // 1 mph = 2.237 meters/sec
            mSpeedUnits = "mph";
        }
        invalidate();
        requestLayout();
    }

    private void initColors(TypedArray attrArr) {
        COMPASS_CIRCLE_STROKE_COLOR =
                attrArr.getColor(R.styleable.DirectionSpeedView_colorCompassCircleStroke, Color.BLACK);
        COMPASS_CIRCLE_FILL_COLOR =
                attrArr.getColor(R.styleable.DirectionSpeedView_colorCompassCircleFill, Color.BLACK);
        LARGE_COMPASS_POINT_COLOR =
                attrArr.getColor(R.styleable.DirectionSpeedView_colorLargeCompassPoint, Color.BLACK);
        SMALL_COMPASS_POINT_COLOR =
                attrArr.getColor(R.styleable.DirectionSpeedView_colorSmallCompassPoint, Color.BLACK);
        SPEED_CIRCLE_FILL_COLOR =
                attrArr.getColor(R.styleable.DirectionSpeedView_colorSpeedCircle, Color.BLACK);
        WIND_MARKER_COLOR = attrArr.getColor(R.styleable.DirectionSpeedView_colorWindMarker, Color.BLACK);

        COMPASS_LABEL_TEXTCOLOR = Color.BLACK;
        SPEED_CIRCLE_STROKE_COLOR = Color.BLACK;
        SPEED_LABEL_TEXTCOLOR = Color.BLACK;
    }

    private void initDimensions(TypedArray attrArr) {
        mSize = attrArr.getDimension(R.styleable.DirectionSpeedView_preferredSize, 200);
        Log.e(LOG_TAG, "Attribute preferredSize is: " + String.valueOf(mSize));

        COMPASS_LABEL_TEXTSIZE = mSize/14;
        SPEED_LABEL_TEXTSIZE = mSize/20;
        SPEED_UNITS_TEXTSIZE = mSize/16;
        COMPASS_LABEL_OFFSET = 0.9f * COMPASS_LABEL_TEXTSIZE;
        LARGE_COMPASS_POINT_WIDTH = mSize/9;
        LARGE_COMPASS_POINT_HEIGHT = LARGE_COMPASS_POINT_WIDTH/2;
        WIND_MARKER_HEIGHT = LARGE_COMPASS_POINT_HEIGHT;

        W_SIZE =  Math.round(mSize);                                        // width of the graphic
        H_SIZE = W_SIZE - (int)Math.round(2*0.33*COMPASS_LABEL_TEXTSIZE);   // height of the graphic

        COMPASS_RADIUS = mSize/2 - (1.5f*COMPASS_LABEL_TEXTSIZE + COMPASS_LABEL_OFFSET +
                LARGE_COMPASS_POINT_HEIGHT);
        COMPASS_CIRCLE_STROKE_WIDTH = LARGE_COMPASS_POINT_HEIGHT/4;
        SMALL_COMPASS_POINT_WIDTH = 0.80f * LARGE_COMPASS_POINT_WIDTH;
        SMALL_COMPASS_POINT_HEIGHT = SMALL_COMPASS_POINT_WIDTH/2;
        COMPASS_LABEL_POSITION = LARGE_COMPASS_POINT_HEIGHT + COMPASS_LABEL_OFFSET;
        WIND_MARKER_WIDTH = WIND_MARKER_HEIGHT/4;

        SPEED_RADIUS = COMPASS_RADIUS - 1.8f * WIND_MARKER_HEIGHT;
        SPEED_START_ANGLE = 10;                  // angle from horizontal on left side
        SPEED_END_ANGLE = 180 - SPEED_START_ANGLE;
        SPEED_TICK_LENGTH = 0.7f * SPEED_LABEL_TEXTSIZE;
    }

    private void initGraphics() {
        // called from each of the three constructors
        mPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        mPaintText = new Paint(Paint.ANTI_ALIAS_FLAG);
        mLargeCompassPoint = createCompassPoint(LARGE_COMPASS_POINT_WIDTH,
                             LARGE_COMPASS_POINT_HEIGHT, LARGE_COMPASS_POINT_COLOR);
        mSmallCompassPoint = createCompassPoint(SMALL_COMPASS_POINT_WIDTH,
                             SMALL_COMPASS_POINT_HEIGHT, SMALL_COMPASS_POINT_COLOR);
        mWindDirectionMarker = createWindMarker(WIND_MARKER_WIDTH, WIND_MARKER_HEIGHT, WIND_MARKER_COLOR);
        mWindSpeedMarker = mWindDirectionMarker;
    }

    private ShapeDrawable createWindMarker(float width, float height, int color) {
        // Create a wind direction graphic which will be drawn on the compass circle at the
        // appropriate angle.
        float[] outerRadii = {3, 3, 3, 3, 3, 3, 3, 3};
        RoundRectShape roundRectShape = new RoundRectShape(outerRadii, null, null);
        ShapeDrawable windMarker = new ShapeDrawable(roundRectShape);
        windMarker.getPaint().setStyle(Paint.Style.FILL_AND_STROKE);
        windMarker.getPaint().setColor(color);
        int width2 = Math.round(width/2);
        windMarker.setBounds(-width2, 0, Math.round(width2 + width), Math.round(height));
        return windMarker;
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
            // wSpecMode == MeasureSpec.AT_MOST or wSpecMode == MeasureSpec.UNSPECIFIED = 0
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
    }

    @Override
    protected void onDraw(Canvas canvas) {
        // The origin (0,0) of the canvas is at the upper left corner of the graphic; the positive
        // x and y directions are to the right and down.
        super.onDraw(canvas);
        canvas.save();
        canvas.translate(W_SIZE/2, H_SIZE/2);     // origin (0,0) of the canvas is now at middle of graphic
        String[] largeLabels = {"S", "W", "N", "E"};
        drawCompassPoints(canvas, mLargeCompassPoint, COMPASS_RADIUS, 0, largeLabels);
        drawCompassPoints(canvas, mSmallCompassPoint, COMPASS_RADIUS, 45, new String[]{});
        drawCompassCircle(canvas);
        drawWindDirectionMarker(canvas, mWindDirectionMarker, COMPASS_RADIUS, mWindDirection);
        drawWindSpeedMarker(canvas, mWindSpeedMarker, SPEED_RADIUS, SPEED_START_ANGLE, SPEED_END_ANGLE);
        drawWindSpeedScale(canvas, SPEED_RADIUS, SPEED_START_ANGLE, SPEED_END_ANGLE, SPEED_TICK_LENGTH);
        drawWindUnits(canvas);
        canvas.restore();            // restore the canvas position, orientation to the saved state
    }

    private void drawCompassCircle(Canvas canvas) {
        mPaint.setStyle(Paint.Style.FILL);
        mPaint.setColor(COMPASS_CIRCLE_FILL_COLOR);
        canvas.drawCircle(0, 0, COMPASS_RADIUS, mPaint);
        mPaint.setStyle(Paint.Style.STROKE);
        mPaint.setStrokeWidth(COMPASS_CIRCLE_STROKE_WIDTH);
        mPaint.setColor(COMPASS_CIRCLE_STROKE_COLOR);
        canvas.drawCircle(0, 0, COMPASS_RADIUS, mPaint);
        mPaint.setStrokeWidth(1f);
    }

    private void drawWindUnits(Canvas canvas) {
        // Draw the wind speed units at the center of the graphic.
        canvas.save();
        canvas.translate(0, SPEED_LABEL_TEXTSIZE - 4);

        mPaintText.setColor(SPEED_LABEL_TEXTCOLOR);
        mPaintText.setTextSize(SPEED_UNITS_TEXTSIZE);
        mPaintText.setTextAlign(Paint.Align.CENTER);

        canvas.drawText(mSpeedUnits, 0, 0, mPaintText);
        canvas.restore();
    }

    private void drawWindSpeedMarker(Canvas canvas, ShapeDrawable windMarker, float radius,
                                     float angleStart, float angleEnd) {
        // Draw the marker for the wind speed.  Assumes that drawWindSpeedScale has already been
        // called.
        int length = mSpeedLabels.length;
        float fractionFullScale = (float) mWindSpeed/Float.parseFloat(mSpeedLabels[length - 1]);
        float speedAngle = -90 + angleStart + fractionFullScale*(angleEnd - angleStart);
        canvas.save();
//        Log.e(LOG_TAG, "Wind speed is " + String.valueOf(mWindSpeed));
//        Log.e(LOG_TAG, "Wind speed marker angle is " + String.valueOf(speedAngle));
        canvas.rotate(speedAngle);
        canvas.translate(0, -radius - WIND_MARKER_HEIGHT);
        windMarker.draw(canvas);
        canvas.restore();
    }

    private void drawWindSpeedScale(Canvas canvas, float radius, float angleStart, float angleEnd,
                                    float tickLength) {
        // Draws the wind speed scale.  Assumes that the canvas has been translated to the center
        // of the compass graphic.
        float angleIncrement = (angleEnd - angleStart)/(mSpeedLabels.length - 1);
        mPaint.setStyle(Paint.Style.FILL);
        mPaint.setColor(SPEED_CIRCLE_FILL_COLOR);
        canvas.drawCircle(0, 0, SPEED_RADIUS, mPaint);
        mPaint.setStyle(Paint.Style.STROKE);
        mPaint.setColor(SPEED_CIRCLE_STROKE_COLOR);
        canvas.drawCircle(0, 0, SPEED_RADIUS, mPaint);
        mPaint.setColor(SPEED_CIRCLE_STROKE_COLOR);

        mPaintText.setColor(SPEED_LABEL_TEXTCOLOR);
        mPaintText.setTextSize(SPEED_LABEL_TEXTSIZE);
        mPaintText.setTextAlign(Paint.Align.CENTER);

        float angle = -90 + angleStart;
        for (int i = 0; i < mSpeedLabels.length; i++) {
            canvas.save();
            canvas.rotate(angle);
            canvas.translate(0, -radius);
            canvas.drawLine(0, 0, 0, tickLength, mPaint);
            canvas.translate(0, tickLength + SPEED_LABEL_TEXTSIZE);
            canvas.drawText(mSpeedLabels[i], 0, 0, mPaintText);
            canvas.restore();
            angle += angleIncrement;
        }
    }

    private void drawWindDirectionMarker(Canvas canvas, ShapeDrawable windMarker, float radius,
                                         double windAngle) {
        // Draws the wind direction or wind speed marker windMarker at the radius and wind angle
        // specified.  Note that 0 deg on the compass graphic is S, but 0 deg for the wind direction
        // from the weather API is N, so we need to add 180 deg to the windAngle when we put in
        // onto the graphic
        canvas.save();
//        Log.e(LOG_TAG, "Wind direction angle is " + String.valueOf(windAngle));
        canvas.rotate((float) windAngle);
        canvas.translate(0, -radius);
        windMarker.draw(canvas);
        canvas.restore();
    }
    private void drawCompassPoints(Canvas canvas, ShapeDrawable compassPoint, float radius,
                                        float angleOffset, String[] labels) {
        // Draw and label the main compass points S, W, N, E.  Canvas must already be translated
        // to the center of the compass.  Use compassPoint as the graphic.
        mPaintText.setColor(COMPASS_LABEL_TEXTCOLOR);
        mPaintText.setTextSize(COMPASS_LABEL_TEXTSIZE);
        mPaintText.setTextAlign(Paint.Align.CENTER);
        float angle = angleOffset;
        for (int i = 0; i < 4; i++) {
            canvas.save();
            canvas.rotate(angle);         // rotate canvas angle deg ccw (graphic rotates angle deg cw)
            canvas.translate(0, radius);
            compassPoint.draw(canvas);
            if (labels.length > 0) {
                canvas.translate(0, COMPASS_LABEL_POSITION);
                canvas.rotate(-angle);
                canvas.translate(0, COMPASS_LABEL_TEXTSIZE / 2 - 2);
                canvas.drawText(labels[i], 0, 0, mPaintText);
            }
            canvas.restore();
            angle += 90;
        }
    }
}
