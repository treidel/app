package com.jebussystems.levelingglass.activity;

import java.util.Set;

import android.app.Activity;
import android.app.AlertDialog;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.Bundle;
import android.provider.Settings;
import android.util.Log;
import android.view.View;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.ArrayAdapter;
import android.widget.ListView;

import com.jebussystems.levelingglass.R;
import com.jebussystems.levelingglass.app.LevelingGlassApplication;
import com.jebussystems.levelingglass.bluetooth.spp.SPPManager;

public class PeerSelectionActivity extends Activity
{
	// /////////////////////////////////////////////////////////////////////////
	// constants
	// /////////////////////////////////////////////////////////////////////////

	private static final String TAG = "activity.peerselection";

	private static final int REQUEST_PAIR_DEVICE = 1;

	// /////////////////////////////////////////////////////////////////////////
	// class variables
	// /////////////////////////////////////////////////////////////////////////

	// /////////////////////////////////////////////////////////////////////////
	// object variables
	// /////////////////////////////////////////////////////////////////////////

	private LevelingGlassApplication application;
	private ArrayAdapter<BluetoothDevice> adapter;

	// /////////////////////////////////////////////////////////////////////////
	// constructors
	// /////////////////////////////////////////////////////////////////////////

	// /////////////////////////////////////////////////////////////////////////
	// public methods
	// /////////////////////////////////////////////////////////////////////////

	// /////////////////////////////////////////////////////////////////////////
	// protected methods
	// /////////////////////////////////////////////////////////////////////////

	@Override
	protected void onCreate(Bundle savedInstanceState)
	{
		super.onCreate(savedInstanceState);

		Log.d(TAG, "onCreate");

		// fetch the application
		this.application = (LevelingGlassApplication) getApplication();
		
		// setup the layout
		setContentView(R.layout.peerselection);

		// create the adapter
		this.adapter = new ArrayAdapter<BluetoothDevice>(this,
				android.R.layout.simple_list_item_1);

		// find the listview
		ListView listView = (ListView) findViewById(R.id.peers_listview);

		// link the adapter to the listview
		listView.setAdapter(this.adapter);

		// setup the listener
		listView.setOnItemClickListener(new ItemClickListener());

	}

	@Override
	protected void onStart()
	{
		super.onStart();

		Log.d(TAG, "onStart");

		// see if we already have a configured device
		BluetoothDevice device = application.getDevice();
		if (null != device)
		{
			// start the connection activity
			startWaitForConnectionActivity(device);
			return;
		}
		
		// load the list of devices
		loadDevices();
	}

	@Override
	protected void onActivityResult(int requestCode, int resultCode, Intent data)
	{
		// reload the devices
		loadDevices();
	}

	// /////////////////////////////////////////////////////////////////////////
	// private methods
	// /////////////////////////////////////////////////////////////////////////

	private void loadDevices()
	{
		// clear the item in the listview
		this.adapter.clear();

		// query the list of peered devices
		Set<BluetoothDevice> devices = BluetoothAdapter.getDefaultAdapter()
		        .getBondedDevices();

		// if there aren't any peered devices complain
		if (0 == devices.size())
		{
			Log.d(TAG, "no devices found");

			// no devices found so let them know
			AlertDialog.Builder builder = new AlertDialog.Builder(
			        PeerSelectionActivity.this);
			builder.setMessage(getResources().getString(
			        R.string.peerselection_nonefound_dialog_message));
			builder.setPositiveButton(android.R.string.ok,
			        new NoneFoundOkClickListener());

			// create + run the dialog
			final AlertDialog dialog = builder.create();
			dialog.show();

			// done
			return;
		}

		// add all of the devices
		this.adapter.addAll(devices);
	}

	private void startWaitForConnectionActivity(BluetoothDevice device)
	{
		// tell the waiting for connection activity to take over
		Intent intent = new Intent(PeerSelectionActivity.this,
		        WaitingForConnectionActivity.class);
		intent.putExtra(WaitingForConnectionActivity.BLUETOOTHDEVICE_NAME,
		        device);
		// take over
		startActivity(intent);
	}

	// /////////////////////////////////////////////////////////////////////////
	// inner classes
	// /////////////////////////////////////////////////////////////////////////

	private class ItemClickListener implements OnItemClickListener
	{

		@Override
		public void onItemClick(AdapterView<?> parent, View view, int position,
		        long id)
		{
			// find the device that was selected
			final BluetoothDevice device = adapter.getItem(position);

			// create a dialog that will tell them we're doing to check the
			// device
			AlertDialog.Builder checkingBuilder = new AlertDialog.Builder(
			        PeerSelectionActivity.this);
			checkingBuilder.setMessage(getResources().getString(
			        R.string.peerselection_checking_dialog_message));
			checkingBuilder.setCancelable(true);

			// create + run the dialog
			final AlertDialog checkingDialog = checkingBuilder.create();
			checkingDialog.show();

			// check the device
			boolean compatible = SPPManager.checkDeviceForCompatibility(device);

			// close the dialog
			checkingDialog.cancel();

			// do stuff if the peer is not compatible
			if (false == compatible)
			{
				// device is not compatible so let them know
				AlertDialog.Builder invalidBuilder = new AlertDialog.Builder(
				        PeerSelectionActivity.this);
				invalidBuilder.setMessage(getResources().getString(
				        R.string.peerselection_invalid_dialog_message));
				invalidBuilder.setPositiveButton(android.R.string.ok,
				        new NotCompatibleOkClickListener());
				invalidBuilder.setCancelable(true);
				// create + run the dialog
				final AlertDialog invalidDialog = invalidBuilder.create();
				invalidDialog.show();
				return;

			}
			// start the connection activity
			startWaitForConnectionActivity(device);
		}

	}

	private class NotCompatibleOkClickListener implements
	        DialogInterface.OnClickListener
	{

		@Override
		public void onClick(DialogInterface dialog, int which)
		{
			Log.d(TAG, "ok clicked");
			dialog.cancel();
		}

	}

	private class NoneFoundOkClickListener implements
	        DialogInterface.OnClickListener
	{

		@Override
		public void onClick(DialogInterface dialog, int which)
		{
			Log.d(TAG, "ok clicked");
			// disable the dialog
			dialog.cancel();
			// pop up the pairing screen
			Intent intent = new Intent(Settings.ACTION_BLUETOOTH_SETTINGS);
			startActivityForResult(intent, REQUEST_PAIR_DEVICE);
		}

	}
}
