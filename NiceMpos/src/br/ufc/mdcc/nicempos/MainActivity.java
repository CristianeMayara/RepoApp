package br.ufc.mdcc.nicempos;

import java.util.ArrayList;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.app.AlertDialog;
import android.app.Fragment;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.graphics.drawable.Drawable;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Process;
import android.provider.Settings;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;
import br.ufc.mdcc.hellompos.R;
import br.ufc.mdcc.mpos.MposFramework;
import br.ufc.mdcc.mpos.config.Inject;
import br.ufc.mdcc.mpos.config.MposConfig;

@SuppressLint("NewApi")
//@MposConfig(endpointSecondary = "10.99.206.104")
@MposConfig
public class MainActivity extends Activity {
	
	// Mpos
	@Inject(CalculadoraImpl.class)
	private ICalculadora calc;
	
	private boolean quit = false;
	
	// Location
	private String provider;
	boolean isKnownPlace = false;
	// flag for GPS status
    boolean isGPSEnabled = false;
    // flag for network status
    boolean isNetworkEnabled = false;
    // flag for GPS status
    boolean canGetLocation = false;
    // The minimum distance to change Updates in meters
    private static final long MIN_DISTANCE_CHANGE_FOR_UPDATES = 1; // 1 meter
    // The minimum time between updates in milliseconds
    private static final long MIN_TIME_BW_UPDATES = 1000 ; // 1 second
    // The distance radius to identify place in meters
    private static final long DISTANCE_RADIUS_IDENTIFY_PLACE = 80; // 200 meters
    // Declaring a Location Manager
    protected LocationManager locationManager;
 
    Location location; // delete
    double latitude;
    double longitude;
	
	// DB
    int idImage;
	ArrayList<Place> knownPlacesList;
	
