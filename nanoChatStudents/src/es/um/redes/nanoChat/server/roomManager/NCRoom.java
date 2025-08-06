package es.um.redes.nanoChat.server.roomManager;

import java.io.DataOutputStream;
import java.io.IOException;
import java.net.Socket;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Date;
import es.um.redes.nanoChat.messageML.NCChatMessage;
import es.um.redes.nanoChat.messageML.NCMessage;
import es.um.redes.nanoChat.messageML.NCRoomMessage;

public class NCRoom extends NCRoomManager {

	// Atributos:
	// Mapa de usuarios con sus correspondientes sockets
	private HashMap<String, Socket> users = new HashMap<>();
	private long timeLastMessage = 0;

	// Constructor:
	public NCRoom() {

	}

	// Implementación métodos abstractos
	@Override
	public boolean registerUser(String u, Socket s) {

		if (s != null && u != null) {
			users.put(u, s);
			return true;
		}

		return false;
	}

	@Override
	public void broadcastMessage(String u, String message) throws IOException {

		timeLastMessage = new Date().getTime();
		for (String user : users.keySet())
			if (user != u) // Enviamos a todos menos al remitente
			{
				NCChatMessage msg = (NCChatMessage) NCMessage.makeChatMessage(NCMessage.OP_MESSAGE, u, message,
						timeLastMessage);
				DataOutputStream dos = new DataOutputStream(users.get(user).getOutputStream());
				dos.writeUTF(msg.toEncodedString());
			}

		addMessageHistory("[" + new Date(timeLastMessage) + "] " + u + ": " + message);
	}

	@Override
	public void removeUser(String u) {
		users.remove(u);
	}

	@Override
	public void setRoomName(String roomName) {
		this.roomName = roomName;
	}

	@Override
	public NCRoomDescription getDescription() {
		ArrayList<String> usuarios = new ArrayList<>();
		usuarios.addAll(users.keySet());
		return new NCRoomDescription(roomName, usuarios, timeLastMessage);
	}

	@Override
	public int usersInRoom() {
		return users.keySet().size();
	}

	@Override
	public void notifyEnterMessage(String u) throws IOException {

		for (String user : users.keySet())
			if (user != u) {
				NCRoomMessage msg = (NCRoomMessage) NCMessage.makeRoomMessage(NCMessage.OP_NOTIFY_ENTER, u);
				DataOutputStream dos = new DataOutputStream(users.get(user).getOutputStream());
				dos.writeUTF(msg.toEncodedString());
			}

		addMessageHistory("* User '" + u + "' joined the room");
	}

	@Override
	public void notifyExitMessage(String u) throws IOException {

		for (String user : users.keySet())
			if (user != u) {
				NCRoomMessage msg = (NCRoomMessage) NCMessage.makeRoomMessage(NCMessage.OP_NOTIFY_EXIT, u);
				DataOutputStream dos = new DataOutputStream(users.get(user).getOutputStream());
				dos.writeUTF(msg.toEncodedString());
			}

		addMessageHistory("* User '" + u + "' left the room");
	}
}