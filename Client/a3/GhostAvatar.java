package a2;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.UUID;

import tage.*;
import org.joml.*;

// A ghost MUST be connected as a child of the root,
// so that it will be rendered, and for future removal.
// The ObjShape and TextureImage associated with the ghost
// must have already been created during loadShapes() and
// loadTextures(), before the game loop is started.

public class GhostAvatar extends GameObject
{
	UUID uuid;
	GameObject backLeftTire, backRightTire, frontLeftTire, frontRightTire;

	private float rollAngle = 0f;
	private float steerAngle = 0f;
	private Vector3f prevPosition = null;
	private static final float WHEEL_RADIUS = 0.6f;

	public GhostAvatar(UUID id, ObjShape s, TextureImage t, Vector3f p, ObjShape ltS, ObjShape rtS, TextureImage tireT)
	{	super(GameObject.root(), s, t);
		uuid = id;
		setPosition(p);

		backLeftTire = spawnObject(this, ltS, tireT, 0.44f, -0.18f, -0.77f, 1f, 0f);
		backRightTire = spawnObject(this, rtS, tireT, -0.38f, -0.18f, -0.77f, 1f, 0f);
		frontLeftTire = spawnObject(this, ltS, tireT, 0.44f, -0.18f, 0.63f, 1f, 0f);
		frontRightTire = spawnObject(this, rtS, tireT, -0.38f, -0.18f, 0.63f, 1f, 0f);

		backLeftTire.applyParentRotationToPosition(true);
		backRightTire.applyParentRotationToPosition(true);
		frontLeftTire.applyParentRotationToPosition(true);
		frontRightTire.applyParentRotationToPosition(true);
	}

	public void updateRoll(Vector3f newPosition) {
		if (prevPosition != null) {
			float dist = newPosition.distance(prevPosition);
			rollAngle += dist / WHEEL_RADIUS;
		}
		prevPosition = new Vector3f(newPosition);
		applyTireRotations();
	}

	public void updateSteer(float wheelAngle) {
		steerAngle = wheelAngle;
		applyTireRotations();
	}

	private void applyTireRotations() {
		applyTireRotation(frontLeftTire, steerAngle);
		applyTireRotation(frontRightTire, steerAngle);
		applyTireRotation(backLeftTire, 0f);
		applyTireRotation(backRightTire, 0f);
	}

	private void applyTireRotation(GameObject tire, float steer) {
		if (tire == null) return;
		Vector3f t = new Vector3f();
		tire.getLocalTranslation().getTranslation(t);
		Vector3f s = tire.getLocalScale().getScale(new Vector3f());
		Matrix4f rot = new Matrix4f().rotationY(steer).rotateX(rollAngle);
		tire.setLocalRotation(rot);
		tire.setLocalTranslation(new Matrix4f().translation(t));
		tire.setLocalScale(new Matrix4f().scaling(s));
	}

	public void removeChildren() {
		List<GameObject> childList = new ArrayList<>();
		Iterator it = getChildrenIterator();
		while (it.hasNext()) {
			childList.add((GameObject) it.next());
		}
		for (GameObject child : childList) {
			if (child != null) {
				Engine.getEngine().getSceneGraph().removeGameObject(child);
			}
		}
	}

	public void hideChildren() {
		Iterator it = getChildrenIterator();
		while (it.hasNext()) {
			GameObject child = (GameObject) it.next();
			if (child != null) {
				child.getRenderStates().disableRendering();
			}
		}
	}

	public UUID getID() { return uuid; }
	public void setPosition(Vector3f m) { setLocalLocation(m); }
	public void setRotation(float m) { globalYaw(m); }
	public Vector3f getPosition() { return getWorldLocation(); }
	public void setNewTexture(TextureImage tex){ setTextureImage(tex);}
}
