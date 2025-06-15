import java.awt.*;
import java.awt.event.*;
import java.util.HashSet;
import java.util.Random;
import javax.swing.*;

public class PacMan extends JPanel implements ActionListener, KeyListener {
    class Block {
        int x;
        int y;
        int width;
        int height;
        Image image;

        int startX;
        int startY;
        char direction = 'U'; // U D L R
        int velocityX = 0;
        int velocityY = 0;

        Block(Image image, int x, int y, int width, int height) {
            this.image = image;
            this.x = x;
            this.y = y;
            this.width = width;
            this.height = height;
            this.startX = x;
            this.startY = y;
        }

        void updateDirection(char direction) {
            char prevDirection = this.direction;
            this.direction = direction;
            updateVelocity();
            this.x += this.velocityX;
            this.y += this.velocityY;
            for (Block wall : walls) {
                if (collision(this, wall)) {
                    this.x -= this.velocityX;
                    this.y -= this.velocityY;
                    this.direction = prevDirection;
                    updateVelocity();
                }
            }
        }

        void updateVelocity() {
            if (this.direction == 'U') {
                this.velocityX = 0;
                this.velocityY = -tileSize/4;
            }
            else if (this.direction == 'D') {
                this.velocityX = 0;
                this.velocityY = tileSize/4;
            }
            else if (this.direction == 'L') {
                this.velocityX = -tileSize/4;
                this.velocityY = 0;
            }
            else if (this.direction == 'R') {
                this.velocityX = tileSize/4;
                this.velocityY = 0;
            }
        }

        void reset() {
            this.x = this.startX;
            this.y = this.startY;
        }
    }

    private int rowCount = 21;
    private int columnCount = 19;
    private int tileSize = 32;
    private int boardWidth = columnCount * tileSize;
    private int boardHeight = rowCount * tileSize;

    private Image wallImage;
    private Image yellowBeeImage;
    private Image orangeBeeImage;
    private Image yellowOrangeBeeImage; // New image for yellow-orange bee
    private Image redBeeImage;

    private Image pacmanUpImage;
    private Image pacmanDownImage;
    private Image pacmanLeftImage;
    private Image pacmanRightImage;

    private Image cherryImage;
    private Image cherry2Image;
    private Image powerFoodImage;
    private Image scaredBeeImage;

    //X = wall, O = skip, P = pac man, ' ' = food
    //Bees: b = blue, o = orange, p = pink, r = red
    private String[] tileMap = {
        "XXXXXXXXXXXXXXXXXXX",
        "XO       X       OX",
        "X XX XXX X XXX XX X",
        "X                 X",
        "X XX X XXXXX X XX X",
        "X    X   O   X    X",
        "XXXX XXXX XXXX XXXX",
        "XXXX X       X XXXX",
        "XXXX X X r X X XXXX",
        " O     XbpoX     O ",
        "XXXX X XXXXX X XXXX",
        "XXXX     O     XXXX",
        "XXXX X XXXXX X XXXX",
        "X        X        X",
        "X XX XXX X XXX XX X",
        "X  X     P     X  X",
        "XX X X XXXXX X X XX",
        "X    X   X   X    X",
        "X XXXXXX X XXXXXX X",
        "XO               OX",
        "XXXXXXXXXXXXXXXXXXX"
    };

    HashSet<Block> walls;
    HashSet<Block> foods;
    HashSet<Block> bees;
    HashSet<Block> powerups;
    Block pacman;
    Block yellowBee; // Track yellow bee for initial direction
    Block orangeBee; // Track orange bee for initial direction
    java.util.List<Block> beeList; // For ordered bee release
    int[] beeReleaseTimers; // Timers for each bee
    boolean[] beePenExitPhase; // Track if bee is in special pen-exit phase
    int beeReleaseInterval = 100; // 6 seconds at 20fps

    Timer gameLoop;
    char[] directions = {'U', 'D', 'L', 'R'}; //up down left right
    Random random = new Random();
    int score = 0;
    int lives = 3;
    boolean gameOver = false;
    boolean powerMode = false;
    int powerModeTicks = 0;
    int[] beeslowCounters; // For slowing bees in power mode

