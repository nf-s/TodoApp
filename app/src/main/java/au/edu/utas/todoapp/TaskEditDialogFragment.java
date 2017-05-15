package au.edu.utas.todoapp;

import android.app.DatePickerDialog;
import android.app.TimePickerDialog;
import android.content.res.Resources;
import android.graphics.drawable.Drawable;
import android.support.design.widget.AppBarLayout;
import android.support.v4.app.DialogFragment;
import android.content.Context;
import android.os.Bundle;
import android.support.v4.content.res.ResourcesCompat;
import android.support.v4.graphics.drawable.DrawableCompat;
import android.support.v4.widget.NestedScrollView;
import android.support.v7.app.ActionBar;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.PopupMenu;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.support.v7.widget.Toolbar;
import android.view.inputmethod.InputMethodManager;
import android.widget.DatePicker;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.TimePicker;
import android.widget.Toast;

import com.android.colorpicker.ColorPickerDialog;
import com.android.colorpicker.ColorPickerSwatch;

import java.util.Calendar;

// Adapted from https://stackoverflow.com/questions/31606871/how-to-achieve-a-full-screen-dialog-as-described-in-material-guidelines

/**
 *  This Fragment is used to edit a TodoDisplayableItem (eg Task or Goal)
 */
public class TaskEditDialogFragment extends DialogFragment {
	public static final String TAG = "TaskEditDialogFragment";

	private Context mContext;
	private TodoFragmentListener mListener;
	private View mRootView;
	private TodoDB mDb;

	private Toolbar mToolbar;
	private ActionBar mActionBar;
	private AppBarLayout mAppBarLayout;

	private EditText mTaskTitle, mTaskNotes, mTaskGoal, mTaskDueDate, mTaskDueTime, mTaskImportance, mTaskUrgency;
	private EditText mGoalTitle, mGoalNotes, mGoalColour;

	// Extras/Intent variables
	private String mInvokedBy;
	private int mSelectedItemId;
	private String mItemType = null;
	private int mParentId;

	private Task mSelectedTask;
	private Goal mSelectedGoal;

	private Calendar mSelectedTaskDueDate = null;
	private Calendar mSelectedTaskDueTime = null;

	/**
	 *  All new instances of this DialogFragment should use this method
	 *  This enforces correct values for extras/arguments
	 */
	public static TaskEditDialogFragment newInstance(String invokedBy, String itemType) {
		return newInstance(invokedBy, itemType, -1);
	}

	public static TaskEditDialogFragment newInstance(String invokedBy, String itemType, int selecetdTaskId) {
		return newInstance(invokedBy, itemType, selecetdTaskId, -1);
	}

	public static TaskEditDialogFragment newInstance(String invokedBy, String itemType, int selecetdTaskId, int parentId) {
		TaskEditDialogFragment fragment = new TaskEditDialogFragment();
		Bundle args = new Bundle();

		args.putString(MainActivity.EXTRA_INVOKED_BY, invokedBy);
		args.putInt(MainActivity.EXTRA_SELECTED_ITEM_ID, selecetdTaskId);
		args.putString(MainActivity.EXTRA_SELECTED_ITEM_TYPE, itemType);
		args.putInt(MainActivity.EXTRA_PARENT_ITEM_ID, parentId);

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
		mParentId = arguments.getInt(MainActivity.EXTRA_PARENT_ITEM_ID);

		super.onCreate(savedInstanceState);
	}

