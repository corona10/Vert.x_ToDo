package todo;

import java.sql.SQLException;
import java.util.List;

import com.google.gson.Gson;
import com.j256.ormlite.dao.Dao;
import com.j256.ormlite.dao.DaoManager;
import com.j256.ormlite.stmt.DeleteBuilder;
import com.j256.ormlite.stmt.UpdateBuilder;

import io.vertx.core.AbstractVerticle;
import io.vertx.core.Future;
import io.vertx.core.Vertx;
import io.vertx.core.http.HttpMethod;
import io.vertx.core.http.HttpServerRequest;
import io.vertx.core.http.HttpServerResponse;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.web.Router;
import io.vertx.ext.web.RoutingContext;
import io.vertx.ext.web.handler.BodyHandler;
import io.vertx.ext.web.handler.CorsHandler;
import io.vertx.ext.web.handler.StaticHandler;

public class ToDoVerticle extends AbstractVerticle {

	final static String H2_URL = "jdbc:h2:mem:todo;DB_CLOSE_DELAY=-1";

  @Override
  public void start(Future<Void> startFuture) throws Exception {
    // TODO Auto-generated method stub

    Router router = Router.router(vertx);
    vertx.<String> executeBlocking(future -> {
      String result = null;
      ToDoDatabase.init_db(H2_URL);
      future.complete(result);
    } , res -> { 
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

    vertx.createHttpServer().requestHandler(router::accept).listen(
        config().getInteger("http.port", 8000), result -> {
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

    vertx.<String> executeBlocking(future -> {
      String result = null;
      try {
        Dao<ToDoModel, Integer> todo_dao = DaoManager.createDao(ToDoDatabase.connectionSource, ToDoModel.class);
        List<ToDoModel> todo_list = todo_dao.queryForAll();
        for (int i = 0; i < todo_list.size(); i++) {
          ToDoModel todo = todo_list.get(i);
          JsonObject json = buidJson(todo, routingContext.request().absoluteURI() + todo.getId());
          jsonArray.add(json);
        }
        ToDoDatabase.connectionSource.close();
      } catch (SQLException e) {
        // TODO Auto-generated catch block
        e.printStackTrace();
      }
      result = jsonArray.encodePrettily();

      future.complete(result);
    } , res -> {
      if (res.succeeded()) {
        if (res.result() != null) {
          response.putHeader("content-type", "application/json").end(res.result());
        } else {
          sendError(400, response);
        }
      } else {
        sendError(400, response);
      }
    });
  }

  private void handleGetToDo(RoutingContext routingContext) {
    int entryId = Integer.parseInt(routingContext.request().getParam("entryId"));
    HttpServerResponse response = routingContext.response();

    vertx.<String> executeBlocking(future -> {
      String result = null;
      try {
        Dao<ToDoModel, Integer> todo_dao = DaoManager.createDao(ToDoDatabase.connectionSource, ToDoModel.class);
        ToDoModel todo = todo_dao.queryBuilder().where().eq("id", entryId).queryForFirst();
        if (todo != null) {
          JsonObject json = buidJson(todo, routingContext.request().absoluteURI());
          result = json.encodePrettily();
        } else {
          JsonObject json = new JsonObject();
          result = json.encodePrettily();
        }
        ToDoDatabase.connectionSource.close();
      } catch (SQLException e) {
        // TODO Auto-generated catch block
        e.printStackTrace();
      }
      future.complete(result);
    } , res -> {
      if (res.succeeded()) {
        if (res.result() != null) {
          response.putHeader("content-type", "application/json").end(res.result());
        } else {
          sendError(400, response);
        }
      } else {
        sendError(400, response);
      }
    });
  }

  private void handleAddToDo(RoutingContext routingContext) {
    Gson gson = new Gson();
    HttpServerResponse response = routingContext.response();
    vertx.<String> executeBlocking(future -> {
      String result = null;
      try {
        JsonObject json = new JsonObject(routingContext.getBodyAsString());
        json.remove("id");
        ToDoModel todo = gson.fromJson(json.encodePrettily(), ToDoModel.class);
        Dao<ToDoModel, Integer> todo_dao = DaoManager.createDao(ToDoDatabase.connectionSource, ToDoModel.class);
        todo_dao.create(todo);
        json = buidJson(todo, routingContext.request().absoluteURI() + todo.getId());
        result = json.encodePrettily();
        ToDoDatabase.connectionSource.close();
      } catch (SQLException e) {
        // TODO Auto-generated catch block
        e.printStackTrace();
      }
      future.complete(result);
    } , res -> {
      if (res.succeeded()) {
        if (res.result() != null) {
          response.putHeader("content-type", "application/json").end(res.result());
        } else {
          sendError(400, response);
        }
      } else {
        sendError(400, response);
      }
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
    vertx.<String> executeBlocking(future -> {
      int entryId = Integer.parseInt(routingContext.request().getParam("entryId"));

      String result = null;
      try {
        Dao<ToDoModel, Integer> todo_dao = DaoManager.createDao(ToDoDatabase.connectionSource, ToDoModel.class);
        DeleteBuilder<ToDoModel, Integer> delb = todo_dao.deleteBuilder();
        delb.where().eq("id", entryId);
        todo_dao.delete(delb.prepare());
        ToDoModel todo = todo_dao.queryBuilder().where().eq("id", entryId).queryForFirst();
        ToDoDatabase.connectionSource.close();
        if (todo != null) {
          JsonObject json = buidJson(todo, routingContext.request().absoluteURI());
          result = json.encodePrettily();
        } else {
          JsonObject json = new JsonObject();
          result = json.encodePrettily();
        }
      } catch (Exception e) {
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

  private void handleDeleteAllToDo(RoutingContext routingContext) {
    HttpServerResponse response = routingContext.response();
    JsonArray jsonArray = new JsonArray();

    vertx.<String> executeBlocking(future -> {
      String result = null;
      try {
        Dao<ToDoModel, Integer> todo_dao = DaoManager.createDao(ToDoDatabase.connectionSource, ToDoModel.class);
        DeleteBuilder<ToDoModel, Integer> delb = todo_dao.deleteBuilder();
        todo_dao.delete(delb.prepare());
        List<ToDoModel> todo_list = todo_dao.queryForAll();

        for (int i = 0; i < todo_list.size(); i++) {
          ToDoModel todo = todo_list.get(i);
          JsonObject json = buidJson(todo, routingContext.request().absoluteURI() + todo.getId());
          jsonArray.add(json);
        }
        ToDoDatabase.connectionSource.close();
      } catch (Exception e) {
        e.printStackTrace();
      }
      result = jsonArray.encodePrettily();
      future.complete(result);
    } , res -> {
      if (res.succeeded()) {
        response.putHeader("content-type", "application/json").end(res.result());
      } else {
        sendError(400, response);
      }
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
}
