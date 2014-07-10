
import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Paint;
import java.awt.Point;
import java.awt.Rectangle;
import java.awt.Stroke;
import java.awt.event.ComponentAdapter;
import java.awt.event.ComponentEvent;
import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.util.ArrayList;
import java.util.List;
import javax.swing.JFrame;
import javax.swing.JPanel;

// TODO make resize changes also take effect when maximize/minimize window
/**
 *
 * @author bob
 */
public class Board extends JPanel implements Runnable {

    // board size contants
    private static final int OUTER_SPACE_SIZE = 20;
    private static final int OUTER_BORDER_SIZE = 2;
    private static final int SQUARE_BORDER_SIZE = 1;
    private static final int SQUARE_SIZE = 15;

    // board color contants
    private static final Color BACKGROUND_COLOR = Color.WHITE;
    private static final Color OUTER_BORDER_COLOR = Color.BLACK;
    private static final Color SQUARE_BORDER_COLOR = Color.GRAY;
    private static final Color EMPTY_SQUARE_COLOR = Color.WHITE;
    private static final Color FILLED_SQUARE_COLOR = Color.BLACK;
    
    // input key constants
    private static final int PAUSE_KEY_CODE = KeyEvent.VK_P;

    // miscellaneous constants
    private static final int TICK_TIME = 1000;

    // variables
    private int numSquaresHorizontal;
    private int numSquaresVertical;
    private SquareState[][] boardState;
    private Rectangle[][] boardRectangles;
    private boolean paused;

    public Board() {
        setFocusable(true);
        setBackground(BACKGROUND_COLOR);
        this.setDoubleBuffered(true);
        this.addComponentListener(new ResizeListener());
        this.addMouseListener(new MouseInputHandler(this));
        this.addKeyListener(new KeyboardInputHandler(this));
    }

    @Override
    public void paint(Graphics g) {
        super.paint(g);

        Graphics2D g2d = (Graphics2D) g;

        drawOuterBorder(g2d);
        drawInternalBorders(g2d);
        drawBoardState(g2d);
    }

    private void drawOuterBorder(Graphics2D g2d) {

        // set up g2d for this drawing
        Stroke oldStroke = g2d.getStroke();
        g2d.setStroke(new BasicStroke(OUTER_BORDER_SIZE));
        Paint oldPaint = g2d.getPaint();
        g2d.setPaint(OUTER_BORDER_COLOR);

        // origin of border rectangle is in top left corner inset by the outer space, and half the
        // outer border as the stroke thickness is half on each side when drawing
        int rectX = OUTER_SPACE_SIZE + OUTER_BORDER_SIZE / 2;
        int rectY = rectX;

        // width and height calculated as sum of all things which need to be drawn inside the outer
        // border - TODO refactor to function?
        int rectWidth = OUTER_BORDER_SIZE + numSquaresHorizontal * (2 * SQUARE_BORDER_SIZE
                + SQUARE_SIZE);

        int rectHeight = OUTER_BORDER_SIZE + numSquaresVertical * (2 * SQUARE_BORDER_SIZE
                + SQUARE_SIZE);

        g2d.drawRect(rectX, rectY, rectWidth, rectHeight);

        // set g2d back to previous properties
        g2d.setStroke(oldStroke);
        g2d.setPaint(oldPaint);
    }

    /**
     * Draw the internal borders between the squares of the grid. Done by just filling in the outer
     * border and then the squares can be drawn over this.
     *
     * @param g2d
     */
    private void drawInternalBorders(Graphics2D g2d) {

        // set up g2d for this drawing
        Stroke oldStroke = g2d.getStroke();
        g2d.setStroke(new BasicStroke(SQUARE_BORDER_SIZE));
        Paint oldPaint = g2d.getPaint();
        g2d.setPaint(SQUARE_BORDER_COLOR);

        int rectX = OUTER_SPACE_SIZE + OUTER_BORDER_SIZE;
        int rectY = rectX;

        int spacePerSquare = 2 * SQUARE_BORDER_SIZE + SQUARE_SIZE;
        int rectWidth = numSquaresHorizontal * spacePerSquare;
        int rectHeight = numSquaresVertical * spacePerSquare;

        g2d.fillRect(rectX, rectY, rectWidth, rectHeight);

        // set g2d back to previous properties
        g2d.setStroke(oldStroke);
        g2d.setPaint(oldPaint);
    }

    private void drawBoardState(Graphics2D g2d) {

        // set up g2d for this drawing
        Stroke oldStroke = g2d.getStroke();
        g2d.setStroke(new BasicStroke(1));
        Paint oldPaint = g2d.getPaint();

        // iterate through all squares in the board state, drawing the corresponding rectangle in 
        // the appropriate place and color depending on if square empty or filled
        for (int x = 0; x < numSquaresHorizontal; x++) {
            for (int y = 0; y < numSquaresVertical; y++) {

                // assign color to use depending on square status
                if (boardState[x][y] == SquareState.DEAD) {
                    g2d.setPaint(EMPTY_SQUARE_COLOR);
                } else if (boardState[x][y] == SquareState.ALIVE) {
                    g2d.setPaint(FILLED_SQUARE_COLOR);
                }

                // draw corresponding rect
                g2d.fill(boardRectangles[x][y]);
            }
        }

        // set g2d back to previous properties
        g2d.setStroke(oldStroke);
        g2d.setPaint(oldPaint);
    }

