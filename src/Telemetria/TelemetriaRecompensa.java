package Telemetria;

import org.jfree.chart.ChartFactory;
import org.jfree.chart.ChartPanel;
import org.jfree.chart.ChartUtils;
import org.jfree.chart.JFreeChart;
import org.jfree.chart.plot.PlotOrientation;
import org.jfree.data.xy.XYSeries;
import org.jfree.data.xy.XYSeriesCollection;

import javax.swing.*;
import java.awt.*;
import java.io.File;
import java.io.IOException;
import java.util.ArrayDeque;
import java.util.Deque;

public class TelemetriaRecompensa extends JFrame {

	private XYSeries epsilonSerie;
	private XYSeries recompensaSerie;

	private JFreeChart epsilonChart;
	private JFreeChart recompensaChart;

	private static final int WINDOW = 10000;

	private Deque<Double> bufferRecompensa = new ArrayDeque<>();
	private double sumaRecompensa = 0.0;

	private double tiempo = 0.0;

	public TelemetriaRecompensa() {
		super("Telemetria");

		epsilonSerie = new XYSeries("Epsilon");
		recompensaSerie = new XYSeries("Recompensa");

		JPanel panel = new JPanel(new GridLayout(2, 1));
		panel.add(crearGrafico(epsilonSerie, "Epsilon ", "Tiempo (s)", "Valor"));
		panel.add(crearGrafico(recompensaSerie, "Recompensa", "Tiempo (s)", "Valor"));

		setContentPane(panel);
		setSize(1200, 800);
		setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
		setVisible(true);
	}

	private ChartPanel crearGrafico(XYSeries serie, String titulo, String ejeX, String ejeY) {
		XYSeriesCollection dataset = new XYSeriesCollection(serie);
		JFreeChart chart = ChartFactory.createXYLineChart(titulo, ejeX, ejeY, dataset, PlotOrientation.VERTICAL, true,
				true, false);

		if (titulo.contains("Epsilon")) {
			epsilonChart = chart;
		} else if (titulo.contains("Recompensa")) {
			recompensaChart = chart;
		}

		return new ChartPanel(chart);
	}

	public void actualizarDatos(double epsilon, double recompensa) {
		tiempo += 0.02; // 20ms
		epsilonSerie.add(tiempo, epsilon);

		bufferRecompensa.addLast(recompensa);
		sumaRecompensa += recompensa;

		if (bufferRecompensa.size() > WINDOW) {
			sumaRecompensa -= bufferRecompensa.removeFirst();
		}

		double mediaMovil = sumaRecompensa / bufferRecompensa.size();
		recompensaSerie.add(tiempo, mediaMovil);
	}

	public void guardarGraficas() {
		try {
			ChartUtils.saveChartAsPNG(new File("GraficaEpsilon.png"), epsilonChart, 1200, 800);
			ChartUtils.saveChartAsPNG(new File("GraficaRecompensa.png"), recompensaChart, 1200, 800);

		} catch (IOException e) {
			e.printStackTrace();
			System.err.println("Error al guardar las graficas");
		}
	}
}
