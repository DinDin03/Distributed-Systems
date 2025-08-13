import org.junit.jupiter.api.*;
import java.rmi.registry.Registry;
import java.time.LocalDate;
import java.rmi.registry.LocateRegistry;
import java.rmi.RemoteException;
import static org.junit.jupiter.api.Assertions.*;

public class CalculatorIntegrationTest {
    private static Registry registry;
    private static Thread serverThread;
    private static final int TEST_RMI_PORT = 1099;

    @BeforeAll
    static void startServer() throws Exception{
        serverThread = new Thread(() -> {
            try{
                Calculator calculator = new CalculatorImplementation();

                registry = LocateRegistry.createRegistry(TEST_RMI_PORT);

                registry.bind("Calculator", calculator);

                System.out.println("Integration test server started on port " + TEST_RMI_PORT);
            } catch (Exception e) {
                System.err.println("Failed to start test server: " + e.getMessage());
            }
        });

        serverThread.start();
        Thread.sleep(2000);
        System.out.println("Server startup completed");
    }
}
