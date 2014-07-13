/*
	Copyright (C) 2013, The Aikuma Project
	AUTHORS: Oliver Adams and Florian Hanke
*/
package org.lp20.aikuma.util;

import android.app.Activity;
import android.content.Context;
import android.util.Log;
import android.widget.TextView;
import android.widget.Toast;
import java.io.File;
import java.io.IOException;
import java.util.concurrent.TimeUnit;
import org.lp20.aikuma.model.ServerCredentials;
//import org.lp20.sync.FTPSyncUtil;
import org.lp20.aikuma.R;

/**
 * Periodically syncs with to a server specified in a ServerCredentials object
 * using an FTP Client.
 *
 * @author	Oliver Adams	<oliver.adams@gmail.com>
 * @author	Florian Hanke	<florian.hanke@gmail.com>
 */
public class SyncUtil {

	/**
	 * Private constructor to ensure the class cannot be instantiated.
	 */
	private SyncUtil() {}

	/**
	 * Starts the thread that attempts a sync every minute.
	 */
	public static void startSyncLoop() {
		if (syncThread == null || !syncThread.isAlive()) {
			syncThread = new SyncLoop(false);
			syncThread.start();
		}
	}

	/**
	 * Forces a sync to occur now.
	 */
	public static void syncNow() {
		//When we interrupt the syncThread, we fire up a new syncThread which
		//syncs immediately.
		syncThread.interrupt();
	}

	/**
	 * A thread that loops and attempts to sync every minute.
	 */
	private static class SyncLoop extends Thread {
		/**
		 * Constructor for the SyncLoop.
		 *
		 * @param	forceSync	if true, ensures a sync will start immediately.
		 */
		public SyncLoop(boolean forceSync) {
			this.forceSync = forceSync;
		}
		/**
		 * Runs the loop that periodically tries to sync. 
		 */
		@Override
		public void run() {
			int waitMins = 1;
			boolean syncResult;
			while (true) {
				try {
					SyncUtil.serverCredentials = ServerCredentials.read();
					//For some reason we get an EPIPE unless we instantiate a new
					//Client at each iteration.
					if (forceSync || serverCredentials.getSyncActivated()) {
						setSyncFlag();
						forceSync = false;
						Client client = new Client();
						client.setClientBaseDir(
								FileIO.getAppRootPath().toString());
						Log.i(TAG, "beginning sync run");
						if (!client.login(serverCredentials.getIPAddress(),
								serverCredentials.getUsername(),
								serverCredentials.getPassword())) {
							unsetSyncFlag("Login failed");
							Log.i(TAG, "login failed: " +
									serverCredentials.getIPAddress());
						} else if (!client.sync()) {
							Log.i(TAG, "sync failed.");
							unsetSyncFlag("Transfer failed");
						} else if (!client.logout()) {
							Log.i(TAG, "Logout failed.");
							unsetSyncFlag("Logout failed");
						} else {
							Log.i(TAG, "sync complete.");
							unsetSyncFlag("Sync successful");
						}
						Log.i(TAG, "end of conditional block");
						waitMins = 1;
						Log.i(TAG, "sync complete");
					} else {
						Log.i(TAG, "not syncing");
					}
				} catch (IOException e) {
					Log.i("npe", "ioexception on serverCredentials.read()");
					unsetSyncFlag("IOException on serverCredentials.read()");
					//We'll cope and assume the old serverCredentials work.
				}
				try {
					TimeUnit.MINUTES.sleep(waitMins);
				} catch (InterruptedException e) {
					SyncUtil.syncThread = new SyncLoop(true);
					SyncUtil.syncThread.start();
					return;
				}
			}
		}
		private boolean forceSync;
	}

	/**
	 * Sets the sync settings activity so that SyncUtil knows what activity to
	 * use to update the sync notification views.
	 *
	 * @param	syncSettingsActivity	the Activity whose views are to be
	 * updated by SyncUtil to reflect syncing progress.
	 */
	public static void setSyncSettingsActivity(Activity syncSettingsActivity) {
		SyncUtil.syncSettingsActivity = syncSettingsActivity;
		updateSyncTextView("Not syncing");
	}

	private static void updateSyncTextView(final String status) {
		if (syncSettingsActivity != null) {
			syncSettingsActivity.runOnUiThread(new Runnable() {
				public void run() {
					TextView syncTextView = (TextView)
							syncSettingsActivity.findViewById(R.id.syncTextNotification);
					if (syncing) {
						syncTextView.setText("Syncing");
					} else {
						syncTextView.setText(status);
					}
				}
			});
		}
	}

	private static void setSyncFlag() {
		syncing = true;
		updateSyncTextView("Syncing");
	}

	private static void unsetSyncFlag(String status) {
		syncing = false;
		updateSyncTextView(status);
	}

	private static ServerCredentials serverCredentials;
	private static Thread syncThread;
	private static Activity syncSettingsActivity;
	private static boolean syncing;
	
	private static final String TAG = "SyncUtil";
}
