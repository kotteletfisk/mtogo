<script lang="ts">
  import type { Order } from "$lib/types";
  export let orders: Order[];
  export let setActiveOrder: (order: Order) => void;
</script>

<div class="section">
  <h2>Placed Orders</h2>

  <div class="order-list">
    <table class="styled-table">
      <thead>
        <tr>
          <th>Order ID</th>
          <th>Customer</th>
          <th>Created</th>
        </tr>
      </thead>

      <tbody>
        {#each orders as o}
          <tr onclick={() => setActiveOrder(o)} class="clickable-row">
            <td>{o.orderId}</td>
            <td>{o.customerId}</td>
            <td>{o.orderCreated}</td>
          </tr>
        {/each}
      </tbody>
    </table>
  </div>
</div>
