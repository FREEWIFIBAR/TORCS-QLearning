package QLearning;

import champ2011client.Action;
import champ2011client.Controller;
import champ2011client.SensorModel;

public class Piloto extends Controller {

	/* Gear Changing Constants */
	final int[] gearUp = { 5000, 6000, 6000, 6500, 7000, 0 };
	final int[] gearDown = { 0, 2500, 3000, 3000, 3500, 3500 };

	/* Stuck constants */
	final int stuckTime = 25;
	final float stuckAngle = (float) 0.523598775; // PI/6

	/* Accel and Brake Constants */
	final float maxSpeedDist = 70;
	final float maxSpeed = 150;
	final float sin5 = (float) 0.08716;
	final float cos5 = (float) 0.99619;

	/* Steering constants */
	final float steerLock = (float) 0.785398;
	final float steerSensitivityOffset = (float) 80.0;
	final float wheelSensitivityCoeff = 1;

	/* ABS Filter Constants */
	final float wheelRadius[] = { (float) 0.3179, (float) 0.3179, (float) 0.3276, (float) 0.3276 };
	final float absSlip = (float) 2.0;
	final float absRange = (float) 3.0;
	final float absMinSpeed = (float) 3.0;

	/* Clutching Constants */
	final float clutchMax = (float) 0.5;
	final float clutchDelta = (float) 0.05;
	final float clutchRange = (float) 0.82;
	final float clutchDeltaTime = (float) 0.02;
	final float clutchDeltaRaced = 10;
	final float clutchDec = (float) 0.01;
	final float clutchMaxModifier = (float) 1.3;
	final float clutchMaxTime = (float) 1.5;

	private int stuck = 0;

	// current clutch
	private float clutch = 0;
	
	private QLearning ql_gear = new QLearning("qtable_gear.csv");
	private QLearning ql_steer = new QLearning("qtable_steer.csv");
	private QLearning ql_accel = new QLearning("qtable_accel.csv");

	public void reset() {
		System.out.println("Restarting the race!");
	}

	public void shutdown() {
		System.out.println("Bye bye!");
	}
	
	private String getEstadoGear(SensorModel sensors) { // 72 estados
		int rpmBin;
		if (sensors.getRPM() < 3000)
			rpmBin = 0;
		else if (sensors.getRPM() < 5000)
			rpmBin = 1;
		else if (sensors.getRPM() < 6500)
			rpmBin = 2;
		else
			rpmBin = 3;

		int speedBin;
		if (sensors.getSpeed() < 60)
			speedBin = 0;
		else if (sensors.getSpeed() < 120)
			speedBin = 1;
		else
			speedBin = 2;

		int gear = Math.max(1, sensors.getGear());

		return rpmBin + "_" + speedBin + "_" + gear;
	}
	
	private String getEstadoSteer(SensorModel sensors) { // 75 estados

		double pos = sensors.getTrackPosition();
		double angle = sensors.getAngleToTrackAxis();

		int posBin;
		if (pos < -0.8)
			posBin = 0;
		else if (pos < -0.3)
			posBin = 1;
		else if (pos < 0.3)
			posBin = 2;
		else if (pos < 0.8)
			posBin = 3;
		else
			posBin = 4;

		int angleBin;
		if (angle < -0.25)
			angleBin = 0;
		else if (angle < -0.08)
			angleBin = 1;
		else if (angle < 0.08)
			angleBin = 2;
		else if (angle < 0.25)
			angleBin = 3;
		else
			angleBin = 4;

		double left = sensors.getTrackEdgeSensors()[8];
		double mid = sensors.getTrackEdgeSensors()[9];
		double right = sensors.getTrackEdgeSensors()[10];

		int curva;
		if (mid > 70)
			curva = 0;
		else if (left > right)
			curva = 1;
		else
			curva = 2;

		return posBin + "_" + angleBin + "_" + curva;
	}
	
	private String getEstadoAccel(SensorModel sensors) { // 90 estados
		int distBin;
		int speedBin;
		int riesgoBin;

		double d = sensors.getTrackEdgeSensors()[9];

		if (d < 25)
			distBin = 0;
		else if (d < 50)
			distBin = 1;
		else if (d < 80)
			distBin = 2;
		else if (d < 120)
			distBin = 3;
		else
			distBin = 4;

		double v = sensors.getSpeed();

		if (v < 40)
			speedBin = 0;
		else if (v < 70)
			speedBin = 1;
		else if (v < 100)
			speedBin = 2;
		else if (v < 130)
			speedBin = 3;
		else if (v < 160)
			speedBin = 4;
		else
			speedBin = 5;

		double riesgo = v / Math.max(d, 1.0);

		if (riesgo < 1.0)
			riesgoBin = 0;
		else if (riesgo < 2.0)
			riesgoBin = 1;
		else
			riesgoBin = 2;

		return distBin + "_" + speedBin + "_" + riesgoBin;
	}

	private int getGear(SensorModel sensors) {
		String estado = getEstadoGear(sensors);

		int accion = ql_gear.elegirAccionTest(estado);

		int gear = sensors.getGear();

		switch (accion) {
		case 0:
			gear = Math.max(1, gear - 1);
			break;
		case 1:
			// mantener
			break;
		case 2:
			gear = Math.min(6, gear + 1);
			break;
		}

		return gear;
	}

