package au.edu.unimelb.boldapp;

import java.util.List;
import java.util.ArrayList;
import java.util.UUID;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.ImageButton;
import android.widget.SeekBar;

import android.media.MediaPlayer;
import android.media.MediaPlayer.OnCompletionListener;

import au.edu.unimelb.boldapp.audio.PlayerInterface;
import au.edu.unimelb.boldapp.audio.SimplePlayer;
import au.edu.unimelb.boldapp.audio.TestPlayer;
//import au.edu.unimelb.boldapp.audio.InterleavedPlayer;

/**
 * Activity that allows the user to listen to recordings
 *
 * @author	Oliver Adams	<oliver.adams@gmail.com>
 * @author	Florian Hanke	<florian.hanke@gmail.com>
 */
public class ListenActivity extends Activity
		implements Runnable, SeekBar.OnSeekBarChangeListener {
	/**
	 * The player that is used
	 */
	private SimplePlayer player;

	/**
	 * The recording that is being played
	 */
	private Recording recording;

	/**
	 * Indicates whether the recording has begun playing or not; is reset to
	 * false when playing is complete.
	 */
	private Boolean startedPlaying;

	/**
	 * The progress bar
	 */
	private SeekBar seekBar;

	/**
	 * Thread to deal with updating of progress bar
	 */
	private Thread seekBarThread;

	private TestPlayer originalPlayer;
	private TestPlayer respeakingPlayer;

	/**
	 * Initialization when the activity starts.
	 *
	 * @param	savedInstanceState	Data the activity most recently supplied to
	 * onSaveInstanceState(Bundle).
	 */
	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.listen);

		startedPlaying = false;

		// Set the recording that is to be played.
		Intent intent = getIntent();
		UUID recordingUUID = (UUID) intent.getExtras().get("recordingUUID");
		this.recording = GlobalState.getRecordingMap().get(recordingUUID);

		// Set up the player
		this.player = new SimplePlayer(this.recording.getUuid());
		//PlayerInterface test = new InterleavedPlayer(
		//		UUID.fromString("e90cf7c7-a2bb-40a0-b7d1-8e67cc5f1e5c"),
		//		UUID.fromString("9c11e147-5576-4c83-b5aa-92275257a554"));

		List<Integer> originalSegments = new ArrayList<Integer>();
		originalSegments.add(0);
		originalSegments.add(87318);
		originalSegments.add(147646);
		originalSegments.add(211106);
		int i = 0;
		Log.i("shizz", " " + originalSegments.get(i++));
		Log.i("shizz", " " + originalSegments.get(i++));
		Log.i("shizz", " " + originalSegments.get(i++));
		Log.i("shizz", " " + originalSegments.get(i++));
		List<Integer> respeakingSegments = new ArrayList<Integer>();
		respeakingSegments.add(0);
		respeakingSegments.add(116000);
		respeakingSegments.add(240000);

		originalPlayer = new
				TestPlayer(this.recording.getOriginalUuid(), originalSegments);
		respeakingPlayer = new TestPlayer(this.recording.getUuid(),
				respeakingSegments);

		originalPlayer.setOtherPlayer(respeakingPlayer);
		respeakingPlayer.setOtherPlayer(originalPlayer);
		originalPlayer.setOwner(this);
		respeakingPlayer.setOwner(this);
		originalPlayer.setPositionNotificationPeriod(10000);
		respeakingPlayer.setPositionNotificationPeriod(10000);

		originalPlayer.play();
		respeakingPlayer.play();
		respeakingPlayer.pause();
		//testPlayer.play(0, 116000, null);
		//testPlayer.play(0, 116000);

		//Thread u = testPlayer.play(116000, 269000, t);

		this.seekBar = (SeekBar) findViewById(R.id.SeekBar);
		this.seekBar.setOnSeekBarChangeListener(this);
		//this.seekBarThread = new Thread(this);


		// The code that is to be run when the player is complete.
		player.setOnCompletionListener(new OnCompletionListener() {
			@Override
			public void onCompletion(MediaPlayer _player) {
				// Reset the play button
				ImageButton button = (ImageButton) 
						findViewById(R.id.Play);
				button.setImageResource(R.drawable.button_play);
				// Adjust relevant booleans
				startedPlaying = false;
				// Stop the seekBarThread and set the progress to max.
				seekBarThread.interrupt();
				seekBar.setProgress(seekBar.getMax());
			}
		});

	}

	/**
	 * When the activity is stopped
	 */
	@Override
	public void onStop() {
		super.onStop();
		if (this.seekBarThread != null) {
			this.seekBarThread.interrupt();
		}
		player.release();
		ListenActivity.this.finish();
	}

	/**
	 * When the back button is pressed
	 *
	 * @param	view	The button that was clicked.
	 */
	public void goBack(View view){
		player.release();
		if (this.seekBarThread != null) {
			this.seekBarThread.interrupt();
		}
		ListenActivity.this.finish();
	}

	/**
	 * When the play button is pressed.
	 *
	 * @param	view	The button that was pressed
	 */
	public void play(View view) {
		ImageButton button = (ImageButton) view;
		if (!player.isPlaying()) {
			button.setImageResource(R.drawable.button_pause);
			player.start();
			if (!startedPlaying) {
				// If the user hasn't changed the progress to some other
				// unfinished point in the recording
				if (seekBar.getProgress() == seekBar.getMax()) {
					seekBar.setProgress(0);
				}
				this.seekBarThread = new Thread(this);
				this.seekBarThread.start();
				startedPlaying = true;
			}
		} else {
			button.setImageResource(R.drawable.button_play);
			player.pause();
			seekBar.setProgress((int)(((float)player.getCurrentPosition()/
					(float)player.getDuration())*100));
		}
	}

	/**
	 * The run function for the thread that updates the seekBar.
	 */
	@Override
	public void run() {
		int currentPosition = 0;
		int total = player.getDuration();
		while (currentPosition<total) {
			try {
				Thread.sleep(1000);
				if (player == null) {
					currentPosition = total;
				} else {
					currentPosition = player.getCurrentPosition();
				}
			} catch (InterruptedException e) {
				return;
			} catch (Exception e) {
				e.printStackTrace();
				return;
			}
			seekBar.setProgress(
					(int)(((float)currentPosition/(float)total)*100));
		}
	}

	/**
	 * When the seekBar's progress is changed.
	 */
	@Override
	public void onProgressChanged(
			SeekBar seekBar, int progress, boolean fromUser) {
		if (fromUser) {
			player.seekTo((int)Math.round(
					(((float)progress)/100)*player.getDuration()));
		} else {
			//Progress was changed programatically
		}
	}

	/**
	 * Obligated to implement this, but we need no functionality here.
	 */
	@Override
	public void onStartTrackingTouch(SeekBar seekBar) {
	}

	/**
	 * Obligated to implement this, but we need no functionality here.
	 */
	@Override
	public void onStopTrackingTouch(SeekBar seekBar) {
	}

	public void swap() {
		Log.i("blorg", "i'm here, playa");
		respeakingPlayer.play();
	}
}
