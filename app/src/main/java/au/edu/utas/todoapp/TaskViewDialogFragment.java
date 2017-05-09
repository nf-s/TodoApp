package au.edu.utas.todoapp;

import android.app.Dialog;
//import android.app.DialogFragment;
import android.support.design.widget.AppBarLayout;
import android.support.v4.app.DialogFragment;
import android.app.FragmentTransaction;
import android.content.Context;
import android.content.res.ColorStateList;
import android.os.Build;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.design.widget.CollapsingToolbarLayout;
import android.support.design.widget.FloatingActionButton;
import android.support.v4.content.res.ResourcesCompat;
import android.support.v7.app.ActionBar;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.support.v7.widget.Toolbar;
import android.widget.TextView;

/**
 * Created by nfs on 4/05/2017.
 */

// Adapted from https://stackoverflow.com/questions/31606871/how-to-achieve-a-full-screen-dialog-as-described-in-material-guidelines

// When adding to MainActivity
// https://stackoverflow.com/questions/30845033/cannot-resolve-method-addint-com-example-utkarsh-beatle-app-mainactivity-plac
public class TaskViewDialogFragment extends DialogFragment {

	public static final String TAG = "TaskViewDialogFragment";

	private TodoDB mDb;
	private Context mContext;
	private TaskViewDialogFragment.OnFragmentInteractionListener mListener;
	private View mRootView;

	private CollapsingToolbarLayout mToolbarLayout;
	private AppBarLayout mAppBarLayout;
	private Toolbar mToolbar;
	private ActionBar mActionBar;

	private FloatingActionButton mTaskEditButton;

	private Task mSelectedTask;
	private int mSelectedTaskId;

	public static TaskViewDialogFragment newInstance(String invokedBy, String itemType, int selecetdTaskId) {
		TaskViewDialogFragment fragment = new TaskViewDialogFragment();
		Bundle args = new Bundle();
		args.putString(MainActivity.EXTRA_INVOKED_BY, invokedBy);
		args.putInt(MainActivity.EXTRA_SELECTED_ITEM_ID, selecetdTaskId);
		args.putString(MainActivity.EXTRA_SELECTED_ITEM_TYPE, itemType);

		fragment.setArguments(args);
		return fragment;
	}

	// Called when a fragment is first attached to its context. onCreate(Bundle) will be called after this.
	@Override
	public void onAttach(Context context) {
		super.onAttach(context);
		if (context instanceof TaskViewDialogFragment.OnFragmentInteractionListener) {
			mListener = (TaskViewDialogFragment.OnFragmentInteractionListener) context;
		} else {
			throw new RuntimeException(context.toString()
					+ " must implement OnFragmentInteractionListener");
		}
		mContext = context;
		mDb = new TodoDB(context.getApplicationContext());
	}

	@Override
	public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {

		mRootView = inflater.inflate(R.layout.fragment_dialog_task_view, container, false);

		initActionBar();

		mSelectedTaskId = getArguments().getInt(MainActivity.EXTRA_SELECTED_ITEM_ID);
		if (mSelectedTaskId > 0) {
			initTask();
		}

		return mRootView;
	}

	@Override
	public void onStop() {
		super.onStop();
	}

	@Override
	public void onDestroy() {
		super.onDestroy();
		mDb.close(); // Close the database to avoid memory leaks.
		//mListener = null;
	}

	@NonNull
	@Override
	public Dialog onCreateDialog(Bundle savedInstanceState) {
		Dialog dialog = super.onCreateDialog(savedInstanceState);
		dialog.requestWindowFeature(Window.FEATURE_NO_TITLE);
		return dialog;
	}

	@Override
	public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
		menu.clear();
		getActivity().getMenuInflater().inflate(R.menu.menu_task_options, menu);
	}

	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		switch (item.getItemId()) {
			// Handle back button on action bars (also used by all DialogFragements attached from this Activity
			case android.R.id.home:
				getFragmentManager().popBackStack();
				getFragmentManager().beginTransaction().commit();
				return true;

			default:
				// If we got here, the user's action was not recognized.
				// Invoke the superclass to handle it.
				return super.onOptionsItemSelected(item);

		}
	}
// ---------------------------- END NAVIGATION METHODS ---------------------------- //

// ---------------------------- START INIT UI METHODS ---------------------------- //
	public void initActionBar() {
		mAppBarLayout = (AppBarLayout) mRootView.findViewById(R.id.app_bar);
		mAppBarLayout.setPadding(0, getStatusBarHeight(), 0 ,0);

		mToolbarLayout = (CollapsingToolbarLayout)  mRootView.findViewById(R.id.toolbar_layout);
		mToolbar = (Toolbar) mRootView.findViewById(R.id.toolbar);

		((AppCompatActivity) mContext).setSupportActionBar(mToolbar);
		mActionBar = ((AppCompatActivity) mContext).getSupportActionBar();

		if (mActionBar != null) {
			mActionBar.setDisplayHomeAsUpEnabled(true);
			mActionBar.setHomeButtonEnabled(true);

		}
		setHasOptionsMenu(true);

		mTaskEditButton = (FloatingActionButton) mRootView.findViewById(R.id.task_view_edit_task_btn);
		mTaskEditButton.setImageDrawable(ResourcesCompat.getDrawable(getResources(), R.drawable.ic_edit_white_36dp, null));
		mTaskEditButton.setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View view) {
				//Snackbar.make(view, "Replace with your own action", Snackbar.LENGTH_LONG).setAction("Action", null).show();
				TaskEditDialogFragment newFragment = TaskEditDialogFragment.newInstance(TAG, Task.TAG, mSelectedTaskId);

				getFragmentManager().beginTransaction().setTransition(FragmentTransaction.TRANSIT_FRAGMENT_OPEN)
						.add(android.R.id.content, newFragment, TaskEditDialogFragment.TAG).addToBackStack(null).commit();
			}
		});
	}

	public void initTask() {
		mSelectedTask = mDb.getTask(mSelectedTaskId);

		TextView taskText = (TextView) mRootView.findViewById(R.id.task_view_text);
		taskText.setText(mSelectedTask.getTitle());

		int goalCol = mSelectedTask.getGoal().getColourOpaque();

		if (Build.VERSION.SDK_INT >= 21 && getActivity() != null) {
			getActivity().getWindow().setStatusBarColor(goalCol);
		}

		mAppBarLayout.setBackgroundColor(goalCol);
		mToolbarLayout.setContentScrimColor(goalCol);
		mTaskEditButton.setBackgroundTintList(ColorStateList.valueOf(goalCol));

		mToolbarLayout.setTitle(mSelectedTask.getTitle());
	}

// ---------------------------- END INIT UI METHODS ---------------------------- //

	public int getStatusBarHeight() {
		int result = 0;
		int resourceId = getResources().getIdentifier("status_bar_height", "dimen", "android");
		if (resourceId > 0) {
			result = getResources().getDimensionPixelSize(resourceId);
		}
		return result;
	}

	/**
	 * This interface must be implemented by activities that contain this
	 * fragment to allow an interaction in this fragment to be communicated
	 * to the activity and potentially other fragments contained in that
	 * activity.
	 * <p>
	 * See the Android Training lesson <a href=
	 * "http://developer.android.com/training/basics/fragments/communicating.html"
	 * >Communicating with Other Fragments</a> for more information.
	 */
	public interface OnFragmentInteractionListener {
		// TODO: Update argument type and name
		void resetPrimaryCol();
		void updatePrimaryCol(int col);
	}
}