    // Add buffered direction for smooth turning
    private char bufferedDirection = 'U'; // Start moving up by default

    private JButton restartButton;
    private JButton playAgainButton; // Button for win state

    PacMan() {
        setPreferredSize(new Dimension(boardWidth, boardHeight));
        setBackground(Color.BLACK);
        addKeyListener(this);
        setFocusable(true);

        //load images
        wallImage = new ImageIcon(getClass().getResource("/assets/wall.PNG")).getImage();
        yellowBeeImage = new ImageIcon(getClass().getResource("/assets/bee-1.png")).getImage();
        yellowOrangeBeeImage = new ImageIcon(getClass().getResource("/assets/bee-2.png")).getImage();
        orangeBeeImage = new ImageIcon(getClass().getResource("/assets/bee-3.PNG")).getImage();
        redBeeImage = new ImageIcon(getClass().getResource("/assets/bee-4.PNG")).getImage();

        pacmanUpImage = new ImageIcon(getClass().getResource("/assets/pacman-up.png")).getImage();
        pacmanDownImage = new ImageIcon(getClass().getResource("/assets/pacman-down.png")).getImage();
        pacmanLeftImage = new ImageIcon(getClass().getResource("/assets/pacman-left.png")).getImage();
        pacmanRightImage = new ImageIcon(getClass().getResource("/assets/pacman-right.png")).getImage();

        cherryImage = new ImageIcon(getClass().getResource("/assets/food.PNG")).getImage();
        cherry2Image = new ImageIcon(getClass().getResource("/assets/food.PNG")).getImage(); // Use same food image for both
        powerFoodImage = new ImageIcon(getClass().getResource("/assets/powerup.png")).getImage();
        scaredBeeImage = new ImageIcon(getClass().getResource("/assets/bee-5.png")).getImage();

        restartButton = new JButton("Restart");
        restartButton.setFocusable(false);
        restartButton.setVisible(false);
        restartButton.addActionListener(e -> restartGame());
        this.setLayout(null);
        restartButton.setBounds(boardWidth/2 - 60, boardHeight/2, 120, 40);
        this.add(restartButton);

        playAgainButton = new JButton("Play Again");
        playAgainButton.setFocusable(false);
        playAgainButton.setVisible(false);
        playAgainButton.addActionListener(e -> restartGame());
        playAgainButton.setBounds(boardWidth/2 - 70, boardHeight/2 + 40, 140, 40);
        this.add(playAgainButton);

        loadMap();
        pacman.direction = 'U';
        pacman.updateVelocity();
        setPacmanImageByDirection('U');
        bufferedDirection = 'U';
        for (Block bee : bees) {
            char newDirection = directions[random.nextInt(4)];
            bee.updateDirection(newDirection);
        }
        //how long it takes to start timer, milliseconds gone between frames
        gameLoop = new Timer(50, this); //20fps (1000/50)
        gameLoop.start();

        // Set Pacman's initial direction and velocity after map load
        pacman.direction = 'U';
        pacman.updateVelocity();
        bufferedDirection = 'U';
        // Initialize bee slow counters
        beeslowCounters = new int[beeList != null ? beeList.size() : 4];
    }

    private void restartGame() {
        restartButton.setVisible(false);
        playAgainButton.setVisible(false);
        loadMap();
        // Now reset bees and all counters on full restart
        for (int i = 0; i < beeList.size(); i++) {
            Block bee = beeList.get(i);
            bee.reset();
            if (bee == yellowBee) {
                bee.direction = 'R';
            } else if (bee == orangeBee) {
                bee.direction = 'L';
            } else {
                bee.direction = 'U';
            }
            bee.updateVelocity();
            beeReleaseTimers[i] = beeReleaseInterval * i; // Reset release timers
            beePenExitPhase[i] = true;
            beeslowCounters[i] = 0;
        }
        lives = 3;
        score = 0;
        gameOver = false;
        powerMode = false;
        powerModeTicks = 0;
        playAgainButton.setVisible(false);
        gameLoop.start();
        requestFocusInWindow();
    }

