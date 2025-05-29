// package com.project.matchingengine.models.order;

// import java.io.Serializable;
// import java.util.Comparator;


// public class OrderPriceTimeComparator implements Comparator<Order>, Serializable {
//     private static final long serialVersionUID = 1L;
    
//     private final boolean descending;
    
//     public OrderPriceTimeComparator(boolean descending) {
//         this.descending = descending;
//     }
    
//     @Override
//     public int compare(Order o1, Order o2) {
//         int priceCompare = Double.compare(o1.getPrice(), o2.getPrice());
//         if (descending) {
//             priceCompare = -priceCompare;
//         }
        
//         return priceCompare != 0 ? priceCompare : 
//                o1.getOrderTimestamp().compareTo(o2.getOrderTimestamp());
//     }
// }