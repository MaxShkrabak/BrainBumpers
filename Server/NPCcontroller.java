import java.util.Random;
import java.util.ArrayList;
import java.util.List;

import org.joml.Vector3f;
import tage.ai.behaviortrees.BTCompositeType;
import tage.ai.behaviortrees.BTSelector;
import tage.ai.behaviortrees.BTSequence;
import tage.ai.behaviortrees.BehaviorTree;

public class NPCcontroller {

    private List<NPC> npcs = new ArrayList<>();
    List<Vector3f> allAvatarPositions;
    private int totalNPCcount = 1, waveCounter = 1;
    private boolean wavePending = false;
    private static final long WAVE_DELAY_MS = 5000;
    private List<BehaviorTree> behaviorTrees = new ArrayList<>();

    Random rn = new Random();
    boolean nearFlag = false;
    long thinkStartTime, tickStartTime, lastThinkUpdateTime, lastTickUpdateTime;
    GameServerUDP server;
    double criteria = 2.0;

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
        npc.randomizeLocation(
                -(rn.nextInt(21) + 20),
                -(rn.nextInt(26) + 15)
        );
        npcs.add(npc);

        //System.out.println("spawned npc at: " + Arrays.toString(npc.getLocation()));
        BehaviorTree bt = new BehaviorTree(BTCompositeType.SELECTOR);
        bt.insertAtRoot(new BTSelector(10));
        bt.insert(10, new AttackAvatar(server, this, npc));
        bt.insert(10, new MoveToAvatar(server, this, npc));

        behaviorTrees.add(bt);
        totalNPCcount++;
        return npc;
    }

    public List<Vector3f> getAllAvatarPositions() { return allAvatarPositions; }
    public void npcLoop() {
        while (!server.getIsGameOver()) {
            long currentTime = System.nanoTime();
            float elapsedThinkMilliSecs = (currentTime - lastThinkUpdateTime) / (1000000.0f);
            float elapsedTickMilliSecs  = (currentTime - lastTickUpdateTime)  / (1000000.0f);

            if (!server.getIsSpawning()) {
                if (elapsedTickMilliSecs >= 35.0f) {
                    lastTickUpdateTime = currentTime;
                    allAvatarPositions = server.getAllAvatarPositions();

                    List<NPC> npcSnapshot = new ArrayList<>(npcs);
                    for (NPC npc : npcSnapshot) {
                        npc.updateLocation(npcs, allAvatarPositions);
                    }
                    updateNPCs();
                }

                if (elapsedThinkMilliSecs >= 200.0f) {
                    lastThinkUpdateTime = currentTime;
                    List<BehaviorTree> btSnapshot = new ArrayList<>(behaviorTrees);
                    for (BehaviorTree bt : btSnapshot) {
                        bt.update(elapsedThinkMilliSecs);
                    }
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

    public boolean removeNPC(int id) {
        for (int i = 0; i < npcs.size(); i++) {
            if (npcs.get(i).getID() == id) {
                npcs.remove(i);
                behaviorTrees.remove(i);
                if (npcs.isEmpty()) triggerNewWave();
                return true;
            }
        }
        return false;
    }
    private void triggerNewWave() {
        if (wavePending) return;
        wavePending = true;
        System.out.println("[ GAME ]: Wave " + waveCounter + " complete! Next wave in " + (WAVE_DELAY_MS / 1000) + " seconds...");
        waveCounter++;
        server.sendSpawnWaveMsg(waveCounter);
        new Thread(() -> {
            try {
                Thread.sleep(WAVE_DELAY_MS);
                totalNPCcount = 1;
                server.spawnWave(waveCounter);
                wavePending = false;
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }).start();
    }

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

}