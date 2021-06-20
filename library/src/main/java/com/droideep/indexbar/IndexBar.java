package com.droideep.indexbar;

import android.annotation.SuppressLint;
import android.annotation.TargetApi;
import android.content.Context;
import android.content.res.TypedArray;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.RectF;
import android.os.Build;
import android.support.annotation.IntDef;
import android.util.AttributeSet;
import android.util.Log;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewGroup.LayoutParams;

import com.droideep.indexbar.utils.MetricsConverter;

/**
 * @author bri (http://my.oschina.net/droideep)
 * @date 15-5-9.
 */
public class IndexBar extends View {

    @IntDef({STATE_NORMAL, STATE_PRESSED})
    public @interface STATE {
    }

    public static final int STATE_NORMAL = 0;
    public static final int STATE_PRESSED = 1;

    private int mState = STATE_NORMAL;

    private static final String LOG_TAG = "IndexBar";

    /**
     * 设置索引条正常显示的背景颜色,
     */
    private int mIndexBarColor = Color.TRANSPARENT;

    /**
     * 设置索引条按压时的背景色
     */
    private int mIndexBarColorPressed = Color.GRAY;

    /**
     * 设置字体的颜色
     */
    private int mAlphabetTextColor = Color.BLACK;

    /**
     * 设置字体的大小
     */
    private float mAlphabetTextSize = MetricsConverter.dpToPx(getContext(), 10);

    /**
     * 设置索引条中字母的间距
     * <p>
     * 这个属性只有在 android:layout_height="wrap_content" 时有效。
     * 当 android:layout_height="match_parent" 时,需要通过字母的高度、{@link IndexBar#getMeasuredHeight()},
     * 来计算字母的间距，具体看{@link IndexBar#onMeasure(int, int)}的实现
     * </p>
     */
    private float mAlphabetPadding = MetricsConverter.dpToPx(getContext(), 5);

    /**
     * 设置索引条圆角,默认为直角矩形
     */
    private float mIndexBarRound = MetricsConverter.dpToPx(getContext(), 0);

    //默认在IndexBar以外，仍然可以滑动
    private boolean mWithinIndexBar = false;

    private String[] mSections = new String[0];

    private IIndexBarFilter mIndexBarFilter;

    private static final int INVALID_SECTION_INDEX = -1;

    private boolean mIsIndexing;

    private int mCurrentSectionPosition;


    /**
     * {@link android.graphics.Paint}, which is used for drawing the elements of
     * <b>Action Button</b>
     */
    protected final Paint paint = new Paint(Paint.ANTI_ALIAS_FLAG);

    public IndexBar(Context context) {
        super(context);
        initIndexBar();
    }

    public IndexBar(Context context, AttributeSet attrs) {
        super(context, attrs);
        initIndexBar(context, attrs, 0, 0);
    }

    public IndexBar(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        initIndexBar(context, attrs, defStyleAttr, 0);
    }

    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        super.onMeasure(widthMeasureSpec, heightMeasureSpec);
        //setMeasuredDimension(calculateMeasuredWidth(), calculateMeasureHeight());

        final int heightMode = MeasureSpec.getMode(heightMeasureSpec);
        final int heightSize = MeasureSpec.getSize(heightMeasureSpec);

        LayoutParams lp = getLayoutParams();
        // 获取IndexBar顶部和底部的margin，这里不用关心左右两边的margin
        int margin = 0;
        // 如果布局文件使用了margin属性时，需要根据margin、mAlphabetTextSize、
        if (lp instanceof ViewGroup.MarginLayoutParams) {
            ViewGroup.MarginLayoutParams mlp = (ViewGroup.MarginLayoutParams) lp;
            margin = mlp.topMargin + mlp.bottomMargin;
        }

