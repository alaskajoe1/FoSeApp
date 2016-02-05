package jeenaa.fose;

import android.content.Intent;
import android.graphics.Color;
import android.os.Bundle;
import android.support.design.widget.NavigationView;
import android.support.v4.view.GravityCompat;
import android.support.v4.widget.DrawerLayout;
import android.support.v7.app.ActionBarDrawerToggle;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Spinner;
import android.widget.SpinnerAdapter;
import android.widget.TextView;
import android.widget.Toast;

import com.androidplot.xy.BoundaryMode;
import com.androidplot.xy.LineAndPointFormatter;
import com.androidplot.xy.SimpleXYSeries;
import com.androidplot.xy.XYPlot;

public class MainActivity  extends BlunoLibrary
        implements NavigationView.OnNavigationItemSelectedListener
{
	private Button buttonScan;
	private Button buttonSerialSend;
	private EditText serialSendText;
	private TextView serialReceivedText;
    public static final String MY_TAG = "outputLog";
    private int zeroValue = -1;
    private long startTime = -1;
    private XYPlot forcePlot;
    private SimpleXYSeries forceHistory = null;
    int HISTORY_SIZE = 100;

    private Toolbar toolbar=null;
    private String[] category=null;
	

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_main);
        onCreateProcess();														//onCreate Process by BlunoLibrary

        forcePlot = (XYPlot) findViewById(R.id.forcePlot);                      //finds force plot
        forceHistory = new SimpleXYSeries("Force");                             //creates new XY series
        //forceHistory.useImplicitXVals();                                        //use impicit x vals for now
        forcePlot.setRangeBoundaries(-5, 40, BoundaryMode.FIXED);               //Sets y axis boundaries
        forcePlot.setDomainBoundaries(0, 20, BoundaryMode.FIXED);               //Sets x axis boundaries
        forcePlot.addSeries(forceHistory, new LineAndPointFormatter(Color.BLUE, Color.BLUE, null, null)); //adds series to plot
        forcePlot.setDomainStepValue(5);
        forcePlot.setTicksPerRangeLabel(3);
        forcePlot.setDomainLabel("Time (s)");
        forcePlot.getDomainLabelWidget().pack();
        forcePlot.setRangeLabel("Force (lbs)");
        forcePlot.getRangeLabelWidget().pack();


        serialBegin(115200);													//set the Uart Baudrate on BLE chip to 115200

        serialReceivedText=(TextView) findViewById(R.id.serialReveicedText);	//initial the EditText of the received data
        serialReceivedText.setTextSize(100);
        serialSendText=(EditText) findViewById(R.id.serialSendText);			//initial the EditText of the sending data

        buttonSerialSend = (Button) findViewById(R.id.buttonSerialSend);		//initial the button for sending the data
        buttonSerialSend.setOnClickListener(new OnClickListener() {

			@Override
			public void onClick(View v) {


				serialSend(serialSendText.getText().toString());				//send the data to the BLUNO
			}
		});

        buttonScan = (Button) findViewById(R.id.buttonScan);					//initial the button for scanning the BLE device
        buttonScan.setOnClickListener(new OnClickListener() {

			@Override
			public void onClick(View v) {

                //TODO: need to add permission code here

				buttonScanOnClickProcess();										//Alert Dialog for selecting the BLE device
			}
		});

        toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        //sets up nav drawer
        DrawerLayout drawer = (DrawerLayout) findViewById(R.id.drawer_layout);
        ActionBarDrawerToggle toggle = new ActionBarDrawerToggle(
                this, drawer, toolbar, R.string.navigation_drawer_open, R.string.navigation_drawer_close);
        drawer.setDrawerListener(toggle);
        toggle.syncState();

        NavigationView navigationView = (NavigationView) findViewById(R.id.nav_view);
        navigationView.setNavigationItemSelectedListener(this);

        getSupportActionBar().setDisplayShowTitleEnabled(false);
        //toolbar.setLogo(R.drawable.ic_drawer);
        toolbar.setBackgroundColor(Color.BLUE);


        category = getResources().getStringArray(R.array.category);

        SpinnerAdapter spinnerAdapter = ArrayAdapter.createFromResource(getApplicationContext(), R.array.category, R.layout.spinner_dropdown_item);
        Spinner navigationSpinner = new Spinner(getSupportActionBar().getThemedContext());
        navigationSpinner.setAdapter(spinnerAdapter);
        toolbar.addView(navigationSpinner, 0);

        navigationSpinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                Toast.makeText(MainActivity.this,
                        "you selected: " + category[position],
                        Toast.LENGTH_SHORT).show();
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {

            }
        });
	}

	protected void onResume(){
		super.onResume();
		System.out.println("BlUNOActivity onResume");
		onResumeProcess();														//onResume Process by BlunoLibrary
	}
	
	
	
	@Override
	protected void onActivityResult(int requestCode, int resultCode, Intent data) {
		onActivityResultProcess(requestCode, resultCode, data);					//onActivityResult Process by BlunoLibrary
		super.onActivityResult(requestCode, resultCode, data);
	}
	
    @Override
    protected void onPause() {
        super.onPause();
        onPauseProcess();														//onPause Process by BlunoLibrary
    }
	
	protected void onStop() {
		super.onStop();
		onStopProcess();														//onStop Process by BlunoLibrary
	}
    
	@Override
    protected void onDestroy() {
        super.onDestroy();	
        onDestroyProcess();														//onDestroy Process by BlunoLibrary
    }

	@Override
	public void onConectionStateChange(connectionStateEnum theConnectionState) {//Once connection state changes, this function will be called
		switch (theConnectionState) {											//Four connection state
		case isConnected:
			buttonScan.setText("Connected");
			break;
		case isConnecting:
			buttonScan.setText("Connecting");
			break;
		case isToScan:
			buttonScan.setText("Scan");
			break;
		case isScanning:
			buttonScan.setText("Scanning");
			break;
		case isDisconnecting:
			buttonScan.setText("isDisconnecting");
			break;
		default:
			break;
		}
	}

	@Override
	public void onSerialReceived(String theString)
    {							//Once connection data received, this function will be called

		//serialReceivedText.append(theString);							//append the text into the EditText
        //serialReceivedText.append("Hello");

        int theVal = -1;
        double curForce = -1;
        double curTime = -1;

        if(startTime == -1)
            startTime = System.currentTimeMillis();

        if(theString.length() > 7)
        {
            theString = theString.substring(0, 7);
            theVal = Integer.valueOf(theString);

            if(zeroValue == -1)
                zeroValue = theVal;

            theVal = theVal - zeroValue;
            curForce = theVal/-30000.0;
            curTime = (System.currentTimeMillis() - startTime)/1000.0;
        }
        else
            serialReceivedText.setText("ERROR");


                       //Sets x axis boundaries

        serialReceivedText.setText(String.format("%.1f", curForce));

        if(forceHistory.size() > HISTORY_SIZE)
        {
            forceHistory.removeFirst();
        }

        forceHistory.addLast(curTime, curForce);

        if(curTime > 18)
            forcePlot.setDomainBoundaries(curTime-18, curTime+2, BoundaryMode.FIXED);

        forcePlot.redraw();




		//The Serial data from the BLUNO may be sub-packaged, so using a buffer to hold the String is a good choice.
        Log.i(MY_TAG, String.format("%.1f", curForce));
					
	}


    @Override
    public void onBackPressed() {
        DrawerLayout drawer = (DrawerLayout) findViewById(R.id.drawer_layout);
        if (drawer.isDrawerOpen(GravityCompat.START)) {
            drawer.closeDrawer(GravityCompat.START);
        } else {
            super.onBackPressed();
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.main, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();

        //noinspection SimplifiableIfStatement
        if (id == R.id.action_settings) {
            return true;
        }

        return super.onOptionsItemSelected(item);
    }

    @SuppressWarnings("StatementWithEmptyBody")
    @Override
    public boolean onNavigationItemSelected(MenuItem item) {
        // Handle navigation view item clicks here.
        int id = item.getItemId();

        if (id == R.id.nav_instructions) {
            // Handle the camera action
        } else if (id == R.id.nav_history) {

        } else if (id == R.id.nav_settings) {

        } else if (id == R.id.nav_share) {

        } else if (id == R.id.nav_feedback) {

        }

        DrawerLayout drawer = (DrawerLayout) findViewById(R.id.drawer_layout);
        drawer.closeDrawer(GravityCompat.START);
        return true;
    }

}