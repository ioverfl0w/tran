package org.wadec.tran.game;

/**
 *
 * @author wadec
 */
public class Profile {

    public final Cart cart;
    public final String name;
    public boolean isDead = false;

    public Profile(String n, Cart c) {
        name = n;
        cart = c;
    }

    @Override
    public String toString() {
        return "Name:" + name;
    }
}
