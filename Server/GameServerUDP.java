import java.io.IOException;
import java.net.InetAddress;
import java.util.UUID;
import java.util.HashMap;

import org.joml.Vector3f;
import tage.networking.server.GameConnectionServer;
import tage.networking.server.IClientInfo;

public class GameServerUDP extends GameConnectionServer<UUID> 
{
	NPCcontroller npcCtrl;

	private static final int MIN_PLAYERS = 2;
	private boolean gameStarted = false;
	private String[] curPos = {"0", "0", "0"};
	HashMap<UUID, Boolean> readyStatus = new HashMap<>();

	public GameServerUDP(int localPort, NPCcontroller npcCtrl) throws IOException
	{
		super(localPort, ProtocolType.UDP);
		this.npcCtrl = npcCtrl;
		npcCtrl.start(this);
	}

	@Override
	public void processPacket(Object o, InetAddress senderIP, int senderPort) {
		String message = (String)o;
		String[] messageTokens = message.split(",");
		
		if(messageTokens.length > 0)
		{	// JOIN -- Case where client just joined the server
			// Received Message Format: (join,localId)
			if(messageTokens[0].compareTo("join") == 0)
			{	try 
				{	UUID clientID = UUID.fromString(messageTokens[1]);
					System.out.println("[CLIENT]: Join request received from - " + clientID.toString());

					if (gameStarted) {
						sendJoinedMessage(clientID, false);
					} else {
						IClientInfo ci;
						ci = getServerSocket().createClientInfo(senderIP, senderPort);
						addClient(ci, clientID);

						sendJoinedMessage(clientID, true);
						readyStatus.put(clientID, false); // Store the connected user in the hashmap
					}
				} 
				catch (IOException e) 
				{	UUID clientID = UUID.fromString(messageTokens[1]);
					System.out.println("[CLIENT]: Join request failed from - " + clientID.toString());
					sendJoinedMessage(clientID, false);
					e.printStackTrace();
			}	}

			// READY -- Case where clients readies in game
			// Received Message Format: (ready,localId)
			if(messageTokens[0].compareTo("ready") == 0)
			{	if (!gameStarted) {
					UUID clientID = UUID.fromString(messageTokens[1]);
					if (!readyStatus.containsKey(clientID)){}
					System.out.println("[CLIENT]: Ready request received from - " + clientID.toString());

					// Check that the player is not already readied up
					if (readyStatus.get(clientID) != true) {
						readyStatus.replace(clientID, false, true); // set ready status to true
					}

					// Starts the game when all players have connected and readied up
					if (getClients().size() >= MIN_PLAYERS && !readyStatus.containsValue(false)) {
						gameStarted = true;
						System.out.println("[SERVER] All players have readied up, starting game!");
						startGameMessage();
						System.out.println("maybe spawning zombie idk");
						String[] pos1 = {"2.0", "0.0", "5.0"};
						//String[] pos2 = {"4.0", "2.0", "10.0"};
						//String[] pos3 = {"-5.0", "0.0", "-5.0"};
						sendCreateNPCmsg(pos1);
						//sendCreateNPCmsg(pos2);
						//sendCreateNPCmsg(pos3);
					}
				}
			}

			// TODO Unready logic

			// BYE -- Case where clients leaves the server
			// Received Message Format: (bye,localId)
			if(messageTokens[0].compareTo("bye") == 0)
			{	UUID clientID = UUID.fromString(messageTokens[1]);
				System.out.println("[CLIENT]: Exit request received from - " + clientID.toString());
				sendByeMessages(clientID);
				removeClient(clientID);

				readyStatus.remove(clientID); // remove disconnected player from ready map
			}
			
			// CREATE -- Case where server receives a create message (to specify avatar location)
			// Received Message Format: (create,localId,x,y,z)
			if(messageTokens[0].compareTo("create") == 0)
			{	UUID clientID = UUID.fromString(messageTokens[1]);
				String[] pos = {messageTokens[2], messageTokens[3], messageTokens[4]};
				System.out.println("[SERVER]: Creating Player " + clientID + " At X:" + pos[0] + " Y:" + pos[1] + " Z:" + pos[2]);
				sendCreateMessages(clientID, pos);
				sendWantsDetailsMessages(clientID);
			}
			
			// DETAILS-FOR --- Case where server receives a details for message
			// Received Message Format: (dsfr,remoteId,localId,x,y,z)
			if(messageTokens[0].compareTo("dsfr") == 0)
			{	UUID clientID = UUID.fromString(messageTokens[1]);
				UUID remoteID = UUID.fromString(messageTokens[2]);
				String[] pos = {messageTokens[3], messageTokens[4], messageTokens[5]};
				sendDetailsForMessage(clientID, remoteID, pos);
			}
			
			// MOVE --- Case where server receives a move message
			// Received Message Format: (move,localId,x,y,z)
			if(messageTokens[0].compareTo("move") == 0)
			{	UUID clientID = UUID.fromString(messageTokens[1]);
				String[] pos = {messageTokens[2], messageTokens[3], messageTokens[4]};
				System.out.println("THIS IS CAR POS: " + pos[0] + " " + pos[1] + " " + pos[2]);
				sendMoveMessages(clientID, pos);
			}

			// TURN --- Case where server receives a turn message
			// Received Message Format: (turn,localId,angle)
			if(messageTokens[0].compareTo("turn") == 0)
			{	UUID clientID = UUID.fromString(messageTokens[1]);
				String[] rot = {messageTokens[2]};
				sendTurnMessages(clientID, rot);
			}

			if(messageTokens[0].compareTo("confirm") == 0)
			{
				UUID clientID = UUID.fromString(messageTokens[1]);
				System.out.println("[CLIENT]: Confirmation from - " + clientID.toString());
			}


			// Case where server receives request for NPCs
			// Received Message Format: (needNPC,id)
			if(messageTokens[0].compareTo("needNPC") == 0)
			{ System.out.println("server got a needNPC message");
				UUID clientID = UUID.fromString(messageTokens[1]);
				sendNPCstart(clientID);
			}

			// Case where server receives notice that an av is close to the npc
			// Received Message Format: (isnear,id)
			if(messageTokens[0].compareTo("isnear") == 0)
			{ UUID clientID = UUID.fromString(messageTokens[1]);
				handleNearTiming(clientID);
			}

		}	}

