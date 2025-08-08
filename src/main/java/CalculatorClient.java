import java.rmi.registry.LocateRegistry;
import java.rmi.registry.Registry;
import java.rmi.RemoteException;

public class CalculatorClient{
    public static void main(String[] args){
        try{
            Registry registry = LocateRegistry.getRegistry("localhost", 1099);

            Calculator calculator = (Calculator) registry.lookup("Calculator");

            
            calculator.pushValue(10);
            System.out.println("Pushed 10");
            calculator.pushValue(20);
            System.out.println("Pushed 20");

            int result = calculator.pop();
            System.out.println("Popped: " + result); 

        }catch(RemoteException e){
            System.err.println("Failed to connect to server: " + e.getMessage());
        }
        catch(Exception e){
            System.err.println("Client error: " + e.getMessage()); 
        }
    }
}