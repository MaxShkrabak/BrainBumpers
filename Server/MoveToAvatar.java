import org.joml.Vector3f;
import tage.ai.behaviortrees.BTAction;
import tage.ai.behaviortrees.BTStatus;

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

        float distance = currNPCLoc.distance(currAvatarLoc);
        if (distance < 1.5f) {
            npc.setTarget(null); // stop moving
            return BTStatus.BH_SUCCESS;
        }

        npc.setTarget(currAvatarLoc);
        return BTStatus.BH_RUNNING;
    }
}