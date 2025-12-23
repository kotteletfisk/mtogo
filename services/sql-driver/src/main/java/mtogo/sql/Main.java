package mtogo.sql;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.fasterxml.jackson.databind.ObjectMapper;

import mtogo.sql.adapter.in.RabbitMQMessageConsumer;
import mtogo.sql.adapter.out.PostgresAuthRepository;
import mtogo.sql.adapter.out.PostgresModelRepository;
import mtogo.sql.adapter.out.RabbitMQMessageProducer;
import mtogo.sql.persistence.SQLConnector;
import mtogo.sql.ports.in.IMessageConsumer;
import mtogo.sql.ports.out.IAuthRepository;
import mtogo.sql.ports.out.IMessageProducer;
import mtogo.sql.ports.out.IModelRepository;

//TIP To <b>Run</b> code, press <shortcut actionId="Run"/> or
// click the <icon src="AllIcons.Actions.Execute"/> icon in the gutter.
public class Main {

    private static final Logger log = LoggerFactory.getLogger(Main.class);

    public static void main(String[] args) {
        log.info("=== Starting SQL Driver ===");

        try {
            ObjectMapper mapper = new ObjectMapper();
            SQLConnector sqlConnector = new SQLConnector();

            IModelRepository modelRepo = new PostgresModelRepository(sqlConnector);
            IAuthRepository authRepo = new PostgresAuthRepository(sqlConnector);

            modelRepo.healthCheck();
            authRepo.healthCheck();


            IMessageConsumer consumer = new RabbitMQMessageConsumer(authRepo, modelRepo, mapper);
            IMessageProducer producer = new RabbitMQMessageProducer(mapper);

            consumer.start();


            log.info("SQL-driver started successfully, listening for messages...");
            log.debug("Debug logging is enabled");

            Thread.currentThread().join();

        } catch (InterruptedException e) {
            log.info("Application interrupted, shutting down...");
            Thread.currentThread().interrupt();
        } catch (Exception e) {
            log.error("FATAL: Failed to start SQL Driver: {}", e.getMessage(), e);
            e.printStackTrace();
            System.exit(1);
        }
    }
}
