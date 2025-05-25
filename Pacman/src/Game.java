import java.awt.Color;
import java.awt.Dimension;
import java.awt.Graphics;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;
import javax.swing.JButton;
import javax.swing.JPanel;
import javax.swing.Timer;

public class Game extends JPanel implements ActionListener {
    private final int TILE_SIZE = 24;
    private final int NUM_TILES_X = 21;
    private final int NUM_TILES_Y = 21;
    private Timer timer;

    private int pacmanX = 10;
    private int pacmanY = 10;
    private int pacmanDirX = 0;
    private int pacmanDirY = 0;

    // Ghost position and direction
    private static class Ghost {
        int x, y, startX, startY;
        boolean alive = true;
        int respawnTimer = 0;
        public Ghost(int x, int y) { this.x = x; this.y = y; this.startX = x; this.startY = y; }
        public void reset() { x = startX; y = startY; alive = true; respawnTimer = 0; }
    }
    private Ghost[] ghosts = {
        new Ghost(1, 1),
        new Ghost(19, 1),
        new Ghost(1, 10),
        new Ghost(19, 10)
    };
    private boolean lose = false;

    // Maze legend: 0 = wall, 1 = dot, 2 = empty, 3 = power pellet, 4 = ghost house
    private int[][] maze = {
        {0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0},
        {0,3,1,1,1,1,1,1,1,1,1,1,1,1,1,1,1,1,1,3,0},
        {0,1,0,0,1,0,0,0,0,0,0,0,0,0,1,0,0,0,0,1,0},
        {0,1,0,0,1,0,0,0,0,0,0,0,0,0,1,0,0,0,0,1,0},
        {0,1,1,1,1,1,1,1,1,1,1,1,1,1,1,1,1,1,1,1,0},
        {0,1,0,0,1,0,1,0,0,2,2,2,0,0,1,0,0,1,0,1,0},
        {0,1,1,1,1,0,1,1,1,2,2,2,1,1,1,0,1,1,1,1,0},
        {0,0,0,0,1,0,0,0,0,2,2,2,0,0,1,0,0,0,0,0,0},
        {0,1,1,1,1,1,1,1,1,1,1,1,1,1,1,1,1,1,1,1,0},
        {0,1,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,1,0},
        {0,3,1,1,1,1,1,1,1,1,1,1,1,1,1,1,1,1,1,3,0},
        {0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0}
    };
    private int score = 0;
    private int totalDots = 0;
    private boolean win = false;

    // Power pellet logic
    private boolean powerMode = false;
    private int powerTimer = 0;
    private final int POWER_DURATION = 40; // Faster power effect (was 100)

    {
        // Count total dots
        for (int y = 0; y < maze.length; y++) {
            for (int x = 0; x < maze[0].length; x++) {
                if (maze[y][x] == 1) totalDots++;
            }
        }
    }

    private JButton restartButton;

    private int lives = 3;
    private int highScore = 0;

    // Adjust spawn positions
    private int pacmanStartX = 10;
    private int pacmanStartY = 7; // Center of the open house
    private int[] ghostStartX = {1, 19, 1, 19}; // Maze corners
    private int[] ghostStartY = {1, 1, 10, 10};   // Spread ghosts vertically in the house

