package au.edu.utas.todoapp;

import android.content.Context;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v4.content.res.ResourcesCompat;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.PopupMenu;
import android.support.v7.widget.RecyclerView;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import java.util.ArrayList;
import java.util.List;

import static java.lang.Boolean.TRUE;

/**
 * This fragment displays a list of tasks
 */
public class TaskListFragment extends Fragment implements TodoDisplayableItemMenu.OnMenuItemClickedListener {
	public static final String TAG = "TaskListFragment";

	public static final String EXTRA_TASK_LIST_FILTER_FIELDNAME ="au.edu.utas.todoapp.task_list_fragment.filter_fieldname";
	public static final String EXTRA_TASK_LIST_FILTER_FIELDVALUE ="au.edu.utas.todoapp..task_list_fragment.filter_fieldvalue";
	public static final String EXTRA_TASK_LIST_SORT_FIELDNAME="au.edu.utas.todoapp.task_list_fragment.sort_fieldname";
	public static final String EXTRA_TASK_LIST_SORT_FIELDVALUE="au.edu.utas.todoapp.task_list_fragment.sort_fieldvalue";
	public static final String EXTRA_TASK_LIST_SORT_ASC="au.edu.utas.todoapp.task_list_fragment.sort_asc";

	private View mRootView;
	private TodoFragmentListener mListener;
	private Context mContext;

	private TodoDB mDb = null;
	private ArrayList<Task> mTaskList;
	private TasksAdapter mTaskAdapter;
	private RecyclerView mTaskRecyclerView;
	private Toolbar mToolbar;
	private TextView mEmptyTextView;

	private String mFilterFieldName;
	private String mFilterFieldValue;
	private String mSortFieldName;
	private Boolean mSortAsc;

	private Goal mFilterGoal = null;

