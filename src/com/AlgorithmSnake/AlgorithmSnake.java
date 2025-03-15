package com.AlgorithmSnake;
import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.*;
import java.util.List;

public class AlgorithmSnake extends JPanel implements ActionListener {
    // Game constants
    private static final int WIDTH = 100;
    private static final int HEIGHT = 100;
    private static final int UNIT_SIZE = 5;
    private static final int GAME_SPEED = 50; // Milliseconds per move
    private boolean optimal;

    // Game objects
    private Snake snake;
    private Eatable eatable;
    private javax.swing.Timer timer;

    public AlgorithmSnake(boolean optimal) {
        this.optimal = optimal;
        setPreferredSize(new Dimension(WIDTH * UNIT_SIZE, HEIGHT * UNIT_SIZE));
        setBackground(Color.BLACK);

        // Initialize snake and eatable
        snake = new Snake(new Point(50, 50));
        eatable = new Eatable();
        eatable.spawn(snake.body);

        // Start game loop
        timer = new javax.swing.Timer(GAME_SPEED, this);
        timer.start();
    }

    @Override
    public void actionPerformed(ActionEvent e) {
        // Find path using A*
        List<Point> path = Pathfinder.aStar(snake, eatable, optimal);

        // Move snake based on path
        if (!path.isEmpty()) {
            Point nextMove = path.get(0);
            snake.setDirection(nextMove);
        }

        snake.move();

        // Check if snake eats the eatable
        if (snake.getHead().equals(eatable.position)) {
            snake.grow();
            eatable.spawn(snake.body);
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
        g.setColor(Color.YELLOW);
        g.fillRect(eatable.position.x * UNIT_SIZE, eatable.position.y * UNIT_SIZE, UNIT_SIZE, UNIT_SIZE);

        // Draw snake
        g.setColor(Color.GREEN);
        for (Point p : snake.body) {
            g.fillRect(p.x * UNIT_SIZE, p.y * UNIT_SIZE, UNIT_SIZE, UNIT_SIZE);
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

    public Snake(Point start) {
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
}

// -------------------- Eatable Class --------------------
class Eatable {
    Point position;

    public void spawn(List<Point> snakeBody) {
        Random random = new Random();
        do {
            position = new Point(random.nextInt(98) + 1, random.nextInt(98) + 1);
        } while (snakeBody.contains(position)); // Ensure it doesn't spawn inside the snake
    }
}

// -------------------- Pathfinder (A* Algorithm) --------------------
class Pathfinder {
    public static List<Point> aStar(Snake snake, Eatable eatable, boolean optimal) {
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
                return reconstructPath(current);
            }

            visited.add(current.position);

            for (Point neighbor : getNeighbors(current.position)) {
                if (visited.contains(neighbor) || snake.body.contains(neighbor)) continue;

                int g = current.g + 1;
                int h = heuristic(neighbor, end);
                Node neighborNode = nodes.getOrDefault(neighbor, new Node(neighbor, null, Integer.MAX_VALUE, h));

                if (g < neighborNode.g) {
                    neighborNode.g = g;
                    neighborNode.f = g + h;
                    neighborNode.parent = current;
                    nodes.put(neighbor, neighborNode);
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
        return Math.abs(a.x - b.x) + Math.abs(a.y - b.y);
    }

    private static List<Point> getNeighbors(Point p) {
        List<Point> neighbors = new ArrayList<>();
        if (p.x > 1) neighbors.add(new Point(p.x - 1, p.y));
        if (p.x < 98) neighbors.add(new Point(p.x + 1, p.y));
        if (p.y > 1) neighbors.add(new Point(p.x, p.y - 1));
        if (p.y < 98) neighbors.add(new Point(p.x, p.y + 1));
        return neighbors;
    }

    private static List<Point> reconstructPath(Node node) {
        List<Point> path = new ArrayList<>();
        while (node != null) {
            path.add(node.position);
            node = node.parent;
        }
        Collections.reverse(path);
        return path.subList(1, path.size()); // Remove the start position
    }
}

// -------------------- Node Class for A* --------------------
class Node {
    Point position;
    Node parent;
    int g, f;

    public Node(Point position, Node parent, int g, int h) {
        this.position = position;
        this.parent = parent;
        this.g = g;
        this.f = g + h;
    }
}


