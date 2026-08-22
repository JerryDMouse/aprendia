package com.aprendia.app;

import android.Manifest;
import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.speech.RecognitionListener;
import android.speech.RecognizerIntent;
import android.speech.SpeechRecognizer;
import android.speech.tts.TextToSpeech;
import android.speech.tts.UtteranceProgressListener;
import android.text.InputType;
import android.view.Gravity;
import android.view.View;
import android.view.animation.AnimationUtils;
import android.view.inputmethod.EditorInfo;
import android.view.inputmethod.InputMethodManager;
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
import android.widget.Toast;

import com.aprendia.app.data.HistoryStore;
import com.aprendia.app.domain.Answer;
import com.aprendia.app.domain.AnswerQuestionUseCase;
import com.aprendia.app.domain.ChatRecord;
import com.aprendia.app.knowledge.KnowledgeAssetsLoader;
import com.aprendia.app.knowledge.KnowledgeRepository;
import com.aprendia.app.llm.LlamaCppLocalLlmEngine;
import com.aprendia.app.llm.ModelFileStore;
import com.aprendia.app.safety.SafetyFilter;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public final class MainActivity extends Activity {
    private static final int REQUEST_RECORD_AUDIO = 100;
    private static final int REQUEST_MODEL_FILE = 200;
    private static final int STREAM_CHARS_PER_TICK = 3;
    private static final long STREAM_TICK_MS = 30;

    private LinearLayout messagesLayout;
    private LinearLayout emptyState;
    private EditText questionInput;
    private ScrollView chatScroll;
    private ImageButton micButton;
    private Button modelButton;
    private HistoryStore historyStore;
    private AnswerQuestionUseCase answerQuestionUseCase;
    private ModelFileStore modelFileStore;
    private TextToSpeech textToSpeech;
    private SpeechRecognizer speechRecognizer;
    private final Handler handler = new Handler(Looper.getMainLooper());
    private Typeface font;
    private ImageButton speakingListenButton;

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
        modelFileStore = new ModelFileStore(this);
        answerQuestionUseCase = new AnswerQuestionUseCase(
                new KnowledgeRepository(KnowledgeAssetsLoader.load(this)),
                new SafetyFilter(),
                new LlamaCppLocalLlmEngine(modelFileStore)
        );
        font = getResources().getFont(R.font.fredoka);

        setContentView(R.layout.activity_main);
        messagesLayout = findViewById(R.id.messages_layout);
        emptyState = findViewById(R.id.empty_state);
        chatScroll = findViewById(R.id.chat_scroll);
        questionInput = findViewById(R.id.question_input);
        micButton = findViewById(R.id.mic_button);
        modelButton = findViewById(R.id.model_button);

        configureComposer();
        configureChips();
        configureTopBar();
        configureTts();
        renderHistory(false);
    }

    @Override
    protected void onDestroy() {
        if (speechRecognizer != null) {
            speechRecognizer.destroy();
        }
        if (textToSpeech != null) {
            textToSpeech.stop();
            textToSpeech.shutdown();
        }
        handler.removeCallbacksAndMessages(null);
        super.onDestroy();
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == REQUEST_RECORD_AUDIO && grantResults.length > 0
                && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
            startVoiceInput();
        } else {
            setListeningState(false);
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == REQUEST_MODEL_FILE && resultCode == RESULT_OK && data != null && data.getData() != null) {
            importModel(data.getData());
        }
    }

    private void configureTts() {
        textToSpeech = new TextToSpeech(this, status -> {
            if (status == TextToSpeech.SUCCESS) {
                textToSpeech.setLanguage(new Locale("es", "CO"));
                textToSpeech.setOnUtteranceProgressListener(new UtteranceProgressListener() {
                    @Override
                    public void onStart(String utteranceId) {
                    }

                    @Override
                    public void onDone(String utteranceId) {
                        runOnUiThread(MainActivity.this::resetSpeakingListenButton);
                    }

                    @Override
                    public void onError(String utteranceId) {
                        runOnUiThread(MainActivity.this::resetSpeakingListenButton);
                    }
                });
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

        micButton.setOnClickListener(view -> startVoiceInput());
    }

    private void configureChips() {
        ((Button) findViewById(R.id.chip_plants)).setOnClickListener(view -> askFromChip("Que es la fotosintesis?"));
        ((Button) findViewById(R.id.chip_math)).setOnClickListener(view -> askFromChip("Que es una suma?"));
        ((Button) findViewById(R.id.chip_words)).setOnClickListener(view -> askFromChip("Que es un sustantivo?"));
        ((Button) findViewById(R.id.chip_water)).setOnClickListener(view -> askFromChip("Como cuidar el agua?"));
    }

    private void configureTopBar() {
        updateModelButton();
        modelButton.setTypeface(font, Typeface.BOLD);
        modelButton.setOnClickListener(view -> openModelPicker());

        Button newChatButton = findViewById(R.id.new_chat_button);
        newChatButton.setTypeface(font, Typeface.BOLD);
        newChatButton.setOnClickListener(view -> {
            historyStore.clear();
            renderHistory(false);
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
        questionInput.setText("");
        hideKeyboard();
        Toast.makeText(this, "Buscando en el material escolar...", Toast.LENGTH_SHORT).show();
        new Thread(() -> {
            Answer answer = answerQuestionUseCase.answer(question);
            runOnUiThread(() -> {
                historyStore.append(new ChatRecord(question, answer.getText(), answer.getSource(), System.currentTimeMillis()));
                renderHistory(true);
            });
        }).start();
    }

    private void openModelPicker() {
        Intent intent = new Intent(Intent.ACTION_OPEN_DOCUMENT);
        intent.addCategory(Intent.CATEGORY_OPENABLE);
        intent.setType("*/*");
        startActivityForResult(intent, REQUEST_MODEL_FILE);
    }

    private void importModel(Uri uri) {
        modelButton.setEnabled(false);
        modelButton.setText("Instalando...");
        Toast.makeText(this, "Copiando modelo local. Esto puede tardar.", Toast.LENGTH_LONG).show();
        new Thread(() -> {
            try {
                modelFileStore.importFrom(uri);
                runOnUiThread(() -> {
                    updateModelButton();
                    Toast.makeText(this, "Modelo local instalado.", Toast.LENGTH_LONG).show();
                });
            } catch (IOException error) {
                runOnUiThread(() -> {
                    updateModelButton();
                    Toast.makeText(this, "No se pudo instalar el modelo.", Toast.LENGTH_LONG).show();
                });
            }
        }).start();
    }

    private void updateModelButton() {
        if (modelButton == null) {
            return;
        }
        modelButton.setEnabled(true);
        modelButton.setText(modelFileStore.isModelInstalled() ? "LLM listo" : "Modelo");
    }

    private void startVoiceInput() {
        if (!SpeechRecognizer.isRecognitionAvailable(this)) {
            questionInput.requestFocus();
            showKeyboard();
            return;
        }
        if (checkSelfPermission(Manifest.permission.RECORD_AUDIO) != PackageManager.PERMISSION_GRANTED) {
            requestPermissions(new String[]{Manifest.permission.RECORD_AUDIO}, REQUEST_RECORD_AUDIO);
            return;
        }
        listen();
    }

    private void listen() {
        if (speechRecognizer == null) {
            speechRecognizer = SpeechRecognizer.createSpeechRecognizer(this);
            speechRecognizer.setRecognitionListener(recognitionListener);
        }
        Intent intent = new Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH);
        intent.putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM);
        intent.putExtra(RecognizerIntent.EXTRA_LANGUAGE, "es-CO");
        intent.putExtra(RecognizerIntent.EXTRA_MAX_RESULTS, 1);
        intent.putExtra(RecognizerIntent.EXTRA_PARTIAL_RESULTS, false);
        setListeningState(true);
        speechRecognizer.startListening(intent);
    }

    private void stopListening() {
        if (speechRecognizer != null) {
            speechRecognizer.stopListening();
        }
        setListeningState(false);
    }

    private void setListeningState(boolean listening) {
        micButton.setBackgroundResource(listening
                ? R.drawable.mic_button_listening_bg
                : R.drawable.mic_button_bg);
    }

    private final RecognitionListener recognitionListener = new RecognitionListener() {
        @Override
        public void onReadyForSpeech(Bundle params) {
        }

        @Override
        public void onBeginningOfSpeech() {
        }

        @Override
        public void onRmsChanged(float rmsdB) {
        }

        @Override
        public void onBufferReceived(byte[] buffer) {
        }

        @Override
        public void onEndOfSpeech() {
            setListeningState(false);
        }

        @Override
        public void onError(int error) {
            setListeningState(false);
        }

        @Override
        public void onResults(Bundle results) {
            setListeningState(false);
            ArrayList<String> matches = results.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION);
            if (matches != null && !matches.isEmpty()) {
                questionInput.setText(matches.get(0));
                askQuestion();
            }
        }

        @Override
        public void onPartialResults(Bundle partialResults) {
        }

        @Override
        public void onEvent(int eventType, Bundle params) {
        }
    };

    private void renderHistory(boolean streamLast) {
        messagesLayout.removeAllViews();
        List<ChatRecord> records = historyStore.load();
        if (records.isEmpty()) {
            emptyState.setVisibility(View.VISIBLE);
            ImageView mascot = findViewById(R.id.mascot_image);
            mascot.startAnimation(AnimationUtils.loadAnimation(this, R.anim.mascot_bounce));
            return;
        }
        emptyState.setVisibility(View.GONE);
        for (int index = 0; index < records.size(); index += 1) {
            ChatRecord record = records.get(index);
            addMessage(record.getQuestion(), true, null);
            boolean isLast = index == records.size() - 1;
            if (isLast && streamLast) {
                streamMessage(record.getAnswer(), record.getSource());
            } else {
                addMessage(record.getAnswer(), false, record.getSource());
            }
        }
        chatScroll.post(() -> chatScroll.fullScroll(View.FOCUS_DOWN));
    }

    private void addMessage(String text, boolean isUser, String source) {
        LinearLayout row = buildMessageRow(isUser);
        TextView message = buildMessageBubble(text, isUser);
        row.addView(message);
        if (!isUser) {
            appendMeta(row, text, source);
        }
        messagesLayout.addView(row);
    }

    private void streamMessage(String text, String source) {
        LinearLayout row = buildMessageRow(false);
        TextView message = buildMessageBubble("", false);
        row.addView(message);
        messagesLayout.addView(row);
        chatScroll.post(() -> chatScroll.fullScroll(View.FOCUS_DOWN));

        final int[] index = {0};
        Runnable tick = new Runnable() {
            @Override
            public void run() {
                index[0] = Math.min(index[0] + STREAM_CHARS_PER_TICK, text.length());
                message.setText(text.substring(0, index[0]));
                chatScroll.post(() -> chatScroll.fullScroll(View.FOCUS_DOWN));
                if (index[0] >= text.length()) {
                    message.startAnimation(AnimationUtils.loadAnimation(MainActivity.this, R.anim.celebrate));
                    appendMeta(row, text, source);
                } else {
                    handler.postDelayed(this, STREAM_TICK_MS);
                }
            }
        };
        handler.post(tick);
    }

    private LinearLayout buildMessageRow(boolean isUser) {
        LinearLayout row = new LinearLayout(this);
        row.setOrientation(LinearLayout.VERTICAL);
        row.setGravity(isUser ? Gravity.END : Gravity.START);

        LinearLayout.LayoutParams rowParams = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT);
        rowParams.setMargins(0, 10, 0, 10);
        row.setLayoutParams(rowParams);
        return row;
    }

    private TextView buildMessageBubble(String text, boolean isUser) {
        TextView message = new TextView(this);
        message.setText(text);
        message.setTextSize(17);
        message.setLineSpacing(0f, 1.2f);
        message.setTypeface(font);
        message.setTextColor(isUser ? Color.WHITE : inkColor);

        int maxWidth = (int) (getResources().getDisplayMetrics().widthPixels * 0.85f);
        message.setMaxWidth(maxWidth);
        message.setLayoutParams(new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.WRAP_CONTENT,
                LinearLayout.LayoutParams.WRAP_CONTENT));

        if (isUser) {
            message.setBackground(roundedBackground(blueColor, false));
            message.setPadding(dp(18), dp(14), dp(18), dp(14));
        } else {
            message.setBackground(roundedBackground(surfaceColor, true));
            message.setPadding(dp(18), dp(14), dp(18), dp(14));
        }
        return message;
    }

    private void appendMeta(LinearLayout row, String text, String source) {
        if (source != null && !source.isEmpty()) {
            TextView sourceView = new TextView(this);
            sourceView.setText("Fuente: " + source);
            sourceView.setTextColor(mintColor);
            sourceView.setTextSize(14);
            sourceView.setTypeface(font, Typeface.BOLD);
            sourceView.setPadding(dp(18), dp(4), dp(18), 0);
            row.addView(sourceView);
        }

        ImageButton listenButton = new ImageButton(this);
        listenButton.setImageResource(R.drawable.ic_volume);
        listenButton.setBackgroundResource(R.drawable.listen_button_bg);
        listenButton.setContentDescription("Escuchar respuesta");
        listenButton.setScaleType(ImageView.ScaleType.CENTER);
        listenButton.setPadding(dp(12), dp(12), dp(12), dp(12));
        LinearLayout.LayoutParams listenParams = new LinearLayout.LayoutParams(
                dp(48),
                dp(48));
        listenParams.setMargins(dp(18), dp(10), dp(18), 0);
        listenButton.setLayoutParams(listenParams);
        listenButton.setOnClickListener(view -> {
            if (speakingListenButton != null) {
                setListenButtonIdle(speakingListenButton);
            }
            setListenButtonSpeaking(listenButton);
            speak(text);
        });
        row.addView(listenButton);
    }

    private void setListenButtonSpeaking(ImageButton button) {
        speakingListenButton = button;
        button.setColorFilter(Color.WHITE);
        button.setBackgroundResource(R.drawable.listen_button_active_bg);
    }

    private void setListenButtonIdle(ImageButton button) {
        button.clearColorFilter();
        button.setBackgroundResource(R.drawable.listen_button_bg);
    }

    private void resetSpeakingListenButton() {
        if (speakingListenButton != null) {
            setListenButtonIdle(speakingListenButton);
            speakingListenButton = null;
        }
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
