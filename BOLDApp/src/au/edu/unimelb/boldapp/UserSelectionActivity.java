package au.edu.unimelb.boldapp;

import android.app.ListActivity;
import android.widget.ListView;
import android.widget.ArrayAdapter;
import android.widget.Toast;
import android.os.Bundle;
import android.view.View;
import android.content.Intent;

import android.util.Log;

public class UserSelectionActivity extends ListActivity {
	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.user_selection);

		User[] users = FileIO.loadUsers();
		ArrayAdapter adapter = new UserArrayAdapter(this, users);
		setListAdapter(adapter);
	}

	@Override
	protected void onListItemClick(ListView l, View v, int position, long id) {
		User user = (User) getListAdapter().getItem(position);
		GlobalState.setCurrentUser(user);
		Toast.makeText(this,
				user.getName() + " selected", Toast.LENGTH_LONG).show();
	}

	@Override
	public void onStop() {
		super.onStop();
	}

	public void goBack(View view){
		UserSelectionActivity.this.finish();
	}

	public void createUser(View view) {
		Intent intent = new Intent(this, CreateUserActivity.class);
		startActivity(intent);
		this.finish();
	}
	/*
	public void changeUser(View view) {
		Intent intent = new Intent(this, UserListActivity.class);
		startActivity(intent);
	}
	*/
}
