package au.edu.utas.todoapp;

import android.app.Dialog;
//import android.app.DialogFragment;
//import android.app.Fragment;
import android.support.design.widget.CollapsingToolbarLayout;
import android.support.v4.app.DialogFragment;
import android.content.Context;
import android.os.Build;
import android.os.Bundle;
import android.support.annotation.NonNull;
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
import android.view.inputmethod.InputMethodManager;
import android.widget.EditText;
import android.widget.Toast;

import java.util.Date;

/**
 * Created by nfs on 4/05/2017.
 */

// Adapted from https://stackoverflow.com/questions/31606871/how-to-achieve-a-full-screen-dialog-as-described-in-material-guidelines

	// When adding to MainActivity
	// https://stackoverflow.com/questions/30845033/cannot-resolve-method-addint-com-example-utkarsh-beatle-app-mainactivity-plac
public class TaskEditDialogFragment extends DialogFragment {
	public static final String TAG = "TaskEditDialogFragment";

	private Context mContext;
	private TaskEditDialogFragment.OnFragmentInteractionListener mListener;
	private View mRootView;
	private TodoDB mDb;

	private Toolbar mToolbar;
	private ActionBar mActionBar;

	private EditText mTaskTitle;
	private EditText mTaskNotes;

	private Boolean mNewTask;
	private int mSelectedTaskId;
	private String mInvokedBy;
	private String mItemType = null;
	private Task mSelectedTask;
	private Goal mSelectedGoal;



	public static TaskEditDialogFragment newInstance(String invokedBy, String itemType) {
		return newInstance(invokedBy, itemType, -1);
	}
	public static TaskEditDialogFragment newInstance(String invokedBy, String itemType, int selecetdTaskId) {
		TaskEditDialogFragment fragment = new TaskEditDialogFragment();
		Bundle args = new Bundle();
		args.putString(MainActivity.EXTRA_INVOKED_BY, invokedBy);
		args.putInt(MainActivity.EXTRA_SELECTED_ITEM_ID, selecetdTaskId);
		args.putString(MainActivity.EXTRA_SELECTED_ITEM_TYPE, itemType);

		fragment.setArguments(args);
		return fragment;
	}

	// Called when a fragment is first attached to its context. onCreate(Bundle) will be called after this. and then OncreateView()
	@Override
	public void onAttach(Context context) {
		super.onAttach(context);
		if (context instanceof TaskEditDialogFragment.OnFragmentInteractionListener) {
			mListener = (TaskEditDialogFragment.OnFragmentInteractionListener) context;
		} else {
			throw new RuntimeException(context.toString()
					+ " must implement OnFragmentInteractionListener");
		}
		mContext = context;
		mDb = new TodoDB(context.getApplicationContext());
	}

	@Override
	public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {

		mRootView = inflater.inflate(R.layout.fragment_dialog_task_edit, container, false);

		initToolbar();

		mTaskTitle = (EditText) mRootView.findViewById(R.id.task_title_edittext);
		mTaskNotes = (EditText) mRootView.findViewById(R.id.task_notes_edittext);

		Bundle arguments = getArguments();
		mItemType = arguments.getString(MainActivity.EXTRA_SELECTED_ITEM_TYPE);
		mInvokedBy = arguments.getString(MainActivity.EXTRA_INVOKED_BY);
		mSelectedTaskId = arguments.getInt(MainActivity.EXTRA_SELECTED_ITEM_ID);

		if(mItemType == Task.TAG) {
			initTask();
		} else if (mItemType == Goal.TAG) {
			initGoal();
		}

		return mRootView;
	}

	// NOTE order: onStop() -> onDestroyView() -> onDestroy() -> onDetach()
	@Override
	public void onStop() {
		super.onStop();
	}