    public void loadMap() {
        walls = new HashSet<Block>();
        foods = new HashSet<Block>();
        bees = new HashSet<Block>();
        powerups = new HashSet<Block>();
        beeList = new java.util.ArrayList<>();
        Block redBee = null;
        yellowBee = null;
        orangeBee = null;
        java.util.List<Block> otherBees = new java.util.ArrayList<>();
        for (int r = 0; r < rowCount; r++) {
            for (int c = 0; c < columnCount; c++) {
                String row = tileMap[r];
                char tileMapChar = row.charAt(c);
                int x = c*tileSize;
                int y = r*tileSize;
                if (tileMapChar == 'X') { //block wall
                    Block wall = new Block(wallImage, x, y, tileSize, tileSize);
                    walls.add(wall);
                }
                else if (tileMapChar == 'b') { //blue bee
                    Block bee = new Block(yellowBeeImage, x, y, tileSize, tileSize);
                    bees.add(bee);
                    yellowBee = bee;
                }
                else if (tileMapChar == 'o') { //orange bee
                    Block bee = new Block(orangeBeeImage, x, y, tileSize, tileSize);
                    bees.add(bee);
                    orangeBee = bee;
                }
                else if (tileMapChar == 'p') { //pink bee (now yellow-orange bee)
                    Block bee = new Block(yellowOrangeBeeImage, x, y, tileSize, tileSize);
                    bees.add(bee);
                    otherBees.add(bee);
                }
                else if (tileMapChar == 'r') { //red bee
                    Block bee = new Block(redBeeImage, x, y, tileSize, tileSize);
                    bees.add(bee);
                    redBee = bee;
                }
                else if (tileMapChar == 'P') { //pacman
                    pacman = new Block(pacmanRightImage, x, y, tileSize, tileSize);
                }
                else if (tileMapChar == ' ') { //food
                    Block food = new Block(null, x + 14, y + 14, 4, 4);
                    foods.add(food);
                }
                else if (tileMapChar == 'O') { //powerup
                    Block powerup = new Block(null, x + 8, y + 8, 16, 16);
                    powerups.add(powerup);
                }
            }
        }
        // Always put red, blue, orange, then others in beeList
        beeList.clear();
        if (redBee != null) beeList.add(redBee);
        if (yellowBee != null) beeList.add(yellowBee);
        if (orangeBee != null) beeList.add(orangeBee);
        beeList.addAll(otherBees);
        // Set up bee release timers and pen-exit phase
        beeReleaseTimers = new int[beeList.size()];
        beePenExitPhase = new boolean[beeList.size()];
        // Initialize bee slow counters
        beeslowCounters = new int[beeList.size()];
        for (int i = 0; i < beeReleaseTimers.length; i++) {
            beeReleaseTimers[i] = beeReleaseInterval * i; // Staggered release
            beePenExitPhase[i] = true; // All bees start in pen-exit phase
            beeslowCounters[i] = 0;
        }
    }

    public void paintComponent(Graphics g) {
        super.paintComponent(g);
        draw(g);
    }

