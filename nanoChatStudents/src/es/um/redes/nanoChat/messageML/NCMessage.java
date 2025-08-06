package es.um.redes.nanoChat.messageML;

import java.io.DataInputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.LinkedList;
import java.util.Map;
import java.util.TreeMap;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import es.um.redes.nanoChat.server.roomManager.NCRoomDescription;

public abstract class NCMessage {

	protected byte opcode;

	// TO IMPLEMENTAR TODAS LAS CONSTANTES RELACIONADAS CON LOS CODIGOS DE
	// OPERACION
	public static final byte OP_INVALID_CODE = 0;
	public static final byte OP_NICK = 1;
	public static final byte OP_NICK_OK = 2;
	public static final byte OP_NICK_DUP = 3;
	public static final byte OP_ROOM_LIST = 4;
	public static final byte OP_GET_ROOMS = 5;
	public static final byte OP_ENTER_ROOM = 6;
	public static final byte OP_ENTER_ROOM_OK = 7;
	public static final byte OP_EXIT_ROOM = 8;
	public static final byte OP_GET_ROOM_INFO = 9;
	public static final byte OP_ROOM_INFO = 10;
	public static final byte OP_MESSAGE = 11;
	public static final byte OP_NOTIFY_ENTER = 12;
	public static final byte OP_NOTIFY_EXIT = 13;
	public static final byte OP_HISTORY = 14;
	public static final byte OP_GET_HISTORY = 15;
	public static final byte OP_ENTER_ROOM_FAIL = 16;
	public static final byte OP_CREATE_ROOM = 17;
	public static final byte OP_CREATE_ROOM_OK = 18;
	public static final byte OP_CREATE_ROOM_FAIL = 19;

	public static final char DELIMITER = ':'; // Define el delimitador
	public static final char END_LINE = '\n'; // Define el carácter de fin de línea

	public static final String OPERATION_MARK = "operation";
	public static final String MESSAGE_MARK = "message";

	/**
	 * Códigos de los opcodes válidos El orden es importante para relacionarlos con
	 * la cadena que aparece en los mensajes
	 */
	private static final Byte[] _valid_opcodes = { OP_INVALID_CODE, OP_NICK, OP_NICK_OK, OP_NICK_DUP, OP_ROOM_LIST,
			OP_GET_ROOMS, OP_ENTER_ROOM, OP_ENTER_ROOM_OK, OP_EXIT_ROOM, OP_GET_ROOM_INFO, OP_ROOM_INFO, OP_MESSAGE,
			OP_NOTIFY_ENTER, OP_NOTIFY_EXIT, OP_HISTORY, OP_GET_HISTORY, OP_ENTER_ROOM_FAIL, OP_CREATE_ROOM,
			OP_CREATE_ROOM_OK, OP_CREATE_ROOM_FAIL };

	/**
	 * cadena exacta de cada orden
	 */
	private static final String[] _valid_operations_str = { "ERROR", "Nick", "Nick Correcto", "Nick Duplicado",
			"Room List", "Get Rooms", "Enter Room", "Enter Room OK", "Exit Room", "Get Room Info", "Room Info",
			"Message", "Notify Enter", "Notify Exit", "History", "Get History", "Enter Room Fail", "Create Room",
			"Create Room OK", "Create Room Fail" };

	private static Map<String, Byte> _operation_to_opcode;
	private static Map<Byte, String> _opcode_to_operation;

	static {
		_operation_to_opcode = new TreeMap<>();
		_opcode_to_operation = new TreeMap<>();
		for (int i = 0; i < _valid_operations_str.length; ++i) {
			_operation_to_opcode.put(_valid_operations_str[i].toLowerCase(), _valid_opcodes[i]);
			_opcode_to_operation.put(_valid_opcodes[i], _valid_operations_str[i]);
		}
	}

	/**
	 * Transforma una cadena en el opcode correspondiente
	 */
	protected static byte stringToOpcode(String opStr) {
		return _operation_to_opcode.getOrDefault(opStr.toLowerCase(), OP_INVALID_CODE);
	}

	/**
	 * Transforma un opcode en la cadena correspondiente
	 */
	protected static String opcodeToString(byte opcode) {
		return _opcode_to_operation.getOrDefault(opcode, null);
	}

