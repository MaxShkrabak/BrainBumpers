package a2;

import tage.Camera;
import tage.GameObject;
import tage.input.action.AbstractInputAction;
import net.java.games.input.Event;
import org.joml.*;

public class MovAction extends AbstractInputAction {
    private MyGame game;
    private Camera c;
    private GameObject av;
    private Vector3f oldPosition, newPosition, cfwdDirection;
    private Vector4f fwdDirection;
    private float direction;

    public MovAction(MyGame g, float d) {
        game = g;
        direction = d;
    }

    @Override
    public void performAction(float time, Event e) {
        float keyValue = e.getValue();
        if (keyValue > -.2 && keyValue < .2) return; // deadzone

        av = game.getAvatar();
        oldPosition = av.getWorldLocation();
        fwdDirection = new Vector4f(0f, 0f, 1f, 1f);
        fwdDirection.mul(av.getWorldRotation());
        fwdDirection.mul(8f * direction * keyValue * time);
        newPosition = oldPosition.add(fwdDirection.x(),
                fwdDirection.y(), fwdDirection.z());

        if (game.isMovSafe(newPosition)) {
            av.setLocalLocation(newPosition);
        }
    }
}
