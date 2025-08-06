package es.um.redes.nanoChat.server;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.net.Socket;
import java.util.ArrayList;
import java.util.LinkedList;

import es.um.redes.nanoChat.messageML.NCChatMessage;
import es.um.redes.nanoChat.messageML.NCHistoryMessage;
import es.um.redes.nanoChat.messageML.NCListRoomsMessage;
import es.um.redes.nanoChat.messageML.NCMessage;
import es.um.redes.nanoChat.messageML.NCOperationCodeMessage;
import es.um.redes.nanoChat.messageML.NCRoomMessage;
import es.um.redes.nanoChat.server.roomManager.NCRoom;
import es.um.redes.nanoChat.server.roomManager.NCRoomDescription;
import es.um.redes.nanoChat.server.roomManager.NCRoomManager;

/**
 * A new thread runs for each connected client
 */
public class NCServerThread extends Thread {

	private Socket socket = null;
	// Manager global compartido entre los Threads
	private NCServerManager serverManager = null;
	// Input and Output Streams
	private DataInputStream dis;
	private DataOutputStream dos;
	// Usuario actual al que atiende este Thread
	String user;
	// RoomManager actual (dependerá de la sala a la que entre el usuario)
	NCRoomManager roomManager;
	// Sala actual
	String currentRoom;

	// Inicialización de la sala
	public NCServerThread(NCServerManager manager, Socket socket) throws IOException {
		super("NCServerThread");
		this.socket = socket;
		this.serverManager = manager;
	}

	// Main loop
	public void run() {
		try {
			// Se obtienen los streams a partir del Socket
			dis = new DataInputStream(socket.getInputStream());
			dos = new DataOutputStream(socket.getOutputStream());
			// En primer lugar hay que recibir y verificar el nick
			receiveAndVerifyNickname();
			// Mientras que la conexión esté activa entonces...
			while (true) {
				// TO Obtenemos el mensaje que llega y analizamos su código de operación
				NCMessage message = NCMessage.readMessageFromSocket(dis);
				switch (message.getOpcode()) {

				// TO 1) si se nos pide la lista de salas se envía llamando a sendRoomList();
				case NCMessage.OP_GET_ROOMS:
					sendRoomList();
					break;

				// TO 2) Si se nos pide entrar en la sala entonces obtenemos el RoomManager de
				// la sala,
				case NCMessage.OP_ENTER_ROOM:

					NCRoomMessage peticion = (NCRoomMessage) message;
					currentRoom = peticion.getName();
					roomManager = serverManager.enterRoom(user, currentRoom, socket);

					NCOperationCodeMessage respuesta;
					if (roomManager != null) {
						// TO 2.1) notificamos al usuario que ha sido aceptado y procesamos mensajes con
						// processRoomMessages()
						respuesta = NCMessage.makeOperationCodeMessage(NCMessage.OP_ENTER_ROOM_OK);
						dos.writeUTF(respuesta.toEncodedString());
						notifyEntry(user);
						processRoomMessages();
					} else {
						// TO 2.2) notificamos al usuario que NO ha sido aceptado
						respuesta = NCMessage.makeOperationCodeMessage(NCMessage.OP_ENTER_ROOM_FAIL);
						dos.writeUTF(respuesta.toEncodedString());
					}
					break;

				case NCMessage.OP_CREATE_ROOM:

					NCRoomMessage peticion1 = (NCRoomMessage) message;
					boolean isValido = serverManager.registerRoomManager(new NCRoom(), peticion1.getName());

					NCOperationCodeMessage respuesta1;
					if (isValido) {
						respuesta1 = NCMessage.makeOperationCodeMessage(NCMessage.OP_CREATE_ROOM_OK);
						dos.writeUTF(respuesta1.toEncodedString());
					} else {
						respuesta1 = NCMessage.makeOperationCodeMessage(NCMessage.OP_CREATE_ROOM_FAIL);
						dos.writeUTF(respuesta1.toEncodedString());
					}

					break;

				}
			}
		} catch (Exception e) {
			// If an error occurs with the communications the user is removed from all the
			// managers and the connection is closed
			System.out.println("* User " + user + " disconnected.");
			serverManager.leaveRoom(user, currentRoom);
			serverManager.removeUser(user);
		} finally {
			if (!socket.isClosed())
				try {
					socket.close();
				} catch (IOException e1) {
					e1.printStackTrace();
				}
		}
	}

