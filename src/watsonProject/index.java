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
import org.apache.lucene.document.Document;
import org.apache.lucene.document.Field;
import org.apache.lucene.document.StringField;
import org.apache.lucene.document.TextField;
import org.apache.lucene.index.DirectoryReader;
import org.apache.lucene.index.IndexReader;
import org.apache.lucene.index.IndexWriter;
import org.apache.lucene.index.IndexWriterConfig;
import org.apache.lucene.index.IndexWriterConfig.OpenMode;
import org.apache.lucene.queryparser.classic.ParseException;
import org.apache.lucene.queryparser.classic.QueryParser;
import org.apache.lucene.search.IndexSearcher;
import org.apache.lucene.search.Query;
import org.apache.lucene.search.ScoreDoc;
import org.apache.lucene.search.TopDocs;
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
	StanfordCoreNLP pipeline;
	// Map<String, List<String>> docRecords;
	// Map<String, List<String>> docCategories;

	public index(String inputPath) {
		// docRecords = new HashMap<String, List<String>>();
		// docCategories = new HashMap<String, List<String>>();
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
		pipeline = createPipeline();
		IndexWriter write = null;
		try {
			write = createIndexWriter();
		} catch (IOException e1) {
			// Issue creating index writer
			e1.printStackTrace();
		}
		for (File file : filesList) {
			System.out.println("File name: " + file.getName());
			try {
				sc = new Scanner(file, "UTF-8");
			} catch (FileNotFoundException e) {
				e.printStackTrace();
			}
			while (sc.hasNextLine()) {
				String temp = sc.nextLine().trim();
				if (temp.startsWith("[[File:") || temp.startsWith("[[Image:")) {
					temp = removeImage(sc, temp);
				}
				if (temp.length() == 0) {
					continue;
				} else if (temp.startsWith("[[") && temp.endsWith("]]")) {
					prevTitle = currTitle;
					currTitle = temp.replaceAll("[\\[\\[|\\]\\]]", "");
					if (!prevTitle.equals("")) {
						try {
							addDoc(write, prevTitle, lemmaSentence, catSentence);
							System.out.println(prevTitle + " has been added");
						} catch (IOException e) {
							e.printStackTrace();
						}
						catSentence = "";
						lemmaSentence = "";
					}
					System.out.println("File name: " + file.getName());
				} else if (temp.startsWith("CATEGORIES")) {
					temp = temp.replace("CATEGORIES: ", "");
					String[] catSplit = temp.split(",");
					for (String category : catSplit) {
						CoreDocument d = pipeline.processToCoreDocument(category);
						for (CoreLabel tok : d.tokens()) {
							catSentence += tok.lemma() + " ";
						}
					}
				} else if (temp.trim().startsWith("|")) {
					continue;
				} else {
					temp = cleanString(temp, sc);
					CoreDocument d = pipeline.processToCoreDocument(temp);
					for (CoreLabel tok : d.tokens()) {
						lemmaSentence += tok.lemma() + " ";
					}

				}

			}
			try {
				addDoc(write, currTitle, lemmaSentence, catSentence);
			} catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
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

	private String cleanString(String temp, Scanner sc) {
		temp = removeMultiLineTag(temp, sc, "[ref]", "[/ref]");
		temp = removeTag(temp, "[tpl]", "[/tpl]");
		temp = removeTag(temp, "<ref>", "</ref>");
		temp = removeTag(temp, "<ref", "/>");
		temp = temp.replaceAll("\\[ref\\]", "");
		temp = temp.replaceAll("^=+|=+$", "");
		temp = temp.replaceAll("}}", "");
		temp = temp.replaceAll("#REDIRECT", "");
		temp = temp.replaceAll("\\[\\[", "");
		temp = temp.replaceAll("\\]\\]", "");
		return temp;
	}

	private String removeTag(String temp, String startTag, String endTag) {
		while (temp.contains(startTag) || temp.contains(endTag)) {
			int start = temp.indexOf(startTag);
			int count = 1;
			int currIndex = start;
			boolean endFound = false;
			while (count != 0) {
				int nextOpen = temp.indexOf(startTag, currIndex + 1);
				int nextEnd = temp.indexOf(endTag, currIndex + 1);
				if (start == -1 && nextEnd != -1) {
					temp = temp.substring(nextEnd + endTag.length());
					endFound = true;
					break;
				}
				if ((nextOpen == -1 && nextEnd != -1) || nextOpen > nextEnd) {
					count -= 1;
					currIndex = nextEnd;
				} else if (nextOpen < nextEnd) {
					count += 1;
					currIndex = nextOpen;
				} else if (nextEnd == -1) {
					temp = temp.substring(0, currIndex);
					endFound = true;
					break;
				}
			}
			if (!endFound) {
				temp = temp.substring(0, start) + temp.substring(currIndex + endTag.length());
			}
		}
		return temp;
	}

	private String removeMultiLineTag(String temp, Scanner sc, String startTag, String endTag) {
		if (temp.contains(startTag)) {
			while (!temp.contains(endTag)) {
				if (sc.hasNextLine()) {
					temp += " " + sc.nextLine();
				} else {
					temp = temp.substring(0, temp.indexOf(startTag));
					return temp;
				}
			}
			temp = temp.substring(0, temp.indexOf(startTag))
					+ temp.substring(temp.indexOf(endTag) + endTag.length());
			return temp;
		} else if (temp.contains(endTag) && !temp.contains(startTag)) {
			temp = temp.substring(temp.indexOf(endTag) + endTag.length());
		} else {
		}
		return temp;
	}

	private String removeImage(Scanner sc, String temp) {
		while (true) {
			// temp = cleanString(temp);
			if (temp.endsWith("]]")) {
				return "";
			} else {
				if (sc.hasNextLine()) {
					temp = sc.nextLine();
				} else {
					return "";
				}
			}
		}
	}

	private void addDoc(IndexWriter w, String docName, String docText, String catSentence) throws IOException {
		Document doc = new Document();
		// Makes this field not tokenized
		doc.add(new StringField("docName", docName, Field.Store.YES));
		// Makes this field tokenized; for use in matching queries
		doc.add(new TextField("docText", docText, Field.Store.YES));
		doc.add(new TextField("docCat", catSentence, Field.Store.YES));
		w.addDocument(doc);
	}

	public List<ResultClass> runQuery(String strQuery) throws java.io.IOException {
		ScoreDoc[] hits = null;
		IndexSearcher searcher = null;
		try {
			// Changes String into Query object for Luene
			CoreDocument d = pipeline.processToCoreDocument(strQuery);
			String queryLemma = "";
			for (CoreLabel tok : d.tokens()) {
				queryLemma += tok.lemma() + " ";
			}
			Query p = new QueryParser("docText", analyzer).parse(queryLemma);
			System.out.println(p.toString());
			int hitsPerPage = 1000;
			// Allows index to be read
			IndexReader reader = DirectoryReader.open(index);
			System.out.println("# of documents in index: " + reader.numDocs());
			searcher = new IndexSearcher(reader);
			// Results from the query sorted
			TopDocs docs = searcher.search(p, hitsPerPage);
			hits = docs.scoreDocs;
		} catch (ParseException e) {
			System.err.println("Query cannot be parsed, terminating program");
			System.exit(1);
		}
		List<ResultClass> ans = new ArrayList<ResultClass>();
		// Fills list with results from running query
		for (ScoreDoc doc : hits) {
			ResultClass temp = new ResultClass();
			int docNum = doc.doc;
			Document d = searcher.doc(docNum);
			temp.DocName = d;
			temp.docScore = doc.score;
			ans.add(temp);
		}
		return ans;
	}
}
