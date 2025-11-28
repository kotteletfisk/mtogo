<script>
    let { orders, setActiveOrder } = $props();

</script>

<div class="container">
  <h1>Placed Orders</h1>

  <div class="order-list">
    <table>
      <thead>
        <tr>
          <th>Order ID</th>
          <th>Customer ID</th>
          <th>Created</th>
        </tr>
      </thead>
      <tbody>
        {#each orders as o}
          <tr onclick={() => setActiveOrder(o)}>
            <td>
              {o.orderId}
            </td>
            <td>
              {o.customerId}
            </td>
            <td>
              {o.orderCreated}
            </td>
          </tr>
        {/each}
      </tbody>
    </table>
  </div>
</div>
