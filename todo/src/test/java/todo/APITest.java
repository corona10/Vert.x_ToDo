package todo;
import io.vertx.core.DeploymentOptions;

import io.vertx.core.Vertx;
import io.vertx.core.http.HttpClient;
import io.vertx.core.http.HttpServer;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.unit.Async;
import io.vertx.ext.unit.TestContext;
import io.vertx.ext.unit.junit.VertxUnitRunner;

import static org.junit.Assert.*;

import javax.ws.rs.client.ClientBuilder;

import org.junit.After;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.runner.RunWith;

import com.sun.jersey.api.client.Client;
import com.sun.jersey.api.client.ClientResponse;
import com.sun.jersey.api.client.WebResource;


@RunWith(VertxUnitRunner.class)
public class APITest {
	  Vertx vertx;
	  HttpServer server;
	  int port = 8000;

	  @Before
	  public void before(TestContext context) {
	    vertx = Vertx.vertx();
	    DeploymentOptions options = new DeploymentOptions()
	    	    .setConfig(new JsonObject().put("http.port", port)
	    	);
	    vertx.deployVerticle(ToDoVerticle.class.getName(), options,
	    		context.asyncAssertSuccess());
	  }

	  @After
	  public void after(TestContext context) {
	    vertx.close(context.asyncAssertSuccess());
	  }

	  /**
	   * POST / -> register to-do test
	   * GET /:entryId -> get entryId's to-do test
	   * DELETE /:entryId -> delete entryId's to-do test
	   */
	  @Test
	  public void AddAndGetAndDeleteTest(TestContext context) {
		Client client = Client.create();
		WebResource webResource = client.resource("http://localhost:" + port);

		// POST / -> register to-do test
		JsonObject input_json = new JsonObject();
		input_json.put("title", "test")
		          .put("order", 2)
		          .put("completed", false);
		ClientResponse response = webResource.type("application/json").post(ClientResponse.class,
				input_json.encodePrettily());
		assertEquals(response.getStatus(), 200);

		JsonObject output_json = new JsonObject();
		output_json.put("completed", false)
                   .put("order", 2)
		           .put("title", "test");
		
		
		String output = response.getEntity(String.class);
		JsonObject real_output = new JsonObject(output);
		assertEquals(real_output.getValue("title"), output_json.getValue("title"));
		assertEquals(real_output.getValue("order"), output_json.getValue("order"));
		assertEquals(real_output.getValue("completed"), output_json.getValue("completed"));

        // GET /:entryId -> get entryId's to-do test
		String todo_url = real_output.getValue("url").toString();
		String sep[] = todo_url.split("/");
		int request_id = Integer.parseInt(sep[3]);
		output_json.put("url", "http://localhost:"+port+"/"+request_id);
		webResource = client.resource("http://localhost:"+port+"/"+request_id);
		response = webResource.accept("application/json")
                 .get(ClientResponse.class);
		assertEquals(response.getStatus(), 200);
		output = response.getEntity(String.class);
		
		real_output = new JsonObject(output);
		assertEquals(real_output.getValue("title"), output_json.getValue("title"));
		assertEquals(real_output.getValue("order"), output_json.getValue("order"));
		assertEquals(real_output.getValue("completed"), output_json.getValue("completed"));
		assertEquals(real_output.getValue("url"), output_json.getValue("url"));
		
		// DELETE /:entryId -> delete entryId's to-do test
		webResource = client.resource("http://localhost:"+port+"/"+request_id);
		response = webResource.type("application/json").delete(ClientResponse.class);
		assertEquals(response.getStatus(), 200);
		output = response.getEntity(String.class);
		JsonArray real_output_json = new JsonArray(output);
		assertTrue(real_output_json.isEmpty());

	  }
	  /**
	   * DELETE / -> delete all to-do list test
	   * GET / -> get all to-do list test
	   * DELETE / -> delete all to-do list test
	   */
	  @Test
	  public void GetAndDeleteTest(TestContext context) {
			Client client = Client.create();
			WebResource webResource = client
			   .resource("http://localhost:"+ port);

			// DELETE / -> delete all to-do list test
			ClientResponse response = webResource.type("application/json").delete(ClientResponse.class);
			assertEquals(response.getStatus(), 200);
			String output = response.getEntity(String.class);
			JsonArray outputArray = new JsonArray(output);
			assertEquals(outputArray.size(), 0);
			
			// GET / -> get all to-do list test
			response = webResource.accept("application/json")
	                   .get(ClientResponse.class);
			assertEquals(response.getStatus(), 200);
			output = response.getEntity(String.class);
			outputArray = new JsonArray(output);
			assertEquals(outputArray.size(), 0);
			
			JsonObject input_json = new JsonObject();
			input_json.put("title", "test").put("order", 2).put("completed", false);
			response = webResource.type("application/json").post(ClientResponse.class,
					input_json.encodePrettily());
			assertEquals(response.getStatus(), 200);
			input_json = new JsonObject();
			input_json.put("title", "hello").put("order", 2).put("completed", false);
			response = webResource.type("application/json").post(ClientResponse.class,
					input_json.encodePrettily());
			assertEquals(response.getStatus(), 200);
			response = webResource.accept("application/json")
	                   .get(ClientResponse.class);
			assertEquals(response.getStatus(), 200);
			output = response.getEntity(String.class);
			outputArray = new JsonArray(output);
			assertEquals(outputArray.size(), 2);
			
			// DELETE / -> delete all to-do list test
			response = webResource.type("application/json").delete(ClientResponse.class);
			assertEquals(response.getStatus(), 200);
			output = response.getEntity(String.class);
			outputArray = new JsonArray(output);
			assertEquals(outputArray.size(), 0);
	  }
}
  