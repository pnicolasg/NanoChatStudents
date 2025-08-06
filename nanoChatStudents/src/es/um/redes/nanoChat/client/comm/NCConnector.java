package es.um.redes.nanoChat.client.comm;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.LinkedList;

import es.um.redes.nanoChat.messageML.NCChatMessage;
import es.um.redes.nanoChat.messageML.NCHistoryMessage;
import es.um.redes.nanoChat.messageML.NCListRoomsMessage;
import es.um.redes.nanoChat.messageML.NCMessage;
import es.um.redes.nanoChat.messageML.NCOperationCodeMessage;
import es.um.redes.nanoChat.messageML.NCRoomMessage;
import es.um.redes.nanoChat.server.roomManager.NCRoomDescription;

//Esta clase proporciona la funcionalidad necesaria para intercambiar mensajes entre el cliente y el servidor de NanoChat
public class NCConnector {
	private Socket socket;
	protected DataOutputStream dos;
	protected DataInputStream dis;

	public NCConnector(InetSocketAddress serverAddress) throws UnknownHostException, IOException {

		// TO Se crea el socket a partir de la dirección proporcionada
		this.socket = new Socket(serverAddress.getAddress(), serverAddress.getPort());

		// TO Se extraen los streams de entrada y salida
		this.dos = new DataOutputStream(socket.getOutputStream());
		this.dis = new DataInputStream(socket.getInputStream());
	}

	// Método para registrar el nick en el servidor. Nos informa sobre si la
	// inscripción se hizo con éxito o no.
	public boolean registerNickname(String nick) throws IOException {
		// Funcionamiento resumido: SEND(nick) and RCV(NICK_OK) or RCV(NICK_DUPLICATED)
		// Creamos un mensaje de tipo RoomMessage con opcode OP_NICK en el que se
		// inserte el nick
		NCRoomMessage message = (NCRoomMessage) NCMessage.makeRoomMessage(NCMessage.OP_NICK, nick);
		// Obtenemos el mensaje de texto listo para enviar
		String rawMessage = message.toEncodedString();
		// Escribimos el mensaje en el flujo de salida, es decir, provocamos que se
		// envíe por la conexión TCP
		dos.writeUTF(rawMessage);

		// TO Leemos el mensaje recibido como respuesta por el flujo de entrada
		NCOperationCodeMessage respuesta = (NCOperationCodeMessage) NCMessage.readMessageFromSocket(dis);

		// TO Analizamos el mensaje para saber si está duplicado el nick (modificar el
		// return en consecuencia)
		if (respuesta.getOpcode() == NCMessage.OP_NICK_OK)
			return true;
		else
			return false;
	}

	// Método para obtener la lista de salas del servidor
	public ArrayList<NCRoomDescription> getRooms() throws IOException {
		// Funcionamiento resumido: SND(GET_ROOMS) and RCV(ROOM_LIST)
		// TO completar el método
		NCOperationCodeMessage peticion = (NCOperationCodeMessage) NCMessage
				.makeOperationCodeMessage(NCMessage.OP_GET_ROOMS);
		dos.writeUTF(peticion.toEncodedString());

		NCListRoomsMessage respuesta = (NCListRoomsMessage) NCMessage.readMessageFromSocket(dis);
		return respuesta.getRoomDescriptionList();
	}

	// Método para solicitar la entrada en una sala
	public boolean enterRoom(String room) throws IOException {
		// Funcionamiento resumido: SND(ENTER_ROOM<room>) and RCV(IN_ROOM) or
		// RCV(REJECT)
		NCRoomMessage peticion = (NCRoomMessage) NCMessage.makeRoomMessage(NCMessage.OP_ENTER_ROOM, room);
		dos.writeUTF(peticion.toEncodedString());

		NCOperationCodeMessage respuesta = (NCOperationCodeMessage) NCMessage.readMessageFromSocket(dis);
		if (respuesta.getOpcode() == NCMessage.OP_ENTER_ROOM_OK)
			return true;

		if (respuesta.getOpcode() == NCMessage.OP_ENTER_ROOM_FAIL)
			return false;

		return false;
	}

