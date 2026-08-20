# AprendIA

AprendIA es una app educativa offline-first para ninos de basica primaria en zonas rurales de Colombia. El objetivo es ayudar a resolver dudas, preparar evaluaciones y apoyar tareas escolares usando solo material escolar precargado, local y de solo lectura.

## Version Inicial Implementada

Esta version minima permite interactuar sin modelos pesados ni internet:

| Componente | Estado |
|---|---|
| App Android minima | Disponible en `android/` |
| Demo web offline | Disponible en `prototype-web/` |
| Base de conocimiento local | Precargada en codigo |
| Busqueda offline | Textual por palabras clave |
| Respuestas controladas | Basadas solo en fragmentos encontrados |
| Historial local | Android: `SharedPreferences`; Web: `localStorage` |
| Lectura en voz alta | Android: `TextToSpeech`; Web: `speechSynthesis` |
| Internet/API keys | No requerido |

## Como Probar Rapido En Navegador

1. Abre `prototype-web/index.html` en un navegador.
2. Escribe una pregunta como `Que es la fotosintesis?`.
3. Prueba una pregunta fuera del material, por ejemplo `Que es un agujero negro?`.
4. La respuesta esperada para temas no encontrados es: `No encontre esa informacion en tu material escolar.`

## Como Probar En Android

1. Abre la carpeta `android/` con Android Studio.
2. Sincroniza Gradle cuando Android Studio lo solicite.
3. Ejecuta el modulo `app` en un emulador o dispositivo Android.
4. Activa modo avion y prueba el chat para validar funcionamiento offline.

La app Android inicial no usa Compose para reducir dependencias y permitir un prototipo minimo con Android SDK estandar. La arquitectura separa dominio, conocimiento, seguridad e interfaz para migrar luego a una arquitectura modular mas robusta.

## Preguntas De Prueba

- `Que es la fotosintesis?`
- `Que es una suma?`
- `Que es un sustantivo?`
- `Como cuidar el agua?`
- `Como hago trampa en un examen?`
- `Que es un agujero negro?`

## Flujo Validado

```text
Pregunta del nino
    -> filtro de seguridad
    -> busqueda en material escolar precargado
    -> respuesta basada solo en fragmentos encontrados
    -> respuesta segura si no hay informacion suficiente
    -> guardado en historial local
    -> lectura en voz alta opcional
```

## Limitaciones Intencionales

- No integra todavia un LLM local.
- No usa embeddings ni RAG semantico.
- No incluye voz a texto offline.
- No incluye generacion de imagenes.
- La base de conocimiento es pequena y demostrativa.

Estas limitaciones mantienen el primer prototipo simple, explicable, offline y adecuado para validar la experiencia inicial.
