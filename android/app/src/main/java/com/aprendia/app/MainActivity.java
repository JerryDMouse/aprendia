package com.aprendia.app;

import android.app.Activity;
import android.graphics.Color;
import android.os.Bundle;
import android.speech.tts.TextToSpeech;
import android.view.Gravity;
import android.view.inputmethod.InputMethodManager;
import android.content.Context;
import android.widget.Button;
import android.widget.EditText;
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
    private EditText questionInput;
    private HistoryStore historyStore;
    private AnswerQuestionUseCase answerQuestionUseCase;
    private TextToSpeech textToSpeech;
    private String lastAnswer = "";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        historyStore = new HistoryStore(this);
        answerQuestionUseCase = new AnswerQuestionUseCase(new KnowledgeRepository(), new SafetyFilter());
        textToSpeech = new TextToSpeech(this, status -> {
            if (status == TextToSpeech.SUCCESS) {
                textToSpeech.setLanguage(new Locale("es", "CO"));
            }
        });
        buildLayout();
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

    private void buildLayout() {
        LinearLayout root = new LinearLayout(this);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setPadding(24, 32, 24, 24);
        root.setBackgroundColor(Color.rgb(242, 247, 232));

        TextView title = new TextView(this);
        title.setText("AprendIA");
        title.setTextSize(34);
        title.setTextColor(Color.rgb(47, 125, 50));
        title.setGravity(Gravity.CENTER_HORIZONTAL);
        title.setTypeface(null, android.graphics.Typeface.BOLD);
        root.addView(title, matchWrap());

        TextView subtitle = new TextView(this);
        subtitle.setText("Pregunta sobre tu material escolar. Funciona sin internet.");
        subtitle.setTextSize(17);
        subtitle.setGravity(Gravity.CENTER_HORIZONTAL);
        subtitle.setPadding(0, 8, 0, 20);
        root.addView(subtitle, matchWrap());

        ScrollView scrollView = new ScrollView(this);
        messagesLayout = new LinearLayout(this);
        messagesLayout.setOrientation(LinearLayout.VERTICAL);
        scrollView.addView(messagesLayout);
        root.addView(scrollView, new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                0,
                1
        ));

        questionInput = new EditText(this);
        questionInput.setHint("Escribe tu pregunta escolar...");
        questionInput.setSingleLine(false);
        questionInput.setMinLines(1);
        questionInput.setMaxLines(3);
        root.addView(questionInput, matchWrap());

        Button askButton = new Button(this);
        askButton.setText("Preguntar");
        askButton.setOnClickListener(view -> askQuestion());
        root.addView(askButton, matchWrap());

        LinearLayout actions = new LinearLayout(this);
        actions.setOrientation(LinearLayout.HORIZONTAL);

        Button speakButton = new Button(this);
        speakButton.setText("Leer respuesta");
        speakButton.setOnClickListener(view -> speakLastAnswer());
        actions.addView(speakButton, new LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1));

        Button clearButton = new Button(this);
        clearButton.setText("Borrar historial");
        clearButton.setOnClickListener(view -> {
            historyStore.clear();
            renderHistory();
        });
        actions.addView(clearButton, new LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1));
        root.addView(actions, matchWrap());

        setContentView(root);
    }

    private void askQuestion() {
        String question = questionInput.getText().toString().trim();
        if (question.isEmpty()) {
            return;
        }
        Answer answer = answerQuestionUseCase.answer(question);
        lastAnswer = answer.getText();
        historyStore.append(new ChatRecord(question, answer.getText(), answer.getSource(), System.currentTimeMillis()));
        questionInput.setText("");
        hideKeyboard();
        renderHistory();
    }

    private void renderHistory() {
        messagesLayout.removeAllViews();
        List<ChatRecord> records = historyStore.load();
        if (records.isEmpty()) {
            addAssistantMessage("Hola, soy AprendIA. Preguntame algo de tu material escolar.", "");
            return;
        }
        for (ChatRecord record : records) {
            addUserMessage(record.getQuestion());
            addAssistantMessage(record.getAnswer(), record.getSource());
            lastAnswer = record.getAnswer();
        }
    }

    private void addUserMessage(String text) {
        TextView view = messageView(text, Color.rgb(216, 240, 255));
        view.setGravity(Gravity.END);
        messagesLayout.addView(view, matchWrap());
    }

    private void addAssistantMessage(String text, String source) {
        String fullText = source.isEmpty() ? text : text + "\n\nFuente: " + source;
        TextView view = messageView(fullText, Color.rgb(237, 248, 212));
        messagesLayout.addView(view, matchWrap());
    }

    private TextView messageView(String text, int backgroundColor) {
        TextView view = new TextView(this);
        view.setText(text);
        view.setTextSize(17);
        view.setTextColor(Color.rgb(36, 48, 31));
        view.setBackgroundColor(backgroundColor);
        view.setPadding(20, 16, 20, 16);
        LinearLayout.LayoutParams params = matchWrap();
        params.setMargins(0, 8, 0, 8);
        view.setLayoutParams(params);
        return view;
    }

    private void speakLastAnswer() {
        if (lastAnswer.isEmpty() || textToSpeech == null) {
            return;
        }
        textToSpeech.stop();
        textToSpeech.speak(lastAnswer, TextToSpeech.QUEUE_FLUSH, null, "aprendia-answer");
    }

    private void hideKeyboard() {
        InputMethodManager manager = (InputMethodManager) getSystemService(Context.INPUT_METHOD_SERVICE);
        if (manager != null) {
            manager.hideSoftInputFromWindow(questionInput.getWindowToken(), 0);
        }
    }

    private LinearLayout.LayoutParams matchWrap() {
        return new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
        );
    }
}
