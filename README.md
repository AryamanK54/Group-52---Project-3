# OrderUp!
Repo for CS 2114, project 3 (Group 52). Order Up! is ordering software for a food truck or other small restaurant, written in Java. This checkpoint is a console program; the final product will have three JavaFX views (cashier, kitchen, customer).

## Running it

- **Program:** run `OrderUpApp.main`. The main menu offers: 1 start a new order, 2 add an item, 3 submit the order, 4 show the next order (kitchen), 5 complete the next order (kitchen), 6 check an order's status (customer), 7 or `q` to quit.
- **Placing an order takes three steps:** start it (1), add one or more items (2), then submit it (3). Only a submitted order gets a ticket number and appears in the kitchen line (4).
- **Tests:** there are six test classes (`MenuItemTest`, `OrderTest`, `KitchenQueueTest`, `OrderBookTest`, `OrderUpModelTest`, `OrderUpControllerTest`), 144 tests in all, and they all pass. Three of them extend `student.TestCase`, which needs Java 17 (the JRE bundled with Eclipse). On Java 24 or later they fail before running, because `student.jar` installs a Security Manager and those versions removed it. The other three use JUnit 5, so Eclipse needs JUnit 5 on the build path.

## Reflection
What changed from the specification:

- **Classes.** The spec called for seven model classes: `OrderUpModel`, `KitchenQueue`, `OrderBook`, `Order`, `OrderItem`, `MenuItem`, and `OrderStatus`. We built those seven plus `StatusInfo`, a small immutable class holding a ticket number, a status, and a queue position. The spec said `getStatus` "returns the order's status and queue position" but never named a return type, and a single value could not carry both. On the view side we added a fourth view, `MainMenuView`, and an `OrderUpApp` class with `main` that builds the model, views, and controller. There is still no inheritance anywhere, which matches the design: every arrow in the system diagram is a uses/holds relationship.

- **Fields.** The spec gave `OrderUpModel` a `currentOrder: Order` that is replaced after every submit. That conflicted with another spec decision: `Order.ticketNumber` is `final` and set at construction, and a ticket should only be issued at submit time. We replaced `currentOrder` with a `final List<OrderItem> draft`. The `Order` is built inside `submitOrder()` once the ticket is known, so an abandoned draft never uses up a ticket number. `OrderBook` uses a `LinkedHashMap` so history stays in submission order, and its `nextTicket` counter is a private field. `OrderItem` (not given fields in the spec) holds a final `MenuItem` and a final `int` quantity. The controller holds the three views, the shared `Scanner`, and a `PrintStream` as well as the model, because it runs the console loop.

