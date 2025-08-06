package es.um.redes.nanoChat.directory.server;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetSocketAddress;
import java.net.SocketException;
import java.nio.ByteBuffer;
import java.util.HashMap;
import java.util.Map;

public class DirectoryThread extends Thread {

	// OPCODE Mensajes:
	private static final byte OPCODE_REGISTRO_SERVER = 1;
	private static final byte OPCODE_OK_REGISTRO_SERVER = 2;
	private static final byte OPCODE_CONSULTA = 3;
	private static final byte OPCODE_DATOS_SERVER = 4;
	private static final byte OPCODE_SERVER_NOTFOUND = 5;

	private static final int SIZE_OK_REGISTRO_SERVER = 1;
	private static final int SIZE_DATOS_SERVER = 9;
	private static final int SIZE_SERVER_NOTFOUND = 1;

	// Tamaño máximo del paquete UDP
	private static final int PACKET_MAX_SIZE = 128;

	// Estructura para guardar las asociaciones ID_PROTOCOLO -> Dirección del
	// servidor
	protected Map<Integer, InetSocketAddress> servers;

	// Socket de comunicación UDP
	protected DatagramSocket socket = null;

	// Probabilidad de descarte del mensaje
	protected double messageDiscardProbability;

	public DirectoryThread(String name, String directoryIP, int directoryPort, double corruptionProbability)
			throws SocketException {
		super(name);
		// TO Anotar la dirección en la que escucha el servidor de Directorio
		InetSocketAddress serverAddress = new InetSocketAddress(directoryIP, directoryPort);

		// TO Crear un socket de servidor
		socket = new DatagramSocket(serverAddress);

		messageDiscardProbability = corruptionProbability;

		// Inicialización del mapa
		servers = new HashMap<Integer, InetSocketAddress>();
	}

	@Override
	public void run() {
		byte[] buf = new byte[PACKET_MAX_SIZE];

		System.out.println("Directory starting...");
		boolean running = true;
		while (running) {

			// TO 1) Recibir la solicitud por el socket
			DatagramPacket datagrama = new DatagramPacket(buf, buf.length);
			try {
				socket.receive(datagrama);
			} catch (Exception e) {
			}

			// TO 2) Extraer quién es el cliente (su dirección)
			InetSocketAddress clientAddress = (InetSocketAddress) datagrama.getSocketAddress();

			// 3) Vemos si el mensaje debe ser descartado por la probabilidad de descarte

			double rand = Math.random();
			if (rand < messageDiscardProbability) {
				System.err.println("Directory DISCARDED corrupt request from... ");
				continue;
			}

			// TO 4) Analizar y procesar la solicitud (llamada a processRequestFromCLient)
			// TO 5) Tratar las excepciones que puedan producirse
			try {
				processRequestFromClient(datagrama.getData(), clientAddress);
			} catch (Exception e) {
				System.err.println("ERROR al procesar la solicitud de un cliente.");
			}
		}
		socket.close();
	}

	// Método para procesar la solicitud enviada por clientAddr
	public void processRequestFromClient(byte[] data, InetSocketAddress clientAddr) throws IOException {
		// TO 1) Extraemos el tipo de mensaje recibido
		ByteBuffer bb = ByteBuffer.wrap(data);
		byte code = bb.get();

		// TO 2) Procesar el caso de que sea un registro y enviar mediante sendOK
		if (code == OPCODE_REGISTRO_SERVER) {
			int port = bb.getInt();
			byte protocol = bb.get();

			// Creamos la direccion
			InetSocketAddress address = new InetSocketAddress(clientAddr.getAddress(), port);

			// Añadimos el nuevo servidor al Mapa
			servers.put((int) protocol, address);

			// Enviamos el mensaje de confirmacion de registro
			sendOK(clientAddr);
		}

		// TO 3) Procesar el caso de que sea una consulta
		// TO 3.1) Devolver una dirección si existe un servidor (sendServerInfo)
		// TO 3.2) Devolver una notificación si no existe un servidor (sendEmpty)
		if (code == OPCODE_CONSULTA) {

			byte protocol = bb.get();

			if (servers.containsKey((int) protocol))
				sendServerInfo(servers.get((int) protocol), clientAddr);
			else
				sendEmpty(clientAddr);
		}
	}

	// Método para enviar una respuesta vacía (no hay servidor)
	private void sendEmpty(InetSocketAddress clientAddr) throws IOException {
		// TO Construir respuesta
		ByteBuffer bb = ByteBuffer.allocate(SIZE_SERVER_NOTFOUND);
		bb.put(OPCODE_SERVER_NOTFOUND);
		byte[] respuesta = bb.array();

		// TO Enviar respuesta
		socket.send(new DatagramPacket(respuesta, respuesta.length, clientAddr));
	}

	// Método para enviar la dirección del servidor al cliente
	private void sendServerInfo(InetSocketAddress serverAddress, InetSocketAddress clientAddr) throws IOException {
		// TO Obtener la representación binaria de la dirección
		byte[] address = serverAddress.getAddress().getAddress();
		int port = serverAddress.getPort();

		// TO Construir respuesta
		ByteBuffer bb = ByteBuffer.allocate(SIZE_DATOS_SERVER);
		bb.put(OPCODE_DATOS_SERVER);
		bb.put(address);
		bb.putInt(port);
		byte[] respuesta = bb.array();

		// TO Enviar respuesta
		socket.send(new DatagramPacket(respuesta, respuesta.length, clientAddr));
	}

	// Método para enviar la confirmación del registro
	private void sendOK(InetSocketAddress clientAddr) throws IOException {
		// TO Construir respuesta
		ByteBuffer bb = ByteBuffer.allocate(SIZE_OK_REGISTRO_SERVER);
		bb.put(OPCODE_OK_REGISTRO_SERVER);
		byte[] respuesta = bb.array();

		// TO Enviar respuesta
		socket.send(new DatagramPacket(respuesta, respuesta.length, clientAddr));
	}
}
