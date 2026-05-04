package a2;

import org.joml.Math;
import tage.GameObject;
import org.joml.*;

public class CarController {
    private GameObject avatar, frontLeftTire, frontRightTire;
    private ProtocolClient protClient;
    private boolean isMultiplayer;

    private float wheelAngle = 0f;
    private float currentSpeed = 0f;
    private float throttle = 0f;
    private boolean turnKeyHeld = false;

    private static final float MAX_WHEEL_ANGLE = (float) Math.toRadians(30.0);
    private static final float WHEEL_RETURN_SPEED = 3.0f;
    private static final float MAX_SPEED = 14f;
    private static final float ACCELERATION = 10f;
    private static final float DECELERATION = 14f;

    public CarController(GameObject avatar, GameObject frontLeftTire, GameObject frontRightTire, ProtocolClient protClient, boolean isMultiplayer) {
        this.avatar = avatar;
        this.frontLeftTire = frontLeftTire;
        this.frontRightTire = frontRightTire;
        this.protClient = protClient;
        this.isMultiplayer = isMultiplayer;
    }

    public void beginFrame() {
        turnKeyHeld = false;
        throttle = 0f;
    }

    public void setThrottle(float input) { throttle = input; }

    public void turn(float delta) {
        wheelAngle += delta;
        if (wheelAngle > MAX_WHEEL_ANGLE) wheelAngle = MAX_WHEEL_ANGLE;
        if (wheelAngle < -MAX_WHEEL_ANGLE) wheelAngle = -MAX_WHEEL_ANGLE;
        turnKeyHeld = true;
        updateFrontTires();
    }

    public void update(float dt) {
        // speeding up and slowing down
        if (throttle != 0f) {
            float target = MAX_SPEED * throttle;
            float diff = target - currentSpeed;
            float step = ACCELERATION * dt;
            if (Math.abs(diff) <= step) currentSpeed = target;
            else currentSpeed += Math.signum(diff) * step;
        } else {
            float step = DECELERATION * dt;
            if (Math.abs(currentSpeed) <= step) currentSpeed = 0f;
            else currentSpeed -= Math.signum(currentSpeed) * step;
        }

        if (currentSpeed != 0f) {
            // turning is less sharp at high speed
            float tightness = 1f - (Math.abs(currentSpeed) / MAX_SPEED) * 0.7f;
            float turnAmount = currentSpeed * (float) org.joml.Math.sin(wheelAngle * tightness) * dt;
            if (turnAmount != 0f) {
                avatar.globalYaw(turnAmount);
                if (isMultiplayer && protClient != null) protClient.sendTurnMessage(turnAmount);
            }

            Vector4f fwd = new Vector4f(0f, 0f, 1f, 1f);
            fwd.mul(avatar.getWorldRotation());
            fwd.mul(currentSpeed * dt);
            Vector3f newPos = avatar.getWorldLocation().add(fwd.x(), fwd.y(), fwd.z());
            avatar.setLocalLocation(newPos);
            if (isMultiplayer && protClient != null) protClient.sendMoveMessage(avatar.getWorldLocation());
        }

        // wheels will return to center when not actively turning
        if (!turnKeyHeld && wheelAngle != 0f) {
            float step = WHEEL_RETURN_SPEED * dt;
            if (Math.abs(wheelAngle) <= step) wheelAngle = 0f;
            else wheelAngle -= Math.signum(wheelAngle) * step;
            updateFrontTires();
        }
    }

    private void updateFrontTires() {
        rotateTire(frontLeftTire);
        rotateTire(frontRightTire);
    }

    private void rotateTire(GameObject tire) {
        if (tire == null) return;
        Vector3f t = new Vector3f();
        tire.getLocalTranslation().getTranslation(t);
        Vector3f s = tire.getLocalScale().getScale(new Vector3f());
        Matrix4f rot = new Matrix4f().rotationY(wheelAngle);
        tire.setLocalRotation(rot);
        tire.setLocalTranslation(new Matrix4f().translation(t));
        tire.setLocalScale(new Matrix4f().scaling(s));
    }

    public float getCurrentSpeed() { return currentSpeed; }
    public float getWheelAngle() { return wheelAngle; }
    public float getMaxWheelAngle() { return MAX_WHEEL_ANGLE; }
}
