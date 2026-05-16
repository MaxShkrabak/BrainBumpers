package a3;

import java.io.IOException;
import java.util.Iterator;
import java.util.UUID;
import java.util.Vector;
import org.joml.*;

import tage.*;
import tage.physics.PhysicsObject;
import tage.shapes.AnimatedShape;

public class GhostManager
{
	private MyGame game;
	private Vector<GhostAvatar> ghostAvatars = new Vector<GhostAvatar>();
	private Vector<GhostNPC> ghostNPCs = new Vector<GhostNPC>();
	Random rn = new Random();

	public GhostManager(VariableFrameRateGame vfrg)
	{	game = (MyGame)vfrg;
	}

	public void createGhostNPC(int id, Vector3f position) throws IOException
	{
		System.out.println("[SERVER]: Adding NPC with ID --> " + id);
		AnimatedShape s = game.getNPCShape();
		TextureImage t = game.getNPCTexture();
		GhostNPC newNPC = new GhostNPC(id, s, t, position, 0, 0.16f);
		ghostNPCs.add(newNPC);

		initializeNPCPhysics(newNPC);
	}

	private void initializeNPCPhysics(GhostNPC ghostNPC) {
		Vector3f spawnPos = ghostNPC.getWorldLocation();
		spawnPos.add(0, 0.6f,0);

		PhysicsObject npcPhysics = (game.getEngine().getSceneGraph()).addPhysicsCapsule(
				0.0f,
				spawnPos,
				new Quaternionf(),
				1,.4f,.5f
		);
		ghostNPC.setPhysicsObject(npcPhysics);
		npcPhysics.disableSleeping();
		npcPhysics.setBounciness(0.1f);
	}

	private void removeNPCPhysics(GhostNPC ghostNPC) {
		game.getEngine().getSceneGraph().removePhysicsObject(ghostNPC.getPhysicsObject());
	}

	public void updateGhostNPC(int id, Vector3f ghostPosition, float rot) throws IOException
	{
		GhostNPC ghostNPC = findNPC(id);
		if (ghostNPC == null) return;
		ghostNPC.setPosition(ghostPosition, rot);
	}

	public void removeGhostNPC(int id)
	{	GhostNPC ghostNPC = findNPC(id);
		if(ghostNPC != null)
		{	game.getEngine().getSceneGraph().removeGameObject(ghostNPC);
			ghostNPCs.remove(ghostNPC);
			removeNPCPhysics(ghostNPC);
		}
		else
		{	System.out.println("[SERVER]: Tried to remove, but unable to find ghost NPC in list");
		}
	}

	private GhostNPC findNPC(int id)
	{	GhostNPC ghostNPC;
		Iterator<GhostNPC> it = ghostNPCs.iterator();
		while(it.hasNext())
		{	ghostNPC= it.next();
			if(ghostNPC.getID() == id)
			{	return ghostNPC;
			}
		}
		return null;
	}

	public Vector<GhostNPC> getGhostNPCs() {
		return ghostNPCs;
	}

	public Vector3f getGhostNPCPosition(int id) {
		GhostNPC npc = findNPC(id);

		if (npc == null) {
			return null;
		}

		return new Vector3f(npc.getWorldLocation());
	}

	
	public void createGhostAvatar(UUID id, int tex, Vector3f position, Matrix4f rotation) throws IOException
	{
		if (findAvatar(id) != null) {
			System.out.println("[CLIENT]: Ghost " + id + " already exists, skipping");
			return;
		}
		System.out.println("[SERVER]: Adding ghost with ID --> " + id);
		ObjShape s = game.getGhostShape();
		ObjShape ltS = game.getLeftTireShape();
		ObjShape rtS = game.getRightTireShape();
		TextureImage gt = game.getGhostTexture(tex);
		TextureImage tt = game.getTireTexture();
		GhostAvatar newAvatar = new GhostAvatar(id, s, gt, position, ltS, rtS, tt);
		Matrix4f initialScale = (new Matrix4f()).scaling(2f);
		newAvatar.setLocalScale(initialScale);
		newAvatar.setLocalRotation(rotation);
		ghostAvatars.add(newAvatar);

		initializeAvatarPhysics(newAvatar);
	}

	private void initializeAvatarPhysics(GhostAvatar ghostAvatar) {
		float[] carSize = {0.5f, 0.3f, 1.25f};
		PhysicsObject avatarPhysics = (game.getEngine().getSceneGraph()).addPhysicsBox(
				1.0f, ghostAvatar.getWorldLocation(), new Quaternionf(), carSize);
		ghostAvatar.setPhysicsObject(avatarPhysics);
		avatarPhysics.disableSleeping();
		avatarPhysics.setBounciness(0.15f);
	}

	private void removeAvatarPhysics(GhostAvatar ghostAvatar) {
		game.getEngine().getSceneGraph().removePhysicsObject(ghostAvatar.getPhysicsObject());
	}

	public void removeGhostAvatar(UUID id)
	{	GhostAvatar ghostAvatar = findAvatar(id);
		if(ghostAvatar != null)
		{
			ghostAvatar.removeChildren();
			game.getEngine().getSceneGraph().removeGameObject(ghostAvatar);
			ghostAvatars.remove(ghostAvatar);
			removeAvatarPhysics(ghostAvatar);
		}
		else
		{	System.out.println("[SERVER]: Tried to remove, but unable to find ghost Avatar in list");
		}
	}

	public void hideGhostAvatar(UUID id) {
		GhostAvatar ghost = findAvatar(id);
		if (ghost != null) {
			ghost.getRenderStates().disableRendering();
			// hide tires too
			ghost.hideChildren();
		}
	}

	private GhostAvatar findAvatar(UUID id)
	{	GhostAvatar ghostAvatar;
		Iterator<GhostAvatar> it = ghostAvatars.iterator();
		while(it.hasNext())
		{	ghostAvatar = it.next();
			if(ghostAvatar.getID().compareTo(id) == 0)
			{	return ghostAvatar;
			}
		}		
		return null;
	}

	public Vector<GhostAvatar> getGhostAvatars() {
		return ghostAvatars;
	}
	
	public void updateGhostAvatarPosition(UUID id, Vector3f position)
	{	GhostAvatar ghostAvatar = findAvatar(id);
		if (ghostAvatar != null)
		{	ghostAvatar.setPosition(position);
			ghostAvatar.updateRoll(position);
		}
		else
		{	System.out.println("[SERVER]: Tried to update ghost avatar position, but unable to find ghost in list");
		}
	}

	public void updateGhostAvatarRotation(UUID id, float rotation, float wheelAngle)
	{	GhostAvatar ghostAvatar = findAvatar(id);
		if (ghostAvatar != null)
		{	ghostAvatar.setRotation(rotation);
			ghostAvatar.updateSteer(wheelAngle);
		}
		else
		{	System.out.println("[SERVER]: Tried to update ghost avatar rotation, but unable to find ghost in list");
		}
	}
	public void updateGhostAvatarTex(UUID id, int opt)
	{	GhostAvatar ghostAvatar = findAvatar(id);
		if (ghostAvatar != null)
		{	ghostAvatar.setNewTexture(game.getGhostTexture(opt));
		}
		else
		{	System.out.println("[SERVER]: Tried to update ghost avatar position, but unable to find ghost in list");
		}
	}
}


