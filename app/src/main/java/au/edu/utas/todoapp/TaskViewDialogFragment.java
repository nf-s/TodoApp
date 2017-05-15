package au.edu.utas.todoapp;

import android.content.res.Resources;
import android.support.design.widget.AppBarLayout;
import android.support.v4.app.DialogFragment;
import android.content.Context;
import android.content.res.ColorStateList;
import android.os.Bundle;
import android.support.design.widget.CollapsingToolbarLayout;
import android.support.design.widget.FloatingActionButton;
import android.support.v4.content.res.ResourcesCompat;
import android.support.v4.widget.NestedScrollView;
import android.support.v7.app.ActionBar;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.support.v7.widget.Toolbar;
import android.widget.ImageView;
import android.widget.TextView;

/**
 * Created by nfs on 4/05/2017.
 */

// Adapted from https://stackoverflow.com/questions/31606871/how-to-achieve-a-full-screen-dialog-as-described-in-material-guidelines


/**
 *  This Fragment is used to view a TodoDisplayableItem (eg Task or Goal)
 *  The user can then choose to edit the item (which uses a TaskEditDialogFragment)
 */
public class TaskViewDialogFragment extends DialogFragment implements TodoDisplayableItemMenu.OnMenuItemClickedListener {

	public static final String TAG = "TaskViewDialogFragment";

	private TodoDB mDb;
	private Context mContext;
	private TodoFragmentListener mListener;
	private View mRootView;

	private CollapsingToolbarLayout mToolbarLayout;
	private AppBarLayout mAppBarLayout;
	private Toolbar mToolbar;
	private ActionBar mActionBar;

	private FloatingActionButton mItemEditButton;

	// Extras/Intent variables
	private String mInvokedBy;
	private int mSelectedItemId;
	private String mItemType = null;

	private Task mSelectedTask;
	private Goal mSelectedGoal;
	private TodoDisplayableItem mSelectedItem;
	private TodoDisplayableItemMenu mToolbarOptionsMenu;

	/**
	 *  All new instances of this DialogFragment should use this method
	 *  This enforces correct values for extras/arguments
	 */
	public static TaskViewDialogFragment newInstance(String invokedBy, String itemType, int itemId) {
		TaskViewDialogFragment fragment = new TaskViewDialogFragment();
		Bundle args = new Bundle();
		args.putString(MainActivity.EXTRA_INVOKED_BY, invokedBy);
		args.putInt(MainActivity.EXTRA_SELECTED_ITEM_ID, itemId);
		args.putString(MainActivity.EXTRA_SELECTED_ITEM_TYPE, itemType);

		fragment.setArguments(args);
		return fragment;
	}

	/**
	 *  Called when the fragment is first attached to its context. (onCreate(Bundle) will be called after this)
	 *  It sets the mListener object (which implements TodoFragmentListener)
	 *  mListener is used to communicate with MainActivity (and other fragments through MainActivity)
	 */
	@Override
	public void onAttach(Context context) {
		super.onAttach(context);
		if (context instanceof TodoFragmentListener) {
			mListener = (TodoFragmentListener) context;
		} else {
			throw new RuntimeException(context.toString()
					+ " must implement TodoFragmentListener");
		}
		mContext = context;
		mDb = new TodoDB(context.getApplicationContext());
	}

	/**
	 *  Called when then fragment is first created.
	 *  Initialises components which are RETAINED if fragment is paused or stopped and then resumed
	 *  Extra/argument vars are created
	 *  Selected item (Task or Goal) is also created
	 */
	@Override
	public void onCreate(Bundle savedInstanceState) {
		Bundle arguments = getArguments();
		mItemType = arguments.getString(MainActivity.EXTRA_SELECTED_ITEM_TYPE);
		mInvokedBy = arguments.getString(MainActivity.EXTRA_INVOKED_BY);
		mSelectedItemId = arguments.getInt(MainActivity.EXTRA_SELECTED_ITEM_ID);

		mSelectedItem = mDb.getItem(mItemType, mSelectedItemId);
		if (mItemType.equals(Task.TAG)) {
			mSelectedTask = (Task) mSelectedItem;
			mSelectedGoal = mSelectedTask.getGoal();
		} else {
			mSelectedGoal = (Goal) mSelectedItem;
		}

		super.onCreate(savedInstanceState);
	}

	/**
	 *  Called to draw fragment UI
	 *  Initialises all UI elements
	 */
	@Override
	public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {

		mRootView = inflater.inflate(R.layout.fragment_dialog_task_view, container, false);

		initAppBar();
		initTaskEditFAB();

		NestedScrollView scrollView = (NestedScrollView) mRootView.findViewById(R.id.task_view_scrollview);
		if (mItemType.equals(Task.TAG)) {
			scrollView.addView(getLayoutInflater(savedInstanceState).inflate(R.layout.content_task_view, null));
			initTaskViews();
		} else {
			scrollView.addView(getLayoutInflater(savedInstanceState).inflate(R.layout.content_goal_view, null));
			initGoalViews();
		}

		return mRootView;
	}

