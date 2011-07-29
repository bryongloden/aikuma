package au.edu.melbuni.boldapp;

import android.graphics.Color;
import android.view.View;
import android.widget.Button;
import android.widget.LinearLayout;

public class Segment {
	
	TimeLine timeLine = null;
	String identifier = null;
	Button view = null;
	private boolean selected;
	
	public Segment(final TimeLine timeLine, int id) {
		this.timeLine   = timeLine;
		this.identifier = timeLine.identifier + new Integer(id).toString();
		this.view       = new Button(timeLine.getContext());
		
		this.view.setWidth(100);
		
		this.view.setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View view) {
				timeLine.setSelectedForRecording(Segment.this);
				timeLine.setSelectedForPlaying(Segment.this);
			}
		});
//		this.view.setOnLongClickListener(new View.OnLongClickListener() {
//			// TODO Ask if it should be removed.
//			//
//			@Override
//			public boolean onLongClick(View v) {
//				timeLine.remove(Segment.this);
//				return false;
//			}
//		});
	}

	public void addTo(LinearLayout layout) {
		layout.addView(view);
	}
	public void removeFrom(LinearLayout layout) {
		layout.removeView(view);
	}
	
	public void startPlaying(Recorder recorder) {
		recorder.startPlaying(identifier);
		
		view.setBackgroundColor(Color.GREEN);
		
//		Animation fade = new AlphaAnimation(0.0f, 1.0f);
//		fade.setDuration(500);
//		fade.setRepeatCount(AnimationSet.INFINITE);
//		fade.setRepeatMode(Animation.REVERSE);
//		view.startAnimation(fade);
	}
	public void stopPlaying(Recorder recorder) {
		recorder.stopPlaying();
		resetColor();
		
//		view.clearAnimation();
	}
	public void startRecording(Recorder recorder) {
		recorder.startRecording(identifier);
		
		view.setBackgroundColor(Color.RED);
	}
	public void stopRecording(Recorder recorder) {
		recorder.stopRecording();
		resetColor();
	}

	public void select() {
		this.selected = true;
		resetColor();
	}
	public void unselect() {
		this.selected = false;
		resetColor();
	}
	public void resetColor() {
		if (selected) {
			view.setBackgroundColor(Color.LTGRAY);
		} else {
			view.setBackgroundColor(Color.GRAY);
		}
	}
	
}
