package au.edu.utas.todoapp;

import android.app.FragmentTransaction;
//import android.app.Fragment;
import android.support.v4.app.Fragment;
import android.content.Context;
import android.content.DialogInterface;
import android.os.Bundle;
import android.support.v4.content.res.ResourcesCompat;
import android.support.v7.app.AlertDialog;
import android.support.v7.widget.DividerItemDecoration;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.PopupMenu;
import android.support.v7.widget.RecyclerView;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import android.widget.Toast;

import java.util.ArrayList;
import java.util.List;

import static java.lang.Boolean.TRUE;

/**
 * A placeholder fragment containing a simple view.
 */
public class TaskListFragment extends Fragment {
	public static final String TAG = "TaskListFragment";

	public static final String EXTRA_TASK_LIST_FILTER_FIELDNAME ="au.edu.utas.todoapp.task_list_fragment.filter_fieldname";
	public static final String EXTRA_TASK_LIST_FILTER_FIELDVALUE ="au.edu.utas.todoapp..task_list_fragment.filter_fieldvalue";
	public static final String EXTRA_TASK_LIST_SORT_FIELDNAME="au.edu.utas.todoapp.task_list_fragment.sort_fieldname";
	public static final String EXTRA_TASK_LIST_SORT_FIELDVALUE="au.edu.utas.todoapp.task_list_fragment.sort_fieldvalue";
	public static final String EXTRA_TASK_LIST_SORT_ASC="au.edu.utas.todoapp.task_list_fragment.sort_asc";

	private View mRootView;
	private TaskListFragment.OnFragmentInteractionListener mListener;
	private Context mContext;

	private TodoDB mDb = null;
	private ArrayList<Task> mTaskList;
	private TasksAdapter mTaskAdapter;
	private RecyclerView mTaskRecyclerView;
	private TextView mEmptyTextView;
	private ArrayList<Goal> mGoalList;

	public TaskListFragment() {
	}

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

	// Called when a fragment is first attached to its context. onCreate(Bundle) will be called after this.
	@Override
	public void onAttach(Context context) {
		super.onAttach(context);
		if (context instanceof TaskListFragment.OnFragmentInteractionListener) {
			mListener = (TaskListFragment.OnFragmentInteractionListener) context;
		} else {
			throw new RuntimeException(context.toString()
					+ " must implement OnFragmentInteractionListener");
		}
		mContext = context;
		mDb = new TodoDB(context.getApplicationContext());
	}

