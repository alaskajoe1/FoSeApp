package jeenaa.fose;

import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.media.MediaPlayer;
import android.media.RingtoneManager;
import android.net.Uri;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.speech.tts.TextToSpeech;
import android.support.design.widget.NavigationView;
import android.support.v4.view.GravityCompat;
import android.support.v4.widget.DrawerLayout;
import android.support.v7.app.ActionBarDrawerToggle;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.*;
import com.androidplot.ui.AnchorPosition;
import com.androidplot.ui.XLayoutStyle;
import com.androidplot.ui.YLayoutStyle;
import com.androidplot.util.PixelUtils;
import com.androidplot.xy.*;

import java.util.Locale;


//main activity
//Bluno library is responsible for communication via Bluetooth
//Navigation view is need for the nav drawer
public class MainActivity extends BlunoLibrary
        implements NavigationView.OnNavigationItemSelectedListener
{
    //Global variable initializations
    public static final String MY_TAG = "outputLog";
    private double zeroValue = -1;                 //zero value for force sensor offset
    private double startTime = -1;              //start time of trial
    private double countDownTimer = -1;         //timer for the countdown
    private double maxForce;                    //keeps track of maximum force
    private int maxForceCounter = 1;
    private double elapsedTime;                 //time elapsed since start of trial
    private XYPlot forcePlot;                   //force plot on main screen
    private SimpleXYSeries forceHistory = null; //the force history graph
    private SimpleXYSeries FirstLevel = null;   //the upper green threshold line
    private SimpleXYSeries LowerLevel = null;   //the lower threshold for reps
    int HISTORY_SIZE = 1000;                    //number of data points kept (approximately 100s)
    boolean pause = true;                       //whether or not the animation is paused
    Menu controlMenu;                           //the top right menu (play/reset/zero)
    double rawValue;                               //raw value converted from force sensor
    boolean countdown = false;                  //controls the beginning of the countdown
    String exercise;                            //what exercise the user has selected
    double FirstLevel_value;                    //the value of the first level line
    double LowerLevel_value;                    //the value of the lower level line
    private NavigationView navigationView;      //the navigation view that makes up the nav drawer
    private Toolbar toolbar = null;             //reference to the nav bar at the top
    private String[] category = null;           //array containing the different exercises
    int repNumber = 0;                          //stores the number of reps
    boolean repReset = true;                    //false once in zone long enough, true once out of zone long enough
    double repTime;                             //how long they must stay in zone before the rep is counted
    double restTime;                            //how long they must rest before they can begin another rep
    double restTimer = 0;                       //keeps track of how long the user has been resting
    double repTimer = 0;                        //keeps track of how long the user has been in the rep zone
    MediaPlayer notificationSound;              //mediaPlayer for playing the default notification noise
    TextToSpeech Nathan;                        //text to speech object

    double[] graph1X;
    double[] graph2X;
    double[] graph3X;
    double[] graph1Y;
    double[] graph2Y;
    double[] graph3Y;

    SharedPreferences preferences;


    //method is called when the app is first started
    @Override
    protected void onCreate(Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);

        preferences = PreferenceManager.getDefaultSharedPreferences(this);

        FirstLevel_value = preferences.getInt("RepAmount", 0);
        LowerLevel_value = preferences.getInt("LowerThreshold", 0);
        repTime = preferences.getInt("RepTime", 0);
        restTime = preferences.getInt("RestTime", 0);

        //sets the content window to activity_main
        setContentView(R.layout.activity_main);

        //onCreate Process by BlunoLibrary
        onCreateProcess();

        //sets up the notification sound player
        Uri notification = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION);
        notificationSound = MediaPlayer.create(this, notification);


        //sets up the text to speech
        Nathan = new TextToSpeech(getApplicationContext(), new TextToSpeech.OnInitListener()
        {
            @Override
            public void onInit(int status)
            {
                if (status != TextToSpeech.ERROR)
                {
                    //where the language is selected
                    Nathan.setLanguage(Locale.UK);
                }
            }
        });


        //Creates the lines, adds them to the graph and formats everything
        formatGraph();


        //set the Baudrate on BLE chip to 115200 and initiates connection
        serialBegin(115200);

        //sets up toolbar at the top
        toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);


        //sets up nav drawer
        DrawerLayout drawer = (DrawerLayout) findViewById(R.id.drawer_layout);
        ActionBarDrawerToggle toggle = new ActionBarDrawerToggle(
                this, drawer, toolbar, R.string.navigation_drawer_open, R.string.navigation_drawer_close);
        drawer.setDrawerListener(toggle);
        toggle.syncState();

        navigationView = (NavigationView) findViewById(R.id.nav_view);
        navigationView.setNavigationItemSelectedListener(this);

        //sets home button as default checked button on nav menu
        navigationView.getMenu().getItem(1).getSubMenu().getItem(0).setChecked(true);


        getSupportActionBar().setDisplayShowTitleEnabled(false);

        //sets background color of toolbar
        toolbar.setBackgroundColor(Color.rgb(137, 169, 201)); //gray-blue

        //sets up the default exercise
        exerciseChange();
    }

    //responsible for graph creation and formatting
    private void formatGraph()
    {
        forcePlot = (XYPlot) findViewById(R.id.forcePlot);                      //finds force plot
        forceHistory = new SimpleXYSeries("Force");                             //creates new XY series
        forcePlot.getLayoutManager().remove(forcePlot.getLegendWidget());       //removes the legend
        forcePlot.setDomainBoundaries(0, preferences.getInt("FreeModeXRange", 0), BoundaryMode.FIXED);               //Sets x axis boundaries

        //formats margins (0 for nexus 7)
        forcePlot.getGraphWidget().setMarginBottom(-40);
        forcePlot.getGraphWidget().setMarginLeft(-40);

        //format of the line for the force graph
        LineAndPointFormatter forceLine = new LineAndPointFormatter(Color.BLUE, null, null, null);
        forceLine.getLinePaint().setStrokeWidth(PixelUtils.dpToPix(5));

        //adds series to plot
        forcePlot.addSeries(forceHistory, forceLine);

        //Sets the number of divisions on the x-axis (time)
        forcePlot.setDomainStepValue(6);

        if (preferences.getInt("YAxisRange", 0) <= 40)
        {
            forcePlot.setRangeStep(XYStepMode.INCREMENT_BY_VAL, 5);
            forcePlot.setRangeBoundaries(-5, preferences.getInt("YAxisRange", 0), BoundaryMode.FIXED);//Sets y axis boundaries
        }
        else if (preferences.getInt("YAxisRange", 0) <= 100)
        {
            forcePlot.setRangeStep(XYStepMode.INCREMENT_BY_VAL, 10);
            forcePlot.setRangeBoundaries(-10, preferences.getInt("YAxisRange", 0), BoundaryMode.FIXED);//Sets y axis boundaries
        }
        else
        {
            forcePlot.setRangeStep(XYStepMode.INCREMENT_BY_VAL, 20);
            forcePlot.setRangeBoundaries(-20, preferences.getInt("YAxisRange", 0), BoundaryMode.FIXED);//Sets y axis boundaries
        }

        //format of the line for the first level line
        LineAndPointFormatter levelLine = new LineAndPointFormatter(Color.GREEN, null, null, null);
        levelLine.getLinePaint().setStrokeWidth(PixelUtils.dpToPix(5));

        FirstLevel = new SimpleXYSeries("Level 1"); //creates new XY series
        FirstLevel.addFirst(-1, FirstLevel_value);  //adds first point at (-1, FirstLevel_value)
        forcePlot.addSeries(FirstLevel, levelLine); //adds series to plot

        //format of the line for the lower level line
        LineAndPointFormatter lowerLevelLine = new LineAndPointFormatter(Color.DKGRAY, null, null, null);
        lowerLevelLine.getLinePaint().setStrokeWidth(PixelUtils.dpToPix(5));

        LowerLevel = new SimpleXYSeries("Level 1"); //creates new XY series
        LowerLevel.addFirst(-1, LowerLevel_value);  //adds first point at (-1, FirstLevel_value)
        forcePlot.addSeries(LowerLevel, lowerLevelLine); //adds series to plot

        //sets all of the background pieces to white
        forcePlot.getBorderPaint().setColor(Color.WHITE);
        forcePlot.getGraphWidget().getBackgroundPaint().setColor(Color.WHITE);
        forcePlot.getBackgroundPaint().setColor(Color.WHITE);

        //sets all of the label text to black
        forcePlot.getGraphWidget().getDomainTickLabelPaint().setColor(Color.BLACK);
        forcePlot.getGraphWidget().getDomainOriginLinePaint().setColor(Color.BLACK);
        forcePlot.getGraphWidget().getRangeTickLabelPaint().setColor(Color.BLACK);
        forcePlot.getGraphWidget().getRangeOriginLinePaint().setColor(Color.BLACK);

        //sets up the x-axis format
        forcePlot.getGraphWidget().getDomainOriginLinePaint().setColor(Color.BLACK);
        forcePlot.getGraphWidget().getDomainOriginLinePaint().setStrokeWidth(4); //7 for N7

        //sets up the y-axis format
        forcePlot.getGraphWidget().getRangeOriginLinePaint().setColor(Color.BLACK);
        forcePlot.getGraphWidget().getRangeOriginLinePaint().setStrokeWidth(4); //7 for N7

        //formats gridlines
        forcePlot.getGraphWidget().getDomainGridLinePaint().setColor(Color.BLACK);
        forcePlot.getGraphWidget().getRangeGridLinePaint().setColor(Color.BLACK);

        //formats x-axis label
        forcePlot.setDomainLabel("Time (s)");
        forcePlot.getDomainLabelWidget().getLabelPaint().setTextSize(PixelUtils.dpToPix(40));
        forcePlot.getDomainLabelWidget().getLabelPaint().setColor(Color.BLACK);
        forcePlot.getDomainLabelWidget().pack();
        forcePlot.getDomainLabelWidget().position(0, XLayoutStyle.ABSOLUTE_FROM_CENTER, 0, YLayoutStyle.RELATIVE_TO_BOTTOM, AnchorPosition.BOTTOM_MIDDLE);

        //formats y-axis label
        forcePlot.setRangeLabel("Force (lbs)");
        forcePlot.getRangeLabelWidget().getLabelPaint().setTextSize(PixelUtils.dpToPix(40));
        forcePlot.getRangeLabelWidget().getLabelPaint().setColor(Color.BLACK);
        forcePlot.getRangeLabelWidget().pack();

        //formats title
        forcePlot.setTitle("Link to FoSe to Start");
        forcePlot.getTitleWidget().getLabelPaint().setTextSize(PixelUtils.dpToPix(40));
        forcePlot.getTitleWidget().getLabelPaint().setColor(Color.BLACK);
        forcePlot.getTitleWidget().pack(); //180, ..., 50 for N7
        forcePlot.getTitleWidget().position(100, XLayoutStyle.ABSOLUTE_FROM_LEFT, 50, YLayoutStyle.ABSOLUTE_FROM_TOP);
        forcePlot.getGraphWidget().setPadding(100, 25, 25, 100);
    }

    //default onResume function
    protected void onResume()
    {
        super.onResume();
        onResumeProcess();  //onResume Process by BlunoLibrary
        updateGraph();
        Log.i("Status", "Resume");
    }


    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data)
    {
        onActivityResultProcess(requestCode, resultCode, data); //onActivityResult Process by BlunoLibrary
        super.onActivityResult(requestCode, resultCode, data);
    }

    @Override
    protected void onPause()
    {
        super.onPause();
        //onPauseProcess();    //onPause Process by BlunoLibrary
        Log.i("Status", "Paused");
    }

    protected void onStop()
    {
        super.onStop();
        //onStopProcess();    //onStop Process by BlunoLibrary
        Log.i("Status", "Stopped");
    }

    @Override
    protected void onDestroy()
    {
        super.onDestroy();
        onDestroyProcess(); //onDestroy Process by BlunoLibrary
        Log.i("Status", "Destroyed");
    }

    //can trigger stuff when the connection states change
    @Override
    public void onConectionStateChange(connectionStateEnum theConnectionState)
    {//Once connection state changes, this function will be called
        switch (theConnectionState)
        {                                            //Four connection state
            case isConnected:
                //buttonScan.setText("Connected");
                break;
            case isConnecting:
                //buttonScan.setText("Connecting");
                break;
            case isToScan:
                //buttonScan.setText("Scan");
                break;
            case isScanning:
                //buttonScan.setText("Scanning");
                break;
            case isDisconnecting:
                //buttonScan.setText("isDisconnecting");
                break;
            default:
                break;
        }
    }


    //Once connection data received, this function will be called
    @Override
    public void onSerialReceived(String theString)
    {
        double currentForce = -1;
        boolean skip = false;

        //if the graph hasn't been started yet
        if (startTime == -1)
        {
            //returns Unix time in seconds
            startTime = System.currentTimeMillis() / 1000.0;
        }

        Log.i(MY_TAG, theString);

        //if the data recieved from the Bluno is the right length
        //TODO: Better error checking here to prevent crashes
        if (theString.length() > 7 && theString.length() < 11)
        {

            rawValue = Double.valueOf(theString);

            //sets the zero value to the starting value by default
            if (zeroValue == -1)
            {
                zeroValue = rawValue;
            }

            //TODO: update the calibration value
            currentForce = (rawValue - zeroValue); // * calibration_value (future option)


            if(currentForce > 220 && !Nathan.isSpeaking())
            {
                Nathan.speak("Danger, Device Limit Exceeded!", TextToSpeech.QUEUE_FLUSH, null, null);
            }
            else if(currentForce > 200 && !Nathan.isSpeaking())
            {
                Nathan.speak("Danger, Approaching Device Limit!", TextToSpeech.QUEUE_FLUSH, null, null);
            }


            elapsedTime = System.currentTimeMillis() / 1000.0 - startTime;
        }
        else
        {
            //if the data recieved from the bluno was bad
            Log.i(MY_TAG, "ERROR!!!");
            //this will prevent the graph from updating with the bad valeu
            skip = true;
        }

        //if a countdown is desired, start the countdown
        if (countdown)
        {
            countDownHandler();
        }

        //if the graph is not paused, the data received from the bluno was good and the program isn't counting down
        if (!pause && !skip && !countdown)
        {
            //prints out (time, force)
            //Log.i("Data:", "(" + Double.toString(elapsedTime) + "," + Double.toString(currentForce) + ")");


            //once the force history reaches the max size, start removing the old data points
            if (forceHistory.size() > HISTORY_SIZE)
            {
                forceHistory.removeFirst();
            }

            //adds the next data point on
            forceHistory.addLast(elapsedTime, currentForce);

            //data smoothing
            if (forceHistory.size() > 1)
            {
                double y1 = forceHistory.getY(forceHistory.size() - 1).doubleValue();
                double y2 = forceHistory.getY(forceHistory.size() - 2).doubleValue();
                forceHistory.setY((y1 + y2) / 2, forceHistory.size() - 1);
            }

            //if free mode is selected
            if (exercise.equals("Free Mode"))
            {
                //prints out current force
                forcePlot.setTitle(String.format(Locale.US,"%.1f lbs", currentForce));

                //adds the second point on the the FirstLevel line, making it visible
                if (FirstLevel.size() == 1)
                {
                    FirstLevel.addLast(10000, FirstLevel_value);
                }

                if(LowerLevel.size() > 1)
                {
                    LowerLevel.removeLast();
                }

                //moves the graph as the line approaches the edge
                if (elapsedTime > preferences.getInt("FreeModeXRange", 0) * 0.9)
                {
                    forcePlot.setDomainBoundaries(elapsedTime - preferences.getInt("FreeModeXRange", 0) * 0.9, elapsedTime + preferences.getInt("FreeModeXRange", 0) * 0.1, BoundaryMode.FIXED);
                }

                //if the force level is above the threshold, turn the background green
                if (currentForce > FirstLevel_value)
                {
                    forcePlot.getGraphWidget().getGridBackgroundPaint().setColor(Color.rgb(125, 210, 125)); //Green
                }
                else //keep the background gray
                {
                    forcePlot.getGraphWidget().getGridBackgroundPaint().setColor(Color.GRAY);
                }
            }

            //if max force mode is selected
            if (exercise.equals("Max Force"))
            {
                if(FirstLevel.size() > 1)
                {
                    FirstLevel.removeLast();
                }

                if(LowerLevel.size() > 1)
                {
                    LowerLevel.removeLast();
                }

                //updates max force value
                if (currentForce > maxForce)
                {
                    maxForce = currentForce;
                }


                //changes the force plot title to the maximum force for that trial
                forcePlot.setTitle(String.format(Locale.US, "Trial %d of 3           Max Force: %.1f lbs", maxForceCounter, maxForce));

                //talking countdown
                if (elapsedTime > 5)
                {
                    Nathan.speak("Stop.", TextToSpeech.QUEUE_FLUSH, null, null);

                    if (maxForceCounter == 1)
                    {
                        graph1X = new double[forceHistory.size()];
                        graph1Y = new double[forceHistory.size()];

                        for (int i = 0; i < forceHistory.size(); i++)
                        {
                            graph1X[i] = forceHistory.getX(i).doubleValue();
                            graph1Y[i] = forceHistory.getY(i).doubleValue();

                        }
                    }
                    else if (maxForceCounter == 2)
                    {
                        graph2X = new double[forceHistory.size()];
                        graph2Y = new double[forceHistory.size()];

                        for (int i = 0; i < forceHistory.size(); i++)
                        {
                            graph2X[i] = forceHistory.getX(i).doubleValue();
                            graph2Y[i] = forceHistory.getY(i).doubleValue();

                        }
                    }
                    else if (maxForceCounter == 3)
                    {
                        graph3X = new double[forceHistory.size()];
                        graph3Y = new double[forceHistory.size()];

                        for (int i = 0; i < forceHistory.size(); i++)
                        {
                            graph3X[i] = forceHistory.getX(i).doubleValue();
                            graph3Y[i] = forceHistory.getY(i).doubleValue();
                        }

                        controlMenu.getItem(1).setTitle("Start New Test");

                        //launches results page and sends force data
                        Intent intent = new Intent("jeenaa.fose.ResultsPage");
                        intent.putExtra("graph1X", graph1X);
                        intent.putExtra("graph1Y", graph1Y);
                        intent.putExtra("graph2X", graph2X);
                        intent.putExtra("graph2Y", graph2Y);
                        intent.putExtra("graph3X", graph3X);
                        intent.putExtra("graph3Y", graph3Y);
                        startActivity(intent);
                    }


                    //pauses when done at 5s
                    pauseHandler();

                    maxForceCounter++;
                    if (maxForceCounter > 3)
                    {
                        maxForceCounter = 1;
                    }
                }
                else if (elapsedTime > 4 && elapsedTime < 4.2 && !Nathan.isSpeaking())
                {
                    Nathan.speak("1", TextToSpeech.QUEUE_FLUSH, null, null);
                }
                else if (elapsedTime > 3 && elapsedTime < 3.2 && !Nathan.isSpeaking())
                {
                    Nathan.speak("2", TextToSpeech.QUEUE_FLUSH, null, null);
                }
                if (elapsedTime > 2 && elapsedTime < 2.2 && !Nathan.isSpeaking())
                {
                    Nathan.speak("3", TextToSpeech.QUEUE_FLUSH, null, null);
                }
            }
            else
            {
                //resets the maxForce counter when the user leaves max force mode
                maxForceCounter = 1;
            }


            if (exercise.equals("Count Reps"))
            {
                //moves the graph as the line approaches the edge
                if (elapsedTime > 27)
                {
                    forcePlot.setDomainBoundaries(elapsedTime - 27, elapsedTime + 3, BoundaryMode.FIXED);
                }

                //adds the second point on the the FirstLevel line, making it visible
                if (FirstLevel.size() == 1)
                {
                    FirstLevel.addLast(10000, FirstLevel_value);
                }

                //adds the second point on the the FirstLevel line, making it visible
                if (LowerLevel.size() == 1)
                {
                    LowerLevel.addLast(10000, LowerLevel_value);
                }

                //if the person is above the target threshold
                if (currentForce > FirstLevel_value)
                {

                    //if the person has been in the target range for repTime
                    if (repReset && (elapsedTime - repTimer) > repTime)
                    {
                        //increase the number of reps, and change the background color
                        repNumber += 1;
                        forcePlot.setTitle("Rep: " + Integer.toString(repNumber));
                        forcePlot.getGraphWidget().getGridBackgroundPaint().setColor(Color.GRAY);

                        //verbally tell the user what rep they are on
                        Nathan.speak(Integer.toString(repNumber), TextToSpeech.QUEUE_ADD, null, null);

                        //start rest timer and reset repReset
                        restTimer = elapsedTime;
                        repReset = false;

                    }

                    //if the patient is in the target range, but hasn't completed the goal amount of time
                    if (repReset && (elapsedTime - repTimer) < repTime)
                    {
                        forcePlot.getGraphWidget().getGridBackgroundPaint().setColor(Color.rgb(0, 127, 92));//bright green
                    }
                }
                else //the patient is below the threshold for a rep
                {
                    //reset the rep timer
                    repTimer = elapsedTime;

                    //once the patient has rested for "restTime" (i.e. below LowerLevel_value) it tells the patient to GO
                    if (currentForce < LowerLevel_value && (elapsedTime - restTimer) > restTime)
                    {
                        repReset = true;
                        notificationSound.start(); //plays noise
                    }

                    if (repReset)
                    {
                        //changes background to green
                        forcePlot.getGraphWidget().getGridBackgroundPaint().setColor(Color.rgb(125, 210, 125)); //Green
                    }
                }
            }
        }

        //refreshes the plot
        forcePlot.redraw();
    }

    //handles the countdown
    private void countDownHandler()
    {
        //starts the countdown timer and plays the countdown noise
        if (countDownTimer == -1)
        {
            countDownTimer = System.currentTimeMillis() / 1000.0;
            MediaPlayer mp = MediaPlayer.create(this, R.raw.countdown);
            mp.start();
        }

        //calculates the time elapsed since starting the countdown
        double timerTime = System.currentTimeMillis() / 1000.0 - countDownTimer;

        //changes the text and background during the countdown
        if (timerTime < 1)
        {
            forcePlot.getGraphWidget().getGridBackgroundPaint().setColor(Color.GRAY);
            forcePlot.setTitle("3");
            Log.i(MY_TAG, "3");
        }
        else if (timerTime < 2)
        {
            forcePlot.getGraphWidget().getGridBackgroundPaint().setColor(Color.rgb(210, 125, 125));//red
            forcePlot.setTitle("2");
            Log.i(MY_TAG, "2");
        }
        else if (timerTime < 3)
        {
            forcePlot.getGraphWidget().getGridBackgroundPaint().setColor(Color.rgb(210, 210, 125));//yellow
            forcePlot.setTitle("1");
            Log.i(MY_TAG, "1");
        }
        else
        {
            forcePlot.getGraphWidget().getGridBackgroundPaint().setColor(Color.rgb(125, 210, 125));//green
            forcePlot.setTitle("GO");
            Log.i(MY_TAG, "GO");

            startTime = System.currentTimeMillis() / 1000.0; //sets the start time
            elapsedTime = 0;                                 //sets the first time point to 0
            countdown = false;                               //stops the countdown
            pause = true;
            pauseHandler();                                  //unapauses the graph
        }
    }

    //sets up the spinner for changing exercises
    public void exerciseChange()
    {
        //sets up spinner
        category = getResources().getStringArray(R.array.category);

        SpinnerAdapter spinnerAdapter = ArrayAdapter.createFromResource(getApplicationContext(), R.array.category, R.layout.spinner_dropdown_item);
        Spinner navigationSpinner = new Spinner(getSupportActionBar().getThemedContext());
        navigationSpinner.setAdapter(spinnerAdapter);
        toolbar.addView(navigationSpinner, 0);

        navigationSpinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener()
        {
            //this is where you can do stuff when a new item is selected
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id)
            {
                Toast.makeText(MainActivity.this,
                        "you selected: " + category[position],
                        Toast.LENGTH_SHORT).show();
                exercise = category[position];
                pause = true;

                clearHandler();


            }

            @Override
            public void onNothingSelected(AdapterView<?> parent)
            {

            }
        });
    }

    @Override
    public void onBackPressed()
    {
        DrawerLayout drawer = (DrawerLayout) findViewById(R.id.drawer_layout);
        if (drawer.isDrawerOpen(GravityCompat.START))
        {
            drawer.closeDrawer(GravityCompat.START);
        }
        else
        {
            super.onBackPressed();
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu)
    {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.main, menu);
        controlMenu = menu;
        return true;
    }

    //takes care of when the toolbar buttons are pressed
    @Override
    public boolean onOptionsItemSelected(MenuItem item)
    {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.

        switch (item.getItemId())
        {
            case R.id.Pause:
                pauseHandler();
                break;
            case R.id.Clear:
                clearHandler();
                break;

            case R.id.Zero:
                zeroValue = rawValue;
                break;

        }

        return super.onOptionsItemSelected(item);
    }


    //controls what happens when the graph is cleared
    private void clearHandler()
    {
        //removes all the force history terms
        while (forceHistory.size() > 0)
        {
            forceHistory.removeLast();
        }

        //sets all the parameters for the max force trial
        if (exercise.equals("Max Force"))
        {
            Nathan.setSpeechRate((float) 0.75);

            //gets rid of the play/pause button
            controlMenu.getItem(0).setVisible(false);

            //changes the clear button to "Next Trial"
            controlMenu.getItem(1).setIcon(R.drawable.ic_replay_white_24dp);
            controlMenu.getItem(1).setTitle("Next Trial");

            //starts the countdown
            countdown = true;
            countDownTimer = -1;

            //resets max force
            maxForce = 0;

            //resets x and y axis boundaries
            forcePlot.setDomainBoundaries(0, 5, BoundaryMode.FIXED);
        }
        else
        {
            //resets the speech rate, play/pause button, clear button;
            Nathan.setSpeechRate(1);
            controlMenu.getItem(0).setVisible(true);
            controlMenu.getItem(1).setIcon(R.drawable.ic_clear_white_24dp);
            controlMenu.getItem(1).setTitle("Clear");
        }

        if (exercise.equals("Free Mode"))
        {
            forcePlot.setDomainBoundaries(0, preferences.getInt("FreeModeXRange", 0), BoundaryMode.FIXED);
        }

        //resets all the parameters for count reps
        if (exercise.equals("Count Reps"))
        {
            //resets x and y axis boundaries
            forcePlot.setDomainBoundaries(0, 30, BoundaryMode.FIXED);

            //resets rep counting variables
            repNumber = 0;
            repReset = true;
            restTimer = 0;
            repTimer = 0;
            forcePlot.setTitle("Rep: " + Integer.toString(repNumber));

        }

        //TODO
        /*
        if (exercise.equals("Hold Force"))
        {
            forcePlot.setDomainBoundaries(0, 30, BoundaryMode.FIXED);
        }*/

        //refreshes force plot
        forcePlot.redraw();

        //pauses the graph
        pause = false;
        pauseHandler();
    }

    //handles the pausing and resuming of the graph
    private void pauseHandler()
    {
        if (pause) //transition from paused to playing
        {
            //switches icon to pause
            controlMenu.getItem(0).setIcon(R.drawable.ic_pause_white_24dp);
            controlMenu.getItem(0).setTitle("Pause");

            //takes care of adjusting the time so it is continuous after pausing
            if (forceHistory.size() > 0)
            {
                Toast.makeText(getApplicationContext(), "Resume", Toast.LENGTH_SHORT).show();
                pause = false;
                startTime = (System.currentTimeMillis() / 1000.0
                        - forceHistory.getX(forceHistory.size() - 1).doubleValue());
            }
            else
            {
                Toast.makeText(getApplicationContext(), "Play", Toast.LENGTH_SHORT).show();
                startTime = System.currentTimeMillis() / 1000.0;
                pause = false;
            }
        }
        else //transition from playing to pause
        {
            //change icons
            controlMenu.getItem(0).setIcon(R.drawable.ic_play_arrow_white_24dp);
            controlMenu.getItem(0).setTitle("Play");
            Toast.makeText(getApplicationContext(), "Pause", Toast.LENGTH_SHORT).show();
            pause = true;
        }
    }

    @SuppressWarnings("StatementWithEmptyBody")
    @Override
    public boolean onNavigationItemSelected(MenuItem item)
    {

        // Handle navigation view item clicks here.
        int id = item.getItemId();

        if (id == R.id.nav_instructions)
        {
            Toast.makeText(getApplicationContext(), "Coming Soon!", Toast.LENGTH_SHORT).show();
            navigationView.getMenu().getItem(1).getSubMenu().getItem(3).setChecked(false);
        }
        else if (id == R.id.nav_history)
        {
            double[] graph1X = {0};
            double[] graph1Y = {0};
            double[] graph2X = {0};
            double[] graph2Y = {0};
            double[] graph3X = {0};
            double[] graph3Y = {0};


            Intent intent = new Intent("jeenaa.fose.ResultsPage");
            intent.putExtra("graph1X", graph1X);
            intent.putExtra("graph1Y", graph1Y);
            intent.putExtra("graph2X", graph2X);
            intent.putExtra("graph2Y", graph2Y);
            intent.putExtra("graph3X", graph3X);
            intent.putExtra("graph3Y", graph3Y);
            startActivity(intent);
        }
        else if (id == R.id.nav_home)
        {
            Toast.makeText(getApplicationContext(), "Coming Soon!", Toast.LENGTH_SHORT).show();
        }
        else if (id == R.id.bluetooch)
        {
            //TODO: Add permission code here
            buttonScanOnClickProcess();
            forcePlot.setTitle("Press Play!");
        }
        else if (id == R.id.nav_settings)
        {
            startActivity(new Intent(this, SettingsActivity.class));
        }
        else if (id == R.id.nav_share)
        {
            Toast.makeText(getApplicationContext(), "Coming Soon!", Toast.LENGTH_SHORT).show();
        }
        else if (id == R.id.nav_feedback)
        {
            Toast.makeText(getApplicationContext(), "Coming Soon!", Toast.LENGTH_SHORT).show();
        }

        DrawerLayout drawer = (DrawerLayout) findViewById(R.id.drawer_layout);
        drawer.closeDrawer(GravityCompat.START);
        return true;
    }

    private void updateGraph()
    {
        if (preferences.getInt("YAxisRange", 0) <= 40)
        {
            forcePlot.setRangeStep(XYStepMode.INCREMENT_BY_VAL, 5);
            forcePlot.setRangeBoundaries(-5, preferences.getInt("YAxisRange", 0), BoundaryMode.FIXED);//Sets y axis boundaries
        }
        else if (preferences.getInt("YAxisRange", 0) <= 100)
        {
            forcePlot.setRangeStep(XYStepMode.INCREMENT_BY_VAL, 10);
            forcePlot.setRangeBoundaries(-10, preferences.getInt("YAxisRange", 0), BoundaryMode.FIXED);//Sets y axis boundaries
        }
        else
        {
            forcePlot.setRangeStep(XYStepMode.INCREMENT_BY_VAL, 20);
            forcePlot.setRangeBoundaries(-20, preferences.getInt("YAxisRange", 0), BoundaryMode.FIXED);//Sets y axis boundaries
        }

        FirstLevel_value = preferences.getInt("RepAmount", 0);
        if (FirstLevel.size() == 2)
        {
            FirstLevel.removeFirst();
            FirstLevel.removeLast();
            FirstLevel.addFirst(-1, FirstLevel_value);
            FirstLevel.addLast(10000, FirstLevel_value);
        }
        else
        {
            FirstLevel.removeFirst();
            FirstLevel.addFirst(-1, FirstLevel_value);
        }

        LowerLevel_value = preferences.getInt("LowerThreshold", 0);
        if (LowerLevel.size() == 2)
        {
            LowerLevel.removeFirst();
            LowerLevel.removeLast();
            LowerLevel.addFirst(-1, LowerLevel_value);
            LowerLevel.addLast(10000, LowerLevel_value);
        }
        else
        {
            LowerLevel.removeFirst();
            LowerLevel.addFirst(-1, LowerLevel_value);
        }

        if (exercise != null && exercise.equals("Free Mode"))
        {
            forcePlot.setDomainBoundaries(0, preferences.getInt("FreeModeXRange", 0), BoundaryMode.FIXED);
        }

        repTime = preferences.getInt("RepTime", 0);
        restTime = preferences.getInt("RestTime", 0);

        forcePlot.redraw();
    }
}