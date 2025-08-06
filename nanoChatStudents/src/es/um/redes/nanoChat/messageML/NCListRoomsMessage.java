package es.um.redes.nanoChat.messageML;

import java.util.ArrayList;
import java.util.regex.*;

import es.um.redes.nanoChat.server.roomManager.NCRoomDescription;

/*

<message>
	<operation>roomlist</operation>
	<RoomList>
		<Room>
			<RoomName>RoomA</RoomName>
			<Nick>Usuario1</Nick>
			<Nick>Usuario2</Nick>
			<Nick>Usuario3</Nick>
			<Time>123456789</Time>
		</Room>
		<Room>
			...
		</Room>
	</RoomList>
</message>

*/

public class NCListRoomsMessage extends NCMessage {

	public static final String ROOM_LIST_MARK = "RoomList";
	public static final String ROOM_MARK = "Room";
	public static final String ROOM_NAME_MARK = "RoomName";
	public static final String NICK_MARK = "Nick";
	public static final String TIME_MARK = "Time";

	public static final String ER_ROOM_LIST = "<RoomList>(.*?)</RoomList>";
	public static final String ER_ROOM = "<Room>(.*?)</Room>";
	public static final String ER_ROOM_NAME = "<RoomName>(.*?)</RoomName>";
	public static final String ER_NICK = "<Nick>(.*?)</Nick>";
	public static final String ER_TIME = "<Time>(.*?)</Time>";

	private ArrayList<NCRoomDescription> roomList;

	public NCListRoomsMessage(byte code, ArrayList<NCRoomDescription> roomList) {
		this.opcode = code;
		this.roomList = new ArrayList<>(roomList);
	}

	@Override
	public String toEncodedString() {

		StringBuffer stringBuffer = new StringBuffer();
		stringBuffer.append("<" + MESSAGE_MARK + ">" + END_LINE);
		stringBuffer.append("<" + OPERATION_MARK + ">");
		stringBuffer.append(opcodeToString(opcode));
		stringBuffer.append("</" + OPERATION_MARK + ">" + END_LINE);
		stringBuffer.append("<" + ROOM_LIST_MARK + ">" + END_LINE);
		for (NCRoomDescription room : roomList) {
			stringBuffer.append("<" + ROOM_MARK + ">" + END_LINE);
			stringBuffer.append("<" + ROOM_NAME_MARK + ">");
			stringBuffer.append(room.roomName);
			stringBuffer.append("</" + ROOM_NAME_MARK + ">" + END_LINE);
			for (String user : room.members) {
				stringBuffer.append("<" + NICK_MARK + ">");
				stringBuffer.append(user);
				stringBuffer.append("</" + NICK_MARK + ">" + END_LINE);
			}
			stringBuffer.append("<" + TIME_MARK + ">");
			stringBuffer.append(room.timeLastMessage);
			stringBuffer.append("</" + TIME_MARK + ">" + END_LINE);
			stringBuffer.append("</" + ROOM_MARK + ">" + END_LINE);
		}
		stringBuffer.append("</" + ROOM_LIST_MARK + ">" + END_LINE);
		stringBuffer.append("</" + MESSAGE_MARK + ">" + END_LINE);

		return stringBuffer.toString();
	}

	public static NCListRoomsMessage readFromString(byte code, String message) {

		String roomName = null;
		long time = 0;
		ArrayList<NCRoomDescription> roomDescriptionList = new ArrayList<>();

		Pattern pattern_lista_rooms = Pattern.compile(ER_ROOM_LIST, Pattern.DOTALL);
		Matcher matcher_lista_rooms = pattern_lista_rooms.matcher(message);

		if (!matcher_lista_rooms.find()) {
			System.out.println("Mensaje mal formado:\n" + message);
			return null;
		}

		String subcadena1 = matcher_lista_rooms.group(1);
		Pattern pattern_rooms = Pattern.compile(ER_ROOM, Pattern.DOTALL);
		Matcher matcher_rooms = pattern_rooms.matcher(subcadena1);

		while (matcher_rooms.find()) {

			// Extraemos el nombre de la sala
			String subcadena2 = matcher_rooms.group(1);
			Pattern pattern_room_name = Pattern.compile(ER_ROOM_NAME);
			Matcher matcher_room_name = pattern_room_name.matcher(subcadena2);
			if (!matcher_room_name.find()) {
				System.out.println("Mensaje mal formado:\n" + message);
				return null;
			}
			roomName = matcher_room_name.group(1);

			// Extraemos los usuarios de la sala
			ArrayList<String> usersList = new ArrayList<>();
			Pattern pattern_nick = Pattern.compile(ER_NICK);
			Matcher matcher_nick = pattern_nick.matcher(subcadena2);
			while (matcher_nick.find())
				usersList.add(matcher_nick.group(1));

			// Extraemos la hora del ultimo mensaje
			Pattern pattern_time = Pattern.compile(ER_TIME);
			Matcher matcher_time = pattern_time.matcher(subcadena2);
			if (!matcher_time.find()) {
				System.out.println("Mensaje mal formado:\n" + message);
				return null;
			}
			time = Long.parseLong(matcher_time.group(1));

			// Añadimos la nueva sala con su descripcion a la lista
			roomDescriptionList.add(new NCRoomDescription(roomName, usersList, time));
		}

		// Devolvemos la lista de las descripciones de las salas que habian en el msj
		return new NCListRoomsMessage(code, roomDescriptionList);
	}

	public ArrayList<NCRoomDescription> getRoomDescriptionList() {
		return new ArrayList<>(roomList);
	}

	// Para probar los metodos de esta clase
	public static void main(String[] args) {
		ArrayList<String> users = new ArrayList<>();
		users.add("juan");
		users.add("pepe");
		users.add("luis");
		ArrayList<NCRoomDescription> roomlist = new ArrayList<>();
		roomlist.add(new NCRoomDescription("RoomA", users, 34434));
		ArrayList<String> users2 = new ArrayList<>();
		users2.add("pedro");
		users2.add("maria");
		roomlist.add(new NCRoomDescription("RoomB", users2, 896745));
		NCListRoomsMessage msg = new NCListRoomsMessage(OP_GET_ROOMS, roomlist);
		System.out.println(msg.toEncodedString());
		System.out.println();
		NCListRoomsMessage msg2 = readFromString(NCMessage.OP_GET_ROOMS, msg.toEncodedString());
		System.out.println(msg2.toEncodedString());
	}
}
