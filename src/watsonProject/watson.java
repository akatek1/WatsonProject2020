package watsonProject;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.Properties;
import java.util.Scanner;

import org.apache.lucene.analysis.core.WhitespaceAnalyzer;
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

/**
 * @author Alexander Katek
 *
 *         Class: CSC 483: Text Retrieval and Web Search
 * 
 *         Final Project: Recreating Part of Watson
 *
 *         This class implements a method to generate an index containing over
 *         180,000 Wikipedia pages, then uses the generated index (or a
 *         user-given one) to attempt to answer 100 questions from Jeopardy by
 *         ranking all documents returned from the index and taking the title of
 *         the best one as the answer. The question document is included in the
 *         repository and is hardcoded into the question slot of my main method.
 *         However, the index is given through the program through either a
 *         relative or absolute path to the index.
 */

public class watson {

	// Global Variables -- the objects used for my index
	String inputFilePath = "";
	static WhitespaceAnalyzer analyzer;
	static Directory index;
	static StanfordCoreNLP pipeline;
	static boolean indexExists = false;

	/**
	 * Main method of my Watson. Runs the index creation (if uncommented) and the
	 * evaluation of the system given the 100 questions in the file in the answers
	 * folder. The index folder (docIndex) must be in the same place as the src
	 * folder for the program to run correctly. Please see report for clarification.
	 * 
	 * @param args The string index of the arguments passed into the program.
	 * 
	 */
	public static void main(String[] args) {
		pipeline = createPipeline();
		analyzer = new WhitespaceAnalyzer();
		// Uncomment the method below to generate the document index from the files in
		// the doc folder
		// buildIndex("src/docs", pipeline);
		List<ResultClass> test = null;
		// Retrieves given question document.
		Scanner questions = null;
		try {
			questions = new Scanner(new File("src/answers/questions.txt"));
		} catch (FileNotFoundException e1) {
			System.err.println(
					"Make sure you imported the whole repositiory as the questons should be in the answers folder in src");
			System.err.println("Program Terminating");
			System.exit(1);
		}
		// Variables to keep track of statistics
		float count = 0;
		int correctCount = 0;
		double totalRankings = 0;
		// Runs through all questions
		while (questions.hasNextLine()) {
			String category = questions.nextLine();
			if (category.equals("")) {
				continue;
			}
			String question = questions.nextLine();
			// Captures all variations on an answer
			String[] answer = questions.nextLine().split("\\|");
			String topAnswer = null;
			System.out.println("Running Question " + (count + 1));
			try {
				test = runQuery(question, category);
			} catch (IOException e) {
				e.printStackTrace();
			}
			// Calculating for MMR score
			boolean answerFound = false;
			for (int i = 0; i < test.size(); i++) {
				if (i == 0) {
					topAnswer = test.get(i).DocName.get("docName");
					// topScore = test.get(i).docScore;
				}
				for (String ans : answer) {
					if (test.get(i).DocName.get("docName").toLowerCase().equals(ans.toLowerCase())) {
						totalRankings += (double) ((double) 1 / ((double) i + 1));
						answerFound = true;
						break;
					}
				}
				if (answerFound) {
					break;
				}
			}
			// Checking if correct answer was found
			if (!(topAnswer == null)) {
				for (String ans : answer) {
					if (topAnswer.toLowerCase().equals(ans.toLowerCase())) {
						correctCount += 1;
						break;
					}
				}
			}
			count += 1;

		}
		// Final Statistics of my program
		System.out.println("Total correct = " + correctCount + "/" + count + " = "
				+ (((float) correctCount / (float) count) * 100) + "% accuracy");
		System.out.println("MMR Score for the set of queries: " + ((1 / count) * totalRankings));
	}

