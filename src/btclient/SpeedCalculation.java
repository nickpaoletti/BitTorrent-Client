package btclient;
/**
 * TrackerAnnounce.java
 * 
 * Code taken of how to run code in Intervals from http://stackoverflow.com/questions/426758/running-a-java-thread-in-intervals
 *
 */
public class SpeedCalculation implements Runnable {
	@Override
	public void run() {
		while(RUBTClient.keepRunning){
			try {
				
				FileManager.downloadSpeed = (FileManager.downloaded - FileManager.oldDownloaded)/2;
				FileManager.oldDownloaded = FileManager.downloaded;
				
				FileManager.uploadSpeed = (FileManager.uploaded - FileManager.oldUploaded)/2;
				FileManager.oldUploaded = FileManager.uploaded;
				
				RUBTClient.tv.content.setDownloadSpeed(FileManager.downloadSpeed/1024 + "KB/s");
				
				System.out.println("Download speed: " + FileManager.downloadSpeed + " Upload Speed: " + FileManager.uploadSpeed);
				Thread.sleep(2000);
			} catch (InterruptedException e) {
				//Do nothing. Just don't do another announce.
			}
		}		
	}
}