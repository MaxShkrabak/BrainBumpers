package a2;

import org.joml.Vector3f;
import org.joml.Matrix4f;
import tage.*;
import tage.shapes.AnimatedShape;

public class GhostNPC extends GameObject {
    private int id;
    private AnimatedShape npcS;
    private Vector3f stablePos;
    private int tickCount = 0;
    private boolean isRunning = false;

    public GhostNPC(int id, AnimatedShape s, TextureImage t, Vector3f p, float rot, float scale)
    { super(GameObject.root(), s, t);
        this.id = id;
        npcS = s;
        stablePos = p;
        setPosition(p, rot);
        this.setLocalScale(new Matrix4f().scaling(scale));
    }

    public int getID() { return id; }

    public void setPosition(Vector3f p, float rot) {
        tickCount++;

        if (tickCount >= 8) {
            tickCount = 0;
            float dist = stablePos.distance(p);

            if (dist > 0.004f) {
                if (!isRunning) {
                    this.npcS.playAnimation("RUN", 0.45f,
                            AnimatedShape.EndType.LOOP, 0);
                    isRunning = true;
                }
            } else {
                if (isRunning) {
                    this.npcS.stopAnimation();
                    isRunning = false;
                }
            }
            stablePos.set(p);
        }

        this.setLocalLocation(p);
        this.setLocalRotation((new Matrix4f()).rotationY((float)java.lang.Math.toRadians(rot)));
    }

    public void setSize(boolean big)
    {
        if (!big) {
            this.setLocalScale((new Matrix4f()).scaling(0.5f));
        }
        else
        {
            this.setLocalScale((new Matrix4f()).scaling(1.0f));
        }
    }
}
