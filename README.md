# Snake Race — ARSW Lab #2 (Java 21, Virtual Threads)

**Escuela Colombiana de Ingeniería – Arquitecturas de Software**  
Laboratorio de programación concurrente: condiciones de carrera, sincronización y colecciones seguras.

---

## Requisitos

- **JDK 21** (Temurin recomendado)
- **Maven 3.9+**
- SO: Windows, macOS o Linux

---

## Cómo ejecutar

```bash
mvn clean verify
mvn -q -DskipTests exec:java -Dsnakes=4
```

- `-Dsnakes=N` → inicia el juego con **N** serpientes (por defecto 2).
- **Controles**:
  - **Flechas**: serpiente **0** (Jugador 1).
  - **WASD**: serpiente **1** (si existe).
  - **Espacio** o botón **Action**: Pausar / Reanudar.

---

## Reglas del juego (resumen)

- **N serpientes** corren de forma autónoma (cada una en su propio hilo).
- **Ratones**: al comer uno, la serpiente **crece** y aparece un **nuevo obstáculo**.
- **Obstáculos**: si la cabeza entra en un obstáculo hay **rebote**.
- **Teletransportadores** (flechas rojas): entrar por uno te **saca por su par**.
- **Rayos (Turbo)**: al pisarlos, la serpiente obtiene **velocidad aumentada** temporal.
- Movimiento con **wrap-around** (el tablero “se repite” en los bordes).

---

## Arquitectura (carpetas)

```
co.eci.snake
├─ app/                 # Bootstrap de la aplicación (Main)
├─ core/                # Dominio: Board, Snake, Direction, Position
├─ core/engine/         # GameClock (ticks, Pausa/Reanudar)
├─ concurrency/         # SnakeRunner (lógica por serpiente con virtual threads)
└─ ui/legacy/           # UI estilo legado (Swing) con grilla y botón Action
```

---

# Actividades del laboratorio

## Parte I — (Calentamiento) `wait/notify` en un programa multi-hilo

