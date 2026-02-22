package Telemetria;

import java.io.*;
import java.util.*;

import org.jfree.chart.ChartFactory;
import org.jfree.chart.ChartUtils;
import org.jfree.chart.JFreeChart;
import org.jfree.chart.plot.PlotOrientation;
import org.jfree.data.category.DefaultCategoryDataset;

public class TelemetriaEstados {

	private Map<String, Integer> identificador = new HashMap<>();
	private Map<Integer, Integer> contador = new HashMap<>();

	public TelemetriaEstados(String archivoQTable) {
		cargarEstadosDesdeQTable(archivoQTable);
	}

	private void cargarEstadosDesdeQTable(String archivo) {
		try (BufferedReader br = new BufferedReader(new FileReader(archivo))) {

			String linea;
			int id = 0;

			while ((linea = br.readLine()) != null) {
				String[] partes = linea.split(",");
				if (partes.length < 4)
					continue;

				String estado = partes[0];

				identificador.put(estado, id);
				contador.put(id, 0);

				id++;
			}

		} catch (IOException e) {
			e.printStackTrace();
			System.err.println("Error al leer la Q-table para telemetria");
		}
	}

	public void registrarEstado(String estado) {
		Integer id = identificador.get(estado);
		if (id == null)
			return;

		contador.put(id, contador.get(id) + 1);
	}

	public void guardarHistograma(String nombre) {
		DefaultCategoryDataset dataset = new DefaultCategoryDataset();

		for (Map.Entry<Integer, Integer> e : contador.entrySet()) {
			dataset.addValue(e.getValue(), "Visitas", e.getKey());
		}

		JFreeChart chart = ChartFactory.createBarChart("Histograma", "Estado", "Ejecuciones", dataset,
				PlotOrientation.VERTICAL, false, true, false);

		try {
			ChartUtils.saveChartAsPNG(new File(nombre), chart, 1200, 800);
		} catch (IOException e) {
			e.printStackTrace();
			System.err.println("Error al guardar el histograma");
		}
	}
}
