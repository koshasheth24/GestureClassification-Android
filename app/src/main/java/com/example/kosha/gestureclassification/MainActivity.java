package com.example.kosha.gestureclassification;

import android.app.Activity;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.os.Bundle;
import android.os.Environment;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.Toast;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

public class MainActivity extends Activity implements SensorEventListener {

    private static final int SAMPLE_INTERVAL = 25;
    private final static float SENSOR_NOISE_LEVEL = 0.32f;
    private SensorManager sensorManager=null;
    private Sensor accelerometer=null;
    boolean start=false;

    private float x_previous=0.0f, y_previous=0.0f, z_previous=0.0f,
            delta_x, delta_y, delta_z;
    private long lastUpdate = System.currentTimeMillis();
    private float[] x_history = new float[]{0.0f,0.0f,0.0f,0.0f},
            y_history = new float[]{0.0f,0.0f,0.0f,0.0f},
            z_history = new float[]{0.0f,0.0f,0.0f,0.0f};
    private final int NUM_DESCRIPTORS = 8;

    private List<Coord3D> data3D= new ArrayList<Coord3D>();
    private List<Coord2D> data2D= new ArrayList<Coord2D>();
    private boolean toggler = false;
    private Button shapeAnalyzerBtn = null,startButton=null,stopButton=null,toggleButton=null;
    private boolean firstTime = true;
    int count =0;
    private String outputFileName = "unselectedShape.txt";
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        shapeAnalyzerBtn = (Button)findViewById(R.id.btnShapeEllipticFourier);
        startButton = (Button)findViewById(R.id.startDataColl);
        stopButton = (Button)findViewById(R.id.stopDataColl);
        toggleButton=(Button) findViewById(R.id.btnToggle);

