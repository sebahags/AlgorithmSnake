package com.AlgorithmSnake;
import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.KeyListener;
import java.awt.event.KeyEvent;
import java.util.*;
import java.util.List;
import java.util.stream.Collectors;

public class AlgorithmSnake extends JPanel implements ActionListener, KeyListener {
    // Game constants
    private static final int WIDTH = 100;
    private static final int HEIGHT = 100;
    private static final int UNIT_SIZE = 5;
    private static final int GAME_SPEED = 50; // Milliseconds per move
    private static final int MIN_POS = 1;
    private static final int MAX_POS = 98;
    // Game objects
    private List<Snake> snakes;
    private Snake greenSnake;
    private Snake redSnake;
    private Eatable eatable;
    private javax.swing.Timer timer;
    private JLabel greenScoreLabel;
    private JLabel redScoreLabel;
    private JLabel blueScoreLabel;
    private JLabel playerScoreLabel;
    private boolean playerMode;
    private Snake playerSnake;
    private boolean gameOver = false;
    enum PathAlgorithm{
        ASTAR,
        BFS,
        DIJKSTRA
    }
    public AlgorithmSnake(boolean playerMode) {
        this.playerMode = playerMode;
        setPreferredSize(new Dimension(WIDTH * UNIT_SIZE, HEIGHT * UNIT_SIZE));
        setBackground(Color.BLACK);
        setFocusable(true);
        addKeyListener(this); // Enable keyboard input

        setLayout(null);
        // Initialize score labels for AI snakes
        greenScoreLabel = new JLabel("ASTAR: 0");
        redScoreLabel = new JLabel("BFS: 0");
        blueScoreLabel = new JLabel("DIJKSTRA: 0");
        greenScoreLabel.setBounds(10, 10, 100, 20);
        redScoreLabel.setBounds(10, 30, 100, 20);
        blueScoreLabel.setBounds(10, 50, 100, 20);
        add(greenScoreLabel);
        add(redScoreLabel);
        add(blueScoreLabel);
        // Initialize snake list and score labels (optional, add labels to your UI as needed)
        snakes = new ArrayList<>();

        // If player mode is enabled, create the player snake (magenta) at a fixed start point.
        if (playerMode) {
            playerSnake = new Snake(new Point(50, 50), Color.MAGENTA, null, false);
            snakes.add(playerSnake);

            playerScoreLabel = new JLabel("PLAYER: 0");
            playerScoreLabel.setBounds(10, 70, 100, 20);
            add(playerScoreLabel);
        }

        // Add AI-controlled snakes
        snakes.add(new Snake(new Point(40, 55), Color.GREEN, PathAlgorithm.ASTAR, true));
        snakes.add(new Snake(new Point(20, 30), Color.RED, PathAlgorithm.BFS, true));
        snakes.add(new Snake(new Point(75, 75), Color.BLUE, PathAlgorithm.DIJKSTRA, true));

        // Create and spawn the eatable
        eatable = new Eatable();
        eatable.spawn(snakes.stream().map(s -> s.body).collect(Collectors.toList()));

        // Start game loop timer
        timer = new javax.swing.Timer(GAME_SPEED, this);
        timer.start();
    }
    private boolean willCollide(Snake currentSnake, Point nextPosition){
        // Check boundaries: only allow positions from 1 to 98 inclusive
        if (nextPosition.x < MIN_POS || nextPosition.x > MAX_POS || nextPosition.y < MIN_POS || nextPosition.y > MAX_POS) {
            return true;
        }
        // Check collision with all snakes (unchanged)
        for (Snake snake : snakes) {
            for (int i = 0; i < snake.body.size(); i++) {
                Point p = snake.body.get(i);
                if (p.equals(nextPosition)) {
                    // Allow current snake to move into its own tail
                    if (snake == currentSnake && i == snake.body.size() - 1) {
                        continue;
                    }
                    return true;
                }
            }
        }
        return false;
    }

