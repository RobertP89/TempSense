
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.OutputStream;
import java.io.PrintStream;
import java.io.PrintWriter;
import java.util.*;
import java.util.List;
import javax.swing.*;

import com.twilio.Twilio;
import com.twilio.exception.ApiException;
import com.twilio.exception.TwilioException;
import com.twilio.rest.api.v2010.account.Message;
import com.twilio.type.PhoneNumber;
import org.knowm.xchart.*;
import org.knowm.xchart.style.Styler;
import org.knowm.xchart.style.markers.SeriesMarkers;
import com.fazecast.jSerialComm.SerialPort;

/**
 * Creates a real-time chart using SwingWorker
 */
public class TemperaturePanel {
    public static final String ACCOUNT_SID = "AC1615f0e31f18e56aefc2ed052f743989";
    public static final String AUTH_TOKEN = "652bcbde901ed504d08f0713ac765086";
    public static final int LOWEST_TEMPERATURE = 10;
    public static final int HIGHEST_TEMPERATURE = 50;
    public static int inputLow;
    public static int inputHigh;
    private static char fctemp = 'c';
    static SerialPort chosenPort;
    private JTextField readLow, readHigh, readCellNumber, highTemp, lowTemp;
    private JButton connectButton, FButton, CButton, LEDButton;
    private JComboBox<String> portList;
    private PrintWriter output;
    private boolean changedToF = false;


    //LinkedList<Double> fifo = new LinkedList<Double>(Arrays.asList(0.0,1.0));
    MySwingWorker mySwingWorker;
    XChartPanel<XYChart> tChart;
    XYChart chart;

    public static void main(String[] args) throws Exception {

        TemperaturePanel swingWorkerRealTime = new TemperaturePanel();
        swingWorkerRealTime.go();
    }

    private void go() {

        JPanel topPanel = new JPanel();
        // create a drop-down box and connect button, then place them at the top of the window
        portList = new JComboBox<>();

        TmpButtonHandler btnHandler = new TmpButtonHandler();
        TextHandler txtFieldHandler = new TextHandler();

        connectButton = new JButton("Connect");
        connectButton.addActionListener(btnHandler);
        FButton = new JButton("F");
        CButton = new JButton("C");
        LEDButton = new JButton("LED OFF");
        LEDButton.addActionListener(btnHandler);

        topPanel.add(portList);
        topPanel.add(connectButton);
        topPanel.add(FButton);
        topPanel.add(CButton);
        topPanel.add(LEDButton);

        // populate the drop-down box
        SerialPort[] portNames = SerialPort.getCommPorts();

        for (int i = 0; i < portNames.length; i++)
            portList.addItem(portNames[i].getSystemPortName());

        lowTemp = new JTextField();
        JLabel textLowTemp = new JLabel("Lowest Temperature");
        readLow = new JTextField(5);

        topPanel.add(textLowTemp);
        topPanel.add(readLow);

        highTemp = new JTextField();
        JLabel textHighTemp = new JLabel("Highest Temperature");
        readHigh = new JTextField(5);

        topPanel.add(textHighTemp);
        topPanel.add(readHigh);
        readHigh.addActionListener(txtFieldHandler);

        //User input cell phone number
        JLabel textPhoneNumber = new JLabel("Cell Number");
        readCellNumber = new JTextField(10);

        topPanel.add(textPhoneNumber);
        topPanel.add(readCellNumber);

        // unnecessary lines?
        /*readCellNumber.addActionListener(e -> {
            int inputCellPhone = Integer.parseInt(readCellNumber.getText());
            phoneNumber.setText(Integer.toString(inputCellPhone));
        });*/

        /* Old Outdated code snippet for reference*/

        /************************
         This begins the chart creation process.
         ***********************/
        tChart = buildPanel();

        javax.swing.SwingUtilities.invokeLater(new Runnable() {

            @Override
            public void run() {

                // Create and set up the window.
                JFrame frame = new JFrame("Temperature Sensor Data");
                frame.setDefaultCloseOperation(WindowConstants.EXIT_ON_CLOSE);
                frame.add(topPanel, BorderLayout.NORTH);
                frame.add(tChart);

                // Display the window.
                frame.pack();
                frame.setVisible(true);

            }
        });
        mySwingWorker = new MySwingWorker();
        mySwingWorker.execute();
    }

