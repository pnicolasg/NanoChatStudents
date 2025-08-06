package es.um.redes.nanoChat.client.application;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.util.ArrayList;
import java.util.Date;
import java.util.LinkedList;

import es.um.redes.nanoChat.client.comm.NCConnector;
import es.um.redes.nanoChat.client.shell.NCCommands;
import es.um.redes.nanoChat.client.shell.NCShell;
import es.um.redes.nanoChat.directory.connector.DirectoryConnector;
import es.um.redes.nanoChat.messageML.NCChatMessage;
import es.um.redes.nanoChat.messageML.NCMessage;
import es.um.redes.nanoChat.messageML.NCRoomMessage;
import es.um.redes.nanoChat.server.roomManager.NCRoomDescription;

public class NCController {

	// Diferentes estados del cliente de acuerdo con el autómata
	private static final byte PRE_CONNECTION = 1;
	private static final byte PRE_REGISTRATION = 2;
	private static final byte REGISTRATION = 3;
	private static final byte IN_ROOM = 4;

	// Código de protocolo implementado por este cliente
	// TO Cambiar para cada grupo
	private static final int PROTOCOL = 72;

	// Conector para enviar y recibir mensajes del directorio
	private DirectoryConnector directoryConnector;
	// Conector para enviar y recibir mensajes con el servidor de NanoChat
	private NCConnector ncConnector;
	// Shell para leer comandos de usuario de la entrada estándar
	private NCShell shell;
	// Último comando proporcionado por el usuario
	private byte currentCommand;
	// Nick del usuario
	private String nickname;
	// Sala de chat en la que se encuentra el usuario (si está en alguna)
	private String room;
	// Mensaje enviado o por enviar al chat
	private String chatMessage;
	// Dirección de internet del servidor de NanoChat
	private InetSocketAddress serverAddress;
	// Estado actual del cliente, de acuerdo con el autómata
	private byte clientStatus = PRE_CONNECTION;
	// Nombre de la sala que quiere crear el usuario
	private String createRoomName;

	// Constructor
	public NCController() {
		shell = new NCShell();
	}

	// Devuelve el comando actual introducido por el usuario
	public byte getCurrentCommand() {
		return this.currentCommand;
	}

	// Establece el comando actual
	public void setCurrentCommand(byte command) {
		currentCommand = command;
	}

	// Registra en atributos internos los posibles parámetros del comando tecleado
	// por el usuario
	public void setCurrentCommandArguments(String[] args) {
		// Comprobaremos también si el comando es válido para el estado actual del
		// autómata
		switch (currentCommand) {
		case NCCommands.COM_NICK:
			if (clientStatus == PRE_REGISTRATION)
				nickname = args[0];
			break;
		case NCCommands.COM_ENTER:
			room = args[0].toLowerCase();
			break;
		case NCCommands.COM_CREATE:
			createRoomName = args[0].toLowerCase();
			break;
		case NCCommands.COM_SEND:
			chatMessage = args[0];
			break;
		default:
		}
	}

	// Procesa los comandos introducidos por un usuario que aún no está dentro de
	// una sala
	public void processCommand() {
		switch (currentCommand) {
		case NCCommands.COM_NICK:

			if (clientStatus == PRE_REGISTRATION)
				registerNickName();
			else
				System.out.println("* You have already registered a nickname (" + nickname + ")");
			break;

		case NCCommands.COM_ROOMLIST:
			// TO LLamar a getAndShowRooms() si el estado actual del autómata lo permite
			// TO Si no está permitido informar al usuario
			if (clientStatus == REGISTRATION)
				getAndShowRooms();
			else
				System.out.println("* You must be registered to get the list of rooms");
			break;

		case NCCommands.COM_ENTER:
			// TO LLamar a enterChat() si el estado actual del autómata lo permite
			// TO Si no está permitido informar al usuario
			if (clientStatus == REGISTRATION)
				enterChat();
			else
				System.out.println("* You must be registered to enter a room");
			break;

		case NCCommands.COM_CREATE:
			// TO LLamar a createRoom() si el estado actual del autómata lo permite
			// TO Si no está permitido informar al usuario
			if (clientStatus == REGISTRATION)
				createRoom();
			else
				System.out.println("* You must be registered to create a room");
			break;

		case NCCommands.COM_QUIT:
			// Cuando salimos tenemos que cerrar todas las conexiones y sockets abiertos
			ncConnector.disconnect();
			directoryConnector.close();
			break;
		default:
		}
	}

