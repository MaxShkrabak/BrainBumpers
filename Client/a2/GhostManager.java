package a2;

import java.io.IOException;
import java.util.Iterator;
import java.util.UUID;
import java.util.Vector;
import org.joml.*;

import tage.*;
import tage.shapes.AnimatedShape;

public class GhostManager
{
	private MyGame game;
	private Vector<GhostAvatar> ghostAvatars = new Vector<GhostAvatar>();
	private Vector<GhostNPC> ghostNPCs = new Vector<GhostNPC>();

	public GhostManager(VariableFrameRateGame vfrg)
	{	game = (MyGame)vfrg;
	}

	public void createGhostNPC(int id, Vector3f position) throws IOException
	{
		System.out.println("[SERVER]: Adding NPC with ID --> " + id);
		AnimatedShape s = game.getNPCShape();
		TextureImage t = game.getNPCTexture();
		GhostNPC newNPC = new GhostNPC(id, s, t, position, 0, 0.08f);
		ghostNPCs.add(newNPC);

		game.initializeNPCPhysics(newNPC);
	}

	public void updateGhostNPC(int id, String[] position, float rot) throws IOException
	{
		GhostNPC ghostNPC = findNPC(id);

		Vector3f ghostPosition = new Vector3f(
				Float.parseFloat(position[0]),
				Float.parseFloat(position[1]),
				Float.parseFloat(position[2]));

        try {
			ghostNPC.setPosition(ghostPosition, rot);
		} catch (Error e){
			e.printStackTrace();
			System.out.println("couldn't update");
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

	
	public void createGhostAvatar(UUID id, Vector3f position, Matrix4f rotation) throws IOException
	{	System.out.println("[SERVER]: Adding ghost with ID --> " + id);
		ObjShape s = game.getGhostShape();
		TextureImage t = game.getGhostTexture();
		GhostAvatar newAvatar = new GhostAvatar(id, s, t, position);
		Matrix4f initialScale = (new Matrix4f()).scaling(1f);
		newAvatar.setLocalScale(initialScale);
		newAvatar.setLocalRotation(rotation);
		ghostAvatars.add(newAvatar);
	}

	public void removeGhostAvatar(UUID id)
	{	GhostAvatar ghostAvatar = findAvatar(id);
		if(ghostAvatar != null)
		{	game.getEngine().getSceneGraph().removeGameObject(ghostAvatar);
			ghostAvatars.remove(ghostAvatar);
		}
		else
		{	System.out.println("[SERVER]: Tried to remove, but unable to find ghost in list");
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

	private Vector<GhostAvatar> getGhostAvatars() {
		return ghostAvatars;
	}
	
	public void updateGhostAvatarPosition(UUID id, Vector3f position)
	{	GhostAvatar ghostAvatar = findAvatar(id);
		if (ghostAvatar != null)
		{	ghostAvatar.setPosition(position);
		}
		else
		{	System.out.println("[SERVER]: Tried to update ghost avatar position, but unable to find ghost in list");
		}
	}

	public void updateGhostAvatarRotation(UUID id, float rotation)
	{	GhostAvatar ghostAvatar = findAvatar(id);
		if (ghostAvatar != null)
		{	ghostAvatar.setRotation(rotation);
		}
		else
		{	System.out.println("[SERVER]: Tried to update ghost avatar rotation, but unable to find ghost in list");
		}
	}
}