    public void draw(Graphics g) {
        g.drawImage(pacman.image, pacman.x, pacman.y, pacman.width, pacman.height, null);

        // Draw bees: if powerMode, use scaredBee.png
        for (Block bee : bees) {
            if (powerMode && scaredBeeImage != null) {
                g.drawImage(scaredBeeImage, bee.x, bee.y, bee.width, bee.height, null);
            } else {
                g.drawImage(bee.image, bee.x, bee.y, bee.width, bee.height, null);
            }
        }

        for (Block wall : walls) {
            g.drawImage(wall.image, wall.x, wall.y, wall.width, wall.height, null);
        }

        g.setColor(Color.WHITE);
        for (Block food : foods) {
            g.fillRect(food.x, food.y, food.width, food.height);
        }
        // Draw powerups: alternate cherry and cherry2
        int idx = 0;
        for (Block powerup : powerups) {
            if (idx % 2 == 0 && cherryImage != null) {
                g.drawImage(cherryImage, powerup.x, powerup.y, powerup.width, powerup.height, null);
            } else if (cherry2Image != null) {
                g.drawImage(cherry2Image, powerup.x, powerup.y, powerup.width, powerup.height, null);
            } else {
                g.setColor(Color.YELLOW);
                g.fillOval(powerup.x, powerup.y, powerup.width, powerup.height);
            }
            idx++;
        }
        // Powerup effect indicator using powerFood.png
        if (powerMode) {
            Graphics2D g2 = (Graphics2D) g;
            g2.setStroke(new BasicStroke(4));
            g2.setColor(new Color(255, 215, 0, 180));
            g2.drawOval(pacman.x - 4, pacman.y - 4, pacman.width + 8, pacman.height + 8);
            if (powerFoodImage != null) {
                g2.drawImage(powerFoodImage, tileSize * 8, 6, 18, 18, null);
            } else {
                g2.setColor(Color.YELLOW);
                g2.fillOval(tileSize * 8, 6, 18, 18);
                g2.setColor(Color.ORANGE);
                g2.drawOval(tileSize * 8, 6, 18, 18);
            }
            g2.setFont(new Font("Arial", Font.BOLD, 18));
            g2.setColor(new Color(255, 215, 0));
            g2.drawString("POWER UP!", tileSize * 9, 20);
        }
        // Draw remaining lives as pacman icons
        for (int i = 0; i < lives; i++) {
            g.drawImage(pacmanRightImage, tileSize/2 + i*tileSize, boardHeight - tileSize, tileSize, tileSize, null);
        }
        //score
        g.setFont(new Font("Arial", Font.PLAIN, 18));
        if (gameOver) {
            g.drawString("Game Over: " + String.valueOf(score), tileSize/2, tileSize/2);
        }
        else if (playAgainButton.isVisible()) {
            g.setFont(new Font("Arial", Font.BOLD, 40));
            g.setColor(new Color(0, 200, 0));
            g.drawString("YOU WON!", boardWidth/2 - 110, boardHeight/2 - 30);
            g.setFont(new Font("Arial", Font.PLAIN, 22));
            g.setColor(Color.WHITE);
            g.drawString("Score: " + score, boardWidth/2 - 40, boardHeight/2);
            g.drawString("Click Play Again to restart", boardWidth/2 - 110, boardHeight/2 + 30);
        }
        if (gameOver) {
            g.setFont(new Font("Arial", Font.BOLD, 32));
            g.setColor(Color.RED);
            g.drawString("Game Over", boardWidth/2 - 90, boardHeight/2 - 30);
            g.setFont(new Font("Arial", Font.PLAIN, 18));
            g.setColor(Color.WHITE);
            g.drawString("Score: " + score, boardWidth/2 - 40, boardHeight/2);
            g.drawString("Click Restart to play again", boardWidth/2 - 90, boardHeight/2 + 30);
        }
    }

    // Helper to set Pacman's image based on direction
    private void setPacmanImageByDirection(char dir) {
        if (dir == 'U') pacman.image = pacmanUpImage;
        else if (dir == 'D') pacman.image = pacmanDownImage;
        else if (dir == 'L') pacman.image = pacmanLeftImage;
        else if (dir == 'R') pacman.image = pacmanRightImage;
    }

