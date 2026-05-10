import org.joml.Vector3f;

public class NPC {
    double locationX, locationY, locationZ;
    double size = 1.0;
    private int id;
    private Vector3f target = null;
    private float rotationY = 0.0f;
    private float targetRotationY = 0.0f;
    private static final float TURN_SPEED = 10.0f;
    private static final float SPEED = 0.0045f; // tune this

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

    public void updateLocation() {
        if (target == null) return;

        Vector3f currLoc = new Vector3f((float) locationX, (float) locationY, (float) locationZ);
        Vector3f direction = new Vector3f();
        target.sub(currLoc, direction);

        if (direction.length() < 0.01f) return; // close enough, stop

        targetRotationY = (float) Math.toDegrees(Math.atan2(direction.x, direction.z));
        rotationY = lerpAngle(rotationY, targetRotationY, TURN_SPEED);

        direction.normalize();
        direction.mul(SPEED * 30.0f);

        locationX += direction.x;
        locationY = 0;
        locationZ += direction.z;
    }

    // smoothly rotate instead of flicker
    private float lerpAngle(float current, float target, float speed) {
        float diff = target - current;

        while (diff > 180.0f)  diff -= 360.0f;
        while (diff < -180.0f) diff += 360.0f;

        if (Math.abs(diff) <= speed) return target;
        return current + Math.signum(diff) * speed;
    }

    public void moveTowardAvatar(Vector3f avatarPos) { }
}