        /**
         * 判断IndexBar高度的裁剪模式,如果{@link heightMode} 为{@link MeasureSpec#EXACTLY}，
         * 表示用户设置了一个确切的高度值，或者使用了{@link LayoutParams#MATCH_PARENT}属性。
         * 如果{@link heightMode} 为 {@link MeasureSpec#AT_MOST},
         * 表示用户使用了{@link LayoutParams#WRAP_CONTENT}属性，这时，我们会使用默认的{@link #mAlphabetPadding}，
         * 或者用户设置的值
         */
        if (heightMode == MeasureSpec.EXACTLY) { // 计算mAlphabetPadding
            final int length = mSections.length;
            mAlphabetPadding = (heightSize - length * mAlphabetTextSize - margin) / (length + 1);

            setMeasuredDimension(widthMeasureSpec, heightMeasureSpec);
        } else {
            setMeasuredDimension(calculateMeasuredWidth(), calculateMeasureHeight());
        }


    }

    private int calculateMeasuredWidth() {
        final int measuredWidth = (int) (getAlphabetTextSize() + getAlphabetPadding() * 2);
        Log.v(LOG_TAG, "Calculated measured width = " + measuredWidth);
        return measuredWidth;
    }

    private int calculateMeasureHeight() {
        final int length = mSections.length;
        final float measureHeight = length * getAlphabetTextSize() + (length + 1) * getAlphabetPadding();
        return (int) measureHeight;
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);
        Log.v(LOG_TAG, "Index Bar onDraw called");
        drawRect(canvas);
        drawSections(canvas);
    }

    @SuppressWarnings("all")
    @SuppressLint("ClickableViewAccessibility")
    @Override
    public boolean onTouchEvent(MotionEvent event) {
        super.onTouchEvent(event);
        int action = event.getAction();
        switch (action) {
            case MotionEvent.ACTION_DOWN:
                if (contains(event.getX(), event.getY())) {
                    mIsIndexing = true;
                    filterListItem(event.getY());
                    setState(STATE_PRESSED);
                    return true;
                } else {
                    mCurrentSectionPosition = INVALID_SECTION_INDEX;
                    return false;
                }
            case MotionEvent.ACTION_MOVE:
                if (mIsIndexing) {
                    if (mWithinIndexBar) {
                        if (contains(event.getX(), event.getY())) {
                            filterListItem(event.getY());
                            return true;
                        }
                    } else {
                        if (contains(event.getY())) {
                            filterListItem(event.getY());
                            return true;
                        }
                    }
                    mCurrentSectionPosition = INVALID_SECTION_INDEX;
                    return false;
                }
                return false;
            case MotionEvent.ACTION_UP:
                if (mIsIndexing) {
                    mIsIndexing = false;
                    mCurrentSectionPosition = INVALID_SECTION_INDEX;
                    if (mIndexBarFilter != null) {
                        mIndexBarFilter.filterList(0, INVALID_SECTION_INDEX, null);
                    }
                    setState(STATE_NORMAL);
                    return true;
                }
            default:
                Log.v(LOG_TAG, "Unrecognized motion event detected");
                return false;
        }
    }

    private boolean contains(float y) {
        return (y >= 0 && y <= getHeight());
    }

    private boolean contains(float x, float y) {
//        return (x >= getLeft() && y >= getTop() && (y <= getTop() + getMeasuredHeight()));
        return (x >= 0 && y >= 0 && x <= getWidth() && y <= getHeight());
    }

    private void filterListItem(float sideIndexY) {
        int top = getTop();
        mCurrentSectionPosition = (int) (((sideIndexY) - mAlphabetPadding) /
                ((getMeasuredHeight() - (2 * mAlphabetPadding)) / mSections.length));
        if (mCurrentSectionPosition >= 0 && mCurrentSectionPosition < mSections.length) {
            Log.d(LOG_TAG, "CurrentSectionPosition:" + mCurrentSectionPosition);
            String previewText = mSections[mCurrentSectionPosition];
            if (mIndexBarFilter != null) {
                mIndexBarFilter.filterList(sideIndexY, mCurrentSectionPosition, previewText);
            }
        }
    }

    private void initIndexBar() {
        initLayerType();
        Log.v(LOG_TAG, "Index Bar initialized");
    }

    private void initIndexBar(Context context, AttributeSet attrs, int defStyleAttr, int defStyleRes) {
        initLayerType();
        TypedArray attributes = context.getTheme().obtainStyledAttributes(attrs, R.styleable.IndexBar, defStyleAttr, defStyleRes);
        try {

            initIndexBarColor(attributes);
            initIndexBarColorPressed(attributes);
            initAlphabetTextColor(attributes);
            initAlphabetTextSize(attributes);
            initAlphabetOffset(attributes);
            initIndexBarRound(attributes);
            initWithinIndexBar(attributes);

        } catch (Exception e) {
            Log.e(LOG_TAG, "Unable to read attr", e);
        } finally {
            attributes.recycle();
        }
    }

    private void initWithinIndexBar(TypedArray attributes) {
        if (attributes.hasValue(R.styleable.IndexBar_withinIndexBar)) {
            mWithinIndexBar = attributes.getBoolean(R.styleable.IndexBar_withinIndexBar, mWithinIndexBar);
            Log.v(LOG_TAG, "Initialized Within Index Bar: " + getWithinIndexBar());
        }
    }

    private void initAlphabetOffset(TypedArray attributes) {
        if (attributes.hasValue(R.styleable.IndexBar_alphabetOffset)) {
            mAlphabetPadding = attributes.getDimension(R.styleable.IndexBar_alphabetOffset, mAlphabetPadding);
            Log.v(LOG_TAG, "Initialized Alphabet Offset: " + getAlphabetPadding());
        }
    }

    private void initAlphabetTextSize(TypedArray attributes) {
        if (attributes.hasValue(R.styleable.IndexBar_alphabetTextSize)) {
            mAlphabetTextSize = attributes.getDimension(R.styleable.IndexBar_alphabetTextSize, mAlphabetTextSize);
            Log.v(LOG_TAG, "Initialized Alphabet TextSize: " + getAlphabetTextSize());
        }
    }

    private void initAlphabetTextColor(TypedArray attributes) {
        if (attributes.hasValue(R.styleable.IndexBar_alphabetTextColor)) {
            mAlphabetTextColor = attributes.getColor(R.styleable.IndexBar_alphabetTextColor, mAlphabetTextColor);
            Log.v(LOG_TAG, "Initialized Alphabet TextColor: " + getAlphabetTextColor());
        }
    }

    private void initIndexBarColorPressed(TypedArray attributes) {
        if (attributes.hasValue(R.styleable.IndexBar_indexBarColorPressed)) {
            mIndexBarColorPressed = attributes.getColor(R.styleable.IndexBar_indexBarColorPressed, mIndexBarColorPressed);
            Log.v(LOG_TAG, "Initialized Index Bar Color Pressed: " + getIndexBarColorPressed());
        }
    }

    private void initIndexBarColor(TypedArray attributes) {
        if (attributes.hasValue(R.styleable.IndexBar_indexBarColor)) {
            mIndexBarColor = attributes.getColor(R.styleable.IndexBar_indexBarColor, mIndexBarColor);
            Log.v(LOG_TAG, "Initialized Index Bar Color: " + getIndexBarColor());
        }
    }

    private void initIndexBarRound(TypedArray attributes) {
        if (attributes.hasValue(R.styleable.IndexBar_indexBarRound)) {
            mIndexBarRound = attributes.getDimension(R.styleable.IndexBar_indexBarRound, mIndexBarRound);
            Log.v(LOG_TAG, "Initialized Index Bar Color: " + getIndexBarRound());
        }
    }

    /**
     * Initializes the layer type needed for shadows drawing
     * <p/>
     * Might be called if target API is HONEYCOMB (11) and higher
     */
    @TargetApi(Build.VERSION_CODES.HONEYCOMB)
    private void initLayerType() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.HONEYCOMB) {
            setLayerType(LAYER_TYPE_SOFTWARE, paint);
            Log.v(LOG_TAG, "Layer type initialized");
        }
    }

    private void drawRect(Canvas canvas) {
        resetPaint();
        paint.setStyle(Paint.Style.FILL);
        paint.setColor(getState() == STATE_NORMAL ? getIndexBarColor() : getIndexBarColorPressed());
        float l = 0;
        float t = 0;
        float r = l + getWidth();
        float b = t + getHeight();
        RectF rectF = new RectF(l, t, r, b);
        if (hasRound()) {
            canvas.drawRoundRect(rectF, mIndexBarRound, mIndexBarRound, paint);
        } else {
            canvas.drawRect(rectF, paint);
        }
    }

    private void drawSections(Canvas canvas) {
        if (mSections == null || mSections.length == 0)
            return;
        resetPaint();

        final int length = mSections.length;

        final float width = getWidth() - mAlphabetPadding * 2;
        final float height = (getHeight() - ((length + 1) * mAlphabetPadding)) / length;
        for (int i = 0; i < length; i++) {
            final float l = mAlphabetPadding;
            final float t = (i + 1) * mAlphabetPadding + i * height;
            final float r = l + width;
            final float b = t + height;

            RectF targetRect = new RectF(l, t, r, b);
            paint.setColor(mAlphabetTextColor);
            paint.setTextAlign(Paint.Align.CENTER);
            paint.setTextSize(mAlphabetTextSize);
            Paint.FontMetricsInt fontMetrics = paint.getFontMetricsInt();
            float baseline = targetRect.top + (targetRect.bottom - targetRect.top - fontMetrics.bottom + fontMetrics.top) / 2 - fontMetrics.top;

            canvas.drawText(mSections[i], targetRect.centerX(), baseline, paint);
        }
    }

    private boolean hasRound() {
        return getIndexBarRound() > 0.0f;
    }

    public void setState(@STATE int state) {
        if (mState != state) {
            mState = state;
            invalidate();
        }
    }

    public int getState() {
        return mState;
    }

    public int getIndexBarColor() {
        return mIndexBarColor;
    }

    public int getIndexBarColorPressed() {
        return mIndexBarColorPressed;
    }

    public int getAlphabetTextColor() {
        return mAlphabetTextColor;
    }

    public float getAlphabetTextSize() {
        return mAlphabetTextSize;
    }

    public void setAlphabetTextSize(float alphabetTextSize) {
        if (mAlphabetTextSize != alphabetTextSize) {
            mAlphabetTextSize = alphabetTextSize;
            invalidate();
        }
    }

    public float getAlphabetPadding() {
        return mAlphabetPadding;
    }

    public float getIndexBarRound() {
        return mIndexBarRound;
    }

    public boolean getWithinIndexBar() {
        return mWithinIndexBar;
    }

    public void setSections(String[] mSections) {
        this.mSections = mSections;
        invalidate();
    }

    /**
     * Resets the paint to its default values and sets initial flags to it
     * <p/>
     * Use this method before drawing the new element of the view
     */
    protected final void resetPaint() {
        paint.reset();
        paint.setFlags(Paint.ANTI_ALIAS_FLAG);
        Log.v(LOG_TAG, "Paint reset");
    }

    public interface IIndexBarFilter {
        void filterList(float sideIndex, int position, String previewText);
    }

    public void setIndexBarFilter(IIndexBarFilter indexBarFilter) {
        this.mIndexBarFilter = indexBarFilter;
    }
}
