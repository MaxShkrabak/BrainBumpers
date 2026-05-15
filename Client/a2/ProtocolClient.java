package a2;

import java.awt.Color;
import java.io.IOException;
import java.net.InetAddress;
import java.util.Iterator;
import java.util.UUID;
import java.util.Vector;
import org.joml.*;

import tage.*;
import tage.networking.client.GameConnectionClient;

public class ProtocolClient extends GameConnectionClient
{
	private MyGame game;
	private GhostManager ghostManager;
	private UUID id;
	private GhostNPC ghostNPC;
	private int ghostCounter = 0;
	
	public ProtocolClient(InetAddress remoteAddr, int remotePort, ProtocolType protocolType, MyGame game) throws IOException
	{	super(remoteAddr, remotePort, protocolType);
		this.game = game;
		this.id = UUID.randomUUID();
		ghostManager = game.getGhostManager();
	}
	
	public UUID getID() { return id; }
	
	@Override
	protected void processPacket(Object message)
	{	if (message == null) return;
		String strMessage = (String)message;
		if (strMessage.isEmpty()) return;
		System.out.println("[SERVER]: " + strMessage);
		String[] messageTokens = strMessage.split(",");

		// Game specific protocol to handle the message
		if(messageTokens.length > 0)
		{
			// Handle JOIN message
			// Format: (join,success) or (join,failure)
			if(messageTokens[0].compareTo("join") == 0)
			{	if(messageTokens[1].compareTo("success") == 0)
				{	System.out.println("[CLIENT]: Join success confirmed");
					game.setIsConnected(true);
					sendCreateMessage(game.getPlayerPosition());
				}
				if(messageTokens[1].compareTo("failure") == 0)
				{	System.out.println("[CLIENT]: Join failure confirmed");
					game.setIsConnected(false);
			}	}
			// Handle JOINMSG message
			// Format: (joinMsg)
			if(messageTokens[0].compareTo("joinMsg") == 0)
			{	System.out.println("[SERVER]: A Player has joined the game!");
				game.showChatMessage("joinMsg");
			}

			// Handle STARTMSG message
			// Format: (startMsg)
			if(messageTokens[0].compareTo("startMsg") == 0)
			{	System.out.println("[SERVER]: The game has been started!");
				game.showChatMessage("startMsg");
			}
			
			// Handle BYE message
			// Format: (bye,remoteId)
			if(messageTokens[0].compareTo("bye") == 0)
			{	// remove ghost avatar with id = remoteId
				// Parse out the id into a UUID
				UUID ghostID = UUID.fromString(messageTokens[1]);
				ghostManager.removeGhostAvatar(ghostID);
			}
			// Handle BYEMSG message
			// Format: (byeMsg)
			if(messageTokens[0].compareTo("byeMsg") == 0)
			{	System.out.println("[SERVER]: A Player has left the game!");
				game.showChatMessage("byeMsg");
			}
			
			// Handle CREATE message
			// Format: (create,remoteId,x,y,z)
			// AND
			// Handle DETAILS_FOR message
			// Format: (dsfr,remoteId,x,y,z)
			if (messageTokens[0].compareTo("create") == 0 || (messageTokens[0].compareTo("dsfr") == 0))
			{	// create a new ghost avatar
				// Parse out the id into a UUID
				UUID ghostID = UUID.fromString(messageTokens[1]);
				
				// Parse out the position into a Vector3f
				Vector3f ghostPosition = new Vector3f(
					Float.parseFloat(messageTokens[2]),
					Float.parseFloat(messageTokens[3]),
					Float.parseFloat(messageTokens[4]));

				Matrix4f ghostRotation = new Matrix4f();

				try
				{	ghostManager.createGhostAvatar(ghostID, ghostPosition, ghostRotation);
				}	catch (IOException e)
				{	System.err.println("[ERROR]: Error creating ghost avatar");
				}
			}

			// Receive set new texture for a ghost from server
			if (messageTokens[0].compareTo("setTex") == 0)
			{
				UUID ghostID = UUID.fromString(messageTokens[1]);
				int skinOpt = Integer.parseInt(messageTokens[2]);
                ghostManager.updateGhostAvatarTex(ghostID, skinOpt);
            }
			
			// Handle WANTS_DETAILS message
			// Format: (wsds,remoteId)
			if (messageTokens[0].compareTo("wsds") == 0)
			{
				// Send the local client's avatar's information
				// Parse out the id into a UUID
				UUID ghostID = UUID.fromString(messageTokens[1]);
				sendDetailsForMessage(ghostID, game.getPlayerPosition(), game.getPlayerRotation());
			}
			
			// Handle MOVE message
			// Format: (move,remoteId,x,y,z)
			if (messageTokens[0].compareTo("move") == 0)
			{
				// move a ghost avatar
				// Parse out the id into a UUID
				UUID ghostID = UUID.fromString(messageTokens[1]);
				
				// Parse out the position into a Vector3f
				Vector3f ghostPosition = new Vector3f(
					Float.parseFloat(messageTokens[2]),
					Float.parseFloat(messageTokens[3]),
					Float.parseFloat(messageTokens[4]));
				
				ghostManager.updateGhostAvatarPosition(ghostID, ghostPosition);
			}

			// Handle TURN message
			// Format: (turn,remoteId,angle)
			if (messageTokens[0].compareTo("turn") == 0)
			{
				// move a ghost avatar
				// Parse out the id into a UUID
				UUID ghostID = UUID.fromString(messageTokens[1]);

				// Parse out the position into a float
				float ghostRotation = Float.parseFloat(messageTokens[2]);

				ghostManager.updateGhostAvatarRotation(ghostID, ghostRotation);
			}

			if (messageTokens[0].compareTo("wave") == 0)
			{
				game.setZombiesAlive(Integer.parseInt(messageTokens[1]));
			}

			if (messageTokens[0].compareTo("createNPC") == 0)
			{
				int NPCid = Integer.parseInt(messageTokens[1]);
				// create a new ghost NPC
				// Parse out the position
				Vector3f ghostPosition = new Vector3f(
						Float.parseFloat(messageTokens[2]),
						Float.parseFloat(messageTokens[3]),
						Float.parseFloat(messageTokens[4]));
				try {
					ghostManager.createGhostNPC(NPCid, ghostPosition);
					//System.out.println("Creating npc in prot client with id: " + NPCid);
				} catch (IOException e) {

				} // error creating ghost avatar
			}

			if (messageTokens[0].compareTo("updateNPC") == 0)
			{
				int NPCid = Integer.parseInt(messageTokens[1]);
				// create a new ghost NPC
				// Parse out the position
				Vector3f ghostPosition = new Vector3f(
						Float.parseFloat(messageTokens[2]),
						Float.parseFloat(messageTokens[3]),
						Float.parseFloat(messageTokens[4]));
				float rot = Float.parseFloat(messageTokens[5]);
				try {
					ghostManager.updateGhostNPC(NPCid, ghostPosition, rot);
				} catch (IOException e) {
					e.printStackTrace();
				} // error updating ghost avatar
			}

			// confirms npc was ran over
			if (messageTokens[0].compareTo("removeNPC") == 0)
			{	int npcId = Integer.parseInt(messageTokens[1]);
				ghostManager.removeGhostNPC(npcId);
				game.zombieKilled();
			}

			// player scores
			if (messageTokens[0].compareTo("scoreUpdate") == 0)
			{	for (int i = 1; i + 1 < messageTokens.length; i += 2)
				{	UUID pid = UUID.fromString(messageTokens[i]);
					int s = Integer.parseInt(messageTokens[i + 1]);
					game.updatePlayerScore(pid, s);
				}
			}

			// npc attacks you
			if (messageTokens[0].compareTo("npcAttack") == 0)
			{
				int npcID = Integer.parseInt(messageTokens[1]);
				game.handleNPCattack(npcID);
			}

			// another avatar died
			if (messageTokens[0].compareTo("playerDead") == 0) {
				UUID deadID = UUID.fromString(messageTokens[1]);
				game.getGhostManager().hideGhostAvatar(deadID);
			}
		}	}

