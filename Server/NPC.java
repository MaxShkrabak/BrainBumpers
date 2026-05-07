import org.joml.Vector3f;
public class NPC {
    double locationX, locationY, locationZ;
    double dir = 0.1;
    double size = 1.0;
    private int id;

    public NPC(int id) {
        this.id = id;
        locationX=0.0;
        locationY=0.0;
        locationZ=0.0;
    }
    public int getID(){return id;}

    public void randomizeLocation(int seedX, int seedZ) {
        locationX = ((double)seedX)/4.0;
        locationY = 0;
        locationZ = seedZ;
    }

    public double getX() { return locationX; }
    public double getY() { return locationY; }
    public double getZ() { return locationZ; }

    public void setX(double x) { locationX = x;}
    public void setY(double y) { locationY = y; }
    public void setZ(double z) { locationZ = z;}

    public String[] getLocation() {
        return new String[]{
                String.valueOf(locationX),
                String.valueOf(locationY),
                String.valueOf(locationZ)
        };
    }

    public void getBig() { size=2.0; }
    public void getSmall() { size=1.0; }
    public double getSize() { return size; }

    public void updateLocation()
    {
//        if (locationX > 10) dir=-0.1;
//        if (locationX < -10) dir=0.1;
//
//        locationX = locationX + dir;
    }
    public void moveTowardAvatar(Vector3f avatarPos){

    }
}