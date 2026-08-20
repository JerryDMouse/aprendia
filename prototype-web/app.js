const KNOWLEDGE_BASE = [
  {
    id: "ciencias-fotosintesis",
    subject: "Ciencias naturales",
    title: "La fotosintesis",
    keywords: ["fotosintesis", "plantas", "sol", "hojas", "alimento"],
    content:
      "La fotosintesis es el proceso por el cual las plantas fabrican su alimento. Usan la luz del sol, agua del suelo y aire. Las hojas ayudan a realizar este proceso.",
  },
  {
    id: "matematicas-suma",
    subject: "Matematicas",
    title: "La suma",
    keywords: ["suma", "sumar", "adicion", "juntar", "total"],
    content:
      "La suma sirve para juntar cantidades y saber cuanto hay en total. Por ejemplo, si tienes 2 mangos y te dan 3 mas, ahora tienes 5 mangos.",
  },
  {
    id: "lenguaje-sustantivo",
    subject: "Lenguaje",
    title: "El sustantivo",
    keywords: ["sustantivo", "nombre", "persona", "animal", "cosa", "lugar"],
    content:
      "Un sustantivo es una palabra que nombra personas, animales, lugares o cosas. Por ejemplo: nina, perro, escuela, rio y cuaderno.",
  },
  {
    id: "ambiental-agua",
    subject: "Educacion ambiental",
    title: "Cuidado del agua",
    keywords: ["agua", "cuidar", "rio", "quebrada", "ahorrar"],
    content:
      "El agua se cuida cerrando la llave cuando no se usa, no botando basura en rios o quebradas y usando solo la cantidad necesaria para las actividades diarias.",
  },
];

const BLOCKED_WORDS = ["trampa", "copiar", "arma", "violencia", "robar", "droga"];
const HISTORY_KEY = "aprendia.history.v1";

const messagesEl = document.querySelector("#messages");
const emptyStateEl = document.querySelector("#empty-state");
const formEl = document.querySelector("#question-form");
const inputEl = document.querySelector("#question-input");
const micButtonEl = document.querySelector("#mic-button");
const clearHistoryEl = document.querySelector("#clear-history");

let history = loadHistory();

function normalize(value) {
  return value
    .toLowerCase()
    .normalize("NFD")
    .replace(/[\u0300-\u036f]/g, "")
    .replace(/[^a-z0-9ñ\s]/g, " ")
    .replace(/\s+/g, " ")
    .trim();
}

function loadHistory() {
  try {
    return JSON.parse(localStorage.getItem(HISTORY_KEY) || "[]");
  } catch {
    return [];
  }
}

function saveHistory() {
  localStorage.setItem(HISTORY_KEY, JSON.stringify(history));
}

function isUnsafeQuestion(question) {
  const normalized = normalize(question);
  return BLOCKED_WORDS.some((word) => normalized.includes(word));
}

function scoreEntry(question, entry) {
  const normalized = normalize(question);
  return entry.keywords.reduce((score, keyword) => {
    return normalized.includes(normalize(keyword)) ? score + 1 : score;
  }, 0);
}

function findBestEntry(question) {
  return KNOWLEDGE_BASE
    .map((entry) => ({ entry, score: scoreEntry(question, entry) }))
    .filter((result) => result.score > 0)
    .sort((a, b) => b.score - a.score)[0]?.entry;
}

function answerQuestion(question) {
  if (isUnsafeQuestion(question)) {
    return {
      text: "Eso no puedo hacerlo. Pero si puedo ayudarte a estudiar.",
      source: "Filtro de seguridad",
    };
  }

  const entry = findBestEntry(question);
  if (!entry) {
    return {
      text: "No encontre eso en mi material escolar.",
      source: "Base de conocimiento local",
    };
  }

  return {
    text: `Mira: ${entry.content}`,
    source: `${entry.subject}: ${entry.title}`,
  };
}

function renderHistory(streamLast = false) {
  messagesEl.innerHTML = "";
  emptyStateEl.style.display = history.length === 0 ? "block" : "none";
  if (history.length === 0) {
    return;
  }

  for (let index = 0; index < history.length; index += 1) {
    const item = history[index];
    appendMessage("user", item.question);
    const isLast = index === history.length - 1;
    if (isLast && streamLast) {
      appendStreamingMessage(item.answer, item.source);
    } else {
      appendMessage("assistant", item.answer, item.source);
    }
  }
}

function appendMessage(role, text, source = "") {
  const messageEl = createMessageBubble(role);
  messageEl.textContent = text;
  appendMeta(messageEl, text, source);
  messagesEl.appendChild(messageEl);
  messagesEl.scrollTop = messagesEl.scrollHeight;
}

function appendStreamingMessage(text, source = "") {
  const messageEl = createMessageBubble("assistant");
  const streamEl = document.createElement("div");
  messageEl.appendChild(streamEl);
  messagesEl.appendChild(messageEl);

  let index = 0;
  const timer = window.setInterval(() => {
    index = Math.min(index + 3, text.length);
    streamEl.textContent = text.slice(0, index);
    messagesEl.scrollTop = messagesEl.scrollHeight;
    if (index >= text.length) {
      window.clearInterval(timer);
      messageEl.classList.add("celebrate");
      appendMeta(messageEl, text, source);
    }
  }, 30);
}

function createMessageBubble(role) {
  const messageEl = document.createElement("div");
  messageEl.className = `message ${role}`;
  return messageEl;
}

function appendMeta(messageEl, text, source = "") {
  if (source) {
    const sourceEl = document.createElement("span");
    sourceEl.className = "source";
    sourceEl.textContent = source;
    messageEl.appendChild(sourceEl);
  }
  const listenButton = document.createElement("button");
  listenButton.className = "listen-button";
  listenButton.type = "button";
  listenButton.textContent = "Escuchar";
  listenButton.addEventListener("click", () => speak(text));
  messageEl.appendChild(listenButton);
}

function speak(text) {
  if (!("speechSynthesis" in window)) return;
  window.speechSynthesis.cancel();
  window.speechSynthesis.speak(new SpeechSynthesisUtterance(text));
}

function startVoiceInput() {
  const SpeechRecognition = window.SpeechRecognition || window.webkitSpeechRecognition;
  if (!SpeechRecognition) {
    inputEl.focus();
    return;
  }
  const recognition = new SpeechRecognition();
  recognition.lang = "es-CO";
  recognition.interimResults = false;
  recognition.maxAlternatives = 1;
  micButtonEl.classList.add("listening");
  recognition.onresult = (event) => {
    const transcript = event.results[0][0].transcript;
    inputEl.value = transcript;
    formEl.requestSubmit();
  };
  recognition.onend = () => micButtonEl.classList.remove("listening");
  recognition.onerror = () => {
    micButtonEl.classList.remove("listening");
    inputEl.focus();
  };
  recognition.start();
}

function ask(question) {
  const trimmed = question.trim();
  if (!trimmed) return;

  const result = answerQuestion(trimmed);
  history.push({
    question: trimmed,
    answer: result.text,
    source: result.source,
    createdAt: new Date().toISOString(),
  });
  saveHistory();
  renderHistory(true);
}

formEl.addEventListener("submit", (event) => {
  event.preventDefault();
  ask(inputEl.value);
  inputEl.value = "";
});

document.querySelectorAll("[data-question]").forEach((button) => {
  button.addEventListener("click", () => ask(button.dataset.question));
});

clearHistoryEl.addEventListener("click", () => {
  history = [];
  saveHistory();
  renderHistory();
});

micButtonEl.addEventListener("click", startVoiceInput);

renderHistory();
