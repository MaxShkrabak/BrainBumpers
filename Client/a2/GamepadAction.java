package a2;

import net.java.games.input.Event;
import tage.input.action.AbstractInputAction;

public class GamepadAction extends AbstractInputAction {
    private MyGame game;
    private char button;

    public GamepadAction(MyGame g, char c) {
        game = g;
        button = c;
    }

    @Override
    public void performAction(float time, Event e) {

        if (button == 'x') {
            game.toggleAxes();
        }
    }
}



