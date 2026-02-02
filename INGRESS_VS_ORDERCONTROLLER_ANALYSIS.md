# Ingress vs OrderController for Order Submission — Analysis

## Your Statement

> "IngressController is unnecessary. The router will decide which server the API is called to, so we only need to use `api/orders/submit` within OrderService (via OrderController)."

This mixes **what currently happens** with **what could happen** if you remove Ingress. Below is what’s correct, what’s off, and why.

---

## 1. What the Router Actually Does Today

**Server router** (`serverRouter.ts`):

- `getServerForSymbol(symbol)` → which server (port, baseUrl) handles that symbol.
- Used for **read operations**: e.g. `getStockDetail(symbol)`, orderbook, etc.  
  → Frontend calls `getServerForSymbol(symbol)` and sends those requests to that server (8080/8081/8082).

**Order submission** (`api.ts`):

```ts
async submitOrder(order: Order) {
  return this.requestToIngress('/orders/submit', { ... });  // always 8085
}
```

- `submitOrder` **does not use the router**.
- It **always** goes to **Ingress (8085)** via `requestToIngress`.
- So today, the **router does not decide** which server receives order submission. Ingress does — by being the only destination.

**Conclusion:**  
**Today, the router does *not* decide which server handles order submission.** It only decides for symbol-based **read** requests. Order submission always goes to Ingress.

---

## 2. Can You Remove Ingress and Use OrderController + OrderService?

**Yes.** You can:

1. Remove Ingress (and `IngressController`).
2. Uncomment and use `OrderController` `POST /api/orders/submit` on each **matching** server (8080/8081/8082).
3. Change the frontend so `submitOrder` uses the **router**: e.g. `getServerForSymbol(order.symbol)` and POST to that server’s `/api/orders/submit`.
4. That server runs `OrderService.submitOrder` (with `placeOrder` + `kafkaProducer.sendOrder`), then produces to Kafka as today.

So **IngressController is not technically required**. You can use `api/orders/submit` only via **OrderController → OrderService** on matching servers.

---

## 3. What Would Need to Change

| Component | Current | If you remove Ingress |
|-----------|---------|------------------------|
| **Frontend `submitOrder`** | `requestToIngress('/orders/submit')` → always 8085 | Use router: `getServerForSymbol(order.symbol)`, POST to that server’s `/api/orders/submit` |
| **Router** | Used for reads only | **Also** used to pick which server receives order submission |
| **Order submission endpoint** | `IngressController` @ 8085 | `OrderController` @ 8080/8081/8082 |
| **Backend flow** | Ingress → Kafka → consumers | OrderController → OrderService → Kafka → consumers |

So:

- **“Router decides which server”** is **not** true today for orders. It **would become** true only **after** you remove Ingress and change the frontend to route submissions by symbol (or by some other rule) to matching servers.
- **“We only need `api/orders/submit` within OrderService”** is correct **if** you remove Ingress: the only submit path would be OrderController → OrderService. Right now you effectively have **two** submit paths (Ingress vs OrderController); one is commented out.

---

## 4. Why Use Ingress at All? (It’s a Design Choice)

**With Ingress:**

- **Single endpoint** for all order submission: always `http://localhost:8085/api/orders/submit`.  
  No router logic for orders; frontend is simple.
- **Stateless gate:** Ingress only validates and produces to Kafka. No OrderBooks, no matching.
- **Clear split:** Ingress = “receive + produce”; matching servers = “consume + match”.  
  Scale ingress separately from matching servers.
- **Kafka decides processing:** Which matching server actually processes an order is determined by **Kafka partition assignment** (key = symbol), not by which server received the HTTP request.

**Without Ingress (router → OrderController):**

- **Fewer moving parts:** No separate ingress process or `IngressController`.
- **Reuse existing code:** OrderController + OrderService handle `/api/orders/submit`.
- **Router decides where to POST:** e.g. by symbol. The server that receives the request produces to Kafka; some consumer (possibly same or different server) processes it.

So Ingress is **not** “unnecessary” in an absolute sense — it’s a **design choice**. You **can** remove it and rely on the router + OrderController + OrderService; that’s valid.

---

## 5. Important Detail: Who “Decides” What

| Decision | Who decides today | If you remove Ingress |
|----------|-------------------|------------------------|
| **Which server receives the HTTP order** | Always Ingress (8085) | Router (e.g. by symbol) → one of 8080/8081/8082 |
| **Which server processes the order ( matching )** | Kafka partition assignment | Same — Kafka (partition by symbol) |

So:

- **Router** would only decide **where to send the HTTP request** (which matching server’s OrderController).
- **Kafka** still decides **which process actually consumes and matches** the order. The HTTP receiver and the matcher can be different servers.

---

## 6. Summary

| Your point | Correct? | Clarification |
|------------|----------|----------------|
| “IngressController is unnecessary” | **Partially** | You **can** remove it and use OrderController + OrderService only. But it’s not “unnecessary” by definition — it’s a deliberate design (single gate, stateless). |
| “Router decides which server the API is called to” | **Not for orders today** | Router is used for **read** requests only. Order submission always goes to Ingress. If you remove Ingress, you’d **add** router-based routing for order submission. |
| “We only need `api/orders/submit` within OrderService” | **Yes, if you drop Ingress** | Then the only submit path is OrderController → OrderService. Ingress’s submit path goes away. |

**Bottom line:**

- You **can** remove Ingress and use **only** OrderController + OrderService for `api/orders/submit`, and have the **router** decide which matching server receives the request.  
- Today, the router **does not** decide that for orders; Ingress does. So the rationale “router decides → Ingress unnecessary” is backwards: you’d first **remove** Ingress and **then** make the router decide.  
- Whether to keep or remove Ingress is a **design choice** (single gate vs simpler deployment), not a correctness requirement.
