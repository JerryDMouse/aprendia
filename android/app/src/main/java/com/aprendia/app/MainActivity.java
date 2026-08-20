package com.aprendia.app;

import android.app.Activity;
import android.os.Bundle;
import android.speech.tts.TextToSpeech;
import android.text.InputType;
import android.view.Gravity;
import android.view.View;
import android.view.inputmethod.EditorInfo;
import android.view.inputmethod.InputMethodManager;
import android.content.Context;
import android.graphics.Color;
import android.graphics.Typeface;
import android.graphics.drawable.GradientDrawable;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;

import com.aprendia.app.data.HistoryStore;
import com.aprendia.app.domain.Answer;
import com.aprendia.app.domain.AnswerQuestionUseCase;
import com.aprendia.app.domain.ChatRecord;
import com.aprendia.app.knowledge.KnowledgeRepository;
import com.aprendia.app.safety.SafetyFilter;

import java.util.List;
import java.util.Locale;

public final class MainActivity extends Activity {
    private LinearLayout messagesLayout;
    private LinearLayout emptyState;
    private EditText questionInput;
    private ScrollView chatScroll;
    private HistoryStore historyStore;
    private AnswerQuestionUseCase answerQuestionUseCase;
    private TextToSpeech textToSpeech;
    private Typeface font;

    private final int bgColor = Color.rgb(240, 244, 248);
    private final int surfaceColor = Color.rgb(255, 255, 255);
    private final int inkColor = Color.rgb(31, 41, 55);
    private final int mutedColor = Color.rgb(107, 114, 128);
    private final int mintColor = Color.rgb(16, 185, 129);
    private final int blueColor = Color.rgb(74, 125, 255);
    private final int borderColor = Color.rgb(229, 231, 235);

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        historyStore = new HistoryStore(this);
        answerQuestionUseCase = new AnswerQuestionUseCase(new KnowledgeRepository(), new SafetyFilter());
        font = getResources().getFont(R.font.fredoka);

        setContentView(R.layout.activity_main);
        messagesLayout = findViewById(R.id.messages_layout);
        emptyState = findViewById(R.id.empty_state);
        chatScroll = findViewById(R.id.chat_scroll);
        questionInput = findViewById(R.id.question_input);

