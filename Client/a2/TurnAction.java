package a2;

import tage.GameObject;
import tage.input.action.AbstractInputAction;
import net.java.games.input.Event;

public class TurnAction extends AbstractInputAction {
    private MyGame game;
    private GameObject av;
    private float direction;
    private ProtocolClient protClient;

    private static final float TURN_SPEED = 2.0f;

    public TurnAction(MyGame g, float d, ProtocolClient pc) {
        game = g;
        direction = d;
        protClient = pc;
    }

    @Override
    public void performAction(float time, Event e) {
        float keyValue = e.getValue();
        if (keyValue > -.2 && keyValue < .2) return; // deadzone

        float delta = TURN_SPEED * direction * keyValue * time;
        game.turn(delta);
    }
}

