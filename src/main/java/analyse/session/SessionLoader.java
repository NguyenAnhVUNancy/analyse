package analyse.session;

import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.json.simple.parser.JSONParser;
import org.json.simple.parser.ParseException;

import analyse.UI.UIUtils;
import analyse.exceptions.JSONParsingException;
import analyse.exceptions.NotEnoughArgumentException;
import analyse.exceptions.NotFoundException;
import analyse.messageanalysis.Author;
import analyse.messageanalysis.Conversation;
import analyse.messageanalysis.Label;
import analyse.messageanalysis.Message;
import analyse.messageanalysis.Parameter;
import analyse.messageanalysis.Reactions;
import analyse.search.SimpleResult;
import analyse.utils.FileNameUtils;
import analyse.utils.JSONUtils;
import analyse.utils.MessengerUtils;
import analyse.utils.WhatsappUtils;

/**
 * Data loader for analyse.session.Session
 */
public class SessionLoader extends SessionTools {
	private static final DateTimeFormatter formatter = 
			DateTimeFormatter.ofPattern("yyyyMMddHHmm");
	private SessionEditor editor;
	
	/**
	 * setter
	 * @param editor SessionEditor
	 */
	public void setEditor(SessionEditor editor) {
		this.editor = editor;
	}
	
	/**
	 * Command-line controller for data loader
	 * @param s String[] arguments
	 * @param session analyse.session.Session
	 * @throws NotEnoughArgumentException
	 */
	public void load(String[] s) throws NotEnoughArgumentException {
		if (this.getSession() == null) {
			this.println("No session started");
		} else {
			UIUtils.notEnoughArguments(s, 3);
			File file = this.getSession().getFileSystem().getPath(s[1]).toFile();
			List<Label> labels = new ArrayList<>();
			if (s.length > 3) {
				List<String> l = Arrays.asList(Arrays.copyOfRange(s, 3, s.length));
				for (String str : l) {
					labels.add(new Label(str));
				}
			}
			switch (s[0]) {
				case "folder":
					List<String> possibleModes = Arrays.asList("fb", "whatsapp, messages");
					if (possibleModes.contains(s[2])) {
						this.loadFolder(file, labels, s[2]);
					} else {
						UIUtils.modeUnknown(s[2], possibleModes);
					}
					break;
				case "whatsapp":
					this.printf("Loading Whatsapp file %s", file.toString());
					WhatsappUtils.load(file, labels, s[2], this.editor);
					this.overwrite();
					this.printfln("Whatsapp file %s finished loading and parsing", file.toString());
					break;
				case "fb":
					this.printf("Loading Whatsapp file %s", file.toString());
					MessengerUtils.load(file, labels, s[2], this.editor);
					this.overwrite();
					this.printfln("Facebook Messenger file %s finished loading and parsing", file.toString());
					break;
				case "session":
					if (Boolean.TRUE.equals(Boolean.valueOf(s[2]))) {
						this.getSession().restart();
					}
					this.loadSession(file);
					break;
				case "messages":
					if (Boolean.TRUE.equals(Boolean.valueOf(s[2]))) {
						this.getSession().restart();
					}
					this.loadMessages(file);
					break;
				default:
					UIUtils.modeUnknown(s[0], Arrays.asList("whatsapp",
							"fb","session","messages"));
			}
		}
	}
	
	/**
	 * Load saved session
	 * @param file File to session save file
	 * @param session analyse.session.Session
	 */
	public void loadSession(File file) {
		try {
			JSONObject jo = JSONUtils.convert(
					(org.json.simple.JSONObject) (new JSONParser().parse(new FileReader(file))));
			this.printf("Session data file %s finished loading", file.toString());
			this.overwrite();
			this.parseAuthors(jo.getJSONArray("authors"));
			this.overwrite();
			this.parseMessages(jo.getJSONArray("messages"));
			this.overwrite();
			this.parseParams(jo.getJSONArray("parameters"));
			this.overwrite();
			this.parseResults(jo.getJSONArray("results"));
			this.overwrite();
			
			this.getSession().getFileSystem().set("sessionFile", file);
			this.overwrite();
			this.printfln("Session data file %s finished loading and parsing", file.toString());
	    } catch (IOException | JSONException | JSONParsingException | ParseException e) {
	    	SessionPrinter.printException(e);
	    }
	}
	
	/**
	 * load authors from save file
	 * @param authors JSONArray containing authors to parse
	 */
	private void parseAuthors(JSONArray authors) {
		for (int i = 0; i < authors.length() ; i++) {
			this.printProgress(i, authors.length(), "Parsing authors");
			JSONObject o = authors.getJSONObject(i);
			Author author = new Author(o.getString("name"));
			JSONArray labels = o.getJSONArray("labels");
			JSONArray conversations = o.getJSONArray("conversations");
			for (int j = 0; j < labels.length(); j++) {
				author.addLabel(new Label(labels.getString(j)));
			}
			for (int j = 0; j <  conversations.length(); j++) {
				author.addConversation(new Conversation(conversations.getString(j)));
			}
			this.editor.addAuthor(author);
		}
	}
	
