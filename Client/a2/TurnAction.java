package a2;

import tage.GameObject;
import tage.input.action.AbstractInputAction;
import net.java.games.input.Event;

public class TurnAction extends AbstractInputAction {
    private MyGame game;
    private GameObject av;
    private float direction;
    private ProtocolClient protClient;

    public TurnAction(MyGame g, float d, ProtocolClient pc) {
        game = g;
        direction = d;
        protClient = pc;
    }

    @Override
    public void performAction(float time, Event e) {
        float keyValue = e.getValue();
        if (keyValue > -.2 && keyValue < .2) return; // deadzone

        float angle = (3f * direction * keyValue * time);

        av = game.getAvatar();
        av.globalYaw(angle);

        protClient.sendTurnMessage(angle);

        //if (!game.isMovSafe(av.getWorldLocation())) {
        //    av.globalYaw(-angle);
        //}
    }
}

