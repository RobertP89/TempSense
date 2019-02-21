
import java.awt.*;
import java.io.PrintWriter;
import java.util.*;
import java.util.List;

import javax.swing.*;

import com.twilio.Twilio;
import com.twilio.rest.api.v2010.account.Message;
import com.twilio.type.PhoneNumber;
import org.knowm.xchart.*;
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
    private static char fctemp='c';
    static SerialPort chosenPort;

    //LinkedList<Double> fifo = new LinkedList<Double>(Arrays.asList(0.0,1.0));
    MySwingWorker mySwingWorker;
    XChartPanel<XYChart> tChart;
    XYChart chart;

    public static void main(String[] args) throws Exception {

        TemperaturePanel swingWorkerRealTime = new TemperaturePanel();
        swingWorkerRealTime.go();
    }

    private void go() {

        // Create Chart
//        chart = QuickChart.getChart("SwingWorker XChart Real-time Demo", "Time", "Value", "randomWalk", new double[] { 0 }, new double[] { 0 });
//        chart.getStyler().setLegendVisible(false);
//        chart.getStyler().setXAxisTicksVisible(false);

        //JFrame tFrame = new JFrame("Test");
        // create a drop-down box and connect button, then place them at the top of the window
        JComboBox<String> portList = new JComboBox<>();
        JButton connectButton = new JButton("Connect");
        JButton FButton = new JButton("F");
        JButton CButton = new JButton("C");
        JButton LEDButton = new JButton("LED");

        JPanel topPanel = new JPanel();

        topPanel.add(portList);
        topPanel.add(connectButton);
        topPanel.add(FButton);
        topPanel.add(CButton);
        topPanel.add(LEDButton);

        // populate the drop-down box
        SerialPort[] portNames = SerialPort.getCommPorts();
        for(int i = 0; i < portNames.length; i++)
            portList.addItem(portNames[i].getSystemPortName());

        JTextField lowTemp = new JTextField();


        JTextArea textLowTemp = new JTextArea("Lowest Temperature");
        textLowTemp.setEditable(false);
        JTextField readLow = new JTextField(5);

        topPanel.add(textLowTemp);
        topPanel.add(readLow);

        readLow.addActionListener(e -> {
            inputLow = Integer.parseInt(readLow.getText());
            lowTemp.setText(Integer.toString(inputLow));
        });

        JTextField highTemp = new JTextField();


        JTextArea textHighTemp = new JTextArea("Highest Temperature");
        textHighTemp.setEditable(false);
        JTextField readHigh = new JTextField(5);

        topPanel.add(textHighTemp);
        topPanel.add(readHigh);

        readHigh.addActionListener(e -> {
            inputHigh = Integer.parseInt(readHigh.getText());
            highTemp.setText(Integer.toString(inputHigh));
        });

        //User input cell phone number
        //JTextField phoneNumber = new JTextField();
        JTextArea textPhoneNumber = new JTextArea("Cell Number");
        textPhoneNumber.setEditable(false);
        JTextField readCellNumber = new JTextField(10);

        topPanel.add(textPhoneNumber);
        topPanel.add(readCellNumber);

        // unnecessary lines?
        /*readCellNumber.addActionListener(e -> {
            int inputCellPhone = Integer.parseInt(readCellNumber.getText());
            phoneNumber.setText(Integer.toString(inputCellPhone));
        });*/

        //START A WHILE LOOP FOR THE USER INPUT INTO TEMPERATURE RANGES AND CELL NUMBER



        // configure the connect button and use another thread to listen for data
        connectButton.addActionListener(arg0 -> {
            if(connectButton.getText().equals("Connect")) {
                // attempt to connect to the serial port
                chosenPort = SerialPort.getCommPort(portList.getSelectedItem().toString());
                chosenPort.setComPortTimeouts(SerialPort.TIMEOUT_SCANNER, 0, 0);
                if(chosenPort.openPort()) {
                    connectButton.setText("Disconnect");
                    portList.setEnabled(false);
                }
                //////////////////////////////////////////////////////////
                Twilio.init(ACCOUNT_SID, AUTH_TOKEN);

                Message message = Message
                        .creator(new PhoneNumber(readCellNumber.getText()), new PhoneNumber("+16182668109"), // To: , From: respectively
                                "Sensor connected.").create();

                System.out.println(message.getSid());

                ////////////////////////////////////////////////////////////////

                // create a new thread that listens for incoming text and populates the graph
                Thread thread = new Thread(() -> {
                    LinkedList<Integer> sensorData= new LinkedList();
                    for(int i=0; i<300; i++){

                        sensorData.add(0);
                    }

                    Scanner scanner = new Scanner(chosenPort.getInputStream());
                    int oneText = 0;
                    int startConn=0;


                    while(scanner.hasNextLine()) {


                        try {
                            if(startConn==0){
                                PrintWriter output=new PrintWriter(chosenPort.getOutputStream());
                                output.print('s');
                                output.flush();
                                for(int i=0; i<300; i++){
                                    String line = scanner.nextLine();
                                    int number = Integer.parseInt(line);
                                    sensorData.add(number);
                                    sensorData.remove();
                                }
                                startConn=1;
                            }


                            String line = scanner.nextLine();
                            int number = Integer.parseInt(line);
                            inputHigh = Integer.parseInt(readHigh.getText());
                            inputLow = Integer.parseInt(readLow.getText());
                            /*
                            System.out.printf("high temp %d\n", h);
                            System.out.printf("low temp %d\n", l);
                            System.out.printf("readlow: %s\n", readLow.getText());
                            System.out.printf("readhigh: %s\n", readHigh.getText());
                            */
                            if (number >= inputHigh && oneText == 0 && number <= 65){
                                Message message1 = Message
                                        .creator(new PhoneNumber(readCellNumber.getText()), new PhoneNumber("+16182668109"), // To: , From: respectively
                                                "WARNING: TEMPERATURE RANGE HAS EXCEEDED REQUIRED TEMPERATURE").create();
                                oneText++;
                            }
                            if (number <= inputLow && oneText == 0 && number >= -20){
                                Message message1 = Message
                                        .creator(new PhoneNumber(readCellNumber.getText()), new PhoneNumber("+16182668109"), // To: , From: respectively
                                                "WARNING: TEMPERATURE RANGE HAS FALLEN BELOW REQUIRED TEMPERATURE").create();
                                oneText++;
                            }

                            sensorData.add(number);
                            sensorData.remove();
                            //series.clear();

                            if(FButton.getModel().isPressed()){
                                fctemp='f';
                            }
                            if(CButton.getModel().isPressed()){
                                fctemp='c';
                            }
                            if(fctemp=='c' && number >= -125){

                                chart.setTitle(Integer.toString(number) + " Degrees");

                            }
                            else if (fctemp == 'f' && number >= -125){
                                chart.setTitle(Double.toString(number*1.8+32) + " Degrees");
                            }
                            else {
                                chart.setTitle("Unplugged Sensor");
                            }


                            if(LEDButton.getModel().isPressed()){
                                PrintWriter output=new PrintWriter(chosenPort.getOutputStream());
                                output.print('o');
                                output.flush();
                            }

                            for(int i=-0;i<300; i++){
                                int y=sensorData.get(i);


                                int x=i;
                                if(y>-50&&y<120){
                                    //series.add(300-x,y);
                                }
                                else{
                                    //series.add(300-x, null);
                                }
                            }


                            //window.repaint();
                        } catch(Exception e) {
                        }

                    }
                    scanner.close();
                });
                thread.start();
            } else {
                // disconnect from the serial port & check for user inputs
                System.out.println("PLEASE ENTER VALID INPUTS BEFORE CONNECTING.");
                /*if(cellNumber.length() != 10) {
                    //MAKE THE USER ENTER A VALID PHONE NUMBER
                    JFrame cellInvalidAlert = new JFrame();
                    cellInvalidAlert.setTitle("INVALID CELL NUMBER");
                    cellInvalidAlert.setSize(500, 300);
                    cellInvalidAlert.setLayout(new BorderLayout());
                    cellInvalidAlert.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
                }*/
                /*if (inputLow < -10 || inputHigh > 63){
                    //MAKE THE USER ENTER VALID LOW RANGE TEMPERATURE
                }
                else
                {
                    //ONE OR MORE OF YOUR VALUES WERE ENTERED WRONG, MESSAGE
                }*/
                chosenPort.closePort();
                portList.setEnabled(true);
                connectButton.setText("Connect");
                //clear the data structure(s) holding data received from the ESP
                //series.clear();
            }
        });
        /************************
        This begins the chart creation process.
         ***********************/
        tChart = buildPanel();


        javax.swing.SwingUtilities.invokeLater(new Runnable() {

            @Override
            public void run() {

                // Create and set up the window.
                JFrame frame = new JFrame("XChart");
                frame.setDefaultCloseOperation(WindowConstants.EXIT_ON_CLOSE);
                frame.add(topPanel, BorderLayout.NORTH);
                frame.add(tChart);

                // Display the window.
                frame.pack();
                frame.setVisible(true);
            }
        });


        // Show it
//        sw = new SwingWrapper<XYChart>(chart);
//        sw.displayChart();
//
        mySwingWorker = new MySwingWorker();
        mySwingWorker.execute();

    }

    private class MySwingWorker extends SwingWorker<Boolean, double[]> {

        LinkedList<Double> fifo = new LinkedList<Double>();
        LinkedList<Double> fifoX = new LinkedList<Double>();


        public MySwingWorker() {

            fifo.add(0.0);
            fifoX.add(0.0);

        }

        @Override
        protected Boolean doInBackground() throws Exception {

            while (!isCancelled()) {

                fifo.add(fifo.get(fifo.size() - 1) + Math.abs(Math.random() - .5));
                fifoX.add(fifoX.getLast()+1);
                if (fifo.size() > 300) {
                    fifo.removeFirst();
                    fifoX.removeFirst();
                }

                double[] array = new double[fifo.size()];
                double[] arrayX = new double[fifoX.size()];

                for (int i = 0; i < fifo.size(); i++) {
                    array[i] = fifo.get(i);
                    arrayX[i] = fifoX.get(i);
                }
                publish(array, arrayX);

                try {
                    Thread.sleep(750);
                } catch (InterruptedException e) {
                    // eat it. caught when interrupt is called
                    System.out.println("MySwingWorker shut down.");
                }

            }

            return true;
        }

        @Override
        protected void process(List<double[]> chunks) {

            System.out.println("number of chunks: " + chunks.size());

            double[] mostRecentDataSet = chunks.get(chunks.size() - 2);
            double[] xData = chunks.get(chunks.size() - 1);




            chart.updateXYSeries("randomWalk", xData , mostRecentDataSet , null).setMarker(SeriesMarkers.CIRCLE);
            tChart.revalidate();
            tChart.repaint();

            long start = System.currentTimeMillis();
            long duration = System.currentTimeMillis() - start;
            try {
                Thread.sleep(1200 - duration); // 40 ms ==> 25fps
                // Thread.sleep(400 - duration); // 40 ms ==> 2.5fps
            } catch (InterruptedException e) {
            }

        }
    }


    public XYChart getChart() {
        chart = QuickChart.getChart("SwingWorker XChart Real-time Demo", "Time (s)", "Temperature", "randomWalk", new double[] { 0 }, new double[] { 0 });
        Map<Double, Object> xMarkMap = new TreeMap<Double, Object>();
        Map<Double, Object> yMarkMap = new TreeMap<Double, Object>();

        for(double i = 0; i <=300; i+=100){
            xMarkMap.put(i, Double.toString(i));
        }
//        for(double i = 0; i <= 60; i+=10){
//            yMarkMap.put(i, Double.toString(i));
//        }
        //chart.setXAxisLabelOverrideMap(xMarkMap);
//        chart.setYAxisLabelOverrideMap(yMarkMap);
        chart.getStyler().setPlotContentSize(1);
        chart.getStyler().setDecimalPattern("#0.0");
        chart.getStyler().setAxisTickMarkLength(3);
        chart.getStyler().setXAxisMax(0.0);
        chart.getStyler().setXAxisMin(0.0);
        chart.getStyler().setXAxisTickMarkSpacingHint(10);
        chart.getStyler().setYAxisMin(0.0);
        chart.getStyler().setLegendVisible(true);
        chart.getStyler().setPlotTicksMarksVisible(true);

        //chart.getStyler().setXAxisTicksVisible(true);

        return chart;
    }

    public XChartPanel<XYChart> buildPanel(){
        return new XChartPanel<XYChart>(getChart());
    }
}