	/**
	 *  Called to draw fragment UI
	 *  Initialises all UI elements
	 */
	@Override
	public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {

		mRootView = inflater.inflate(R.layout.fragment_dialog_task_edit, container, false);

		initAppBar();

		NestedScrollView scrollView = (NestedScrollView) mRootView.findViewById(R.id.task_edit_scrollview);
		// if Task
		if (mItemType.equals(Task.TAG)) {
			scrollView.addView(getLayoutInflater(savedInstanceState).inflate(R.layout.content_task_edit, null));
			initTaskUI();

			// If a new task is being created
			if (mSelectedItemId == -1) {
				mSelectedTask = new Task(-1);
				initNewTask();
			}
			// Or a task is being edited
			else {
				mSelectedTask = mDb.getTask(mSelectedItemId);
				mSelectedGoal = mSelectedTask.getGoal();
				initEditTask();
			}
		}
		// if Goal
		else {
			scrollView.addView(getLayoutInflater(savedInstanceState).inflate(R.layout.content_goal_edit, null));
			initGoalUI();

			// If a new goal is being created
			if (mSelectedItemId == -1) {
				mSelectedGoal = new Goal(-1);
				initNewGoal();
			}
			// Or a goal is being edited
			else {
				mSelectedGoal = mDb.getGoal(mSelectedItemId);
				initEditGoal();
			}
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
	// NAVIGATION METHODS (HANDLES SELECTING TOOLBAR ITEMS)
	// ------------------------------------------------------------------------

	/**
	 *  Creates options menu in toolbar for item (Task or Goal)
	 *  Menu item selection is handled with onOptionsItemSelected()
	 */
	@Override
	public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
		menu.clear();
		if(mItemType.equals(Task.TAG))
			inflater.inflate(R.menu.menu_task_edit, menu);
		else
			inflater.inflate(R.menu.menu_goal_edit, menu);
	}

	/**
	 *  Handles all toolbar item selection
	 */
	@Override
	public boolean onOptionsItemSelected(MenuItem item) {

		// Hide keyboard on exit
		// Must try to find something other than getActivity
		if (getActivity() != null) {
			InputMethodManager imm = (InputMethodManager) getActivity().getSystemService(Context.INPUT_METHOD_SERVICE);
			imm.hideSoftInputFromWindow(getActivity().getCurrentFocus().getWindowToken(), 0);
		}

		switch (item.getItemId()) {
			case R.id.action_save_task:
				if (mItemType.equals(Task.TAG)) {
					saveTask();
					getFragmentManager().popBackStack();
					getFragmentManager().beginTransaction().commit();

					return true;
				} else {
					return super.onOptionsItemSelected(item);
				}
			case R.id.action_save_goal:
				if (mItemType.equals(Goal.TAG)) {
					saveGoal();
					getFragmentManager().popBackStack();
					getFragmentManager().beginTransaction().commit();
					return true;
				} else {
					return super.onOptionsItemSelected(item);
				}
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

	// ------------------------------------------------------------------------
	// UI INITIALISATION METHODS
	// ------------------------------------------------------------------------

	/**
	 *  Initialises AppBar and toolbar related UI
	 *  Sets home button as up button (which displays as a close "X" button)
	 */
	public void initAppBar() {
		mAppBarLayout = (AppBarLayout) mRootView.findViewById(R.id.app_bar);

		mToolbar = (Toolbar) mRootView.findViewById(R.id.toolbar);
		// This is needed because of the transparent status bar - which increases the height of the application
		mToolbar.setPadding(0, MainActivity.getStatusBarHeight(getResources()),0,0);

		((AppCompatActivity) mContext).setSupportActionBar(mToolbar);
		mActionBar = ((AppCompatActivity) mContext).getSupportActionBar();

		if (mActionBar != null) {
			mActionBar.setDisplayHomeAsUpEnabled(true);
			mActionBar.setHomeButtonEnabled(true);
			mActionBar.setHomeAsUpIndicator(ResourcesCompat.getDrawable(getResources(), R.drawable.ic_close_white_24dp, null));
		}

		setHasOptionsMenu(true);
	}

	/**
	 *  Initialises all UI elements needed to add/edit a Task item
	 *  and creates popup menus
	 */
	public void initTaskUI() {
		mTaskTitle = (EditText) mRootView.findViewById(R.id.item_edit_title_text);
		mTaskGoal = (EditText) mRootView.findViewById(R.id.task_edit_goal_text);
		mTaskDueDate = (EditText) mRootView.findViewById(R.id.task_edit_dutedate_text);
		mTaskDueTime = (EditText) mRootView.findViewById(R.id.task_edit_duetime_text);
		mTaskImportance = (EditText) mRootView.findViewById(R.id.task_edit_importance_text);
		mTaskUrgency = (EditText) mRootView.findViewById(R.id.task_edit_urgent_text);
		mTaskNotes = (EditText) mRootView.findViewById(R.id.task_edit_notes_text);

		createGoalPopupMenu();
		createDateTimePopupMenus();
	}

	/**
	 *  Sets all UI elements for adding a new task
	 */
	public void initNewTask() {
		mToolbar.setTitle("Add New Task");

		// If a task goal has been provided (goal id = parent id)
		if (mParentId != -1) {
			mSelectedGoal=mDb.getGoal(mParentId);
			mSelectedTask.setGoal(mSelectedGoal);
			mTaskGoal.setText(mSelectedGoal.getTitle());
			setPrimaryUIColour();
		}
	}

	/**
	 *  Sets all UI elements for editing a task
	 *  Populates fields with values of task
	 */
	public void initEditTask(){
		mToolbar.setTitle("Edit Task");

		mTaskTitle.setText(mSelectedTask.getTitle());
		mTaskGoal.setText(mSelectedGoal.getTitle());

		mSelectedTaskDueDate = mSelectedTask.getDueDate();
		mTaskDueDate.setText(Task.getDueDateString(mSelectedTaskDueDate));
		mSelectedTaskDueTime = mSelectedTask.getDueTime();
		mTaskDueTime.setText(Task.getDueTimeString(mSelectedTaskDueTime));

		mTaskImportance.setText(Integer.toString(mSelectedTask.getImportance()));
		mTaskUrgency.setText(Integer.toString(mSelectedTask.getUrgency()));
		mTaskNotes.setText(mSelectedTask.getNotes());

		setPrimaryUIColour();
	}

	/**
	 *  Initialises all UI elements needed to add/edit a Goal item
	 *  and creates popup menus
	 */
	public void initGoalUI() {
		mGoalTitle = (EditText) mRootView.findViewById(R.id.item_edit_title_text);
		mGoalColour = (EditText) mRootView.findViewById(R.id.goal_edit_colour_text);
		mGoalNotes = (EditText) mRootView.findViewById(R.id.goal_edit_notes_text);

		createColourPickerDialog();
	}

	/**
	 *  Sets all UI elements for adding a new goal
	 */
	public void initNewGoal() {
		mToolbar.setTitle("Add New Goal");
	}

	/**
	 *  Sets all UI elements for editing a goal
	 *  Populates fields with values of goal
	 */
	public void initEditGoal() {
		mToolbar.setTitle("Edit Goal");

		mGoalTitle.setText(mSelectedGoal.getTitle());
		mGoalNotes.setText(mSelectedGoal.getTitle());

		setPrimaryUIColour();
	}

	/**
	 *  Sets colour of some UI elements to that of the Goal (or Task's goal)
	 */
	public void setPrimaryUIColour() {
		mToolbar.setBackgroundColor(mSelectedGoal.getColourOpaque());
		mAppBarLayout.setBackgroundColor(mSelectedGoal.getColourOpaque());

		if (mItemType.equals(Task.TAG)) {
			((ImageView)mRootView.findViewById(R.id.task_edit_goal_img)).setColorFilter(mSelectedGoal.getColourOpaque());
		} else {
			// Try to find goal colour name (format col_XXXXXX [X = hex])
			((ImageView)mRootView.findViewById(R.id.goal_edit_colour_img)).setColorFilter(mSelectedGoal.getColourOpaque());
			String colString = "col_"+ String.format("%06X",mSelectedGoal.getColour());
			try {
				mGoalColour.setText(getResources().getIdentifier(colString, "string", mContext.getPackageName()));
			} catch (Resources.NotFoundException e) {
				mGoalColour.setText("Goal colour");
			}
		}
	}

	// ------------------------------------------------------------------------
	// DIALOG/POPUP MENU METHODS
	// ------------------------------------------------------------------------

	/**
	 *  Creates popup menu to select goal when adding/editing a task
	 */
	private void createGoalPopupMenu() {
		mTaskGoal.setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View v) {
				PopupMenu popupMenu = new PopupMenu(mContext, mTaskGoal);
				Menu menu = popupMenu.getMenu();

				int currentGoalMenuOrderId = 1;
				for (Goal g: mDb.getAllGoals()) {
					// Creates new drawer menu item with new view id and an order id of 1000+currentMenuId
					MenuItem item = menu.add(R.id.drawer_menu_group_goals, View.generateViewId(), currentGoalMenuOrderId, g.getTitle());

					// Set icon to coloured circle (with goal colour)
					Drawable icon = DrawableCompat.wrap(ResourcesCompat.getDrawable(getResources(), R.drawable.circle_24dp, null));
					DrawableCompat.setTint(icon, g.getColourOpaque());
					item.setIcon(icon);

					handleGoalSelection(item, g.getId());

					// Clones icon drawable - so tint doesn't not re-write all icons
					icon = icon.mutate().getConstantState().newDrawable();
					currentGoalMenuOrderId++;
				}

				menu.add(R.id.drawer_menu_group_goals, R.id.drawer_menu_new_goal, currentGoalMenuOrderId, "Add new goal...")
						.setOnMenuItemClickListener(new MenuItem.OnMenuItemClickListener() {
							@Override
							public boolean onMenuItemClick(MenuItem item) {
								MainActivity.newTaskEditDialog(getFragmentManager(), TAG, Goal.TAG);
								return true;
							}
						});
				popupMenu.show();
			}
		});
	}

