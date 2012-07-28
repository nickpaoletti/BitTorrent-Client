package btclient;
import java.io.*;
import java.util.logging.ConsoleHandler;
import java.util.logging.Handler;
import java.util.logging.Level;
import java.util.logging.LogManager;
import java.util.logging.Logger;

/* RUBTClient.java
 * 
 * by Nick Paoletti and Daniel Selmon
 * 
 * This is the main class. It takes in file arguments, converts them into a TrackerInfo object,
 * and downloads the file (with the name specified in the arguments) using that object and the
 * download class.
 */
class RUBTClient {

	private static final Level LOG_LEVEL = Level.ALL;

	
	static {
		// Load the logger configuration file.
		
		InputStream logStream = RUBTClient.class.getClassLoader()
				.getResourceAsStream("logging.properties");
		
		// Read it into the log manager
		try {
			LogManager.getLogManager().readConfiguration(logStream);
		} catch (SecurityException e1) {
			// TODO Auto-generated catch block
			e1.printStackTrace();
		} catch (IOException e1) {
			// TODO Auto-generated catch block
			e1.printStackTrace();
		}

		// Set the log level as desired for the root level.
		Logger rootLogger = Logger.getLogger("");
		replaceConsoleHandler(rootLogger, LOG_LEVEL);
	}
	
	private static final Logger log = Logger.getLogger(RUBTClient.class
			.getName());
//	static {
//		log.setLevel(Level.ALL);
//	}

	public static void main(String[] args) {
		
	
		Metadata data = null;
		TrackerInfo tracker = null;
		if (args.length != 2) {
			// Quit if program arguments are incorrect.
			System.out
					.println("Correct Usage: java RUBTClient [torretfile].torrent [outputfilename]");
			return;
		}
		try {
			// Convert the .torrent file into a TrackerInfo object, and download
			// the file using
			// the given tracker info.
			data = new Metadata(new File(args[0]));
			File f = new File(args[1]);
			//Code obtained partially from here: http://www.java2s.com/Code/Java/File-Input-Output/Appendingdatatoexistingfile.htm
			FileManager.file = new RandomAccessFile(f, "rw");
			if (f.exists()){
				
				//READ IN DATA FROM STUFF.
				//CHECK IF FULL, ETC.
			}
			
			tracker = data.httpGetRequest();
			FileManager.tracker = tracker;
			FileManager.data = data;
			
			for (int i = 0; i < FileManager.approvedPeers.size(); i++){
				Download peer = new Download();
				new Thread(peer).start();
			}
			
			FileManager.storeFileProgress(args[0]);
		} catch (BencodingException e) {
			// Throw exception in the case of Bencoding issue
			System.out.println(e.toString());
			return;
		} catch (IOException e) {
			// Throw exception in the case of IO issues
			System.out.println(e.toString());
			return;
		} catch (Exception e) {
			// In general or other case exceptions, throw an exception.
			System.out.println("Unknown Exception");
			e.printStackTrace();
			return;
		}
	}

	/*
	 * Rob's code from the Java Programming Sakai site.
	 * https://sakai.rutgers.edu/portal/site/e07619c5-a492-
	 * 4ebe-8771-179dfe450ae4/page/0a7200cf-0538-479a-a197-8d398c438484
	 */

	/**
	 * Replaces the ConsoleHandler for a specific Logger with one that will log
	 * all messages. This method could be adapted to replace other types of
	 * loggers if desired.
	 * 
	 * @param logger
	 *            the logger to update.
	 * @param newLevel
	 *            the new level to log.
	 */
	public static void replaceConsoleHandler(Logger logger, Level newLevel) {

		// Handler for console (reuse it if it already exists)
		Handler consoleHandler = null;
		// see if there is already a console handler
		for (Handler handler : logger.getHandlers()) {
			if (handler instanceof ConsoleHandler) {
				// found the console handler
				consoleHandler = handler;
				break;
			}
		}

		if (consoleHandler == null) {
			// there was no console handler found, create a new one
			consoleHandler = new ConsoleHandler();
			logger.addHandler(consoleHandler);
		}
		// set the console handler to fine:
		consoleHandler.setLevel(newLevel);
	}
	/*
	 * End Rob's code.
	 */
}
