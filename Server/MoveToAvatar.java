import org.joml.Vector3f;
import tage.ai.behaviortrees.BTAction;
import tage.ai.behaviortrees.BTStatus;

public class MoveToAvatar extends BTAction {
    NPC npc;
    NPCcontroller npcc;
    GameServerUDP server;

    public MoveToAvatar(GameServerUDP s, NPCcontroller c, NPC n) {
        server = s; npcc = c; npc = n;
    }

    @Override
    protected BTStatus update(float elapsedTime) {
        Vector3f currAvatarLoc = server.sendAvatarLocation(npc);

        npc.setTarget(currAvatarLoc);
        return BTStatus.BH_RUNNING;
    }
}