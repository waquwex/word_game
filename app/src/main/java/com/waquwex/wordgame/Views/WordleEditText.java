package com.waquwex.wordgame.Views;

import android.animation.ValueAnimator;
import android.content.Context;
import android.content.res.TypedArray;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.AttributeSet;
import android.view.ViewGroup;
import android.view.inputmethod.EditorInfo;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.waquwex.wordgame.R;
import com.waquwex.wordgame.Utils.PixelUtils;

import java.util.Arrays;
import java.util.Locale;
import java.util.regex.Pattern;


public class WordleEditText extends androidx.appcompat.widget.AppCompatEditText {
    static final int LETTER_SAME_POS_COLOR = 0xFF105422;
    static final int LETTER_EXISTS_COLOR = 0xFF706e01;
    static final int LETTER_DEFAULT_COLOR = 0xFF808080;
    // Space value getting from XML
    private int spaceInPx = 0;
    private final int borderWidth = (int) PixelUtils.convertDpToPixel(2, getContext());
    private final Pattern englishLettersRegex = Pattern.compile("^[a-zA-Z]+$");
    private Paint textPaint;
    private Paint backgroundPaint;
    private Paint borderBoxPaint;
    private final int[] backgroundColors = new int[5];
    private boolean finalized = false;
    private int activeLetterBorderColor = Color.WHITE;

    public WordleEditText(@NonNull Context context) {
        super(context);
        init(context, null, 0, 0);
    }

    // Creating it from XML layout files
    public WordleEditText(@NonNull Context context, @Nullable AttributeSet attrs) {
        super(context, attrs);
        init(context, attrs, 0, 0);
    }

    // Creating it from XML layout files with default style attribute
    public WordleEditText(@NonNull Context context, @Nullable AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        init(context, attrs, defStyleAttr, 0);
    }

    private void init(@NonNull Context context, @Nullable AttributeSet attrs, int defStyleAttr, int defStyleRes) {
        // Get XML attributes
        TypedArray ta = null;
        try {
            ta = context.obtainStyledAttributes
                    (attrs, R.styleable.WordleEditText, 0, 0);
            int spaceDimension = ta.getLayoutDimension(R.styleable.WordleEditText_space, 2);
            spaceInPx = (int) PixelUtils.convertDpToPixel(spaceDimension, getContext());
        } finally {
            if (ta != null) {
                ta.recycle();
            }
        }

        // Assign letter background colors
        Arrays.fill(backgroundColors, Color.BLACK);

        // Remove default background
        setBackground(null);
        // Remove default cursor
        setCursorVisible(false);
        setSingleLine(true);
        // Submit action will be rely on this:
        setImeOptions(EditorInfo.IME_ACTION_DONE);

        // Make all letters uppercase also don't accept characters are not stored in English alphabet
        addTextChangedListener(new TextWatcher() {
            boolean selfChange = false;
            String previousValue = "";

            @Override
            public void beforeTextChanged(CharSequence charSequence, int start, int before, int count) {
                // Called to notify you that characters within `start` and `start + before` are about to be replaced with new text with length `count`.
            }

            @Override
            public void onTextChanged(CharSequence charSequence, int start, int before, int count) {
                // Called to notify you that somewhere within `start` and `start + before`, the text has been replaced with new text with length `count`.
            }

            @Override
            public void afterTextChanged(Editable editable) {
                // Prevent infinite loop
                if (selfChange) {
                    return;
                }

                //
                if (editable.toString().isEmpty()) {
                    previousValue = "";
                    return;
                }

                // if input is not valid restore old input
                if (!englishLettersRegex.matcher(editable.toString()).matches()) {
                    selfChange = true;
                    editable.replace(0, editable.length(), previousValue);
                    selfChange = false;
                } else {
                    selfChange = true;
                    editable.replace(0, editable.length(), editable.toString().toUpperCase(Locale.UK));
                    selfChange = false;
                }

                // Store previous value so we can restore invalid input
                previousValue = editable.toString();

                onLetterInserted();
            }
        });

        borderBoxPaint = new Paint();
        borderBoxPaint.setStyle(Paint.Style.FILL);

        // Initialize Paint s for canvas drawing
        backgroundPaint = new Paint();
        backgroundPaint.setStyle(Paint.Style.FILL);

        textPaint = new Paint();
        textPaint.setSubpixelText(true);
        textPaint.setAntiAlias(true);
        setTextSize(0); // Prevent to display invalid cursor
    }

    public void finalizeResult(String result) {
        if (result.length() != 5 || getText().length() != 5) {
            throw new RuntimeException();
        }
        setEnabled(false);

        String enteredText = getText().toString();
        // Set new background colors
        for (int i = 0; i < 5; i++) {
            if (result.charAt(i) == enteredText.charAt(i)) {
                backgroundColors[i] = LETTER_SAME_POS_COLOR; // Green
            } else if (result.contains(String.valueOf(enteredText.charAt(i)))) {
                backgroundColors[i] = LETTER_EXISTS_COLOR; // Yellow
            } else {
                backgroundColors[i] = LETTER_DEFAULT_COLOR; // Gray
            }
        }
        finalized = true;
        invalidate();
    }