    public Game() {
        setPreferredSize(new Dimension(TILE_SIZE * NUM_TILES_X, TILE_SIZE * NUM_TILES_Y));
        setBackground(Color.BLACK);
        timer = new Timer(180, this); // Even slower speed for everything
        timer.start();
        setFocusable(true);
        addKeyListener(new KeyAdapter() {
            @Override
            public void keyPressed(KeyEvent e) {
                switch (e.getKeyCode()) {
                    case KeyEvent.VK_UP:
                        pacmanDirX = 0; pacmanDirY = -1;
                        break;
                    case KeyEvent.VK_DOWN:
                        pacmanDirX = 0; pacmanDirY = 1;
                        break;
                    case KeyEvent.VK_LEFT:
                        pacmanDirX = -1; pacmanDirY = 0;
                        break;
                    case KeyEvent.VK_RIGHT:
                        pacmanDirX = 1; pacmanDirY = 0;
                        break;
                }
            }
        });
        restartButton = new JButton("Restart");
        restartButton.setFocusable(false);
        restartButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                resetGame();
            }
        });
        this.add(restartButton);

        pacmanX = pacmanStartX;
        pacmanY = pacmanStartY;
        for (int i = 0; i < ghosts.length; i++) {
            ghosts[i].x = ghostStartX[i];
            ghosts[i].y = ghostStartY[i];
            ghosts[i].startX = ghostStartX[i];
            ghosts[i].startY = ghostStartY[i];
        }
    }

    private void resetGame() {
        // Reset maze
        int[][] newMaze = {
            {0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0},
            {0,3,1,1,1,1,1,1,1,1,1,1,1,1,1,1,1,1,1,3,0},
            {0,1,0,0,1,0,0,0,0,0,0,0,0,0,1,0,0,0,0,1,0},
            {0,1,0,0,1,0,0,0,0,0,0,0,0,0,1,0,0,0,0,1,0},
            {0,1,1,1,1,1,1,1,1,1,1,1,1,1,1,1,1,1,1,1,0},
            {0,1,0,0,1,0,1,0,0,2,2,2,0,0,1,0,0,1,0,1,0},
            {0,1,1,1,1,0,1,1,1,2,2,2,1,1,1,0,1,1,1,1,0},
            {0,0,0,0,1,0,0,0,0,2,2,2,0,0,1,0,0,0,0,0,0},
            {0,1,1,1,1,1,1,1,1,1,1,1,1,1,1,1,1,1,1,1,0},
            {0,1,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,1,0},
            {0,3,1,1,1,1,1,1,1,1,1,1,1,1,1,1,1,1,1,3,0},
            {0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0}
        };
        for (int y = 0; y < maze.length; y++) {
            for (int x = 0; x < maze[0].length; x++) {
                maze[y][x] = newMaze[y][x];
            }
        }
        // Reset positions and state
        pacmanX = pacmanStartX;
        pacmanY = pacmanStartY;
        pacmanDirX = 0;
        pacmanDirY = 0;
        for (int i = 0; i < ghosts.length; i++) {
            ghosts[i].reset();
            ghosts[i].x = ghostStartX[i];
            ghosts[i].y = ghostStartY[i];
        }
        lives = 3;
        score = 0;
        win = false;
        lose = false;
        // Recount dots
        totalDots = 0;
        for (int y = 0; y < maze.length; y++) {
            for (int x = 0; x < maze[0].length; x++) {
                if (maze[y][x] == 1) totalDots++;
            }
        }
        repaint();
    }

    @Override
    protected void paintComponent(Graphics g) {
        super.paintComponent(g);
        // Improved UI: background
        g.setColor(new Color(20, 20, 40));
        g.fillRect(0, 0, getWidth(), getHeight());
        // Draw maze
        for (int y = 0; y < maze.length; y++) {
            for (int x = 0; x < maze[0].length; x++) {
                if (maze[y][x] == 0) {
                    g.setColor(Color.BLUE);
                    g.fillRoundRect(x * TILE_SIZE, y * TILE_SIZE, TILE_SIZE, TILE_SIZE, 8, 8);
                } else if (maze[y][x] == 1) {
                    g.setColor(Color.WHITE);
                    g.fillOval(x * TILE_SIZE + TILE_SIZE/3, y * TILE_SIZE + TILE_SIZE/3, TILE_SIZE/3, TILE_SIZE/3);
                } else if (maze[y][x] == 3) {
                    g.setColor(Color.PINK);
                    g.fillOval(x * TILE_SIZE + TILE_SIZE/6, y * TILE_SIZE + TILE_SIZE/6, 2*TILE_SIZE/3, 2*TILE_SIZE/3);
                }
            }
        }
        // Draw ghost house
        g.setColor(Color.LIGHT_GRAY);
        for (int y = 0; y < maze.length; y++) {
            for (int x = 0; x < maze[0].length; x++) {
                if (maze[y][x] == 4) {
                    g.fillRect(x * TILE_SIZE, y * TILE_SIZE, TILE_SIZE, TILE_SIZE);
                }
            }
        }
        // Animate Pacman mouth (open/close)
        int startAngle = 30;
        int arcAngle = 300;
        int mouthAnim = (int)(40 * Math.abs(Math.sin(Math.PI * mouthFrame / mouthFrameMax)));
        if (pacmanDirX == 1) startAngle = 30;
        else if (pacmanDirX == -1) startAngle = 210;
        else if (pacmanDirY == -1) startAngle = 120;
        else if (pacmanDirY == 1) startAngle = 300;
        g.setColor(Color.YELLOW);
        g.fillArc(pacmanX * TILE_SIZE, pacmanY * TILE_SIZE, TILE_SIZE, TILE_SIZE, startAngle + mouthAnim/2, arcAngle - mouthAnim);
        // Draw ghosts
        for (Ghost ghost : ghosts) {
            if (!ghost.alive) continue;
            g.setColor(powerMode ? Color.CYAN : Color.RED);
            g.fillOval(ghost.x * TILE_SIZE, ghost.y * TILE_SIZE, TILE_SIZE, TILE_SIZE);
        }
        // Draw lives
        g.setColor(Color.YELLOW);
        for (int i = 0; i < lives; i++) {
            g.fillArc(10 + i * 20, 30, 16, 16, 30, 300);
        }
        // Draw high score
        g.setColor(Color.WHITE);
        g.drawString("High Score: " + highScore, 10, 60);
        // Draw score and power mode
        g.setColor(Color.WHITE);
        g.drawString("Score: " + score, 10, 20);
        if (powerMode) {
            g.setColor(Color.CYAN);
            g.drawString("POWER!", 120, 20);
        }
        // Win/lose messages
        if (win) {
            g.setColor(Color.GREEN);
            g.drawString("You Win!", TILE_SIZE * 8, TILE_SIZE * 6);
        }
        if (lose) {
            g.setColor(Color.RED);
            g.drawString("Game Over! Press Restart", TILE_SIZE * 6, TILE_SIZE * 7);
        }
    }

    private int ghostMoveCounter = 0;
    private final int GHOST_MOVE_DELAY = 2; // Ghosts move every 2 frames (faster)

    @Override
    public void actionPerformed(ActionEvent e) {
        if (win || lose) return;
        // Pacman movement (allow movement in house)
        int nextX = pacmanX + pacmanDirX;
        int nextY = pacmanY + pacmanDirY;
        if (nextX >= 0 && nextX < maze[0].length && nextY >= 0 && nextY < maze.length && maze[nextY][nextX] != 0) {
            pacmanX = nextX;
            pacmanY = nextY;
        }
        // Eat dot or power pellet
        if (maze[pacmanY][pacmanX] == 1) {
            maze[pacmanY][pacmanX] = 2;
            score++;
        } else if (maze[pacmanY][pacmanX] == 3) {
            maze[pacmanY][pacmanX] = 2;
            powerMode = true;
            powerTimer = POWER_DURATION;
        }
        // Win condition: check if any dots or power pellets remain
        boolean anyDotsLeft = false;
        for (int y = 0; y < maze.length && !anyDotsLeft; y++) {
            for (int x = 0; x < maze[0].length && !anyDotsLeft; x++) {
                if (maze[y][x] == 1 || maze[y][x] == 3) anyDotsLeft = true;
            }
        }
        if (!anyDotsLeft) win = true;
        // Power mode timer
        if (powerMode) {
            powerTimer--;
            if (powerTimer <= 0) powerMode = false;
        }
        // Ghosts move slower
        ghostMoveCounter++;
        if (ghostMoveCounter >= GHOST_MOVE_DELAY) {
            for (Ghost ghost : ghosts) {
                if (!ghost.alive) continue;
                int[] dx = {1, -1, 0, 0};
                int[] dy = {0, 0, 1, -1};
                int bestDir = -1;
                double bestScore = powerMode ? -1 : Double.MAX_VALUE;
                for (int i = 0; i < 4; i++) {
                    int gx = ghost.x + dx[i];
                    int gy = ghost.y + dy[i];
                    if (gx >= 0 && gx < maze[0].length && gy >= 0 && gy < maze.length && maze[gy][gx] != 0 && maze[gy][gx] != 4) {
                        double dist = Math.hypot(pacmanX - gx, pacmanY - gy);
                        // Avoid reversing direction
                        boolean notReverse = true;
                        if (ghost.x - dx[i] == ghost.x && ghost.y - dy[i] == ghost.y) notReverse = false;
                        if (!powerMode && dist < bestScore && notReverse) {
                            bestScore = dist;
                            bestDir = i;
                        } else if (powerMode && dist > bestScore && notReverse) {
                            bestScore = dist;
                            bestDir = i;
                        }
                    }
                }
                // If no bestDir found (cornered), pick any valid
                if (bestDir == -1) {
                    for (int i = 0; i < 4; i++) {
                        int gx = ghost.x + dx[i];
                        int gy = ghost.y + dy[i];
                        if (gx >= 0 && gx < maze[0].length && gy >= 0 && gy < maze.length && maze[gy][gx] != 0 && maze[gy][gx] != 4) {
                            bestDir = i;
                            break;
                        }
                    }
                }
                if (bestDir != -1) {
                    ghost.x += dx[bestDir];
                    ghost.y += dy[bestDir];
                }
            }
            ghostMoveCounter = 0;
        }
        // Check collisions
        for (Ghost ghost : ghosts) {
            if (!ghost.alive) continue;
            if (pacmanX == ghost.x && pacmanY == ghost.y) {
                if (powerMode) {
                    ghost.alive = false;
                    score += 10;
                } else {
                    lives--;
                    if (lives <= 0) {
                        lose = true;
                        if (score > highScore) highScore = score;
                    } else {
                        // Respawn Pacman in the house (center)
                        pacmanX = pacmanStartX;
                        pacmanY = pacmanStartY;
                        pacmanDirX = 0;
                        pacmanDirY = 0;
                        for (Ghost g : ghosts) g.reset();
                        // Optionally: add a short pause or invulnerability here
                    }
                    break;
                }
            }
        }
        // When ghost is eaten, return to house (but keep it invisible for a short time)
        for (Ghost ghost : ghosts) {
            if (!ghost.alive) {
                if (ghost.respawnTimer == 0) {
                    ghost.respawnTimer = 30; // frames to stay invisible
                } else {
                    ghost.respawnTimer--;
                    if (ghost.respawnTimer == 0) {
                        ghost.x = ghost.startX;
                        ghost.y = ghost.startY;
                        ghost.alive = true;
                    }
                }
            }
        }
        // Animate Pacman mouth
        mouthFrame = (mouthFrame + 1) % mouthFrameMax;
        repaint();
    }

    // Animation state for Pacman mouth
    private int mouthFrame = 0;
    private final int mouthFrameMax = 12; // Slower mouth animation
}