        sensorManager = (SensorManager)getSystemService(SENSOR_SERVICE);
        accelerometer = this.sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER);


    }



    @Override
    protected void onStart() {
        super.onStart();
    }

    protected void onResume() {
        super.onResume();
        this.sensorManager.registerListener(this, this.accelerometer, SensorManager.SENSOR_DELAY_FASTEST);
    }

    protected void onPause() {
        sensorManager.unregisterListener(this);
        super.onPause();
    }

    public void onAccuracyChanged(Sensor sensor, int accuracy) {
    }

    public void onSensorChanged(SensorEvent event)
    {
        if (event.sensor.getType() != Sensor.TYPE_ACCELEROMETER)
            return;
        long curTime = System.currentTimeMillis();

        /*
         * quit if sampling period has not elapsed
         */
        if ((curTime - lastUpdate) > SAMPLE_INTERVAL) {
            lastUpdate = curTime;
        }
        else
            return;

        /*
         * process sensor data
         */

        if(start==true) {
            final float x = event.values[0];
            final float y = event.values[1];
            final float z = event.values[2];

            x_history[3] = x_history[2];
            x_history[2] = x_history[1];
            x_history[1] = x_history[0];
            x_history[0] = x - x_previous;

            y_history[3] = y_history[2];
            y_history[2] = y_history[1];
            y_history[1] = y_history[0];
            y_history[0] = y - y_previous;

            z_history[3] = z_history[2];
            z_history[2] = z_history[1];
            z_history[1] = z_history[0];
            z_history[0] = z - z_previous;

           /* if (Math.abs(x_history[0] - x_history[1]) > SENSOR_NOISE_LEVEL)
                delta_x = x_history[0];
            else    // compute weighted mean
                delta_x = 0.5f * x_history[0] + 0.3f * x_history[1] + 0.1f * x_history[2] + 0.1f * x_history[3];
*/
            delta_x = computeDirectionalAcceleration(x_history);
            delta_y = computeDirectionalAcceleration(y_history);
            delta_z = computeDirectionalAcceleration(z_history);
            x_previous = x;
            y_previous = y;
            z_previous = z;


            data3D.add(new Coord3D(x_previous,    // X acceleration vector
                    y_previous,    // Y acceleration vector
                    z_previous));  // Z acceleration vector
        }

    }

    private float computeDirectionalAcceleration(float[] dataWindow)
    {
        if(Math.abs(dataWindow[0]-dataWindow[1]) > SENSOR_NOISE_LEVEL)
            return(dataWindow[0]);
        else    // compute weighted mean
            return(0.5f*dataWindow[0] + 0.3f*dataWindow[1] + 0.1f*dataWindow[2] + 0.1f*dataWindow[3]);
    }


    public void doEllipticFourierShapeAnalysis(View v)
    {
        List<List<Double>> harmonics = displayEllipticFourierDescriptor(this.data2D);
        classifyShape(harmonics);
    }


    private List<List<Double>> displayEllipticFourierDescriptor(List<Coord2D> points)
    {
        List<List<Double>> harmonics = new ArrayList<List<Double>>();
        List<Double> harmonic = null;

        int extent = points.size();
        double[] x = new double[extent];
        double[] y = new double[extent];

        for(int idx = 0; idx < extent; idx++){
            x[idx] = points.get(idx).getA();
            y[idx] = points.get(idx).getB();
        }

        EllipticFourierDesc efd = new EllipticFourierDesc(x,y,NUM_DESCRIPTORS);
        StringBuilder buff = new StringBuilder();
        for(int idx = 0; idx < NUM_DESCRIPTORS; idx++)
        {
            buff.append(efd.efd_ax[idx] + ",");
            buff.append(efd.efd_ay[idx] + ",");
            buff.append(efd.efd_bx[idx] + ",");
            buff.append(efd.efd_by[idx] + "\n");

            // cache this harmonic
            harmonic = new ArrayList<Double>(Arrays.asList(efd.efd_ax[idx],
                    efd.efd_ay[idx],
                    efd.efd_bx[idx],
                    efd.efd_by[idx]));
            harmonics.add(harmonic);
        }
        buff.toString();
        //Toast.makeText(this,buff.toString(),Toast.LENGTH_SHORT).show();
        //writeToFile("\nData:\n"+buff.toString(), outputFileName);
        return(harmonics);

    }

    private void classifyShape(List<List<Double>> descriptors)
    {
        final CopyOnWriteArrayList<EllipticFourierDistanceMeasure.Distance> combinedMetricsConcurrent = new CopyOnWriteArrayList<EllipticFourierDistanceMeasure.Distance>();

        try {

            final List<List<Double>> descriptorsForThread = descriptors;
            int MAX_CONCURRENT = 5;
            ExecutorService executor = Executors.newFixedThreadPool(MAX_CONCURRENT);
            Callable<Void> callableCircle = new Callable<Void>() {
                @Override
                public Void call()
                {
                    try {
                        EllipticFourierDistanceMeasure comparator_circle = new EllipticFourierDistanceMeasure(descriptorsForThread, Constants.SHAPE_TYPE.CIRCLE);
                        final List<EllipticFourierDistanceMeasure.Distance> distanceToCircle = comparator_circle.computeDistanceMetric();
                        combinedMetricsConcurrent.addAll(distanceToCircle);

                    }catch(Exception ex)
                    {
                        // @ToDo --
                    }

                    return(null);
                }
            };
            Callable<Void> callableVee = new Callable<Void>() {
                @Override
                public Void call()
                {
                    try {
                        EllipticFourierDistanceMeasure comparator_vee = new EllipticFourierDistanceMeasure(descriptorsForThread, Constants.SHAPE_TYPE.VEE);
                        final List<EllipticFourierDistanceMeasure.Distance> distanceToVee = comparator_vee.computeDistanceMetric();
                        combinedMetricsConcurrent.addAll(distanceToVee);

                    }catch(Exception ex)
                    {
                        // @ToDo --
                    }

                    return(null);
                }
            };
            Callable<Void> callableLine = new Callable<Void>() {
                @Override
                public Void call()
                {
                    try {
                        EllipticFourierDistanceMeasure comparator_line = new EllipticFourierDistanceMeasure(descriptorsForThread, Constants.SHAPE_TYPE.LINE);
                        final List<EllipticFourierDistanceMeasure.Distance> distanceToLine = comparator_line.computeDistanceMetric();
                        combinedMetricsConcurrent.addAll(distanceToLine);

                    }catch(Exception ex)
                    {
                        // @ToDo --
                    }

                    return(null);
                }
            };

            Future<Void> future1 = executor.submit(callableCircle);
            Future<Void> future4 = executor.submit(callableVee);
            Future<Void> future5 = executor.submit(callableLine);

            future1.get();
            future4.get();
            future5.get();
            Constants.SHAPE_TYPE classification = EllipticFourierDistanceMeasure.findBestMatch(combinedMetricsConcurrent);
            Toast.makeText(this,"CLASSIFICATION : "+classification,Toast.LENGTH_LONG).show();

        }catch(Exception ex){
            String mesg = ex.getMessage();
            System.out.println(mesg);
        }
    }

    public void toggleViews(View v)
    {
        if(data3D.isEmpty()) {
            Toast.makeText(MainActivity.this, "You must first generate motion data",Toast.LENGTH_SHORT).show();
            return;
        }

        if(firstTime) {
            firstTime = false;
        }


        shapeAnalyzerBtn.setVisibility(View.VISIBLE);
        PCA pca = new PCA();
        pca.setup(this.data3D.size(), Coord3D.DIMENSIONALITY);

        for(Coord3D point : this.data3D)
        {
            double[] vector = new double[]{point.getX(), point.getY(), point.getZ()};
            pca.addSample(vector);
        }

        pca.computeBasis(2);
        StringBuilder str=new StringBuilder();
        this.data2D.clear();
        for(Coord3D point : this.data3D)
        {
            double[] vector = pca.sampleToEigenSpace(new double[]{point.getX(), point.getY(), point.getZ()});
            Coord2D p = new Coord2D(vector[0], vector[1]);
            str.append(p.getA()+" "+p.getB()+"\n");
            this.data2D.add(p);
        }
        str.length();

    }


    @Override
    public void onStop() {

        super.onStop();
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
         return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        return super.onOptionsItemSelected(item);
    }

    private void writeToFile(String data, String filename) {
        try {
            String root= Environment.getExternalStorageDirectory().toString();
            File myDir = new File(root+"/GESTURES");
            if(!myDir.exists())
            {
                myDir.mkdir();
            }

            FileOutputStream  outputStream = new FileOutputStream (new File(myDir.getAbsolutePath().toString()+"/"+filename), true);
            outputStream.write(data.getBytes());
            outputStream.close();

        }
        catch (IOException e) {
            android.util.Log.e("Exception", "******* File write failed: "+e.toString());
        }
    }

    public void startDataColl(View V){
        data3D.clear();
        this.start=true;
    }
    public void stopDataColl(View V){
        this.start=false;
    }

}
