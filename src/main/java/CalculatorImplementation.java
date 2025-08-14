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
        Deque<Integer> clientStack = getCurrentClientStack();
        clientStack.push(val);
        System.out.println("Pushed " + val + " to session " + currentSession.get());

    }

    @Override
    public synchronized int pop() throws RemoteException{
        Deque<Integer> clientStack = getCurrentClientStack();

        if(clientStack.isEmpty()){
            throw new RemoteException("Cannot pop from an empty stack");
        }

        int value = clientStack.pop();
        System.out.println("Popped " + value + " from session " + currentSession.get());
        return value;
    }

    @Override
    public synchronized boolean isEmpty() throws RemoteException{
        Deque<Integer> clientStack = getCurrentClientStack();
        return clientStack.isEmpty();
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
    public synchronized void pushOperation(String operator) throws RemoteException {
        if (!validOperators.contains(operator)) {
            throw new RemoteException("Invalid operator " + operator +
                    " Valid operators are min, max, gcd, lcm");
        }

        Deque<Integer> clientStack = getCurrentClientStack();

        if (clientStack.isEmpty()) {
            throw new RemoteException("Cannot perform operation on empty stack");
        }

        List<Integer> values = new ArrayList<>();
        while (!clientStack.isEmpty()) {
            values.add(clientStack.pop());
        }

        int result;
        if (operator.equals("min")) {
            result = Collections.min(values);
        }
        else if (operator.equals("max")) {
            result = Collections.max(values);
        }
        else if (operator.equals("gcd")) {
            result = gcdMultiple(values);
        }
        else if (operator.equals("lcm")) {
            long lcmResult = lcmMultiple(values);
            if (lcmResult > Integer.MAX_VALUE) {
                throw new RemoteException("LCM result too large for integer " + lcmResult);
            }
            result = (int)lcmResult;
        }
        else {
            throw new RemoteException("Unknown operator " + operator);
        }

        clientStack.push(result);

        System.out.println("Operation " + operator + " completed for session " + currentSession.get() +
                ", result: " + result);
    }

    @Override
    public synchronized String createSession() throws RemoteException{
        String sessionId = UUID.randomUUID().toString();
        clientStacks.put(sessionId, new ArrayDeque<>());
        System.out.println("\nNew session created " + sessionId);

        return sessionId;
    }

    @Override
    public synchronized void setSession(String sessionId) throws RemoteException {
        if (sessionId == null) {
            throw new RemoteException("Session ID cannot be null");
        }

        if (!clientStacks.containsKey(sessionId)) {
            throw new RemoteException("Invalid session ID " + sessionId);
        }

        currentSession.set(sessionId);
        System.out.println("Client set session to " + sessionId);
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

    private Deque<Integer> getCurrentClientStack() throws RemoteException{
        String sessionId = currentSession.get();

        if(sessionId == null){
            throw new RemoteException("No session set, create and set session first");
        }

        Deque<Integer> clientStack = clientStacks.get(sessionId);

        if(clientStack == null){
            throw new RemoteException("Invalid session. Expired or incorrect session id");
        }

        return clientStack;
    }


}