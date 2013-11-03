
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.ResourceBundle;

import javafx.beans.value.ChangeListener;
import javafx.beans.value.ObservableValue;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.concurrent.Worker.State;
import javafx.event.EventHandler;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.ListView;
import javafx.scene.control.TextArea;
import javafx.scene.control.TextField;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyEvent;
import javafx.scene.input.MouseEvent;
import javafx.scene.web.WebEngine;
import javafx.scene.web.WebView;

import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import org.json.simple.parser.ParseException;


public class Controller implements Initializable {
	@FXML
	private static WebView webView;
	@FXML
	private static TextField inputField;
	@FXML
	private static TextField countField;
	@FXML
	private static TextField parseField;
	@FXML
	private static ListView<String> listView;
	@FXML
	private static TextArea songText;
	
	private static WebEngine engine;
	
	private static final String urlParameters = "authorize?client_id=3972445&scope=8&redirect_uri=http://oauth.vk.com/blank.html&display=popup&response_type=token";
	private static final String auth = "https://oauth.vk.com/" + urlParameters;
	
	private static final String audioSearch = "https://api.vk.com/method/audio.search?q=";
	private static final String lyricsSearch = "https://api.vk.com/method/audio.getLyrics?";
	private static String accessToken = null;
	
	private static String json;
	
	private static int count = 0;
	
	@Override
	public void initialize(URL arg0, ResourceBundle arg1) {
		countField.setText("10");
		parseField.setText("title");
		
		engine = webView.getEngine();
		engine.load(auth);
		
		engine.getLoadWorker().stateProperty().addListener(new ChangeListener<State>() {
			@Override
			public void changed(ObservableValue<? extends State> arg0, State arg1, State newState) {				
				if(newState == State.SUCCEEDED) {
					System.out.println(engine.getLocation());
					++count;
					
					if(count == 2) {
						int start = engine.getLocation().indexOf("access_token=");
						int end = engine.getLocation().indexOf("expires_in") - 1;
						if(accessToken == null) {
							accessToken = engine.getLocation().substring(start, end);
							inputField.setDisable(false);
							countField.setDisable(false);
							parseField.setDisable(false);
						}
					}
				}
			}
		});
		
		inputField.setOnKeyReleased(new EventHandler<KeyEvent>() {
			@Override
			public void handle(KeyEvent e) {
				executeSearch(e);
			}
		});
		
		parseField.setOnKeyReleased(new EventHandler<KeyEvent>() {
			@Override
			public void handle(KeyEvent e) {
				executeSearch(e);
			}
		});
		
		countField.setOnKeyReleased(new EventHandler<KeyEvent>() {
			@Override
			public void handle(KeyEvent e) {
				executeSearch(e);
			}
		});
		
		listView.setOnMouseReleased(new EventHandler<MouseEvent>() {
			@Override
			public void handle(MouseEvent e) {
				
				try {
					ObservableList<String> parsed = parseJson(json, "lyrics_id");
					String json = executeLyrics(parsed.get(listView.getSelectionModel().getSelectedIndex()));
					
					JSONParser jsonParser = new JSONParser();
					JSONObject jsonObject = (JSONObject) jsonParser.parse(json);
					jsonObject = (JSONObject) jsonObject.get("response");
					
					String text = (String) jsonObject.get("text");
					songText.setText(text);
				} catch (ParseException e1) {
					e1.printStackTrace();
				}
				
			}
		});
		
	}
	
	private static void executeSearch(KeyEvent e) {
		if(e.getCode() == KeyCode.ENTER) {
			engine.load(audioSearch + inputField.getText() + "&auto_complete=1&sort=2&count=" + countField.getText().trim() +"&" + accessToken);
			
			try {
				URL url = new URL(engine.getLocation());
				HttpURLConnection connection = (HttpURLConnection) url.openConnection();
				
				if(connection.getResponseCode() == HttpURLConnection.HTTP_OK) {
					BufferedReader reader = new BufferedReader(new InputStreamReader(connection.getInputStream()));
					json = reader.readLine();	
					
					ObservableList<String> parsed = parseJson(json, parseField.getText().trim());
					listView.setItems(parsed);
				}
				
			} catch (IOException | ParseException e1) {
				e1.printStackTrace();
			}
		}
	}
	
	private static String executeLyrics(String id) {
		engine.load(lyricsSearch + "lyrics_id=" + id +"&" + accessToken);
		StringBuilder sb = new StringBuilder();
		
		try {
			URL url = new URL(engine.getLocation());
			HttpURLConnection connection = (HttpURLConnection) url.openConnection();
			
			if(connection.getResponseCode() == HttpURLConnection.HTTP_OK) {
				BufferedReader reader = new BufferedReader(new InputStreamReader(connection.getInputStream()));
				String line = "";
				while((line = reader.readLine()) != null) {
					sb.append(line);
				}
			}
			
		} catch (IOException e) {
			e.printStackTrace();
		}
		
		return sb.toString();
	}
	
	private static ObservableList<String> parseJson(String json, String parameter) throws ParseException {
		ObservableList<String> list = FXCollections.observableArrayList();
		
		JSONParser parser = new JSONParser();		
		JSONObject jsonObject = (JSONObject) parser.parse(json);
		
		JSONArray jsonArray = (JSONArray) jsonObject.get("response");
		
		for(int i=1; i<jsonArray.size(); ++i) {
			JSONObject tmp = (JSONObject) jsonArray.get(i);
			list.add(tmp.get(parameter).toString());
		}
		
		return list;
	}

}