	// Método para solicitar la creación de una sala
	public boolean createRoom(String roomName) throws IOException {
		// Funcionamiento resumido: SND(CREATE_ROOM<room>)
		// and RCV(OP_CREATE_ROOM_OK) or RCV(OP_CREATE_ROOM_FAIL)

		NCRoomMessage peticion = (NCRoomMessage) NCMessage.makeRoomMessage(NCMessage.OP_CREATE_ROOM, roomName);
		dos.writeUTF(peticion.toEncodedString());

		NCOperationCodeMessage respuesta = (NCOperationCodeMessage) NCMessage.readMessageFromSocket(dis);
		if (respuesta.getOpcode() == NCMessage.OP_CREATE_ROOM_OK)
			return true;

		if (respuesta.getOpcode() == NCMessage.OP_CREATE_ROOM_FAIL)
			return false;

		return false;
	}

	// Método para salir de una sala
	public void leaveRoom(String room) throws IOException {
		// Funcionamiento resumido: SND(EXIT_ROOM)
		// TO completar el método
		NCRoomMessage peticion = (NCRoomMessage) NCMessage.makeRoomMessage(NCMessage.OP_EXIT_ROOM, room);
		dos.writeUTF(peticion.toEncodedString());
	}

	// Método que utiliza el Shell para ver si hay datos en el flujo de entrada
	public boolean isDataAvailable() throws IOException {
		return (dis.available() != 0);
	}

	// IMPORTANTE!!
	// TO Es necesario implementar métodos para recibir y enviar mensajes de chat
	// a una sala
	// Método para enviar un mensaje al chat de la sala
	public void sendChatMessage(String nickname, String chatMessage) {
		// TO Mandamos al servidor un mensaje de chat
		NCChatMessage msg = (NCChatMessage) NCMessage.makeChatMessage(NCMessage.OP_MESSAGE, nickname, chatMessage, 0);
		try {
			dos.writeUTF(msg.toEncodedString());
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	// Método para recibir un mensaje al chat de la sala
	public NCMessage receiveChatMessage() {
		try {
			return (NCMessage) NCMessage.readMessageFromSocket(dis);
		} catch (IOException e) {
			e.printStackTrace();
		}

		return null;
	}

	// Método para pedir la descripción de una sala
	public NCRoomDescription getRoomInfo(String room) throws IOException {
		// Funcionamiento resumido: SND(GET_ROOMINFO) and RCV(ROOMINFO)

		// No hace falta enviarle el nombre de la sala al servidor, puesto que ya sabe
		// en que sala estamos y este comando solo permite solicitar la informacion de
		// la sala en la que nos encontramos actualmente

		// TO Construimos el mensaje de solicitud de información de la sala específica
		NCOperationCodeMessage peticion = NCMessage.makeOperationCodeMessage(NCMessage.OP_GET_ROOM_INFO);
		dos.writeUTF(peticion.toEncodedString());

		// TO Recibimos el mensaje de respuesta
		NCListRoomsMessage respuesta = (NCListRoomsMessage) NCMessage.readMessageFromSocket(dis);

		// TO Devolvemos la descripción contenida en el mensaje
		if (respuesta.getRoomDescriptionList().size() != 1)
			return null;

		return respuesta.getRoomDescriptionList().get(0);
	}

	// Método para pedir el historial de una sala
	public LinkedList<String> getHistory(String room) throws IOException {
		// Funcionamiento resumido: SND(GET_HISTORY) and RCV(HISTORY)

		// No hace falta enviarle el nombre de la sala al servidor, puesto que ya sabe
		// en que sala estamos y este comando solo permite solicitar el historial de
		// la sala en la que nos encontramos actualmente

		// TO Construimos el mensaje de solicitud de información de la sala específica
		NCOperationCodeMessage peticion = NCMessage.makeOperationCodeMessage(NCMessage.OP_GET_HISTORY);
		dos.writeUTF(peticion.toEncodedString());

		// TO Recibimos el mensaje de respuesta
		NCHistoryMessage respuesta = (NCHistoryMessage) NCMessage.readMessageFromSocket(dis);

		return new LinkedList<>(respuesta.getHistory());
	}

	// Método para cerrar la comunicación con la sala
	// TO (Opcional) Enviar un mensaje de salida del servidor de Chat
	public void disconnect() {
		try {
			if (socket != null) {
				socket.close();
			}
		} catch (IOException e) {
		} finally {
			socket = null;
		}
	}
}