	@Override
	public void onDestroy() {
		super.onDestroy();
		mDb.close(); // Close the database to avoid memory leaks.
		mListener = null;
	}

// ---------------------------- START NAVIGATION METHODS ---------------------------- //
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
		getActivity().getMenuInflater().inflate(R.menu.menu_task_edit, menu);
	}

	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		// Hide keyboard on exit
		// Must try to find something other than getActivity
		if (getActivity() != null) {
			InputMethodManager imm = (InputMethodManager) getActivity().getSystemService(Context.INPUT_METHOD_SERVICE);
			imm.hideSoftInputFromWindow(getActivity().getCurrentFocus().getWindowToken(), 0);
		}

		switch (item.getItemId()) {
			case R.id.action_save:
				saveTask();
				mListener.dismissTaskEditDialog(mInvokedBy, mSelectedTaskId);
				getFragmentManager().popBackStack();
				getFragmentManager().beginTransaction().commit();
				return true;
			case android.R.id.home:
				mListener.dismissTaskEditDialog(mInvokedBy, mSelectedTaskId);
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
	public void initToolbar() {
		mToolbar = (Toolbar) mRootView.findViewById(R.id.toolbar);
		mToolbar.setPadding(0, getStatusBarHeight(),0,0);

		((AppCompatActivity) mContext).setSupportActionBar(mToolbar);

		mActionBar = ((AppCompatActivity) mContext).getSupportActionBar();
		if (mActionBar != null) {
			mActionBar.setDisplayHomeAsUpEnabled(true);
			mActionBar.setHomeButtonEnabled(true);

			mActionBar.setHomeAsUpIndicator(ResourcesCompat.getDrawable(getResources(), R.drawable.ic_close_white_24dp, null));
		}
		setHasOptionsMenu(true);
	}

	public void initTask(){
		if (mSelectedTaskId == -1) {
			mToolbar.setTitle("Add New Task");
			mNewTask = true;
		} else {

			mSelectedTask = mDb.getTask(mSelectedTaskId);
			mSelectedGoal = mSelectedTask.getGoal();

			mTaskTitle.setText(mSelectedTask.getTitle());
			mTaskNotes.setText(mSelectedTask.getNotes());

			mToolbar.setTitle("Edit: " + mSelectedTask.getTitle());
			mToolbar.setBackgroundColor(mSelectedGoal.getColourOpaque());
			mNewTask = false;
		}
	}

	public void initGoal() {

	}

// ---------------------------- END INIT UI METHODS ---------------------------- //

	public void saveTask() {
		if (mNewTask) {
			Task t = new Task(-1);
			t.setTitle(mTaskTitle.getText().toString());
			t.setNotes(mTaskNotes.getText().toString());
			t.setDailyPlanner(new Date());
			t.setDueDate(new Date());
			t.setTaskUrgency(0);
			t.setImportance(0);
			t.setGoal(new Goal(1));
			mDb.addTask(t);

			Toast.makeText(mContext, "New Task Created", Toast.LENGTH_SHORT).show();
			//mListener.dismissTaskEditDialog(mInvokedBy, -1);
		} else {
			mSelectedTask.setTitle(mTaskTitle.getText().toString());
			mSelectedTask.setNotes(mTaskNotes.getText().toString());
			mSelectedTask.setDailyPlanner(new Date());
			mSelectedTask.setDueDate(new Date());
			mSelectedTask.setTaskUrgency(0);
			mSelectedTask.setImportance(0);
			mSelectedTask.setGoal(new Goal(1));

			mDb.editTask(mSelectedTask);

			Toast.makeText(mContext, "Task Updated", Toast.LENGTH_SHORT).show();
			// If the user was viewing the task with a TaskViewDialogFragemnt
			// => update fragment through MainActivity (which implements TaskEditDialogFragmentListener)
			//mListener.dismissTaskEditDialog(mInvokedBy, mSelectedTaskId);
		}
	}

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
		void dismissTaskEditDialog(String dialogInvokedBy, int itemId);
	}
}