package com.example.itstore.utils;
import com.example.itstore.model.CartItem;
import java.util.ArrayList;
import java.util.List;
public class CartManager {
    private static CartManager instance;
    private List<CartItem> cartList;

    private List<CartItem> checkoutList;
    private boolean needRefresh = true;

    public void markCartNeedRefresh() { needRefresh = true; }
    public boolean isCartNeedRefresh() { return needRefresh; }
    public void clearCartNeedRefresh() { needRefresh = false; }
    private CartManager() {

        cartList = new ArrayList<>();
        checkoutList = new ArrayList<>();
    }

    public static CartManager getInstance() {
        if (instance == null) {
            instance = new CartManager();
        }
        return instance;
    }

    public List<CartItem> getCartList() {
        return cartList;
    }
    public void setCartList(List<CartItem> items) {
        this.cartList = (items != null) ? items : new ArrayList<>();
    }
    public void clearCart() {
        this.cartList = new ArrayList<>();
        this.checkoutList = new ArrayList<>();
    }
    public void addToCart(CartItem newItem) {
        boolean isExist = false;
        for (CartItem item : cartList) {
            if (item.getProduct().getId() == newItem.getProduct().getId() &&
                    item.getVariantId() == newItem.getVariantId()) {
                item.setQuantity(item.getQuantity() + newItem.getQuantity());
                isExist = true;
                break;
            }
        }
        if (!isExist) {
            cartList.add(newItem);
        }
    }

    public void setCheckoutList(List<CartItem> items) {
        this.checkoutList = items;
    };

    public List<CartItem> getCheckoutList() {
        return checkoutList;
    }

    public void clearPurchasedItems(){
        if (cartList != null) {
            for (int i = cartList.size() - 1; i >= 0; i--) {
                CartItem item = cartList.get(i);
                if (item.isSelected()) {
                    cartList.remove(i);
                }
            }
        }

        if (checkoutList != null){
            checkoutList.clear();
        }
    }
}
