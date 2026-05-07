package a2;

import org.joml.Vector3f;
import org.joml.Matrix4f;
import tage.*;
import tage.shapes.AnimatedShape;

import java.util.UUID;

public class GhostNPC extends GameObject {
    private int id;
    public GhostNPC(int id, AnimatedShape s, TextureImage t, Vector3f p, float scale)
    { super(GameObject.root(), s, t);
        this.id = id;
        setPosition(p);
        this.setLocalScale(new Matrix4f().scaling(scale));
    }

    public int getID() { return id; }
    public void setPosition(Vector3f p) {
        this.setLocalLocation(p);
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