	// Called to do initial creation of a fragment.
	// This is called after onAttach(Activity) and before onCreateView(LayoutInflater, ViewGroup, Bundle),
	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
	}

	// Called to have the fragment instantiate its user interface view.
	// If you return a View from here, you will later be called in onDestroyView() when the view is being released.
	@Override
	public View onCreateView(LayoutInflater inflater, ViewGroup container,
							 Bundle savedInstanceState) {
		mRootView = inflater.inflate(R.layout.fragment_task_list, container, false);
		Toolbar toolbar = (Toolbar) getActivity().findViewById(R.id.toolbar);

		Bundle arguments = getArguments();
		String filterFieldName = arguments.getString(EXTRA_TASK_LIST_FILTER_FIELDNAME);
		String filterFieldValue = arguments.getString(EXTRA_TASK_LIST_FILTER_FIELDVALUE);
		String sortFieldName = arguments.getString(EXTRA_TASK_LIST_SORT_FIELDNAME);
		String sortFieldValue = arguments.getString(EXTRA_TASK_LIST_SORT_FIELDVALUE);
		Boolean sortAsc = arguments.getBoolean(EXTRA_TASK_LIST_SORT_ASC);

		mDb = new TodoDB(mContext.getApplicationContext());

		mGoalList = mDb.getAllGoals();
		mTaskList = mDb.getAllTasks();

		initTaskListRecycleView();

		// Filter Tasks
		if (!filterFieldName.equals("")) {
			filterTaskList(filterFieldName, filterFieldValue);
			if (filterFieldName.equals(TodoDB.KEY_TASK_GOALID)) {
				if (toolbar != null) {
					Goal g = mDb.getGoal(Integer.parseInt(filterFieldValue));
					toolbar.setTitle(g.getTitle());

					mListener.updatePrimaryCol(g.getColourOpaque());
				}
			}
		} else {
			if (toolbar != null) {
				toolbar.setTitle("All Tasks");
				int toolbarCol = ResourcesCompat.getColor(getResources(), R.color.colorPrimary, null);
				// For status bar
				//getActivity().getWindow().setStatusBarColor(0xff000000 + mSelectedTask.getGoal().getColour());
				toolbar.setBackgroundColor(toolbarCol);
			}
		}

		return mRootView;
	}

	@Override
	public void onResume() {
		super.onResume();
	}

	//Called when the fragment is no longer in use. This is called after onStop() and before onDetach().
	@Override
	public void onDestroy() {
		super.onDestroy();
		mDb.close(); // Close the database to avoid memory leaks.
		mListener = null;
	}

	// This creates and initialises the Task "list" (not ListView, it is a RecycleView)
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
		RecyclerView.ItemDecoration itemDecoration = new
				DividerItemDecoration(mContext, DividerItemDecoration.VERTICAL);
		mTaskRecyclerView.addItemDecoration(itemDecoration);

		checkRecyclerViewEmpty();
			//adapter.notifyItemInserted(0);
			//adapter.notifyItemRemvoed/Changed...
			//adapter.notifydatasetchanged (last resort)
	}

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

	public void refreshTaskListAdapter() {
		mGoalList.clear();
		mGoalList.addAll(mDb.getAllGoals());

		mTaskList.clear();
		mTaskList.addAll(mDb.getAllTasks());

		checkRecyclerViewEmpty();
		mTaskAdapter.notifyDataSetChanged();
	}

	public void filterTaskList(String fieldName, String fieldValue) {
		ArrayList<Task> newTaskList = new ArrayList<>();
		for(Task t:mTaskList) {
			String currentValue = "";
			switch (fieldName) {
				case TodoDB.KEY_TASK_GOALID:
					currentValue = Integer.toString(t.getGoal().getId());
			}

			if (currentValue.equals(fieldValue)) {
				newTaskList.add(t);
			}
		}
		mTaskList.clear();
		mTaskList.addAll(newTaskList);
		mTaskAdapter.notifyDataSetChanged();
	}

	public void sortTaskList(String fieldName) { sortTaskList(fieldName, TRUE); }
	public void sortTaskList(String fieldName, Boolean ascending) {

	}

	private void newTaskViewDialog(Task t) {
		TaskViewDialogFragment newFragment = TaskViewDialogFragment.newInstance(TAG, Task.TAG, t.getId());
		getFragmentManager().beginTransaction().setTransition(FragmentTransaction.TRANSIT_FRAGMENT_OPEN)
				.add(android.R.id.content, newFragment, TaskViewDialogFragment.TAG).addToBackStack(null).commit();
	}

	private void newTaskEditDialog(Task t) {
		TaskEditDialogFragment newFragment = TaskEditDialogFragment.newInstance(TAG, Task.TAG,  t.getId());
		getFragmentManager().beginTransaction().setTransition(FragmentTransaction.TRANSIT_FRAGMENT_OPEN)
				.add(android.R.id.content, newFragment, TaskEditDialogFragment.TAG).addToBackStack(null).commit();
	}


	// Adapted from https://github.com/codepath/android_guides/wiki/Using-the-RecyclerView
	// Create the basic adapter extending from RecyclerView.Adapter
	// Note that we specify the custom ViewHolder which gives us access to our views
	class TasksAdapter extends
			RecyclerView.Adapter<TasksAdapter.ViewHolder> {

		private List<Task> mTasks;
		// Store the context for easy access
		private Context mContext;

		public TasksAdapter(Context context, List<Task> tasks) {
			mTasks = tasks;
			mContext = context;
		}

		// Usually involves inflating a layout from XML and returning the holder
		@Override
		public TasksAdapter.ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
			//Context context = parent.getContext();
			LayoutInflater inflater = LayoutInflater.from(mContext);

			// Inflate the custom layout
			View taskView = inflater.inflate(R.layout.task_list_item, parent, false);

			// Return a new holder instance
			ViewHolder viewHolder = new ViewHolder(taskView);
			return viewHolder;
		}

		// Involves populating data into the item through holder
		@Override
		public void onBindViewHolder(TasksAdapter.ViewHolder viewHolder, int position) {
			// Get the data model based on position
			Task task = mTasks.get(position);

			// Set item views based on your views and data model
			TextView textView = viewHolder.taskTitleView;
			textView.setText(task.getTitle());
		}

		// Returns the total count of items in the list
		@Override
		public int getItemCount() {
			return mTasks.size();
		}


		//This class represents each 'Item' in the list (ie each task)
		// It is responsible for handling click events - on the task itself, toggling the options menu...
		class ViewHolder extends RecyclerView.ViewHolder implements View.OnClickListener {
			// Your holder should contain a member variable
			// for any view that will be set as you render a row
			TextView taskTitleView;
			TextView taskDueDateView;
			TextView viewOptionsMenu;

			// We also create a constructor that accepts the entire item row
			// and does the view lookups to find each subview
			ViewHolder(View itemView) {
				// Stores the itemView in a public final member variable that can be used
				// to access the context from any ViewHolder instance.
				super(itemView);

				taskTitleView = (TextView) itemView.findViewById(R.id.task_name);
				taskDueDateView = (TextView) itemView.findViewById(R.id.task_due_date);
				viewOptionsMenu = (TextView) itemView.findViewById(R.id.task_options_menu);

				itemView.setOnClickListener(this);
				initTaskOptionsMenu();
			}

			// Adapted from https://www.javatpoint.com/android-popup-menu-example
			// When the user presses the "vertical three dots" icon on the right of the list
			// Create a new popup menu, and handle click events of each menu item
			private void initTaskOptionsMenu() {
				viewOptionsMenu.setOnClickListener(new View.OnClickListener() {
										@Override public void onClick(View view) {

					//creating a popup menu
					final PopupMenu popup = new PopupMenu(mContext, viewOptionsMenu);
					//inflating menu from xml resource
					popup.inflate(R.menu.menu_task_options);

					// Handle click events of all menu items
					popup.setOnMenuItemClickListener(new PopupMenu.OnMenuItemClickListener() {
											@Override public boolean onMenuItemClick(MenuItem item) {

						if (getAdapterPosition() != RecyclerView.NO_POSITION) {// Check if an item was deleted, but the user clicked it before the UI removed it
							// Get the task for the options menu
							final Task selectedTask = mTaskList.get(getAdapterPosition());

							switch (item.getItemId()) {
							case R.id.task_options_menu_mark_completed:
								break;
							case R.id.task_options_menu_add_to_daily_planner:
								break;
							case R.id.task_options_menu_edit:
								newTaskEditDialog(selectedTask);

								break;
							case R.id.task_options_menu_delete:
								// Create delete confirm dialog box
								AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());

								builder.setMessage("Are you sure you want to delete " + selectedTask.getTitle() + "?")
										.setTitle("Confirm Delete");

								builder.setPositiveButton("Delete", new DialogInterface.OnClickListener() {
									public void onClick(DialogInterface dialog, int id) {
										mDb.removeTask(selectedTask.getId());
										mTaskList.clear();
										mTaskList.addAll(mDb.getAllTasks());

										notifyItemRemoved(getAdapterPosition());
										checkRecyclerViewEmpty();

										Toast.makeText(mContext, "Task Deleted", Toast.LENGTH_SHORT).show();
									}
								});

								builder.setNegativeButton("Cancel", new DialogInterface.OnClickListener() {
									public void onClick(DialogInterface dialog, int id) {
										dialog.dismiss();
									}
								});

								AlertDialog dialog = builder.create();
								dialog.show();

								break;
							}
						}
						return false;
					}});

					// Show popup
					popup.show();
				}});
			}

			// Handles a row (task) click event
			// Triggers TaskView DialogFragment
			@Override
			public void onClick(View view) {
				int position = getAdapterPosition(); // gets item position
				if (position != RecyclerView.NO_POSITION) { // Check if an item was deleted, but the user clicked it before the UI removed it
					Task selectedTask = mTaskList.get(position);
					newTaskViewDialog(selectedTask);

					// We can access the data within the views
					//Toast.makeText(mContext, tvName.getText(), Toast.LENGTH_SHORT).show();
				}
			}
		}
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
	interface OnFragmentInteractionListener {
		void updatePrimaryCol(int col);
		void resetPrimaryCol();
		// TODO: Update argument type and name
	}

}
