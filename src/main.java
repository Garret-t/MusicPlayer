import java.io.File;
import java.io.FilenameFilter;
import java.util.*;

import javafx.application.Application;
import javafx.application.Platform;
import javafx.beans.InvalidationListener;
import javafx.beans.binding.Bindings;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.event.ActionEvent;
import javafx.event.EventHandler;
import javafx.geometry.HPos;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.ListView;
import javafx.scene.control.OverrunStyle;
import javafx.scene.control.Slider;
import javafx.scene.control.ToolBar;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.ColumnConstraints;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.RowConstraints;
import javafx.scene.layout.StackPane;
import javafx.scene.media.Media;
import javafx.scene.media.MediaPlayer;
import javafx.scene.media.MediaPlayer.Status;
import javafx.scene.text.Text;
import javafx.scene.text.TextAlignment;
import javafx.stage.DirectoryChooser;
import javafx.stage.Stage;
import javafx.stage.StageStyle;
import javafx.util.Duration;

public class main extends Application{
	Button pathBtn, skipBtn, shufBtn;
	static Button playBtn;
	
	Stage window;
	GridPane grid;
	static Label time, totalTime;
	
	File folder;
	DirectoryChooser dirChoose;
	
	Status mpstatus;
	Media media; 
	static MediaPlayer mp;
	
	static ArrayList<String> files, querry;
	static ListView<String> list;
	ObservableList<String> songs;
	
	public static int pos = 0;
	
	static Duration totalDur;
	static String realDur;
	static Duration curDur;
	
	static Slider timeSlider = new Slider(), volSlider = new Slider();
	static double vol;
	
	static Boolean shuffle = false;
	
	public static void setPlayer(ArrayList<String> q) {
		try {
			//repeat the playlist
			if (pos == querry.toArray().length) {
				pos = 0;
			}
			playBtn.setText("||");
			
			//make a new media player at the position of the next song
			mp = new MediaPlayer(new Media(q.get(pos)));
			//go to next song at the end
			mp.setOnReady(new Runnable() {
				@Override
				public void run() {
					mp.play();
					mp.setVolume(vol);
					
					list.scrollTo(pos);
					list.getSelectionModel().select(pos);
					
					totalDur = mp.getTotalDuration();
					realDur = String.format("%4d:%02d:%04.1f",
							(int) totalDur.toHours(),
							(int) totalDur.toMinutes() % 60,
				        totalDur.toSeconds() % 3600);
					time.textProperty().bind(
							Bindings.createStringBinding(()-> {
								curDur = mp.getCurrentTime();
								return String.format("%4d:%02d:%04.1f",
									(int) curDur.toHours(),
									(int) curDur.toMinutes() % 60,
						        curDur.toSeconds() % 3600);
							}, mp.currentTimeProperty()));
					totalTime.setText(realDur);
					
					
					//timeslider properties robbed from varius sources such as stack exchange and the javafx media docs
					mp.totalDurationProperty().addListener((obs, oldDuration, newDuration) -> timeSlider.setMax(newDuration.toSeconds()));
			        timeSlider.valueChangingProperty().addListener((obs, wasChanging, isChanging) -> {
			            if (!isChanging) {
			                mp.seek(Duration.seconds(timeSlider.getValue()));
			            }
			        });
				    timeSlider.valueProperty().addListener((obs, oldValue, newValue) -> {
				        if (!timeSlider.isValueChanging()) {
				        	 double currentTime = mp.getCurrentTime().toSeconds();
				        	 if (Math.abs(currentTime - newValue.doubleValue()) > .5) {
				        		 mp.seek(Duration.seconds(newValue.doubleValue()));
				             }
				        }
				    });
			        mp.currentTimeProperty().addListener((obs, oldTime, newTime) -> {
			            if (!timeSlider.isValueChanging()) {
			                timeSlider.setValue(newTime.toSeconds());
			            }
			        });
			        timeSlider.maxProperty().bind(
			        	Bindings.createDoubleBinding(
			        		() -> mp.getTotalDuration().toSeconds(),
			        	mp.totalDurationProperty()));
			        
				}
			});
			
			mp.setOnEndOfMedia(new Runnable() {
				@Override
				public void run() {
					next();
				}
			});
		} catch(Exception e) {
			System.out.println(e.getMessage());
		}
	}
	
	public static void next() {
		playBtn.setText(">");
		//delete currently running one
		mp.stop();
		mp.dispose();
		
		if (shuffle) {
			pos = (int )(Math.random() * querry.toArray().length + 0);
		} else { 
			pos += 1; //sets position to queue next song 
		}
		setPlayer(querry); //next
	}
	