        configureComposer();
        configureChips();
        configureTopBar();
        configureTts();
        renderHistory();
    }

    @Override
    protected void onDestroy() {
        if (textToSpeech != null) {
            textToSpeech.stop();
            textToSpeech.shutdown();
        }
        super.onDestroy();
    }

    private void configureTts() {
        textToSpeech = new TextToSpeech(this, status -> {
            if (status == TextToSpeech.SUCCESS) {
                textToSpeech.setLanguage(new Locale("es", "CO"));
            }
        });
    }

    private void configureComposer() {
        questionInput.setTypeface(font);
        questionInput.setImeOptions(EditorInfo.IME_ACTION_SEND);
        questionInput.setRawInputType(InputType.TYPE_CLASS_TEXT);
        questionInput.setOnEditorActionListener((view, actionId, event) -> {
            if (actionId == EditorInfo.IME_ACTION_SEND) {
                askQuestion();
                return true;
            }
            return false;
        });

        ImageButton sendButton = findViewById(R.id.send_button);
        sendButton.setOnClickListener(view -> askQuestion());

        ImageButton micButton = findViewById(R.id.mic_button);
        micButton.setOnClickListener(view -> {
            questionInput.requestFocus();
            showKeyboard();
        });
    }

    private void configureChips() {
        ((Button) findViewById(R.id.chip_plants)).setOnClickListener(view -> askFromChip("Que es la fotosintesis?"));
        ((Button) findViewById(R.id.chip_math)).setOnClickListener(view -> askFromChip("Que es una suma?"));
        ((Button) findViewById(R.id.chip_words)).setOnClickListener(view -> askFromChip("Que es un sustantivo?"));
        ((Button) findViewById(R.id.chip_water)).setOnClickListener(view -> askFromChip("Como cuidar el agua?"));
    }

    private void configureTopBar() {
        Button newChatButton = findViewById(R.id.new_chat_button);
        newChatButton.setTypeface(font, Typeface.BOLD);
        newChatButton.setOnClickListener(view -> {
            historyStore.clear();
            renderHistory();
        });
    }

    private void askFromChip(String question) {
        questionInput.setText(question);
        askQuestion();
    }

    private void askQuestion() {
        String question = questionInput.getText().toString().trim();
        if (question.isEmpty()) {
            return;
        }
        Answer answer = answerQuestionUseCase.answer(question);
        historyStore.append(new ChatRecord(question, answer.getText(), answer.getSource(), System.currentTimeMillis()));
        questionInput.setText("");
        hideKeyboard();
        renderHistory();
    }

    private void renderHistory() {
        messagesLayout.removeAllViews();
        List<ChatRecord> records = historyStore.load();
        if (records.isEmpty()) {
            emptyState.setVisibility(View.VISIBLE);
            ImageView mascot = findViewById(R.id.mascot_image);
            mascot.startAnimation(android.view.animation.AnimationUtils.loadAnimation(this, R.anim.mascot_bounce));
            return;
        }
        emptyState.setVisibility(View.GONE);
        for (ChatRecord record : records) {
            addMessage(record.getQuestion(), true, null);
            addMessage(record.getAnswer(), false, record.getSource());
        }
        chatScroll.post(() -> chatScroll.fullScroll(View.FOCUS_DOWN));
    }

    private void addMessage(String text, boolean isUser, String source) {
        LinearLayout row = new LinearLayout(this);
        row.setOrientation(LinearLayout.VERTICAL);
        row.setGravity(isUser ? Gravity.END : Gravity.START);

        LinearLayout.LayoutParams rowParams = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT);
        rowParams.setMargins(0, 10, 0, 10);
        row.setLayoutParams(rowParams);

        TextView message = new TextView(this);
        message.setText(text);
        message.setTextSize(17);
        message.setLineSpacing(0f, 1.2f);
        message.setTypeface(font);
        message.setTextColor(isUser ? Color.WHITE : inkColor);

        int maxWidth = (int) (getResources().getDisplayMetrics().widthPixels * 0.85f);
        message.setMaxWidth(maxWidth);
        LinearLayout.LayoutParams messageParams = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.WRAP_CONTENT,
                LinearLayout.LayoutParams.WRAP_CONTENT);
        message.setLayoutParams(messageParams);

        if (isUser) {
            message.setBackground(roundedBackground(blueColor, false));
            message.setPadding(dp(18), dp(14), dp(18), dp(14));
        } else {
            message.setBackground(roundedBackground(surfaceColor, true));
            message.setPadding(dp(18), dp(14), dp(18), dp(14));
        }

        row.addView(message);

        if (!isUser && source != null && !source.isEmpty()) {
            TextView sourceView = new TextView(this);
            sourceView.setText("Fuente: " + source);
            sourceView.setTextColor(mintColor);
            sourceView.setTextSize(14);
            sourceView.setTypeface(font, Typeface.BOLD);
            sourceView.setPadding(dp(18), dp(4), dp(18), 0);
            row.addView(sourceView);
        }

        if (!isUser) {
            Button listenButton = new Button(this);
            listenButton.setText("Leer respuesta");
            listenButton.setTypeface(font, Typeface.BOLD);
            listenButton.setTextColor(inkColor);
            listenButton.setTextSize(14);
            listenButton.setBackground(listenBackground());
            LinearLayout.LayoutParams listenParams = new LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.WRAP_CONTENT,
                    dp(44));
            listenParams.setMargins(dp(18), dp(10), dp(18), 0);
            listenButton.setLayoutParams(listenParams);
            listenButton.setOnClickListener(view -> speak(text));
            row.addView(listenButton);
        }

        messagesLayout.addView(row);
    }

    private GradientDrawable roundedBackground(int color, boolean outlined) {
        GradientDrawable drawable = new GradientDrawable();
        drawable.setColor(color);
        drawable.setCornerRadii(new float[]{dp(28), dp(28), dp(28), dp(28), dp(28), dp(28), dp(10), dp(10)});
        if (outlined) {
            drawable.setStroke(dp(2), borderColor);
        }
        return drawable;
    }

    private GradientDrawable listenBackground() {
        GradientDrawable drawable = new GradientDrawable();
        drawable.setColor(bgColor);
        drawable.setStroke(dp(2), borderColor);
        drawable.setCornerRadius(dp(999));
        return drawable;
    }

    private void speak(String text) {
        if (text == null || text.isEmpty() || textToSpeech == null) {
            return;
        }
        textToSpeech.stop();
        textToSpeech.speak(text, TextToSpeech.QUEUE_FLUSH, null, "aprendia-answer");
    }

    private void hideKeyboard() {
        InputMethodManager manager = (InputMethodManager) getSystemService(Context.INPUT_METHOD_SERVICE);
        if (manager != null) {
            manager.hideSoftInputFromWindow(questionInput.getWindowToken(), 0);
        }
    }

    private void showKeyboard() {
        InputMethodManager manager = (InputMethodManager) getSystemService(Context.INPUT_METHOD_SERVICE);
        if (manager != null) {
            manager.showSoftInput(questionInput, InputMethodManager.SHOW_IMPLICIT);
        }
    }

    private int dp(int value) {
        return Math.round(value * getResources().getDisplayMetrics().density);
    }
}