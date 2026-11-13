# OrderUp!
Repo for CS 2114, project 3 (Group 52). Built in Java, this project features a GUI and implements ordering software for food trucks or other restaurant-like establishments.

## Reflection 
What changed from the specification: 

  - **Classes**: The spec called for seven model classes `OrderUpModel`, `KitchenQueue`, `OrderBook`, `Order`, `OrderItem`, `MenuItem`, and `OrderStatus`. We built those seven plus one more, `StatusInfo`, a small record holding a ticket, a status, and a queue position. The spec said `getStatus` "returns the order's status and queue position" but never named a return type, and a single value could not carry both. There is still no inheritance anywhere, which matches the design: every arrow in the system diagram is a uses/holds relationship.

  - **Fields**: The spec gave `OrderUpModel` a `currentOrder: Order` that is replaced after every submit. That conflicted with another spec decision: that `Order.ticketNumber` is `final` and set at construction, because a ticket should only be issued at submit time. We replaced `currentOrder` with a `final List<OrderItem> draft`. The Order is built inside submitOrder() once the ticket is known, which also means abandoned drafts never burn a ticket number. `OrderBook` uses a LinkedHashMap so history stays in submission order, and the nextTicket counter the spec mentioned is a private field. OrderItem (not given fields in the spec) holds a final MenuItem and a final int quantity.

