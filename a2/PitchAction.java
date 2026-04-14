package a2;

import tage.GameObject;
import tage.input.action.AbstractInputAction;
import net.java.games.input.Event;

public class PitchAction extends AbstractInputAction {
    private MyGame game;
    private GameObject av;
    private float directionScale;

    public PitchAction(MyGame g, float d) {
        game = g;
        directionScale = d;
    }

    @Override
    public void performAction(float time, Event e) {
        float keyValue = e.getValue();
        if (keyValue > -.2 && keyValue < .2) return; // deadzone

        float angle = (2f * directionScale * keyValue * time);

        av = game.getAvatar();
        av.pitch(angle);
    }
}