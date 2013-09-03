package org.lp20.aikuma.ui;

import android.app.Fragment;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.SeekBar;
import android.widget.Toast;
import java.io.File;
import java.io.IOException;
import java.util.Iterator;
import java.util.UUID;
import org.lp20.aikuma.model.Recording;
import org.lp20.aikuma.audio.Player;;
import org.lp20.aikuma.audio.record.PhoneRespeaker;;
import org.lp20.aikuma.audio.SimplePlayer;
import org.lp20.aikuma.audio.InterleavedPlayer;
import org.lp20.aikuma.model.Segments;
import org.lp20.aikuma.model.Segments.Segment;
import org.lp20.aikuma.ui.sensors.ProximityDetector;
import org.lp20.aikuma.R;

public class PhoneRespeakFragment extends Fragment implements OnClickListener {

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
	}

	@Override
	public void onActivityCreated(Bundle savedInstanceState) {
		super.onActivityCreated(savedInstanceState);
	}

	@Override
	public View onCreateView(LayoutInflater inflater, ViewGroup container,
			Bundle savedInstanceState) {
		View v = inflater.inflate(R.layout.listen_fragment, container, false);
		playPauseButton = (ImageButton) v.findViewById(R.id.PlayPauseButton);
		playPauseButton.setOnClickListener(this);
		seekBar = (InterleavedSeekBar) v.findViewById(R.id.InterleavedSeekBar);
		seekBar.setOnSeekBarChangeListener(
				new SeekBar.OnSeekBarChangeListener() {
					public void onProgressChanged(SeekBar seekBar,
							int progress, boolean fromUser) {
						if (fromUser) {
							respeaker.getSimplePlayer().seekToMsec((int)Math.round(
									(((float)progress)/100)*
									respeaker.getSimplePlayer().getDurationMsec()));
						}
					}
					public void onStopTrackingTouch(SeekBar _seekBar) {};
					public void onStartTrackingTouch(SeekBar _seekBar) {};
				});
		seekBar.invalidate();
		return v;
	}

	@Override
	public void onPause() {
		Log.i("issue54", "pause");
		super.onPause();
		pause();
		this.proximityDetector.stop();
	}

	@Override
	public void onResume() {
		super.onResume();
		this.proximityDetector = new ProximityDetector(getActivity()) {
			public void near(float distance) {
				if (!respeaker.getSimplePlayer().isPlaying()) {
					play();
				}
			}
			public void far(float distance) {
				if (respeaker.getSimplePlayer().isPlaying()) {
					pause();
				}
			}
		};
		this.proximityDetector.start();
	}

	@Override
	public void onStop() {
		super.onStop();
	}

	@Override
	public void onDestroy() {
		super.onDestroy();
		if (respeaker != null) {
			//If you hit the stop button really quickly, the player may not
			//have been initialized fully.
			respeaker.release();
		}
	}

	@Override
	public void onClick(View v) {
		if (v == playPauseButton) {
			if (respeaker.getSimplePlayer().isPlaying()) {
				pause();
			} else {
				play();
			}
		}
	}

	private void stopThread(Thread thread) {
		if (thread != null) {
			thread.interrupt();
		}
	}

	private void pause() {
		respeaker.pause();
		stopThread(seekBarThread);
		playPauseButton.setImageResource(R.drawable.play);
	}

	private void play() {
		respeaker.resume();
		seekBarThread = new Thread(new Runnable() {
				public void run() {
					int currentPosition;
					while (true) {
						currentPosition = respeaker.getSimplePlayer().getCurrentMsec();
						seekBar.setProgress(
								(int)(((float)currentPosition/(float)
								respeaker.getSimplePlayer().getDurationMsec())*100));
						try {
							Thread.sleep(1000);
						} catch (InterruptedException e) {
							return;
						}
					}
				}
		});
		seekBarThread.start();
		playPauseButton.setImageResource(R.drawable.pause);
	}

	public void setRecording(Recording recording) {
		this.recording = recording;
	}

	public void setSampleRate(int sampleRate) {
		this.sampleRate = sampleRate;
	}

	public void setUUID(UUID uuid) {
		this.uuid = uuid;
	}

	public void setPhoneRespeaker(PhoneRespeaker respeaker) {
		this.respeaker = respeaker;
		respeaker.getSimplePlayer().setOnCompletionListener(onCompletionListener);
	}

	private Player.OnCompletionListener onCompletionListener =
			new Player.OnCompletionListener() {
				public void onCompletion(Player _player) {
					playPauseButton.setImageResource(R.drawable.play);
					stopThread(seekBarThread);
					seekBar.setProgress(seekBar.getMax());
				}
			};

	private PhoneRespeaker respeaker;
	private ImageButton playPauseButton;
	private InterleavedSeekBar seekBar;
	private Thread seekBarThread;
	private Recording recording;
	private UUID uuid;
	private int sampleRate;
	private ProximityDetector proximityDetector;
}