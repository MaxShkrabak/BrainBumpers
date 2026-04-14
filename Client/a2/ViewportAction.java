package a2;

import net.java.games.input.Component;
import net.java.games.input.Event;
import org.joml.Vector3f;
import tage.Camera;
import tage.input.action.AbstractInputAction;

public class ViewportAction extends AbstractInputAction {
    private Camera cam;
    private float xDir, yDir, zDir;
    private final float MIN_HEIGHT = 1f;

    public ViewportAction(Camera c, float x, float y, float z) {
        cam = c;
        xDir = x;
        yDir = y;
        zDir = z;
    }

    @Override
    public void performAction(float time, Event e) {
        float keyValue = e.getValue();
        float speed = 25f * time;
        Vector3f loc = cam.getLocation();

        float currentX = xDir;
        float currentY = yDir;
        float currentZ = zDir;

        // gamepad
        if (e.getComponent().getIdentifier() == Component.Identifier.Axis.POV) {
            currentX = 0f;
            currentY = 0f;
            currentZ = 0f;

            if (keyValue == 0.25f) {
                currentZ = -1f; // up
            } else if (keyValue == 0.75f) {
                currentZ = 1f;  // down
            } else if (keyValue == 1.0f) {
                currentX = -1f; // left
            } else if (keyValue == 0.5f) {
                currentX = 1f;  // right
            } else {
                return;
            }
        }

        float newX = loc.x + (currentX * speed);
        float newY = loc.y + (currentY * speed);
        float newZ = loc.z + (currentZ * speed);

        // to stop zoom just above the ground
        newY = Math.max(newY, MIN_HEIGHT);

        cam.setLocation(new Vector3f(newX, newY, newZ));
    }
}