    /*
    This class handles what happens when the connect button is pressed.
     */
    private class TextHandler implements ActionListener {
        public void actionPerformed(ActionEvent e) {
            if (e.getSource() == readHigh) {
                inputHigh = Integer.parseInt(readHigh.getText());
                highTemp.setText(Integer.toString(inputHigh));
            } else if (e.getSource() == readLow) {
                inputLow = Integer.parseInt(readLow.getText());
                lowTemp.setText(Integer.toString(inputLow));
            }
        }
    }

    private class TmpButtonHandler implements ActionListener {
        public void actionPerformed(ActionEvent e) {
            if (e.getSource() == connectButton) {
                if (connectButton.getText().equals("Connect")) {
                    // attempt to connect to the serial port
                    chosenPort = SerialPort.getCommPort(portList.getSelectedItem().toString());
                    chosenPort.setComPortTimeouts(SerialPort.TIMEOUT_SCANNER, 0, 0);
                    if (chosenPort.openPort()) {
                        connectButton.setText("Disconnect");
                        portList.setEnabled(false);
                    }

                    //////////////////////////////////////////////////////////
                    try {
                        Twilio.init(ACCOUNT_SID, AUTH_TOKEN);

                        Message message = Message
                                .creator(new PhoneNumber(readCellNumber.getText()), new PhoneNumber("+16182668109"), // To: , From: respectively
                                        "Sensor connected.").create();

                        System.out.println(message.getSid());
                        //These are two very important lines of code, this is what basically allows for data acquisition
                        //in the background.
//                        mySwingWorker = new MySwingWorker();
//                        mySwingWorker.execute();
                        output = new PrintWriter(chosenPort.getOutputStream());

                    }catch(TwilioException T){
                        JOptionPane.showMessageDialog(null, T.getMessage(), "Error Sending Text Message.", JOptionPane.ERROR_MESSAGE);
                    }
                    catch(NullPointerException N){
                        JOptionPane.showMessageDialog(null, N.getCause(), "The device on this port is rejecting data acquisition.", JOptionPane.ERROR_MESSAGE);
                    }
                } else {
                    //Check if the port is still open, if it is proceed to close it gracefully
                    if (chosenPort.isOpen()) {
                        //flush any data still in the output buffer
                        output.flush();
                        //close the output buffer
                        output.close();
                        //close/disconnected from the COM port
                        chosenPort.closePort();
                        connectButton.setText("Connect");
                        //stop graphing and trying to acquire data
                        mySwingWorker.cancel(true);
                        portList.setEnabled(true);
                    }
                }
            }
            else if(e.getSource() == LEDButton){
                if(LEDButton.getText().equals("LED ON")) {
                    //turn the led off
                    LEDButton.setText("LED OFF");
                    output.print("t\n");
                }
                else {
                    //turn the led on
                    LEDButton.setText("LED ON");
                    output.print("T\n");
                }
                output.flush();

            }
            else if(e.getSource() == FButton){
                if(fctemp == 'c'){
                    fctemp = 'f';
                    chart.setYAxisTitle("Temperature (" + fctemp + ")");
                }
            }
        }
    }

    /*
    This class handles all data acquisition and creation of the actual graph. This class runs in a separate
    thread independent of the GUI so that the GUI can still respond.
     */
    private class MySwingWorker extends SwingWorker<Boolean, double[]> {

        //holds all received data that is to be plotted in the graph
        LinkedList<Double> fifo = new LinkedList<Double>();

        public MySwingWorker() {
        }