	// Obtenemos el nick y solicitamos al ServerManager que verifique si está
	// duplicado
	private void receiveAndVerifyNickname() throws IOException {
		// La lógica de nuestro programa nos obliga a que haya un nick registrado antes
		// de proseguir

		// TO Entramos en un bucle hasta comprobar que alguno de los nicks
		// proporcionados no está duplicado
		boolean isRegistrado = false;
		while (!isRegistrado) {

			// TO Extraer el nick del mensaje
			NCRoomMessage mensaje = (NCRoomMessage) NCMessage.readMessageFromSocket(dis);

			// TO Validar el nick utilizando el ServerManager - addUser()
			// TO Contestar al cliente con el resultado (éxito o duplicado)
			if (serverManager.addUser(mensaje.getName())) {
				user = mensaje.getName();
				NCOperationCodeMessage respuesta = (NCOperationCodeMessage) NCMessage
						.makeOperationCodeMessage(NCMessage.OP_NICK_OK);
				dos.writeUTF(respuesta.toEncodedString());
				isRegistrado = true;
			} else {

				NCOperationCodeMessage respuesta = (NCOperationCodeMessage) NCMessage
						.makeOperationCodeMessage(NCMessage.OP_NICK_DUP);
				dos.writeUTF(respuesta.toEncodedString());
			}
		}
	}

	// Mandamos al cliente la lista de salas existentes
	private void sendRoomList() throws IOException {
		// TO La lista de salas debe obtenerse a partir del RoomManager y después
		// enviarse mediante su mensaje correspondiente
		ArrayList<NCRoomDescription> roomList = serverManager.getRoomList();
		NCListRoomsMessage respuesta = NCMessage.makeListRoomsMessage(NCMessage.OP_ROOM_LIST, roomList);
		dos.writeUTF(respuesta.toEncodedString());
	}

	private void processRoomMessages() throws IOException {

		// TO Comprobamos los mensajes que llegan hasta que el usuario decida salir de
		// la sala
		boolean exit = false;
		while (!exit) {

			// TO Se recibe el mensaje enviado por el usuario
			dis = new DataInputStream(socket.getInputStream());

			// TO Se analiza el código de operación del mensaje y se trata en consecuencia
			NCMessage peticion = NCMessage.readMessageFromSocket(dis);

			switch (peticion.getOpcode()) {
			case NCMessage.OP_EXIT_ROOM:
				NCRoomMessage peticion2 = (NCRoomMessage) peticion;
				currentRoom = peticion2.getName();
				serverManager.leaveRoom(user, currentRoom);
				notifyExit(user);
				exit = true;
				break;

			case NCMessage.OP_MESSAGE:
				NCChatMessage msg = (NCChatMessage) peticion;
				roomManager.broadcastMessage(user, msg.getMessage());
				break;

			case NCMessage.OP_GET_ROOM_INFO:
				NCRoomDescription roomDescription = serverManager.getRoomDescription(currentRoom);
				ArrayList<NCRoomDescription> listDescription = new ArrayList<>();
				listDescription.add(roomDescription);
				NCListRoomsMessage msg2 = NCMessage.makeListRoomsMessage(NCMessage.OP_ROOM_INFO, listDescription);
				dos.writeUTF(msg2.toEncodedString());
				break;

			case NCMessage.OP_GET_HISTORY:
				LinkedList<String> history = serverManager.getMessageHistory(currentRoom);
				NCHistoryMessage msg3 = NCMessage.makeHistoryMessage(NCMessage.OP_HISTORY, history);
				dos.writeUTF(msg3.toEncodedString());
				break;

			default:
				break;
			}
		}
	}

	// Para indicarle al room manager que notifique a los usuarios de la sala actual
	// que el usuario u se acaba de unir
	public void notifyEntry(String u) {
		try {
			roomManager.notifyEnterMessage(u);
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	// Para indicarle al room manager que notifique a los usuarios de la sala actual
	// que el usuario u se va a salir
	public void notifyExit(String u) {
		try {
			roomManager.notifyExitMessage(u);
		} catch (IOException e) {
			e.printStackTrace();
		}
	}
}
