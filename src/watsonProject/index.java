package watsonProject;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.net.URI;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.Scanner;

import org.apache.lucene.analysis.core.WhitespaceAnalyzer;
import org.apache.lucene.analysis.standard.StandardAnalyzer;
import org.apache.lucene.index.IndexWriter;
import org.apache.lucene.index.IndexWriterConfig;
import org.apache.lucene.index.IndexWriterConfig.OpenMode;
import org.apache.lucene.store.Directory;
import org.apache.lucene.store.FSDirectory;

import edu.stanford.nlp.ling.CoreLabel;
import edu.stanford.nlp.pipeline.CoreDocument;
import edu.stanford.nlp.pipeline.StanfordCoreNLP;

public class index {

	boolean indexExists = false;
	String inputFilePath = "";
	WhitespaceAnalyzer analyzer;
	Directory index;
	//Map<String, List<String>> docRecords;
	//Map<String, List<String>> docCategories;
	
	public index(String inputPath) {
		//docRecords = new HashMap<String, List<String>>();
		//docCategories = new HashMap<String, List<String>>();
		// Creating a File object for directory
		File directoryPath = new File(inputPath);
		// List of all files and directories
		File filesList[] = directoryPath.listFiles();
		System.out.println("List of files and directories in the specified directory:");
		Scanner sc = null;
		String currTitle = "";
		String prevTitle = "";
		String catSentence = "";
		String lemmaSentence = "";
		StanfordCoreNLP pipeline = createPipeline();
		IndexWriter write = null;
		try {
			write = createIndexWriter();
		} catch (IOException e1) {
			// Issue creating index writer
			e1.printStackTrace();
		}
		for (int i = 0; i < 1; i++) {// (File file : filesList) {
			System.out.println("File name: " + filesList[i].getName());
			try {
				sc = new Scanner(filesList[i]);
			} catch (FileNotFoundException e) {
				e.printStackTrace();
			}
			while (sc.hasNextLine()) {
				String temp = sc.nextLine();
				if (temp.length() == 0) {
					continue;
				} else if (temp.startsWith("[[")) {
					prevTitle = currTitle;
					currTitle = temp.replaceAll("[\\[\\[|\\]\\]]", "");
					if (!prevTitle.equals("")) {
						addDoc(write, prevTitle, lemmaSentence);
						catSentence = "";
						lemmaSentence = "";
					}
					//docRecords.put(currTitle, new ArrayList<String>());
					//docCategories.put(currTitle, new ArrayList<String>());
				} else if (temp.startsWith("CATEGORIES")) {
					temp.replace("CATEGORIES: ", "");
					String[] catSplit = temp.split(",");
					for (String category : catSplit) {
						CoreDocument d = pipeline.processToCoreDocument(category);
						for (CoreLabel tok : d.tokens()) {
							catSentence += tok + " ";
						}
					}
				} else if (temp.startsWith("==")) {
					continue;
				} else if (temp.contains("\\[ref\\]")) {
					
				} else {
					CoreDocument d = pipeline.processToCoreDocument(temp);
					for (CoreLabel tok : d.tokens()) {
						lemmaSentence += tok.lemma() + " ";
					}
					
					
				}
			}
		}
		indexExists = true;
		// Must close the writer for index to work
		try {
			write.close();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}

	private StanfordCoreNLP createPipeline() {
		Properties props = new Properties();
		props.setProperty("annotators", "tokenize,ssplit,pos,lemma");
		StanfordCoreNLP pipeline = new StanfordCoreNLP(props);
		return pipeline;
	}
	
	private IndexWriter createIndexWriter() throws IOException {
		analyzer = new WhitespaceAnalyzer();
		index = FSDirectory.open(Paths.get("docIndex"));
		IndexWriterConfig config = new IndexWriterConfig(analyzer);
		config.setOpenMode(OpenMode.CREATE);
		IndexWriter w = new IndexWriter(index, config);
		return w;
	}
	
	private void addDoc(IndexWriter w, String docID, String docText) {
		
	}
}