- **Method signatures.**
  1. `KitchenQueue.isEmpty()` was added.
  2. `KitchenQueue.size()`, `peekFront()`, and `positionOf()` ignore cancelled orders, and `dequeueFront()` discards cancelled orders that reach the front. This is how we implemented the "flag, never remove" decision without splicing the middle of the deque.
  3. `KitchenQueue.enqueue` and `addRemakeToFront` also throw `IllegalArgumentException` if the order is not WAITING. The spec only said null.
  4. `OrderUpModel` gained `startNewOrder()` (for the controller's `handleNewOrder`), `remakeOrder(int ticket)` (the model-level entry point for `addRemakeToFront`), and `cancelOrder(int ticket)` (pulled forward from the stretch goals). `addToOrder` also rejects an item that is not on the menu.
  5. `OrderBook` gained `register(Order)`, which the diagram labels "register / look up" but the spec's method list left out.
  6. `Order` gained `MIN_QTY` and `MAX_QTY` constants (1 and 99), read-only getters, `isEmpty()`, `getTotal()`, and a public `setStatus()`.
  7. `OrderItem` exposes `getMenuItem()`, `getQuantity()`, and `getLineTotal()`. `StatusInfo` exposes `getTicket()`, `getStatus()`, `getPosition()`, and the constant `NOT_IN_LINE` (-1).
  8. The view prompt methods return the raw text the user typed (`String`), not an `int` as the diagram shows. The spec says the controller catches letters and blank entries, which it cannot do if the view has already parsed the input.
  9. The spec's `validateQuantity` description said `1 < q < 99`, which would wrongly reject 1 and 99. The intended rule, which the controller and model both use, is 1 to 99 inclusive.

- **Where validation lives.** The design split was: the controller catches malformed input (letters, blank entries, a menu choice that doesn't exist, a non-numeric ticket) and re-prompts up to three times, and the model enforces business rules. We built the model side as a full safety net. It throws `IllegalArgumentException` for null items, items not on the menu, out-of-stock items, and quantities outside 1 to 99. It throws `IllegalStateException` for submitting an empty order, completing from an empty line, and cancelling or remaking an order in the wrong state. An unknown ticket in `getStatus` is still a normal outcome (`NOT_FOUND`), but `cancelOrder` and `remakeOrder` throw on an unknown ticket because there is no sensible default there. Inside the model the 1-99 range is enforced in one place, the `OrderItem` constructor, which both `Order.addItem` and `OrderUpModel.addToOrder` go through. The controller's `validateQuantity` checks the same range first so it can show a friendly message.

- **Test cases.** The spec's test plan had four methods. Our tests cover them and add more, but several details differ from the plan:
  1. The plan said all tests run against model classes only. We also wrote `OrderUpControllerTest`, which drives the controller with scripted input and checks what the views print.
  2. The plan used `cancelOrder(String orderId)` returning false on bad input. We built `cancelOrder(int ticket)` returning void and throwing, because tickets are `int` everywhere else in the design and exceptions match how the rest of the model reports invalid state.
  3. The plan's "already marked Completed" is the READY status in the enum. `addRemakeToFront` rejects any order that is not WAITING, and the model's `remakeOrder` only accepts READY orders.
  4. The plan said `dequeue` on an empty queue could return null or throw. We chose to throw (`NoSuchElementException` from the queue, `IllegalStateException` from the model), and peek returns null.
  5. The plan's "an order with negative/zero quantities" bad input for `enqueue` can no longer happen, because `Order.addItem` refuses to create such an order. That case is tested at `addItem` instead.

- **Did the model really stand alone?** Yes. Every model class imports only `java.util` and other model classes. No model class prints, reads input, or refers to a view or controller, and `OrderUpModelTest`, `KitchenQueueTest`, `OrderBookTest`, `OrderTest`, and `MenuItemTest` all run with no UI at all. Nothing had to be moved across the line to make the model run. Putting the pieces together did expose mismatches between files written separately: `KitchenQueue` was in its own package while everything else was not, `OrderStatus` lacked the `CANCELLED` value the model uses, and `Order`, `OrderItem`, and `StatusInfo` were missing constants and methods that the model, controller, and tests expected. We fixed those before the checkpoint. Places that are still less clean than the design says:
  1. `MenuItem.toString()` and `Order.toString()` format prices and status, which is presentation. The views build their own display strings from the getters, so these are only for debugging.
  2. `getMenu()` returns an unmodifiable list, but the `MenuItem` objects inside are the real ones and `setInStock` is public. A view holding a reference could change stock, which breaks the "views never change state" rule. We rely on convention here; a package-private setter would fix it.
  3. `addToOrder` checks `menu.contains(item)` by object identity, so the controller must pass the exact `MenuItem` objects returned by `getMenu()`, not copies. This will matter when wiring up a JavaFX list.
  4. On the controller/view line: the controller prints the main menu itself instead of using `MainMenuView`, which is display work sitting in the controller. `MainMenuView` is currently unused.

- **What happened to our unknowns?** Our scoping document listed three.
  1. *The exact system architecture and how our methods relate.* Figured out. The specification's MVC diagram, with `OrderUpModel` as the single entry point to the model, is what we built, with the changes listed above.
  2. *How we will present the program to the user.* Partly figured out. This checkpoint uses console views, and the controller re-prompts on bad input. The JavaFX design for the final product is still open.
  3. *Whether we can finish our stretch goals.* Still open. Cancellation is built and tested in the model but not reachable from the menu yet. Priced receipts have a start (`Order.getTotal()`), and the end-of-day summary is not started.

- **What doesn't work yet?**
  1. **Cancel and remake are not reachable from the program.** Both are built and tested in the model, but the controller has no menu option for them.
  2. **Cancelled orders display wrongly.** `CustomerStatusView.showStatus` would show a CANCELLED order as "not found" instead of "Cancelled".
  3. **The cashier can't see the order being built.** "Start new order" only clears the draft. Nothing appears in the kitchen until the order is submitted, and there is no option to view the draft before submitting, which confused us during testing.
  4. **No persistence.** Every run starts with an empty order book.
  5. **Not built yet:** priced receipts, the end-of-day sales summary, and the JavaFX views.

## Next Steps

- **What is left to finish the MVP**, in order:
  1. Make the cashier flow clearer: say what to do after starting an order, and add a way to view the current order before submitting.
  2. Have the controller use `MainMenuView` for the main menu, or delete it.
  3. Run the full flow by hand (cashier places an order, cook completes it, customer checks status) plus every bad-input case in the validation list, and fix what breaks.
  4. Build the three JavaFX views as display-only classes that get everything from the controller, then hook each screen's buttons to controller methods.

- **Our change feature: rush orders.** The cashier can mark an order as "rush." A rush order goes to the front of the line, behind any remakes already at the front but ahead of normal orders.

- **Queue Implementation** The project requires our own implementation for the queue data structure, not using a premade library. This discrepancy in understanding is something we have to correct

  _Predicted to change:_
  - **Model:** `Order` gets a `rush` boolean field; `KitchenQueue` gets an insertion rule for rush orders; `OrderUpModel.submitOrder` gets a rush parameter or an overload.
  - **Controller:** `handleSubmitOrder` (or a new `handleSubmitRush`) passes the flag along, plus a prompt for it.
  - **View:** `CashierView` needs a rush prompt or toggle.

  _Predicted not to change:_ `KitchenView` and `CustomerStatusView`, because they already show whatever order is next and whatever position the controller passes in. Also `OrderBook`, `MenuItem`, `OrderItem`, `OrderStatus`, and `StatusInfo`. We will compare this prediction to what actually changes and report any difference.

- **Stretch goals**
  - **Committing to:** order cancellation, our primary stretch goal. The model side (`cancelOrder`, the CANCELLED status, queue skipping) is already built and tested, so the remaining work is a controller handler, a cancel option in the cashier view, and showing "Cancelled" in the customer view.
  - **Secondary / likely dropping:** priced receipts and the end-of-day sales summary. We will only attempt them if the MVP, cancellation, and rush orders are all finished and tested; otherwise they are dropped.

- **New tests needed**
  1. Rush orders: a rush order lands ahead of normal orders but behind a remake; two rush orders stay in submission order; a rush order can be cancelled; positions are reported correctly.
  2. Controller cancel: cancelling a waiting order, an unknown ticket, a READY order, an already-cancelled order, and non-numeric ticket input, all without crashing.
  3. `CustomerStatusView` shows "Cancelled" for a cancelled order.
  4. The new "view current order" option, including on an empty draft.

- **Who does what**
  - Vanessa: rush orders in the model (`Order`, `KitchenQueue`, `OrderUpModel`) and their tests.
  - Rishad: controller handlers for cancel and rush, and the controller tests for them.
  - Aryaman: view changes (cancel and rush prompts, "Cancelled" status, current-order display, `MainMenuView`) and the JavaFX views.

## Attribution

| File | Who | How |
| --- | --- | --- |
| `MenuItem.java` | Aryaman | GenAI-drafted from our spec; reviewed by Vanessa |
| `OrderItem.java` | Aryaman | GenAI-drafted from our spec; reviewed by Vanessa; revised with GenAI to fix integration mismatches |
| `OrderStatus.java` | Aryaman | GenAI-drafted from our spec; reviewed by Vanessa; revised with GenAI to add CANCELLED |
| `StatusInfo.java` | Aryaman | GenAI-drafted from our spec; reviewed by Vanessa; revised with GenAI to fix integration mismatches |
| `Order.java` | Aryaman | GenAI-drafted from our spec; reviewed by Vanessa; revised with GenAI to add quantity constants |
| `KitchenQueue.java` | Vanessa | GenAI-drafted from our spec; reviewed by Vanessa; revised with GenAI to remove a package mismatch |
| `OrderUpModel.java` | Rishad | GenAI-drafted from our spec; reviewed by Rishad |
| `OrderBook.java` | Rishad | GenAI-drafted from our spec; reviewed by Rishad |
| `OrderUpController.java` | Rishad | GenAI-drafted from our spec; reviewed by Rishad |
| `OrderUpApp.java` | Rishad | GenAI-drafted from our spec; reviewed by Rishad |
| `CashierView.java` | Aryaman | GenAI-drafted from our spec; reviewed by Aryaman |
| `KitchenView.java` | Aryaman | GenAI-drafted from our spec; reviewed by Aryaman |
| `CustomerStatusView.java` | Aryaman | GenAI-drafted from our spec; reviewed by Aryaman |
| `MainMenuView.java` | Aryaman | GenAI-drafted from our spec; reviewed by Aryaman |
| `MenuItemTest.java` | Aryaman | GenAI-drafted from our test plan; ran and reviewed by Aryaman |
| `OrderTest.java` | Aryaman | GenAI-drafted from our test plan; ran and reviewed by Aryaman |
| `KitchenQueueTest.java` | Vanessa | GenAI-drafted from our test plan; ran and reviewed by Vanessa |
| `OrderBookTest.java` | Rishad | GenAI-drafted from our test plan; ran and reviewed by Rishad |
| `OrderUpModelTest.java` | Rishad | GenAI-drafted from our test plan; ran and reviewed by Rishad |
| `OrderUpControllerTest.java` | Rishad | GenAI-drafted from our test plan; ran and reviewed by Rishad |
| `README.md` | Vanessa, Aryaman | GenAI-drafted; revised by Vanessa and Aryaman |

## GenAI Note

1. **Design review.** Claude reviewed our system diagram and helped us talk through the deque-versus-queue and flag-versus-remove decisions.
2. **Specification text.** Claude helped draft the method-signature and validation sections of the spec from the system diagram, which we then edited.
3. **Code and tests.** Claude drafted the model classes, the console views, the controller, and the test files from our spec and diagram. Each owner read over their files, and we ran the tests ourselves.
4. **Integration check.** Because the files were drafted separately, they did not compile together at first. We used Claude Code to compile the whole project and run every test. It found five mismatches between files (a stray package declaration, the missing CANCELLED status, missing quantity constants, a renamed getter, and a different `StatusInfo` constructor) and one real bug (the model accepted a quantity of 100). With our approval it made those six fixes, after which all 144 tests passed.
5. **README.** Claude drafted this README from our spec, the code, and the test results, and we edited it.
