package btclient.gui;

import java.awt.BorderLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.util.Random;
import javax.swing.ImageIcon;
import javax.swing.JFrame;
import javax.swing.JMenu;
import javax.swing.JMenuBar;
import javax.swing.JMenuItem;
import javax.swing.JOptionPane;

public class TorrentView extends JFrame{
	private static final long serialVersionUID = 2160729035270663944L;
	private static final String aboutText = "About Text goes here!";
	public TorrentViewContent content;
	private boolean[] chunky;
	private Thread t;
	private JMenuBar menu;
	private JMenuItem open;
	private JMenuItem about;
	private JMenu file;
	private JMenu help;
	private JMenuItem quit;
	public TorrentView(boolean[] chunks){
		super("Bitorrent Client");
		file = new JMenu("File");
		help = new JMenu("Help");
		open = new JMenuItem("Open");
		about = new JMenuItem("About");
		quit = new JMenuItem("Exit");
		menu = new JMenuBar();
		file.add(open);
		file.add(quit);
		help.add(about);
		menu.add(file);
		menu.add(help);
		/**
		 * ActionListener for the menu quit option.
		 */
		quit.addActionListener(new ActionListener(){
			public void actionPerformed(ActionEvent e) {
				// TODO link gracefully exit code here.
				System.out.println("Exit event heard, (Menu Exit)");
	        	System.exit(0);
			}
		});
		/**
		 * ActionListener for the menu open file option.
		 */
		open.addActionListener(new ActionListener(){
			public void actionPerformed(ActionEvent e){
				// TODO Auto-generated place file choose dialog here.
				
				JOptionPane.showInputDialog("Enter a path to download to:");
			}
		});
		/**
		 * ActionListener for the About menu option.
		 */
		about.addActionListener(new ActionListener(){

			@Override
			public void actionPerformed(ActionEvent arg0) {
				//JOptionPane.showMessageDialog(TorrentView.this,aboutText);
				JOptionPane.showMessageDialog(TorrentView.this, aboutText, "Title", JOptionPane.INFORMATION_MESSAGE, new ImageIcon("icon.jpg"));
				
			}
			
		});
		chunky = chunks;
		setDefaultCloseOperation(JFrame.DO_NOTHING_ON_CLOSE);
	    
		addWindowListener(new WindowAdapter() {
	    	 
	        public void windowClosing(WindowEvent e) {
	        	// TODO Link gracefully exit method here.
	        	System.out.println("Exit event heard, (X was clicked)");
	        	System.exit(0);
	        }
	    });
		
		
		
		setSize(700,600);
		setLocationRelativeTo(null);
		setContent(new TorrentViewContent(chunks));
		setLayout(new BorderLayout());
		add(content,BorderLayout.CENTER);
		add(menu,BorderLayout.NORTH);
		setVisible(true);
	}
	public void setPercentage(int num){
		content.setProgressbarPercentage(num);
	}
	public void updateGraphics(){
		content.updateBar();
	}
	public void updatePeers(int num){
		content.updatePeers(num);
	}
	public void setFilename(String s){
		content.setFilename(s);
	}
	public void setFileSize(String s){
		content.setFileSize(s);
	}
	public void setUploadSpeed(String s){
		content.setUploadSpeed(s);
	}
	public void setDownloadSpeed(String s){
		content.setDownloadSpeed(s);
	}
	public void setSeedsPeers(String s){
		content.setSeedsPeers(s);
	}
	public void setFileChunkRatio(String s){
		content.setFileChunkRatio(s);
	}
	public void setProgressbarPercentage(int num){
		content.setProgressbarPercentage(num);
	}
	public void createTestThread(){
		t = new Thread(new Runnable(){
			public void run(){
				int trueCount = 0;
				Random rng = new Random();
				while(trueCount < chunky.length){
					int index = rng.nextInt(chunky.length);
					if(chunky[index]){
						// do nothing
					}else{
						chunky[index] = true;
						updateGraphics();
						trueCount++;
						content.setUploadSpeed(rng.nextInt(900) + "Kb/s");
						content.setFileChunkRatio(trueCount + "/" + chunky.length);
						content.setProgressbarPercentage((int)(1000 * (double)trueCount/(double)chunky.length));
						content.setSeedsPeers(rng.nextInt(100) + "/" + rng.nextInt(300));
					}
					try {
						Thread.sleep(800);
					} catch (InterruptedException e) {
						// TODO Auto-generated catch block
						e.printStackTrace();
					}
				}
				
			}
		});
		
	}
	public void startTestThread(){
		t.start();
	}
	public void setContent(TorrentViewContent content) {
		this.content = content;
	}
	
	
}