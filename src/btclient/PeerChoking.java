package btclient;

import java.io.IOException;

import btclient.message.Message;

/**
 * TrackerAnnounce.java
 * 
 * Code taken of how to run code in Intervals from http://stackoverflow.com/questions/426758/running-a-java-thread-in-intervals
 *
 */
public class PeerChoking implements Runnable {
	@Override
	public void run() {
		while(RUBTClient.keepRunning){
			try {
				Peer topPeers[] = new Peer[FileManager.maxUnchokedPeers];
				
				for (int i = 0; i < FileManager.approvedPeers.size(); i++){
					FileManager.approvedPeers.get(i).setDownloadRate(30);
					FileManager.approvedPeers.get(i).setUploadRate(30);
					
					
					int worstPeerIndex = -1;
					double worstPeerRate = Double.MAX_VALUE;
					
					if (i < topPeers.length){
						topPeers[i] = (FileManager.approvedPeers.get(i));
						
						if (FileManager.fileComplete == false){
							if (FileManager.approvedPeers.get(i).getUploadRate() < worstPeerRate){
								worstPeerIndex = i;
								worstPeerRate = FileManager.approvedPeers.get(i).getUploadRate();
							}
						}
						else if (FileManager.fileComplete == true){
							if (FileManager.approvedPeers.get(i).getDownloadRate() < worstPeerRate){
								worstPeerIndex = i;
								worstPeerRate = FileManager.approvedPeers.get(i).getDownloadRate();
							}
						}
						
					}
					else {
						if (FileManager.fileComplete == false){
							if (FileManager.approvedPeers.get(i).getUploadRate() < worstPeerRate){
								topPeers[worstPeerIndex] = FileManager.approvedPeers.get(i);
								
								for (int j = 0; j < topPeers.length; j++){
									worstPeerRate = Double.MAX_VALUE;
									if (topPeers[j].getUploadRate() < worstPeerRate){
										worstPeerIndex = j;
										worstPeerRate = topPeers[j].getUploadRate();
									}
								}
							}	
						}
						else if (FileManager.fileComplete == true){
							if (FileManager.approvedPeers.get(i).getDownloadRate() < worstPeerRate){
								topPeers[worstPeerIndex] = FileManager.approvedPeers.get(i);
								
								for (int j = 0; j < topPeers.length; j++){
									worstPeerRate = Double.MAX_VALUE;
									if (topPeers[j].getDownloadRate() < worstPeerRate){
										worstPeerIndex = j;
										worstPeerRate = topPeers[j].getDownloadRate();
									}
								}
							}	
						}
					}
				}
				
				boolean[] topStatus = new boolean[FileManager.approvedPeers.size()];
				
				for (int i = 0; i < topPeers.length; i++){
					topStatus[FileManager.approvedPeers.indexOf(topPeers[i])] = true;
				}
				
				for (int i = 0 ; i < FileManager.approvedPeers.size(); i ++ ){
					Peer tempPeer = FileManager.approvedPeers.get(i);
					
					tempPeer.setUploaded(0);
					
					if (topStatus[i] == true){
						tempPeer.sendMessage(Message.UNCHOKE);
						tempPeer.setChokeStatus(1);
					}
					else {
						tempPeer.sendMessage(Message.CHOKE);
						tempPeer.setChokeStatus(0);
					}
				}
				
				//Wait 30 seconds
				Thread.sleep(30*1000);
			}catch (InterruptedException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			} catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		}		
	}
}