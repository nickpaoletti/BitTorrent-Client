package btclient;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.nio.ByteBuffer;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
/**
 * FileManager.java
 * 
 * @author Nick Paoletti
 * @author Daniel Selmon
 *
 */
public class FileManager{
        //This array keeps track of how many pieces you have downloaded:
        public static boolean[] bitfield;
        public static TorrentInfo info;
        public static TrackerInfo tracker;
        public static boolean[] perPieceBitfield;
        public static ByteBuffer[] isRequested;
        public static RandomAccessFile file;
        public static ArrayList<Peer> approvedPeers;
        public static boolean havePieces;
        public static int downloaded, uploaded;
        /**
         * 
         * File writing code obtained from http://www.roseindia.net/java/beginners/java-write-to-file.shtml
         * 
         * @param filename
         * @throws IOException
         */
        public static void storeFileProgress(String filename) throws IOException {
                FileWriter write = new FileWriter(filename.substring(0, filename.lastIndexOf(".mp3")) + "PROGRESS.txt");
                BufferedWriter out = new BufferedWriter(write);
                for (int i = 0; i < bitfield.length; i++){
                        if (bitfield[i] == true){
                                out.write('1');
                        }
                        else {
                                out.write('0');
                        }
                }
                out.close();
                write.close();
        }
        /**
         * 
         * @param filename
         * @throws IOException
         * @throws NoSuchAlgorithmException
         */
        public static void readFileProgress(String filename) throws IOException, NoSuchAlgorithmException {
                FileManager.havePieces = true;
                FileInputStream fileRead = new FileInputStream(new File(filename.substring(0, filename.lastIndexOf(".mp3")) + "PROGRESS.txt"));
                int indexCounter = 0;
                while(fileRead.available() > 0){
                        if((char)fileRead.read() == '1'){
                                //Verify each piece.
                                byte[] pieceCheck = new byte[info.piece_length];
                    FileManager.file.seek(indexCounter * (info.piece_length));
                    FileManager.file.readFully(pieceCheck);  
                    FileManager.bitfield[indexCounter] = Download.shaHash(pieceCheck, info.piece_hashes[indexCounter].array());
                        } else {
                                bitfield[indexCounter] = false;
                        }
                        indexCounter++;
                }
                fileRead.close();
        }
        /**
         * 
         * @param bytes
         */
        public static void addDownloaded(int bytes){
                downloaded += bytes;
        }
        /**
         * 
         * @param bytes
         */
        public static void addUploaded(int bytes){
                uploaded += bytes;
        }
        /**
         * 
         */
        public static void initializeFields(){
                int numpieces = (int) Math.ceil(info.file_length/16384);
                bitfield = new boolean[info.piece_hashes.length];
                perPieceBitfield = new boolean[numpieces];
                isRequested = new ByteBuffer[numpieces];
                uploaded = 0;
                downloaded = 0;
        }       
}