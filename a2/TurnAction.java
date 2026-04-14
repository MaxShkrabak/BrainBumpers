package a2;

import tage.GameObject;
import tage.input.action.AbstractInputAction;
import net.java.games.input.Event;

public class TurnAction extends AbstractInputAction {
    private MyGame game;
    private GameObject av;
    private float direction;

    public TurnAction(MyGame g, float d) {
        game = g;
        direction = d;
    }

    @Override
    public void performAction(float time, Event e) {
        float keyValue = e.getValue();
        if (keyValue > -.2 && keyValue < .2) return; // deadzone

        float angle = (3f * direction * keyValue * time);

        av = game.getAvatar();
        av.globalYaw(angle);

        //if (!game.isMovSafe(av.getWorldLocation())) {
        //    av.globalYaw(-angle);
        //}
    }
}