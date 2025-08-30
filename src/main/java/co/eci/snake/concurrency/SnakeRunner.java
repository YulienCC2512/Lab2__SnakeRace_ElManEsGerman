package co.eci.snake.concurrency;



import co.eci.snake.core.Board;
import co.eci.snake.core.Direction;
import co.eci.snake.core.Position;
import co.eci.snake.core.Snake;


public class SnakeRunner implements Runnable {

    private final Snake snake;
    private final Board board;

    // flags de control
    private volatile boolean paused = false;
    private volatile boolean running = true;

    public SnakeRunner(Snake snake, Board board) {
        this.snake = snake;
        this.board = board;
    }

    @Override
    public void run() {
        // bucle principal del runner: avanza la snake periódicamente
        while (running) {
            if (!paused) {
          
                Position head = snake.head();
                Direction dir = snake.direction();
                int nx = head.x();
                int ny = head.y();

                if (dir == Direction.UP)    ny--;
                else if (dir == Direction.DOWN)  ny++;
                else if (dir == Direction.LEFT)  nx--;
                else if (dir == Direction.RIGHT) nx++;

                Position newHead = new Position(nx, ny);
                boolean grow = board.mice().contains(newHead);
                snake.advance(newHead, grow);
                if (grow) board.mice().remove(newHead);

            }
            try {
                Thread.sleep(150); // velocidad configurable
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                break;
            }
        }
    }

    // control por instancia (SnakeApp usa referencia a cada runner)
    public void pauseRunner() {
        paused = true;
    }

    public void resumeRunner() {
        paused = false;
    }

    public void stopRunner() {
        running = false;
    }
}