	public static void main(String[] args) {
		launch(args);
	}
	
	//The main entry point for JavaFx applications
	//The stage is the top level container 
	public void start(Stage primaryStage) {
		window = primaryStage;
		window.setTitle("Music Player");
		
		//time labels
		time = new Label();
		totalTime = new Label();
		
		vol = 50;
		volSlider.setValue(vol);
		volSlider.valueProperty().addListener(new InvalidationListener() {
			@Override
		    public void invalidated(javafx.beans.Observable ov) {
		        if (volSlider.isValueChanging()) {
		        	vol = volSlider.getValue() / 100.0;
		        	mp.setVolume(vol);
		        } else
		        	volSlider.setValue((int)Math.round(mp.getVolume() * 100));
		    }
		});
		//Play and pause button
		playBtn = new Button();
		playBtn.setText(">");
		playBtn.setOnAction(new EventHandler<ActionEvent>() {
			@Override
			public void handle(ActionEvent event) {
				mpstatus = mp.getStatus();
				if (mpstatus != Status.PLAYING) {
					mp.play();
					playBtn.setText("||");
				}
				else {
					mp.pause();
					playBtn.setText(">");
				}
			}
		});

		list = new ListView<String>();
		
		//Folder button
		pathBtn = new Button();
		pathBtn.setText("Folder");
		pathBtn.setOnAction(new EventHandler<ActionEvent>() {
			@Override
			public void handle(ActionEvent event) {

				//asks the user for a directory
				dirChoose = new DirectoryChooser();
				folder = dirChoose.showDialog(window);
				
				if(folder != null) {
					try {
						next();
						pos = 0;
					} catch(Exception e){
						
					}
					files = new ArrayList<>();
					//gets the folder and files in it
					folder.getAbsolutePath();
					files = new ArrayList<String>(Arrays.asList(folder.list(new FilenameFilter(){
				        @Override
				        public boolean accept(File dir, String name) {
				            return name.endsWith(".mp3") || name.endsWith(".mp4") || name.endsWith(".wav");
				        }
					})));
					
					querry = new ArrayList<>(files);
					//sets the path into something that the media will recognize
					for (int i = 0; i < files.toArray().length; i++) {
						querry.set(i,("file:\\\\\\" + folder.getPath() + "\\" + files.get(i)).replace("\\", "/"));
					}
					songs = FXCollections.observableArrayList(files);
					list.setItems(songs);
				    list.setOnMouseClicked(new EventHandler<MouseEvent>() {

				        @Override
				        public void handle(MouseEvent event) {
				            pos =  list.getSelectionModel().getSelectedIndex() - 1;
				            next();
				        }
				    });
					setPlayer(querry);
				}
			}
		});
		
		list.setMaxSize(400, 400);
		
		
		//Skip Button
		skipBtn = new Button();
		skipBtn.setText(">|");
		skipBtn.setOnAction(new EventHandler<ActionEvent>() {
			@Override
			public void handle(ActionEvent event) {
				next();
			}
		});
		
		//shuffle Button
		shufBtn = new Button();
		shufBtn.setText("Shuffle");
		shufBtn.setOnAction(new EventHandler<ActionEvent>() {
			@Override
			public void handle(ActionEvent event) {
				if (shuffle) {
					shuffle = false;
				} else {
					shuffle = true;
				}
				next();
			}
		});
		
		//creates the grid and gaps and padding
		grid = new GridPane();
		grid.setHgap(5);
		grid.setVgap(5);
		grid.setPadding(new Insets(5, 5, 5, 5));
		
		//Constraints
		RowConstraints row = new RowConstraints();
		ColumnConstraints col = new ColumnConstraints(25);
		row.setVgrow(Priority.NEVER);
		col.setHgrow(Priority.ALWAYS);
        grid.getRowConstraints().addAll(row);
		grid.getColumnConstraints().addAll(col);
		
		//position of everything on the grid
		grid.add(playBtn, 0, 1);
		grid.add(list, 0, 2, 5, 3);
		grid.add(skipBtn, 1, 1);
		grid.add(time, 5, 1);
		grid.add(totalTime, 5, 2);
		grid.add(pathBtn, 5, 3);
		grid.add(timeSlider, 4, 1);
		grid.add(volSlider, 3, 1);
		grid.add(shufBtn, 2, 1);
		
		Scene scene = new Scene(grid, 475, 200);
		//sets the scene inside the primary stage window
		scene.getStylesheets().add(main.class.getResource("player.css").toExternalForm());
		window.setScene(scene);
		window.setResizable(false);
		window.show();
	}
}
