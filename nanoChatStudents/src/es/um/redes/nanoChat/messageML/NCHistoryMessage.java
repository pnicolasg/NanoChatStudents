package es.um.redes.nanoChat.messageML;

import java.util.LinkedList;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/*
<message>
	<operation>History</operation>
	<Text>Este es el contenido del mensaje 1</Text>
	<Text>Este es el contenido del mensaje 2</Text>
	<Text>Este es el contenido del mensaje 3</Text>
	...
	<Text>Este es el contenido del mensaje n</Text>
</message>
*/

public class NCHistoryMessage extends NCMessage {

	public static final String TEXT_MARK = "Text";
	public static final String ER_TEXT = "<Text>(.*?)</Text>";

	// Atributos
	LinkedList<String> history = new LinkedList<>();

	// Constructor
	public NCHistoryMessage(byte opcode, LinkedList<String> history) {
		this.opcode = opcode;
		this.history = history;
	}

	@Override
	public String toEncodedString() {
		StringBuffer stringBuffer = new StringBuffer();
		stringBuffer.append("<" + MESSAGE_MARK + ">" + END_LINE);
		stringBuffer.append("<" + OPERATION_MARK + ">");
		stringBuffer.append(opcodeToString(opcode));
		stringBuffer.append("</" + OPERATION_MARK + ">" + END_LINE);
		for (String message : history) {
			stringBuffer.append("<" + TEXT_MARK + ">");
			stringBuffer.append(message);
			stringBuffer.append("</" + TEXT_MARK + ">" + END_LINE);
		}
		stringBuffer.append("</" + MESSAGE_MARK + ">" + END_LINE);
		return stringBuffer.toString();
	}

	public static NCHistoryMessage readFromString(byte code, String message) {

		LinkedList<String> historial = new LinkedList<>();

		Pattern pattern_text = Pattern.compile(ER_TEXT);
		Matcher matcher_text = pattern_text.matcher(message);
		while (matcher_text.find())
			historial.add(matcher_text.group(1));

		return new NCHistoryMessage(code, historial);
	}

	public LinkedList<String> getHistory() {
		return new LinkedList<>(history);
	}

	public static void main(String[] args) {

		LinkedList<String> historial = new LinkedList<>();
		historial.add("Hola");
		historial.add("Que tal?");
		historial.add("Bien");
		historial.add("Adios");
		NCHistoryMessage msg = new NCHistoryMessage(OP_HISTORY, historial);
		System.out.println(msg.toEncodedString());
		System.out.println(msg.getHistory());
		System.out.println();
		NCHistoryMessage msg2 = NCHistoryMessage.readFromString(OP_HISTORY, msg.toEncodedString());
		System.out.println(msg2.toEncodedString());
		System.out.println(msg2.getHistory());
	}
}
