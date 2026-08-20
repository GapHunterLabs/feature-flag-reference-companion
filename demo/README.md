# Cómo probar este plugin

Este plugin cuenta cuántas veces se usa cada "interruptor de
funciones" (una técnica común para activar/desactivar partes de una
app sin tener que volver a publicarla) en todo el proyecto — para
encontrar interruptores que ya nadie usa y se pueden borrar.

## Qué hacer

1. En el panel de la izquierda, abrí el archivo
   **`OrderProcessor.java`** (dentro de `src` → `main` → `java` →
   `com` → `acmecorp` → `orders`).
2. Buscá en el panel superior (menú "Tools" o similar) una opción del
   plugin para "actualizar" o "refresh" — hacé click ahí primero. Este
   plugin no se actualiza solo, hay que pedirle que revise el
   proyecto.
3. Después de eso, mirá al lado izquierdo del número de línea en las
   2 líneas que dicen `flags.isEnabled(...)`.

## Qué deberías ver

- La línea que dice `"new-checkout-flow"`: debería tener un ícono que
  indica "esto se sigue usando" — porque ese mismo nombre aparece
  2 veces en el proyecto.
- La línea que dice `"legacy-discount-banner-2024"`: debería tener un
  ícono distinto que indica "esto parece abandonado" — porque es la
  única vez que aparece ese nombre en todo el proyecto.

## Si algo no se ve así

Sacá la captura igual, y avisame qué línea no coincide con lo de
arriba.
