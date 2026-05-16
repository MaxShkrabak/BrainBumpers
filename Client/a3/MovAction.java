package a2;

import tage.input.action.AbstractInputAction;
import net.java.games.input.Event;

public class MovAction extends AbstractInputAction {
    private MyGame game;
    private float direction;

    public MovAction(MyGame g, float d, ProtocolClient pc) {
        game = g;
        direction = d;
    }

    @Override
    public void performAction(float time, Event e) {
        if(game.isSpectating() || game.isGameOver()) return;
        float keyValue = e.getValue();
        if (keyValue > -.2 && keyValue < .2) return; // deadzone
        game.setThrottle(direction * keyValue);
    }
}


