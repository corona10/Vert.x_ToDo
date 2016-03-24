package todo;
import io.vertx.core.json.JsonObject;

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

  public ToDoModel(JsonObject json) {
    // TODO Auto-generated constructor stub
    if(json.getString("title") != null)
      this.title = json.getString("title");
    if(json.getInteger("order") != null)
      this.order = json.getInteger("order");
    if(json.getBoolean("completed") != null)
    {
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

  public void setId(int n) {
    this.id = n;
  }

}
