import org.joml.Vector3f;
import tage.ai.behaviortrees.BTAction;
import tage.ai.behaviortrees.BTCondition;
import tage.ai.behaviortrees.BTStatus;

import java.util.Vector;

public class MoveToAvatar extends BTAction {
    NPC npc;
    NPCcontroller npcc;
    GameServerUDP server;

    public MoveToAvatar(GameServerUDP s, NPCcontroller c, NPC n)
    {
        server = s; npcc = c; npc = n;
    }

    @Override
    protected BTStatus update(float elapsedTime) {
        Vector3f currAvatarLoc = server.sendAvatarLocation(npc);
        Vector3f currNPCLoc = new Vector3f((float) npc.getX(), (float) npc.getY(), (float) npc.getZ());

        Vector3f direction = new Vector3f();
        currAvatarLoc.sub(currNPCLoc, direction);
        direction.normalize();
        direction.mul(0.002f * elapsedTime);

        Vector3f newLocation = new Vector3f();
        currNPCLoc.add(direction, newLocation);

        npc.setX(newLocation.x);
        npc.setY(newLocation.y);
        npc.setZ(newLocation.z);

        return BTStatus.BH_SUCCESS;
    }
}