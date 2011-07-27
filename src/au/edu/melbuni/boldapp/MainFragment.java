package au.edu.melbuni.boldapp;

import android.app.Fragment;
import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;

public class MainFragment extends Fragment {
	@Override
	public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
	    // Inflate the layout for this fragment
	    View view = inflater.inflate(R.layout.main, container, false);
	    
        // Set button actions.
        //
        final ImageButton recordButton = (ImageButton) view.findViewById(R.id.recordButton);
        recordButton.setOnClickListener(new View.OnClickListener() {
            public void onClick(View view) {
            	// Add the original choice activity on the stack
            	// with the instruction to move on to the record
            	// activity after having chosen an original.
                startActivityForResult(new Intent(view.getContext(), OriginalChoiceActivity.class), 0);
            }
        });
	    
        final ImageButton listenButton = (ImageButton) view.findViewById(R.id.listenButton);
        listenButton.setOnClickListener(new View.OnClickListener() {
            public void onClick(View view) {
                // Perform action on click
            	
            }
        });
        
        final ImageButton respeakButton = (ImageButton) view.findViewById(R.id.respeakButton);
        respeakButton.setOnClickListener(new View.OnClickListener() {
            public void onClick(View view) {
                // Perform action on click
            	
            }
        });
        
        final ImageButton translateButton = (ImageButton) view.findViewById(R.id.translateButton);
        translateButton.setOnClickListener(new View.OnClickListener() {
            public void onClick(View view) {
                // Perform action on click
            	
            }
        });
	    
	    return view;
	}
}