	// NPC STUFF

	// --- additional protocol for NPCs ----
	public void sendCheckForAvatarNear()
	{
		try
		{
			String message = new String("isnr");
			message += "," + (npcCtrl.getNPC()).getX();
			message += "," + (npcCtrl.getNPC()).getY();
			message += "," + (npcCtrl.getNPC()).getZ();
			message += "," + (npcCtrl.getCriteria());
			sendPacketToAll(message);
		}
		catch (IOException e)
		{ System.out.println("couldnt send msg"); e.printStackTrace(); }
	}

	// get current avatar location
	public Vector3f sendAvatarLocation() {

		float x = Float.parseFloat(curPos[0]);
		float y = Float.parseFloat(curPos[1]);
		float z = Float.parseFloat(curPos[2]);

		return new Vector3f(x, y, z);
	}

	public void sendNPCinfo()
	{
		try {
			String message = "updateNPC";
			message += "," + npcCtrl.getNPC().getX();
			message += "," + npcCtrl.getNPC().getY();
			message += "," + npcCtrl.getNPC().getZ();

			sendPacketToAll(message);
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	public void sendNPCstart(UUID clientID) {

		try {
			String message = "createNPC," + clientID.toString() + "," + 6 + "," + 0 + "," + 2;
			sendPacket(message, clientID);
		} catch (IOException e) {
            throw new RuntimeException(e);
        }

    }

	// ------------ SENDING NPC MESSAGES -----------------
	// Informs clients of the whereabouts of the NPCs.
	public void sendCreateNPCmsg(String[] position)
	{
		try
		{
			System.out.println("server telling clients about an NPC");
			String message = new String("createNPC");
			message += "," + position[0];
			message += "," + position[1];
			message += "," + position[2];
			sendPacketToAll(message);
			System.out.println("printing this to client: " + message);

		} catch (IOException e) {
			e.printStackTrace();
		}

	}

	public void handleNearTiming(UUID clientID)
	{
		npcCtrl.setNearFlag(true);
	}


	// AVATAR STUFF


	// Informs the client who just requested to join the server if their
	// request was able to be granted. 
	// Message Format: (join,success) or (join,failure)
	
	public void sendJoinedMessage(UUID clientID, boolean success)
	{	try 
		{	String message = new String("join,");
			if(success) {
				System.out.println("[SERVER]: Confirming join...");
				System.out.println("[SERVER]: A player has joined the game!");
				message += "success";
				//Notify all players that a player has joined the game IN GAME
				sendPacketToAll("joinMsg");
			} else {
				System.out.println("[SERVER]: Could NOT confirm join.");
				message += "failure";
			}
			sendPacket(message, clientID);
		}
		catch (IOException e) 
		{	e.printStackTrace();
	}	}
	
	// Informs a client that the avatar with the identifier remoteId has left the server. 
	// This message is meant to be sent to all client currently connected to the server 
	// when a client leaves the server.
	// Message Format: (bye,remoteId)
	
	public void sendByeMessages(UUID clientID)
	{	try 
		{	String message = new String("bye," + clientID.toString());
			forwardPacketToAll(message, clientID);
			//Notify all players that a player has left the game IN GAME
			forwardPacketToAll("joinMsg", clientID);
		} 
		catch (IOException e) 
		{	e.printStackTrace();
	}	}

	public void startGameMessage()
	{	try
	{	String message = new String("start");
		sendPacketToAll(message);
		//Notify all players that game is starting
		sendPacketToAll("startMsg");
	}
	catch (IOException e)
	{	e.printStackTrace();
	}	}
	
	// Informs a client that a new avatar has joined the server with the unique identifier 
	// remoteId. This message is intended to be send to all clients currently connected to 
	// the server when a new client has joined the server and sent a create message to the 
	// server. This message also triggers WANTS_DETAILS messages to be sent to all client 
	// connected to the server. 
	// Message Format: (create,remoteId,x,y,z) where x, y, and z represent the position

	public void sendCreateMessages(UUID clientID, String[] position)
	{	try 
		{	String message = new String("create," + clientID.toString());
			message += "," + position[0];
			message += "," + position[1];
			message += "," + position[2];	
			forwardPacketToAll(message, clientID);
		} 
		catch (IOException e) 
		{	e.printStackTrace();
	}	}
	
	// Informs a client of the details for a remote clients avatar. This message is in response 
	// to the server receiving a DETAILS_FOR message from a remote client. That remote clients 
	// messages localId becomes the remoteId for this message, and the remote clients messages 
	// remoteId is used to send this message to the proper client. 
	// Message Format: (dsfr,remoteId,x,y,z) where x, y, and z represent the position.

	public void sendDetailsForMessage(UUID clientID, UUID remoteId, String[] position)
	{	try 
		{	String message = new String("dsfr," + remoteId.toString());
			message += "," + position[0];
			message += "," + position[1];
			message += "," + position[2];	
			sendPacket(message, clientID);
		} 
		catch (IOException e) 
		{	e.printStackTrace();
	}	}
	
	// Informs a local client that a remote client wants the local clients avatars information. 
	// This message is meant to be sent to all clients connected to the server when a new client 
	// joins the server. 
	// Message Format: (wsds,remoteId)
	
	public void sendWantsDetailsMessages(UUID clientID)
	{	try 
		{	String message = new String("wsds," + clientID.toString());	
			forwardPacketToAll(message, clientID);
		} 
		catch (IOException e) 
		{	e.printStackTrace();
	}	}
	
	// Informs a client that a remote clients avatar has changed position. x, y, and z represent 
	// the new position of the remote avatar. This message is meant to be forwarded to all clients
	// connected to the server when it receives a MOVE message from the remote client.   
	// Message Format: (move,remoteId,x,y,z) where x, y, and z represent the position.

	public void sendMoveMessages(UUID clientID, String[] position)
	{	try 
		{	String message = new String("move," + clientID.toString());
			message += "," + position[0];
			message += "," + position[1];
			message += "," + position[2];
			curPos[0] = position[0];
			curPos[1] = position[1];
			curPos[2] = position[2];
			System.out.println(curPos[0] + " " + curPos[1] + " " + curPos[2]);// store the coordinates of car in server memory
			forwardPacketToAll(message, clientID);
		} 
		catch (IOException e) 
		{	e.printStackTrace();
	}	}

	// Informs a client that a remote clients avatar has changed rotation.
	// This message is meant to be forwarded to all clients
	// connected to the server when it receives a TURN message from the remote client.
	// Message Format: (turn,remoteId,angle).
	public void sendTurnMessages(UUID clientID, String[] rotation)
	{	try
	{	String message = new String("turn," + clientID.toString());
		message += "," + rotation[0];
		forwardPacketToAll(message, clientID);
	}
	catch (IOException e)
	{	e.printStackTrace();
	}	}
}