1. Toma el programa [**PrimeFinder**](https://github.com/ARSW-ECI/wait-notify-excercise).
2. Modifícalo para que **cada _t_ milisegundos**:
   - Se **pausen** todos los hilos trabajadores.
   - Se **muestre** cuántos números primos se han encontrado.
   - El programa **espere ENTER** para **reanudar**.
3. La sincronización debe usar **`synchronized`**, **`wait()`**, **`notify()` / `notifyAll()`** sobre el **mismo monitor** (sin _busy-waiting_).
4. Entrega en el reporte de laboratorio **las observaciones y/o comentarios** explicando tu diseño de sincronización (qué lock, qué condición, cómo evitas _lost wakeups_).

> Objetivo didáctico: practicar suspensión/continuación **sin** espera activa y consolidar el modelo de monitores en Java.

---

## Parte II — SnakeRace concurrente (núcleo del laboratorio)

### 1) Análisis de concurrencia

- Explica **cómo** el código usa hilos para dar autonomía a cada serpiente.
- **Identifica** y documenta en **`el reporte de laboratorio`**:
  - Posibles **condiciones de carrera**.
  - **Colecciones** o estructuras **no seguras** en contexto concurrente.
  - Ocurrencias de **espera activa** (busy-wait) o de sincronización innecesaria.

### 2) Correcciones mínimas y regiones críticas

- **Elimina** esperas activas reemplazándolas por **señales** / **estados** o mecanismos de la librería de concurrencia.
- Protege **solo** las **regiones críticas estrictamente necesarias** (evita bloqueos amplios).
- Justifica en **`el reporte de laboratorio`** cada cambio: cuál era el riesgo y cómo lo resuelves.

### 3) Control de ejecución seguro (UI)

- Implementa la **UI** con **Iniciar / Pausar / Reanudar** (ya existe el botón _Action_ y el reloj `GameClock`).
- Al **Pausar**, muestra de forma **consistente** (sin _tearing_):
  - La **serpiente viva más larga**.
  - La **peor serpiente** (la que **primero murió**).
- Considera que la suspensión **no es instantánea**; coordina para que el estado mostrado no quede “a medias”.

## Desarrollo parte III:

Para garantizar un control seguro desde la interfaz gráfica, se implementó la lógica de **Iniciar / Pausar / Reanudar** dentro del botón existente `actionButton`.

El funcionamiento es el siguiente:

1. **Iniciar**
    - El juego comienza al presionar el botón `Start`, que inmediatamente cambia a `Pause`.
    - Se activa el `GameClock` y se inicializan los hilos de cada serpiente (`SnakeRunner`).

2. **Pausar**
    - Al presionar el botón en estado `Pause`, este cambia a `Resume`.
    - Se detiene el `GameClock` y se suspenden controladamente todos los hilos de las serpientes (`pauseRunner()`), evitando que la animación continúe en segundo plano.
    - En este punto se llama al método `showStats()`, que despliega:
        - La serpiente más larga (viva en ese momento).
        - La serpiente más corta (adaptación al no existir la lógica de “muerte”).
    - Para evitar inconsistencias visuales (*tearing*), el estado se captura únicamente cuando tanto el `GameClock` como los *runners* están completamente detenidos.

3. **Reanudar**
    - Al presionar el botón en estado `Resume`, este vuelve a `Pause`.
    - Se reanuda el `GameClock` y los hilos de las serpientes (`resumeRunner()`), continuando el juego desde el mismo estado.

---

### Fragmento de código modificado

- togglePause() :
```java
  private void togglePause() {
    if (!started) {
        actionButton.setText("Pause");
        clock.start();
        runners.forEach(SnakeRunner::resumeRunner);
        started = true;
    }
    else if ("Pause".equals(actionButton.getText())) {
        actionButton.setText("Resume");
        runners.forEach(SnakeRunner::pauseRunner);
        pauseStats();
        clock.pause();

    } else {
        actionButton.setText("Pause");
        runners.forEach(SnakeRunner::resumeRunner);
        clock.resume();
    }
}
```
- Metodo run() de SnakeRunner:
```java
@Override
  public void run() {
    try {
      while (!Thread.currentThread().isInterrupted()) {
        synchronized (this) {
          while (paused) {
            wait();
          }
        }

        maybeTurn();
        var result = board.step(snake);
        if (result == Board.MoveResult.HIT_OBSTACLE) {
          randomTurn();
        } else if (result == Board.MoveResult.ATE_TURBO) {
          turboTicks = 100;
        }
        int sleep = (turboTicks > 0) ? turboSleepMs : baseSleepMs;
        if (turboTicks > 0) turboTicks--;
        Thread.sleep(sleep);
      }
      } catch (InterruptedException e) {
        Thread.currentThread().interrupt();
      }
    }
```
- pauseStats() :
```java
  private void pauseStats() {
    Snake longest = null;
    Snake shortest = null;

    for (SnakeRunner runner : runners) {
      Snake snake = runner.getSnake();
      if (longest == null || snake.getLength() > longest.getLength()) {
        longest = snake;
      }
      if (shortest == null || snake.getLength() < shortest.getLength()) {
        shortest = snake;
      }
    }

    String message = String.format("Longest snake: %s (length: %d)\nShortest snake: %s (length: %d)",
            longest.getName(), longest.getLength(),
            shortest.getName(), shortest.getLength());
    JOptionPane.showMessageDialog(frame, message, "Game Stats", JOptionPane.INFORMATION_MESSAGE);
  }
  ```
### 4) Robustez bajo carga

- Ejecuta con **N alto** (`-Dsnakes=20` o más) y/o aumenta la velocidad.
- El juego **no debe romperse**: sin `ConcurrentModificationException`, sin lecturas inconsistentes, sin _deadlocks_.
- Si habilitas **teleports** y **turbo**, verifica que las reglas no introduzcan carreras.

> Entregables detallados más abajo.

---

## Entregables

1. **Código fuente** funcionando en **Java 21**.
2. Todo de manera clara en **`**el reporte de laboratorio**`** con:
   - Data races encontradas y su solución.
   - Colecciones mal usadas y cómo se protegieron (o sustituyeron).
   - Esperas activas eliminadas y mecanismo utilizado.
   - Regiones críticas definidas y justificación de su **alcance mínimo**.
3. UI con **Iniciar / Pausar / Reanudar** y estadísticas solicitadas al pausar.

---

## Criterios de evaluación (10)

- (3) **Concurrencia correcta**: sin data races; sincronización bien localizada.
- (2) **Pausa/Reanudar**: consistencia visual y de estado.
- (2) **Robustez**: corre **con N alto** y sin excepciones de concurrencia.
- (1.5) **Calidad**: estructura clara, nombres, comentarios; sin _code smells_ obvios.
- (1.5) **Documentación**: **`reporte de laboratorio`** claro, reproducible;

---

## Tips y configuración útil

- **Número de serpientes**: `-Dsnakes=N` al ejecutar.
- **Tamaño del tablero**: cambiar el constructor `new Board(width, height)`.
- **Teleports / Turbo**: editar `Board.java` (métodos de inicialización y reglas en `step(...)`).
- **Velocidad**: ajustar `GameClock` (tick) o el `sleep` del `SnakeRunner` (incluye modo turbo).

---

## Cómo correr pruebas

```bash
mvn clean verify
```

Incluye compilación y ejecución de pruebas JUnit. Si tienes análisis estático, ejecútalo en `verify` o `site` según tu `pom.xml`.

---

## Créditos

Este laboratorio es una adaptación modernizada del ejercicio **SnakeRace** de ARSW. El enunciado de actividades se conserva para mantener los objetivos pedagógicos del curso.

**Base construida por el Ing. Javier Toquica.**
