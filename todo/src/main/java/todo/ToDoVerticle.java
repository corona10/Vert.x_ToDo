package todo;

import java.util.List;

import com.google.gson.Gson;


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

  final static String H2_URL = "jdbc:h2:mem:todojdbc;DB_CLOSE_DELAY=-1";
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
        .put("driver_class", "org.h2.Driver")
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
          buildJson(json, routingContext.request().absoluteURI() + id);
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
            buildJson(json, routingContext.request().absoluteURI());
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
        JsonObject result_json = buildJson(response_todo, routingContext.request().absoluteURI() + response_todo.getId());
        response.putHeader("content-type", "application/json").end(result_json.encodePrettily());
        connection.close();
      });
    });
  }

  private void handleModifyToDo(RoutingContext routingContext) {
    int entryId = Integer.parseInt(routingContext.request().getParam("entryId"));
    HttpServerResponse response = routingContext.response();
    JsonObject json = new JsonObject(routingContext.getBodyAsString());
    jdbc.getConnection(ar ->{
      String update_col = null;
      if (json.getValue("title") != null) {
        update_col = "title";
      }

      if (json.getValue("order") != null) {
        update_col = "order";
      }

      if (json.getValue("completed") != null) {
        update_col = "completed";
      }
      SQLConnection connection = ar.result();
      update(entryId, update_col, json, connection, (rs) -> {
        
      select(rs.result(), connection, result -> {
        JsonObject rs_json = result.result();
        JsonArray jsonArray = new JsonArray();
        buildJson(rs_json, routingContext.request().absoluteURI());
        jsonArray.add(json);
        response.putHeader("content-type", "application/json").end(rs_json.encodePrettily());
        connection.close();
      });
      });
    });
  }

  private void handleDeleteToDo(RoutingContext routingContext) {
    HttpServerResponse response = routingContext.response();
    int entryId = Integer.parseInt(routingContext.request().getParam("entryId"));
    JsonArray jsonArray = new JsonArray();
    jdbc.getConnection(ar -> {
      SQLConnection connection = ar.result();
      connection.execute("DELETE FROM `todo` WHERE `id` = " +entryId, (rs) ->{
        response.putHeader("content-type", "application/json").end(jsonArray.encodePrettily());
        connection.close();
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
        connection.close();
      });
    });
  }

  private void sendError(int statusCode, HttpServerResponse response) {
    response.setStatusCode(statusCode).end();
  }

  private JsonObject buildJson(ToDoModel model, String url) {
    JsonObject json = new JsonObject();
    json.put("completed", model.isCompleted());
    if (model.getOrder() != 0) {
      json.put("order", model.getOrder());
    }
    json.put("title", model.getTitle());
    json.put("url", url);
    return json;
  }
  
  private JsonObject buildJson(JsonObject json, String url)
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
  
  private void update(int id, String col, JsonObject json, SQLConnection connection,Handler<AsyncResult<Integer>> handler)
  {
    if(json.getValue("title") != null && json.getValue("order") == null && json.getValue("completed") == null)
    {
      String sql = "UPDATE `todo` SET `title` = ? WHERE `id` = ? ";
      connection.updateWithParams(sql,new JsonArray()
                                        .add(json.getString("title"))
                                        .add(id),  update ->{
        if (update.failed()) {
          handler.handle(Future.failedFuture("update faild"));
          return;
        }
        update.result().getUpdated();
        handler.handle(Future.succeededFuture(id));
        connection.close();
      }); 
    }
    if(json.getValue("title") != null && json.getValue("order") != null && json.getValue("completed") == null)
    {
      String sql = "UPDATE `todo` SET `title` = ?, `order` = ?  WHERE `id` = ? ";
      connection.updateWithParams(sql,new JsonArray()
                                        .add(json.getString("title"))
                                        .add(json.getInteger("order"))
                                        .add(id),  update ->{
        if (update.failed()) {
          handler.handle(Future.failedFuture("update faild"));
          return;
        }
        update.result().getUpdated();
        handler.handle(Future.succeededFuture(id));
        connection.close();
      }); 
    }
    if(json.getValue("title") != null && json.getValue("order") == null && json.getValue("completed") != null)
    {
      String sql = "UPDATE `todo` SET `title` = ?, `completed` = ? WHERE `id` = ? ";
      connection.updateWithParams(sql,new JsonArray()
                                        .add(json.getString("title"))
                                        .add(json.getBoolean("completed"))
                                        .add(id),  update ->{
        if (update.failed()) {
          handler.handle(Future.failedFuture("update faild"));
          return;
        }
        update.result().getUpdated();
        handler.handle(Future.succeededFuture(id));
        connection.close();
      }); 
    }
    
    if(json.getValue("title") != null && json.getValue("order") != null && json.getValue("completed") != null)
    {
      String sql = "UPDATE `todo` SET `title` = ?, `completed` = ?, `order` = ? WHERE `id` = ? ";
      connection.updateWithParams(sql,new JsonArray()
                                        .add(json.getString("title"))
                                        .add(json.getBoolean("completed"))
                                        .add(json.getInteger("order"))
                                        .add(id),  update ->{
        if (update.failed()) {
          handler.handle(Future.failedFuture("update faild"));
          return;
        }
        update.result().getUpdated();
        handler.handle(Future.succeededFuture(id));
        connection.close();
      }); 
    }
    
    if(json.getValue("title") == null && json.getValue("order") != null && json.getValue("completed") == null)
    {
      String sql = "UPDATE `todo` SET `order` = ? WHERE `id` = ?";
    connection.updateWithParams(sql,new JsonArray()
                                        .add(json.getInteger(col))
                                        .add(id), update ->{
        if (update.failed()) {
          handler.handle(Future.failedFuture("update failed"));
          return;
        }
        update.result().getUpdated();
        handler.handle(Future.succeededFuture(id));
        connection.close();
      });
    }
    if (json.getValue("title") == null && json.getValue("order") == null && json.getValue("completed") != null) {
      String sql = "UPDATE `todo` SET `completed` = ? WHERE `id` = ? ";
      connection.updateWithParams(sql, new JsonArray().add(json.getBoolean(col)).add(id), update -> {
        if (update.failed()) {
          handler.handle(Future.failedFuture("completed failed"));
          return;
        }
        update.result().getUpdated();
        handler.handle(Future.succeededFuture(id));
        connection.close();
      });
    }
    if (json.getValue("title") == null && json.getValue("order") != null && json.getValue("completed") != null) {
      String sql = "UPDATE `todo` SET `completed` = ?, `order` = ? WHERE `id` = ? ";
      connection.updateWithParams(sql, new JsonArray()
                                           .add(json.getBoolean(col))
                                           .add(json.getInteger(col))
                                           .add(id), update -> {
        if (update.failed()) {
          handler.handle(Future.failedFuture("completed failed"));
          return;
        }
        update.result().getUpdated();
        handler.handle(Future.succeededFuture(id));
        connection.close();
      });
    }
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
          connection.close();
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
          connection.close();
        });
  } 
}

