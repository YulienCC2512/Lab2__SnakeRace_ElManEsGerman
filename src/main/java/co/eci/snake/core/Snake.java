package co.eci.snake.core;

import java.util.ArrayDeque;
import java.util.Deque;
import java.util.concurrent.locks.ReentrantLock;

/**
 * Snake thread-safe: usa ReentrantLock para proteger su estado interno.
 */
public final class Snake {
  private final Deque<Position> body = new ArrayDeque<>();
  private Direction direction;
  private int maxLength = 5;

  // Lock para proteger body + dirección + maxLength
  private final ReentrantLock lock = new ReentrantLock();

  private Snake(Position start, Direction dir) {
    body.addFirst(start);
    this.direction = dir;
  }

  public static Snake of(int x, int y, Direction dir) {
    return new Snake(new Position(x, y), dir);
  }

  // Devuelve la cabeza (Position es inmutable - record)
  public Position head() {
    lock.lock();
    try {
      return body.peekFirst();
    } finally {
      lock.unlock();
    }
  }

  // Devuelve la dirección actual
  public Direction direction() {
    lock.lock();
    try {
      return direction;
    } finally {
      lock.unlock();
    }
  }

  // Snapshot seguro: copia la deque dentro del lock
  public Deque<Position> snapshot() {
    lock.lock();
    try {
      return new ArrayDeque<>(body);
    } finally {
      lock.unlock();
    }
  }

  // Avanzar (mutación protegida)
  public void advance(Position newHead, boolean grow) {
    lock.lock();
    try {
      body.addFirst(newHead);
      if (grow) maxLength++;
      while (body.size() > maxLength) body.removeLast();
    } finally {
      lock.unlock();
    }
  }

  // Cambiar dirección (protegida)
  public void turn(Direction newDir) {
    if (newDir == null) return;
    lock.lock();
    try {
      this.direction = newDir;
    } finally {
      lock.unlock();
    }
  }
}