        /*
        This function is responsible for the acquisition and processing of incoming data from
        a connected temperature probe either via Bluetooth, or a connected COM port of some design.
        A scanner is created for the purpose of processing incoming data from the COM port selected
        by the user.

        Upon receiving data that either exceeds the maximum safe temperature, or falls below the minimum
        safety temperature; a text message will be sent to a specified phone number. This message is sent
        sent through a service named Twilio.
         */
        @Override
        protected Boolean doInBackground() throws Exception {

            int oneText = 0;
            Scanner scanner = new Scanner(chosenPort.getInputStream());
            while (!isCancelled()) {



                if (scanner.hasNextLine()) {
                        try {

                        String line = scanner.nextLine();
                        double number = Double.parseDouble(line);
                        inputHigh = Integer.parseInt(readHigh.getText());
                        inputLow = Integer.parseInt(readLow.getText());
                            /*
                            System.out.printf("high temp %d\n", h);
                            System.out.printf("low temp %d\n", l);
                            System.out.printf("readlow: %s\n", readLow.getText());
                            System.out.printf("readhigh: %s\n", readHigh.getText());
                            */
                        if (number >= inputHigh && oneText == 0 && number <= 65) {
                            Message message1 = Message
                                    .creator(new PhoneNumber(readCellNumber.getText()), new PhoneNumber("+16182668109"), // To: , From: respectively
                                            "WARNING: TEMPERATURE RANGE HAS EXCEEDED REQUIRED TEMPERATURE").create();
                            oneText++;
                        }
                        if (number <= inputLow && oneText == 0 && number >= -20) {
                            Message message1 = Message
                                    .creator(new PhoneNumber(readCellNumber.getText()), new PhoneNumber("+16182668109"), // To: , From: respectively
                                            "WARNING: TEMPERATURE RANGE HAS FALLEN BELOW REQUIRED TEMPERATURE").create();
                            oneText++;
                        }

                        fifo.add(number);
                        //sensorData.remove();
                        //series.clear();

//                        if(FButton.getModel().isPressed()){
//                            fctemp='f';
//                        }
//                        if(CButton.getModel().isPressed()){
//                            fctemp='c';
//                        }
//                        if(fctemp=='c' && number >= -125){
//
//                            chart.setTitle(Double.toString(number) + " Degrees");
//
//                        }
//                        else if (fctemp == 'f' && number >= -125){
//                            chart.setTitle(Double.toString(number*1.8+32) + " Degrees");
//                        }
//                        else {
//                            chart.setTitle("Unplugged Sensor");
//                        }

//                    if (LEDButton.getModel().isPressed()) {
//                        PrintWriter output = new PrintWriter(chosenPort.getOutputStream());
//                        output.print('o');
//                        output.flush();
//                    }

//                        for(int i=-0;i<300; i++){
//                            double y=fifo.get(i);
//
//
//                            int x=i;
//                            if(y>-50&&y<120){
//                                //series.add(300-x,y);
//                            }
//                            else{
//                                //series.add(300-x, null);
//                            }
//                        }
                        //fifoX.add(fifoX.getLast()+1);
                        /*
                        If the data-set exceeds 300, this will remove the oldest element from the list.
                        This also serves to keep the maximum locked at 300 on the x-axis.
                         */
                        //fifo.add(Math.random()-.5);
                        if (fifo.size() > 300) {
                            fifo.removeFirst();
                            //fifoX.removeFirst();
                        }

                        double[] array = new double[fifo.size()];
                        //double[] arrayX = new double[fifoX.size()];

                        for (int i = 0; i < fifo.size(); i++) {
                            array[i] = fifo.get(i);
                            //arrayX[i] = fifoX.get(i);
                        }
                        publish(array);

                    }catch(Exception e){
                    }

                    }
                }

                try {
                    Thread.sleep(1000);
                } catch (InterruptedException e) {
                    // eat it. caught when interrupt is called
                    System.out.println("MySwingWorker shut down.");
                }
                scanner.close();
                return true;
            }
/*
    Use to reference random data input into graph for further debugging.
 */
//                //fifo.add(fifo.get(fifo.size() - 1) + Math.abs(Math.random() - .5));
//                fifoX.add(fifoX.getLast()+1);
//                if (fifo.size() > 300) {
//                    fifo.removeFirst();
//                    fifoX.removeFirst();
//                }
//
//                double[] array = new double[fifo.size()];
//                double[] arrayX = new double[fifoX.size()];
//
//                for (int i = 0; i < fifo.size(); i++) {
//                    array[i] = fifo.get(i);
//                    arrayX[i] = fifoX.get(i);
//                }
//                publish(array, arrayX);
//
//                try {
//                    Thread.sleep(750);
//                } catch (InterruptedException e) {
//                    // eat it. caught when interrupt is called
//                    System.out.println("MySwingWorker shut down.");
//                }