	private float getSteer(SensorModel sensors) {
		String estado = getEstadoSteer(sensors);

		int accion = ql_steer.elegirAccionTest(estado);

		float steer = 0.0f;

		switch (accion) {
		case 0:
			steer = -0.3f;
			break;
		case 1:
			// mantener
			break;
		case 2:
			steer = 0.3f;
			break;
		}

		return steer;
	}

	private float getAccel(SensorModel sensors) {
		String estado = getEstadoAccel(sensors);

		int accion = ql_accel.elegirAccionTest(estado);

		float accel = 0.0f;

		switch (accion) {
		case 0:
			accel = -0.6f;
			break;
		case 1:
			// mantener
			break;
		case 2:
			accel = 1.0f;
			break;
		}

		return accel;
	}

	public Action control(SensorModel sensors) {
		// check if car is currently stuck
		if (Math.abs(sensors.getAngleToTrackAxis()) > stuckAngle) {
			// update stuck counter
			stuck++;
		} else {
			// if not stuck reset stuck counter
			stuck = 0;
		}

		// after car is stuck for a while apply recovering policy
		if (stuck > stuckTime) {
			/*
			 * set gear and sterring command assuming car is pointing in a direction out of
			 * track
			 */

			// to bring car parallel to track axis
			float steer = (float) (-sensors.getAngleToTrackAxis() / steerLock);
			int gear = -1; // gear R

			// if car is pointing in the correct direction revert gear and steer
			if (sensors.getAngleToTrackAxis() * sensors.getTrackPosition() > 0) {
				gear = 1;
				steer = -steer;
			}
			clutch = clutching(sensors, clutch);
			// build a CarControl variable and return it
			Action action = new Action();
			action.gear = gear;
			action.steering = steer;
			action.accelerate = 1.0;
			action.brake = 0;
			action.clutch = clutch;
			return action;
		}

		else // car is not stuck
		{
			// compute accel/brake command
			float accel_and_brake = getAccel(sensors);
			// compute gear
			int gear = getGear(sensors);
			// compute steering
			float steer = getSteer(sensors);

			// normalize steering
			if (steer < -1)
				steer = -1;
			if (steer > 1)
				steer = 1;

			// set accel and brake from the joint accel/brake command
			float accel, brake;
			if (accel_and_brake > 0) {
				accel = accel_and_brake;
				brake = 0;
			} else {
				accel = 0;
				// apply ABS to brake
				brake = filterABS(sensors, -accel_and_brake);
			}

			clutch = clutching(sensors, clutch);

			// build a CarControl variable and return it
			Action action = new Action();
			action.gear = gear;
			action.steering = steer;
			action.accelerate = accel;
			action.brake = brake;
			action.clutch = clutch;
			return action;
		}
	}

	private float filterABS(SensorModel sensors, float brake) {
		// convert speed to m/s
		float speed = (float) (sensors.getSpeed() / 3.6);
		// when spedd lower than min speed for abs do nothing
		if (speed < absMinSpeed)
			return brake;

		// compute the speed of wheels in m/s
		float slip = 0.0f;
		for (int i = 0; i < 4; i++) {
			slip += sensors.getWheelSpinVelocity()[i] * wheelRadius[i];
		}
		// slip is the difference between actual speed of car and average speed of
		// wheels
		slip = speed - slip / 4.0f;
		// when slip too high applu ABS
		if (slip > absSlip) {
			brake = brake - (slip - absSlip) / absRange;
		}

		// check brake is not negative, otherwise set it to zero
		if (brake < 0)
			return 0;
		else
			return brake;
	}

	float clutching(SensorModel sensors, float clutch) {

		float maxClutch = clutchMax;

		// Check if the current situation is the race start
		if (sensors.getCurrentLapTime() < clutchDeltaTime && getStage() == Stage.RACE
				&& sensors.getDistanceRaced() < clutchDeltaRaced)
			clutch = maxClutch;

		// Adjust the current value of the clutch
		if (clutch > 0) {
			double delta = clutchDelta;
			if (sensors.getGear() < 2) {
				// Apply a stronger clutch output when the gear is one and the race is just
				// started
				delta /= 2;
				maxClutch *= clutchMaxModifier;
				if (sensors.getCurrentLapTime() < clutchMaxTime)
					clutch = maxClutch;
			}

			// check clutch is not bigger than maximum values
			clutch = Math.min(maxClutch, clutch);

			// if clutch is not at max value decrease it quite quickly
			if (clutch != maxClutch) {
				clutch -= delta;
				clutch = Math.max((float) 0.0, clutch);
			}
			// if clutch is at max value decrease it very slowly
			else
				clutch -= clutchDec;
		}
		return clutch;
	}

	public float[] initAngles() {

		float[] angles = new float[19];

		/*
		 * set angles as
		 * {-90,-75,-60,-45,-30,-20,-15,-10,-5,0,5,10,15,20,30,45,60,75,90}
		 */
		for (int i = 0; i < 5; i++) {
			angles[i] = -90 + i * 15;
			angles[18 - i] = 90 - i * 15;
		}

		for (int i = 5; i < 9; i++) {
			angles[i] = -20 + (i - 5) * 5;
			angles[18 - i] = 20 - (i - 5) * 5;
		}
		angles[9] = 0;
		return angles;
	}
}
