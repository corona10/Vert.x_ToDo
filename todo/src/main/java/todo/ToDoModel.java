package todo;

import com.google.gson.annotations.SerializedName;
import com.j256.ormlite.field.DatabaseField;
import com.j256.ormlite.table.DatabaseTable;

@DatabaseTable(tableName = "todo")
public class ToDoModel {
	@DatabaseField(allowGeneratedIdInsert = true, generatedId = true, columnName = "id")
	private int id;
	@DatabaseField
	private String title;
	@DatabaseField
	private int order;
	@DatabaseField
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