    private void onLetterInserted() {
        ValueAnimator animator = ValueAnimator.ofArgb(Color.WHITE, Color.GREEN); // Interpolate from white to green
        animator.setDuration(150);
        animator.addUpdateListener(new ValueAnimator.AnimatorUpdateListener() {
            @Override
            public void onAnimationUpdate(@NonNull ValueAnimator animation) {
                activeLetterBorderColor = (int) animation.getAnimatedValue();
                invalidate(); // Redraw the view to reflect the updated value
            }
        });
        animator.start();
    }

    public void reset() {
        finalized = false;
        Arrays.fill(backgroundColors, Color.BLACK);
    }

    @Override
    protected void onDraw(Canvas canvas) {
        if (getText() == null) {
            return;
        }

        String enteredText = getText().toString();

        // center canvas with respecting aspect ratio
        int blockSize = getMeasuredHeight();
        if ((5 * blockSize) > getMeasuredWidth()) {
            blockSize = getMeasuredWidth() / 5;
        }

        blockSize -= (2 * spaceInPx);

        int marginTop = (getMeasuredHeight() - blockSize) / 2;
        int marginLeft = (getMeasuredWidth() - (blockSize * 5)) / 2;

        // Draw letter backgrounds
        for (int i = 0; i < 5; i++) {
            // set active letter box border color
            if (i == enteredText.length() - 1 && !finalized) {
                borderBoxPaint.setColor(activeLetterBorderColor);
            } else {
                borderBoxPaint.setColor(Color.WHITE);
            }

            int left = marginLeft + (i * blockSize) + spaceInPx;
            int right = left + blockSize - spaceInPx;
            int top = marginTop + spaceInPx;
            int bottom = top + blockSize - spaceInPx;

            // Draw borders
            canvas.drawRect(
                    left,
                    top,
                    right,
                    bottom,
                    borderBoxPaint);

            // Draw background
            backgroundPaint.setColor(backgroundColors[i]);
            canvas.drawRect(
                    left + borderWidth,
                    top + borderWidth,
                    right - borderWidth,
                    bottom - borderWidth,
                    backgroundPaint);

            // Draw letters
            if (i < enteredText.length()) {
                textPaint.setTextSize(blockSize);
                textPaint.setTypeface(getTypeface());
                textPaint.setColor(Color.WHITE);

                canvas.drawText(
                        String.valueOf(enteredText.charAt(i)),
                        left + borderWidth + blockSize / 8.2f,
                        bottom - borderWidth - blockSize / 9.5f,
                        textPaint);
            }
        }
    }

    // Remember views are able to handle Bundle on save and on restore
    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        int heightMode = MeasureSpec.getMode(heightMeasureSpec);
        int heightSize = MeasureSpec.getSize(heightMeasureSpec);
        int widthMode = MeasureSpec.getMode(widthMeasureSpec);
        int widthSize = MeasureSpec.getSize(widthMeasureSpec);

        // UNSPECIFIED: wrap_content
        // AT_MOST: match_parent
        if ((heightMode == MeasureSpec.EXACTLY) && (widthMode == MeasureSpec.EXACTLY)) {
            setMeasuredDimension(widthSize, heightSize);
        } else if ((heightMode == MeasureSpec.AT_MOST) && (widthMode == MeasureSpec.EXACTLY)) {
            int calculatedHeightSize = widthSize / 5;
            setMeasuredDimension(widthSize, calculatedHeightSize);
        } else if ((heightMode == MeasureSpec.EXACTLY) && (widthMode == MeasureSpec.AT_MOST)) {
            int calculatedWidthSize = heightSize * 5;
            setMeasuredDimension(calculatedWidthSize, heightSize);
        } else if ((heightMode == MeasureSpec.AT_MOST) && (widthMode == MeasureSpec.AT_MOST)) {
            int calculatedHeightSize = widthSize / 5;
            setMeasuredDimension(widthSize, calculatedHeightSize);
        } else if ((heightMode == MeasureSpec.UNSPECIFIED) && (widthMode == MeasureSpec.AT_MOST)) {
            int calculatedHeightSize = widthSize / 5;
            setMeasuredDimension(widthSize, calculatedHeightSize);
        } else if ((heightMode == MeasureSpec.AT_MOST) && (widthMode == MeasureSpec.UNSPECIFIED)) {
            int calculatedWidthSize = heightSize * 5;
            setMeasuredDimension(calculatedWidthSize, heightSize);
        } else if ((heightMode == MeasureSpec.UNSPECIFIED) && (widthMode == MeasureSpec.UNSPECIFIED)) {
            setMeasuredDimension(0, 0);
        } else {
            setMeasuredDimension(widthSize, heightSize);
        }
    }

    // Get sibling count so we can adjust height to prevent pixel leftover
    // Extra prevention
    public int getSiblingCount() {
        if (getParent() instanceof ViewGroup) {
            ViewGroup parent = (ViewGroup) getParent();
            return parent.getChildCount();
        } else {
            return 1; // No parent or parent is not a ViewGroup
        }
    }
}