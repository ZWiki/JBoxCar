package org.jboxcar.gui;

import static org.jboxcar.util.Globals.gravity;
import static org.jboxcar.util.Globals.output;
import static org.jboxcar.util.Globals.viz;
import static org.jboxcar.util.Globals.world;

import org.jbox2d.dynamics.World;
import org.jboxcar.api.JBoxCarGraphicsAPI;

import javafx.animation.AnimationTimer;
import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Scene;
import javafx.stage.Stage;

public class JBoxCar extends Application {

	public static void main(String[] args){
		for (int i = 0; i < args.length; i++) {
			if (args[i].equalsIgnoreCase("output")) {
				output = true;
			} else if(args[i].equalsIgnoreCase("noviz")) {
				viz = false;
			}
		}
		if (viz){
			launch(args);
		} else {
			world = new World(gravity);
			world.setAllowSleep(true);
			JBoxCarGraphicsAPI jg;
			jg = new JBoxCarGraphicsAPI(10, 123120);
			if (output) {
				jg.setDoOutput(true);
				System.out.println("Generation: 0");
				jg.getGenerationListener().addPropertyChangeListener(
						evt -> {
							System.out.println("Generation: " + jg.getGenerationListener().getValue());
							System.out.flush();
						}
				);
			}
			jg.createFloor();
			AnimationTimer timer = new AnimationTimer() {
				@Override
				public void handle(long now) {
					jg.step();
					System.gc();
				}
			};
			timer.start();
		}
	}
	
	@Override
	public void start(Stage stage) throws Exception {
		Scene scene = new Scene(FXMLLoader.load(getClass().getResource("application.fxml")));
		stage.setScene(scene);
		
	}
	
}
