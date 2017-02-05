package net.squanchy.ui.fragment;

import android.os.AsyncTask;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import net.squanchy.R;
import net.squanchy.model.Model;
import net.squanchy.model.UpdatesManager;
import net.squanchy.model.data.Location;
import net.squanchy.model.managers.LocationManager;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.UiSettings;
import com.google.android.gms.maps.model.CameraPosition;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.MarkerOptions;

import java.util.List;

public class LocationFragment extends Fragment implements CustomMapFragment.OnActivityCreatedListener {

    private static final int ZOOM_LEVEL = 15;
    private static final int TILT_LEVEL = 0;
    private static final int BEARING_LEVEL = 0;

    public static final String TAG = "LocationsFragment";
    private GoogleMap mGoogleMap;

    private UpdatesManager.DataUpdatedListener updateListener = requestIds -> replaceMapFragment();

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setHasOptionsMenu(true);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fr_location, container, false);
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        Model.getInstance().getUpdatesManager().unregisterUpdateListener(updateListener);
    }

    @Override
    public void onViewCreated(View view, Bundle savedInstanceState) {
        Model.getInstance().getUpdatesManager().registerUpdateListener(updateListener);
        replaceMapFragment();
    }

    @Override
    public void onActivityCreated(GoogleMap googleMap) {
        mGoogleMap = googleMap;
        new LoadLocations().execute();
    }

    private class LoadLocations extends AsyncTask<Void, Void, List<Location>> {

        @Override
        protected List<Location> doInBackground(Void... params) {
            LocationManager locationManager = Model.getInstance().getLocationManager();
            return locationManager.getLocations();
        }

        @Override
        protected void onPostExecute(List<Location> locations) {
            hideProgressBar();
            fillMapViews(locations);
        }
    }

    private void fillMapViews(List<Location> locations) {
        if (mGoogleMap == null) {
            return;
        }

        View view = getView();
        if (view != null && locations == null || locations.isEmpty()) {
            TextView textViewAddress = (TextView) view.findViewById(R.id.txtAddress);
            textViewAddress.setText(getString(R.string.placeholder_location));
        }

        if (locations != null) {
            for (int i = 0; i < locations.size(); i++) {
                Location location = locations.get(i);
                LatLng position = new LatLng(location.getLat(), location.getLon());
                mGoogleMap.addMarker(new MarkerOptions().position(position));

                if (i == 0) {
                    CameraPosition camPos = new CameraPosition(position, ZOOM_LEVEL, TILT_LEVEL, BEARING_LEVEL);
                    mGoogleMap.moveCamera(CameraUpdateFactory.newCameraPosition(camPos));
                    fillTextViews(location);
                }
            }
        }

        UiSettings uiSettings = mGoogleMap.getUiSettings();
        uiSettings.setMapToolbarEnabled(false);
        uiSettings.setMyLocationButtonEnabled(false);
        uiSettings.setCompassEnabled(false);
        uiSettings.setRotateGesturesEnabled(false);
    }

    private void fillTextViews(Location location) {
        if (getView() == null) {
            return;
        }

        TextView txtAmsterdam = (TextView) getView().findViewById(R.id.txtPlace);
        TextView txtAddress = (TextView) getView().findViewById(R.id.txtAddress);

        txtAmsterdam.setText(location.getName());
        txtAddress.setText(location.getAddress());
    }

    private void replaceMapFragment() {
        CustomMapFragment mapFragment = CustomMapFragment.newInstance(LocationFragment.this);
        LocationFragment
                .this.getFragmentManager()
                .beginTransaction()
                .replace(R.id.fragmentHolder, mapFragment)
                .commitAllowingStateLoss();
    }

    private void hideProgressBar() {
        if (getView() != null) {
            getView().findViewById(R.id.progressBar).setVisibility(View.GONE);
        }
    }
}