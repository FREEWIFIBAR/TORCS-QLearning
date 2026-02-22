package Datos;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;

import champ2011client.Action;
import champ2011client.SensorModel;

public class RecolectorDatos {

	private PrintWriter pw = null;
	private boolean header = false;
	private final String dataset = "dataset.csv";

	public void guardarDatos(SensorModel sensors, Action action) {
		try {
			if (pw == null) {
				pw = new PrintWriter(new FileWriter(dataset, true));
			}

			if (!header) {
				File f = new File(dataset);
				if (f.length() == 0) {
					StringBuilder sb = new StringBuilder();

					sb.append("angle,");
					sb.append("track_pos,");
					sb.append("z,");

					sb.append("speed,");
					sb.append("rpm,");
					sb.append("gear,");

					sb.append("wheel_spin_0,");
					sb.append("wheel_spin_1,");
					sb.append("wheel_spin_2,");
					sb.append("wheel_spin_3,");

					for (int i = 0; i < sensors.getTrackEdgeSensors().length; i++) {
						sb.append("track_").append(i).append(",");
					}

					for (int i = 0; i < sensors.getFocusSensors().length; i++) {
						sb.append("focus_").append(i).append(",");
					}

					sb.append("A_gear,");
					sb.append("A_steer,");
					sb.append("A_accel,");
					sb.append("A_brake");

					pw.println(sb.toString());
				}
				header = true;
			}

			StringBuilder sb = new StringBuilder();

			sb.append(sensors.getAngleToTrackAxis()).append(",");
			sb.append(sensors.getTrackPosition()).append(",");
			sb.append(sensors.getZ()).append(",");

			sb.append(sensors.getSpeed()).append(",");
			sb.append(sensors.getRPM()).append(",");
			sb.append(sensors.getGear()).append(",");

			double[] wheelSpin = sensors.getWheelSpinVelocity();
			for (double w : wheelSpin) {
				sb.append(w).append(",");
			}

			double[] track = sensors.getTrackEdgeSensors();
			for (double t : track) {
				sb.append(t).append(",");
			}

			double[] focus = sensors.getFocusSensors();
			for (double f : focus) {
				sb.append(f).append(",");
			}

			sb.append(action.gear).append(",");
			sb.append(action.steering).append(",");
			sb.append(action.accelerate).append(",");
			sb.append(action.brake);

			pw.println(sb.toString());
			pw.flush();

		} catch (IOException e) {
			System.err.println("Error escribiendo el CSV: " + e.getMessage());
		}
	}
}
