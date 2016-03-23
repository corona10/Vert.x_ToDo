package todo;

import com.google.gson.annotations.SerializedName;


public class ToDoModel {
	private int id;
	private String title;
	private int order;
	private boolean completed;

	public ToDoModel() {
		super();
	}

	public ToDoModel(String title, int order, boolean completed) {
		super();
		this.title = title;
		this.order = order;
		this.completed = completed;
	}

	public int getId() {
		return id;
	}

	public String getTitle() {
		return title;
	}

	public int getOrder() {
		return order;
	}

	public Boolean isCompleted() {
		return completed;
	}
	
	public void setId(int n)
	{
	  this.id = n;
	}

}