	/**
	 * Load session data from message save file
	 * @param file File to save file
	 */
	private void loadMessages(File file) {
		try {
			JSONArray jo = JSONUtils.convert(
					(org.json.simple.JSONArray) (new JSONParser().parse(new FileReader(file))));
			this.printf("Messages data file %s finished loading", file.toString());
			this.overwrite();
			this.parseMessages(jo);
			this.overwrite();
			this.printfln("Messages data file %s finished loading and parsing", file.toString());
		} catch (IOException | JSONParsingException | JSONException | ParseException e) {
			SessionPrinter.printException(e);;
		}
		
	}
	
	/***
	 * load messages from save file
	 * @param messages JSONArray containing messages to parse
	 * @throws JSONParsingException 
	 */
	private void parseMessages(JSONArray messages) throws JSONParsingException {
		for (int i = 0; i < messages.length(); i++) {
			this.printProgress(i, messages.length(), "Parsing messages");
			JSONObject o = messages.getJSONObject(i);
			LocalDateTime date = LocalDateTime.parse(o.getString("date"),formatter);
			Author author = new Author(o.getString("author"));
			Conversation conv = new Conversation(o.getString("conversation"));
			if (o.has("author_labels")) {
				JSONArray authorLabels = o.getJSONArray("author_labels");
				for (int j = 0; j < authorLabels.length(); j++) {
					author.addLabel(new Label(authorLabels.getString(j)));
				}
			}
			JSONArray labels = o.getJSONArray("labels");
			for (int j = 0; j < labels.length(); j++) {
				conv.addLabel(new Label(labels.getString(j)));
			}
			String content = o.getString("content");
			author.addConversation(conv);
			Reactions reactions = Reactions.parse(o);
			this.editor.addMessage(new Message(0l, date, author, content,conv, reactions));
		}
	}
	
	/**
	 * load results from save file
	 * @param results JSONArray containing results to parse
	 */
	private void parseResults(JSONArray results) {
		for (int i = 0; i < results.length(); i++) {
			this.printProgress(i, results.length(), "Parsing results");
			JSONObject o = results.getJSONObject(i);
			if (o.getString("type").contentEquals("SIMPLE")) {
				try {
					this.getSession().getSearchHandler().addResult(SimpleResult.parse(o, this.getSession()));
				} catch (JSONException | NotFoundException e) {
					SessionPrinter.printException(e);
				}
			}
		}
	}
	
	/**
	 * load search parameters from save file
	 * @param params
	 */
	private void parseParams(JSONArray params) {
		for (int i = 0; i < params.length(); i++) {
			this.printProgress(i, params.length(), "Parsing parameters");
			JSONObject o = params.getJSONObject(i);
			this.getSession().getSearchHandler().addParams(Parameter.parse(o));
		}
	}
	
	/**
	 * load whole folder 
	 * @param file File folder to parse
	 * @param labels
	 * @param editor
	 */
	private void loadFolder(File file, List<Label> labels, String mode) {
		if (!file.isDirectory()) {
			this.printfln("%s not a directory", file.toString());
		} else {
			int size = file.listFiles().length;
			int count = 0;
			for (File f : file.listFiles()) {
				String[] fileName = f.toPath().getFileName().toString().split("\\."); 
				if (fileName.length > 1 && fileName[1]
						.contentEquals("json") && mode.contentEquals("fb")) {
					this.overwrite();
					count++;
					this.printProgress(count, size, f, mode);
					MessengerUtils.load(f, labels, FileNameUtils.check(fileName[0]), this.editor);
				} else if (fileName.length > 1 && fileName[1]
						.contentEquals("txt") && mode.contentEquals("whatsapp")) {
					this.overwrite();
					count++;
					this.printProgress(count, size, f, mode);
					WhatsappUtils.load(f, labels, FileNameUtils.check(fileName[0]), this.editor);
				} else if (fileName.length > 1 && fileName[1]
						.contentEquals("json") && mode.contentEquals("messages")) {
					this.overwrite();
					count++;
					this.printProgress(count, size, f, mode);
					this.loadMessages(f);
				} 
			}
			this.overwrite();
			this.printfln("%d %s files in %s successfully loaded", count, mode, file.toString());
		}
	}
	
	private void printProgress(Integer total, Integer size, String text) {
		if (size < 1000 || total % (size / 1000) == 0) {
			float progression = ((float) total * 100)/size;
			this.printf("\r%s %.02f%% %s", 
					UIUtils.progressBar(progression, 10), progression, text);
		}
	}
	
	private void printProgress(int count, int size, File file, String mode) {
		this.printf("%s %d/%d loading %s file: %s", 
				UIUtils.progressBar(((float) count * 100)/size, 10),
				count, size, mode, file.toPath().getFileName().toString());
	}
}
