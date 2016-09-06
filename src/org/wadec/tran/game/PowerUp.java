package org.wadec.tran.game;

import java.awt.Point;

/**
 *
 * @author wadec
 */ 
public class PowerUp {
    
    public final Type type;
    public final int value;
    public final Point position;

    public PowerUp(Type t, int v, Point p) {
        type = t;
        value = v;
        position = p;
    }
    
    public int getDrawingX() {
        return position.x * Game.BUFF_SIZE + Game.FRM_OFFSET_X;
    }
    
    public int getDrawingY() {
        return position.y * Game.BUFF_SIZE + Game.FRM_OFFSET_Y;
    }
    
    public enum Type {
        POINTS, TRAIL
    }
}
