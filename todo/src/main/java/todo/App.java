package todo;

import io.vertx.core.Verticle;
import io.vertx.core.Vertx;

public class App {

    public static void main(String[] args) {
        Vertx vertx = Vertx.vertx();
        Verticle verticle = new ToDoVerticle();
        vertx.deployVerticle(verticle);
    }
}