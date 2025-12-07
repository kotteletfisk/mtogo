<script lang="ts">
  import type { Order } from "$lib/types";
  let { activeOrder }: { activeOrder: Order | null } = $props();
</script>

<div class="section">
  <h2>Orderlines</h2>

  {#if activeOrder}
    <strong>Order {activeOrder.orderId}</strong>

    <ul class="orderlines-list">
      {#each activeOrder.orderlineDTOs as line}
        <li class="orderline-row">
          <span>Item {line.itemId}</span>
          <span>Unit Price: {line.priceSnapshot}</span>
          <span>Amount: {line.amount}</span>
        </li>
      {/each}
    </ul>

    <div class="order-total">
      <strong>
        Total:
        {
          activeOrder.orderlineDTOs.reduce(
            (sum, l) => sum + l.priceSnapshot * l.amount,
            0
          )
        }
      </strong>
    </div>

  {:else}
    <p>Select an order</p>
  {/if}
</div>