    public void move() {
        // Try to turn if bufferedDirection is set and possible
        if (bufferedDirection != pacman.direction) {
            int vx = 0, vy = 0;
            if (bufferedDirection == 'U') vy = -tileSize/4;
            if (bufferedDirection == 'D') vy = tileSize/4;
            if (bufferedDirection == 'L') vx = -tileSize/4;
            if (bufferedDirection == 'R') vx = tileSize/4;
            int testX = pacman.x + vx;
            int testY = pacman.y + vy;
            boolean canTurn = true;
            for (Block wall : walls) {
                if (collision(new Block(null, testX, testY, pacman.width, pacman.height), wall)) {
                    canTurn = false;
                    break;
                }
            }
            if (canTurn) {
                pacman.updateDirection(bufferedDirection);
                setPacmanImageByDirection(bufferedDirection);
            }
        }
        // Calculate next position
        int nextX = pacman.x + pacman.velocityX;
        int nextY = pacman.y + pacman.velocityY;
        // Tunnel wrap: if Pacman goes off left/right edge, wrap to the other side
        if (nextX + pacman.width <= 0) {
            pacman.x = boardWidth - pacman.width;
        } else if (nextX >= boardWidth) {
            pacman.x = 0;
        } else if (nextY < 0 || nextY + pacman.height > boardHeight) {
            // Prevent Pacman from moving out of bounds vertically
            return;
        } else {
            pacman.x = nextX;
            pacman.y = nextY;
        }
        setPacmanImageByDirection(pacman.direction);

        //check wall collisions
        for (Block wall : walls) {
            if (collision(pacman, wall)) {
                pacman.x -= pacman.velocityX;
                pacman.y -= pacman.velocityY;
                break;
            }
        }

        // Bee release logic
        for (int i = 0; i < beeList.size(); i++) {
            Block bee = beeList.get(i);
            if (beeReleaseTimers[i] > 0) {
                beeReleaseTimers[i]--;
                continue;
            }
            // Pen exit phase: blue goes right, orange goes left, then up until out
            if (beePenExitPhase[i]) {
                // Only blue and orange: move one tile right/left, then up
                if (bee == yellowBee && bee.direction != 'R' && bee.x == yellowBee.startX && bee.y == yellowBee.startY) {
                    bee.direction = 'R';
                    bee.updateVelocity();
                } else if (bee == orangeBee && bee.direction != 'L' && bee.x == orangeBee.startX && bee.y == orangeBee.startY) {
                    bee.direction = 'L';
                    bee.updateVelocity();
                } else if (bee.direction != 'U') {
                    bee.direction = 'U';
                    bee.updateVelocity();
                }
                // Move bee
                bee.x += bee.velocityX;
                bee.y += bee.velocityY;
                // For blue/orange, after moving one tile, switch to up
                if (bee == yellowBee && bee.direction == 'R' && bee.x >= yellowBee.startX + tileSize) {
                    bee.direction = 'U';
                    bee.updateVelocity();
                } else if (bee == orangeBee && bee.direction == 'L' && bee.x <= orangeBee.startX - tileSize) {
                    bee.direction = 'U';
                    bee.updateVelocity();
                }
                // If bee is above the pen, exit pen-exit phase
                if (bee.y <= tileSize * 8) {
                    beePenExitPhase[i] = false;
                }
                continue;
            }
            // Make bees slower in power mode (move 2 out of every 3 frames)
            if (powerMode) {
                beeslowCounters[i] = (beeslowCounters[i] + 1) % 3;
                if (beeslowCounters[i] == 2) {
                    continue; // Skip this frame for this bee (move on 0,1; skip on 2)
                }
            } else {
                beeslowCounters[i] = 0; // Reset when not in power mode
            }
            // Move bee as normal
            bee.x += bee.velocityX;
            bee.y += bee.velocityY;
            for (Block wall : walls) {
                if (collision(bee, wall) || bee.x <= 0 || bee.x + bee.width >= boardWidth) {
                    bee.x -= bee.velocityX;
                    bee.y -= bee.velocityY;
                    char newDirection = directions[random.nextInt(4)];
                    bee.updateDirection(newDirection);
                }
            }
        }

        //check bee collisions
        for (int i = 0; i < beeList.size(); i++) {
            Block bee = beeList.get(i);
            if (beeReleaseTimers[i] > 0) continue; // Not yet released
            if (collision(bee, pacman)) {
                if (powerMode) {
                    score += 200;
                    bee.reset();
                    // Do NOT reset beeReleaseTimers[i] here; bee respawns immediately
                } else {
                    lives--;
                    if (lives <= 0) {
                        gameOver = true;
                        return;
                    }
                    resetPositions();
                    return;
                }
            }
        }

        //check food collision
        Block foodEaten = null;
        for (Block food : foods) {
            if (collision(pacman, food)) {
                foodEaten = food;
                score += 10;
            }
        }
        foods.remove(foodEaten);

        //check powerup collision
        Block powerupEaten = null;
        for (Block powerup : powerups) {
            if (collision(pacman, powerup)) {
                powerupEaten = powerup;
                powerMode = true;
                powerModeTicks = 150; // shorter duration, e.g. 4 seconds at 20fps
            }
        }
        powerups.remove(powerupEaten);
        // If all cherries are gone, spawn a new one
        if (powerups.isEmpty()) {
            spawnRandomCherry();
        }

        if (foods.isEmpty()) {
            playAgainButton.setVisible(true);
            gameLoop.stop();
            return;
        }

        if (powerMode) {
            powerModeTicks--;
            if (powerModeTicks <= 0) {
                powerMode = false;
            }
        }
    }

