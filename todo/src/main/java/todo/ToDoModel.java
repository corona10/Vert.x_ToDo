package todo;

import com.j256.ormlite.field.DatabaseField;
import com.j256.ormlite.table.DatabaseTable;

import io.vertx.core.json.JsonObject;

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

  public ToDoModel(JsonObject json) {
    // TODO Auto-generated constructor stub
    if (json.getString("title") != null)
      this.title = json.getString("title");
    if (json.getInteger("order") != null)
      this.order = json.getInteger("order");
    if (json.getBoolean("completed") != null) {
      this.completed = json.getBoolean("completed");
    }
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

}
