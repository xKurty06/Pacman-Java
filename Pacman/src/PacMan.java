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
    private Image blueGhostImage;
    private Image orangeGhostImage;
    private Image pinkGhostImage;
    private Image redGhostImage;

    private Image pacmanUpImage;
    private Image pacmanDownImage;
    private Image pacmanLeftImage;
    private Image pacmanRightImage;

    private Image cherryImage;
    private Image cherry2Image;
    private Image powerFoodImage;
    private Image scaredGhostImage;

    //X = wall, O = skip, P = pac man, ' ' = food
    //Ghosts: b = blue, o = orange, p = pink, r = red
    private String[] tileMap = {
        "XXXXXXXXXXXXXXXXXXX",
        "XO       X       OX",
        "X XX XXX X XXX XX X",
        "X                 X",
        "X XX X XXXXX X XX X",
        "X    X   O   X    X",
        "XXXX XXXX XXXX XXXX",
        "   X X       X X   ",
        "XXXX X XXrXX X XXXX",
        "XO      bpo      OX",
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
    HashSet<Block> ghosts;
    HashSet<Block> powerups;
    Block pacman;

    Timer gameLoop;
    char[] directions = {'U', 'D', 'L', 'R'}; //up down left right
    Random random = new Random();
    int score = 0;
    int lives = 3;
    boolean gameOver = false;
    boolean powerMode = false;
    int powerModeTicks = 0;

    // Add buffered direction for smooth turning
    private char bufferedDirection = 'R'; // Start moving right by default

    private JButton restartButton;

    PacMan() {
        setPreferredSize(new Dimension(boardWidth, boardHeight));
        setBackground(Color.BLACK);
        addKeyListener(this);
        setFocusable(true);

        //load images
        wallImage = new ImageIcon(getClass().getResource("/assets/wall.png")).getImage();
        blueGhostImage = new ImageIcon(getClass().getResource("/assets/blueGhost.png")).getImage();
        orangeGhostImage = new ImageIcon(getClass().getResource("/assets/orangeGhost.png")).getImage();
        pinkGhostImage = new ImageIcon(getClass().getResource("/assets/pinkGhost.png")).getImage();
        redGhostImage = new ImageIcon(getClass().getResource("/assets/redGhost.png")).getImage();

        pacmanUpImage = new ImageIcon(getClass().getResource("/assets/pacmanUp.png")).getImage();
        pacmanDownImage = new ImageIcon(getClass().getResource("/assets/pacmanDown.png")).getImage();
        pacmanLeftImage = new ImageIcon(getClass().getResource("/assets/pacmanLeft.png")).getImage();
        pacmanRightImage = new ImageIcon(getClass().getResource("/assets/pacmanRight.png")).getImage();

        cherryImage = new ImageIcon(getClass().getResource("/assets/cherry.png")).getImage();
        cherry2Image = new ImageIcon(getClass().getResource("/assets/cherry2.png")).getImage();
        powerFoodImage = new ImageIcon(getClass().getResource("/assets/powerFood.png")).getImage();
        scaredGhostImage = new ImageIcon(getClass().getResource("/assets/scaredGhost.png")).getImage();

        restartButton = new JButton("Restart");
        restartButton.setFocusable(false);
        restartButton.setVisible(false);
        restartButton.addActionListener(e -> restartGame());
        this.setLayout(null);
        restartButton.setBounds(boardWidth/2 - 60, boardHeight/2, 120, 40);
        this.add(restartButton);

        loadMap();
        pacman.direction = 'U';
        pacman.updateVelocity();
        setPacmanImageByDirection('U');
        bufferedDirection = 'U';
        for (Block ghost : ghosts) {
            char newDirection = directions[random.nextInt(4)];
            ghost.updateDirection(newDirection);
        }
        //how long it takes to start timer, milliseconds gone between frames
        gameLoop = new Timer(50, this); //20fps (1000/50)
        gameLoop.start();

        // Set Pacman's initial direction and velocity after map load
        pacman.direction = 'R';
        pacman.updateVelocity();
        bufferedDirection = 'R';
    }

    private void restartGame() {
        loadMap();
        resetPositions();
        lives = 3;
        score = 0;
        gameOver = false;
        restartButton.setVisible(false);
        gameLoop.start();
        requestFocusInWindow();
    }

    public void loadMap() {
        walls = new HashSet<Block>();
        foods = new HashSet<Block>();
        ghosts = new HashSet<Block>();
        powerups = new HashSet<Block>();

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
                else if (tileMapChar == 'b') { //blue ghost
                    Block ghost = new Block(blueGhostImage, x, y, tileSize, tileSize);
                    ghosts.add(ghost);
                }
                else if (tileMapChar == 'o') { //orange ghost
                    Block ghost = new Block(orangeGhostImage, x, y, tileSize, tileSize);
                    ghosts.add(ghost);
                }
                else if (tileMapChar == 'p') { //pink ghost
                    Block ghost = new Block(pinkGhostImage, x, y, tileSize, tileSize);
                    ghosts.add(ghost);
                }
                else if (tileMapChar == 'r') { //red ghost
                    Block ghost = new Block(redGhostImage, x, y, tileSize, tileSize);
                    ghosts.add(ghost);
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
    }

    public void paintComponent(Graphics g) {
        super.paintComponent(g);
        draw(g);
    }

    public void draw(Graphics g) {
        g.drawImage(pacman.image, pacman.x, pacman.y, pacman.width, pacman.height, null);

        // Draw ghosts: if powerMode, use scaredGhost.png
        for (Block ghost : ghosts) {
            if (powerMode && scaredGhostImage != null) {
                g.drawImage(scaredGhostImage, ghost.x, ghost.y, ghost.width, ghost.height, null);
            } else {
                g.drawImage(ghost.image, ghost.x, ghost.y, ghost.width, ghost.height, null);
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
        else {
            g.drawString("x" + String.valueOf(lives) + " Score: " + String.valueOf(score), tileSize/2, tileSize/2);
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
        // Prevent Pacman from moving out of bounds
        if (nextX < 0 || nextX + pacman.width > boardWidth || nextY < 0 || nextY + pacman.height > boardHeight) {
            return;
        }
        pacman.x = nextX;
        pacman.y = nextY;
        setPacmanImageByDirection(pacman.direction);

        //check wall collisions
        for (Block wall : walls) {
            if (collision(pacman, wall)) {
                pacman.x -= pacman.velocityX;
                pacman.y -= pacman.velocityY;
                break;
            }
        }

        //check ghost collisions
        for (Block ghost : ghosts) {
            if (collision(ghost, pacman)) {
                if (powerMode) {
                    score += 200;
                    ghost.reset();
                } else {
                    lives--;
                    if (lives <= 0) {
                        gameOver = true;
                        return;
                    }
                    resetPositions();
                }
            }

            if (ghost.y == tileSize*9 && ghost.direction != 'U' && ghost.direction != 'D') {
                ghost.updateDirection('U');
            }
            ghost.x += ghost.velocityX;
            ghost.y += ghost.velocityY;
            for (Block wall : walls) {
                if (collision(ghost, wall) || ghost.x <= 0 || ghost.x + ghost.width >= boardWidth) {
                    ghost.x -= ghost.velocityX;
                    ghost.y -= ghost.velocityY;
                    char newDirection = directions[random.nextInt(4)];
                    ghost.updateDirection(newDirection);
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
            loadMap();
            resetPositions();
        }

        if (powerMode) {
            powerModeTicks--;
            if (powerModeTicks <= 0) {
                powerMode = false;
            }
        }
    }

    private void spawnRandomCherry() {
        // Find all empty tiles (no wall, no food, no powerup, no pacman, no ghost)
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
                for (Block ghost : ghosts) if (ghost.x == x && ghost.y == y) occupied = true;
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
        pacman.reset();
        pacman.velocityX = 0;
        pacman.velocityY = 0;
        pacman.direction = 'U';
        pacman.updateVelocity();
        setPacmanImageByDirection('U');
        bufferedDirection = 'U';
        for (Block ghost : ghosts) {
            ghost.reset();
            char newDirection = directions[random.nextInt(4)];
            ghost.updateDirection(newDirection);
        }
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
    }

    @Override
    public void keyTyped(KeyEvent e) {}

    @Override
    public void keyPressed(KeyEvent e) {}

    @Override
    public void keyReleased(KeyEvent e) {
        if (gameOver) {
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
