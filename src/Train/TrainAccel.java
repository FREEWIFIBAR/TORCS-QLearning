package Train;

import QLearning.QLearning;
import Telemetria.TelemetriaRecompensa;
import champ2011client.Action;
import champ2011client.Controller;
import champ2011client.SensorModel;

public class TrainAccel extends Controller {

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

	// current clutch
	private float clutch = 0;

	private boolean reiniciando = false;

	private QLearning ql = new QLearning("qtable_accel.csv");
	private String lastEstado = null;
	private int lastAccion = 1;
	private double lastDist = 0.0;

	private TelemetriaRecompensa chart = new TelemetriaRecompensa();

	public void reset() {
		reiniciando = false;
		ql.guardarQTable();
		System.out.println("Restarting the race!");
	}

	public void shutdown() {
		ql.guardarQTable();
		chart.guardarGraficas();
		System.out.println("Bye bye!");
	}

	private String getEstado(SensorModel sensors) { // 90 estados
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

	private double getRecompensa(SensorModel sensors, double lastDist) {
		double recompensa = 0.0;

		double distancia = sensors.getDistanceRaced() - lastDist;
		recompensa += distancia * 5.0;

		double medio = sensors.getTrackEdgeSensors()[9];
		double velocidad = sensors.getSpeed();
		double angulo = Math.abs(sensors.getAngleToTrackAxis());
		double posicion = Math.abs(sensors.getTrackPosition());

		double riesgo = velocidad / Math.max(medio, 1.0);

		if (riesgo < 1.0) {
			if (lastAccion == 2)
				recompensa += 0.6;
			if (lastAccion == 0)
				recompensa -= 0.2;
		} else if (riesgo < 2.0) {
			if (lastAccion == 1)
				recompensa += 0.5;
			if (lastAccion == 2)
				recompensa -= 0.5;
		} else {
			if (lastAccion == 0)
				recompensa += 1.2;
			if (lastAccion == 1)
				recompensa += 0.3;
			if (lastAccion == 2)
				recompensa -= 3.5;
		}

		if (velocidad > 160 && medio < 60 && lastAccion == 2) {
			recompensa -= 3.0;
		}

		if (velocidad > 160 && medio < 60 && lastAccion == 0) {
			recompensa += 1.0;
		}

		recompensa -= angulo * 2.0;
		recompensa -= posicion * 2.0;

		if (riesgo > 2.0 && medio < 35)
			recompensa -= 1.5;

		if (posicion > 0.85)
			recompensa -= 5.0;

		return Math.max(-5.0, Math.min(5.0, recompensa));
	}

	private int getGear(SensorModel sensors) {
		int gear = sensors.getGear();
		double rpm = sensors.getRPM();

		// if gear is 0 (N) or -1 (R) just return 1
		if (gear < 1)
			return 1;
		// check if the RPM value of car is greater than the one suggested
		// to shift up the gear from the current one
		if (gear < 6 && rpm >= gearUp[gear - 1])
			return gear + 1;
		else
		// check if the RPM value of car is lower than the one suggested
		// to shift down the gear from the current one
		if (gear > 1 && rpm <= gearDown[gear - 1])
			return gear - 1;
		else // otherwhise keep current gear
			return gear;
	}

	private float getSteer(SensorModel sensors) {
		// steering angle is compute by correcting the actual car angle w.r.t. to track
		// axis [sensors.getAngle()] and to adjust car position w.r.t to middle of track
		// [sensors.getTrackPos()*0.5]
		float targetAngle = (float) (sensors.getAngleToTrackAxis() - sensors.getTrackPosition() * 0.5);
		// at high speed reduce the steering command to avoid loosing the control
		if (sensors.getSpeed() > steerSensitivityOffset)
			return (float) (targetAngle
					/ (steerLock * (sensors.getSpeed() - steerSensitivityOffset) * wheelSensitivityCoeff));
		else
			return (targetAngle) / steerLock;
	}

	private float getAccel(SensorModel sensors) {
		String estado = getEstado(sensors);
		int accion;

		accion = ql.elegirAccionTrain(estado);

		if (lastEstado != null) {
			double recompensa = getRecompensa(sensors, lastDist);
			ql.actualizarQ(lastEstado, lastAccion, recompensa, estado);
			chart.actualizarDatos(ql.getEpsilon(), recompensa);
		}

		lastDist = sensors.getDistanceRaced();
		lastEstado = estado;
		lastAccion = accion;

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

		if (!reiniciando) {
			if (Math.abs(sensors.getTrackPosition()) > 0.9 || sensors.getCurrentLapTime() > 270) {
				reiniciando = true;
				System.out.println(" Fuera de pista, reiniciando...");

				lastEstado = null;
				lastAccion = 1;
				lastDist = 0.0;
				clutch = 0;

				Action reset = new Action();
				reset.restartRace = true;
				return reset;
			}
		}

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
