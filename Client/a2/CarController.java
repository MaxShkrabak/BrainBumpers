package a2;

import org.joml.Math;
import tage.GameObject;
import org.joml.*;
import tage.physics.PhysicsObject;

public class CarController {
    private GameObject avatar, frontLeftTire, frontRightTire, backLeftTire, backRightTire;
    private ProtocolClient protClient;
    private boolean isMultiplayer;

    private float wheelAngle = 0f;
    private float rollAngle = 0f;
    private float currentSpeed = 0f;
    private float throttle = 0f;
    private boolean turnKeyHeld = false;
    private boolean skipSpeedSync = false;

    private static final float MAX_WHEEL_ANGLE = (float) Math.toRadians(30.0);
    private static final float WHEEL_RETURN_SPEED = 3.0f;
    private static final float MAX_SPEED = 14f;
    private static final float ACCELERATION = 10f;
    private static final float DECELERATION = 14f;
    private static final float WHEEL_RADIUS = 0.6f;

    public CarController(GameObject avatar, GameObject frontLeftTire, GameObject frontRightTire, GameObject backLeftTire, GameObject backRightTire, ProtocolClient protClient, boolean isMultiplayer) {
        this.avatar = avatar;
        this.frontLeftTire = frontLeftTire;
        this.frontRightTire = frontRightTire;
        this.backLeftTire = backLeftTire;
        this.backRightTire = backRightTire;
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
        updateAllTires();
    }

    public void update(float dt, PhysicsObject physicsObj, GameObject terrain) {
        boolean isGrounded = avatar.getWorldLocation().y() <= terrain.getHeight(avatar.getWorldLocation().x(), avatar.getWorldLocation().z()) + 1.2f;

        if (physicsObj != null && isGrounded && !skipSpeedSync) {
            float[] vel = physicsObj.getLinearVelocity();
            float actualXZSpeed = Math.sqrt(vel[0] * vel[0] + vel[2] * vel[2]);
            if (Math.abs(currentSpeed) - actualXZSpeed > 1.0f) {
                currentSpeed = Math.signum(currentSpeed) * actualXZSpeed;
            }
        }
        skipSpeedSync = false;

        if (isGrounded && avatar.getWorldLocation().y() >= -1.5f ) {
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
                float turnAmount = currentSpeed * Math.sin(wheelAngle * tightness) * dt;
                if (turnAmount != 0f) {
                    avatar.globalYaw(turnAmount);
                    if (isMultiplayer && protClient != null) protClient.sendTurnMessage(turnAmount);
                }
            }
        }

        if (physicsObj != null) {
            float[] curVel = physicsObj.getLinearVelocity();

            if (isGrounded) {
                Vector4f fwd = new Vector4f(0f, 0f, 1f, 1f);
                fwd.mul(avatar.getWorldRotation());
                float vertVel = Math.min(curVel[1], 0f); // to help prevent collision climbing
                physicsObj.setLinearVelocity(new float[]{fwd.x() * currentSpeed, vertVel, fwd.z() * currentSpeed});
            } else {
                currentSpeed = Math.sqrt((curVel[0] * curVel[0]) + (curVel[2] * curVel[2]));
            }

            if (isMultiplayer && protClient != null) protClient.sendMoveMessage(avatar.getWorldLocation());
        }

        // spin tires based on speed
        rollAngle += currentSpeed * dt / WHEEL_RADIUS;

        // wheels will return to center when not actively turning
        if (!turnKeyHeld && wheelAngle != 0f) {
            float step = WHEEL_RETURN_SPEED * dt;
            if (Math.abs(wheelAngle) <= step) wheelAngle = 0f;
            else wheelAngle -= Math.signum(wheelAngle) * step;
        }

        updateAllTires();
    }

    private void updateAllTires() {
        applyTireRotation(frontLeftTire, wheelAngle);
        applyTireRotation(frontRightTire, wheelAngle);
        applyTireRotation(backLeftTire, 0f);
        applyTireRotation(backRightTire, 0f);
    }

    private void applyTireRotation(GameObject tire, float steer) {
        if (tire == null) return;
        Vector3f t = new Vector3f();
        tire.getLocalTranslation().getTranslation(t);
        Vector3f s = tire.getLocalScale().getScale(new Vector3f());
        Matrix4f rot = new Matrix4f().rotationY(steer).rotateX(rollAngle);
        tire.setLocalRotation(rot);
        tire.setLocalTranslation(new Matrix4f().translation(t));
        tire.setLocalScale(new Matrix4f().scaling(s));
    }

    public float getCurrentSpeed() { return currentSpeed; }
    public void skipSpeedSyncOnce() { skipSpeedSync = true; }
    public void reduceSpeed(float amount) { currentSpeed = Math.signum(currentSpeed) * Math.max(0f, Math.abs(currentSpeed) - amount); }
}
