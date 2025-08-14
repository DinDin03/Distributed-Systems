import org.junit.jupiter.api.*;
import java.rmi.registry.Registry;
import java.time.LocalDate;
import java.rmi.registry.LocateRegistry;
import java.rmi.RemoteException;
import static org.junit.jupiter.api.Assertions.*;
import java.util.Random;

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

    @AfterAll
    static void stopServer() throws Exception{
        System.out.println("Stopping integration test server");

        try{
            if(registry != null){
                registry.unbind("Calculator");
                System.out.println("Calculator service unbound from the registry");
            }
        }catch(Exception e){
            System.out.println("Error unbinding service " + e.getMessage());
        }

        try{
            if(serverThread != null && serverThread.isAlive()){
                serverThread.interrupt();
                serverThread.join(500);

                if (serverThread.isAlive()) {
                    System.err.println("Server thread did not stop");
                } else {
                    System.out.println("Server thread stopped successfully");
                }
            }
        }catch(Exception e){
            System.out.println("Error stopping server thread " + e.getMessage());
            Thread.currentThread().interrupt();
        }

        System.out.println("Server shutdown completed");
    }

    @Test
    @DisplayName("Connect client to server to perform basic operations")
    void testClientServerConnection() throws Exception{
        Random random = new Random();
        System.out.println("Testing client server connection");

        Registry clientRegistry = LocateRegistry.getRegistry("localhost", TEST_RMI_PORT);
        Calculator calculator = (Calculator) clientRegistry.lookup("Calculator");

        assertNotNull(calculator, "Calculator should not be null");
        assertTrue(calculator.isEmpty(), "Calculator should initially be empty");

        int randomInt = random.nextInt(100);
        calculator.pushValue(randomInt);
        assertFalse(calculator.isEmpty(), "Calculator should now be NOT empty");

        int res = calculator.pop();
        assertEquals(randomInt, res, "Calculator should pop the same value that was pushed");

        assertTrue(calculator.isEmpty(), "Calculator should now be empty again");

        System.out.println("Client server test completed successfully");
    }

    @Test
    @DisplayName("Test multiple clients sharing the same stack")
    void testSharedStack() throws Exception{
        System.out.println("Testing multiple clients sharing the same stack");

        final int NUM_CLIENTS = 3;
        final int VALUES_PER_CLIENT = 5;

        Calculator[] clients = new Calculator[NUM_CLIENTS];
        for(int i = 0; i < NUM_CLIENTS; i++){
            Registry clientRegistry = LocateRegistry.getRegistry("localHost", TEST_RMI_PORT);
            clients[i] = (Calculator) clientRegistry.lookup("Calculator");
        }

        for(Calculator client : clients){
            assertTrue(client.isEmpty(), "Initially the stack should be empty for all clients");
        }

        Random random = new Random();
        for(int i = 0; i < NUM_CLIENTS; i++){
            for(int j = 0; j < VALUES_PER_CLIENT; j++){
                int randomInt = random.nextInt(100);
                clients[i].pushValue(randomInt);
                System.out.println("Client " + i + " pushed the value " + randomInt);
            }
        }

        for (int i = 0; i < NUM_CLIENTS; i++) {
            assertFalse(clients[i].isEmpty(), "Clients should see non empty stack");
        }

        int TOTAL_VALUES = NUM_CLIENTS * VALUES_PER_CLIENT;
        for(int i = 0; i < TOTAL_VALUES; i++){
            clients[0].pop();
        }

        for (int i = 0; i < NUM_CLIENTS; i++) {
            assertTrue(clients[i].isEmpty(), "All clients should see empty stack now");
        }

        System.out.println("Multi client shared stack test completed successfully");
    }

    @Test
    @DisplayName("DelayPop with multiple clients correctly")
    void testConcurrentDelayPop() throws Exception {
        System.out.println("Testing concurrent delayPop operations");

        Registry registry1 = LocateRegistry.getRegistry("localhost", TEST_RMI_PORT);
        Registry registry2 = LocateRegistry.getRegistry("localhost", TEST_RMI_PORT);

        Calculator client1 = (Calculator) registry1.lookup("Calculator");
        Calculator client2 = (Calculator) registry2.lookup("Calculator");

        client1.pushValue(100);
        client1.pushValue(200);

        final long[] client1Result = new long[2];
        final long[] client2Result = new long[2];
        final Exception[] exceptions = new Exception[2];

        Thread thread1 = new Thread(() -> {
            try {
                long startTime = System.currentTimeMillis();
                int value = client1.delayPop(1000);
                long endTime = System.currentTimeMillis();

                client1Result[0] = value;
                client1Result[1] = endTime - startTime;

                System.out.println("Client 1 delayPop returned: " + value +
                        " after " + (endTime - startTime) + "ms");
            } catch (Exception e) {
                exceptions[0] = e;
            }
        });

        Thread thread2 = new Thread(() -> {
            try {
                Thread.sleep(500);
                long startTime = System.currentTimeMillis();
                int value = client2.delayPop(1000);
                long endTime = System.currentTimeMillis();

                client2Result[0] = value;
                client2Result[1] = endTime - startTime;

                System.out.println("Client 2 delayPop returned " + value +
                        " after " + (endTime - startTime) + "milliseconds");
            } catch (Exception e) {
                exceptions[1] = e;
            }
        });

        long testStartTime = System.currentTimeMillis();
        thread1.start();
        thread2.start();

        thread1.join();
        thread2.join();
        long testEndTime = System.currentTimeMillis();

        assertNull(exceptions[0], "Client 1 should not have thrown exception");
        assertNull(exceptions[1], "Client 2 should not have thrown exception");

        assertTrue(client1Result[1] >= 1000, "Client 1 should have waited at least 1 second");
        assertTrue(client2Result[1] >= 1000, "Client 2 should have waited at least 1 second");

        assertEquals(200, client1Result[0], "First delayPop should return last pushed value which is 200");
        assertEquals(100, client2Result[0], "Second delayPop should return second-to-last value (100)");

        assertTrue(client1.isEmpty(), "Stack should be empty after both delayPops");
        assertTrue(client2.isEmpty(), "Stack should be empty after both delayPops");

        long totalTestTime = testEndTime - testStartTime;
        System.out.println("Total test time: " + totalTestTime + "milliseconds");
        System.out.println("Concurrent delayPop test completed successfully");
    }
}
