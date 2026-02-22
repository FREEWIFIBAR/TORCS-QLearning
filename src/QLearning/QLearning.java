package QLearning;

import java.io.*;
import java.util.*;

public class QLearning {

	private static final double ALPHA = 0.1;
	private static final double GAMMA = 0.9;

	private double epsilon = 1;
	private static final double EPSILON_MIN = 0.05;
	private static final double EPSILON_DECAY = 0.99995;

	private static final int NUM_ACCIONES = 3;

	private Map<String, double[]> tablaQ = new HashMap<>();
	private Random random = new Random();
	private final String archivo;

	public QLearning(String nombreArchivo) {
		this.archivo = nombreArchivo;
		cargarQTable();
	}

	private double[] initEstado() {
		return new double[NUM_ACCIONES];
	}

	private int mejorAccion(double[] qValues) {
		double max = Double.NEGATIVE_INFINITY;
		List<Integer> mejoresAcciones = new ArrayList<>();

		for (int i = 0; i < qValues.length; i++) {
			if (qValues[i] > max) {
				max = qValues[i];
				mejoresAcciones.clear();
				mejoresAcciones.add(i);
			} else if (qValues[i] == max) {
				mejoresAcciones.add(i);
			}
		}

		return mejoresAcciones.get(random.nextInt(mejoresAcciones.size()));
	}

	public int elegirAccionTrain(String estado) {
		tablaQ.putIfAbsent(estado, initEstado());

		int accion;
		if (random.nextDouble() < epsilon) {
			accion = random.nextInt(NUM_ACCIONES);
		} else {
			accion = mejorAccion(tablaQ.get(estado));
		}

		epsilon = Math.max(EPSILON_MIN, epsilon * EPSILON_DECAY);
		return accion;
	}

	public int elegirAccionTest(String estado) {
		double[] q = tablaQ.get(estado);

		if (q == null) {
			return 1;
		}

		int mejor = 0;
		for (int i = 1; i < NUM_ACCIONES; i++) {
			if (q[i] > q[mejor])
				mejor = i;
		}
		return mejor;
	}

	public void actualizarQ(String estado, int accion, double recompensa, String nuevoEstado) {
		tablaQ.putIfAbsent(estado, initEstado());
		tablaQ.putIfAbsent(nuevoEstado, initEstado());

		double qActual = tablaQ.get(estado)[accion];

		double[] qNext = tablaQ.get(nuevoEstado);
		double maxQNuevo = Math.max(qNext[0], Math.max(qNext[1], qNext[2]));

		double nuevoQ = qActual + ALPHA * (recompensa + GAMMA * maxQNuevo - qActual);
		tablaQ.get(estado)[accion] = nuevoQ;
	}

	public double getEpsilon() {
		return epsilon;
	}

	public void guardarQTable() {
		try (BufferedWriter bw = new BufferedWriter(new FileWriter(archivo))) {
			for (var e : tablaQ.entrySet()) {
				bw.write(e.getKey() + "," + e.getValue()[0] + "," + e.getValue()[1] + "," + e.getValue()[2]);
				bw.newLine();
			}
			System.out.println("[QLearning] Tabla Q guardada en " + archivo + " (" + tablaQ.size() + " estados)");
		} catch (IOException ex) {
			System.err.println("[QLearning] Error al guardar Q-table: " + ex.getMessage());
		}
	}

	private void cargarQTable() {
		File f = new File(archivo);
		if (!f.exists()) {
			System.out.println("[QLearning] No se encontró " + archivo);
			return;
		}

		try (BufferedReader br = new BufferedReader(new FileReader(f))) {
			String linea;
			while ((linea = br.readLine()) != null) {
				String[] p = linea.split(",");
				if (p.length < 4)
					continue;

				double[] qVals = new double[NUM_ACCIONES];
				for (int i = 0; i < NUM_ACCIONES; i++) {
					qVals[i] = Double.parseDouble(p[i + 1]);
				}

				tablaQ.put(p[0], qVals);
			}
			System.out.println("[QLearning] Cargada tabla Q con " + tablaQ.size() + " estados.");
		} catch (Exception ex) {
			System.err.println("[QLearning] Error al cargar Q-table: " + ex.getMessage());
		}
	}
}
