package tage;

import net.java.games.input.Component;
import org.joml.Math;
import tage.input.InputManager;
import tage.input.action.AbstractInputAction;
import net.java.games.input.Event;
import org.joml.*;

/**
 * Controls the camera around a target GameObject (avatar).
 * Supports azimuth rotation, elevation, and zoom via gamepad or mouse input.
 */
public class CameraOrbit3D {
    private Engine engine;
    private Camera camera; // the camera being controlled
    private GameObject avatar; // the target avatar the camera looks at
    private float cameraAzimuth; // rotation around target Y axis
    private float cameraElevation; // elevation of camera above target
    private float cameraRadius; // distance between camera and target

    public CameraOrbit3D(Camera cam, GameObject av,
                         String gpName, Engine e) {
        engine = e;
        camera = cam;
        avatar = av;
        cameraAzimuth = 0.0f; // start BEHIND and ABOVE the target
        cameraElevation = 20.0f; // elevation is in degrees
        cameraRadius = 2.0f; // distance from camera to avatar
        setupInputs(gpName);
        updateCameraPosition();
    }

    /**
     * Binds gamepad buttons to allow camera orbit around the avatar.
     */
    private void setupInputs(String gp) {
        OrbitAzimuthAction azmAction = new OrbitAzimuthAction();
        OrbitRadiusAction radAction = new OrbitRadiusAction();
        OrbitElevationAction elevAction = new OrbitElevationAction();
        InputManager im = engine.getInputManager();


        if (gp != null) {
            im.associateAction(gp,
                    Component.Identifier.Axis.RX, azmAction,
                    InputManager.INPUT_ACTION_TYPE.REPEAT_WHILE_DOWN);
            im.associateAction(gp,
                    Component.Identifier.Button._4, radAction,
                    InputManager.INPUT_ACTION_TYPE.REPEAT_WHILE_DOWN);
            im.associateAction(gp,
                    Component.Identifier.Button._5, radAction,
                    InputManager.INPUT_ACTION_TYPE.REPEAT_WHILE_DOWN);
            im.associateAction(gp,
                    Component.Identifier.Axis.RY, elevAction,
                    InputManager.INPUT_ACTION_TYPE.REPEAT_WHILE_DOWN);
        }
    }

    /**
     * Compute the camera’s azimuth, elevation, and distance, relative to
     * the target in spherical coordinates, then convert to world Cartesian
     * coordinates and set the camera position from that.
     */
    public void updateCameraPosition() {
        Vector3f avatarRot = avatar.getWorldForwardVector();
        double avatarAngle = Math.toDegrees((double)
                avatarRot.angleSigned(new Vector3f(0, 0, -1), new Vector3f(0, 1, 0)));
        float totalAz = cameraAzimuth - (float) avatarAngle;
        double theta = Math.toRadians(totalAz);
        double phi = Math.toRadians(cameraElevation);
        float x = cameraRadius * (float) (Math.cos(phi) * Math.sin(theta));
        float y = Math.max(0f,cameraRadius * (float) (Math.sin(phi)));
        float z = cameraRadius * (float) (Math.cos(phi) * Math.cos(theta));

        camera.setLocation(new
                Vector3f(x, y, z).add(avatar.getWorldLocation()));

        camera.lookAt(avatar);
    }

    /**
     * Rotates the camera horizontally around the avatar.
     */
    private class OrbitAzimuthAction extends AbstractInputAction {
        public void performAction(float time, Event event) {
            float rotSpeed = 125f;
            float rotAmount;

            if (event.getValue() < -0.2) {
                rotAmount = rotSpeed * time;
            } else {
                if (event.getValue() > 0.2) {
                    rotAmount = -rotSpeed * time;
                } else {
                    rotAmount = 0.0f;
                }
            }
            cameraAzimuth += rotAmount;
            cameraAzimuth = cameraAzimuth % 360;
            updateCameraPosition();
        }
    }

    /**
     * Zooms the camera in or out by adjusting the orbit radius.
     */
    private class OrbitRadiusAction extends AbstractInputAction {
        public void performAction(float time, Event event) {
            float zoomSpeed = 35f;
            float zoomAmount;

            Component.Identifier button = event.getComponent().getIdentifier();

            // left bumper _4, right bumper _5
            if (button.equals(Component.Identifier.Button._4)) {
                zoomAmount = zoomSpeed * time;
            } else if (button.equals(Component.Identifier.Button._5)) {
                zoomAmount = -zoomSpeed * time;
            } else {
                zoomAmount = 0.0f;
            }

            cameraRadius += zoomAmount;
            if (cameraRadius < 0.5f) cameraRadius = 0.5f;
            updateCameraPosition();
        }
    }

    /**
     * Adjusts vertical elevation of the camera above the avatar.
     */
    private class OrbitElevationAction extends AbstractInputAction {
        public void performAction(float time, Event event) {
            float elevSpeed = 85f;
            float elevationAmount;

            if (event.getValue() < -0.2) {
                elevationAmount = -elevSpeed * time;
            } else {
                if (event.getValue() > 0.2) {
                    elevationAmount = elevSpeed * time;
                } else {
                    elevationAmount = 0.0f;
                }
            }
            cameraElevation += elevationAmount;
            if (cameraElevation < 0.0f) cameraElevation = 0.0f;
            if (cameraElevation > 89.0f) cameraElevation = 89.0f;
            updateCameraPosition();
        }
    }

    /**
     * Rotates the camera horizontally around the avatar.
     * Helper for mouse controls.
     */
    public void updateAzimuth(float delta, float time) {
        cameraAzimuth += delta * time;
        cameraAzimuth = cameraAzimuth % 360;
        updateCameraPosition();
    }

    /**
     * Adjusts vertical elevation of the camera above the avatar.
     * Helper for mouse controls.
     */
    public void updateElevation(float delta, float time) {
        cameraElevation += delta * time;
        if (cameraElevation < 0.0f) cameraElevation = 0.0f;
        if (cameraElevation > 89.0f) cameraElevation = 89.0f;
        updateCameraPosition();
    }

    /**
     * Zooms the camera in or out by adjusting the orbit radius.
     * Helper for mouse controls.
     */
    public void updateRadius(float delta, float time) {
        float zoomSpeed = 50f;
        if (delta > 0) { // Zooming Out
            cameraRadius += cameraRadius * zoomSpeed * time;
        } else if (delta < 0) { // Zooming In
            cameraRadius -= cameraRadius * zoomSpeed * time;
        }
        updateCameraPosition();
    }
}