	// Método para registrar el nick del usuario en el servidor de NanoChat
	private void registerNickName() {
		try {
			// Pedimos que se registre el nick (se comprobará si está duplicado)
			boolean registered = ncConnector.registerNickname(nickname);
			// TO: Cambiar la llamada anterior a registerNickname() al usar mensajes
			// formateados
			if (registered) {
				// TO Si el registro fue exitoso pasamos al siguiente estado del autómata
				clientStatus = REGISTRATION;
				System.out.println("* Your nickname is now " + nickname);
			} else
				// En este caso el nick ya existía
				System.out.println("* The nickname is already registered. Try a different one.");
		} catch (IOException e) {
			System.out.println("* There was an error registering the nickname");
		}
	}

	// Método que solicita al servidor de NanoChat la lista de salas e imprime el
	// resultado obtenido
	private void getAndShowRooms() {
		// TO Lista que contendrá las descripciones de las salas existentes
		// TO Le pedimos al conector que obtenga la lista de salas
		// ncConnector.getRooms()
		ArrayList<NCRoomDescription> roomDescriptionList = new ArrayList<>();
		try {

			roomDescriptionList = (ArrayList<NCRoomDescription>) ncConnector.getRooms();
			// TO Una vez recibidas iteramos sobre la lista para imprimir información de
			// cada sala
			for (NCRoomDescription room : roomDescriptionList)
				System.out.println(room.toPrintableString());

		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	// Método para tramitar la solicitud de acceso del usuario a una sala concreta
	private void enterChat() {

		// TO Se solicita al servidor la entrada en la sala correspondiente
		// ncConnector.enterRoom()
		boolean isAccepted = false;
		try {
			isAccepted = ncConnector.enterRoom(room);
		} catch (IOException e) {
			e.printStackTrace();
		}

		if (!isAccepted) {
			System.out.println("* It wasn't possible to enter the room " + "'" + room + "'");
			return;
		}

		// TO En caso contrario informamos que estamos dentro y seguimos
		System.out.println("* You have joined the room " + "'" + room + "'");

		// TO Cambiamos el estado del autómata para aceptar nuevos comandos
		clientStatus = IN_ROOM;

		do {
			// Pasamos a aceptar sólo los comandos que son válidos dentro de una sala
			readRoomCommandFromShell();
			processRoomCommand();
		} while (currentCommand != NCCommands.COM_EXIT);
		System.out.println("* Your are out of the room");

		// TO Llegados a este punto el usuario ha querido salir de la sala, cambiamos
		// el estado del autómata
		clientStatus = REGISTRATION;
	}

	// Método para solicitar al servidor la creación de una sala
	private void createRoom() {

		// TO Se solicita al servidor la creación de la sala correspondiente
		boolean isAccepted = false;
		try {
			isAccepted = ncConnector.createRoom(createRoomName);
		} catch (IOException e) {
			e.printStackTrace();
		}

		if (!isAccepted) {
			System.out.println("* It wasn't possible create the room " + "'" + createRoomName + "'");
			return;
		}

		// TO En caso contrario informamos que estamos dentro y seguimos
		System.out.println("* You have created the room " + "'" + createRoomName + "'");
	}

	// Método para procesar los comandos específicos de una sala
	private void processRoomCommand() {
		switch (currentCommand) {
		case NCCommands.COM_ROOMINFO:
			// El usuario ha solicitado información sobre la sala y llamamos al método que
			// la obtendrá
			getAndShowInfo();
			break;
		case NCCommands.COM_SEND:
			// El usuario quiere enviar un mensaje al chat de la sala
			sendChatMessage();
			break;
		case NCCommands.COM_SOCKET_IN:
			// En este caso lo que ha sucedido es que hemos recibido un mensaje desde la
			// sala y hay que procesarlo
			processIncommingMessage();
			break;
		case NCCommands.COM_EXIT:
			// El usuario quiere salir de la sala
			if (clientStatus == IN_ROOM)
				exitTheRoom();
			break;
		case NCCommands.COM_HISTORY:
			// El usuario ha solicitado el historial de la sala y llamamos al método que lo
			// obtendrá
			getAndShowHistory();
			break;
		}
	}

	// Método para solicitar al servidor la información sobre una sala y para
	// mostrarla por pantalla
	private void getAndShowInfo() {
		try {

			// TO Pedimos al servidor información sobre la sala en concreto
			NCRoomDescription roomDescription = ncConnector.getRoomInfo(room);

			// TO Mostramos por pantalla la información
			System.out.println(roomDescription.toPrintableString());

		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	// Método para solicitar al servidor la el historial de mensajes de una sala y
	// para mostrarlo por pantalla
	private void getAndShowHistory() {
		try {

			// TO Pedimos al servidor información sobre la sala en concreto
			LinkedList<String> history = ncConnector.getHistory(room);

			if (!history.isEmpty()) {
				System.out.println("* HISTORY:");
				// TO Mostramos por pantalla la información
				for (String message : history)
					System.out.println(message);
			} else
				System.out.println("* No message has been sent in this room yet");

		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	// Método para notificar al servidor que salimos de la sala
	private void exitTheRoom() {
		// TO Mandamos al servidor el mensaje de salida
		try {
			ncConnector.leaveRoom(room);
		} catch (IOException e) {
			e.printStackTrace();
		}

		// TO Cambiamos el estado del autómata para indicar que estamos fuera de la
		// sala
		clientStatus = REGISTRATION;
	}

	// Método para enviar un mensaje al chat de la sala
	private void sendChatMessage() {
		// TO Mandamos al servidor un mensaje de chat
		ncConnector.sendChatMessage(nickname, chatMessage);
	}

	// Método para procesar los mensajes recibidos del servidor mientras que el
	// shell estaba esperando un comando de usuario
	private void processIncommingMessage() {
		// TO Recibir el mensaje
		NCMessage message = ncConnector.receiveChatMessage();

		// TO En función del tipo de mensaje, actuar en consecuencia
		if (message.getOpcode() == NCMessage.OP_MESSAGE) {

			// TO (Ejemplo) En el caso de que fuera un mensaje de chat de broadcast
			// mostramos la información de quién envía el mensaje y el mensaje en sí
			NCChatMessage msg = (NCChatMessage) message;
			System.out.println("[" + new Date(msg.getTime()) + "] " + msg.getNick() + ": " + msg.getMessage());
		}

		// En el caso de que el mensaje que nos llegue sea para notidicar una entrada en
		// la sala:
		if (message.getOpcode() == NCMessage.OP_NOTIFY_ENTER) {
			NCRoomMessage msg = (NCRoomMessage) message;
			System.out.println("* User '" + msg.getName() + "' joined the room");
		}

		// En el caso de que el mensaje que nos llegue sea para notidicar una salida en
		// la sala:
		if (message.getOpcode() == NCMessage.OP_NOTIFY_EXIT) {
			NCRoomMessage msg = (NCRoomMessage) message;
			System.out.println("* User '" + msg.getName() + "' left the room");
		}
	}

	// MNétodo para leer un comando de la sala
	public void readRoomCommandFromShell() {
		// Pedimos un nuevo comando de sala al shell (pasando el conector por si nos
		// llega un mensaje entrante)
		shell.readChatCommand(ncConnector);
		// Establecemos el comando tecleado (o el mensaje recibido) como comando actual
		setCurrentCommand(shell.getCommand());
		// Procesamos los posibles parámetros (si los hubiera)
		setCurrentCommandArguments(shell.getCommandArguments());
	}

	// Método para leer un comando general (fuera de una sala)
	public void readGeneralCommandFromShell() {
		// Pedimos el comando al shell
		shell.readGeneralCommand();
		// Establecemos que el comando actual es el que ha obtenido el shell
		setCurrentCommand(shell.getCommand());
		// Analizamos los posibles parámetros asociados al comando
		setCurrentCommandArguments(shell.getCommandArguments());
	}

	// Método para obtener el servidor de NanoChat que nos proporcione el directorio
	public boolean getServerFromDirectory(String directoryHostname) {
		// Inicializamos el conector con el directorio y el shell
		System.out.println("* Connecting to the directory...");
		// Intentamos obtener la dirección del servidor de NanoChat que trabaja con
		// nuestro protocolo
		try {
			directoryConnector = new DirectoryConnector(directoryHostname);
			serverAddress = directoryConnector.getServerForProtocol(PROTOCOL);
		} catch (IOException e1) {
			// Auto-generated catch block
			serverAddress = null;
		}
		// Si no hemos recibido la dirección entonces nos quedan menos intentos
		if (serverAddress == null) {
			System.out.println("* Check your connection, the directory is not available.");
			return false;
		} else
			return true;
	}

	// Método para establecer la conexión con el servidor de Chat (a través del
	// NCConnector)
	public boolean connectToChatServer() {
		try {
			// Inicializamos el conector para intercambiar mensajes con el servidor de
			// NanoChat (lo hace la clase NCConnector)
			ncConnector = new NCConnector(serverAddress);
		} catch (IOException e) {
			System.out.println("* Check your connection, the game server is not available.");
			serverAddress = null;
		}
		// Si la conexión se ha establecido con éxito informamos al usuario y cambiamos
		// el estado del autómata
		if (serverAddress != null) {
			System.out.println("* Connected to " + serverAddress);
			clientStatus = PRE_REGISTRATION;
			return true;
		} else
			return false;
	}

	// Método que comprueba si el usuario ha introducido el comando para salir de la
	// aplicación
	public boolean shouldQuit() {
		return currentCommand == NCCommands.COM_QUIT;
	}
}
