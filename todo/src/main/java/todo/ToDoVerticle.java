package todo;

import java.sql.SQLException;
import java.util.List;
import java.util.stream.Collectors;

import com.google.gson.Gson;
import com.j256.ormlite.dao.Dao;
import com.j256.ormlite.dao.DaoManager;
import com.j256.ormlite.stmt.DeleteBuilder;
import com.j256.ormlite.stmt.UpdateBuilder;

import io.vertx.core.AbstractVerticle;
import io.vertx.core.AsyncResult;
import io.vertx.core.Future;
import io.vertx.core.Handler;
import io.vertx.core.Vertx;
import io.vertx.core.http.HttpMethod;
import io.vertx.core.http.HttpServerRequest;
import io.vertx.core.http.HttpServerResponse;
import io.vertx.core.json.Json;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.jdbc.JDBCClient;
import io.vertx.ext.sql.SQLConnection;
import io.vertx.ext.sql.UpdateResult;
import io.vertx.ext.web.Router;
import io.vertx.ext.web.RoutingContext;
import io.vertx.ext.web.handler.BodyHandler;
import io.vertx.ext.web.handler.CorsHandler;
import io.vertx.ext.web.handler.StaticHandler;

public class ToDoVerticle extends AbstractVerticle {

  final static String H2_URL = "jdbc:h2:mem:todo;DB_CLOSE_DELAY=-1";
  private JDBCClient jdbc;
  @Override
  public void start(Future<Void> startFuture) throws Exception {
    // TODO Auto-generated method stub

    Integer port = 8000;
    String ip = "localhost";
    String oepnshift = System.getenv("OPENSHIFT_VERTX_IP");
    if (oepnshift != null) {
        port = Integer.getInteger("http.port");
        ip = System.getProperty("http.address");
    }
    Router router = Router.router(vertx);
    jdbc = JDBCClient.createShared(vertx, new JsonObject()
        .put("url", H2_URL)
        .put("driver_class", "org.hsqldb.jdbcDriver")
        .put("max_pool_size", 30));
    
    jdbc.getConnection(conn ->{
      if(conn.failed())
      {
        return ;
      }
      final SQLConnection connection = conn.result();
      connection.execute(
          "CREATE TABLE IF NOT EXISTS "
          + "`todo` (`id` INTEGER AUTO_INCREMENT , `title` VARCHAR(255) , "
          + "`order` INTEGER , `completed` TINYINT(1) , PRIMARY KEY (`id`) ) ",
          create -> {
            if (create.failed()) {
              System.out.println("Can not create table");
              connection.close();
              return;
            }
            router.route().handler(BodyHandler.create());
            router.route()
                .handler(CorsHandler.create("*")
                .allowedMethod(HttpMethod.GET)
                .allowedMethod(HttpMethod.POST)
                .allowedMethod(HttpMethod.DELETE)
                .allowedMethod(HttpMethod.PATCH)
                .allowedHeader("X-PINGARUNER")
                .allowedHeader("Content-Type")
                .allowedHeader("Access-Control-Allow-Origin"));

            router.get("/").handler(this::handleGetAllToDo);
            router.post("/").handler(this::handleAddToDo);
            router.delete("/").handler(this::handleDeleteAllToDo);

            router.get("/:entryId").handler(this::handleGetToDo);
            router.patch("/:entryId").handler(this::handleModifyToDo);
            router.delete("/:entryId").handler(this::handleDeleteToDo);
          });
    });

    vertx.createHttpServer().requestHandler(router::accept).listen(port, ip, result -> {
      if (result.succeeded()) {
        startFuture.complete();
      } else {
        startFuture.fail(result.cause());
      }
    });
  }

  private void handleGetAllToDo(RoutingContext routingContext) {
    HttpServerResponse response = routingContext.response();
    JsonArray jsonArray = new JsonArray();
    jdbc.getConnection(ar ->{
      SQLConnection connection = ar.result();
      connection.query("SELECT * FROM todo", rs ->{
        List<JsonObject> todo_list = rs.result().getRows();
        for(int i = 0; i < todo_list.size(); i++)
        {
          JsonObject json = todo_list.get(i);
          int id = json.getInteger("ID");
          buildJson2(json, routingContext.request().absoluteURI() + id);
          jsonArray.add(json);
        }
        response.putHeader("content-type", "application/json").end(jsonArray.encodePrettily());
        connection.close();
      });
    });
  }

  private void handleGetToDo(RoutingContext routingContext) {
    int entryId = Integer.parseInt(routingContext.request().getParam("entryId"));
    HttpServerResponse response = routingContext.response();
      jdbc.getConnection(ar -> {
        SQLConnection connection = ar.result(); 
        select(entryId, connection, result -> {
          JsonObject json = result.result();
          JsonArray jsonArray = new JsonArray();
          if(json != null)
          {
            buildJson2(json, routingContext.request().absoluteURI());
          }else{
            json = new JsonObject();
          }
          jsonArray.add(json);
          response.putHeader("content-type", "application/json").end(json.encodePrettily());
          connection.close();
        });
      });
  }

  private void handleAddToDo(RoutingContext routingContext) {
    Gson gson = new Gson();
    HttpServerResponse response = routingContext.response();
    jdbc.getConnection(ar -> {
      JsonObject json = new JsonObject(routingContext.getBodyAsString());
      ToDoModel todo = gson.fromJson(json.encodePrettily(), ToDoModel.class);
      SQLConnection connection = ar.result();
      insert(todo, connection, (r)->{
        ToDoModel response_todo = r.result();
        JsonObject result_json = buidJson(response_todo, routingContext.request().absoluteURI() + response_todo.getId());
        response.putHeader("content-type", "application/json").end(result_json.encodePrettily());
      });
    });
  }

