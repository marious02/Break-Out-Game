import java.util.ArrayList;
import javafx.scene.paint.*;
import javafx.application.Platform;

// The model represents all the actual content and functionality of the app
// For Breakout, it manages all the game objects that the View needs
// (the bat, ball, bricks, and the score), provides methods to allow the Controller
// to move the bat (and a couple of other fucntions - change the speed or stop 
// the game), and runs a background process (a 'thread') that moves the ball 
// every 20 milliseconds and checks for collisions 
public class Model 
{
    // First,a collection of useful values for calculating sizes and layouts etc.

    private int B              = 6;      // Border round the edge of the panel
    private int M              = 40;     // Height of menu bar space at the top

    private   int BALL_SIZE      = 30;     // Ball side
    private int BRICK_WIDTH    = 50;     // Brick size
    private int BRICK_HEIGHT   = 30;

    protected int BAT_MOVE       = 5;      // Distance to move bat on each keypress
    protected int BALL_MOVE      = 3;      // Units to move the ball on each step

    protected int HIT_BRICK      = 50;     // Score for hitting a brick
    protected int HIT_BOTTOM     = -100;   // Score (penalty) for hitting the bottom of the screen

    // The other parts of the model-view-controller setup
    View view;
    Controller controller;

    // The game 'model' - these represent the state of the game
    // and are used by the View to display it
    protected GameObj ball;                // The ball
    protected ArrayList<GameObj> bricks;   // The bricks
    protected GameObj bat;                 // The bat
    private int score = 0;               // The score
    private int highscore;                  //The Highscore
    // variables that control the game 
    private boolean gameRunning = true;  // Set false to stop the game
    private boolean fast = false;        // Set true to make the ball go faster

    // initialisation parameters for the model
    protected int width;                   // Width of game
    protected int height;                  // Height of game

    // CONSTRUCTOR - needs to know how big the window will be
    public Model( int w, int h )
    {
        Debug.trace("Model::<constructor>");  
        width = w; 
        height = h;

        initialiseGame();
    }
    /**Code uses nested loops to make multiple bricks at the same point so each ball hit gives more
       score*/
    // Initialise the game - reset the score and create the game objects 
    public void initialiseGame()
      {
        score = 0;
        ball   = new GameObj(width/2, height/2, BALL_SIZE, BALL_SIZE, Color.RED );
        bat    = new GameObj(width/2, height - BRICK_HEIGHT*3/2, BRICK_WIDTH*4, BRICK_HEIGHT/4, Color.GRAY);    
        int NUM_BRICKS = width/BRICK_WIDTH; // how many bricks fit on screen
        int WALL_TOP = 100;                // how far down the screen the wall starts
        bricks = new ArrayList<>();
        //*[1]******************************************************[1]*
       for (int j=0; j < 4; j++) {
         for (int i=0; i < NUM_BRICKS; i++) {
          GameObj brick3 = new GameObj(BRICK_WIDTH*i,WALL_TOP + BRICK_HEIGHT , BRICK_WIDTH, BRICK_HEIGHT, Color.PURPLE);
            bricks.add(brick3); 
          }
        }
       for (int j=0; j < 3; j++) {
         for (int i=0; i < NUM_BRICKS; i++){
          GameObj brick2 = new GameObj(BRICK_WIDTH*i,WALL_TOP + BRICK_HEIGHT*2, BRICK_WIDTH, BRICK_HEIGHT, Color.GREEN);
            bricks.add(brick2);
         }
       }
       for (int j=0; j<2; j++) {
         for (int i=0; i < NUM_BRICKS; i++) {
            GameObj brick1 = new GameObj(BRICK_WIDTH*i, WALL_TOP + BRICK_HEIGHT * 3 , BRICK_WIDTH, BRICK_HEIGHT, Color.BLUE);
            bricks.add(brick1);      // add this brick to the list of bricks
         }
       }
       for (int i=0; i < NUM_BRICKS; i++) {
            GameObj brick = new GameObj(BRICK_WIDTH*i, WALL_TOP + BRICK_HEIGHT * 4 , BRICK_WIDTH, BRICK_HEIGHT, Color.PINK);
            bricks.add(brick);      // add this brick to the list of bricks
       }
    }
    // Animating the game
    // The game is animated by using a 'thread'. Threads allow the program to do 
    // two (or more) things at the same time. In this case the main program is
    // doing the usual thing (View waits for input, sends it to Controller,
    // Controller sends to Model, Model updates), but a second thread runs in 
    // a loop, updating the position of the ball, checking if it hits anything
    // (and changing direction if it does) and then telling the View the Model 
    // changed.
    
