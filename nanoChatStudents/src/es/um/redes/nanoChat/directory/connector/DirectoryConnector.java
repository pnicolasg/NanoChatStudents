package es.um.redes.nanoChat.directory.connector;

import java.nio.ByteBuffer;
import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.UnknownHostException;

/**
 * Cliente con métodos de consulta y actualización específicos del directorio
 */
public class DirectoryConnector {
	// Tamaño máximo del paquete UDP (los mensajes intercambiados son muy cortos)
	private static final int PACKET_MAX_SIZE = 128;
	// Puerto en el que atienden los servidores de directorio
	private static final int DEFAULT_PORT = 6868;
	// Valor del TIMEOUT
	private static final int TIMEOUT = 1000;

	// OPCODE Mensajes:
	private static final byte OPCODE_REGISTRO_SERVER = 1;
	private static final byte OPCODE_OK_REGISTRO_SERVER = 2;
	private static final byte OPCODE_CONSULTA = 3;
	private static final byte OPCODE_DATOS_SERVER = 4;
	private static final byte OPCODE_SERVER_NOTFOUND = 5;

	private static final int SIZE_REGISTRO_SERVER = 6;
	private static final int SIZE_OK_REGISTRO_SERVER = 1;
	private static final int SIZE_CONSULTA = 2;

	private static final int MAX_INTENTOS = 5;

	private DatagramSocket socket; // socket UDP
	private InetSocketAddress directoryAddress; // dirección del servidor de directorio

	public DirectoryConnector(String agentAddress) throws IOException {
		// TO A partir de la dirección y del puerto generar la dirección de conexión
		// para el Socket
		directoryAddress = new InetSocketAddress(agentAddress, DEFAULT_PORT);

		// TO Crear el socket UDP
		socket = new DatagramSocket();
	}

	/**
	 * Envía una solicitud para obtener el servidor de chat asociado a un
	 * determinado protocolo
	 * 
	 */
	public InetSocketAddress getServerForProtocol(int protocol) throws IOException {

		// TO Generar el mensaje de consulta llamando a buildQuery()
		byte[] query = buildQuery(protocol);

		// TO Construir el datagrama con la consulta
		DatagramPacket datagrama = new DatagramPacket(query, query.length, directoryAddress);

		for (int i = 0; i < MAX_INTENTOS; i++) {

			// TO Enviar datagrama por el socket
			socket.send(datagrama);

			// TO preparar el buffer para la respuesta
			byte[] response = new byte[PACKET_MAX_SIZE];
			DatagramPacket datagramaRespuesta = new DatagramPacket(response, response.length);

			// TO Establecer el temporizador para el caso en que no haya respuesta
			socket.setSoTimeout(TIMEOUT);

			try {
				// TO Recibir la respuesta
				socket.receive(datagramaRespuesta);

				// TO Procesamos la respuesta para devolver la dirección que hay en ella
				return getAddressFromResponse(datagramaRespuesta);
			} catch (Exception e) {
				System.out.println("Expira el TIMEOUT");
			}
		}

		return null;
	}

	// Método para generar el mensaje de consulta (para obtener el servidor asociado
	// a un protocolo)
	private byte[] buildQuery(int protocol) {
		// TO Devolvemos el mensaje codificado en binario según el formato acordado
		ByteBuffer bb = ByteBuffer.allocate(SIZE_CONSULTA);
		bb.put(OPCODE_CONSULTA);
		bb.put((byte) protocol);
		return bb.array();
	}

	// Método para obtener la dirección de internet a partir del mensaje UDP de
	// respuesta
	private InetSocketAddress getAddressFromResponse(DatagramPacket packet) throws UnknownHostException {

		// TO Analizar si la respuesta no contiene dirección (devolver null)
		ByteBuffer respuesta = ByteBuffer.wrap(packet.getData());
		byte code = respuesta.get();

		if (code == OPCODE_SERVER_NOTFOUND)
			return null;

		// TO Si la respuesta no está vacía, devolver la dirección (extraerla del
		// mensaje)
		if (code == OPCODE_DATOS_SERVER) {
			byte[] direccion = new byte[4];
			respuesta.get(direccion);
			return new InetSocketAddress(InetAddress.getByAddress(direccion), respuesta.getInt());
		}

		return null;
	}

	/**
	 * Envía una solicitud para registrar el servidor de chat asociado a un
	 * determinado protocolo
	 * 
	 */
	public boolean registerServerForProtocol(int protocol, int port) throws IOException {

		// TO Construir solicitud de registro (buildRegistration)
		byte[] solicitud = buildRegistration(protocol, port);

		for (int i = 0; i < MAX_INTENTOS; i++) {
			// TO Enviar solicitud
			socket.send(new DatagramPacket(solicitud, solicitud.length, directoryAddress));

			socket.setSoTimeout(TIMEOUT);

			// TO Recibe respuesta
			ByteBuffer bb = ByteBuffer.allocate(SIZE_OK_REGISTRO_SERVER);
			byte[] respuesta = bb.array();
			DatagramPacket paqueteRespuesta = new DatagramPacket(respuesta, respuesta.length);

			try {

				socket.receive(paqueteRespuesta);

				// TO Procesamos la respuesta para ver si se ha podido registrar correctamente
				ByteBuffer bbr = ByteBuffer.wrap(paqueteRespuesta.getData());
				byte code = bbr.get();

				if (code == OPCODE_OK_REGISTRO_SERVER)
					return true;

			} catch (Exception e) {
				System.out.println("Expira el TIMEOUT");
			}
		}
		return false;
	}

	// Método para construir una solicitud de registro de servidor
	// OJO: No hace falta proporcionar la dirección porque se toma la misma desde la
	// que se envió el mensaje
	private byte[] buildRegistration(int protocol, int port) {
		// TO Devolvemos el mensaje codificado en binario según el formato acordado
		ByteBuffer bb = ByteBuffer.allocate(SIZE_REGISTRO_SERVER);
		bb.put(OPCODE_REGISTRO_SERVER);
		bb.putInt(port);
		bb.put((byte) protocol);
		return bb.array();
	}

	public void close() {
		socket.close();
	}
}
