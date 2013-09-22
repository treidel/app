package com.jebussystems.levelingglass.activity;

import java.util.Set;

import android.annotation.TargetApi;
import android.app.Activity;
import android.app.AlertDialog;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Build;
import android.os.Bundle;
import android.os.ParcelUuid;
import android.os.Parcelable;
import android.provider.Settings;
import android.view.View;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.ArrayAdapter;
import android.widget.ListView;

import com.jebussystems.levelingglass.R;
import com.jebussystems.levelingglass.app.LevelingGlassApplication;
import com.jebussystems.levelingglass.control.v1.ControlV1;
import com.jebussystems.levelingglass.util.LogWrapper;

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
		LogWrapper.v(TAG, "PeerSelectionActivity::onCreate enter", "this=",
		        this, "savedInstanceState=", savedInstanceState);

		super.onCreate(savedInstanceState);

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

		// see if we already have a configured device
		BluetoothDevice device = application.getDevice();
		if (null != device)
		{
			LogWrapper.d(TAG, "existing device found=", device);
			// start the connection activity
			handleDeviceSelected(device);
			return;
		}

		LogWrapper.v(TAG, "PeerSelectionActivity::onCreate exit");
	}

	@Override
	protected void onStart()
	{
		LogWrapper
		        .v(TAG, "PeerSelectionActivity::onStart enter", "this=", this);

		super.onStart();

		// load the list of devices
		loadDevices();

		LogWrapper.v(TAG, "PeerSelectionActivity::onStart exit");
	}

	@Override
	protected void onActivityResult(int requestCode, int resultCode, Intent data)
	{
		LogWrapper.v(TAG, "PeerSelectionActivity::onActivityResult enter",
		        "this=", this, "requestCode=", requestCode, "resultCode=",
		        resultCode, "data=" + data);
		// reload the devices
		loadDevices();

		LogWrapper.v(TAG, "PeerSelectionActivity::onActivityResult exit");
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
			LogWrapper.d(TAG, "no peered devices found");

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
		for (BluetoothDevice device : devices)
		{
			this.adapter.add(device);
		}
	}

	private void handleDeviceSelected(BluetoothDevice device)
	{
		LogWrapper.d(TAG, "device selected=", device);
		// store the device
		application.setDevice(device);
		// tell the waiting for connection activity to take over
		Intent intent = new Intent(PeerSelectionActivity.this,
		        MainActivity.class);
		// take over
		startActivity(intent);
	}

	private void notifyDeviceNotCompatible()
	{
		// if we get here the device is not compatible so let them know
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

	}

	// /////////////////////////////////////////////////////////////////////////
	// inner classes
	// /////////////////////////////////////////////////////////////////////////

	private class ItemClickListener implements OnItemClickListener
	{

		@TargetApi(Build.VERSION_CODES.ICE_CREAM_SANDWICH_MR1)
        @Override
		public void onItemClick(AdapterView<?> parent, View view, int position,
		        long id)
		{
			LogWrapper
			        .v(TAG,
			                "PeerSelectionActivity::ItemClickListener::onItemClick enter",
			                "this=", this, "parent=", parent, "view=", view,
			                "position=", position, "id=", id);
			// find the device that was selected
			final BluetoothDevice device = adapter.getItem(position);

			// see if we're able to check that the device is selected
			if (Build.VERSION.SDK_INT < Build.VERSION_CODES.ICE_CREAM_SANDWICH_MR1)
			{
				// we can't query the remote device via SDP to see what it
				// supports so
				// we'll assume the user knows what they are doing
				LogWrapper
				        .d(TAG,
				                "API level 15 not available, assuming compatibility for device=",
				                device);
				handleDeviceSelected(device);
				return;
			}

			// create a dialog that will tell them we're doing to check the
			// device
			AlertDialog.Builder checkingBuilder = new AlertDialog.Builder(
			        PeerSelectionActivity.this);
			checkingBuilder.setMessage(getResources().getString(
			        R.string.peerselection_checking_dialog_message));
			checkingBuilder.setCancelable(true);

			// create the dialog
			final AlertDialog checkingDialog = checkingBuilder.create();

			// unfortunately the "best" place to handle the bluetooth SDP
			// request for the remote's UUIDs is here
			final IntentReceiver receiver = new IntentReceiver(checkingDialog);
			final IntentFilter filter = new IntentFilter();
			filter.addAction(BluetoothDevice.ACTION_UUID);
			registerReceiver(receiver, filter);
			checkingDialog.setOnCancelListener(new CancelCompatibilityListener(
			        receiver));

			// start the SDP query
			if (false == device.fetchUuidsWithSdp())
			{
				notifyDeviceNotCompatible();
			}
			else
			{
				// show the dialog
				checkingDialog.show();
			}

			LogWrapper
			        .v(TAG,
			                "PeerSelectionActivity::ItemClickListener::onItemClick exit");
		}
	}

	@TargetApi(Build.VERSION_CODES.ICE_CREAM_SANDWICH_MR1)
    private class IntentReceiver extends BroadcastReceiver
	{
		private final AlertDialog dialog;

		public IntentReceiver(AlertDialog dialog)
		{
			this.dialog = dialog;
		}

		@Override
		public void onReceive(Context context, Intent intent)
		{
			LogWrapper.v(TAG,
			        "PeerSelectionActivity::IntentReceiver::onReceive enter",
			        "this=", this, "context=", context, "intent=", intent);
			String action = intent.getAction();
			// Evaluate service discovery result
			if (BluetoothDevice.ACTION_UUID.equals(action))
			{
				// deregister ourselves
				unregisterReceiver(this);
				// close the dialog
				dialog.dismiss();
				// extract the data from the intent
				Bundle bundle = intent.getExtras();
				Parcelable[] uuids = bundle
				        .getParcelableArray(BluetoothDevice.EXTRA_UUID);
				BluetoothDevice device = bundle
				        .getParcelable(BluetoothDevice.EXTRA_DEVICE);
				for (int i = 0; i != uuids.length; ++i)
				{
					ParcelUuid uuid = (ParcelUuid) uuids[i];
					if (true == uuid.getUuid().equals(ControlV1.SERVER_UUID))
					{
						// start the connection activity
						handleDeviceSelected(device);
						return;
					}
				}
				// if we get here the device is not compatible so let them know
				notifyDeviceNotCompatible();
			}
			LogWrapper.v(TAG,
			        "PeerSelectionActivity::IntentReceiver::onReceive exit");
		}
	};

	private class CancelCompatibilityListener implements
	        DialogInterface.OnCancelListener
	{
		private final BroadcastReceiver receiver;

		public CancelCompatibilityListener(BroadcastReceiver receiver)
		{
			this.receiver = receiver;
		}

		@Override
		public void onCancel(DialogInterface dialog)
		{
			LogWrapper
			        .v(TAG,
			                "PeerSelectionActivity::CancelCompatibilityListener::onCancel enter",
			                "this=", this, "dialog=", dialog);
			// deregister the receiver
			unregisterReceiver(receiver);
			LogWrapper
			        .v(TAG,
			                "PeerSelectionActivity::CancelCompatibilityListener::onCancel exit");
		}
	}

	private class NotCompatibleOkClickListener implements
	        DialogInterface.OnClickListener
	{

		@Override
		public void onClick(DialogInterface dialog, int which)
		{
			LogWrapper
			        .v(TAG,
			                "PeerSelectionActivity::NotCompatibleOkClickListener::onClick enter",
			                "this=", this, "dialog=", dialog, "which=", which);
			dialog.cancel();
			LogWrapper
			        .v(TAG,
			                "PeerSelectionActivity::NotCompatibleOkClickListener::onClick exit");
		}

	}

	private class NoneFoundOkClickListener implements
	        DialogInterface.OnClickListener
	{

		@Override
		public void onClick(DialogInterface dialog, int which)
		{
			LogWrapper
			        .v(TAG,
			                "PeerSelectionActivity::NoneFoundOkClickListener::onClick enter",
			                "this=", this, "dialog=", dialog, "which=", which);
			// disable the dialog
			dialog.dismiss();
			// pop up the pairing screen
			Intent intent = new Intent(Settings.ACTION_BLUETOOTH_SETTINGS);
			startActivityForResult(intent, REQUEST_PAIR_DEVICE);

			LogWrapper
			        .v(TAG,
			                "PeerSelectionActivity::NoneFoundOkClickListener::onClick exit");
		}

	}
}