    private void spawnRandomCherry() {
        // Find all empty tiles (no wall, no food, no powerup, no pacman, no bee)
        java.util.List<int[]> emptyTiles = new java.util.ArrayList<>();
        for (int r = 0; r < rowCount; r++) {
            for (int c = 0; c < columnCount; c++) {
                int x = c * tileSize;
                int y = r * tileSize;
                boolean occupied = false;
                for (Block wall : walls) if (wall.x == x && wall.y == y) occupied = true;
                for (Block food : foods) if (food.x == x + 14 && food.y == y + 14) occupied = true;
                for (Block powerup : powerups) if (powerup.x == x + 8 && powerup.y == y + 8) occupied = true;
                if (pacman.x == x && pacman.y == y) occupied = true;
                for (Block bee : bees) if (bee.x == x && bee.y == y) occupied = true;
                if (!occupied) emptyTiles.add(new int[]{x, y});
            }
        }
        if (!emptyTiles.isEmpty()) {
            int[] pos = emptyTiles.get(random.nextInt(emptyTiles.size()));
            // Alternate between cherry.png and cherry2.png
            Image cherryImg = (random.nextBoolean() ? cherryImage : cherry2Image);
            Block cherry = new Block(cherryImg, pos[0] + 8, pos[1] + 8, 16, 16);
            powerups.add(cherry);
        }
    }

    public boolean collision(Block a, Block b) {
        return  a.x < b.x + b.width &&
                a.x + a.width > b.x &&
                a.y < b.y + b.height &&
                a.y + a.height > b.y;
    }

    public void resetPositions() {
        // Respawn Pacman at pink bee's spawn location
        Block pinkBee = null;
        for (Block bee : beeList) {
            if (bee.image == yellowOrangeBeeImage) {
                pinkBee = bee;
                break;
            }
        }
        if (pinkBee != null) {
            pacman.x = pinkBee.startX;
            pacman.y = pinkBee.startY - 4 * tileSize;
        } else {
            pacman.reset(); // fallback
            pacman.y -= 4 * tileSize;
        }
        pacman.velocityX = 0;
        pacman.velocityY = 0;
        pacman.direction = 'U';
        pacman.updateVelocity();
        setPacmanImageByDirection('U');
        bufferedDirection = 'U';
        // Do NOT reset bees here; bees remain where they are
        // Only reset bee release/pen/slow counters on full game restart
    }

    @Override
    public void actionPerformed(ActionEvent e) {
        move();
        repaint();
        if (gameOver) {
            gameLoop.stop();
            restartButton.setVisible(true);
            restartButton.requestFocusInWindow();
        }
        if (playAgainButton.isVisible()) {
            playAgainButton.requestFocusInWindow();
        }
    }

    @Override
    public void keyTyped(KeyEvent e) {}

    @Override
    public void keyPressed(KeyEvent e) {}

    @Override
    public void keyReleased(KeyEvent e) {
        if (gameOver || playAgainButton.isVisible()) {
            // Do nothing, use button to restart
            return;
        }
        if (e.getKeyCode() == KeyEvent.VK_UP) {
            bufferedDirection = 'U';
        }
        else if (e.getKeyCode() == KeyEvent.VK_DOWN) {
            bufferedDirection = 'D';
        }
        else if (e.getKeyCode() == KeyEvent.VK_LEFT) {
            bufferedDirection = 'L';
        }
        else if (e.getKeyCode() == KeyEvent.VK_RIGHT) {
            bufferedDirection = 'R';
        }
    }
}