	public void sendConfirmationMessage()
	{	try
	{	sendPacket(new String("confirm," + id.toString()));
	} catch (IOException e)
	{	e.printStackTrace();
	}	}

	public void sendDeathMessage() {
		try {
			String message = "dead," + id.toString();
			sendPacket(message);
		} catch (IOException e) {
			e.printStackTrace();
		}
	}
	
	// The initial message from the game client requesting to join the 
	// server. localId is a unique identifier for the client. Recommend 
	// a random UUID.
	// Message Format: (join,localId)
	
	public void sendJoinMessage()
	{	try 
		{	sendPacket(new String("join," + id.toString()));
		} catch (IOException e) 
		{	e.printStackTrace();
	}	}
	
	// Informs the server that the client is leaving the server. 
	// Message Format: (bye,localId)

	public void sendByeMessage()
	{	try 
		{	sendPacket(new String("bye," + id.toString()));
		} catch (IOException e) 
		{	e.printStackTrace();
	}	}

	public void sendReadyMessage() {
		try {
			sendPacket(new String("ready," + id.toString()));
		} catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

	public void sendSetTexMessage(int opt) {
		try {
			sendPacket(new String("setTex," + id.toString() + "," + opt));
		} catch (IOException e) {
			throw new RuntimeException(e);
		}
	}
	
	// Informs the server of the clients Avatars position. The server 
	// takes this message and forwards it to all other clients registered 
	// with the server.
	// Message Format: (create,localId,x,y,z) where x, y, and z represent the position

	public void sendCreateMessage(Vector3f position)
	{	try 
		{	String message = new String("create," + id.toString());
			message += "," + position.x();
			message += "," + position.y();
			message += "," + position.z();
			
			sendPacket(message);
		} catch (IOException e) 
		{	e.printStackTrace();
	}	}
	
	// Informs the server of the local avatar's position. The server then 
	// forwards this message to the client with the ID value matching remoteId. 
	// This message is generated in response to receiving a WANTS_DETAILS message 
	// from the server.
	// Message Format: (dsfr,remoteId,localId,x,y,z) where x, y, and z represent the position.

	public void sendDetailsForMessage(UUID remoteId, Vector3f position, Matrix4f angle)
	{	try 
		{	String message = new String("dsfr," + remoteId.toString() + "," + id.toString());
			message += "," + position.x();
			message += "," + position.y();
			message += "," + position.z();
			message += "," + angle;
			
			sendPacket(message);
		} catch (IOException e) 
		{	e.printStackTrace();
	}	}
	
	// Informs the server that the local avatar has changed position.  
	// Message Format: (move,localId,x,y,z) where x, y, and z represent the position.

	public void sendMoveMessage(Vector3f position)
	{	try 
		{	String message = new String("move," + id.toString());
			message += "," + position.x();
			message += "," + position.y();
			message += "," + position.z();
			
			sendPacket(message);
		} catch (IOException e) 
		{	e.printStackTrace();
	}	}

	// Informs the server that the local player killed an NPC.
	// Message Format: (killNPC,localId,npcId)
	public void sendKillMessage(int npcId)
	{	try
		{	sendPacket("killNPC," + id.toString() + "," + npcId);
		} catch (IOException e)
		{	e.printStackTrace();
		}
	}

	// Informs the server that the local avatar has changed rotation.
	// Message Format: (turn,localId,angle).
	public void sendTurnMessage(float angle)
	{	try
	{	String message = new String("turn," + id.toString());
		message += "," + angle;

		sendPacket(message);
	} catch (IOException e)
	{	e.printStackTrace();
	}	}
}



