package es.um.redes.nanoChat.messageML;

import java.util.regex.*;

/*
<message>
	<operation>Message</operation>
	<Nick>Usuario1</Nick>
	<Text>Este es el contenido del mensaje</Text>
	<Time>123456789</Time>
</message>
*/

public class NCChatMessage extends NCMessage {

	public static final String NICK_MARK = "Nick";
	public static final String TEXT_MARK = "Text";
	public static final String TIME_MARK = "Time";

	public static final String ER_NICK = "<Nick>(.*?)</Nick>";
	public static final String ER_TEXT = "<Text>(.*?)</Text>";
	public static final String ER_TIME = "<Time>(.*?)</Time>";

	// Atributos:
	private String nick;
	private String message;
	private long time;

	public NCChatMessage(byte opcode, String nick, String message, long time) {
		this.nick = nick;
		this.message = message;
		this.time = time;
		this.opcode = opcode;
	}

	@Override
	public String toEncodedString() {
		
		StringBuffer stringBuffer = new StringBuffer();
		stringBuffer.append("<" + MESSAGE_MARK + ">" + END_LINE);
		stringBuffer.append("<" + OPERATION_MARK + ">" + opcodeToString(opcode));
		stringBuffer.append("</" + OPERATION_MARK + ">" + END_LINE);
		stringBuffer.append("<" + NICK_MARK + ">" + nick + "</" + NICK_MARK + ">" + END_LINE);
		stringBuffer.append("<" + TEXT_MARK + ">" + message + "</" + TEXT_MARK + ">" + END_LINE);
		stringBuffer.append("<" + TIME_MARK + ">" + time + "</" + TIME_MARK + ">" + END_LINE);
		stringBuffer.append("</" + MESSAGE_MARK + ">");
		
		return stringBuffer.toString();
	}

	// Parseamos el mensaje contenido en message con el fin de obtener los distintos
	// campos
	public static NCChatMessage readFromString(byte code, String message) {
		String nick;
		String content;
		long time;

		Pattern pattern_nick = Pattern.compile(ER_NICK);
		Matcher matcher_nick = pattern_nick.matcher(message);
		if (!matcher_nick.find()) {
			System.out.println("Mensaje mal formado:\n" + message);
			return null;
		}

		nick = matcher_nick.group(1);

		Pattern pattern_text = Pattern.compile(ER_TEXT);
		Matcher matcher_text = pattern_text.matcher(message);
		if (!matcher_text.find()) {
			System.out.println("Mensaje mal formado:\n" + message);
			return null;
		}

		content = matcher_text.group(1);

		Pattern pattern_time = Pattern.compile(ER_TIME);
		Matcher matcher_time = pattern_time.matcher(message);
		if (!matcher_time.find()) {
			System.out.println("Mensaje mal formado:\n" + message);
			return null;
		}

		time = Long.parseLong(matcher_time.group(1));

		return new NCChatMessage(code, nick, content, time);
	}

	public String getNick() {
		return nick;
	}

	public String getMessage() {
		return message;
	}

	public long getTime() {
		return time;
	}

	// Para probar los metodos de esta clase
	public static void main(String[] args) {
		NCChatMessage msj = new NCChatMessage(OP_MESSAGE, "Pedro", "Este es el mensaje", 56445656);
		System.out.println(msj.toEncodedString());
		System.out.println();
		String mensaje = msj.toEncodedString();
		NCChatMessage msj2 = NCChatMessage.readFromString(OP_MESSAGE, mensaje);
		System.out.println(msj2.toEncodedString());
	}
}