    /**
     * Calculate the coordinates of the rectangle for every square on the board.
     */
    private void calculateSquareRectangles() {

        // initialize array to fit rectangles for current number of squares
        boardRectangles = new Rectangle[numSquaresHorizontal][numSquaresVertical];

        // the spaces before and between the rectangles
        int totalBorderBeforeFirstRect = OUTER_SPACE_SIZE + OUTER_BORDER_SIZE + SQUARE_BORDER_SIZE;
        int spaceBetweenRects = 2 * SQUARE_BORDER_SIZE + SQUARE_SIZE;

        // iterate through all squares of the board assigning a rectangle in the appropriate place
        for (int x = 0; x < numSquaresHorizontal; x++) {
            for (int y = 0; y < numSquaresVertical; y++) {

                int rectX = totalBorderBeforeFirstRect + x * spaceBetweenRects;
                int rectY = totalBorderBeforeFirstRect + y * spaceBetweenRects;
                boardRectangles[x][y] = new Rectangle(rectX, rectY, SQUARE_SIZE, SQUARE_SIZE);
            }
        }

    }

    private void calculateNumSquaresHorizontal() {
        int panelWidth = this.getWidth();
        numSquaresHorizontal = calculateNumSquaresInDimension(panelWidth);
    }

    private void calculateNumSquaresVertical() {
        int panelHeight = this.getHeight();
        numSquaresVertical = calculateNumSquaresInDimension(panelHeight);
    }

    private int calculateNumSquaresInDimension(int dimSize) {

        // calculate the number of squares as the largest number which will fit on the board with 
        // the current size with the given parameters
        int totalOuterSpace = 2 * OUTER_SPACE_SIZE;
        int totalOuterBorder = 2 * OUTER_BORDER_SIZE;
        int numSquares = (dimSize - (totalOuterSpace + totalOuterBorder))
                / (2 * SQUARE_BORDER_SIZE + SQUARE_SIZE);

        return numSquares;
    }

    private void tick() {

        // initialize the next state of the board after this tick
        SquareState[][] newBoardState = new SquareState[numSquaresHorizontal][numSquaresVertical];

        // for each square in the current board state calculate its next state
        for (int x = 0; x < numSquaresHorizontal; x++) {
            for (int y = 0; y < numSquaresVertical; y++) {

                boolean isAlive = boardState[x][y] == SquareState.ALIVE;

                List<SquareState> neighbourStates = getNeighbourStates(x, y);
                int numAliveNeighbours = 0;
                for (SquareState ss : neighbourStates) {
                    if (ss == SquareState.ALIVE) {
                        numAliveNeighbours++;
                    }
                }

                // decide square's new state
                SquareState newSqState;
                if (isAlive) {
                    // if square alive and has 2 or 3 neighbours then stay alive
                    if (numAliveNeighbours == 2 || numAliveNeighbours == 3) {
                        newSqState = SquareState.ALIVE;
                    } // otherwise die as if by underpopulation or overcrowding
                    else {
                        newSqState = SquareState.DEAD;
                    }
                } else {
                    // if square dead and exactly three neighbours then come alive as if by reproduction
                    if (numAliveNeighbours == 3) {
                        newSqState = SquareState.ALIVE;
                    } // otherwise stay dead
                    else {
                        newSqState = SquareState.DEAD;
                    }
                }
                
                newBoardState[x][y] = newSqState;
            }
        }
        
        // set the overall board state to calculated new state
        boardState = newBoardState;
    }