        /*
        This function is responsible for processing intermediate data that has been pushed for publishing on the
        thread. The data that has been passed over is used to update the existing series being plotted on the graph.
        This is accomplished through the updateXYSeries method that belong to the XYChart object chart.

        No x-data needs to be passed for the graph to be updated. This allows for the graph to resize up to a maximum
        value as specified in the function getChart(). The values on the x-axis could be dynamic and every changing but
        have been replaced with a custom mapping in the getChart() function.
         */
        @Override
        protected void process(List<double[]> chunks) {

            System.out.println("number of chunks: " + chunks.size());

            //get the data to be plotted, this is only the y-data
            double[] mostRecentDataSet = chunks.get(chunks.size() - 1);

            /*the x-data can be null due to XChart API functionality
            For each y data point, an accompanying x data point is plotted
            this x data point is calculated based on how long in-between y data points are plotted
            if it takes 200 ms to plot a y data point consistently, the x data points will show as
            0.2, 0.4,0.6, and so on as a result.*/
                chart.updateXYSeries("Sensor Data", null, mostRecentDataSet, null);
                chart.setYAxisTitle("Temperature (" + fctemp + ")");
                tChart.revalidate();
                tChart.repaint();

            long start = System.currentTimeMillis();
            long duration = System.currentTimeMillis() - start;
            try {
                Thread.sleep(400 - duration); // 40 ms ==> 25fps
                // Thread.sleep(400 - duration); // 40 ms ==> 2.5fps
            } catch (InterruptedException e) {
            }


        }
    }

    /*
    This function will build and create the XYChart object that will display plotted data that is acquired
    in the doInBackground function and passed to the process() function. This function merely serves to
    setup the chart with a custom design.
     */
        public XYChart getChart() {
            chart = QuickChart.getChart("Temperature Sensor Data", "Time (s)", "Temperature (" + fctemp + ")", "Sensor Data", new double[]{0}, new double[]{0});
            chart.getStyler().setMarkerSize(1);
            Map<Double, Object> xMarkMap = new TreeMap<Double, Object>();

            //create a custom map that will display the x-axis from left to right as follow
            // 300, 275, 250, 225, ..., 0 with 0 being the most recent data point
            for (double i = 0; i <= 300; i += 25) {
                    xMarkMap.put(i, Double.toString(300 - i));

            }

            chart.setXAxisLabelOverrideMap(xMarkMap);
            chart.getStyler().setPlotContentSize(1);
            chart.getStyler().setDecimalPattern("#0.0");
            chart.getStyler().setAxisTickMarkLength(3);
            chart.getStyler().setXAxisMax(300.0);
            //chart.getStyler().setXAxisMin(0.0);
            chart.getStyler().setXAxisTickMarkSpacingHint(80);
            chart.getStyler().setYAxisMin(10.0);
            chart.getStyler().setYAxisMax(50.0);
            chart.getStyler().setYAxisGroupPosition(0, Styler.YAxisPosition.Right);
            chart.getStyler().setLegendVisible(true);
            chart.getStyler().setPlotTicksMarksVisible(true);

            return chart;
        }

        public XChartPanel<XYChart> buildPanel() {
            return new XChartPanel<XYChart>(getChart());
        }
    }

