<script>
  let { activeOrder } = $props();

</script>

<div class="container">
  <h1>Orderlines</h1>
  {#if activeOrder}
    <strong>Order {activeOrder.orderId}</strong>
    <ul>
      {#each activeOrder.orderLines as line}
        <li>
          Item {line.itemId} - Unit Price: {line.priceSnapshot} - Amount: {line.amount}
        </li>
      {/each}
      <strong
        >Total: {activeOrder.orderLines.reduce(
          (sum, l) => sum + l.priceSnapshot * l.amount,
          0
        )}</strong
      >
    </ul>
  {:else}
    <p>Select an order</p>
  {/if}
</div>