    private List<Point> getPossibleDirections(Point currentDirection) {
        List<Point> directions = new ArrayList<>();
        // Add current direction
        directions.add(new Point(currentDirection.x, currentDirection.y));
        // Add perpendicular directions based on current direction
        if (currentDirection.x != 0) { // Horizontal movement (left or right)
            directions.add(new Point(0, 1));  // Down
            directions.add(new Point(0, -1)); // Up
        } else if (currentDirection.y != 0) { // Vertical movement (up or down)
            directions.add(new Point(1, 0));  // Right
            directions.add(new Point(-1, 0)); // Left
        }
        return directions;
    }

    @Override
    public void actionPerformed(ActionEvent e) {
        if (gameOver) return;

        List<Snake> snakesToRemove = new ArrayList<>();

        // Iterate over a copy of the list to avoid concurrent modification
        for (Snake snake : new ArrayList<>(snakes)) {
            List<List<Point>> allBodies = snakes.stream().map(s -> s.body).collect(Collectors.toList());

            if (playerMode && snake == playerSnake) {
                movePlayerSnake(snakesToRemove);
                continue;
            }

            List<Point> path;
            if (snake.algorithm == PathAlgorithm.ASTAR) {
                path = Pathfinder.aStar(snake, eatable, allBodies, snake.optimal, MIN_POS, MAX_POS);
            } else if (snake.algorithm == PathAlgorithm.BFS) {
                path = Pathfinder.bfs(snake, eatable, allBodies, snake.optimal, MIN_POS, MAX_POS);
            } else if (snake.algorithm == PathAlgorithm.DIJKSTRA) {
                path = Pathfinder.dijkstra(snake, eatable, allBodies, snake.optimal, MIN_POS, MAX_POS);
            } else {
                path = new ArrayList<>();
            }
            System.out.println("Path for " + snake.color + " snake: " + path);

            boolean moved = false;
            if (!path.isEmpty()) {
                Point nextPosition = path.get(0);
                if (!willCollide(snake, nextPosition)) {
                    snake.setDirection(nextPosition);
                    snake.move();
                    moved = true;
                }
            }
            if (!moved) {
                Point newHead = new Point(snake.getHead().x + snake.direction.x, snake.getHead().y + snake.direction.y);
                if (!willCollide(snake, newHead)) {
                    snake.move();
                    moved = true;
                } else {
                    List<Point> possibleDirections = getPossibleDirections(snake.direction);
                    for (Point dir : possibleDirections) {
                        Point testHead = new Point(snake.getHead().x + dir.x, snake.getHead().y + dir.y);
                        if (!willCollide(snake, testHead)) {
                            snake.setDirection(testHead);
                            snake.move();
                            moved = true;
                            break;
                        }
                    }
                    if (!moved) {
                        snakesToRemove.add(snake);
                    }
                }
            }

            if (snake.getHead().equals(eatable.position)) {
                snake.eatEatable();
                eatable.spawn(snakes.stream().map(s -> s.body).collect(Collectors.toList()));
            }
        }

        // Update scores (omitted here for brevity)
        for (Snake snake : snakes) {
            if (snake.color == Color.GREEN) {
                greenScoreLabel.setText("ASTAR: " + snake.score);
            } else if (snake.color == Color.RED) {
                redScoreLabel.setText("BFS: " + snake.score);
            } else if (snake.color == Color.BLUE) {
                blueScoreLabel.setText("DIJKSTRA: " + snake.score);
            } else if (playerMode && snake == playerSnake) {
                playerScoreLabel.setText("PLAYER: " + snake.score);
            }
        }
        // Remove snakes marked for removal
        snakes.removeAll(snakesToRemove);

        if (snakes.size() <= 1) {
            gameOver = true;
            timer.stop();
        }

        repaint();
    }

