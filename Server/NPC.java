import org.joml.Vector3f;

import java.util.List;

public class NPC {
    double locationX, locationY, locationZ;
    double size = 1.0;
    private int id;
    private Vector3f target = null;
    private float rotationY = 0.0f;
    private float targetRotationY = 0.0f;
    private static final float TURN_SPEED = 10.0f;
    private static final float SPEED = 0.0035f;

    public NPC(int id) {
        this.id = id;
        locationX = 0.0;
        locationY = 0.0;
        locationZ = 0.0;
    }

    public int getID() { return id; }

    public void randomizeLocation(int seedX, int seedZ) {
        locationX = ((double) seedX) / 4.0;
        locationY = 0;
        locationZ = seedZ;
    }

    public double getX() { return locationX; }
    public double getY() { return locationY; }
    public double getZ() { return locationZ; }

    public void setX(double x) { locationX = x; }
    public void setY(double y) { locationY = y; }
    public void setZ(double z) { locationZ = z; }

    public void setTarget(Vector3f t) { target = t; }
    public Vector3f getTarget() { return target; }

    public String[] getLocation() {
        return new String[]{
                String.valueOf(locationX),
                String.valueOf(locationY),
                String.valueOf(locationZ)
        };
    }

    public void getBig()   { size = 2.0; }
    public void getSmall() { size = 1.0; }
    public double getSize() { return size; }
    public float getRotationY() { return rotationY; }

    public void updateLocation(List<NPC> allNPCs, List<Vector3f> allAvatarPositions) {

        // move to avatar
        if (target != null) {
            Vector3f currLoc = new Vector3f((float) locationX, 0, (float) locationZ);
            Vector3f direction = new Vector3f();
            Vector3f targetFlat = new Vector3f(target.x, 0, target.z);
            targetFlat.sub(currLoc, direction);

            if (direction.length() >= 0.01f) {
                targetRotationY = (float) Math.toDegrees(Math.atan2(direction.x, direction.z));
                rotationY = lerpAngle(rotationY, targetRotationY, TURN_SPEED);

                float rad = (float) Math.toRadians(rotationY);
                locationX += Math.sin(rad) * SPEED * 30.0f;
                locationZ += Math.cos(rad) * SPEED * 30.0f;
            }
        }

        // separate from other zombies
        for (NPC other : allNPCs) {
            if (other == this) continue;
            Vector3f myPos = new Vector3f((float)locationX, 0, (float)locationZ);
            Vector3f otherPos = new Vector3f((float)other.getX(), 0, (float)other.getZ());
            float dist = myPos.distance(otherPos);

            // how close zombies stand next to each other
            float zombieCollisionRadius = 0.6f;

            if (dist < zombieCollisionRadius && dist > 0.001f) {
                Vector3f push = new Vector3f();
                myPos.sub(otherPos, push);
                push.normalize();

                float overlap = zombieCollisionRadius - dist;

                locationX += push.x * (overlap * 0.5f);
                locationZ += push.z * (overlap * 0.5f);
            }
        }

        // dont walk past the avatar model
        if (allAvatarPositions != null) {
            for (Vector3f avatarPos : allAvatarPositions) {
                Vector3f myPos = new Vector3f((float)locationX, 0, (float)locationZ);
                Vector3f avatarFlat = new Vector3f(avatarPos.x, 0, avatarPos.z);

                float dist = myPos.distance(avatarFlat);
                float collisionRadius = 1f;

                if (dist < collisionRadius && dist > 0.001f) {
                    Vector3f push = new Vector3f();
                    myPos.sub(avatarFlat, push);
                    push.normalize();

                    // clamp stuff
                    locationX = avatarFlat.x + (push.x * collisionRadius);
                    locationZ = avatarFlat.z + (push.z * collisionRadius);
                }
            }
        }
    }

    // smoothly rotate instead of flicker
    private float lerpAngle(float current, float target, float speed) {
        float diff = target - current;

        while (diff > 180.0f)  diff -= 360.0f;
        while (diff < -180.0f) diff += 360.0f;

        if (Math.abs(diff) <= speed) return target;
        return current + Math.signum(diff) * speed;
    }
}