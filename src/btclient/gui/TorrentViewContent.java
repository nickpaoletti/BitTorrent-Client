package btclient.gui;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JProgressBar;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.table.AbstractTableModel;
import javax.swing.table.TableCellRenderer;


public class TorrentViewContent extends JPanel{
	private static final long serialVersionUID = -1604665101703310595L;
	private DownloadBar db;
	private GridBagLayout gb;
	private GridBagConstraints gc;
	private JScrollPane tableView;
	private JTable tableData; 
	//table stuff
	private JLabel lblPeers = new JLabel();
	private JLabel downloadRate = new JLabel();;
	private JLabel percent = new JLabel();;
	private JLabel uploadRate = new JLabel();;
	private JLabel size = new JLabel();
	private JLabel fileName = new JLabel();
	private JLabel seedsPeers = new JLabel();
	private JProgressBar progressBar;
	private BittorrentModel tableModel;
	private JLabel fileChunkRatio = new JLabel();
	public TorrentViewContent(boolean[] chunks){
		tableModel = new BittorrentModel();
		db = new DownloadBar(chunks);
		gb = new GridBagLayout();
		gc = new GridBagConstraints();
		tableData = new JTable(tableModel);
		tableData.setPreferredScrollableViewportSize(new Dimension(400,400));
		tableData.setFillsViewportHeight(true);
		tableView = new JScrollPane(tableData);
		setLayout(gb);
		configureTable();
		gc.gridx = 1;
		gc.gridy = 1;
		gc.insets = new Insets(4,4,4,4);
		gc.fill = GridBagConstraints.BOTH;
		add(tableView,gc);
		gc.gridx = 1;
		gc.gridy = 2;
		add(db,gc);
	}
	public void setProgressbarPercentage(int num){
		progressBar.setValue(num);
		percent.setText(Double.toString((double)num/10.0));
	}
	public void setDownloadRate(String s){
		downloadRate.setText(s);
	}
	
	private void configureTable(){
		progressBar = new JProgressBar();
		progressBar.setMinimum(0);
		progressBar.setMaximum(1000);
		tableData.setAutoResizeMode(JTable.AUTO_RESIZE_OFF);
		tableData.setValueAt(fileName, 0, 0);
		tableData.setValueAt(size, 0, 1);
		tableData.setValueAt(percent, 0, 2);
		tableData.setValueAt(progressBar, 0, 3);
		tableData.setValueAt(downloadRate, 0, 4);
		tableData.setValueAt(uploadRate, 0, 5);
		tableData.setValueAt(seedsPeers, 0, 6);
		tableData.setValueAt(fileChunkRatio, 0, 7);
		tableData.getColumnModel().getColumn(3).setCellRenderer(new ProgressBarRenderer());
		tableData.getColumnModel().getColumn(0).setCellRenderer(new JLabelRenderer());
		tableData.getColumnModel().getColumn(1).setCellRenderer(new JLabelRenderer());
		tableData.getColumnModel().getColumn(2).setCellRenderer(new JLabelRenderer());
		tableData.getColumnModel().getColumn(4).setCellRenderer(new JLabelRenderer());
		tableData.getColumnModel().getColumn(5).setCellRenderer(new JLabelRenderer());
		tableData.getColumnModel().getColumn(6).setCellRenderer(new JLabelRenderer());
		tableData.getColumnModel().getColumn(7).setCellRenderer(new JLabelRenderer());
	}
	public void setFilename(String s){
		fileName.setText(s);
	}
	public void setFileSize(String s){
		size.setText(s);
	}
	public void updatePeers(int num){
		lblPeers.setText("Peers: " + num);
	}
	public void updateBar(){
		db.repaint();
		repaint();
	}
	public void setUploadSpeed(String s){
		uploadRate.setText(s);
	}
	public void setDownloadSpeed(String s){
		downloadRate.setText(s);
	}
	public void setSeedsPeers(String s){
		seedsPeers.setText(s);
	}
	public void setFileChunkRatio(String s){
		fileChunkRatio.setText(s);
	}
	private class BittorrentModel extends AbstractTableModel{
		private static final long serialVersionUID = 8660851901355169755L;
		private JComponent[][] components = new JComponent[1][8];
		private String[] columns = {"Filename","Size","%","Progress","Download",
				"Upload","Seeds/Peers","File Chunks/Total"};
		@Override
		public int getColumnCount() {
			// TODO Auto-generated method stub
			return columns.length;
		}
		public String getColumnName(int i){
			return columns[i];
		}
		@Override
		public int getRowCount() {
			// TODO Auto-generated method stub
			return components.length;
		}

		@Override
		public Object getValueAt(int arg0, int arg1) {
			
			return components[arg0][arg1];
		}
		public void setValueAt(Object value, int row, int col) {
	        components[row][col] = (JComponent)value;
	        fireTableCellUpdated(row, col);
	    }
	}
	private class ProgressBarRenderer  extends JProgressBar implements TableCellRenderer{
		private static final long serialVersionUID = -2469430971296540559L;
		@Override
		public Component getTableCellRendererComponent(JTable arg0,
				Object arg1, boolean arg2, boolean arg3, int arg4, int arg5) {
			
			return (JProgressBar)arg1;
		}
	}
	private class JLabelRenderer extends JLabel implements TableCellRenderer{
		private static final long serialVersionUID = -2750750375934947790L;
		@Override
		public Component getTableCellRendererComponent(JTable table,
				Object value, boolean isSelected, boolean hasFocus, int row,
				int column) {
			// TODO Auto-generated method stub
			return (JLabel)value;
		}
	}	
}