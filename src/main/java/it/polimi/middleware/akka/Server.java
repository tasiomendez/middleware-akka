package it.polimi.middleware.akka;

import com.typesafe.config.Config;
import com.typesafe.config.ConfigFactory;

import akka.actor.ActorSystem;
import it.polimi.middleware.akka.api.HttpManager;
import it.polimi.middleware.akka.api.ClusterApiManager;
import it.polimi.middleware.akka.node.Node;

public class Server {

	public static void main(String[] args) {
		
		Config config = ConfigFactory.load();

		// Override the configuration of the port
//		final Config config = ConfigFactory //
//				.parseFile(new File("resources/application.conf")) //
//				.withValue("akka.remote.netty.tcp.port", ConfigValueFactory.fromAnyRef("9000"));
		

		final ActorSystem system = ActorSystem.create(config.getString("clustering.name"), config);
		system.actorOf(Node.props(), "node");
		
		ClusterApiManager clusterManager = ClusterApiManager.get(system);
		HttpManager httpManager = HttpManager.get(system);
		
		if (config.getBoolean("akka.management.http.enable"))
			httpManager.importRoutes(clusterManager.exportRoutes());
		
		httpManager.start();

	}
}
