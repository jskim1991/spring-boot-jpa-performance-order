package io.jay.order;

import io.jay.order.repository.Address;
import io.jay.order.repository.Book;
import io.jay.order.repository.Delivery;
import io.jay.order.repository.DeliveryStatus;
import io.jay.order.repository.Member;
import io.jay.order.repository.Order;
import io.jay.order.repository.OrderItem;
import io.jay.order.repository.OrderJpaRepository;
import io.jay.order.repository.OrderStatus;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.stereotype.Component;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDateTime;
import java.util.List;

@SpringBootApplication
public class MainApplication {

    public static void main(String[] args) {
        SpringApplication.run(MainApplication.class, args);
    }
}

@Component
class DataInitializer implements CommandLineRunner {

    @PersistenceContext
    private EntityManager em;

    @Override
    @Transactional
    public void run(String... args) throws Exception {
        Member firstMember = new Member();
        firstMember.setName("Jay");
        firstMember.setAddress(new Address("Seoul", "Gangnam", "12345"));
        em.persist(firstMember);

        Member secondMember = new Member();
        secondMember.setName("Su");
        secondMember.setAddress(new Address("Seoul", "Gangnam", "12345"));
        em.persist(secondMember);

        Book firstBook = new Book();
        firstBook.setName("Jay Book");
        firstBook.setPrice(10000);
        firstBook.setStockQuantity(10);
        firstBook.setAuthor("Jay");
        firstBook.setIsbn("12345");
        em.persist(firstBook);

        Book secondBook = new Book();
        secondBook.setName("Su Book");
        secondBook.setPrice(20000);
        secondBook.setStockQuantity(10);
        secondBook.setAuthor("Su");
        secondBook.setIsbn("99999");
        em.persist(secondBook);

        var firstOrderItem = new OrderItem();
        firstOrderItem.setItem(firstBook);
        firstOrderItem.setCount(1);
        firstOrderItem.setOrderPrice(10000);

        var secondOrderItem = new OrderItem();
        secondOrderItem.setItem(secondBook);
        secondOrderItem.setCount(1);
        secondOrderItem.setOrderPrice(20000);

        var thirdOrderItem = new OrderItem();
        thirdOrderItem.setItem(secondBook);
        thirdOrderItem.setCount(1);
        thirdOrderItem.setOrderPrice(20000);


        Order firstOrder = new Order();
        firstOrder.setMember(firstMember);
        firstOrder.setOrderItems(List.of(firstOrderItem, thirdOrderItem));
        firstOrder.setOrderDate(LocalDateTime.now());
        firstOrder.setStatus(OrderStatus.ORDERED);

        var firstDelivery = new Delivery();
        firstDelivery.setAddress(firstMember.getAddress());
        firstDelivery.setStatus(DeliveryStatus.DELIVERING);
        firstOrder.setDelivery(firstDelivery);

        firstOrderItem.setOrder(firstOrder);
        thirdOrderItem.setOrder(firstOrder);

        em.persist(firstOrder);

        Order secondOrder = new Order();
        secondOrder.setMember(secondMember);
        secondOrder.setOrderItems(List.of(secondOrderItem));
        secondOrder.setOrderDate(LocalDateTime.now());
        secondOrder.setStatus(OrderStatus.ORDERED);

        var secondDelivery = new Delivery();
        secondDelivery.setAddress(secondMember.getAddress());
        secondDelivery.setStatus(DeliveryStatus.DELIVERING);
        secondOrder.setDelivery(secondDelivery);

        secondOrderItem.setOrder(secondOrder);

        em.persist(secondOrder);

//        for (int i = 0; i < 10000; i++) {
            var newOrderItem = new OrderItem();
            newOrderItem.setItem(firstBook);
            newOrderItem.setCount(1);
            newOrderItem.setOrderPrice(10000);

            var order = new Order();
            order.setMember(firstMember);
            order.setOrderItems(List.of(newOrderItem));
            order.setOrderDate(LocalDateTime.now());
            order.setStatus(OrderStatus.ORDERED);


            newOrderItem.setOrder(order);

            var delivery = new Delivery();
            delivery.setAddress(firstMember.getAddress());
            delivery.setStatus(DeliveryStatus.DELIVERING);
            order.setDelivery(delivery);

            em.persist(order);
//        }
    }
}

@RestController
@RequiredArgsConstructor
class MainController {

    private final OrderJpaRepository orderRepository;

    @PersistenceContext
    private EntityManager em;

    @GetMapping("/api/v1/orders")
    public List<OrderResponse> ordersV1() {
        return orderRepository.findAll()
                .stream()
                .map(this::transform)
                .toList();
    }

    @GetMapping("/api/v2/orders")
    public List<OrderResponse> ordersV2() {
        List<Order> orders = em.createQuery("""
                        select order from Order order
                        join fetch order.member member
                        join fetch order.delivery delivery
                        """, Order.class)
                .getResultList();
        return orders.stream()
                .map(this::transform)
                .toList();
    }

    @GetMapping("/api/v3/orders")
    public List<OrderResponse> ordersV3() {
        List<Order> orders = em.createQuery("""
                        select order from Order order
                        join fetch order.member member
                        join fetch order.delivery delivery
                        join fetch order.orderItems orderItems
                        join fetch orderItems.item item
                        """, Order.class)
                .getResultList();
        return orders.stream()
                .map(this::transform)
                .toList();
    }

    private OrderResponse transform(Order order) {
        return new OrderResponse(
                order.getId(),
                order.getMember().getName(), // member
                order.getOrderDate(),
                order.getOrderItems().stream() // orderItem, item
                        .map(orderItem -> new ItemResponse(
                                orderItem.getItem().getName(),
                                orderItem.getItem().getPrice(),
                                orderItem.getCount(),
                                orderItem.getOrderPrice()
                        ))
                        .toList(),
                order.getOrderItems().stream()
                        .map(OrderItem::getOrderPrice)
                                .reduce(0, Integer::sum),
                order.getStatus(),
                new AddressResponse(
                        order.getDelivery().getAddress().getCity(),
                        order.getDelivery().getAddress().getStreet(),
                        order.getDelivery().getAddress().getZipcode()
                ) // delivery, address
        );
    }
}


record OrderResponse(Long orderId,
                     String name,
                     LocalDateTime orderDate,
                     List<ItemResponse> items,
                     int totalPrice,
                     OrderStatus orderStatus,
                     AddressResponse address
) {}

record AddressResponse(String city, String street, String zipcode) {}

record ItemResponse(String itemName, int itemPrice, int itemCount, int orderPrice) {

}