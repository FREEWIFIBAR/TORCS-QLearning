package QLearning;

import java.io.*;
import java.util.*;

public class QTableStats {

	private static final int NUM_ACCIONES = 3;

	private final List<String> estados = new ArrayList<>();
	private final List<double[]> qVals = new ArrayList<>();

	public QTableStats(String nombre) throws IOException {
		cargarQTable(nombre);
	}

	private void cargarQTable(String nombre) throws IOException {
		try (BufferedReader br = new BufferedReader(new FileReader(nombre))) {
			String linea;
			while ((linea = br.readLine()) != null) {
				String[] p = linea.split(",");
				if (p.length < 4)
					continue;

				estados.add(p[0]);

				double[] q = new double[NUM_ACCIONES];
				for (int i = 0; i < NUM_ACCIONES; i++)
					q[i] = Double.parseDouble(p[i + 1]);

				qVals.add(q);
			}
		}
	}

	public int[] histogramaMejorAccion() {
		int[] histograma = new int[NUM_ACCIONES];
		for (double[] q : qVals)
			histograma[argMax(q)]++;
		return histograma;
	}

	private int argMax(double[] q) {
		int mejor = 0;
		for (int i = 1; i < q.length; i++)
			if (q[i] > q[mejor])
				mejor = i;
		return mejor;
	}

	private double maxQ(double[] q) {
		double max = q[0];
		for (int i = 1; i < q.length; i++)
			max = Math.max(max, q[i]);
		return max;
	}

	private double confianza(double[] q) {
		double mejor = Double.NEGATIVE_INFINITY;
		double segundo = Double.NEGATIVE_INFINITY;

		for (double v : q) {
			if (v > mejor) {
				segundo = mejor;
				mejor = v;
			} else if (v > segundo) {
				segundo = v;
			}
		}
		return mejor - segundo;
	}

	private double varianza(double[] q) {
		double media = Arrays.stream(q).average().orElse(0);
		double suma = 0;
		for (double v : q)
			suma += Math.pow(v - media, 2);
		return suma / q.length;
	}

	public double estadosPositivos() {
		long positivo = qVals.stream().filter(q -> maxQ(q) >= 0).count();
		return (double) positivo / qVals.size() * 100;
	}

	private static double round(double v) {
		return Math.round(v * 100.0) / 100.0;
	}

	public void mostrarEstadisticas() {
		System.out.println("=== ESTADISTICAS DE LA TABLA Q ===");
		System.out.println("\nEstados: " + qVals.size());

		int[] h = histogramaMejorAccion();
		System.out.println("\nPolitica aprendida:");
		for (int i = 0; i < h.length; i++)
			System.out.println(" Accion optima " + i + ": " + h[i] + " estados");

		System.out.println("\nValor medio : " + round(qVals.stream().mapToDouble(this::maxQ).average().orElse(0.0)));
		System.out.println("Valor minimo: " + round(qVals.stream().mapToDouble(this::maxQ).min().orElse(0.0)));
		System.out.println("Valor maximo: " + round(qVals.stream().mapToDouble(this::maxQ).max().orElse(0.0)));

		System.out.println("\nConfianza: " + round(qVals.stream().mapToDouble(this::confianza).average().orElse(0.0)));
		System.out.println("Varianza: " + round(qVals.stream().mapToDouble(this::varianza).average().orElse(0.0)));

		System.out.println("\nEstados positivos: " + round(estadosPositivos()) + "%");
	}

	public static void main(String[] args) throws Exception {
		QTableStats qTable = new QTableStats("qtable_accel.csv");
		qTable.mostrarEstadisticas();
	}
}
