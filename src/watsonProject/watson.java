package watsonProject;

import java.io.File;
import java.io.IOException;
import java.util.List;

import org.apache.lucene.analysis.standard.StandardAnalyzer;
import org.apache.lucene.index.IndexReader;
import org.apache.lucene.store.Directory;

public class watson {
	
	// Global Variables -- the objects used for my index
	boolean indexExists = false;
	String inputFilePath = "";
	StandardAnalyzer analyzer;
	Directory index;
	
	public static void main(String[] args) {
		index ind = new index("src/docs");
		/*List<ResultClass> test = null;
		try {
			test = ind.runQuery("the");
		} catch (IOException e) {
			e.printStackTrace();
		}
		for (ResultClass result : test) {
			System.out.println(result.DocName.get("docName") + ": " + result.docScore);
		}*/
	}
	
	private void buildIndex() {
		

	}
}
