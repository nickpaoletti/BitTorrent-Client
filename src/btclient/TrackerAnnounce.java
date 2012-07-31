package btclient;

public class TrackerAnnounce implements Runnable {

	//Code taken of how to run code in Intervals from http://stackoverflow.com/questions/426758/running-a-java-thread-in-intervals
	
	@Override
	public void run() {
		while(RUBTClient.keepRunning){
			try {
				FileManager.tracker.makeURL(FileManager.info, "started");
				Thread.sleep(FileManager.tracker.getInterval() * 1000);
			} catch (InterruptedException e) {
				//Do nothing. Just don't do another announce.
			}
		}
		
	}

}
