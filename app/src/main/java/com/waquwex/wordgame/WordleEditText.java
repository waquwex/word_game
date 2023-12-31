package com.waquwex.wordgame;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Typeface;
import android.os.CountDownTimer;
import android.os.Parcelable;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.AttributeSet;
import android.view.inputmethod.EditorInfo;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import java.util.Locale;
import java.util.regex.Pattern;

public class WordleEditText extends androidx.appcompat.widget.AppCompatEditText {
    private Paint borderPaint;
    private Paint textPaint;
    private Paint backgroundPaint;

    private int[] backgroundColors = {0x00000000, 0x00000000, 0x00000000, 0x00000000,0x00000000};
    private boolean animatingLetterInsert = false;
    private int letterInsertAnimationTimer = 0;
    private boolean finalized = false;

    private CountDownTimer letterInsertAnimCountDownTimer;

    public void finalizeResult(String result) {
        if (result.length() != 5 || getText().length() != 5) {
            throw new RuntimeException();
        }
        setEnabled(false);

        String enteredText = getText().toString();
        // Set new background colors
        for (int i = 0; i < 5; i++) {
            if (result.charAt(i) == enteredText.charAt(i)) {
                backgroundColors[i] = 0xFF105422; // Green
            } else if (result.contains(String.valueOf(enteredText.charAt(i)))) {
                backgroundColors[i] = 0xFF706e01; // Yellow
            } else {
                backgroundColors[i] = 0xFF808080; // Gray
            }
        }

        finalized = true;
        invalidate();

        // @TODO: Play finalize animation
    }

    public void resetColors() {
        finalized = false;
        backgroundColors = new int[]{0x00000000, 0x00000000, 0x00000000, 0x00000000, 0x00000000};
    }

    public WordleEditText(@NonNull Context context) {
        super(context);
        init();
    }

    public WordleEditText(@NonNull Context context, @Nullable AttributeSet attrs) {
        super(context, attrs);
        init();
    }

    public WordleEditText(@NonNull Context context, @Nullable AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        init();
    }

    private void init() {
        setBackground(null);
        setCursorVisible(false);
        setSingleLine(true);
        setImeOptions(EditorInfo.IME_ACTION_DONE);

        // Make all letters uppercase also don't accept characters are not stored in English alphabet
        addTextChangedListener(new TextWatcher() {
            boolean selfChange = false;
            String previousValue = "";

            Pattern pattern = Pattern.compile("^[a-zA-Z]+$");

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
                if (selfChange) {
                    return;
                }

                if (editable.toString().isEmpty()) {
                    previousValue = "";
                    return;
                }

                if (!pattern.matcher(editable.toString()).matches()) {
                    selfChange = true;
                    editable.replace(0, editable.length(), previousValue);
                    selfChange = false;
                } else {
                    selfChange = true;
                    editable.replace(0, editable.length(), editable.toString().toUpperCase(Locale.UK));
                    selfChange = false;
                }

                previousValue = editable.toString();
                // Get the text from the EditText
                startLetterInsertedAnimation();
            }
        });

        // Initialize paint for drawing border
        borderPaint = new Paint();
        borderPaint.setStyle(Paint.Style.STROKE);
        borderPaint.setColor(Color.GRAY);
        borderPaint.setStrokeWidth(5);

        backgroundPaint = new Paint();
        backgroundPaint.setStyle(Paint.Style.FILL);

        textPaint = new Paint();
        textPaint.setColor(Color.WHITE);
        textPaint.setTypeface(Typeface.MONOSPACE);
        setTextSize(0); // Prevent to display invalid cursor
    }

    private void startLetterInsertedAnimation() {
        if (animatingLetterInsert) {
            return;
        }
        animatingLetterInsert = true;
        letterInsertAnimCountDownTimer = new CountDownTimer(255, 25) {
            @Override
            public void onTick(long millisUntilFinished) {
                letterInsertAnimationTimer = 255 - (int) millisUntilFinished;
                invalidate();
            }

            @Override
            public void onFinish() {
                animatingLetterInsert = false;
                letterInsertAnimationTimer = 0;
            }
        };

        letterInsertAnimCountDownTimer.start();
    }

    @Override
    protected void onDraw(Canvas canvas) {
        if (getText() == null) {
            return;
        }

        String enteredText = getText().toString();

        //super.onDraw(canvas);
        int block = getMeasuredHeight() - 4;

        if (!finalized) {
            for (int i = 0; i < 5; i++) {
                if (i == enteredText.length() - 1) {
                    if (animatingLetterInsert) {
                        int color = 0xFF000000; // white color
                        color += letterInsertAnimationTimer << 8;
                        borderPaint.setColor(color);
                    } else {
                        borderPaint.setColor(0xFF00FF00);
                    }
                } else {
                    borderPaint.setColor(Color.WHITE);
                }

                int left = block * i;
                int right = block * (i + 1);
                canvas.drawRect(left + i * 18 + 4, 4, right + i * 18 + 4, block, borderPaint);
            }
        }

            for (int i = 0; i < 5; i++) {
                backgroundPaint.setColor(backgroundColors[i]);
                int left = block * i;
                int right = block * (i + 1);
                canvas.drawRect(left + i * 18 + 4, 4, right + i * 18 + 4, block, backgroundPaint);
            }

            textPaint.setTextSize(getMeasuredHeight() - 26);
            for (int i = 0; i < enteredText.length(); i++) {
                canvas.drawText(String.valueOf(enteredText.charAt(i)),
                        i * block + i * 18 + Math.round(getMeasuredHeight() / 3.75),
                        Math.round(getMeasuredHeight() / 1.25),
                        textPaint);
            }
    }

    // Remember views are able to handle Bundle on save and on restore
    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        int heightMode = MeasureSpec.getMode(heightMeasureSpec);
        int heightSize = MeasureSpec.getSize(heightMeasureSpec);

        int widthMode = MeasureSpec.getMode(widthMeasureSpec);
        int defaultWidthSize = MeasureSpec.getSize(widthMeasureSpec);
        int widthSize = heightSize * 5 + 4 * 18;

        if (widthSize > defaultWidthSize) {
            widthSize = defaultWidthSize;
            heightSize = (widthSize - 4 * 18) / 5;
        }

        // Convert the dimensions from pixels to density-independent pixels (DP)
        float density = getResources().getDisplayMetrics().density;
        int widthInDp = (int) (widthSize / density);
        int heightInDp = (int) (heightSize / density);

        // Log or use the obtained dimensions in DP as needed
        // For example, you can use widthInDp and heightInDp in your custom view logic

        // Call setMeasuredDimension to set the measured width and height
        setMeasuredDimension(widthSize, heightSize);
    }
}
