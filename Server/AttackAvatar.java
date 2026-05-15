import tage.ai.behaviortrees.BTAction;
import tage.ai.behaviortrees.BTStatus;
import org.joml.Vector3f;
import java.util.List;
import java.util.Map;
import java.util.UUID;

public class AttackAvatar extends BTAction {
    private NPC npc;
    private NPCcontroller npcc;
    private GameServerUDP server;

    private static final float ATTACK_RANGE = 1.5f;
    private static final long ATTACK_COOLDOWN_MS = 3000;
    private long lastAttackTime = 0;

    public AttackAvatar(GameServerUDP s, NPCcontroller c, NPC n) {
        super();
        server = s; npcc = c; npc = n;
    }

    @Override
    protected BTStatus update(float elapsedTime) {
        Vector3f npcPos = new Vector3f((float) npc.getX(), 0, (float) npc.getZ());

        UUID closestID = null;
        float closestDist = Float.MAX_VALUE;

        for (Map.Entry<UUID, String[]> entry : server.getCurPositions().entrySet()) {
            String[] pos = entry.getValue();
            Vector3f avatarFlat = new Vector3f(
                    Float.parseFloat(pos[0]), 0, Float.parseFloat(pos[2]));
            float dist = npcPos.distance(avatarFlat);
            if (dist < closestDist) {
                closestDist = dist;
                closestID = entry.getKey();
            }
        }

        if (closestID != null && closestDist <= ATTACK_RANGE) {
            long now = System.currentTimeMillis();
            if (now - lastAttackTime >= ATTACK_COOLDOWN_MS) {
                lastAttackTime = now;
                server.sendNPCattack(npc.getID(), closestID);
            }
            return BTStatus.BH_SUCCESS;
        }

        return BTStatus.BH_FAILURE;
    }
}