package es.um.redes.nanoChat.server.roomManager;

import java.io.IOException;
import java.net.Socket;
import java.util.LinkedList;

public abstract class NCRoomManager {

	String roomName;
	LinkedList<String> history = new LinkedList<>();

	// Método para registrar a un usuario u en una sala (se anota también su socket
	// de comunicación)
	public abstract boolean registerUser(String u, Socket s);

	// Método para hacer llegar un mensaje enviado por un usuario u
	public abstract void broadcastMessage(String u, String message) throws IOException;

	// Método para eliminar un usuario de una sala
	public abstract void removeUser(String u);

	// Método para nombrar una sala
	public abstract void setRoomName(String roomName);

	// Método para devolver la descripción del estado actual de la sala
	public abstract NCRoomDescription getDescription();

	// Método para devolver el número de usuarios conectados a una sala
	public abstract int usersInRoom();

	// Método para notificar a los usuarios de una sala que el usuario 'u' se unió
	public abstract void notifyEnterMessage(String u) throws IOException;

	// Método para notificar a los usuarios de una sala que el usuario 'u' salió
	public abstract void notifyExitMessage(String u) throws IOException;

	// Metodo que añade el mensage 'message en el historial de la sala'
	public void addMessageHistory(String message) {
		history.add(message);
	}

	// Metodo que devuelve el historial de mensajes de la sala
	public LinkedList<String> getMessageHistory() {
		return new LinkedList<>(history);
	}
}
