/*
	Copyright (C) 2013-2015, The Aikuma Project
	AUTHORS: Oliver Adams and Florian Hanke
*/
package org.lp20.aikuma.util;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.util.Log;
import android.view.View;
import android.widget.TextView;
import android.widget.Toast;
import java.io.File;
import java.io.IOException;
import java.net.InetAddress;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.TimeUnit;

import org.apache.commons.io.FileUtils;
import org.lp20.aikuma.model.Recording;
import org.lp20.aikuma.model.ServerCredentials;
import org.lp20.aikuma.model.Speaker;
import org.lp20.aikuma.service.GoogleCloudService;
import org.lp20.aikuma.Aikuma;
import org.lp20.aikuma.MainActivity;
//import org.lp20.sync.FTPSyncUtil;
import org.lp20.aikuma2.R;

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
	 * (deprecated: previously used by ftp sync with ftp server)
	 * @param activity	MainActivity which starts automatic FTP-sync
	 */
	public static void startSyncLoop(Activity activity) {
		if (syncThread == null || !syncThread.isAlive()) {
			setMainActivity(activity);
			syncThread = new SyncLoop(false);
			syncThread.start();
		}
	}
	
	/**
	 * Starts the thread that attempts a sync once
	 * @param activity	WifiSyncActivity which starts the wifi-p2p-sync
	 * @param ipAddr	The host(server) ip address
	 */
	public static void startSyncLoop(Activity activity, String ipAddr) {
		if (syncThread == null || !syncThread.isAlive()) {
			setMainActivity(activity);
			serverIP = ipAddr;//ipAddr.getHostAddress();
			
			if(serverCredentials == null) {
				try {
					serverCredentials = ServerCredentials.read();
				} catch (IOException e) {
					Log.e(TAG, "failed to read credential: " + e.getMessage());
					serverCredentials = 
							new ServerCredentials(serverIP, "admin", "admin", false, "");
				}
			}
			
			Log.i(TAG, "start sync with " + serverIP + ", automation: " + serverCredentials.getSyncActivated());
			syncThread = new SyncLoop(true);
			syncThread.start();
		}
	}

	/**
	 * Forces a sync to occur now only if syncThread already exists.
	 */
	public static void syncNow() {
		//When we interrupt the syncThread, we fire up a new syncThread which
		//syncs immediately.
		if(syncThread != null)
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
			int waitMins = (int) AikumaSettings.SYNC_INTERVAL/(60000);
			while (true) {
				previousStatus = "";
				try {
					//SyncUtil.serverCredentials = ServerCredentials.read();
					
					//For some reason we get an EPIPE unless we instantiate a new
					//Client at each iteration.
					if (forceSync || serverCredentials.getSyncActivated()) {
						updateMainView(0);
						setSyncFlag();
						forceSync = false;
						Client client = new Client();
						client.setClientBaseDir(
								FileIO.getAppRootPath().toString());
						Log.i(TAG, "beginning sync run");
						if (!client.login(serverIP, //serverCredentials.getIPAddress(),
								serverCredentials.getUsername(),
								serverCredentials.getPassword())) {
							unsetSyncFlag(previousStatus + 
									"\nAuthentication failed.\n(Check IP/UserID/Password/FTP-setup)\n");
							Log.i(TAG, "login failed: " +
									serverCredentials.getIPAddress());
							updateMainView(1);
						} else if (!client.sync()) {
							Log.i(TAG, "sync failed.");
							unsetSyncFlag(previousStatus + "\nTransfer failed\n");
							updateMainView(1);
						} else if (!client.logout()) {
							Log.i(TAG, "Logout failed.");
							unsetSyncFlag("Logout failed.");
							updateMainView(1);
						} else {
							Log.i(TAG, "sync complete.");
							// Create an index file after ftp-sync is finished
							Recording.indexAll();
							
							// Log the success-date in credential file
							serverCredentials.setLastSyncDate(
									new StandardDateFormat().format(new Date()));
							commitServerCredentials(new Date());
							
							unsetSyncFlag("Sync was finished successfully");
							updateMainView(2);
						}
						Log.i(TAG, "end of conditional block");
						waitMins = (int) AikumaSettings.SYNC_INTERVAL/(60000);
						Log.i(TAG, "sync complete");
						updateMainView(1);
					} else {
						Log.i(TAG, "not syncing");
					}
				} catch (IOException e) {
					Log.i("npe", "ioexception on serverCredentials.read()");
					unsetSyncFlag("IOException on serverCredentials.read()");
					//We'll cope and assume the old serverCredentials work.
					updateMainView(1);
				}
				
				
				if(forceSync)	// One-time sync
					break; 
				else {			// Automatic/periodic sync
					try {
						TimeUnit.MINUTES.sleep(waitMins);
					} catch (InterruptedException e) {
						SyncUtil.syncThread = new SyncLoop(true);
						SyncUtil.syncThread.start();
						return;
					}
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
	
	/**
	 * Sets the MainActivity so that SyncUtil invalidates the view of the front-page
	 * @param mainActivity		The activity of the front-page
	 */
	private static void setMainActivity(Activity mainActivity) {
		SyncUtil.mainActivity = mainActivity;
	}

	/**
	 * Show the feedback in the SettingActivity
	 * 
	 * @param status	Feedback-text to the user for current sync-status
	 */
	public static void updateSyncTextView(final String status) {
		if (syncSettingsActivity != null) {
			syncSettingsActivity.runOnUiThread(new Runnable() {
				public void run() {
					TextView syncTextView = (TextView)
							syncSettingsActivity.findViewById(R.id.syncTextNotification);
					syncTextView.setText(status);
				}
			});
			previousStatus = status;
		}
	}

	private static void setSyncFlag() {
		syncing = true;
		updateSyncTextView("Authenticating with FTP server...");
	}

	private static void unsetSyncFlag(String status) {
		syncing = false;
		String date = "";
		if(serverCredentials != null)
			date = serverCredentials.getLastSyncDate();
		updateSyncTextView(status + "\n" + 
		"Last success sync: " + date);
	}

	// Writes the server credentials with latest sync-success date
	private static void commitServerCredentials(Date date) throws IOException {
		try {
			serverCredentials.write();
		} catch (IllegalArgumentException e) {
			Log.e(TAG, e.getMessage());
		}
	}	
	
	// status(0: ftp-sync-start, 1: ftp-sync-error, 2:ftp-sync-end) 
	// because it will be fast, individual files don't have to be informed. 
	private static void updateMainView(final int status) {
		if (mainActivity != null) {
			mainActivity.runOnUiThread(new Runnable() {
				public void run() {
					
					if(status == 2) {
						int numOfSpeakers = 0, numOfItems = 0;
						long usedSpace = 0, freeSpace = 0;
						
						numOfSpeakers = Speaker.readAll().size();
						numOfItems = UsageUtils.numOriginals();
						usedSpace = FileUtils.sizeOfDirectory(FileIO.getAppRootPath());
						freeSpace = FileIO.getAppRootPath().getUsableSpace();
						
						StringBuilder sb = new StringBuilder();
						sb.append("Status:\n");
						sb.append(UsageUtils.getStorageFormat(usedSpace) + 
								" (" + UsageUtils.getTimeFormat(16000, 16, usedSpace) + ") used\n");
						sb.append(UsageUtils.getStorageFormat(freeSpace) + 
								" (" + UsageUtils.getTimeFormat(16000, 16, freeSpace) + ") available\n");
						sb.append(numOfSpeakers + " speakers\n");
						sb.append(numOfItems + " items\n");
						
						
						Toast.makeText(mainActivity, sb.toString(), Toast.LENGTH_LONG).show();
					}
						
					
					/*
					if(status == 0) {
						((MainActivity)mainActivity).showProgressStatus(View.VISIBLE);
					} else if(status == 1) {
						((MainActivity)mainActivity).showProgressStatus(View.GONE);
					} else {
						List<Recording> recordings = Recording.readAll();
						for (Recording recording : recordings) {
							if (recording.isOriginal()) {
								((MainActivity)mainActivity).updateRecordingView(recording);
							}
						}
						((MainActivity)mainActivity).showProgressStatus(View.GONE);
					}*/
					
				}
			});
		}
	}
	
	private static ServerCredentials serverCredentials;
	private static Thread syncThread;
	private static Activity syncSettingsActivity;
	private static Activity mainActivity;
	private static boolean syncing;
	
	private static String previousStatus;

	private static final String TAG = "SyncUtil";

	private static String serverIP;
}
