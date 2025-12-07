<script lang="ts">
  import Orderlist from "./orderlist.svelte";
  import Orderlines from "./orderlines.svelte";

  // Yes this is comment

  let orders = $state([]);
  let errmsg;
  const testOrders = [
    {
      orderId: 1,
      customerId: 2,
      supplierId: 1,
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
      supplierId: 2,
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

  async function fetchOrders(supplierId) {
    console.log("fetching orders for " + supplierId);
    const response = await fetch(`/api/orders?supplierId=${supplierId}`, {
      method: "GET",
      headers: {
        "Content-Type": "application/json",
      }
    })

    if (!response.ok) {
        throw new Error(`HTTP error: status: ${response.status}`);
    }

    const data = await response.json();

    orders = data;
  }
</script>

<div>
  <div class="h-container">
    <Orderlist {orders} {setActiveOrder} />
    <Orderlines {activeOrder} />
  </div>
  <button onclick={() => fetchOrders(1)}>Get Orders</button>
  <div class="error">{errmsg}</div>
</div>
