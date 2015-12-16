package org.jboxcar.gui;

import static org.jboxcar.util.Globals.fpsBox2D;
import static org.jboxcar.util.Globals.gravity;
import static org.jboxcar.util.Globals.output;
import static org.jboxcar.util.Globals.title;
import static org.jboxcar.util.Globals.viz;
import static org.jboxcar.util.Globals.world;

import java.net.URL;
import java.util.ResourceBundle;

import org.jbox2d.dynamics.World;
import org.jboxcar.algorithm.JBCIndividual;
import org.jboxcar.api.JBoxCarGraphicsAPI;

import com.sun.javafx.perf.PerformanceTracker;

import javafx.animation.AnimationTimer;
import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.canvas.Canvas;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.control.RadioMenuItem;
import javafx.scene.control.TextField;
import javafx.scene.control.ToggleButton;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Pane;
import javafx.scene.paint.Color;
import javafx.scene.paint.CycleMethod;
import javafx.scene.paint.LinearGradient;
import javafx.scene.paint.Stop;
import javafx.scene.shape.Rectangle;
import javafx.scene.shape.StrokeLineCap;
import javafx.scene.shape.StrokeLineJoin;
import javafx.stage.Stage;

public class JBoxCarController implements Initializable{
	private GraphicsContext gc2d;
	private JBoxCarGraphicsAPI jg;
	private int populationSize = 10;
	private AnimationTimer timer;
	private PerformanceTracker tracker;
	private Scene scene;
	private float fpsNanoTimestamp = 0;
	private float fpsNanoUpdate = (float) (.5f * 1e9);
	@FXML private Canvas canvas;
	@FXML private ComboBox<String> cb_algorithm;
	@FXML private Label lbl_fps;
	@FXML private ToggleButton btn_play;
	@FXML private Label lbl_cars_left;
	@FXML private Label lbl_distance;
	@FXML private Label lbl_generation;
	@FXML private TextField tf_gravity;
	@FXML private Pane p_gradient;
	@FXML private GridPane gp_density;
	@FXML private RadioMenuItem rmi_speed;

	private float minChassisDensity = 30f;
	private float maxChassisDensity = 300f;
	private Stage stage;
	private boolean shouldClear = true;

	@Override
	public void initialize(URL location, ResourceBundle resources) {
		Platform.runLater(new Runnable(){

			@Override
			public void run() {
				scene = canvas.getScene();
				stage = (Stage)scene.getWindow();
				stage.setTitle(title);
				stage.show();
				initGradient();
				initCbAlgorithm();
				world = new World(gravity);
				world.setAllowSleep(true);
				gc2d = canvas.getGraphicsContext2D();
				gc2d.setLineCap(StrokeLineCap.ROUND);
				gc2d.setLineJoin(StrokeLineJoin.ROUND);
				gc2d.setLineWidth(.1);
				jg = new JBoxCarGraphicsAPI(gc2d, populationSize, 123120);
				lbl_cars_left.setText(String.format("Cars Left: %d",populationSize));
				lbl_generation.setText("Generation: " + 0);
				tf_gravity.setText(String.valueOf(gravity.y));
				// listener to update cars remaining
				jg.getCarsRemainingListener().addPropertyChangeListener(evt -> lbl_cars_left.setText(String.format("Cars Left: %d", jg.getCarsRemainingListener().getValue())));
				// listener to update distance of leader
				jg.getLeaderPositionListener().addPropertyChangeListener(evt -> lbl_distance.setText(String.format("Distance (m): %.2f", jg.getLeaderPositionListener().getValue().x)));
				// Listener to update generation
				jg.getGenerationListener().addPropertyChangeListener(evt -> lbl_generation.setText(String.format("Generation: %d", jg.getGenerationListener().getValue())));

				jg.createFloor();
				while (!canvas.isVisible()) { } 

				tracker =  PerformanceTracker.getSceneTracker(scene);
				timer = new AnimationTimer() {
					@Override
					public void handle(long now) {
						if (!rmi_speed.isSelected()) {
							if (!shouldClear) {
								shouldClear = true;
							}
							jg.drawScreen();
						} else if (shouldClear) {
							gc2d.clearRect(0, 0, canvas.getWidth(), canvas.getHeight());
							shouldClear = false;
						}

						jg.step();
						updateFps(now);
						System.gc();
					}
				};
				timer.start();

			}
		});



		//		stepTimer = new Timer(true);
		//		stepTimer.schedule(new TimerTask() {
		//			@Override
		//			public void run() {
		//				jg.step();
		//			}
		//		}, 0, 1000/fpsBox2D);


	}

	@FXML
	protected void handlePlayBtn() {
		if (btn_play.isSelected()) {
			btn_play.setText("Resume");
			timer.stop();
		} else {
			btn_play.setText("Pause");
			timer.start();
		}
	}

	@FXML
	protected void handleUpdateBtn() {
		System.out.println("here");
	}

	@FXML
	protected void handleRadioMenuItemDraw() {

	}

	private void initCbAlgorithm() {
		cb_algorithm.getItems().addAll(
				"GA",
				"PSO"
				);
	}

	private void initGradient() {

		int size = 360;
		Stop[] stops = new Stop[size];
		for (int i = 0; i < size; i++) {
			stops[i] = new Stop((double)i/(size-1), Color.hsb(i, 1.0, .8));
		}
		Rectangle rectangle = new Rectangle(p_gradient.getWidth(), p_gradient.getHeight(), 
				new LinearGradient(.5f, 1f, .5f, 0f, true, CycleMethod.NO_CYCLE, stops));
		p_gradient.getChildren().add(rectangle);

		float value = minChassisDensity;
		float increment = (maxChassisDensity - minChassisDensity)/9;
		for (int i = 9; i >= 0; i--) {
			HBox b = new HBox();
			b.setAlignment(Pos.CENTER_RIGHT);
			Label l = new Label(String.format("%.2f-", value));
			b.getChildren().add(l);
			gp_density.add(b, 1, i);
			value += increment;
		}
	}


	private void updateFps(float time) {
		if (Math.abs(time - fpsNanoTimestamp) > fpsNanoUpdate) {
			fpsNanoTimestamp = time;
			float fps = tracker.getAverageFPS();
			tracker.resetAverageFPS();
			lbl_fps.setText(String.format("FPS: %.3f", fps));
		}

	}

}