    private void movePlayerSnake(List<Snake> snakesToRemove) {
        if (playerSnake == null) return;
        Point newHead = new Point(playerSnake.getHead().x + playerSnake.direction.x,
                playerSnake.getHead().y + playerSnake.direction.y);
        if (willCollide(playerSnake, newHead)) {
            // Mark the player snake for removal.
            snakesToRemove.add(playerSnake);
            playerSnake = null;
        } else {
            playerSnake.move();
            if (playerSnake.getHead().equals(eatable.position)) {
                playerSnake.eatEatable();
                eatable.spawn(snakes.stream().map(s -> s.body).collect(Collectors.toList()));
            }
        }
    }

    @Override
    protected void paintComponent(Graphics g) {
        super.paintComponent(g);
        // Draw borders
        g.setColor(Color.WHITE);
        g.fillRect(0, 0, WIDTH * UNIT_SIZE, UNIT_SIZE);
        g.fillRect(0, HEIGHT * UNIT_SIZE - UNIT_SIZE, WIDTH * UNIT_SIZE, UNIT_SIZE);
        g.fillRect(0, 0, UNIT_SIZE, HEIGHT * UNIT_SIZE);
        g.fillRect(WIDTH * UNIT_SIZE - UNIT_SIZE, 0, UNIT_SIZE, HEIGHT * UNIT_SIZE);
        // Draw eatable
        eatable.Paint(g, UNIT_SIZE);
        // Draw snake
        for (Snake snake : snakes){
            snake.Paint(g, UNIT_SIZE);
        }
        if (gameOver) {
            g.setColor(Color.WHITE);
            g.setFont(new Font("Arial", Font.BOLD, 20));
            String winner = "No one";
            if (snakes.size() == 1) {
                Snake winSnake = snakes.get(0);
                winner = (winSnake.algorithm == null) ? "PLAYER" : winSnake.algorithm.name();
            }
            String text = "Game Over, " + winner + " won! Esc for main menu.";
            FontMetrics fm = g.getFontMetrics();
            int textWidth = fm.stringWidth(text);
            int x = (WIDTH * UNIT_SIZE - textWidth) / 2;
            int y = HEIGHT * UNIT_SIZE / 2;
            g.drawString(text, x, y);
        }
    }

    // KeyListener implementations
    @Override
    public void keyPressed(KeyEvent e) {
        if (e.getKeyCode() == KeyEvent.VK_ESCAPE) {
            // Stop the timer if it's still running.
            if (timer != null && timer.isRunning()) {
                timer.stop();
            }
            // Get the frame, remove the game panel and add a new MainMenu.
            JFrame frame = (JFrame) SwingUtilities.getWindowAncestor(this);
            frame.getContentPane().removeAll();
            MainMenu menu = new MainMenu(frame);
            frame.add(menu);
            frame.pack();  // The MainMenu has its preferred size set.
            frame.revalidate();
            frame.repaint();
            return;
        }

        if (playerSnake == null) return;

        int key = e.getKeyCode();
        Point currentDir = playerSnake.direction;

        if (key == KeyEvent.VK_RIGHT) {
            // Rotate 90 degrees to the left
            playerSnake.direction = new Point(-currentDir.y, currentDir.x);
        } else if (key == KeyEvent.VK_LEFT) {
            // Rotate 90 degrees to the right
            playerSnake.direction = new Point(currentDir.y, -currentDir.x);
        }
    }

    @Override
    public void keyReleased(KeyEvent e) {
        // No action needed here.
    }

    @Override
    public void keyTyped(KeyEvent e) {
        // No action needed here.
    }

    public static void main(String[] args) {
        JFrame frame = new JFrame("Battle of Algorithms");
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        frame.setSize(800, 600);
        frame.setResizable(false);

        MainMenu menu = new MainMenu(frame);
        frame.add(menu);
        frame.setVisible(true);
    }
}


class Snake {
    List<Point> body;
    Point direction;
    Color color;
    AlgorithmSnake.PathAlgorithm algorithm;
    boolean optimal;
    int score = 0;

