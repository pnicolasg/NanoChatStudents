package es.um.redes.nanoChat.messageML;

/*

<message>
	<operation> OP_CODE </operation>
</message>

OP_CODE, podra ser:

OP_NICK_OK
OP_NICK_DUP
OP_GET_ROOMS
OP_ENTER_ROOM_OK
OP_GET_ROOM_INFO

*/

public class NCOperationCodeMessage extends NCMessage {

	public NCOperationCodeMessage(byte opcode) {
		this.opcode = opcode;
	}

	@Override
	public String toEncodedString() {

		StringBuffer stringBuffer = new StringBuffer();
		stringBuffer.append("<" + MESSAGE_MARK + ">" + END_LINE);
		stringBuffer.append("<" + OPERATION_MARK + ">");
		stringBuffer.append(opcodeToString(opcode));
		stringBuffer.append("</" + OPERATION_MARK + ">" + END_LINE);
		stringBuffer.append("</" + MESSAGE_MARK + ">" + END_LINE);

		return stringBuffer.toString();
	}

	// Parseamos el mensaje contenido en message con el fin de obtener los distintos
	// campos
	public static NCOperationCodeMessage readFromString(byte code) {
		return new NCOperationCodeMessage(code);
	}
}
