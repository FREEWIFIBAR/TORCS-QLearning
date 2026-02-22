package Telemetria;

import org.jfree.chart.ChartFactory;
import org.jfree.chart.ChartPanel;
import org.jfree.chart.ChartUtils;
import org.jfree.chart.JFreeChart;
import org.jfree.chart.plot.PlotOrientation;
import org.jfree.data.category.DefaultCategoryDataset;

import javax.swing.*;
import java.awt.*;
import java.io.File;

public class TelemetriaProgreso extends JFrame {

	private DefaultCategoryDataset progresoDataset;

	public TelemetriaProgreso() {
		super("Telemetria");

		progresoDataset = new DefaultCategoryDataset();

		JPanel panel = new JPanel(new GridLayout(1, 1));
		panel.add(crearGraficoBarras(progresoDataset, "Progreso del Test", "Intento", "Distancia (m)"));

		setContentPane(panel);
		setSize(1200, 800);
		setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
		setVisible(true);
	}

	private ChartPanel crearGraficoBarras(DefaultCategoryDataset dataset, String titulo, String ejeX, String ejeY) {
		JFreeChart chart = ChartFactory.createBarChart(titulo, ejeX, ejeY, dataset, PlotOrientation.VERTICAL, true,
				true, false);
		return new ChartPanel(chart);
	}

	public void registrarProgreso(int intento, double distancia) {
		progresoDataset.addValue(distancia, "Distancia recorrida", String.valueOf(intento));
	}

	public void guardarGrafica() {
		try {
			JFreeChart chart = ChartFactory.createBarChart("Progreso del Test", "Intento", "Distancia (m)",
					progresoDataset, PlotOrientation.VERTICAL, true, true, false);

			File archivo = new File("GraficaProgreso.png");
			ChartUtils.saveChartAsPNG(archivo, chart, 1200, 800);

		} catch (Exception e) {
			e.printStackTrace();
			System.err.println("Error al guardar la grafica");
		}
	}
}
