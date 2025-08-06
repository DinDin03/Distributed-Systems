package server;

import interfaces.ShoppingCart;
import interfaces.CartItem;
import java.rmi.server.UnicastRemoteObject;
import java.rmi.RemoteException;
import java.util.HashMap;

public class ShoppingCartImpl extends UnicastRemoteObject implements ShoppingCart {
    private HashMap<String, CartItem> shoppingCart;
    
    public ShoppingCartImpl() throws RemoteException {
        super();
        shoppingCart = new HashMap<>();
    }

    @Override
    public void addItem(CartItem item) throws RemoteException{
        String itemName = item.getItemName();

        if(shoppingCart.containsKey(itemName)){
            CartItem existingItem = shoppingCart.get(itemName);
            int newQuantity = existingItem.getQuantity() + item.getQuantity();
            CartItem  combinedItem = new CartItem(itemName, newQuantity, item.getPricePerUnit());
            shoppingCart.put(itemName, combinedItem);
        }else{
            shoppingCart.put(itemName, item);
        }
    }


}
