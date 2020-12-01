package watsonProject;

import java.io.File;

import org.apache.lucene.analysis.standard.StandardAnalyzer;
import org.apache.lucene.store.Directory;

public class watson {
	
	// Global Variables -- the objects used for my index
	boolean indexExists = false;
	String inputFilePath = "";
	StandardAnalyzer analyzer;
	Directory index;
	
	public static void main(String[] args) {
		index Ind = new index("src/docs");
	}
	
	private void buildIndex() {
		

	}
}