- **Method signatures**
  1. `KitchenQueue.isEmpty()` was added.
  2. `KitchenQueue.size()`, `peekFront()`, and `positionOf()` now ignore cancelled orders, and         `dequeueFront()` discards cancelled orders that have reached the front. This is how we implemented the "flag, never remove" decision without splicing the middle of the deque.
  3. `KitchenQueue.enqueue` and `addRemakeToFront` also throw IllegalArgumentException if the           order is not WAITING. The spec only said null.
  4. `OrderUpModel` gained `startNewOrder()` (for the controller's `handleNewOrder`), `remakeOrder(int ticket)` (the model-level entry point for `addRemakeToFront`), and `cancelOrder(int ticket)` (pulled forward from the stretch goals). `addToOrder` additionally rejects an item that is not on the menu.
  5. `OrderBook` gained `register(Order)`, which the diagram labels "register / look up" but the spec's method list omitted.
  6. `Order.addItem` rejects quantities above 99 as well as below 1 (the spec only mentioned below 1), and Order gained read-only getters and `getTotal(). setStatus` is package-private so only the model package can change an order's state.
    The spec's `validateQuantity` description said `1 < q < 99`, which would wrongly reject 1 and 99. The intended rule, and what the model's constants (`Order.MIN_QTY`, `Order.MAX_QTY`) implement, is 1 to 99 inclusive.

- **Where validation lives.**
    The design split was: the controller catches malformed input (letters, blank entries, a menu choice that doesn't exist, a non-numeric ticket) and the model enforces business rules. We built the model side as a full safety net. It throws `IllegalArgumentException` for null items, items not on the menu, out-of-stock items, and quantities outside 1 to 99. It throws `IllegalStateException` for submitting an empty order, completing from an empty line, and cancelling or remaking an order in the wrong state. An unknown ticket in `getStatus` is still a normal outcome (`NOT_FOUND`), but `cancelOrder` and `remakeOrder` throw on an unknown ticket because there is no sensible default there. The quantity range is checked in two places (`OrderUpModel.addToOrder` and `Order.addItem`), which is duplication we would remove if we had more time.

- **Test cases.** The spec's test plan had four methods. The test file we built covers them and adds more, but several details differ from the plan:

  1. The plan used `cancelOrder(String orderId)` returning false on bad input. We built `cancelOrder(int ticket)` returning void and throwing, because tickets are int everywhere else in the design and exceptions match how the rest of the model reports invalid state.
  2. The plan's "already marked Completed" is the READY status in the enum. `addRemakeToFront` rejects any order that is not WAITING, and the facade's `remakeOrder` only accepts `READY` orders.
  3. The plan said `dequeue` on an empty queue could return null or throw. We chose to throw (`NoSuchElementException` from the queue, `IllegalStateException` from the facade), and peek returns null.
  4. The plan's "an order with negative/zero quantities" bad input for `enqueue` can no longer exist, because `Order.addItem` refuses to create such an order. That case is tested at `addItem` instead.


 - **Did the model really stand alone?** 
    Yes. Every model class imports only java.util and other model classes. No model class prints, reads input, or refers to a view or controller, and the tests run with no UI at all. Nothing had to be moved across the line to make the model run. There are three places where the model is still less clean than the design says:
  
    1. `MenuItem.toString()` and `Order.toString()` format prices (`$8.99`) and flags (`[out of stock]`), which is presentation. The views should build their own display strings from the getters, and these toString methods are only meant for debugging.
    2. `getMenu()` returns an unmodifiable list, but the `MenuItem` objects inside are the real ones and `setInStock` is public. A view that held a reference could change stock, which breaks the "views never change state" rule. We rely on convention here, and a defensive copy or a package-private setter would fix it.
    3. `addToOrder` checks `menu.contains(item)` by object identity, so the controller must pass the exact MenuItem objects returned by getMenu() and not copies. This is easy to get wrong when wiring up a JavaFX list, so the controller and view owners need to know.

- **What happened to our unknowns?**

  [blank for now]

 - **What doesn't work yet?**

    1. **Integration.** The model is complete and has been exercised on its own, but it has not been tested end-to-end through the controller and the three JavaFX views. [Update with the current state of OrderUpController and the views.]
    2. **Missing tests.** There are no direct unit tests for KitchenQueue, OrderBook, or Order on their own, which the spec's test plan promised. Their behavior is only covered indirectly through the facade. Also untested: Order.getTotal(), MenuItem constructor validation, and a duplicate-ticket register.
    3. **Known gaps in behavior.** A READY order cannot be cancelled, and a WAITING order cannot be remade (both throw). The model has no persistence, so every run starts with an empty order book. The end-of-day sales summary is not built.
    4. **Duplicated check.** The quantity range is enforced in two places (see above).




## Next Steps

- **What is left to finish the MVP**
  1. Pull the model branch, run `OrderUpModelTest` in Eclipse, and fix anything that fails. 
  2. Write the missing direct tests for `KitchenQueue`, `OrderBook`, and `Order`. 
  3. Finish `OrderUpController` against the real model: input validation (blank, non-numeric, out-of-range, menu choice that doesn't exist), the handlers, and catching model exceptions so the program never crashes. 
  4. Finish the three JavaFX views as display-only classes that get everything from the controller. 
  5. Join the layers: hook each screen's button events to controller methods. 
  6. Run the full flow by hand (cashier places an order, cook completes it, customer checks status) plus every bad-input case in the validation list, then fix what breaks.

- **Our change feature**
  **Rush orders.** The cashier can mark an order as "rush." A rush order goes to the front of the line, behind any remakes already at the front but ahead of normal orders. 

  _Predicted to change:_
    - **Model**: `Order` gets a `rush: boolean` field; `KitchenQueue` gets an insertion rule for rush orders; `OrderUpModel.submitOrder` gets a rush parameter or an overload.
    - **Controller**: `handleSubmitOrder` (or a new `handleSubmitRush`) passes the flag along.
    - **View**: `CashierView` needs a rush toggle.

    Design says these should not need to change: `KitchenView` and `CustomerStatusView`, because they already show whatever order is next and whatever position the controller passes in. Also `OrderBook`, `MenuItem`, `OrderStatus`, and `StatusInfo`. We will compare this prediction to what actually changes and report any difference.

- **Stretch goals**
    - **Committing to:** order cancellation, our primary stretch goal. The model side (`cancelOrder`, the CANCELLED flag, queue skipping) is already built and tested, so the remaining work is the controller handler and a cancel control in the UI.
    - **Secondary / likely dropping:** the end-of-day sales summary. We will only attempt it if the MVP, cancellation, and the rush-order feature are all finished and tested, and otherwise it is dropped.
    
**New tests needed**
  1. `KitchenQueue`: enqueue/dequeue FIFO order; `addRemakeToFront` ahead of waiting orders; dequeue and peek on empty; null and non-WAITING orders rejected; cancelled orders skipped by `peekFront`, `size`, `positionOf`, and `dequeueFront`.
  2. `OrderBook`: tickets are sequential; `find` returns null for an unknown ticket; registering a duplicate ticket or a null order throws.
  3. `Order: addItem` boundary values 0, 1, 99, and 100; null item; `getTotal`; the item list cannot be modified from outside.
  4. Rush orders: a rush order lands ahead of normal orders but behind a remake; two rush orders stay in submission order; cancelling a rush order; position reported correctly.
  5. Controller: `validateQuantity` at 0, 1, 99, and 100; non-numeric and blank ticket input; completing from an empty line shows an error and does not crash.

## Attribution 

| File | Who | How |
| --- | --- | --- |
| `OrderUpModel.java` | | copied from given file |
| `KitchenQueue.java` | | GenAI-generated from our spec; reviewed by: Vanessa| read over the code
| `OrderBook.java` | | GenAI-generated from our spec; reviewed by: Rishad| read over the code
| `Order.java` | | GenAI-generated from our spec; reviewed by: Vanessa| read over the code
| `OrderItem.java` | | GenAI-generated from our spec; reviewed by: Vanessa| read over the code
| `MenuItem.java` | | GenAI-generated from our spec; reviewed by: Vanessa| read over the code
| `OrderStatus.java` | | GenAI-generated from our spec; reviewed by: Vanessa| read over the code
| `StatusInfo.java` | | GenAI-generated from our spec; reviewed by: Vanessa| read over the code
| `OrderUpModelTest.java` | | GenAI-generated from our spec; reviewed by: Rishad| read over the code
| `OrderUpController.java` | | GenAI-generated from our spec; reviewed by: Rishad| read over the code
| `CashierView.java` | | GenAI-generated from our spec; reviewed by: Aryaman| read over the code
| `KitchenView.java` | | GenAI-generated from our spec; reviewed by: Aryaman| read over the code
| `CustomerStatusView.java` | | GenAI-generated from our spec; reviewed by: Aryaman| read over the code
| Test files (___) | | GenAI-generated from our test plan; reviewed by: Vanessa | ran the test files
| Test files (___) | | GenAI-generated from our test plan; reviewed by: Rishad | ran the test files
| `README.md` | | GenAI-drafted; revised by: Vanessa | filled in the gaps

## GenAI Note

  1. **Design review.** Claude reviewed our system diagram and helped us talk through the Deque-versus-queue and flag-versus-remove decisions.
  2. **Specification text.** Claude helped draft the method-signature and validation sections of the spec from the system diagram, which we then edited.
  3. **Model code and tests.** Claude drafted the eight model classes and the JUnit test file from the spec and the diagram. Claude also compiled the model and ran a quick smoke test of the main behaviors (FIFO order, remake to front, cancel, empty-queue errors, bad quantities). The JUnit file itself was not run by Claude and had to be run by us in Eclipse.


