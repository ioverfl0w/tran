package org.wadec.tran.ui;

import java.awt.Color;
import java.awt.Font;
import java.awt.Graphics;
import java.awt.Image;
import java.awt.Point;
import java.awt.event.KeyEvent;
import javax.swing.JFrame;
import org.wadec.tran.game.Game;
import org.wadec.tran.game.Game.Direction;
import org.wadec.tran.game.PowerUp;
import org.wadec.tran.game.Profile;

/**
 *
 * @author wadec
 */
public class GFrame extends JFrame {

    private Image buffer;
    private Graphics b;
    private final Game game;
    private int badMove = 0;
    private int countDown = 0;
    private final Font header = new Font(Font.SANS_SERIF, Font.BOLD, 28),
            normal = new Font(Font.SANS_SERIF, Font.PLAIN, 14),
            counter = new Font(Font.SANS_SERIF, Font.PLAIN, 48);
    private long messagePost = 0;
    private String message = null;

    public GFrame(Game g) {
        game = g;
        init();
        setVisible(true);
    }

    public void postMessage(String msg) {
        message = msg;
        messagePost = System.currentTimeMillis();
    }

    public void clearMessage() {
        message = null;
        messagePost = 0;
    }

    public void badMove() {
        badMove += 5;
    }

    public void countDown(int set) {
        countDown = set;
    }

    @Override
    public void paint(Graphics g) {
        try {
            buffer = createImage(getWidth(), getHeight());
            b = buffer.getGraphics();
            g.clearRect(0, 0, getWidth(), getHeight());

            b.setColor(Color.BLACK);
            b.setFont(header);
            b.drawString("tran", 150, Game.FRM_OFFSET_Y);
            b.setFont(normal);

            // draw game boundaries
            b.drawRect(Game.FRM_OFFSET_X, Game.FRM_OFFSET_Y, (Game.BOUNDS_X * 10) - 5, (Game.BOUNDS_Y * 10) - 5);

            // set up our stats and draw our players
            int count = 0;
            b.drawString("Scores", 510, 70);
            for (Profile player : game.getPlayers()) {
                b.setColor(player.cart.color);
                b.drawString(player.name + ": " + player.cart.score + (player.isDead ? "(dead)" : ""), 510, 100 + (count * 30));

                //draw the cart
                if (!player.isDead) {
                    // fill in the following trail
                    for (Point t : player.cart.getTrail()) {
                        if (t == null) {
                            break;
                        }
                        b.fillRect(Game.FRM_OFFSET_X + (t.x * Game.BUFF_SIZE) - Game.CART_SIZE,
                                Game.FRM_OFFSET_Y + (t.y * Game.BUFF_SIZE) - Game.CART_SIZE,
                                Game.CART_SIZE * 2,
                                Game.CART_SIZE * 2);
                    }
                    b.fillOval(Game.FRM_OFFSET_X + player.cart.getX() - Game.CART_SIZE - 2,
                            Game.FRM_OFFSET_Y + player.cart.getY() - Game.CART_SIZE - 2,
                            (Game.CART_SIZE * 2) + 4,
                            (Game.CART_SIZE * 2) + 4);

                    b.setColor(Color.BLACK);
                    b.drawOval(Game.FRM_OFFSET_X + player.cart.getX() - Game.CART_SIZE - 2,
                            Game.FRM_OFFSET_Y + player.cart.getY() - Game.CART_SIZE - 2,
                            (Game.CART_SIZE * 2) + 4,
                            (Game.CART_SIZE * 2) + 4);

                    b.drawString("(" + (player.cart.getX() / Game.BUFF_SIZE) + "," + (player.cart.getY() / Game.BUFF_SIZE) + ")", 600, 100 + (count * 30));
                } else if (player.name.equalsIgnoreCase(game.core.player.name)) {
                    b.setColor(Color.RED);
                    b.drawString("You died. You will respawn shortly...", 150, 150);
                }
                count++;
            }

            // draw all the power ups
            for (PowerUp power : game.getPowerups()) {
                b.setColor(power.type == PowerUp.Type.POINTS ? Color.GREEN : Color.magenta);
                b.fillOval(power.getDrawingX() - 8, power.getDrawingY() - 8, 16, 16);
                b.setColor(Color.BLACK);
                b.drawOval(power.getDrawingX() - 8, power.getDrawingY() - 8, 16, 16);
                b.drawString(power.value + "", power.getDrawingX() - 4, power.getDrawingY() + 5);
            }

            // show the countdown
            if (countDown > 0) {
                b.setColor(Color.red);
                b.setFont(counter);
                b.drawString("" + countDown, 250, 100);
            }

            // bad move indicator
            if (badMove > 0) {
                b.setColor(Color.red);
                b.setFont(normal);
                b.drawString("invalid move", 20, 40);
                badMove--;
            }

            // message indicator
            if (message != null) {
                if (System.currentTimeMillis() - messagePost >= Game.MESSAGE_TIME * 1000) {
                    clearMessage();
                } else {
                    b.setColor(Color.black);
                    b.drawString(message, 510, 100 + (count * 30));
                }
            }

            g.drawImage(buffer, 0, 0, this);
        } catch (Exception ex) {
            // oops
        }
    }

    private void init() {
        setDefaultCloseOperation(javax.swing.WindowConstants.EXIT_ON_CLOSE);
        setTitle("tran :: " + game.core.player.name + (game.core.engine.isHost() ? "(Host)" : ""));
        setResizable(false);
        setSize(700, 350);

        addKeyListener(new java.awt.event.KeyAdapter() {
            @Override
            public void keyPressed(java.awt.event.KeyEvent evt) {
                formKeyPressed(evt);
            }
        });
    }

    private void formKeyPressed(java.awt.event.KeyEvent evt) {
        int code = evt.getKeyCode();

        switch (code) {
            case KeyEvent.VK_UP:
                game.core.player.conn.send(2);
                game.core.player.conn.send(Direction.NORTH.ordinal());
                break;
            case KeyEvent.VK_RIGHT:
                game.core.player.conn.send(2);
                game.core.player.conn.send(Direction.EAST.ordinal());
                break;
            case KeyEvent.VK_DOWN:
                game.core.player.conn.send(2);
                game.core.player.conn.send(Direction.SOUTH.ordinal());
                break;
            case KeyEvent.VK_LEFT:
                game.core.player.conn.send(2);
                game.core.player.conn.send(Direction.WEST.ordinal());
                break;
            case KeyEvent.VK_Q:
                game.core.player.conn.send(4);
                break;
        }
    }
}
