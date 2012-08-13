package btclient.gui;

import java.awt.Color;
import java.awt.Dimension;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.geom.Rectangle2D;
//import java.math.BigInteger;
import javax.swing.JComponent;

public class DownloadBar extends JComponent{
	private static final long serialVersionUID = 1L;
	private boolean[] chunkArray = null;
	//private BigInteger numBytes = null;
	private static final int MAX_LENGTH = 400;
	private static final int HEIGHT = 20;
	private double chunkWidth = -1;
	public DownloadBar(boolean[] chunkArray){
		setMinimumSize(new Dimension(MAX_LENGTH, HEIGHT));
		setPreferredSize(new Dimension(MAX_LENGTH, HEIGHT));
		setMaximumSize(new Dimension(MAX_LENGTH, HEIGHT));
		this.chunkArray = chunkArray;
		calculateChunkWidth();	
	}
	private void calculateChunkWidth(){
		chunkWidth = (double)MAX_LENGTH/ (double)chunkArray.length;
	}
	protected void paintComponent(Graphics g){
		super.paintComponent(g);
		Graphics2D g2d = (Graphics2D) g.create();
		double start = 0.0;
		for(boolean b: chunkArray){
			if(b){
				g2d.setPaint(Color.blue);
			}else{
				g2d.setPaint(Color.RED);
			}
			Rectangle2D r2d = new Rectangle2D.Double(start,0,chunkWidth,HEIGHT);
			g2d.fill(r2d);
			start += chunkWidth;
		}
	}
}
