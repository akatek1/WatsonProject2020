package watsonProject;

import java.io.File;
import java.net.URI;
import java.util.Scanner;

public class index {
	
	public index(String inputPath) {
		//Creating a File object for directory
	      File directoryPath = new File(inputPath);
	      //List of all files and directories
	      File filesList[] = directoryPath.listFiles();
	      System.out.println("List of files and directories in the specified directory:");
	      Scanner sc = null;
	      for(File file : filesList) {
	         System.out.println("File name: "+file.getName());
	      }
	}
}