	/**
	 *  Called when the fragment is no longer in use.
	 *  Note order: onStop() -> onDestroyView() -> onDestroy() -> onDetach()
	 */
	@Override
	public void onDestroy() {
		super.onDestroy();
		mDb.close(); // Close the database to avoid memory leaks.
		mListener = null;
	}

	// ------------------------------------------------------------------------
	// NAVIGATION METHODS (HANDLES SELECTING TOOLBAR & NAVIGATION DRAWER MENU ITEMS)
	// ------------------------------------------------------------------------

	/**
	 *  Creates options menu in toolbar for item (Task or Goal)
	 *  Options menu creation and event handling uses TodoDisplayableItemMenu class
	 *  Menu item selection is then passed to onTodoItemOptionsMenuItemClick() method
	 *
	 *  All other toolbar items (such as back or home) are dealt with as usual - with onOptionsItemSelected()
	 */
	@Override
	public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
		if (getArguments().containsKey(MainActivity.EXTRA_SELECTED_ITEM_TYPE)) {
			if (getArguments().getString(MainActivity.EXTRA_SELECTED_ITEM_TYPE).equals(Task.TAG)) {
				mToolbarOptionsMenu = new TodoDisplayableItemMenu().new TaskOptionsMenu(mContext, this, mSelectedTask);
				mToolbarOptionsMenu.setMenuItems(menu);
				mToolbar.setOnMenuItemClickListener(mToolbarOptionsMenu);
			} else {
				mToolbarOptionsMenu = new TodoDisplayableItemMenu().new GoalOptionsMenu(mContext, this, mSelectedGoal);
				mToolbarOptionsMenu.setMenuItems(menu);
				mToolbar.setOnMenuItemClickListener(mToolbarOptionsMenu);
			}
		}
	}

	/**
	 *  Handles all non item related toolbar items
	 *  Currently only the "home" button (which is configured as an "up" button)
	 *  which pops this fragment off the stack (closes the fragment)
	 */
	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		//mSelectedItem.handlePopupOnClick(getActivity(), item.getItemId(), 0, this);

		switch (item.getItemId()) {
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

	/**
	 *  Handles all item related toolbar items (Task or Goal)
	 *  All modification of items are handled by TodoDisplayableItemMenu class (such as edit or delete)
	 *  This method only deals with operations specific to this class (ie closing the fragment after deleting)
	 */
	@Override
	public void onTodoItemOptionsMenuItemClick(MenuItem item, TodoDisplayableItem todoItem) {
		switch (item.getItemId()) {
			case R.id.item_popup_menu_delete:
				if(mItemType.equals(Task.TAG))
					mListener.taskListChanged(mSelectedItemId, mInvokedBy);
				else
					mListener.goalListChanged(mSelectedItemId, mInvokedBy);
				getFragmentManager().popBackStack();
				getFragmentManager().beginTransaction().commit();
				break;
			case R.id.item_popup_menu_edit:
				MainActivity.newTaskEditDialog(getFragmentManager(), TAG, mItemType, mSelectedItemId);
				break;
			case R.id.item_popup_menu_add_dailyplan:
				mListener.taskListChanged(mSelectedItemId, mInvokedBy);
				break;
			case R.id.item_popup_menu_mark_completed:
				mListener.taskListChanged(mSelectedItemId, mInvokedBy);
				initTaskViews();
				break;
			case R.id.item_popup_menu_mark_uncompleted:
				mListener.taskListChanged(mSelectedItemId, mInvokedBy);
				initTaskViews();
				break;
		}
		mToolbar.getMenu().clear();
		mToolbarOptionsMenu.setMenuItems(mToolbar.getMenu());
	}

	// ------------------------------------------------------------------------
	// UI INITIALISATION METHODS
	// ------------------------------------------------------------------------

	/**
	 *  Initialises AppBar and toolbar related UI
	 *  Sets home button as up button
	 */
	public void initAppBar() {
		mAppBarLayout = (AppBarLayout) mRootView.findViewById(R.id.app_bar);
		// This is needed because of the transparent status bar - which increases the height of the application
		mAppBarLayout.setPadding(0, MainActivity.getStatusBarHeight(getResources()), 0 ,0);

		mToolbarLayout = (CollapsingToolbarLayout)  mRootView.findViewById(R.id.toolbar_layout);
		mToolbar = (Toolbar) mRootView.findViewById(R.id.toolbar);

		((AppCompatActivity) mContext).setSupportActionBar(mToolbar);
		mActionBar = ((AppCompatActivity) mContext).getSupportActionBar();

		if (mActionBar != null) {
			mActionBar.setDisplayHomeAsUpEnabled(true);
			mActionBar.setHomeButtonEnabled(true);
		}

		setHasOptionsMenu(true);
	}

	/**
	 *  Initialises Floating Action Button for editing the item
	 */
	private void initTaskEditFAB() {
		mItemEditButton = (FloatingActionButton) mRootView.findViewById(R.id.task_view_edit_task_btn);
		mItemEditButton.setImageDrawable(ResourcesCompat.getDrawable(getResources(), R.drawable.ic_edit_white_36dp, null));
		mItemEditButton.setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View view) {
				//Snackbar.make(view, "Replace with your own action", Snackbar.LENGTH_LONG).setAction("Action", null).show();
				MainActivity.newTaskEditDialog(getFragmentManager(), TAG, mItemType, mSelectedItemId);
			}
		});
	}

	/**
	 *  Initialises all UI elements needed to view a Task item
	 *  and changes the colour of certain elements to that of the goal of the task selected
	 */
	public void initTaskViews() {
		TextView taskText = (TextView) mRootView.findViewById(R.id.task_view_goal_text);
		taskText.setText(mSelectedGoal.getTitle());
		((ImageView)mRootView.findViewById(R.id.task_view_goal_img)).setColorFilter(mSelectedGoal.getColourOpaque());

		TextView taskCompletedDate = (TextView) mRootView.findViewById(R.id.task_view_completed_text);
		String taskCompleteDateString = Task.getDueDateString(mSelectedTask.getMarkedCompletedDate());
		if (taskCompleteDateString.equals("")) {
			(mRootView.findViewById(R.id.task_view_completed_layout)).setVisibility(View.GONE);
		}
		else {
			(mRootView.findViewById(R.id.task_view_completed_layout)).setVisibility(View.VISIBLE);
			taskCompletedDate.setText(String.format(getString(R.string.item_completed_date), taskCompleteDateString));
		}

		TextView taskDueDate = (TextView) mRootView.findViewById(R.id.task_view_dutedate_text);
		taskDueDate.setText(mSelectedTask.getDueDateString());

		TextView taskDueTime = (TextView) mRootView.findViewById(R.id.task_view_duetime_text);
		taskDueTime.setText(mSelectedTask.getDueTimeString());

		TextView taskImportance = (TextView) mRootView.findViewById(R.id.task_view_importance_text);
		try {
			String importanceString = "task_view_importance_"+ Integer.toString(mSelectedTask.getImportance());
			taskImportance.setText(getString(getResources().getIdentifier(importanceString, "string", mContext.getPackageName())));
		} catch (Resources.NotFoundException e) {
			taskImportance.setText(getString(R.string.task_view_importance_0));
		}

		TextView taskUrgency = (TextView) mRootView.findViewById(R.id.task_view_urgent_text);
		try {
			String urgencyString = "task_view_urgency_"+ Integer.toString(mSelectedTask.getImportance());
			taskUrgency.setText(getResources().getIdentifier(urgencyString, "string", mContext.getPackageName()));
		} catch (Resources.NotFoundException e) {
			taskUrgency.setText(getString(R.string.task_view_importance_0));
		}

		TextView taskNotesText = (TextView) mRootView.findViewById(R.id.task_view_notes_text);
		taskNotesText.setText(mSelectedTask.getNotes());

		int goalCol = mSelectedTask.getGoal().getColourOpaque();

		mAppBarLayout.setBackgroundColor(goalCol);
		mToolbarLayout.setContentScrimColor(goalCol);
		mItemEditButton.setBackgroundTintList(ColorStateList.valueOf(goalCol));

		mToolbarLayout.setTitle(mSelectedTask.getTitle());
	}

	/**
	 *  Initialises all UI elements needed to view a Goal item
	 *  and changes the colour of certain elements to that of the goal selected
	 */
	public void initGoalViews() {
		TextView goalColText = (TextView) mRootView.findViewById(R.id.goal_view_colour_text);
		String colString = "col_"+ String.format("%06X",mSelectedGoal.getColour());
		Log.d(TAG, colString);
		try {
			goalColText.setText(getResources().getIdentifier(colString, "string", mContext.getPackageName()));
		} catch (Resources.NotFoundException e) {
			goalColText.setText("Goal colour");
		}

		TextView goalNotesText = (TextView) mRootView.findViewById(R.id.goal_view_notes_text);
		goalNotesText.setText(mSelectedGoal.getNotes());

		int goalCol = mSelectedGoal.getColourOpaque();

		((ImageView)mRootView.findViewById(R.id.goal_view_colour_img)).setColorFilter(goalCol);
		mAppBarLayout.setBackgroundColor(goalCol);
		mToolbarLayout.setContentScrimColor(goalCol);
		mItemEditButton.setBackgroundTintList(ColorStateList.valueOf(goalCol));

		mToolbarLayout.setTitle(mSelectedGoal.getTitle());
	}

	// ------------------------------------------------------------------------
	// RUNTIME METHODS
	// ------------------------------------------------------------------------

	/**
	 *  Refreshes current UI elements
	 *  (called if a task or goal is updated while this fragment is active)
	 */
	public void refresh() {
		mSelectedItem = mDb.getItem(mItemType, mSelectedItemId);
		if (mSelectedItem != null) {
			if (mItemType.equals(Task.TAG)) {
				mSelectedTask = (Task) mSelectedItem;
				mSelectedGoal = mSelectedTask.getGoal();
				initTaskViews();
			} else {
				mSelectedGoal = (Goal) mSelectedItem;
				initGoalViews();
			}
		}
	}

}