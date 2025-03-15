package com.AlgorithmSnake;
import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.*;
import java.util.List;
import java.util.stream.Collectors;

public class AlgorithmSnake extends JPanel implements ActionListener {
    // Game constants
    private static final int WIDTH = 100;
    private static final int HEIGHT = 100;
    private static final int UNIT_SIZE = 5;
    private static final int GAME_SPEED = 50; // Milliseconds per move
    private boolean optimal;
    private static final int MIN_POS = 1;
    private static final int MAX_POS = 98;

    // Game objects
    private List<Snake> snakes;
    private Snake greenSnake;
    private Snake redSnake;
    private Eatable eatable;
    private javax.swing.Timer timer;

    public AlgorithmSnake(boolean optimal) {
        this.optimal = optimal;
        setPreferredSize(new Dimension(WIDTH * UNIT_SIZE, HEIGHT * UNIT_SIZE));
        setBackground(Color.BLACK);
        snakes = new ArrayList<>();

        // Initialize snake and eatable
        snakes.add(new Snake(new Point(50, 50), Color.GREEN, true));
        snakes.add(new Snake(new Point(10, 20), Color.RED, false));
        eatable = new Eatable();
        eatable.spawn(snakes.stream().map(s -> s.body).collect(Collectors.toList()));

        // Start game loop
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

    @Override
    public void actionPerformed(ActionEvent e) {
        System.out.println("Game loop tick");
        for (Snake snake : snakes) {
            // Gather all snake bodies as obstacles
            List<List<Point>> allBodies = snakes.stream().map(s -> s.body).collect(Collectors.toList());

            // Find path using the snake's optimal setting
            List<Point> path = Pathfinder.aStar(snake, eatable, allBodies, snake.optimal);
            System.out.println("Path for " + snake.color + " snake: " + path);

            if (!path.isEmpty()) {
                Point nextPosition = path.get(0);
                if (!willCollide(snake, nextPosition)) {
                    snake.setDirection(nextPosition);
                    snake.move();
                } else {
                    // Recalculate path if there's a collision
                    allBodies = snakes.stream().map(s -> s.body).collect(Collectors.toList()); // Reassign, no new declaration
                    path = Pathfinder.aStar(snake, eatable, allBodies, snake.optimal);
                    if (!path.isEmpty() && !willCollide(snake, path.get(0))) {
                        snake.setDirection(path.get(0));
                        snake.move();
                    }
                }
            } else {
                // Fallback movement if no path exists
                Point newHead = new Point(snake.getHead().x + snake.direction.x,
                        snake.getHead().y + snake.direction.y);
                if (!willCollide(snake, newHead)) {
                    snake.move();
                }
            }

            // Check if snake eats the eatable
            if (snake.getHead().equals(eatable.position)) {
                snake.grow();
                eatable.spawn(snakes.stream().map(s -> s.body).collect(Collectors.toList()));
            }
        }
        repaint();
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
    }

    public static void main(String[] args) {
        JFrame frame = new JFrame("A* Snake Game");
        AlgorithmSnake game = new AlgorithmSnake(false);
        frame.add(game);
        frame.pack();
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        frame.setVisible(true);
    }
}

// -------------------- Snake Class --------------------
class Snake {
    List<Point> body;
    Point direction;
    Color color;
    boolean optimal;

    public Snake(Point start, Color color, boolean optimal) {
        this.color = color;
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

// -------------------- Eatable Class --------------------
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

// -------------------- Pathfinder (A* Algorithm) --------------------
class Pathfinder {
    public static List<Point> aStar(Snake snake, Eatable eatable, List<List<Point>> allSnakeBodies, boolean optimal) {
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

            for (Point neighbor : getNeighbors(current.position)) {
                if (visited.contains(neighbor) || isObstacle(neighbor, allSnakeBodies)) {
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

    private static List<Point> getNeighbors(Point p) {
        List<Point> neighbors = new ArrayList<>();
        if (p.x < 98) neighbors.add(new Point(p.x + 1, p.y)); // Right
        if (p.x > 1) neighbors.add(new Point(p.x - 1, p.y)); // Left
        if (p.y < 98) neighbors.add(new Point(p.x, p.y + 1)); // Down
        if (p.y > 1) neighbors.add(new Point(p.x, p.y - 1)); // Up
        return neighbors;
    }

    private static boolean isObstacle(Point p, List<List<Point>> bodies) {
        return bodies.stream().anyMatch(body -> body.contains(p));
    }
}

// Helper class for A* nodes
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


