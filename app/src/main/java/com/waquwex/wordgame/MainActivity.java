package com.waquwex.wordgame;

import android.content.res.Configuration;
import android.graphics.Rect;
import android.os.Bundle;
import android.text.Spannable;
import android.text.SpannableString;
import android.text.style.BackgroundColorSpan;
import android.util.Log;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewTreeObserver;
import android.view.inputmethod.EditorInfo;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.constraintlayout.widget.ConstraintLayout;

import com.waquwex.wordgame.Utils.ArrayUtils;
import com.waquwex.wordgame.Views.WordleEditText;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.Locale;
import java.util.Random;

public class MainActivity extends AppCompatActivity {
    private ArrayList<String> allWords;
    private String randomWord;
    private final WordleEditText[] wordleEditTexts = new WordleEditText[6];
    private int activeWordleEditTextIndex = 0;
    private boolean gameOver = false;
    Button replayButton;
    TextView historyTextView;
    ViewGroup mainContainer;
    private static final char[] englishChars = {'A', 'B', 'C', 'D', 'E', 'F', 'G', 'H', 'I', 'J', 'K', 'L',
            'M', 'N', 'O', 'P', 'Q', 'R', 'S', 'T', 'U', 'V', 'W', 'X', 'Y', 'Z'};
    private int[] historyPoints = new int[26];
    private int mainContentDefaultHeight = 0;

    @Override
    public void onSaveInstanceState(@NonNull Bundle outState) {
        super.onSaveInstanceState(outState);

        outState.putString("randomWord", randomWord);
        outState.putIntArray("historyPoints", historyPoints);
        outState.putInt("activeWordleEditTextIndex", activeWordleEditTextIndex);
        String[] currentWords = new String[6];
        for (int i = 0; i < 6; i++) {
            currentWords[i] = wordleEditTexts[i].getText().toString();
        }

        outState.putStringArray("currentWords", currentWords);
        outState.putBoolean("gameOver", gameOver);
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        mainContainer = findViewById(R.id.mainContainer);
        if (mainContainer != null) {
            mainContentDefaultHeight = mainContainer.getLayoutParams().height;
        }

        readWordList();

        LinearLayout wordsRoot = findViewById(R.id.wordsRoot);
        for (int i = 0; i < 6; i++) {
            wordleEditTexts[i] = (WordleEditText) wordsRoot.getChildAt(i);
            wordleEditTexts[i].setOnEditorActionListener((v, actionId, event) -> {
                if (actionId == EditorInfo.IME_ACTION_DONE) {
                    if (wordleEditTexts[activeWordleEditTextIndex] == v && v.getText().length() == 5) {
                        submitWord();
                    }
                    return true; // Consume the event
                }
                return false; // Continue with the default behavior (e.g., newline character)
            });
        }

        replayButton = findViewById(R.id.replayButton);

        // Retrieve saved state, e.g when rotation changes
        if (savedInstanceState != null) {
            String randomWordBundle = savedInstanceState.getString("randomWord");
            if (randomWordBundle == null) {
                Random random = new Random();
                randomWord = allWords.get(random.nextInt(allWords.size())).toUpperCase(Locale.UK);
                Log.i("RANDOM", randomWord);
            } else {
                randomWord = randomWordBundle;
                Log.i("RANDOM", randomWord);
            }

            int[] historyPointsBundle = savedInstanceState.getIntArray("historyPoints");
            if (historyPointsBundle == null) {
                historyPoints = new int[26];
            } else {
                historyPoints = historyPointsBundle;
            }

            activeWordleEditTextIndex = savedInstanceState.getInt("activeWordleEditTextIndex");

            gameOver = savedInstanceState.getBoolean("gameOver");

            String[] currentWordsBundle = savedInstanceState.getStringArray("currentWords");
            if (currentWordsBundle != null) {
                for (int i = 0; i < 6; i++) {
                    wordleEditTexts[i].setText(currentWordsBundle[i]);
                    if (i < activeWordleEditTextIndex) {
                        wordleEditTexts[i].finalizeResult(randomWord);
                    }
                }

                if (gameOver) {
                    wordleEditTexts[activeWordleEditTextIndex].finalizeResult(randomWord);
                    replayButton.setVisibility(View.VISIBLE);
                }
            }
        } else {
            Random random = new Random();
            randomWord = allWords.get(random.nextInt(allWords.size())).toUpperCase(Locale.UK);
            historyPoints = new int[26];
        }

        if (!gameOver) {
            wordleEditTexts[activeWordleEditTextIndex].setEnabled(true);
        }

        historyTextView = findViewById(R.id.historyTextView);
        renderHistory(historyTextView);

        replayButton.setOnClickListener(view -> {
            gameOver = false;
            for (int i = 0; i < 6; i++) {
                wordleEditTexts[i].setText("");
                wordleEditTexts[i].reset();
            }

            historyPoints = new int[26];

            renderHistory(historyTextView);
            activeWordleEditTextIndex = 0;
            wordleEditTexts[activeWordleEditTextIndex].setEnabled(true);
            replayButton.setVisibility(View.INVISIBLE);
            Random random = new Random();
            randomWord = allWords.get(random.nextInt(allWords.size())).toUpperCase(Locale.UK);
        });

        // Get the root view
        // Listen for layout changes to detect keyboard visibility
        View rootView = getWindow().getDecorView().getRootView();
        rootView.getViewTreeObserver().addOnGlobalLayoutListener(new ViewTreeObserver.OnGlobalLayoutListener() {
            @Override
            public void onGlobalLayout() {
                Rect visibleFrame = new Rect();
                rootView.getWindowVisibleDisplayFrame(visibleFrame);
                int rootViewHeight = rootView.getHeight();
                int visibleHeight = visibleFrame.height();

                int orientation = getResources().getConfiguration().orientation;

                ConstraintLayout.LayoutParams layoutParams = (ConstraintLayout.LayoutParams) mainContainer.getLayoutParams();
                if ((rootViewHeight - visibleHeight) > (rootViewHeight * 0.15)) { // Keyboard is visible
                    // If all content is visible don't apply height change
                    Rect bottomViewRect = new Rect();
                    layoutParams.height = visibleHeight;
                    if (orientation == Configuration.ORIENTATION_PORTRAIT) {
                        mainContainer.getGlobalVisibleRect(bottomViewRect);
                        // if mainContainer not completely visible adjust height:
                        // @NOTE: There is some slight coordinate miscalculation
                        if (bottomViewRect.bottom > visibleHeight) {
                            mainContainer.setLayoutParams(layoutParams);
                        }
                    } else { // Landscape
                        mainContainer.setLayoutParams(layoutParams);
                    }
                } else {
                    layoutParams.height = mainContentDefaultHeight; // reset it to defaults
                    mainContainer.setLayoutParams(layoutParams);
                }
            }
        });
    }