	/**
	 * This method takes the clue and the category of the clue, tokenizes and
	 * lemmatizes them and runs these through the index. It stores all the results
	 * and their score in a list of ResultClass objects and returns the list.
	 * 
	 * @param strQuery The Jeopardy clue we are attempting to answer
	 * @param catQuery The category of the Jeopardy clue
	 * @return A list of ResultClass objects containing all results returned from
	 *         the index and their scores
	 * @throws java.io.IOException Thrown if the docIndex does not exist or if the
	 *                             searcher fails.
	 */
	public static List<ResultClass> runQuery(String strQuery, String catQuery) throws java.io.IOException {
		ScoreDoc[] hits = null;
		IndexSearcher searcher = null;
		try {
			// Changes String into Query object for Lucene
			CoreDocument d = pipeline.processToCoreDocument(catQuery + " " + strQuery);
			String queryLemma = "";
			for (CoreLabel tok : d.tokens()) {
				queryLemma += tok.lemma() + " ";
			}
			Query p = new QueryParser("docText", analyzer).parse(queryLemma);
			// System.out.println(p.toString());
			int hitsPerPage = 190000;
			// Allows index to be read
			IndexReader reader = DirectoryReader.open(FSDirectory.open(Paths.get("docIndex")));
			searcher = new IndexSearcher(reader);
			// Results from the query sorted
			TopDocs docs = searcher.search(p, hitsPerPage);
			hits = docs.scoreDocs;
		} catch (ParseException e) {
			e.printStackTrace();
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

	/**
	 * Helper method to create the Pipeline for lemmatization
	 * 
	 * @param None
	 * @return pipeline A StandfordCoreNLP object that will be used to tokenize and
	 *         lemmatize documents and quieres
	 */
	private static StanfordCoreNLP createPipeline() {
		Properties props = new Properties();
		props.setProperty("annotators", "tokenize,ssplit,pos,lemma");
		StanfordCoreNLP pipeline = new StanfordCoreNLP(props);
		return pipeline;
	}

	/**
	 * This method constructs the index of all documents in the doc folder by
	 * cleaning all the content of the document of Wikipedia specific formatting and
	 * joining the title of the document with its content
	 * 
	 * @param inputPath Filepath to the folder full of documents
	 * @param pipeline  A StandfordCoreNLP object that will be used to tokenize and
	 *                  lemmatize documents and quieres
	 */
	public static void buildIndex(String inputPath, StanfordCoreNLP pipeline) {
		// Creating a File object for directory
		File directoryPath = new File(inputPath);
		// List of all files and directories
		File filesList[] = directoryPath.listFiles();
		System.out.println("List of files and directories in the specified directory:");
		IndexWriter write = null;
		try {
			write = createIndexWriter();
		} catch (IOException e1) {
			// Issue creating index writer
			System.err.println("Unable to create index, terminating program");
			System.exit(1);
		}
		// Runs for each file
		for (File file : filesList) {
			processFile(file, write);
		}
		indexExists = true;
		// Must close the writer for index to work
		try {
			write.close();
		} catch (IOException e) {
			System.err.println("Index Writer cannot be closed, terminating program");
			System.exit(1);
		}

	}

	/**
	 * Helper method to generate the IndexWriter object to create the index
	 * 
	 * @return IndexWriter object that will be used to create the index
	 * @throws IOException Thrown if unable to create the writer or if the path for
	 *                     the index cannot be found
	 */
	private static IndexWriter createIndexWriter() throws IOException {
		analyzer = new WhitespaceAnalyzer();
		index = FSDirectory.open(Paths.get("docIndex"));
		IndexWriterConfig config = new IndexWriterConfig(analyzer);
		config.setOpenMode(OpenMode.CREATE);
		IndexWriter w = new IndexWriter(index, config);
		return w;
	}

	/**
	 * Method that takes in a file and IndexWriter, parses the whole file, and
	 * generates index entries from this file.
	 * 
	 * @param file  Current file being read and parsed for the index
	 * @param write IndexWriter object that will be used to create the index
	 */
	private static void processFile(File file, IndexWriter write) {
		Scanner sc = null;
		String currTitle = "";
		String prevTitle = "";
		String lemmaSentence = "";
		System.out.println("File name: " + file.getName());
		try {
			sc = new Scanner(file, "UTF-8");
		} catch (FileNotFoundException e) {
			e.printStackTrace();
		}
		// Go through whole file
		while (sc.hasNextLine()) {
			String temp = sc.nextLine().trim();
			// Images or files that need to be removed
			if (temp.startsWith("[[File:") || temp.startsWith("[[Image:")) {
				temp = removeImage(sc, temp);
			}
			if (temp.length() == 0) {
				continue;
				// Indicator of title
			} else if (temp.startsWith("[[") && temp.endsWith("]]")) {
				prevTitle = currTitle;
				currTitle = temp.replaceAll("[\\[\\[|\\]\\]]", "");
				if (currTitle.contains("(")) {
					currTitle = currTitle.substring(0, currTitle.indexOf("("))
							+ currTitle.substring(currTitle.indexOf(")") + 1);
					currTitle = currTitle.trim();
				} // Add new entry once we find the next title
				if (!prevTitle.equals("")) {
					try {
						addDoc(write, prevTitle, lemmaSentence);
						System.out.println(prevTitle + " has been added");
					} catch (IOException e) {
						System.err.println("Document cannot be added, being skipped");
					}
					lemmaSentence = "";
				}
				System.out.println("File name: " + file.getName());
				// Category line present in some files
			} else if (temp.startsWith("CATEGORIES")) {
				temp = temp.replace("CATEGORIES: ", "");
				String[] catSplit = temp.split(",");
				for (String category : catSplit) {
					CoreDocument d = pipeline.processToCoreDocument(category);
					for (CoreLabel tok : d.tokens()) {
						lemmaSentence += tok.lemma() + " ";
					}
				}
				// Link information that needs to be removed
			} else if (temp.trim().startsWith("|")) {
				continue;
			} else { // Processing document content
				temp = cleanString(temp);
				CoreDocument d = pipeline.processToCoreDocument(temp);
				for (CoreLabel tok : d.tokens()) {
					lemmaSentence += tok.lemma() + " ";
				}

			}

		}
		// Add last document of the file
		try {
			addDoc(write, currTitle, lemmaSentence);
			System.out.println(currTitle + " has been added");
		} catch (IOException e) {
			System.err.println("Document cannot be added, being skipped");
		}
	}

	/**
	 * Helper method that cleans all the formatting off a line of content for a
	 * document
	 * 
	 * @param temp Current string being cleaned
	 * @return String that has been cleaned of all Wikipedia formatting
	 */
	private static String cleanString(String temp) {
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

	/**
	 * Helper method that removes start and end tags, along with the content in
	 * between
	 * 
	 * @param temp     Current string being cleaned of these tags
	 * @param startTag The tag indicating the start
	 * @param endTag   The tag indicating the end
	 * @return String cleans of the given tags and the content in between
	 */
	private static String removeTag(String temp, String startTag, String endTag) {
		while (temp.contains(startTag) || temp.contains(endTag)) {
			int start = temp.indexOf(startTag);
			int count = 1;
			int currIndex = start;
			boolean endFound = false;
			while (count != 0) { // If matching start and end tags have been found
				int nextOpen = temp.indexOf(startTag, currIndex + 1);
				int nextEnd = temp.indexOf(endTag, currIndex + 1);
				if (start == -1 && nextEnd != -1) { // If no other tags are found
					temp = temp.substring(nextEnd + endTag.length());
					endFound = true;
					break;
				} // If an end tag is found
				if ((nextOpen == -1 && nextEnd != -1) || nextOpen > nextEnd) {
					count -= 1;
					currIndex = nextEnd;
				} else if (nextOpen < nextEnd) { // If another open tag found before an end tag
					count += 1;
					currIndex = nextOpen;
				} else if (nextEnd == -1) { // If no end tag is found
					temp = temp.substring(0, currIndex);
					endFound = true;
					break;
				}
			}
			if (!endFound) { // If start tag was found, but no endTag
				temp = temp.substring(0, start) + temp.substring(currIndex + endTag.length());
			}
		}
		return temp;
	}

	/**
	 * Helper method that removes image content from documents over multiple lines
	 * @param sc Scanner that is reading over the current file of documents
	 * @param temp Current string having the image removed from
	 * @return String in which the image content has been removed
	 */
	private static String removeImage(Scanner sc, String temp) {
		while (true) {
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

	/**
	 * Helper method to add this document to the index
	 * @param w IndexWriter object that writes to the index
	 * @param docName Document title
	 * @param docText Document content
	 * @throws IOException Thrown if using IndexWriter crashes
	 */
	private static void addDoc(IndexWriter w, String docName, String docText) throws IOException {
		Document doc = new Document();
		// Makes this field not tokenized
		doc.add(new StringField("docName", docName, Field.Store.YES));
		// Makes this field tokenized; for use in matching queries
		doc.add(new TextField("docText", docText, Field.Store.YES));
		w.addDocument(doc);
	}

}
