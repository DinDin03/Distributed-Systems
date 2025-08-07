import java.rmi.RemoteException;
import java.rmi.server.UnicastRemoteObject;
import java.util.ArrayDeque;
import java.util.Deque;

public class CalculatorImplementation extends UnicastRemoteObject implements Calculator{

    private final Deque<Integer> stack;

    public CalculatorImplementation() throws RemoteException{
        super();
        stack = new ArrayDeque<>();
    }

    public void pushValue(int val) throws RemoteException{
        stack.push(val);
    }

    public int pop() throws RemoteException{
        if(stack.isEmpty()){
            throw new RemoteException("Cannot pop from an empty stack");
        }
        return stack.pop();
    }

    public boolean isEmpty() throws RemoteException{
        return stack.isEmpty();
    }

    public int delayPop(int millis) throws RemoteException{
        try{
            Thread.sleep(millis);
        }catch(InterruptedException e){
            Thread.currentThread().interrupt();
            throw new RemoteException("Delay pop was interrupted", e);
        }
        return pop();
    }

    private int gcd(int a, int b){
        
    }
} 