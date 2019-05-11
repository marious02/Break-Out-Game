import javafx.scene.paint.*;

// An object in the game, represented as a rectangle, with a position,
// a colour, and a direction of movement.
public class GameObj
{
    // state variables for a game object
    protected boolean visible  = true;     // Can see (change to false when the brick gets hit)
    protected int topX   = 0;              // Position - top left corner X
    protected int topY   = 0;              // position - top left corner Y
    protected int width  = 0;              // Width of object
    protected int height = 0;              // Height of object
    protected Color colour;                // Colour of object
    protected int   dirX   = 1;            // Direction X (1 or -1)
    protected int   dirY   = 1;            // Direction Y (1 or -1)


    public GameObj( int x, int y, int w, int h, Color c )
    {
        topX   = x;       
        topY = y;
        width  = w; 
        height = h; 
        colour = c;
    }

    // move in x axis
    public void moveX( int units )
    {
        topX += units * dirX;
    }

    // move in y axis
    public void moveY( int units )
    {
        topY += units * dirY;
    }

    // change direction of movement in x axis (-1, 0 or +1)
    public void changeDirectionX()
    {
        dirX = -dirX;
    }

    // change direction of movement in y axis (-1, 0 or +1)
    public void changeDirectionY()
    {
        dirY = -dirY;
    }

    // Detect collision between this object and the argument object
    // It's easiest to work out if they do NOT overlap, and then
    // return the negative (with the ! at the beginning)
    public boolean hitBy( GameObj obj )
    {
        return ! ( topX >= obj.topX+obj.width     ||
            topX+width <= obj.topX         ||
            topY >= obj.topY+obj.height    ||
            topY+height <= obj.topY );

    }

}