    // When we use more than one thread, we have to take care that they don't
    // interfere with each other (for example, one thread changing the value of 
    // a variable at the same time the other is reading it). We do this by 
    // SYNCHRONIZING methods. For any object, only one synchronized method can
    // be running at a time - if another thread tries to run the same or another
    // synchronized method on the same object, it will stop and wait for the
    // first one to finish.
    
    // Start the animation thread
    public void startGame()
    {
        
        Thread t = new Thread( this::runGame );     // create a thread runnng the runGame method
        t.setDaemon(true);                          // Tell system this thread can die when it finishes
        t.start();                                  // Start the thread running
    }   
    
    // The main animation loop
    /**Main animation loop (updates the games state and refreshes the screen)*/
    public void runGame()
    {
        try
        {
            // set gameRunning true - game will stop if it is set false (eg from main thread)
            setGameRunning(true);
            while (getGameRunning())
            {
                updateGame();                        // update the game state
                modelChanged();                      // Model changed - refresh screen
                Thread.sleep( getFast() ? 10 : 20 ); // wait a few milliseconds
            }
        } catch (Exception e) 
        { 
            Debug.error("Model::runAsSeparateThread error: " + e.getMessage() );
        }
    }
  
    // updating the game - this happens about 50 times a second to give the impression of movement
    public synchronized void updateGame()
    {
        // move the ball one step (the ball knows which direction it is moving in)
        ball.moveX(BALL_MOVE);                      
        ball.moveY(BALL_MOVE);
        // get the current ball possition (top left corner)
        int x = ball.topX;  
        int y = ball.topY;
        // Deal with possible edge of board hit
        if (x >= width - B - BALL_SIZE)  ball.changeDirectionX();
        if (x <= 0 + B)  ball.changeDirectionX();
        if (y >= height - B - BALL_SIZE)  // Bottom
        { 
            ball.changeDirectionY(); 
            addToScore( HIT_BOTTOM );     // score penalty for hitting the bottom of the screen
        }
        if (y <= 0 + M)  ball.changeDirectionY();
        // check whether ball has hit a (visible) brick
        boolean hit = false;
        /**Loop below is to check for collisions between the ball and bricks and also makes the
           bricks invisible after a hit has been registered*/
        // *[3]******************************************************[3]*
        // Loop below is for the program to constantly check if the ball has hit a brick and tells it to add to the score variable.
         for (GameObj brick: bricks) {
            if (brick.visible && brick.hitBy(ball)) {
                hit = true;                 // Ball hits the brick.
                brick.visible = false;      // Make the brick invisible.
                addToScore( HIT_BRICK );    // Add to score for each brick hit.
                compHighscore();   //Compare Highscore to score and if Hscore is biger then return highscore.
                
            }
        }
        // *************************************************************
        if (hit)
            ball.changeDirectionY();

        // check whether ball has hit the bat
        if ( ball.hitBy(bat) )
            ball.changeDirectionY();
    }

    // This is how the Model talks to the View
    // Whenever the Model changes, this method calls the update method in
    // the View. It needs to run in the JavaFX event thread, and Platform.runLater 
    // is a utility that makes sure this happens even if called from the
    // runGame thread
    public synchronized void modelChanged()
    {
        Platform.runLater(view::update);
    }
    
    
    // Methods for accessing and updating values
    // these are all synchronized so that the can be called by the main thread 
    // or the animation thread safely
    
    // Change game running state - set to false to stop the game
    public synchronized void setGameRunning(Boolean value)
    {  
        gameRunning = value;
    }
    
    // Return game running state
    public synchronized Boolean getGameRunning()
    {  
        return gameRunning;
    }

    // Change game speed - false is normal speed, true is fast
    public synchronized void setFast(Boolean value)
    {  
        fast = value;
    }
    
    // Return game speed - false is normal speed, true is fast
    public synchronized Boolean getFast()
    {  
        return(fast);
    }
    // Return bat object
    public synchronized GameObj getBat()
    {
        return(bat);
    }
    
    // return ball object
    public synchronized GameObj getBall()
    {
        return(ball);
    }
    
    // return bricks
    public synchronized ArrayList<GameObj> getBricks()
    {
        return(bricks);
    }
    
    // return score
    public synchronized int getScore()
    {
        return(score);
    }
    //return Highscore 
    public synchronized int getHighscore()
    {
        return(highscore);
    }
    /**check if the score is higher than the highscore and if yes the highscore takes the value of
       score*/
    public synchronized void compHighscore()
    {
        if (score >= highscore )
        highscore = score;
        
    }
    // update the score
    public synchronized void addToScore(int n)    
    {
        score += n;        
    }
    
    // move the bat one step - -1 is left, +1 is right
    public synchronized void moveBat( int direction )
    {        
        int dist = direction * BAT_MOVE;    // Actual distance to move
        Debug.trace( "Model::moveBat: Move bat = " + dist );
        bat.moveX(dist);
    }
}   
    