  private void handleModifyToDo(RoutingContext routingContext) {
    int entryId = Integer.parseInt(routingContext.request().getParam("entryId"));
    HttpServerResponse response = routingContext.response();
    vertx.<String> executeBlocking(future -> {
      String result = null;
      try {
        Dao<ToDoModel, Integer> todo_dao = DaoManager.createDao(ToDoDatabase.connectionSource, ToDoModel.class);
        UpdateBuilder<ToDoModel, Integer> updb = todo_dao.updateBuilder();
        JsonObject json = new JsonObject(routingContext.getBodyAsString());
        updb.where().eq("id", entryId);
        if (json.getValue("title") != null) {
          updb.updateColumnValue("title", json.getValue("title"));
        }

        if (json.getValue("order") != null) {
          updb.updateColumnValue("order", json.getValue("order"));
        }

        if (json.getValue("completed") != null) {
          updb.updateColumnValue("completed", json.getValue("completed"));
        }
        updb.update();
        ToDoModel todo = todo_dao.queryBuilder().where().eq("id", entryId).queryForFirst();
        ToDoDatabase.connectionSource.close();
        JsonObject result_json = null;
        if (todo != null) {
          result_json = buidJson(todo, routingContext.request().absoluteURI());
          result = result_json.encodePrettily();
        } else {
          result_json = new JsonObject();
          result = result_json.encodePrettily();
        }
      } catch (SQLException e) {
        // TODO Auto-generated catch block
        e.printStackTrace();
      }
      future.complete(result);
    } , res -> {
      if (res.succeeded()) {
        response.putHeader("content-type", "application/json").end(res.result());
      } else {
        sendError(400, response);
      }
    });
  }

  private void handleDeleteToDo(RoutingContext routingContext) {
    HttpServerResponse response = routingContext.response();
    int entryId = Integer.parseInt(routingContext.request().getParam("entryId"));
    JsonArray jsonArray = new JsonArray();
    jdbc.getConnection(ar -> {
      SQLConnection connection = ar.result();
      connection.queryWithParams("DELETE FROM todo WHERE id = ?",
          new JsonArray().add(entryId), (rs) ->{
        response.putHeader("content-type", "application/json").end(jsonArray.encodePrettily());
      });
    });
  }

  private void handleDeleteAllToDo(RoutingContext routingContext) {
    HttpServerResponse response = routingContext.response();
    JsonArray jsonArray = new JsonArray();
    jdbc.getConnection(ar -> {
      SQLConnection connection = ar.result();
      connection.execute("DELETE FROM todo", rs ->{
        response.putHeader("content-type", "application/json").end(jsonArray.encodePrettily());
      });
    });
  }

  private void sendError(int statusCode, HttpServerResponse response) {
    response.setStatusCode(statusCode).end();
  }

  private JsonObject buidJson(ToDoModel model, String url) {
    JsonObject json = new JsonObject();
    json.put("completed", model.isCompleted());
    if (model.getOrder() != 0) {
      json.put("order", model.getOrder());
    }
    json.put("title", model.getTitle());
    json.put("url", url);
    return json;
  }
  
  private JsonObject buildJson2(JsonObject json, String url)
  {
    int id = json.getInteger("ID");
    json.put("title", json.getString("TITLE"));
    json.put("order", json.getInteger("ORDER"));
    boolean iscompleted = true;
    if(json.getInteger("COMPLETED") == 0)
    {
      iscompleted = false;
    }
    json.put("completed", iscompleted);
    json.put("url",  url);
    json.remove("ID");
    json.remove("TITLE");
    json.remove("ORDER");
    json.remove("COMPLETED");
    json.remove("id");
    return json;
  }
  private void update(int id, SQLConnection connection,Handler<AsyncResult<JsonObject>> handler)
  {
    //String sql = "UPDATE todo SET t"
  }
  private void select(int id, SQLConnection connection, Handler<AsyncResult<JsonObject>> handler) {
    String sql = "SELECT * FROM todo WHERE id = ?";
    connection.queryWithParams(sql,
        new JsonArray().add(id),
        (ar) -> {
          if (ar.failed()) {
            handler.handle(Future.failedFuture(ar.cause()));
            connection.close();
            return;
          }else{
            if(ar.result().getNumRows() > 0)
            {
              JsonObject obj = ar.result().getRows().get(0);
              handler.handle(Future.succeededFuture(obj));
            }else{
              JsonObject obj = null;
              handler.handle(Future.succeededFuture(obj));
            }
          }
        });
  }
  
  private void insert(ToDoModel todo, SQLConnection connection, Handler<AsyncResult<ToDoModel>> next) {
    String sql = "INSERT INTO `todo` (`title` ,`order` ,`completed` ) VALUES (?,?,?)";
    connection.updateWithParams(sql,
        new JsonArray().add(todo.getTitle())
                       .add(todo.getOrder())
                       .add(todo.isCompleted()), (ar) -> {
          if (ar.failed()) {
            next.handle(Future.failedFuture(ar.cause()));
            connection.close();
            return;
          }
          UpdateResult result = ar.result();
          todo.setId(result.getKeys().getInteger(0));
          next.handle(Future.succeededFuture(todo));
        });
  } 
}

