package jeenaa.fose;

import android.app.Dialog;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.support.design.widget.NavigationView;
import android.support.v4.view.GravityCompat;
import android.support.v4.widget.DrawerLayout;
import android.support.v7.app.ActionBarDrawerToggle;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.text.method.KeyListener;
import android.util.Log;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.widget.*;
import com.androidplot.ui.AnchorPosition;
import com.androidplot.ui.XLayoutStyle;
import com.androidplot.ui.YLayoutStyle;
import com.androidplot.util.PixelUtils;
import com.androidplot.xy.*;

import java.io.*;
import java.util.Calendar;

public class ResultsPage extends AppCompatActivity implements NavigationView.OnNavigationItemSelectedListener
{

    SimpleXYSeries graph1, graph2, graph3, graph1b, graph2b, graph3b;
    XYPlot comparePlot1, comparePlot2, comparePlot3;

    TextView MaxForce1, MaxForce2, MaxForce3, OverallMaxForce, AvgMaxForce1, COV, AvgMaxForceDef;
    TextView MaxForce1b, MaxForce2b, MaxForce3b, OverallMaxForceb, AvgMaxForce1b, COVb, OverallMaxForceDef;

    EditText Title, Date;
    TableLayout resultsTable;

    double maxForce1, maxForce2, AMF1, AMF2;

    Menu controlMenu;

    boolean compareModeOn;

    KeyListener titleEditListener;

    String plot1Name, plot1Date, plot2Name, plot2Date;

    SharedPreferences preferences;

    private NavigationView navigationView;      //the navigation view that makes up the nav drawer