    /**
     * Get a set of the square states of the neighbouring squares to that given by the given
     * coordinates (neighbouring squares are those horizontally, vertically, or diagonally adjacent
     * to the given square).
     *
     * The set will have length 8 for an internal square, length 5 for a side square, and length 3
     * for a corner square.
     *
     * TODO improve algorithm? currently does not seem best way
     *
     * @param squareX
     * @param squareY
     * @return
     */
    private List<SquareState> getNeighbourStates(int squareX, int squareY) {

        // calculate whether the square is at various edge positions
        boolean atLeft = squareX == 0;
        boolean atRight = squareX == numSquaresHorizontal - 1;
        boolean atTop = squareY == 0;
        boolean atBottom = squareY == numSquaresVertical - 1;

        // go through adding the neighbours states where they exist
        List<SquareState> neighbourStates = new ArrayList<>();
        
        // if not at left
        if (!atLeft) {
            // add left state
            SquareState leftState = boardState[squareX - 1][squareY];
            neighbourStates.add(leftState);
            
            // if not at top add top-left state
            if (!atTop) {
                SquareState topLeftState = boardState[squareX - 1][squareY - 1];
                neighbourStates.add(topLeftState);
            }
            
            // if not at bottom add bottom-left state
            if (!atBottom) {
                SquareState bottomLeftState = boardState[squareX - 1][squareY + 1];
                neighbourStates.add(bottomLeftState);
            }
        }
            
        // if not at right
        if (!atRight) {
            // add right state
            SquareState rightState = boardState[squareX + 1][squareY];
            neighbourStates.add(rightState);
            
            // if not at top add top-right state
            if (!atTop) {
                SquareState topRightState = boardState[squareX + 1][squareY - 1];
                neighbourStates.add(topRightState);
            }
            
            // if not at bottom add bottom-right state
            if (!atBottom) {
                SquareState bottomRightState = boardState[squareX + 1][squareY + 1];
                neighbourStates.add(bottomRightState);
            }
        }
        
        // if not at top at top state
        if (!atTop) {
            SquareState topState = boardState[squareX][squareY - 1];
            neighbourStates.add(topState);
        }
        
        // if not at bottom add bottom state
        if (!atBottom) {
            SquareState bottomState = boardState[squareX][squareY + 1];
            neighbourStates.add(bottomState);
        }

        return neighbourStates;
    }

    @Override
    public void run() {

        long previousTime;
        long timeDiff;
        long sleep;

        previousTime = System.currentTimeMillis();

        while (true) {
            
            // if paused continue
            if (this.isPaused()) {
                System.out.println("paused");
                continue;
            }

            tick();
            repaint();

            // calculate time this cycle has been running and so time to sleep for
            timeDiff = System.currentTimeMillis() - previousTime;
            sleep = TICK_TIME - timeDiff;

            // time cycle runs each time will always be DELAY as account for different drawing 
            // and calculating times
            if (sleep < 0) {
                sleep = 2;
            }

            try {
                Thread.sleep(sleep);
            } catch (InterruptedException ex) {
                System.out.println("Interrupted: " + ex.getMessage());
            }

            previousTime = System.currentTimeMillis();
        }
    }

    /**
     * @return the paused
     */
    private boolean isPaused() {
        return paused;
    }

    /**
     * @param paused the paused to set
     */
    private void setPaused(boolean paused) {
        this.paused = paused;
    }

    private enum SquareState {

        ALIVE, DEAD
    }

    private class MouseInputHandler extends MouseAdapter {

        private final JPanel panel;

        public MouseInputHandler(JPanel panel) {
            this.panel = panel;
        }

        @Override
        /**
         * When mouse is clicked identify the square clicked and flip its state.
         */
        public void mouseClicked(MouseEvent event) {

            Point clickLocation = event.getPoint();

            // iterate through all rectangles on the board to see if one contains the click location
            findRect:
            for (int x = 0; x < numSquaresHorizontal; x++) {
                for (int y = 0; y < numSquaresVertical; y++) {

                    Rectangle currentRect = boardRectangles[x][y];

                    if (currentRect.contains(clickLocation)) {
                        // flip the corresponding state and stop searching as rect found

                        SquareState currentState = boardState[x][y];
                        if (currentState == SquareState.DEAD) {
                            boardState[x][y] = SquareState.ALIVE;
                        } else if (currentState == SquareState.ALIVE) {
                            boardState[x][y] = SquareState.DEAD;
                        }

                        break findRect;
                    }
                }
            }

            // refresh the panel so change takes effect immediately
            panel.repaint();
        }
    }
    
    private class KeyboardInputHandler extends KeyAdapter {
        
        private final Board board;

        public KeyboardInputHandler(Board board) {
            this.board = board;
        }
        
        @Override
        public void keyPressed(KeyEvent event) {
            
            int keyCode = event.getKeyCode();
            
            // pause/unpause game
            if (keyCode == PAUSE_KEY_CODE) {
                if (board.isPaused()) {
                    board.setPaused(false);
                }
                else {
                    board.setPaused(true);
                }                
            }
            
        }
        
    }

    private class ResizeListener extends ComponentAdapter {

        @Override
        public void componentResized(ComponentEvent e) {

            // recalculate num squares can fit in panel
            calculateNumSquaresHorizontal();
            calculateNumSquaresVertical();

            boardState = new SquareState[numSquaresHorizontal][numSquaresVertical];
            for (int x = 0; x < numSquaresHorizontal; x++) {
                for (int y = 0; y < numSquaresVertical; y++) {
                    boardState[x][y] = SquareState.DEAD;
                }
            }

            calculateSquareRectangles();
        }
    }

    public static void main(String[] args) {
        JFrame window = new JFrame();
        window.setSize(500, 500);
        window.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        window.setLocationRelativeTo(null);
        window.setTitle("Game of Life");
        Board board = new Board();
        window.add(board);
        new Thread(board).start();
        window.setVisible(true);
    }
}
