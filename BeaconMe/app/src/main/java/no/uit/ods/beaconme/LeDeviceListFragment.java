package no.uit.ods.beaconme;

import android.app.Activity;
import android.bluetooth.BluetoothDevice;
import android.content.Context;
import android.os.Bundle;
import android.app.Fragment;
import android.os.IBinder;
import android.util.Log;
import android.view.ContextMenu;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AbsListView;
import android.widget.AdapterView;
import android.widget.BaseAdapter;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;


/**
 * A fragment representing a list of Items.
 * <p/>
 * Large screen devices (such as tablets) are supported by replacing the ListView
 * with a GridView.
 * <p/>
 * Activities containing this fragment MUST implement the {@link OnFragmentInteractionListener}
 * interface.
 */
public class LeDeviceListFragment extends Fragment implements AbsListView.OnItemClickListener {
//    private OnFragmentInteractionListener mListener;
    private LeScannerService mService;
//    private View mView;
    private LeBeacon lastBeaconClicked;

    /**
     * The fragment's ListView/GridView.
     */
    private AbsListView mListView;

    /**
     * The Adapter which will be used to populate the ListView/GridView with
     * Views.
     */
//    private LeDeviceListAdapter leDeviceListAdapter;

    /**
     * Mandatory empty constructor for the fragment manager to instantiate the
     * fragment (e.g. upon screen orientation changes).
     */
    public LeDeviceListFragment() {
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        Log.i("FRAGMENT", "onCreate");
        super.onCreate(savedInstanceState);

//        mService = ( (LeScannerService.LocalBinder) savedInstanceState.getBinder("binder")).getService();
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        Log.i("FRAGMENT", "onCreateView");

        View view = inflater.inflate(R.layout.fragment_ledevicelist_list, container, false);

        // Set OnItemClickListener so we can be notified on item clicks
        mListView = (AbsListView) view.findViewById(android.R.id.list);
        mListView.setOnItemClickListener(this);

        return view;
    }

    @Override
    public void onAttach(Activity activity) {
        Log.i("FRAGMENT", "onAttach()");
        super.onAttach(activity);

//        if (mService == null) {
//            Bundle bundle = getArguments();
//            if (bundle == null) {
//                Log.i("DOHDOHDOH", "bundle == null.....");
//                bundle = activity.getIntent().getExtras();
//                if (bundle == null)
//                    Log.i("DOHDOHDOH", "bundle == null.....again...");
//
//            }
//            IBinder iBinder = bundle.getBinder("binder");
//            if (iBinder == null)
//                Log.i("DOHDOHDOH", "binder == null.....");
//            LeScannerService.LocalBinder binder = (LeScannerService.LocalBinder) iBinder;
//            mService = binder.getService();
//        }
//        ArrayListBeacon list = mService.getList();
//        leDeviceListAdapter = new LeDeviceListAdapter();
//        leDeviceListAdapter.setList(list);

    }

    @Override
    public void onStop () {
        super.onStop();
        Log.i("FRAGMENT", "onStop()");
    }

    @Override
    public void onDetach() {
        super.onDetach();
        Log.i("FRAGMENT", "onDetatch()");
//        mListener = null;
    }

//    public void onPause () {
//        super.onPause();
//        Log.i("FRAGMENT", "onPause()");
//    }

    @Override
    public void onCreateContextMenu(ContextMenu menu, View v, ContextMenu.ContextMenuInfo menuInfo) {
        super.onCreateContextMenu(menu, v, menuInfo);
        MenuInflater inflater = getActivity().getMenuInflater();
        inflater.inflate(R.menu.menu_context_device_list, menu);

    }

    @Override
    public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
        Log.i("List Fragment", "onItemClick() " + String.valueOf(position));

//        Toast.makeText(parent.getContext(), leDeviceListAdapter.getItem(position).getBtDevice().getName() + " - " + leDeviceListAdapter.getItem(position).getBtDevice().getAddress(), Toast.LENGTH_SHORT).show();
        lastBeaconClicked = mService.getList().get(position);
        Log.i("CLICKED BEACON: ", lastBeaconClicked.getId());
//        ListView lv = (ListView) view.findViewById(R.id.listMode);
//        registerForContextMenu(lv);

        registerForContextMenu(view);
//        if (null != mListener) {
//            // Notify the active callbacks interface (the activity, if the
//            // fragment is attached to one) that an item has been selected.
//            mListener.onFragmentInteraction(DummyContent.ITEMS.get(position).id);
//        }
    }

    public boolean onContextItemSelected(MenuItem item) {

        switch (item.getItemId()) {
            case R.id.menu_context_device_list_add:

                break;
        }
        return true;
    }

    /**
     * This interface must be implemented by activities that contain this
     * fragment to allow an interaction in this fragment to be communicated
     * to the activity and potentially other fragments contained in that
     * activity.
     * <p/>
     * See the Android Training lesson <a href=
     * "http://developer.android.com/training/basics/fragments/communicating.html"
     * >Communicating with Other Fragments</a> for more information.
     */
    public interface OnFragmentInteractionListener {
        // TODO: Update argument type and name
        public void onFragmentInteraction(String id);
    }

}