    @Override
    protected void onCreate(Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_results_page);
        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);

        preferences = PreferenceManager.getDefaultSharedPreferences(this);

        compareModeOn = false;



        //sets up nav drawer
        DrawerLayout drawer = (DrawerLayout) findViewById(R.id.drawer_layout);
        ActionBarDrawerToggle toggle = new ActionBarDrawerToggle(
                this, drawer, toolbar, R.string.navigation_drawer_open, R.string.navigation_drawer_close);
        drawer.setDrawerListener(toggle);
        toggle.syncState();

        navigationView = (NavigationView) findViewById(R.id.nav_view);
        navigationView.setNavigationItemSelectedListener(this);

        //sets home button as default checked button on nav menu
        navigationView.getMenu().getItem(1).getSubMenu().getItem(0).setChecked(false);
        navigationView.getMenu().getItem(1).getSubMenu().getItem(2).setChecked(true);

        //sets background color of toolbar
        toolbar.setBackgroundColor(Color.rgb(137, 169, 201)); //gray-blue

        comparePlot1 = (XYPlot) findViewById(R.id.comparePlot1);            //finds force plot
        comparePlot2 = (XYPlot) findViewById(R.id.comparePlot2);            //finds force plot
        comparePlot3 = (XYPlot) findViewById(R.id.comparePlot3);            //finds force plot
        formatGraph(comparePlot1);
        formatGraph(comparePlot2);
        formatGraph(comparePlot3);

        resultsTable = (TableLayout) findViewById(R.id.resultsTable);
        resultsTable.setColumnCollapsed(2, true);
        resultsTable.setColumnCollapsed(3, true);


        graph1 = new SimpleXYSeries("Force");
        graph2 = new SimpleXYSeries("Force");
        graph3 = new SimpleXYSeries("Force");

        graph1b = new SimpleXYSeries("Force");
        graph2b = new SimpleXYSeries("Force");
        graph3b = new SimpleXYSeries("Force");


        //format of the line for the force graph
        LineAndPointFormatter forceLine = new LineAndPointFormatter(Color.BLUE, null, null, null);
        forceLine.getLinePaint().setStrokeWidth(PixelUtils.dpToPix(4));

        Bundle extras = getIntent().getExtras();

        double[] graph1X = extras.getDoubleArray("graph1X");
        double[] graph1Y = extras.getDoubleArray("graph1Y");
        //Log.i("Out", Double.toString(graph1X.length));

        for (int i = 0; i < graph1X.length; i++)
        {
            //Log.i("Data",Double.toString(graph1X[i]) + "," + Double.toString(graph1Y[i]));
            graph1.addLast(graph1X[i], graph1Y[i]);
        }

        double[] graph2X = extras.getDoubleArray("graph2X");
        double[] graph2Y = extras.getDoubleArray("graph2Y");

        for (int i = 0; i < graph2X.length; i++)
        {
            graph2.addLast(graph2X[i], graph2Y[i]);
        }

        double[] graph3X = extras.getDoubleArray("graph3X");
        double[] graph3Y = extras.getDoubleArray("graph3Y");

        for (int i = 0; i < graph3X.length; i++)
        {
            graph3.addLast(graph3X[i], graph3Y[i]);
        }

        //adds series to plot
        comparePlot1.addSeries(graph1, forceLine);
        comparePlot2.addSeries(graph2, forceLine);
        comparePlot3.addSeries(graph3, forceLine);

        Date = (EditText) findViewById(R.id.dateText);
        Title = (EditText) findViewById(R.id.ExerciseText);
        MaxForce1 = (TextView) findViewById(R.id.MaxForce1);
        MaxForce2 = (TextView) findViewById(R.id.MaxForce2);
        MaxForce3 = (TextView) findViewById(R.id.MaxForce3);
        OverallMaxForce = (TextView) findViewById(R.id.OverallMaxForce);
        AvgMaxForce1 = (TextView) findViewById(R.id.AvgMaxForce);
        COV = (TextView) findViewById(R.id.COV);

        MaxForce1b = (TextView) findViewById(R.id.MaxForce1b);
        MaxForce2b = (TextView) findViewById(R.id.MaxForce2b);
        MaxForce3b = (TextView) findViewById(R.id.MaxForce3b);
        OverallMaxForceb = (TextView) findViewById(R.id.OverallMaxForceb);
        AvgMaxForce1b = (TextView) findViewById(R.id.AvgMaxForceb);
        COVb = (TextView) findViewById(R.id.COVb);
        AvgMaxForceDef = (TextView) findViewById(R.id.AvgMaxForceDef);
        OverallMaxForceDef = (TextView) findViewById(R.id.MaxForceDef);

        calculateStats();

        Calendar c = Calendar.getInstance();
        Date.setText(String.format("%tB %te, %tY %tl:%tM %tp", c, c, c, c, c, c));
    }

    public boolean onCreateOptionsMenu(Menu menu)
    {
        MenuInflater menuInflater = getMenuInflater();
        menuInflater.inflate(R.menu.comparemenu, menu);
        controlMenu = menu;
        return super.onCreateOptionsMenu(menu);
    }

    //default onResume function
    protected void onResume()
    {
        super.onResume();

        XYPlot[] Z = new XYPlot[]{comparePlot1, comparePlot2, comparePlot3};

        for(int i = 0; i < 3; i++)
        {
            XYPlot thePlot = Z[i];

            //Sets the number of divisions on the y-axis (lbs)
            if(preferences.getInt("YAxisRange", 0) <= 40)
            {
                thePlot.setRangeStep(XYStepMode.INCREMENT_BY_VAL, 5);
                thePlot.setRangeBoundaries(-5, preferences.getInt("YAxisRange", 0), BoundaryMode.FIXED);//Sets y axis boundaries
            }
            else if(preferences.getInt("YAxisRange", 0) <= 100)
            {
                thePlot.setRangeStep(XYStepMode.INCREMENT_BY_VAL, 10);
                thePlot.setRangeBoundaries(-10, preferences.getInt("YAxisRange", 0), BoundaryMode.FIXED);//Sets y axis boundaries
            }
            else
            {
                thePlot.setRangeStep(XYStepMode.INCREMENT_BY_VAL, 20);
                thePlot.setRangeBoundaries(-20, preferences.getInt("YAxisRange", 0), BoundaryMode.FIXED);//Sets y axis boundaries
            }

            thePlot.redraw();
        }




        Log.i("Status", "Resume");
    }

    //finds and returns the maximum of a double array (cause apparently that's not a built in function?)
    private double findMax(double[] X)
    {
        double currentMax = 0;

        for (int i = 0; i < X.length; i++)
        {
            if (X[i] > currentMax)
            {
                currentMax = X[i];
            }
        }

        return currentMax;
    }

    private double[] findMax(XYSeries X)
    {
        double[] currentMax = {0,0};

        for (int i = 0; i < X.size(); i++)
        {
            if (X.getY(i).doubleValue() > currentMax[0])
            {
                currentMax[0] = X.getY(i).doubleValue();
                currentMax[1] = X.getX(i).doubleValue();
            }
        }

        return currentMax;
    }

    private void formatGraph(XYPlot thePlot)
    {
        thePlot.getLayoutManager().remove(thePlot.getLegendWidget());       //removes the legend
        thePlot.setDomainBoundaries(0, 5, BoundaryMode.FIXED);               //Sets x axis boundaries

        thePlot.getGraphWidget().getGridBackgroundPaint().setColor(Color.rgb(125, 210, 125));//Green

        //Sets the number of divisions on the y-axis (lbs)
        if(preferences.getInt("YAxisRange", 0) <= 40)
        {
            thePlot.setRangeStep(XYStepMode.INCREMENT_BY_VAL, 5);
            thePlot.setRangeBoundaries(-5, preferences.getInt("YAxisRange", 0), BoundaryMode.FIXED);//Sets y axis boundaries
        }
        else if(preferences.getInt("YAxisRange", 0) <= 100)
        {
            thePlot.setRangeStep(XYStepMode.INCREMENT_BY_VAL, 10);
            thePlot.setRangeBoundaries(-10, preferences.getInt("YAxisRange", 0), BoundaryMode.FIXED);//Sets y axis boundaries
        }
        else
        {
            thePlot.setRangeStep(XYStepMode.INCREMENT_BY_VAL, 20);
            thePlot.setRangeBoundaries(-20, preferences.getInt("YAxisRange", 0), BoundaryMode.FIXED);//Sets y axis boundaries
        }


        //sets all of the background pieces to white
        thePlot.getBorderPaint().setColor(Color.WHITE);
        thePlot.getGraphWidget().getBackgroundPaint().setColor(Color.WHITE);
        thePlot.getBackgroundPaint().setColor(Color.WHITE);

        //sets all of the label text to black
        thePlot.getGraphWidget().getDomainTickLabelPaint().setColor(Color.BLACK);
        thePlot.getGraphWidget().getDomainOriginLinePaint().setColor(Color.BLACK);
        thePlot.getGraphWidget().getRangeTickLabelPaint().setColor(Color.BLACK);
        thePlot.getGraphWidget().getRangeOriginLinePaint().setColor(Color.BLACK);

        //sets up the x-axis format
        thePlot.getGraphWidget().getDomainOriginLinePaint().setColor(Color.BLACK);
        thePlot.getGraphWidget().getDomainOriginLinePaint().setStrokeWidth(4);

        //sets up the y-axis format
        thePlot.getGraphWidget().getRangeOriginLinePaint().setColor(Color.BLACK);
        thePlot.getGraphWidget().getRangeOriginLinePaint().setStrokeWidth(4);

        //formats gridlines
        thePlot.getGraphWidget().getDomainGridLinePaint().setColor(Color.BLACK);
        thePlot.getGraphWidget().getRangeGridLinePaint().setColor(Color.BLACK);

        //formats x-axis label
        thePlot.setDomainLabel("Time (s)");
        thePlot.getDomainLabelWidget().getLabelPaint().setTextSize(PixelUtils.dpToPix(20));
        thePlot.getDomainLabelWidget().getLabelPaint().setColor(Color.BLACK);
        thePlot.getDomainLabelWidget().pack();
        thePlot.getDomainLabelWidget().position(0, XLayoutStyle.ABSOLUTE_FROM_CENTER, 0, YLayoutStyle.RELATIVE_TO_BOTTOM, AnchorPosition.BOTTOM_MIDDLE);
        thePlot.setDomainStepValue(6);

        //formats y-axis label
        thePlot.setRangeLabel("Force (lbs)");
        thePlot.getRangeLabelWidget().getLabelPaint().setTextSize(PixelUtils.dpToPix(20));
        thePlot.getRangeLabelWidget().getLabelPaint().setColor(Color.BLACK);
        thePlot.getRangeLabelWidget().pack();

        //formats title
        thePlot.setTitle("Title");
        thePlot.getTitleWidget().getLabelPaint().setTextSize(PixelUtils.dpToPix(20));
        thePlot.getTitleWidget().getLabelPaint().setColor(Color.BLACK);
        thePlot.getTitleWidget().pack();
        thePlot.getTitleWidget().position(130, XLayoutStyle.ABSOLUTE_FROM_LEFT, 50, YLayoutStyle.ABSOLUTE_FROM_TOP);
        thePlot.getGraphWidget().setPadding(50, 15, 15, 50);
    }

    public void textListeners()
    {
        Title.setOnClickListener(
                new View.OnClickListener()
                {
                    @Override
                    public void onClick(View v)
                    {
                        loadGraphs();
                    }
                }
        );


        Date.setOnClickListener(
                new View.OnClickListener()
                {
                    @Override
                    public void onClick(View v)
                    {
                        loadGraphsb();
                    }
                }
        );

    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item)
    {
        switch (item.getItemId())
        {
            case R.id.save:
                saveGraphs();
                break;
            case R.id.load:
                loadGraphs();
                break;
            case R.id.compare:
                Log.i("CompareModeOn:", Boolean.toString(compareModeOn));
                if(!compareModeOn)
                {
                    Log.i("CompareModeOn:", "Turning Compare Mode On");
                    turnCompareOn();
                }
                else
                {
                    Log.i("CompareModeOn:", "Turning Compare Mode Off");
                    turnCompareOff();
                }
                break;
            case R.id.delete:
                deleteGraphs();
                break;
        }
        return super.onOptionsItemSelected(item);
    }

    public void loadGraphs()
    {

        //load data
        final Dialog dialog = new Dialog(ResultsPage.this);
        dialog.setContentView(R.layout.loaddialog);

        File[] x = getFilesDir().listFiles();
        String[] fileNames = new String[x.length];

        for (int i = 0; i < x.length; i++)
        {
            String temp = x[i].toString();
            temp = temp.substring(temp.lastIndexOf('/') + 1);
            fileNames[i] = temp;
            //Log.i("Data", temp);
        }

        final AutoCompleteTextView loadBox = (AutoCompleteTextView) dialog.findViewById(R.id.loadBox);
        ArrayAdapter adapter = new ArrayAdapter(ResultsPage.this, android.R.layout.select_dialog_item, fileNames);

        loadBox.setThreshold(1);
        loadBox.setAdapter(adapter);

        Button loadButton = (Button) dialog.findViewById(R.id.dialog_load_button);
        loadButton.setOnClickListener(new View.OnClickListener()
        {
            public void onClick(View v)
            {
                //where data is finally loaded
                //Log.i("Test", loadBox.getText().toString());
                clearPlotData();
                stringToPlots(loadFile(loadBox.getText().toString()));
                comparePlot1.redraw();
                comparePlot2.redraw();
                comparePlot3.redraw();
                calculateStats();
                dialog.dismiss();
            }
        });

        Button cancelButton = (Button) dialog.findViewById(R.id.load_cancel_button);
        cancelButton.setOnClickListener(new View.OnClickListener()
        {
            @Override
            public void onClick(View v)
            {
                dialog.dismiss();
            }
        });

        dialog.show();
    }


    public void saveGraphs()
    {
        final Dialog dialog = new Dialog(ResultsPage.this);
        dialog.setContentView(R.layout.savedialog);

        final EditText fileNameEditText = (EditText) dialog.findViewById(R.id.fileNameBox);
        fileNameEditText.setText(Title.getText().toString() + " " + Date.getText().toString());

        Button saveButton = (Button) dialog.findViewById(R.id.dialog_save_button);
        saveButton.setOnClickListener(new View.OnClickListener()
        {
            public void onClick(View v)
            {

                String filename = fileNameEditText.getText().toString();
                saveFile(filename, plotsToString());
                dialog.dismiss();
            }
        });

        Button cancelButton = (Button) dialog.findViewById(R.id.save_cancel_button);
        cancelButton.setOnClickListener(new View.OnClickListener()
        {
            @Override
            public void onClick(View v)
            {
                dialog.dismiss();
            }
        });

        dialog.show();

    }

    private void saveFile(String name, String contents)
    {
        FileOutputStream fos = null;

        try
        {
            fos = openFileOutput(name, Context.MODE_PRIVATE);
            byte[] buf = contents.getBytes();
            fos.write(buf);
        } catch (IOException e)
        {
            e.printStackTrace();
        } finally
        {
            try
            {
                fos.close();
            } catch (IOException e)
            {
                e.printStackTrace();
            }
        }
    }

    private String loadFile(String name)
    {
        StringBuilder buffer = new StringBuilder();

        try
        {
            FileInputStream fis = openFileInput(name);
            int read;

            while ((read = fis.read()) != -1)
            {
                buffer.append((char) read);
            }

        } catch (IOException e)
        {
            e.printStackTrace();
        }

        //Log.i("FileContents", buffer.toString());

        return buffer.toString();
    }


    private String plotsToString()
    {
        String masterString = "Graph 1: " + graph1.size() + "\n";

        for (int i = 0; i < graph1.size(); i++)
        {
            masterString += graph1.getX(i) + ", ";
            masterString += graph1.getY(i) + "\n";
        }

        masterString += "Graph 2: " + graph2.size() + "\n";

        for (int i = 0; i < graph2.size(); i++)
        {
            masterString += graph2.getX(i) + ", ";
            masterString += graph2.getY(i) + "\n";
        }

        masterString += "Graph 3: " + graph3.size() + "\n";

        for (int i = 0; i < graph3.size(); i++)
        {
            masterString += graph3.getX(i) + ", ";
            masterString += graph3.getY(i) + "\n";
        }

        masterString += Title.getText().toString() + "\n";
        masterString += Date.getText().toString();


        return masterString;
    }

    private void stringToPlots(String masterString)
    {
        int graph1Length, graph2Length, graph3Length;
        double a, b;

        String line = masterString.substring(0, masterString.indexOf('\n'));
        masterString = masterString.substring(masterString.indexOf('\n') + 1);

        //Log.d("A", line);
        //Log.d("B", Integer.toString(masterString.length()));

        graph1Length = Integer.parseInt(line.substring(9));

        //Log.d("Int Parser", line.substring(9));
        //Log.d("Length", Integer.toString(graph1Length));


        for (int i = 0; i < graph1Length; i++)
        {
            line = masterString.substring(0, masterString.indexOf('\n') + 1);
            Log.d("A", line);
            Log.d("A", Integer.toString(masterString.indexOf('\n')));

            a = Double.parseDouble(line.substring(0, line.indexOf(',')));
            b = Double.parseDouble(line.substring(line.indexOf(' '), line.indexOf('\n')));
            graph1.addLast(a, b);
            masterString = masterString.substring(masterString.indexOf('\n') + 1);
            //Log.i("Check", Double.toString(a) + ", " + Double.toString(b));
        }

        line = masterString.substring(0, masterString.indexOf('\n'));
        graph2Length = Integer.parseInt(line.substring(9));

        masterString = masterString.substring(masterString.indexOf('\n') + 1);

        //Log.d("Length2", Integer.toString(graph2Length));

        for (int i = 0; i < graph2Length; i++)
        {
            line = masterString.substring(0, masterString.indexOf('\n') + 1);
            //Log.d("A", line);
            //Log.d("A", Integer.toString(masterString.indexOf('\n')));

            a = Double.parseDouble(line.substring(0, line.indexOf(',')));
            b = Double.parseDouble(line.substring(line.indexOf(' '), line.indexOf('\n')));
            graph2.addLast(a, b);
            masterString = masterString.substring(masterString.indexOf('\n') + 1);
            //Log.i("Check", Double.toString(a) + ", " + Double.toString(b));
        }

        line = masterString.substring(0, masterString.indexOf('\n'));
        graph3Length = Integer.parseInt(line.substring(9));

        masterString = masterString.substring(masterString.indexOf('\n') + 1);

        //Log.d("Length3", Integer.toString(graph2Length));

        for (int i = 0; i < graph3Length; i++)
        {
            line = masterString.substring(0, masterString.indexOf('\n') + 1);
            //Log.d("A", line);
            //Log.d("A", Integer.toString(masterString.indexOf('\n')));

            a = Double.parseDouble(line.substring(0, line.indexOf(',')));
            b = Double.parseDouble(line.substring(line.indexOf(' '), line.indexOf('\n')));
            graph3.addLast(a, b);
            masterString = masterString.substring(masterString.indexOf('\n') + 1);
            //Log.i("Check", Double.toString(a) + ", " + Double.toString(b));
        }

        Log.d("masterString:", masterString);

        line = masterString.substring(0, masterString.indexOf('\n'));
        plot1Name = line;


        Log.d("line:", line);

        masterString = masterString.substring(masterString.indexOf('\n') + 1);
        plot1Date = masterString;

        Log.d("masterString:", masterString);

        if(!compareModeOn)
        {
            Title.setText(plot1Name);
            Date.setText(plot1Date);
        }
        else
        {
            Title.setText(plot1Name + "\n" + plot1Date);
        }
    }

    private void clearPlotData()
    {

        while (graph1.size() > 0)
        {
            graph1.removeLast();
        }

        while (graph2.size() > 0)
        {
            graph2.removeLast();
        }

        while (graph3.size() > 0)
        {
            graph3.removeLast();
        }
    }

    private void calculateStats()
    {
        double[] mf1 = findMax(graph1);
        double[] mf2 = findMax(graph2);
        double[] mf3 = findMax(graph3);
        maxForce1 = findMax(new double[]{mf1[0], mf2[0], mf3[0]});
        AMF1 = (mf1[0] + mf2[0] + mf3[0]) / 3;
        double stdev = Math.sqrt(Math.pow(mf1[0] - AMF1, 2) + Math.pow(mf2[0] - AMF1, 2) + Math.pow(mf3[0] - AMF1, 2));
        double CoeffVar = stdev / AMF1;

        if(!compareModeOn)
        {
            comparePlot1.setTitle(String.format("Trial 1: Max Force = %.1f lbs", mf1[0]));
            comparePlot2.setTitle(String.format("Trial 2: Max Force = %.1f lbs", mf2[0]));
            comparePlot3.setTitle(String.format("Trial 3: Max Force = %.1f lbs", mf3[0]));
        }

        MaxForce1.setText(String.format("%.1f lbs at %.1f s", mf1[0], mf1[1]));
        MaxForce2.setText(String.format("%.1f lbs at %.1f s", mf2[0], mf2[1]));
        MaxForce3.setText(String.format("%.1f lbs at %.1f s", mf3[0], mf3[1]));
        OverallMaxForce.setText(String.format("%.1f lbs", maxForce1));
        AvgMaxForce1.setText(String.format("%.1f lbs", AMF1));
        COV.setText(String.format("%.4f", CoeffVar));
    }

    private void turnCompareOn()
    {
        compareModeOn = true;

        graph1b.addFirst(0, 0);
        graph2b.addFirst(0, 0);
        graph3b.addFirst(0, 0);

        controlMenu.getItem(0).setVisible(false);
        controlMenu.getItem(1).setVisible(false);
        controlMenu.getItem(2).setVisible(false);
        controlMenu.getItem(3).setTitle("Compare Off");

        plot1Name = Title.getText().toString();
        plot1Date = Date.getText().toString();

        if(graph1.size() > 1)
        {
            Title.setText(plot1Name + "\n" + plot1Date);
        }
        else
        {
            Title.setText("Click to load!" +"\n");
        }



        titleEditListener = Title.getKeyListener();
        Title.setKeyListener(null);
        Title.setClickable(true);
        Date.setClickable(true);
        Title.setBackgroundColor(Color.rgb(102, 140, 255)); //light blue
        Date.setBackgroundColor(Color.rgb(255, 102, 102));  //light red
        Date.setText("Click to load! \n ");

        textListeners();

        comparePlot1.setTitle("Trial 1");
        comparePlot2.setTitle("Trial 2");
        comparePlot3.setTitle("Trial 3");

        //format of the line for the force graph
        LineAndPointFormatter forceLine = new LineAndPointFormatter(Color.RED, null, null, null);
        forceLine.getLinePaint().setStrokeWidth(PixelUtils.dpToPix(4));

        comparePlot1.addSeries(graph1b, forceLine);
        comparePlot2.addSeries(graph2b, forceLine);
        comparePlot3.addSeries(graph3b, forceLine);

        calculateStatsb();

        resultsTable.getLayoutParams().width = 1500;
        resultsTable.setColumnCollapsed(2, false);
        resultsTable.setColumnCollapsed(3, false);
        resultsTable.invalidate();


        comparePlot1.redraw();
        comparePlot2.redraw();
        comparePlot3.redraw();
    }

    private void turnCompareOff()
    {
        compareModeOn = false;
        clearPlotDatab();

        controlMenu.getItem(0).setVisible(true);
        controlMenu.getItem(1).setVisible(true);
        controlMenu.getItem(2).setVisible(true);
        controlMenu.getItem(3).setTitle("Compare On");

        Title.setText(plot1Name);
        Date.setText(plot1Date);

        Title.setOnClickListener(null);
        Date.setOnClickListener(null);

        Title.setKeyListener(titleEditListener);

        Title.setClickable(false);

        Date.setClickable(false);

        Title.setBackgroundColor(Color.WHITE);
        Date.setBackgroundColor(Color.WHITE);



        comparePlot1.setTitle(String.format("Trial 1: Max Force = %.1f lbs", findMax(graph1)[0]));
        comparePlot2.setTitle(String.format("Trial 1: Max Force = %.1f lbs", findMax(graph2)[0]));
        comparePlot3.setTitle(String.format("Trial 1: Max Force = %.1f lbs", findMax(graph3)[0]));

        comparePlot1.removeSeries(graph1b);
        comparePlot2.removeSeries(graph2b);
        comparePlot3.removeSeries(graph3b);


        resultsTable.getLayoutParams().width = 800;
        resultsTable.setColumnCollapsed(2, true);
        resultsTable.setColumnCollapsed(3, true);
        //resultsTable.invalidate();


        comparePlot1.redraw();
        comparePlot2.redraw();
        comparePlot3.redraw();
    }

    private void calculateStatsb()
    {
        double[] mf1 = findMax(graph1b);
        double[] mf2 = findMax(graph2b);
        double[] mf3 = findMax(graph3b);
        maxForce2 = findMax(new double[]{mf1[0], mf2[0], mf3[0]});
        AMF2 = (mf1[0] + mf2[0] + mf3[0]) / 3;
        double stdev = Math.sqrt(Math.pow(mf1[0] - AMF2, 2) + Math.pow(mf2[0] - AMF2, 2) + Math.pow(mf3[0] - AMF2, 2));
        double CoeffVar = stdev / AMF2;
        double overallDeficit = maxForce1 - maxForce2;
        double averageDeficit = AMF1 - AMF2;


        MaxForce1b.setText(String.format("%.1f lbs at %.1f s", mf1[0], mf1[1]));
        MaxForce2b.setText(String.format("%.1f lbs at %.1f s", mf2[0], mf2[1]));
        MaxForce3b.setText(String.format("%.1f lbs at %.1f s", mf3[0], mf3[1]));
        OverallMaxForceb.setText(String.format("%.1f lbs", maxForce2));
        AvgMaxForce1b.setText(String.format("%.1f lbs", AMF2));
        COVb.setText(String.format("%.4f", CoeffVar));
        OverallMaxForceDef.setText(String.format("%.1f lbs", overallDeficit));
        AvgMaxForceDef.setText(String.format("%.1f lbs", averageDeficit));
    }

    public void loadGraphsb()
    {

        //load data
        final Dialog dialog = new Dialog(ResultsPage.this);
        dialog.setContentView(R.layout.loaddialog);

        File[] x = getFilesDir().listFiles();
        String[] fileNames = new String[x.length];

        for (int i = 0; i < x.length; i++)
        {
            String temp = x[i].toString();
            temp = temp.substring(temp.lastIndexOf('/') + 1);
            fileNames[i] = temp;
            //Log.i("Data", temp);
        }

        final AutoCompleteTextView loadBox = (AutoCompleteTextView) dialog.findViewById(R.id.loadBox);
        ArrayAdapter adapter = new ArrayAdapter(ResultsPage.this, android.R.layout.select_dialog_item, fileNames);

        loadBox.setThreshold(1);
        loadBox.setAdapter(adapter);

        Button loadButton = (Button) dialog.findViewById(R.id.dialog_load_button);
        loadButton.setOnClickListener(new View.OnClickListener()
        {
            public void onClick(View v)
            {
                //where data is finally loaded
                //Log.i("Test", loadBox.getText().toString());
                clearPlotDatab();
                stringToPlotsb(loadFile(loadBox.getText().toString()));
                comparePlot1.redraw();
                comparePlot2.redraw();
                comparePlot3.redraw();
                calculateStatsb();
                dialog.dismiss();
            }
        });

        Button cancelButton = (Button) dialog.findViewById(R.id.load_cancel_button);
        cancelButton.setOnClickListener(new View.OnClickListener()
        {
            @Override
            public void onClick(View v)
            {
                dialog.dismiss();
            }
        });

        dialog.show();
    }

    private void stringToPlotsb(String masterString)
    {
        int graph1Length, graph2Length, graph3Length;
        double a, b;

        String line = masterString.substring(0, masterString.indexOf('\n'));
        masterString = masterString.substring(masterString.indexOf('\n') + 1);

        //Log.d("A", line);
        //Log.d("B", Integer.toString(masterString.length()));

        graph1Length = Integer.parseInt(line.substring(9));

        //Log.d("Int Parser", line.substring(9));
        //Log.d("Length", Integer.toString(graph1Length));


        for (int i = 0; i < graph1Length; i++)
        {
            line = masterString.substring(0, masterString.indexOf('\n') + 1);
            Log.d("A", line);
            Log.d("A", Integer.toString(masterString.indexOf('\n')));

            a = Double.parseDouble(line.substring(0, line.indexOf(',')));
            b = Double.parseDouble(line.substring(line.indexOf(' '), line.indexOf('\n')));
            graph1b.addLast(a, b);
            masterString = masterString.substring(masterString.indexOf('\n') + 1);
            //Log.i("Check", Double.toString(a) + ", " + Double.toString(b));
        }

        line = masterString.substring(0, masterString.indexOf('\n'));
        graph2Length = Integer.parseInt(line.substring(9));

        masterString = masterString.substring(masterString.indexOf('\n') + 1);

        //Log.d("Length2", Integer.toString(graph2Length));

        for (int i = 0; i < graph2Length; i++)
        {
            line = masterString.substring(0, masterString.indexOf('\n') + 1);
            //Log.d("A", line);
            //Log.d("A", Integer.toString(masterString.indexOf('\n')));

            a = Double.parseDouble(line.substring(0, line.indexOf(',')));
            b = Double.parseDouble(line.substring(line.indexOf(' '), line.indexOf('\n')));
            graph2b.addLast(a, b);
            masterString = masterString.substring(masterString.indexOf('\n') + 1);
            //Log.i("Check", Double.toString(a) + ", " + Double.toString(b));
        }

        line = masterString.substring(0, masterString.indexOf('\n'));
        graph3Length = Integer.parseInt(line.substring(9));

        masterString = masterString.substring(masterString.indexOf('\n') + 1);

        //Log.d("Length3", Integer.toString(graph2Length));

        for (int i = 0; i < graph3Length; i++)
        {
            line = masterString.substring(0, masterString.indexOf('\n') + 1);
            //Log.d("A", line);
            //Log.d("A", Integer.toString(masterString.indexOf('\n')));

            a = Double.parseDouble(line.substring(0, line.indexOf(',')));
            b = Double.parseDouble(line.substring(line.indexOf(' '), line.indexOf('\n')));
            graph3b.addLast(a, b);
            masterString = masterString.substring(masterString.indexOf('\n') + 1);
            //Log.i("Check", Double.toString(a) + ", " + Double.toString(b));
        }

        Log.d("masterString:", masterString);

        line = masterString.substring(0, masterString.indexOf('\n'));
        plot2Name = line;

        Log.d("line:", line);

        masterString = masterString.substring(masterString.indexOf('\n') + 1);
        plot2Date = masterString;
        Date.setText(plot2Name + "\n" + plot2Date);
        Log.d("masterString:", masterString);
    }

    private void clearPlotDatab()
    {

        while (graph1b.size() > 0)
        {
            graph1b.removeLast();
        }

        while (graph2b.size() > 0)
        {
            graph2b.removeLast();
        }

        while (graph3b.size() > 0)
        {
            graph3b.removeLast();
        }
    }

    private void deleteGraphs()
    {
        //load data
        final Dialog dialog = new Dialog(ResultsPage.this);
        dialog.setContentView(R.layout.deletedialog);

        File[] x = getFilesDir().listFiles();
        String[] fileNames = new String[x.length];

        for (int i = 0; i < x.length; i++)
        {
            String temp = x[i].toString();
            temp = temp.substring(temp.lastIndexOf('/') + 1);
            fileNames[i] = temp;
        }

        final AutoCompleteTextView deleteBox = (AutoCompleteTextView) dialog.findViewById(R.id.deleteBox);
        ArrayAdapter adapter = new ArrayAdapter(ResultsPage.this, android.R.layout.select_dialog_item, fileNames);

        deleteBox.setThreshold(1);
        deleteBox.setAdapter(adapter);

        Button loadButton = (Button) dialog.findViewById(R.id.dialog_delete_button);
        loadButton.setOnClickListener(new View.OnClickListener()
        {
            public void onClick(View v)
            {
                stringToPlotsb(loadFile(deleteBox.getText().toString()));
                File file = new File(getFilesDir() + "/" +deleteBox.getText().toString());
                Log.d("File Deleted?", getFilesDir() + "/" +deleteBox.getText().toString());
                Log.d("File Deleted?", Boolean.toString(file.delete()));

                dialog.dismiss();
            }
        });

        Button cancelButton = (Button) dialog.findViewById(R.id.delete_cancel_button);
        cancelButton.setOnClickListener(new View.OnClickListener()
        {
            @Override
            public void onClick(View v)
            {
                dialog.dismiss();
            }
        });

        dialog.show();
    }

    @SuppressWarnings("StatementWithEmptyBody")
    @Override
    public boolean onNavigationItemSelected(MenuItem item)
    {
        //unchecks home button
        //navigationView.getMenu().getItem(1).getSubMenu().getItem(0).setChecked(false);

        // Handle navigation view item clicks here.
        int id = item.getItemId();

        if (id == R.id.nav_instructions)
        {
            Toast.makeText(getApplicationContext(), "Coming Soon!", Toast.LENGTH_SHORT).show();
            navigationView.getMenu().getItem(1).getSubMenu().getItem(3).setChecked(false);
        }
        else if (id == R.id.nav_history)
        {

        }
        else if (id == R.id.nav_home)
        {
            this.finish();
        }
        else if (id == R.id.bluetooch)
        {
            this.finish();
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


}
