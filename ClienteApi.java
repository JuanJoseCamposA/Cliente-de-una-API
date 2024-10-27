package topicos;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import org.json.JSONArray;
import org.json.JSONObject;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;

/**
 * Clase ClienteApi que crea una interfaz gráfica para consultar datos de terremotos 
 * desde el servicio web de USGS. Permite a los usuarios especificar un rango de fechas 
 * y muestra la lista de terremotos ocurridos en ese rango.
 */
public class ClienteApi extends JFrame {
    private JTextArea textArea;
    private JButton fetchButton;
    private JTextField startDateField;
    private JTextField endDateField;

    /**
     * Constructor de la clase ClienteApi.
     * Configura la ventana, los paneles y los componentes de la interfaz gráfica.
     */
    public ClienteApi() {
        // Configuración de la ventana
        setTitle("EarthquakeApp");
        setSize(600, 400);
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setLayout(new BorderLayout());

        // Panel para las entradas de fecha
        JPanel datePanel = new JPanel();
        datePanel.setLayout(new FlowLayout());
        datePanel.add(new JLabel("Fecha Inicio (YYYY-MM-DD):"));
        startDateField = new JTextField(10);
        datePanel.add(startDateField);
        datePanel.add(new JLabel("Fecha Fin (YYYY-MM-DD):"));
        endDateField = new JTextField(10);
        datePanel.add(endDateField);
        add(datePanel, BorderLayout.NORTH);

        // Área de texto para mostrar resultados
        textArea = new JTextArea();
        textArea.setEditable(false);
        JScrollPane scrollPane = new JScrollPane(textArea);
        add(scrollPane, BorderLayout.CENTER);

        // Botón para consultar la API
        fetchButton = new JButton("Buscar terremotos");
        fetchButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                fetchEarthquakeData();
            }
        });
        add(fetchButton, BorderLayout.SOUTH);
    }

    /**
     * Método que realiza la consulta a la API de USGS para obtener los datos de terremotos 
     * en el rango de fechas especificado por el usuario.
     */
    private void fetchEarthquakeData() {
        String startTime = startDateField.getText();
        String endTime = endDateField.getText();

        // Validar formato de fecha
        if (!isValidDate(startTime) || !isValidDate(endTime)) {
            textArea.setText("Formato de fecha inválido. Usa el formato YYYY-MM-DD.");
            return;
        }

        // Comparar fechas
        try {
            SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd");
            Date startDate = sdf.parse(startTime);
            Date endDate = sdf.parse(endTime);

            if (startDate.after(endDate)) {
                textArea.setText("La fecha de inicio no puede ser posterior a la fecha de finalización.");
                return;
            }
        } catch (ParseException e) {
            textArea.setText("Error al procesar las fechas: " + e.getMessage());
            return;
        }

        String urlString = "https://earthquake.usgs.gov/fdsnws/event/1/query?format=geojson&starttime=" + startTime + "&endtime=" + endTime;

        StringBuilder response = new StringBuilder();

        try {
            // Crear la conexión
            URL url = new URL(urlString);
            HttpURLConnection conn = (HttpURLConnection) url.openConnection();
            conn.setRequestMethod("GET");

            // Verificar el código de respuesta
            int responseCode = conn.getResponseCode();
            if (responseCode != HttpURLConnection.HTTP_OK) {
                textArea.setText("Error en la consulta a la API: " + responseCode);
                return;
            }

            // Leer la respuesta
            BufferedReader in = new BufferedReader(new InputStreamReader(conn.getInputStream()));
            String inputLine;
            while ((inputLine = in.readLine()) != null) {
                response.append(inputLine);
            }
            in.close();

            // Procesar la respuesta JSON
            JSONObject jsonResponse = new JSONObject(response.toString());
            JSONArray features = jsonResponse.getJSONArray("features");

            // Crear una lista de terremotos
            ArrayList<Earthquake> earthquakes = new ArrayList<>();
            for (int i = 0; i < features.length(); i++) {
                JSONObject earthquake = features.getJSONObject(i);
                JSONObject properties = earthquake.getJSONObject("properties");

                // Verificar si la magnitud es null
                if (!properties.isNull("mag")) {
                    double magnitude = properties.getDouble("mag"); // Magnitud del terremoto
                    String place = properties.getString("place"); // Lugar del terremoto
                    long time = properties.getLong("time"); // Extraer tiempo en milisegundos
                    earthquakes.add(new Earthquake(magnitude, place, time)); // Añadir a la lista
                }
            }

            // Ordenar los terremotos por fecha (tiempo) en orden descendente
            Collections.sort(earthquakes, Comparator.comparingLong(Earthquake::getTime).reversed());

            // Mostrar los resultados en formato de lista
            StringBuilder output = new StringBuilder("Terremotos:\n\n");
            SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
            dateFormat.setTimeZone(java.util.TimeZone.getTimeZone("UTC")); // Configurar la zona horaria a UTC

            for (Earthquake eq : earthquakes) {
                String date = dateFormat.format(new java.util.Date(eq.getTime())); // Formatear la fecha a UTC
                output.append(String.format("Fecha: %s, Magnitud: %.1f, Ubicación: %s%n", date, eq.getMagnitude(), eq.getPlace()));
            }
            textArea.setText(output.toString()); // Mostrar resultados en el área de texto

        } catch (IOException e) {
            textArea.setText("Error al obtener datos: " + e.getMessage()); // Mostrar mensaje de error
        }
    }

    /**
     * Método que valida el formato de la fecha.
     * 
     * @param date La cadena de fecha a validar.
     * @return true si el formato es válido; false en caso contrario.
     */
    private boolean isValidDate(String date) {
        String regex = "^\\d{4}-\\d{2}-\\d{2}$"; // Regex para validar el formato YYYY-MM-DD
        return date.matches(regex);
    }

    /**
     * Método principal para iniciar la aplicación.
     * 
     * @param args Argumentos de la línea de comandos (no utilizados).
     */
    public static void main(String[] args) {
        // Crear y mostrar la ventana
        SwingUtilities.invokeLater(() -> {
            ClienteApi client = new ClienteApi();
            client.setVisible(true);
        });
    }

    /**
     * Clase interna para representar un terremoto.
     */
    private static class Earthquake {
        private final double magnitude;
        private final String place;
        private final long time;

        /**
         * Constructor de la clase Earthquake.
         * 
         * @param magnitude La magnitud del terremoto.
         * @param place La ubicación del terremoto.
         * @param time La hora del terremoto en milisegundos.
         */
        public Earthquake(double magnitude, String place, long time) {
            this.magnitude = magnitude;
            this.place = place;
            this.time = time;
        }

        public double getMagnitude() {
            return magnitude;
        }

        public String getPlace() {
            return place;
        }

        public long getTime() {
            return time;
        }
    }
}