    public Snake(Point start, Color color, AlgorithmSnake.PathAlgorithm algorithm, boolean optimal) {
        this.color = color;
        this.algorithm = algorithm;
        this.optimal = optimal;
        body = new ArrayList<>();
        direction = new Point(1, 0); // Moves right initially

        // Initialize snake with 3 segments
        for (int i = 0; i < 3; i++) {
            body.add(new Point(start.x - i, start.y));
        }
    }
    public Point getHead() {
        return body.get(0);
    }
    public void setDirection(Point nextMove) {
        direction = new Point(nextMove.x - getHead().x, nextMove.y - getHead().y);
    }
    public void move() {
        Point newHead = new Point(getHead().x + direction.x, getHead().y + direction.y);
        body.add(0, newHead);
        body.remove(body.size() - 1);
    }
    public void eatEatable(){
        grow();
        score++;
    }
    public void grow() {
        body.add(new Point(body.get(body.size() - 1))); // Add at tail
    }
    public void Paint(Graphics g, int unitSize){
        g.setColor(color);
        for (Point p : body){
            g.fillRect(p.x * unitSize, p.y * unitSize, unitSize, unitSize);
        }
    }
}


class Eatable {
    Point position;

    public void Paint(Graphics g, int unitSize){
        g.setColor(Color.YELLOW);
        g.fillRect(position.x * unitSize, position.y * unitSize, unitSize, unitSize);
    }
    public void spawn(List<List<Point>> allSnakeBodies) {
        Random random = new Random();
        do {
            position = new Point(random.nextInt(98) + 1, random.nextInt(98) + 1);
        } while (allSnakeBodies.stream().anyMatch(body -> body.contains(position))); // Ensure it doesn't spawn inside the snake
    }
}


class Pathfinder {
    public static List<Point> aStar(Snake snake, Eatable eatable, List<List<Point>> allSnakeBodies, boolean optimal, int minPos, int maxPos) {
        PriorityQueue<Node> queue = new PriorityQueue<>(Comparator.comparingInt(n -> n.f));
        Map<Point, Node> nodes = new HashMap<>();
        Set<Point> visited = new HashSet<>();

        Point start = snake.getHead();
        Point end = eatable.position;

        Node startNode = new Node(start, null, 0, heuristic(start, end));
        nodes.put(start, startNode);
        queue.add(startNode);

        while (!queue.isEmpty()) {
            Node current = queue.poll();
            if (current.position.equals(end)) {
                return reconstructPath(current); // Found a path
            }

            visited.add(current.position);

            for (Point neighbor : getNeighbors(current.position, minPos, maxPos)) {
                if (visited.contains(neighbor) || isObstacle(neighbor, allSnakeBodies, snake)) {
                    continue;
                }

                int g = current.g + 1; // Cost to neighbor
                int h = heuristic(neighbor, end); // Heuristic to goal
                Node neighborNode = nodes.getOrDefault(neighbor, new Node(neighbor, null, Integer.MAX_VALUE, h));

                if (g < neighborNode.g) {
                    neighborNode.g = g;
                    neighborNode.f = g + h;
                    neighborNode.parent = current;
                    nodes.put(neighbor, neighborNode);

                    // If non-optimal and target reached, return immediately
                    if (!optimal && neighbor.equals(end)) {
                        return reconstructPath(neighborNode);
                    }
                    queue.add(neighborNode);
                }
            }
        }
        return new ArrayList<>(); // No path found
    }

