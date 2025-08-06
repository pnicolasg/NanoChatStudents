package es.um.redes.nanoChat.directory.server;

import java.net.SocketException;

public class Directory {
	public static final int DIRECTORY_PORT = 6868;
	public static final double DEFAULT_CORRUPTION_PROBABILITY = 0.0;
	private static String DIRECTORY_IP = "localhost";

	public static void main(String[] args) {
		double datagramCorruptionProbability = DEFAULT_CORRUPTION_PROBABILITY;

		/**
		 * Command line argument to directory is optional, if not specified, default
		 * value is used: -loss: probability of corruption of received datagrams
		 */
		String arg1;

		// Analizamos si hay parámetro
		if (args.length == 4 && args[0].startsWith("-") && args[2].startsWith("-")) {
			arg1 = args[0];
			// Examinamos si es un parámetro válido
			if (arg1.equals("-loss")) {
				try {
					// El segundo argumento contiene la probabilidad de descarte
					datagramCorruptionProbability = Double.parseDouble(args[1]);
				} catch (NumberFormatException e) {
					System.err.println("Wrong value passed to option " + arg1);
					return;
				}
			} else {
				System.err.println("USO: -loss [probability(0.0)] -ip [dir.IP(localhost)]");
				return;
			}

			// Examinanos y leemos los parametros referentes a la IP
			if (args[2].equals("-ip")) {
				DIRECTORY_IP = args[3];
			} else {
				System.err.println("USO: -loss [probability(0.0)] -ip [dir.IP(localhost)]");
				return;
			}

		} else {
			System.err.println("USO: -loss [probability(0.0)] -ip [dir.IP(localhost)]");
			return;
		}

		System.out.println("Probability of corruption for received datagrams: " + datagramCorruptionProbability);
		System.out.println("IP Directory: " + DIRECTORY_IP + "  PORT Directory: " + DIRECTORY_PORT);

		DirectoryThread dt;
		try {
			dt = new DirectoryThread("Directory", DIRECTORY_IP, DIRECTORY_PORT, datagramCorruptionProbability);
			dt.start();
		} catch (SocketException e) {
			System.err.println("Directory cannot create UDP socket on port " + DIRECTORY_PORT);
			System.err.println("Most likely a Directory process is already running and listening on that port...");
			System.exit(-1);
		}
	}
}
