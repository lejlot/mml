package MML;

import java.io.BufferedWriter;
import java.io.DataOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.util.Scanner;

/**
 * Helper class
 * @author lejlot
 */
public class Utils {
     /**
   * Used to load the entire file into String object
   * @param in FIle to be loaded
   * @return FIle content
   * @throws IOException IO errors
   */
  public static String load(File in) throws IOException{
      Scanner sc = new Scanner(in);
      StringBuilder sb = new StringBuilder();
      while (sc.hasNextLine()){
            sb.append(sc.nextLine()).append("\n");
      }
      return sb.toString();
  }
   /**
   * Creates file with given text
   * @param text text of the file
   * @param out file location
   * @throws IOException IO errors
   */
  public static void save(String text, File out) throws IOException{
      BufferedWriter bw = new BufferedWriter(new OutputStreamWriter(new DataOutputStream(new FileOutputStream(out))));
      bw.write(text);
      bw.close();
  }
  
    /**
    * 
    * @return Path to the system temp directory
    */
    public static String getTempDir() {
        return System.getProperty("java.io.tmpdir");
    }
}