    public static List<Point> bfs(Snake snake, Eatable eatable, List<List<Point>> allSnakeBodies, boolean optimal, int minPos, int maxPos) {
        Queue<Node> queue = new LinkedList<>();
        Map<Point, Node> visited = new HashMap<>();

        Point start = snake.getHead();
        Point end = eatable.position;

        Node startNode = new Node(start, null, 0, 0); // BFS doesn’t use heuristic
        queue.add(startNode);
        visited.put(start, startNode);

        while (!queue.isEmpty()) {
            Node current = queue.poll();
            if (current.position.equals(end)) {
                return reconstructPath(current);
            }

            for (Point neighbor : getNeighbors(current.position, minPos, maxPos)) {
                if (!visited.containsKey(neighbor) && !isObstacle(neighbor, allSnakeBodies, snake)) {
                    int g = current.g + 1;
                    Node neighborNode = new Node(neighbor, current, g, 0); // No heuristic in BFS
                    visited.put(neighbor, neighborNode);
                    queue.add(neighborNode);
                    if (!optimal && neighbor.equals(end)) {
                        return reconstructPath(neighborNode); // Early exit if non-optimal
                    }
                }
            }
        }
        return new ArrayList<>(); // No path found
    }
    public static List<Point> dijkstra(Snake snake, Eatable eatable, List<List<Point>> allSnakeBodies, boolean optimal, int minPos, int maxPos) {
        PriorityQueue<Node> queue = new PriorityQueue<>(Comparator.comparingInt(n -> n.g));
        Map<Point, Integer> bestG = new HashMap<>();

        Point start = snake.getHead();
        Point end = eatable.position;

        Node startNode = new Node(start, null, 0, 0); // h=0 for Dijkstra's
        bestG.put(start, 0);
        queue.add(startNode);

        while (!queue.isEmpty()) {
            Node current = queue.poll();

            // Skip if we've found a better path to this position already
            if (current.g > bestG.getOrDefault(current.position, Integer.MAX_VALUE)) {
                continue;
            }

            if (current.position.equals(end)) {
                return reconstructPath(current); // Shortest path found
            }

            for (Point neighbor : getNeighbors(current.position, minPos, maxPos)) {
                if (isObstacle(neighbor, allSnakeBodies, snake)) {
                    continue;
                }

                int tentative_g = current.g + 1;
                if (tentative_g < bestG.getOrDefault(neighbor, Integer.MAX_VALUE)) {
                    bestG.put(neighbor, tentative_g);
                    Node neighborNode = new Node(neighbor, current, tentative_g, 0);
                    queue.add(neighborNode);
                }
            }
        }
        return new ArrayList<>(); // No path found
    }

    private static int heuristic(Point a, Point b) {
        return Math.abs(a.x - b.x) + Math.abs(a.y - b.y); // Manhattan distance
    }

    private static List<Point> reconstructPath(Node node) {
        List<Point> path = new ArrayList<>();
        // Start from the parent of the end node to exclude the start position
        while (node.parent != null) {
            path.add(0, node.position);
            node = node.parent;
        }
        return path;
    }

    private static List<Point> getNeighbors(Point p, int minPos, int maxPos) {
        List<Point> neighbors = new ArrayList<>();
        if (p.x < maxPos) neighbors.add(new Point(p.x + 1, p.y)); // Right
        if (p.x > minPos) neighbors.add(new Point(p.x - 1, p.y)); // Left
        if (p.y < maxPos) neighbors.add(new Point(p.x, p.y + 1)); // Down
        if (p.y > minPos) neighbors.add(new Point(p.x, p.y - 1)); // Up
        return neighbors;
    }

    private static boolean isObstacle(Point p, List<List<Point>> bodies, Snake currentSnake) {
        for (List<Point> body : bodies) {
            if (body == currentSnake.body) {
                // Exclude the tail (last segment)
                for (int i = 0; i < body.size() - 1; i++) {
                    if (body.get(i).equals(p)) {
                        return true;
                    }
                }
            } else {
                if (body.contains(p)) {
                    return true;
                }
            }
        }
        return false;
    }
}

class Node {
    Point position;
    Node parent;
    int g; // Cost from start
    int f; // g + heuristic

    Node(Point position, Node parent, int g, int h) {
        this.position = position;
        this.parent = parent;
        this.g = g;
        this.f = g + h;
    }
}