	/**
	 *  All new instances of this DialogFragment should use this method
	 *  This enforces correct values for extras/arguments
	 */
	public static TaskListFragment newInstance() {return newInstance("", "", "", "", false);}
	public static TaskListFragment newInstance(String filterFieldName, String filterFieldValue, String sortFieldName, String sortFieldValue, Boolean sortAscending) {
		TaskListFragment fragment = new TaskListFragment();
		Bundle args = new Bundle();

		args.putString(EXTRA_TASK_LIST_FILTER_FIELDNAME, filterFieldName);
		args.putString(EXTRA_TASK_LIST_FILTER_FIELDVALUE, filterFieldValue);
		args.putString(EXTRA_TASK_LIST_SORT_FIELDNAME, sortFieldName);
		args.putString(EXTRA_TASK_LIST_SORT_FIELDVALUE, sortFieldValue);
		args.putBoolean(EXTRA_TASK_LIST_SORT_ASC, sortAscending);
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
					+ " must implement OnFragmentInteractionListener");
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
		mFilterFieldName = arguments.getString(EXTRA_TASK_LIST_FILTER_FIELDNAME);
		mFilterFieldValue = arguments.getString(EXTRA_TASK_LIST_FILTER_FIELDVALUE);
		mSortFieldName = arguments.getString(EXTRA_TASK_LIST_SORT_FIELDNAME);
		mSortAsc = arguments.getBoolean(EXTRA_TASK_LIST_SORT_ASC);
		super.onCreate(savedInstanceState);
	}

	/**
	 *  Called to draw fragment UI
	 *  Initialises all UI elements
	 */
	@Override
	public View onCreateView(LayoutInflater inflater, ViewGroup container,
							 Bundle savedInstanceState) {
		mRootView = inflater.inflate(R.layout.fragment_task_list, container, false);
		mToolbar = (Toolbar) getActivity().findViewById(R.id.toolbar);
		mToolbar.getMenu().removeGroup(R.id.menu_task_list_group);
		mToolbar.inflateMenu(R.menu.menu_task_list);

		mDb = new TodoDB(mContext.getApplicationContext());

		mTaskList = new ArrayList<>();

		initTaskList();
		initTaskListRecycleView();

		if (mFilterFieldName.equals(TodoDB.KEY_TASK_GOALID)) {
			initFilterByGoal();
		} else {
			mToolbar.setTitle("All Tasks");
			int toolbarCol = ResourcesCompat.getColor(getResources(), R.color.colorPrimary, null);
			mToolbar.setBackgroundColor(toolbarCol);
		}

		return mRootView;
	}

	@Override
	public void onResume() {
		super.onResume();
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
	 *  Handles all non item related toolbar items
	 *  Currently only the "home" button (which is configured as an "up" button)
	 *  which pops this fragment off the stack (closes the fragment)
	 */
	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		switch (item.getItemId()) {
			case R.id.menu_task_list_show_archived_tasks:
				mFilterFieldName = TodoDB.KEY_TASK_ARCHIVED_DATE;
				mFilterFieldValue = "true";
				refreshTaskListAdapter();
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
	 *
	 *  In this fragment, the only possible actions are edit/delete goal (if a goal filter has been applied)
	 */
	@Override
	public void onTodoItemOptionsMenuItemClick(MenuItem item, TodoDisplayableItem todoItem) {
		switch (item.getItemId()) {
			case R.id.item_popup_menu_edit:
				MainActivity.newTaskEditDialog(getFragmentManager(), TAG, Goal.TAG, mFilterGoal.getId());
				break;
			case R.id.item_popup_menu_delete:
				mListener.goalListChanged(mFilterGoal.getId(), TAG);
				break;
		}
	}

	// ------------------------------------------------------------------------
	// UI INITIALISATION METHODS
	// ------------------------------------------------------------------------

	/**
	 *  Initialises the RecycleView and sets the task list adapter
	 */
	private void initTaskListRecycleView(){
		mTaskRecyclerView = (RecyclerView) mRootView.findViewById(R.id.recycler_view);
		mEmptyTextView = (TextView) mRootView.findViewById(R.id.empty_view);

		// Create adapter passing in the sample user data
		mTaskAdapter = new TasksAdapter(mContext, mTaskList);
		// Attach the adapter to the recyclerview to populate items
		mTaskRecyclerView.setAdapter(mTaskAdapter);
		// Set layout manager to position the items
		mTaskRecyclerView.setLayoutManager(new LinearLayoutManager(mContext));

		// Add horizontal line between each item
		//RecyclerView.ItemDecoration itemDecoration = new
		//DividerItemDecoration(mContext, DividerItemDecoration.VERTICAL);
		//mTaskRecyclerView.addItemDecoration(itemDecoration);

		checkRecyclerViewEmpty();
	}

	// ------------------------------------------------------------------------
	// TASK ADAPTER INITIALISATION METHODS
	// ------------------------------------------------------------------------

	/**
	 *  Initialises and populates Task List
	 *  Triggers filter and sort methods if needed
	 */
	private void initTaskList() {
		// Removes leftover goal related toolbar menu items
		mToolbar.getMenu().removeGroup(R.id.menu_item_options_group);

		mTaskList.clear();
		if (mFilterFieldValue == TodoDB.KEY_TASK_ARCHIVED_DATE) {
			mTaskList.addAll(mDb.getArchivedTasks());
			mFilterFieldValue = mFilterFieldName = "";
		}
		else
			mTaskList.addAll(mDb.getAllActiveTasks());


		if (!mFilterFieldName.equals("")) {
			filterTaskList(mFilterFieldName, mFilterFieldValue);
		}
		if (!mSortFieldName.equals(""))
			sortTaskList(mSortFieldName, mSortAsc);
	}

	/**
	 *  Filters Task list adapter by goal
	 *  Also sets colour of the current activity (through mListener)
	 */
	private void initFilterByGoal (){
		mFilterGoal = mDb.getGoal(Integer.parseInt(mFilterFieldValue));
		if(mFilterGoal == null){
			mListener.gotoAppHome();
		} else {
			mToolbar.setTitle(mFilterGoal.getTitle());

			mListener.updatePrimaryCol(mFilterGoal.getColourOpaque());
			initFilterByGoalToolbarMenu();
		}
	}

	/**
	 *  Sets colour of toolbar to filter goal colour
	 *  Also add goal options menu items to the toolbar (eg edit goal)
	 */
	private void initFilterByGoalToolbarMenu() {
		Menu toolbarMenu = mToolbar.getMenu();
		TodoDisplayableItemMenu.GoalOptionsMenu mToolbarOptionsMenu = new TodoDisplayableItemMenu().new GoalOptionsMenu(mContext, this, mFilterGoal);
		mToolbarOptionsMenu.setMenuItems(toolbarMenu);
	}

	// ------------------------------------------------------------------------
	// RECYCLER VIEW / TASK ADAPTER METHODS
	// ------------------------------------------------------------------------

	/**
	 *  If RecyclerView is empty => View is removed and a message is displayed
	 */
	public void checkRecyclerViewEmpty() {
		if (mTaskList.isEmpty()) {
			mTaskRecyclerView.setVisibility(View.GONE);
			mEmptyTextView.setVisibility(View.VISIBLE);
		}
		else {
			mTaskRecyclerView.setVisibility(View.VISIBLE);
			mEmptyTextView.setVisibility(View.GONE);
		}
	}

	/**
	 *  Filter task list by field name and field value
	 */
	public void filterTaskList(String fieldName, String fieldValue) {
		ArrayList<Task> newTaskList = new ArrayList<>();
		for(Task t:mTaskList) {
			String currentValue = "";
			switch (fieldName) {
				case TodoDB.KEY_TASK_GOALID:
					currentValue = Integer.toString(t.getGoal().getId());
					break;
			}

			if (currentValue.equals(fieldValue)) {
				newTaskList.add(t);
			}
		}
		mTaskList.clear();
		mTaskList.addAll(newTaskList);
	}

	/**
	 *  Sort task list by field name and direction
	 */
	public void sortTaskList(String fieldName) { sortTaskList(fieldName, TRUE); }
	public void sortTaskList(String fieldName, Boolean ascending) {

	}

	/**
	 *  Refresh task list adapter and UI elements
	 */
	public void refreshTaskListAdapter() {
		initTaskList();
		if (mFilterFieldName.equals(TodoDB.KEY_TASK_GOALID))
			if (mDb.getGoal(mFilterGoal.getId()) == null) {
				mListener.gotoAppHome();
			} else {
				initFilterByGoal();
			}
		checkRecyclerViewEmpty();
		mTaskAdapter.notifyDataSetChanged();
	}

	// ------------------------------------------------------------------------
	// TASK LIST ADAPTER
	// ------------------------------------------------------------------------

	/**
	 *	Create the basic adapter extending from RecyclerView.Adapter
	 *  Used to display task items in a RecyclerView
	 *	ViewHolder class is used to control each task in the recyclerview
	 *
	 *	[Adapted from https://github.com/codepath/android_guides/wiki/Using-the-RecyclerView]
	 */
	class TasksAdapter extends
			RecyclerView.Adapter<TasksAdapter.ViewHolder> {

		private List<Task> mTasks;
		private Context mContext;

		public TasksAdapter(Context context, List<Task> tasks) {
			mTasks = tasks;
			mContext = context;
		}

		@Override
		public int getItemViewType(int position) {
			if (mTaskList.get(position).isMarkedCompleted()) {
				return R.layout.task_completed_list_item;
			} else {
				return R.layout.task_list_item;
			}
		}

		/**
		 *  Creates viewholder for a task item
		 */
		@Override
		public TasksAdapter.ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
			//Context context = parent.getContext();
			LayoutInflater inflater = LayoutInflater.from(mContext);

			View taskView = null;
			// Inflate the custom layout
			taskView = inflater.inflate(viewType, parent, false);

			// Return a new holder instance
			ViewHolder viewHolder = new ViewHolder(taskView);
			return viewHolder;
		}

		/**
		 *  Involves populating data into the item through holder
		 */
		@Override
		public void onBindViewHolder(TasksAdapter.ViewHolder viewHolder, int position) {
			// Get the data model based on position
			Task task = mTasks.get(position);

			// Set item views based on your views and data model
			TextView textView = viewHolder.taskTitleView;
			textView.setText(task.getTitle());
			//viewHolder.taskGoalCircle.setColorFilter(task.getGoal().getColourOpaque());

			viewHolder.initTaskOptionsMenu();
		}

		/**
		 *  Returns the total count of items in the list
		 */
		@Override
		public int getItemCount() {
			return mTasks.size();
		}


		// ------------------------------------------------------------------------
		// VIEWHOLDER CLASS - REPRESENTS EACH TASK IN RECYCLER VIEW
		// (initialises and sets views for each task)
		// ------------------------------------------------------------------------


		/**
		 *  This class represents each 'Item' in the list (ie each task)
		 *  It is responsible for handling click events - on the task itself, toggling the options menu...
		 */
		class ViewHolder extends RecyclerView.ViewHolder implements View.OnClickListener, TodoDisplayableItemMenu.OnMenuItemClickedListener {
			// Your holder should contain a member variable
			// for any view that will be set as you render a row
			TodoDisplayableItemMenu.OnMenuItemClickedListener mThisListener;
			TextView taskTitleView;
			TextView taskDueDateView;
			TextView viewOptionsMenu;
			ImageView taskGoalCircle;

			TodoDisplayableItemMenu mTaskPopupOptionsMenu;

			ViewHolder(View itemView) {
				super(itemView);

				mThisListener = this;

				taskTitleView = (TextView) itemView.findViewById(R.id.task_name);
				taskDueDateView = (TextView) itemView.findViewById(R.id.task_due_date);
				viewOptionsMenu = (TextView) itemView.findViewById(R.id.task_options_menu);
				taskGoalCircle = ((ImageView)itemView.findViewById(R.id.task_goal_circle));

				itemView.setOnClickListener(this);
			}

			/**
			 *  Popup options menu when the user presses the "vertical three dots" icon on the right of the list
			 *  Options menu creation and event handling uses TodoDisplayableItemMenu class
			 *  Menu item selection is then passed to onTodoItemOptionsMenuItemClick() method
			 */
			public void initTaskOptionsMenu() {
				viewOptionsMenu.setOnClickListener(new View.OnClickListener() { @Override public void onClick(View view) {
					Task currentTask = mTaskList.get(getAdapterPosition());

					//creating a popup menu
					PopupMenu popup = new PopupMenu(mContext, viewOptionsMenu);
					mTaskPopupOptionsMenu = new TodoDisplayableItemMenu().new TaskOptionsMenu(mContext, mThisListener, currentTask);
					mTaskPopupOptionsMenu.setMenuItems(popup.getMenu());
					popup.setOnMenuItemClickListener(mTaskPopupOptionsMenu);

					// Show popup
					popup.show();
				}});
			}

			/**
			 *  Handles a row (task) click event
			 *  Triggers TaskViewDialogFragment
			 */
			@Override
			public void onClick(View view) {
				int position = getAdapterPosition(); // gets item position
				if (position != RecyclerView.NO_POSITION) { // Check if an item was deleted, but the user clicked it before the UI removed it
					Task selectedTask = mTaskList.get(position);
					MainActivity.newTaskViewDialog(getFragmentManager(), TAG, Task.TAG, selectedTask.getId());

					// We can access the data within the views
					//Toast.makeText(mContext, tvName.getText(), Toast.LENGTH_SHORT).show();
				}
			}

			/**
			 *  Handles all popup menu item selection
			 *  All modification of items are handled by TodoDisplayableItemMenu class (such as edit or delete)
			 *  This method only deals with operations specific to this class (ie refreshing the task list adapter after delete)
			 */
			@Override
			public void onTodoItemOptionsMenuItemClick(MenuItem item, TodoDisplayableItem todoItem) {
				Log.d(TAG, "here");
				switch (item.getItemId()) {
					case R.id.item_popup_menu_edit:
						MainActivity.newTaskEditDialog(getFragmentManager(), TAG, Task.TAG, todoItem.getId());
						break;
					case R.id.item_popup_menu_delete:
						mTaskList.remove(todoItem);
						mTaskAdapter.notifyItemRemoved(getAdapterPosition());
						checkRecyclerViewEmpty();
						break;
					case R.id.item_popup_menu_add_dailyplan:
						break;
					case R.id.item_popup_menu_mark_completed:
						refreshTaskListAdapter();
						break;
					case R.id.item_popup_menu_mark_uncompleted:
						refreshTaskListAdapter();
						break;
					default:
						break;
				}
			}
		}
	}

}
