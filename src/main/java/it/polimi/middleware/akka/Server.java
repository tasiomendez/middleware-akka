package it.polimi.middleware.akka;

import akka.actor.ActorSystem;
import com.typesafe.config.Config;
import com.typesafe.config.ConfigFactory;
import it.polimi.middleware.akka.api.ClusterApiManager;
import it.polimi.middleware.akka.api.HttpManager;
import it.polimi.middleware.akka.master.Master;
import it.polimi.middleware.akka.node.Node;

public class Server {

    public static void main(String[] args) {

        Config config = ConfigFactory.load();

        final ActorSystem system = ActorSystem.create(config.getString("clustering.name"), config);
        system.actorOf(Node.props(), "node");
        final String role = config.getString("clustering.role");
        if (role.equals("master")) {
            system.actorOf(Master.props(), "master");
        }

        ClusterApiManager clusterManager = ClusterApiManager.get(system);
        HttpManager httpManager = HttpManager.get(system);

        if (config.getBoolean("akka.management.http.enable"))
            httpManager.importRoutes(clusterManager.exportRoutes());

        httpManager.start();
    }
}
