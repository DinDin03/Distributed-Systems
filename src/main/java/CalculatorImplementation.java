import java.rmi.RemoteException;
import java.rmi.server.UnicastRemoteObject;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

public class CalculatorImplementation extends UnicastRemoteObject implements Calculator{

    private final Deque<Integer> stack;
    private final List<String> validOperators = Arrays.asList("min", "max", "gcd", "lcm");

    private final Map<String, Deque<Integer>> clientStacks;
    private final ThreadLocal<String> currentSession;

    public CalculatorImplementation() throws RemoteException{
        super();
        stack = new ArrayDeque<>();

        clientStacks = new ConcurrentHashMap<>();
        currentSession = new ThreadLocal<>();
    }

    @Override
    public synchronized void pushValue(int val) throws RemoteException{
        stack.push(val);
    }

    @Override
    public synchronized int pop() throws RemoteException{
        if(stack.isEmpty()){
            throw new RemoteException("Cannot pop from an empty stack");
        }
        return stack.pop();
    }

    @Override
    public synchronized boolean isEmpty() throws RemoteException{
        return stack.isEmpty();
    }

    @Override
    public synchronized int delayPop(int millis) throws RemoteException{
        try{
            Thread.sleep(millis);
        }catch(InterruptedException e){
            Thread.currentThread().interrupt();
            throw new RemoteException("Delay pop was interrupted", e);
        }
        return pop();
    }

    @Override
    public synchronized void pushOperation(String operator) throws RemoteException{
        if(!validOperators.contains(operator)){
            throw new RemoteException("Invalid operator: " + operator + 
                                    ". Valid operators are: min, max, gcd, lcm");
        }
        if(stack.isEmpty()){
            throw new RemoteException("Cannot perform operation on empty stack");
        }

        List<Integer> values = new ArrayList<>();
        while(!stack.isEmpty()){
            values.add(stack.pop());
        }

        int result;
        if(operator.equals("min")){
            result = Collections.min(values);
        }
        else if(operator.equals("max")){
            result = Collections.max(values);
        }
        else if(operator.equals("gcd")){
            result = gcdMultiple(values);
        }
        else if(operator.equals("lcm")){
            long lcmResult = lcmMultiple(values);
            if(lcmResult > Integer.MAX_VALUE){
                throw new RemoteException("LCM result too large for integer: " + lcmResult);
            }
            result = (int)lcmResult;
        }
        else {
            throw new RemoteException("Unknown operator: " + operator);
        }

        stack.push(result);
    }

    @Override
    public synchronized String createSession() throws RemoteException{
        String sessionId = UUID.randomUUID().toString();
        clientStacks.put(sessionId, new ArrayDeque<>());
        System.out.println("New session created " + sessionId);

        return sessionId;
    }

    private int gcd(int a, int b){
        if(b == 0) return a;
        else{
            return gcd(b, a%b);
        }
    }

    private int gcdMultiple(List<Integer> numbers){
        int res = Math.abs(numbers.get(0));
        for(int i = 1; i < numbers.size(); i++){
            res = gcd(res, Math.abs(numbers.get(i)));
        }
        return res;
    }

    private long lcm(int a, int b){
        if(a == 0 || b == 0){
            return 0;
        }
        return Math.abs((long)a * b) / gcd(a, b); 
    }

    private long lcmMultiple(List<Integer> numbers){
        long res = Math.abs(numbers.get(0));
        for(int i = 1; i < numbers.size(); i++){
            res = lcm((int)res, Math.abs(numbers.get(i)));
            
            if (res > Integer.MAX_VALUE) {
                throw new ArithmeticException("LCM result too large");
            }
        }
        return res;
    }


}