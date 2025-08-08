import java.rmi.registry.LocateRegistry;
import java.rmi.registry.Registry;
import java.rmi.RemoteException;

public class CalculatorServer{
    public static void main(String[] args){
        try{
            Calculator calculator = new CalculatorImplementation();

            Registry registry = LocateRegistry.createRegistry(1099);

            registry.bind("Calculator", calculator);

            System.out.println("Calculator Server is running on port 1099");

            Thread.currentThread().join();

        }catch(RemoteException e){
            System.err.println("Server failed to start: " + e.getMessage());
            return;
        }catch(InterruptedException e){
            System.out.println("Server interrupted. Shutting down");
        }catch(Exception e){
            System.err.println("Failed to bind service: " + e.getMessage());
            return;
        }
    }
}