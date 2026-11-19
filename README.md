# OrderUp!
Repo for CS 2114, project 3 (Group 52). Order Up! is ordering software for a food truck or other small restaurant, written in Java. It has one model and one controller with two sets of views: a console version and a JavaFX version with separate Cashier, Kitchen, and Customer screens.

## Project layout

- `src/` holds the model, the controller, the console views, `OrderUpApp` (console `main`), and all the tests. It compiles without JavaFX.
- `fx/` holds the JavaFX screens and `OrderUpFxApp` (JavaFX `main`). Only this folder needs JavaFX.
- `lib/junit-platform-console-standalone-1.14.4.jar` runs the tests from the command line.

## How to compile and run

Run these commands from the repository folder. They are written for Windows (PowerShell or Command Prompt). On macOS or Linux, use `:` instead of `;` between classpath entries, and use `/` instead of `\` in paths.

**Console version** (needs only a JDK, version 17 or newer):

```
javac -d bin -sourcepath src src\OrderUpApp.java
java -cp bin OrderUpApp
```

The console menu is grouped by role. Cashier: 1 start a new order, 2 add an item, 7 view the current order, 3 submit the order (it then asks "Rush order? (y/n)"), 8 cancel an order. Kitchen: 4 show the next order, 5 mark the next order done, 9 remake a finished order. Customer: 6 check an order's status. 0 or `q` quits. An order gets a ticket number and appears in the kitchen line only after it is submitted.

**JavaFX version** (needs the JavaFX SDK, for example OpenJFX 21 from https://gluonhq.com/products/javafx/; replace `C:\javafx-sdk-21\lib` with the `lib` folder of your copy):

```
javac -d bin --module-path C:\javafx-sdk-21\lib --add-modules javafx.controls -sourcepath src fx\*.java
java --module-path C:\javafx-sdk-21\lib --add-modules javafx.controls -cp bin OrderUpFxApp
```

The buttons along the top switch between the Cashier, Kitchen, and Customer screens. Each screen only has the actions for that role. For example, "Complete next order" is only on the Kitchen screen.

**Tests.** Three test classes extend `student.TestCase`, so you need `student.jar` from the CS 2114 support files; replace `C:\path\to\student.jar` with where yours is. Those three classes need **Java 17** to run, because `student.jar` installs a Security Manager, which Java 24 and later removed. Eclipse's bundled Java 17 works. Compile with `--release 17`, then run with a Java 17 `java`:

```
javac --release 17 -d bin -cp "lib\junit-platform-console-standalone-1.14.4.jar;C:\path\to\student.jar" src\*.java
java -jar lib\junit-platform-console-standalone-1.14.4.jar execute --class-path "bin;C:\path\to\student.jar" --scan-class-path bin
```

The result is 233 tests found and 233 successful across seven classes: `LinkedDequeTest`, `KitchenQueueTest`, `OrderTest`, `MenuItemTest`, `OrderBookTest`, `OrderUpModelTest`, and `OrderUpControllerTest`. In Eclipse, add `student.jar` and JUnit 5 to the build path, set the JRE to Java 17, and run the `src` folder as a JUnit test.

## Reflection

### How did our design absorb the change?

Our change feature is **rush orders**, as named in our Deliverable 3 Next Steps. The cashier can mark an order as rush. A rush order goes ahead of all regular orders, but behind any remakes and any earlier rush orders, so the line is always [remakes][rush orders][regular orders].

Classes the feature actually touched, compared with our D3 prediction:

| Class | Predicted? | What changed |
| --- | --- | --- |
| `Order` | Yes | `rush` field, `Order(int ticket, boolean rush)` constructor, `isRush()`. The old constructor still makes a regular order. |
| `KitchenQueue` | Yes | `enqueueRush(Order)`. `addRemakeToFront` now marks the start of the priority region. |
| `LinkedDeque` | **No (new class)** | Our own generic doubly linked deque, which replaced `java.util.ArrayDeque`. It keeps a pointer to the last node of the priority region, so a rush insert takes constant time and rush orders stay in submission order. |
| `OrderUpModel` | Yes | `submitOrder(boolean rush)`; `submitOrder()` calls it with `false`, so existing callers did not change. |
| `OrderUpController` | Yes | `handleSubmitOrder(boolean)` and the "Rush order? (y/n)" prompt, with the same three-tries rule as the other prompts. |
| `CashierView` | Yes | `promptRush()` (returns the raw text) and `showRushOrderPlaced(ticket)`. `showOrderPlaced` is unchanged. |
| Tests | Yes | `KitchenQueueTest`, the new `LinkedDequeTest`, `OrderUpModelTest`, `OrderUpControllerTest`. |

**What matched:** every class we predicted would change did change, and in the way we described. No class we said should *not* change had to change for rush orders: `KitchenView`, `OrderBook`, `MenuItem`, `OrderItem`, `OrderStatus`, and `StatusInfo` are untouched since the Deliverable 3 checkpoint (commit `1c8ff92`). `CustomerStatusView` gained one `case` since then, but for our cancellation stretch goal, not for rush: it showed a cancelled order as "not found", and it now says "was cancelled". Rush orders needed nothing from it, since the customer screen already showed whatever position the controller passed in.

**What surprised us:** the queue itself. We predicted "an insertion rule" in `KitchenQueue`, but a rush order has to land in the *middle* of the line, and our D3 queue (an `ArrayDeque`) can only add at the two ends. The course also requires our own queue implementation. So the feature brought in a new class, `LinkedDeque`, with a boundary pointer for the priority region. That changed how remakes are added too, since a remake now has to start or extend that region.

### Did the change cross the model/view line?

Barely, and only in the read-only direction. The model learned nothing about any view. The only new thing a view reads from the model is `Order.isRush()`, which the JavaFX kitchen screen uses to tag a rush order "(RUSH)". The console `KitchenView` did not change at all. The controller's `handleSubmitOrder(boolean)` is shared by both UIs: the console answers it with a y/n prompt and the JavaFX screen with a "Rush order" checkbox, and neither difference reached the model.

There was one ripple along the way, which we fixed. The first version of the controller printed the rush confirmation and the cancel confirmation with its own `println`, because `CashierView` had no method for them yet. We added `showRushOrderPlaced` and `showOrderCancelled` to `CashierView` and wired the controller's main menu through `MainMenuView`. The controller now never prints or reads anything itself.

### What changed from our D3 plan?

- **Queue implementation.** D3 used `java.util.ArrayDeque`. We now use our own `LinkedDeque` (see above), with `KitchenQueue`'s public methods unchanged, so the earlier tests still pass.
- **How the cashier picks rush.** D3 said "`handleSubmitOrder` (or a new `handleSubmitRush`)" plus a prompt. The first version added a separate "submit as rush" menu option. We replaced it with a "Rush order? (y/n)" question after Submit, which accepts y, yes, n, or no in any case and re-prompts on anything else (blank, "maybe") up to three times. If the prompt gives up, nothing is submitted and the items stay on the order.
- **View current order.** D3 listed this as the most confusing gap. It needed a model change we had not planned: `OrderUpModel.getCurrentItems()`, a read-only view of the order being built. There is a new `CashierView.showCurrentOrder` and a controller handler (console option 7, and a JavaFX button).
- **Main menu.** `MainMenuView` is now used. The controller's constructor takes it instead of a `Scanner` and `PrintStream`.
- **Cancel and remake are reachable.** In D3 they were model-only. They are now console options 8 and 9 and buttons on the Cashier and Kitchen screens.
- **JavaFX started early.** `fx/` has three working screens that call the same controller methods as the console. To keep the controller in charge of validation, we added controller handlers that take typed text (`handleAddItem(String, String)`, `handleStatusLookup(String)`, `handleCancelOrder(String)`, `handleRemakeOrder(String)`).
- **Tests.** We went from 144 to 233 tests. They cover the deque on its own, interleaved enqueue/rush/remake/dequeue sequences checked after every step, cancelled rush orders, every rush-prompt answer (y, yes, n, NO, blank, "maybe", three bad answers, input ending at the prompt), the view-current-order option, the typed-text handlers, and a cancelled order's status lookup.

### What doesn't work yet?

1. **The JavaFX screens don't refresh themselves.** The kitchen screen refreshes when you switch to it or press "Show next order", and the customer screen updates only when you press "Check status". There is no live update between screens.
2. **No way to change stock from either UI.** Out-of-stock items are rejected and marked on the menu, but only code can mark an item out of stock.
3. **No persistence.** Every run starts with an empty order book.
4. **Not built:** priced receipts and the end-of-day sales summary.
5. **Not unit tested:** the JavaFX screens, which is intended since the tests target the model and controller. We checked the JavaFX screens with a scripted click-through instead.

### What design change would make the next feature cheaper?

Give the queue a general priority-insert rule instead of one special case per feature. Right now `LinkedDeque` knows about exactly one priority region (remakes and rush orders) through `addFirstPriority` and `addAfterPriority`. A third kind of order, such as a mobile pickup lane or VIP orders, would need yet another special-case method and pointer. If each `Order` had a priority level (for example remake = 0, rush = 1, regular = 2) and the deque kept a "last node" pointer per level, a new kind of order would only need a new level. `KitchenQueue` and the deque would not change again.

A second cheaper path: the JavaFX screens extend the console view classes so that the controller didn't have to change. A small view interface per role would make adding a third UI, or a fake view for tests, simpler.

## Next Steps

### What is left for the final build, in order

1. JavaFX polish: refresh the kitchen and customer screens after changes on another screen, tidy the layout, and disable buttons that can't do anything (such as Submit on an empty order).
2. Fill in the placeholder Javadoc on `KitchenQueue.isEmpty()` and any other leftover comments.
3. Stretch goals (see below).
4. Final README update and attribution, then rehearse the demo.

### Stretch goals

- **Committing to:** order cancellation. This is done: it's in the model, the controller, the console menu, and the JavaFX Cashier screen, with tests.
- **Dropping unless time allows:** priced receipts and the end-of-day sales summary. We will only attempt them after items 1 and 2 above are finished and tested.

### What will our 3-minute demo show?

We'll use the JavaFX version (`OrderUpFxApp`). Times are cumulative and leave a 10-second buffer.

| Time | Step |
| --- | --- |
| 0:00–0:15 | Launch the app; point out the Cashier / Kitchen / Customer buttons and that one model and controller sit behind all three screens. |
| 0:15–0:45 | **Cashier:** select Cheeseburger, quantity 2, Add item; the current order shows 2 x Cheeseburger with a total. Submit, giving ticket #1. Add Fries and submit, giving ticket #2. |
| 0:45–1:10 | **Bad input:** quantity `abc` shows an error; quantity `100` shows an error; Submit on an empty order shows an error. Nothing crashes. |
| 1:10–1:35 | **Change feature:** add Lemonade, tick "Rush order", Submit, giving "Rush order placed! Ticket #3". |
| 1:35–2:05 | **Kitchen:** ticket #3 (RUSH) is first even though it was submitted last; 3 waiting. Complete next order, so #3 is ready and #1 is next. Remake #3 and it goes back to the front. |
| 2:05–2:30 | **Cancel + Customer:** on Cashier, cancel #2. On Customer, check #2 (cancelled), #1 (waiting, position 2), #99 (not found), and blank (error). |
| 2:30–2:50 | **Tests:** show the test command's summary, with 233 tests found and 233 successful. |

### Who does what

- **Aryaman:** item 1 (JavaFX), item 4 (README and attribution), and running the demo.
- **Vanessa:** item 2 (`KitchenQueue` Javadoc) and the model side of any stretch goal we attempt.
- **Rishad:** the controller side of any stretch goal we attempt, and the controller tests for it.
- **Everyone:** rehearse the demo with a stopwatch to keep it under 3:00.

## Attribution

| File | Who | How |
| --- | --- | --- |
| `MenuItem.java` | Aryaman | reviewed by Vanessa |
| `OrderItem.java` | Aryaman | reviewed by Vanessa; revised with GenAI to fix integration mismatches |
| `OrderStatus.java` | Aryaman | reviewed by Vanessa; revised to add CANCELLED |
| `StatusInfo.java` | Aryaman | reviewed by Vanessa; revised with GenAI to fix integration mismatches |
| `Order.java` | Aryaman, Vanessa | Aryaman: GenAI-drafted from our spec. Vanessa: rush flag |
| `LinkedDeque.java` | Vanessa |
| `KitchenQueue.java` | Vanessa|
| `OrderBook.java` | Rishad | GenAI-drafted from our spec; reviewed by Rishad |
| `OrderUpModel.java` | Rishad, Aryaman | Rishad: GenAI-drafted from our spec and plan (rush, cancel, remake); reviewed by Rishad. Aryaman: `getCurrentItems()` |
| `OrderUpController.java` | Rishad, Aryaman | Rishad: GenAI-drafted from our spec and plan (handlers for rush, cancel, remake); reviewed by Rishad. Aryaman: rush y/n prompt, view current order, `MainMenuView` wiring, typed-text handlers, GenAI-drafted with Claude Code; reviewed by Aryaman |
| `OrderUpApp.java` | Rishad, Aryaman | reviewed by Rishad. Aryaman: `MainMenuView` wiring |
| `CashierView.java` | Aryaman |
| `KitchenView.java` | Aryaman | 
| `CustomerStatusView.java` | Aryaman | 
| `MainMenuView.java` | Aryaman |
| `fx/FxCashierView.java` | Aryaman | tested with a scripted click-through using Claude |
| `fx/FxKitchenView.java` | Aryaman | tested with a scripted click-through using Claude|
| `fx/FxCustomerStatusView.java` | Aryaman | tested with a scripted click-through using Claude|
| `fx/OrderUpFxApp.java` | Aryaman | 
| `MenuItemTest.java` | Aryaman | GenAI-drafted from our test plan |
| `OrderTest.java` | Aryaman | GenAI-drafted from our test plan; ran and reviewed by Aryaman |
| `LinkedDequeTest.java` | Vanessa | GenAI-drafted from our plan; ran and reviewed by Vanessa |
| `KitchenQueueTest.java` | Vanessa | GenAI-drafted from our test plan and plan (rush tests); ran and reviewed by Vanessa |
| `OrderBookTest.java` | Rishad | GenAI-drafted from our test plan; ran and reviewed by Rishad |
| `OrderUpModelTest.java` | Rishad, Aryaman | Rishad: GenAI-drafted from our test plan and plan; ran and reviewed by Rishad. Aryaman: `getCurrentItems` tests, GenAI-drafted with Claude Code |
| `OrderUpControllerTest.java` | Rishad, Aryaman | Rishad: GenAI-drafted from our test plan and plan; ran and reviewed by Rishad. Aryaman: rush-prompt, view-current-order, main-menu, and typed-text tests, GenAI-drafted with Claude Code |
| `README.md` | Vanessa, Aryaman, Rishad | GenAI-drafted; revised by the team |

## GenAI Note

1. **README.** Claude drafted this README from our spec, the code, `git diff --stat` against the D3 checkpoint, and the test results, and we edited it.
2. **TESTS** Using our implemented classes, Claude wrote and complied tests for said classes ensuring all bad inputs were handled and tested for.

## Deliverable 3 checkpoint reflection (kept for reference)

These notes describe what changed between our specification and the first prototype.

- **Classes.** We built the seven model classes from the spec plus `StatusInfo`, which holds a ticket number, status, and queue position because `getStatus` has to return both a status and a position. We also added `MainMenuView` and `OrderUpApp`.
- **Fields.** `OrderUpModel` keeps a `final List<OrderItem> draft` instead of the spec's `currentOrder: Order`. The `Order` is built at submit time once the ticket is known, so an abandoned order never uses up a ticket number.
- **Signatures.** We added `KitchenQueue.isEmpty()`, `OrderBook.register(Order)`, `OrderUpModel.startNewOrder()`, `cancelOrder(int)`, and `remakeOrder(int)`. Prompt methods return raw text so the controller can catch letters and blanks. Quantities are 1 to 99 inclusive; the spec's `1 < q < 99` was a typo.
- **Validation.** The controller catches malformed input and re-prompts up to three times. The model throws for business-rule violations as a safety net, and the 1–99 range is enforced in the `OrderItem` constructor.
- **Model independence.** No model class prints, reads input, or refers to a view or controller, and the model tests run with no UI.