	// Devuelve el opcode del mensaje
	public byte getOpcode() {
		return opcode;
	}

	// Método que debe ser implementado por cada subclase de NCMessage
	protected abstract String toEncodedString();

	// Analiza la operación de cada mensaje y usa el método readFromString() de cada
	// subclase para parsear
	public static NCMessage readMessageFromSocket(DataInputStream dis) throws IOException {
		String message = dis.readUTF();
		String regexpr = "<" + MESSAGE_MARK + ">(.*?)</" + MESSAGE_MARK + ">";
		Pattern pat = Pattern.compile(regexpr, Pattern.DOTALL);
		Matcher mat = pat.matcher(message);
		if (!mat.find()) {
			System.out.println("Mensaje mal formado:\n" + message);
			return null;
			// Message not found
		}
		String inner_msg = mat.group(1); // extraemos el mensaje

		String regexpr1 = "<" + OPERATION_MARK + ">(.*?)</" + OPERATION_MARK + ">";
		Pattern pat1 = Pattern.compile(regexpr1);
		Matcher mat1 = pat1.matcher(inner_msg);
		if (!mat1.find()) {
			System.out.println("Mensaje mal formado:\n" + message);
			return null;
			// Operation not found
		}
		String operation = mat1.group(1); // extraemos la operación

		byte code = stringToOpcode(operation);
		if (code == OP_INVALID_CODE)
			return null;

		switch (code) {
		// TO Parsear el resto de mensajes
		case OP_NICK: {
			return NCRoomMessage.readFromString(code, message);
		}
		case OP_NICK_DUP: {
			return NCOperationCodeMessage.readFromString(code);
		}
		case OP_NICK_OK: {
			return NCOperationCodeMessage.readFromString(code);
		}
		case OP_GET_ROOMS: {
			return NCOperationCodeMessage.readFromString(code);
		}
		case OP_ROOM_LIST: {
			return NCListRoomsMessage.readFromString(code, message);
		}
		case OP_ENTER_ROOM: {
			return NCRoomMessage.readFromString(code, message);
		}
		case OP_ENTER_ROOM_OK: {
			return NCOperationCodeMessage.readFromString(code);
		}
		case OP_EXIT_ROOM: {
			return NCRoomMessage.readFromString(code, message);
		}
		case OP_MESSAGE: {
			return NCChatMessage.readFromString(code, message);
		}
		case OP_GET_ROOM_INFO: {
			return NCOperationCodeMessage.readFromString(code);
		}
		case OP_ROOM_INFO: {
			return NCListRoomsMessage.readFromString(code, message);
		}
		case OP_NOTIFY_ENTER: {
			return NCRoomMessage.readFromString(code, message);
		}
		case OP_NOTIFY_EXIT: {
			return NCRoomMessage.readFromString(code, message);
		}
		case OP_GET_HISTORY: {
			return NCOperationCodeMessage.readFromString(code);
		}
		case OP_HISTORY: {
			return NCHistoryMessage.readFromString(code, message);
		}
		case OP_ENTER_ROOM_FAIL: {
			return NCOperationCodeMessage.readFromString(code);
		}
		case OP_CREATE_ROOM: {
			return NCRoomMessage.readFromString(code, message);
		}
		case OP_CREATE_ROOM_OK: {
			return NCOperationCodeMessage.readFromString(code);
		}
		case OP_CREATE_ROOM_FAIL: {
			return NCOperationCodeMessage.readFromString(code);
		}
		default:
			System.err.println("Unknown message type received:" + code);
			return null;
		}
	}

	// TO Programar el resto de métodos para crear otros tipos de mensajes
	public static NCMessage makeRoomMessage(byte code, String room) {
		return new NCRoomMessage(code, room);
	}

	public static NCOperationCodeMessage makeOperationCodeMessage(byte code) {
		return new NCOperationCodeMessage(code);
	}

	public static NCListRoomsMessage makeListRoomsMessage(byte code, ArrayList<NCRoomDescription> roomList) {
		return new NCListRoomsMessage(code, roomList);
	}

	public static NCChatMessage makeChatMessage(byte code, String nickname, String message, long time) {
		return new NCChatMessage(code, nickname, message, time);
	}

	public static NCHistoryMessage makeHistoryMessage(byte code, LinkedList<String> history) {
		return new NCHistoryMessage(code, history);
	}
}
