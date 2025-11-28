<script>
    import Orderlist from "./orderlist.svelte";
    import Orderlines from "./orderlines.svelte";

    // Yes this is comment

    const testOrders = [
      {
        orderId: 1,
        customerId: 2,
        orderCreated: 3,
        orderLines: [
          {
            orderLineId: 1,
            orderId: 1,
            itemId: 1,
            priceSnapshot: 1.0,
            amount: 1,
          },
          {
            orderLineId: 1,
            orderId: 1,
            itemId: 1,
            priceSnapshot: 1.0,
            amount: 1,
          },
        ],
      },
      {
        orderId: 2,
        customerId: 3,
        orderCreated: 4,
        orderLines: [
          {
            orderLineId: 1,
            orderId: 2,
            itemId: 1,
            priceSnapshot: 1.0,
            amount: 1,
          },
          {
            orderLineId: 2,
            orderId: 2,
            itemId: 3,
            priceSnapshot: 2.0,
            amount: 2,
          },
        ],
      },
    ];

    let activeOrder = $state(null);

    function setActiveOrder(order) {
        activeOrder = order;
    }

</script>

<div class="h-container">
    <Orderlist orders={testOrders} setActiveOrder={setActiveOrder}/>
    <Orderlines activeOrder={activeOrder}/>
</div>
