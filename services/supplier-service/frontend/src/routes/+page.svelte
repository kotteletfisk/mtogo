<script lang="ts">
  import Orderlist from "./orderlist.svelte";
  import Orderlines from "./orderlines.svelte";
  import type { Order } from "$lib/types";
  import type { OrderLine } from "$lib/types";
  import type { APIException } from "$lib/types";

  // Yes this is comment

let errmsg: string | null = $state(null);
const testOrders = [
  {
    orderId: "a1f0b2f9-8321-4c7e-95b7-11bd8a0e7e11",
    customerId: 10,
    supplierId: 1,
    orderCreated: "2025-12-07 12:10:11.100",
    orderUpdated: "2025-12-07 12:10:11.100",
    orderlineDTOs: [
      {
        orderLineId: 1,
        orderId: "a1f0b2f9-8321-4c7e-95b7-11bd8a0e7e11",
        itemId: 1,
        priceSnapshot: 4.99,
        amount: 2
      },
      {
        orderLineId: 2,
        orderId: "a1f0b2f9-8321-4c7e-95b7-11bd8a0e7e11",
        itemId: 3,
        priceSnapshot: 2.50,
        amount: 1
      }
    ],
    orderStatus: "created"
  },

  {
    orderId: "bb902cd2-ae22-4fa0-b8c4-0a4cfa5f1222",
    customerId: 14,
    supplierId: 1,
    orderCreated: "2025-12-07 13:22:45.340",
    orderUpdated: "2025-12-07 13:22:45.340",
    orderlineDTOs: [
      {
        orderLineId: 1,
        orderId: "bb902cd2-ae22-4fa0-b8c4-0a4cfa5f1222",
        itemId: 2,
        priceSnapshot: 7.25,
        amount: 5
      }
    ],
    orderStatus: "created"
  },

  {
    orderId: "d1227d9e-2f77-49ce-a927-96b711a9aa33",
    customerId: 21,
    supplierId: 1,
    orderCreated: "2025-12-07 15:44:01.903",
    orderUpdated: "2025-12-07 15:44:01.903",
    orderlineDTOs: [
      {
        orderLineId: 1,
        orderId: "d1227d9e-2f77-49ce-a927-96b711a9aa33",
        itemId: 4,
        priceSnapshot: 1.99,
        amount: 3
      },
      {
        orderLineId: 2,
        orderId: "d1227d9e-2f77-49ce-a927-96b711a9aa33",
        itemId: 1,
        priceSnapshot: 3.00,
        amount: 1
      }
    ],
    orderStatus: "created"
  }
];

  let activeOrder: Order | null = $state(null);
  let orders: Order[] = $state([]);

  function setActiveOrder(order: Order) {
    activeOrder = order;
  }

  function setOrders(inputOrders: Order[]) {
    orders = inputOrders;
  }


  async function fetchOrders(supplierId: number): Promise<void> {
    console.log("fetching orders for " + supplierId);
    const response = await fetch(`/api/orders?supplierId=${supplierId}`, {
      method: "GET",
      headers: {
        "Content-Type": "application/json",
      },
    });

    if (!response.ok) {
      errmsg = `${response.status}: ${response.statusText}`;
    }

    //const data = (await response.json()) as Order[];

    setOrders(testOrders);
  }
</script>

<div class="page">
  <h1>Orders</h1>
  <p class="subtitle">View placed orders and inspect individual orderlines</p>

  <div class="section grid">
    <Orderlist {orders} {setActiveOrder} />
    <Orderlines {activeOrder} />
  </div>

  <div class="actions">
    <button class="btn btn-primary" onclick={() => fetchOrders(1)}>
      Get Orders
    </button>
  </div>

  {#if errmsg}
    <div class="status-message status-error">
      {errmsg}
    </div>
  {/if}
</div>
