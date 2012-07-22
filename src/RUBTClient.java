import java.io.*;

/* RUBTClient.java
 * 
 * by Nick Paoletti and Daniel Selmon
 * 
 * This is the main class. It takes in file arguments, converts them into a TrackerInfo object,
 * and downloads the file (with the name specified in the arguments) using that object and the
 * download class.
 */
class RUBTClient{	
	public static void main(String[] args) {
		Metadata data = null;
		TrackerInfo tracker = null;
		if (args.length != 2) {
			//Quit if program arguments are incorrect.
			System.out.println("Correct Usage: java RUBTClient [torretfile].torrent [outputfilename]");
			return;
		}
		try {
			//Convert the .torrent file into a TrackerInfo object, and download the file using
			//the given tracker info.
			data = new Metadata(new File(args[0]));
			tracker = data.httpGetRequest();
			Download.downloadFile(tracker, data);
			tracker.makeImage(args[1]);
		} catch (BencodingException e) {
			//Throw exception in the case of Bencoding issue
			System.out.println(e.toString());
			return;
		} catch (IOException e) {
			//Throw exception in the case of IO issues
			System.out.println(e.toString());
			return;
		} catch (Exception e) {
			//In general or other case exceptions, throw an exception.
			System.out.println("Unknown Exception");
			e.printStackTrace();
			return;
		}
	}
}

