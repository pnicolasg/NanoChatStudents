package es.um.redes.nanoChat.server;

import java.net.Socket;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.Map;
import java.util.Set;

import es.um.redes.nanoChat.server.roomManager.NCRoom;
import es.um.redes.nanoChat.server.roomManager.NCRoomDescription;
import es.um.redes.nanoChat.server.roomManager.NCRoomManager;

/**
 * Esta clase contiene el estado general del servidor (sin la lógica relacionada
 * con cada sala particular)
 */
class NCServerManager {

	// Usuarios registrados en el servidor
	private Set<String> users = new HashSet<String>();
	// Habitaciones actuales asociadas a sus correspondientes RoomManagers
	private Map<String, NCRoomManager> rooms = new HashMap<String, NCRoomManager>();

	NCServerManager() {
		// Aqui siempre va a devolver 'true'
		registerRoomManager(new NCRoom(), "room_a");
	}

	// Método para registrar un RoomManager
	public boolean registerRoomManager(NCRoomManager rm, String roomName) {

		// TO Dar soporte para que pueda haber más de una sala en el servidor
		// Si la sala ya existe no se puede crear
		if (rooms.containsKey(roomName))
			return false;

		// Creamos la sala nueva
		rooms.put(roomName, rm);
		rm.setRoomName(roomName);
		return true;
	}

	// Devuelve la descripción de las salas existentes
	public synchronized ArrayList<NCRoomDescription> getRoomList() {

		ArrayList<NCRoomDescription> roomList = new ArrayList<>();

		// TO Pregunta a cada RoomManager cuál es la descripción actual de su sala
		// TO Añade la información al ArrayList
		for (String room : rooms.keySet())
			roomList.add(rooms.get(room).getDescription());

		return roomList;
	}

	// Devuelve la descripción de las salas existentes
	public synchronized NCRoomDescription getRoomDescription(String room) {
		return rooms.get(room).getDescription();
	}

	// Devuelve el historial de la sala que se le pasa como parametro
	public synchronized LinkedList<String> getMessageHistory(String room) {
		return rooms.get(room).getMessageHistory();
	}

	// Intenta registrar al usuario en el servidor.
	public synchronized boolean addUser(String user) {
		// TO Devuelve true si no hay otro usuario con su nombre
		// TO Devuelve false si ya hay un usuario con su nombre
		if (users.contains(user))
			return false;
		else {
			users.add(user);
			return true;
		}
	}

	// Elimina al usuario del servidor
	public synchronized void removeUser(String user) {
		// TO Elimina al usuario del servidor
		users.remove(user);
	}

	// Un usuario solicita acceso para entrar a una sala y registrar su conexión en
	// ella
	public synchronized NCRoomManager enterRoom(String u, String roomName, Socket s) {

		// TO Verificamos si la sala existe
		if (rooms.containsKey(roomName)) {
			// TO Si la sala existe y si es aceptado en la sala entonces devolvemos el
			// RoomManager de la sala
			rooms.get(roomName).registerUser(u, s);
			return rooms.get(roomName);
		}

		// TO Decidimos qué hacer si la sala no existe (devolver error O crear la sala)
		// Devolvemos 'null' si la sala no existe
		return null;
	}

	// Un usuario deja la sala en la que estaba
	public synchronized void leaveRoom(String u, String room) {
		// TO Verificamos si la sala existe
		if (rooms.containsKey(room)) {

			// TO Si la sala existe sacamos al usuario de la sala
			rooms.get(room).removeUser(u);

			// TO Decidir qué hacer si la sala se queda vacía || Borramos la sala tambien
			if (rooms.get(room).usersInRoom() == 0)
				rooms.remove(room);
		}
	}
}
