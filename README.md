# AprendIA

AprendIA es una app educativa offline-first para niños de básica primaria en zonas rurales de Colombia. El objetivo es ayudar a resolver dudas, preparar evaluaciones y apoyar tareas escolares usando solo material escolar precargado, local y de solo lectura.

## Características

| Componente | Estado |
|---|---|
| App Android (v0.4.0) | Disponible en `android/` |
| Demo web offline | Disponible en `prototype-web/` |
| Base de conocimiento local | Precargada, cerrada y de solo lectura (117 temas) |
| Búsqueda offline | Textual por palabras clave, priorizando coincidencias específicas |
| Respuestas controladas | Basadas solo en fragmentos encontrados |
| LLM local | Integrado con `llama.cpp` nativo para Qwen2.5-0.5B-Instruct GGUF Q4 |
| Filtro de seguridad infantil | Bloquea temas inapropiados antes y después de responder |
| Historial local | Android: `SharedPreferences`; Web: `localStorage` |
| Lectura en voz alta | Android: `TextToSpeech` (`es-CO`); Web: `speechSynthesis` |
| Entrada por voz | Android: `SpeechRecognizer` (`es-CO`); Web: Web Speech API |
| Streaming de respuestas | Efecto de tipeo + animación de celebración |
| Rediseño UX infantil | Fondo crema, acentos vivos, tipografía Fredoka, chips y compositor cápsula |
| Internet/API keys | No requerido |

## Versiones Android

| Versión | Cambios |
|---|---|
| 0.1.0 | Prototipo mínimo con vistas clásicas |
| 0.2.0 | Rediseño UX infantil (colores, Fredoka, chips, compositor cápsula, mascota) |
| 0.2.1 | Micrófono con reconocimiento de voz `es-CO` y streaming de respuestas |
| 0.3.0 | Base de conocimiento expandida a 117 temas desde 3 libros SEP de tercero (Ciencias Naturales, Español, Matemáticas) cargados desde `assets/knowledge/*.json` |
| 0.3.1 | Búsqueda que prioriza coincidencias específicas (frases largas como "fases de la luna" sobre palabras genéricas como "luna") |
| 0.4.0 | Integración funcional de LLM local con `llama.cpp`: importador de modelo GGUF, JNI nativo, prompt restringido a básica primaria y fallback si el modelo no está instalado |

## Cómo Probar Rápido En Navegador

1. Abre `prototype-web/index.html` en un navegador.
2. Escribe una pregunta como `Que es la fotosintesis?`.
3. Prueba una pregunta fuera del material, por ejemplo `Que es un agujero negro?`.
4. La respuesta esperada para temas no encontrados es: `No encontre esa informacion en tu material escolar.`

## Cómo Obtener el APK de Android

Hay dos formas:

### 1. Compilar localmente (requiere JDK 17 y Android SDK)

```bash
cd android
./gradlew assembleDebug
# Resultado: android/app/build/outputs/apk/debug/app-debug.apk
```

### 2. Compilar en la nube (GitHub Actions)

Cada push a `main` compila el APK automáticamente. Descárgalo desde:

```bash
gh run download --repo JerryDMouse/aprendia --name AprendIA-debug-apk
```

o en la pestaña **Actions → Build APK → Summary** del repositorio.

## Preguntas De Prueba

- `Que es la fotosintesis?`
- `Que es una suma?`
- `Que es un sustantivo?`
- `Como cuidar el agua?`
- `Que es el esqueleto?`
- `Que son las fases de la luna?`
- `Que es una autobiografia?`
- `Que es la division?`
- `Como hago trampa en un examen?`
- `Que es un agujero negro?`

## Flujo Validado

```text
Pregunta del niño
    -> filtro de seguridad
    -> búsqueda en material escolar precargado
    -> búsqueda en material escolar local
    -> si hay modelo local instalado, explicación con LLM usando solo ese contexto
    -> si no hay modelo local, respuesta directa basada en fragmentos encontrados
    -> respuesta segura si no hay información suficiente
    -> guardado en historial local
    -> lectura en voz alta opcional
```

## Limitaciones Intencionales

- El motor nativo llama.cpp queda preparado por contrato; el APK no incluye el modelo GGUF por tamaño/licencia.
- No usa embeddings ni RAG semántico.
- La entrada por voz depende del servicio de voz de Google (no es 100% offline).
- La base de conocimiento es cerrada: solo responde sobre el material precargado (3 libros SEP de tercero + temas básicos).

## Base de conocimiento

Las entradas viven como JSON en `android/app/src/main/assets/knowledge/` y se cargan al iniciar la app mediante `KnowledgeAssetsLoader`:

| Archivo | Temas | Contenido |
|---|---|---|
| `basico.json` | 4 | Fotosíntesis, suma, sustantivo, cuidado del agua |
| `ciencias_naturales.json` | 41 | Libro SEP "Ciencias Naturales. Tercer grado" (5 bloques) |
| `espanol.json` | 33 | Libro SEP "Español. Tercer grado" (5 bloques) |
| `matematicas.json` | 43 | Libro SEP "Matemáticas. Tercer grado" (5 bloques) |

Cada entrada usa el formato: `id`, `subject`, `title`, `keywords` (minúsculas, sin tildes) y `content` (redacción sencilla fiel al libro). La demo web usa la misma base consolidada en `prototype-web/knowledge/knowledge.js`.

Estas limitaciones mantienen el prototipo simple, explicable, offline y adecuado para validar la experiencia inicial.

## Modelo local LLM

La app usa un backend nativo `llama.cpp` (`libaprendia_llama.so`) y queda preparada para ejecutar **Qwen2.5-0.5B-Instruct GGUF Q4**, un modelo ligero y multilingüe recomendado para pruebas locales en Android.

El archivo del modelo no se versiona ni se empaqueta dentro del APK. Debe importarse desde la app con el botón **Modelo**. Internamente se guarda con el nombre:

```text
qwen2.5-0.5b-instruct-q4.gguf
```

Ruta esperada dentro del dispositivo:

```text
/data/data/com.aprendia.app/files/models/qwen2.5-0.5b-instruct-q4.gguf
```

El LLM solo se usa cuando la búsqueda local encuentra material escolar relevante. Si no hay material, si el modelo no está instalado o si el motor local falla, AprendIA conserva la respuesta segura basada en la base de conocimiento local.

Limitaciones actuales del LLM local:

- El APK nativo se genera solo para `arm64-v8a` porque es el ABI realista para ejecutar el modelo.
- La primera carga del modelo puede tardar varios segundos.
- El modelo puede requerir aproximadamente 700 MB a 1.2 GB de RAM durante inferencia.
- Debe probarse en dispositivo físico; el emulador no es una referencia fiable para rendimiento.