	// Functionality
	int currentPlaceId;  // placeId starts in 0
	int currentVoteId;   // from user
	int currentRatingId; // from server

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_main);
		if (savedInstanceState == null) {
			getFragmentManager().beginTransaction().add(R.id.container, new MainFragment()).commit();
		}
		MposFramework.getInstance().start(this);
		
		// Initialize list of places
		knownPlacesList = new ArrayList<Place>();
		idImage = -1;
		initPlaceList();
		
		// Get the location manager
	    locationManager = (LocationManager) getSystemService(Context.LOCATION_SERVICE);
	    provider = LocationManager.GPS_PROVIDER;
	    
		// Verify availability of localization providers 
		verifyLocalizationProviders();
		
	    locationManager.requestLocationUpdates(LocationManager.GPS_PROVIDER,
	    		MIN_TIME_BW_UPDATES, MIN_DISTANCE_CHANGE_FOR_UPDATES, new LocationListener() {
			@Override
			public void onStatusChanged(String provider, int status, Bundle extras) {
				Log.v("location-method", "onStatusChanged");
			}
			
			@Override
			public void onProviderEnabled(String provider) {
				Log.v("location-method", "onProviderEnabled");
				Toast.makeText(getApplicationContext(), "Enabled new provider " + provider, Toast.LENGTH_SHORT).show();
			}
			
			@Override
			public void onProviderDisabled(String provider) {
				Log.v("location-method", "onProviderDisabled");
				Toast.makeText(getApplicationContext(), "Disabled provider " + provider, Toast.LENGTH_SHORT).show();
			}
			
			@Override
			public void onLocationChanged(Location location) {
				Log.v("location-method", "onLocationChanged");
				TextView tv = (TextView)findViewById(R.id.suggestion_name_tv);
				if (location != null)
					tv.setText(""+(double) location.getLatitude()+" "+(double) location.getLongitude());
				else
					tv.setText("location is null");
				
				// atualizar latitude e longitude
				if (location != null){
					latitude = location.getLatitude();
					longitude = location.getLongitude();
					
					// verificar se eh de um canto conhecido
					int placeId = isKnownPlace (location);
					showPlaceInformations(placeId);
					if (isKnownPlace) isKnownPlace = false;
				}
			}
		});
	    
	    Location location = locationManager.getLastKnownLocation(LocationManager.GPS_PROVIDER);
	    
	}
	
	@Override
	protected void onDestroy() {
		super.onDestroy();
		if (quit) {
			MposFramework.getInstance().stop();
			Process.killProcess(Process.myPid());
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
		if (id == R.id.action_settings) {
			return true;
		}
		return super.onOptionsItemSelected(item);
	}
	
	@Override
	protected void onResume() {
		super.onResume();
		Log.v("location-method", "Entrou onResume");
		// Request updates at startup
		//locationManager.requestLocationUpdates(provider, 400, 1, this);
	}
	
	@Override
	protected void onPause() {
		super.onPause();
		Log.v("location-method", "Entrou onPause");
		// Remove the locationlistener updates when Activity is paused
		//locationManager.removeUpdates(this);
	}
	
	/**
	 * A placeholder fragment (MainFragment) containing a simple view.
	 */
	public final class MainFragment extends Fragment {

		@Override
		public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
			View rootView = inflater.inflate(R.layout.fragment_main, container, false);
			initButtons(rootView);
			return rootView;
		}

		private void initButtons(View root) {
			Button answer1Button = (Button) root.findViewById(R.id.button_sum);
			answer1Button.setOnClickListener(new OnClickListener() {
				@Override
				public void onClick(View v) {
					currentVoteId = 1;
					executeCommand("1");
				}
			});

			Button answer2Button = (Button) root.findViewById(R.id.button_sub);
			answer2Button.setOnClickListener(new OnClickListener() {
				@Override
				public void onClick(View v) {
					currentVoteId = 2;
					executeCommand("2");
				}
			});

			Button answer3Button = (Button) root.findViewById(R.id.button_mul);
			answer3Button.setOnClickListener(new OnClickListener() {
				@Override
				public void onClick(View v) {
					currentVoteId = 3;
					executeCommand("3");
				}
			});

			Button answer4Button = (Button) root.findViewById(R.id.button_div);
			answer4Button.setOnClickListener(new OnClickListener() {
				@Override
				public void onClick(View v) {
					currentVoteId = 4;
					executeCommand("4");
				}
			});
			
			Button answer5Button = (Button) root.findViewById(R.id.button_answer5);
			answer5Button.setOnClickListener(new OnClickListener() {
				@Override
				public void onClick(View v) {
					currentVoteId = 5;
					executeCommand("5");
				}
			});
		}

		private void executeCommand(String function) {
			View root = getView();
			//EditText number01 = (EditText) root.findViewById(R.id.number1_et);
			//EditText number02 = (EditText) root.findViewById(R.id.number2_et);

			Command command = new Command();
			command.input = new Input(currentPlaceId,currentVoteId); // dados obtidos da entrada do usuario
			command.function = function;

			new CalculatorTask(root).execute(command);
		}
	}

	// helper object to transfer data from UI to backend process
	private final class Command {
		private Input input;
		private String function;
	}
	
	// helper object to store locations
	private final class Place {
		private String name;
		private double latitude;
		private double longitude;
		private int imageId;
		
		public Place (String name, double lat, double lon, int id){
			this.name = name;
			this.latitude = lat;
			this.longitude = lon;
			this.imageId = id;
		}
	}

	// your process is recommend executed in parallel the thread UI, to avoid
	// exceptions
	final class CalculatorTask extends AsyncTask<Command, Void, Object> {

		private View root;

		public CalculatorTask(View root) {
			this.root = root;
		}

		@Override
		protected Object doInBackground(Command... params) {
			Command command = params[0];

			switch (command.function) {
			case "1":
				return calc.chosenAnswerOne(command.input);

			case "2":
				return calc.chosenAnswerTwo(command.input);

			case "3":
				return calc.chosenAnswerThree(command.input);

			case "4":
				return calc.chosenAnswerFour(command.input);
			
			case "5":
				return calc.chosenAnswerFive(command.input);
			}

			return null;
		}

		@Override
		protected void onPostExecute(Object result) {
		    TextView nameTv = (TextView)findViewById(R.id.place_name_tv);
		    
		    Bundle bundle = new Bundle();
		    
		    bundle.putString("placeNameKey", nameTv.getText().toString());
            bundle.putInt("ratingIdKey", (int)result);
            bundle.putInt("nVotesPlaceKey", 10);
            
            Intent resultActivity = new Intent(getApplicationContext(), ResultActivity.class);
            resultActivity.putExtras(bundle);
            startActivity(resultActivity);
		}
	}
	
	/**
     * Function to check GPS/wifi enabled
     * @return boolean
     * */
    public boolean canGetLocation() {
        return this.canGetLocation;
    }
    
    public void verifyLocalizationProviders () {
    	// Get GPS status
        isGPSEnabled = locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER);
        Log.i("flag", "isGPSEnabled: " + isGPSEnabled);

        // Get network status
        isNetworkEnabled = locationManager.isProviderEnabled(LocationManager.NETWORK_PROVIDER);
        Log.i("flag", "isNetworkEnabled: " + isNetworkEnabled);
        
        if (!isGPSEnabled && !isNetworkEnabled) {
            // no network provider is enabled
        	Log.i("flag", "no network provider is enabled");
        	showSettingsAlert();
        } else {
        	Log.i("flag", "network provider is enabled");
            this.canGetLocation = true;
        }
    }
    
    public void getLatitude () {
    	if (canGetLocation()){
    		
    	}
    }
    
    public void getLongitude () {
    	if (canGetLocation()){
    		
    	}
    }
    
	public void initPlaceList () {
		addPlace("Lagoa Campus do Pici",-3.742994,-38.574781);
		addPlace("Restaurante Universit�rio UFC",-3.744741,-38.572775);
		addPlace("Departamento da Computa��o UFC",-3.745846,-38.574147);
		addPlace("Biblioteca da F�sica UFC",-3.746942,-38.575325);
		addPlace("Pra�a das Mangueiras UFC",-3.746001,-38.574882);
		addPlace("GREat UFC",-3.746536,-38.578153);
		addPlace("Est�tua Praia de Iracema",-3.720572,-38.509571);
		addPlace("Theatro Jos� de Alencar",-3.727711,-38.53169);
		addPlace("North Shopping",-3.735455,-38.565949);
		addPlace("Jericoacoara",-2.7956,-40.5142);
		addPlace("Cumbuco",-3.618444,-38.748628);
		addPlace("Drag�o do Mar",-3.721306,-38.520198);
	}
	
	public void addPlace (String name, double lat, double lon) {
		knownPlacesList.add(new Place(name, lat, lon, idImage++));
	}
	
	public int isKnownPlace (Location currentLocation) {
		for(int i = 0; i < knownPlacesList.size(); i++){
			if (isNearThePlace(currentLocation, i)){
				isKnownPlace = true;
				currentPlaceId = i;
				return i;
			}
		}
		return -1;
	}
	
	public boolean isNearThePlace (Location currentLocation, int placeId) {
		Location l = new Location(provider);
		l.setLatitude(knownPlacesList.get(placeId).latitude);
		l.setLongitude(knownPlacesList.get(placeId).longitude);
		
		Log.i("distance", "" + l.distanceTo(currentLocation) + "metros");
		if (l.distanceTo(currentLocation) <= DISTANCE_RADIUS_IDENTIFY_PLACE)
			return true;
		else
			return false;
	}
	
	// Function to show settings alert dialog
	public void showSettingsAlert() {
        AlertDialog.Builder alertDialog = new AlertDialog.Builder(this);
        
        alertDialog.setTitle("Configura��es de GPS");
        alertDialog.setMessage("GPS n�o est� ativado. Voc� gostaria de ir para o menu de configura��es?");
        
        alertDialog.setPositiveButton("Sim", new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialog,int which) {
                Intent intent = new Intent(Settings.ACTION_LOCATION_SOURCE_SETTINGS);
                startActivity(intent);
            }
        });
        
        alertDialog.setNegativeButton("N�o", new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialog, int which) {
            	dialog.cancel();
            }
        });
        
        alertDialog.show();
    }
	
	public void showPlaceInformations (int placeId) {
		TextView nameTv = (TextView)findViewById(R.id.place_name_tv);
		ImageView imageIv = (ImageView)findViewById(R.id.place_image_iv);
		
		@SuppressWarnings("deprecation")
		Drawable drawable = getResources().getDrawable(getImageIdInR(placeId));
		
		if (placeId == -1)
			nameTv.setText(R.string.unavailable_place);
		else
			nameTv.setText(knownPlacesList.get(placeId).name);
		imageIv.setImageDrawable(drawable);
	}
	
	// O id da imagem deve ser a posicao do local na lista
	public int getImageIdInR (int id) {	
		switch (id){
			case -1: return R.drawable.place_nophoto;
			case 0: return R.drawable.place_campuspici;
			case 1: return R.drawable.place_ru;
			case 2: return R.drawable.place_dc;
			case 3: return R.drawable.place_bibliofisica;
			case 4: return R.drawable.place_mangueiras;
			case 5: return R.drawable.place_great;
			case 6: return R.drawable.place_iracema;
			case 7: return R.drawable.place_teatrojosealencar;
			case 8: return R.drawable.place_northshopping;
			case 9: return R.drawable.place_jericoacoara;
			case 10: return R.drawable.place_cumbuco;
			case 11: return R.drawable.place_dragaodomar;
		}
		return 0;
		
	}
	
}