	/**
	 *  Handles when a goal is selected when adding/editing a task
	 */
	public void handleGoalSelection(MenuItem item, final int goalId) {
		item.setOnMenuItemClickListener(new MenuItem.OnMenuItemClickListener() {
			@Override
			public boolean onMenuItemClick(MenuItem item) {
				setTaskGoal(goalId);
				return true;
			}
		});
	}

	/**
	 *  Creates popup menu to select due date and time when adding/editing a task
	 */
	private void createDateTimePopupMenus() {
		mTaskDueDate.setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View v) {
				//DateTimePickerListener class at bottom of this file
				Calendar tempDueDate = mSelectedTaskDueDate;
				if (tempDueDate == null) {
					tempDueDate = Calendar.getInstance();
				} else {
					if(tempDueDate.getTimeInMillis() == 0)
						tempDueDate = Calendar.getInstance();
				}
				DateTimePickerListener listener = new DateTimePickerListener();
				new DatePickerDialog(mContext, listener, tempDueDate.get(Calendar.YEAR), tempDueDate.get(Calendar.MONTH), tempDueDate.get(Calendar.DAY_OF_MONTH)).show();
			}
		});

		mTaskDueTime.setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View v) {
				//DateTimePickerListener class at bottom of this file
				Calendar tempDueTime = mSelectedTaskDueTime;
				if (tempDueTime == null) {
					tempDueTime = Calendar.getInstance();
				} else {
					if(tempDueTime.getTimeInMillis() == 0)
						tempDueTime = Calendar.getInstance();
				}
				DateTimePickerListener listener = new DateTimePickerListener();
				new TimePickerDialog(mContext, listener, tempDueTime.get(Calendar.HOUR_OF_DAY), tempDueTime.get(Calendar.MINUTE), false).show();
			}
		});
	}

	/**
	 *  Handles when a due date and time is selected when adding/editing a task
	 */
	class DateTimePickerListener implements TimePickerDialog.OnTimeSetListener, DatePickerDialog.OnDateSetListener {
		@Override
		public void onDateSet(DatePicker view, int year, int month, int dayOfMonth) {
			mSelectedTaskDueDate = Calendar.getInstance();
			mSelectedTaskDueDate.set(Calendar.YEAR, year);
			mSelectedTaskDueDate.set(Calendar.MONTH, month);
			mSelectedTaskDueDate.set(Calendar.DAY_OF_MONTH, dayOfMonth);

			mTaskDueDate.setText(Task.getDueDateString(mSelectedTaskDueDate));
		}

		@Override
		public void onTimeSet(TimePicker view, int hourOfDay, int minute) {
			mSelectedTaskDueTime = Calendar.getInstance();
			mSelectedTaskDueTime.set(Calendar.HOUR_OF_DAY, hourOfDay);
			mSelectedTaskDueTime.set(Calendar.MINUTE, minute);

			mTaskDueTime.setText(Task.getDueTimeString(mSelectedTaskDueTime));
		}
	}

	/**
	 *  Creates popup menu to select colour when adding/editing a goal
	 *  Also handles when a colour is selected
	 */
	private void createColourPickerDialog() {
		final ColorPickerDialog colorPickerDialog = new ColorPickerDialog();
		int[] colArray = mContext.getResources().getIntArray(R.array.colourPickerColourArray);

		colorPickerDialog.initialize(
				R.string.goal_colpicker_title, colArray, mSelectedGoal.getColourOpaque(), 5, colArray.length);

		colorPickerDialog.setOnColorSelectedListener(new ColorPickerSwatch.OnColorSelectedListener() {
			@Override
			public void onColorSelected(int color) {
				mSelectedGoal.setColour(color - 0xFF000000);
				setPrimaryUIColour();
			}
		});

		mGoalColour.setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View v) {
				colorPickerDialog.show(getFragmentManager(), TAG);
			}
		});
	}


	// ------------------------------------------------------------------------
	// RUNTIME METHODS
	// ------------------------------------------------------------------------

	public void saveTask() {
		mSelectedTask.setTitle(mTaskTitle.getText().toString());
		mSelectedTask.setNotes(mTaskNotes.getText().toString());
		mSelectedTask.setDueDate(mSelectedTaskDueDate);
		mSelectedTask.setDueTime(mSelectedTaskDueTime);

		try {
			mSelectedTask.setTaskUrgency(Integer.parseInt(mTaskUrgency.getText().toString()));
		} catch (NumberFormatException e) {
			mSelectedTask.setTaskUrgency(0);
		}

		try {
			mSelectedTask.setImportance(Integer.parseInt(mTaskImportance.getText().toString()));
		} catch (NumberFormatException e) {
			mSelectedTask.setImportance(0);
		}

		if (mSelectedItemId ==-1) {
			mSelectedItemId = mDb.addTask(mSelectedTask);
			Toast.makeText(mContext, "New Task Created", Toast.LENGTH_SHORT).show();
		} else {
			mDb.editTask(mSelectedTask);
			Toast.makeText(mContext, "Task Updated", Toast.LENGTH_SHORT).show();
		}

		mListener.taskListChanged(mSelectedItemId, mInvokedBy);
	}

	public void saveGoal() {
		mSelectedGoal.setTitle(mGoalTitle.getText().toString());
		mSelectedGoal.setNotes(mGoalNotes.getText().toString());
		if (mSelectedItemId ==-1) {
			mSelectedItemId = mDb.addGoal(mSelectedGoal);
			Toast.makeText(mContext, "New Goal Created", Toast.LENGTH_SHORT).show();
		} else {
			mDb.editGoal(mSelectedGoal);
			Toast.makeText(mContext, "Goal Updated", Toast.LENGTH_SHORT).show();
		}

		mListener.goalListChanged(mSelectedItemId, mInvokedBy);
	}

	public void setTaskGoal(int id) {
		mSelectedGoal = mDb.getGoal(id);
		mSelectedTask.setGoal(mSelectedGoal);
		mTaskGoal.setText(mSelectedGoal.getTitle());
		setPrimaryUIColour();
	}
}