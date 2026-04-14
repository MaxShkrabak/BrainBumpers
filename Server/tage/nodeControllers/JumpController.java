package tage.nodeControllers;

import org.joml.Math;
import tage.*;
import org.joml.*;

/**
 * A JumpController is a node controller that, when enabled, causes any object
 * it is attached to, to bounce up and down.
 */
public class JumpController extends NodeController {
    private float bounceHeight = 2.0f;
    private float bounceSpeed = 3.0f;
    private float totalTime = 0.0f;
    private float baseY;
    private boolean baseYSet = false;
    private Engine engine;

    /** Creates a jump controller with bounce height = 2.0f and speed = 3.0f. */
    public JumpController(Engine e) {
        super();
        engine = e;
    }

    /** This is called automatically by the RenderSystem (via SceneGraph) once per frame
     *   during display().  It is for engine use and should not be called by the application.
     */
    public void apply(GameObject go) {
        float elapsedTime = super.getElapsedTime();
        totalTime += elapsedTime / 1000.0f;

        // if not checked the object will fly away
        if (!baseYSet) {
            baseY = go.getWorldLocation().y();
            baseYSet = true;
        }

        float bounce = Math.abs(Math.sin(totalTime * bounceSpeed));

        Vector3f loc = go.getWorldLocation();
        go.setLocalLocation(new Vector3f(loc.x(), baseY + bounce * bounceHeight, loc.z()));
    }
}
