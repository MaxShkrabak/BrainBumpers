import java.util.Random;
import java.util.ArrayList;
import java.util.List;

import org.joml.Vector3f;
import tage.ai.behaviortrees.BTCompositeType;
import tage.ai.behaviortrees.BTSequence;
import tage.ai.behaviortrees.BehaviorTree;

public class NPCcontroller {

    private List<NPC> npcs = new ArrayList<>();
    private int totalNPCcount = 1;
    private List<BehaviorTree> behaviorTrees = new ArrayList<>();

    Random rn = new Random();
    boolean nearFlag = false;
    long thinkStartTime, tickStartTime, lastThinkUpdateTime, lastTickUpdateTime;
    GameServerUDP server;
    double criteria = 2.0;
    Vector3f carPos;

    public void updateNPCs() {
        //System.out.println("printing count of npcs: " + npcs.size());
        if (!npcs.isEmpty()) {
            for (int i = 0; i < npcs.size(); i++) {
                server.sendNPCinfo(npcs.get(i));
                //System.out.println("sending info for id: " + i);
            }
        }
    }

    public void start(GameServerUDP s)
    {
        thinkStartTime = System.nanoTime();
        tickStartTime = System.nanoTime();
        lastThinkUpdateTime = thinkStartTime;
        lastTickUpdateTime = tickStartTime;
        server = s;
        npcLoop();
    }

    public NPC spawnNPC()
    {
        NPC npc = new NPC(totalNPCcount);
        npc.randomizeLocation(rn.nextInt(40), rn.nextInt(15));
        npcs.add(npc);

        //System.out.println("spawned npc at: " + Arrays.toString(npc.getLocation()));
        BehaviorTree bt = new BehaviorTree(BTCompositeType.SELECTOR);
        bt.insertAtRoot(new BTSequence(10));
        bt.insert(10, new MoveToAvatar(server, this, npc));
        behaviorTrees.add(bt);

        totalNPCcount++;
        return npc;
    }

    public void npcLoop()
    {
        while (true) {
            long currentTime = System.nanoTime();
            float elapsedThinkMilliSecs =
                    (currentTime-lastThinkUpdateTime)/(1000000.0f);
            float elapsedTickMilliSecs =
                    (currentTime-lastTickUpdateTime)/(1000000.0f);

            if (elapsedTickMilliSecs >= 25.0f) {
                lastTickUpdateTime = currentTime;
                for (NPC npc : npcs) {
                    npc.updateLocation();
                }
                if (!server.getIsSpawning()) {
                    updateNPCs();
                }
            }

            if (elapsedThinkMilliSecs >= 250.0f) {
                lastThinkUpdateTime = currentTime;
                for (BehaviorTree bt : behaviorTrees) {
                    bt.update(elapsedThinkMilliSecs);
                }
            }

            Thread.yield();
        }
    }

//    public void setupBehaviorTree()
//    {
//        bt.insertAtRoot(new BTSequence(10));
//        //bt.insertAtRoot(new BTSequence(20));
//        //bt.insert(10, new AvatarNear(server,this,npc,false));
//        bt.insert(10, new MoveToAvatar(server,this,npc));
//        //bt.insert(10, new GetSmall(npc));
//        //bt.insert(20, new AvatarNear(server,this,npc,false));
//        //bt.insert(20, new GetBig(npc));
//    }

    public NPC getNPC(int index) {
        return npcs.get(index);
    }

    // Get all NPCs
    public List<NPC> getNPCs() {
        return npcs;
    }

    public double getCriteria() {
        return criteria;
    }
    public boolean getNearFlag(){
        return nearFlag;
    }
    public void setNearFlag(boolean flag){
        nearFlag = flag;
    }
    public void setCurAvatarPos(Vector3f pos){ carPos = pos;}

}