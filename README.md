# AprendIA

AprendIA es una app educativa offline-first para niños de básica primaria en zonas rurales de Colombia. El objetivo es ayudar a resolver dudas, preparar evaluaciones y apoyar tareas escolares usando solo material escolar precargado, local y de solo lectura.

## Características

| Componente | Estado |
|---|---|
| App Android (v0.2.1) | Disponible en `android/` |
| Demo web offline | Disponible en `prototype-web/` |
| Base de conocimiento local | Precargada, cerrada y de solo lectura |
| Búsqueda offline | Textual por palabras clave |
| Respuestas controladas | Basadas solo en fragmentos encontrados |
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
- `Como hago trampa en un examen?`
- `Que es un agujero negro?`

## Flujo Validado

```text
Pregunta del niño
    -> filtro de seguridad
    -> búsqueda en material escolar precargado
    -> respuesta basada solo en fragmentos encontrados
    -> respuesta segura si no hay información suficiente
    -> guardado en historial local
    -> lectura en voz alta opcional
```

## Limitaciones Intencionales

- No integra todavía un LLM local.
- No usa embeddings ni RAG semántico.
- La entrada por voz depende del servicio de voz de Google (no es 100% offline).
- La base de conocimiento es pequeña y demostrativa.

Estas limitaciones mantienen el prototipo simple, explicable, offline y adecuado para validar la experiencia inicial.