    // Submit word
    private void submitWord() {
        String activeWord = wordleEditTexts[activeWordleEditTextIndex].getText().toString();

        if (activeWord.equals(randomWord)) {
            addToHistory(activeWord);
            gameOver(true);
        } else if (!validWord(activeWord)) {
            Toast.makeText(getApplicationContext(), "INVALID WORD!", Toast.LENGTH_SHORT).show();
        } else {
            if (activeWordleEditTextIndex == 5) {
                addToHistory(activeWord);
                gameOver(false);
            } else {
                wordleEditTexts[activeWordleEditTextIndex].finalizeResult(randomWord);
                activeWordleEditTextIndex++;
                wordleEditTexts[activeWordleEditTextIndex].setEnabled(true);
                addToHistory(activeWord);
            }
        }
    }

    // Add to history with points related to "contain"
    private void addToHistory(String activeWord) {
        for (int i = 0; i < 5; i++) {
            int charIndex = ArrayUtils.indexOf(englishChars, activeWord.charAt(i));
            if (activeWord.charAt(i) == randomWord.charAt(i)) {
                historyPoints[charIndex] = 3;
            } else if (randomWord.contains(String.valueOf(activeWord.charAt(i)))) {
                if (historyPoints[charIndex] != 3) {
                    historyPoints[charIndex] = 2;
                }
            } else {
                historyPoints[charIndex] = 1;
            }
        }
        renderHistory(historyTextView);
    }

    // Render letter history
    private void renderHistory(TextView historyTextView) {
        Spannable historySpannable = new SpannableString("A B C D E F G H I J K L M N O P Q R S T U V W X Y Z");
        int spanStartIndex = 0;
        for (int i = 0; i < 26; i++) {
            int color = 0xd9d9d9;
            if (historyPoints[i] == 1) {
                color = 0xFF808080;
            } else if (historyPoints[i] == 2) {
                color = 0xFF706e01;
            } else if (historyPoints[i] == 3) {
                color = 0xFF105422;
            }
            historySpannable.setSpan(new BackgroundColorSpan(color), spanStartIndex,
                    spanStartIndex + 1, Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
            spanStartIndex += 2;
        }

        historyTextView.setText(historySpannable, TextView.BufferType.SPANNABLE);
    }

    // Switch buttons when game is over
    private void gameOver(boolean won) {
        gameOver = true;
        if (won) {
            Toast.makeText(getApplicationContext(), "YOU WON!", Toast.LENGTH_LONG).show();
            wordleEditTexts[activeWordleEditTextIndex].finalizeResult(randomWord);
        } else {
            Toast.makeText(getApplicationContext(), "YOU LOST! The word was: " + randomWord, Toast.LENGTH_LONG).show();
            wordleEditTexts[activeWordleEditTextIndex].finalizeResult(randomWord);
        }
        replayButton.setVisibility(View.VISIBLE);
    }

    // Check word exists in list
    private boolean validWord(String word) {
        for (String w : allWords) {
            if (w.toUpperCase(Locale.UK).equals(word)) {
                return true;
            }
        }
        return false;
    }

    // Get all words as list from file(R.raw.word_list)
    private void readWordList() {
        allWords = new ArrayList<>();
        try {
            InputStream inputStream = getApplication().getResources().openRawResource(R.raw.word_list);
            BufferedReader reader = new BufferedReader(new InputStreamReader(inputStream));
            String line = reader.readLine();
            while (line != null) {
                allWords.add(line);
                line = reader.readLine();
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}